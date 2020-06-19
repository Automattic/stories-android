/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.automattic.photoeditor.camera

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.webkit.URLUtil
import androidx.fragment.app.Fragment
import com.automattic.photoeditor.camera.interfaces.SurfaceFragmentHandler
import com.automattic.photoeditor.camera.interfaces.VideoPlayerSoundOnOffHandler
import com.automattic.photoeditor.state.AuthenticationHeadersInterface
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

interface PlayerPrepareReadyListener {
    fun onPlayerPrepared()
    fun onPlayerError()
}

class VideoPlayingBasicHandling : Fragment(), SurfaceFragmentHandler, VideoPlayerSoundOnOffHandler {
    // holds the File handle to the current video file to be played
    var currentFile: File? = null
    var currentExternalUri: Uri? = null
    var currentExternalUriHeaders: Map<String, String>? = null
    var isMuted = false
    var playerPreparedListener: PlayerPrepareReadyListener? = null
    var mAuthenticationHeadersInterface: AuthenticationHeadersInterface? = null

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            if (active) {
                CoroutineScope(Dispatchers.Main).launch {
                    startVideoPlay(texture)
                }
            }
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            if (currentExternalUri != null && videoHeight > 0 && videoWidth > 0) {
//                updateTextureViewSizeForCropping(width, height)
                updateTextureViewSizeForLetterbox(videoWidth.toInt(), videoHeight.toInt())
            }
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    }

    /**
     * An [AutoFitTextureView] for camera preview.
     */
    lateinit var textureView: AutoFitTextureView
    lateinit var originalMatrix: Matrix

    private var active: Boolean = false

    private var mediaPlayer: MediaPlayer? = null

    private var videoWidth: Float = 0f
    private var videoHeight: Float = 0f
    private var videoOrientation: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onResume() {
        super.onResume()
        startUp()
    }

    override fun onPause() {
        // pause playing video
        deactivate()
        super.onPause()
    }

    private fun stopVideoPlay() {
        if (active) {
            if (mediaPlayer?.isPlaying() == true) {
                mediaPlayer?.stop()
            }
        }
        mediaPlayer?.setDisplay(null)
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
        active = false
        // leave the transform for reusable TextureView as per the original
        textureView.setTransform(originalMatrix)
    }

    override fun activate() {
        active = true
        // perform all needed tasks to make Video Player up
        startUp()
    }

    override fun deactivate() {
        stopVideoPlay()
    }

    private fun startUp() {
        if (textureView.isAvailable && active) {
            CoroutineScope(Dispatchers.Main).launch {
                startVideoPlay(textureView.surfaceTexture)
            }
        }
    }

    // WARNING: this will take currentFile and play it if not null, or take currentExternalUri and play it if available.
    // This means currentFile (local file, for videos that were just captured by the app) has precedence.
    suspend fun startVideoPlay(texture: SurfaceTexture) {
        val s = Surface(texture)
        try {
            if (mediaPlayer != null) {
                stopVideoPlay()
            }

            if (currentFile != null && currentExternalUri != null) {
                throw Exception("Can't have both currentFile and currentExternalUri play together")
            }

            currentFile?.takeIf { it.exists() }?.let { file ->
                val inputStream = FileInputStream(file)
                textureView.setTransform(originalMatrix)

                mediaPlayer = MediaPlayer()
                withContext(Dispatchers.IO) {
                    mediaPlayer?.setDataSource(inputStream.getFD())
                }

                mediaPlayer?.apply {
                    setSurface(s)
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                    setLooping(true)
                    setOnPreparedListener {
                        playerPreparedListener?.onPlayerPrepared()
                        it.start()
                    }
                    prepareAsync()
                    setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                }
            }

            currentExternalUri?.let { uri ->
                textureView.setTransform(originalMatrix)

                mediaPlayer = MediaPlayer()
                withContext(Dispatchers.IO) {
                    calculateVideoSizeAndOrientation(uri)
                    mAuthenticationHeadersInterface?.let {
                        mediaPlayer?.setDataSource(requireContext(),
                                currentExternalUri!!,
                                it.getAuthHeaders(uri.toString()))
                    } ?: mediaPlayer?.setDataSource(requireContext(), currentExternalUri!!)
                }

                // only use letterbox for landscape video
                if (videoOrientation == 0 || videoOrientation == 180) {
                    updateTextureViewSizeForLetterbox(videoWidth.toInt(), videoHeight.toInt())
                }

                mediaPlayer?.apply {
                    setSurface(s)
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                    setLooping(true)
                    setOnPreparedListener {
                        playerPreparedListener?.onPlayerPrepared()
                        it.start()
                    }
                    prepareAsync()
                    // TODO check whether we want fine grained error handling by setting these listeners
                    //                setOnBufferingUpdateListener(this)
                    //                setOnCompletionListener(this)
                    //                setOnPreparedListener(this)
                    //                setOnVideoSizeChangedListener(this)
                    setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                }
            }
        } catch (e: IllegalArgumentException) {
            playerPreparedListener?.onPlayerError()
            e.printStackTrace()
        } catch (e: SecurityException) {
            playerPreparedListener?.onPlayerError()
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            playerPreparedListener?.onPlayerError()
            e.printStackTrace()
        } catch (e: IOException) {
            playerPreparedListener?.onPlayerError()
            e.printStackTrace()
        }
    }

    override fun mute() {
        mediaPlayer?.setVolume(0f, 0f)
        isMuted = true
    }

    override fun unmute() {
        mediaPlayer?.setVolume(1f, 1f)
        isMuted = false
    }

    private fun calculateVideoSizeAndOrientation(videoUri: Uri) {
        val metadataRetriever = MediaMetadataRetriever()
        try {
            val isNetworkUrl = URLUtil.isNetworkUrl(videoUri.toString())
            if (!isNetworkUrl) {
                metadataRetriever.setDataSource(context, videoUri)
            } else {
                mAuthenticationHeadersInterface?.let {
                    metadataRetriever.setDataSource(videoUri.toString(), it.getAuthHeaders(videoUri.toString()))
                } ?: metadataRetriever.setDataSource(videoUri.toString(), HashMap<String, String>())
            }
            val height = metadataRetriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val width = metadataRetriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            var rotation = Integer.valueOf(metadataRetriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION))
            videoHeight = java.lang.Float.parseFloat(height)
            videoWidth = java.lang.Float.parseFloat(width)
            videoOrientation = rotation
        } catch (e: IOException) {
            Log.d(TAG, e.message)
        } catch (e: NumberFormatException) {
            Log.d(TAG, e.message)
        } finally {
            metadataRetriever.release()
        }
    }

    private fun updateTextureViewSizeForCropping(viewWidth: Int, viewHeight: Int) {
        var scaleX = 1.0f
        var scaleY = 1.0f

        if (videoWidth > viewWidth && videoHeight > viewHeight) {
            scaleX = videoWidth / viewWidth
            scaleY = videoHeight / viewHeight
        } else if (videoWidth < viewWidth && videoHeight < viewHeight) {
            scaleY = viewWidth / videoWidth
            scaleX = viewHeight / videoHeight
        } else if (viewWidth > videoWidth) {
            scaleY = viewWidth / videoWidth / (viewHeight / videoHeight)
        } else if (viewHeight > videoHeight) {
            scaleX = viewHeight / videoHeight / (viewWidth / videoWidth)
        }

        // pivot on center
        val pivotPointX = viewWidth / 2
        val pivotPointY = viewHeight / 2

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, pivotPointX.toFloat(), pivotPointY.toFloat())

        textureView.setTransform(matrix)
    }

    private fun updateTextureViewSizeForLetterbox(videoWidth: Int, videoHeight: Int) {
        val viewWidth = textureView.getWidth()
        val viewHeight = textureView.getHeight()
        val aspectRatio = videoHeight.toDouble() / videoWidth

        val newWidth: Int
        val newHeight: Int
        if (viewHeight > (viewWidth * aspectRatio).toInt()) {
            // limited by narrow width; restrict height
            newWidth = viewWidth
            newHeight = (viewWidth * aspectRatio).toInt()
        } else {
            // limited by short height; restrict width
            newWidth = (viewHeight / aspectRatio).toInt()
            newHeight = viewHeight
        }
        val xoff = (viewWidth - newWidth) / 2
        val yoff = (viewHeight - newHeight) / 2
        Log.v(
            TAG, "video=" + videoWidth + "x" + videoHeight +
                    " view=" + viewWidth + "x" + viewHeight +
                    " newView=" + newWidth + "x" + newHeight +
                    " off=" + xoff + "," + yoff
        )

        val txform = Matrix()
        textureView.getTransform(txform)
        txform.setScale(newWidth.toFloat() / viewWidth, newHeight.toFloat() / viewHeight)
        txform.postTranslate(xoff.toFloat(), yoff.toFloat())
        textureView.setTransform(txform)
    }

    companion object {
        private val instance = VideoPlayingBasicHandling()

        /**
         * Tag for the [Log].
         */
        private val TAG = "VideoPlayingBasic"

        @JvmStatic fun getInstance(textureView: AutoFitTextureView): VideoPlayingBasicHandling {
            instance.textureView = textureView
            instance.originalMatrix = textureView.getTransform(null)
            return instance
        }
    }
}
