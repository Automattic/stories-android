package com.automattic.portkey.compose.story

import kotlinx.serialization.Serializable

@Serializable
data class Story(val frames: ArrayList<StoryFrameItem>)
