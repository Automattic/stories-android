package com.wordpress.stories.util

import android.content.Intent
import android.os.Bundle
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryRepository.DEFAULT_NONE_SELECTED

fun getStoryIndexFromIntentOrBundle(savedInstanceState: Bundle?, intent: Intent?): Int {
    var index: Int = DEFAULT_NONE_SELECTED
    // before instantiating the ViewModel, we need to get the storyIndex
    if (savedInstanceState == null) {
        intent?.let {
            index = it.getIntExtra(KEY_STORY_INDEX, DEFAULT_NONE_SELECTED)
        }
    } else {
        index = savedInstanceState.getInt(STATE_KEY_CURRENT_STORY_INDEX)
    }

    // if selection could not be obtained as KEY_STORY_INDEX intent extra or savedInstanceState,
    // see if we're getting one from KEY_STORY_SAVE_RESULT.
    if (index == DEFAULT_NONE_SELECTED) {
        val saveResult = intent?.getParcelableExtra(KEY_STORY_SAVE_RESULT) as StorySaveResult?
        saveResult?.let {
            index = it.storyIndex
        }
    }

    return index
}

const val KEY_STORY_EDIT_MODE = "key_story_edit_mode"
const val KEY_STORY_INDEX = "key_story_index"
const val KEY_STORY_SAVE_RESULT = "key_story_save_result"
const val STATE_KEY_CURRENT_STORY_INDEX = "key_current_story_index"
