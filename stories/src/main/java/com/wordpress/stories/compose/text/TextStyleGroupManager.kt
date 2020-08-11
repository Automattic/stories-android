package com.wordpress.stories.compose.text

import android.content.Context
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
                typeface = getFont(R.font.nunito_bold),
                label = getString(R.string.typeface_label_nunito),
                defaultFontSize = 22F,
                lineSpacingMultiplier = 1.07F,
                shadowLayer = ShadowLayer(1F, 0F, 2F, getColor(R.color.black_25_transparent))
        )

        supportedTypefaces[TYPEFACE_ID_LIBRE_BASKERVILLE] = TextStyleRule(
                id = TYPEFACE_ID_LIBRE_BASKERVILLE,
                typeface = getFont(R.font.libre_baskerville),
                label = getString(R.string.typeface_label_libre_baskerville),
                defaultFontSize = 18F,
                lineSpacingMultiplier = 1.35F
        )

        supportedTypefaces[TYPEFACE_ID_OSWALD] = TextStyleRule(
                id = TYPEFACE_ID_OSWALD,
                typeface = getFont(R.font.oswald_upper),
                label = getString(R.string.typeface_label_oswald),
                defaultFontSize = 20F,
                lineSpacingMultiplier = 1.21F,
                letterSpacing = 0.06F
        )
    }

    private fun getFont(@FontRes fontRes: Int) = ResourcesCompat.getFont(context, fontRes)

    private fun getString(@StringRes stringRes: Int) = context.resources.getString(stringRes)

    @ColorInt private fun getColor(@ColorRes colorRes: Int) = ContextCompat.getColor(context, colorRes)

    fun styleTextView(typefaceId: Int, textView: TextView) {
        val textStyleRule = supportedTypefaces[typefaceId] ?: return

        with(textStyleRule) {
            textView.typeface = typeface
            textView.setShadowLayer(shadowLayer)

            textView.setLineSpacing(0F, lineSpacingMultiplier)
            textView.letterSpacing = letterSpacing

            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultFontSize)
        }
    }

    fun styleAndLabelTextView(typefaceId: Int, textView: TextView) {
        val textStyleRule = supportedTypefaces[typefaceId] ?: return

        textView.typeface = textStyleRule.typeface
        textView.setShadowLayer(textStyleRule.shadowLayer)

        textView.text = textStyleRule.label
    }

    /**
     * Returns the next typeface in the pre-defined order.
     */
    fun getNextTypeface(typefaceId: Int): Int {
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

        fun getIdentifiableTypefaceForId(@TypefaceId typefaceId: Int, context: Context): IdentifiableTypeface {
            @FontRes val fontRes = when (typefaceId) {
                TYPEFACE_ID_NUNITO -> R.font.nunito_bold
                TYPEFACE_ID_LIBRE_BASKERVILLE -> R.font.libre_baskerville
                TYPEFACE_ID_OSWALD -> R.font.oswald_upper
                else -> 0
            }
            return IdentifiableTypeface(typefaceId, ResourcesCompat.getFont(context, fontRes))
        }
    }
}
