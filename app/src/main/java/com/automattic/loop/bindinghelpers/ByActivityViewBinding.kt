package com.automattic.loop.bindinghelpers

import android.os.Looper
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Created by Bulent Turkmen(@faranjit) on 5.01.2021.
 *
 * https://gist.github.com/seanghay/0fd991d7a823815500557fe043b052ce#file-view-binding-ktx-kt
 */
fun <T : ViewBinding> AppCompatActivity.viewBinding(initializer: (LayoutInflater) -> T) =
    ViewBindingPropertyDelegate(this, initializer)

class ViewBindingPropertyDelegate<T : ViewBinding>(
    private val activity: AppCompatActivity,
    private val initializer: (LayoutInflater) -> T
) : ReadOnlyProperty<AppCompatActivity, T>, LifecycleObserver {
    private var binding: T? = null

    init {
        activity.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    @Suppress("Unused")
    fun onCreate() {
        if (binding == null) {
            binding = initializer(activity.layoutInflater)
        }

        activity.setContentView(binding?.root!!)
        activity.lifecycle.removeObserver(this)
    }

    override fun getValue(thisRef: AppCompatActivity, property: KProperty<*>): T {
        if (binding == null) {
            // This must be on the main thread only
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw IllegalThreadStateException(
                    "This cannot be called from other threads. It should be on the main thread only."
                )
            }

            binding = initializer(thisRef.layoutInflater)
        }
        return binding!!
    }
}
