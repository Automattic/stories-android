package com.automattic.photoeditor.state

import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.TextureView
import androidx.fragment.app.DialogFragment
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
import com.automattic.photoeditor.R
import com.automattic.photoeditor.camera.Camera2BasicHandling
import com.automattic.photoeditor.camera.CameraXBasicHandling
import com.automattic.photoeditor.camera.ErrorDialog
import com.automattic.photoeditor.camera.ErrorDialogOk
import com.automattic.photoeditor.camera.PlayerPreparedListener
import com.automattic.photoeditor.camera.VideoPlayingBasicHandling
import com.automattic.photoeditor.camera.interfaces.CameraSelection
import com.automattic.photoeditor.camera.interfaces.CameraSelection.BACK
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState
import com.automattic.photoeditor.camera.interfaces.ImageCaptureListener
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFinished
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFragment
import com.automattic.photoeditor.camera.interfaces.VideoRecorderFragment.FlashSupportChangeListener
import com.automattic.photoeditor.state.BackgroundSurfaceManager.SurfaceHandlerType.CAMERA2
import com.automattic.photoeditor.state.BackgroundSurfaceManager.SurfaceHandlerType.CAMERAX
import com.automattic.photoeditor.state.BackgroundSurfaceManager.SurfaceHandlerType.VIDEOPLAYER
import com.automattic.photoeditor.views.PhotoEditorView
import java.io.File

interface AuthenticationHeadersInterface {
    fun getAuthHeaders(url: String): Map<String, String>?
}

interface BackgroundSurfaceManagerReadyListener {
    fun onBackgroundSurfaceManagerReady()
}

