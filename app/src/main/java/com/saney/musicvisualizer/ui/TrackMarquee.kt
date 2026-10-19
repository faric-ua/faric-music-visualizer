package com.saney.musicvisualizer.ui

/**
 * «Стрічка» for track lines that do not fit between ♡ and ⋮ (V64, faric 19.10 23:17).
 * Pure math so it can be tested on JVM: stand still for [PAUSE_MS] at the start,
 * then scroll left at [SPEED_DP_PER_S]; the text repeats after a [GAP_DP] gap, so the
 * loop is seamless.
 */
object TrackMarquee {
    const val PAUSE_MS = 1_500L
    const val SPEED_DP_PER_S = 36f
    const val GAP_DP = 48f

    fun gapPx(density: Float): Float = GAP_DP * density

    /** Horizontal shift of the text in px, or null when the line fits and stays centred. */
    fun offsetPx(
        textWidthPx: Float,
        availablePx: Float,
        density: Float,
        clockMs: Long,
    ): Float? {
        if (textWidthPx <= availablePx || availablePx <= 0f) return null
        val speedPxPerMs = SPEED_DP_PER_S * density / 1000f
        if (speedPxPerMs <= 0f) return null
        val cycle = textWidthPx + gapPx(density)
        val scrollMs = (cycle / speedPxPerMs).toLong().coerceAtLeast(1L)
        val t = Math.floorMod(clockMs, PAUSE_MS + scrollMs)
        return if (t < PAUSE_MS) 0f else ((t - PAUSE_MS) * speedPxPerMs).coerceAtMost(cycle)
    }
}
