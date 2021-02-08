
package com.automattic.photoeditor.camera.interfaces

// import androidx.camera.core.FlashMode
import androidx.camera.core.ImageCapture
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState.AUTO
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState.OFF
import com.automattic.photoeditor.camera.interfaces.FlashIndicatorState.ON

interface CameraFlashStateHandler {
    fun advanceFlashState()
    fun setFlashState(flashIndicatorState: FlashIndicatorState)
    fun currentFlashState(): FlashIndicatorState
}

interface CameraFlashSupportQuery {
    fun isFlashAvailable(): Boolean
}

enum class FlashIndicatorState(val id: Int) {
    OFF(0),
    AUTO(1),
    ON(2);

    companion object {
        fun valueOf(value: Int): FlashIndicatorState? = values().find { it.id == value }
    }
}

class CameraFlashStateKeeper : CameraFlashStateHandler {
    private var flashState = OFF
    override fun advanceFlashState() {
        flashState = when (flashState) {
            AUTO -> ON
            ON -> OFF
            OFF -> AUTO
        }
    }

    override fun setFlashState(flashIndicatorState: FlashIndicatorState) {
        flashState = flashIndicatorState
    }

    override fun currentFlashState(): FlashIndicatorState {
        return flashState
    }
}

// helper method to get CameraX flash mode from CameraFlashStateHandler.FlashIndicatorState enum
fun cameraXflashModeFromStoriesFlashState(flashIndicatorState: FlashIndicatorState): Int {
    return when (flashIndicatorState) {
        AUTO -> ImageCapture.FLASH_MODE_AUTO
        ON -> ImageCapture.FLASH_MODE_ON
        OFF -> ImageCapture.FLASH_MODE_OFF
    }
}

// helper method to get Camera2 flash mode from CameraFlashStateHandler.FlashIndicatorState enum
fun camera2flashModeFromStoriesFlashState(flashIndicatorState: FlashIndicatorState): Int {
    return when (flashIndicatorState) {
        AUTO -> ImageCapture.FLASH_MODE_AUTO
        ON -> ImageCapture.FLASH_MODE_ON
        OFF -> ImageCapture.FLASH_MODE_OFF
    }
}
