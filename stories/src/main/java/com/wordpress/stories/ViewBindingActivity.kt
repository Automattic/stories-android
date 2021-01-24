package com.wordpress.stories

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Created by Bulent Turkmen(@faranjit) on 4.01.2021.
 *
 * This class is for Java classes that we can not use Kotlin delegates
 * such as @see ViewBindingPropertyDelegate.
 */
abstract class ViewBindingActivity<VB : ViewBinding> : AppCompatActivity() {
    protected val binding: VB by viewBinding {
        inflateBinding(it)
    }

    /**
     * Create binding object with given inflater and returns that object
     * @param layoutInflater LayoutInflater
     * @return Binding
     */
    abstract fun inflateBinding(layoutInflater: LayoutInflater): VB
}
