package com.automattic.photoeditor.gesture

import android.view.MotionEvent
import android.view.View

class RotationGestureDetector {
    private var fX: Float = 0.toFloat()
    private var fY: Float = 0.toFloat()
    private var sX: Float = 0.toFloat()
    private var sY: Float = 0.toFloat()
    private var ptrID1: Int = 0
    private var ptrID2: Int = 0
    var angle: Float = 0.toFloat()
        private set

    private var currentSpanVector = Vector2D()
    private var prevSpanVector = Vector2D()

    init {
        ptrID1 = INVALID_POINTER_ID
        ptrID2 = INVALID_POINTER_ID
    }

    fun onTouchEvent(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> ptrID1 = event.getPointerId(event.actionIndex)
            MotionEvent.ACTION_POINTER_DOWN -> {
                ptrID2 = event.getPointerId(event.actionIndex)
                sX = event.getX(event.findPointerIndex(ptrID1))
                sY = event.getY(event.findPointerIndex(ptrID1))
                fX = event.getX(event.findPointerIndex(ptrID2))
                fY = event.getY(event.findPointerIndex(ptrID2))

                val pvx = fX - sX
                val pvy = fY - sY

                prevSpanVector.set(pvx, pvy)
            }
            MotionEvent.ACTION_MOVE -> if (ptrID1 != INVALID_POINTER_ID && ptrID2 != INVALID_POINTER_ID) {
                val nfX: Float
                val nfY: Float
                val nsX: Float
                val nsY: Float
                nsX = event.getX(event.findPointerIndex(ptrID1))
                nsY = event.getY(event.findPointerIndex(ptrID1))
                nfX = event.getX(event.findPointerIndex(ptrID2))
                nfY = event.getY(event.findPointerIndex(ptrID2))

                val cvx = nfX - nsX
                val cvy = nfY - nsY

                currentSpanVector.set(cvx, cvy)

                angle = Vector2D.getAngle(
                    prevSpanVector,
                    currentSpanVector
                )

                // set calculated rotation on view
                view.rotation += angle
            }
            MotionEvent.ACTION_UP -> ptrID1 = INVALID_POINTER_ID
            MotionEvent.ACTION_POINTER_UP -> ptrID2 = INVALID_POINTER_ID
            MotionEvent.ACTION_CANCEL -> {
                ptrID1 = INVALID_POINTER_ID
                ptrID2 = INVALID_POINTER_ID
            }
        }
        return true
    }

    companion object {
        private val INVALID_POINTER_ID = -1
    }
}
