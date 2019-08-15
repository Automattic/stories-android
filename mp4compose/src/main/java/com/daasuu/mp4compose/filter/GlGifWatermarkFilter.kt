package com.daasuu.mp4compose.filter

import android.content.Context

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.GifHeader
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

class GlGifWatermarkFilter(
    val context: Context,
    gifAsInputStream: InputStream,
    val positionInfo: ViewPositionInfo? = null
) : GlOverlayFilter() {
    private val TAG = "GlGifWatermarkFilter"
    private var bitmap: Bitmap? = null
    private var position = Position.LEFT_TOP
    private val byteBuffer: ByteBuffer?
    private val gifDecoder: GifDecoder
    private var currentGifDuration: Long = 0

    private var matrixF: Matrix? = null
    private var cachedResizedBitmap: Bitmap? = null

    init {
        val glide = Glide.get(context)
        val sourceData = inputStreamToBytes(gifAsInputStream)
        byteBuffer = ByteBuffer.wrap(sourceData)

        val parser = GifHeaderParser()
        parser.setData(byteBuffer)
        val header = parser.parseHeader()
        // val sampleSize = getSampleSize(header, width, height)
        val sampleSize = getSampleSize(header, 1794, 1794) // TODO change hardcoded values
        gifDecoder = StandardGifDecoder(GifBitmapProvider(glide.bitmapPool, glide.arrayPool),
            header, byteBuffer, sampleSize)

        // use bitmap config with alpha for animated stickers (i.e. GIFs with transparent background)
        // Bitmap.Config.RGB_565 or ARGB_8888 for transparency
        gifDecoder.setDefaultBitmapConfig(Bitmap.Config.ARGB_8888)
        gifDecoder.advance()
        val firstFrame = gifDecoder.nextFrame
        if (firstFrame == null) {
            // TODO we can't do anything else
            //  return null
        }
    }

    fun calculateMatrix(canvas: Canvas) {
        // calculate once
        if (positionInfo != null) {
            // calculate once
            // transform coordinates from original parent View coordinate system to this video Canvas
            // coordinate system
            val newScaleY = canvas.height.toFloat() / positionInfo.parentViewHeight.toFloat()
            val newScaleX = canvas.width.toFloat() / positionInfo.parentViewWidth.toFloat()

            val quadrant1XOffset = (positionInfo.parentViewWidth / 2).toFloat()
            val quadrant1YOffset = (positionInfo.parentViewHeight / 2).toFloat()
            val newXcoord = (quadrant1XOffset - (positionInfo.width / 2)) * newScaleX
            val newYcoord = (quadrant1YOffset - (positionInfo.height / 2)) * newScaleY

            // deep copy the Matrix from original pinched/dragged view, re-scale with new destination surface scale
            // and translate to new coordinate system
            matrixF = Matrix(positionInfo.matrix)
            matrixF?.postScale(newScaleX, newScaleX)
            matrixF?.postTranslate(newXcoord, newYcoord)
        }
    }

    fun recreateBitmap() {
        cachedResizedBitmap = Bitmap.createScaledBitmap(bitmap!!, positionInfo!!.width, positionInfo.height, true)
    }

    override fun drawCanvas(canvas: Canvas, presentationTime: Long) {
        val gifNextDelay = gifDecoder.nextDelay * 1000
        bitmap = gifDecoder.nextFrame
        if (matrixF == null) {
            calculateMatrix(canvas)
            recreateBitmap()
        }

        if (shouldAdvanceGifFrame(gifNextDelay, presentationTime)) {
            currentGifDuration += gifNextDelay
            gifDecoder.advance()

            bitmap = gifDecoder.nextFrame
            recreateBitmap()
        }

        if (bitmap != null && !bitmap!!.isRecycled) {
            if (positionInfo != null) {
                val p = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
                canvas.drawBitmap(cachedResizedBitmap!!, matrixF!!, p)
            } else {
                when (position) {
                    Position.LEFT_TOP -> canvas.drawBitmap(bitmap!!, 0f, 0f, null)
                    Position.LEFT_BOTTOM -> canvas.drawBitmap(
                        bitmap!!,
                        0f,
                        (canvas.height - bitmap!!.height).toFloat(),
                        null
                    )
                    Position.RIGHT_TOP -> canvas.drawBitmap(
                        bitmap!!,
                        (canvas.width - bitmap!!.width).toFloat(),
                        0f,
                        null
                    )
                    Position.RIGHT_BOTTOM -> canvas.drawBitmap(
                        bitmap!!,
                        (canvas.width - bitmap!!.width).toFloat(),
                        (canvas.height - bitmap!!.height).toFloat(),
                        null
                    )
                }
            }
        }
    }

    private fun shouldAdvanceGifFrame(gifNextDelay: Int, presentationTime: Long): Boolean {
        if ((currentGifDuration + gifNextDelay) <= presentationTime) {
            return true
        }
        return false
    }

    enum class Position {
        LEFT_TOP,
        LEFT_BOTTOM,
        RIGHT_TOP,
        RIGHT_BOTTOM
    }

    private fun getSampleSize(gifHeader: GifHeader, targetWidth: Int, targetHeight: Int): Int {
        val exactSampleSize = Math.min(
            gifHeader.height / targetHeight,
            gifHeader.width / targetWidth
        )
        val powerOfTwoSampleSize = if (exactSampleSize == 0) 0 else Integer.highestOneBit(exactSampleSize)
        // Although functionally equivalent to 0 for BitmapFactory, 1 is a safer default for our code
        // than 0.
        val sampleSize = Math.max(1, powerOfTwoSampleSize)
        if (Log.isLoggable(TAG, Log.VERBOSE) && sampleSize > 1) {
            Log.v(
                TAG, "Downsampling GIF" +
                        ", sampleSize: " + sampleSize +
                        ", target dimens: [" + targetWidth + "x" + targetHeight + "]" +
                        ", actual dimens: [" + gifHeader.width + "x" + gifHeader.height + "]"
            )
        }
        return sampleSize
    }

    private fun inputStreamToBytes(inputStream: InputStream): ByteArray? {
        val bufferSize = 16384
        val buffer = ByteArrayOutputStream(bufferSize)
        try {
            var nRead: Int
            val data = ByteArray(bufferSize)

            nRead = inputStream.read(data)
            while (nRead != -1) {
                buffer.write(data, 0, nRead)
                nRead = inputStream.read(data)
            }
            buffer.flush()
        } catch (e: IOException) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Error reading data from stream", e)
            }
            return null
        }

        return buffer.toByteArray()
    }
}
