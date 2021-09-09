package com.wordpress.stories.compose.event

import com.wordpress.stories.compose.story.StoryIndex

class StoryLoadEvents {
    data class StoryLoadStart(
        var storyIndex: StoryIndex
    )

    data class StoryLoadEnd(
        var storyIndex: StoryIndex
    )
}
