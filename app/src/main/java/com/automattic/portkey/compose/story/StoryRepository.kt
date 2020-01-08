package com.automattic.portkey.compose.story

import java.util.Collections

object StoryRepository {
    private val currentStoryFrames = ArrayList<StoryFrameItem>()
    private val stories = ArrayList<Story>()

    fun loadStory(storyIndex: Int): ArrayList<StoryFrameItem> {
        if (stories.size > storyIndex) {
            currentStoryFrames.clear()
            currentStoryFrames.addAll(stories[storyIndex].frames)
        } else {
            // just crete a new story if we didn't find such index
            currentStoryFrames.clear()
        }
        return currentStoryFrames
    }

    fun addStoryFrameItemToCurrentStory(item: StoryFrameItem) {
        currentStoryFrames.add(0, item)
    }

    // when the user finishes a story, just add it to our repo for now and clear currentStory
    fun finishCurrentStory() {
        stories.add(Story(currentStoryFrames)) // create new Story wrapper with the finished frames in there
        currentStoryFrames.clear()
    }

    fun discardCurrentStory() {
        currentStoryFrames.clear()
    }

    fun getCurrentStoryFrameAt(index: Int): StoryFrameItem {
        return currentStoryFrames[index]
    }

    fun getImmutableCurrentStoryFrames(): List<StoryFrameItem> {
        return Collections.unmodifiableList<StoryFrameItem>(currentStoryFrames)
    }

    fun getCurrentStorySize(): Int {
        return currentStoryFrames.size
    }

    fun removeFrameAt(pos: Int) {
        currentStoryFrames.removeAt(pos)
    }
}
