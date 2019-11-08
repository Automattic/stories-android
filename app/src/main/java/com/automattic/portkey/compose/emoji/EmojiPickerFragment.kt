package com.automattic.portkey.compose.emoji

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.emoji.text.EmojiCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.automattic.photoeditor.PhotoEditor
import com.automattic.portkey.R
import com.automattic.portkey.compose.hideStatusBar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_bottom_sticker_emoji_dialog.view.*
import kotlinx.android.synthetic.main.row_emoji.view.*

class EmojiPickerFragment : BottomSheetDialogFragment() {
    private var listener: EmojiListener? = null

    private val bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            // Tweak to make swipe down fully dismiss the view because of this issue:
            // Swiping down on the sheet, the stickers get stuck half way
            // down and then you have to swipe again to fully dismiss the view.
            if (slideOffset < 0.5f) {
                dismiss()
            }
        }
    }

    interface EmojiListener {
        fun onEmojiClick(emojiUnicode: String)
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.let { hideStatusBar(it) }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        activity?.let { activity ->
            val contentView = View.inflate(context, R.layout.fragment_bottom_sticker_emoji_dialog, null)
            dialog.setContentView(contentView)
            val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams

            (params.behavior as? BottomSheetBehavior)?.setBottomSheetCallback(bottomSheetBehaviorCallback)
            (params.behavior as? BottomSheetBehavior)?.state = BottomSheetBehavior.STATE_EXPANDED

            (contentView.parent as View).setBackgroundColor(
                ContextCompat.getColor(activity, android.R.color.transparent))
            contentView.rvEmoji.layoutManager = GridLayoutManager(activity, COLUMNS)
            contentView.rvEmoji.adapter = EmojiAdapter(PhotoEditor.getEmojis(activity))
        }
    }

    fun setEmojiListener(emojiListener: EmojiListener) {
        listener = emojiListener
    }

    inner class EmojiAdapter(internal val emojiList: List<String>) : RecyclerView.Adapter<EmojiAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row_emoji, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // holder.txtEmojiRef.text = emojiList[position]
            // use EmojiCompat to process the string and make sure we have an emoji that can be rendered
            EmojiCompat.get().registerInitCallback(object : EmojiCompat.InitCallback() {
                override fun onInitialized() {
                    EmojiCompat.get().unregisterInitCallback(this)

                    val regularTextView = holder.txtEmojiRef
                    if (regularTextView != null) {
                        val compat = EmojiCompat.get()
                        regularTextView.text = compat.process(emojiList[position])
                    }
                }

                override fun onFailed(throwable: Throwable?) {
                    EmojiCompat.get().unregisterInitCallback(this)

                    // just fallback to setting the text
                    holder.txtEmojiRef.text = emojiList[position]
                }
            })
        }

        override fun getItemCount(): Int {
            return emojiList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var txtEmojiRef: TextView = itemView.txtEmoji

            init {
                itemView.setOnClickListener {
                    listener?.onEmojiClick(emojiList[layoutPosition])
                    dismiss()
                }
            }
        }
    }

    companion object {
        const val COLUMNS = 6
    }
}
