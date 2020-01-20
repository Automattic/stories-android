package com.automattic.portkey.util

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children

fun removeViewFromParent(view: View) {
    view.parent?.let {
        it as ViewGroup
        if (it.children.contains(view)) {
            it.removeView(view)
        }
    }
}
