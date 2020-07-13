package com.automattic.portkey

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import com.automattic.portkey.photopicker.MediaBrowserType
import com.automattic.portkey.photopicker.PhotoPickerActivity
import com.automattic.portkey.photopicker.PhotoPickerFragment
import com.automattic.portkey.photopicker.RequestCodes
import com.google.android.material.snackbar.Snackbar
import com.wordpress.stories.compose.ComposeLoopFrameActivity
import com.wordpress.stories.compose.MediaPickerProvider
import com.wordpress.stories.compose.MetadataProvider
import com.wordpress.stories.compose.NotificationIntentLoader
import com.wordpress.stories.compose.SnackbarProvider
import com.wordpress.stories.compose.StoryDiscardListener
import com.wordpress.stories.compose.story.StoryIndex

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
    StoryDiscardListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSnackbarProvider(this)
        setMediaPickerProvider(this)
        setNotificationExtrasLoader(this)
        setMetadataProvider(this)
        setStoryDiscardListener(this) // optionally listen to discard events
        setNotificationTrackerProvider(application as Portkey) // optionally set Notification Tracker.
        // The notifiationTracker needs to be something that outlives the Activity, given the Service could be running
        // after the user has exited ComposeLoopFrameActivity
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
        requestCodes.EXTRA_MEDIA_URIS = PhotoPickerActivity.EXTRA_MEDIA_URIS
    }

    override fun showProvidedMediaPicker() {
        val intent = Intent(this, PhotoPickerActivity::class.java)
        intent.putExtra(PhotoPickerFragment.ARG_BROWSER_TYPE, MediaBrowserType.PORTKEY_PICKER)

        startActivityForResult(
            intent,
            RequestCodes.PHOTO_PICKER,
            ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun providerHandlesOnActivityResult(): Boolean {
        return false
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

    companion object {
        const val KEY_EXAMPLE_METADATA = "key_example_metadata"
        const val KEY_STORY_INDEX = "key_story_index"
        const val BASE_FRAME_MEDIA_ERROR_NOTIFICATION_ID: Int = 72300
    }

    override fun onStoryDiscarded() {
        // example: do any cleanup you may need here
        Toast.makeText(this, "Story has been discarded!", Toast.LENGTH_SHORT).show()
    }
}
