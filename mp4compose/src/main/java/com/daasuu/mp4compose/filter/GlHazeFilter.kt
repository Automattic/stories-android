package com.daasuu.mp4compose.filter

import android.opengl.GLES20

/**
 * Created by sudamasayuki on 2018/01/06.
 */

class GlHazeFilter : GlFilter(GlFilter.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER) {

    var distance = 0.2f
    var slope = 0.0f

    public override fun onDraw(presentationTime: Long) {
        GLES20.glUniform1f(getHandle("distance"), distance)
        GLES20.glUniform1f(getHandle("slope"), slope)
    }

    companion object {

        private val FRAGMENT_SHADER = "precision mediump float;" +
                "varying highp vec2 vTextureCoord;" +
                "uniform lowp sampler2D sTexture;" +
                "uniform lowp float distance;" +
                "uniform highp float slope;" +

                "void main() {" +
                "highp vec4 color = vec4(1.0);" +

                "highp float  d = vTextureCoord.y * slope  +  distance;" +

                "highp vec4 c = texture2D(sTexture, vTextureCoord);" +
                "c = (c - d * color) / (1.0 -d);" +
                "gl_FragColor = c;" + // consider using premultiply(c);

                "}"
    }
}
