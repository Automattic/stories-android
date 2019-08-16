
package com.automattic.photoeditor.camera.interfaces

interface CameraFlashStateHandler {
    fun advanceFlashState()
    fun setFlashState(flashIndicatorState: FlashIndicatorState)
    fun isFlashAvailable(): Boolean
    fun currentFlashState(): FlashIndicatorState
}

enum class FlashIndicatorState {
    ON, OFF, AUTO
}
