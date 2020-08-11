package com.automattic.photoeditor.text

import com.automattic.photoeditor.text.IdentifiableTypeface.TypefaceId

interface FontResolver {
    fun resolve(@TypefaceId typefaceId: Int): IdentifiableTypeface
}
