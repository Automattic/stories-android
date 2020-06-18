package com.automattic.portkey

import android.app.ActivityOptions
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
import com.wordpress.stories.compose.SnackbarProvider

fun Snackbar.config(context: Context) {
    this.view.background = context.getDrawable(R.drawable.snackbar_background)
    val params = this.view.layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(12, 12, 12, 12)
    this.view.layoutParams = params
    ViewCompat.setElevation(this.view, 6f)
}

class StoryComposerActivity : ComposeLoopFrameActivity(), SnackbarProvider, MediaPickerProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSnackbarProvider(this)
        setMediaPickerProvider(this)
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
}
