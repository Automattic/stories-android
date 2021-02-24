package com.wordpress.stories.compose.story

import android.net.Uri
import com.automattic.photoeditor.views.added.AddedView
import com.automattic.photoeditor.views.added.AddedViewList
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveSuccess
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItemType.IMAGE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import com.wordpress.stories.compose.story.StoryFrameItemType.VIDEO
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

@Serializable
data class StoryFrameItem(
    val source: BackgroundSource,
    val frameItemType: StoryFrameItemType = IMAGE,
    @Serializable(with = AddedViewListSerializer::class)
    var addedViews: AddedViewList = AddedViewList(),
    var saveResultReason: SaveResultReason = SaveSuccess,
    @Serializable(with = FileSerializer::class)
    var composedFrameFile: File? = null,
    var id: String? = null
) {

    @Serializable
    data class BackgroundViewInfo(
        val imageMatrixValues: FloatArray
    )

    @Serializable
    sealed class BackgroundSource {
        var backgroundViewInfo: BackgroundViewInfo? = null
        @Serializable
        data class UriBackgroundSource(
            @Serializable(with = UriSerializer::class)
            var contentUri: Uri? = null
        ) : BackgroundSource()

        @Serializable
        data class FileBackgroundSource(
            @Serializable(with = FileSerializer::class)
            var file: File? = null
        ) : BackgroundSource()
    }

    @Serializer(forClass = Uri::class)
    object UriSerializer : KSerializer<Uri> {
        override fun deserialize(decoder: Decoder): Uri {
            return Uri.parse(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: Uri) {
            encoder.encodeString(value.toString())
        }
    }

    @Serializer(forClass = File::class)
    object FileSerializer : KSerializer<File> {
        override fun deserialize(decoder: Decoder): File {
            return File(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: File) {
            encoder.encodeString(value.toString())
        }
    }

    @Serializer(forClass = AddedViewList::class)
    object AddedViewListSerializer : KSerializer<AddedViewList> {
        override fun deserialize(decoder: Decoder): AddedViewList {
            val newList = AddedViewList()
            newList.addAll(decoder.decodeSerializableValue(ListSerializer(AddedView.serializer())))
            return newList
        }

        override fun serialize(encoder: Encoder, value: AddedViewList) {
            encoder.encodeSerializableValue(ListSerializer(AddedView.serializer()), value)
        }
    }

    companion object {
        @JvmStatic
        fun getNewStoryFrameItemFromUri(uri: Uri, isVideo: Boolean): StoryFrameItem {
            return StoryFrameItem(
                    UriBackgroundSource(uri),
                    if (isVideo) {
                        VIDEO(false)
                    } else {
                        IMAGE
                    }
            )
        }

        @JvmStatic
        fun getAltTextFromFrameAddedViews(frame: StoryFrameItem): String {
            return frame.addedViews.joinToString(separator = " ") { it.viewInfo.addedViewTextInfo.text }
        }
    }
}
