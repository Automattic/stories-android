package com.automattic.portkey.compose.story

import com.automattic.portkey.compose.story.StoryFrameItemType.IMAGE

data class StoryFrameItem(val name: String, val filePath: String? = null, val frameItemType: StoryFrameItemType = IMAGE)
