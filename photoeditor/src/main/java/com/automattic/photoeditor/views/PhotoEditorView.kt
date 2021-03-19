package com.automattic.photoeditor.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_CROP
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import com.automattic.photoeditor.OnSaveBitmap
import com.automattic.photoeditor.R.styleable
import com.automattic.photoeditor.views.background.fixed.BackgroundImageView
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import com.automattic.photoeditor.views.brush.BrushDrawingView
import com.automattic.photoeditor.views.filter.CustomEffect
import com.automattic.photoeditor.views.filter.ImageFilterView
import com.automattic.photoeditor.views.filter.PhotoFilter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation

/**
 *
 *
 * This ViewGroup will have the [BrushDrawingView] to draw paint on it with [ImageView]
 * which our source image
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 1/18/2018
 */

class PhotoEditorView : RelativeLayout {
    private lateinit var autoFitTextureView: AutoFitTextureView
    private lateinit var backgroundImage: BackgroundImageView
    private lateinit var backgroundImageBlurred: AppCompatImageView
    private lateinit var brushDrawingView: BrushDrawingView
    private lateinit var imageFilterView: ImageFilterView
    private lateinit var progressBar: ProgressBar
    private var attachedToWindow: Boolean = false

    private var surfaceListeners: ArrayList<SurfaceTextureListener> = ArrayList()

