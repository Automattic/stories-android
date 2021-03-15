package com.daasuu.mp4compose.composer

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodecInfo.CodecProfileLevel
import android.net.Uri
import android.util.Size
import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.VideoFormatMimeType
import com.daasuu.mp4compose.composer.ComposerUseCase.CompressVideo
import com.daasuu.mp4compose.composer.ComposerUseCase.SaveVideoAsFile
import com.daasuu.mp4compose.composer.ComposerUseCase.SaveVideoFromBgAsFile
import com.daasuu.mp4compose.filter.GlFilter

sealed class ComposerUseCase {
    data class SaveVideoAsFile(
        val srcUri: Uri,
        val destPath: String,
        val context: Context,
        val headers: Map<String, String>?
    ) : ComposerUseCase()

    data class SaveVideoFromBgAsFile(val bkgBmp: Bitmap, val destPath: String) : ComposerUseCase()

    data class CompressVideo @JvmOverloads constructor (
        val srcPath: String,
        val destPath: String,
        val videoFormatMimeType: VideoFormatMimeType,
        val bitrate: Int,
        val iFrameInterval: Int = 1,
        val audioBitRate: Int = 128000,
        val aacProfile: Int = CodecProfileLevel.AACObjectELD,
        val forceAudioEncoding: Boolean = false
    ) : ComposerUseCase()
}

interface ComposerInterface {
    fun size(size: Size): ComposerInterface
    fun fillMode(fillMode: FillMode): ComposerInterface
    fun filter(filter: GlFilter?): ComposerInterface
    fun mute(mute: Boolean): ComposerInterface
    fun listener(listener: Listener): ComposerInterface
    fun start(): ComposerInterface
}

object ComposerProvider {
    fun getComposerForUseCase(useCase: ComposerUseCase): ComposerInterface {
        return when (useCase) {
            is SaveVideoAsFile -> {
                Mp4Composer(useCase.srcUri, useCase.destPath)
                        .with(useCase.context)
                        .addedHeaders(useCase.headers)
            }
            is SaveVideoFromBgAsFile -> {
                Mp4Composer(useCase.bkgBmp, useCase.destPath)
            }
            is CompressVideo -> {
                Mp4ComposerBasic(useCase.srcPath, useCase.destPath)
                        .videoFormatMimeType(useCase.videoFormatMimeType)
                        .videoBitrate(useCase.bitrate)
                        .iFrameInterval(useCase.iFrameInterval)
                        .audioBitRate(useCase.audioBitRate)
                        .aacProfile(useCase.aacProfile)
                        .forceAudioEncoding(useCase.forceAudioEncoding)
            }
        }
    }
}