class BackgroundSurfaceManager(
    private val savedInstanceState: Bundle?,
    private val lifeCycle: Lifecycle,
    private val photoEditorView: PhotoEditorView,
    private val supportFragmentManager: FragmentManager,
    private val flashSupportChangeListener: FlashSupportChangeListener,
    private val useCameraX: Boolean,
    private val managerReadyListener: BackgroundSurfaceManagerReadyListener? = null,
    private val authenticationHeadersInterface: AuthenticationHeadersInterface? = null
) : LifecycleObserver {
    private lateinit var cameraBasicHandler: VideoRecorderFragment
    private lateinit var videoPlayerHandling: VideoPlayingBasicHandling

    enum class SurfaceHandlerType {
        CAMERA2, CAMERAX, VIDEOPLAYER
    }

    // state flags
    private var isCameraVisible: Boolean = false
    private var isVideoPlayerVisible: Boolean = false
    private var isCameraRecording: Boolean = false

    @Suppress("unused")
    @OnLifecycleEvent(ON_CREATE)
    fun onCreate(source: LifecycleOwner) {
        // clear surfaceTexture listeners
        photoEditorView.listeners.clear()

        // add f
        // ragments
        if (useCameraX) {
            addHandlerFragmentOrFindByTag(CAMERAX)
        } else {
            addHandlerFragmentOrFindByTag(CAMERA2)
        }
        addHandlerFragmentOrFindByTag(VIDEOPLAYER)

        // important: only retrieve state after having restored fragments with addHandlerFragmentOrFindByTag as above
        getStateFromBundle()

        // add general BackgroundSurfaceManager's surfaceTextureListener
        managerReadyListener?.let {
            photoEditorView.listeners.add(
                    object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                            it.onBackgroundSurfaceManagerReady()
                        }
                        override
                        fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit

                        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

                        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                    }
            )
        }
        if (isCameraVisible || isVideoPlayerVisible) { photoEditorView.toggleTextureView() }
    }

    @Suppress("unused")
    @OnLifecycleEvent(ON_DESTROY)
    fun onDestroy(source: LifecycleOwner) {
        if (lifeCycle.currentState.isAtLeast(Lifecycle.State.DESTROYED)) {
            cameraBasicHandler.deactivate()
            videoPlayerHandling.deactivate()
            photoEditorView.hideLoading()
            // clear surfaceTexture listeners
            photoEditorView.listeners.clear()
        }
        // stop listening to events - should be safe to not remove it as per mentioned here
        // https://github.com/googlecodelabs/android-lifecycles/issues/5#issuecomment-303717013
        // but, removing it in ON_DESTROY just in case
        lifeCycle.removeObserver(this)
    }

    @Suppress("unused")
    @OnLifecycleEvent(ON_START)
    fun onStart(source: LifecycleOwner) {
        // TODO: get state and restart fragments / camera preview?
    }

    @Suppress("unused")
    @OnLifecycleEvent(ON_STOP)
    fun onStop(source: LifecycleOwner) {
        // TODO: save state and pause fragments / camera preview?
    }

    @Suppress("unused")
    @OnLifecycleEvent(ON_RESUME)
    fun onResume(source: LifecycleOwner) {
        // TODO: get state and restart fragments / camera preview?
    }

    @Suppress("unused")
    @OnLifecycleEvent(ON_PAUSE)
    fun onPause(source: LifecycleOwner) {
        // TODO: save state and pause fragments / camera preview?
    }

    fun saveStateToBundle(outState: Bundle?) {
        outState?.putBoolean(KEY_IS_CAMERA_VISIBLE, isCameraVisible)
        outState?.putBoolean(KEY_IS_VIDEO_PLAYER_VISIBLE, isVideoPlayerVisible)
        outState?.putBoolean(KEY_IS_CAMERA_RECORDING, isCameraRecording)
        outState?.putInt(KEY_CAMERA_SELECTION, cameraBasicHandler.currentCamera().id)
        outState?.putInt(KEY_FLASH_MODE_SELECTION, cameraBasicHandler.currentFlashState().id)
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
        if (isCameraRecording) {
            stopRecordingVideo()
        }
        isCameraVisible = false
        if (isVideoPlayerVisible) {
            isVideoPlayerVisible = false
            videoPlayerHandling.deactivate()
        }
        photoEditorView.hideLoading()
        photoEditorView.turnTextureViewOff()
    }

    fun preTurnTextureViewOn() {
        photoEditorView.turnTextureViewOn()
    }

    fun switchCameraPreviewOn() {
        isCameraVisible = true
        // now, start showing camera preview
        photoEditorView.turnTextureViewOn()
        if (isVideoPlayerVisible) {
            isVideoPlayerVisible = false
            videoPlayerHandling.deactivate()
        }
        photoEditorView.hideLoading()
        cameraBasicHandler.activate()
    }

    fun flipCamera(): CameraSelection {
        if (isCameraVisible) {
            return cameraBasicHandler.flipCamera()
        }
        return BACK // default
    }

    fun selectCamera(cameraSelection: CameraSelection) {
        cameraBasicHandler.selectCamera(cameraSelection)
    }

    fun switchFlashState(): FlashIndicatorState {
        if (isCameraVisible) {
            cameraBasicHandler.advanceFlashState()
        }
        return cameraBasicHandler.currentFlashState()
    }

    fun setFlashState(flashIndicatorState: FlashIndicatorState) {
        cameraBasicHandler.setFlashState(flashIndicatorState)
    }

    fun isFlashAvailable(): Boolean {
        return cameraBasicHandler.isFlashAvailable()
    }

    fun switchVideoPlayerOnFromFile(videoFile: File? = null) {
        // if coming from Activity restart, use the passed parameter
        if (videoFile != null) {
            videoPlayerHandling.currentFile = videoFile
            cameraBasicHandler.currentFile = videoFile
            videoPlayerHandling.currentExternalUri = null
        } else {
            videoPlayerHandling.currentFile = cameraBasicHandler.currentFile
            videoPlayerHandling.currentExternalUri = null
        }
        switchVideoPlayerOn()
    }

    fun switchVideoPlayerOnFromUri(videoUri: Uri) {
        // if coming from Activity restart, use the passed parameter
        videoPlayerHandling.currentExternalUri = videoUri
        videoPlayerHandling.currentExternalUriHeaders =
                authenticationHeadersInterface?.getAuthHeaders(videoUri.toString())
        videoPlayerHandling.currentFile = null
        cameraBasicHandler.currentFile = null
        switchVideoPlayerOn()
    }

    private fun switchVideoPlayerOn() {
        // in case the Camera was being visible, set if off
        isVideoPlayerVisible = true
        photoEditorView.showLoading()
        if (isCameraVisible) {
            isCameraVisible = false
            if (isCameraRecording) {
                stopRecordingVideo()
            }

            // if the camera was visible (either for preview or recording) before switching to video player,
            // we need to give a bit of time before changing the surface as the video stops recording,
            // saves to file and the surface gets deactivated and activated again.
            // This is to circumvent an issue in CameraX that should be solved in the beta version.
            // TODO: implement this in the saveFile listener so we're sure to only change to the option
            // wanted (video player) once we're sure video has been successfully saved
            val handler = Handler()
            handler.postDelayed({
                videoPlayerHandling.currentFile = cameraBasicHandler.currentFile
                doDeactivateReactivateSurfaceAndPlay()
            }, 500)
            return
        }
        doDeactivateReactivateSurfaceAndPlay()
    }

    private fun doDeactivateReactivateSurfaceAndPlay() {
        cameraXAwareSurfaceDeactivate()
        photoEditorView.turnTextureViewOn()
        videoPlayerHandling.activate()
    }

    fun videoPlayerMute() {
        if (isVideoPlayerVisible) {
            videoPlayerHandling.mute()
        }
    }

    fun videoPlayerUnmute() {
        if (isVideoPlayerVisible) {
            videoPlayerHandling.unmute()
        }
    }

    private fun cameraXAwareSurfaceDeactivate() {
        if (cameraBasicHandler.isActive()) {
            cameraBasicHandler.deactivate()
        }

        if (useCameraX) {
            // IMPORTANT: remove and add the TextureView back again to the view hierarchy so the SurfaceTexture
            // is available for reuse by other fragments (i.e. VideoPlayingBasicHandler)
            photoEditorView.removeAndAddTextureViewBack()
        }
    }

    fun startRecordingVideo(finishedListener: VideoRecorderFinished? = null) {
        if (isCameraVisible) {
            // let's start recording
            isCameraRecording = true
            cameraBasicHandler.startRecordingVideo(finishedListener)
        }
    }

    fun stopRecordingVideo() {
        if (isCameraRecording) {
            // stop recording
            isCameraRecording = false
            cameraBasicHandler.stopRecordingVideo()
        }
    }

    fun takePicture(listener: ImageCaptureListener) {
        cameraBasicHandler.takePicture(listener)
    }

    fun getCurrentFile(): File? {
        return cameraBasicHandler.currentFile
    }

    fun getCurrentBackgroundMedia(): Uri? {
        if (videoPlayerHandling.currentExternalUri != null) {
            return videoPlayerHandling.currentExternalUri
        } else {
            return Uri.parse(cameraBasicHandler.currentFile.toString())
        }
    }

    private fun getStateFromBundle() {
        if (savedInstanceState != null) {
            isCameraVisible = savedInstanceState.getBoolean(KEY_IS_CAMERA_VISIBLE)
            isVideoPlayerVisible = savedInstanceState.getBoolean(KEY_IS_VIDEO_PLAYER_VISIBLE)
            isCameraRecording = savedInstanceState.getBoolean(KEY_IS_CAMERA_RECORDING)
            CameraSelection.valueOf(savedInstanceState.getInt(KEY_CAMERA_SELECTION))?.let {
                cameraBasicHandler.selectCamera(it)
            }
            FlashIndicatorState.valueOf(savedInstanceState.getInt(KEY_FLASH_MODE_SELECTION))?.let {
                cameraBasicHandler.setFlashState(it)
            }
        }
    }

    private fun addHandlerFragmentOrFindByTag(type: SurfaceHandlerType) {
        when (type) {
            CAMERAX -> {
                val cameraFragment = supportFragmentManager.findFragmentByTag(KEY_CAMERA_HANDLING_FRAGMENT_TAG)
                if (cameraFragment == null) {
                    cameraBasicHandler = CameraXBasicHandling.getInstance(photoEditorView.textureView,
                        flashSupportChangeListener)
                    supportFragmentManager
                        .beginTransaction().add(cameraBasicHandler, KEY_CAMERA_HANDLING_FRAGMENT_TAG).commit()
                } else {
                    // get the existing cameraXBasicHandler object reference in this new Activity instance
                    cameraBasicHandler = cameraFragment as CameraXBasicHandling
                    // the photoEditorView layout has been recreated so, re-assign its TextureView
                    cameraBasicHandler.textureView = photoEditorView.textureView
                    cameraBasicHandler.flashSupportChangeListener = flashSupportChangeListener
                }
            }
            CAMERA2 -> {
                // ask FragmentManager to add the headless fragment so it receives the Activity's lifecycle callback calls
                val cameraFragment = supportFragmentManager.findFragmentByTag(KEY_CAMERA_HANDLING_FRAGMENT_TAG)
                if (cameraFragment == null) {
                    cameraBasicHandler = Camera2BasicHandling.getInstance(photoEditorView.textureView,
                        flashSupportChangeListener)
                    supportFragmentManager
                        .beginTransaction().add(cameraBasicHandler, KEY_CAMERA_HANDLING_FRAGMENT_TAG).commit()
                } else {
                    // get the existing camera2BasicHandler object reference in this new Activity instance
                    cameraBasicHandler = cameraFragment as Camera2BasicHandling
                    // the photoEditorView layout has been recreated so, re-assign its TextureView
                    cameraBasicHandler.textureView = photoEditorView.textureView
                    cameraBasicHandler.flashSupportChangeListener = flashSupportChangeListener
                }
                // add camera handling texture listener
                photoEditorView.listeners.add((cameraBasicHandler as Camera2BasicHandling).surfaceTextureListener)
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
                    videoPlayerHandling.originalMatrix = photoEditorView.textureView.getTransform(null)
                }

                videoPlayerHandling.playerPreparedListener = object : PlayerPreparedListener {
                    override fun onPlayerPrepared() {
                        photoEditorView.hideLoading()
                    }

                    override fun onPlayerError() {
                        photoEditorView.hideLoading()
                        ErrorDialog.newInstance(requireNotNull(videoPlayerHandling.context)
                                .getString(R.string.toast_error_playing_video),
                                    object : ErrorDialogOk {
                                        override fun OnOkClicked(dialog: DialogFragment) {
                                            dialog.dismiss()
                                        }
                                    }
                                ).show(supportFragmentManager,
                                        FRAGMENT_DIALOG
                                )
                    }
                }

                videoPlayerHandling.mAuthenticationHeadersInterface = authenticationHeadersInterface

                // add video player texture listener
                photoEditorView.listeners.add(videoPlayerHandling.surfaceTextureListener)
            }
        }
    }

    companion object {
        private const val KEY_CAMERA_HANDLING_FRAGMENT_TAG = "CAMERA_TAG"
        private const val KEY_IS_CAMERA_VISIBLE = "key_is_camera_visible"
        private const val KEY_VIDEOPLAYER_HANDLING_FRAGMENT_TAG = "VIDEOPLAYER_TAG"
        private const val KEY_IS_VIDEO_PLAYER_VISIBLE = "key_is_video_player_visible"
        private const val KEY_IS_CAMERA_RECORDING = "key_is_camera_recording"
        private const val KEY_CAMERA_SELECTION = "key_camera_selection"
        private const val KEY_FLASH_MODE_SELECTION = "key_flash_mode_selection"
        private const val FRAGMENT_DIALOG = "fragment_dialog"
    }
}
