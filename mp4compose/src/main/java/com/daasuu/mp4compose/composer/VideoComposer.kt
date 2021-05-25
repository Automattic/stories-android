package com.daasuu.mp4compose.composer

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Size

import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.FillModeCustomItem
import com.daasuu.mp4compose.Rotation
import com.daasuu.mp4compose.filter.GlFilter
import com.daasuu.mp4compose.utils.BitmapEncodingUtils

import java.io.IOException
import java.nio.ByteBuffer

// Refer: https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java
// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/VideoTrackTranscoder.java
internal class VideoComposer {
    private val mediaExtractor: MediaExtractor?
    private val trackIndex: Int
    private val outputFormat: MediaFormat
    private val muxRender: MuxRender
    private val bufferInfo = MediaCodec.BufferInfo()
    private var decoder: MediaCodec? = null
    private var encoder: MediaCodec? = null
    private var actualOutputFormat: MediaFormat? = null
    private var decoderSurface: DecoderSurface? = null
    private var encoderSurface: EncoderSurface? = null
    private var isExtractorEOS: Boolean = false
    private var isDecoderEOS: Boolean = false
    var isFinished: Boolean = false
        private set
    private var decoderStarted: Boolean = false
    private var encoderStarted: Boolean = false
    var writtenPresentationTimeUs: Long = 0
        private set
    // TODO: currently we do not use the timeScale feature. Also the timeScale ends up
    // being converted into an int in here being a float in upper layers.
    // See https://github.com/Automattic/stories-android/issues/685 for more context.
    private val timeScale: Int
    private var useStaticBkg: Boolean = false
    private var addedFrameCount = 0
    private var bkgBitmapBytesNV12: ByteArray? = null
    private var lastBufferIdx = 0

    constructor(
        mediaExtractor: MediaExtractor,
        trackIndex: Int,
        outputFormat: MediaFormat,
        muxRender: MuxRender,
        timeScale: Int
    ) {
        this.mediaExtractor = mediaExtractor
        this.trackIndex = trackIndex
        this.outputFormat = outputFormat
        this.muxRender = muxRender
        this.timeScale = timeScale
    }

    // alternate constructor that doesn't have a MediaExtractor, since we won't be converting from video to another
    constructor(bkgBitmap: Bitmap, outputFormat: MediaFormat, muxRender: MuxRender, timeScale: Int) {
        this.outputFormat = outputFormat
        this.muxRender = muxRender
        this.timeScale = timeScale
        this.useStaticBkg = true

        this.bkgBitmapBytesNV12 = BitmapEncodingUtils.getNV12(bkgBitmap.width, bkgBitmap.height, bkgBitmap)
        bkgBitmap.recycle()

        this.mediaExtractor = null
        this.trackIndex = -1
    }

    fun setUp(
        filter: GlFilter,
        rotation: Rotation,
        outputResolution: Size,
        inputResolution: Size,
        fillMode: FillMode,
        fillModeCustomItem: FillModeCustomItem?,
        flipVertical: Boolean,
        flipHorizontal: Boolean
    ) {
        try {
            encoder = MediaCodec.createEncoderByType(outputFormat.getString(MediaFormat.KEY_MIME)!!)
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }

        encoder!!.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        if (!useStaticBkg) {
            encoderSurface = EncoderSurface(encoder!!.createInputSurface())
            encoderSurface!!.makeCurrent()
        }
        encoder!!.start()
        encoderStarted = true

        if (!useStaticBkg) {
            // only set up mediaExtractor if source is a video we can decode :)
            mediaExtractor!!.selectTrack(trackIndex)
            val inputFormat = mediaExtractor.getTrackFormat(trackIndex)
            if (inputFormat.containsKey("rotation-degrees")) {
                // Decoded video is rotated automatically in Android 5.0 lollipop.
                // Turn off here because we don't want to encode rotated one.
                // refer: https://android.googlesource.com/platform/frameworks/av/+blame/lollipop-release/media/libstagefright/Utils.cpp
                inputFormat.setInteger("rotation-degrees", 0)
            }

            decoderSurface = DecoderSurface(filter)
            decoderSurface!!.setRotation(rotation)
            decoderSurface!!.setOutputResolution(outputResolution)
            decoderSurface!!.setInputResolution(inputResolution)
            decoderSurface!!.setFillMode(fillMode)
            decoderSurface!!.setFillModeCustomItem(fillModeCustomItem)
            decoderSurface!!.setFlipHorizontal(flipHorizontal)
            decoderSurface!!.setFlipVertical(flipVertical)
            decoderSurface!!.completeParams()

            try {
                decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }

            decoder!!.configure(inputFormat, decoderSurface!!.surface, null, 0)
            decoder!!.start()
            decoderStarted = true
        }
    }

