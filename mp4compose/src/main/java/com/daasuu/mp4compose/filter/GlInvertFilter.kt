package com.daasuu.mp4compose.filter

/**
 * Created by sudamasayuki on 2018/01/06.
 */

class GlInvertFilter : GlFilter(GlFilter.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER) {
    companion object {
        private val FRAGMENT_SHADER = "precision mediump float;" +
                "varying vec2 vTextureCoord;" +
                "uniform lowp sampler2D sTexture;" +
                "void main() {" +
                "lowp vec4 color = texture2D(sTexture, vTextureCoord);" +
                "gl_FragColor = vec4((1.0 - color.rgb), color.w);" +
                "}"
    }
}
