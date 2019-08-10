package com.automattic.photoeditor.camera

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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

    private var active: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun activate() {
        active = true
        startUp()
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

    private fun windDown() {
        if (CameraX.isBound(videoCapture)) {
            CameraX.unbind(videoCapture)
        }
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
        val preview = Preview(previewConfig)

        // Create a configuration object for the video capture use case
        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setTargetRotation(textureView.display.rotation)
        }.build()
        videoCapture = VideoCapture(videoCaptureConfig)

        preview.setOnPreviewOutputUpdateListener {
            textureView.surfaceTexture = it.surfaceTexture
        }

        // Bind use cases to lifecycle
        CameraX.bindToLifecycle(activity, preview, videoCapture)
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
