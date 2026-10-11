package com.saney.musicvisualizer.board

/**
 * Uniformly fits the ENTIRE original bitmap canvas inside a square maximum
 * extent. The original width:height ratio (including transparent margins)
 * is kept; nothing is cropped or rewritten on disk.
 *
 * [maxExtent] is the original per-layer "Розмір" slider's upper bounding
 * dimension, not a requested square destination. Square sources retain the
 * old exact size and position, and only non-square PNGs stop being stretched.
 */
object BoardBitmapGeometry {
    data class Extent(val width: Float, val height: Float)

    fun fitCanvas(bitmapWidth: Int, bitmapHeight: Int, maxExtent: Float): Extent {
        if (bitmapWidth <= 0 || bitmapHeight <= 0 ||
            !maxExtent.isFinite() || maxExtent <= 0f
        ) return Extent(0f, 0f)

        val longer = maxOf(bitmapWidth, bitmapHeight).toFloat()
        return Extent(
            width = maxExtent * (bitmapWidth / longer),
            height = maxExtent * (bitmapHeight / longer),
        )
    }
}
