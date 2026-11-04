package com.saney.musicvisualizer.export

/**
 * V199 (faric 29.10 17:25, після замірів V198: «відео 720p — тест»): the MP4's frame size. 1080p as
 * before (long edge at most 1920); 720p — short edge 720 (1080 × 1920 → 720 × 1280), every layer
 * (projectM, HUD, theme) is drawn on a 2/3 smaller frame, the encoder has less to squeeze.
 *
 * V213 (faric 04.11 17:00: «720, 1080, 2К, 4К — щоб ти тести додавав… може, який уривок захочу 4К»): 2K (short edge
 * 1440) and 4K (2160) — the 1080p frame grown ×4/3 and ×2; every layer is drawn at that size. Pure — JVM tests.
 */
enum class VideoSize(
    val shortEdge: Int,
    val label: String,
) {
    P720(720, "720p (швидше, трохи м'якше)"),
    P1080(1080, "1080p (як було)"),
    P1440(1440, "2K · 1440p (тест, ~1,8× довше)"),
    P2160(2160, "4K · 2160p (тест, ~4× довше)"),
    ;

    fun next(): VideoSize = entries[(ordinal + 1) % entries.size]

    companion object {
        /** Saved name → size; before V213 only «720p» was a yes / no. */
        fun from(
            stored: String?,
            old720: Boolean,
        ): VideoSize = entries.firstOrNull { it.name == stored } ?: if (old720) P720 else P1080
    }
}

object ExportVideoSize {
    private const val MAX_LONG_EDGE = 1920
    private const val BASE_SHORT_EDGE = 1080

    /** The format's [width] × [height] → the video's frame; both even. */
    fun fit(
        width: Int,
        height: Int,
        size: VideoSize = VideoSize.P1080,
    ): Pair<Int, Int> {
        val scale = scaleOf(width, height, size)
        if (scale == 1f) return even(width) to even(height)
        return even(Math.round(width * scale)) to even(Math.round(height * scale))
    }

    fun fit(
        width: Int,
        height: Int,
        is720: Boolean,
    ): Pair<Int, Int> = fit(width, height, if (is720) VideoSize.P720 else VideoSize.P1080)

    /** How much bigger / smaller than the format's own size (2/3 for 720p, 1 for 1080p, 2 for 4K). */
    fun scaleOf(
        width: Int,
        height: Int,
        size: VideoSize,
    ): Float {
        val longEdge = maxOf(width, height).coerceAtLeast(1)
        val shortEdge = minOf(width, height).coerceAtLeast(1)
        val base = if (longEdge > MAX_LONG_EDGE) MAX_LONG_EDGE.toFloat() / longEdge else 1f
        return when {
            size.shortEdge < BASE_SHORT_EDGE ->
                if (shortEdge * base > size.shortEdge) size.shortEdge.toFloat() / shortEdge else base
            size.shortEdge == BASE_SHORT_EDGE -> base
            // 2K / 4K: the 1080p frame grown; a format with a smaller short edge grows in the same proportion.
            else -> base * size.shortEdge / BASE_SHORT_EDGE
        }
    }

    fun scaleOf(
        width: Int,
        height: Int,
        is720: Boolean,
    ): Float = scaleOf(width, height, if (is720) VideoSize.P720 else VideoSize.P1080)

    fun label(is720: Boolean): String = (if (is720) VideoSize.P720 else VideoSize.P1080).label

    private fun even(v: Int): Int = (v.coerceAtLeast(2) / 2) * 2
}
