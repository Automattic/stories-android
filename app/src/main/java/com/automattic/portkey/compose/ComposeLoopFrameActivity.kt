package com.automattic.portkey.compose

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.automattic.photoeditor.OnPhotoEditorListener
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.SaveSettings
import com.automattic.photoeditor.camera.interfaces.ImageCaptureListener
import com.automattic.photoeditor.state.BackgroundSurfaceManager
import com.automattic.photoeditor.util.FileUtils.Companion.getLoopFrameFile
import com.automattic.photoeditor.util.PermissionUtils
import com.automattic.photoeditor.views.ViewType
import com.automattic.portkey.BuildConfig
import com.automattic.portkey.R
import com.automattic.portkey.R.color
import com.automattic.portkey.R.id
import com.automattic.portkey.R.layout
import com.automattic.portkey.R.string
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_composer.*

import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_composer.*
import java.io.File
import java.io.IOException

class ComposeLoopFrameActivity : AppCompatActivity() {
    private lateinit var photoEditor: PhotoEditor
    private lateinit var backgroundSurfaceManager: BackgroundSurfaceManager
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_composer)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
                        txtCurrentTool.setText(string.label_tool_text)
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
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI(window)
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        // This example uses decor view, but you can use any visible view.
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // test actions just to test photoEditor module integration
        return when (item.itemId) {
            id.action_brush -> {
                testBrush()
                true
            }
            id.action_eraser -> {
                testEraser()
                true
            }
            id.action_text -> {
                testText()
                true
            }
            id.action_emoji -> {
                testEmoji()
                true
            }
            id.action_sticker -> {
                testSticker()
                true
            }

            id.action_bkg_camera_preview -> {
                testCameraPreview()
                true
            }
            id.action_bkg_static -> {
                testStaticBackground()
                true
            }
            id.action_bkg_play_video -> {
                testPlayVideo()
                true
            }
            id.action_bkg_take_picture -> {
                testTakeStillPicture()
                true
            }

            id.action_save -> {
                saveLoopFrame()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun testBrush() {
        txtCurrentTool.setText(string.label_tool_brush)
        photoEditor.setBrushDrawingMode(true)
        photoEditor.brushColor = ContextCompat.getColor(baseContext, color.red)
    }

    private fun testEraser() {
        txtCurrentTool.setText(string.label_tool_eraser)
        photoEditor.setBrushDrawingMode(false)
        photoEditor.brushEraser()
    }

    private fun testText() {
        txtCurrentTool.setText("")
        photoEditor.addText(
            text = getString(string.text_placeholder),
            colorCodeTextView = ContextCompat.getColor(baseContext, color.white))
    }

    private fun testEmoji() {
        txtCurrentTool.setText("")
        val emojisList = PhotoEditor.getEmojis(this)
        // get some random emoji
        val randomEmojiPos = (0..emojisList.size).shuffled().first()
        photoEditor.addEmoji(emojisList.get(randomEmojiPos))
    }

    private fun testSticker() {
        txtCurrentTool.setText("")
        photoEditor.addNewImageView(true, Uri.parse("https://i.giphy.com/Ok4HaWlYrewuY.gif"))
    }

    private fun testCameraPreview() {
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

        txtCurrentTool.setText(string.main_test_camera_preview)
        backgroundSurfaceManager.switchCameraPreviewOn()
    }

    private fun testPlayVideo() {
        txtCurrentTool.setText(string.main_test_play_video)
        backgroundSurfaceManager.switchVideoPlayerOn()
    }

    private fun testStaticBackground() {
        txtCurrentTool.setText(string.main_test_static_background)
        backgroundSurfaceManager.switchStaticImageBackgroundModeOn()
    }

    private fun testTakeStillPicture() {
        txtCurrentTool.setText(string.main_test_take_picture)
        backgroundSurfaceManager.takePicture(object : ImageCaptureListener {
            override fun onImageSaved(file: File) {
                runOnUiThread({
                    Glide.with(this@ComposeLoopFrameActivity)
                        .load(file)
                        .into(photoEditorView.source)

                    backgroundSurfaceManager.switchStaticImageBackgroundModeOn()
                })
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
