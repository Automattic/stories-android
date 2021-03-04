package com.wordpress.stories.compose

import android.Manifest
import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Drawable
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.Matrix
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.automattic.photoeditor.text.FontResolver
import com.automattic.photoeditor.OnPhotoEditorListener
import com.automattic.photoeditor.PhotoEditor
import com.automattic.photoeditor.SaveSettings
import com.automattic.photoeditor.text.TextStyler
import com.automattic.photoeditor.camera.interfaces.CameraSelection
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState
import com.automattic.photoeditor.camera.interfaces.ImageCaptureListener
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFinished
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFragment.FlashSupportChangeListener
import com.automattic.photoeditor.state.AuthenticationHeadersInterface
import com.automattic.photoeditor.state.BackgroundSurfaceManager
import com.automattic.photoeditor.state.BackgroundSurfaceManagerReadyListener
import com.automattic.photoeditor.util.FileUtils
import com.automattic.photoeditor.util.FileUtils.Companion.getLoopFrameFile
import com.automattic.photoeditor.text.IdentifiableTypeface
import com.automattic.photoeditor.text.IdentifiableTypeface.TypefaceId
import com.automattic.photoeditor.util.PermissionUtils
import com.automattic.photoeditor.views.ViewType
import com.automattic.photoeditor.views.ViewType.TEXT
import com.automattic.photoeditor.views.added.AddedViewList
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView
import com.wordpress.stories.BuildConfig
import com.wordpress.stories.R
import com.wordpress.stories.compose.ComposeLoopFrameActivity.ExternalMediaPickerRequestCodesAndExtraKeys
import com.wordpress.stories.compose.FinishButton.FinishButtonMode.DONE
import com.wordpress.stories.compose.ScreenTouchBlockMode.BLOCK_TOUCH_MODE_DELETE_SLIDE
import com.wordpress.stories.compose.ScreenTouchBlockMode.BLOCK_TOUCH_MODE_FULL_SCREEN
import com.wordpress.stories.compose.ScreenTouchBlockMode.BLOCK_TOUCH_MODE_NONE
import com.wordpress.stories.compose.ScreenTouchBlockMode.BLOCK_TOUCH_MODE_PHOTO_EDITOR_ERROR_PENDING_RESOLUTION
import com.wordpress.stories.compose.ScreenTouchBlockMode.BLOCK_TOUCH_MODE_PHOTO_EDITOR_READY
import com.wordpress.stories.compose.emoji.EmojiPickerFragment
import com.wordpress.stories.compose.emoji.EmojiPickerFragment.EmojiListener
import com.wordpress.stories.compose.frame.FrameIndex
import com.wordpress.stories.compose.frame.FrameSaveManager
import com.wordpress.stories.compose.frame.FrameSaveNotifier
import com.wordpress.stories.compose.frame.FrameSaveService
import com.wordpress.stories.compose.frame.StoryNotificationType
import com.wordpress.stories.compose.frame.StorySaveEvents
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveError
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveSuccess
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.OnStoryFrameSelectorTappedListener
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundViewInfo
import com.wordpress.stories.compose.story.StoryFrameItemType
import com.wordpress.stories.compose.story.StoryFrameItemType.IMAGE
import com.wordpress.stories.compose.story.StoryFrameItemType.VIDEO
import com.wordpress.stories.compose.story.StoryFrameSelectorFragment
import com.wordpress.stories.compose.story.StoryIndex
import com.wordpress.stories.compose.story.StoryRepository
import com.wordpress.stories.compose.story.StorySerializerUtils
import com.wordpress.stories.compose.story.StoryViewModel
import com.wordpress.stories.compose.story.StoryViewModel.StoryFrameListItemUiState.StoryFrameListItemUiStateFrame
import com.wordpress.stories.compose.story.StoryViewModelFactory
import com.wordpress.stories.compose.text.TextEditorDialogFragment
import com.wordpress.stories.compose.text.TextStyleGroupManager
import com.wordpress.stories.util.KEY_STORY_EDIT_MODE
import com.wordpress.stories.util.KEY_STORY_SAVE_RESULT
import com.wordpress.stories.util.STATE_KEY_CURRENT_STORY_INDEX
import com.wordpress.stories.util.getDisplayPixelSize
import com.wordpress.stories.util.getStoryIndexFromIntentOrBundle
import com.wordpress.stories.util.isScreenTallerThan916
import com.wordpress.stories.util.isVideo
import com.wordpress.stories.util.normalizeSizeExportTo916
import kotlinx.android.synthetic.main.activity_composer.*
import kotlinx.android.synthetic.main.content_composer.*
import kotlinx.android.synthetic.main.fragment_story_frame_selector.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.IOException
import kotlin.math.abs

fun Group.setAllOnClickListener(listener: OnClickListener?) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id).setOnClickListener(listener)
    }
}

enum class ScreenTouchBlockMode {
    BLOCK_TOUCH_MODE_NONE,
    BLOCK_TOUCH_MODE_FULL_SCREEN, // used when saving - user is not allowed to touch anything
    BLOCK_TOUCH_MODE_PHOTO_EDITOR_ERROR_PENDING_RESOLUTION, // used when in error resolution mode: user needs to take
    // action, so we allow them to use the StoryFrameSelector and menu, but no edits on
    // the Photo Editor canvas are allowed at this stage
    BLOCK_TOUCH_MODE_PHOTO_EDITOR_READY, // used when errors have been sorted out by the user - no edits allowed,
    // but they should be good to upload the Story now
    BLOCK_TOUCH_MODE_DELETE_SLIDE // Used in delete slide mode, tapping the screen releases the block
}

interface SnackbarProvider {
    fun showProvidedSnackbar(message: String, actionLabel: String?, callback: () -> Unit)
}

interface MediaPickerProvider {
    fun setupRequestCodes(requestCodes: ExternalMediaPickerRequestCodesAndExtraKeys)
    fun showProvidedMediaPicker()
    fun providerHandlesOnActivityResult(): Boolean
}

interface NotificationIntentLoader {
    fun loadIntentForErrorNotification(): Intent
    fun loadPendingIntentForErrorNotificationDeletion(notificationId: Int): PendingIntent?
    fun setupErrorNotificationBaseId(): Int
}

interface NotificationTrackerProvider {
    fun trackShownNotification(storyNotificationType: StoryNotificationType)
    fun trackTappedNotification(storyNotificationType: StoryNotificationType)
    fun trackDismissedNotification(storyNotificationType: StoryNotificationType)
}

interface AuthenticationHeadersProvider {
    fun getAuthHeaders(url: String): Map<String, String>?
}

// metadata is going to be passed from init to finish so the caller can identify and add whatever information they need
interface MetadataProvider {
    fun loadMetadataForStory(index: StoryIndex): Bundle?
}

interface PrepublishingEventProvider {
    fun onStorySaveButtonPressed()
}

interface PermanentPermissionDenialDialogProvider {
    fun showPermissionPermanentlyDeniedDialog(permission: String)
}

interface GenericAnnouncementDialogProvider {
    fun showGenericAnnouncementDialog()
}

interface StoryDiscardListener {
    fun onStoryDiscarded()
    fun onFrameRemove(storyIndex: StoryIndex, storyFrameIndex: Int) // called right before actual removal
}

abstract class ComposeLoopFrameActivity : AppCompatActivity(), OnStoryFrameSelectorTappedListener {
    private lateinit var photoEditor: PhotoEditor
    private lateinit var backgroundSurfaceManager: BackgroundSurfaceManager
    private var currentOriginalCapturedFile: File? = null
    private lateinit var workingAreaRect: Rect
    private var bottomOpaqueBarHeight: Int = 0 // default: no opaque bottom bar

    private val timesUpRunnable = Runnable {
        stopRecordingVideo(false) // time's up, it's not a cancellation
    }
    private val timesUpHandler = Handler()
    private var launchCameraRequestPending = false
    private var launchVideoPlayerRequestPending = false
    private lateinit var launchVideoPlayerRequestPendingSource: BackgroundSource
    private var cameraOperationInCourse = false

    private var cameraSelection = CameraSelection.BACK
    private var flashModeSelection = FlashIndicatorState.OFF

    private lateinit var emojiPickerFragment: EmojiPickerFragment
    private lateinit var swipeDetector: GestureDetectorCompat
    private var screenSizeX: Int = 0
    private var screenSizeY: Int = 0
    private var topControlsBaseTopMargin: Int = 0
    private var nextButtonBaseTopMargin: Int = 0
    private var bottomNavigationBarMargin: Int = 0
    private var isEditingText: Boolean = false

    private lateinit var storyViewModel: StoryViewModel
    private lateinit var transition: LayoutTransition

