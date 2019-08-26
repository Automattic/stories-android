package com.automattic.photoeditor.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.ViewGroup
import androidx.camera.core.CameraX
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CaptureMode
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCapture.UseCaseError
import androidx.camera.core.ImageCaptureConfig
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.camera.core.VideoCapture
import androidx.camera.core.VideoCaptureConfig
import com.automattic.photoeditor.camera.interfaces.CameraSelection
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState
import com.automattic.photoeditor.camera.interfaces.ImageCaptureListener
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFragment
import com.automattic.photoeditor.camera.interfaces.cameraXLensFacingFromPortkeyCameraSelection
import com.automattic.photoeditor.camera.interfaces.cameraXflashModeFromPortkeyFlashState
import com.automattic.photoeditor.camera.interfaces.portkeyCameraSelectionFromCameraXLensFacing
import com.automattic.photoeditor.util.FileUtils
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import java.io.File
import java.lang.Exception

class CameraXBasicHandling : VideoRecorderFragment() {
    private lateinit var videoCapture: VideoCapture
    private lateinit var videoPreview: Preview
    private lateinit var imageCapture: ImageCapture
    private var lensFacing = CameraX.LensFacing.BACK

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

    private fun startUp() {
        if (active) {
            startCamera()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun windDown() {
        videoPreview.clear()
        videoCapture.clear()
        imageCapture.clear()
        CameraX.unbindAll()
    }

    // TODO remove this RestrictedApi annotation once androidx.camera:camera moves out of alpha
    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { textureView.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        // retrieve flash availability for this camera
        val cameraId = CameraX.getCameraWithLensFacing(lensFacing)
        cameraId?.let {
            updateFlashSupported(cameraId)
        }

        // Create configuration object for the preview use case
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            // We request aspect ratio but no r esolution to let CameraX optimize our use cases
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(textureView.display.rotation)
        }.build()

        videoPreview = Preview(previewConfig)

        // Set up the capture use case to allow users to take photos
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setFlashMode(cameraXflashModeFromPortkeyFlashState(currentFlashState.currentFlashState()))
            setCaptureMode(CaptureMode.MIN_LATENCY)
            // We request aspect ratio but no resolution to match preview config but letting
            // CameraX optimize for whatever specific resolution best fits requested capture mode
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(textureView.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        // Create a configuration object for the video capture use case
        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetRotation(textureView.display.rotation)
        }.build()
        videoCapture = VideoCapture(videoCaptureConfig)

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

        // Bind use cases to lifecycle
        CameraX.bindToLifecycle(activity, videoPreview, videoCapture, imageCapture)
    }

    @SuppressLint("RestrictedApi")
    override fun startRecordingVideo() {
        activity?.let {
            currentFile = FileUtils.getLoopFrameFile(it, true, "orig_")
        }
        currentFile?.createNewFile()

        videoCapture.startRecording(currentFile, object : VideoCapture.OnVideoSavedListener {
            override fun onVideoSaved(file: File?) {
                Log.i(tag, "Video File : $file")
            }
            override fun onError(useCaseError: VideoCapture.UseCaseError?, message: String?, cause: Throwable?) {
                Log.i(tag, "Video Error: $message")
            }
        })
    }

    @SuppressLint("RestrictedApi")
    override fun stopRecordingVideo() {
        videoCapture.stopRecording()
    }

    override fun takePicture(onImageCapturedListener: ImageCaptureListener) {
        // Create output file to hold the image
        currentFile = FileUtils.getLoopFrameFile(context!!,false, "orig_")
        currentFile?.createNewFile()

        // Setup image capture metadata
        val metadata = Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
        }

        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(currentFile, object : ImageCapture.OnImageSavedListener {
            override fun onImageSaved(file: File) {
                onImageCapturedListener.onImageSaved(file)
            }

            override fun onError(useCaseError: UseCaseError, message: String, cause: Throwable?) {
                onImageCapturedListener.onError(message, cause)
            }
        }, metadata)
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
                    updateFlashSupported(cameraId)
                }

                // Unbind all use cases and bind them again with the new lens facing configuration
                CameraX.unbindAll()
                startCamera()
            } catch (exc: Exception) {
                // Do nothing
                // TODO error handling here
            }
        }
        return portkeyCameraSelectionFromCameraXLensFacing(lensFacing)
    }

    override fun selectCamera(camera: CameraSelection) {
        lensFacing = cameraXLensFacingFromPortkeyCameraSelection(camera)
    }

    override fun currentCamera(): CameraSelection {
        return portkeyCameraSelectionFromCameraXLensFacing(lensFacing)
    }

    override fun advanceFlashState() {
        super.advanceFlashState()
        imageCapture.flashMode = cameraXflashModeFromPortkeyFlashState(currentFlashState.currentFlashState())
    }

    override fun setFlashState(flashIndicatorState: FlashIndicatorState) {
        super.setFlashState(flashIndicatorState)
        if (active) {
            imageCapture.flashMode = cameraXflashModeFromPortkeyFlashState(currentFlashState.currentFlashState())
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
        private val TAG = "CameraXBasicHandling"

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
