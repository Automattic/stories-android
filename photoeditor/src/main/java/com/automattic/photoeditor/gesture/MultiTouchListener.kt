package com.automattic.photoeditor.gesture

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.RelativeLayout
import com.automattic.photoeditor.OnPhotoEditorListener
import com.automattic.photoeditor.gesture.ScaleGestureDetector.SimpleOnScaleGestureListener
import com.automattic.photoeditor.util.BitmapUtil
import com.automattic.photoeditor.views.ViewType

/**
 * Created on 18/01/2017.
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 *
 *
 */
internal class MultiTouchListener(
    private val mainView: View?,
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
    private var deleteViewBitmap: Bitmap? = null

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(viewTouched: View, event: MotionEvent): Boolean {
        val view = mainView ?: viewTouched

        mScaleGestureDetector.onTouchEvent(view, event)
        mGestureListener.onTouchEvent(event)

        if (!isTranslateEnabled) {
            return true
        }

        val action = event.action

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
                        // if workingAreaRect is set, verify movement is within the area
                        mOnPhotoEditorListener?.getWorkingAreaRect()?.let {
                            if (isViewCenterInWorkingAreaBounds(view, currX - mPrevX, currY - mPrevY)) {
                                adjustTranslation(
                                    view,
                                    currX - mPrevX,
                                    currY - mPrevY
                                )
                            }
                        } ?: adjustTranslation(view,
                            currX - mPrevX,
                            currY - mPrevY
                        )
                    }

                    onMultiTouchListener?.let { touchListener ->
                        deleteView?.let { delView ->
                            // initialize bitmap for deleteView once
                            if (deleteViewBitmap == null && deleteView.isLaidOut) {
                                deleteViewBitmap = BitmapUtil.createBitmapFromView(deleteView)
                            }

                            deleteViewBitmap?.let {
                                val readyForDelete = isViewOverlappingDeleteView(delView, view)
                                // fade the view a bit to indicate it's going bye bye
                                setAlphaOnView(view, readyForDelete)
                                touchListener.onRemoveViewReadyListener(view, readyForDelete)
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> mActivePointerId =
                INVALID_POINTER_ID
            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
                deleteView?.let { delView ->
                    if (isViewOverlappingDeleteView(delView, view)) {
                        onMultiTouchListener?.onRemoveViewListener(view)
                    }
                    delView.visibility = View.GONE
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

    fun isViewOverlappingDeleteView(deleteView: View, viewB: View): Boolean {
        // using the View's matrix so the bitmap also has its content rotated and scaled.
        val bmpForDraggedView = BitmapUtil.createRotatedBitmapFromViewWithMatrix(viewB)

        val globalVisibleRectB = Rect()
        viewB.getGlobalVisibleRect(globalVisibleRectB)

        val globalVisibleRectDelete = Rect()
        deleteView.getGlobalVisibleRect(globalVisibleRectDelete)

        return isPixelOverlapping(
                requireNotNull(deleteViewBitmap), globalVisibleRectDelete.left, globalVisibleRectDelete.top,
                bmpForDraggedView, globalVisibleRectB.left, globalVisibleRectB.top
        )
    }

    // when we find both pixels on the same coordinate for each bitmap being not transparent, that means
    // there is an overlap between both images
    fun isPixelOverlapping(
        bitmap1: Bitmap,
        x1: Int,
        y1: Int,
        bitmap2: Bitmap,
        x2: Int,
        y2: Int
    ): Boolean {
        val bounds1 = Rect(
                x1,
                y1,
                x1 + bitmap1.width,
                y1 + bitmap1.height
        )
        val bounds2 = Rect(
                x2,
                y2,
                x2 + bitmap2.width,
                y2 + bitmap2.height
        )
        if (Rect.intersects(bounds1, bounds2)) {
            val collisionRect = getCollisionRect(bounds1, bounds2)
            for (i in collisionRect.left until collisionRect.right) {
                for (j in collisionRect.top until collisionRect.bottom) {
                    val bitmap1Pixel = bitmap1.getPixel(i - x1, j - y1)
                    val bitmap2Pixel = bitmap2.getPixel(i - x2, j - y2)
                    if (isNonTransparent(bitmap1Pixel) && isNonTransparent(bitmap2Pixel)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getCollisionRect(
        rect1: Rect,
        rect2: Rect
    ): Rect {
        return Rect(
                Math.max(rect1.left, rect2.left),
                Math.max(rect1.top, rect2.top),
                Math.min(rect1.right, rect2.right),
                Math.min(rect1.bottom, rect2.bottom)
        )
    }

    private fun isNonTransparent(pixel: Int): Boolean {
        return pixel != Color.TRANSPARENT
    }

    private fun isViewCenterInWorkingAreaBounds(view: View, deltaX: Float, deltaY: Float): Boolean {
        val deltaVector = getWouldBeTranslation(view, deltaX, deltaY)
        val wouldBeY = deltaVector[1] + view.y

        mOnPhotoEditorListener?.getWorkingAreaRect()?.let {
            val distanceToCenter = view.height / 2
            return ((wouldBeY - distanceToCenter) > it.top) && ((wouldBeY + distanceToCenter) < it.bottom)
        } ?: return true
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
            val deltaVector = getWouldBeTranslation(view, deltaX, deltaY)
            view.translationX = view.translationX + deltaVector[0]
            view.translationY = view.translationY + deltaVector[1]
        }

        private fun getWouldBeTranslation(view: View, deltaX: Float, deltaY: Float): FloatArray {
            val deltaVector = floatArrayOf(deltaX, deltaY)
            view.matrix.mapVectors(deltaVector)
            return deltaVector
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
