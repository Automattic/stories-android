package com.daasuu.mp4compose.composer

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import android.util.Size
import android.view.Surface

import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.FillModeCustomItem
import com.daasuu.mp4compose.Rotation
import com.daasuu.mp4compose.filter.GlFilter
import com.daasuu.mp4compose.gl.GlFramebufferObject
import com.daasuu.mp4compose.gl.GlPreviewFilter
import com.daasuu.mp4compose.gl.GlSurfaceTexture
import com.daasuu.mp4compose.utils.EglUtil

import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_FRAMEBUFFER
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_MAX_TEXTURE_SIZE
import android.opengl.GLES20.GL_NEAREST
import android.opengl.GLES20.GL_TEXTURE_2D

// Refer : https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/OutputSurface.java

/**
 * Holds state associated with a Surface used for MediaCodec decoder output.
 *
 *
 * The (width,height) constructor for this class will prepare GL, create a SurfaceTexture,
 * and then create a Surface for that SurfaceTexture.  The Surface can be passed to
 * MediaCodec.configure() to receive decoder output.  When a frame arrives, we latch the
 * texture with updateTexImage, then render the texture with GL to a pbuffer.
 *
 *
 * The no-arg constructor skips the GL preparation step and doesn't allocate a pbuffer.
 * Instead, it just creates the Surface and SurfaceTexture, and when a frame arrives
 * we just draw it on whatever surface is current.
 *
 *
 * By default, the Surface will be using a BufferQueue in asynchronous mode, so we
 * can potentially drop frames.
 */
