package com.wordpress.stories.compose.text

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wordpress.stories.R
import com.wordpress.stories.databinding.ColorPickerListItemBinding
import com.wordpress.stories.util.extractColorList

typealias OnTextColorPickerClickListener = (Int) -> Unit

class TextColorPickerAdapter internal constructor(private val context: Context, mode: Mode, startColor: Int? = null) :
        RecyclerView.Adapter<TextColorPickerAdapter.ViewHolder>() {
    private val colorPickerColors = getDefaultColors(context, mode)

    private var onTextColorPickerClickListener: OnTextColorPickerClickListener? = null

    private var selectedPosition = colorPickerColors.indexOf(startColor)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ColorPickerListItemBinding.inflate(LayoutInflater.from(context)))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.isSelected = (selectedPosition == position)

        if (colorPickerColors[position] == Color.TRANSPARENT) {
            holder.binding.colorPickerViewActualColor.setBackgroundResource(R.drawable.ic_gridicons_block_cropped)
            holder.binding.colorPickerSelectedCheckmarkView.visibility = View.GONE
        } else {
            holder.binding.colorPickerViewActualColor.setBackgroundResource(R.drawable.ic_color_picker_filler)
            val background = holder.binding.colorPickerSelectedCheckmarkView.background
            if (background is GradientDrawable) {
                background.setColor(colorPickerColors[position])
            } else if (background is ColorDrawable) {
                background.color = colorPickerColors[position]
            }

            holder.binding.colorPickerSelectedCheckmarkView.visibility = if (holder.itemView.isSelected) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount() = colorPickerColors.size

    fun setOnColorPickerClickListener(onTextColorPickerClickListener: OnTextColorPickerClickListener) {
        this.onTextColorPickerClickListener = onTextColorPickerClickListener
    }

    inner class ViewHolder(val binding: ColorPickerListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                notifyItemChanged(selectedPosition)
                selectedPosition = layoutPosition
                notifyItemChanged(selectedPosition)

                onTextColorPickerClickListener?.let {
                    it(colorPickerColors[adapterPosition])
                }
            }
        }
    }

    companion object {
        enum class Mode { FOREGROUND, BACKGROUND }

        fun getDefaultColors(context: Context, mode: Mode): List<Int> {
            val baseArray = context.resources.obtainTypedArray(
                    when (mode) {
                        Mode.FOREGROUND -> R.array.text_colors
                        Mode.BACKGROUND -> R.array.text_background_colors
                    }
            )

            return baseArray.extractColorList(context.resources).also { baseArray.recycle() }
        }
    }
}
