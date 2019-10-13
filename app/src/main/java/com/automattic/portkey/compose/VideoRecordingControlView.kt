package com.automattic.portkey.compose

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.animation.LinearInterpolator

import androidx.appcompat.widget.AppCompatButton
import com.automattic.portkey.R

class VideoRecordingControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {
    private var bitmap: Bitmap? = null
    private var viewCanvas: Canvas? = null

    private lateinit var circleOuterBounds: RectF
    private lateinit var circleInnerBounds: RectF

    private val circlePaint: Paint
    private val eraserPaint: Paint

    private var circleSweepAngle: Float = 0.toFloat()

    private var valueAnimator: ValueAnimator? = null

    private var strokeFillColor = Color.WHITE
    private var strokeProgressColor = Color.MAGENTA
    private var strokeWidth = 1

    init {
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.VideoRecordingControlView)
            strokeWidth = ta.getDimensionPixelSize(R.styleable.VideoRecordingControlView_strokeThickness, strokeWidth)
            strokeFillColor = ta.getColor(R.styleable.VideoRecordingControlView_strokeFillColor, strokeFillColor)
            strokeProgressColor =
                ta.getColor(R.styleable.VideoRecordingControlView_strokeProgressColor, strokeProgressColor)
            ta.recycle()
        }

        circlePaint = Paint()
        circlePaint.isAntiAlias = true
        circlePaint.color = strokeProgressColor

        eraserPaint = Paint()
        eraserPaint.isAntiAlias = true
        eraserPaint.color = strokeFillColor
        eraserPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw || h != oldh) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)?.apply {
                eraseColor(Color.TRANSPARENT)
                viewCanvas = Canvas(this)
            }
        }
        super.onSizeChanged(w, h, oldw, oldh)
        updateBounds()
    }

    override fun onDraw(canvas: Canvas) {
        valueAnimator.takeIf { it == null || !it.isRunning }?.let {
            super.onDraw(canvas)
            return
        }

        viewCanvas?.apply {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            if (circleSweepAngle > 0f) {
                drawArc(
                    circleOuterBounds,
                    START_POSITION_ANGLE.toFloat(),
                    circleSweepAngle,
                    true,
                    circlePaint
                )
                drawOval(circleInnerBounds, eraserPaint)
            }

            bitmap?.let {
                canvas.drawBitmap(it, 0f, 0f, null)
            }
        }
    }

    fun startProgressingAnimation(waitTimeMs: Long) {
        stopProgressingAnimation()

        valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator?.apply {
            duration = waitTimeMs
            interpolator = LinearInterpolator()
            addUpdateListener { animation -> drawProgress(animation.animatedValue as Float) }
            start()
        }
    }

    fun stopProgressingAnimation() {
        valueAnimator?.takeIf { it.isRunning }?.let {
            it.cancel()
            valueAnimator = null

            drawProgress(0f)
        }
    }

    private fun drawProgress(progress: Float) {
        circleSweepAngle = 360 * progress

        invalidate()
    }

    private fun updateBounds() {
        val thickness = strokeWidth.toFloat()

        circleOuterBounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        circleInnerBounds = RectF(
            circleOuterBounds.left + thickness,
            circleOuterBounds.top + thickness,
            circleOuterBounds.right - thickness,
            circleOuterBounds.bottom - thickness
        )

        invalidate()
    }

    companion object {
        private val START_POSITION_ANGLE = 270
    }
}
