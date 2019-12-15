package com.automattic.portkey.compose.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class StoryViewModelFactory(private val repository: StoryRepository, private val storyIndex: Int) :
    ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StoryViewModel(repository, storyIndex) as T
        }
}
