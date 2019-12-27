package com.automattic.portkey.compose

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProviders
import com.automattic.photoeditor.OnPhotoEditorListener
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.SaveSettings
import com.automattic.photoeditor.camera.interfaces.CameraSelection
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState
import com.automattic.photoeditor.camera.interfaces.ImageCaptureListener
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFinished
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFragment.FlashSupportChangeListener
import com.automattic.photoeditor.state.BackgroundSurfaceManager
import com.automattic.photoeditor.util.FileUtils.Companion.getLoopFrameFile
import com.automattic.photoeditor.util.PermissionUtils
import com.automattic.photoeditor.views.ViewType
import com.automattic.photoeditor.views.ViewType.TEXT
import com.automattic.photoeditor.views.added.AddedViewList
import com.automattic.portkey.BuildConfig
import com.automattic.portkey.R
import com.automattic.portkey.compose.emoji.EmojiPickerFragment
import com.automattic.portkey.compose.emoji.EmojiPickerFragment.EmojiListener
import com.automattic.portkey.compose.photopicker.MediaBrowserType
import com.automattic.portkey.compose.photopicker.PhotoPickerActivity
import com.automattic.portkey.compose.photopicker.PhotoPickerFragment
import com.automattic.portkey.compose.photopicker.RequestCodes
import com.automattic.portkey.compose.story.OnStoryFrameSelectorTappedListener
import com.automattic.portkey.compose.story.StoryFrameItem
import com.automattic.portkey.compose.story.StoryFrameItem.BackgroundSource
import com.automattic.portkey.compose.story.StoryFrameItemType
import com.automattic.portkey.compose.story.StoryFrameItemType.IMAGE
import com.automattic.portkey.compose.story.StoryFrameItemType.VIDEO
import com.automattic.portkey.compose.story.StoryFrameSelectorFragment
import com.automattic.portkey.compose.story.StoryRepository
import com.automattic.portkey.compose.story.StoryViewModel
import com.automattic.portkey.compose.story.StoryViewModelFactory
import com.automattic.portkey.compose.text.TextEditorDialogFragment
import com.automattic.portkey.util.CrashLoggingUtils
import com.automattic.portkey.util.getDisplayPixelSize
import com.automattic.portkey.util.isVideo
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_composer.*
import kotlinx.android.synthetic.main.content_composer.*
import java.io.File
import java.io.IOException
import kotlin.math.abs

fun Group.setAllOnClickListener(listener: OnClickListener?) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id).setOnClickListener(listener)
    }
}

fun Snackbar.config(context: Context) {
    this.view.background = context.getDrawable(R.drawable.snackbar_background)
    val params = this.view.layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(12, 12, 12, 12)
    this.view.layoutParams = params
    ViewCompat.setElevation(this.view, 6f)
}

class ComposeLoopFrameActivity : AppCompatActivity(), OnStoryFrameSelectorTappedListener {
    private lateinit var photoEditor: PhotoEditor
    private lateinit var backgroundSurfaceManager: BackgroundSurfaceManager
    private var currentOriginalCapturedFile: File? = null

    private val timesUpRunnable = Runnable {
        stopRecordingVideo(false) // time's up, it's not a cancellation
    }
    private val timesUpHandler = Handler()
    private var cameraOperationInCourse = false

    private var cameraSelection = CameraSelection.BACK
    private var flashModeSelection = FlashIndicatorState.OFF
    private var videoPlayerMuted = false

    private lateinit var emojiPickerFragment: EmojiPickerFragment
    private lateinit var swipeDetector: GestureDetectorCompat
    private var screenSizeX: Int = 0
    private var screenSizeY: Int = 0
    private var topControlsBaseTopMargin: Int = 0
    private var isEditingText: Boolean = false

