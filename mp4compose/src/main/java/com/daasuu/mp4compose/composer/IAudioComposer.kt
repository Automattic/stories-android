package com.daasuu.mp4compose.composer

/**
 * Created by sudamasayuki2 on 2018/02/24.
 */

internal interface IAudioComposer {

    val writtenPresentationTimeUs: Long

    val isFinished: Boolean

    fun setup()

    fun stepPipeline(): Boolean

    fun release()
}
