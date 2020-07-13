package com.wordpress.stories.compose.frame

class FrameSaveTimeTracker {
    private var startTime: Long = 0
    private var endTime: Long = 0

    fun start() {
        startTime = System.currentTimeMillis()
    }

    fun end() {
        endTime = System.currentTimeMillis()
    }

    fun elapsedTime(): Long {
        return endTime - startTime
    }
}
