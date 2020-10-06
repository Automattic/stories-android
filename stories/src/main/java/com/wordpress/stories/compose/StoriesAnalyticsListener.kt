package com.wordpress.stories.compose

interface StoriesAnalyticsListener {
    fun trackStoryTextChanged(properties: Map<String, *>)
}
