package com.automattic.photoeditor.views.background.fixed

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.util.AttributeSet
import com.github.chrisbanes.photoview.PhotoView

/**
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.2
 * @since 5/21/2018
 */
class BackgroundImageView : PhotoView {
    private var mOnImageChangedListener: OnImageChangedListener? = null

    val bitmap: Bitmap?
        get() = if (drawable != null) {
            (drawable as BitmapDrawable).bitmap
        } else null

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    fun setOnImageChangedListener(onImageChangedListener: OnImageChangedListener) {
        mOnImageChangedListener = onImageChangedListener
    }

    interface OnImageChangedListener {
        fun onBitmapLoaded(sourceBitmap: Bitmap?)
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageIcon(icon: Icon?) {
        super.setImageIcon(icon)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageMatrix(matrix: Matrix) {
        super.setImageMatrix(matrix)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageState(state: IntArray, merge: Boolean) {
        super.setImageState(state, merge)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageTintList(tint: ColorStateList?) {
        super.setImageTintList(tint)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageTintMode(tintMode: PorterDuff.Mode?) {
        super.setImageTintMode(tintMode)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }

    override fun setImageLevel(level: Int) {
        super.setImageLevel(level)
        mOnImageChangedListener?.onBitmapLoaded(bitmap)
    }
}
