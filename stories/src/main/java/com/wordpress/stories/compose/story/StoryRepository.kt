package com.wordpress.stories.compose.story

import com.wordpress.stories.compose.frame.FrameIndex
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveResult
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveSuccess
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItemType.VIDEO
import java.util.Collections

typealias StoryIndex = Int

object StoryRepository {
    const val DEFAULT_NONE_SELECTED = -1
    const val DEFAULT_FRAME_NONE_SELECTED = -1
    var currentStoryIndex = DEFAULT_NONE_SELECTED
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
                return stories[storyIndex]
            }
            else -> {
                return null
            }
        }
    }

    fun loadStory(story: Story): StoryIndex {
        stories.add(story)
        currentStoryIndex = stories.size - 1
        return currentStoryIndex
    }

    fun replaceCurrentStory(story: Story): StoryIndex {
        if (isStoryIndexValid(currentStoryIndex)) {
            stories[currentStoryIndex] = story
            return currentStoryIndex
        } else {
            return loadStory(story)
        }
    }

    fun isStoryIndexValid(storyIndex: StoryIndex): Boolean {
        return storyIndex > DEFAULT_NONE_SELECTED && stories.size > storyIndex
    }

    fun getStoryAtIndex(index: StoryIndex): Story {
        return stories[index]
    }

    fun getImmutableStories(): List<Story> {
        return stories.toList()
    }

    fun findStoryContainingStoryFrameItemsByIds(ids: ArrayList<String>): StoryIndex {
        // now look for a Story in the StoryRepository that contains a matching frame id and return the story index
        for ((index, story) in stories.withIndex()) {
            // find the MediaModel for a given Uri from composedFrameFile
            if (story.frames.filter { ids.contains(it.id) }.size == ids.size) {
                // here we found the story whose frames collection contains all of the passed ids
                return index
            }
        }
        return DEFAULT_NONE_SELECTED
    }

    private fun createNewStory(): StoryIndex {
        val story = Story(ArrayList())
        stories.add(story)
        currentStoryIndex = stories.size - 1
        return currentStoryIndex
    }

    fun addStoryFrameItemToCurrentStory(item: StoryFrameItem) {
        if (!isStoryIndexValid(currentStoryIndex)) return
        stories[currentStoryIndex].frames.add(item)
    }

    // when the user finishes a story, just add it to our repo for now and clear currentStory
    fun finishCurrentStory(title: String? = null) {
        val frameList = ArrayList<StoryFrameItem>()
        frameList.addAll(stories[currentStoryIndex].frames)
        // override with passed title if not null, otherwise keep it from already existing current Story
        stories[currentStoryIndex] = Story(frameList, title ?: stories[currentStoryIndex].title)
        currentStoryIndex = DEFAULT_NONE_SELECTED
    }

    fun discardCurrentStory() {
        if (isStoryIndexValid(currentStoryIndex)) {
            stories.removeAt(currentStoryIndex)
        }
        currentStoryIndex = DEFAULT_NONE_SELECTED
    }

    fun setCurrentStoryTitle(title: String) {
        if (isStoryIndexValid(currentStoryIndex)) {
            stories[currentStoryIndex].title = title
        }
    }

    fun getCurrentStoryTitle(): String? {
        if (isStoryIndexValid(currentStoryIndex)) {
            return stories[currentStoryIndex].title
        } else {
            return ""
        }
    }

    fun setCurrentStorySaveResultsOnFrames(storyIndex: StoryIndex, saveResult: StorySaveResult) {
        // iterate over the StorySaveResult, check their indexes, and set the corresponding frame result
        if (isStoryIndexValid(storyIndex)) {
            for (index in 0..saveResult.frameSaveResult.size - 1) {
                if (saveResult.frameSaveResult[index] != null) {
                    val frameIdxToSet = saveResult.frameSaveResult[index].frameIndex
                    stories[storyIndex].frames[frameIdxToSet].saveResultReason =
                            saveResult.frameSaveResult[index].resultReason
                }
            }
        }
    }

    fun updateCurrentStorySaveResultOnFrame(frameIndex: FrameIndex, frameSaveResult: FrameSaveResult) {
        if (!isStoryIndexValid(currentStoryIndex)) return
        stories[currentStoryIndex].frames[frameIndex].saveResultReason = frameSaveResult.resultReason
    }

    fun updateCurrentSelectedFrameOnAudioMuted(frameIndex: FrameIndex, muteAudio: Boolean) {
        if (!isStoryIndexValid(currentStoryIndex)) return
        (stories[currentStoryIndex].frames[frameIndex].frameItemType as? VIDEO)?.muteAudio = muteAudio
    }

    fun getCurrentStoryFrameAt(index: Int): StoryFrameItem {
        return stories[currentStoryIndex].frames[index]
    }

    fun getImmutableCurrentStoryFrames(): List<StoryFrameItem> {
        if (!isStoryIndexValid(currentStoryIndex)) {
            return emptyList()
        }
        return stories[currentStoryIndex].frames.toList()
    }

    fun getCurrentStorySize(): Int {
        if (!isStoryIndexValid(currentStoryIndex)) {
            return 0
        }
        return stories[currentStoryIndex].frames.size
    }

    fun removeFrameAt(pos: Int) {
        if (!isStoryIndexValid(currentStoryIndex)) return
        stories[currentStoryIndex].frames.removeAt(pos)
    }

    fun swapItemsInPositions(pos1: Int, pos2: Int) {
        if (!isStoryIndexValid(currentStoryIndex)) return
        Collections.swap(stories[currentStoryIndex].frames, pos1, pos2)
    }

    fun getCurrentStoryThumbnailUrl(): String {
        if (!isStoryIndexValid(currentStoryIndex)) return ""

        val model = stories[currentStoryIndex].frames[0]
        return if ((model.source is UriBackgroundSource)) {
            model.source.contentUri.toString()
        } else {
            (model.source as FileBackgroundSource).file.toString()
        }
    }

    fun getCurrentStorySaveProgress(storyIndex: StoryIndex, oneItemActualProgress: Float = 0.0F): Float {
        var currentProgress = 0.0F
        if (isStoryIndexValid(storyIndex)) {
            val story = getStoryAtIndex(storyIndex)
            if (story.frames.isNotEmpty()) {
                val readyFrameCount = story.frames.filter {
                    it.composedFrameFile != null && it.saveResultReason is SaveSuccess
                }.count()
                currentProgress = (readyFrameCount / story.frames.size).toFloat()
                currentProgress += (oneItemActualProgress / story.frames.size)
            }
        }
        return currentProgress
    }
}
