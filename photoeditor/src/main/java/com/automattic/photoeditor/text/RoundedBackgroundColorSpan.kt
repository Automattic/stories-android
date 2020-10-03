package com.automattic.photoeditor.text

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.SpannableStringBuilder
import android.text.style.LineBackgroundSpan
import android.view.View
import android.widget.TextView
import com.automattic.photoeditor.R
import kotlin.math.abs
import kotlin.math.sign

/**
 * A text background color span with padding, and rounded corners.
 * Corner rounding rules apply to the text as a whole, not to individual lines,
 * so we never have two rounded corners in contact.
 *
 * Based on https://stackoverflow.com/q/48096722.
 */
class RoundedBackgroundColorSpan(
    val backgroundColor: Int,
    alignment: Int?,
    resources: Resources,
    val padding: Float = resources.getDimensionPixelSize(R.dimen.rounded_background_color_span_padding).toFloat(),
    val radius: Float = resources.getDimensionPixelSize(R.dimen.rounded_background_color_span_radius).toFloat()
) : LineBackgroundSpan {
    private val align = alignment ?: ALIGN_CENTER
    private val rect = RectF()
    private val paint = Paint()
    private val paintStroke = Paint()
    private val path = Path()
    private var prevWidth = -1f
    private var prevLeft = -1f
    private var prevRight = -1f
    private var prevBottom = -1f
    private var prevTop = -1f

    init {
        paint.color = backgroundColor
        paintStroke.color = backgroundColor
    }

    override fun drawBackground(
        c: Canvas,
        p: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lnum: Int
    ) {
        val width = p.measureText(text, start, end) + 2f * padding
        val shiftLeft: Float
        val shiftRight: Float

        when (align) {
            ALIGN_START -> {
                shiftLeft = 0f - padding
                shiftRight = width + shiftLeft
            }

            ALIGN_END -> {
                shiftLeft = right - width + padding
                shiftRight = (right + padding)
            }
            else -> {
                shiftLeft = (right - width) / 2
                shiftRight = right - shiftLeft
            }
        }

        rect.set(shiftLeft, top.toFloat(), shiftRight, bottom.toFloat())

        c.drawRoundRect(rect, radius, radius, paint)

        if (lnum > 0) {
            path.reset()
            val difference = width - prevWidth
            val diff = -sign(difference) * (2f * radius).coerceAtMost(abs(difference / 2f)) / 2f
            path.moveTo(
                    prevLeft, prevBottom - radius
            )

            if (align != ALIGN_START) {
                path.cubicTo(
                        prevLeft, prevBottom - radius,
                        prevLeft, rect.top,
                        prevLeft + diff, rect.top
                )
            } else {
                path.lineTo(prevLeft, prevBottom + radius)
            }
            path.lineTo(
                    rect.left - diff, rect.top
            )
            path.cubicTo(
                    rect.left - diff, rect.top,
                    rect.left, rect.top,
                    rect.left, rect.top + radius
            )
            path.lineTo(
                    rect.left, rect.bottom - radius
            )
            path.cubicTo(
                    rect.left, rect.bottom - radius,
                    rect.left, rect.bottom,
                    rect.left + radius, rect.bottom
            )
            path.lineTo(
                    rect.right - radius, rect.bottom
            )
            path.cubicTo(
                    rect.right - radius, rect.bottom,
                    rect.right, rect.bottom,
                    rect.right, rect.bottom - radius
            )
            path.lineTo(
                    rect.right, rect.top + radius
            )

            if (align != ALIGN_END) {
                path.cubicTo(
                        rect.right, rect.top + radius,
                        rect.right, rect.top,
                        rect.right + diff, rect.top
                )
                path.lineTo(
                        prevRight - diff, rect.top
                )
                path.cubicTo(
                        prevRight - diff, rect.top,
                        prevRight, rect.top,
                        prevRight, prevBottom - radius
                )
            } else {
                path.lineTo(prevRight, prevBottom - radius)
            }
            path.cubicTo(
                    prevRight, prevBottom - radius,
                    prevRight, prevBottom,
                    prevRight - radius, prevBottom
            )

            path.lineTo(
                    prevLeft + radius, prevBottom
            )

            path.cubicTo(
                    prevLeft + radius, prevBottom,
                    prevLeft, prevBottom,
                    prevLeft, rect.top - radius
            )
            c.drawPath(path, paintStroke)
        }
        prevWidth = width
        prevLeft = rect.left
        prevRight = rect.right
        prevBottom = rect.bottom
        prevTop = rect.top
    }

    companion object {
        private const val ALIGN_CENTER = View.TEXT_ALIGNMENT_CENTER
        private const val ALIGN_START = View.TEXT_ALIGNMENT_TEXT_START
        private const val ALIGN_END = View.TEXT_ALIGNMENT_TEXT_END

        fun from(textView: TextView): RoundedBackgroundColorSpan? {
            val length = textView.text?.length ?: 0
            return SpannableStringBuilder(textView.text)
                    .getSpans(0, length, RoundedBackgroundColorSpan::class.java)?.firstOrNull()
        }
    }
}
