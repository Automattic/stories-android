package com.daasuu.mp4compose.filter

import android.opengl.GLES20

/**
 * Created by sudamasayuki on 2018/01/06.
 */

class GlBulgeDistortionFilter : GlFilter(GlFilter.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER) {
    var centerX = 0.5f
    var centerY = 0.5f
    var radius = 0.25f
    var scale = 0.5f

    public override fun onDraw(presentationTime: Long) {
        GLES20.glUniform2f(getHandle("center"), centerX, centerY)
        GLES20.glUniform1f(getHandle("radius"), radius)
        GLES20.glUniform1f(getHandle("scale"), scale)
    }

    companion object {
        private val FRAGMENT_SHADER = "precision mediump float;" +

                "varying highp vec2 vTextureCoord;" +
                "uniform lowp sampler2D sTexture;" +

                "uniform highp vec2 center;" +
                "uniform highp float radius;" +
                "uniform highp float scale;" +

                "void main() {" +
                "highp vec2 textureCoordinateToUse = vTextureCoord;" +
                "highp float dist = distance(center, vTextureCoord);" +
                "textureCoordinateToUse -= center;" +
                "if (dist < radius) {" +
                "highp float percent = 1.0 - ((radius - dist) / radius) * scale;" +
                "percent = percent * percent;" +
                "textureCoordinateToUse = textureCoordinateToUse * percent;" +
                "}" +
                "textureCoordinateToUse += center;" +

                "gl_FragColor = texture2D(sTexture, textureCoordinateToUse);" +
                "}"
    }
}
