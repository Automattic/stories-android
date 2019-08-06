package com.daasuu.mp4compose.utils

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLException
import android.opengl.GLUtils
import android.util.Log

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

import android.opengl.GLES20.GL_ARRAY_BUFFER
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_LINK_STATUS
import android.opengl.GLES20.GL_STATIC_DRAW
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.GL_TRUE
import android.opengl.GLES20.glCreateProgram

/**
 * Created by sudamasayuki on 2017/11/14.
 */

object EglUtil {
    val NO_TEXTURE = -1
    private val FLOAT_SIZE_BYTES = 4

    fun loadShader(strSource: String, iType: Int): Int {
        val compiled = IntArray(1)
        val iShader = GLES20.glCreateShader(iType)
        GLES20.glShaderSource(iShader, strSource)
        GLES20.glCompileShader(iShader)
        GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.d("Load Shader Failed", "Compilation\n" + GLES20.glGetShaderInfoLog(iShader))
            return 0
        }
        return iShader
    }

    @Throws(GLException::class)
    fun createProgram(vertexShader: Int, pixelShader: Int): Int {
        val program = glCreateProgram()
        if (program == 0) {
            throw RuntimeException("Could not create program")
        }

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, pixelShader)

        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GL_TRUE) {
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Could not link program")
        }
        return program
    }

    fun setupSampler(target: Int, mag: Int, min: Int) {
        GLES20.glTexParameterf(target, GL_TEXTURE_MAG_FILTER, mag.toFloat())
        GLES20.glTexParameterf(target, GL_TEXTURE_MIN_FILTER, min.toFloat())
        GLES20.glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }

    fun createBuffer(data: FloatArray): Int {
        return createBuffer(toFloatBuffer(data))
    }

    fun createBuffer(data: FloatBuffer): Int {
        val buffers = IntArray(1)
        GLES20.glGenBuffers(buffers.size, buffers, 0)
        updateBufferData(buffers[0], data)
        return buffers[0]
    }

    fun toFloatBuffer(data: FloatArray): FloatBuffer {
        val buffer = ByteBuffer
            .allocateDirect(data.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(data).position(0)
        return buffer
    }

    fun updateBufferData(bufferName: Int, data: FloatBuffer) {
        GLES20.glBindBuffer(GL_ARRAY_BUFFER, bufferName)
        GLES20.glBufferData(GL_ARRAY_BUFFER, data.capacity() * FLOAT_SIZE_BYTES, data, GL_STATIC_DRAW)
        GLES20.glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    fun loadTexture(img: Bitmap, usedTexId: Int, recycle: Boolean): Int {
        val textures = IntArray(1)
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
            )

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId)
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, img)
            textures[0] = usedTexId
        }
        if (recycle) {
            img.recycle()
        }
        return textures[0]
    }
}
