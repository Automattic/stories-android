package com.automattic.photoeditor.text

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Custom TextView which deprecates usual typeface settings/getters in favor of [identifiableTypeface],
 * which allows resolving a typeface from a TextView.
 */
class PhotoEditorTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    var identifiableTypeface: IdentifiableTypeface? = null
        set(value) {
            super.setTypeface(value?.typeface)
            field = value
        }

    @Deprecated("Use IdentifiableTypeface", ReplaceWith("identifiableTypeface"))
    override fun setTypeface(tf: Typeface?) {
        super.setTypeface(tf)
    }

    @Deprecated("Use IdentifiableTypeface", ReplaceWith("identifiableTypeface"))
    override fun setTypeface(tf: Typeface?, style: Int) {
        super.setTypeface(tf, style)
    }

    @Deprecated("Use IdentifiableTypeface", ReplaceWith("identifiableTypeface"))
    override fun getTypeface(): Typeface {
        return super.getTypeface()
    }
}
