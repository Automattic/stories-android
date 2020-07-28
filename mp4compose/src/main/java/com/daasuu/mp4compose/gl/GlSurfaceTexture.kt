package com.daasuu.mp4compose.gl

import android.graphics.SurfaceTexture

class GlSurfaceTexture(texName: Int) : SurfaceTexture.OnFrameAvailableListener {
    val surfaceTexture: SurfaceTexture
    private var onFrameAvailableListener: SurfaceTexture.OnFrameAvailableListener? = null

    val textureTarget: Int
        get() = GlPreview.GL_TEXTURE_EXTERNAL_OES

    init {
        // SupressWarnings explanation:
        // Many resources, such as TypedArrays, VelocityTrackers, etc., should be recycled (with a recycle() call) after
        // use. This lint check looks for missing recycle() calls.
        // Note from editor: it is being released in fun release() so, should be OK.
        @SuppressWarnings("Recycle")
        surfaceTexture = SurfaceTexture(texName)
        surfaceTexture.setOnFrameAvailableListener(this)
    }

    fun setOnFrameAvailableListener(l: SurfaceTexture.OnFrameAvailableListener) {
        onFrameAvailableListener = l
    }

    fun updateTexImage() {
        surfaceTexture.updateTexImage()
    }

    fun getTransformMatrix(mtx: FloatArray) {
        surfaceTexture.getTransformMatrix(mtx)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        if (onFrameAvailableListener != null) {
            onFrameAvailableListener!!.onFrameAvailable(this.surfaceTexture)
        }
    }

    fun release() {
        surfaceTexture.release()
    }
}
