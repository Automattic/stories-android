
package com.automattic.photoeditor.camera.interfaces

interface CameraFlashStateHandler {
    fun advanceFlashState()
    fun setFlashState(flashState: FlashState)
    fun isFlashAvailable(): Boolean
    fun currentFlashState(): FlashState
}

enum class FlashState {
    ON, OFF, AUTO
}
