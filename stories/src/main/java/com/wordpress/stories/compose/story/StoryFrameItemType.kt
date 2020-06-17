package com.wordpress.stories.compose.story

sealed class StoryFrameItemType {
    fun isSameType(second: StoryFrameItemType): Boolean {
        return this::class == second::class
    }
    object IMAGE : StoryFrameItemType()
    data class VIDEO(var muteAudio: Boolean = false) : StoryFrameItemType()
}
