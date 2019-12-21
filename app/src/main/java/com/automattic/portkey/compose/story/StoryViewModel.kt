package com.automattic.portkey.compose.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StoryViewModel(val repository: StoryRepository, val storyIndex: Int) : ViewModel() {
    private var currentSelectedFrameIndex: Int = DEFAULT_SELECTION

    private val _onStoryFrameItems: MutableLiveData<List<StoryFrameItem>> by lazy {
        MutableLiveData<List<StoryFrameItem>>().also {
            it.value = repository.loadStory(storyIndex)
        }
    }
    val onStoryFrameItems: LiveData<List<StoryFrameItem>> = _onStoryFrameItems

    private val _onSelectedFrameIndex: MutableLiveData<Pair<Int, Int>> by lazy {
        MutableLiveData<Pair<Int, Int>>().also {
            it.value = Pair(DEFAULT_SELECTION, currentSelectedFrameIndex)
        }
    }
    val onSelectedFrameIndex: LiveData<Pair<Int, Int>> = _onSelectedFrameIndex

    fun loadStory(storyIndex: Int) {
        val framesInStory = repository.loadStory(storyIndex)
        _onStoryFrameItems.value = framesInStory
        _onSelectedFrameIndex.value = Pair(0, 0) // default selected frame when loading a new Story
    }

    fun addStoryFrameItemToCurrentStory(item: StoryFrameItem) {
        repository.addStoryFrameItemToCurrentStory(item)
        _onStoryFrameItems.value = repository.getImmutableCurrentStoryFrames()
    }

    // when the user finishes a story, just add it to our repo for now and clear currentStory
    fun finishCurrentStory() {
        repository.finishCurrentStory()
        _onStoryFrameItems.value = repository.getImmutableCurrentStoryFrames()
        currentSelectedFrameIndex = DEFAULT_SELECTION // default selected frame when loading a new Story
        _onSelectedFrameIndex.value = Pair(DEFAULT_SELECTION, currentSelectedFrameIndex)
    }

    fun discardCurrentStory() {
        repository.discardCurrentStory()
        currentSelectedFrameIndex = DEFAULT_SELECTION // default selected frame when loading a new Story
        _onSelectedFrameIndex.value = Pair(DEFAULT_SELECTION, currentSelectedFrameIndex)
        _onStoryFrameItems.value = repository.getImmutableCurrentStoryFrames()
    }

    // used when user taps on a different frame and keep track record of its state
    fun setSelectedFrame(index: Int): StoryFrameItem {
        val newlySelectedFrame = repository.getImmutableCurrentStoryFrames()[index]
        val oldIndex = currentSelectedFrameIndex
        currentSelectedFrameIndex = index
        _onSelectedFrameIndex.value = Pair(oldIndex, index)
        return newlySelectedFrame
    }

    fun getSelectedFrameIndex(): Int {
        return currentSelectedFrameIndex
    }

    companion object {
        const val DEFAULT_SELECTION = 0
    }
}
