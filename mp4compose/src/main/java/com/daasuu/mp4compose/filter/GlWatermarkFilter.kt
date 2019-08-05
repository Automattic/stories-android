package com.daasuu.mp4compose.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

/**
 * Created by sudamasayuki2 on 2018/01/27.
 */

class GlWatermarkFilter : GlOverlayFilter {

    private var bitmap: Bitmap? = null
    private val position = Position.LEFT_TOP
    private val positionInfo: ViewPositionInfo?
    private var matrixF: Matrix? = null

    constructor(bitmap: Bitmap) {
        this.bitmap = bitmap
    }

    constructor(bitmap: Bitmap, position: Position) {
        this.bitmap = bitmap
        this.position = position
    }

    constructor(bitmap: Bitmap, position: ViewPositionInfo) {
        this.bitmap = bitmap
        this.positionInfo = position
    }

    override fun drawCanvas(canvas: Canvas, presentationTime: Long) {
        if (bitmap != null && !bitmap!!.isRecycled) {
            if (positionInfo != null) {
                // calculate once
                if (matrixF == null) {
                    // transform coordinates from original parent View coordinate system to this video Canvas
                    // coordinate system
                    val newScaleY = canvas.height.toFloat() / positionInfo.parentViewHeight.toFloat()
                    val newScaleX = canvas.width.toFloat() / positionInfo.parentViewWidth.toFloat()

                    val quadrant1XOffset = (positionInfo.parentViewWidth / 2).toFloat()
                    val quadrant1YOffset = (positionInfo.parentViewHeight / 2).toFloat()
                    val newXcoord = (quadrant1XOffset - positionInfo.width / 2) * newScaleX
                    val newYcoord = (quadrant1YOffset - positionInfo.height / 2) * newScaleY

                    // deep copy the Matrix from original pinched/dragged view, re-scale with new destination surface scale
                    // and translate to new coordinate system
                    matrixF = Matrix(positionInfo.matrix)
                    matrixF!!.postScale(newScaleX, newScaleX)
                    matrixF!!.postTranslate(newXcoord, newYcoord)
                }
                val p = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
                canvas.drawBitmap(bitmap!!, matrixF!!, p)
            } else {
                when (position) {
                    GlWatermarkFilter.Position.LEFT_TOP -> canvas.drawBitmap(bitmap!!, 0f, 0f, null)
                    GlWatermarkFilter.Position.LEFT_BOTTOM -> canvas.drawBitmap(
                        bitmap!!,
                        0f,
                        (canvas.height - bitmap!!.height).toFloat(),
                        null
                    )
                    GlWatermarkFilter.Position.RIGHT_TOP -> canvas.drawBitmap(
                        bitmap!!,
                        (canvas.width - bitmap!!.width).toFloat(),
                        0f,
                        null
                    )
                    GlWatermarkFilter.Position.RIGHT_BOTTOM -> canvas.drawBitmap(
                        bitmap!!,
                        (canvas.width - bitmap!!.width).toFloat(),
                        (canvas.height - bitmap!!.height).toFloat(),
                        null
                    )
                }
            }
        }
    }

    enum class Position {
        LEFT_TOP,
        LEFT_BOTTOM,
        RIGHT_TOP,
        RIGHT_BOTTOM
    }
}