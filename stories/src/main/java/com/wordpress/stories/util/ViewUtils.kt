package com.wordpress.stories.util

import android.util.Size
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children

const val TARGET_RATIO_9_16 = 0.5625f // 9:16

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

fun isSizeRatio916(originalWidth: Int, originalHeight: Int): Boolean {
    return (originalWidth.toFloat() / originalHeight.toFloat()) == TARGET_RATIO_9_16
}

fun normalizeSizeExportTo916(originalWidth: Int, originalHeight: Int): Size {
    /*
        1. if the screen is 16:9, we're OK
        2. if not, resolve what the height should be for the screen's given width.
        3. if the result of (2) is less than actual height, set the new target
            height for the cloned view to the number calculated as of (2) (cropping)
        4. if the result of (2) is greater than actual height --> crop on width to make it fit 9:16
     */
    if (isSizeRatio916(originalWidth, originalHeight)) {
        // 1. if the screen is already 16:9, we're OK, return the original width and height
        return Size(originalWidth, originalHeight)
    } else {
        // 2. if not, resolve what the height should be for the screen's given width.
        val normalizedHeightShouldBe = originalWidth / TARGET_RATIO_9_16
        if (normalizedHeightShouldBe <= originalHeight) {
            // 3. if the result of (2) is less than actual height, set the new target height for the cloned
            // view to the number calculated as of (2) (cropping on height)
            return Size(originalWidth, normalizedHeightShouldBe.toInt())
        } else {
            // 4. if the result of (2) is greater than actual height --> crop the width to match the ratio needed
            // for the given original height (cropping on width)
            val normalizedWidthShouldBe = originalHeight * TARGET_RATIO_9_16
            return Size(normalizedWidthShouldBe.toInt(), originalHeight)
        }
    }
}
