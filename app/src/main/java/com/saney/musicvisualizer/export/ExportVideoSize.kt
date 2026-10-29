package com.saney.musicvisualizer.export

/**
 * V199 (faric 29.10 17:25, після замірів V198: «відео 720p — тест»): the MP4's frame size. 1080p as
 * before (long edge at most 1920); 720p — short edge 720 (1080 × 1920 → 720 × 1280), every layer
 * (projectM, HUD, theme) is drawn on a 2/3 smaller frame, the encoder has less to squeeze. Pure — JVM tests.
 */
object ExportVideoSize {
    private const val MAX_LONG_EDGE = 1920
    private const val SHORT_EDGE_720P = 720

    /** The format's [width] × [height] → the video's frame; only shrinks; both even. */
    fun fit(
        width: Int,
        height: Int,
        is720: Boolean = false,
    ): Pair<Int, Int> {
        val scale = scaleOf(width, height, is720)
        if (scale >= 1f) return even(width) to even(height)
        return even(Math.round(width * scale)) to even(Math.round(height * scale))
    }

    /** How much smaller than the format's own size (1 for 1080p, 2/3 for 720p). */
    fun scaleOf(
        width: Int,
        height: Int,
        is720: Boolean,
    ): Float {
        val longEdge = maxOf(width, height).coerceAtLeast(1)
        val shortEdge = minOf(width, height).coerceAtLeast(1)
        var scale = if (longEdge > MAX_LONG_EDGE) MAX_LONG_EDGE.toFloat() / longEdge else 1f
        if (is720 && shortEdge * scale > SHORT_EDGE_720P) scale = SHORT_EDGE_720P.toFloat() / shortEdge
        return scale
    }

    fun label(is720: Boolean): String = if (is720) "720p (швидше, трохи м'якше)" else "1080p (як було)"

    private fun even(v: Int): Int = (v.coerceAtLeast(2) / 2) * 2
}
