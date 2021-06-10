package com.wordpress.stories.compose.frame

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveError
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveSuccess
import com.wordpress.stories.compose.story.StoryIndex
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

class StorySaveEvents {
    @Parcelize
    @SuppressLint("ParcelCreator")
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
    @SuppressLint("ParcelCreator")
    data class FrameSaveResult(val frameIndex: FrameIndex, val resultReason: SaveResultReason) : Parcelable

    @Serializable
    sealed class SaveResultReason : Parcelable {
        @Serializable
        @Parcelize
        @SuppressLint("ParcelCreator")
        object SaveSuccess : SaveResultReason()

        @Serializable
        @Parcelize
        @SuppressLint("ParcelCreator")
        data class SaveError(
            var reason: String? = null
        ) : SaveResultReason()
    }

    data class StorySaveProcessStart(
        var storyIndex: StoryIndex
    )

    // StoryFrameSave progress events broadcasted with EventBus
    data class FrameSaveProgress(
        val storyIndex: StoryIndex,
        val frameIndex: FrameIndex,
        val frameId: String?,
        val progress: Float
    )

    data class FrameSaveStart(
        val storyIndex: StoryIndex,
        val frameIndex: FrameIndex,
        val frameId: String?
    )

    data class FrameSaveCompleted(
        val storyIndex: StoryIndex,
        val frameIndex: FrameIndex,
        val frameId: String?
    )

    data class FrameSaveFailed(
        val storyIndex: StoryIndex,
        val frameIndex: FrameIndex,
        val frameId: String?
    )

    data class FrameSaveCanceled(
        val storyIndex: StoryIndex,
        val frameIndex: FrameIndex,
        val frameId: String?
    )

    companion object {
        @JvmStatic fun allErrorsInResult(frameSaveResult: List<FrameSaveResult>): List<FrameSaveResult> {
            return frameSaveResult.filter { it.resultReason is SaveError }
        }
    }
}
