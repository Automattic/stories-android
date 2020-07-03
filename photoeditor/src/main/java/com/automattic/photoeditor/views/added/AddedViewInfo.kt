package com.automattic.photoeditor.views.added

import kotlinx.serialization.Serializable

@Serializable
data class AddedViewInfo(
    val rotation: Float,
    val translationX: Float,
    val translationY: Float,
    val scale: Float,
    val text: String
)
