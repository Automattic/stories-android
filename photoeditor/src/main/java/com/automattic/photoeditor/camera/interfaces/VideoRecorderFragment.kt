
package com.automattic.photoeditor.camera.interfaces

import androidx.fragment.app.Fragment
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import java.io.File
import kotlin.properties.Delegates

abstract class VideoRecorderFragment : Fragment(),
    VideoRecorderHandler,
    ImageCaptureHandler,
    CameraSelectionHandler,
    CameraFlashStateHandler,
    CameraFlashSupportQuery,
    SurfaceFragmentHandler {
    /**
     * An [AutoFitTextureView] for camera preview.
     */
    lateinit var textureView: AutoFitTextureView
    var currentFile: File? = null
    protected var active: Boolean = false
    protected var currentFlashState = CameraFlashStateKeeper()
    lateinit var flashSupportChangeListener: FlashSupportChangeListener
    protected var flashSupported: Boolean by Delegates.observable(
        initialValue = false,
        onChange = {
            prop, old, new -> flashSupportChangeListener.onFlashSupportChanged(new)
        }
    )

    interface FlashSupportChangeListener {
        fun onFlashSupportChanged(isSupported: Boolean)
    }

    override fun advanceFlashState() {
        currentFlashState.advanceFlashState()
    }

    override fun setFlashState(flashIndicatorState: FlashIndicatorState) {
        currentFlashState.setFlashState(flashIndicatorState)
    }

    override fun currentFlashState(): FlashIndicatorState {
        return currentFlashState.currentFlashState()
    }
}
