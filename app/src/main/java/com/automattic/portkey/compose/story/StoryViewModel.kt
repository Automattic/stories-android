package com.automattic.portkey.compose.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StoryViewModel(val repository: StoryRepository, val storyId: Int) : ViewModel() {
    private val storyFrameItems: MutableLiveData<List<StoryFrameItem>> by lazy {
        MutableLiveData<List<StoryFrameItem>>().also {
            repository.loadStory(storyId)
        }
    }

    fun getStoryFrameItems(): LiveData<List<StoryFrameItem>> {
        return storyFrameItems
    }
}
