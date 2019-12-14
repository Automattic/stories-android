package com.automattic.portkey.compose.story

class StoryRepository {
    val FAKE_CONTENT = Story(ArrayList())
    fun loadStory(storyId: Int) : ArrayList<StoryFrameItem> {
        // TODO load the story items here, for the storyId passed as argument
        FAKE_CONTENT.frames.add(StoryFrameItem("test1"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test2"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test3"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test4"))
        FAKE_CONTENT.frames.add(StoryFrameItem("test5"))
        return FAKE_CONTENT.frames
    }
}
