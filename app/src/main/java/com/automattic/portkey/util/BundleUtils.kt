package com.automattic.portkey.util

import android.content.Intent
import android.os.Bundle
import com.automattic.portkey.compose.story.StoryRepository.DEFAULT_NONE_SELECTED

fun getStoryIndexFromIntentOrBundle(savedInstanceState: Bundle?, intent: Intent?): Int {
    var index: Int = DEFAULT_INDEX
    // before instantiating the ViewModel, we need to get the storyIndex
    if (savedInstanceState == null) {
        intent?.let {
            index = it.getIntExtra(KEY_STORY_INDEX, DEFAULT_NONE_SELECTED)
        }
    } else {
        index = savedInstanceState.getInt(STATE_KEY_CURRENT_STORY_INDEX)
    }
    return index
}

const val DEFAULT_INDEX = -1
const val KEY_STORY_INDEX = "key_story_index"
const val STATE_KEY_CURRENT_STORY_INDEX = "key_current_story_index"
