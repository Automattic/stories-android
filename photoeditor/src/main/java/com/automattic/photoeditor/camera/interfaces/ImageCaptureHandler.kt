
package com.automattic.photoeditor.camera.interfaces

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.io.File

interface ImageCaptureHandler {
    fun takePicture(onImageCapturedListener: ImageCaptureListener)
}

interface ImageCaptureListener {
    fun onImageSaved(@NonNull file: File)
    fun onError(
        @NonNull message: String,
        @Nullable cause: Throwable?
    )
}
