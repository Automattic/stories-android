package com.automattic.portkey.compose.story

import com.automattic.photoeditor.views.added.AddedViewList
import com.automattic.portkey.compose.story.StoryFrameItemType.IMAGE
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable

@Serializable
data class StoryFrameItem(
    val filePath: String,
    val frameItemType: StoryFrameItemType = IMAGE,
    @ContextualSerialization val addedViews: AddedViewList = AddedViewList(),
    val name: String? = null
)
