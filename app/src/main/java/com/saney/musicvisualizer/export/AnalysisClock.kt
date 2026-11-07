package com.saney.musicvisualizer.export

/**
 * V242 (знайдено 07.11 під час V241): which sound-analysis signal belongs to a moment of the video.
 *
 * The analysis makes one signal per 1024 samples: 23,22 ms at 44,1 kHz, 21,33 ms at 48 kHz. The step used to be
 * counted in whole ms (23 / 21), so the index ran 0,96 % / 1,56 % ahead — at the end of a 4:30 song the equalizer,
 * hero and HUD reacted to sound 2,6 s / 4,2 s before it played. Pure — tested on JVM.
 */
object AnalysisClock {
    /** The true step between signals, ms with fractions. */
    fun intervalMs(
        hopSamples: Int,
        sampleRate: Int,
    ): Double = hopSamples * 1000.0 / sampleRate.coerceAtLeast(1)

    /** The signal for [timeMs] after the analysis start (held until the next one). */
    fun indexAt(
        timeMs: Long,
        intervalMs: Double,
        size: Int,
    ): Int {
        if (size <= 0) return 0
        val step = if (intervalMs > 0.0) intervalMs else 1.0
        return (timeMs.coerceAtLeast(0L) / step).toInt().coerceIn(0, size - 1)
    }
}
