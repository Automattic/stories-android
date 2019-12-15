package com.automattic.portkey.compose.story

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StoryViewModel(val repository: StoryRepository, val storyIndex: Int) : ViewModel() {
    val storyFrameItems: MutableLiveData<List<StoryFrameItem>> by lazy {
        MutableLiveData<List<StoryFrameItem>>().also {
            it.value = repository.loadStory(storyIndex)
        }
    }
}
