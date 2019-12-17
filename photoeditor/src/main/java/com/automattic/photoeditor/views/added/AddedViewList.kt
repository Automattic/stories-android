package com.automattic.photoeditor.views.added

import android.view.View
import java.util.ArrayList
import kotlinx.serialization.Serializable

@Serializable
class AddedViewList : ArrayList<AddedView>() {
    fun containsView(element: View): Boolean {
        for (n in this) {
            if (n.view == element) {
                return true
            }
        }
        return false
    }
    fun removeView(element: View): AddedView? {
        for (n in this) {
            if (n.view == element) {
                this.remove(n)
                return n
            }
        }
        return null
    }
    fun indexOfView(element: View): Int {
        for (n in this) {
            if (n.view == element) {
                return this.indexOf(n)
            }
        }
        return -1
    }
}
