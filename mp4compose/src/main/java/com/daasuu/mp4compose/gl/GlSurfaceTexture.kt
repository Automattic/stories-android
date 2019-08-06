package com.daasuu.mp4compose.gl

import android.graphics.SurfaceTexture

class GlSurfaceTexture(texName: Int) : SurfaceTexture.OnFrameAvailableListener {
    val surfaceTexture: SurfaceTexture
    private var onFrameAvailableListener: SurfaceTexture.OnFrameAvailableListener? = null

    val textureTarget: Int
        get() = GlPreview.GL_TEXTURE_EXTERNAL_OES

    init {
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
