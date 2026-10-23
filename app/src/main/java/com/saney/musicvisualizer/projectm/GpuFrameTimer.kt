package com.saney.musicvisualizer.projectm

import android.opengl.GLES20
import android.opengl.GLES30

/**
 * V165 (скрін faric 23.10 18:03: «GPU 100+ %» при 61 кадрі на секунду). glFinish → render → glFinish
 * counted the CPU sending the frame plus the graphics doing it one after the other, so a frame that the
 * phone easily keeps up with looked like 18 ms. This asks the graphics chip itself how long it worked
 * (GL_EXT_disjoint_timer_query, «таймер у самій графіці»): no waiting, the answer is read a few frames
 * later. Phones without the extension use the old measure, capped by the real frame rate
 * ([com.saney.musicvisualizer.ui.LoadMeter.fallbackFrameMs]).
 *
 * GL thread only. A new surface = a new GL context → [reset].
 */
internal class GpuFrameTimer {
    private var supported: Boolean? = null
    private val ids = IntArray(SLOTS)
    private val pending = BooleanArray(SLOTS)
    private val one = IntArray(1)
    private var next = 0
    private var active = -1
    private var sumNs = 0L
    private var count = 0
    private var goodResults = 0

    val isExact: Boolean
        get() = supported == true

    fun reset() {
        supported = null
        pending.fill(false)
        next = 0
        active = -1
        sumNs = 0L
        count = 0
    }

    /** Starts timing this frame's GL work; false — nothing started (not supported, or the chip is behind). */
    fun begin(): Boolean {
        if (!ensureSupported()) return false
        if (pending[next]) return false
        GLES30.glBeginQuery(GL_TIME_ELAPSED_EXT, ids[next])
        active = next
        return true
    }

    fun end() {
        if (active < 0) return
        GLES30.glEndQuery(GL_TIME_ELAPSED_EXT)
        pending[active] = true
        next = (active + 1) % SLOTS
        active = -1
        poll()
    }

    /** The average graphics time of the frames measured since the last call, or null if none. */
    fun takeAverageMs(): Float? {
        if (count == 0) return null
        val ms = sumNs / count / 1_000_000f
        sumNs = 0L
        count = 0
        return ms
    }

    private fun poll() {
        GLES20.glGetIntegerv(GL_GPU_DISJOINT_EXT, one, 0)
        val disjoint = one[0] != 0
        for (step in 0 until SLOTS) {
            // oldest first: the slot after the one just used
            val i = (next + step) % SLOTS
            if (!pending[i]) continue
            GLES30.glGetQueryObjectuiv(ids[i], GLES30.GL_QUERY_RESULT_AVAILABLE, one, 0)
            if (one[0] == 0) break
            GLES30.glGetQueryObjectuiv(ids[i], GLES30.GL_QUERY_RESULT, one, 0)
            pending[i] = false
            // the chip was interrupted (power state, another app) — that result means nothing
            if (disjoint) continue
            val ns = one[0].toLong() and 0xFFFF_FFFFL
            if (ns in 1L..MAX_SANE_NS) {
                sumNs += ns
                count++
                if (++goodResults == GpuTimerGate.GOOD_RESULTS_FOR_OK) GpuTimerGate.markOk()
            }
        }
    }

    private fun ensureSupported(): Boolean {
        supported?.let { return it }
        // V168: a phone where the timer once crashed the app never uses it again.
        if (!GpuTimerGate.allowed) {
            supported = false
            return false
        }
        val ok =
            runCatching {
                val version = GLES20.glGetString(GLES20.GL_VERSION).orEmpty()
                val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS).orEmpty()
                if (!version.contains("OpenGL ES 3") || !extensions.contains("GL_EXT_disjoint_timer_query")) {
                    return@runCatching false
                }
                GpuTimerGate.markTrying()
                while (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
                    // clear what projectM left, so the check below is only about the queries
                }
                GLES30.glGenQueries(SLOTS, ids, 0)
                GLES30.glBeginQuery(GL_TIME_ELAPSED_EXT, ids[0])
                GLES30.glEndQuery(GL_TIME_ELAPSED_EXT)
                GLES20.glGetIntegerv(GL_GPU_DISJOINT_EXT, one, 0)
                GLES20.glGetError() == GLES20.GL_NO_ERROR
            }.getOrDefault(false)
        if (ok) {
            // the test query above is simply left to finish; slot 0 waits for it
            pending[0] = true
            next = 1
        }
        supported = ok
        return ok
    }

    private companion object {
        const val SLOTS = 4
        const val GL_TIME_ELAPSED_EXT = 0x88BF
        const val GL_GPU_DISJOINT_EXT = 0x8FBB
        const val MAX_SANE_NS = 2_000_000_000L
    }
}
