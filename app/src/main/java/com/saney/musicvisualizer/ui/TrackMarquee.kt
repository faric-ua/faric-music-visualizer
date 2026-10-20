package com.saney.musicvisualizer.ui

/**
 * «Стрічка» for the track lines between ♡ and ⋮ (V64), taking turns (V69, faric 19.10 23:24:
 * «назва потихеньку йде; дійшла до кінця — виконавець починає йти, а назва вертається;
 * плавно, красивенько»). Only ONE line moves at a time:
 *   wait [PAUSE_MS] → slide left until its end is visible → hold [END_HOLD_MS] →
 *   slide back at the SAME speed (faric 20.10 05:36: «назад з такою самою швидкістю, не так
 *   швидко») → the next line that does not fit … → round again.
 * Lines that fit stay centred. Pure math, tested on JVM; MP4 export passes the video time.
 */
object TrackMarquee {
    const val PAUSE_MS = 1_500L
    const val END_HOLD_MS = 1_000L
    const val SPEED_DP_PER_S = 36f

    /**
     * Shift (px) of every line at [clockMs]: null = the line fits (draw it centred),
     * a number = draw it from the left edge moved left by that many px.
     */
    fun offsets(
        textWidthsPx: List<Float>,
        availablePx: Float,
        density: Float,
        clockMs: Long,
    ): List<Float?> {
        val overflow = textWidthsPx.map { (it - availablePx).takeIf { extra -> extra > 0.5f && availablePx > 0f } }
        val moving = overflow.withIndex().filter { it.value != null }
        if (moving.isEmpty()) return overflow
        val speedPxPerMs = (SPEED_DP_PER_S * density / 1000f).coerceAtLeast(0.001f)
        val phases =
            moving.map { (index, extra) ->
                val scrollMs = (extra!! / speedPxPerMs).toLong().coerceAtLeast(1L)
                Triple(index, extra, scrollMs)
            }
        val cycle = phases.sumOf { (_, _, scrollMs) -> PAUSE_MS + scrollMs + END_HOLD_MS + scrollMs }
        var t = Math.floorMod(clockMs, cycle)
        val result = overflow.map { if (it == null) null else 0f }.toMutableList()
        for ((index, extra, scrollMs) in phases) {
            val span = PAUSE_MS + scrollMs + END_HOLD_MS + scrollMs
            if (t < span) {
                result[index] =
                    when {
                        t < PAUSE_MS -> 0f
                        t < PAUSE_MS + scrollMs -> (t - PAUSE_MS) * speedPxPerMs
                        t < PAUSE_MS + scrollMs + END_HOLD_MS -> extra
                        else -> extra - (t - PAUSE_MS - scrollMs - END_HOLD_MS) * speedPxPerMs
                    }.coerceIn(0f, extra)
                break
            }
            t -= span
        }
        return result
    }
}
