package com.automattic.photoeditor

import android.graphics.Rect
import android.view.View
import com.automattic.photoeditor.text.TextStyler
import com.automattic.photoeditor.views.ViewType

/**
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 18/01/2017
 *
 *
 * This are the callbacks when any changes happens while editing the photo to make and custimization
 * on client side
 *
 */
interface OnPhotoEditorListener {
    /**
     * When user long press the existing text this event will trigger implying that user want to
     * edit the current [android.widget.TextView]
     *
     * @param rootView view on which the long press occurs
     * @param text current text set on the view
     * @param textStyler the [TextStyler] containing style rules for the view
     * @param isJustAdded true if this view has just been added to the parentView
     */
    fun onEditTextChangeListener(
        rootView: View,
        text: String,
        textStyler: TextStyler,
        isJustAdded: Boolean = false
    )

    /**
     * This is a callback when user adds any view on the [PhotoEditorView] it can be
     * brush,text or sticker i.e bitmap on parent view
     *
     * @param viewType enum which define type of view is added
     * @param numberOfAddedViews number of views currently added
     * @see ViewType
     */
    fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int)

    /**
     * This is a callback when user remove any view on the [PhotoEditorView] it happens when usually
     * undo and redo happens or text is removed
     *
     * @param numberOfAddedViews number of views currently added
     */
    @Deprecated("Use {@link OnPhotoEditorListener#onRemoveViewListener(ViewType, int)} instead")
    fun onRemoveViewListener(numberOfAddedViews: Int)

    /**
     * This is a callback when user remove any view on the [PhotoEditorView] it happens when usually
     * undo and redo happens or text is removed
     *
     * @param viewType enum which define type of view is added
     * @param numberOfAddedViews number of views currently added
     */
    fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int)

    /**
     * A callback when user start dragging a view which can be
     * any of [ViewType]
     *
     * @param viewType enum which define type of view is added
     */
    fun onStartViewChangeListener(viewType: ViewType)

    /**
     * A callback when user stop/up touching a view which can be
     * any of [ViewType]
     *
     * @param viewType enum which define type of view is added
     */
    fun onStopViewChangeListener(viewType: ViewType)

    fun onRemoveViewReadyListener(removedView: View, ready: Boolean)

    fun getWorkingAreaRect(): Rect?
}
