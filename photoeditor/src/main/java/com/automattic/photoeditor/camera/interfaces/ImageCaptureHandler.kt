
package com.automattic.photoeditor.camera.interfaces

import android.net.Uri

interface ImageCaptureHandler {
    fun takePicture(onImageCapturedListener: ImageCaptureListener)
}

interface ImageCaptureListener {
    fun onImageSaved(uri: Uri?)
    fun onError(
        message: String,
        cause: Throwable?
    )
}
