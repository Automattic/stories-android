package com.automattic.portkey.compose.story

import java.util.Collections

object StoryRepository {
    private val currentStoryFrames = ArrayList<StoryFrameItem>()
    var currentStoryIndex = 0
        private set
    private val stories = ArrayList<Story>()
    const val DEFAULT_NONE_SELECTED = -1

    fun loadStory(storyIndex: Int): Story? {
        if (storyIndex == DEFAULT_NONE_SELECTED) {
            // if there's no specific Story to select, create and add a new empty Story, and return it
            createNewStory()
            return stories[currentStoryIndex]
        } else if (currentStoryIndex == storyIndex) {
            // if they ask to load the same Story that is already loaded, return the current Story
            return stories[storyIndex]
        } else if (stories.size > storyIndex) {
            // otherwise update the currentStoryIndex and currentStoryFrames values
            currentStoryIndex = storyIndex
            currentStoryFrames.clear()
            currentStoryFrames.addAll(stories[storyIndex].frames)
            return stories[storyIndex]
        } else {
            return null
        }
    }

    fun getStoryAtIndex(index: Int): Story {
        return stories[index]
    }

    fun createNewStory(): Int {
        currentStoryFrames.clear()
        val story = Story(ArrayList())
        stories.add(story)
        currentStoryIndex = stories.size - 1
        return currentStoryIndex
    }

    fun addStoryFrameItemToCurrentStory(item: StoryFrameItem) {
        currentStoryFrames.add(0, item)
    }

    // when the user finishes a story, just add it to our repo for now and clear currentStory
    fun finishCurrentStory(title: String? = null) {
        val frameList = ArrayList<StoryFrameItem>()
        frameList.addAll(currentStoryFrames)
        // override with passed title if not null, otherwise keep it from already existing current Story
        stories[currentStoryIndex] = Story(frameList, title ?: stories[currentStoryIndex].title)
        currentStoryFrames.clear()
        currentStoryIndex = DEFAULT_NONE_SELECTED
    }

    fun discardCurrentStory() {
        currentStoryFrames.clear()
        currentStoryIndex = DEFAULT_NONE_SELECTED
    }

    fun setCurrentStoryTitle(title: String) {
        stories[currentStoryIndex].title = title
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

    fun swapItemsInPositions(pos1: Int, pos2: Int) {
        Collections.swap(currentStoryFrames, pos1, pos2)
    }
}
