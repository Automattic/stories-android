package com.wordpress.stories.compose.story

import kotlinx.serialization.Serializable

@Serializable
data class Story(val frames: ArrayList<StoryFrameItem>, var title: String? = null)
