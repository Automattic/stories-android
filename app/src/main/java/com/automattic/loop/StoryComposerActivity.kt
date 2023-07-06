package com.automattic.loop

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import com.automattic.loop.photopicker.MediaBrowserType
import com.automattic.loop.photopicker.PhotoPickerActivity
import com.automattic.loop.photopicker.PhotoPickerFragment
import com.automattic.loop.photopicker.RequestCodes
import com.automattic.loop.util.ANALYTICS_TAG
import com.automattic.loop.util.CopyExternalUrisLocallyUseCase
import com.google.android.material.snackbar.Snackbar
import com.wordpress.stories.compose.ComposeLoopFrameActivity
import com.wordpress.stories.compose.FrameSaveErrorDialog
import com.wordpress.stories.compose.GenericAnnouncementDialogProvider
import com.wordpress.stories.compose.MediaPickerProvider
import com.wordpress.stories.compose.MetadataProvider
import com.wordpress.stories.compose.NotificationIntentLoader
import com.wordpress.stories.compose.PrepublishingEventProvider
import com.wordpress.stories.compose.SnackbarProvider
import com.wordpress.stories.compose.StoriesAnalyticsListener
import com.wordpress.stories.compose.StoryDiscardListener
import com.wordpress.stories.compose.story.StoryIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun Snackbar.config(context: Context) {
    this.view.background = context.getDrawable(R.drawable.snackbar_background)
    val params = this.view.layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(12, 12, 12, 12)
    this.view.layoutParams = params
    ViewCompat.setElevation(this.view, 6f)
}

class StoryComposerActivity : ComposeLoopFrameActivity(),
    SnackbarProvider,
    MediaPickerProvider,
    NotificationIntentLoader,
    MetadataProvider,
    StoryDiscardListener,
    StoriesAnalyticsListener,
    PrepublishingEventProvider,
    GenericAnnouncementDialogProvider,
    CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        setSnackbarProvider(this)
        setMediaPickerProvider(this)
        super.onCreate(savedInstanceState)
        setNotificationExtrasLoader(this)
        setMetadataProvider(this)
        setStoryDiscardListener(this) // optionally listen to discard events
        setStoriesAnalyticsListener(this)
        setPrepublishingEventProvider(this)
        setNotificationTrackerProvider(application as Loop) // optionally set Notification Tracker.
        setGenericAnnouncementDialogProvider(this)
        // The notifiationTracker needs to be something that outlives the Activity, given the Service could be running
        // after the user has exited ComposeLoopFrameActivity
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.hasExtra(PhotoPickerActivity.EXTRA_MEDIA_URIS)) {
                data.getStringArrayExtra(PhotoPickerActivity.EXTRA_MEDIA_URIS)?.let {
                    val uriList: List<Uri> = convertStringArrayIntoUrisList(it)
                    // if there are any external Uris in this list, copy them locally before trying to use them
                    processExternalUris(uriList)
                }
            }
        }
    }

    private fun processExternalUris(uriList: List<Uri>) {
        launch {
            val copyUrisLocallyUseCase = CopyExternalUrisLocallyUseCase()
            // Copy files to apps storage to make sure they are permanently accessible.
            val copyFilesResult =
                    copyUrisLocallyUseCase.copyFilesToAppStorageIfNecessary(this@StoryComposerActivity, uriList)
            addFramesToStoryFromMediaUriList(copyFilesResult.permanentlyAccessibleUris)
            setDefaultSelectionAndUpdateBackgroundSurfaceUI(copyFilesResult.permanentlyAccessibleUris)
        }
    }

    override fun showProvidedSnackbar(message: String, actionLabel: String?, callback: () -> Unit) {
        runOnUiThread {
            val view = findViewById<View>(android.R.id.content)
            if (view != null) {
                val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                snackbar.config(this)
                actionLabel?.let {
                    snackbar.setAction(it) {
                        callback()
                    }
                }
                snackbar.show()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun setupRequestCodes(requestCodes: ExternalMediaPickerRequestCodesAndExtraKeys) {
        requestCodes.PHOTO_PICKER = RequestCodes.PHOTO_PICKER
        requestCodes.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED =
            PhotoPickerActivity.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED
        requestCodes.EXTRA_LAUNCH_WPSTORIES_MEDIA_PICKER_REQUESTED =
                PhotoPickerActivity.EXTRA_LAUNCH_WPSTORIES_MEDIA_PICKER_REQUESTED
        requestCodes.EXTRA_MEDIA_URIS = PhotoPickerActivity.EXTRA_MEDIA_URIS
    }

    override fun showProvidedMediaPicker() {
        val intent = Intent(this, PhotoPickerActivity::class.java)
        intent.putExtra(PhotoPickerFragment.ARG_BROWSER_TYPE, MediaBrowserType.LOOP_PICKER)

        @Suppress("DEPRECATION")
        startActivityForResult(
            intent,
            RequestCodes.PHOTO_PICKER,
            ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun providerHandlesOnActivityResult(): Boolean {
        return true
    }

    override fun loadIntentForErrorNotification(): Intent {
        val notificationIntent = Intent(applicationContext, StoryComposerActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return notificationIntent
    }

    override fun loadPendingIntentForErrorNotificationDeletion(notificationId: Int): PendingIntent? {
        // demo app doesn't need to provide an implementation
        return null
    }

    override fun setupErrorNotificationBaseId(): Int {
        return BASE_FRAME_MEDIA_ERROR_NOTIFICATION_ID
    }

    override fun loadMetadataForStory(index: StoryIndex): Bundle? {
        // this is optional, external metadata that will be returned to you after the FrameSaveService finishes
        val bundle = Bundle()
        bundle.putString(KEY_EXAMPLE_METADATA, "example metadata")
        bundle.putInt(KEY_STORY_INDEX, index)
        return bundle
    }

    override fun onStoryDiscarded() {
        // example: do any cleanup you may need here
        Toast.makeText(this, "Story has been discarded!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onFrameRemove(storyIndex: StoryIndex, storyFrameIndex: Int) {
        Toast.makeText(this, "Story frame has been discarded!: index: " + storyFrameIndex,
                Toast.LENGTH_SHORT).show()
    }

    override fun onStorySaveButtonPressed() {
        processStorySaving()
    }

    override fun showGenericAnnouncementDialog() {
        FrameSaveErrorDialog.newInstance(
                title = getString(R.string.dialog_general_announcement_title),
                message = getString(R.string.dialog_general_announcement_title)
        ).show(supportFragmentManager, FRAGMENT_DIALOG)
    }

    override fun trackStoryTextChanged(properties: Map<String, *>) {
        Log.i(ANALYTICS_TAG, "story_text_changed: $properties")
    }

    companion object {
        protected const val FRAGMENT_DIALOG = "dialog"
        const val KEY_EXAMPLE_METADATA = "key_example_metadata"
        const val KEY_STORY_INDEX = "key_story_index"
        const val BASE_FRAME_MEDIA_ERROR_NOTIFICATION_ID: Int = 72300
    }
}
