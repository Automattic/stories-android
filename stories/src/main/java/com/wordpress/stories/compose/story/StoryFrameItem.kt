package com.wordpress.stories.compose.story

import android.net.Uri
import com.automattic.photoeditor.views.added.AddedViewList
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveSuccess
import com.wordpress.stories.compose.story.StoryFrameItemType.IMAGE
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Transient
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.io.File

@Serializable
data class StoryFrameItem(
    val source: BackgroundSource,
    val frameItemType: StoryFrameItemType = IMAGE,
    var addedViews: AddedViewList = AddedViewList(),
    var saveResultReason: SaveResultReason = SaveSuccess,
    @Transient
    var composedFrameFile: File? = null
) {
    @Serializable
    sealed class BackgroundSource {
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
        override fun deserialize(input: Decoder): Uri {
            return Uri.parse(input.decodeString())
        }

        override fun serialize(output: Encoder, obj: Uri) {
            output.encodeString(obj.toString())
        }
    }

    @Serializer(forClass = File::class)
    object FileSerializer : KSerializer<File> {
        override fun deserialize(input: Decoder): File {
            return File(input.decodeString())
        }

        override fun serialize(output: Encoder, obj: File) {
            output.encodeString(obj.toString())
        }
    }
}
