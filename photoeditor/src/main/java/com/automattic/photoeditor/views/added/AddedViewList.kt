package com.automattic.photoeditor.views.added

import android.view.View
import com.automattic.photoeditor.views.ViewType
import java.util.ArrayList
import kotlinx.serialization.Serializable

@Serializable
class AddedViewList : ArrayList<AddedView>() {
    fun copyOf(addedViewList: AddedViewList): AddedViewList {
        addedViewList.let {
            addAll(it)
        }
        return this
    }

    fun containsView(element: View): Boolean {
        for (n in this) {
            n.view?.let{
                if (it == element) {
                    return true
                }
            }
        }
        return false
    }
    fun removeView(element: View): AddedView? {
        for (n in this) {
            n.view?.let {
                if (it == element) {
                    this.remove(n)
                    return n
                }
            }
        }
        return null
    }
    fun indexOfView(element: View): Int {
        for (n in this) {
            n.view?.let {
                if (it == element) {
                    return this.indexOf(n)
                }
            }
        }
        return -1
    }

    fun containsAnyAddedViewsOfType(type: ViewType): Boolean {
        for (v: AddedView in this) {
            if (v.viewType == type) {
                return true
            }
        }
        return false
    }
}
