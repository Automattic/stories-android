package com.daasuu.mp4compose.filter

import android.opengl.GLES20
import android.util.Pair

import com.daasuu.mp4compose.gl.GlFramebufferObject

import java.util.ArrayList
import java.util.Arrays

import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_FRAMEBUFFER

class GlFilterGroup(private val filters: Collection<GlFilter>?) : GlFilter() {

    private val list = ArrayList<Pair<GlFilter, GlFramebufferObject>>()

    private var prevTexName: Int = 0

    constructor(vararg glFilters: GlFilter) : this(Arrays.asList<GlFilter>(*glFilters)) {}

    override fun setup() {
        super.setup()

        if (filters != null) {
            val max = filters.size
            var count = 0

            for (shader in filters) {
                shader.setup()
                val fbo: GlFramebufferObject?
                if (count + 1 < max) {
                    fbo = GlFramebufferObject()
                } else {
                    fbo = null
                }
                list.add(Pair.create(shader, fbo))
                count++
            }
        }
    }

    override fun release() {
        for (pair in list) {
            if (pair.first != null) {
                pair.first.release()
            }
            if (pair.second != null) {
                pair.second.release()
            }
        }
        list.clear()
        super.release()
    }

    override fun setFrameSize(width: Int, height: Int) {
        super.setFrameSize(width, height)

        for (pair in list) {
            if (pair.first != null) {
                pair.first.setFrameSize(width, height)
            }
            if (pair.second != null) {
                pair.second.setup(width, height)
            }
        }
    }

    override fun draw(texName: Int, fbo: GlFramebufferObject?, presentationTime: Long) {
        prevTexName = texName
        for (pair in list) {
            if (pair.second != null) {
                if (pair.first != null) {
                    pair.second.enable()
                    GLES20.glClear(GL_COLOR_BUFFER_BIT)

                    pair.first.draw(prevTexName, pair.second, presentationTime)
                }
                prevTexName = pair.second.texName
            } else {
                if (fbo != null) {
                    fbo.enable()
                } else {
                    GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0)
                }

                if (pair.first != null) {
                    pair.first.draw(prevTexName, fbo, presentationTime)
                }
            }
        }
    }
}
