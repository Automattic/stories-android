package com.daasuu.mp4compose.composer

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.Size

import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.FillModeCustomItem
import com.daasuu.mp4compose.Rotation
import com.daasuu.mp4compose.composer.Mp4ComposerEngine.ProgressCallback
import com.daasuu.mp4compose.filter.GlFilter
import com.daasuu.mp4compose.utils.DataSourceUtil

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by sudamasayuki on 2017/11/15.
 */

class Mp4Composer: ComposerInterface {
    private val srcUri: Uri?
    private val destPath: String
    private var filter: GlFilter? = null
    private var outputResolution: Size? = null
    private var bitrate = -1
    private var mute = false
    private var rotation = Rotation.NORMAL
    private var listener: Listener? = null
    private var fillMode: FillMode = FillMode.PRESERVE_ASPECT_FIT
    private var fillModeCustomItem: FillModeCustomItem? = null
    private var timeScale = 1
    private var flipVertical = false
    private var flipHorizontal = false
    private var isStaticImageBkgSource = false
    private var bkgBitmap: Bitmap? = null
    private var context: Context? = null
    private var addedRequestHeaders: Map<String, String>? = null

    private var executorService: ExecutorService? = null

    constructor(srcUri: Uri, destPath: String) {
        this.srcUri = srcUri
        this.bkgBitmap = null
        this.destPath = destPath
        isStaticImageBkgSource = false
    }

    constructor(bkgBmp: Bitmap, destPath: String) {
        this.bkgBitmap = bkgBmp
        this.destPath = destPath
        isStaticImageBkgSource = true

        this.srcUri = null
    }

    fun with(context: Context): Mp4Composer {
        // needed for Uri handling (content resolver)
        this.context = context
        return this
    }

    fun addedHeaders(headers: Map<String, String>?): Mp4Composer {
        this.addedRequestHeaders = headers
        return this
    }

    override fun filter(filter: GlFilter?): Mp4Composer {
        this.filter = filter
        return this
    }

    fun size(width: Int, height: Int): Mp4Composer {
        this.outputResolution = Size(width, height)
        return this
    }

    override fun size(size: Size): Mp4Composer {
        this.outputResolution = size
        return this
    }

    fun videoBitrate(bitrate: Int): Mp4Composer {
        this.bitrate = bitrate
        return this
    }

    override fun mute(mute: Boolean): Mp4Composer {
        this.mute = mute
        return this
    }

    fun flipVertical(flipVertical: Boolean): Mp4Composer {
        this.flipVertical = flipVertical
        return this
    }

    fun flipHorizontal(flipHorizontal: Boolean): Mp4Composer {
        this.flipHorizontal = flipHorizontal
        return this
    }

    fun rotation(rotation: Rotation): Mp4Composer {
        this.rotation = rotation
        return this
    }

    override fun fillMode(fillMode: FillMode): Mp4Composer {
        this.fillMode = fillMode
        return this
    }

    fun customFillMode(fillModeCustomItem: FillModeCustomItem): Mp4Composer {
        this.fillModeCustomItem = fillModeCustomItem
        this.fillMode = FillMode.CUSTOM
        return this
    }

    override fun listener(listener: Listener): Mp4Composer {
        this.listener = listener
        return this
    }

    fun timeScale(timeScale: Int): Mp4Composer {
        this.timeScale = timeScale
        return this
    }