    private lateinit var frameSaveService: FrameSaveService
    private var saveServiceBound: Boolean = false
    private var preHookRun: Boolean = false
    private var storyIndexToSelect = -1
    private var storyFrameIndexToRetry: FrameIndex = StoryRepository.DEFAULT_FRAME_NONE_SELECTED
    private var snackbarProvider: SnackbarProvider? = null
    private var mediaPickerProvider: MediaPickerProvider? = null
    private var notificationIntentLoader: NotificationIntentLoader? = null
    private var authHeadersProvider: AuthenticationHeadersProvider? = null
    private var metadataProvider: MetadataProvider? = null
    private var storyDiscardListener: StoryDiscardListener? = null
    private var analyticsListener: StoriesAnalyticsListener? = null
    private var notificationTrackerProvider: NotificationTrackerProvider? = null
    private var prepublishingEventProvider: PrepublishingEventProvider? = null
    private var firstIntentLoaded: Boolean = false
    protected var permissionsRequestForCameraInProgress: Boolean = false
    private var permissionDenialDialogProvider: PermanentPermissionDenialDialogProvider? = null
    private var genericAnnouncementDialogProvider: GenericAnnouncementDialogProvider? = null
    private var showGenericAnnouncementDialogWhenReady = false
    private var useTempCaptureFile = true

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d("ComposeLoopFrame", "onServiceConnected()")
            val binder = service as FrameSaveService.FrameSaveServiceBinder
            frameSaveService = binder.getService()
            frameSaveService.useTempCaptureFile = useTempCaptureFile
            frameSaveService.isEditMode = intent.getBooleanExtra(KEY_STORY_EDIT_MODE, false)

            // keep these as they're changing when we call `storyViewModel.finishCurrentStory()`
            val storyIndex = storyViewModel.getCurrentStoryIndex()

            // Setup notification intent for notifications triggered from the FrameSaveService.FrameSaveNotifier class
            notificationIntentLoader?.let {
                // set the base notification Error Id. This is given on purpose so the host app can give a unique
                // set of notific   ations ID to base our error notifications from, and avoid collision with other
                // notifications the host app may have
                // IMPORTANT: this needs to be the first call in the methods linedup for NotificationIntentLoader
                frameSaveService.setNotificationErrorBaseId(
                    it.setupErrorNotificationBaseId()
                )

                frameSaveService.setNotificationIntent(it.loadIntentForErrorNotification())
                val notificationId = FrameSaveNotifier.getNotificationIdForError(
                    frameSaveService.getNotificationErrorBaseId(),
                    storyIndex
                )

                frameSaveService.setDeleteNotificationPendingIntent(
                    it.loadPendingIntentForErrorNotificationDeletion(notificationId)
                )
            }

            // setup notification tracker if such a thing exists
            notificationTrackerProvider?.let {
                frameSaveService.setNotificationTrackerProvider(it)
            }

            metadataProvider?.let {
                frameSaveService.setMetadata(it.loadMetadataForStory(storyIndex))
            }

            frameSaveService.saveStoryFrames(storyIndex, photoEditor, storyFrameIndexToRetry)
            saveServiceBound = true

