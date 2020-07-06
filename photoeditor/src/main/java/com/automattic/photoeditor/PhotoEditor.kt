package com.automattic.photoeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.RequiresPermission
import androidx.annotation.UiThread
import androidx.core.widget.TextViewCompat
import androidx.emoji.text.EmojiCompat
import com.automattic.photoeditor.gesture.MultiTouchListener
import com.automattic.photoeditor.gesture.MultiTouchListener.OnMultiTouchListener
import com.automattic.photoeditor.state.AuthenticationHeadersInterface
import com.automattic.photoeditor.util.BitmapUtil
import com.automattic.photoeditor.util.FileUtils
import com.automattic.photoeditor.views.PhotoEditorView
import com.automattic.photoeditor.views.ViewType
import com.automattic.photoeditor.views.ViewType.EMOJI
import com.automattic.photoeditor.views.ViewType.TEXT
import com.automattic.photoeditor.views.added.AddedView
import com.automattic.photoeditor.views.added.AddedViewInfo
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
import kotlinx.android.synthetic.main.view_photo_editor_emoji.view.*
import kotlinx.android.synthetic.main.view_photo_editor_text.view.*
import java.io.File
import java.io.FileInputStream
import java.util.ArrayList
import java.lang.ref.WeakReference

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
    private var workAreaRect: Rect?
    private val brushDrawingView: BrushDrawingView
    private val addedViews: AddedViewList
    private val redoViews: AddedViewList
    private var mOnPhotoEditorListener: OnPhotoEditorListener? = null
    private val isTextPinchZoomable: Boolean
    private val mDefaultTextTypeface: Typeface?
    private val mDefaultEmojiTypeface: Typeface?
    private val authenticationHeadersInterface: AuthenticationHeadersInterface?

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
     */
    /**
     * set the size of bursh user want to paint on canvas i.e [BrushDrawingView]
     *
     */
    var brushSize: Float
        get() = brushDrawingView.brushSize
        set(size) {
                brushDrawingView.brushSize = size
        }

    /**
     * @return provide the size of eraser
     */
    /**
     * set brush color which user want to paint
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

    val source: ImageView
        get() = parentView.source

    val composedCanvas: PhotoEditorView
        get() = parentView

    init {
        this.context = builder.context
        this.parentView = builder.parentView
        this.imageView = builder.imageView
        this.deleteView = builder.deleteView
        this.workAreaRect = builder.workAreaRect
        this.brushDrawingView = builder.brushDrawingView
        this.isTextPinchZoomable = builder.isTextPinchZoomable
        this.mDefaultTextTypeface = builder.textTypeface
        this.mDefaultEmojiTypeface = builder.emojiTypeface
        this.authenticationHeadersInterface = builder.authenticationHeadersInterface
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        brushDrawingView.setBrushViewChangeListener(this)
        addedViews = AddedViewList()
        redoViews = AddedViewList()
    }

    fun updateWorkAreaRect(workAreaBounds: Rect) {
        this.workAreaRect = workAreaBounds
    }

    private fun getNewMultitouchListener(mainView: View? = null): MultiTouchListener {
            return MultiTouchListener(
                mainView,
                deleteView,
                workAreaRect,
                parentView,
                imageView,
                isTextPinchZoomable,
                mOnPhotoEditorListener,
                object : OnMultiTouchListener {
                    override fun onEditTextClickListener(text: String, colorCode: Int) {
                        // no op
                    }

                    override fun onRemoveViewListener(removedView: View) {
                        // here do actually remove the view
                        val viewType = removedView.tag as ViewType
                        viewUndo(removedView, viewType)
                    }

                    override fun onRemoveViewReadyListener(removedView: View, ready: Boolean) {
                        mOnPhotoEditorListener?.onRemoveViewReadyListener(removedView, ready)
                    }
                }
            )
    }

    /**
     * This will add image on [PhotoEditorView] which you drag,rotate and scale using pinch
     * if [PhotoEditor.Builder.setPinchTextScalable] enabled
     *
     * @param desiredImage bitmap image you want to add
     */
    fun addImage(desiredImage: Bitmap) {
        getLayout(ViewType.IMAGE)?.apply {
            val imageView = findViewById<ImageView>(R.id.imgPhotoEditorImage)

            imageView.setImageBitmap(desiredImage)

            addViewToParent(this, ViewType.IMAGE)
        }
    }

    fun addNewImageView(isAnimated: Boolean, uri: Uri) {
        getLayout(ViewType.IMAGE)?.apply {
            val imageView = findViewById<ImageView>(R.id.imgPhotoEditorImage)

            addViewToParent(this, if (isAnimated) ViewType.STICKER_ANIMATED else ViewType.IMAGE, uri)

            // now load the gif on this ImageView with Glide
            Glide.with(context)
                .load(uri)
                .into(imageView)
        }
    }

    fun addNewImageView(isAnimated: Boolean): ImageView? {
        getLayout(ViewType.IMAGE)?.apply {
            val imageView = findViewById<ImageView>(R.id.imgPhotoEditorImage)

            addViewToParent(this, if (isAnimated) ViewType.STICKER_ANIMATED else ViewType.IMAGE)

            return imageView
        }
        return null
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
    fun addText(
        text: String,
        colorCodeTextView: Int,
        textTypeface: Typeface? = null,
        fontSizeSp: Float = 18f,
        isViewReadd: Boolean = false
    ): View? {
        brushDrawingView.brushDrawingMode = false
        val view: View?
        view = getLayout(ViewType.TEXT)?.apply {
            val textInputTv = findViewById<TextView>(R.id.tvPhotoEditorText)

            textInputTv.text = text
            textInputTv.setTextColor(colorCodeTextView)
            textInputTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
//            textInputTv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            if (textTypeface != null) {
                textInputTv.typeface = textTypeface
            }

            val multiTouchListenerInstance = getNewMultitouchListener() // newMultiTouchListener
            multiTouchListenerInstance.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
                override fun onClick() {
                    val textInput = textInputTv.text.toString()
                    val currentTextColor = textInputTv.currentTextColor
                    mOnPhotoEditorListener?.onEditTextChangeListener(this@apply, textInput, currentTextColor, false)
                }

                override fun onLongClick() {
                    // no op
                }
            })

            setOnTouchListener(multiTouchListenerInstance)
            addViewToParent(this, ViewType.TEXT)

            // now open TextEditor right away if this is new text being added
            if (mOnPhotoEditorListener != null && !isViewReadd) {
                val textInput = textInputTv.text.toString()
                val currentTextColor = textInputTv.currentTextColor
                mOnPhotoEditorListener?.onEditTextChangeListener(this, textInput, currentTextColor, true)
            }
        }
        return view
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
            if (i > -1) {
                addedViews[i] = AddedView.buildAddedViewFromView(view, addedViews[i].viewType)
            }
        }
    }

    /**
     * Adds emoji to the [PhotoEditorView] which you drag,rotate and scale using pinch
     * if [PhotoEditor.Builder.setPinchTextScalable] enabled
     *
     * @param emojiName unicode in form of string to display emoji
     */
    fun addEmoji(emojiName: String): View? {
        return addEmoji(null, emojiName)
    }

    /**
     * Adds emoji to the [PhotoEditorView] which you drag,rotate and scale using pinch
     * if [PhotoEditor.Builder.setPinchTextScalable] enabled
     *
     * @param emojiTypeface typeface for custom font to show emoji unicode in specific font
     * @param emojiName unicode in form of string to display emoji
     */
    fun addEmoji(emojiTypeface: Typeface?, emojiName: String): View? {
        brushDrawingView.brushDrawingMode = false
        val view = getLayout(ViewType.EMOJI)
        view?.apply {
            val emojiTextView = findViewById<TextView>(R.id.tvPhotoEditorEmoji)

            if (emojiTypeface != null) {
                emojiTextView.typeface = emojiTypeface
                emojiTextView.text = emojiName
            } else {
                // if user did not set a specific emojiTypeface, use EmojiCompat to process
                // the string and make sure we have an emoji that can be rendered
                EmojiCompat.get().registerInitCallback(object : EmojiCompat.InitCallback() {
                    val regularTextViewRef = WeakReference(emojiTextView)
                    override fun onInitialized() {
                        EmojiCompat.get().unregisterInitCallback(this)

                        val regularTextView = regularTextViewRef.get()
                        if (regularTextView != null) {
                            val compat = EmojiCompat.get()
                            regularTextView.text = compat.process(emojiName)
                        }
                    }

                    override fun onFailed(throwable: Throwable?) {
                        EmojiCompat.get().unregisterInitCallback(this)

                        // just fallback to setting the text
                        emojiTextView.text = emojiName
                    }
                })
            }

            val multiTouchListenerInstance = getNewMultitouchListener(this) // newMultiTouchListener
            multiTouchListenerInstance.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
                override fun onClick() {
                }

                override fun onLongClick() {
                    // no op
                }
            })
            touchableArea.setOnTouchListener(multiTouchListenerInstance)
            // setOnTouchListener(multiTouchListenerInstance)
            addViewToParent(this, ViewType.EMOJI)
        }
        return view
    }

    /**
     * Add to root view from image,emoji and text to our parent view
     *
     * @param rootView rootview of image,text and emoji
     */
    fun addViewToParent(rootView: View, viewType: ViewType, sourceUri: Uri? = null) {
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        parentView.addView(rootView, params)
        addedViews.add(AddedView.buildAddedViewFromView(rootView, viewType, sourceUri))
        mOnPhotoEditorListener?.onAddViewListener(viewType, addedViews.size)
    }

    fun addViewToParentWithTouchListener(addedView: AddedView) {
        addedView.view?.let {
            addViewToParentWithTouchListener(it, addedView.viewType, addedView.uri)
        } ?: buildViewFromAddedViewInfo(addedView.viewInfo, addedView.viewType)
    }

    private fun buildViewFromAddedViewInfo(addedViewInfo: AddedViewInfo, viewType: ViewType): View? {
        var view: View? = null
        when (viewType) {
            EMOJI -> {
                // create emoji view layout
                view = addEmoji(addedViewInfo.addedViewTextInfo.text)
                // apply specific TextView parameters for emoji (fontsize)
                val emojiTextView = view?.findViewById<TextView>(R.id.tvPhotoEditorEmoji)
                emojiTextView?.textSize = addedViewInfo.addedViewTextInfo.fontSizeSp
            }
            TEXT -> {
                // create TEXT view layout
                view = addText(
                    text = addedViewInfo.addedViewTextInfo.text,
                    colorCodeTextView = addedViewInfo.addedViewTextInfo.textColor,
                    isViewReadd = true
                )
                // apply specific TextView parameters for text (fontsize, text color)
                val normalTextView = view?.findViewById<TextView>(R.id.tvPhotoEditorText)
                normalTextView?.textSize = addedViewInfo.addedViewTextInfo.fontSizeSp
                normalTextView?.setTextColor(addedViewInfo.addedViewTextInfo.textColor)
            }
        }

        // now apply all common parameters to newly created view object
        view?.let {
            it.rotation = addedViewInfo.rotation
            it.translationX = addedViewInfo.translationX
            it.translationY = addedViewInfo.translationY
            it.scaleX = addedViewInfo.scale
            it.scaleY = addedViewInfo.scale
        }

        return view
    }

    private fun addViewToParentWithTouchListener(rootView: View, viewType: ViewType, sourceUri: Uri? = null) {
        val multiTouchListenerInstance = getNewMultitouchListener(rootView) // newMultiTouchListener
        when {
            viewType == EMOJI -> {
                multiTouchListenerInstance.setOnGestureControl(object :
                    MultiTouchListener.OnGestureControl {
                    override fun onClick() {
                        // TODO implement emoji linking
                    }

                    override fun onLongClick() {
                        // no op
                    }
                })
                rootView.touchableArea.setOnTouchListener(multiTouchListenerInstance)
            }

            viewType == TEXT -> {
                val textInputTv = rootView.tvPhotoEditorText
                multiTouchListenerInstance.setOnGestureControl(object :
                    MultiTouchListener.OnGestureControl {
                    override fun onClick() {
                        val textInput = textInputTv.text.toString()
                        val currentTextColor = textInputTv.currentTextColor
                        mOnPhotoEditorListener?.onEditTextChangeListener(
                            rootView,
                            textInput,
                            currentTextColor,
                            false
                        )
                    }

                    override fun onLongClick() {
                        // no op
                    }
                })
                rootView.setOnTouchListener(multiTouchListenerInstance)
            }
        }
        addViewToParent(rootView, viewType, sourceUri)
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
                if (rootView.tvPhotoEditorText != null) {
                    rootView.tvPhotoEditorText.gravity = Gravity.CENTER
                    if (mDefaultTextTypeface != null) {
                        rootView.tvPhotoEditorText.typeface = mDefaultTextTypeface
                    }
                }
            }
            ViewType.IMAGE -> rootView = layoutInflater.inflate(R.layout.view_photo_editor_image, null)
            ViewType.EMOJI -> {
                rootView = layoutInflater.inflate(R.layout.view_photo_editor_emoji, null)
                val txtTextEmoji = rootView.tvPhotoEditorEmoji
                if (txtTextEmoji != null) {
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(
                        txtTextEmoji, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
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

    fun viewUndo(removedView: View, viewType: ViewType, addToRedoList: Boolean = true) {
        if (addedViews.size > 0) {
            if (addedViews.containsView(removedView)) {
                parentView.removeView(removedView)
                val removedViewWithData = addedViews.removeView(removedView)
                if (removedViewWithData != null && addToRedoList) {
                    redoViews.add(removedViewWithData)
                }
                mOnPhotoEditorListener?.onRemoveViewListener(addedViews.size)
                mOnPhotoEditorListener?.onRemoveViewListener(viewType, addedViews.size)
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
            val viewTag = removeView.view?.tag
            (viewTag as? ViewType)?.let {
                mOnPhotoEditorListener?.onRemoveViewListener(it, addedViews.size)
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
                return brushDrawingView.redo()
            } else {
                redoViews.removeAt(redoViews.size - 1)
                parentView.addView(redoView.view)
                addedViews.add(redoView)
            }
            val viewTag = redoView.view?.tag
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
        for (addedView in addedViews) {
            addedView.view?.let {
                parentView.removeView(it)
            }
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
//        for (i in 0 until parentView.childCount) {
//            val childAt = parentView.getChildAt(i)
//            val frmBorder = childAt.findViewById<FrameLayout>(R.id.frmBorder)
//            frmBorder?.setBackgroundResource(0)
//            val imgClose = childAt.findViewById<ImageView>(R.id.imgPhotoEditorClose)
//            if (imgClose != null) {
//                imgClose.visibility = View.GONE
//            }
//        }
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
     * A callback to save the edited media asynchronously, with progress
     */
    interface OnSaveWithCancelAndProgressListener : OnSaveWithCancelListener {
        /**
         * Call to show progress on lengthy operations
         */
        fun onProgress(progress: Double)
    }

    fun saveImageFromPhotoEditorViewAsLoopFrameFile(sequenceId: Int, photoEditorView: PhotoEditorView): File {
        val localFile = FileUtils.getLoopFrameFile(context, false, sequenceId.toString())
        localFile.createNewFile()
        val saveSettings = SaveSettings.Builder()
            .setClearViewsEnabled(true)
            .setTransparencyEnabled(false)
            .build()
        FileUtils.saveViewToFile(localFile.absolutePath, saveSettings, photoEditorView)
        return localFile
    }

    fun saveVideoAsLoopFrameFile(
        sequenceId: Int,
        videoInputPath: Uri,
        muteAudio: Boolean,
        canvasWidth: Int,
        canvasHeight: Int,
        customAddedViews: AddedViewList,
        onSaveListener: OnSaveWithCancelAndProgressListener
    ): File {
        val localFile = FileUtils.getLoopFrameFile(context, true, sequenceId.toString())
        localFile.createNewFile()
        saveVideoAsFile(
            videoInputPath = videoInputPath,
            videoOutputPath = localFile.absolutePath,
            muteAudio = muteAudio,
            originalCanvasWidth = canvasWidth,
            originalCanvasHeight = canvasHeight,
            customAddedViews = customAddedViews,
            onSaveListener = onSaveListener
        )
        return localFile
    }

    fun saveVideoAsFile(
        videoInputPath: Uri,
        videoOutputPath: String,
        muteAudio: Boolean,
        originalCanvasWidth: Int,
        originalCanvasHeight: Int,
        customAddedViews: AddedViewList,
        onSaveListener: OnSaveWithCancelAndProgressListener
    ) {
        Log.d(TAG, "Video Path: $videoInputPath")

        // get the images currently on top of the screen, and add them as Filters to the mp4composer
        val filterCollection = ArrayList<GlFilter>()
        for (v in customAddedViews) {
            val viewPositionInfo = ViewPositionInfo(
                originalCanvasWidth,
                originalCanvasHeight,
                requireNotNull(v.view).width,
                requireNotNull(v.view).height,
                requireNotNull(v.view).matrix
            )
            when (v.viewType) {
                ViewType.STICKER_ANIMATED -> {
                    v.uri?.path?.let { path ->
                        val file = File(path)
                        val fileInputStream = FileInputStream(file)
                        filterCollection.add(GlGifWatermarkFilter(context, fileInputStream, viewPositionInfo))
                    }
                }
                else -> {
                    clearHelperBox()
                    filterCollection.add(GlWatermarkFilter(BitmapUtil.createBitmapFromView(v.view), viewPositionInfo))
                }
            }
        }

        Mp4Composer(videoInputPath, videoOutputPath)
            .with(context)
            .addedHeaders(authenticationHeadersInterface?.getAuthHeaders(videoInputPath.toString()))
//            .size(width, height)
            // IMPORTANT: as we aim at a WYSIWYG UX, we need to produce a video of size equal to that of the phone
            // screen, given the user may be seeing a letterbox landscape video and placing emoji / text around
            // the black parts of the screen.
            // .size(originalCanvasWidth, originalCanvasHeight)
            // normalize output video size if actual screen size does not match a "normal" video size
            .size(normalizeTargetVideoSize(originalCanvasWidth, originalCanvasHeight))
            .fillMode(FillMode.PRESERVE_ASPECT_FIT)
            .filter(if (customAddedViews.isNotEmpty()) GlFilterGroup(filterCollection) else null)
            .mute(muteAudio)
            .listener(object : Mp4Composer.Listener {
                override fun onProgress(progress: Double) {
                    Log.d(TAG, "onProgress = $progress")
                    onSaveListener.onProgress(progress)
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

    private fun normalizeTargetVideoSize(
        requestedWidth: Int,
        requestedHeight: Int
    ): Size {
        var adjustedSize = Size(requestedWidth, requestedHeight)
        // As per CDD, all android devices running API level 21 (our minSdk) with H.264 codec must support 720 x 480 px.
        // see https://source.android.com/compatibility/5.0/android-5.0-cdd#5_2_video_encoding
        // MUST
        // - 320 x 240 px
        // - 720 x 480 px
        // SHOULD (when hardware available)
        // - 1280 x 720 px
        // - 1920 x 1080 px

        // the other formats (2160p, 1440p, 1080p etc) are "popular" ones found on many devices
        // note we reverse the width/height given we support portrait mode only.
        when {
            // 2160p = 3840x2160
            (requestedWidth % 2160 == 0) -> {
                adjustedSize = Size(2160, 3840)
            }

            // 1440p = 2560x1440
            (requestedWidth % 1440 == 0) -> {
                adjustedSize = Size(1440, 2560)
            }

            // 1080p = 1920x1080
            (requestedWidth % 1080 == 0) -> {
                adjustedSize = Size(1080, 1920)
            }

            // 720p = 1280x720
            (requestedWidth % 720 == 0) -> {
                adjustedSize = Size(720, 1280)
            }

            (requestedWidth % 480 == 0) -> {
                adjustedSize = Size(480, 720)
            }

            (requestedWidth % 240 == 0) -> {
                adjustedSize = Size(240, 320)
            }
        }
        return adjustedSize
    }

    /**
     * Save the edited VIDEO on given path
     *
     * @param videoInputPath path on which video to be saved
     * @param saveSettings builder for multiple save options [SaveSettings]
     * @param onSaveListener callback for saving video
     * @see OnSaveListener
     */
    fun saveVideoAsFile(
        videoInputPath: Uri,
        videoOutputPath: String,
        muteAudio: Boolean,
        onSaveListener: OnSaveWithCancelAndProgressListener
    ) {
        saveVideoAsFile(
            videoInputPath,
            videoOutputPath,
            muteAudio,
            parentView.width,
            parentView.height,
            addedViews,
            onSaveListener
        )
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
        val widthParent = parentView.width
        val heightParent = parentView.height

        // get the images currently on top of the screen, and add them as Filters to the mp4composer
        val filterCollection = ArrayList<GlFilter>()
        for (v in addedViews) {
            val viewPositionInfo = ViewPositionInfo(
                widthParent,
                heightParent,
                requireNotNull(v.view).width,
                requireNotNull(v.view).height,
                requireNotNull(v.view).matrix
            )
            when (v.viewType) {
                ViewType.STICKER_ANIMATED -> {
                    v.uri?.path?.let { path ->
                        val file = File(path)
                        val fileInputStream = FileInputStream(file)
                        filterCollection.add(GlGifWatermarkFilter(context, fileInputStream, viewPositionInfo))
                    }
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
        addedViews.add(AddedView.buildAddedViewFromView(brushDrawingView, ViewType.BRUSH_DRAWING))
        mOnPhotoEditorListener?.onAddViewListener(ViewType.BRUSH_DRAWING, addedViews.size)
    }

    override fun onViewRemoved(brushDrawingView: BrushDrawingView) {
        if (addedViews.size > 0) {
            val removeView = addedViews.removeAt(addedViews.size - 1)
            removeView.view?.let {
                if (it !is BrushDrawingView) {
                    parentView.removeView(it)
                }
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

    fun anyViewsAdded(): Boolean {
        return (addedViews.size > 0)
    }

    fun getViewsAdded(): AddedViewList {
        return addedViews
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
     * @param parentView [PhotoEditorView]
     */
        (val context: Context, val parentView: PhotoEditorView) {
        val imageView: ImageView = parentView.source
        var deleteView: View? = null
        var workAreaRect: Rect? = null
        val brushDrawingView: BrushDrawingView = parentView.brush
        var textTypeface: Typeface? = null
        var emojiTypeface: Typeface? = null
        // By Default pinch zoom on text is enabled
        var isTextPinchZoomable = true
        var authenticationHeadersInterface: AuthenticationHeadersInterface? = null

        fun setDeleteView(deleteView: View): Builder {
            this.deleteView = deleteView
            return this
        }

        fun setWorkAreaRect(workAreaBounds: Rect): Builder {
            this.workAreaRect = workAreaBounds
            return this
        }

        fun setAuthenticatitonHeaderInterface(authHeaderInterface: AuthenticationHeadersInterface): Builder {
            this.authenticationHeadersInterface = authHeaderInterface
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
        private const val TAG = "PhotoEditor"

        private fun convertEmoji(emoji: String): String {
            return try {
                val convertEmojiToInt = Integer.parseInt(emoji.substring(2), 16)
                String(Character.toChars(convertEmojiToInt))
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Could not convert emoji to int: $emoji")
                ""
            }
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
