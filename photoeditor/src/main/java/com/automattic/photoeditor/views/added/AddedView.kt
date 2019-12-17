package com.automattic.photoeditor.views.added

import android.net.Uri
import android.view.View
import com.automattic.photoeditor.views.ViewType

import kotlinx.serialization.Serializable

@Serializable
data class AddedView(val view: View, val viewType: ViewType, val uri: Uri? = null)
