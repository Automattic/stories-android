package com.wordpress.stories.util

import android.util.Size
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.automattic.photoeditor.views.added.AddedViewList

const val TARGET_RATIO_9_16 = 0.5625f // 9:16

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
        val adjustedSize = normalizeSizeExportTo916(originalView.width, originalView.height)
        heightToUse = adjustedSize.height
        widthToUse = adjustedSize.width
    }

    val measuredWidth = View.MeasureSpec.makeMeasureSpec(widthToUse, View.MeasureSpec.EXACTLY)
    val measuredHeight = View.MeasureSpec.makeMeasureSpec(heightToUse, View.MeasureSpec.EXACTLY)

    targetView.measure(measuredWidth, measuredHeight)
    targetView.layout(0, 0, targetView.measuredWidth, targetView.measuredHeight)
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

fun adjustAddedViewCoordinatesToNormalizedExportedSize(
    addedViews: AddedViewList,
    originalCanvasSize: Size,
    normalizedCanvasSize: Size
): AddedViewList {
    val adjustedAddedViewList = AddedViewList()
    var adjustedWidthRatio = 1.0f
    var adjustedHeightRatio = 1.0f

    // 1. first find the relation between originalCanvasSize.width and normalizedCanvasSize.width.
    if (normalizedCanvasSize.width < originalCanvasSize.width) {
        // adjust the view's width ratio
        adjustedWidthRatio = normalizedCanvasSize.width.toFloat() / originalCanvasSize.width
    }
    if (normalizedCanvasSize.height < originalCanvasSize.height) {
        // adjust the view's height ratio
        adjustedHeightRatio = normalizedCanvasSize.height.toFloat() / originalCanvasSize.height
    }

    val scaleAdjustedRadio = Math.min(adjustedHeightRatio, adjustedWidthRatio)

    // now go through the passed AddedViewList and re-create / modify each view
    for (addedView in addedViews) {
        // make an AddedView clone
//        val addedViewClone = AddedView.buildAddedViewFromView(requireNotNull(addedView.view), addedView.viewType)
        // now adjust the clone as necessary
        // TODO remove this and make an actual clone
        val addedViewClone = addedView
        addedViewClone.view?.apply {
            translationX = translationX * adjustedWidthRatio
            translationY = translationY * adjustedHeightRatio
            scaleX = scaleX * scaleAdjustedRadio
            scaleY = scaleY * scaleAdjustedRadio
        }

        // append it to our new list
        adjustedAddedViewList.add(addedViewClone)
    }

    // we need to keep a new copy of the AddedViews because in case these are serialized and re-presented on
    // the screen we can't show a modified copy that has been normalized to fit 9:16
    return adjustedAddedViewList
}
