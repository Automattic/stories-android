package com.daasuu.mp4compose.gl

import android.opengl.GLES20

import com.daasuu.mp4compose.filter.GlFilter

import android.opengl.GLES20.GL_ARRAY_BUFFER
import android.opengl.GLES20.GL_FLOAT
import android.opengl.GLES20.GL_TEXTURE0
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TRIANGLE_STRIP

class GlPreviewFilter(private val texTarget: Int) :
    GlFilter(VERTEX_SHADER, createFragmentShaderSourceOESIfNeed(texTarget)) {

    fun draw(texName: Int, mvpMatrix: FloatArray, stMatrix: FloatArray, aspectRatio: Float) {
        useProgram()

        GLES20.glUniformMatrix4fv(getHandle("uMVPMatrix"), 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(getHandle("uSTMatrix"), 1, false, stMatrix, 0)
        GLES20.glUniform1f(getHandle("uCRatio"), aspectRatio)

        GLES20.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferName)
        GLES20.glEnableVertexAttribArray(getHandle("aPosition"))
        GLES20.glVertexAttribPointer(
            getHandle("aPosition"),
            GlFilter.VERTICES_DATA_POS_SIZE,
            GL_FLOAT,
            false,
            GlFilter.VERTICES_DATA_STRIDE_BYTES,
            GlFilter.VERTICES_DATA_POS_OFFSET
        )
        GLES20.glEnableVertexAttribArray(getHandle("aTextureCoord"))
        GLES20.glVertexAttribPointer(
            getHandle("aTextureCoord"),
            GlFilter.VERTICES_DATA_UV_SIZE,
            GL_FLOAT,
            false,
            GlFilter.VERTICES_DATA_STRIDE_BYTES,
            GlFilter.VERTICES_DATA_UV_OFFSET
        )

        GLES20.glActiveTexture(GL_TEXTURE0)
        GLES20.glBindTexture(texTarget, texName)
        GLES20.glUniform1i(getHandle(GlFilter.DEFAULT_UNIFORM_SAMPLER), 0)

        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(getHandle("aPosition"))
        GLES20.glDisableVertexAttribArray(getHandle("aTextureCoord"))
        GLES20.glBindBuffer(GL_ARRAY_BUFFER, 0)
        GLES20.glBindTexture(GL_TEXTURE_2D, 0)
    }

    companion object {

        val GL_TEXTURE_EXTERNAL_OES = 0x8D65

        private val VERTEX_SHADER = "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "uniform float uCRatio;\n" +

                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying highp vec2 vTextureCoord;\n" +

                "void main() {\n" +
                "vec4 scaledPos = aPosition;\n" +
                "scaledPos.x = scaledPos.x * uCRatio;\n" +
                "gl_Position = uMVPMatrix * scaledPos;\n" +
                "vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                "}\n"

        private fun createFragmentShaderSourceOESIfNeed(texTarget: Int): String {
            return if (texTarget == GL_TEXTURE_EXTERNAL_OES) {
                StringBuilder()
                    .append("#extension GL_OES_EGL_image_external : require\n")
                    .append(GlFilter.DEFAULT_FRAGMENT_SHADER.replace("sampler2D", "samplerExternalOES"))
                    .toString()
            } else GlFilter.DEFAULT_FRAGMENT_SHADER
        }
    }
}
