package com.wordpress.stories.compose.story

import com.automattic.photoeditor.views.added.AddedView
import com.automattic.photoeditor.views.added.AddedViewList
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.json.Json

fun serializeStory(story: Story): String = Json.stringify(Story.serializer(), story)

fun serializeStoryFrameItem(storyFrameItem: StoryFrameItem) =
    Json.stringify(StoryFrameItem.serializer(), storyFrameItem)

fun deserializeStory(story: String) = Json.parse(Story.serializer(), story)

fun deserializeStoryFrameItem(storyFrameItem: String) = Json.parse(StoryFrameItem.serializer(), storyFrameItem)

fun serializeAddedViews(addedViews: AddedViewList) =
    Json.stringify(ArrayListSerializer(AddedView.serializer()), addedViews)

fun serializeAddedView(addedView: AddedView) = Json.stringify(AddedView.serializer(), addedView)

fun deserializeAddedViews(addedViews: String): AddedViewList {
    val newList = AddedViewList()
    newList.addAll(Json.parse(ArrayListSerializer(AddedView.serializer()), addedViews))
    return newList
}
