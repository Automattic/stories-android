package com.automattic.portkey.compose.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class StoryViewModelFactory(private val repository: StoryRepository, private val storyIndex: Int) :
    ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StoryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StoryViewModel(repository, storyIndex) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
}
