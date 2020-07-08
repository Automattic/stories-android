package com.wordpress.stories.compose.story

import kotlinx.serialization.Serializable

@Serializable
sealed class StoryFrameItemType {
    fun isSameType(second: StoryFrameItemType): Boolean {
        return this::class == second::class
    }

    @Serializable
    object IMAGE : StoryFrameItemType()

    @Serializable
    data class VIDEO(var muteAudio: Boolean = false) : StoryFrameItemType()
}
