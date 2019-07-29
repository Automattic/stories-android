package com.automattic.photoeditor


/**
 *
 *
 * Enum define for various operation happening on the [PhotoEditorView] while editing
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 18/01/2017.
 */
enum class ViewType {
    BRUSH_DRAWING,
    TEXT,
    IMAGE,
    EMOJI,
    STICKER_STATIC,     // this is static
    STICKER_ANIMATED    // this can be animated - needed this distinction for post-processing
}
