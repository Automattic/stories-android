package com.automattic.photoeditor.camera

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.automattic.photoeditor.R
import com.automattic.photoeditor.util.PermissionUtils
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import java.io.File

class CameraXBasicHandling : Fragment(), View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback, SurfaceFragmentHandler {
    /**
     * An [AutoFitTextureView] for camera preview.
     */
    lateinit var textureView: AutoFitTextureView

    private var active: Boolean = false

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            if (active) {
                // openCamera(width, height)
            }
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
//            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    }

    var currentFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

//    override fun onResume() {
//        super.onResume()
//        startUp()
//    }
//
//    override fun onPause() {
//        windDown()
//        super.onPause()
//    }
//
    override fun activate() {
        active = true
        startUp()
    }

    override fun deactivate() {
        active = false
        windDown()
    }

    private fun startUp() {
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable && active) {
//            startBackgroundThread()
//            openCamera(textureView.width, textureView.height)
        }
    }

    private fun windDown() {
//        closeCamera()
//        stopBackgroundThread()
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

    override fun onClick(view: View) {
//        when (view.id) {
//            R.id.picture -> lockFocus()
//            R.id.info -> {
//                if (activity != null) {
//                    AlertDialog.Builder(activity)
//                            .setMessage(R.string.intro_message)
//                            .setPositiveButton(android.R.string.ok, null)
//                            .show()
//                }
//            }
//        }
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
