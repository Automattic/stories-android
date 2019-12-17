package com.automattic.photoeditor.views.added

import android.net.Uri
import android.view.View
import com.automattic.photoeditor.views.ViewType
import kotlinx.serialization.ContextualSerialization

import kotlinx.serialization.Serializable

@Serializable
data class AddedView(
    @ContextualSerialization val view: View,
    val viewType: ViewType,
    @ContextualSerialization val uri: Uri? = null
)
