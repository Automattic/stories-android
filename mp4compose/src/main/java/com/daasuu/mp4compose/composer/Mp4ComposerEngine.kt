package com.daasuu.mp4compose.composer

import android.graphics.Bitmap
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.util.Log
import android.util.Size

import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.FillModeCustomItem
import com.daasuu.mp4compose.Rotation
import com.daasuu.mp4compose.filter.GlFilter

import java.io.FileInputStream
import java.io.IOException

import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/MediaTranscoderEngine.java

/**
 * Internal engine, do not use this directly.
 */
internal class Mp4ComposerEngine {
    private var isSourceInputStream: FileInputStream? = null
    private var videoComposer: VideoComposer? = null
    private var audioComposer: IAudioComposer? = null
    private var mediaExtractor: MediaExtractor? = null
    private var mediaMuxer: MediaMuxer? = null
    private var progressCallback: ProgressCallback? = null
    private var durationUs: Long = 0
    private var mediaMetadataRetriever: MediaMetadataRetriever? = null

    private var useStaticBkg: Boolean = false
    private var bkgBitmap: Bitmap? = null

    @Throws(IOException::class)
    fun setDataSource(sourceIS: FileInputStream) {
        isSourceInputStream = sourceIS
        // make this call to make sure a file descriptor can be obtained
        isSourceInputStream!!.fd
    }

    fun setProgressCallback(progressCallback: ProgressCallback) {
        this.progressCallback = progressCallback
    }

    @Throws(IOException::class)
    fun composeFromVideoSource(
        destPath: String,
        outputResolution: Size,
        filter: GlFilter,
        bitrate: Int,
        mute: Boolean,
        rotation: Rotation,
        inputResolution: Size,
        fillMode: FillMode,
        fillModeCustomItem: FillModeCustomItem?,
        timeScale: Int,
        flipVertical: Boolean,
        flipHorizontal: Boolean
    ) {
        this.useStaticBkg = false
        compose(
            destPath, outputResolution, filter, bitrate, mute, rotation, inputResolution, fillMode,
            fillModeCustomItem, timeScale, flipVertical, flipHorizontal
        )
    }

    @Throws(IOException::class)
    fun composeFromStaticImageSource(
        bkgBitmap: Bitmap,
        destPath: String,
        outputResolution: Size,
        filter: GlFilter,
        bitrate: Int,
        mute: Boolean,
        rotation: Rotation,
        inputResolution: Size,
        fillMode: FillMode,
        fillModeCustomItem: FillModeCustomItem?,
        timeScale: Int,
        flipVertical: Boolean,
        flipHorizontal: Boolean
    ) {
        this.useStaticBkg = true
        this.bkgBitmap = bkgBitmap
        compose(
            destPath, outputResolution, filter, bitrate, mute, rotation, inputResolution, fillMode,
            fillModeCustomItem, timeScale, flipVertical, flipHorizontal
        )
    }

