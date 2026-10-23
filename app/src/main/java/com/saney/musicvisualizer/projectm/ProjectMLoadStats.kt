package com.saney.musicvisualizer.projectm

/**
 * V157: what the on-screen projectM costs — written by the live renderer (GL thread), read once a
 * second by the load meter on the right page. [gpuFrameMs]: one frame from «render» to «the graphics
 * finished it» (glFinish), measured once a second so it does not slow the picture down.
 */
object ProjectMLoadStats {
    @Volatile var fps: Float = 0f

    @Volatile var gpuFrameMs: Float = 0f

    /** V165: true — the graphics chip's own timer; V169: that timer is removed (Mali crash), always false. */
    @Volatile var gpuExact: Boolean = false

    @Volatile var updatedAtMs: Long = 0L
}
