package com.wordpress.stories.compose.text

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.wordpress.stories.R
import kotlinx.android.synthetic.main.color_picker_list_item.view.*
import java.util.ArrayList

/**
 * Created by Ahmed Adel on 5/8/17.
 */

typealias OnColorPickerClickListener = (Int) -> Unit

class ColorPickerAdapter internal constructor(private val context: Context, private val colorPickerColors: List<Int>) :
    RecyclerView.Adapter<ColorPickerAdapter.ViewHolder>() {
    private var onColorPickerClickListener: OnColorPickerClickListener? = null

    internal constructor(context: Context) : this(context, getDefaultColors(context))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.color_picker_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val background = holder.colorPickerActualColorViewRef.background
        if (background is GradientDrawable) {
            background.setColor(colorPickerColors[position])
        } else if (background is ColorDrawable) {
            background.color = colorPickerColors[position]
        }

        holder.colorPickerActualColorViewRef.background = background
    }

    override fun getItemCount(): Int {
        return colorPickerColors.size
    }

    fun setOnColorPickerClickListener(onColorPickerClickListener: OnColorPickerClickListener) {
        this.onColorPickerClickListener = onColorPickerClickListener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var colorPickerActualColorViewRef: View = itemView.color_picker_view_actual_color

        init {
            itemView.setOnClickListener {
                onColorPickerClickListener?.let {
                    it(colorPickerColors[adapterPosition])
                }
            }
        }
    }

    companion object {
        fun getDefaultColors(context: Context): List<Int> {
            return ArrayList<Int>().apply {
                add(ContextCompat.getColor(context, R.color.text_color_white))
                add(ContextCompat.getColor(context, R.color.text_color_black))
                add(ContextCompat.getColor(context, R.color.text_color_red))
                add(ContextCompat.getColor(context, R.color.text_color_orange))
                add(ContextCompat.getColor(context, R.color.text_color_yellow))
                add(ContextCompat.getColor(context, R.color.text_color_green))
                add(ContextCompat.getColor(context, R.color.text_color_blue))
                add(ContextCompat.getColor(context, R.color.text_color_violet))
                add(ContextCompat.getColor(context, R.color.text_color_magenta))
            }
        }
    }
}
