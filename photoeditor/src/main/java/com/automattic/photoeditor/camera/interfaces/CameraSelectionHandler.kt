
package com.automattic.photoeditor.camera.interfaces

import android.hardware.camera2.CameraMetadata
import androidx.camera.core.CameraX.LensFacing

interface CameraSelectionHandler {
    fun flipCamera(): CameraSelection
    fun selectCamera(camera: CameraSelection)
    fun currentCamera(): CameraSelection
}

enum class CameraSelection(val id: Int) {
    BACK(0),
    FRONT(1);

    companion object {
        fun valueOf(value: Int): CameraSelection? = values().find { it.id == value }
    }
}

// helper method to get CameraX lens facing from stories' CameraSelection
fun cameraXLensFacingFromStoriesCameraSelection(cameraSelection: CameraSelection): LensFacing {
    return when (cameraSelection) {
        CameraSelection.BACK -> LensFacing.BACK
        CameraSelection.FRONT -> LensFacing.FRONT
    }
}

// helper method to get stories' CameraSelection from CameraX lens facing
fun storiesCameraSelectionFromCameraXLensFacing(lensFacing: LensFacing): CameraSelection {
    return when (lensFacing) {
        LensFacing.BACK -> CameraSelection.BACK
        LensFacing.FRONT -> CameraSelection.FRONT
    }
}

// helper method to get Camera2 lens facing from stories' CameraSelection
fun camera2LensFacingFromStoriesCameraSelection(cameraSelection: CameraSelection): Int {
    return when (cameraSelection) {
        CameraSelection.BACK -> CameraMetadata.LENS_FACING_BACK
        CameraSelection.FRONT -> CameraMetadata.LENS_FACING_FRONT
    }
}

// helper method to get stories' CameraSelection from Camera2 lens facing
fun storiesCameraSelectionFromCamera2LensFacing(lensfacing: Int): CameraSelection {
    return when (lensfacing) {
        CameraMetadata.LENS_FACING_BACK -> CameraSelection.BACK
        CameraMetadata.LENS_FACING_FRONT -> CameraSelection.FRONT
        else -> CameraSelection.BACK // default to back facing camera
    }
}
