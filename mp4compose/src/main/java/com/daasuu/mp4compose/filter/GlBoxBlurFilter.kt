package com.daasuu.mp4compose.filter

import android.opengl.GLES20

/**
 * Created by sudamasayuki on 2018/01/06.
 */

class GlBoxBlurFilter : GlFilter(VERTEX_SHADER, FRAGMENT_SHADER) {

    var texelWidthOffset = 0.003f
    var texelHeightOffset = 0.003f
    var blurSize = 1.0f

    public override fun onDraw(presentationTime: Long) {
        GLES20.glUniform1f(getHandle("texelWidthOffset"), texelWidthOffset)
        GLES20.glUniform1f(getHandle("texelHeightOffset"), texelHeightOffset)
        GLES20.glUniform1f(getHandle("blurSize"), blurSize)
    }

    companion object {

        private val VERTEX_SHADER = "attribute vec4 aPosition;" +
                "attribute vec4 aTextureCoord;" +

                "uniform highp float texelWidthOffset;" +
                "uniform highp float texelHeightOffset;" +
                "uniform highp float blurSize;" +

                "varying highp vec2 centerTextureCoordinate;" +
                "varying highp vec2 oneStepLeftTextureCoordinate;" +
                "varying highp vec2 twoStepsLeftTextureCoordinate;" +
                "varying highp vec2 oneStepRightTextureCoordinate;" +
                "varying highp vec2 twoStepsRightTextureCoordinate;" +

                "void main() {" +
                "gl_Position = aPosition;" +

                "vec2 firstOffset = vec2(1.5 * texelWidthOffset, 1.5 * texelHeightOffset) * blurSize;" +
                "vec2 secondOffset = vec2(3.5 * texelWidthOffset, 3.5 * texelHeightOffset) * blurSize;" +

                "centerTextureCoordinate = aTextureCoord.xy;" +
                "oneStepLeftTextureCoordinate = centerTextureCoordinate - firstOffset;" +
                "twoStepsLeftTextureCoordinate = centerTextureCoordinate - secondOffset;" +
                "oneStepRightTextureCoordinate = centerTextureCoordinate + firstOffset;" +
                "twoStepsRightTextureCoordinate = centerTextureCoordinate + secondOffset;" +
                "}"

        private val FRAGMENT_SHADER = "precision mediump float;" +

                "uniform lowp sampler2D sTexture;" +

                "varying highp vec2 centerTextureCoordinate;" +
                "varying highp vec2 oneStepLeftTextureCoordinate;" +
                "varying highp vec2 twoStepsLeftTextureCoordinate;" +
                "varying highp vec2 oneStepRightTextureCoordinate;" +
                "varying highp vec2 twoStepsRightTextureCoordinate;" +

                "void main() {" +
                "lowp vec4 color = texture2D(sTexture, centerTextureCoordinate) * 0.2;" +
                "color += texture2D(sTexture, oneStepLeftTextureCoordinate) * 0.2;" +
                "color += texture2D(sTexture, oneStepRightTextureCoordinate) * 0.2;" +
                "color += texture2D(sTexture, twoStepsLeftTextureCoordinate) * 0.2;" +
                "color += texture2D(sTexture, twoStepsRightTextureCoordinate) * 0.2;" +
                "gl_FragColor = color;" +
                "}"
    }
}
