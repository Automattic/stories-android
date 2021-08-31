package com.daasuu.mp4compose.composer

import android.media.MediaCodec

import java.nio.ByteBuffer

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/compat/MediaCodecBufferCompatWrapper.java

/**
 * A Wrapper to MediaCodec that facilitates the use of API-dependent get{Input/Output}Buffer methods,
 * in order to prevent: http://stackoverflow.com/q/30646885
 */

internal class MediaCodecBufferCompatWrapper(private val mediaCodec: MediaCodec) {
    private val inputBuffers: Array<ByteBuffer>? = null
    private val outputBuffers: Array<ByteBuffer>? = null

    fun getInputBuffer(index: Int): ByteBuffer? {
        return mediaCodec.getInputBuffer(index)
    }

    fun getOutputBuffer(index: Int): ByteBuffer? {
        return mediaCodec.getOutputBuffer(index)
    }
}
