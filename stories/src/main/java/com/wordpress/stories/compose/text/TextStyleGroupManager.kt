package com.wordpress.stories.compose.text

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.annotation.Dimension.SP
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.automattic.photoeditor.text.IdentifiableTypeface
import com.automattic.photoeditor.text.IdentifiableTypeface.TypefaceId
import com.wordpress.stories.R
import java.util.TreeMap

/**
 * Helper class that keeps track of predefined supported text style rules and supports
 * formatting [TextView]s.
 */
class TextStyleGroupManager(val context: Context) {
    private data class TextStyleRule(
        val id: Int,
        val typeface: Typeface?,
        val label: String,
        @Dimension(unit = SP) val defaultFontSize: Float,
        val lineSpacingMultiplier: Float = 1F,
        val letterSpacing: Float = 0F,
        val shadowLayer: ShadowLayer? = null
    )

    data class ShadowLayer(
        @Dimension(unit = SP) val radius: Float,
        @Dimension(unit = SP) val dx: Float,
        @Dimension(unit = SP) val dy: Float,
        @ColorInt val color: Int
    )

    private var supportedTypefaces = TreeMap<Int, TextStyleRule>()

    init {
        supportedTypefaces[TYPEFACE_ID_NUNITO] = TextStyleRule(
                id = TYPEFACE_ID_NUNITO,
                typeface = getFont(TYPEFACE_ID_NUNITO),
                label = getString(R.string.typeface_label_nunito),
                defaultFontSize = 24F,
                lineSpacingMultiplier = 1.07F,
                shadowLayer = ShadowLayer(1F, 0F, 2F, getColor(R.color.black_25_transparent))
        )

        supportedTypefaces[TYPEFACE_ID_LIBRE_BASKERVILLE] = TextStyleRule(
                id = TYPEFACE_ID_LIBRE_BASKERVILLE,
                typeface = getFont(TYPEFACE_ID_LIBRE_BASKERVILLE),
                label = getString(R.string.typeface_label_libre_baskerville),
                defaultFontSize = 20F,
                lineSpacingMultiplier = 1.35F
        )

        supportedTypefaces[TYPEFACE_ID_OSWALD] = TextStyleRule(
                id = TYPEFACE_ID_OSWALD,
                typeface = getFont(TYPEFACE_ID_OSWALD),
                label = getString(R.string.typeface_label_oswald),
                defaultFontSize = 22F,
                lineSpacingMultiplier = 1.21F,
                letterSpacing = 0.06F
        )

        supportedTypefaces[TYPEFACE_ID_PACIFICO] = TextStyleRule(
                id = TYPEFACE_ID_PACIFICO,
                typeface = getFont(TYPEFACE_ID_PACIFICO),
                label = getString(R.string.typeface_label_pacifico),
                defaultFontSize = 24F,
                lineSpacingMultiplier = 0.99F,
                letterSpacing = 0.05F,
                shadowLayer = ShadowLayer(5F, 0F, 0F, getColor(R.color.white_50_transparent))
        )

        supportedTypefaces[TYPEFACE_ID_SPACE_MONO] = TextStyleRule(
                id = TYPEFACE_ID_SPACE_MONO,
                typeface = getFont(TYPEFACE_ID_SPACE_MONO),
                label = getString(R.string.typeface_label_space_mono),
                defaultFontSize = 20F,
                lineSpacingMultiplier = 1.20F,
                letterSpacing = -0.0138F
        )

        supportedTypefaces[TYPEFACE_ID_SHRIKHAND] = TextStyleRule(
                id = TYPEFACE_ID_SHRIKHAND,
                typeface = getFont(TYPEFACE_ID_SHRIKHAND),
                label = getString(R.string.typeface_label_shrikhand),
                defaultFontSize = 22F,
                lineSpacingMultiplier = 1.16F,
                shadowLayer = ShadowLayer(1F, 2F, 4F, getColor(R.color.black_25_transparent))
        )
    }

    private fun getFont(@TypefaceId typefaceId: Int) = resolveTypeface(typefaceId, context)

    private fun getString(@StringRes stringRes: Int) = context.resources.getString(stringRes)

    @ColorInt private fun getColor(@ColorRes colorRes: Int) = ContextCompat.getColor(context, colorRes)

    fun styleTextView(@TypefaceId typefaceId: Int, textView: TextView) {
        val textStyleRule = supportedTypefaces[typefaceId] ?: return

        with(textStyleRule) {
            textView.typeface = typeface
            textView.setShadowLayer(shadowLayer)

            textView.setLineSpacing(0F, lineSpacingMultiplier)
            textView.letterSpacing = letterSpacing

            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultFontSize)
        }
    }

    fun styleAndLabelTextView(@TypefaceId typefaceId: Int, textView: TextView) {
        val textStyleRule = supportedTypefaces[typefaceId] ?: return

        textView.typeface = textStyleRule.typeface
        textView.setShadowLayer(textStyleRule.shadowLayer)

        textView.text = textStyleRule.label
    }

    /**
     * Returns the next typeface in the pre-defined order.
     */
    fun getNextTypeface(@TypefaceId typefaceId: Int): Int {
        return supportedTypefaces.higherKey(typefaceId) ?: supportedTypefaces.firstKey()
    }

    private fun TextView.setShadowLayer(shadowLayer: ShadowLayer?) {
        shadowLayer?.run {
            setShadowLayer(radius.toPx(), dx.toPx(), dy.toPx(), color)
        } ?: run {
            setShadowLayer(0F, 0F, 0F, 0)
        }
    }

    private fun Float.toPx(): Float {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, this, context.resources.displayMetrics)
    }

    companion object {
        const val TYPEFACE_ID_NUNITO = 1001
        const val TYPEFACE_ID_LIBRE_BASKERVILLE = 1002
        const val TYPEFACE_ID_OSWALD = 1003
        const val TYPEFACE_ID_PACIFICO = 1004
        const val TYPEFACE_ID_SPACE_MONO = 1005
        const val TYPEFACE_ID_SHRIKHAND = 1006

        fun getIdentifiableTypefaceForId(@TypefaceId typefaceId: Int, context: Context): IdentifiableTypeface {
            return IdentifiableTypeface(typefaceId, resolveTypeface(typefaceId, context))
        }

        private fun resolveTypeface(@TypefaceId typefaceId: Int, context: Context): Typeface? {
            return when (typefaceId) {
                TYPEFACE_ID_NUNITO -> getFontWithFallback(context, R.font.nunito_bold, Typeface.SANS_SERIF)
                TYPEFACE_ID_LIBRE_BASKERVILLE -> getFontWithFallback(context, R.font.libre_baskerville, Typeface.SERIF)
                TYPEFACE_ID_OSWALD -> getFontWithFallback(context, R.font.oswald_upper, Typeface.SANS_SERIF)
                TYPEFACE_ID_PACIFICO -> getFontWithFallback(context, R.font.pacifico, Typeface.SERIF)
                TYPEFACE_ID_SPACE_MONO -> getFontWithFallback(context, R.font.space_mono_bold, Typeface.MONOSPACE)
                TYPEFACE_ID_SHRIKHAND -> getFontWithFallback(context, R.font.shrikhand, Typeface.DEFAULT_BOLD)
                else -> null
            }
        }

        private fun getFontWithFallback(context: Context, @FontRes fontRes: Int, fallback: Typeface?): Typeface? {
            return try {
                ResourcesCompat.getFont(context, fontRes)
            } catch (e: NotFoundException) {
                fallback
            }
        }
    }
}
