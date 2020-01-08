package com.automattic.portkey.compose.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.automattic.portkey.compose.story.StoryViewModel.StoryFrameListItemUiState.StoryFrameListItemUiStateFrame
import com.automattic.portkey.compose.story.StoryViewModel.StoryFrameListItemUiState.StoryFrameListItemUiStatePlusIcon
import com.automattic.portkey.util.SingleLiveEvent

class StoryViewModel(private val repository: StoryRepository, val storyIndex: Int) : ViewModel() {
    private var currentSelectedFrameIndex: Int = DEFAULT_SELECTION

    private val _uiState: MutableLiveData<StoryFrameListUiState> = MutableLiveData()
    val uiState: LiveData<StoryFrameListUiState> = _uiState

    private val _onSelectedFrameIndex: SingleLiveEvent<Pair<Int, Int>> by lazy {
        SingleLiveEvent<Pair<Int, Int>>().also {
            it.value = Pair(DEFAULT_SELECTION, currentSelectedFrameIndex)
        }
    }
    val onSelectedFrameIndex: SingleLiveEvent<Pair<Int, Int>> = _onSelectedFrameIndex

    private val _addButtonClicked = SingleLiveEvent<Unit>()
    val addButtonClicked = _addButtonClicked

    private val _onUserSelectedFrame = SingleLiveEvent<Pair<Int, Int>>()
    val onUserSelectedFrame = _onUserSelectedFrame

    fun loadStory(storyIndex: Int) {
        repository.loadStory(storyIndex)
        updateUiState(createUiStateFromModelState(repository.getImmutableCurrentStoryFrames()))
        // default selected frame when loading a new Story
        _onSelectedFrameIndex.value = Pair(DEFAULT_SELECTION, DEFAULT_SELECTION)
    }

    fun addStoryFrameItemToCurrentStory(item: StoryFrameItem) {
        repository.addStoryFrameItemToCurrentStory(item)
        updateUiState(createUiStateFromModelState(repository.getImmutableCurrentStoryFrames()))
    }

    // when the user finishes a story, just add it to our repo for now and clear currentStory
    fun finishCurrentStory() {
        repository.finishCurrentStory()
        updateUiState(createUiStateFromModelState(repository.getImmutableCurrentStoryFrames()))
        currentSelectedFrameIndex = DEFAULT_SELECTION // default selected frame when loading a new Story
        _onSelectedFrameIndex.value = Pair(DEFAULT_SELECTION, currentSelectedFrameIndex)
    }

    fun discardCurrentStory() {
        repository.discardCurrentStory()
        currentSelectedFrameIndex = DEFAULT_SELECTION // default selected frame when loading a new Story
        _onSelectedFrameIndex.value = Pair(DEFAULT_SELECTION, currentSelectedFrameIndex)
        updateUiState(createUiStateFromModelState(repository.getImmutableCurrentStoryFrames()))
    }

    fun setSelectedFrame(index: Int): StoryFrameItem {
        val newlySelectedFrame = repository.getImmutableCurrentStoryFrames()[index]
        val oldIndex = currentSelectedFrameIndex
        currentSelectedFrameIndex = index
        updateUiStateForSelection(oldIndex, index)
        return newlySelectedFrame
    }

    fun setSelectedFrameByUser(index: Int) {
        val oldIndex = currentSelectedFrameIndex
        setSelectedFrame(index)
        _onUserSelectedFrame.value = Pair(oldIndex, index)
    }

    fun getSelectedFrameIndex(): Int {
        return currentSelectedFrameIndex
    }

    fun getSelectedFrame(): StoryFrameItem {
        return getCurrentStoryFrameAt(currentSelectedFrameIndex)
    }

    fun getCurrentStoryFrameAt(index: Int): StoryFrameItem {
        return repository.getImmutableCurrentStoryFrames()[index]
    }

    fun getCurrentStorySize(): Int {
        return repository.getCurrentStorySize()
    }

    fun removeFrameAt(pos: Int) {
        // remove from the repo
        repository.removeFrameAt(pos)
        // adjust index
        if (currentSelectedFrameIndex > 0) {
            currentSelectedFrameIndex--
        }

        // if this Story no longer contains any frames, just discard it
        if (repository.getCurrentStorySize() == 0) {
            discardCurrentStory()
        } else {
            // if we have frames to keep, update the UI state with the new resulting frame array
            updateUiState(createUiStateFromModelState(repository.getImmutableCurrentStoryFrames()))
            // now let's select the one to the left
            setSelectedFrameByUser(currentSelectedFrameIndex)
        }
    }

    private fun updateUiState(uiState: StoryFrameListUiState) {
        _uiState.value = uiState
    }

    private fun updateUiStateForSelection(oldSelectedIndex: Int, newSelectedIndex: Int) {
        _uiState.value?.let { immutableStory ->
            (immutableStory.items[oldSelectedIndex + 1] as? StoryFrameListItemUiStateFrame)?.let {
                it.selected = false
            }

            (immutableStory.items[newSelectedIndex + 1] as? StoryFrameListItemUiStateFrame)?.let {
                it.selected = true
            }
            _onSelectedFrameIndex.value = Pair(oldSelectedIndex + 1, newSelectedIndex + 1)
        }
    }

    private fun createUiStateFromModelState(storyItems: List<StoryFrameItem>): StoryFrameListUiState {
        val uiStateItems = ArrayList<StoryFrameListItemUiState>()
        val newUiState = StoryFrameListUiState(uiStateItems)
        // add the plus icon to the UiState array
        uiStateItems.add(StoryFrameListItemUiStatePlusIcon)
        StoryFrameListItemUiStatePlusIcon.onItemTapped = {
            _addButtonClicked.call()
        }
        storyItems.forEachIndexed { index, model ->
            val isSelected = (getSelectedFrameIndex() == index)
            val filePath =
                if (model.source.isUri()) model.source.contentUri.toString() else model.source.file.toString()
            val oneFrameUiState = StoryFrameListItemUiStateFrame(
                selected = isSelected, filePath = filePath
            )
            oneFrameUiState.onItemTapped = {
                setSelectedFrameByUser(index)
            }
            uiStateItems.add(oneFrameUiState)
        }
        return newUiState
    }

    data class StoryFrameListUiState(val items: List<StoryFrameListItemUiState>)

    sealed class StoryFrameListItemUiState() {
        var onItemTapped: (() -> Unit)? = null

        object StoryFrameListItemUiStatePlusIcon : StoryFrameListItemUiState()

        data class StoryFrameListItemUiStateFrame(
            var selected: Boolean = false,
            var filePath: String? = null
        ) : StoryFrameListItemUiState()
    }

    companion object {
        const val DEFAULT_SELECTION = 0
    }
}
