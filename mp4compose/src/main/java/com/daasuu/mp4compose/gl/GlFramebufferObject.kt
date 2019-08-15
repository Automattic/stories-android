package com.daasuu.mp4compose.gl

import android.opengl.GLES20

import com.daasuu.mp4compose.utils.EglUtil

import android.opengl.GLES20.GL_COLOR_ATTACHMENT0
import android.opengl.GLES20.GL_DEPTH_ATTACHMENT
import android.opengl.GLES20.GL_DEPTH_COMPONENT16
import android.opengl.GLES20.GL_FRAMEBUFFER
import android.opengl.GLES20.GL_FRAMEBUFFER_BINDING
import android.opengl.GLES20.GL_FRAMEBUFFER_COMPLETE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_MAX_RENDERBUFFER_SIZE
import android.opengl.GLES20.GL_MAX_TEXTURE_SIZE
import android.opengl.GLES20.GL_NEAREST
import android.opengl.GLES20.GL_RENDERBUFFER
import android.opengl.GLES20.GL_RENDERBUFFER_BINDING
import android.opengl.GLES20.GL_RGBA
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_BINDING_2D
import android.opengl.GLES20.GL_UNSIGNED_BYTE

class GlFramebufferObject {
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    private var framebufferName: Int = 0
    private var renderBufferName: Int = 0
    var texName: Int = 0
        private set

    fun setup(width: Int, height: Int) {
        val args = IntArray(1)

        GLES20.glGetIntegerv(GL_MAX_TEXTURE_SIZE, args, 0)
        if (width > args[0] || height > args[0]) {
            throw IllegalArgumentException("GL_MAX_TEXTURE_SIZE " + args[0])
        }

        GLES20.glGetIntegerv(GL_MAX_RENDERBUFFER_SIZE, args, 0)
        if (width > args[0] || height > args[0]) {
            throw IllegalArgumentException("GL_MAX_RENDERBUFFER_SIZE " + args[0])
        }

        GLES20.glGetIntegerv(GL_FRAMEBUFFER_BINDING, args, 0)
        val saveFramebuffer = args[0]
        GLES20.glGetIntegerv(GL_RENDERBUFFER_BINDING, args, 0)
        val saveRenderbuffer = args[0]
        GLES20.glGetIntegerv(GL_TEXTURE_BINDING_2D, args, 0)
        val saveTexName = args[0]

        release()

        try {
            this.width = width
            this.height = height

            GLES20.glGenFramebuffers(args.size, args, 0)
            framebufferName = args[0]
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName)

            GLES20.glGenRenderbuffers(args.size, args, 0)
            renderBufferName = args[0]
            GLES20.glBindRenderbuffer(GL_RENDERBUFFER, renderBufferName)
            GLES20.glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height)
            GLES20.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBufferName)

            GLES20.glGenTextures(args.size, args, 0)
            texName = args[0]
            GLES20.glBindTexture(GL_TEXTURE_2D, texName)

            EglUtil.setupSampler(GL_TEXTURE_2D, GL_LINEAR, GL_NEAREST)

            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)
            GLES20.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texName, 0)

            val status = GLES20.glCheckFramebufferStatus(GL_FRAMEBUFFER)
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                throw RuntimeException("Failed to initialize framebuffer object $status")
            }
        } catch (e: RuntimeException) {
            release()
            throw e
        }

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, saveFramebuffer)
        GLES20.glBindRenderbuffer(GL_RENDERBUFFER, saveRenderbuffer)
        GLES20.glBindTexture(GL_TEXTURE_2D, saveTexName)
    }

    fun release() {
        val args = IntArray(1)
        args[0] = texName
        GLES20.glDeleteTextures(args.size, args, 0)
        texName = 0
        args[0] = renderBufferName
        GLES20.glDeleteRenderbuffers(args.size, args, 0)
        renderBufferName = 0
        args[0] = framebufferName
        GLES20.glDeleteFramebuffers(args.size, args, 0)
        framebufferName = 0
    }

    fun enable() {
        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName)
    }
}
