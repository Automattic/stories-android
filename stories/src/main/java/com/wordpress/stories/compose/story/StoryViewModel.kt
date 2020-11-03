package com.wordpress.stories.compose.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wordpress.stories.compose.frame.FrameSaveService
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveResult
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveSuccess
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItemType.VIDEO
import com.wordpress.stories.compose.story.StoryViewModel.StoryFrameListItemUiState.StoryFrameListItemUiStateFrame
import com.wordpress.stories.util.SingleLiveEvent

class StoryViewModel(private val repository: StoryRepository, val storyIndex: StoryIndex) : ViewModel() {
    private var currentSelectedFrameIndex: Int = DEFAULT_SELECTION
    var useTempCaptureFile = true

    private val _uiState: MutableLiveData<StoryFrameListUiState> = MutableLiveData()
    val uiState: LiveData<StoryFrameListUiState> = _uiState

    private val _ErroredItemUiState: MutableLiveData<StoryFrameListItemUiStateFrame> = MutableLiveData()
    val erroredItemUiState: LiveData<StoryFrameListItemUiStateFrame> = _ErroredItemUiState

    private val _itemAtIndexChangedUiState = SingleLiveEvent<Int>()
    val itemAtIndexChangedUiState = _itemAtIndexChangedUiState

    private val _muteFrameAudioUiState = SingleLiveEvent<Int>()
    val muteFrameAudioUiState = _muteFrameAudioUiState

    private val _onSelectedFrameIndex: MutableLiveData<Pair<Int, Int>> by lazy {
        MutableLiveData<Pair<Int, Int>>().also {
            it.value = Pair(DEFAULT_SELECTION, currentSelectedFrameIndex)
        }
    }
    val onSelectedFrameIndex: MutableLiveData<Pair<Int, Int>> = _onSelectedFrameIndex

    private val _onFrameIndexMoved = SingleLiveEvent<Pair<Int, Int>>()
    val onFrameIndexMoved: SingleLiveEvent<Pair<Int, Int>> = _onFrameIndexMoved

    private val _addButtonClicked = SingleLiveEvent<Unit>()
    val addButtonClicked = _addButtonClicked

    private val _onUserSelectedFrame = SingleLiveEvent<Pair<Int, Int>>()
    val onUserSelectedFrame = _onUserSelectedFrame

    private val _onUserTappedCurrentFrame = SingleLiveEvent<Unit>()
    val onUserTappedCurrentFrame = _onUserTappedCurrentFrame

    private val _onUserLongPressedFrame = SingleLiveEvent<Pair<Int, Int>>()
    val onUserLongPressedFrame = _onUserLongPressedFrame

    fun createNewStory() {
        loadStory(StoryRepository.DEFAULT_NONE_SELECTED)
    }

    fun loadStory(storyIndex: StoryIndex) {
        repository.loadStory(storyIndex)?.let {
            updateUiState(createUiStateFromModelState(repository.getImmutableCurrentStoryFrames()))
            // default selected frame when loading a new Story
            _onSelectedFrameIndex.value = Pair(DEFAULT_SELECTION, DEFAULT_SELECTION)
        }
    }

    fun loadStory(story: Story) {
        repository.loadStory(story).let {
            updateUiState(createUiStateFromModelState(repository.getImmutableCurrentStoryFrames()))
            // default selected frame when loading a new Story
            _onSelectedFrameIndex.value = Pair(DEFAULT_SELECTION, DEFAULT_SELECTION)
        }
    }

    fun addStoryFrameItemToCurrentStory(item: StoryFrameItem) {
        repository.addStoryFrameItemToCurrentStory(item)
        updateUiState(createUiStateFromModelState(repository.getImmutableCurrentStoryFrames()))
    }

    fun discardCurrentStory() {
        if (useTempCaptureFile) {
            FrameSaveService.cleanUpTempStoryFrameFiles(getImmutableCurrentStoryFrames())
        }
        repository.discardCurrentStory()
        currentSelectedFrameIndex = DEFAULT_SELECTION // default selected frame when loading a new Story
        _onSelectedFrameIndex.value = Pair(DEFAULT_SELECTION, currentSelectedFrameIndex)
        updateUiState(createUiStateFromModelState(repository.getImmutableCurrentStoryFrames()))
    }

