package com.wordpress.stories.compose.text

import android.view.View

/**
 * A helper enum for declaring supported text alignments, and allowing them to be cycled through via [getNext].
 * Relies on allowed values of [View.setTextAlignment] as unique identifiers.
 */
enum class TextAlignment(val value: Int) {
    LEFT(View.TEXT_ALIGNMENT_TEXT_START),
    CENTER(View.TEXT_ALIGNMENT_CENTER),
    RIGHT(View.TEXT_ALIGNMENT_TEXT_END);

    companion object {
        fun default(): Int {
            return values().first().value
        }

        fun valueOf(value: Int): TextAlignment {
            return values().firstOrNull { it.value == value } ?: values().first()
        }

        /**
         * Given a [TextAlignment], returns the next [TextAlignment] by the order in which they were declared.
         */
        fun getNext(textAlignment: TextAlignment): TextAlignment {
            val oldIndex = values().indexOf(textAlignment)
            return values().getOrNull(oldIndex + 1) ?: values().first()
        }
    }
}
