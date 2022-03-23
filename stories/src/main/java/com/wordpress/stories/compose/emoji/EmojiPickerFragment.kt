package com.wordpress.stories.compose.emoji

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.emoji.text.EmojiCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.automattic.photoeditor.PhotoEditor
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wordpress.stories.R
import com.wordpress.stories.compose.hideStatusBar
import com.wordpress.stories.databinding.FragmentBottomStickerEmojiDialogBinding
import com.wordpress.stories.databinding.RowEmojiBinding
import com.wordpress.stories.util.getDisplayPixelWidth

class EmojiPickerFragment : BottomSheetDialogFragment() {
    private var listener: EmojiListener? = null
    private var emojiViewWidth: Int? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.EmojiBottomSheetDialogTheme)
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.let { hideStatusBar(it) }
        context?.let {
            emojiViewWidth = getEmojiViewWidthForScreenWidthAndColumns(getDisplayPixelWidth(it), COLUMNS)
        }
    }

    private fun getEmojiViewWidthForScreenWidthAndColumns(screenWidth: Int, columns: Int): Int {
        // let's calculate the emoji view width here, given the dialog may be displayed while the user goes
        // out to change the fontSize manually in Settings -> Accessibility
        val itemPadding = resources.getDimension(R.dimen.emoji_picker_item_padding) / resources.displayMetrics.density
        val itemMargin = resources.getDimension(R.dimen.emoji_picker_item_margin) / resources.displayMetrics.density
        val startEndMargin =
            resources.getDimension(R.dimen.emoji_picker_whole_list_side_margin) / resources.displayMetrics.density

        val wholeListTakenSpace = (itemPadding * 2) + (itemMargin * 2) + (startEndMargin * 2)

        val remainingScreenOperatingSpace = screenWidth - wholeListTakenSpace
        val maxEmojiWidth = remainingScreenOperatingSpace / columns

        return maxEmojiWidth.toInt()
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        activity?.let { activity ->
            val binding = FragmentBottomStickerEmojiDialogBinding.inflate(LayoutInflater.from(context))
//            val contentView = View.inflate(context, R.layout.fragment_bottom_sticker_emoji_dialog, null)
            dialog.setContentView(binding.root)
            val params = binding.root.layoutParams as CoordinatorLayout.LayoutParams

            (params.behavior as? BottomSheetBehavior)?.setBottomSheetCallback(bottomSheetBehaviorCallback)
            (params.behavior as? BottomSheetBehavior)?.state = BottomSheetBehavior.STATE_EXPANDED

            binding.rvEmoji.layoutManager = GridLayoutManager(activity, COLUMNS)
            binding.rvEmoji.adapter = EmojiAdapter(PhotoEditor.getEmojis(activity))
        }
    }

    fun setEmojiListener(emojiListener: EmojiListener) {
        listener = emojiListener
    }

    inner class EmojiAdapter(internal val emojiList: List<String>) : RecyclerView.Adapter<EmojiAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row_emoji, parent, false)
            emojiViewWidth?.let {
                val params = view.layoutParams
                // Changes the height and width to the specified dp
                params.height = it
                params.width = it
                view.layoutParams = params
            }
            return ViewHolder(RowEmojiBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        @SuppressLint("RecyclerView")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // use EmojiCompat to process the string and make sure we have an emoji that can be rendered
            EmojiCompat.get().registerInitCallback(object : EmojiCompat.InitCallback() {
                override fun onInitialized() {
                    EmojiCompat.get().unregisterInitCallback(this)

                    val compat = EmojiCompat.get()
                    holder.binding.txtEmoji.text = compat.process(emojiList[position])
                }

                override fun onFailed(throwable: Throwable?) {
                    EmojiCompat.get().unregisterInitCallback(this)

                    // just fallback to setting the text
                    holder.binding.txtEmoji.text = emojiList[position]
                }
            })
        }

        override fun getItemCount(): Int {
            return emojiList.size
        }

        inner class ViewHolder(val binding: RowEmojiBinding) : RecyclerView.ViewHolder(binding.root) {
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
