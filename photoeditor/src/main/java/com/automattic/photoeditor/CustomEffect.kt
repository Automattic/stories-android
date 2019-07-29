package com.automattic.photoeditor

import android.text.TextUtils

import java.util.HashMap

/**
 * Define your custom effect using [Builder] class
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.2
 * @since 5/22/2018
 */
class CustomEffect private constructor(builder: Builder) {
    /**
     * @return Custom effect name from [android.media.effect.EffectFactory.createEffect]
     */
    val effectName: String
    /**
     * @return map of key and value of parameters for [android.media.effect.Effect.setParameter]
     */
    val parameters: Map<String, Any>

    init {
        effectName = builder.mEffectName
        parameters = builder.parametersMap
    }

    /**
     * Set customize effect to image using this builder class
     */
    class Builder
    /**
     * Initiate your custom effect
     *
     * @param effectName custom effect name from [android.media.effect.EffectFactory.createEffect]
     * @throws RuntimeException exception when effect name is empty
     */
    @Throws(RuntimeException::class)
    constructor(val mEffectName: String) {
        val parametersMap = HashMap<String, Any>()

        init {
            if (TextUtils.isEmpty(mEffectName)) {
                throw RuntimeException("Effect name cannot be empty.Please provide effect name from EffectFactory")
            }
        }

        /**
         * set parameter to the attributes with its value
         *
         * @param paramKey attribute key for [android.media.effect.Effect.setParameter]
         * @param paramValue value for [android.media.effect.Effect.setParameter]
         * @return builder instance to setup multiple parameters
         */
        fun setParameter(paramKey: String, paramValue: Any): Builder {
            parametersMap[paramKey] = paramValue
            return this
        }

        /**
         * @return instance for custom effect
         */
        fun build(): CustomEffect {
            return CustomEffect(this)
        }
    }
}
