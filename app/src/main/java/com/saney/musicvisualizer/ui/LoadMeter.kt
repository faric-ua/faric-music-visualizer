package com.saney.musicvisualizer.ui

/**
 * V157 (faric 23.10 15:27: «шкала навантаження — наскільки навантажує телефон ця візуалізація…
 * GPU, CPU»). Pure maths for the meter on the right page — LoadMeterTest.
 *  - GPU: how long the phone's graphics took to finish one projectM frame (measured once a second),
 *    as a share of the 16,7 ms a 60-frames-a-second screen has;
 *  - CPU: how much of the processor PulseDeck takes (all its threads, all cores), from /proc/self/stat.
 */
object LoadMeter {
    const val FRAME_BUDGET_MS = 1000f / 60f

    fun gpuPercent(frameMs: Float): Int = Math.round(frameMs / FRAME_BUDGET_MS * 100f).coerceIn(0, 999)

    /**
     * V185 (faric 24.10 03:54: «50 перевірив — усі 50 важкі, а вони нормальні»): the bar «Кадр» —
     * a frame's whole work against 33 ms (two screen frames): a light one ~5–10 ms is short and
     * green, a heavy one ≥ 25 ms long and red; it no longer stands at ~97 % for everything.
     */
    const val FRAME_SCALE_MS = 33.3f

    fun framePercent(workMs: Float): Int = Math.round(workMs / FRAME_SCALE_MS * 100f).coerceIn(0, 999)

    /**
     * V165: without the chip's timer the glFinish measure also counts the CPU sending the frame, so it
     * can say «18 ms» while the screen keeps 61 frames a second. The graphics cannot be slower than the
     * frames it actually delivers — cap it by 1000 / fps.
     */
    fun fallbackFrameMs(
        finishMs: Float,
        fps: Float,
    ): Float = if (fps > 1f) minOf(finishMs, 1000f / fps) else finishMs

    /** utime + stime (clock ticks) from a /proc/<pid>/stat line; the name in ( ) may hold spaces. */
    fun cpuTicks(statLine: String): Long? {
        val close = statLine.lastIndexOf(')')
        if (close < 0) return null
        val fields = statLine.substring(close + 1).trim().split(Regex("\\s+"))
        // after «)» the fields start at field 3 (state); utime is field 14, stime 15
        val utime = fields.getOrNull(11)?.toLongOrNull() ?: return null
        val stime = fields.getOrNull(12)?.toLongOrNull() ?: return null
        return utime + stime
    }

    fun cpuPercent(
        ticksDelta: Long,
        ticksPerSecond: Long,
        wallMs: Long,
        cores: Int,
    ): Int {
        if (ticksPerSecond <= 0 || wallMs <= 0 || cores <= 0) return 0
        val busySeconds = ticksDelta.toDouble() / ticksPerSecond
        return (busySeconds / (wallMs / 1000.0) / cores * 100.0).toInt().coerceIn(0, 100)
    }

    /** Green → yellow → red, like the recording level in Track Catcher. */
    enum class Level { LOW, MEDIUM, HIGH }

    fun level(percent: Int): Level =
        when {
            percent < 50 -> Level.LOW
            percent < 85 -> Level.MEDIUM
            else -> Level.HIGH
        }
}
