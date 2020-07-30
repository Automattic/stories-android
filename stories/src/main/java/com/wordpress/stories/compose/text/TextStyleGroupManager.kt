package com.wordpress.stories.compose.text

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.wordpress.stories.R
import java.util.TreeMap

/**
 * Helper class that keeps track of predefined supported text style rules and supports
 * formatting [TextView]s.
 */
class TextStyleGroupManager(val context: Context) {
    private data class TextStyleRule(val id: Int, val typeface: Typeface?, val label: String)

    private var supportedTypefaces = TreeMap<Int, TextStyleRule>()

    init {
        supportedTypefaces[TYPEFACE_ID_NUNITO] = TextStyleRule(
                id = TYPEFACE_ID_NUNITO,
                typeface = getFont(R.font.nunito_bold),
                label = getString(R.string.typeface_label_nunito)
        )

        supportedTypefaces[TYPEFACE_ID_LIBRE_BASKERVILLE] = TextStyleRule(
                id = TYPEFACE_ID_LIBRE_BASKERVILLE,
                typeface = getFont(R.font.libre_baskerville),
                label = getString(R.string.typeface_label_libre_baskerville)
        )

        supportedTypefaces[TYPEFACE_ID_OSWALD] = TextStyleRule(
                id = TYPEFACE_ID_OSWALD,
                typeface = getFont(R.font.oswald),
                label = getString(R.string.typeface_label_oswald)
        )
    }

    private fun getFont(@FontRes fontRes: Int) = ResourcesCompat.getFont(context, fontRes)

    private fun getString(@StringRes stringRes: Int) = context.resources.getString(stringRes)

    fun styleTextView(typefaceId: Int, textView: TextView) {
        val typefaceRule = supportedTypefaces[typefaceId] ?: return

        applyStyleRulesToTextView(typefaceRule, textView)
    }

    fun styleAndLabelTextView(typefaceId: Int, textView: TextView) {
        val typefaceRule = supportedTypefaces[typefaceId] ?: return

        applyStyleRulesToTextView(typefaceRule, textView)

        textView.text = typefaceRule.label
    }

    private fun applyStyleRulesToTextView(textStyleRule: TextStyleRule, textView: TextView) {
        textView.typeface = textStyleRule.typeface
    }

    /**
     * Returns the next typeface in the pre-defined order.
     */
    fun getNextTypeface(typefaceId: Int): Int {
        return supportedTypefaces.higherKey(typefaceId) ?: supportedTypefaces.firstKey()
    }

    companion object {
        const val TYPEFACE_ID_NUNITO = 1001
        const val TYPEFACE_ID_LIBRE_BASKERVILLE = 1002
        const val TYPEFACE_ID_OSWALD = 1003

        fun getTypefaceResForId(typefaceId: Int) : Int {
            return when (typefaceId) {
                TYPEFACE_ID_NUNITO -> R.font.nunito_bold
                TYPEFACE_ID_LIBRE_BASKERVILLE -> R.font.libre_baskerville
                TYPEFACE_ID_OSWALD -> R.font.oswald
                else -> 0
            }
        }
    }
}
