package com.automattic.photoeditor.state

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.automattic.photoeditor.camera.Camera2BasicHandling
import com.automattic.photoeditor.camera.CameraXBasicHandling
import com.automattic.photoeditor.camera.VideoPlayingBasicHandling
import com.automattic.photoeditor.state.BackgroundSurfaceManager.SurfaceHandlerType.CAMERA2
import com.automattic.photoeditor.state.BackgroundSurfaceManager.SurfaceHandlerType.CAMERAX
import com.automattic.photoeditor.state.BackgroundSurfaceManager.SurfaceHandlerType.VIDEOPLAYER
import com.automattic.photoeditor.views.PhotoEditorView
import java.io.File

class BackgroundSurfaceManager(
    private val activity: Activity,
    private val savedInstanceState: Bundle?,
    private val lifeCycle: Lifecycle,
    private val photoEditorView: PhotoEditorView,
    private val supportFragmentManager: FragmentManager,
    private val useCameraX: Boolean
) : LifecycleObserver {
    private lateinit var camera2BasicHandler: Camera2BasicHandling
    private lateinit var cameraXBasicHandler: CameraXBasicHandling
    private lateinit var videoPlayerHandling: VideoPlayingBasicHandling

    enum class SurfaceHandlerType {
        CAMERA2, CAMERAX, VIDEOPLAYER
    }

    // state flags
    private var isCameraVisible: Boolean = false
    private var isVideoPlayerVisible: Boolean = false
    private var isCameraRecording: Boolean = false

    @OnLifecycleEvent(ON_CREATE)
    fun onCreate(source: LifecycleOwner) {
        // clear surfaceTexture listeners
        photoEditorView.listeners.clear()
        getStateFromBundle()

        // add fragments
        if (useCameraX) {
            addHandlerFragmentOrFindByTag(CAMERAX)
        } else {
            addHandlerFragmentOrFindByTag(CAMERA2)
        }
        addHandlerFragmentOrFindByTag(VIDEOPLAYER)

        if (isCameraVisible || isVideoPlayerVisible) { photoEditorView.toggleTextureView() }
    }

    @OnLifecycleEvent(ON_DESTROY)
    fun onDestroy(source: LifecycleOwner) {
        if (lifeCycle.currentState.isAtLeast(Lifecycle.State.DESTROYED)) {
            // clear surfaceTexture listeners
            photoEditorView.listeners.clear()
        }
        // stop listening to events - should be safe to not remove it as per mentioned here
        // https://github.com/googlecodelabs/android-lifecycles/issues/5#issuecomment-303717013
        // but, removing it in ON_DESTROY just in case
        lifeCycle.removeObserver(this)
    }

    @OnLifecycleEvent(ON_START)
    fun onStart(source: LifecycleOwner) {
        // TODO: get state and restart fragments / camera preview?
    }
    @OnLifecycleEvent(ON_STOP)
    fun onStop(source: LifecycleOwner) {
        // TODO: save state and pause fragments / camera preview?
    }

    @OnLifecycleEvent(ON_RESUME)
    fun onResume(source: LifecycleOwner) {
        // TODO: get state and restart fragments / camera preview?
    }

    @OnLifecycleEvent(ON_PAUSE)
    fun onPause(source: LifecycleOwner) {
        // TODO: save state and pause fragments / camera preview?
    }

    fun saveStateToBundle(outState: Bundle?) {
        outState?.putBoolean(KEY_IS_CAMERA_VISIBLE, isCameraVisible)
        outState?.putBoolean(KEY_IS_VIDEO_PLAYER_VISIBLE, isVideoPlayerVisible)
        outState?.putBoolean(KEY_IS_CAMERA_RECORDING, isCameraRecording)
    }

    fun cameraVisible(): Boolean {
        return isCameraVisible
    }

    fun videoPlayerVisible(): Boolean {
        return isVideoPlayerVisible
    }

    fun cameraRecording(): Boolean {
        return isCameraRecording
    }

    fun switchStaticImageBackgroundModeOn() {
        isCameraVisible = false
        isVideoPlayerVisible = false
        if (useCameraX) {
            cameraXBasicHandler.deactivate()
        } else {
            camera2BasicHandler.deactivate()
        }
        videoPlayerHandling.deactivate()
        photoEditorView.turnTextureViewOff()
    }

    fun switchCameraPreviewOn() {
        if (isCameraVisible) {
            // camera preview is ON
            if (!isCameraRecording) {
                startRecordingVideo()
            } else {
                stopRecordingVideo()
            }
        } else {
            isCameraVisible = true
            isVideoPlayerVisible = false
            // now, start playing video
            photoEditorView.turnTextureViewOn()
            if (useCameraX) {
                cameraXBasicHandler.activate()
            } else {
                camera2BasicHandler.activate()
            }
            videoPlayerHandling.deactivate()
        }
    }

    fun switchVideoPlayerOn() {
        // in case the Camera was being visible, set if off
        isVideoPlayerVisible = true
        isCameraVisible = false
        photoEditorView.turnTextureViewOn()
        if (useCameraX) {
            cameraXBasicHandler.deactivate()
        } else {
            camera2BasicHandler.deactivate()
        }
        videoPlayerHandling.activate()
    }

    fun startRecordingVideo() {
        // let's start recording
        isCameraRecording = true
        // TODO txtRecording.visibility = View.VISIBLE
        if (useCameraX) {
            cameraXBasicHandler.startRecordingVideo()
        } else {
            camera2BasicHandler.startRecordingVideo()
        }
    }

    fun stopRecordingVideo() {
        // stop recording
        isCameraRecording = false
        // TODO txtRecording.visibility = View.GONE
        if (useCameraX) {
            cameraXBasicHandler.stopRecordingVideo()
        } else {
            camera2BasicHandler.stopRecordingVideo()
        }
    }

    fun getCurrentVideoFile(): File? {
        if (useCameraX) {
            return cameraXBasicHandler.currentFile
        } else {
            return camera2BasicHandler.currentFile
        }
    }

    private fun getStateFromBundle() {
        if (savedInstanceState != null) {
            isCameraVisible = savedInstanceState.getBoolean(KEY_IS_CAMERA_VISIBLE)
            isVideoPlayerVisible = savedInstanceState.getBoolean(KEY_IS_VIDEO_PLAYER_VISIBLE)
            isCameraRecording = savedInstanceState.getBoolean(KEY_IS_CAMERA_RECORDING)
        }
    }

    private fun addHandlerFragmentOrFindByTag(type: SurfaceHandlerType) {
        when (type) {
            CAMERAX -> {
                val cameraFragment = supportFragmentManager.findFragmentByTag(KEY_CAMERAX_HANDLING_FRAGMENT_TAG)
                if (cameraFragment == null) {
                    cameraXBasicHandler = CameraXBasicHandling.getInstance(photoEditorView.textureView)
                    supportFragmentManager
                        .beginTransaction().add(cameraXBasicHandler, KEY_CAMERAX_HANDLING_FRAGMENT_TAG).commit()
                } else {
                    // get the existing cameraXBasicHandler object reference in this new Activity instance
                    cameraXBasicHandler = cameraFragment as CameraXBasicHandling
                    // the photoEditorView layout has been recreated so, re-assign its TextureView
                    cameraXBasicHandler.textureView = photoEditorView.textureView
                }
            }
            CAMERA2 -> {
                // ask FragmentManager to add the headless fragment so it receives the Activity's lifecycle callback calls
                val cameraFragment = supportFragmentManager.findFragmentByTag(KEY_CAMERA2_HANDLING_FRAGMENT_TAG)
                if (cameraFragment == null) {
                    camera2BasicHandler = Camera2BasicHandling.getInstance(photoEditorView.textureView)
                    supportFragmentManager
                        .beginTransaction().add(camera2BasicHandler, KEY_CAMERA2_HANDLING_FRAGMENT_TAG).commit()
                } else {
                    // get the existing camera2BasicHandler object reference in this new Activity instance
                    camera2BasicHandler = cameraFragment as Camera2BasicHandling
                    // the photoEditorView layout has been recreated so, re-assign its TextureView
                    camera2BasicHandler.textureView = photoEditorView.textureView
                }
                // add camera handling texture listener
                photoEditorView.listeners.add(camera2BasicHandler.surfaceTextureListener)
            }
            VIDEOPLAYER -> {
                // ask FragmentManager to add the headless fragment so it receives the Activity's lifecycle callback calls
                val videoPlayerFragment =
                    supportFragmentManager.findFragmentByTag(KEY_VIDEOPLAYER_HANDLING_FRAGMENT_TAG)
                if (videoPlayerFragment == null) {
                    videoPlayerHandling = VideoPlayingBasicHandling.getInstance(photoEditorView.textureView)
                    supportFragmentManager
                        .beginTransaction().add(videoPlayerHandling, KEY_VIDEOPLAYER_HANDLING_FRAGMENT_TAG).commit()
                } else {
                    // get the existing VideoPlayingBasicHandling object reference in this new Activity instance
                    videoPlayerHandling = videoPlayerFragment as VideoPlayingBasicHandling
                    // the photoEditorView layout has been recreated so, re-assign its TextureView
                    videoPlayerHandling.textureView = photoEditorView.textureView
                }
                // add video player texture listener
                photoEditorView.listeners.add(videoPlayerHandling.surfaceTextureListener)
            }
        }
    }

    companion object {
        private const val KEY_CAMERA2_HANDLING_FRAGMENT_TAG = "CAMERA2_TAG"
        private const val KEY_CAMERAX_HANDLING_FRAGMENT_TAG = "CAMERAX_TAG"
        private const val KEY_IS_CAMERA_VISIBLE = "key_is_camera_visible"
        private const val KEY_VIDEOPLAYER_HANDLING_FRAGMENT_TAG = "VIDEOPLAYER_TAG"
        private const val KEY_IS_VIDEO_PLAYER_VISIBLE = "key_is_video_player_visible"
        private const val KEY_IS_CAMERA_RECORDING = "key_is_camera_recording"
    }
}