    fun setCurrentStoryTitle(title: String) {
        repository.setCurrentStoryTitle(title)
    }

    fun setSelectedFrame(index: Int): StoryFrameItem {
        val newlySelectedFrame = repository.getImmutableCurrentStoryFrames()[index]
        val oldIndex = currentSelectedFrameIndex
        currentSelectedFrameIndex = index
        updateUiStateForSelection(oldIndex, index)
        return newlySelectedFrame
    }

    fun setSelectedFrameByUser(index: Int, userInitiated: Boolean = false) {
        val oldIndex = currentSelectedFrameIndex
        setSelectedFrame(index)
        if (userInitiated && (oldIndex == index)) {
            _onUserTappedCurrentFrame.call()
        } else {
            _onUserSelectedFrame.value = Pair(oldIndex, index)
        }
    }

    private fun setLongPressedFrame(index: Int) {
        val oldIndex = currentSelectedFrameIndex
        setSelectedFrame(index)
        _onUserLongPressedFrame.value = Pair(oldIndex, index)
    }

    fun updateCurrentSelectedFrameOnRetryResult(frameSaveResult: FrameSaveResult) {
        repository.updateCurrentStorySaveResultOnFrame(
            currentSelectedFrameIndex,
            frameSaveResult
        )
        updateUiStateForError(currentSelectedFrameIndex, frameSaveResult.resultReason != SaveSuccess)
    }

    fun updateCurrentSelectedFrameOnAudioMuted(muteAudio: Boolean) {
        repository.updateCurrentSelectedFrameOnAudioMuted(
            currentSelectedFrameIndex,
            muteAudio
        )
        updateUiStateForAudioMuted(currentSelectedFrameIndex, muteAudio)
    }

    fun flipCurrentSelectedFrameOnAudioMuted() {
        if (getSelectedFrame()?.frameItemType is VIDEO) {
            updateCurrentSelectedFrameOnAudioMuted(!isSelectedFrameAudioMuted())
        }
    }

    fun isSelectedFrameAudioMuted(): Boolean {
        return (getSelectedFrame()?.frameItemType as? VIDEO)?.muteAudio ?: false
    }

    fun getSelectedFrameIndex(): Int {
        return currentSelectedFrameIndex
    }

    fun getSelectedFrame(): StoryFrameItem? {
        return getCurrentStoryFrameAt(currentSelectedFrameIndex)
    }

    fun getCurrentStoryFrameAt(index: Int): StoryFrameItem? {
        if (getCurrentStorySize() > index) {
            return repository.getImmutableCurrentStoryFrames()[index]
        } else {
            return null
        }
    }

    fun getCurrentStorySize(): Int {
        return repository.getCurrentStorySize()
    }

    fun getLastFrameIndexInCurrentStory(): Int {
        return getCurrentStorySize() - 1
    }

    fun getImmutableCurrentStoryFrames(): List<StoryFrameItem> {
        return repository.getImmutableCurrentStoryFrames()
    }

    fun getCurrentStoryIndex(): Int {
        return repository.currentStoryIndex
    }

    fun getStoryAtIndex(index: Int): Story {
        return repository.getStoryAtIndex(index)
    }

    fun anyOfCurrentStoryFramesIsErrored(): Boolean {
        val frames = repository.getImmutableCurrentStoryFrames()
        for (frame in frames) {
            if (frame.saveResultReason !is SaveSuccess) {
                return true
            }
        }
        return false
    }

    fun anyOfCurrentStoryFramesHasViews(): Boolean {
        val frames = repository.getImmutableCurrentStoryFrames()
        for (frame in frames) {
            if (frame.addedViews.size > 0) {
                return true
            }
        }
        return false
    }

