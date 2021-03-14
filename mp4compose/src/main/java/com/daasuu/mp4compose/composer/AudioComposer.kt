package com.daasuu.mp4compose.composer

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat

import java.nio.ByteBuffer
import java.nio.ByteOrder

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/PassThroughTrackTranscoder.java
internal class AudioComposer(
    private val mediaExtractor: MediaExtractor,
    private val trackIndex: Int,
    private val muxRender: MuxRender,
    private val useFallBacks: Boolean = false
) : IAudioComposer {
    private val sampleType = MuxRender.SampleType.AUDIO
    private val bufferInfo = MediaCodec.BufferInfo()
    private var bufferSize: Int
    private var buffer: ByteBuffer
    override var isFinished: Boolean = false
        private set
    private val actualOutputFormat: MediaFormat
    override var writtenPresentationTimeUs: Long = 0
        private set

    init {
        actualOutputFormat = this.mediaExtractor.getTrackFormat(this.trackIndex)
        this.muxRender.setOutputFormat(this.sampleType, actualOutputFormat)

        // TODO: maybe the original assignement could work as well?
        // bufferSize = actualOutputFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE) ? actualOutputFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) : (64 * 1024);
        bufferSize = if (useFallBacks && !actualOutputFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            (64 * 1024)
        } else {
            actualOutputFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        }

        buffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
    }

    @SuppressLint("Assert")
    override fun stepPipeline(): Boolean {
        if (isFinished) return false
        val trackIndex = mediaExtractor.sampleTrackIndex
        if (trackIndex < 0) {
            buffer.clear()
            bufferInfo.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            muxRender.writeSampleData(sampleType, buffer, bufferInfo)
            isFinished = true
            return true
        }
        if (trackIndex != this.trackIndex) return false

        buffer.clear()
        val sampleSize = mediaExtractor.readSampleData(buffer, 0)
        if (useFallBacks && sampleSize > bufferSize) {
            bufferSize = 2 * sampleSize;
            buffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
        } else {
            assert(sampleSize <= bufferSize)
        }
        val isKeyFrame = mediaExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
        val flags = if (isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
        bufferInfo.set(0, sampleSize, mediaExtractor.sampleTime, flags)
        muxRender.writeSampleData(sampleType, buffer, bufferInfo)
        // TODO: should we use the original writtenPresentationTimeUs = mediaExtractor.getSampleTime();
        // at least for the Video Compression use case?
        writtenPresentationTimeUs = bufferInfo.presentationTimeUs

        mediaExtractor.advance()
        return true
    }

    override fun setup() {
        // do nothing
    }

    override fun release() {
        // do nothing
    }
}
