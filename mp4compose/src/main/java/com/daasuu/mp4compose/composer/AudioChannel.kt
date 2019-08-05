package com.daasuu.mp4compose.composer

import android.media.MediaCodec
import android.media.MediaFormat

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.ArrayDeque

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/AudioChannel.java

/**
 * Created by sudamasayuki2 on 2018/02/22.
 */

internal class AudioChannel(
    private val decoder: MediaCodec,
    private val encoder: MediaCodec,
    private val encodeFormat: MediaFormat
) {

    private val emptyBuffers = ArrayDeque<AudioBuffer>()
    private val filledBuffers = ArrayDeque<AudioBuffer>()

    private var inputSampleRate: Int = 0
    private var inputChannelCount: Int = 0
    private var outputChannelCount: Int = 0

    private val decoderBuffers: MediaCodecBufferCompatWrapper
    private val encoderBuffers: MediaCodecBufferCompatWrapper

    private val overflowBuffer = AudioBuffer()

    private var actualDecodedFormat: MediaFormat? = null

    private class AudioBuffer {
        internal var bufferIndex: Int = 0
        internal var presentationTimeUs: Long = 0
        internal var data: ShortBuffer? = null
    }

    init {

        decoderBuffers = MediaCodecBufferCompatWrapper(this.decoder)
        encoderBuffers = MediaCodecBufferCompatWrapper(this.encoder)
    }

    fun setActualDecodedFormat(decodedFormat: MediaFormat) {
        actualDecodedFormat = decodedFormat

        inputSampleRate = actualDecodedFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        if (inputSampleRate != encodeFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)) {
            throw UnsupportedOperationException("Audio sample rate conversion not supported yet.")
        }

        inputChannelCount = actualDecodedFormat!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        outputChannelCount = encodeFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        if (inputChannelCount != 1 && inputChannelCount != 2) {
            throw UnsupportedOperationException("Input channel count ($inputChannelCount) not supported.")
        }

        if (outputChannelCount != 1 && outputChannelCount != 2) {
            throw UnsupportedOperationException("Output channel count ($outputChannelCount) not supported.")
        }

        overflowBuffer.presentationTimeUs = 0
    }

    fun drainDecoderBufferAndQueue(bufferIndex: Int, presentationTimeUs: Long) {
        if (actualDecodedFormat == null) {
            throw RuntimeException("Buffer received before format!")
        }

        val data = if (bufferIndex == BUFFER_INDEX_END_OF_STREAM)
            null
        else
            decoderBuffers.getOutputBuffer(bufferIndex)

        var buffer = emptyBuffers.poll()
        if (buffer == null) {
            buffer = AudioBuffer()
        }

        buffer.bufferIndex = bufferIndex
        buffer.presentationTimeUs = presentationTimeUs
        buffer.data = data?.asShortBuffer()

        if (overflowBuffer.data == null) {
            overflowBuffer.data = ByteBuffer
                .allocateDirect(data!!.capacity())
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
            overflowBuffer.data!!.clear().flip()
        }

        filledBuffers.add(buffer)
    }

    fun feedEncoder(timeoutUs: Long): Boolean {
        val hasOverflow = overflowBuffer.data != null && overflowBuffer.data!!.hasRemaining()
        if (filledBuffers.isEmpty() && !hasOverflow) {
            // No audio data - Bail out
            return false
        }

        val encoderInBuffIndex = encoder.dequeueInputBuffer(timeoutUs)
        if (encoderInBuffIndex < 0) {
            // Encoder is full - Bail out
            return false
        }

        // Drain overflow first
        val outBuffer = encoderBuffers.getInputBuffer(encoderInBuffIndex)!!.asShortBuffer()
        if (hasOverflow) {
            val presentationTimeUs = drainOverflow(outBuffer)
            encoder.queueInputBuffer(
                encoderInBuffIndex,
                0, outBuffer.position() * BYTES_PER_SHORT,
                presentationTimeUs, 0
            )
            return true
        }

        val inBuffer = filledBuffers.poll()
        if (inBuffer!!.bufferIndex == BUFFER_INDEX_END_OF_STREAM) {
            encoder.queueInputBuffer(encoderInBuffIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return false
        }

        val presentationTimeUs = remixAndMaybeFillOverflow(inBuffer, outBuffer)
        encoder.queueInputBuffer(
            encoderInBuffIndex,
            0, outBuffer.position() * BYTES_PER_SHORT,
            presentationTimeUs, 0
        )
        if (inBuffer != null) {
            decoder.releaseOutputBuffer(inBuffer.bufferIndex, false)
            emptyBuffers.add(inBuffer)
        }

        return true
    }

    private fun drainOverflow(outBuff: ShortBuffer): Long {
        val overflowBuff = overflowBuffer.data
        val overflowLimit = overflowBuff!!.limit()
        val overflowSize = overflowBuff.remaining()

        val beginPresentationTimeUs = overflowBuffer.presentationTimeUs + sampleCountToDurationUs(
            overflowBuff.position(),
            inputSampleRate,
            outputChannelCount
        )

        outBuff.clear()
        // Limit overflowBuff to outBuff's capacity
        overflowBuff.limit(outBuff.capacity())
        // Load overflowBuff onto outBuff
        outBuff.put(overflowBuff)

        if (overflowSize >= outBuff.capacity()) {
            // Overflow fully consumed - Reset
            overflowBuff.clear().limit(0)
        } else {
            // Only partially consumed - Keep position & restore previous limit
            overflowBuff.limit(overflowLimit)
        }

        return beginPresentationTimeUs
    }

    private fun remixAndMaybeFillOverflow(
        input: AudioBuffer,
        outBuff: ShortBuffer
    ): Long {
        val inBuff = input.data
        val overflowBuff = overflowBuffer.data

        outBuff.clear()

        // Reset position to 0, and set limit to capacity (Since MediaCodec doesn't do that for us)
        inBuff!!.clear()

        if (inBuff.remaining() > outBuff.remaining()) {
            // Overflow
            // Limit inBuff to outBuff's capacity
            inBuff.limit(outBuff.capacity())
            outBuff.put(inBuff)

            // Reset limit to its own capacity & Keep position
            inBuff.limit(inBuff.capacity())

            // Remix the rest onto overflowBuffer
            // NOTE: We should only reach this point when overflow buffer is empty
            val consumedDurationUs = sampleCountToDurationUs(inBuff.position(), inputSampleRate, inputChannelCount)
            overflowBuff!!.put(inBuff)

            // Seal off overflowBuff & mark limit
            overflowBuff.flip()
            overflowBuffer.presentationTimeUs = input.presentationTimeUs + consumedDurationUs
        } else {
            // No overflow
            outBuff.put(inBuff)
        }

        return input.presentationTimeUs
    }

    companion object {

        val BUFFER_INDEX_END_OF_STREAM = -1

        private val BYTES_PER_SHORT = 2
        private val MICROSECS_PER_SEC: Long = 1000000

        private fun sampleCountToDurationUs(
            sampleCount: Int,
            sampleRate: Int,
            channelCount: Int
        ): Long {
            return sampleCount / (sampleRate * MICROSECS_PER_SEC) / channelCount
        }
    }
}
