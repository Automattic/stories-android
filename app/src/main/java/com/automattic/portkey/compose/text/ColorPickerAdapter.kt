package com.automattic.portkey.compose.text

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.automattic.portkey.R
import kotlinx.android.synthetic.main.color_picker_item_list.view.*

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
        val view = LayoutInflater.from(context).inflate(R.layout.color_picker_item_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.colorPickerViewRef.setBackgroundColor(colorPickerColors[position])
    }

    override fun getItemCount(): Int {
        return colorPickerColors.size
    }

    private fun buildColorPickerView(view: View, colorCode: Int) {
        view.visibility = View.VISIBLE

        val biggerCircle = ShapeDrawable(OvalShape())
        biggerCircle.intrinsicHeight = 20
        biggerCircle.intrinsicWidth = 20
        biggerCircle.bounds = Rect(0, 0, 20, 20)
        biggerCircle.paint.color = colorCode

        val smallerCircle = ShapeDrawable(OvalShape())
        smallerCircle.intrinsicHeight = 5
        smallerCircle.intrinsicWidth = 5
        smallerCircle.bounds = Rect(0, 0, 5, 5)
        smallerCircle.paint.color = Color.WHITE
        smallerCircle.setPadding(10, 10, 10, 10)
        val drawables = arrayOf<Drawable>(smallerCircle, biggerCircle)

        val layerDrawable = LayerDrawable(drawables)

        view.setBackgroundDrawable(layerDrawable)
    }

    fun setOnColorPickerClickListener(onColorPickerClickListener: OnColorPickerClickListener) {
        this.onColorPickerClickListener = onColorPickerClickListener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var colorPickerViewRef: View

        init {
            colorPickerViewRef = itemView.color_picker_view

            itemView.setOnClickListener {
                onColorPickerClickListener?.let {
                    it (colorPickerColors[adapterPosition])
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
