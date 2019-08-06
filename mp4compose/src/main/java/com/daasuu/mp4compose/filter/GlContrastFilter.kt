package com.daasuu.mp4compose.filter

import android.opengl.GLES20

/**
 * Changes the contrast of the image.
 * contrast value ranges from 0.0 to 4.0, with 1.0 as the normal level
 */
class GlContrastFilter : GlFilter(GlFilter.DEFAULT_VERTEX_SHADER, CONTRAST_FRAGMENT_SHADER) {
    private var contrast = 1.2f

    fun setContrast(contrast: Float) {
        this.contrast = contrast
    }

    public override fun onDraw(presentationTime: Long) {
        GLES20.glUniform1f(getHandle("contrast"), contrast)
    }

    companion object {
        private val CONTRAST_FRAGMENT_SHADER = "" +
                "precision mediump float;" +
                " varying vec2 vTextureCoord;\n" +
                " \n" +
                " uniform lowp sampler2D sTexture;\n" +
                " uniform lowp float contrast;\n" +
                " \n" +
                " void main()\n" +
                " {\n" +
                "     lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
                "     \n" +
                "     gl_FragColor = vec4(((textureColor.rgb - vec3(0.5)) * contrast + vec3(0.5)), textureColor.w);\n" +
                " }"
    }
}
