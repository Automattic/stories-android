package com.wordpress.stories.compose

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION

const val IMMERSIVE_FLAG_TIMEOUT = 500L
/** Combination of all flags required to put activity into immersive mode */
@Suppress("DEPRECATION")
const val FLAGS_FULLSCREEN =
    View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

@Suppress("DEPRECATION") fun hideSystemUI(window: Window) {
    window.decorView.systemUiVisibility = FLAGS_FULLSCREEN
}

@Suppress("DEPRECATION") fun hideStatusBar(window: Window) {
    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    window.setFlags(
        FLAG_TRANSLUCENT_NAVIGATION,
        FLAG_TRANSLUCENT_NAVIGATION
    )
}

// Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
@Suppress("DEPRECATION") fun showSystemUI(window: Window) {
    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
}

fun addInsetTopMargin(layoutParams: LayoutParams, baseTopMargin: Int, insetTopMargin: Int) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        layoutParams.topMargin = baseTopMargin + insetTopMargin
    }
}

fun getLayoutTopMarginBeforeInset(layoutParams: LayoutParams): Int {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        return layoutParams.topMargin
    }
    return 0
}
