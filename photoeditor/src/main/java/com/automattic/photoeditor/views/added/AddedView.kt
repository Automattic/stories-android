package com.automattic.photoeditor.views.added

import android.net.Uri
import android.view.View
import android.widget.TextView
import com.automattic.photoeditor.R
import com.automattic.photoeditor.views.ViewType
import com.automattic.photoeditor.views.ViewType.EMOJI
import com.automattic.photoeditor.views.ViewType.TEXT
import kotlinx.serialization.Transient
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
class AddedView(
    @Transient val view: View? = null,
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
            ) : AddedView {
                return AddedView(
                    view,
                    viewType,
                    buildViewInfoFromView(view,
                        getTextFromActualView(view, viewType)
                    ),
                    uri
                )
            }

            fun getTextFromActualView(view: View, viewType: ViewType): String {
                var text = ""
                when (viewType) {
                    EMOJI -> {
                        val txtView = view.findViewById<TextView>(R.id.tvPhotoEditorEmoji)
                        text = txtView.text.toString()
                    }
                    TEXT -> {
                        val txtView = view.findViewById<TextView>(R.id.tvPhotoEditorText)
                        text = txtView.text.toString()
                    }
                }
                return text
            }

        fun buildViewInfoFromView(view: View, text: String): AddedViewInfo {
            return AddedViewInfo(
                view.rotation,
                view.translationX,
                view.translationY,
                view.scaleX,
                text
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
                getTextFromActualView(view, viewType)
            )
        }
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
}
