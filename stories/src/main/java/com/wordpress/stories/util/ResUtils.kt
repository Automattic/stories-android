package com.wordpress.stories.util

import android.content.res.Resources
import android.content.res.TypedArray
import java.util.ArrayList

/**
 * Returns a list of color ints contained in this TypedArray.
 * Handles nested arrays (recursively), and arrays combining colors and other arrays.
 */
fun TypedArray.extractColorList(resources: Resources, outList: ArrayList<Int> = ArrayList()): List<Int> {
    for (i in 0 until length()) {
        val nestedArrayResourceId = getResourceId(i, 0)
        if (nestedArrayResourceId > 0) {
            // This is a nested array, parse it recursively
            val colorArray = resources.obtainTypedArray(nestedArrayResourceId)
            colorArray.extractColorList(resources, outList)
            colorArray.recycle()
        } else {
            // This is an attribute pointing to a color resource
            outList.add(getColor(i, 0))
        }
    }

    return outList
}