    fun removeFrameAt(pos: Int) {
        // delete any temporal files
        if (useTempCaptureFile) {
            FrameSaveService.cleanUpTempStoryFrameFiles(getImmutableCurrentStoryFrames().subList(pos, pos + 1))
        }

        // remove from the repo
        repository.removeFrameAt(pos)

        // adjust index
        if (currentSelectedFrameIndex > 0 && pos <= currentSelectedFrameIndex) {
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

    fun swapItemsInPositions(pos1: Int, pos2: Int) {
        repository.swapItemsInPositions(pos1, pos2)
        // adjust currentSelectedFrameIndex so it reflects the movement only
        // if the movement occurred entierly to the left of the selection, don't update it
        // if the movement occurred from its left to its right, set the currentselection to be one position less
        // if the movement occurred from its right to its left, an insertion occurred on the left so our position
        // should get moved one position to the right
        // if the movement occurred entirely to the right of the selection, don't update it
        if (pos1 < currentSelectedFrameIndex && pos2 >= currentSelectedFrameIndex) {
            currentSelectedFrameIndex--
        } else if (pos1 > currentSelectedFrameIndex && pos2 <= currentSelectedFrameIndex) {
            currentSelectedFrameIndex++
        } else if (pos1 == currentSelectedFrameIndex) {
            currentSelectedFrameIndex = pos2
        }

        updateUiStateForItemSwap(pos1, pos2)
    }

    fun onSwapActionEnded() {
        updateUiState(createUiStateFromModelState(repository.getImmutableCurrentStoryFrames()))
    }

    private fun updateUiState(uiState: StoryFrameListUiState) {
        _uiState.value = uiState
    }

    private fun updateUiStateForSelection(oldSelectedIndex: Int, newSelectedIndex: Int) {
        _uiState.value?.let { immutableStory ->
            (immutableStory.items[oldSelectedIndex] as? StoryFrameListItemUiStateFrame)?.let {
                it.selected = false
            }

            (immutableStory.items[newSelectedIndex] as? StoryFrameListItemUiStateFrame)?.let {
                it.selected = true
            }
            _onSelectedFrameIndex.value = Pair(oldSelectedIndex, newSelectedIndex)
        }
    }

    private fun updateUiStateForError(selectedIndex: Int, errored: Boolean) {
        _uiState.value?.let { immutableStory ->
            (immutableStory.items[selectedIndex] as? StoryFrameListItemUiStateFrame)?.let {
                it.errored = errored
                _ErroredItemUiState.value = it
            }
        }
        _itemAtIndexChangedUiState.value = selectedIndex
    }

    private fun updateUiStateForAudioMuted(selectedIndex: Int, muteAudio: Boolean) {
        _uiState.value?.let { immutableStory ->
            (immutableStory.items[selectedIndex] as? StoryFrameListItemUiStateFrame)?.let {
                it.muteAudio = muteAudio
            }
        }
        _muteFrameAudioUiState.value = selectedIndex
    }

    private fun updateUiStateForItemSwap(oldIndex: Int, newIndex: Int) {
        _onFrameIndexMoved.value = Pair(oldIndex, newIndex)
    }

    private fun createUiStateFromModelState(storyItems: List<StoryFrameItem>): StoryFrameListUiState {
        val uiStateItems = ArrayList<StoryFrameListItemUiState>()
        val newUiState = StoryFrameListUiState(uiStateItems)
        storyItems.forEachIndexed { index, model ->
            val isSelected = (getSelectedFrameIndex() == index)
            val filePath =
                if ((model.source is UriBackgroundSource)) {
                    model.source.contentUri.toString()
                } else {
                    (model.source as FileBackgroundSource).file.toString()
                }
            val oneFrameUiState = StoryFrameListItemUiStateFrame(
                selected = isSelected, filePath = filePath, errored = model.saveResultReason != SaveSuccess
            )
            oneFrameUiState.onItemTapped = {
                setSelectedFrameByUser(index, userInitiated = true)
            }
            oneFrameUiState.onItemLongPressed = {
                setLongPressedFrame(index)
            }
            uiStateItems.add(oneFrameUiState)
        }
        return newUiState
    }

    data class StoryFrameListUiState(val items: List<StoryFrameListItemUiState>)

    sealed class StoryFrameListItemUiState() {
        var onItemTapped: (() -> Unit)? = null
        var onItemLongPressed: (() -> Unit)? = null

        data class StoryFrameListItemUiStateFrame(
            var selected: Boolean = false,
            var errored: Boolean = false,
            var filePath: String? = null,
            var muteAudio: Boolean = false
        ) : StoryFrameListItemUiState()
    }

    companion object {
        const val DEFAULT_SELECTION = 0
    }
}
