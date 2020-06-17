package com.automattic.portkey

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import com.wordpress.stories.compose.ComposeLoopFrameActivity
import com.wordpress.stories.compose.SnackbarProvider

fun Snackbar.config(context: Context) {
    this.view.background = context.getDrawable(R.drawable.snackbar_background)
    val params = this.view.layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(12, 12, 12, 12)
    this.view.layoutParams = params
    ViewCompat.setElevation(this.view, 6f)
}

class StoryComposerActivity : ComposeLoopFrameActivity(), SnackbarProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSnackbarProvider(this)
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
}
