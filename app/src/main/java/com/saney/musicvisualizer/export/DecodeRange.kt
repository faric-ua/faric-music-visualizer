package com.saney.musicvisualizer.export

/**
 * Which part of a song to decode (V60). Whole song = [0, ∞). A «3 s test» decodes only
 * its piece plus a short lead-in, instead of the whole song (faric 19.10 23:08: «3 с відео,
 * а пісня готується 30–45 с»).
 */
data class DecodeRange(
    val startUs: Long,
    val endUs: Long,
) {
    val isWindow: Boolean get() = startUs > 0L || endUs != Long.MAX_VALUE

    /** Stop feeding the decoder once the extractor is past the end of the piece. */
    fun isPastEnd(sampleTimeUs: Long): Boolean = endUs != Long.MAX_VALUE && sampleTimeUs >= endUs

    fun progressPercent(ptsUs: Long, sourceDurationUs: Long): Int {
        val from = if (isWindow) startUs else 0L
        val to = if (endUs != Long.MAX_VALUE) endUs else sourceDurationUs
        val span = (to - from).coerceAtLeast(1L)
        return ((ptsUs - from).coerceAtLeast(0L) * 100L / span).toInt().coerceIn(0, 99)
    }

    companion object {
        /** Lead-in before a clip, so bass / beat smoothing is warmed up at the first frame. */
        const val LEAD_IN_MS = 1_000L
        const val TAIL_MS = 250L

        fun of(startMs: Long, endMs: Long): DecodeRange =
            DecodeRange(
                startUs = startMs.coerceAtLeast(0L) * 1000L,
                endUs = if (endMs == Long.MAX_VALUE) Long.MAX_VALUE else endMs.coerceAtLeast(startMs + 1L) * 1000L,
            )

        /** Range for a clip of a song: [clipStart - lead-in, clipEnd + tail]. */
        fun forClip(clipStartMs: Long, clipLengthMs: Long): Pair<Long, Long> =
            (clipStartMs - LEAD_IN_MS).coerceAtLeast(0L) to (clipStartMs + clipLengthMs + TAIL_MS)
    }
}
