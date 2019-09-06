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

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import java.io.File
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.fragment.app.Fragment
import com.automattic.photoeditor.camera.interfaces.SurfaceFragmentHandler
import com.automattic.photoeditor.camera.interfaces.VideoPlayerSoundOnOffHandler
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import java.io.FileInputStream
import java.io.IOException
import android.media.MediaMetadataRetriever
import android.graphics.Matrix

class VideoPlayingBasicHandling : Fragment(), SurfaceFragmentHandler, VideoPlayerSoundOnOffHandler {
    // holds the File handle to the current video file to be played
    var currentFile: File? = null
    var currentExternalUri: Uri? = null

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            if (active) {
                startVideoPlay(texture)
            }
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            if (currentExternalUri != null && videoHeight > 0 && videoWidth > 0) {
                updateTextureViewSizeForCropping(width, height)
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
        windDown()
        super.onPause()
    }

    fun stopVideoPlay() {
        if (active) {
            if (mediaPlayer?.isPlaying() == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = null
        }
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
        active = false
    }

    private fun startUp() {
        if (textureView.isAvailable && active) {
            startVideoPlay(textureView.surfaceTexture)
        }
    }

    private fun windDown() {
        stopVideoPlay()
    }

    // WARNING: this will take currentFile and play it if not null, or take currentExternalUri and play it if available.
    // This means currentFile (local file, for videos that were just captured by the app) has precedence.
    fun startVideoPlay(texture: SurfaceTexture) {
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
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(inputStream.getFD())
                    setSurface(s)
                    setLooping(true)
                    prepare()
                    // TODO check whether we want fine grained error handling by setting these listeners
    //                setOnBufferingUpdateListener(this)
    //                setOnCompletionListener(this)
    //                setOnPreparedListener(this)
    //                setOnVideoSizeChangedListener(this)
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                    start()
                }
            }

            currentExternalUri?.let {
                textureView.setTransform(originalMatrix)
                calculateVideoSize(it)
                updateTextureViewSizeForCropping(textureView.measuredWidth, textureView.measuredHeight)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context!!, currentExternalUri!!)
                    setSurface(s)
                    setLooping(true)
                    prepare()
                    // TODO check whether we want fine grained error handling by setting these listeners
                    //                setOnBufferingUpdateListener(this)
                    //                setOnCompletionListener(this)
                    //                setOnPreparedListener(this)
                    //                setOnVideoSizeChangedListener(this)
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                    start()
                }
            }
        } catch (e: IllegalArgumentException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: SecurityException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    override fun mute() {
        mediaPlayer?.setVolume(0f, 0f)
    }

    override fun unmute() {
        mediaPlayer?.setVolume(1f, 1f)
    }

    private fun calculateVideoSize(videoUri: Uri) {
        val metadataRetriever = MediaMetadataRetriever()
        try {
            metadataRetriever.setDataSource(context, videoUri)
            val height = metadataRetriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val width = metadataRetriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            videoHeight = java.lang.Float.parseFloat(height)
            videoWidth = java.lang.Float.parseFloat(width)
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
