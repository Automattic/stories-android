package com.automattic.portkey.compose.story

import android.net.Uri
import com.automattic.photoeditor.views.added.AddedViewList
import com.automattic.portkey.compose.story.StoryFrameItemType.IMAGE
import java.io.File

data class StoryFrameItem(
    val source: BackgroundSource,
    val frameItemType: StoryFrameItemType = IMAGE,
    var addedViews: AddedViewList = AddedViewList()
) {
    class BackgroundSource {
        var file: File? = null
        var contentUri: Uri? = null

        constructor (file: File) {
            this.file = file
        }
        constructor(contentUri: Uri) {
            this.contentUri = contentUri
        }

        fun isUri(): Boolean {
            return contentUri != null
        }

        fun isFile(): Boolean {
            return file != null
        }

        fun isDefault(): Boolean {
            return isFile() && file == DEFAULT_SOURCE
        }

        companion object {
            private val DEFAULT_SOURCE = File("")
            fun getDefault(): BackgroundSource {
                return BackgroundSource(DEFAULT_SOURCE)
            }
        }
    }
}
