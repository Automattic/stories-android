package com.automattic.photoeditor.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.camera.core.CameraX
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import java.lang.Long
import java.util.Arrays
import java.util.Collections
import java.util.Comparator

class CameraUtils {
    companion object {
        private val TAG = "CameraUtils"
        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        val MAX_PREVIEW_HEIGHT = 1080

        /**
         * Given `choices` of `Size`s supported by a camera, choose the smallest one that
         * is at least as large as the respective texture view size, and that is at most as large as
         * the respective max size, and whose aspect ratio matches with the specified value. If such
         * size doesn't exist, choose the largest one that is at most as large as the respective max
         * size, and whose aspect ratio matches with the specified value.
         *
         * @param choices The list of sizes that the camera supports for the intended
         *                          output class
         * @param textureViewWidth The width of the texture view relative to sensor coordinate
         * @param textureViewHeight The height of the texture view relative to sensor coordinate
         * @param maxWidth The maximum width that can be chosen
         * @param maxHeight The maximum height that can be chosen
         * @param aspectRatio The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        @JvmStatic fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {
            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizesByArea())
            } else if (notBigEnough.size > 0) {
                return Collections.max(notBigEnough, CompareSizesByArea())
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                return choices[0]
            }
        }

        /**
         * Determines if the dimensions are swapped given the phone's current rotation.
         *
         * @param displayRotation The current rotation of the display
         *
         * @return true if the dimensions are swapped, false otherwise.
         */
        @JvmStatic fun areDimensionsSwapped(displayRotation: Int, sensorOrientation: Int): Boolean {
            var swappedDimensions = false
            when (displayRotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        swappedDimensions = true
                    }
                }
                Surface.ROTATION_90, Surface.ROTATION_270 -> {
                    if (sensorOrientation == 0 || sensorOrientation == 180) {
                        swappedDimensions = true
                    }
                }
                else -> {
                    Log.e(TAG, "Display rotation is invalid: $displayRotation")
                }
            }
            return swappedDimensions
        }

        fun setupOptimalCameraPreviewSize(
            activity: FragmentActivity,
            textureView: AutoFitTextureView,
            isCameraFacingBack: Boolean) : Size {
            // Get screen metrics used to setup camera for full screen resolution
            val metrics = DisplayMetrics().also { textureView.display.getRealMetrics(it) }
            val displaySize = Point(metrics.widthPixels, metrics.heightPixels)
            var optimalPreviewSize = Size(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
            Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

            val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraDirection != null && isCameraFacingBack && cameraDirection != CameraMetadata.LENS_FACING_BACK) {
                    continue
                }

                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                val displayRotation = activity!!.windowManager.defaultDisplay.rotation

                val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: continue
                val swappedDimensions = CameraUtils.areDimensionsSwapped(displayRotation, sensorOrientation)

                val height = textureView.height
                val width = textureView.width
                val rotatedPreviewWidth = if (swappedDimensions) height else width
                val rotatedPreviewHeight = if (swappedDimensions) width else height
                var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
                var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

                if (maxPreviewWidth > CameraUtils.MAX_PREVIEW_WIDTH) maxPreviewWidth =
                    CameraUtils.MAX_PREVIEW_WIDTH
                if (maxPreviewHeight > CameraUtils.MAX_PREVIEW_HEIGHT) maxPreviewHeight =
                    CameraUtils.MAX_PREVIEW_HEIGHT

                // For still image captures, we use the largest available size.
                val largest = Collections.max(
                    Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea()
                )

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                optimalPreviewSize = chooseOptimalSize(
                    map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewWidth, rotatedPreviewHeight,
                    maxPreviewWidth, maxPreviewHeight,
                    largest
                )

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(optimalPreviewSize.width, optimalPreviewSize.height)
                } else {
                    textureView.setAspectRatio(optimalPreviewSize.height, optimalPreviewSize.width)
                }
            }
            return optimalPreviewSize
        }
    }

    internal class CompareSizesByArea : Comparator<Size> {
        // We cast here to ensure the multiplications won't overflow
        override fun compare(lhs: Size, rhs: Size) =
            Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
    }
}
