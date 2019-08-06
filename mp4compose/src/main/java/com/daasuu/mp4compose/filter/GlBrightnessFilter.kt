package com.daasuu.mp4compose.filter

import android.opengl.GLES20

class GlBrightnessFilter : GlFilter(GlFilter.DEFAULT_VERTEX_SHADER, BRIGHTNESS_FRAGMENT_SHADER) {
    private var brightness = 0f

    fun setBrightness(brightness: Float) {
        this.brightness = brightness
    }

    public override fun onDraw(presentationTime: Long) {
        GLES20.glUniform1f(getHandle("brightness"), brightness)
    }

    companion object {
        private val BRIGHTNESS_FRAGMENT_SHADER = "" +
                "precision mediump float;" +
                " varying vec2 vTextureCoord;\n" +
                " \n" +
                " uniform lowp sampler2D sTexture;\n" +
                " uniform lowp float brightness;\n" +
                " \n" +
                " void main()\n" +
                " {\n" +
                "     lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
                "     \n" +
                "     gl_FragColor = vec4((textureColor.rgb + vec3(brightness)), textureColor.w);\n" +
                " }"
    }
}
