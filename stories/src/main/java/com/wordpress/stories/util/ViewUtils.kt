package com.wordpress.stories.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Size
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children

const val TARGET_RATIO_9_16 = 0.5625f // 9:16

/**
 * Immutable class for describing width and height dimensions in pixels.
 * Much like [android.util.Size] but available to unit tests.
 */
data class ScreenSize(val height: Int, val width: Int) {
    fun toSize() = Size(height, width)
}

fun removeViewFromParent(view: View) {
    view.parent?.let {
        it as ViewGroup
        if (it.children.contains(view)) {
            it.removeView(view)
        }
    }
}

fun cloneViewSpecs(originalView: View, targetView: View) {
    val originalWidth = originalView.width
    val originalHeight = originalView.height

    val measuredWidth = View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY)
    val measuredHeight = View.MeasureSpec.makeMeasureSpec(originalHeight, View.MeasureSpec.EXACTLY)

    targetView.measure(measuredWidth, measuredHeight)
    targetView.layout(0, 0, targetView.measuredWidth, targetView.measuredHeight)
}

fun isSizeRatio916(originalWidth: Int, originalHeight: Int): Boolean {
    return (originalWidth.toFloat() / originalHeight.toFloat()) == TARGET_RATIO_9_16
}

fun isScreenTallerThan916(originalWidth: Int, originalHeight: Int): Boolean {
    return (originalWidth.toFloat() / originalHeight.toFloat()) < TARGET_RATIO_9_16
}

fun normalizeSizeExportTo916(originalWidth: Int, originalHeight: Int): ScreenSize {
    /*
        1. if the screen is 16:9, we're OK
        2. if not, resolve what the height should be for the screen's given width.
        3. if the result of (2) is less than actual height, set the new target
            height for the cloned view to the number calculated as of (2) (cropping)
        4. if the result of (2) is greater than actual height --> crop on width to make it fit 9:16
     */
    if (isSizeRatio916(originalWidth, originalHeight)) {
        // 1. if the screen is already 16:9, we're OK, return the original width and height
        return ScreenSize(originalWidth, originalHeight)
    } else {
        // 2. if not, resolve what the height should be for the screen's given width.
        val normalizedHeightShouldBe = originalWidth / TARGET_RATIO_9_16
        return if (normalizedHeightShouldBe <= originalHeight) {
            // 3. if the result of (2) is less than actual height, set the new target height for the cloned
            // view to the number calculated as of (2) (cropping on height)
            ScreenSize(originalWidth, normalizedHeightShouldBe.toInt())
        } else {
            // 4. if the result of (2) is greater than actual height --> crop the width to match the ratio needed
            // for the given original height (cropping on width)
            val normalizedWidthShouldBe = originalHeight * TARGET_RATIO_9_16
            ScreenSize(normalizedWidthShouldBe.toInt(), originalHeight)
        }
    }
}

fun calculateAspectRatioForDrawable(drawable: Drawable): Float {
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight
    return width.toFloat() / height.toFloat()
}

fun calculateAspectRatioForBitmap(bitmap: Bitmap): Float {
    val width = bitmap.width
    val height = bitmap.height
    return width.toFloat() / height.toFloat()
}

fun isAspectRatioSimilarByPercentage(aspectRatio1: Float, aspectRatio2: Float, percentage: Float): Boolean {
    return (Math.abs(aspectRatio1 - aspectRatio2) < percentage)
}

fun isWidthMultiple(width1: Int, width2: Int): Boolean {
    return isMultipleOf(width1, width2) || isMultipleOf(width2, width1)
}

private fun isMultipleOf(num1: Int, num2: Int): Boolean {
    return (num1 % num2 == 0)
}

fun getSizeRatio(originalWidth: Int, originalHeight: Int): Float {
    return (originalWidth.toFloat() / originalHeight.toFloat())
}