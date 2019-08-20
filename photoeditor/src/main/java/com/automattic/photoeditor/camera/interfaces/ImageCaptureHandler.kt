
package com.automattic.photoeditor.camera.interfaces

import java.io.File

interface ImageCaptureHandler {
    fun takePicture(onImageCapturedListener: ImageCaptureListener)
}

interface ImageCaptureListener {
    fun onImageSaved(file: File)
    fun onError(
        message: String,
        cause: Throwable?
    )
}
