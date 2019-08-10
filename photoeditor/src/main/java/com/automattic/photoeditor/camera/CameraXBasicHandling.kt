package com.automattic.photoeditor.camera

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.camera.core.VideoCapture
import androidx.camera.core.VideoCaptureConfig
import androidx.core.app.ActivityCompat
import com.automattic.photoeditor.R
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFragment
import com.automattic.photoeditor.util.FileUtils
import com.automattic.photoeditor.util.PermissionUtils
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import java.io.File

class CameraXBasicHandling : VideoRecorderFragment(),
        ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var videoCapture: VideoCapture
    private lateinit var videoPreview: Preview

    private var active: Boolean = false

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
        CameraX.unbindAll()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (!PermissionUtils.allRequiredPermissionsGranted(activity!!)) {
            ErrorDialog.newInstance(getString(R.string.request_permissions))
                .show(childFragmentManager,
                    FRAGMENT_DIALOG
                )
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // TODO remove this RestrictedApi annotation once androidx.camera:camera moves out of alpha
    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        // Create configuration object for the preview use case
        val previewConfig = PreviewConfig.Builder().build()
        videoPreview = Preview(previewConfig)

        // Create a configuration object for the video capture use case
        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
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
        CameraX.bindToLifecycle(activity, videoPreview, videoCapture)
    }

    @SuppressLint("RestrictedApi")
    override fun startRecordingVideo() {
        currentFile = FileUtils.getLoopFrameFile(true, "orig_")
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

    companion object {
        private val instance = CameraXBasicHandling()

        private val FRAGMENT_DIALOG = "dialog"
        /**
         * Tag for the [Log].
         */
        private val TAG = "CameraXBasicHandling"

        @JvmStatic fun getInstance(textureView: AutoFitTextureView): CameraXBasicHandling {
            instance.textureView = textureView
            return instance
        }
    }
}
