package com.automattic.photoeditor.gesture

import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.RelativeLayout
import com.automattic.photoeditor.OnPhotoEditorListener
import com.automattic.photoeditor.gesture.ScaleGestureDetector.SimpleOnScaleGestureListener
import com.automattic.photoeditor.views.ViewType

/**
 * Created on 18/01/2017.
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 *
 *
 */
internal class MultiTouchListener(
    private val deleteView: View?,
    private val parentView: RelativeLayout,
    private val photoEditImageView: ImageView,
    private val mIsTextPinchZoomable: Boolean,
    private val mOnPhotoEditorListener: OnPhotoEditorListener?,
    private var onMultiTouchListener: OnMultiTouchListener? = null
) : OnTouchListener {
    private val mGestureListener: GestureDetector
    private val isRotateEnabled = true
    private val isTranslateEnabled = true
    private val isScaleEnabled = true
    private val minimumScale = 0.5f
    private val maximumScale = 4.2f // 10.0f
    private var mActivePointerId = INVALID_POINTER_ID
    private var mPrevX: Float = 0.toFloat()
    private var mPrevY: Float = 0.toFloat()
    private var mPrevRawX: Float = 0.toFloat()
    private var mPrevRawY: Float = 0.toFloat()
    private val mScaleGestureDetector: ScaleGestureDetector

    private val location = IntArray(2)
    private var outRect: Rect? = null

    // private var onMultiTouchListener: OnMultiTouchListener? = null
    private var mOnGestureControl: OnGestureControl? = null

    init {
        mScaleGestureDetector = ScaleGestureDetector(ScaleGestureListener())
        mGestureListener = GestureDetector(GestureListener())
        if (deleteView != null) {
            outRect = Rect(
                deleteView.left, deleteView.top,
                deleteView.right, deleteView.bottom
            )
        } else {
            outRect = Rect(0, 0, 0, 0)
        }
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        mGestureListener.onTouchEvent(event)
        // filter out touch events that fall around the edges of the view
        if (!touchIsWellWithinView(view, event)) {
            return true
        }
        mScaleGestureDetector.onTouchEvent(view, event)

        if (!isTranslateEnabled) {
            return true
        }

        val action = event.action

        val x = event.rawX.toInt()
        val y = event.rawY.toInt()

        when (action and event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mPrevX = event.x
                mPrevY = event.y
                mPrevRawX = event.rawX
                mPrevRawY = event.rawY
                mActivePointerId = event.getPointerId(0)
                if (deleteView != null) {
                    deleteView.visibility = View.VISIBLE
                }
                view.bringToFront()
                firePhotoEditorSDKListener(view, true)
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndexMove = event.findPointerIndex(mActivePointerId)
                if (pointerIndexMove != -1) {
                    val currX = event.getX(pointerIndexMove)
                    val currY = event.getY(pointerIndexMove)
                    if (!mScaleGestureDetector.isInProgress) {
                        adjustTranslation(
                            view,
                            currX - mPrevX,
                            currY - mPrevY
                        )
                    }
                    if (onMultiTouchListener != null && deleteView != null) {
                        val readyForDelete = isViewInBounds(deleteView, x, y)
                        // fade the view a bit to indicate it's going bye bye
                        setAlphaOnView(view, readyForDelete)
                        onMultiTouchListener?.onRemoveViewReadyListener(view, readyForDelete)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> mActivePointerId =
                INVALID_POINTER_ID
            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
                if (deleteView != null && isViewInBounds(deleteView, x, y)) {
                    onMultiTouchListener?.onRemoveViewListener(view)
                }
//                else if (!isViewInBounds(photoEditImageView, x, y)) {
//                    view.animate().translationY(0f).translationY(0f)
//                }
                if (deleteView != null) {
                    deleteView.visibility = View.GONE
                }
                firePhotoEditorSDKListener(view, false)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndexPointerUp =
                    action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndexPointerUp)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndexPointerUp == 0) 1 else 0
                    mPrevX = event.getX(newPointerIndex)
                    mPrevY = event.getY(newPointerIndex)
                    mActivePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }

    private fun setAlphaOnView(view: View, makeTransparent: Boolean) {
        if (makeTransparent) {
            view.alpha = 0.5f
        } else {
            view.alpha = 1f
        }
    }

    private fun firePhotoEditorSDKListener(view: View, isStart: Boolean) {
        val viewTag = view.tag
        if (mOnPhotoEditorListener != null && viewTag != null && viewTag is ViewType) {
            if (isStart)
                mOnPhotoEditorListener.onStartViewChangeListener(view.tag as ViewType)
            else
                mOnPhotoEditorListener.onStopViewChangeListener(view.tag as ViewType)
        }
    }

    private fun isViewInBounds(view: View, x: Int, y: Int): Boolean {
        view.getDrawingRect(outRect)
        view.getLocationOnScreen(location)
        outRect?.offset(location[0], location[1])
        return outRect?.contains(x, y) ?: false
    }

    private fun touchIsWellWithinView(view: View, event: MotionEvent): Boolean {
        view.getLocationOnScreen(location)
        Log.d("PORTKEY", "location: x=" + location[0] + " y=" + location[1])
        var viewRect = Rect()
        view.getDrawingRect(viewRect)
        viewRect.offset(location[0], location[1])
        // now we have a viewRect that contains all possible view points according to the device screen coordinates
        // let's now offset once more to bring the size of the view down by a percentage
        val currentDx = view.right - view.left
        val currentDy = view.bottom - view.top
        val percentageOnX = (currentDx * TOUCH_IGNORE_AREA_PERCENTAGE).toInt()
        val percentageOnY = (currentDy * TOUCH_IGNORE_AREA_PERCENTAGE).toInt()
        viewRect.offset(percentageOnX, percentageOnY)
        return viewRect.contains(event.rawX.toInt(), event.rawY.toInt())
    }

    fun setOnMultiTouchListener(onMultiTouchListener: OnMultiTouchListener) {
        this.onMultiTouchListener = onMultiTouchListener
    }

    private inner class ScaleGestureListener : SimpleOnScaleGestureListener() {
        private var mPivotX: Float = 0.toFloat()
        private var mPivotY: Float = 0.toFloat()
        private val mPrevSpanVector = Vector2D()

        override fun onScaleBegin(view: View, detector: ScaleGestureDetector): Boolean {
            mPivotX = detector.focusX
            mPivotY = detector.focusY
            mPrevSpanVector.set(detector.currentSpanVector)
            return mIsTextPinchZoomable
        }

        override fun onScale(view: View, detector: ScaleGestureDetector): Boolean {
            val info = TransformInfo()
            info.deltaScale = if (isScaleEnabled) detector.scaleFactor else 1.0f
            info.deltaAngle =
                if (isRotateEnabled) Vector2D.getAngle(
                    mPrevSpanVector,
                    detector.currentSpanVector
                ) else 0.0f
            info.deltaX = if (isTranslateEnabled) detector.focusX - mPivotX else 0.0f
            info.deltaY = if (isTranslateEnabled) detector.focusY - mPivotY else 0.0f
            info.pivotX = mPivotX
            info.pivotY = mPivotY
            info.minimumScale = minimumScale
            info.maximumScale = maximumScale
            move(view, info)
            return !mIsTextPinchZoomable
        }
    }

    private inner class TransformInfo {
        internal var deltaX: Float = 0.toFloat()
        internal var deltaY: Float = 0.toFloat()
        internal var deltaScale: Float = 0.toFloat()
        internal var deltaAngle: Float = 0.toFloat()
        internal var pivotX: Float = 0.toFloat()
        internal var pivotY: Float = 0.toFloat()
        internal var minimumScale: Float = 0.toFloat()
        internal var maximumScale: Float = 0.toFloat()
    }

    interface OnMultiTouchListener {
        fun onEditTextClickListener(text: String, colorCode: Int)

        fun onRemoveViewListener(removedView: View)

        fun onRemoveViewReadyListener(removedView: View, ready: Boolean)
    }

    internal interface OnGestureControl {
        fun onClick()

        fun onLongClick()
    }

    fun setOnGestureControl(onGestureControl: OnGestureControl) {
        mOnGestureControl = onGestureControl
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            mOnGestureControl?.onClick()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            mOnGestureControl?.onLongClick()
        }
    }

    companion object {
        private val INVALID_POINTER_ID = -1
        private val TOUCH_IGNORE_AREA_PERCENTAGE = 0.2f

        private fun adjustAngle(origDegrees: Float): Float {
            var degrees = origDegrees
            if (degrees > 180.0f) {
                degrees -= 360.0f
            } else if (degrees < -180.0f) {
                degrees += 360.0f
            }
            return degrees
        }

        private fun move(view: View, info: TransformInfo) {
            computeRenderOffset(
                view,
                info.pivotX,
                info.pivotY
            )
            adjustTranslation(
                view,
                info.deltaX,
                info.deltaY
            )

            var scale = view.scaleX * info.deltaScale
            scale = Math.max(info.minimumScale, Math.min(info.maximumScale, scale))
            view.scaleX = scale
            view.scaleY = scale

            val rotation =
                adjustAngle(view.rotation + info.deltaAngle)
            view.rotation = rotation
        }

        private fun adjustTranslation(view: View, deltaX: Float, deltaY: Float) {
            val deltaVector = floatArrayOf(deltaX, deltaY)
            view.matrix.mapVectors(deltaVector)
            view.translationX = view.translationX + deltaVector[0]
            view.translationY = view.translationY + deltaVector[1]
        }

        private fun computeRenderOffset(view: View, pivotX: Float, pivotY: Float) {
            if (view.pivotX == pivotX && view.pivotY == pivotY) {
                return
            }

            val prevPoint = floatArrayOf(0.0f, 0.0f)
            view.matrix.mapPoints(prevPoint)

            view.pivotX = pivotX
            view.pivotY = pivotY

            val currPoint = floatArrayOf(0.0f, 0.0f)
            view.matrix.mapPoints(currPoint)

            val offsetX = currPoint[0] - prevPoint[0]
            val offsetY = currPoint[1] - prevPoint[1]

            view.translationX = view.translationX - offsetX
            view.translationY = view.translationY - offsetY
        }
    }
}
