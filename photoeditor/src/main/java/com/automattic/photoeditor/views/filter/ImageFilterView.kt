package com.automattic.photoeditor.views.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.effect.Effect
import android.media.effect.EffectContext
import android.media.effect.EffectFactory.EFFECT_AUTOFIX
import android.media.effect.EffectFactory.EFFECT_BLACKWHITE
import android.media.effect.EffectFactory.EFFECT_BRIGHTNESS
import android.media.effect.EffectFactory.EFFECT_CONTRAST
import android.media.effect.EffectFactory.EFFECT_CROSSPROCESS
import android.media.effect.EffectFactory.EFFECT_DOCUMENTARY
import android.media.effect.EffectFactory.EFFECT_DUOTONE
import android.media.effect.EffectFactory.EFFECT_FILLLIGHT
import android.media.effect.EffectFactory.EFFECT_FISHEYE
import android.media.effect.EffectFactory.EFFECT_FLIP
import android.media.effect.EffectFactory.EFFECT_GRAIN
import android.media.effect.EffectFactory.EFFECT_GRAYSCALE
import android.media.effect.EffectFactory.EFFECT_LOMOISH
import android.media.effect.EffectFactory.EFFECT_NEGATIVE
import android.media.effect.EffectFactory.EFFECT_POSTERIZE
import android.media.effect.EffectFactory.EFFECT_ROTATE
import android.media.effect.EffectFactory.EFFECT_SATURATE
import android.media.effect.EffectFactory.EFFECT_SEPIA
import android.media.effect.EffectFactory.EFFECT_SHARPEN
import android.media.effect.EffectFactory.EFFECT_TEMPERATURE
import android.media.effect.EffectFactory.EFFECT_TINT
import android.media.effect.EffectFactory.EFFECT_VIGNETTE
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import com.automattic.photoeditor.OnSaveBitmap
import com.automattic.photoeditor.util.BitmapUtil
import com.automattic.photoeditor.views.filter.PhotoFilter.AUTO_FIX
import com.automattic.photoeditor.views.filter.PhotoFilter.BLACK_WHITE
import com.automattic.photoeditor.views.filter.PhotoFilter.BRIGHTNESS
import com.automattic.photoeditor.views.filter.PhotoFilter.CONTRAST
import com.automattic.photoeditor.views.filter.PhotoFilter.CROSS_PROCESS
import com.automattic.photoeditor.views.filter.PhotoFilter.DOCUMENTARY
import com.automattic.photoeditor.views.filter.PhotoFilter.DUE_TONE
import com.automattic.photoeditor.views.filter.PhotoFilter.FILL_LIGHT
import com.automattic.photoeditor.views.filter.PhotoFilter.FISH_EYE
import com.automattic.photoeditor.views.filter.PhotoFilter.FLIP_HORIZONTAL
import com.automattic.photoeditor.views.filter.PhotoFilter.FLIP_VERTICAL
import com.automattic.photoeditor.views.filter.PhotoFilter.GRAIN
import com.automattic.photoeditor.views.filter.PhotoFilter.GRAY_SCALE
import com.automattic.photoeditor.views.filter.PhotoFilter.LOMISH
import com.automattic.photoeditor.views.filter.PhotoFilter.NEGATIVE
import com.automattic.photoeditor.views.filter.PhotoFilter.NONE
import com.automattic.photoeditor.views.filter.PhotoFilter.POSTERIZE
import com.automattic.photoeditor.views.filter.PhotoFilter.ROTATE
import com.automattic.photoeditor.views.filter.PhotoFilter.SATURATE
import com.automattic.photoeditor.views.filter.PhotoFilter.SEPIA
import com.automattic.photoeditor.views.filter.PhotoFilter.SHARPEN
import com.automattic.photoeditor.views.filter.PhotoFilter.TEMPERATURE
import com.automattic.photoeditor.views.filter.PhotoFilter.TINT
import com.automattic.photoeditor.views.filter.PhotoFilter.VIGNETTE
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *
 *
 * Filter Images using ImageFilterView
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.2
 * @since 2/14/2018
 */
