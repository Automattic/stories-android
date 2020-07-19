package com.wordpress.stories.compose.story

import android.os.Parcelable
import com.automattic.photoeditor.views.added.AddedView
import com.automattic.photoeditor.views.added.AddedViewList
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveError
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveSuccess
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

class StorySerializerUtils {
    companion object {
        private val saveResultModule = SerializersModule {
            polymorphic(Parcelable::class, SaveResultReason::class) {
                SaveSuccess::class with SaveSuccess.serializer()
                SaveError::class with SaveError.serializer()
            }
        }
        private val json = Json(context = saveResultModule)

        fun serializeStory(story: Story) = json.stringify(Story.serializer(), story)

        fun serializeStoryFrameItem(storyFrameItem: StoryFrameItem) =
            json.stringify(StoryFrameItem.serializer(), storyFrameItem)

        fun deserializeStory(story: String) = json.parse(Story.serializer(), story)

        fun deserializeStoryFrameItem(storyFrameItem: String) =
            json.parse(StoryFrameItem.serializer(), storyFrameItem)

        fun serializeAddedViews(addedViews: AddedViewList) =
            Json.stringify(ArrayListSerializer(AddedView.serializer()), addedViews)

        fun serializeAddedView(addedView: AddedView) = Json.stringify(AddedView.serializer(), addedView)

        fun deserializeAddedViews(addedViews: String): AddedViewList {
            val newList = AddedViewList()
            newList.addAll(Json.parse(ArrayListSerializer(AddedView.serializer()), addedViews))
            return newList
        }
    }
}
