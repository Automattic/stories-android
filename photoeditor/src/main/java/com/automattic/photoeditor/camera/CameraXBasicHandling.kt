package com.automattic.photoeditor.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.Result.RESULT_INVALID_SURFACE
import androidx.camera.core.SurfaceRequest.Result.RESULT_REQUEST_CANCELLED
import androidx.camera.core.SurfaceRequest.Result.RESULT_SURFACE_ALREADY_PROVIDED
import androidx.camera.core.SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY
import androidx.camera.core.SurfaceRequest.Result.RESULT_WILL_NOT_PROVIDE_SURFACE
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
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
import com.google.common.util.concurrent.ListenableFuture

class CameraXBasicHandling : VideoRecorderFragment() {
    private var videoCapture: VideoCapture? = null
    private lateinit var videoPreview: Preview
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider
    private var cameraProviderInitialized = false
    private lateinit var currentCamera: Camera
    private var surfaceRequest: SurfaceRequest? = null

    val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            if (active) {
                surfaceRequest?.let {
                    texture.setDefaultBufferSize(
                            it.resolution.getWidth(), it.resolution.getHeight())
                    it.provideSurface(
                            Surface(texture),
                            ContextCompat.getMainExecutor(context),
                            Consumer {
                                when (it.resultCode) {
                                    RESULT_SURFACE_USED_SUCCESSFULLY ->
                                        Log.d("DEBUG", "RESULT_SURFACE_USED_SUCCESSFULLY")
                                    RESULT_REQUEST_CANCELLED -> Log.d("DEBUG", "RESULT_REQUEST_CANCELLED")
                                    RESULT_INVALID_SURFACE -> Log.d("DEBUG", "RESULT_INVALID_SURFACE")
                                    RESULT_SURFACE_ALREADY_PROVIDED ->
                                        Log.d("DEBUG", "RESULT_SURFACE_ALREADY_PROVIDED")
                                    RESULT_WILL_NOT_PROVIDE_SURFACE ->
                                        Log.d("DEBUG", "RESULT_WILL_NOT_PROVIDE_SURFACE")
                                }
                            }
                    )
                }
            } else {
                surfaceRequest?.willNotProvideSurface()
            }
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            cameraProviderInitialized = true
            activate()
        }, ContextCompat.getMainExecutor(context))
        retainInstance = true
    }

    override fun activate() {
        // either activates the Camera or hold a short wait to retry until cameraProvider is set on next run
        if (cameraProviderInitialized) {
            if (!active) {
                cameraProvider.unbindAll()
                active = true
                startUp()
            }
        } else {
            Handler().postDelayed({ activate() }, 300)
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
        cameraProvider.unbindAll()
    }

    // TODO remove this RestrictedApi annotation once androidx.camera:camera moves out of alpha
    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError")
    private fun startCamera() {
        videoPreview = Preview.Builder()
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
                // .setTargetAspectRatioCustom(screenAspectRatio)
                // .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(textureView.display.rotation)
                .build()

        // Set up the capture use case to allow users to take photos
        imageCapture = ImageCapture.Builder()
                .setFlashMode(cameraXflashModeFromStoriesFlashState(currentFlashState.currentFlashState()))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config but letting
                // CameraX optimize for whatever specific resolution best fits requested capture mode
                // setTargetAspectRatio(RATIO_4_3)
                // .setTargetAspectRatioCustom(screenAspectRatio)
                // .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(textureView.display.rotation)
                .build()

        videoPreview.setSurfaceProvider(object : SurfaceProvider {
            override fun onSurfaceRequested(request: SurfaceRequest) {
                surfaceRequest = request
                resetTextureView(request.resolution)
            }
        })

        // we used to bind all use cases to lifecycle on start
        // DON'T do this, may end up with this: https://github.com/Automattic/stories-android/issues/50
        // CameraX.bindToLifecycle(activity, videoPreview, videoCapture, imageCapture)

        // image capture only
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        currentCamera = cameraProvider.bindToLifecycle(
                activity as LifecycleOwner, cameraSelector, videoPreview, imageCapture
        )
        // retrieve flash availability for this camera
        flashSupported = currentCamera.cameraInfo.hasFlashUnit()
    }

    private fun resetTextureView(resolution: Size) {
        if (!active) {
            return
        }
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
        // Important: we need to set the aspect ratio on the TextureView in order for it to be reused
        // passing the surfaceRequest's requested resolution as calculated by CameraX
        textureView.setAspectRatio(resolution.height, resolution.width)
        parent.addView(textureView, 0)
    }

    @SuppressLint("RestrictedApi")
    override fun startRecordingVideo(finishedListener: VideoRecorderFinished?) {
        // FIXME tackled video recording later
//        activity?.let {
//            if (useTempCaptureFile) {
//                currentFile = FileUtils.getTempCaptureFile(it, true)
//            } else {
//                currentFile = FileUtils.getLoopFrameFile(it, true)
//            }
//        }
//
//        currentFile?.let {
//            it.createNewFile()
//
//            // unbind this use case for now, we'll re-bind later
//            imageCapture?.let {
//                if (cameraProvider.isBound(it)) {
//                    cameraProvider.unbind(it)
//                }
//            }
//
//            // if a previous instance exists, request to release muxer and buffers
//            videoCapture?.let {
//                if (cameraProvider.isBound(it)) {
//                    cameraProvider.unbind(it)
//                }
//                videoCapture?.clear()
//            }
//
//            val metrics = DisplayMetrics().also { textureView.display.getRealMetrics(it) }
//
//            videoCapture = VideoCaptureConfig.Builder().apply {
//                setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
//                setTargetRotation(textureView.display.rotation)
//            }.build()
// //            videoCapture = VideoCapture(videoCaptureConfig)
//
//            // video capture only
//            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
//            cameraProvider.bindToLifecycle(activity as LifecycleOwner, cameraSelector, videoCapture)
//
//            videoCapture?.startRecording(
//                it,
//                AsyncTask.THREAD_POOL_EXECUTOR,
//                object : VideoCapture.OnVideoSavedCallback {
//                    override fun onVideoSaved(file: File) {
//                        Log.i(tag, "Video File : $file")
//                        finishedListener?.onVideoSaved(file)
//                    }
//
//                    override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
//                        Log.i(tag, "Video Error: $message")
//                        finishedListener?.onError(message, cause)
//                    }
//            })
//        }
    }

    @SuppressLint("RestrictedApi")
    override fun stopRecordingVideo() {
        videoCapture?.stopRecording()
    }

    override fun takePicture(onImageCapturedListener: ImageCaptureListener) {
        // Create output file to hold the image
        context?.let { context ->
            if (useTempCaptureFile) {
                currentFile = FileUtils.getTempCaptureFile(context, false).apply { createNewFile() }
            } else {
                currentFile = FileUtils.getLoopFrameFile(context, false).apply { createNewFile() }
            }

            currentFile?.let { captureFile ->
                // Setup image capture metadata
                val metadata = Metadata().apply {
                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                val outputFileOptions = OutputFileOptions.Builder(captureFile)
                        .setMetadata(metadata)
                        .build()

                // image capture only
                imageCapture?.let {
                    if (!cameraProvider.isBound(it)) {
                        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                        cameraProvider.bindToLifecycle(activity as LifecycleOwner, cameraSelector, it)
                    }
                }

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture?.takePicture(
                        outputFileOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: OutputFileResults) {
                                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(captureFile)
                                onImageCapturedListener.onImageSaved(savedUri)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                onImageCapturedListener.onError(exception.message ?: "", exception)
                            }
                        }
                )
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun flipCamera(): CameraSelection {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        if (active) {
            try {
                // Unbind all use cases and bind them again with the new lens facing configuration
                cameraProvider.unbindAll()
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
            flashSupportChangeListener: FlashSupportChangeListener,
            useTempCaptureFile: Boolean
        ): CameraXBasicHandling {
            instance.textureView = textureView
            instance.flashSupportChangeListener = flashSupportChangeListener
            instance.useTempCaptureFile = useTempCaptureFile
            return instance
        }
    }
}
