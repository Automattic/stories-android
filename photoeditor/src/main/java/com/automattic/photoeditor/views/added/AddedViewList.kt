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
        for (oneView in this) {
            oneView.view?.let {
                if (it == element) {
                    return true
                }
            }
        }
        return false
    }
    fun removeView(element: View): AddedView? {
        for (oneView in this) {
            oneView.view?.let {
                if (it == element) {
                    this.remove(oneView)
                    return oneView
                }
            }
        }
        return null
    }
    fun indexOfView(element: View): Int {
        for (oneView in this) {
            oneView.view?.let {
                if (it == element) {
                    return this.indexOf(oneView)
                }
            }
        }
        return -1
    }

    fun containsAnyAddedViewsOfType(type: ViewType): Boolean {
        for (oneView: AddedView in this) {
            if (oneView.viewType == type) {
                return true
            }
        }
        return false
    }
}
