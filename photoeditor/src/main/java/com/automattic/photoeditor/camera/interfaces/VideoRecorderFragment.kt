
package com.automattic.photoeditor.camera.interfaces

import androidx.fragment.app.Fragment
import com.automattic.photoeditor.views.background.video.AutoFitTextureView
import java.io.File

abstract class VideoRecorderFragment : Fragment(), VideoRecorderHandler, ImageCaptureHandler, SurfaceFragmentHandler {
    /**
     * An [AutoFitTextureView] for camera preview.
     */
    lateinit var textureView: AutoFitTextureView
    var currentFile: File? = null
}
