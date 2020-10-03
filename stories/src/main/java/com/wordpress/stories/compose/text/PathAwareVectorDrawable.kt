package com.wordpress.stories.compose.text

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.util.Log
import android.util.Xml
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.lang.IllegalArgumentException
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible

/**
 * Wrapper class for VectorDrawableCompat which allows resolving and coloring individual paths in the vector.
 * This logic already exists within VectorDrawableCompat, but is not currently exposed.
 *
 * This class duplicates some of [VectorDrawableCompat]'s own creation logic, and uses reflection to access private
 * methods and colorize individual paths.
 *
 * If any of the reflection calls fail, this will gracefully fall back to the [fallbackVectorRes], or the unmodified
 * based resource if no fallback is set.
 */
class PathAwareVectorDrawable(
    context: Context,
    @DrawableRes vectorResource: Int,
    imageView: ImageView,
    @DrawableRes private val fallbackVectorRes: Int = vectorResource
) {
    private var colorableVectorDrawable: VectorDrawableCompat?

    init {
        colorableVectorDrawable = createVectorDrawableWithoutDelegate(context.resources, vectorResource).apply {
            callPrivateFunc("setAllowCaching", true)
        }

        colorableVectorDrawable?.let {
            imageView.setImageDrawable(it)
        } ?: imageView.setImageResource(fallbackVectorRes)
    }

    /**
     * Based on [VectorDrawableCompat.create], skipping the API >= 24 delegate logic to directly inflate a vector
     * drawable from an XML resource.
     */
    private fun createVectorDrawableWithoutDelegate(res: Resources, resId: Int, theme: Theme? = null):
            VectorDrawableCompat? {
        try {
            @SuppressLint("ResourceType") val parser: XmlPullParser = res.getXml(resId)
            val attrs = Xml.asAttributeSet(parser)
            var type: Int
            while (parser.next().also { type = it } != XmlPullParser.START_TAG &&
                    type != XmlPullParser.END_DOCUMENT) {
                // Empty loop
            }
            if (type != XmlPullParser.START_TAG) {
                throw XmlPullParserException("No start tag found")
            }
            return VectorDrawableCompat.createFromXmlInner(
                    res,
                    parser,
                    attrs,
                    theme
            )
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "Parser error", e)
        } catch (e: IOException) {
            Log.e(TAG, "Parser error", e)
        }
        return null
    }

    /**
     * Looks for a path in the vector drawable with the 'name' property [pathName],
     * and applies [color] as a fill color.
     *
     * The lookup and fill color methods are accessed by reflection.
     *
     * If the reflective calls fail, or no path matching [pathName] is found,
     * the drawable is silently left unchanged.
     */
    fun setPathFillColor(pathName: String, @ColorInt color: Int) {
        colorableVectorDrawable?.let { vector ->
            val targetPath = vector.callPrivateFunc("getTargetByName", pathName)
            targetPath?.let { path ->
                val fillColorFn = resolvePrivateFunction(path, "setFillColor")
                try {
                    fillColorFn?.call(targetPath, color)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "IllegalArgumentException, signature of setFillColor has changed.", e)
                }
            }
        }
    }

    private inline fun <reified T> T.callPrivateFunc(name: String, vararg args: Any?): Any? {
        return try {
            T::class
                    .declaredMemberFunctions
                    .firstOrNull { it.name == name }
                    ?.apply { isAccessible = true }
                    ?.call(this, *args)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException, signature of $name has changed.", e)
            null
        }
    }

    @Suppress("SameParameterValue")
    private fun resolvePrivateFunction(target: Any, name: String): KFunction<*>? {
        return target::class
                .declaredMemberFunctions
                .firstOrNull { it.name == name }
                ?.apply { isAccessible = true }
    }

    companion object {
        val TAG = PathAwareVectorDrawable::class.simpleName
    }
}
