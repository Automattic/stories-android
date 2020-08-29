package com.automattic.photoeditor.text

import android.graphics.Typeface
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * A wrapper class for [Typeface] which includes a user-set id.
 *
 * This allows typeface data to be serialized, since there's otherwise
 * no way to identify which typeface is active on a TextView.
 */
data class IdentifiableTypeface(@TypefaceId val id: Int, val typeface: Typeface?) {
    @Retention(BINARY)
    @Target(FUNCTION, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER)
    annotation class TypefaceId
}
