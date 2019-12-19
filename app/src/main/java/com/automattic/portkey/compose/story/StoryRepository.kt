package com.automattic.portkey.compose.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object StoryRepository {
    private val currentStoryFrames = ArrayList<StoryFrameItem>()
    private var currentSelectedFrameIndex: Int = 0
    private val stories = ArrayList<Story>()
    private val currentStoryFramesLiveData = MutableLiveData<List<StoryFrameItem>>()

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
        currentStoryFramesLiveData.postValue(currentStoryFrames)
    }

    // when the user finishes a story, just add it to our repo for now and clear currentStory
    fun finishCurrentStory() {
        stories.add(Story(currentStoryFrames)) // create new Story wrapper with the finished frames in there
        currentStoryFrames.clear()
        currentStoryFramesLiveData.postValue(currentStoryFrames)
    }

    fun discardCurrentStory() {
        currentStoryFrames.clear()
        currentStoryFramesLiveData.postValue(currentStoryFrames)
    }

    // used when user taps on a different frame and keep track record of its state
    fun setSelectedFrame(index: Int): StoryFrameItem {
        currentSelectedFrameIndex = index
        return currentStoryFrames[index]
    }

    fun getSelectedFrameIndex(): Int {
        return currentSelectedFrameIndex
    }

    fun getCurrentStoryFramesLiveData(): LiveData<List<StoryFrameItem>> {
        return currentStoryFramesLiveData
    }
}
