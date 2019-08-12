package com.daasuu.mp4compose

/**
 * Created by sudamasayuki on 2018/01/01.
 */

enum class FillMode {
    PRESERVE_ASPECT_FIT,
    PRESERVE_ASPECT_CROP,
    CUSTOM;

    companion object {
        fun getScaleAspectFit(angle: Int, widthIn: Int, heightIn: Int, widthOut: Int, heightOut: Int): FloatArray {
            var widthIn = widthIn
            var heightIn = heightIn
            val scale = floatArrayOf(1f, 1f)
            scale[1] = 1f
            scale[0] = scale[1]
            if (angle == 90 || angle == 270) {
                val cx = widthIn
                widthIn = heightIn
                heightIn = cx
            }

            val aspectRatioIn = widthIn.toFloat() / heightIn.toFloat()
            val heightOutCalculated = widthOut.toFloat() / aspectRatioIn

            if (heightOutCalculated < heightOut) {
                scale[1] = heightOutCalculated / heightOut
            } else {
                scale[0] = heightOut * aspectRatioIn / widthOut
            }

            return scale
        }

        fun getScaleAspectCrop(angle: Int, widthIn: Int, heightIn: Int, widthOut: Int, heightOut: Int): FloatArray {
            var widthIn = widthIn
            var heightIn = heightIn
            val scale = floatArrayOf(1f, 1f)
            scale[1] = 1f
            scale[0] = scale[1]
            if (angle == 90 || angle == 270) {
                val cx = widthIn
                widthIn = heightIn
                heightIn = cx
            }

            val aspectRatioIn = widthIn.toFloat() / heightIn.toFloat()
            val aspectRatioOut = widthOut.toFloat() / heightOut.toFloat()

            if (aspectRatioIn > aspectRatioOut) {
                val widthOutCalculated = heightOut.toFloat() * aspectRatioIn
                scale[0] = widthOutCalculated / widthOut
            } else {
                val heightOutCalculated = widthOut.toFloat() / aspectRatioIn
                scale[1] = heightOutCalculated / heightOut
            }

            return scale
        }
    }
}
