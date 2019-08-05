package com.daasuu.mp4compose.composer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/QueuedMuxer.java

internal class MuxRender(private val muxer: MediaMuxer) {
    private var videoFormat: MediaFormat? = null
    private var audioFormat: MediaFormat? = null
    private var videoTrackIndex: Int = 0
    private var audioTrackIndex: Int = 0
    private var byteBuffer: ByteBuffer? = null
    private val sampleInfoList: MutableList<SampleInfo>
    private var started: Boolean = false

    init {
        sampleInfoList = ArrayList()
    }

    fun setOutputFormat(sampleType: SampleType, format: MediaFormat) {
        when (sampleType) {
            MuxRender.SampleType.VIDEO -> videoFormat = format
            MuxRender.SampleType.AUDIO -> audioFormat = format
            else -> throw AssertionError()
        }
    }

    fun onSetOutputFormat() {
        if (videoFormat != null && audioFormat != null) {
            videoTrackIndex = muxer.addTrack(videoFormat!!)
            Log.v(
                TAG,
                "Added track #" + videoTrackIndex + " with " + videoFormat!!.getString(MediaFormat.KEY_MIME) +
                        " to muxer"
            )
            audioTrackIndex = muxer.addTrack(audioFormat!!)
            Log.v(
                TAG,
                "Added track #" + audioTrackIndex + " with " + audioFormat!!.getString(MediaFormat.KEY_MIME) +
                        " to muxer"
            )
        } else if (videoFormat != null) {

            videoTrackIndex = muxer.addTrack(videoFormat!!)
            Log.v(
                TAG,
                "Added track #" + videoTrackIndex + " with " + videoFormat!!.getString(MediaFormat.KEY_MIME) +
                        " to muxer"
            )
        }

        muxer.start()
        started = true

        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocate(0)
        }
        byteBuffer!!.flip()
        Log.v(
            TAG, "Output format determined, writing " + sampleInfoList.size +
                    " samples / " + byteBuffer!!.limit() + " bytes to muxer."
        )
        val bufferInfo = MediaCodec.BufferInfo()
        var offset = 0
        for (sampleInfo in sampleInfoList) {
            sampleInfo.writeToBufferInfo(bufferInfo, offset)
            muxer.writeSampleData(getTrackIndexForSampleType(sampleInfo.sampleType), byteBuffer!!, bufferInfo)
            offset += sampleInfo.size
        }
        sampleInfoList.clear()
        byteBuffer = null
    }

    fun writeSampleData(sampleType: SampleType, byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (started) {
            muxer.writeSampleData(getTrackIndexForSampleType(sampleType), byteBuf, bufferInfo)
            return
        }
        byteBuf.limit(bufferInfo.offset + bufferInfo.size)
        byteBuf.position(bufferInfo.offset)
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder())
        }
        byteBuffer!!.put(byteBuf)
        sampleInfoList.add(SampleInfo(sampleType, bufferInfo.size, bufferInfo))
    }

    private fun getTrackIndexForSampleType(sampleType: SampleType): Int {
        when (sampleType) {
            MuxRender.SampleType.VIDEO -> return videoTrackIndex
            MuxRender.SampleType.AUDIO -> return audioTrackIndex
            else -> throw AssertionError()
        }
    }

    enum class SampleType {
        VIDEO, AUDIO
    }

    private class SampleInfo constructor(
        val sampleType: SampleType,
        val size: Int,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        private val presentationTimeUs: Long
        private val flags: Int

        init {
            presentationTimeUs = bufferInfo.presentationTimeUs
            flags = bufferInfo.flags
        }

        fun writeToBufferInfo(bufferInfo: MediaCodec.BufferInfo, offset: Int) {
            bufferInfo.set(offset, size, presentationTimeUs, flags)
        }
    }

    companion object {
        private val TAG = "MuxRender"
        private val BUFFER_SIZE = 64 * 1024 // I have no idea whether this value is appropriate or not...
    }
}