    fun stepPipeline(): Boolean {
        var busy = false

        var status: Int
        while (drainEncoder() != DRAIN_STATE_NONE) {
            busy = true
        }
        do {
            status = drainDecoder()
            if (status != DRAIN_STATE_NONE) {
                busy = true
            }
            // NOTE: not repeating to keep from deadlock when encoder is full.
        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY)
        while (drainExtractor() != DRAIN_STATE_NONE) {
            busy = true
        }

        return busy
    }

    fun stepPipelineStaticImageBackground(): Boolean {
        var busy = false

        val inputBufIdx = encoder!!.dequeueInputBuffer(0)
        // inject the image
        if (inputBufIdx >= 0) {
            // TODO figure out here when we have run out of bitmaps OR when it's the end time for the video?
            // FIXME hardcoded 5 seconds length
            if (writtenPresentationTimeUs > 5000000) { // fixed at 5 seconds
                isExtractorEOS = true
                encoder!!.queueInputBuffer(inputBufIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            } else {
                // add bitmap into decoder.inputBuffer
                // byte[] input = BitmapEncodingUtils.getNV12(bkgBitmap.getWidth(), bkgBitmap.getHeight(), bkgBitmap);
                val inputBuffer = encoder!!.getInputBuffer(inputBufIdx)
                inputBuffer!!.clear()
                inputBuffer.put(bkgBitmapBytesNV12)
                encoder!!.queueInputBuffer(
                    inputBufIdx, 0, bkgBitmapBytesNV12!!.size,
                    getPresentationTimeUsec(addedFrameCount), 0
                )
                addedFrameCount++
            }
        }

        while (drainEncoder() != DRAIN_STATE_NONE) {
            busy = true
        }
        //        do {
        //            status = drainDecoder();
        //            if (status != DRAIN_STATE_NONE) {
        //                busy = true;
        //            }
        //            // NOTE: not repeating to keep from deadlock when encoder is full.
        //        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);
        //        while (drainExtractor() != DRAIN_STATE_NONE) {
        //            busy = true;
        //        }

        return busy
    }

    fun release() {
        if (decoderSurface != null) {
            decoderSurface!!.release()
            decoderSurface = null
        }
        if (encoderSurface != null) {
            encoderSurface!!.release()
            encoderSurface = null
        }
        if (decoder != null) {
            if (decoderStarted) decoder!!.stop()
            decoder!!.release()
            decoder = null
        }
        if (encoder != null) {
            if (encoderStarted) encoder!!.stop()
            encoder!!.release()
            encoder = null
        }
    }

    private fun drainExtractor(): Int {
        if (isExtractorEOS) return DRAIN_STATE_NONE

        val trackIndex = mediaExtractor!!.sampleTrackIndex
        if (trackIndex >= 0 && trackIndex != this.trackIndex) {
            return DRAIN_STATE_NONE
        }
        decoder?.let { decoder ->
            val result = decoder.dequeueInputBuffer(0)
            if (result < 0) return DRAIN_STATE_NONE
            if (trackIndex < 0) {
                isExtractorEOS = true
                decoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                return DRAIN_STATE_NONE
            }
            val decoderInputBuffer = decoder.getInputBuffer(result)
            decoderInputBuffer?.let {
                val sampleSize = mediaExtractor.readSampleData(it, 0)
                val isKeyFrame = mediaExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
                decoder.queueInputBuffer(
                    result,
                    0,
                    sampleSize,
                    mediaExtractor.sampleTime / timeScale,
                    if (isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                )
            }
        }
        mediaExtractor.advance()
        return DRAIN_STATE_CONSUMED
    }

    private fun drainDecoder(): Int {
        if (isDecoderEOS) return DRAIN_STATE_NONE
        val result = decoder!!.dequeueOutputBuffer(bufferInfo, 0)
        when (result) {
            MediaCodec.INFO_TRY_AGAIN_LATER -> return DRAIN_STATE_NONE
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED, MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ->
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
        }
        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            encoder!!.signalEndOfInputStream()
            isDecoderEOS = true
            bufferInfo.size = 0
        }
        val doRender = bufferInfo.size > 0
        // NOTE: doRender will block if buffer (of encoder) is full.
        // Refer: http://bigflake.com/mediacodec/CameraToMpegTest.java.txt
        decoder!!.releaseOutputBuffer(result, doRender)
        if (doRender) {
            decoderSurface!!.awaitNewImage()
            decoderSurface!!.drawImage(bufferInfo.presentationTimeUs)
            encoderSurface!!.setPresentationTime(bufferInfo.presentationTimeUs * 1000)
            encoderSurface!!.swapBuffers()
        }
        return DRAIN_STATE_CONSUMED
    }

    private fun drainEncoder(): Int {
        if (isFinished) return DRAIN_STATE_NONE
        val result = encoder!!.dequeueOutputBuffer(bufferInfo, 0)
        var encoderOutputBuffer: ByteBuffer? = null
        when (result) {
            MediaCodec.INFO_TRY_AGAIN_LATER -> return DRAIN_STATE_NONE
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                if (actualOutputFormat != null) {
                    throw RuntimeException("Video output format changed twice.")
                }
                actualOutputFormat = encoder!!.outputFormat
                muxRender.setOutputFormat(MuxRender.SampleType.VIDEO, actualOutputFormat!!)
                muxRender.onSetOutputFormat()
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
            }
            MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                encoderOutputBuffer = encoder!!.getOutputBuffer(result)
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
            }
            else -> {
                if (result >= 0) {
                    encoderOutputBuffer = encoder!!.getOutputBuffer(result)
                    if (encoderOutputBuffer == null) {
                        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
                    }
                }
            }
        }

