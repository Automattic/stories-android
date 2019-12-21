package com.automattic.portkey.compose.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StoryViewModel(val repository: StoryRepository, val storyIndex: Int) : ViewModel() {
    private var currentSelectedFrameIndex: Int = 0

    private val _onStoryFrameItems: MutableLiveData<List<StoryFrameItem>> by lazy {
        MutableLiveData<List<StoryFrameItem>>().also {
            it.value = repository.loadStory(storyIndex)
        }
    }
    val onStoryFrameItems: LiveData<List<StoryFrameItem>> = _onStoryFrameItems

    private val _onSelectedFrameIndex: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>().also {
            it.value = currentSelectedFrameIndex
        }
    }
    val onSelectedFrameIndex: LiveData<Int> = _onSelectedFrameIndex

    fun loadStory(storyIndex: Int) {
        val framesInStory = repository.loadStory(storyIndex)
        _onStoryFrameItems.value = framesInStory
        _onSelectedFrameIndex.value = 0 // default selected frame when loading a new Story
    }

    fun addStoryFrameItemToCurrentStory(item: StoryFrameItem) {
        repository.addStoryFrameItemToCurrentStory(item)
        _onStoryFrameItems.value = repository.getImmutableCurrentStoryFrames()
    }

    // when the user finishes a story, just add it to our repo for now and clear currentStory
    fun finishCurrentStory() {
        repository.finishCurrentStory()
        _onStoryFrameItems.value = repository.getImmutableCurrentStoryFrames()
        _onSelectedFrameIndex.value = 0 // default selected frame when loading a new Story
    }

    fun discardCurrentStory() {
        repository.discardCurrentStory()
        _onStoryFrameItems.value = repository.getImmutableCurrentStoryFrames()
    }

    // used when user taps on a different frame and keep track record of its state
    fun setSelectedFrame(index: Int): StoryFrameItem {
        val newlySelectedFrame = repository.getImmutableCurrentStoryFrames()[index]
        currentSelectedFrameIndex = index
        _onSelectedFrameIndex.value = index // default selected frame when loading a new Story
        return newlySelectedFrame
    }

    fun getSelectedFrameIndex(): Int {
        return currentSelectedFrameIndex
    }
}
