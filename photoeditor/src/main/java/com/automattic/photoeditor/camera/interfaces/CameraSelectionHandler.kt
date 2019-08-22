
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

// helper method to get CameraX lens facing from portkey's CameraSelection
fun cameraXLensFacingFromPortkeyCameraSelection(cameraSelection: CameraSelection): LensFacing {
    return when (cameraSelection) {
        CameraSelection.BACK -> LensFacing.BACK
        CameraSelection.FRONT -> LensFacing.FRONT
    }
}

// helper method to get portkey's CameraSelection from CameraX lens facing
fun portkeyCameraSelectionFromCameraXLensFacing(lensFacing: LensFacing): CameraSelection {
    return when (lensFacing) {
        LensFacing.BACK -> CameraSelection.BACK
        LensFacing.FRONT -> CameraSelection.FRONT
    }
}

// helper method to get Camera2 lens facing from portkey's CameraSelection
fun camera2LensFacingFromPortkeyCameraSelection(cameraSelection: CameraSelection): Int {
    return when (cameraSelection) {
        CameraSelection.BACK -> CameraMetadata.LENS_FACING_BACK
        CameraSelection.FRONT -> CameraMetadata.LENS_FACING_FRONT
    }
}

// helper method to get portkey's CameraSelection from Camera2 lens facing
fun portkeyCameraSelectionFromCamera2LensFacing(lensfacing: Int): CameraSelection {
    return when (lensfacing) {
        CameraMetadata.LENS_FACING_BACK -> CameraSelection.BACK
        CameraMetadata.LENS_FACING_FRONT -> CameraSelection.FRONT
        else -> CameraSelection.BACK // default to back facing camera
    }
}
