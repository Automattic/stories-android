package com.automattic.photoeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.RequiresPermission
import androidx.annotation.UiThread
import com.automattic.photoeditor.views.ViewType.BRUSH_DRAWING
import com.automattic.photoeditor.views.ViewType.STICKER_ANIMATED
import com.automattic.photoeditor.gesture.MultiTouchListener
import com.automattic.photoeditor.util.BitmapUtil
import com.automattic.photoeditor.views.PhotoEditorView
import com.automattic.photoeditor.views.ViewType
import com.automattic.photoeditor.views.added.AddedView
import com.automattic.photoeditor.views.added.AddedViewList
import com.automattic.photoeditor.views.brush.BrushDrawingView
import com.automattic.photoeditor.views.brush.BrushViewChangeListener
import com.automattic.photoeditor.views.filter.CustomEffect
import com.automattic.photoeditor.views.filter.PhotoFilter
import com.bumptech.glide.Glide
import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.composer.Mp4Composer
import com.daasuu.mp4compose.filter.GlFilter
import com.daasuu.mp4compose.filter.GlFilterGroup
import com.daasuu.mp4compose.filter.GlGifWatermarkFilter
import com.daasuu.mp4compose.filter.GlWatermarkFilter
import com.daasuu.mp4compose.filter.ViewPositionInfo

import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList
import kotlinx.android.synthetic.main.view_photo_editor_text.view.*
import java.io.FileInputStream

/**
 *
 *
 * This class in initialize by [PhotoEditor.Builder] using a builder pattern with multiple
 * editing attributes
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 18/01/2017
 */

