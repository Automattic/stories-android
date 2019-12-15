package com.automattic.portkey.compose.story

class StoryRepository {
    private var currentStory = Story(ArrayList())
    private var currentSelectedFrameIndex = 0
    private val stories = ArrayList<Story>()
    fun loadStory(storyIndex: Int): ArrayList<StoryFrameItem> {
        if (stories.size > storyIndex) {
            currentStory = stories[storyIndex]
        } else {
            // just crete a new story if we didn't find such index
            currentStory = Story(ArrayList())
        }
        return currentStory.frames
    }

    fun addStoryFrameItemToCurrentStory(item: StoryFrameItem) {
        currentStory.frames.add(0, item)
    }

    // when the user finishes a story, just add it to our repo for now and clear currentStory
    fun finishCurrentStory() {
        stories.add(currentStory)
        currentStory = Story(ArrayList())
    }

    // used when user taps on a different frame and keep track record of its state
    fun setSelectedFrame(index: Int): StoryFrameItem {
        currentSelectedFrameIndex = index
        return currentStory.frames[index]
    }

    companion object {
        private var singleRepoInstance: StoryRepository? = null
        fun getInstance(): StoryRepository {
            if (singleRepoInstance == null) {
                singleRepoInstance = StoryRepository()
            }
            return singleRepoInstance!!
        }
    }
}
