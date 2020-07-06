package com.automattic.photoeditor.views.added

import kotlinx.serialization.Serializable

@Serializable
data class AddedViewInfo(
    val rotation: Float,
    val translationX: Float,
    val translationY: Float,
    val scale: Float,
    val addedViewTextInfo: AddedViewTextInfo
)

@Serializable
data class AddedViewTextInfo(val text: String, val fontSizePx: Float, val textColor: Int)
