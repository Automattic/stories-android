package com.daasuu.mp4compose.filter

import android.graphics.Matrix

data class ViewPositionInfo(
    val parentViewWidth: Int,
    val parentViewHeight: Int,
    val width: Int,
    val height: Int,
    val matrix: Matrix
)
