package com.automattic.portkey.compose.story

import android.net.Uri
import com.automattic.photoeditor.views.added.AddedViewList
import com.automattic.portkey.compose.story.StoryFrameItemType.IMAGE
import java.io.File

data class StoryFrameItem(
    val file: File? = null,
    val contentUri: Uri? = null,
    val frameItemType: StoryFrameItemType = IMAGE,
    var addedViews: AddedViewList = AddedViewList(),
    val name: String? = null
)
