package com.automattic.photoeditor.views.added

import android.net.Uri
import android.view.View
import android.widget.TextView
import com.automattic.photoeditor.R
import com.automattic.photoeditor.views.ViewType
import com.automattic.photoeditor.views.ViewType.EMOJI
import com.automattic.photoeditor.views.ViewType.TEXT
import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.withName

@Serializable
class AddedView(
    @Transient val view: View?,
    val viewType: ViewType,
    var viewInfo: AddedViewInfo,
    @Serializable(with = UriSerializer::class)
    val uri: Uri? = null
) {
    @Serializer(forClass = AddedView::class)
    companion object : KSerializer<AddedView> {
        override val descriptor: SerialDescriptor = StringDescriptor.withName("AddedView")

        override fun serialize(encoder: Encoder, obj: AddedView) {
            val compositeOutput = encoder.beginStructure(descriptor)
            compositeOutput.encodeSerializableElement(descriptor, 0, ViewType.serializer(), obj.viewType)
            compositeOutput.encodeSerializableElement(descriptor, 1, AddedViewInfo.serializer(), obj.viewInfo)
            obj.uri?.let {
                compositeOutput.encodeSerializableElement(descriptor, 2, UriSerializer, it)
            }
            compositeOutput.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): AddedView {
            var viewType: ViewType? = null
            var viewInfo: AddedViewInfo? = null
            var uri: Uri? = null

            decoder.beginStructure(descriptor).run {
                loop@while (true) {
                    when (val i = decodeElementIndex(descriptor)) {
                        CompositeDecoder.READ_DONE -> break@loop
                        0 -> viewType = decodeSerializableElement(descriptor, i, ViewType.serializer())
                        1 -> viewInfo = decodeSerializableElement(descriptor, i, AddedViewInfo.serializer())
                        2 -> uri = decodeSerializableElement(descriptor, i, UriSerializer)
                        else -> throw SerializationException("Unknown index $i")
                    }
                }
                endStructure(descriptor)
            }
            return AddedView(
                null,  // this will be initialized later when inflated and added to a ViewGroup
                viewType ?: throw MissingFieldException("viewType"),
                viewInfo ?: throw MissingFieldException("viewInfo"),
                uri
            )
        }

        fun buildAddedViewFromView(
            view: View,
            viewType: ViewType,
            uri: Uri? = null
        ) : AddedView {
            return AddedView(view, viewType, buildViewInfoFromView(view, getTextFromActualView(view, viewType)), uri)
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
