package com.daasuu.mp4compose.composer

interface Listener {
    /**
     * Called to notify progress.
     *
     * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
     */
    fun onProgress(progress: Double)

    /**
     * Called when transcode completed.
     */
    fun onCompleted()

    /**
     * Called when transcode canceled.
     */
    fun onCanceled()

    fun onFailed(exception: Exception)

    fun onStart()
}
