package com.wordpress.stories.compose.text

class TextEditorAnalyticsHandler(private val callback: (properties: Map<String, *>) -> Unit) {
    private var textStyle = ANALYTICS_PROPERTY_TEXT_STYLE_VALUE_UNCHANGED

    fun trackTextStyleToggled(newStyle: String) {
        textStyle = newStyle
    }

    fun report() = callback.invoke(assembleProperties())

    private fun assembleProperties(): Map<String, *> {
        return mapOf(
                ANALYTICS_PROPERTY_KEY_TEXT_STYLE to textStyle
        )
    }

    companion object {
        const val ANALYTICS_PROPERTY_KEY_TEXT_STYLE = "text_style"
        const val ANALYTICS_PROPERTY_TEXT_STYLE_VALUE_UNCHANGED = "unchanged"
    }
}
