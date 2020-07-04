package com.wordpress.stories.compose.story

import com.automattic.photoeditor.views.added.AddedView
import com.automattic.photoeditor.views.added.AddedViewList
import kotlinx.serialization.json.Json

fun serializeStory(story: Story): String {
    return Json.stringify(Story.serializer(), story)
}

fun serializeStoryFrameItem(storyFrameItem: StoryFrameItem): String {
    return Json.stringify(StoryFrameItem.serializer(), storyFrameItem)
}

fun deserializeStory(story: String): Story {
    return Json.parse(Story.serializer(), story)
}

fun deserializeStoryFrameItem(storyFrameItem: String): StoryFrameItem {
    return Json.parse(StoryFrameItem.serializer(), storyFrameItem)
}

fun serializeAddedViews(addedViews: AddedViewList): String {
    return Json.stringify(AddedViewList.serializer(), addedViews)
}

fun serializeAddedViewTest(addedView: AddedView): String {
    return Json.stringify(AddedView.serializer(), addedView)
}

fun deserializeAddedViews(addedViews: String): AddedViewList {
    return Json.parse(AddedViewList.serializer(), addedViews)
}
