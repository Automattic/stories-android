package com.automattic.photoeditor.views.added

import android.net.Uri
import android.view.View
import android.widget.TextView
import com.automattic.photoeditor.R
import com.automattic.photoeditor.views.ViewType
import com.automattic.photoeditor.views.ViewType.EMOJI
import com.automattic.photoeditor.views.ViewType.TEXT
import kotlinx.serialization.Transient
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
class AddedView(
    @Transient var view: View? = null,
    val viewType: ViewType,
    var viewInfo: AddedViewInfo,
    @Serializable(with = UriSerializer::class)
    val uri: Uri? = null
) {
    companion object {
        fun buildAddedViewFromView(
            view: View,
            viewType: ViewType,
            uri: Uri? = null
        ): AddedView {
            return AddedView(
                view,
                viewType,
                buildViewInfoFromView(
                    view,
                    getTextInfoFromActualView(view, viewType)
                ),
                uri
            )
        }

        fun getTextInfoFromActualView(view: View, viewType: ViewType): AddedViewTextInfo {
            val txtView: TextView = when (viewType) {
                EMOJI -> {
                    view.findViewById(R.id.tvPhotoEditorEmoji)
                }
                TEXT -> {
                    view.findViewById(R.id.tvPhotoEditorText)
                }
                else -> {
                    // default text
                    view.findViewById(R.id.tvPhotoEditorText)
                }
            }
            return AddedViewTextInfo.fromTextView(txtView)
        }

        fun buildViewInfoFromView(view: View, addedViewText: AddedViewTextInfo): AddedViewInfo {
            return AddedViewInfo(
                view.rotation,
                view.translationX,
                view.translationY,
                view.scaleX,
                addedViewText
            )
        }
    }

    fun update() {
        view?.let {
            viewInfo = AddedViewInfo(
                it.rotation,
                it.translationX,
                it.translationY,
                it.scaleX,
                getTextInfoFromActualView(it, viewType)
            )
        }
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
}
