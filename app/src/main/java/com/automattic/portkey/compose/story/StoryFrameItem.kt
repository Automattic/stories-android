package com.automattic.portkey.compose.story

import com.automattic.portkey.compose.story.StoryFrameItemType.IMAGE
import kotlinx.serialization.Serializable

@Serializable
data class StoryFrameItem(
    val filePath: String,
    val frameItemType: StoryFrameItemType = IMAGE,
//    @ContextualSerialization var addedViews: AddedViewList = AddedViewList(),
    var addedViewsSerialized: String? = null,
    val name: String? = null
)
