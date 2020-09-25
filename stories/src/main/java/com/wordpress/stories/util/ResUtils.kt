package com.wordpress.stories.util

import android.content.res.Resources
import android.content.res.TypedArray
import java.util.ArrayList

/**
 * Given a TypedArray, returns a list of color ints contained in the TypedArray.
 * Handles nested arrays (recursively), and arrays combining colors and other arrays.
 */
fun colorListFromTypedArray(resources: Resources, baseArray: TypedArray, outList: ArrayList<Int> = ArrayList()):
        List<Int> {
    for (i in 0 until baseArray.length()) {
        val nestedArrayResourceId = baseArray.getResourceId(i, 0)
        if (nestedArrayResourceId > 0) {
            // This is a nested array, parse it recursively
            val colorArray = resources.obtainTypedArray(nestedArrayResourceId)
            colorListFromTypedArray(resources, colorArray, outList)
        } else {
            // This is an attribute pointing to a color resource
            outList.add(baseArray.getColor(i, 0))
        }
    }

    baseArray.recycle()
    return outList
}
