package com.automattic.portkey.compose.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.automattic.photoeditor.views.added.AddedViewList
import kotlinx.serialization.json.Json

class StoryRepository {
    private var currentStoryFrames = ArrayList<StoryFrameItem>()
    private var currentSelectedFrameIndex: Int = 0
    private val stories = ArrayList<Story>()
    private val currentStoryFramesLiveData = MutableLiveData<List<StoryFrameItem>>()

    fun loadStory(storyIndex: Int): ArrayList<StoryFrameItem> {
        if (stories.size > storyIndex) {
            currentStoryFrames = stories[storyIndex].frames
        } else {
            // just crete a new story if we didn't find such index
            currentStoryFrames = ArrayList()
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
        currentStoryFrames = ArrayList()
        currentStoryFramesLiveData.postValue(currentStoryFrames)
    }

    fun discardCurrentStory() {
        currentStoryFrames = ArrayList()
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

    fun getCurrentStoryFrameAt(index: Int): StoryFrameItem {
        return currentStoryFrames[index]
    }

    fun getCurrentStoryFramesLiveData(): LiveData<List<StoryFrameItem>> {
        return currentStoryFramesLiveData
    }

    fun serializeStory(story: Story): String {
        return Json.stringify(Story.serializer(), story)
    }

    fun serializeStoryFrameItem(storyFrameItem: StoryFrameItem): String {
        return Json.stringify(StoryFrameItem.serializer(), storyFrameItem)
    }

    fun deserializeStory(story: String): Story {
        return Json.parse(Story.serializer(), story)
    }

    fun deserializeStoryFrameItem(storyFrameItem: String): StoryFrameItem {
        return Json.parse(StoryFrameItem.serializer(), storyFrameItem)
    }

    fun serializeAddedViews(addedViews: AddedViewList): String {
        return Json.stringify(AddedViewList.serializer(), addedViews)
    }

    fun deserializeAddedViews(addedViews: String): AddedViewList {
        return Json.parse(AddedViewList.serializer(), addedViews)
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
