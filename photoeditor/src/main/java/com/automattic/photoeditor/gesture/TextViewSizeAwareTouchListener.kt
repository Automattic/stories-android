package com.automattic.photoeditor.gesture

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.automattic.photoeditor.OnPhotoEditorListener
import com.automattic.photoeditor.views.AutoResizeTextView.OnMaxMinFontSizeReached
import com.automattic.photoeditor.views.ViewType

class TextViewSizeAwareTouchListener(
    val minWidth: Int,
    val minHeight: Int,
    private val deleteView: View?,
    private val onDeleteViewListener: OnDeleteViewListener?,
    private val onPhotoEditorListener: OnPhotoEditorListener?
) : View.OnTouchListener, OnMaxMinFontSizeReached {
    private var originX = 0f
    private var originY = 0f
    private var secondOriginX = 0f
    private var secondOriginY = 0f
    private var lastDiffX = 0f
    private var lastDiffY = 0f

    private var originUp = false
    private var secondOriginUp = false
    private var rotationDetector: RotationGestureDetector

    // will hold deleteView Rect if passed, and location of current dragged view to see if it's a match
    private var outRect: Rect? = null
    private val location = IntArray(2)

    private var onGestureControl: OnGestureControl? = null
    private val gestureListener: GestureDetector

    private var maxFontSizeReached: Boolean = false
    private var minFontSizeReached: Boolean = false
    private var maxHeightForFontMeasured: Int = 0
    private var maxWidthForFontMeasured: Int = 0

    init {
        rotationDetector = RotationGestureDetector()
        if (deleteView != null) {
            outRect = Rect(
                deleteView.left, deleteView.top,
                deleteView.right, deleteView.bottom
            )
        } else {
            outRect = Rect(0, 0, 0, 0)
        }
        gestureListener = GestureDetector(GestureListener())
    }

    interface OnDeleteViewListener {
        fun onRemoveViewListener(removedView: View)
        fun onRemoveViewReadyListener(removedView: View, ready: Boolean)
    }

    interface OnGestureControl {
        fun onClick()
        fun onLongClick()
    }

    fun setOnGestureControl(onGestureControl: OnGestureControl) {
        this.onGestureControl = onGestureControl
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onGestureControl?.onClick()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            onGestureControl?.onLongClick()
        }
    }

    override fun onMaxFontSizeReached() {
        maxFontSizeReached = true
        minFontSizeReached = false
    }

    override fun onMinFontSizeReached() {
        minFontSizeReached = true
        maxFontSizeReached = false
    }

    override fun onFontSizeChangedWithinMinMaxRange() {
        minFontSizeReached = false
        maxFontSizeReached = false
    }

    private fun isZoomOutMovement(newWidth: Int, newHeight: Int, view: View): Boolean {
        val params = view.layoutParams
        return newWidth <= params.width || newHeight <= params.height
    }

    private fun setMaximumWidthAndHeightAfterFontMaxSizeReached(newWidth: Int, newHeight: Int) {
        // the maximum width / height will only be set once (as it'll remain that way forever for this view)
        if (maxFontSizeReached && maxHeightForFontMeasured == 0 && maxWidthForFontMeasured == 0) {
            maxHeightForFontMeasured = newHeight
            maxWidthForFontMeasured = newWidth
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        rotationDetector.onTouchEvent(view, event)
        gestureListener.onTouchEvent(event)

        event.offsetLocation(event.rawX - event.x, event.rawY - event.y)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                originUp = false
                secondOriginUp = false

                originX = view.x - event.getX(0)
                originY = view.y - event.getY(0)
                if (deleteView != null) {
                    deleteView.visibility = View.VISIBLE
                }
                view.bringToFront()
                firePhotoEditorSDKListener(view, true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                secondOriginX = view.x - event.getX(1)
                secondOriginY = view.y - event.getY(1)

                lastDiffX = Math.abs(secondOriginX - originX)
                lastDiffY = Math.abs(secondOriginY - originY)
            }
            MotionEvent.ACTION_MOVE -> { // a pointer was moved
                deleteView?.let {
                    val readyForDelete = isViewInBounds(it, event.rawX.toInt(), event.rawY.toInt())
                    // fade the view a bit to indicate it's going bye bye
                    setAlphaOnView(view, readyForDelete)
                    onDeleteViewListener?.onRemoveViewReadyListener(view, readyForDelete)
                }

                if (event.pointerCount == 2) {
                    val diffX = Math.abs(event.getX(1) - event.getX(0))
                    val diffY = Math.abs(event.getY(1) - event.getY(0))
                    var newWidth = (diffX * view.measuredWidth.toFloat() / lastDiffX).toInt()
                    var newHeight = (diffY * view.measuredHeight.toFloat() / lastDiffY).toInt()

                    if (newWidth > minWidth && newHeight > minHeight) {
                        if (isZoomOutMovement(newWidth, newHeight, view) && !minFontSizeReached ||
                            !isZoomOutMovement(newWidth, newHeight, view) && !maxFontSizeReached) {
                            val parentWidth = (view.parent as View).width
                            val parentHeight = (view.parent as View).height
                            val params = view.layoutParams

                            // cap the size of the view to never be larger than the parent's view (container)
                            if (newWidth + view.x > parentWidth) {
                                newWidth = parentWidth - view.x.toInt()
                            }
                            if (newHeight + view.y > parentHeight) {
                                newHeight = parentHeight - view.y.toInt()
                            }

                            // also adjust view size to stay within the maximum view size allowed / found for the
                            // maximum fontSize set on the TextView
                            if (maxWidthForFontMeasured > 0) {
                                if (newWidth > maxWidthForFontMeasured) {
                                    newWidth = maxWidthForFontMeasured //  - view.x.toInt()
                                }
                            }
                            if (maxHeightForFontMeasured > 0) {
                                if (newHeight > maxHeightForFontMeasured) {
                                    newHeight = maxHeightForFontMeasured // - view.y.toInt()
                                }
                            }

                            // did we mess the calculations and ran below a minimum?
                            // did the user cross fingers while pinching, getting us negative diff values?
                            // let's cap it to the inferior limit
                            if (newWidth < minWidth) {
                                newWidth = minWidth
                            }
                            if (newHeight < minHeight) {
                                newHeight = minHeight
                            }

                            params.width = newWidth
                            params.height = newHeight

                            setMaximumWidthAndHeightAfterFontMaxSizeReached(newWidth, newHeight)

                            view.layoutParams = params
                            // note: requestLayout() is needed to get AutoResizeTextView to recalculate its fontSize after a
                            // change in view's width/height is made
                            view.requestLayout()
                        }
                        lastDiffX = diffX
                        lastDiffY = diffY
                    }
                } else if (!originUp && !secondOriginUp) {
                    var newX = event.getX(0) + originX
                    var newY = event.getY(0) + originY

                    view.x = newX
                    view.y = newY
                }
            }
            MotionEvent.ACTION_UP -> {
                originUp = true
                // if max fontSize reached, save current width / height for the view as the maximum
                setMaximumWidthAndHeightAfterFontMaxSizeReached(view.measuredWidth, view.measuredHeight)

                deleteView?.let {
                    if (isViewInBounds(it, event.rawX.toInt(), event.rawY.toInt())) {
                        onDeleteViewListener?.onRemoveViewListener(view)
                    }
                    it.visibility = View.GONE
                }
                firePhotoEditorSDKListener(view, false)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                secondOriginUp = true
                // if max fontSize reached, save current width / height for the view as the maximum
                setMaximumWidthAndHeightAfterFontMaxSizeReached(view.measuredWidth, view.measuredHeight)
            }
        }
        return true
    }

    private fun isViewInBounds(view: View, x: Int, y: Int): Boolean {
        view.getDrawingRect(outRect)
        view.getLocationOnScreen(location)
        outRect?.offset(location[0], location[1])
        return outRect?.contains(x, y) ?: false
    }

    private fun setAlphaOnView(view: View, makeTransparent: Boolean) {
        if (makeTransparent) {
            view.alpha = 0.5f
        } else {
            view.alpha = 1f
        }
    }

    private fun firePhotoEditorSDKListener(view: View, isStart: Boolean) {
        onPhotoEditorListener?.let {
            val viewTag = view.tag
            if (viewTag != null && viewTag is ViewType) {
                if (isStart)
                    it.onStartViewChangeListener(view.tag as ViewType)
                else
                    it.onStopViewChangeListener(view.tag as ViewType)
            }
        }
    }
}
