package com.wordpress.stories.compose.frame

import android.os.Bundle
import android.os.Parcelable
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveError
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveSuccess
import com.wordpress.stories.compose.story.StoryIndex
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable

class StorySaveEvents {
    @Parcelize
    data class StorySaveResult(
        var storyIndex: StoryIndex = 0,
        val frameSaveResult: MutableList<FrameSaveResult> = mutableListOf(),
        val isRetry: Boolean = false,
        val isEditMode: Boolean = false,
        var elapsedTime: Long = 0,
        val metadata: Bundle? = null
    ) : Parcelable {
        fun isSuccess(): Boolean {
            return frameSaveResult.all { it.resultReason == SaveSuccess }
        }
    }
    @Parcelize
    data class FrameSaveResult(val frameIndex: FrameIndex, val resultReason: SaveResultReason) : Parcelable

    @Serializable
    sealed class SaveResultReason : Parcelable {
        @Serializable
        @Parcelize
        object SaveSuccess : SaveResultReason()

        @Serializable
        @Parcelize
        data class SaveError(
            var reason: String? = null
        ) : SaveResultReason()
    }

    data class StorySaveProcessStart(
        var storyIndex: StoryIndex
    )

    companion object {
        @JvmStatic fun allErrorsInResult(frameSaveResult: List<FrameSaveResult>): List<FrameSaveResult> {
            return frameSaveResult.filter { it.resultReason is SaveError }
        }
    }
}
