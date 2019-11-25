package com.automattic.photoeditor.gesture

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import com.automattic.photoeditor.OnPhotoEditorListener
import com.automattic.photoeditor.views.ViewType

class TextViewSizeAwareTouchListener(
    val minWidth: Int,
    val minHeight: Int,
    private val deleteView: View?,
    private val onDeleteViewListener: OnDeleteViewListener?,
    private val onPhotoEditorListener: OnPhotoEditorListener?
) : View.OnTouchListener {
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
    }

    interface OnDeleteViewListener {
        fun onRemoveViewListener(removedView: View)
        fun onRemoveViewReadyListener(removedView: View, ready: Boolean)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        rotationDetector.onTouchEvent(view, event)
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
                        val parentWidth = (view.parent as View).width
                        val parentHeight = (view.parent as View).height
                        val params = view.layoutParams

                        if (newWidth + view.x > parentWidth) {
                            newWidth = parentWidth - view.x.toInt()
                        }
                        if (newHeight + view.y > parentHeight) {
                            newHeight = parentHeight - view.y.toInt()
                        }

                        params.width = newWidth
                        params.height = newHeight

                        view.layoutParams = params
                        // note: requestLayout() is needed to get AutoResizeTextView to recalculate its fontSize after a
                        // change in view's width/height is made
                        view.requestLayout()
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