    @Throws(IOException::class)
    private fun compose(
        destPath: String,
        outputResolution: Size,
        filter: GlFilter,
        bitrate: Int,
        mute: Boolean,
        rotation: Rotation,
        inputResolution: Size,
        fillMode: FillMode,
        fillModeCustomItem: FillModeCustomItem?,
        timeScale: Int,
        flipVertical: Boolean,
        flipHorizontal: Boolean
    ) {
        try {
            mediaMuxer = MediaMuxer(destPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val videoOutputFormat =
                MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, outputResolution.width, outputResolution.height)
            val muxRender = MuxRender(mediaMuxer!!)

            if (!useStaticBkg) {
                mediaExtractor = MediaExtractor()
                mediaExtractor!!.setDataSource(isSourceInputStream!!.fd)
                mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever!!.setDataSource(isSourceInputStream!!.fd)
                try {
                    durationUs =
                        java.lang.Long.parseLong(
                            mediaMetadataRetriever!!.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000
                } catch (e: NumberFormatException) {
                    durationUs = -1
                }

                Log.d(TAG, "Duration (us): $durationUs")

                videoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                videoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                videoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                videoOutputFormat.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )

                // identify track indices
                val format = mediaExtractor!!.getTrackFormat(0)
                val mime = format.getString(MediaFormat.KEY_MIME)

                val videoTrackIndex: Int
                val audioTrackIndex: Int

                if (mime!!.startsWith("video/")) {
                    videoTrackIndex = 0
                    audioTrackIndex = 1
                } else {
                    videoTrackIndex = 1
                    audioTrackIndex = 0
                }

                // setup video composer
                videoComposer =
                    VideoComposer(mediaExtractor!!, videoTrackIndex, videoOutputFormat, muxRender, timeScale)
                videoComposer!!.setUp(
                    filter,
                    rotation,
                    outputResolution,
                    inputResolution,
                    fillMode,
                    fillModeCustomItem,
                    flipVertical,
                    flipHorizontal
                )
                mediaExtractor!!.selectTrack(videoTrackIndex)
                // setup audio if present and not muted
                if (mediaMetadataRetriever!!
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null && !mute) {
                    // has Audio video
                    if (timeScale < 2) {
                        audioComposer = AudioComposer(mediaExtractor!!, audioTrackIndex, muxRender)
                    } else {
                        audioComposer = RemixAudioComposer(
                            mediaExtractor!!,
                            audioTrackIndex,
                            mediaExtractor!!.getTrackFormat(audioTrackIndex),
                            muxRender,
                            timeScale
                        )
                    }

                    audioComposer!!.setup()

                    mediaExtractor!!.selectTrack(audioTrackIndex)

                    runPipelines()
                } else {
                    // no audio video
                    runPipelinesNoAudio()
                }
            } else {
                videoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                videoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                videoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                videoOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420SemiPlanar)

                // setup video composer for static background image
                videoComposer = VideoComposer(bkgBitmap!!, videoOutputFormat, muxRender, timeScale)
                videoComposer!!.setUp(
                    filter,
                    rotation,
                    outputResolution,
                    inputResolution,
                    fillMode,
                    fillModeCustomItem,
                    flipVertical,
                    flipHorizontal
                )
                runPipelinesNoAudioForStaticBackground()
            }

            mediaMuxer!!.stop()
        } finally {
            try {
                if (videoComposer != null) {
                    videoComposer!!.release()
                    videoComposer = null
                }
                if (audioComposer != null) {
                    audioComposer!!.release()
                    audioComposer = null
                }
                if (mediaExtractor != null) {
                    mediaExtractor!!.release()
                    mediaExtractor = null
                }
            } catch (e: RuntimeException) {
                // Too fatal to make alive the app, because it may leak native resources.

                throw Error("Could not shutdown mediaExtractor, codecs and mediaMuxer pipeline.", e)
            }

            try {
                if (mediaMuxer != null) {
                    mediaMuxer!!.release()
                    mediaMuxer = null
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to release mediaMuxer.", e)
            }

            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever!!.release()
                    mediaMetadataRetriever = null
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to release mediaMetadataRetriever.", e)
            }
        }
    }

    private fun runPipelines() {
        var loopCount: Long = 0
        if (durationUs <= 0) {
            if (progressCallback != null) {
                progressCallback!!.onProgress(PROGRESS_UNKNOWN)
            } // unknown
        }
        while (!(videoComposer!!.isFinished && audioComposer!!.isFinished)) {
            val stepped = videoComposer!!.stepPipeline() || audioComposer!!.stepPipeline()
            loopCount++
            if (durationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0L) {
                val videoProgress = if (videoComposer!!.isFinished) 1.0 else Math.min(
                    1.0,
                    videoComposer!!.writtenPresentationTimeUs.toDouble() / durationUs
                )
                val audioProgress = if (audioComposer!!.isFinished) 1.0 else Math.min(
                    1.0,
                    audioComposer!!.writtenPresentationTimeUs.toDouble() / durationUs
                )
                val progress = (videoProgress + audioProgress) / 2.0
                if (progressCallback != null) {
                    progressCallback!!.onProgress(progress)
                }
            }
            if (!stepped) {
                try {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS)
                } catch (e: InterruptedException) {
                    // nothing to do
                }
            }
        }
    }

    private fun runPipelinesNoAudio() {
        var loopCount: Long = 0
        if (durationUs <= 0) {
            if (progressCallback != null) {
                progressCallback!!.onProgress(PROGRESS_UNKNOWN)
            } // unknown
        }
        while (!videoComposer!!.isFinished) {
            val stepped = videoComposer!!.stepPipeline()
            loopCount++
            if (durationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0L) {
                val videoProgress = if (videoComposer!!.isFinished) 1.0 else Math.min(
                    1.0,
                    videoComposer!!.writtenPresentationTimeUs.toDouble() / durationUs
                )
                if (progressCallback != null) {
                    progressCallback!!.onProgress(videoProgress)
                }
            }
            if (!stepped) {
                try {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS)
                } catch (e: InterruptedException) {
                    // nothing to do
                }
            }
        }
    }

    private fun runPipelinesNoAudioForStaticBackground() {
        var loopCount: Long = 0
        if (durationUs <= 0) {
            if (progressCallback != null) {
                progressCallback!!.onProgress(PROGRESS_UNKNOWN)
            } // unknown
        }
        while (!videoComposer!!.isFinished) {
            val stepped = videoComposer!!.stepPipelineStaticImageBackground()
            loopCount++
            if (durationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0L) {
                val videoProgress = if (videoComposer!!.isFinished) 1.0 else Math.min(
                    1.0,
                    videoComposer!!.writtenPresentationTimeUs.toDouble() / durationUs
                )
                if (progressCallback != null) {
                    progressCallback!!.onProgress(videoProgress)
                }
            }
            if (!stepped) {
                try {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS)
                } catch (e: InterruptedException) {
                    // nothing to do
                }
            }
        }
    }

    internal interface ProgressCallback {
        /**
         * Called to notify progress. Same thread which initiated transcode is used.
         *
         * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
         */
        fun onProgress(progress: Double)
    }

    companion object {
        private val TAG = "Mp4ComposerEngine"
        private val PROGRESS_UNKNOWN = -1.0
        private val SLEEP_TO_WAIT_TRACK_TRANSCODERS: Long = 10
        private val PROGRESS_INTERVAL_STEPS: Long = 10

        private val BIT_RATE = 2000000
        private val FRAME_RATE = 20
        private val I_FRAME_INTERVAL = 5
    }
}