internal class DecoderSurface
/**
 * Creates an DecoderSurface using the current EGL context (rather than establishing a
 * new one).  Creates a Surface that can be passed to MediaCodec.configure().
 */
    (private var filter: GlFilter?) : SurfaceTexture.OnFrameAvailableListener {
    private var eglDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface = EGL14.EGL_NO_SURFACE
    /**
     * Returns the Surface that we draw onto.
     */
    var surface: Surface? = null
        private set
    private val frameSyncObject = Object() // guards frameAvailable
    private var frameAvailable: Boolean = false

    private var texName: Int = 0

    private var previewTexture: GlSurfaceTexture? = null

    private var filterFramebufferObject: GlFramebufferObject? = null
    private var previewShader: GlPreviewFilter? = null
    private var normalShader: GlFilter? = null
    private var framebufferObject: GlFramebufferObject? = null

    private val MVPMatrix = FloatArray(16)
    private val ProjMatrix = FloatArray(16)
    private val MMatrix = FloatArray(16)
    private val VMatrix = FloatArray(16)
    private val STMatrix = FloatArray(16)

    private var rotation = Rotation.NORMAL
    private var outputResolution: Size? = null
    private var inputResolution: Size? = null
    private var fillMode = FillMode.PRESERVE_ASPECT_FIT
    private var fillModeCustomItem: FillModeCustomItem? = null
    private var flipVertical = false
    private var flipHorizontal = false

    init {
        setup()
    }

    /**
     * Creates instances of TextureRender and SurfaceTexture, and a Surface associated
     * with the SurfaceTexture.
     */
    private fun setup() {
        // Even if we don't access the SurfaceTexture after the constructor returns, we
        // still need to keep a reference to it.  The Surface doesn't retain a reference
        // at the Java level, so if we don't either then the object can get GCed, which
        // causes the native finalizer to run.

        // if (VERBOSE) Log.d(TAG, "textureID=" + filter.getTextureId());
        // surfaceTexture = new SurfaceTexture(filter.getTextureId());

        // This doesn't work if DecoderSurface is created on the thread that CTS started for
        // these test cases.
        //
        // The CTS-created thread has a Looper, and the SurfaceTexture constructor will
        // create a Handler that uses it.  The "frame available" message is delivered
        // there, but since we're not a Looper-based thread we'll never see it.  For
        // this to do anything useful, DecoderSurface must be created on a thread without
        // a Looper, so that SurfaceTexture uses the main application Looper instead.
        //
        // Java language note: passing "this" out of a constructor is generally unwise,
        // but we should be able to get away with it here.

        filter!!.setup()
        framebufferObject = GlFramebufferObject()
        normalShader = GlFilter()
        normalShader!!.setup()

        val args = IntArray(1)

        GLES20.glGenTextures(args.size, args, 0)
        texName = args[0]

        // SurfaceTextureを生成
        previewTexture = GlSurfaceTexture(texName)
        previewTexture!!.setOnFrameAvailableListener(this)
        // SupressWarnings explanation:
        // Many resources, such as TypedArrays, VelocityTrackers, etc., should be recycled (with a recycle() call) after
        // use. This lint check looks for missing recycle() calls.
        // Note from editor: it is being released in fun release() so, should be OK.
        @SuppressWarnings("Recycle")
        surface = Surface(previewTexture!!.surfaceTexture)

        GLES20.glBindTexture(previewTexture!!.textureTarget, texName)
        // GL_TEXTURE_EXTERNAL_OES
        // OpenGlUtils.setupSampler(previewTexture.getTextureTarget(), GL_LINEAR, GL_NEAREST);
        EglUtil.setupSampler(previewTexture!!.textureTarget, GL_LINEAR, GL_NEAREST)

        GLES20.glBindTexture(GL_TEXTURE_2D, 0)

        // GL_TEXTURE_EXTERNAL_OES
        previewShader = GlPreviewFilter(previewTexture!!.textureTarget)
        previewShader!!.setup()
        filterFramebufferObject = GlFramebufferObject()

        Matrix.setLookAtM(
            VMatrix, 0,
            0.0f, 0.0f, 5.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f
        )

        GLES20.glGetIntegerv(GL_MAX_TEXTURE_SIZE, args, 0)
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    fun release() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        surface!!.release()
        previewTexture!!.release()
        // this causes a bunch of warnings that appear harmless but might confuse someone:
        //  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        // surfaceTexture.release();
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
        filter!!.release()
        filter = null
        surface = null
        previewTexture = null
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the DecoderSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    fun awaitNewImage() {
        val TIMEOUT_MS = 10000
        synchronized(frameSyncObject) {
            while (!frameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    frameSyncObject.wait(TIMEOUT_MS.toLong())
                    if (!frameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw RuntimeException("Surface frame wait timed out")
                    }
                } catch (ie: InterruptedException) {
                    // shouldn't happen
                    throw RuntimeException(ie)
                }
            }
            frameAvailable = false
        }
        // Latch the data.
        //  EglUtil.checkGlError("before updateTexImage");
        previewTexture!!.updateTexImage()
        previewTexture!!.getTransformMatrix(STMatrix)
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    fun drawImage(presentationTime: Long) {
        framebufferObject!!.enable()
        GLES20.glViewport(0, 0, framebufferObject!!.width, framebufferObject!!.height)

        if (filter != null) {
            filterFramebufferObject!!.enable()
            GLES20.glViewport(0, 0, filterFramebufferObject!!.width, filterFramebufferObject!!.height)
            GLES20.glClearColor(
                filter!!.clearColor[0],
                filter!!.clearColor[1],
                filter!!.clearColor[2],
                filter!!.clearColor[3]
            )
        }

        GLES20.glClear(GL_COLOR_BUFFER_BIT)

        Matrix.multiplyMM(MVPMatrix, 0, VMatrix, 0, MMatrix, 0)
        Matrix.multiplyMM(MVPMatrix, 0, ProjMatrix, 0, MVPMatrix, 0)

        val scaleDirectionX = (if (flipHorizontal) -1 else 1).toFloat()
        val scaleDirectionY = (if (flipVertical) -1 else 1).toFloat()

        val scale: FloatArray
        when (fillMode) {
            FillMode.PRESERVE_ASPECT_FIT -> {
                scale = FillMode.getScaleAspectFit(
                    rotation.rotation,
                    inputResolution!!.width,
                    inputResolution!!.height,
                    outputResolution!!.width,
                    outputResolution!!.height
                )

                // Log.d(TAG, "scale[0] = " + scale[0] + " scale[1] = " + scale[1]);

                Matrix.scaleM(MVPMatrix, 0, scale[0] * scaleDirectionX, scale[1] * scaleDirectionY, 1f)
                if (rotation != Rotation.NORMAL) {
                    Matrix.rotateM(MVPMatrix, 0, (-rotation.rotation).toFloat(), 0f, 0f, 1f)
                }
            }
            FillMode.PRESERVE_ASPECT_CROP -> {
                scale = FillMode.getScaleAspectCrop(
                    rotation.rotation,
                    inputResolution!!.width,
                    inputResolution!!.height,
                    outputResolution!!.width,
                    outputResolution!!.height
                )
                Matrix.scaleM(MVPMatrix, 0, scale[0] * scaleDirectionX, scale[1] * scaleDirectionY, 1f)
                if (rotation != Rotation.NORMAL) {
                    Matrix.rotateM(MVPMatrix, 0, (-rotation.rotation).toFloat(), 0f, 0f, 1f)
                }
            }
            FillMode.CUSTOM -> if (fillModeCustomItem != null) {
                Matrix.translateM(MVPMatrix, 0, fillModeCustomItem!!.translateX, -fillModeCustomItem!!.translateY, 0f)
                scale = FillMode.getScaleAspectCrop(
                    rotation.rotation,
                    inputResolution!!.width,
                    inputResolution!!.height,
                    outputResolution!!.width,
                    outputResolution!!.height
                )

                if (fillModeCustomItem!!.rotate == 0f || fillModeCustomItem!!.rotate == 180f) {
                    Matrix.scaleM(
                        MVPMatrix,
                        0,
                        fillModeCustomItem!!.scale * scale[0] * scaleDirectionX,
                        fillModeCustomItem!!.scale * scale[1] * scaleDirectionY,
                        1f
                    )
                } else {
                    Matrix.scaleM(
                        MVPMatrix,
                        0,
                        fillModeCustomItem!!.scale * scale[0] *
                                (1 / fillModeCustomItem!!.videoWidth * fillModeCustomItem!!.videoHeight) *
                                scaleDirectionX,
                        fillModeCustomItem!!.scale * scale[1] *
                                (fillModeCustomItem!!.videoWidth / fillModeCustomItem!!.videoHeight) *
                                scaleDirectionY,
                        1f
                    )
                }

                Matrix.rotateM(MVPMatrix, 0, -(rotation.rotation + fillModeCustomItem!!.rotate), 0f, 0f, 1f)

                //                    Log.d(TAG, "inputResolution = " + inputResolution.getWidth() + " height = " + inputResolution.getHeight());
                //                    Log.d(TAG, "out = " + outputResolution.getWidth() + " height = " + outputResolution.getHeight());
                //                    Log.d(TAG, "rotation = " + rotation.getRotation());
                //                    Log.d(TAG, "scale[0] = " + scale[0] + " scale[1] = " + scale[1]);
            }
            else -> {
            }
        }

        previewShader!!.draw(texName, MVPMatrix, STMatrix, 1f)

        if (filter != null) {
            // 一度shaderに描画したものを、fboを利用して、drawする。drawには必要なさげだけど。
            framebufferObject!!.enable()
            GLES20.glClear(GL_COLOR_BUFFER_BIT)
            filter!!.draw(filterFramebufferObject!!.texName, framebufferObject!!, presentationTime)
        }

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, framebufferObject!!.width, framebufferObject!!.height)

        GLES20.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        normalShader!!.draw(framebufferObject!!.texName, null, presentationTime)
    }

    override fun onFrameAvailable(st: SurfaceTexture) {
        if (VERBOSE) Log.d(TAG, "new frame available")
        synchronized(frameSyncObject) {
            if (frameAvailable) {
                throw RuntimeException("frameAvailable already set, frame could be dropped")
            }
            frameAvailable = true
            frameSyncObject.notifyAll()
        }
    }

    fun setRotation(rotation: Rotation) {
        this.rotation = rotation
    }

    fun setOutputResolution(resolution: Size) {
        this.outputResolution = resolution
    }

    fun setFillMode(fillMode: FillMode) {
        this.fillMode = fillMode
    }

    fun setInputResolution(resolution: Size) {
        this.inputResolution = resolution
    }

    fun setFillModeCustomItem(fillModeCustomItem: FillModeCustomItem?) {
        this.fillModeCustomItem = fillModeCustomItem
    }

    fun setFlipVertical(flipVertical: Boolean) {
        this.flipVertical = flipVertical
    }

    fun setFlipHorizontal(flipHorizontal: Boolean) {
        this.flipHorizontal = flipHorizontal
    }

    fun completeParams() {
        val width = outputResolution!!.width
        val height = outputResolution!!.height
        framebufferObject!!.setup(width, height)
        normalShader!!.setFrameSize(width, height)

        filterFramebufferObject!!.setup(width, height)
        previewShader!!.setFrameSize(width, height)
        // MCLog.d("onSurfaceChanged width = " + width + " height = " + height + " aspectRatio = " + scaleRatio);
        Matrix.frustumM(ProjMatrix, 0, -1f, 1f, -1f, 1f, 5f, 7f)
        Matrix.setIdentityM(MMatrix, 0)

        if (filter != null) {
            filter!!.setFrameSize(width, height)
        }
    }

    companion object {
        private val TAG = "DecoderSurface"
        private val VERBOSE = false
    }
}