internal class ImageFilterView : GLSurfaceView, GLSurfaceView.Renderer {
    private val mTextures = IntArray(2)
    private var mEffectContext: EffectContext? = null
    private var mEffect: Effect? = null
    private val mTexRenderer = TextureRenderer()
    private var mImageWidth: Int = 0
    private var mImageHeight: Int = 0
    private var mInitialized = false
    private var mCurrentEffect: PhotoFilter? = null
    private var mSourceBitmap: Bitmap? = null
    private var mCustomEffect: CustomEffect? = null
    private var mOnSaveBitmap: OnSaveBitmap? = null
    private var isSaveImage = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        setFilterEffect(NONE)
    }

    fun setSourceBitmap(sourceBitmap: Bitmap?) {
        mSourceBitmap = sourceBitmap
        mInitialized = false
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        mTexRenderer.updateViewSize(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        if (!mInitialized) {
            // Only need to do this once
            mEffectContext = EffectContext.createWithCurrentGlContext()
            mTexRenderer.init()
            loadTextures()
            mInitialized = true
        }
        if (mCurrentEffect != NONE || mCustomEffect != null) {
            // if an effect is chosen initialize it and apply it to the texture
            initEffect()
            applyEffect()
        }
        renderResult()
        if (isSaveImage) {
            val filterBitmap = BitmapUtil.createBitmapFromGLSurface(this, gl)
            Log.e(TAG, "onDrawFrame: $filterBitmap")
            filterBitmap?.let { bitmap ->
                isSaveImage = false
                mOnSaveBitmap?.let { onSaveBitmap ->
                    Handler(Looper.getMainLooper()).post { onSaveBitmap.onBitmapReady(bitmap) }
                }
            }
        }
    }

    fun setFilterEffect(effect: PhotoFilter) {
        mCurrentEffect = effect
        mCustomEffect = null
        requestRender()
    }

    fun setFilterEffect(customEffect: CustomEffect) {
        mCustomEffect = customEffect
        requestRender()
    }

    fun saveBitmap(onSaveBitmap: OnSaveBitmap) {
        mOnSaveBitmap = onSaveBitmap
        isSaveImage = true
        requestRender()
    }

    private fun loadTextures() {
        // Generate textures
        GLES20.glGenTextures(2, mTextures, 0)

        // Load input bitmap
        mSourceBitmap?.let { sourceBitmap ->
            mImageWidth = sourceBitmap.width
            mImageHeight = sourceBitmap.height
            mTexRenderer.updateTextureSize(mImageWidth, mImageHeight)

            // Upload to texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0])
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mSourceBitmap, 0)

            // Set texture parameters
            GLToolbox.initTexParams()
        }
    }

    private fun initEffect() {
        val effectFactory = mEffectContext?.factory ?: return
        mEffect?.release()

        mCustomEffect?.let { customEffect ->
            mEffect = effectFactory.createEffect(customEffect.effectName)
            val parameters = customEffect.parameters
            for ((key, value) in parameters) {
                mEffect?.setParameter(key, value)
            }
        } ?: run {
            // Initialize the correct effect based on the selected menu/action item
            when (mCurrentEffect) {
                AUTO_FIX -> {
                    mEffect = effectFactory.createEffect(EFFECT_AUTOFIX).apply {
                        setParameter("scale", 0.5f)
                    }
                }
                BLACK_WHITE -> {
                    mEffect = effectFactory.createEffect(EFFECT_BLACKWHITE).apply {
                        setParameter("black", .1f)
                        setParameter("white", .7f)
                    }
                }
                BRIGHTNESS -> {
                    mEffect = effectFactory.createEffect(EFFECT_BRIGHTNESS).apply {
                        setParameter("brightness", 2.0f)
                    }
                }
                CONTRAST -> {
                    mEffect = effectFactory.createEffect(EFFECT_CONTRAST).apply {
                        setParameter("contrast", 1.4f)
                    }
                }
                CROSS_PROCESS -> mEffect = effectFactory.createEffect(EFFECT_CROSSPROCESS)
                DOCUMENTARY -> mEffect = effectFactory.createEffect(EFFECT_DOCUMENTARY)
                DUE_TONE -> {
                    mEffect = effectFactory.createEffect(EFFECT_DUOTONE).apply {
                        setParameter("first_color", Color.YELLOW)
                        setParameter("second_color", Color.DKGRAY)
                    }
                }
                FILL_LIGHT -> {
                    mEffect = effectFactory.createEffect(EFFECT_FILLLIGHT).apply {
                        setParameter("strength", .8f)
                    }
                }
                FISH_EYE -> {
                    mEffect = effectFactory.createEffect(EFFECT_FISHEYE).apply {
                        setParameter("scale", .5f)
                    }
                }
                FLIP_HORIZONTAL -> {
                    mEffect = effectFactory.createEffect(EFFECT_FLIP).apply {
                        setParameter("horizontal", true)
                    }
                }
                FLIP_VERTICAL -> {
                    mEffect = effectFactory.createEffect(EFFECT_FLIP).apply {
                        setParameter("vertical", true)
                    }
                }
                GRAIN -> {
                    mEffect = effectFactory.createEffect(EFFECT_GRAIN).apply {
                        setParameter("strength", 1.0f)
                    }
                }
                GRAY_SCALE -> mEffect = effectFactory.createEffect(EFFECT_GRAYSCALE)
                LOMISH -> mEffect = effectFactory.createEffect(EFFECT_LOMOISH)
                NEGATIVE -> mEffect = effectFactory.createEffect(EFFECT_NEGATIVE)
                NONE -> {
                }
                POSTERIZE -> mEffect = effectFactory.createEffect(EFFECT_POSTERIZE)
                ROTATE -> {
                    mEffect = effectFactory.createEffect(EFFECT_ROTATE).apply {
                        setParameter("angle", 180)
                    }
                }
                SATURATE -> {
                    mEffect = effectFactory.createEffect(EFFECT_SATURATE).apply {
                        setParameter("scale", .5f)
                    }
                }
                SEPIA -> mEffect = effectFactory.createEffect(EFFECT_SEPIA)
                SHARPEN -> mEffect = effectFactory.createEffect(EFFECT_SHARPEN)
                TEMPERATURE -> {
                    mEffect = effectFactory.createEffect(EFFECT_TEMPERATURE).apply {
                        setParameter("scale", .9f)
                    }
                }
                TINT -> {
                    mEffect = effectFactory.createEffect(EFFECT_TINT).apply {
                        setParameter("tint", Color.MAGENTA)
                    }
                }
                VIGNETTE -> {
                    mEffect = effectFactory.createEffect(EFFECT_VIGNETTE).apply {
                        setParameter("scale", .5f)
                    }
                }
            }
        }
    }

    private fun applyEffect() {
        mEffect?.apply(mTextures[0], mImageWidth, mImageHeight, mTextures[1])
    }

    private fun renderResult() {
        if (mCurrentEffect != NONE || mCustomEffect != null) {
            // if no effect is chosen, just render the original bitmap
            mTexRenderer.renderTexture(mTextures[1])
        } else {
            // render the result of applyEffect()
            mTexRenderer.renderTexture(mTextures[0])
        }
    }

    companion object {
        private const val TAG = "ImageFilterView"
    }
}
