package com.wordpress.stories.compose.text

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wordpress.stories.R
import kotlinx.android.synthetic.main.color_picker_list_item.view.*
import java.util.ArrayList

typealias OnTextColorPickerClickListener = (Int) -> Unit

class TextColorPickerAdapter internal constructor(private val context: Context, mode: Mode) :
        RecyclerView.Adapter<TextColorPickerAdapter.ViewHolder>() {
    private val colorPickerColors = getDefaultColors(context, mode)

    private var onTextColorPickerClickListener: OnTextColorPickerClickListener? = null

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.color_picker_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.isSelected = (selectedPosition == position)

        val background = holder.colorPickerActualColorViewRef.background
        if (background is GradientDrawable) {
            background.setColor(colorPickerColors[position])
        } else if (background is ColorDrawable) {
            background.color = colorPickerColors[position]
        }

        holder.colorPickerSelectionViewRef.visibility = if (holder.itemView.isSelected) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = colorPickerColors.size

    fun setOnColorPickerClickListener(onTextColorPickerClickListener: OnTextColorPickerClickListener) {
        this.onTextColorPickerClickListener = onTextColorPickerClickListener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var colorPickerActualColorViewRef: View = itemView.color_picker_view_actual_color
        var colorPickerSelectionViewRef: View = itemView.color_picker_selected_checkmark_view

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
            val colorArray = context.resources.obtainTypedArray(when (mode) {
                Mode.FOREGROUND -> R.array.text_colors
                Mode.BACKGROUND -> R.array.text_background_colors
            })

            val arrayList = ArrayList<Int>()
            for (i in 0 until colorArray.length()) {
                arrayList.add(colorArray.getColor(i, 0))
            }

            colorArray.recycle()
            return arrayList
        }
    }
}
