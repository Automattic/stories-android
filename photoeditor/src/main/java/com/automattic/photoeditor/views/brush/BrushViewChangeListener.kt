package com.automattic.photoeditor.views.brush

/**
 * Created on 1/17/2018.
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 *
 *
 */

internal interface BrushViewChangeListener {
    fun onViewAdd(brushDrawingView: BrushDrawingView)

    fun onViewRemoved(brushDrawingView: BrushDrawingView)

    fun onStartDrawing()

    fun onStopDrawing()
}
