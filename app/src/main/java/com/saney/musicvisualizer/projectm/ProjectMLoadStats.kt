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

    /**
     * V185: one frame's whole work — sending it and the graphics doing it, one after the other
     * (glFinish → render → glFinish), NOT capped by the frame rate. Not the graphics alone (no timer —
     * V169), but light and heavy visualizations differ in it clearly; [gpuFrameMs] (capped) was ~16 ms
     * for everything that keeps 60 frames.
     */
    @Volatile var frameWorkMs: Float = 0f

    @Volatile var updatedAtMs: Long = 0L
}
