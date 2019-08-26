
package com.automattic.photoeditor.camera.interfaces

import java.io.File

interface VideoRecorderFinished {
    fun onVideoSaved(file: File?)
    fun onError(message: String?, cause: Throwable?)
}
interface VideoRecorderHandler {
    fun startRecordingVideo(finishedListener: VideoRecorderFinished? = null)
    fun stopRecordingVideo()
}
