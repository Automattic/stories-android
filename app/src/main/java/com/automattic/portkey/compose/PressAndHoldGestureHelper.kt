package com.automattic.portkey.compose

import android.graphics.Rect
import android.os.Handler
import android.view.MotionEvent
import android.view.View

interface PressAndHoldGestureListener {
    fun onClickGesture()
    fun onHoldingGestureStart()
    fun onHoldingGestureEnd()
    fun onHoldingGestureCanceled()
}

class PressAndHoldGestureHelper(
    val initialWait: Long,
    val animateControl: Boolean = true,
    val pressAndHoldGestureListener: PressAndHoldGestureListener?
) : View.OnTouchListener {
    private var holding = false
    private var canceled = false
    private var handler = Handler()
    private val runnable = Runnable {
        // when this section runs, it means they've been holding the button
        // pressed for as long as initialWait so, safe to assume gesture is
        // they're holding it
        if (!holding) {
            holding = true
            pressAndHoldGestureListener?.onHoldingGestureStart()
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val action: Int = motionEvent.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // here start a timer, and if no CANCEL or UP event passes
                // before the timer ellapses, then trigger the HOLD action
                if (!holding) {
                    // remove any pending callback if a new ACTION_DOWN event
                    // arrives before the runnable gets run
                    handler.removeCallbacksAndMessages(null)
                } else {
                    undoAnimation(view)
                }
                startAnimation(view)
                handler.postDelayed(runnable, initialWait)
            }
            MotionEvent.ACTION_MOVE -> {
                // if finger is moving out of the bounds of the button, issue a cancel call before
                // it gets run in the handler
                if (!holding) {
                    if (!isPointWithinView(view, motionEvent.rawX.toInt(), motionEvent.rawY.toInt())) {
                        undoAnimation(view)
                        handler.removeCallbacksAndMessages(null)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (holding) {
                    holding = false
                    canceled = true
                    undoAnimation(view)
                    pressAndHoldGestureListener?.onHoldingGestureCanceled()
                }
            }
            MotionEvent.ACTION_UP -> {
                undoAnimation(view)
                if (holding) {
                    holding = false
                    if (isPointWithinView(view, motionEvent.rawX.toInt(), motionEvent.rawY.toInt())) {
                        pressAndHoldGestureListener?.onHoldingGestureEnd()
                    } else {
                        canceled = true
                        pressAndHoldGestureListener?.onHoldingGestureCanceled()
                    }
                } else {
                    if (isPointWithinView(view, motionEvent.rawX.toInt(), motionEvent.rawY.toInt())) {
                        pressAndHoldGestureListener?.onClickGesture()
                    } else {
                        canceled = true
                    }
                    // remove any pending callback if a new ACTION_UP event
                    // arrives before the runnable gets run
                    handler.removeCallbacksAndMessages(null)
                }
            }
        }
        return view.onTouchEvent(motionEvent)
    }

    private fun isPointWithinView(view: View, x: Int, y: Int): Boolean {
        val outRect = Rect(view.left, view.top, view.right, view.bottom)
        return outRect.contains(x, y)
    }

    private fun startAnimation(view: View) {
        if (animateControl) {
            view.animate().scaleXBy(0.3f).scaleYBy(0.3f).duration = initialWait
        }
    }

    private fun undoAnimation(view: View) {
        if (animateControl) {
            view.clearAnimation()
            view.animate().scaleX(1.0f).scaleY(1.0f).duration = initialWait / 4
        }
    }

    companion object {
        // time that needs to pass at first for the "hold" gesture to be detected as an actual hold
        val CLICK_LENGTH = 400L
    }
}
