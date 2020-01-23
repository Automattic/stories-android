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
    sealed class BackgroundSource {
        data class UriBackgroundSource(var contentUri: Uri? = null) : BackgroundSource()
        data class FileBackgroundSource(var file: File? = null) : BackgroundSource()
    }
}
