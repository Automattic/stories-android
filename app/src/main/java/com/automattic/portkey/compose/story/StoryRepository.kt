package com.automattic.portkey.compose.story

import com.automattic.portkey.compose.frame.FrameSaveService.StorySaveResult
import java.util.Collections

typealias StoryIndex = Int

object StoryRepository {
    const val DEFAULT_NONE_SELECTED = -1
    private val currentStoryFrames = ArrayList<StoryFrameItem>()
    var currentStoryIndex = DEFAULT_NONE_SELECTED
        private set
    private val stories = ArrayList<Story>()

    fun loadStory(storyIndex: StoryIndex): Story? {
        when {
            storyIndex == DEFAULT_NONE_SELECTED -> {
                // if there's no specific Story to select, create and add a new empty Story, and return it
                createNewStory()
                return stories[currentStoryIndex]
            }
            (currentStoryIndex == storyIndex && isStoryIndexValid(storyIndex)) -> {
                // if they ask to load the same Story that is already loaded, return the current Story
                return stories[storyIndex]
            }
            (stories.size > storyIndex && isStoryIndexValid(storyIndex)) -> {
                // otherwise update the currentStoryIndex and currentStoryFrames values
                currentStoryIndex = storyIndex
                currentStoryFrames.clear()
                currentStoryFrames.addAll(stories[storyIndex].frames)
                return stories[storyIndex]
            } else -> {
                return null
            }
        }
    }

    private fun isStoryIndexValid(storyIndex: Int): Boolean {
        return storyIndex > DEFAULT_NONE_SELECTED
    }

    fun getStoryAtIndex(index: Int): Story {
        return stories[index]
    }

    private fun createNewStory(): Int {
        currentStoryFrames.clear()
        val story = Story(ArrayList())
        stories.add(story)
        currentStoryIndex = stories.size - 1
        return currentStoryIndex
    }

    fun addStoryFrameItemToCurrentStory(item: StoryFrameItem) {
        currentStoryFrames.add(item)
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

    fun setCurrentStorySaveResultsOnFrames(storyIndex: Int, saveResult: StorySaveResult) {
        // iterate over the StorySaveResult, check their indexes, and set the corresponding frame result
        for (index in 0..saveResult.frameSaveResult.size - 1) {
            val frameIdxToSet = saveResult.frameSaveResult[index].frameIndex
            stories[storyIndex].frames[frameIdxToSet].saveResultReason = saveResult.frameSaveResult[index].resultReason
        }
    }

    fun getCurrentStoryFrameAt(index: Int): StoryFrameItem {
        return currentStoryFrames[index]
    }

    fun getImmutableCurrentStoryFrames(): List<StoryFrameItem> {
        return currentStoryFrames.toList()
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
