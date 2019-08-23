package com.automattic.portkey.compose.emoji

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.automattic.photoeditor.PhotoEditor
import com.automattic.portkey.R
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
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    interface EmojiListener {
        fun onEmojiClick(emojiUnicode: String)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.fragment_bottom_sticker_emoji_dialog, null)
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams

        (params.behavior as? BottomSheetBehavior)?.setBottomSheetCallback(bottomSheetBehaviorCallback)

        (contentView.parent as View).setBackgroundColor(resources.getColor(android.R.color.transparent))
        contentView.rvEmoji.layoutManager = GridLayoutManager(activity, COLUMNS)
        contentView.rvEmoji.adapter = EmojiAdapter()
    }

    fun setEmojiListener(emojiListener: EmojiListener) {
        listener = emojiListener
    }

    inner class EmojiAdapter : RecyclerView.Adapter<EmojiAdapter.ViewHolder>() {
        internal var emojisList = PhotoEditor.getEmojis(activity!!)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row_emoji, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.txtEmojiRef.text = emojisList[position]
        }

        override fun getItemCount(): Int {
            return emojisList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var txtEmojiRef: TextView

            init {
                txtEmojiRef = itemView.txtEmoji

                itemView.setOnClickListener {
                    listener?.onEmojiClick(emojisList[layoutPosition])
                    dismiss()
                }
            }
        }
    }

    companion object {
        const val COLUMNS = 6
    }
}
