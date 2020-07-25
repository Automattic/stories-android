package com.wordpress.stories.compose.text

import android.view.View

enum class TextAlignment(val value: Int) {
    LEFT(View.TEXT_ALIGNMENT_TEXT_START),
    CENTER(View.TEXT_ALIGNMENT_CENTER),
    RIGHT(View.TEXT_ALIGNMENT_TEXT_END);

    companion object {
        fun default(): Int {
            return values().first().value
        }

        fun valueOf(value: Int): TextAlignment {
            return values().firstOrNull() { it.value == value } ?: values().first()
        }

        fun getNext(textAlignment: TextAlignment): TextAlignment {
            val oldIndex = values().indexOf(textAlignment)
            return values().getOrNull(oldIndex + 1) ?: values().first()
        }
    }
}
