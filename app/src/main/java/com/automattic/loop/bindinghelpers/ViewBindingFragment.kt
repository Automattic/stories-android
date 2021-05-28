package com.automattic.loop.bindinghelpers

import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Created by Bulent Turkmen(@faranjit) on 5.01.2021.
 *
 * This class is for Java classes that we can not use Kotlin delegates
 * such as @see FragmentViewBindingDelegate.
 */
abstract class ViewBindingFragment<VB : ViewBinding> : Fragment() {
    protected val binding by viewBinding {
        inflateBinding(it)
    }

    /**
     * Create binding object with given inflater and returns that object.
     * Note that {@link androidx.fragment.app.Fragment.#onCreateView(LayoutInflater, ViewGroup, Bundle)} must be called
     * before calling binding.
     *
     * @param view Root view of fragment
     * @return Binding
     */
    abstract fun inflateBinding(view: View): VB
}