    private fun getExecutorService(): ExecutorService {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor()
        }
        return executorService!!
    }

    override fun start(): Mp4Composer {
        getExecutorService().execute(Runnable {
            val engine = Mp4ComposerEngine()

            engine.setProgressCallback(
                object : ProgressCallback {
                    override fun onProgress(progress: Double) {
                        listener?.onProgress(progress)
                    }
                }
            )

            if (filter == null) {
                filter = GlFilter()
            }

            if (fillModeCustomItem != null) {
                fillMode = FillMode.CUSTOM
            }

            // TODO: improve this and just treat both video and static image as srcPath, and use FileInputStream
            // instead of being passsed the bitmap object directly with specialized constructor
            // Mp4Composer(final Bitmap bkgBmp, final String destPath) (remove such constructor).
            if (!isStaticImageBkgSource) {
                initializeUriDataSource(engine)
                val videoRotate = getVideoRotation(srcUri!!)
                val srcVideoResolution = getVideoResolution(srcUri, DEFAULT_FALLBACK_VIDEO_RESOLUTION)

                if (outputResolution == null) {
                    if (fillMode == FillMode.CUSTOM) {
                        outputResolution = srcVideoResolution
                    } else {
                        val rotate = Rotation.fromInt(rotation.rotation + videoRotate)
                        if (rotate == Rotation.ROTATION_90 || rotate == Rotation.ROTATION_270) {
                            outputResolution = Size(srcVideoResolution.height, srcVideoResolution.width)
                        } else {
                            outputResolution = srcVideoResolution
                        }
                    }
                }

                if (timeScale < 2) {
                    timeScale = 1
                }

                Log.d(TAG, "rotation = " + (rotation.rotation + videoRotate))
                Log.d(
                    TAG,
                    "inputResolution width = " + srcVideoResolution.width + " height = " + srcVideoResolution.height
                )
                Log.d(
                    TAG,
                    "outputResolution width = " + outputResolution!!.width + " height = " + outputResolution!!.height
                )
                Log.d(TAG, "fillMode = " + fillMode)

                try {
                    if (bitrate < 0) {
                        bitrate = calcBitRate(outputResolution!!.width, outputResolution!!.height)
                    }
                    engine.composeFromVideoSource(
                        context,
                        destPath,
                        outputResolution!!,
                        filter!!,
                        bitrate,
                        mute,
                        Rotation.fromInt(rotation.rotation + videoRotate),
                        srcVideoResolution,
                        fillMode,
                        fillModeCustomItem,
                        timeScale,
                        flipVertical,
                        flipHorizontal
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (listener != null) {
                        listener!!.onFailed(e)
                    }
                    executorService!!.shutdown()
                    return@Runnable
                }
            } else {
                // FIXME hardcoded video output resolution
                outputResolution = DEFAULT_FALLBACK_VIDEO_RESOLUTION
                // outputResolution = new Size(640, 480);
                timeScale = 1
                bitrate = calcBitRate(outputResolution!!.width, outputResolution!!.height)
                try {
                    val staticImageResolution = Size(bkgBitmap!!.width, bkgBitmap!!.height)
                    engine.composeFromStaticImageSource(
                        bkgBitmap!!,
                        destPath,
                        outputResolution!!,
                        filter!!,
                        bitrate,
                        mute,
                        Rotation.fromInt(rotation.rotation), // FIXME assume portrait for now
                        staticImageResolution,
                        fillMode!!,
                        fillModeCustomItem!!,
                        timeScale,
                        flipVertical,
                        flipHorizontal
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (listener != null) {
                        listener!!.onFailed(e)
                    }
                    executorService!!.shutdown()
                    return@Runnable
                }
            }

            if (listener != null) {
                listener!!.onCompleted()
            }
            executorService!!.shutdown()
        })

        return this
    }

    fun cancel() {
        getExecutorService().shutdownNow()
    }

    private fun initializeUriDataSource(engine: Mp4ComposerEngine) {
        engine.setDataSource(srcUri, addedRequestHeaders)
    }

    private fun getVideoRotation(videoUri: Uri): Int {
        var mediaMetadataRetriever: MediaMetadataRetriever? = null
        try {
            mediaMetadataRetriever = MediaMetadataRetriever()
            videoUri?.let { uri ->
                context?.let {
                    DataSourceUtil.setDataSource(
                        it,
                        uri,
                        mediaExtractor = null,
                        mediaMetadataRetriever = mediaMetadataRetriever,
                        addedRequestHeaders = addedRequestHeaders
                    )
                }
            }
            val orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            return Integer.valueOf(orientation)
        } catch (e: IllegalArgumentException) {
            Log.e("MediaMetadataRetriever", "getVideoRotation IllegalArgumentException")
            return 0
        } catch (e: RuntimeException) {
            Log.e("MediaMetadataRetriever", "getVideoRotation RuntimeException")
            return 0
        } catch (e: Exception) {
            Log.e("MediaMetadataRetriever", "getVideoRotation Exception")
            return 0
        } finally {
            try {
                mediaMetadataRetriever?.release()
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to release mediaMetadataRetriever.", e)
            }
        }
    }

    private fun calcBitRate(width: Int, height: Int): Int {
        val bitrate = (0.25 * 30.0 * width.toDouble() * height.toDouble()).toInt()
        Log.i(TAG, "bitrate=$bitrate")
        return bitrate
    }

    private fun getVideoResolution(videoUri: Uri, defaultResolution: Size): Size {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            context?.let {
                DataSourceUtil.setDataSource(
                    it,
                    videoUri,
                    mediaExtractor = null,
                    mediaMetadataRetriever = retriever,
                    addedRequestHeaders = addedRequestHeaders
                )
            }

            val width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
            val height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))

            return Size(width, height)
        } catch (e: IllegalArgumentException) {
            Log.e("MediaMetadataRetriever", "getVideoResolution IllegalArgumentException")
            return defaultResolution
        } catch (e: RuntimeException) {
            Log.e("MediaMetadataRetriever", "getVideoResolution RuntimeException")
            return defaultResolution
        } catch (e: Exception) {
            Log.e("MediaMetadataRetriever", "getVideoResolution Exception")
            return defaultResolution
        } finally {
            try {
                retriever?.release()
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to release mediaMetadataRetriever.", e)
            }
        }
    }

    companion object {
        private val TAG = Mp4Composer::class.java.simpleName
        private val DEFAULT_FALLBACK_VIDEO_RESOLUTION = Size(480, 720)
    }
}
