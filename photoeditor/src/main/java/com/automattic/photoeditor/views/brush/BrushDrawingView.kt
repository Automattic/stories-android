package com.automattic.photoeditor.views.brush

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IntRange

import java.util.Stack

/**
 *
 *
 * This is custom drawing view used to do painting on user touch events it it will paint on canvas
 * as per attributes provided to the paint
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 12/1/18
 */
class BrushDrawingView : View {
    internal var brushSize = 25f
        set(size) {
            field = size
            brushDrawingMode = true
        }
    internal var eraserSize = 50f
        private set
    private var mOpacity = 255

    private val mDrawnPaths = Stack<LinePath>()
    private val mRedoPaths = Stack<LinePath>()
    private var mDrawPaint: Paint

    private var mDrawCanvas: Canvas? = null
    private var mBrushDrawMode: Boolean = false

    private var mPath: Path
    private var mTouchX: Float = 0.toFloat()
    private var mTouchY: Float = 0.toFloat()

    private var mBrushViewChangeListener: BrushViewChangeListener? = null

    internal var brushDrawingMode: Boolean
        get() = mBrushDrawMode
        set(brushDrawMode) {
            this.mBrushDrawMode = brushDrawMode
            if (brushDrawMode) {
                this.visibility = View.VISIBLE
                refreshBrushDrawing()
            }
        }

    internal var brushColor: Int
        get() = mDrawPaint.color
        set(@ColorInt color) {
            mDrawPaint.color = color
            brushDrawingMode = true
        }

    init {
        // Caution: This line is to disable hardware acceleration to make eraser feature work properly
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        mDrawPaint = Paint()
        mPath = Path()
        mDrawPaint.isAntiAlias = true
        mDrawPaint.isDither = true
        mDrawPaint.color = Color.BLACK
        mDrawPaint.style = Paint.Style.STROKE
        mDrawPaint.strokeJoin = Paint.Join.ROUND
        mDrawPaint.strokeCap = Paint.Cap.ROUND
        mDrawPaint.strokeWidth = brushSize
        mDrawPaint.alpha = mOpacity
        // Resolve Brush color changes after saving image  #52
        // Resolve Brush bug using PorterDuff.Mode.SRC_OVER #80 and PR #83
        mDrawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        this.visibility = View.GONE
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    }

    private fun refreshBrushDrawing() {
        mBrushDrawMode = true
        mPath = Path()
        mDrawPaint.isAntiAlias = true
        mDrawPaint.isDither = true
        mDrawPaint.style = Paint.Style.STROKE
        mDrawPaint.strokeJoin = Paint.Join.ROUND
        mDrawPaint.strokeCap = Paint.Cap.ROUND
        mDrawPaint.strokeWidth = brushSize
        mDrawPaint.alpha = mOpacity
        // Resolve Brush color changes after saving image  #52
        // Resolve Brush bug using PorterDuff.Mode.SRC_OVER #80 and PR #83
        mDrawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    internal fun brushEraser() {
        mBrushDrawMode = true
        mDrawPaint.strokeWidth = eraserSize
        mDrawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    internal fun setOpacity(@IntRange(from = 0, to = 255) opacity: Int) {
        this.mOpacity = opacity
        brushDrawingMode = true
    }

    internal fun setBrushEraserSize(brushEraserSize: Float) {
        this.eraserSize = brushEraserSize
        brushDrawingMode = true
    }

    internal fun setBrushEraserColor(@ColorInt color: Int) {
        mDrawPaint.color = color
        brushDrawingMode = true
    }

    internal fun clearAll() {
        mDrawnPaths.clear()
        mRedoPaths.clear()
        if (mDrawCanvas != null) {
            mDrawCanvas!!.drawColor(0, PorterDuff.Mode.CLEAR)
        }
        invalidate()
    }

    internal fun setBrushViewChangeListener(brushViewChangeListener: BrushViewChangeListener) {
        mBrushViewChangeListener = brushViewChangeListener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mDrawCanvas = Canvas(canvasBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        for (linePath in mDrawnPaths) {
            canvas.drawPath(linePath.drawPath, linePath.drawPaint)
        }
        canvas.drawPath(mPath, mDrawPaint)
    }

    /**
     * Handle touch event to draw paint on canvas i.e brush drawing
     *
     * @param event points having touch info
     * @return true if handling touch events
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mBrushDrawMode) {
            val touchX = event.x
            val touchY = event.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> touchStart(touchX, touchY)
                MotionEvent.ACTION_MOVE -> touchMove(touchX, touchY)
                MotionEvent.ACTION_UP -> touchUp()
            }
            invalidate()
            return true
        } else {
            return false
        }
    }

    private inner class LinePath internal constructor(drawPath: Path, drawPaints: Paint) {
        internal val drawPaint: Paint
        internal val drawPath: Path

        init {
            drawPaint = Paint(drawPaints)
            this.drawPath = Path(drawPath)
        }
    }

    internal fun undo(): Boolean {
        if (!mDrawnPaths.empty()) {
            mRedoPaths.push(mDrawnPaths.pop())
            invalidate()
        }
        if (mBrushViewChangeListener != null) {
            mBrushViewChangeListener!!.onViewRemoved(this)
        }
        return !mDrawnPaths.empty()
    }

    internal fun redo(): Boolean {
        if (!mRedoPaths.empty()) {
            mDrawnPaths.push(mRedoPaths.pop())
            invalidate()
        }

        if (mBrushViewChangeListener != null) {
            mBrushViewChangeListener!!.onViewAdd(this)
        }
        return !mRedoPaths.empty()
    }

    private fun touchStart(x: Float, y: Float) {
        mRedoPaths.clear()
        mPath.reset()
        mPath.moveTo(x, y)
        mTouchX = x
        mTouchY = y
        if (mBrushViewChangeListener != null) {
            mBrushViewChangeListener!!.onStartDrawing()
        }
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = Math.abs(x - mTouchX)
        val dy = Math.abs(y - mTouchY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mTouchX, mTouchY, (x + mTouchX) / 2, (y + mTouchY) / 2)
            mTouchX = x
            mTouchY = y
        }
    }

    private fun touchUp() {
        mPath.lineTo(mTouchX, mTouchY)
        // Commit the path to our offscreen
        mDrawCanvas!!.drawPath(mPath, mDrawPaint)
        // kill this so we don't double draw
        mDrawnPaths.push(LinePath(mPath, mDrawPaint))
        mPath = Path()
        if (mBrushViewChangeListener != null) {
            mBrushViewChangeListener!!.onStopDrawing()
            mBrushViewChangeListener!!.onViewAdd(this)
        }
    }

    companion object {
        private val TOUCH_TOLERANCE = 4f
    }
}
