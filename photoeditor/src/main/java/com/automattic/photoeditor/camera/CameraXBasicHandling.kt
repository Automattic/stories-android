package com.automattic.photoeditor.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.ViewGroup
import androidx.camera.core.CameraX
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CaptureMode
import androidx.camera.core.ImageCapture.ImageCaptureError
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCaptureConfig
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.camera.core.VideoCapture
import androidx.camera.core.VideoCaptureConfig
import com.automattic.photoeditor.camera.interfaces.CameraSelection
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState
import com.automattic.photoeditor.camera.interfaces.ImageCaptureListener
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFinished
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFragment
import com.automattic.photoeditor.camera.interfaces.cameraXLensFacingFromStoriesCameraSelection
import com.automattic.photoeditor.camera.interfaces.cameraXflashModeFromStoriesFlashState
import com.automattic.photoeditor.camera.interfaces.storiesCameraSelectionFromCameraXLensFacing
import com.automattic.photoeditor.util.FileUtils
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import java.io.File

class CameraXBasicHandling : VideoRecorderFragment() {
    private var videoCapture: VideoCapture? = null
    private lateinit var videoPreview: Preview
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraX.LensFacing.BACK
    private var screenAspectRatio = Rational(9, 16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun activate() {
        if (!active) {
            CameraX.unbindAll()
            active = true
            startUp()
        }
    }

    override fun deactivate() {
        if (active) {
            active = false
            windDown()
        }
    }

    override fun isActive(): Boolean {
        return active
    }

    private fun startUp() {
        if (active) {
            startCamera()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun windDown() {
        videoPreview.clear()
        videoCapture?.clear()
        imageCapture?.clear()
        CameraX.unbindAll()
    }

    // TODO remove this RestrictedApi annotation once androidx.camera:camera moves out of alpha
    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { textureView.display.getRealMetrics(it) }
        screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        // retrieve flash availability for this camera
        val cameraId = CameraX.getCameraWithLensFacing(lensFacing)
        cameraId?.let {
            updateFlashSupported(it)
        }

        // Create configuration object for the preview use case
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            /*  From https://developer.android.com/jetpack/androidx/releases/camera#camera2-core-1.0.0-alpha06
                Aspect Ratios: For each use case, applications should call only one of setTargetResolution() or
                setTargetAspectRatio(). Calling both on the same builder will return an error.
                In general it’s recommended to use setTargetAspectRatio() based on the application’s UI design.
                Specific resolutions will be based on the use case. For example, preview will be near screen resolutions
                and image capture will provide high resolution stills. See the automatic resolutions table for more
                information. https://developer.android.com/training/camerax/configuration#automatic-resolution
                Use setTargetResolution() for more specific cases, such as when minimum (to save computation) or
                maximum resolutions (for processing details) are required.
             */
            // for now, we're calling setTargetAspectRatioCustom() with this device's screen aspect ratio, given
            // setting an aspect ratio of 4:3 would show undesired effects on alpha06 such as a stretched preview
            setTargetAspectRatioCustom(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(textureView.display.rotation)
        }.build()

        videoPreview = Preview(previewConfig)

        // Set up the capture use case to allow users to take photos
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setFlashMode(cameraXflashModeFromStoriesFlashState(currentFlashState.currentFlashState()))
            setCaptureMode(CaptureMode.MIN_LATENCY)
            // We request aspect ratio but no resolution to match preview config but letting
            // CameraX optimize for whatever specific resolution best fits requested capture mode
            // setTargetAspectRatio(RATIO_4_3)
            setTargetAspectRatioCustom(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(textureView.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        videoPreview.setOnPreviewOutputUpdateListener {
            // if, for whatever reason a pre-existing surfaceTexture was being used,
            // then call `release()`  on it, as per docs
            // https://developer.android.com/reference/androidx/camera/core/Preview.html#setOnPreviewOutputUpdateListener(androidx.camera.core.Preview.OnPreviewOutputUpdateListener)
            // * <p>Once {@link OnPreviewOutputUpdateListener#onUpdated(PreviewOutput)}  is called,
            //     * ownership of the {@link PreviewOutput} and its contents is transferred to the application. It
            //     * is the application's responsibility to release the last {@link SurfaceTexture} returned by
            //     * {@link PreviewOutput#getSurfaceTexture()} when a new SurfaceTexture is provided via an update
            //     * or when the user is finished with the use case.  A SurfaceTexture is created each time the
            //     * use case becomes active and no previous SurfaceTexture exists.
            textureView.surfaceTexture?.release()

            // Also removing and re-adding the TextureView here, due to the following reasons:
            // https://developer.android.com/reference/androidx/camera/core/Preview.html#setOnPreviewOutputUpdateListener(androidx.camera.core.Preview.OnPreviewOutputUpdateListener)
            // * Calling TextureView.setSurfaceTexture(SurfaceTexture) when the TextureView's SurfaceTexture is already
            // * created, should be preceded by calling ViewGroup.removeView(View) and ViewGroup.addView(View) on the
            // * parent view of the TextureView to ensure the setSurfaceTexture() call succeeds.
            val parent = textureView.parent as ViewGroup
            parent.removeView(textureView)
            parent.addView(textureView, 0)
            textureView.surfaceTexture = it.surfaceTexture
        }

        // we used to bind all use cases to lifecycle on start
        // DON'T do this, may end up with this: https://github.com/Automattic/stories-android/issues/50
        // CameraX.bindToLifecycle(activity, videoPreview, videoCapture, imageCapture)

        // image capture only
        CameraX.bindToLifecycle(activity, videoPreview, imageCapture)
    }

    @SuppressLint("RestrictedApi")
    override fun startRecordingVideo(finishedListener: VideoRecorderFinished?) {
        activity?.let {
            currentFile = FileUtils.getTempCaptureFile(it, true)
        }

        currentFile?.let {
            it.createNewFile()

            // unbind this use case for now, we'll re-bind later
            imageCapture?.let {
                imageCapture?.clear()
                if (CameraX.isBound(imageCapture)) {
                    CameraX.unbind(imageCapture)
                }
            }

            // if a previous instance exists, request to release muxer and buffers
            videoCapture?.let {
                if (CameraX.isBound(videoCapture)) {
                    CameraX.unbind(videoCapture)
                }
                videoCapture?.clear()
            }

            val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
                setLensFacing(lensFacing)
                setTargetAspectRatioCustom(screenAspectRatio)
                setTargetRotation(textureView.display.rotation)
            }.build()
            videoCapture = VideoCapture(videoCaptureConfig)

            // video capture only
            CameraX.bindToLifecycle(activity, videoCapture)

            videoCapture?.startRecording(
                it,
                AsyncTask.THREAD_POOL_EXECUTOR,
                object : VideoCapture.OnVideoSavedListener {
                    override fun onVideoSaved(file: File) {
                        Log.i(tag, "Video File : $file")
                        finishedListener?.onVideoSaved(file)
                    }
                    override fun onError(
                        useCaseError: VideoCapture.VideoCaptureError,
                        message: String,
                        cause: Throwable?
                    ) {
                        Log.i(tag, "Video Error: $message")
                        finishedListener?.onError(message, cause)
                    }
            })
        }
    }

    @SuppressLint("RestrictedApi")
    override fun stopRecordingVideo() {
        videoCapture?.stopRecording()
    }

    override fun takePicture(onImageCapturedListener: ImageCaptureListener) {
        // Create output file to hold the image
        context?.let { context ->
            currentFile = FileUtils.getTempCaptureFile(context, false).apply { createNewFile() }

            currentFile?.let {
                // Setup image capture metadata
                val metadata = Metadata().apply {
                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
                }

                // image capture only
                if (!CameraX.isBound(imageCapture)) {
                    CameraX.bindToLifecycle(activity, imageCapture)
                }

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture?.takePicture(
                    it,
                    metadata,
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    object : ImageCapture.OnImageSavedListener {
                        override fun onImageSaved(file: File) {
                            onImageCapturedListener.onImageSaved(file)
                        }

                        override fun onError(useCaseError: ImageCaptureError, message: String, cause: Throwable?) {
                            onImageCapturedListener.onError(message, cause)
                        }
                })
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun flipCamera(): CameraSelection {
        lensFacing = if (CameraX.LensFacing.FRONT == lensFacing) {
            CameraX.LensFacing.BACK
        } else {
            CameraX.LensFacing.FRONT
        }
        if (active) {
            try {
                // Only bind use cases if we can query a camera with this orientation
                val cameraId = CameraX.getCameraWithLensFacing(lensFacing)

                // retrieve flash availability for this camera
                cameraId?.let {
                    updateFlashSupported(it)
                }

                // Unbind all use cases and bind them again with the new lens facing configuration
                CameraX.unbindAll()
                startCamera()
            } catch (exc: Exception) {
                // no op - they can most probably just tap the flip switch again and it'll work
            }
        }
        return storiesCameraSelectionFromCameraXLensFacing(lensFacing)
    }

    override fun selectCamera(camera: CameraSelection) {
        lensFacing = cameraXLensFacingFromStoriesCameraSelection(camera)
    }

    override fun currentCamera(): CameraSelection {
        return storiesCameraSelectionFromCameraXLensFacing(lensFacing)
    }

    override fun advanceFlashState() {
        super.advanceFlashState()
        imageCapture?.flashMode = cameraXflashModeFromStoriesFlashState(currentFlashState.currentFlashState())
    }

    override fun setFlashState(flashIndicatorState: FlashIndicatorState) {
        super.setFlashState(flashIndicatorState)
        if (active) {
            imageCapture?.flashMode = cameraXflashModeFromStoriesFlashState(currentFlashState.currentFlashState())
        }
    }

    override fun isFlashAvailable(): Boolean {
        return flashSupported
    }

    private fun updateFlashSupported(cameraId: String) {
        val cameraManager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        flashSupported =
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    companion object {
        private val instance = CameraXBasicHandling()

        /**
         * Tag for the [Log].
         */
        private const val TAG = "CameraXBasicHandling"

        @JvmStatic fun getInstance(
            textureView: AutoFitTextureView,
            flashSupportChangeListener: FlashSupportChangeListener
        ): CameraXBasicHandling {
            instance.textureView = textureView
            instance.flashSupportChangeListener = flashSupportChangeListener
            return instance
        }
    }
}
