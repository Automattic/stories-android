package com.wordpress.stories.compose.story

import com.automattic.photoeditor.views.added.AddedView
import com.automattic.photoeditor.views.added.AddedViewList
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

class StorySerializerUtils {
    companion object {
        private val saveResultModule = SerializersModule {
            SaveResultReason.serializer()
        }
        private val json = Json {
            this.serializersModule = saveResultModule
        }

        fun serializeStory(story: Story) = json.encodeToString(Story.serializer(), story)

        fun serializeStoryFrameItem(storyFrameItem: StoryFrameItem) =
                json.encodeToString(StoryFrameItem.serializer(), storyFrameItem)

        fun deserializeStory(story: String) = json.decodeFromString(Story.serializer(), story)

        fun deserializeStoryFrameItem(storyFrameItem: String) =
                json.decodeFromString(StoryFrameItem.serializer(), storyFrameItem)

        fun serializeAddedViews(addedViews: AddedViewList) =
                Json.encodeToString(ListSerializer(AddedView.serializer()), addedViews)

        fun serializeAddedView(addedView: AddedView) = Json.encodeToString(AddedView.serializer(), addedView)

        fun deserializeAddedViews(addedViews: String): AddedViewList {
            val newList = AddedViewList()
            newList.addAll(Json.decodeFromString(ListSerializer(AddedView.serializer()), addedViews))
            return newList
        }
    }
}
