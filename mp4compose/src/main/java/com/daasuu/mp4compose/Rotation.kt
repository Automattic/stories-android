package com.daasuu.mp4compose

/**
 * Created by sudamasayuki on 2017/11/15.
 */

enum class Rotation private constructor(val rotation: Int) {
    NORMAL(0),
    ROTATION_90(90),
    ROTATION_180(180),
    ROTATION_270(270);

    companion object {
        fun fromInt(rotate: Int): Rotation {
            for (rotation in Rotation.values()) {
                if (rotate == rotation.rotation) return rotation
            }

            return NORMAL
        }
    }
}
