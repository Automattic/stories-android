package com.automattic.photoeditor.util

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.fragment.app.FragmentActivity
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import java.lang.Long.signum
import java.util.Collections
import java.util.Comparator
import kotlin.math.max

class CameraUtils {
    companion object {
        private const val TAG = "CameraUtils"
        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        const val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        const val MAX_PREVIEW_HEIGHT = 1080

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
            aspectRatio: Size,
            fitAspectRatioStrictly: Boolean = false
        ): Size {
            // Collect the supported resolutions that are at least as big as the preview Surface
            var bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            var notBigEnough = ArrayList<Size>()
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

            if (fitAspectRatioStrictly) {
                bigEnough = ArrayList(filterOutByAspectRatio(bigEnough, aspectRatio))
                notBigEnough = ArrayList(filterOutByAspectRatio(notBigEnough, aspectRatio))
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            return when {
                bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
                notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
                else -> {
                    Log.e(TAG, "Couldn't find any suitable preview size")
                    choices[0]
                }
            }
        }

        fun filterOutByAspectRatio(sizes: List<Size>, aspectRatio: Size): List<Size> {
            val ratio: Float = aspectRatio.width.toFloat() / aspectRatio.height.toFloat()
            return sizes.filter { (it.width.toFloat() / it.height.toFloat()) == ratio }
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

        fun calculateOptimalCameraPreviewSize(
            activity: FragmentActivity,
            textureView: AutoFitTextureView,
            cameraId: String,
            fitAspectRatio: Boolean
        ): Size {
            // Get screen metrics used to setup camera for full screen resolution
            val metrics = DisplayMetrics().also { textureView.display.getRealMetrics(it) }
            val displaySize = Point(metrics.widthPixels, metrics.heightPixels)
            val optimalPreviewSize: Size
            Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

            val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(cameraId)

            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw IllegalStateException("Could not obtain SCALER_STREAM_CONFIGURATION_MAP")

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            val displayRotation = activity.windowManager.defaultDisplay.rotation

            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            val swappedDimensions = areDimensionsSwapped(displayRotation, sensorOrientation)

            val height = textureView.height
            val width = textureView.width
            val rotatedPreviewWidth = if (swappedDimensions) height else width
            val rotatedPreviewHeight = if (swappedDimensions) width else height
            var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
            var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

            // For still image captures, we use the largest available size.
            var largest = Collections.max(listOf(*map.getOutputSizes(ImageFormat.JPEG)), CompareSizesByArea())

            // if we need to fit the aspect ratio of both the preview feed and rendering surface, only choose the
            // largest value that matches the preview surface's aspect ratio
            if (fitAspectRatio) {
                val filteredList = filterOutByAspectRatio(
                        listOf(*map.getOutputSizes(ImageFormat.JPEG)),
                        Size(maxPreviewWidth, maxPreviewHeight)
                )
                if (filteredList.isNotEmpty()) {
                    largest = Collections.max(filteredList, CompareSizesByArea())
                }
            }

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            optimalPreviewSize = chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java),
                rotatedPreviewWidth, rotatedPreviewHeight,
                maxPreviewWidth, maxPreviewHeight,
                largest,
                fitAspectRatio
            )
            return optimalPreviewSize
        }

        /**
         * Configures the necessary [android.graphics.Matrix] transformation to `textureView`.
         * This method should be called after the camera preview size is determined in
         * either with setUpCameraOutputs in Camera2BasicHandling or after CameraX preview use case has been bound,
         * and also the size of `textureView` is fixed.
         *
         * @param textureView the TextureView to which the transformation witll be applied
         * @param previewSize the Size of the Preview output by the Cameras
         */
        fun configureTransform(textureView: AutoFitTextureView, previewSize: Size) {
            val displayRotation = textureView.display.rotation
            val matrix = Matrix()
            val viewRect = RectF(0f, 0f, textureView.width.toFloat(), textureView.height.toFloat())
            val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
            val centerX = viewRect.centerX()
            val centerY = viewRect.centerY()

            if (Surface.ROTATION_90 == displayRotation || Surface.ROTATION_270 == displayRotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
                val scale = max(
                        textureView.height.toFloat() / previewSize.height,
                        textureView.width.toFloat() / previewSize.width)
                with(matrix) {
                    setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                    postScale(scale, scale, centerX, centerY)
                    postRotate((90 * (displayRotation - 2)).toFloat(), centerX, centerY)
                }
            } else if (Surface.ROTATION_180 == displayRotation) {
                matrix.postRotate(180f, centerX, centerY)
            }

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            if (Surface.ROTATION_90 == displayRotation || Surface.ROTATION_270 == displayRotation) {
                textureView.setAspectRatio(previewSize.width, previewSize.height)
            } else {
                textureView.setAspectRatio(previewSize.height, previewSize.width)
            }

            // finally apply the transform
            textureView.setTransform(matrix)
        }
    }

    internal class CompareSizesByArea : Comparator<Size> {
        // We cast here to ensure the multiplications won't overflow
        override fun compare(lhs: Size, rhs: Size) =
            signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
    }
}
