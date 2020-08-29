package com.automattic.photoeditor.text

import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Dimension.PX
import com.automattic.photoeditor.text.IdentifiableTypeface.TypefaceId
import com.automattic.photoeditor.views.added.AddedViewTextInfo

class TextStyler(
    val textAlignment: Int? = null,
    @TypefaceId val typefaceId: Int? = null,
    @Dimension(unit = PX) val fontSize: Float = 0F,
    val lineSpacingMultiplier: Float = 1F,
    val letterSpacing: Float = 0F,
    @ColorInt val textColor: Int? = null,
    @Dimension(unit = PX) val shadowRadius: Float = 0F,
    @Dimension(unit = PX) val shadowDx: Float = 0F,
    @Dimension(unit = PX) val shadowDy: Float = 0F,
    @ColorInt val shadowColor: Int? = null
) {
    fun styleText(textView: PhotoEditorTextView, fontResolver: FontResolver?) {
        textAlignment?.let { textView.textAlignment = it }

        typefaceId?.let {
            textView.identifiableTypeface = fontResolver?.resolve(it)
        }

        textView.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor ?: 0)

        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
        textView.setLineSpacing(0F, lineSpacingMultiplier)
        textView.letterSpacing = letterSpacing

        textColor?.let { textView.setTextColor(it) }
    }

    companion object {
        fun from(textView: PhotoEditorTextView): TextStyler {
            return from(textView, textView.identifiableTypeface?.id)
        }

        fun from(textView: TextView, @TypefaceId typefaceId: Int?): TextStyler {
            return TextStyler(
                    textAlignment = textView.textAlignment,
                    typefaceId = typefaceId,
                    fontSize = textView.textSize,
                    lineSpacingMultiplier = textView.lineSpacingMultiplier,
                    letterSpacing = textView.letterSpacing,
                    textColor = textView.currentTextColor,
                    shadowRadius = textView.shadowRadius,
                    shadowDx = textView.shadowDx,
                    shadowDy = textView.shadowDy,
                    shadowColor = textView.shadowColor
            )
        }

        fun from(addedViewTextInfo: AddedViewTextInfo): TextStyler {
            return TextStyler(
                    textAlignment = addedViewTextInfo.textAlignment,
                    typefaceId = addedViewTextInfo.typefaceId,
                    fontSize = addedViewTextInfo.fontSizePx,
                    lineSpacingMultiplier = addedViewTextInfo.lineSpacingMultiplier,
                    letterSpacing = addedViewTextInfo.letterSpacing,
                    textColor = addedViewTextInfo.textColor,
                    shadowRadius = addedViewTextInfo.shadowRadius,
                    shadowDx = addedViewTextInfo.shadowDx,
                    shadowDy = addedViewTextInfo.shadowDy,
                    shadowColor = addedViewTextInfo.shadowColor
            )
        }
    }
}