    private lateinit var storyViewModel: StoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_composer)

        topControlsBaseTopMargin = getLayoutTopMarginBeforeInset(edit_mode_controls.layoutParams)
        ViewCompat.setOnApplyWindowInsetsListener(compose_loop_frame_layout) { view, insets ->
            // set insetTop as margin to all controls appearing at the top of the screen
            addInsetTopMargin(edit_mode_controls.layoutParams, topControlsBaseTopMargin, insets.systemWindowInsetTop)
            addInsetTopMargin(close_button.layoutParams, topControlsBaseTopMargin, insets.systemWindowInsetTop)
            addInsetTopMargin(control_flash_group.layoutParams, topControlsBaseTopMargin, insets.systemWindowInsetTop)
            insets
        }

        photoEditor = PhotoEditor.Builder(this, photoEditorView)
            .setPinchTextScalable(true) // set flag to make text scalable when pinch
            .setDeleteView(delete_view)
            .build() // build photo editor sdk

        photoEditor.setOnPhotoEditorListener(object : OnPhotoEditorListener {
            override fun onEditTextChangeListener(rootView: View, text: String, colorCode: Int, isJustAdded: Boolean) {
                if (isEditingText) {
                    return
                }

                isEditingText = true
                editModeHideAllUIControls(false)
                if (isJustAdded) {
                    // hide new text views
                    rootView.visibility = View.GONE
                }
                val textEditorDialogFragment = TextEditorDialogFragment.show(
                    this@ComposeLoopFrameActivity,
                    text,
                    colorCode)
                textEditorDialogFragment.setOnTextEditorListener(object : TextEditorDialogFragment.TextEditor {
                    override fun onDone(inputText: String, colorCode: Int) {
                        isEditingText = false
                        // make sure to set it to visible, as newly added views are originally hidden until
                        // proper text is set
                        rootView.visibility = View.VISIBLE
                        if (TextUtils.isEmpty(inputText)) {
                            // just remove the view here, we don't need it - also don't  add to the `redo` stack
                            photoEditor.viewUndo(rootView, TEXT, false)
                        } else {
                            photoEditor.editText(rootView, inputText, colorCode)
                        }
                        editModeRestoreAllUIControls()
                    }
                })
            }

            override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                // only show save button if any views have been added
                save_button.visibility = View.VISIBLE
            }

            override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                showSaveButtonIfViewsAdded()
            }

            override fun onStartViewChangeListener(viewType: ViewType) {
                // in this case, also hide the SAVE button
                editModeHideAllUIControls(true)
            }

            override fun onStopViewChangeListener(viewType: ViewType) {
                if (!(viewType == TEXT && isEditingText)) {
                    editModeRestoreAllUIControls()
                }
            }

            @Suppress("OverridingDeprecatedMember")
            override fun onRemoveViewListener(numberOfAddedViews: Int) {
                // no op
            }

            override fun onRemoveViewReadyListener(removedView: View, ready: Boolean) {
                delete_view.setReadyForDelete(ready)
            }
        })

        backgroundSurfaceManager = BackgroundSurfaceManager(
            savedInstanceState,
            lifecycle,
            photoEditorView,
            supportFragmentManager,
            object : FlashSupportChangeListener {
                override fun onFlashSupportChanged(isSupported: Boolean) {
                    if (isSupported) {
                        camera_flash_button.visibility = View.VISIBLE
                        label_flash.visibility = View.VISIBLE
                    } else {
                        camera_flash_button.visibility = View.INVISIBLE
                        label_flash.visibility = View.INVISIBLE
                    }
                }
            },
            BuildConfig.USE_CAMERAX)

        lifecycle.addObserver(backgroundSurfaceManager)

        emojiPickerFragment = EmojiPickerFragment()
        emojiPickerFragment.setEmojiListener(object : EmojiListener {
            override fun onEmojiClick(emojiUnicode: String) {
                photoEditor.addEmoji(emojiUnicode)
            }
        })

        // calculate screen size, used to detect swipe from bottom
        calculateScreenSize()

        // add click listeners
        addClickListeners()

        swipeDetector = GestureDetectorCompat(this, FlingGestureListener())

        // TODO storyIndex here is hardcoded to 0, will need to change once we have multiple stories stored.
        storyViewModel = ViewModelProviders.of(this,
            StoryViewModelFactory(StoryRepository, 0)
        )[StoryViewModel::class.java]

        if (savedInstanceState == null) {
            // small tweak to make sure to not show the background image for the static image background mode
            backgroundSurfaceManager.preTurnTextureViewOn()

            // check camera selection, flash state from preferences
            CameraSelection.valueOf(
                getPreferences(Context.MODE_PRIVATE).getInt(getString(R.string.pref_camera_selection), 0))?.let {
                cameraSelection = it
            }
            FlashIndicatorState.valueOf(
                getPreferences(Context.MODE_PRIVATE).getInt(getString(R.string.pref_flash_mode_selection), 0))?.let {
                flashModeSelection = it
            }

            // also, update the UI
            updateFlashModeSelectionIcon()

            photoEditorView.postDelayed({
                launchCameraPreview()
            }, SURFACE_MANAGER_READY_LAUNCH_DELAY)
        } else {
            currentOriginalCapturedFile =
                savedInstanceState.getSerializable(STATE_KEY_CURRENT_ORIGINAL_CAPTURED_FILE) as File?

            photoEditorView.postDelayed({
                when {
                    backgroundSurfaceManager.videoPlayerVisible() -> showPlayVideo(currentOriginalCapturedFile)
                    backgroundSurfaceManager.cameraVisible() -> launchCameraPreview()
                    else -> {
                        Glide.with(this@ComposeLoopFrameActivity)
                            .load(currentOriginalCapturedFile)
                            .transform(CenterCrop())
                            .into(photoEditorView.source)
                        showStaticBackground()
                    }
                }
            }, SURFACE_MANAGER_READY_LAUNCH_DELAY)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar(window)
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        photoEditorView.postDelayed({
                hideStatusBar(window)
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        backgroundSurfaceManager.saveStateToBundle(outState)
        outState.putSerializable(STATE_KEY_CURRENT_ORIGINAL_CAPTURED_FILE, currentOriginalCapturedFile)
        super.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (PermissionUtils.allRequiredPermissionsGranted(this)) {
            switchCameraPreviewOn()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onBackPressed() {
        if (!backgroundSurfaceManager.cameraVisible()) {
            close_button.performClick()
        } else {
            super.onBackPressed()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        swipeDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCodes.PHOTO_PICKER -> if (resultCode == Activity.RESULT_OK && data != null) {
                val strMediaUri = data.getStringExtra(PhotoPickerActivity.EXTRA_MEDIA_URI)
                if (strMediaUri == null) {
                    Log.e("Composer", "Can't resolve picked media")
                    showToast("Can't resolve picked media")
                    return
                }

                // decide whether the picked media is a VIDEO or an IMAGE
                val isVideo = isVideo(strMediaUri)
                if (isVideo) {
                    // now start playing the video we just recorded
                    showPlayVideo(Uri.parse(strMediaUri))
                } else {
                    // assuming image for now
                    Glide.with(this@ComposeLoopFrameActivity)
                        .load(strMediaUri)
                        .transform(CenterCrop())
                        .into(photoEditorView.source)
                    showStaticBackground()
                }
                storyViewModel.apply {
                    addStoryFrameItemToCurrentStory(StoryFrameItem(
                        BackgroundSource(contentUri = Uri.parse(strMediaUri)),
                        frameItemType = if (isVideo) VIDEO else IMAGE
                    ))
                    setSelectedFrame(0)
                }
            }
        }
    }

    private fun addClickListeners() {
        camera_capture_button
            .setOnTouchListener(
                PressAndHoldGestureHelper(
                    PressAndHoldGestureHelper.CLICK_LENGTH,
                    object : PressAndHoldGestureListener {
                        override fun onClickGesture() {
                            if (cameraOperationInCourse) {
                                showToast("Operation in progress, try again")
                                return
                            }
                            timesUpHandler.removeCallbacksAndMessages(null)
                            takeStillPicture()
                        }
                        override fun onHoldingGestureStart() {
                            timesUpHandler.removeCallbacksAndMessages(null)
                            startRecordingVideoAfterVibrationIndication()
                        }

                        override fun onHoldingGestureEnd() {
                            stopRecordingVideo(false)
                        }

                        override fun onHoldingGestureCanceled() {
                            stopRecordingVideo(true)
                        }

                        override fun onStartDetectionWait() {
                            if (cameraOperationInCourse) {
                                showToast("Operation in progress, try again")
                                return
                            }
                            // when the wait to see whether this is a "press and hold" gesture starts,
                            // start the animation to grow the capture button radius
                            camera_capture_button
                                .animate()
                                .scaleXBy(0.3f) // scale up by 30%
                                .scaleYBy(0.3f)
                                .duration = PressAndHoldGestureHelper.CLICK_LENGTH
                        }

                        override fun onTouchEventDetectionEnd() {
                            if (cameraOperationInCourse) {
                                return
                            }
                            // when gesture detection ends, we're good to
                            // get the capture button shape as it originally was (idle state)
                            camera_capture_button.clearAnimation()
                            camera_capture_button
                                .animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .duration = PressAndHoldGestureHelper.CLICK_LENGTH / 4
                        }
                    })
            )

        container_gallery_upload.setOnClickListener {
            showMediaPicker()
        }

        camera_flip_group.setOnClickListener {
            cameraSelection = backgroundSurfaceManager.flipCamera()
            saveCameraSelectionPref()
        }

        // attach listener a bit delayed as we need to have cameraBasicHandling created first
        photoEditorView.postDelayed({
            camera_flash_group.setAllOnClickListener(OnClickListener {
                flashModeSelection = backgroundSurfaceManager.switchFlashState()
                updateFlashModeSelectionIcon()
                saveFlashModeSelectionPref()
            })
        }, SURFACE_MANAGER_READY_LAUNCH_DELAY)

        close_button.setOnClickListener {
            // add discard dialog
            if (photoEditor.anyViewsAdded()) {
                // show dialog
                DiscardDialog.newInstance(getString(R.string.dialog_discard_message), object : DiscardOk {
                    override fun discardOkClicked() {
                        photoEditor.clearAllViews()
                        storyViewModel.discardCurrentStory()
                        launchCameraPreview()
                        deleteCapturedMedia()
                    }
                }).show(supportFragmentManager, FRAGMENT_DIALOG)
            } else {
                storyViewModel.discardCurrentStory()
                launchCameraPreview()
                deleteCapturedMedia()
            }
        }

        sound_button_group.setOnClickListener {
            if (videoPlayerMuted) {
                backgroundSurfaceManager.videoPlayerUnmute()
                videoPlayerMuted = false
                sound_button.background = getDrawable(R.drawable.ic_volume_up_black_24dp)
                label_sound.text = getString(R.string.label_control_sound_on)
            } else {
                backgroundSurfaceManager.videoPlayerMute()
                videoPlayerMuted = true
                sound_button.background = getDrawable(R.drawable.ic_volume_mute_black_24dp)
                label_sound.text = getString(R.string.label_control_sound_off)
            }
        }

        text_add_button_group.setOnClickListener {
            addNewText()
        }

        stickers_button_group.setOnClickListener {
            emojiPickerFragment.show(supportFragmentManager, emojiPickerFragment.tag)
        }

        save_button.setOnClickListener {
            saveLoopFrame()
        }
    }

    private fun showMediaPicker() {
        val intent = Intent(this@ComposeLoopFrameActivity, PhotoPickerActivity::class.java)
        intent.putExtra(PhotoPickerFragment.ARG_BROWSER_TYPE, MediaBrowserType.PORTKEY_PICKER)

        startActivityForResult(
            intent,
            RequestCodes.PHOTO_PICKER,
            ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    private fun deleteCapturedMedia() {
        currentOriginalCapturedFile?.delete()

        // reset
        currentOriginalCapturedFile = null
    }

    private fun switchCameraPreviewOn() {
        backgroundSurfaceManager.switchCameraPreviewOn()
        hideStoryFrameSelector()
    }

    private fun testBrush() {
        photoEditor.setBrushDrawingMode(true)
        photoEditor.brushColor = ContextCompat.getColor(baseContext, R.color.red)
    }

    private fun testEraser() {
        photoEditor.setBrushDrawingMode(false)
        photoEditor.brushEraser()
    }

    private fun addNewText() {
        val dp = resources.getDimension(R.dimen.editor_initial_text_size) / resources.displayMetrics.density
        photoEditor.addText(
            "",
            colorCodeTextView = ContextCompat.getColor(baseContext, R.color.text_color_white),
            fontSizeSp = dp
        )
    }

    private fun testEmoji() {
        val emojisList = PhotoEditor.getEmojis(this)
        // get some random emoji
        val randomEmojiPos = (0..emojisList.size).shuffled().first()
        photoEditor.addEmoji(emojisList[randomEmojiPos])
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

        hideStoryFrameSelector()
        hideEditModeUIControls()

        // set the correct camera as selected by the user last time they used the app
        backgroundSurfaceManager.selectCamera(cameraSelection)
        // same goes for flash state
        backgroundSurfaceManager.setFlashState(flashModeSelection)
        switchCameraPreviewOn()
    }

    private fun showPlayVideo(videoFile: File? = null) {
        showStoryFrameSelector()
        showEditModeUIControls(false)
        backgroundSurfaceManager.switchVideoPlayerOnFromFile(videoFile)
    }

    private fun showPlayVideo(videoUri: Uri) {
        showStoryFrameSelector()
        showEditModeUIControls(false)
        backgroundSurfaceManager.switchVideoPlayerOnFromUri(videoUri)
    }

    private fun showStaticBackground() {
        showStoryFrameSelector()
        showEditModeUIControls(true)
        backgroundSurfaceManager.switchStaticImageBackgroundModeOn()
    }

    private fun takeStillPicture() {
        if (backgroundSurfaceManager.cameraRecording() || cameraOperationInCourse) {
            return
        }

        cameraOperationInCourse = true
        camera_capture_button.startProgressingAnimation(CAMERA_STILL_PICTURE_ANIM_MS)
        backgroundSurfaceManager.takePicture(object : ImageCaptureListener {
            override fun onImageSaved(file: File) {
                runOnUiThread {
                    Glide.with(this@ComposeLoopFrameActivity)
                        .load(file)
                        .transform(CenterCrop(), RoundedCorners(16))
                        .into(gallery_upload_img)
                    Glide.with(this@ComposeLoopFrameActivity)
                        .load(file)
                        .transform(CenterCrop())
                        .into(photoEditorView.source)
                    storyViewModel.apply {
                        addStoryFrameItemToCurrentStory(
                            StoryFrameItem(BackgroundSource(file = file), frameItemType = StoryFrameItemType.IMAGE)
                        )
                        setSelectedFrame(0)
                    }
                    showStaticBackground()
                    currentOriginalCapturedFile = file
                    waitToReenableCapture()
                }
            }
            override fun onError(message: String, cause: Throwable?) {
                // TODO implement error handling
                runOnUiThread {
                    showToast("ERROR SAVING IMAGE")
                    waitToReenableCapture()
                }
            }
        })
    }

    private fun startRecordingVideoAfterVibrationIndication() {
        if (backgroundSurfaceManager.cameraRecording() || cameraOperationInCourse) {
            return
        }

        timesUpHandler.postDelayed({
            startRecordingVideo()
        }, VIBRATION_INDICATION_LENGTH_MS)
        hideVideoUIControls()
        showToast("VIDEO STARTED")
        vibrate()
    }

    private fun startRecordingVideo() {
        if (backgroundSurfaceManager.cameraRecording()) {
            return
        }

        cameraOperationInCourse = true
        // force stop recording video after maximum time limit reached
        timesUpHandler.postDelayed(timesUpRunnable, CAMERA_VIDEO_RECORD_MAX_LENGTH_MS)
        // strat progressing animation
        camera_capture_button.startProgressingAnimation(CAMERA_VIDEO_RECORD_MAX_LENGTH_MS)
        backgroundSurfaceManager.startRecordingVideo(object : VideoRecorderFinished {
            override fun onVideoSaved(file: File?) {
                currentOriginalCapturedFile = file
                file?.let {
                    runOnUiThread {
                        storyViewModel.apply {
                            addStoryFrameItemToCurrentStory(StoryFrameItem(BackgroundSource(file = it),
                                frameItemType = VIDEO))
                            setSelectedFrame(0)
                        }
                    }
                }
                runOnUiThread {
                    // now start playing the video we just recorded
                    showPlayVideo()
                }
                waitToReenableCapture()
            }

            override fun onError(message: String?, cause: Throwable?) {
                // TODO implement error handling
                runOnUiThread {
                    showToast("Video could not be saved: $message")
                }
                waitToReenableCapture()
            }
        })
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // Vibrate for 100 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                VIBRATION_INDICATION_LENGTH_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // deprecated in API 26
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_INDICATION_LENGTH_MS)
        }
    }

    private fun stopRecordingVideo(isCanceled: Boolean) {
        if (isCanceled) {
            // remove any pending callback if video was cancelled
            timesUpHandler.removeCallbacksAndMessages(null)
        }

        if (backgroundSurfaceManager.cameraRecording()) {
            camera_capture_button.stopProgressingAnimation()
            camera_capture_button.clearAnimation()
            camera_capture_button
                .animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .duration = PressAndHoldGestureHelper.CLICK_LENGTH / 4
            backgroundSurfaceManager.stopRecordingVideo()
            showVideoUIControls()
            if (isCanceled) {
                showToast("GESTURE CANCELLED, VIDEO SAVED")
            } else {
                showToast("VIDEO SAVED")
            }
        }
    }

    // artificial wait to re-enable capture mode
    private fun waitToReenableCapture() {
        timesUpHandler.postDelayed({
            cameraOperationInCourse = false
        }, CAMERA_STILL_PICTURE_WAIT_FOR_NEXT_CAPTURE_MS)
    }

    // this one saves one composed unit: ether an Image or a Video
    private fun saveLoopFrame() {
        // check wether we have an Image or a Video, and call its save functionality accordingly
        if (backgroundSurfaceManager.cameraVisible() || backgroundSurfaceManager.videoPlayerVisible()) {
            val currentBkgMedia = backgroundSurfaceManager.getCurrentBackgroundMedia()
            if (currentBkgMedia != null) {
                saveVideo(currentBkgMedia)
            } else {
                CrashLoggingUtils.log("An error occurred trying to save video, current background media not found")
                showToast("An error occurred trying to save video, current background media not found")
            }
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
            val file = getLoopFrameFile(this, false)
            try {
                file.createNewFile()

                val saveSettings = SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(true)
                    .build()

                photoEditor.saveAsFile(file.absolutePath, saveSettings, object : PhotoEditor.OnSaveListener {
                    override fun onSuccess(filePath: String) {
                        hideLoading()
                        deleteCapturedMedia()
                        sendNewLoopReadyBroadcast(file)
                        showSnackbar(
                            getString(R.string.label_snackbar_loop_frame_saved),
                            getString(R.string.label_snackbar_share),
                            OnClickListener { shareAction(file) }
                        )
                        hideEditModeUIControls()
                        switchCameraPreviewOn()
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
    private fun saveVideo(inputFile: Uri) {
        if (PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showLoading("Saving...")
            try {
                val file = getLoopFrameFile(this, true)
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
                            runOnUiThread {
                                hideLoading()
                                showSnackbar("No views added - original video saved")
                            }
                        }

                        override fun onSuccess(filePath: String) {
                            runOnUiThread {
                                hideLoading()
                                deleteCapturedMedia()
                                photoEditor.clearAllViews()
                                sendNewLoopReadyBroadcast(file)
                                showSnackbar(
                                    getString(R.string.label_snackbar_loop_frame_saved),
                                    getString(R.string.label_snackbar_share),
                                    OnClickListener { shareAction(file) }
                                )
                                hideEditModeUIControls()
                                switchCameraPreviewOn()
                            }
                        }

                        override fun onFailure(exception: Exception) {
                            runOnUiThread {
                                hideLoading()
                                showSnackbar("Failed to save Video")
                            }
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
                val file = getLoopFrameFile(this, true, "tmp")
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

                        override fun onSuccess(filePath: String) {
                            // now save the video with emoji, but using the previously saved video as input
                            hideLoading()
                            saveVideo(Uri.parse(filePath))
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

    private fun showLoading(message: String) {
        editModeHideAllUIControls(false)
        save_button.setSaving(true)
        blockTouchOnPhotoEditor()
    }

    private fun hideLoading() {
        editModeRestoreAllUIControls()
        save_button.setSaving(false)
        releaseTouchOnPhotoEditor()
    }

    private fun showSnackbar(message: String, actionLabel: String? = null, listener: OnClickListener? = null) {
        runOnUiThread {
            val view = findViewById<View>(android.R.id.content)
            if (view != null) {
                val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                snackbar.config(this)
                actionLabel?.let {
                    snackbar.setAction(it, listener)
                }
                snackbar.show()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP, 0, 0)
        toast.show()
    }

    private fun hideVideoUIControls() {
        camera_flash_button.visibility = View.INVISIBLE
        label_flash.visibility = View.INVISIBLE

        camera_flip_group.visibility = View.INVISIBLE

        container_gallery_upload.visibility = View.INVISIBLE
    }

    private fun showVideoUIControls() {
        camera_flash_button.visibility = View.VISIBLE
        label_flash.visibility = View.VISIBLE

        camera_flip_group.visibility = View.VISIBLE

        container_gallery_upload.visibility = View.VISIBLE
    }

    private fun showEditModeUIControls(noSound: Boolean) {
        // hide capturing mode controls
        hideVideoUIControls()
        camera_capture_button.visibility = View.INVISIBLE

        // show proper edit mode controls
        close_button.visibility = View.VISIBLE
        edit_mode_controls.visibility = View.VISIBLE
//        if (photoEditor.anyViewsAdded()) {
            // only show save button if any views have been added
            save_button.visibility = View.VISIBLE
//        }

        if (noSound) {
            sound_button_group.visibility = View.INVISIBLE
        } else {
            sound_button_group.visibility = View.VISIBLE
        }
    }

    private fun hideStoryFrameSelector() {
        (bottom_strip_view as StoryFrameSelectorFragment).hide()
    }

    private fun showStoryFrameSelector() {
        (bottom_strip_view as StoryFrameSelectorFragment).show()
    }

    private fun hideEditModeUIControls() {
        camera_capture_button.visibility = View.VISIBLE

        // hide proper edit mode controls
        close_button.visibility = View.INVISIBLE
        edit_mode_controls.visibility = View.INVISIBLE
        save_button.visibility = View.INVISIBLE
        // show capturing mode controls
        showVideoUIControls()
    }

    private fun editModeHideAllUIControls(hideSaveButton: Boolean) {
        // momentarily hide proper edit mode controls
        close_button.visibility = View.INVISIBLE
        edit_mode_controls.visibility = View.INVISIBLE
        sound_button_group.visibility = View.INVISIBLE
        if (hideSaveButton) {
            save_button.visibility = View.INVISIBLE
        }
    }

    private fun editModeRestoreAllUIControls() {
        // momentarily hide proper edit mode controls
        close_button.visibility = View.VISIBLE
        edit_mode_controls.visibility = View.VISIBLE

        // restore Save button if it was hidden before
        showSaveButtonIfViewsAdded()

        // noSound parameter here should be true if video player is off
        val noSound = !backgroundSurfaceManager.videoPlayerVisible()
        if (noSound) {
            sound_button_group.visibility = View.INVISIBLE
        } else {
            sound_button_group.visibility = View.VISIBLE
        }
    }

    private fun showSaveButtonIfViewsAdded() {
//        if (photoEditor.anyViewsAdded()) {
            // only show save button if any views have been added
            save_button.visibility = View.VISIBLE
//        } else {
//            save_button.visibility = View.INVISIBLE
//        }
    }

    private fun updateFlashModeSelectionIcon() {
        when (flashModeSelection) {
            FlashIndicatorState.AUTO ->
                camera_flash_button.background = getDrawable(R.drawable.ic_flash_auto_black_24dp)
            FlashIndicatorState.ON ->
                camera_flash_button.background = getDrawable(R.drawable.ic_flash_on_black_24dp)
            FlashIndicatorState.OFF ->
                camera_flash_button.background = getDrawable(R.drawable.ic_flash_off_black_24dp)
        }
    }

    private fun saveCameraSelectionPref() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt(getString(R.string.pref_camera_selection), cameraSelection.id)
            commit()
        }
    }

    private fun saveFlashModeSelectionPref() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt(getString(R.string.pref_flash_mode_selection), flashModeSelection.id)
            commit()
        }
    }

    private fun shareAction(mediaFile: File) {
        val apkURI = FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".provider", mediaFile)
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            setDataAndType(apkURI, "image/jpeg")
            putExtra(Intent.EXTRA_STREAM, apkURI)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.label_share_to)))
    }

    private fun sendNewLoopReadyBroadcast(mediaFile: File) {
        // Implicit broadcasts will be ignored for devices running API
        // level >= 24, so if you only target 24+ you can remove this statement
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            if (mediaFile.extension.startsWith("jpg")) {
                sendBroadcast(Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(mediaFile)))
            } else {
                sendBroadcast(Intent(Camera.ACTION_NEW_VIDEO, Uri.fromFile(mediaFile)))
            }
        }

        // If the folder selected is an external media directory, this is unnecessary
        // but otherwise other apps will not be able to access our images unless we
        // scan them using [MediaScannerConnection]
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(mediaFile.extension)
        MediaScannerConnection.scanFile(
            this, arrayOf(mediaFile.absolutePath), arrayOf(mimeType), null)
    }

    private fun blockTouchOnPhotoEditor() {
        translucent_view.visibility = View.VISIBLE
        translucent_view.setOnTouchListener { _, _ ->
            // no op
            true
        }
    }

    private fun releaseTouchOnPhotoEditor() {
        translucent_view.visibility = View.GONE
        translucent_view.setOnTouchListener(null)
    }

    private fun calculateScreenSize() {
        val size = getDisplayPixelSize(this)
        screenSizeX = size.x
        screenSizeY = size.y
    }

    override fun onStoryFrameSelected(oldIndex: Int, index: Int) {
        if (index != oldIndex) {
            // first, remember the currently added views
            val currentStoryFrameItem = storyViewModel.getCurrentStoryFrameAt(oldIndex)

            // set addedViews on the current frame (copy array so we don't share the same one with PhotoEditor)
            currentStoryFrameItem.addedViews = AddedViewList(photoEditor.getViewsAdded())

            // now clear addedViews so we don't leak View.Context
            photoEditor.clearAllViews()

            // now set the current capturedFile to be the one pointed to by the index frame
            val newSelectedFrame = storyViewModel.setSelectedFrame(index)
            val source = newSelectedFrame.source
            if (source.isFile()) {
                currentOriginalCapturedFile = source.file
            }

            // decide which background surface to activate here, possibilities are:
            // 1. video/uri source
            // 2. video/file source
            // 3. image/uri source
            // 4. image/file source
            if (newSelectedFrame.frameItemType == VIDEO) {
                source.apply {
                    if (isFile()) {
                        backgroundSurfaceManager.switchVideoPlayerOnFromFile(file)
                    } else contentUri?.let {
                        backgroundSurfaceManager.switchVideoPlayerOnFromUri(it)
                    }
                }
            } else {
                Glide.with(this@ComposeLoopFrameActivity)
                    .load(source.file ?: source.contentUri)
                    .transform(CenterCrop())
                    .into(photoEditorView.source)
                showStaticBackground()
            }

            // now call addViewToParent the addedViews remembered by this frame
            newSelectedFrame.addedViews.let {
                for (oneView in it) {
                    photoEditor.addViewToParent(oneView.view, oneView.viewType)
                }
            }
        }
    }

    override fun onStoryFrameAddTapped() {
        launchCameraPreview()
    }

    private inner class FlingGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            e1?.let {
                e2?.let {
                    if (e1.y - e2.y > SWIPE_MIN_DISTANCE && abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        // Bottom to top
                        val ycoordStart = e1.y
                        if ((screenSizeY - ycoordStart) < SWIPE_MIN_DISTANCE_FROM_BOTTOM) {
                            // if swipe started as close as bottom of the screen as possible, then interpret this
                            // as a swipe from bottom of the screen gesture

                            // in edit mode, show Emoji picker
                            if (edit_mode_controls.visibility == View.VISIBLE) {
                                emojiPickerFragment.show(supportFragmentManager, emojiPickerFragment.tag)
                            } else {
                                // in capture mode, show media picker
                                showMediaPicker()
                            }
                        }
                        return false
                    } else if (e2.y - e1.y > SWIPE_MIN_DISTANCE &&
                        abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        // Top to bottom
                        return false
                    }
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    companion object {
        private const val FRAGMENT_DIALOG = "dialog"

        private const val SURFACE_MANAGER_READY_LAUNCH_DELAY = 500L
        private const val CAMERA_VIDEO_RECORD_MAX_LENGTH_MS = 10000L
        private const val CAMERA_STILL_PICTURE_ANIM_MS = 300L
        private const val CAMERA_STILL_PICTURE_WAIT_FOR_NEXT_CAPTURE_MS = 1000L
        private const val STATE_KEY_CURRENT_ORIGINAL_CAPTURED_FILE = "key_current_original_captured_file"
        private const val VIBRATION_INDICATION_LENGTH_MS = 100L
        private const val SWIPE_MIN_DISTANCE = 120
        private const val SWIPE_MIN_DISTANCE_FROM_BOTTOM = 80
        private const val SWIPE_THRESHOLD_VELOCITY = 200
    }
}