class PhotoEditor private constructor(builder: Builder) :
    BrushViewChangeListener {
    private val layoutInflater: LayoutInflater
    private val context: Context
    private val parentView: PhotoEditorView
    private val imageView: ImageView
    private val deleteView: View?
    private val brushDrawingView: BrushDrawingView
    private val addedViews: AddedViewList
    private val redoViews: AddedViewList
    private var mOnPhotoEditorListener: OnPhotoEditorListener? = null
    private val isTextPinchZoomable: Boolean
    private val mDefaultTextTypeface: Typeface?
    private val mDefaultEmojiTypeface: Typeface?

    /**
     * Create a new instance and scalable touchview
     *
     * @return scalable multitouch listener
     */
    private val newMultiTouchListener: MultiTouchListener
        get() = MultiTouchListener(
            deleteView,
            parentView,
            imageView,
            isTextPinchZoomable,
            mOnPhotoEditorListener
        )

    /**
     * @return true is brush mode is enabled
     */
    val brushDrawableMode: Boolean?
        get() = brushDrawingView.brushDrawingMode

    /**
     * @return provide the size of eraser
     * @see PhotoEditor.setBrushEraserSize
     */
    val eraserSize: Float
        get() = brushDrawingView.eraserSize

    /**
     * @return provide the size of eraser
     * @see PhotoEditor.setBrushSize
     */
    /**
     * set the size of bursh user want to paint on canvas i.e [BrushDrawingView]
     *
     * @param size size of brush
     */
    var brushSize: Float
        get() = brushDrawingView.brushSize
        set(size) {
                brushDrawingView.brushSize = size
        }

    /**
     * @return provide the size of eraser
     * @see PhotoEditor.setBrushColor
     */
    /**
     * set brush color which user want to paint
     *
     * @param color color value for paint
     */
    var brushColor: Int
        get() = brushDrawingView.brushColor
        set(@ColorInt color) {
                brushDrawingView.brushColor = color
        }

    /**
     * Check if any changes made need to save
     *
     * @return true if nothing is there to change
     */
    val isCacheEmpty: Boolean
        get() = addedViews.size == 0 && redoViews.size == 0

    init {
        this.context = builder.context
        this.parentView = builder.parentView
        this.imageView = builder.imageView
        this.deleteView = builder.deleteView
        this.brushDrawingView = builder.brushDrawingView
        this.isTextPinchZoomable = builder.isTextPinchZoomable
        this.mDefaultTextTypeface = builder.textTypeface
        this.mDefaultEmojiTypeface = builder.emojiTypeface
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        brushDrawingView.setBrushViewChangeListener(this)
        addedViews = AddedViewList()
        redoViews = AddedViewList()
    }

    /**
     * This will add image on [PhotoEditorView] which you drag,rotate and scale using pinch
     * if [PhotoEditor.Builder.setPinchTextScalable] enabled
     *
     * @param desiredImage bitmap image you want to add
     */
    fun addImage(desiredImage: Bitmap) {
        val imageRootView = getLayout(ViewType.IMAGE)
        val imageView = imageRootView!!.findViewById<ImageView>(R.id.imgPhotoEditorImage)
        val frmBorder = imageRootView.findViewById<FrameLayout>(R.id.frmBorder)
        val imgClose = imageRootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)

        imageView.setImageBitmap(desiredImage)

        val multiTouchListenerInstance = newMultiTouchListener
        multiTouchListenerInstance.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
            override fun onClick() {
                val isBackgroundVisible = frmBorder.tag != null && frmBorder.tag as Boolean
                frmBorder.setBackgroundResource(if (isBackgroundVisible) 0 else R.drawable.rounded_border_tv)
                imgClose.visibility = if (isBackgroundVisible) View.GONE else View.VISIBLE
                frmBorder.tag = !isBackgroundVisible
            }

            override fun onLongClick() {}
        })

        imageRootView.setOnTouchListener(multiTouchListenerInstance)

        addViewToParent(imageRootView, ViewType.IMAGE)
    }

    fun addNewImageView(isAnimated: Boolean, uri: Uri) {
        val imageRootView = getLayout(ViewType.IMAGE)
        val imageView = imageRootView!!.findViewById<ImageView>(R.id.imgPhotoEditorImage)
        val frmBorder = imageRootView.findViewById<FrameLayout>(R.id.frmBorder)
        val imgClose = imageRootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)

        val multiTouchListenerInstance = newMultiTouchListener
        multiTouchListenerInstance.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
            override fun onClick() {
                val isBackgroundVisible = frmBorder.tag != null && frmBorder.tag as Boolean
                frmBorder.setBackgroundResource(if (isBackgroundVisible) 0 else R.drawable.rounded_border_tv)
                imgClose.visibility = if (isBackgroundVisible) View.GONE else View.VISIBLE
                frmBorder.tag = !isBackgroundVisible
            }

            override fun onLongClick() {}
        })

        imageRootView.setOnTouchListener(multiTouchListenerInstance)

        addViewToParent(imageRootView, if (isAnimated) ViewType.STICKER_ANIMATED else ViewType.IMAGE, uri)

        // now load the gif on this ImageView with Glide
        Glide.with(context)
            .load(uri)
            .into(imageView)
    }

    fun addNewImageView(isAnimated: Boolean): ImageView {
        val imageRootView = getLayout(ViewType.IMAGE)
        val imageView = imageRootView!!.findViewById<ImageView>(R.id.imgPhotoEditorImage)
        val frmBorder = imageRootView.findViewById<FrameLayout>(R.id.frmBorder)
        val imgClose = imageRootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)

        val multiTouchListenerInstance = newMultiTouchListener
        multiTouchListenerInstance.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
            override fun onClick() {
                val isBackgroundVisible = frmBorder.tag != null && frmBorder.tag as Boolean
                frmBorder.setBackgroundResource(if (isBackgroundVisible) 0 else R.drawable.rounded_border_tv)
                imgClose.visibility = if (isBackgroundVisible) View.GONE else View.VISIBLE
                frmBorder.tag = !isBackgroundVisible
            }

            override fun onLongClick() {}
        })

        imageRootView.setOnTouchListener(multiTouchListenerInstance)

        addViewToParent(imageRootView, if (isAnimated) ViewType.STICKER_ANIMATED else ViewType.IMAGE)

        return imageView
    }

    /**
     * This add the text on the [PhotoEditorView] with provided parameters
     * by default [TextView.setText] will be 18sp
     *
     * @param textTypeface typeface for custom font in the text
     * @param text text to display
     * @param colorCodeTextView text color to be displayed
     */
    @SuppressLint("ClickableViewAccessibility")
    fun addText(text: String, colorCodeTextView: Int, textTypeface: Typeface? = null, fontSizeSp: Float = 18f) {
        brushDrawingView.brushDrawingMode = false
        val textRootView = getLayout(ViewType.TEXT)
        val textInputTv = textRootView!!.findViewById<TextView>(R.id.tvPhotoEditorText)
        val imgClose = textRootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)
        val frmBorder = textRootView.findViewById<FrameLayout>(R.id.frmBorder)

        // hide cross and background borders for now
        imgClose.visibility = View.GONE
        frmBorder.setBackgroundResource(0)

        textInputTv.text = text
        textInputTv.setTextColor(colorCodeTextView)
        textInputTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
        if (textTypeface != null) {
            textInputTv.typeface = textTypeface
        }

        val multiTouchListenerInstance = newMultiTouchListener
        multiTouchListenerInstance.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
            override fun onClick() {
                val textInput = textInputTv.text.toString()
                val currentTextColor = textInputTv.currentTextColor
                if (mOnPhotoEditorListener != null) {
                    mOnPhotoEditorListener!!.onEditTextChangeListener(textRootView, textInput, currentTextColor)
                }
            }

            override fun onLongClick() {
                // TODO implement the DELETE action (hide every other view, allow this view to be dragged to the trash
                // bin)
            }
        })

        textRootView.setOnTouchListener(multiTouchListenerInstance)
        addViewToParent(textRootView, ViewType.TEXT)

        // now open TextEditor right away
        if (mOnPhotoEditorListener != null) {
            val textInput = textInputTv.text.toString()
            val currentTextColor = textInputTv.currentTextColor
            mOnPhotoEditorListener!!.onEditTextChangeListener(textRootView, textInput, currentTextColor)
        }
    }

    /**
     * This will update text and color on provided view
     *
     * @param view view on which you want update
     * @param inputText text to update [TextView]
     * @param colorCode color to update on [TextView]
     */
    fun editText(view: View, inputText: String, colorCode: Int) {
        editText(view, null, inputText, colorCode)
    }

    /**
     * This will update the text and color on provided view
     *
     * @param view root view where text view is a child
     * @param textTypeface update typeface for custom font in the text
     * @param inputText text to update [TextView]
     * @param colorCode color to update on [TextView]
     */
    fun editText(view: View, textTypeface: Typeface?, inputText: String, colorCode: Int) {
        val inputTextView = view.findViewById<TextView>(R.id.tvPhotoEditorText)
        if (inputTextView != null && addedViews.containsView(view) && !TextUtils.isEmpty(inputText)) {
            inputTextView.text = inputText
            if (textTypeface != null) {
                inputTextView.typeface = textTypeface
            }
            inputTextView.setTextColor(colorCode)
            parentView.updateViewLayout(view, view.layoutParams)
            val i = addedViews.indexOfView(view)
            if (i > -1) addedViews[i] = AddedView(view, addedViews[i].viewType)
        }
    }

    /**
     * Adds emoji to the [PhotoEditorView] which you drag,rotate and scale using pinch
     * if [PhotoEditor.Builder.setPinchTextScalable] enabled
     *
     * @param emojiName unicode in form of string to display emoji
     */
    fun addEmoji(emojiName: String) {
        addEmoji(null, emojiName)
    }

    /**
     * Adds emoji to the [PhotoEditorView] which you drag,rotate and scale using pinch
     * if [PhotoEditor.Builder.setPinchTextScalable] enabled
     *
     * @param emojiTypeface typeface for custom font to show emoji unicode in specific font
     * @param emojiName unicode in form of string to display emoji
     */
    fun addEmoji(emojiTypeface: Typeface?, emojiName: String) {
        brushDrawingView.brushDrawingMode = false
        val emojiRootView = getLayout(ViewType.EMOJI)
        val emojiTextView = emojiRootView!!.findViewById<TextView>(R.id.tvPhotoEditorText)
        val frmBorder = emojiRootView.findViewById<FrameLayout>(R.id.frmBorder)
        val imgClose = emojiRootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)

        if (emojiTypeface != null) {
            emojiTextView.typeface = emojiTypeface
        }
        emojiTextView.textSize = 56f
        emojiTextView.text = emojiName

        val multiTouchListenerInstance = newMultiTouchListener
        multiTouchListenerInstance.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
            override fun onClick() {
                val isBackgroundVisible = frmBorder.tag != null && frmBorder.tag as Boolean
                frmBorder.setBackgroundResource(if (isBackgroundVisible) 0 else R.drawable.rounded_border_tv)
                imgClose.visibility = if (isBackgroundVisible) View.GONE else View.VISIBLE
                frmBorder.tag = !isBackgroundVisible
            }

            override fun onLongClick() {}
        })
        emojiRootView.setOnTouchListener(multiTouchListenerInstance)
        addViewToParent(emojiRootView, ViewType.EMOJI)
    }

    /**
     * Add to root view from image,emoji and text to our parent view
     *
     * @param rootView rootview of image,text and emoji
     */
    private fun addViewToParent(rootView: View, viewType: ViewType, sourceUri: Uri? = null) {
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        parentView.addView(rootView, params)
        addedViews.add(AddedView(rootView, viewType, sourceUri))
        if (mOnPhotoEditorListener != null)
            mOnPhotoEditorListener!!.onAddViewListener(viewType, addedViews.size)
    }

    /**
     * Get root view by its type i.e image,text and emoji
     *
     * @param viewType image,text or emoji
     * @return rootview
     */
    private fun getLayout(viewType: ViewType): View? {
        var rootView: View? = null
        when (viewType) {
            ViewType.TEXT -> {
                rootView = layoutInflater.inflate(R.layout.view_photo_editor_text, null)
                if (rootView.tvPhotoEditorText != null && mDefaultTextTypeface != null) {
                    rootView.tvPhotoEditorText.gravity = Gravity.CENTER
                    if (mDefaultEmojiTypeface != null) {
                        rootView.tvPhotoEditorText.typeface = mDefaultTextTypeface
                    }
                }
            }
            ViewType.IMAGE -> rootView = layoutInflater.inflate(R.layout.view_photo_editor_image, null)
            ViewType.EMOJI -> {
                rootView = layoutInflater.inflate(R.layout.view_photo_editor_text, null)
                val txtTextEmoji = rootView.tvPhotoEditorText
                if (txtTextEmoji != null) {
                    if (mDefaultEmojiTypeface != null) {
                        txtTextEmoji.typeface = mDefaultEmojiTypeface
                    }
                    txtTextEmoji.gravity = Gravity.CENTER
                    txtTextEmoji.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }
            }
        }

        if (rootView != null) {
            // We are setting tag as ViewType to identify what type of the view it is
            // when we remove the view from stack i.e onRemoveViewListener(ViewType viewType, int numberOfAddedViews);
            rootView.tag = viewType
            val imgClose = rootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)
            val finalRootView = rootView
            imgClose?.setOnClickListener { viewUndo(finalRootView, viewType) }
        }
        return rootView
    }

    /**
     * Enable/Disable drawing mode to draw on [PhotoEditorView]
     *
     * @param brushDrawingMode true if mode is enabled
     */
    fun setBrushDrawingMode(brushDrawingMode: Boolean) {
            brushDrawingView.brushDrawingMode = brushDrawingMode
    }

    /**
     * set opacity/transparency of brush while painting on [BrushDrawingView]
     *
     * @param opacity opacity is in form of percentage
     */
    fun setOpacity(@IntRange(from = 0, to = 100) opacity: Int) {
        brushDrawingView.setOpacity((opacity / 100.0 * 255.0).toInt())
    }

    /**
     * set the eraser size
     * <br></br>
     * **Note :** Eraser size is different from the normal brush size
     *
     * @param brushEraserSize size of eraser
     */
    fun setBrushEraserSize(brushEraserSize: Float) {
        brushDrawingView.setBrushEraserSize(brushEraserSize)
    }

    internal fun setBrushEraserColor(@ColorInt color: Int) {
        brushDrawingView.setBrushEraserColor(color)
    }

    /**
     *
     *
     * Its enables eraser mode after that whenever user drags on screen this will erase the existing
     * paint
     * <br></br>
     * **Note** : This eraser will work on paint views only
     *
     *
     */
    fun brushEraser() {
        brushDrawingView.brushEraser()
    }

    /*private void viewUndo() {
        if (addedViews.size() > 0) {
            parentView.removeView(addedViews.remove(addedViews.size() - 1));
            if (mOnPhotoEditorListener != null)
                mOnPhotoEditorListener.onRemoveViewListener(addedViews.size());
        }
    }*/

    private fun viewUndo(removedView: View, viewType: ViewType) {
        if (addedViews.size > 0) {
            if (addedViews.containsView(removedView)) {
                parentView.removeView(removedView)
                val removedViewWithData = addedViews.removeView(removedView)
                if (removedViewWithData != null) {
                    redoViews.add(removedViewWithData)
                }
                if (mOnPhotoEditorListener != null) {
                    mOnPhotoEditorListener!!.onRemoveViewListener(addedViews.size)
                    mOnPhotoEditorListener!!.onRemoveViewListener(viewType, addedViews.size)
                }
            }
        }
    }

    /**
     * Undo the last operation perform on the [PhotoEditor]
     *
     * @return true if there nothing more to undo
     */
    fun undo(): Boolean {
        if (addedViews.size > 0) {
            val removeView = addedViews[addedViews.size - 1]
            if (removeView.view is BrushDrawingView) {
                return brushDrawingView.undo()
            } else {
                addedViews.removeAt(addedViews.size - 1)
                parentView.removeView(removeView.view)
                redoViews.add(removeView)
            }
            mOnPhotoEditorListener?.onRemoveViewListener(addedViews.size)
            val viewTag = removeView.view.tag
            if (viewTag != null && viewTag is ViewType) {
                mOnPhotoEditorListener?.onRemoveViewListener(viewTag, addedViews.size)
            }
        }
        return addedViews.size != 0
    }

    /**
     * Redo the last operation perform on the [PhotoEditor]
     *
     * @return true if there nothing more to redo
     */
    fun redo(): Boolean {
        if (redoViews.size > 0) {
            val redoView = redoViews[redoViews.size - 1]
            if (redoView.view is BrushDrawingView) {
                return brushDrawingView != null && brushDrawingView.redo()
            } else {
                redoViews.removeAt(redoViews.size - 1)
                parentView.addView(redoView.view)
                addedViews.add(redoView)
            }
            val viewTag = redoView.view.tag
            if (viewTag != null && viewTag is ViewType) {
                mOnPhotoEditorListener?.onAddViewListener(viewTag, addedViews.size)
            }
        }
        return redoViews.size != 0
    }

    private fun clearBrushAllViews() {
        brushDrawingView.clearAll()
    }

    /**
     * Removes all the edited operations performed [PhotoEditorView]
     * This will also clear the undo and redo stack
     */
    fun clearAllViews() {
        for (i in addedViews.indices) {
            parentView.removeView(addedViews[i].view)
        }

        if (addedViews.containsView(brushDrawingView)) {
            parentView.addView(brushDrawingView)
        }
        addedViews.clear()
        redoViews.clear()
        clearBrushAllViews()
    }

    /**
     * Remove all helper boxes from views
     */
    @UiThread
    fun clearHelperBox() {
        for (i in 0 until parentView.childCount) {
            val childAt = parentView.getChildAt(i)
            val frmBorder = childAt.findViewById<FrameLayout>(R.id.frmBorder)
            frmBorder?.setBackgroundResource(0)
            val imgClose = childAt.findViewById<ImageView>(R.id.imgPhotoEditorClose)
            if (imgClose != null) {
                imgClose.visibility = View.GONE
            }
        }
    }

    /**
     * Setup of custom effect using effect type and set parameters values
     *
     * @param customEffect [CustomEffect.Builder.setParameter]
     */
    fun setFilterEffect(customEffect: CustomEffect) {
        parentView.setFilterEffect(customEffect)
    }

    /**
     * Set pre-define filter available
     *
     * @param filterType type of filter want to apply [PhotoEditor]
     */
    fun setFilterEffect(filterType: PhotoFilter) {
        parentView.setFilterEffect(filterType)
    }

    fun turnTextureViewOn() {
        parentView.turnTextureViewOn()
    }

    fun turnTextureViewOff() {
        parentView.turnTextureViewOff()
    }

    fun toggleTextureView(): Boolean {
        return parentView.toggleTextureView()
    }

    fun turnTextureAndImageViewOff() {
        parentView.turnTextureAndImageViewOff()
    }

    fun isTextureViewVisible(): Boolean {
        return parentView.textureView.visibility == View.VISIBLE
    }

    /**
     * A callback to save the edited media asynchronously
     */
    interface OnSaveListener {
        /**
         * Call when edited media is saved successfully on given path
         *
         * @param filePath path on which file is saved
         */
        fun onSuccess(filePath: String)

        /**
         * Call when failed to saved media on given path
         *
         * @param exception exception thrown while saving media
         */
        fun onFailure(exception: Exception)
    }

    /**
     * A callback to save the edited media asynchronously
     */
    interface OnSaveWithCancelListener {
        /**
         * Call when edited media is saved successfully on given path
         *
         * @param filePath path on which file is saved
         */
        fun onSuccess(filePath: String)

        /**
         * Call when failed to saved media on given path
         *
         * @param exception exception thrown while saving media
         */
        fun onFailure(exception: Exception)

        /**
         * Call when user cancelled operation
         */
        fun onCancel(noAddedViews: Boolean = false)
    }

    /**
     * @param imagePath path on which image to be saved
     * @param onSaveListener callback for saving image
     * @see OnSaveListener
     *
     */
    @SuppressLint("StaticFieldLeak")
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    @Deprecated("Use {@link #saveAsFile(String, OnSaveListener)} instead")
    fun saveImage(imagePath: String, onSaveListener: OnSaveListener) {
        saveAsFile(imagePath, onSaveListener)
    }

    /**
     * Save the edited image on given path
     *
     * @param imagePath path on which image to be saved
     * @param onSaveListener callback for saving image
     * @see OnSaveListener
     */
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    fun saveAsFile(imagePath: String, onSaveListener: OnSaveListener) {
        saveAsFile(imagePath, SaveSettings.Builder().build(), onSaveListener)
    }

    /**
     * Save the edited image on given path
     *
     * @param imagePath path on which image to be saved
     * @param saveSettings builder for multiple save options [SaveSettings]
     * @param onSaveListener callback for saving image
     * @see OnSaveListener
     */
    @SuppressLint("StaticFieldLeak")
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    fun saveAsFile(
        imagePath: String,
        saveSettings: SaveSettings,
        onSaveListener: OnSaveListener
    ) {
        Log.d(TAG, "Image Path: $imagePath")
        parentView.saveFilter(object : OnSaveBitmap {
            override fun onBitmapReady(saveBitmap: Bitmap) {
                object : AsyncTask<String, String, Exception>() {
                    override fun onPreExecute() {
                        super.onPreExecute()
                        clearHelperBox()
                    }

                    @SuppressLint("MissingPermission")
                    override fun doInBackground(vararg strings: String): Exception? {
                        // Create a media file name
                        val file = File(imagePath)
                        try {
                            val out = FileOutputStream(file, false)
                            val wholeBitmap = if (saveSettings.isTransparencyEnabled)
                                BitmapUtil.removeTransparency(BitmapUtil.createBitmapFromView(parentView))
                            else
                                BitmapUtil.createBitmapFromView(parentView)
                            wholeBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                            out.flush()
                            out.close()
                            Log.d(TAG, "Filed Saved Successfully")
                            return null
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(TAG, "Failed to save File")
                            return e
                        }
                    }

                    override fun onPostExecute(e: Exception?) {
                        super.onPostExecute(e)
                        if (e == null) {
                            // Clear all views if its enabled in save settings
                            if (saveSettings.isClearViewsEnabled) clearAllViews()
                            onSaveListener.onSuccess(imagePath)
                        } else {
                            onSaveListener.onFailure(e)
                        }
                    }
                }.execute()
            }

            override fun onFailure(e: Exception) {
                onSaveListener.onFailure(e)
            }
        })
    }

    /**
     * Save the edited VIDEO on given path
     *
     * @param videoInputPath path on which video to be saved
     * @param saveSettings builder for multiple save options [SaveSettings]
     * @param onSaveListener callback for saving video
     * @see OnSaveListener
     */
    @SuppressLint("StaticFieldLeak")
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    fun saveVideoAsFile(
        videoInputPath: String,
        videoOutputPath: String,
        saveSettings: SaveSettings,
        onSaveListener: OnSaveWithCancelListener
    ) {
        Log.d(TAG, "Video Path: $videoInputPath")
        var widthParent = parentView.getWidth()
        var heightParent = parentView.getHeight()

        if (addedViews.size == 0) {
            onSaveListener.onCancel(true)
            return
        }

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoInputPath)
        var width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        var height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        var rotation = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION))
        // if rotation is vertical, then swap height/width
        if (rotation == 90 || rotation == 270) {
            width = height.also { height = width }
        }
        retriever.release()

        // get the images currently on top of the screen, and add them as Filters to the mp4composer
        val filterCollection = ArrayList<GlFilter>()
        for (v in addedViews) {
            val viewPositionInfo = ViewPositionInfo(
                widthParent,
                heightParent,
                v.view.width,
                v.view.height,
                v.view.matrix
            )
            when (v.viewType) {
                STICKER_ANIMATED -> {
                    val file = File(v.uri?.path)
                    val fileInputStream = FileInputStream(file)
                    filterCollection.add(GlGifWatermarkFilter(context, fileInputStream, viewPositionInfo))
                }
                else -> {
                    clearHelperBox()
                    filterCollection.add(GlWatermarkFilter(BitmapUtil.createBitmapFromView(v.view), viewPositionInfo))
                }
            }
        }

        Mp4Composer(videoInputPath, videoOutputPath)
            .size(width, height)
            .fillMode(FillMode.PRESERVE_ASPECT_FIT)
            .filter(GlFilterGroup(filterCollection))
            .listener(object : Mp4Composer.Listener {
                override fun onProgress(progress: Double) {
                    Log.d(TAG, "onProgress = $progress")
                    // TODO: show progress to user
                }

                override fun onCompleted() {
                    Log.d(TAG, "onCompleted()")
                    onSaveListener.onSuccess(videoOutputPath)
                }

                override fun onCanceled() {
                    Log.d(TAG, "onCanceled")
                    onSaveListener.onCancel()
                }

                override fun onFailed(exception: Exception) {
                    Log.e(TAG, "onFailed()", exception)
                    onSaveListener.onFailure(exception)
                }
            })
            .start()
    }

    /**
     * Produce a new video with a static background image
     *
     * @param saveSettings builder for multiple save options [SaveSettings]
     * @param onSaveListener callback for saving video
     * @see OnSaveListener
     */
    @SuppressLint("StaticFieldLeak")
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    fun saveVideoFromStaticBackgroundAsFile(
        videoOutputPath: String,
        saveSettings: SaveSettings,
        onSaveListener: OnSaveWithCancelListener
    ) {
        var widthParent = parentView.getWidth()
        var heightParent = parentView.getHeight()

        // get the images currently on top of the screen, and add them as Filters to the mp4composer
        val filterCollection = ArrayList<GlFilter>()
        for (v in addedViews) {
            val viewPositionInfo = ViewPositionInfo(
                widthParent,
                heightParent,
                v.view.width,
                v.view.height,
                v.view.matrix
            )
            when (v.viewType) {
                ViewType.STICKER_ANIMATED -> {
                    val file = File(v.uri?.path)
                    val fileInputStream = FileInputStream(file)
                    filterCollection.add(GlGifWatermarkFilter(context, fileInputStream, viewPositionInfo))
                }
                else -> {
                    clearHelperBox()
                    filterCollection.add(GlWatermarkFilter(BitmapUtil.createBitmapFromView(v.view), viewPositionInfo))
                }
            }
        }

        // take the static background image
        val bmp = createBitmapFromView(parentView.source)

        Mp4Composer(bmp, videoOutputPath)
            .size(bmp.width, bmp.height) // FIXME check whether these are the right values or not
            .fillMode(FillMode.PRESERVE_ASPECT_FIT)
            .filter(GlFilterGroup(filterCollection))
            .listener(object : Mp4Composer.Listener {
                override fun onProgress(progress: Double) {
                    Log.d(TAG, "onProgress = $progress")
                    // TODO: show progress to user
                }

                override fun onCompleted() {
                    Log.d(TAG, "onCompleted()")
                    onSaveListener.onSuccess(videoOutputPath)
                }

                override fun onCanceled() {
                    Log.d(TAG, "onCanceled")
                    onSaveListener.onCancel()
                }

                override fun onFailed(exception: Exception) {
                    Log.e(TAG, "onFailed()", exception)
                    onSaveListener.onFailure(exception)
                }
            })
            .start()
    }

    /**
     * Save the edited image as bitmap
     *
     * @param onSaveBitmap callback for saving image as bitmap
     * @see OnSaveBitmap
     */
    @SuppressLint("StaticFieldLeak")
    fun saveAsBitmap(onSaveBitmap: OnSaveBitmap) {
        saveAsBitmap(SaveSettings.Builder().build(), onSaveBitmap)
    }

    /**
     * Save the edited image as bitmap
     *
     * @param saveSettings builder for multiple save options [SaveSettings]
     * @param onSaveBitmap callback for saving image as bitmap
     * @see OnSaveBitmap
     */
    @SuppressLint("StaticFieldLeak")
    fun saveAsBitmap(
        saveSettings: SaveSettings,
        onSaveBitmap: OnSaveBitmap
    ) {
        parentView.saveFilter(object : OnSaveBitmap {
            override fun onBitmapReady(saveBitmap: Bitmap) {
                object : AsyncTask<String, String, Bitmap>() {
                    override fun onPreExecute() {
                        super.onPreExecute()
                        clearHelperBox()
                    }

                    override fun doInBackground(vararg strings: String): Bitmap? {
                        return if (saveSettings.isTransparencyEnabled)
                            BitmapUtil.removeTransparency(BitmapUtil.createBitmapFromView(parentView))
                        else
                            BitmapUtil.createBitmapFromView(parentView)
                    }

                    override fun onPostExecute(bitmap: Bitmap?) {
                        super.onPostExecute(bitmap)
                        if (bitmap != null) {
                            if (saveSettings.isClearViewsEnabled) clearAllViews()
                            onSaveBitmap.onBitmapReady(bitmap)
                        } else {
                            onSaveBitmap.onFailure(Exception("Failed to load the bitmap"))
                        }
                    }
                }.execute()
            }

            override fun onFailure(e: Exception) {
                onSaveBitmap.onFailure(e)
            }
        })
    }

    // TODO to be used in conjunction with mp4composer
    private fun createBitmapFromView(v: View): Bitmap {
        val bitmap = Bitmap.createBitmap(v.width,
            v.height,
            Bitmap.Config.ARGB_8888)
//        val c = Canvas(bitmap)
//        v.draw(c)
//        return bitmap

        // FIXME creating a scaled bitmap
        // if we don't scale the bitmap we get an BufferOverflowException:
        // java.nio.BufferOverflowException
        // 2019-07-09 16:41:50.965 9252-9536/com.automattic.loops W/System.err:
        //      at java.nio.DirectByteBuffer.put(DirectByteBuffer.java:291)
        // 2019-07-09 16:41:50.965 9252-9536/com.automattic.loops W/System.err:
        //      at java.nio.ByteBuffer.put(ByteBuffer.java:642)
        // 2019-07-09 16:41:50.965 9252-9536/com.automattic.loops W/System.err:
        //      at com.daasuu.mp4compose.composer.VideoComposer.stepPipelineStaticImageBackground(VideoComposer.java:173)
        val bitmap2 = Bitmap.createScaledBitmap(bitmap, 480, 720, true)
        bitmap.recycle()
        val c = Canvas(bitmap2)
        v.draw(c)
        return bitmap2
    }

    /**
     * Callback on editing operation perform on [PhotoEditorView]
     *
     * @param onPhotoEditorListener [OnPhotoEditorListener]
     */
    fun setOnPhotoEditorListener(onPhotoEditorListener: OnPhotoEditorListener) {
        this.mOnPhotoEditorListener = onPhotoEditorListener
    }

    override fun onViewAdd(brushDrawingView: BrushDrawingView) {
        if (redoViews.size > 0) {
            redoViews.removeAt(redoViews.size - 1)
        }
        addedViews.add(AddedView(brushDrawingView, BRUSH_DRAWING))
        mOnPhotoEditorListener?.onAddViewListener(ViewType.BRUSH_DRAWING, addedViews.size)
    }

    override fun onViewRemoved(brushDrawingView: BrushDrawingView) {
        if (addedViews.size > 0) {
            val removeView = addedViews.removeAt(addedViews.size - 1)
            if (removeView.view !is BrushDrawingView) {
                parentView.removeView(removeView.view)
            }
            redoViews.add(removeView)
        }
        mOnPhotoEditorListener?.onRemoveViewListener(addedViews.size)
        mOnPhotoEditorListener?.onRemoveViewListener(ViewType.BRUSH_DRAWING, addedViews.size)
    }

    override fun onStartDrawing() {
        mOnPhotoEditorListener?.onStartViewChangeListener(ViewType.BRUSH_DRAWING)
    }

    override fun onStopDrawing() {
        mOnPhotoEditorListener?.onStopViewChangeListener(ViewType.BRUSH_DRAWING)
    }

    fun anyStickersAdded(): Boolean {
        for (v: AddedView in addedViews) {
            if (v.viewType == STICKER_ANIMATED) {
                return true
            }
        }
        return false
    }

    /**
     * Builder pattern to define [PhotoEditor] Instance
     */
    class Builder
    /**
     * Building a PhotoEditor which requires a Context and PhotoEditorView
     * which we have setup in our xml layout
     *
     * @param context context
     * @param photoEditorView [PhotoEditorView]
     */
        (val context: Context, val parentView: PhotoEditorView) {
        val imageView: ImageView
        var deleteView: View? = null
        val brushDrawingView: BrushDrawingView
        var textTypeface: Typeface? = null
        var emojiTypeface: Typeface? = null
        // By Default pinch zoom on text is enabled
        var isTextPinchZoomable = true

        init {
            imageView = parentView.source
            brushDrawingView = parentView.brush
        }

        internal fun setDeleteView(deleteView: View): Builder {
            this.deleteView = deleteView
            return this
        }

        /**
         * set default text font to be added on image
         *
         * @param textTypeface typeface for custom font
         * @return [Builder] instant to build [PhotoEditor]
         */
        fun setDefaultTextTypeface(textTypeface: Typeface): Builder {
            this.textTypeface = textTypeface
            return this
        }

        /**
         * set default font specific to add emojis
         *
         * @param emojiTypeface typeface for custom font
         * @return [Builder] instant to build [PhotoEditor]
         */
        fun setDefaultEmojiTypeface(emojiTypeface: Typeface): Builder {
            this.emojiTypeface = emojiTypeface
            return this
        }

        /**
         * set false to disable pinch to zoom on text insertion.By deafult its true
         *
         * @param isTextPinchZoomable flag to make pinch to zoom
         * @return [Builder] instant to build [PhotoEditor]
         */
        fun setPinchTextScalable(isTextPinchZoomable: Boolean): Builder {
            this.isTextPinchZoomable = isTextPinchZoomable
            return this
        }

        /**
         * @return build PhotoEditor instance
         */
        fun build(): PhotoEditor {
            return PhotoEditor(this)
        }
    }

    companion object {
        private val TAG = "PhotoEditor"

        private fun convertEmoji(emoji: String): String {
            var returnedEmoji: String
            try {
                val convertEmojiToInt = Integer.parseInt(emoji.substring(2), 16)
                returnedEmoji = String(Character.toChars(convertEmojiToInt))
            } catch (e: NumberFormatException) {
                returnedEmoji = ""
            }

            return returnedEmoji
        }

        /**
         * Provide the list of emoji in form of unicode string
         *
         * @param context context
         * @return list of emoji unicode
         */
        fun getEmojis(context: Context): ArrayList<String> {
            val convertedEmojiList = ArrayList<String>()
            val emojiList = context.resources.getStringArray(R.array.photo_editor_emoji)
            for (emojiUnicode in emojiList) {
                convertedEmojiList.add(convertEmoji(emojiUnicode))
            }
            return convertedEmojiList
        }
    }
}
