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
import com.automattic.photoeditor.text.IdentifiableTypeface
import com.automattic.photoeditor.text.IdentifiableTypeface.TypefaceId
import com.automattic.photoeditor.text.RoundedBackgroundColorSpan
import com.wordpress.stories.R
import java.util.Locale
import java.util.TreeMap
import kotlin.math.roundToInt

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

    // Tracks whether the user has changed the font size
    var customFontSizeApplied = false

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
                lineSpacingMultiplier = 1.15F,
                letterSpacing = -0.0138F
        )

        supportedTypefaces[TYPEFACE_ID_SHRIKHAND] = TextStyleRule(
                id = TYPEFACE_ID_SHRIKHAND,
                typeface = getFont(TYPEFACE_ID_SHRIKHAND),
                label = getString(R.string.typeface_label_shrikhand),
                defaultFontSize = 22F,
                lineSpacingMultiplier = 1.11F,
                letterSpacing = 0.03F,
                shadowLayer = ShadowLayer(1F, 1F, 2F, getColor(R.color.black_25_transparent))
        )
    }

    private fun getFont(@TypefaceId typefaceId: Int) = resolveTypeface(typefaceId)

    private fun getString(@StringRes stringRes: Int) = context.resources.getString(stringRes)

    @ColorInt private fun getColor(@ColorRes colorRes: Int) = ContextCompat.getColor(context, colorRes)

    fun styleTextView(@TypefaceId typefaceId: Int, textView: TextView, fontSizePx: Float = 0F) {
        val textStyleRule = supportedTypefaces[typefaceId] ?: return

        with(textStyleRule) {
            textView.typeface = typeface

            // If the text has a background color span, apply a shadow layer matching the span padding
            // Otherwise, set the custom shadow layer for the font style
            val backgroundColorSpan = RoundedBackgroundColorSpan.from(textView)
            if (backgroundColorSpan != null) {
                textView.setShadowLayer(backgroundColorSpan.padding, 0F, 0F, 0)
            } else {
                textView.setShadowLayer(shadowLayer)
            }

            textView.setLineSpacing(0F, lineSpacingMultiplier)
            textView.letterSpacing = letterSpacing

            if (customFontSizeApplied) {
                // No op: leave the textView's font size intact
            } else if (fontSizePx > 0 && fontSizePx != defaultFontSize.spToPx()) {
                // We were given a specific font size, and it's not the default for this font, so it's been
                // customized by the user at some point.
                // Apply the font and toggle on custom font size mode.
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx)
                customFontSizeApplied = true
            } else {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultFontSize)
            }
        }
    }

    fun styleAndLabelTextView(@TypefaceId typefaceId: Int, textView: TextView) {
        val textStyleRule = supportedTypefaces[typefaceId] ?: return

        textView.typeface = textStyleRule.typeface
        textView.setShadowLayer(textStyleRule.shadowLayer)

        textView.text = textStyleRule.label

        // Add some corrective padding so the label for each font is kept at roughly center,
        // despite their different intrinsic heights and placements
        adjustTextViewLabelAlignment(typefaceId, textView)
    }

    /**
     * Returns the next typeface in the pre-defined order.
     */
    fun getNextTypeface(@TypefaceId typefaceId: Int): Int {
        return supportedTypefaces.higherKey(typefaceId) ?: supportedTypefaces.firstKey()
    }

    fun getAnalyticsLabelFor(@TypefaceId typefaceId: Int): String {
        return supportedTypefaces[typefaceId]?.label?.toLowerCase(Locale.ROOT).orEmpty()
    }

    private fun adjustTextViewLabelAlignment(@TypefaceId typefaceId: Int, textView: TextView) {
        // Always reset text size since it's modified for Pacifico
        var newTextSize = 18F
        var paddingBottom = 0F
        var paddingTop = 0F

        when (typefaceId) {
            TYPEFACE_ID_NUNITO -> paddingBottom = 1.9F
            TYPEFACE_ID_LIBRE_BASKERVILLE -> paddingTop = 1.5F
            TYPEFACE_ID_OSWALD -> paddingBottom = 2.3F
            TYPEFACE_ID_PACIFICO -> {
                paddingBottom = 4.5F
                newTextSize = 15F
            }
            TYPEFACE_ID_SPACE_MONO -> {}
            TYPEFACE_ID_SHRIKHAND -> paddingTop = 2.3F
        }

        textView.setPadding(0, paddingTop.dpToPx().roundToInt(), 0, paddingBottom.dpToPx().roundToInt())
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, newTextSize)
    }

    private fun TextView.setShadowLayer(shadowLayer: ShadowLayer?) {
        shadowLayer?.run {
            setShadowLayer(radius.spToPx(), dx.spToPx(), dy.spToPx(), color)
        } ?: run {
            setShadowLayer(0F, 0F, 0F, 0)
        }
    }

    private fun Float.spToPx(): Float {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, this, context.resources.displayMetrics)
    }

    private fun Float.dpToPx(): Float {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)
    }

    companion object {
        const val TYPEFACE_ID_NUNITO = 1001
        const val TYPEFACE_ID_LIBRE_BASKERVILLE = 1002
        const val TYPEFACE_ID_OSWALD = 1003
        const val TYPEFACE_ID_PACIFICO = 1004
        const val TYPEFACE_ID_SPACE_MONO = 1005
        const val TYPEFACE_ID_SHRIKHAND = 1006

        fun getIdentifiableTypefaceForId(@TypefaceId typefaceId: Int): IdentifiableTypeface {
            return IdentifiableTypeface(typefaceId, resolveTypeface(typefaceId))
        }

        /**
         * Initializes the custom fonts, including possibly downloading some of them for the first time.
         * This is so they're ready when the text editor is eventually opened and we don't have to block
         * the main thread or use the fallback fonts.
         */
        fun preloadFonts(context: Context) {
            FontRequester.registerFont(context, R.font.nunito_bold, Typeface.SANS_SERIF)
            FontRequester.registerFont(context, R.font.libre_baskerville, Typeface.SERIF)
            FontRequester.registerFont(context, R.font.oswald_upper, Typeface.SANS_SERIF)
            FontRequester.registerFont(context, R.font.pacifico, Typeface.SERIF)
            FontRequester.registerFont(context, R.font.space_mono_bold, Typeface.MONOSPACE)
            FontRequester.registerFont(context, R.font.shrikhand, Typeface.DEFAULT_BOLD)
        }

        private fun resolveTypeface(@TypefaceId typefaceId: Int): Typeface? {
            return when (typefaceId) {
                TYPEFACE_ID_NUNITO -> getFontWithFallback(R.font.nunito_bold, Typeface.SANS_SERIF)
                TYPEFACE_ID_LIBRE_BASKERVILLE -> getFontWithFallback(R.font.libre_baskerville, Typeface.SERIF)
                TYPEFACE_ID_OSWALD -> getFontWithFallback(R.font.oswald_upper, Typeface.SANS_SERIF)
                TYPEFACE_ID_PACIFICO -> getFontWithFallback(R.font.pacifico, Typeface.SERIF)
                TYPEFACE_ID_SPACE_MONO -> getFontWithFallback(R.font.space_mono_bold, Typeface.MONOSPACE)
                TYPEFACE_ID_SHRIKHAND -> getFontWithFallback(R.font.shrikhand, Typeface.DEFAULT_BOLD)
                else -> null
            }
        }

        private fun getFontWithFallback(@FontRes fontRes: Int, fallback: Typeface): Typeface {
            return FontRequester.getFont(fontRes, fallback)
        }
    }
}