    private val mainSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            for (listener in surfaceListeners) {
                listener.onSurfaceTextureAvailable(texture, width, height)
            }
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            for (listener in surfaceListeners) {
                listener.onSurfaceTextureSizeChanged(texture, width, height)
            }
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    }

    /**
     * Source image which you want to edit
     *
     * @return source ImageView
     */
    val source: ImageView
        get() = backgroundImage

    val sourceBlurredBkg: ImageView
        get() = backgroundImageBlurred

    val brush: BrushDrawingView
        get() = brushDrawingView

    val textureView: AutoFitTextureView
        get() = autoFitTextureView

    val listeners: ArrayList<SurfaceTextureListener>
        get() = surfaceListeners

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(attrs)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attachedToWindow = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
    }

    fun onComposerDestroyed() {
        attachedToWindow = false
    }

    @SuppressLint("Recycle")
    private fun init(attrs: AttributeSet?) {
        backgroundImageBlurred = AppCompatImageView(context).apply {
            id = imgBlurSrcId
            adjustViewBounds = true
            scaleType = CENTER_CROP
        }

        // Setup image attributes
        backgroundImage = BackgroundImageView(context).apply {
            id = imgSrcId
            adjustViewBounds = true
            scaleType = FIT_CENTER
        }

        val imgSrcParam = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            addRule(CENTER_IN_PARENT, TRUE)
        }
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, styleable.PhotoEditorView)
            val imgSrcDrawable = a.getDrawable(styleable.PhotoEditorView_photo_src)
            if (imgSrcDrawable != null) {
                backgroundImage.setImageDrawable(imgSrcDrawable)
            }
        }

        // Setup Camera preview view
        val cameraParam = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            addRule(CENTER_IN_PARENT, TRUE)
        }
        // Setup Camera preview view
        autoFitTextureView = AutoFitTextureView(context).apply {
            id = cameraPreviewId
            visibility = View.GONE
            // set main listener
            surfaceTextureListener = mainSurfaceTextureListener
        }

        // Setup brush view
        brushDrawingView = BrushDrawingView(context).apply {
            visibility = View.GONE
            id = brushSrcId
        }
        // Align brush to the size of image view
        val brushParam = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            addRule(CENTER_IN_PARENT, TRUE)
            addRule(ALIGN_TOP, imgSrcId)
            addRule(ALIGN_BOTTOM, imgSrcId)
        }

        // Setup GLSurface attributes
        imageFilterView = ImageFilterView(context)
        imageFilterView.id = glFilterId
        imageFilterView.visibility = View.GONE
        val imgFilterParam = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            addRule(CENTER_IN_PARENT, TRUE)
            addRule(ALIGN_TOP, imgSrcId)
            addRule(ALIGN_BOTTOM, imgSrcId)
        }

        backgroundImage.setOnImageChangedListener(object : BackgroundImageView.OnImageChangedListener {
            override fun onBitmapLoaded(sourceBitmap: Bitmap?) {
                if (attachedToWindow) {
                    Glide.with(context).load(sourceBitmap)
                            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
                            .into(backgroundImageBlurred)
                }
                imageFilterView.setFilterEffect(PhotoFilter.NONE)
                imageFilterView.setSourceBitmap(sourceBitmap)
                Log.d(TAG, "onBitmapLoaded() called with: sourceBitmap = [$sourceBitmap]")
            }
        })

        // Setup loading view
        progressBar = ProgressBar(context).apply {
            visibility = View.GONE
        }
        // Align brush to the size of image view
        val progressBarParam = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(CENTER_IN_PARENT, TRUE)
            addRule(ALIGN_TOP, imgSrcId)
            addRule(ALIGN_BOTTOM, imgSrcId)
        }

        // Add camera preview
        addView(autoFitTextureView, cameraParam)

        // Add image source
        addView(backgroundImageBlurred, imgSrcParam)

        // Add image source
        addView(backgroundImage, imgSrcParam)

        // Add Gl FilterView
        addView(imageFilterView, imgFilterParam)

        // Add brush view
        addView(brushDrawingView, brushParam)

        // Add progress view
        addView(progressBar, progressBarParam)
    }

    // added this method as a helper due to the reasons outlined here:
    // https://developer.android.com/reference/androidx/camera/core/Preview.html#setOnPreviewOutputUpdateListener(androidx.camera.core.Preview.OnPreviewOutputUpdateListener)
    // Copying here to make it clear:
    //
    // * Calling TextureView.setSurfaceTexture(SurfaceTexture) when the TextureView's SurfaceTexture is already created,
    // * should be preceded by calling ViewGroup.removeView(View) and ViewGroup.addView(View) on the parent view of the
    // * TextureView to ensure the setSurfaceTexture() call succeeds.
    fun removeAndAddTextureViewBack() {
        val parent = textureView.parent as ViewGroup
        val index = parent.indexOfChild(textureView)
        parent.removeView(textureView)

        val cameraParam = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(CENTER_IN_PARENT, TRUE)
        }

        // Add camera preview
        parent.addView(textureView, index, cameraParam)
    }

    internal fun saveFilter(onSaveBitmap: OnSaveBitmap) {
        // check which background is currently visible: if it's
        // - imageFilterView:  a filter has been applied, process it first
        // - autoFitTextureView:  a video (camera preview or video player) is being shown, use that
        // - else: use deafult backgroundImage.bitmap
        if (imageFilterView.visibility == View.VISIBLE) {
            imageFilterView.saveBitmap(object : OnSaveBitmap {
                override fun onBitmapReady(saveBitmap: Bitmap) {
                    Log.e(TAG, "saveFilter: $saveBitmap")
                    backgroundImage.setImageBitmap(saveBitmap)
                    imageFilterView.visibility = View.GONE
                    onSaveBitmap.onBitmapReady(saveBitmap)
                }

                override fun onFailure(e: Exception) {
                    onSaveBitmap.onFailure(e)
                }
            })
        } else if (autoFitTextureView.visibility == View.VISIBLE) {
            // this saves just a snapshot of whatever is being displayed on the TextureSurface
            backgroundImage.setImageBitmap(autoFitTextureView.bitmap)
            toggleTextureView()
            onSaveBitmap.onBitmapReady(backgroundImage.bitmap!!)
        } else {
            onSaveBitmap.onBitmapReady(backgroundImage.bitmap!!)
        }
    }

    internal fun setFilterEffect(filterType: PhotoFilter) {
        imageFilterView.visibility = View.VISIBLE
        imageFilterView.setSourceBitmap(backgroundImage.bitmap)
        imageFilterView.setFilterEffect(filterType)
    }

    internal fun setFilterEffect(customEffect: CustomEffect) {
        imageFilterView.visibility = View.VISIBLE
        imageFilterView.setSourceBitmap(backgroundImage.bitmap)
        imageFilterView.setFilterEffect(customEffect)
    }

    internal fun turnTextureViewOn() {
        backgroundImage.visibility = View.INVISIBLE
        backgroundImageBlurred.visibility = View.INVISIBLE
        autoFitTextureView.visibility = View.VISIBLE
    }

    internal fun turnTextureViewOff() {
        backgroundImage.visibility = View.VISIBLE
        backgroundImageBlurred.visibility = View.VISIBLE
        autoFitTextureView.visibility = View.INVISIBLE
    }

    internal fun toggleTextureView(): Boolean {
        backgroundImage.visibility = autoFitTextureView.visibility.also {
            autoFitTextureView.visibility = backgroundImage.visibility
        }
        backgroundImageBlurred.visibility = backgroundImage.visibility
        return autoFitTextureView.visibility == View.VISIBLE
    }

    internal fun turnTextureAndImageViewOff() {
        backgroundImage.visibility = View.INVISIBLE
        backgroundImageBlurred.visibility = View.INVISIBLE
        autoFitTextureView.visibility = View.INVISIBLE
    }

    internal fun showLoading() {
        progressBar.visibility = View.VISIBLE
        progressBar.invalidate()
    }

    internal fun hideLoading() {
        progressBar.visibility = View.GONE
        progressBar.invalidate()
    }

    companion object {
        private val TAG = "PhotoEditorView"
        private val imgBlurSrcId = 5
        private val imgSrcId = 1
        private val brushSrcId = 2
        private val glFilterId = 3
        private val cameraPreviewId = 4
    }
}