            // only leave the Activity if we're saving the full Story. Stay here if the user is retrying.
            if (storyFrameIndexToRetry == -1) {
                // leave the Activity - now it's all the app's responsibility to deal with saving, uploading and
                // publishing. Users can't edit this Story now, unless an error happens and then we'll notify them
                // and let them open the Composer screen again.
                finish()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d("ComposeLoopFrame", "onServiceDisconnected()")
            saveServiceBound = false
        }
    }

    private fun calculateWorkingArea(): Rect {
        val location = IntArray(2)
        photoEditorView.getLocationOnScreen(location)
        val xCoord = location[0]
        val yCoord = location[1]
        val width = photoEditorView.measuredWidth
        val height = photoEditorView.measuredHeight

        val normalizedScreenSize = normalizeSizeExportTo916(width, height).toSize()

        val bottomAreaHeight = resources.getDimensionPixelSize(R.dimen.bottom_strip_height) + bottomNavigationBarMargin
        val topAreaHeight = resources.getDimensionPixelSize(R.dimen.edit_mode_button_size)

        return Rect(
            xCoord,
            yCoord + topAreaHeight,
            xCoord + normalizedScreenSize.width,
            yCoord + normalizedScreenSize.height - bottomAreaHeight
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_composer)
        EventBus.getDefault().register(this)

        topControlsBaseTopMargin = getLayoutTopMarginBeforeInset(close_button.layoutParams)
        nextButtonBaseTopMargin = getLayoutTopMarginBeforeInset(next_button.layoutParams)
        ViewCompat.setOnApplyWindowInsetsListener(compose_loop_frame_layout) { _, insets ->
            // set insetTop as margin to all controls appearing at the top of the screen
            addInsetTopMargin(next_button.layoutParams, nextButtonBaseTopMargin, insets.systemWindowInsetTop)
            addInsetTopMargin(close_button.layoutParams, topControlsBaseTopMargin, insets.systemWindowInsetTop)
            addInsetTopMargin(control_flash_group.layoutParams, topControlsBaseTopMargin, insets.systemWindowInsetTop)
            bottomNavigationBarMargin = insets.systemWindowInsetBottom
            workingAreaRect = calculateWorkingArea()
            photoEditor.updateWorkAreaRect(workingAreaRect)
            bottomOpaqueBarHeight = preCalculateOpaqueBarHeight()
            delete_view.addBottomOffset(bottomNavigationBarMargin)
            delete_slide_view.addBottomOffset(bottomNavigationBarMargin)
            (bottom_strip_view as StoryFrameSelectorFragment).setBottomOffset(bottomNavigationBarMargin)
            insets
        }

        val authHeaderInterfaceBridge = object : AuthenticationHeadersInterface {
            override fun getAuthHeaders(url: String): Map<String, String>? {
                return authHeadersProvider?.getAuthHeaders(url)
            }
        }

        // Pre-load the custom fonts if necessary
        TextStyleGroupManager.preloadFonts(this)

        workingAreaRect = calculateWorkingArea()
        photoEditor = PhotoEditor.Builder(this, photoEditorView)
            .setPinchTextScalable(true) // set flag to make text scalable when pinch
            .setDeleteView(delete_view)
            .setWorkAreaRect(workingAreaRect)
            .setAuthenticatitonHeaderInterface(authHeaderInterfaceBridge)
            .build() // build photo editor sdk

        photoEditor.setOnPhotoEditorListener(object : OnPhotoEditorListener {
            override fun onEditTextChangeListener(
                rootView: View,
                text: String,
                textStyler: TextStyler?
            ) {
                if (isEditingText) {
                    return
                }

                isEditingText = true
                editModeHideAllUIControls(hideNextButton = true, hideCloseButton = true)
                // Hide the text in the background while it's being edited
                rootView.visibility = View.INVISIBLE

                val textEditorDialogFragment = TextEditorDialogFragment.show(
                    this@ComposeLoopFrameActivity,
                    text,
                    textStyler)
                textEditorDialogFragment.setOnTextEditorListener(object : TextEditorDialogFragment.TextEditor {
                    override fun onDone(inputText: String, textStyler: TextStyler) {
                        // fixes https://github.com/Automattic/stories-android/issues/453
                        // when don't keep activities is ON, the onDismiss override gets called only through
                        // Activity.onDestroy() -> Fragment.onDestroy() (see stacktrace)
                        if (lifecycle.currentState == DESTROYED) {
                            return
                        }
                        isEditingText = false
                        // make sure to set it to visible, as newly added views are originally hidden until
                        // proper text is set
                        rootView.visibility = View.VISIBLE
                        if (TextUtils.isEmpty(inputText)) {
                            // just remove the view here, we don't need it - also don't  add to the `redo` stack
                            photoEditor.viewUndo(rootView, TEXT, false)
                        } else {
                            photoEditor.editText(rootView, inputText, textStyler)
                        }
                        editModeRestoreAllUIControls()
                    }
                })
                textEditorDialogFragment.setAnalyticsEventListener(analyticsListener)
            }

            override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                // before: only show save button if any views have been added
            }

            override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                next_button.visibility = View.VISIBLE
            }

            override fun onStartViewChangeListener(viewType: ViewType) {
                // in this case, also hide the SAVE button, but don't hide the bottom strip view.
                editModeHideAllUIControls(hideNextButton = true, hideCloseButton = false, hideFrameSelector = false)
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

            override fun getWorkingAreaRect(): Rect? {
                return workingAreaRect
            }
        })

        photoEditor.setFontResolver(object : FontResolver {
            override fun resolve(@TypefaceId typefaceId: Int): IdentifiableTypeface {
                return TextStyleGroupManager.getIdentifiableTypefaceForId(typefaceId)
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
            BuildConfig.USE_CAMERAX,
            object : BackgroundSurfaceManagerReadyListener {
                override fun onBackgroundSurfaceManagerReady() {
                    if (savedInstanceState == null && !firstIntentLoaded) {
                        onLoadFromIntent(intent)
                        firstIntentLoaded = true
                    }

                    if (launchCameraRequestPending) {
                        launchCameraRequestPending = false
                        launchCameraPreviewWithSurfaceSafeguard()
                    } else if (launchVideoPlayerRequestPending) {
                        launchVideoPlayerRequestPending = false
                        showPlayVideoWithSurfaceSafeguard(launchVideoPlayerRequestPendingSource)
                    }
                }
            },
                authHeaderInterfaceBridge,
                useTempCaptureFile
        )

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

        // before instantiating the ViewModel, we need to get the storyIndexToSelect
        storyIndexToSelect = getStoryIndexFromIntentOrBundle(savedInstanceState, intent)

        storyViewModel = ViewModelProvider(this,
            StoryViewModelFactory(StoryRepository, storyIndexToSelect)
        )[StoryViewModel::class.java]

        // request the BackgroundSurfaceManager to prime the textureView so it's ready when needed.
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

        setupStoryViewModelObservers()

        if (intent.getBooleanExtra(KEY_STORY_EDIT_MODE, false)) {
            next_button.buttonMode = DONE
        }

        if (savedInstanceState != null) {
            currentOriginalCapturedFile =
                savedInstanceState.getSerializable(STATE_KEY_CURRENT_ORIGINAL_CAPTURED_FILE) as File?
            preHookRun = savedInstanceState.getBoolean(STATE_KEY_PREHOOK_RUN)

            firstIntentLoaded = savedInstanceState.getBoolean(STATE_KEY_FIRST_INTENT_LOADED)
            permissionsRequestForCameraInProgress = savedInstanceState.getBoolean(STATE_KEY_PERMISSION_REQ_IN_PROGRESS)

            storyViewModel.replaceCurrentStory(
                StorySerializerUtils.deserializeStory(
                        requireNotNull(savedInstanceState.getString(STATE_KEY_STORY_SAVE_STATE))
                )
            )

            val selectedFrameIndex = savedInstanceState.getInt(STATE_KEY_STORY_SAVE_STATE_SELECTED_FRAME)
            if (selectedFrameIndex < storyViewModel.getCurrentStorySize()) {
                storyViewModel.setSelectedFrame(selectedFrameIndex)
            }
        } else if (storyIndexToSelect != StoryRepository.DEFAULT_NONE_SELECTED) {
            onLoadFromIntent(intent)
        }
    }

    private fun preCalculateOpaqueBarHeight(): Int {
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        if (isScreenTallerThan916(width, height)) {
            val normalizedSize = normalizeSizeExportTo916(width, height).toSize()
            return (height - normalizedSize.height)
        } else {
            return 0
        }
    }

    private fun setOpaqueBarHeightAndStoryFrameSelectorBackgroundColor() {
        if (bottomOpaqueBarHeight > 0) {
            bottom_opaque_bar.layoutParams.height = bottomOpaqueBarHeight
        } else {
            bottom_opaque_bar.visibility = View.GONE
        }
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        if (isScreenTallerThan916(screenWidth, screenHeight)) {
            (bottom_strip_view as StoryFrameSelectorFragment)
                    .setBackgroundColor(R.color.black_opaque_story_frame_selector)
        } else {
            (bottom_strip_view as StoryFrameSelectorFragment)
                    .setBackgroundColor(R.color.black_transp_story_frame_selector)
        }
    }

    override fun onStart() {
        super.onStart()
        val selectedFrameIndex = storyViewModel.getSelectedFrameIndex()
        if (!launchCameraRequestPending && !launchVideoPlayerRequestPending &&
                selectedFrameIndex < storyViewModel.getCurrentStorySize()) {
            updateBackgroundSurfaceUIWithStoryFrame(selectedFrameIndex)
        }
        // upon loading an existing Story, show the generic announcement dialog if present
        if (showGenericAnnouncementDialogWhenReady) {
            showGenericAnnouncementDialogWhenReady = false
            genericAnnouncementDialogProvider?.showGenericAnnouncementDialog()
        }
    }

    private fun setupStoryViewModelObservers() {
        storyViewModel.uiState.observe(this, Observer {
            // if no frames in Story, finish
            // note momentarily there will be times when this LiveData is triggered while permissions are
            // being requested so, don't proceed if that is the case
            if (storyViewModel.getCurrentStorySize() == 0 &&
                    firstIntentLoaded && !permissionsRequestForCameraInProgress) {
                // finally, delete the captured media
                deleteCapturedMedia()
                finish()
            }
        })

        storyViewModel.onSelectedFrameIndex.observe(this, Observer { selectedFrameIndexChange ->
            updateSelectedFrameControls(selectedFrameIndexChange.first, selectedFrameIndexChange.second)
        })

        storyViewModel.erroredItemUiState.observe(this, Observer { uiStateFrame ->
            updateContentUiStateFrame(uiStateFrame)
        })

        storyViewModel.muteFrameAudioUiState.observe(this, Observer { frameIndex ->
            updateUiStateForAudioMuted(frameIndex)
        })
    }

    private fun updateUiStateForAudioMuted(frameIndex: Int) {
        if (frameIndex == storyViewModel.getSelectedFrameIndex()) {
            updateSoundControl()
        }
    }

    @Suppress("unused")
    private fun updateSelectedFrameControls(oldSelection: Int, newSelection: Int) {
        if (storyViewModel.getCurrentStorySize() > newSelection) {
            val selectedFrame = storyViewModel.getCurrentStoryFrameAt(newSelection)
            updateSoundControl()
            showRetryButtonAndHideEditControlsForErroredFrame(selectedFrame?.saveResultReason !is SaveSuccess)
        }
    }

    // this will invoked when a RETRY operation ends on the currently selected frame
    private fun updateContentUiStateFrame(uiStateFrame: StoryFrameListItemUiStateFrame) {
        showRetryButtonAndHideEditControlsForErroredFrame(uiStateFrame.errored)
    }

    private fun showRetryButtonAndHideEditControlsForErroredFrame(showRetry: Boolean) {
        if (showRetry) {
            disableEditControlsForErroredFrame()
        } else {
            enableEditControlsForNonErroredFrame()
        }
    }

    private fun prepareErrorScreen(storySaveResult: StorySaveResult) {
        // disable the Publish button - need to pass a postDelayed given it somehow doesn't play well right on start
        // being shown
        next_button.postDelayed({
            next_button.isEnabled = false
        }, 500)

        val errors = storySaveResult.frameSaveResult.filter { it.resultReason is SaveError }
        val minIndexToSelect = errors.minBy { it.frameIndex }

        // select the first errored frame
        onStoryFrameSelected(
            oldIndex = StoryRepository.DEFAULT_FRAME_NONE_SELECTED,
            newIndex = minIndexToSelect!!.frameIndex
        )

        // show dialog
        val stringSingularOrPlural = if (errors.size == 1)
            getString(R.string.dialog_story_saving_error_title_singular)
        else getString(R.string.dialog_story_saving_error_title_plural)

        val errorDialogTitle = String.format(stringSingularOrPlural, errors.size)

        FrameSaveErrorDialog.newInstance(
            errorDialogTitle,
            getString(R.string.dialog_story_saving_error_message),
            getString(android.R.string.ok)
        ).show(supportFragmentManager, FRAGMENT_DIALOG)
    }

    private fun checkForLowSpaceAndShowDialog() {
        if (FileUtils.isAvailableSpaceLow(this)) {
            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                FrameSaveErrorDialog.newInstance(
                    title = getString(R.string.dialog_insufficient_device_storage_error_title),
                    message = getString(R.string.dialog_insufficient_device_storage_error_message),
                    okButtonLabel = getString(R.string.dialog_insufficient_device_storage_error_ok_button),
                    listener = object : FrameSaveErrorDialogOk {
                        override fun OnOkClicked(dialog: DialogFragment) {
                            dialog.dismiss()
                            val settingsIntent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                            if (settingsIntent.resolveActivity(packageManager) != null) {
                                startActivity(settingsIntent)
                            }
                        }
                    }).show(supportFragmentManager, FRAGMENT_DIALOG)
            } else {
                FrameSaveErrorDialog.newInstance(
                    title = getString(R.string.dialog_insufficient_device_storage_error_title),
                    message = getString(R.string.dialog_insufficient_device_storage_error_message)
                ).show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // See https://developer.android.com/reference/android/app/Activity#onNewIntent(android.content.Intent)
        setIntent(intent)
        onLoadFromIntent(intent)
    }

    protected open fun onLoadFromIntent(intent: Intent) {
        val partialCameraOperationInProgress = intent.hasExtra(requestCodes.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED) ||
                permissionsRequestForCameraInProgress

        if (storyViewModel.getCurrentStoryIndex() == StoryRepository.DEFAULT_NONE_SELECTED) {
            storyViewModel.loadStory(storyIndexToSelect)
            storyIndexToSelect = storyViewModel.getCurrentStoryIndex()
        } else if (!partialCameraOperationInProgress && storyIndexToSelect != StoryRepository.DEFAULT_NONE_SELECTED &&
                StoryRepository.getStoryAtIndex(storyIndexToSelect).frames.isNotEmpty()) {
            storyViewModel.loadStory(storyIndexToSelect)
            showGenericAnnouncementDialogWhenReady = true
            return
        }

        if (partialCameraOperationInProgress) {
            launchCameraPreviewWithSurfaceSafeguard()
            checkForLowSpaceAndShowDialog()
        } else if (intent.hasExtra(KEY_STORY_SAVE_RESULT)) {
            val storySaveResult = intent.getParcelableExtra(KEY_STORY_SAVE_RESULT) as StorySaveResult?
            if (storySaveResult != null &&
                    StoryRepository.getStoryAtIndex(storySaveResult.storyIndex).frames.isNotEmpty()) {
                // dismiss the error notification
                intent.action?.let {
                    val notificationManager = NotificationManagerCompat.from(this)
                    notificationManager.cancel(it.toInt())
                }

                if (!storySaveResult.isSuccess()) {
                    prepareErrorScreen(storySaveResult)
                } else {
                    onStoryFrameSelected(oldIndex = StoryRepository.DEFAULT_FRAME_NONE_SELECTED, newIndex = 0)
                }
            } else {
                showToast(getString(R.string.toast_story_page_not_found))
                finish()
            }
        } else if (intent.hasExtra(requestCodes.EXTRA_MEDIA_URIS)) {
            val uriList: List<Uri> = convertStringArrayIntoUrisList(
                    intent.getStringArrayExtra(requestCodes.EXTRA_MEDIA_URIS)
            )
            addFramesToStoryFromMediaUriList(uriList)
            setDefaultSelectionAndUpdateBackgroundSurfaceUI(uriList)
        }
    }

    override fun onDestroy() {
        doUnbindService()
        EventBus.getDefault().unregister(this)
        super.onDestroy()
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
            workingAreaRect = calculateWorkingArea()
            photoEditor.updateWorkAreaRect(workingAreaRect)
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        backgroundSurfaceManager.saveStateToBundle(outState)
        outState.putSerializable(STATE_KEY_CURRENT_ORIGINAL_CAPTURED_FILE, currentOriginalCapturedFile)
        outState.putInt(STATE_KEY_CURRENT_STORY_INDEX, storyIndexToSelect)
        outState.putBoolean(STATE_KEY_PREHOOK_RUN, preHookRun)
        outState.putBoolean(STATE_KEY_FIRST_INTENT_LOADED, firstIntentLoaded)
        outState.putBoolean(STATE_KEY_PERMISSION_REQ_IN_PROGRESS, permissionsRequestForCameraInProgress)

        // save Story slide (frame) state
        addCurrentViewsToFrameAtIndex(storyViewModel.getSelectedFrameIndex())
        outState.putString(STATE_KEY_STORY_SAVE_STATE, StorySerializerUtils.serializeStory(
                storyViewModel.getStoryAtIndex(storyViewModel.getCurrentStoryIndex())
            )
        )
        outState.putInt(STATE_KEY_STORY_SAVE_STATE_SELECTED_FRAME, storyViewModel.getSelectedFrameIndex())
        super.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtils.processRequestedPermissionsResultAndSave(this, permissions, grantResults)
        if (PermissionUtils.allRequestedPermissionsGranted(grantResults)) {
            onLoadFromIntent(intent)
        } else if (permissions.isEmpty() || storyViewModel.getCurrentStorySize() == 0) {
            // an empty permissions array means the user cancelled giving permissions. End the interaction.
            // same if we are already in the midst of requesting permission for camera but user will refuse some,
            // and there are 0 frames on the current story (just bail).
            // On the contrary, if we already have some Story slides/frames, we will just stay where we are,
            // and people can try adding more slides from the provided media picker.
            finish()
        } else {
            // if user won't give permissions but we have an ongoing Story with frames, just come back to where we were.
            showCurrentSelectedFrame()
        }
        // clear flag in the end only - we are in onRequestPermissionsResult so that means the request has a result
        // already, and clearing it as a last step given we need to check it's state in the above calls to
        // onLoadIntent(), but also clear it if the user denied permissions this time.
        permissionsRequestForCameraInProgress = false
    }

    override fun onBackPressed() {
        if (!backgroundSurfaceManager.cameraVisible()) {
            close_button.performClick()
        } else if (storyViewModel.getCurrentStorySize() > 0) {
            showCurrentSelectedFrame()
        } else {
            storyDiscardListener?.onStoryDiscarded()
            super.onBackPressed()
        }
    }

    private fun showCurrentSelectedFrame() {
        // get currently selected frame and check whether this is a video or an image
        updateBackgroundSurfaceUIWithStoryFrame(storyViewModel.getSelectedFrameIndex())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        swipeDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            requestCodes.PHOTO_PICKER -> if (resultCode == Activity.RESULT_OK && data != null) {
                val providerHandlesMediaPickerResult = mediaPickerProvider?.providerHandlesOnActivityResult() ?: false
                if (data.hasExtra(requestCodes.EXTRA_MEDIA_URIS) && !providerHandlesMediaPickerResult) {
                    val uriList: List<Uri> = convertStringArrayIntoUrisList(
                        data.getStringArrayExtra(requestCodes.EXTRA_MEDIA_URIS)
                    )
                    addFramesToStoryFromMediaUriList(uriList)
                    setDefaultSelectionAndUpdateBackgroundSurfaceUI(uriList)
                } else if (data.hasExtra(requestCodes.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED)) {
                    if (!PermissionUtils.allRequiredPermissionsGranted(this)) {
                        // at this point, the user wants to launch the camera
                        // but we need to check whether we have permissions for that.
                        // after permissions are requested, we need the original intent to be set differently
                        // so: we need to tweak the intent for when we come back after user gives us permission
                        if (intent.hasExtra(requestCodes.EXTRA_MEDIA_URIS)) {
                            intent.removeExtra(requestCodes.EXTRA_MEDIA_URIS)
                        }
                    }
                    launchCameraPreviewWithSurfaceSafeguard()
                }
            }
        }
    }

    protected fun convertStringArrayIntoUrisList(stringArray: Array<String>): List<Uri> {
        val uris: ArrayList<Uri> = ArrayList(stringArray.size)
        for (stringUri in stringArray) {
            uris.add(Uri.parse(stringUri))
        }
        return uris
    }

    protected fun setDefaultSelectionAndUpdateBackgroundSurfaceUI(uriList: List<Uri>) {
        val defaultSelectedFrameIndex = storyViewModel.getCurrentStorySize() - uriList.size
        storyViewModel.setSelectedFrame(defaultSelectedFrameIndex)
        updateBackgroundSurfaceUIWithStoryFrame(defaultSelectedFrameIndex)
    }

    protected fun addFramesToStoryFromMediaUriList(uriList: List<Uri>) {
        for (mediaUri in uriList) {
            addFrameToStoryFromMediaUri(mediaUri)
        }
    }

    protected fun addFrameToStoryFromMediaUri(mediaUri: Uri) {
        storyViewModel
            .addStoryFrameItemToCurrentStory(StoryFrameItem(
                UriBackgroundSource(contentUri = mediaUri),
                frameItemType = if (isVideo(mediaUri.toString())) VIDEO() else IMAGE
            ))
    }

    private fun updateBackgroundSurfaceUIWithStoryFrame(
        storyFrameIndex: Int
    ) {
        // omit keeping AddedViews for old selection by passing -1 given these were saved elsewhere
        onStoryFrameSelected(-1, storyFrameIndex)
    }

    private fun addClickListeners() {
        camera_capture_button
            .setOnTouchListener(
                PressAndHoldGestureHelper(
                    PressAndHoldGestureHelper.CLICK_LENGTH,
                    object : PressAndHoldGestureListener {
                        override fun onClickGesture() {
                            if (cameraOperationInCourse) {
                                showToast(getString(R.string.toast_capture_operation_in_progress))
                                return
                            }
                            timesUpHandler.removeCallbacksAndMessages(null)
                            takeStillPicture()
                        }
                        override fun onHoldingGestureStart() {
                            timesUpHandler.removeCallbacksAndMessages(null)
                            if (PermissionUtils.allVideoPermissionsGranted(this@ComposeLoopFrameActivity)) {
                                // if we at least have
                                startRecordingVideoAfterVibrationIndication()
                            } else {
                                // request permissions including audio for video
                                val permissionName = PermissionUtils.anyVideoNeededPermissionPermanentlyDenied(
                                        this@ComposeLoopFrameActivity
                                )

                                permissionName?.let {
                                    showPermissionPermanentlyDeniedDialog(it)
                                } ?: PermissionUtils.requestAllRequiredPermissionsIncludingAudioForVideo(
                                        this@ComposeLoopFrameActivity
                                ).also {
                                    permissionsRequestForCameraInProgress = true
                                }
                            }
                        }

                        override fun onHoldingGestureEnd() {
                            stopRecordingVideo(false)
                        }

                        override fun onHoldingGestureCanceled() {
                            stopRecordingVideo(true)
                        }

                        override fun onStartDetectionWait() {
                            if (cameraOperationInCourse) {
                                showToast(getString(R.string.toast_capture_operation_in_progress))
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
            when {
                backgroundSurfaceManager.cameraVisible() -> {
                    onBackPressed()
                }
                !backgroundSurfaceManager.cameraVisible() -> {
                    // Show discard dialog
                    var discardTitle = getString(R.string.dialog_discard_story_title)
                    var discardMessage = getString(R.string.dialog_discard_story_message)
                    var discardOkButton = getString(R.string.dialog_discard_story_ok_button)
                    if (intent.getBooleanExtra(KEY_STORY_EDIT_MODE, false)) {
                        discardTitle = getString(R.string.dialog_discard_story_title_edit)
                        discardMessage = getString(R.string.dialog_discard_story_message_edit)
                        discardOkButton = getString(R.string.dialog_discard_story_ok_button_edit)
                    }
                    FrameSaveErrorDialog.newInstance(
                        title = discardTitle,
                        message = discardMessage,
                        okButtonLabel = discardOkButton,
                        listener = object : FrameSaveErrorDialogOk {
                            override fun OnOkClicked(dialog: DialogFragment) {
                                addCurrentViewsToFrameAtIndex(storyViewModel.getSelectedFrameIndex())
                                dialog.dismiss()
                                // discard the whole story
                                safelyDiscardCurrentStoryAndCleanUpIntent()
                                storyDiscardListener?.onStoryDiscarded()
                            }
                        }).show(supportFragmentManager, FRAGMENT_DIALOG)
                }
            }
        }

        sound_button.setOnClickListener {
            // flip the flag on audio muted on the VM, the change will get propagated to the UI
            storyViewModel.flipCurrentSelectedFrameOnAudioMuted()
        }

        text_add_button.setOnClickListener {
            addNewText()
        }

        stickers_add_button.setOnClickListener {
            // avoid multiple clicks when the one click is already being processed, fixes
            // https://github.com/Automattic/stories-android/issues/455
            if (!emojiPickerFragment.isAdded && !emojiPickerFragment.isVisible) {
                emojiPickerFragment.show(supportFragmentManager, emojiPickerFragment.tag)
            }
        }

        next_button.setOnClickListener {
            prepublishingEventProvider?.onStorySaveButtonPressed()
        }

        retry_button.setOnClickListener {
            // trigger the Service again, for this frame only
            storyFrameIndexToRetry = storyViewModel.getSelectedFrameIndex()
            retry_button.setSaving(true)
            saveStory()
        }

        delete_slide_view.setOnClickListener {
            var messageToUse = getString(R.string.dialog_discard_page_message)
            if (storyViewModel.getSelectedFrame()?.saveResultReason != SaveSuccess) {
                messageToUse = getString(R.string.dialog_discard_errored_page_message)
            }
            // show dialog
            FrameSaveErrorDialog.newInstance(
                    title = getString(R.string.dialog_discard_page_title),
                    message = messageToUse,
                    okButtonLabel = getString(R.string.dialog_discard_page_ok_button),
                    listener = object : FrameSaveErrorDialogOk {
                        override fun OnOkClicked(dialog: DialogFragment) {
                            dialog.dismiss()
                            if (storyViewModel.getCurrentStorySize() == 1) {
                                // discard the whole story
                                safelyDiscardCurrentStoryAndCleanUpIntent()
                                storyDiscardListener?.onStoryDiscarded()
                            } else {
                                // get currentFrame value as it will change after calling onAboutToDeleteStoryFrame
                                val currentFrameToDeleteIndex = storyViewModel.getSelectedFrameIndex()
                                onAboutToDeleteStoryFrame(currentFrameToDeleteIndex)
                                storyDiscardListener?.onFrameRemove(storyViewModel.getCurrentStoryIndex(),
                                        currentFrameToDeleteIndex)
                                // now discard it from the viewModel
                                storyViewModel.removeFrameAt(currentFrameToDeleteIndex)
                            }
                        }
                    }).show(supportFragmentManager, FRAGMENT_DIALOG)
            disableDeleteSlideMode()
        }
    }

    fun processStorySaving() {
        addCurrentViewsToFrameAtIndex(storyViewModel.getSelectedFrameIndex())

        // if we were in an error-handling situation but now all pages are OK, we don't need to save them again
        if (anyOfOriginalIntentResultsIsError() && !storyViewModel.anyOfCurrentStoryFramesIsErrored()) {
            // everything is already saved by now
            // TODO kick the UploadService here! when in WPAndroid
            showToast("Awesome! Upload starting...")
            finish()
        } else {
            // fresh intent, go fully save the Story
            if (storyViewModel.getCurrentStorySize() > 0) {
                // save all composed frames
                if (PermissionUtils.checkAndRequestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    storyFrameIndexToRetry = StoryRepository.DEFAULT_NONE_SELECTED
                    saveStory()
                }
            }
        }
    }

    private fun anyOfOriginalIntentResultsIsError(): Boolean {
        if (intent.hasExtra(KEY_STORY_SAVE_RESULT)) {
            val storySaveResult =
                intent.getParcelableExtra(KEY_STORY_SAVE_RESULT) as StorySaveResult?
            storySaveResult?.let {
                // where there any errors when we opened the Activity to handle those errors?
                return storySaveResult.isSuccess()
            }
        }
        return false
    }

    private fun safelyDiscardCurrentStoryAndCleanUpIntent() {
        photoEditor.clearAllViews()
        storyViewModel.discardCurrentStory()
        cleanupOriginalIntentSaveResult()
        EventBus.getDefault().removeStickyEvent(StorySaveEvents.StorySaveProcessStart::class.java)
        // cancel any outstanding error notifications
        intent.action?.let {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.cancel(it.toInt())
        }
        storyIndexToSelect = StoryRepository.DEFAULT_NONE_SELECTED
        storyViewModel.loadStory(StoryRepository.DEFAULT_NONE_SELECTED)
    }

    private fun cleanupOriginalIntentSaveResult() {
        if (intent.hasExtra(KEY_STORY_SAVE_RESULT)) {
            intent.removeExtra(KEY_STORY_SAVE_RESULT)
        }
    }

    private fun saveStory() {
        saveStoryPreHook()
        // Bind to FrameSaveService
        FrameSaveService.startServiceAndGetSaveStoryIntent(this).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun saveStoryPreHook() {
        showLoading()
        refreshBackgroundViewInfoOnSelectedFrame()
        // disable layout change animations, we need this to make added views immediately visible, otherwise
        // we may end up capturing a Bitmap of a backing drawable that still has not been updated
        // (i.e. no visible added Views)
        transition = photoEditorView.getLayoutTransition()
        photoEditorView.layoutTransition = null
        preHookRun = true
    }

    private fun refreshBackgroundViewInfoOnSelectedFrame() {
        storyViewModel.getSelectedFrame()?.let {
            setBackgroundViewInfoOnFrame(
                    it,
                    photoEditor.composedCanvas.source as PhotoView
            )
        }
    }

    private fun saveStoryPostHook(result: StorySaveResult) {
        doUnbindService()
        // re-enable layout change animations
        photoEditorView.layoutTransition = transition

        // do this if we are retrying to save the current frame
        if (storyFrameIndexToRetry != StoryRepository.DEFAULT_NONE_SELECTED) {
            retry_button.showSavedAnimation(Runnable {
                if (result.isSuccess()) {
                    hideRetryButton()
                } else {
                    checkForLowSpaceAndShowDialog()
                }
                storyViewModel.updateCurrentSelectedFrameOnRetryResult(
                    result.frameSaveResult[0]
                )
                // need to do this so AddedViews get properly placed on the PhotoEditor
                refreshStoryFrameSelection()
            })
        }

        if (anyOfOriginalIntentResultsIsError() && !storyViewModel.anyOfCurrentStoryFramesIsErrored()) {
            // all solved? cancel any outstanding error notifications
            intent.action?.let {
                val notificationManager = NotificationManagerCompat.from(this)
                notificationManager.cancel(it.toInt())
            }
        }

        hideLoading()
    }

    private fun hideRetryButton() {
        retry_button.visibility = View.GONE
        // we need this force call given some timing issue when resetting the layout
        retry_button.invalidate()
    }

    private fun showRetryButton() {
        retry_button.setSaving(false)
        retry_button.visibility = View.VISIBLE
        // we need this force call given some timing issue when resetting the layout
        retry_button.invalidate()
    }

    private fun refreshStoryFrameSelection() {
        storyViewModel.setSelectedFrameByUser(storyViewModel.getSelectedFrameIndex())
    }

    private fun addCurrentViewsToFrameAtIndex(index: Int) {
        // first, remember the currently added views
        val currentStoryFrameItem = storyViewModel.getCurrentStoryFrameAt(index)

        // purge multitouch listeners
        val addedViews = photoEditor.getViewsAdded()
        for (addedView in addedViews) {
            addedView.view?.let {
                // while iterating, also update the ViewInfo for each view
                addedView.update()
                addedView.view?.setOnTouchListener(null)
            }
        }

        // set addedViews on the current frame (copy array so we don't share the same one with PhotoEditor)
        currentStoryFrameItem?.addedViews = AddedViewList().copyOf(photoEditor.getViewsAdded())
    }

    private fun showMediaPicker() {
        mediaPickerProvider?.showProvidedMediaPicker() ?: throw Exception("MediaPickerProvider not set")
    }

    private fun deleteCapturedMedia() {
        currentOriginalCapturedFile?.delete()

        // reset
        currentOriginalCapturedFile = null
    }

    private fun switchCameraPreviewOn() {
        cameraOperationInCourse = false
        hideStoryFrameSelector()
        backgroundSurfaceManager.switchCameraPreviewOn()
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
        photoEditor.addText("")
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

    private fun launchCameraPreviewWithSurfaceSafeguard() {
        // securely switch camera preview on
        if (backgroundSurfaceManager.isTextureViewAvailable()) {
            CoroutineScope(Dispatchers.Main).launch {
                launchCameraPreview()
            }
        } else {
            // prep the surface and wait for BackgroundSurfaceManagerReadyListener
            launchCameraRequestPending = true
            backgroundSurfaceManager.preTurnTextureViewOn()
        }
    }

    // IMPORTANT: don't call this method from any thread / CoroutineScope. Given we need the TextureSurface
    // to be ready, use launchCameraPreviewWithSurfaceSafeguard() instead.
    private fun launchCameraPreview() {
        hideStoryFrameSelector()
        hideEditModeUIControls()
        photoEditor.clearAllViews()

        if (!PermissionUtils.allRequiredPermissionsGranted(this)) {
            permissionsRequestForCameraInProgress = true
            PermissionUtils.requestAllRequiredPermissions(this)
            return
        }

        if (backgroundSurfaceManager.cameraVisible()) {
            return
        }

        // set the correct camera as selected by the user last time they used the app
        backgroundSurfaceManager.selectCamera(cameraSelection)
        // same goes for flash state
        backgroundSurfaceManager.setFlashState(flashModeSelection)
        switchCameraPreviewOn()
    }

    // IMPORTANT: don't call this method from any thread / CoroutineScope. Given we need the TextureSurface
    // to be ready, use showPlayVideoWithSurfaceSafeguard() instead.
    private fun showPlayVideo(videoFile: File? = null) {
        cameraOperationInCourse = false
        showStoryFrameSelector()
        showEditModeUIControls()
        backgroundSurfaceManager.switchVideoPlayerOnFromFile(videoFile)
    }

    // IMPORTANT: don't call this method from any thread / CoroutineScope. Given we need the TextureSurface
    // to be ready, use showPlayVideoWithSurfaceSafeguard() instead.
    private fun showPlayVideo(videoUri: Uri) {
        cameraOperationInCourse = false
        showStoryFrameSelector()
        showEditModeUIControls()
        backgroundSurfaceManager.switchVideoPlayerOnFromUri(videoUri)
    }

    private fun showStaticBackground() {
        cameraOperationInCourse = false
        showStoryFrameSelector()
        showEditModeUIControls()
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
                            StoryFrameItem(FileBackgroundSource(file = file), frameItemType = StoryFrameItemType.IMAGE)
                        )
                        setSelectedFrame(storyViewModel.getLastFrameIndexInCurrentStory())
                    }
                    showStaticBackground()
                    currentOriginalCapturedFile = file
                    waitToReenableCapture()
                }
            }
            override fun onError(message: String, cause: Throwable?) {
                runOnUiThread {
                    showToast(getString(R.string.toast_error_saving_image))
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
                            addStoryFrameItemToCurrentStory(StoryFrameItem(FileBackgroundSource(file = it),
                                frameItemType = VIDEO()))
                            setSelectedFrame(storyViewModel.getLastFrameIndexInCurrentStory())
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
                runOnUiThread {
                    showToast(getString(R.string.toast_error_saving_video) + ": $message")
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
        }
    }

    // artificial wait to re-enable capture mode
    private fun waitToReenableCapture() {
        timesUpHandler.postDelayed({
            cameraOperationInCourse = false
        }, CAMERA_STILL_PICTURE_WAIT_FOR_NEXT_CAPTURE_MS)
    }

    @SuppressLint("MissingPermission")
    private fun saveVideo(inputFile: Uri) {
        if (PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showLoading()
            try {
                val file = getLoopFrameFile(this, true)
                file.createNewFile()

                photoEditor.saveVideoAsFile(
                    inputFile,
                    file.absolutePath,
                    storyViewModel.isSelectedFrameAudioMuted(),
                    object : PhotoEditor.OnSaveWithCancelAndProgressListener {
                        override fun onProgress(progress: Double) {
                            // TODO implement progress
                        }

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
                                sendNewStoryFrameReadyBroadcast(file)
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
            showLoading()
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

    protected fun showLoading() {
        editModeHideAllUIControls(true)
        blockTouchOnPhotoEditor(BLOCK_TOUCH_MODE_FULL_SCREEN)
    }

    protected fun hideLoading() {
        editModeRestoreAllUIControls()
        releaseTouchOnPhotoEditor(BLOCK_TOUCH_MODE_FULL_SCREEN)
    }

    private fun showSnackbar(message: String, actionLabel: String? = null, listener: OnClickListener? = null) {
        snackbarProvider?.let {
            it.showProvidedSnackbar(message, actionLabel) {
                listener?.onClick(null)
            }
        } ?: Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showPermissionPermanentlyDeniedDialog(permission: String) {
        permissionDenialDialogProvider?.showPermissionPermanentlyDeniedDialog(permission)
                ?: showToast(getString(R.string.toast_capture_operation_permission_needed))
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

    private fun showEditModeUIControls() {
        // hide capturing mode controls
        hideVideoUIControls()
        camera_capture_button.visibility = View.INVISIBLE

        // show proper edit mode controls
        updateEditMode()
        next_button.visibility = View.VISIBLE
        close_button.visibility = View.VISIBLE
        delete_slide_view.visibility = View.GONE
    }

    private fun hideStoryFrameSelector() {
        (bottom_strip_view as StoryFrameSelectorFragment).hide()
        bottom_opaque_bar.visibility = View.INVISIBLE
    }

    private fun showStoryFrameSelector() {
        setOpaqueBarHeightAndStoryFrameSelectorBackgroundColor()
        bottom_opaque_bar.visibility = View.VISIBLE
        (bottom_strip_view as StoryFrameSelectorFragment).show()
    }

    private fun hideEditModeUIControls() {
        camera_capture_button.visibility = View.VISIBLE

        // hide proper edit mode controls
        edit_mode_controls.visibility = View.INVISIBLE
        sound_button.visibility = View.INVISIBLE
        next_button.visibility = View.INVISIBLE
        retry_button.visibility = View.GONE
        // show capturing mode controls
        showVideoUIControls()
    }

    private fun editModeHideAllUIControls(
        hideNextButton: Boolean,
        hideCloseButton: Boolean = false,
        hideFrameSelector: Boolean = true
    ) {
        // momentarily hide proper edit mode controls
        edit_mode_controls.visibility = View.INVISIBLE
        sound_button.visibility = View.INVISIBLE
        if (hideFrameSelector) {
            hideStoryFrameSelector()
        }
        if (hideNextButton) {
            next_button.visibility = View.INVISIBLE
        }
        if (hideCloseButton) {
            close_button.visibility = View.INVISIBLE
        }
    }

    private fun updateSoundControl() {
        if (storyViewModel.getSelectedFrame()?.frameItemType is VIDEO) {
            sound_button.visibility = View.VISIBLE
            if (!storyViewModel.isSelectedFrameAudioMuted()) {
                backgroundSurfaceManager.videoPlayerUnmute()
                sound_button.setImageResource(R.drawable.ic_volume_up_black_24dp)
            } else {
                backgroundSurfaceManager.videoPlayerMute()
                sound_button.setImageResource(R.drawable.ic_volume_off_black_24dp)
            }
        } else {
            // images don't have audio
            sound_button.visibility = View.INVISIBLE
        }
    }

    private fun updateEditMode() {
        val originallyErrored = anyOfOriginalIntentResultsIsError()
        val currentlyErrored = storyViewModel.anyOfCurrentStoryFramesIsErrored()

        when {
            // if we were in an error-handling situation but now all pages are OK we're ready to go
            // don't allow editing or adding new frames but do allow publishing the Story
            originallyErrored && !currentlyErrored -> {
                blockTouchOnPhotoEditor(BLOCK_TOUCH_MODE_PHOTO_EDITOR_READY)
                edit_mode_controls.visibility = View.INVISIBLE
                sound_button.visibility = View.INVISIBLE
                next_button.isEnabled = true
                (bottom_strip_view as StoryFrameSelectorFragment).hideAddFrameControl()
            }
            currentlyErrored -> {
                blockTouchOnPhotoEditor(BLOCK_TOUCH_MODE_PHOTO_EDITOR_ERROR_PENDING_RESOLUTION)
                edit_mode_controls.visibility = View.INVISIBLE
                sound_button.visibility = View.INVISIBLE
                next_button.isEnabled = false
                (bottom_strip_view as StoryFrameSelectorFragment).hideAddFrameControl()
            }
            else -> { // no errors here! this is the normal creation situation: release touch block, enable editing
                releaseTouchOnPhotoEditor(BLOCK_TOUCH_MODE_NONE)
                edit_mode_controls.visibility = View.VISIBLE
                updateSoundControl()
                next_button.isEnabled = true
                (bottom_strip_view as StoryFrameSelectorFragment).showAddFrameControl()
            }
        }
    }

    private fun editModeRestoreAllUIControls() {
        // show all edit mode controls
        updateEditMode()
        next_button.visibility = View.VISIBLE
        close_button.visibility = View.VISIBLE

        showStoryFrameSelector()
    }

    private fun disableEditControlsForErroredFrame() {
        showRetryButton()
        updateEditMode()
    }

    private fun enableEditControlsForNonErroredFrame() {
        hideRetryButton()
        updateEditMode()
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

    private fun sendNewStoryFrameReadyBroadcast(mediaFile: File) {
        // Implicit broadcasts will be ignored for devices running API
        // level >= 24, so if you only target 24+ you can remove this statement
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            if (mediaFile.extension == "jpg") {
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

    private fun sendNewStoryReadyBroadcast(rawMediaFileList: List<File?>) {
        // Implicit broadcasts will be ignored for devices running API
        // level >= 24, so if you only target 24+ you can remove this statement
        val mediaFileList = rawMediaFileList.filterNotNull()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            for (mediaFile in mediaFileList) {
                if (mediaFile.extension == "jpg") {
                    sendBroadcast(Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(mediaFile)))
                } else {
                    sendBroadcast(Intent(Camera.ACTION_NEW_VIDEO, Uri.fromFile(mediaFile)))
                }
            }
        }

        val arrayOfmimeTypes = arrayOfNulls<String>(mediaFileList.size)
        val arrayOfPaths = arrayOfNulls<String>(mediaFileList.size)
        for ((index, mediaFile) in mediaFileList.withIndex()) {
            arrayOfmimeTypes[index] = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(mediaFile.extension)
            arrayOfPaths[index] = mediaFile.absolutePath
        }

        // If the folder selected is an external media directory, this is unnecessary
        // but otherwise other apps will not be able to access our images unless we
        // scan them using [MediaScannerConnection]
        MediaScannerConnection.scanFile(
            this, arrayOfPaths, arrayOfmimeTypes, null)
    }

    private fun blockTouchOnPhotoEditor(touchBlockMode: ScreenTouchBlockMode, message: String? = null) {
        when (touchBlockMode) {
            BLOCK_TOUCH_MODE_FULL_SCREEN -> {
                translucent_view.visibility = View.VISIBLE
                translucent_error_view.visibility = View.INVISIBLE
                operation_text.text = message
                translucent_view.setOnTouchListener { _, _ ->
                    // no op
                    true
                }
            }
            BLOCK_TOUCH_MODE_PHOTO_EDITOR_ERROR_PENDING_RESOLUTION -> {
                translucent_view.visibility = View.GONE
                translucent_error_view.visibility = View.VISIBLE
                translucent_error_view.background = ColorDrawable(
                    ContextCompat.getColor(this, R.color.black_transp_error_scrim)
                )
                translucent_error_view.setOnTouchListener { _, _ ->
                    // no op
                    true
                }
            }
            // do block touch but don't show scrim (make it transparent)
            BLOCK_TOUCH_MODE_PHOTO_EDITOR_READY -> {
                translucent_view.visibility = View.GONE
                translucent_error_view.visibility = View.VISIBLE
                translucent_error_view.background = ColorDrawable(
                    ContextCompat.getColor(this, android.R.color.transparent)
                )
                translucent_error_view.setOnTouchListener { _, _ ->
                    // no op
                    true
                }
            }
            // block touch, no scrim, tapping releases block
            BLOCK_TOUCH_MODE_DELETE_SLIDE -> {
                translucent_view.visibility = View.GONE
                translucent_error_view.visibility = View.VISIBLE
                translucent_error_view.background = ColorDrawable(
                        ContextCompat.getColor(this, android.R.color.transparent)
                )
                translucent_error_view.setOnTouchListener { _, _ ->
                    // If the error view is tapped, dismiss it and cancel delete slide mode
                    // Don't consume the touch event, pass it along
                    disableDeleteSlideMode()
                    false
                }
            }
            // just don't block touch
            BLOCK_TOUCH_MODE_NONE -> {
                translucent_view.visibility = View.GONE
                translucent_error_view.visibility = View.GONE
            }
        }
    }

    private fun releaseTouchOnPhotoEditor(touchBlockMode: ScreenTouchBlockMode) {
        when (touchBlockMode) {
            BLOCK_TOUCH_MODE_FULL_SCREEN -> {
                translucent_view.visibility = View.GONE
                operation_text.text = null
                translucent_view.setOnTouchListener(null)
            }
            BLOCK_TOUCH_MODE_PHOTO_EDITOR_ERROR_PENDING_RESOLUTION,
            BLOCK_TOUCH_MODE_PHOTO_EDITOR_READY,
            BLOCK_TOUCH_MODE_NONE -> {
                translucent_error_view.visibility = View.GONE
                translucent_error_view.setOnTouchListener(null)
            }
        }
    }

    private fun calculateScreenSize() {
        val size = getDisplayPixelSize(this)
        screenSizeX = size.x
        screenSizeY = size.y
    }

    // switch frames before deletion
    fun onAboutToDeleteStoryFrame(indexToDelete: Int) {
        // first let's make sure we update the added views on photoEditor and switch to the next
        // available frame - then update the StoryViewModel
        var nextIdxToSelect = indexToDelete
        // adjust index
        if (nextIdxToSelect > 0) {
            // if there are items to the left, then prefer to select the next item to the left
            nextIdxToSelect--
        } else {
            // if there are no items to the left and there are items to the right, then choose
            // an item to the right
            if (nextIdxToSelect < storyViewModel.getLastFrameIndexInCurrentStory()) {
                nextIdxToSelect++
            }
        }

        onStoryFrameSelected(indexToDelete, nextIdxToSelect)
    }

    override fun onStoryFrameSelected(oldIndex: Int, newIndex: Int) {
        if (oldIndex >= 0) {
            // only remember added views for frame if current index is valid
            addCurrentViewsToFrameAtIndex(oldIndex)

            // save current imageMatrix as the background image may have been resized
            val oldSelectedFrame = storyViewModel.getCurrentStoryFrameAt(oldIndex)
            if (oldSelectedFrame?.frameItemType is IMAGE) {
                setBackgroundViewInfoOnFrame(
                        oldSelectedFrame,
                        photoEditor.composedCanvas.source as PhotoView
                )
            } // TODO add else clause and handle VIDEO frameItemType
        }

        // This is tricky. See https://stackoverflow.com/questions/45860434/cant-remove-view-from-root-view
        // we need to disable layout transition animations so changes in views' parent are set
        // immediately. Otherwise a view's parent will only change once the animation ends, and hence
        // we could reach an IllegalStateException where we are trying to add a view to another parent while it still
        // belongs to a definite parent.
        val transition = photoEditor.composedCanvas.getLayoutTransition()
        photoEditor.composedCanvas.layoutTransition = null

        // now clear addedViews so we don't leak View.Context
        photoEditor.clearAllViews()

        // now set the current capturedFile to be the one pointed to by the index frame
        val newSelectedFrame = storyViewModel.setSelectedFrame(newIndex)
        val source = newSelectedFrame.source
        if (source is FileBackgroundSource) {
            currentOriginalCapturedFile = source.file
        }

        // decide which background surface to activate here, possibilities are:
        // 1. video/uri source
        // 2. video/file source
        // 3. image/uri source
        // 4. image/file source
        if (newSelectedFrame.frameItemType is VIDEO) {
            showPlayVideoWithSurfaceSafeguard(source)
        } else {
            val model = (source as? FileBackgroundSource)?.file ?: (source as UriBackgroundSource).contentUri
            Glide.with(this@ComposeLoopFrameActivity)
                    .load(model)
                    .transform(FitCenter())
                    .listener(provideGlideRequestListenerWithHandler {
                        setBackgroundViewInfoOnPhotoView(
                                newSelectedFrame,
                                photoEditor.composedCanvas.source as PhotoView
                        )
                    })
                    .into(photoEditorView.source)

            showStaticBackground()
        }

        // make sure to release the added views before re-selecting them
        FrameSaveManager.releaseAddedViews(newSelectedFrame)
        // now call addViewToParent the addedViews remembered by this frame
        newSelectedFrame.addedViews.let {
            for (oneView in it) {
                photoEditor.addViewToParentWithTouchListener(oneView)
            }
        }

        // re-enable layout change animations
        photoEditor.composedCanvas.layoutTransition = transition

        showRetryButtonAndHideEditControlsForErroredFrame(newSelectedFrame.saveResultReason !is SaveSuccess)
    }

    private fun setBackgroundViewInfoOnFrame(frame: StoryFrameItem, backgroundImageSource: PhotoView) {
        val matrixValues = FloatArray(9)
        val matrix = Matrix()
        // fill in matrix with PhotoView Support matrix
        backgroundImageSource.getSuppMatrix(matrix)
        // extract matrix to float array matrixValues
        matrix.getValues(matrixValues)
        frame.source.backgroundViewInfo = BackgroundViewInfo(
                imageMatrixValues = matrixValues
        )
    }

    private fun setBackgroundViewInfoOnPhotoView(frame: StoryFrameItem, backgroundImageSource: PhotoView) {
        val backgroundViewInfo = frame.source.backgroundViewInfo
        // load image matrix from data if it exists
        backgroundViewInfo?.let {
            val matrix = Matrix()
            matrix.setValues(it.imageMatrixValues)
            backgroundImageSource.apply {
                setSuppMatrix(matrix)
            }
        }
    }

    private fun provideGlideRequestListenerWithHandler(setupPhotoViewMatrix: Runnable): RequestListener<Drawable> {
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                // let the default implementation run
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                // here setup the PhotoView support matrix
                // we use a handler because we need to set the support matrix only once the drawable
                // has been set on the PhotoView, otherwise the matrix is not applied
                // see
                // https://github.com/Baseflow/PhotoView/blob/139a9ffeaf70bd628b015374cb6530fcf7d0bcb7/photoview/src/main/java/com/github/chrisbanes/photoview/PhotoViewAttacher.java#L279-L289
                Handler().post(setupPhotoViewMatrix)
                // return false to let Glide proceed and set the drawable
                return false
            }
        }
    }

    override fun onStoryFrameAddTapped() {
        addCurrentViewsToFrameAtIndex(storyViewModel.getSelectedFrameIndex())
        refreshBackgroundViewInfoOnSelectedFrame()
        showMediaPicker()
    }

    override fun onCurrentFrameTapped() {
        toggleDeleteSlideMode()
    }

    override fun onStoryFrameLongPressed(oldIndex: Int, newIndex: Int) {
        // On long press we want to switch to that frame and show the delete slide move all together.
        if (oldIndex != newIndex) {
            // The long-pressed frame was not the one already in focus - switch to it first.
            onStoryFrameSelected(oldIndex, newIndex)
        }
        toggleDeleteSlideMode()
    }

    override fun onStoryFrameMovedLongPressed() {
        disableDeleteSlideMode()
    }

    private fun toggleDeleteSlideMode() {
        if (delete_slide_view.visibility == View.VISIBLE) {
            disableDeleteSlideMode()
        } else {
            enableDeleteSlideMode()
        }
    }

    private fun enableDeleteSlideMode() {
        delete_slide_view.visibility = View.VISIBLE
        editModeHideAllUIControls(hideNextButton = true, hideFrameSelector = false)
        blockTouchOnPhotoEditor(BLOCK_TOUCH_MODE_DELETE_SLIDE)
    }

    private fun disableDeleteSlideMode() {
        delete_slide_view.visibility = View.GONE
        editModeRestoreAllUIControls()
        releaseTouchOnPhotoEditor(BLOCK_TOUCH_MODE_NONE)
    }

    private fun showPlayVideoWithSurfaceSafeguard(source: BackgroundSource) {
        if (backgroundSurfaceManager.isTextureViewAvailable()) {
            CoroutineScope(Dispatchers.Main).launch {
                source.apply {
                    if (this is FileBackgroundSource) {
                        showPlayVideo(file)
                    } else (source as UriBackgroundSource).contentUri?.let {
                        showPlayVideo(it)
                    }
                }
                updateSoundControl()
            }
        } else {
            // prep the surface and wait for BackgroundSurfaceManagerReadyListener
            launchVideoPlayerRequestPendingSource = source
            launchVideoPlayerRequestPending = true
            backgroundSurfaceManager.preTurnTextureViewOn()
        }
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

    private fun doUnbindService() {
        if (saveServiceBound) {
            unbindService(connection)
            saveServiceBound = false
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStorySaveResult(event: StorySaveResult) {
        EventBus.getDefault().removeStickyEvent(event)
        // only run saveStoryPostHook if preHook has been run for this Activity's instance lifespan.
        if (preHookRun) {
            saveStoryPostHook(event)
        }
    }

    fun setSnackbarProvider(provider: SnackbarProvider) {
        snackbarProvider = provider
    }

    fun setMediaPickerProvider(provider: MediaPickerProvider) {
        mediaPickerProvider = provider
        mediaPickerProvider?.setupRequestCodes(requestCodes)
    }

    fun setNotificationExtrasLoader(loader: NotificationIntentLoader) {
        notificationIntentLoader = loader
    }

    fun setAuthenticationProvider(provider: AuthenticationHeadersProvider) {
        authHeadersProvider = provider
    }

    fun setMetadataProvider(provider: MetadataProvider) {
        metadataProvider = provider
    }

    fun setStoryDiscardListener(listener: StoryDiscardListener) {
        storyDiscardListener = listener
    }

    fun setStoriesAnalyticsListener(listener: StoriesAnalyticsListener) {
        analyticsListener = listener
    }

    fun setNotificationTrackerProvider(provider: NotificationTrackerProvider) {
        notificationTrackerProvider = provider
    }

    fun setPrepublishingEventProvider(provider: PrepublishingEventProvider) {
        prepublishingEventProvider = provider
    }

    fun setPermissionDialogProvider(provider: PermanentPermissionDenialDialogProvider) {
        permissionDenialDialogProvider = provider
    }

    // true: default, files are created in the internal app directory and these will be cleaned on exit
    // false: we won't do cleanup and the files will be saved on the app's output directory
    fun setUseTempCaptureFile(useTempFiles: Boolean) {
        this.useTempCaptureFile = useTempFiles
        this.storyViewModel.useTempCaptureFile = useTempFiles
        this.backgroundSurfaceManager.useTempCaptureFile = useTempFiles
    }

    fun setGenericAnnouncementDialogProvider(provider: GenericAnnouncementDialogProvider) {
        genericAnnouncementDialogProvider = provider
    }

    class ExternalMediaPickerRequestCodesAndExtraKeys {
        var PHOTO_PICKER: Int = 0 // default code, can be overriden.
        // Leave this value in zero so it's evident if something is not working (will break
        // if not properly initialized)
        lateinit var EXTRA_MEDIA_URIS: String
        lateinit var EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED: String
    }

    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
        private val requestCodes = ExternalMediaPickerRequestCodesAndExtraKeys()

        private const val SURFACE_MANAGER_READY_LAUNCH_DELAY = 500L
        private const val CAMERA_VIDEO_RECORD_MAX_LENGTH_MS = 10000L
        private const val CAMERA_STILL_PICTURE_ANIM_MS = 300L
        private const val CAMERA_STILL_PICTURE_WAIT_FOR_NEXT_CAPTURE_MS = 1000L
        private const val STATE_KEY_CURRENT_ORIGINAL_CAPTURED_FILE = "key_current_original_captured_file"
        private const val STATE_KEY_PREHOOK_RUN = "key_prehook_run"
        private const val STATE_KEY_STORY_SAVE_STATE = "key_story_save_state"
        private const val STATE_KEY_STORY_SAVE_STATE_SELECTED_FRAME = "key_story_save_state_selected_frame"
        private const val STATE_KEY_FIRST_INTENT_LOADED = "key_state_first_intent_loaded"
        private const val STATE_KEY_PERMISSION_REQ_IN_PROGRESS = "key_state_permission_req_in_progress"
        private const val VIBRATION_INDICATION_LENGTH_MS = 100L
        private const val SWIPE_MIN_DISTANCE = 120
        private const val SWIPE_MIN_DISTANCE_FROM_BOTTOM = 80
        private const val SWIPE_THRESHOLD_VELOCITY = 200
    }
}