        if (actualOutputFormat == null) {
            throw RuntimeException("Could not determine actual output format.")
        }

        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            isFinished = true
            bufferInfo.set(0, 0, 0, bufferInfo.flags)
        }
        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            // SPS or PPS, which should be passed by MediaFormat.
            encoder!!.releaseOutputBuffer(result, false)
            return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
        }

        if (useStaticBkg) {
            val encodedData = encoder!!.getOutputBuffer(result)
            if (encodedData != null) {
                encodedData.position(bufferInfo.offset)
                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                muxRender.writeSampleData(MuxRender.SampleType.VIDEO, encodedData, bufferInfo)
                writtenPresentationTimeUs = bufferInfo.presentationTimeUs
                encoder!!.releaseOutputBuffer(result, false)
                // encodedFrameCount++;
            }
        } else {
            muxRender.writeSampleData(MuxRender.SampleType.VIDEO, encoderOutputBuffer!!, bufferInfo)
            writtenPresentationTimeUs = bufferInfo.presentationTimeUs
            encoder!!.releaseOutputBuffer(result, false)
        }
        return DRAIN_STATE_CONSUMED
    }

    companion object {
        private val TAG = "VideoComposer"
        private val DRAIN_STATE_NONE = 0
        private val DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1
        private val DRAIN_STATE_CONSUMED = 2
        // private Bitmap bkgBitmap;
        private val ONE_SEC: Long = 1000000

        private fun getPresentationTimeUsec(frameIndex: Int): Long {
            return frameIndex.toLong() * ONE_SEC / 20
        }
    }
}
