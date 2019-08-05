package com.daasuu.mp4compose

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by sudamasayuki2 on 2018/01/08.
 */

class FillModeCustomItem : Parcelable {
    val scale: Float
    val rotate: Float
    val translateX: Float
    val translateY: Float
    val videoWidth: Float
    val videoHeight: Float

    constructor(
        scale: Float,
        rotate: Float,
        translateX: Float,
        translateY: Float,
        videoWidth: Float,
        videoHeight: Float
    ) {
        this.scale = scale
        this.rotate = rotate
        this.translateX = translateX
        this.translateY = translateY
        this.videoWidth = videoWidth
        this.videoHeight = videoHeight
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeFloat(this.scale)
        dest.writeFloat(this.rotate)
        dest.writeFloat(this.translateX)
        dest.writeFloat(this.translateY)
        dest.writeFloat(this.videoWidth)
        dest.writeFloat(this.videoHeight)
    }

    protected constructor(`in`: Parcel) {
        this.scale = `in`.readFloat()
        this.rotate = `in`.readFloat()
        this.translateX = `in`.readFloat()
        this.translateY = `in`.readFloat()
        this.videoWidth = `in`.readFloat()
        this.videoHeight = `in`.readFloat()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<FillModeCustomItem> = object : Parcelable.Creator<FillModeCustomItem> {
            override fun createFromParcel(source: Parcel): FillModeCustomItem {
                return FillModeCustomItem(source)
            }

            override fun newArray(size: Int): Array<FillModeCustomItem?> {
                return arrayOfNulls(size)
            }
        }
    }
}
