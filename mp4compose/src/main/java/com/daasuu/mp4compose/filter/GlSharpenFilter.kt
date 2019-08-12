package com.daasuu.mp4compose.filter

import android.opengl.GLES20

/**
 * Created by sudamasayuki on 2018/01/07.
 */

class GlSharpenFilter : GlFilter(VERTEX_SHADER, FRAGMENT_SHADER) {
    private var imageWidthFactor = 0.004f
    private var imageHeightFactor = 0.004f
    var sharpness = 1f

    override fun setFrameSize(width: Int, height: Int) {
        imageWidthFactor = 1f / width
        imageHeightFactor = 1f / height
    }

    public override fun onDraw(presentationTime: Long) {
        GLES20.glUniform1f(getHandle("imageWidthFactor"), imageWidthFactor)
        GLES20.glUniform1f(getHandle("imageHeightFactor"), imageHeightFactor)
        GLES20.glUniform1f(getHandle("sharpness"), sharpness)
    }

    companion object {
        private val VERTEX_SHADER = "attribute vec4 aPosition;" +
                "attribute vec4 aTextureCoord;" +

                "uniform float imageWidthFactor;" +
                "uniform float imageHeightFactor;" +
                "uniform float sharpness;" +

                "varying highp vec2 textureCoordinate;" +
                "varying highp vec2 leftTextureCoordinate;" +
                "varying highp vec2 rightTextureCoordinate;" +
                "varying highp vec2 topTextureCoordinate;" +
                "varying highp vec2 bottomTextureCoordinate;" +

                "varying float centerMultiplier;" +
                "varying float edgeMultiplier;" +

                "void main() {" +
                "gl_Position = aPosition;" +

                "mediump vec2 widthStep = vec2(imageWidthFactor, 0.0);" +
                "mediump vec2 heightStep = vec2(0.0, imageHeightFactor);" +

                "textureCoordinate       = aTextureCoord.xy;" +
                "leftTextureCoordinate   = textureCoordinate - widthStep;" +
                "rightTextureCoordinate  = textureCoordinate + widthStep;" +
                "topTextureCoordinate    = textureCoordinate + heightStep;" +
                "bottomTextureCoordinate = textureCoordinate - heightStep;" +

                "centerMultiplier = 1.0 + 4.0 * sharpness;" +
                "edgeMultiplier = sharpness;" +
                "}"

        private val FRAGMENT_SHADER = "precision highp float;" +

                "uniform lowp sampler2D sTexture;" +

                "varying highp vec2 textureCoordinate;" +
                "varying highp vec2 leftTextureCoordinate;" +
                "varying highp vec2 rightTextureCoordinate;" +
                "varying highp vec2 topTextureCoordinate;" +
                "varying highp vec2 bottomTextureCoordinate;" +

                "varying float centerMultiplier;" +
                "varying float edgeMultiplier;" +

                "void main() {" +
                "mediump vec3 textureColor       = texture2D(sTexture, textureCoordinate).rgb;" +
                "mediump vec3 leftTextureColor   = texture2D(sTexture, leftTextureCoordinate).rgb;" +
                "mediump vec3 rightTextureColor  = texture2D(sTexture, rightTextureCoordinate).rgb;" +
                "mediump vec3 topTextureColor    = texture2D(sTexture, topTextureCoordinate).rgb;" +
                "mediump vec3 bottomTextureColor = texture2D(sTexture, bottomTextureCoordinate).rgb;" +

                "gl_FragColor = vec4((textureColor * centerMultiplier - (leftTextureColor * edgeMultiplier " +
                "+ rightTextureColor * edgeMultiplier + topTextureColor * edgeMultiplier + bottomTextureColor " +
                "* edgeMultiplier)), texture2D(sTexture, bottomTextureCoordinate).w);" +
                "}"
    }
}
