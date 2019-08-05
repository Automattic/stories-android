package com.daasuu.mp4compose.filter

import android.opengl.GLES20

/**
 * Created by sudamasayuki on 2018/01/07.
 */

class GlSphereRefractionFilter : GlFilter(GlFilter.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER) {
    private var centerX = 0.5f
    private var centerY = 0.5f
    private var radius = 0.5f
    private var aspectRatio = 1.0f
    private var refractiveIndex = 0.71f

    fun setCenterX(centerX: Float) {
        this.centerX = centerX
    }

    fun setCenterY(centerY: Float) {
        this.centerY = centerY
    }

    fun setRadius(radius: Float) {
        this.radius = radius
    }

    fun setAspectRatio(aspectRatio: Float) {
        this.aspectRatio = aspectRatio
    }

    fun setRefractiveIndex(refractiveIndex: Float) {
        this.refractiveIndex = refractiveIndex
    }

    // ////////////////////////////////////////////////////////////////////////

    public override fun onDraw(presentationTime: Long) {
        GLES20.glUniform2f(getHandle("center"), centerX, centerY)
        GLES20.glUniform1f(getHandle("radius"), radius)
        GLES20.glUniform1f(getHandle("aspectRatio"), aspectRatio)
        GLES20.glUniform1f(getHandle("refractiveIndex"), refractiveIndex)
    }

    companion object {
        private val FRAGMENT_SHADER = "precision mediump float;" +

                "varying vec2 vTextureCoord;" +
                "uniform lowp sampler2D sTexture;" +
                "uniform highp vec2 center;" +
                "uniform highp float radius;" +
                "uniform highp float aspectRatio;" +
                "uniform highp float refractiveIndex;" +

                "void main() {" +
                "highp vec2 textureCoordinateToUse = vec2(vTextureCoord.x, " +
                "(vTextureCoord.y * aspectRatio + 0.5 - 0.5 " +
                "* aspectRatio));" +
                "highp float distanceFromCenter = distance(center, textureCoordinateToUse);" +
                "lowp float checkForPresenceWithinSphere = step(distanceFromCenter, radius);" +

                "distanceFromCenter = distanceFromCenter / radius;" +

                "highp float normalizedDepth = radius * sqrt(1.0 - distanceFromCenter * distanceFromCenter);" +
                "highp vec3 sphereNormal = normalize(vec3(textureCoordinateToUse - center, normalizedDepth));" +

                "highp vec3 refractedVector = refract(vec3(0.0, 0.0, -1.0), sphereNormal, refractiveIndex);" +

                "gl_FragColor = texture2D(sTexture, (refractedVector.xy + 1.0) * 0.5) * checkForPresenceWithinSphere;" +
                "}"
    }
}
