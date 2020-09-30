package com.automattic.photoeditor.views.added

import android.graphics.Color
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Dimension.PX
import com.automattic.photoeditor.text.PhotoEditorTextView
import com.automattic.photoeditor.text.IdentifiableTypeface.TypefaceId
import com.automattic.photoeditor.text.TextStyler.Companion.getTextBackgroundColor
import kotlinx.serialization.Serializable

@Serializable
data class AddedViewInfo(
    val rotation: Float,
    val translationX: Float,
    val translationY: Float,
    val scale: Float,
    val addedViewTextInfo: AddedViewTextInfo
)

@Serializable
data class AddedViewTextInfo(
    val text: String,
    val textAlignment: Int,
    @TypefaceId val typefaceId: Int,
    @Dimension(unit = PX) val fontSizePx: Float,
    val lineSpacingMultiplier: Float = 1F,
    val letterSpacing: Float = 0F,
    @ColorInt val textColor: Int,
    @ColorInt val textBackgroundColor: Int,
    @Dimension(unit = PX) val shadowRadius: Float = 0F,
    @Dimension(unit = PX) val shadowDx: Float = 0F,
    @Dimension(unit = PX) val shadowDy: Float = 0F,
    @ColorInt val shadowColor: Int? = null
) {
    companion object {
        fun fromTextView(textView: TextView): AddedViewTextInfo {
            with(textView) {
                return AddedViewTextInfo(
                        text = text.toString(),
                        textAlignment = textAlignment,
                        typefaceId = (this as? PhotoEditorTextView)?.identifiableTypeface?.id ?: 0,
                        fontSizePx = textSize,
                        lineSpacingMultiplier = lineSpacingMultiplier,
                        letterSpacing = letterSpacing,
                        textColor = currentTextColor,
                        textBackgroundColor = getTextBackgroundColor() ?: Color.TRANSPARENT,
                        shadowRadius = shadowRadius,
                        shadowDx = shadowDx,
                        shadowDy = shadowDy,
                        shadowColor = shadowColor
                )
            }
        }
    }
}
