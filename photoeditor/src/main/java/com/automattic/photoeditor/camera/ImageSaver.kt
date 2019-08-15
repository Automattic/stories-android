package com.automattic.photoeditor.camera

import android.media.Image
import android.util.Log
import com.automattic.photoeditor.camera.interfaces.ImageCaptureListener

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Saves a JPEG [Image] into the specified [File].
 */
internal class ImageSaver(
    /**
     * The JPEG image
     */
    private val image: Image,

    /**
     * The file we save the image into.
     */
    private val file: File,

    private val imageCaptureListener: ImageCaptureListener?
) : Runnable {
    override fun run() {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(file).apply {
                write(bytes)
            }
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
            imageCaptureListener?.onError(e.toString(), e)
        } finally {
            image.close()
            output?.let {
                try {
                    it.close()
                    imageCaptureListener?.onImageSaved(file)
                } catch (e: IOException) {
                    Log.e(TAG, e.toString())
                    imageCaptureListener?.onError(e.toString(), e)
                }
            }
        }
    }

    companion object {
        /**
         * Tag for the [Log].
         */
        private val TAG = "ImageSaver"
    }
}
