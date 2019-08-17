package com.automattic.portkey.compose

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import com.automattic.photoeditor.OnPhotoEditorListener
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.SaveSettings
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState.AUTO
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState.OFF
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState.ON
import com.automattic.photoeditor.camera.interfaces.ImageCaptureListener
import com.automattic.photoeditor.state.BackgroundSurfaceManager
import com.automattic.photoeditor.util.FileUtils.Companion.getLoopFrameFile
import com.automattic.photoeditor.util.PermissionUtils
import com.automattic.photoeditor.views.ViewType
import com.automattic.portkey.BuildConfig
import com.automattic.portkey.R
import com.automattic.portkey.R.color
import com.automattic.portkey.R.layout
import com.automattic.portkey.R.string
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.content_composer.*
import java.io.File
import java.io.IOException

fun Group.setAllOnClickListener(listener: View.OnClickListener?) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id).setOnClickListener(listener)
    }
}

class ComposeLoopFrameActivity : AppCompatActivity() {
    private lateinit var photoEditor: PhotoEditor
    private lateinit var backgroundSurfaceManager: BackgroundSurfaceManager
    private var progressDialog: ProgressDialog? = null
    private val CAMERA_PREVIEW_LAUNCH_DELAY = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_composer)

        photoEditor = PhotoEditor.Builder(this, photoEditorView)
            .setPinchTextScalable(true) // set flag to make text scalable when pinch
            .build() // build photo editor sdk

        photoEditor.setOnPhotoEditorListener(object : OnPhotoEditorListener {
            override fun onEditTextChangeListener(rootView: View, text: String, colorCode: Int) {
                val textEditorDialogFragment = TextEditorDialogFragment.show(
                    this@ComposeLoopFrameActivity,
                    text,
                    colorCode)
                textEditorDialogFragment.setOnTextEditorListener(object : TextEditorDialogFragment.TextEditor {
                    override fun onDone(inputText: String, colorCode: Int) {
                        photoEditor.editText(rootView, inputText, colorCode)
                    }
                })
            }

            override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                // no op
            }

            override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                // no op
            }

            override fun onStartViewChangeListener(viewType: ViewType) {
                // no op
            }

            override fun onStopViewChangeListener(viewType: ViewType) {
                // no op
            }

            override fun onRemoveViewListener(numberOfAddedViews: Int) {
                // no op
            }
        })

        backgroundSurfaceManager = BackgroundSurfaceManager(
            savedInstanceState,
            lifecycle,
            photoEditorView,
            supportFragmentManager,
            BuildConfig.USE_CAMERAX)

        lifecycle.addObserver(backgroundSurfaceManager)

        // add click listeners
        addClickListeners()

        photoEditorView.postDelayed({
            launchCameraPreview()
        }, CAMERA_PREVIEW_LAUNCH_DELAY)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI(window)
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        photoEditorView.postDelayed({
                hideSystemUI(window)
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        backgroundSurfaceManager.saveStateToBundle(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (PermissionUtils.allRequiredPermissionsGranted(this)) {
            backgroundSurfaceManager.switchCameraPreviewOn()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun addClickListeners() {
        camera_capture_button.setOnClickListener {
            // capture still image
            takeStillPicture()
        }

        gallery_upload_img.setOnClickListener {
            // TODO implement tapping on thumbnail
            Toast.makeText(this, "not implemented yet", Toast.LENGTH_SHORT).show()
        }

        camera_flip_group.setAllOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                backgroundSurfaceManager.flipCamera()
            }
        })

        // attach listener a bit delayed as we need to have cameraBasicHandling created first
        photoEditorView.postDelayed({
            if (backgroundSurfaceManager.isFlashAvailable()) {
                camera_flash_group.setAllOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        val flashState = backgroundSurfaceManager.switchFlashState()
                        when (flashState) {
                            AUTO -> camera_flash_button.background = getDrawable(R.drawable.ic_flash_auto_black_24dp)
                            ON -> camera_flash_button.background = getDrawable(R.drawable.ic_flash_on_black_24dp)
                            OFF -> camera_flash_button.background = getDrawable(R.drawable.ic_flash_off_black_24dp)
                        }
                    }
                })
            } else {
                camera_flash_group.visibility = View.GONE
            }
        }, CAMERA_PREVIEW_LAUNCH_DELAY)
    }

    private fun testBrush() {
        photoEditor.setBrushDrawingMode(true)
        photoEditor.brushColor = ContextCompat.getColor(baseContext, color.red)
    }

    private fun testEraser() {
        photoEditor.setBrushDrawingMode(false)
        photoEditor.brushEraser()
    }

    private fun testText() {
        photoEditor.addText(
            text = getString(string.text_placeholder),
            colorCodeTextView = ContextCompat.getColor(baseContext, color.white))
    }

    private fun testEmoji() {
        val emojisList = PhotoEditor.getEmojis(this)
        // get some random emoji
        val randomEmojiPos = (0..emojisList.size).shuffled().first()
        photoEditor.addEmoji(emojisList.get(randomEmojiPos))
    }

    private fun testSticker() {
        photoEditor.addNewImageView(true, Uri.parse("https://i.giphy.com/Ok4HaWlYrewuY.gif"))
    }

    private fun launchCameraPreview() {
        if (!PermissionUtils.checkPermission(this, Manifest.permission.RECORD_AUDIO) ||
            !PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
            !PermissionUtils.checkPermission(this, Manifest.permission.CAMERA)) {
            val permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            PermissionUtils.requestPermissions(this, permissions)
            return
        }

        backgroundSurfaceManager.switchCameraPreviewOn()
    }

    private fun testPlayVideo() {
        backgroundSurfaceManager.switchVideoPlayerOn()
    }

    private fun testStaticBackground() {
        backgroundSurfaceManager.switchStaticImageBackgroundModeOn()
    }

    private fun takeStillPicture() {
        backgroundSurfaceManager.takePicture(object : ImageCaptureListener {
            override fun onImageSaved(file: File) {
                runOnUiThread {
                    Glide.with(this@ComposeLoopFrameActivity)
                        .load(file)
                        .transform(CenterCrop(), RoundedCorners(16))
                        .into(gallery_upload_img)
                }
            }
            override fun onError(message: String, cause: Throwable?) {
                // TODO implement error handling
            }
        })
    }

    // this one saves one composed unit: ether an Image or a Video
    private fun saveLoopFrame() {
        // check wether we have an Image or a Video, and call its save functionality accordingly
        if (backgroundSurfaceManager.cameraVisible() || backgroundSurfaceManager.videoPlayerVisible()) {
            saveVideo(backgroundSurfaceManager.getCurrentFile().toString())
        } else {
            // check whether there are any GIF stickers - if there are, we need to produce a video instead
            if (photoEditor.anyStickersAdded()) {
                saveVideoWithStaticBackground()
            } else {
                saveImage()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveImage() {
        if (PermissionUtils.checkAndRequestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showLoading("Saving...")
            val file = getLoopFrameFile(false)
            try {
                file.createNewFile()

                val saveSettings = SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(true)
                    .build()

                photoEditor.saveAsFile(file.absolutePath, saveSettings, object : PhotoEditor.OnSaveListener {
                    override fun onSuccess(imagePath: String) {
                        hideLoading()
                        showSnackbar("Image Saved Successfully")
                        photoEditorView.source.setImageURI(Uri.fromFile(File(imagePath)))
                    }

                    override fun onFailure(exception: Exception) {
                        hideLoading()
                        showSnackbar("Failed to save Image")
                    }
                })
            } catch (e: IOException) {
                e.printStackTrace()
                hideLoading()
                e.message?.takeIf { it.isNotEmpty() }?.let { showSnackbar(it) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveVideo(inputFile: String) {
        if (PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showLoading("Saving...")
            try {
                val file = getLoopFrameFile(true)
                file.createNewFile()

                val saveSettings = SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(true)
                    .build()

                photoEditor.saveVideoAsFile(
                    inputFile,
                    file.absolutePath,
                    saveSettings,
                    object : PhotoEditor.OnSaveWithCancelListener {
                    override fun onCancel(noAddedViews: Boolean) {
                            hideLoading()
                            showSnackbar("No views added - original video saved")
                        }

                        override fun onSuccess(imagePath: String) {
                            hideLoading()
                            showSnackbar("Video Saved Successfully")
                        }

                        override fun onFailure(exception: Exception) {
                            hideLoading()
                            showSnackbar("Failed to save Video")
                        }
                    })
            } catch (e: IOException) {
                e.printStackTrace()
                hideLoading()
                e.message?.takeIf { it.isNotEmpty() }?.let { showSnackbar(it) }
            }
        } else {
            showSnackbar("Please allow WRITE TO STORAGE permissions")
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveVideoWithStaticBackground() {
        if (PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showLoading("Saving...")
            try {
                val file = getLoopFrameFile(true, "tmp")
                file.createNewFile()

                val saveSettings = SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(true)
                    .build()

                photoEditor.saveVideoFromStaticBackgroundAsFile(
                    file.absolutePath,
                    saveSettings,
                    object : PhotoEditor.OnSaveWithCancelListener {
                        override fun onCancel(noAddedViews: Boolean) {
                            // TODO not implemented
                        }

                        override fun onSuccess(imagePath: String) {
                            // now save the video with emoji, but using the previously saved video as input
                            hideLoading()
                            saveVideo(imagePath)
                            // TODO: delete the temporal video produced originally
                        }

                        override fun onFailure(exception: Exception) {
                            hideLoading()
                            showSnackbar("Failed to save Video")
                        }
                    })
            } catch (e: IOException) {
                e.printStackTrace()
                hideLoading()
                e.message?.takeIf { it.isNotEmpty() }?.let { showSnackbar(it) }
            }
        } else {
            showSnackbar("Please allow WRITE TO STORAGE permissions")
        }
    }

    protected fun showLoading(message: String) {
        runOnUiThread {
            progressDialog = ProgressDialog(this)
            progressDialog!!.setMessage(message)
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progressDialog!!.setCancelable(false)
            progressDialog!!.show()
        }
    }

    protected fun hideLoading() {
        runOnUiThread {
            if (progressDialog != null) {
                progressDialog!!.dismiss()
            }
        }
    }

    protected fun showSnackbar(message: String) {
        runOnUiThread {
            val view = findViewById<View>(android.R.id.content)
            if (view != null) {
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
