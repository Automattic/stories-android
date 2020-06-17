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

fun cloneViewSpecs(originalView: View, targetView: View) {
    val originalWidth = originalView.getWidth()
    val originalHeight = originalView.getHeight()

    val measuredWidth = View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY)
    val measuredHeight = View.MeasureSpec.makeMeasureSpec(originalHeight, View.MeasureSpec.EXACTLY)

    targetView.measure(measuredWidth, measuredHeight)
    targetView.layout(0, 0, targetView.getMeasuredWidth(), targetView.getMeasuredHeight())
}
