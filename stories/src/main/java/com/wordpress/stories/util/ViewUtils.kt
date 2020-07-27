package com.wordpress.stories.util

import android.util.Size
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

fun cloneViewSpecs(originalView: View, targetView: View, doNormalizeTo9_16: Boolean) {
    var widthToUse = originalView.width
    var heightToUse = originalView.height

    if (doNormalizeTo9_16) {
        val adjustedSize = normalizeHeightExportTo916(originalView.width, originalView.height)
        heightToUse = adjustedSize.height
        widthToUse = adjustedSize.width
    }

    val measuredWidth = View.MeasureSpec.makeMeasureSpec(widthToUse, View.MeasureSpec.EXACTLY)
    val measuredHeight = View.MeasureSpec.makeMeasureSpec(heightToUse, View.MeasureSpec.EXACTLY)

    targetView.measure(measuredWidth, measuredHeight)
    targetView.layout(0, 0, targetView.measuredWidth, targetView.measuredHeight)
}

fun normalizeHeightExportTo916(originalWidth: Int, originalHeight: Int): Size {
    /*
        1. if the screen is 16:9, we're OK
        2. if not, resolve what the height should be for the screen's given width.
        3. if the result of (2) is less than actual height, set the new target
            height for the cloned view to the number calculated as of (2) (cropping)
        4. (very unlikely) if the result of (2) is greater than actual height --> crop on width to make it fit 9:16
     */
    val targetRatio = 0.5625f // 9:16

    if ((originalWidth.toFloat() / originalHeight.toFloat()) == targetRatio) {
        // 1. if the screen is 16:9, we're OK
        return Size(originalWidth, originalHeight)
    } else {
        // 2. if not, resolve what the height should be for the screen's given width.
        val normalizedHeightShouldBe = originalWidth / targetRatio
        if (normalizedHeightShouldBe <= originalHeight) {
            // 3. if the result of (2) is less than actual height, set the new target height for the cloned
            // view to the number calculated as of (2) (cropping)
            return Size(originalWidth, normalizedHeightShouldBe.toInt())
        } else {
            // (less likely to happen in practice)
            // 4. if the result of (2) is greater than actual height --> crop the width to match the ratio needed
            // for the given height
            val normalizedWidthShouldBe = normalizedHeightShouldBe * targetRatio
            return Size(normalizedWidthShouldBe.toInt(), normalizedHeightShouldBe.toInt())
        }
    }
}
