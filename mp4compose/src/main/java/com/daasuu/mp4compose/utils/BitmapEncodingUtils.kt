package com.daasuu.mp4compose.utils

import android.graphics.Bitmap

object BitmapEncodingUtils {
    // read more on https://wiki.videolan.org/YUV
    fun getNV12(inputWidth: Int, inputHeight: Int, scaled: Bitmap): ByteArray {
        val argb = IntArray(inputWidth * inputHeight)
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        val yuv = ByteArray(inputWidth * inputHeight * 3 / 2)
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight)
        scaled.recycle()
        return yuv
    }

    // encodes bitmap in YUV 4:2:0 format
    fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        var yIndex = 0
        var uvIndex = width * height
        var index = 0
        var j = 0
        while (j < height) {
            var uvIndex2: Int
            var yIndex2: Int
            var i = 0
            while (true) {
                uvIndex2 = uvIndex
                yIndex2 = yIndex
                if (i >= width) {
                    break
                }
                val R = argb[index] and 0xFF0000 shr 16
                val G = argb[index] and 0xFF00 shr 8
                val B = argb[index] and 0x0000FF shr 0
                var Y = R * 77 + G * 150 + B * 29 + 128 shr 8
                var V = (R * -43 - G * 84 + B * 127 + 128 shr 8) + 128
                var U = (R * 127 - G * 106 - B * 21 + 128 shr 8) + 128
                yIndex = yIndex2 + 1
                if (Y < 0) {
                    Y = 0
                } else if (Y > 255) {
                    Y = 255
                }
                yuv420sp[yIndex2] = Y.toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    uvIndex = uvIndex2 + 1
                    if (V < 0) {
                        V = 0
                    } else if (V > 255) {
                        V = 255
                    }
                    yuv420sp[uvIndex2] = V.toByte()
                    uvIndex2 = uvIndex + 1
                    if (U < 0) {
                        U = 0
                    } else if (U > 255) {
                        U = 255
                    }
                    yuv420sp[uvIndex] = U.toByte()
                }
                uvIndex = uvIndex2
                index++
                i++
            }
            j++
            uvIndex = uvIndex2
            yIndex = yIndex2
        }
    }
}
