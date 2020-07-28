package com.wordpress.stories.util

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

fun getDisplayPixelSize(context: Context): Point {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val size = Point()
    display.getSize(size)
    return size
}

fun getDisplayPixelWidth(context: Context): Int {
    val size = getDisplayPixelSize(context)
    return size.x
}

fun getDisplayPixelHeight(context: Context): Int {
    val size = getDisplayPixelSize(context)
    return size.y
}
