package com.automattic.portkey.compose

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.Window

const val IMMERSIVE_FLAG_TIMEOUT = 500L
/** Combination of all flags required to put activity into immersive mode */
const val FLAGS_FULLSCREEN =
    View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

fun hideSystemUI(window: Window) {
    window.decorView.systemUiVisibility = FLAGS_FULLSCREEN
}

fun hideStatusBar(window: Window) {
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
}

// Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
fun showSystemUI(window: Window) {
    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
}

fun addInsetTopMargin(layoutParams: LayoutParams, insetTopMargin: Int) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        layoutParams.topMargin = layoutParams.topMargin + insetTopMargin
    }
}
