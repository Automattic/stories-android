
package com.automattic.photoeditor.camera.interfaces

import androidx.fragment.app.Fragment
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState.AUTO
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState.OFF
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState.ON
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import java.io.File

abstract class VideoRecorderFragment : Fragment(),
    VideoRecorderHandler,
    ImageCaptureHandler,
    CameraFlipHandler,
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
