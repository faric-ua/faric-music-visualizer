package com.saney.musicvisualizer.ui

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * V174 крок 3 (faric 24.10 15:48: «поки ти зробиш… будемо чекати, поки в нас рамка вийде»): the video
 * draws the HUD on a software canvas that is scaled to the video size. Every PNG panel, the cover picture
 * and the cover text were squeezed (filtered) on every frame — about half of the export time. Instead
 * the HUD keeps copies already at the video's pixel size and copies them 1:1 onto whole pixels — the
 * cheapest drawing the CPU can do. The screen (graphics chip) is not touched.
 *
 * Pure math, no Android — tested in `SoftwareBlitTest`.
 */
object SoftwareBlitMath {
    /** One cached copy may not be bigger than this (bytes). */
    const val MAX_ENTRY_BYTES = 12L * 1024 * 1024

    /** All copies of the PNG panels together. */
    const val PANEL_BUDGET_BYTES = 40L * 1024 * 1024

    /** The cover text: all its sub-pixel copies together. */
    const val TEXT_BUDGET_BYTES = 24L * 1024 * 1024

    /** A whole-pixel rectangle on the video picture. */
    data class PixelRect(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int,
    )

    /**
     * The matrix [values] (Android order: scaleX, skewX, transX, skewY, scaleY, transY, p0, p1, p2) is
     * usable only when it only stretches and moves — no turn, no mirror, no perspective.
     */
    fun isAxisAligned(values: FloatArray): Boolean {
        if (values.size < 9) return false
        if (abs(values[1]) > 1e-4f || abs(values[3]) > 1e-4f) return false
        if (abs(values[6]) > 1e-6f || abs(values[7]) > 1e-6f || abs(values[8] - 1f) > 1e-4f) return false
        return values[0] > 1e-3f && values[4] > 1e-3f
    }

    /** Where the rectangle lands on the picture, edges rounded to whole pixels. Null when it vanishes. */
    fun deviceRect(
        values: FloatArray,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ): PixelRect? {
        val sx = values[0]
        val sy = values[4]
        val tx = values[2]
        val ty = values[5]
        val l = (left * sx + tx).roundToInt()
        val t = (top * sy + ty).roundToInt()
        val r = (right * sx + tx).roundToInt()
        val b = (bottom * sy + ty).roundToInt()
        val w = r - l
        val h = b - t
        if (w <= 0 || h <= 0) return null
        return PixelRect(l, t, w, h)
    }

    fun bytesOf(width: Int, height: Int, bytesPerPixel: Int): Long = width.toLong() * height.toLong() * bytesPerPixel

    /**
     * How many sub-pixel copies of the crawling text to keep (4 → quarter-pixel steps, 2 → half-pixel).
     * 0 — too big: keep the old way (filtered drawing every frame), never whole-pixel hops (V148).
     */
    fun textPhases(oneCopyBytes: Long): Int =
        when {
            oneCopyBytes <= 0L -> 0
            oneCopyBytes > MAX_ENTRY_BYTES -> 0
            oneCopyBytes * 4 <= TEXT_BUDGET_BYTES -> 4
            oneCopyBytes * 2 <= TEXT_BUDGET_BYTES -> 2
            else -> 0
        }

    /** Whole pixel row + which of the [phases] copies (each drawn [phase]/[phases] px lower) to use. */
    data class RowPhase(
        val row: Int,
        val phase: Int,
    )

    fun rowPhase(
        y: Float,
        phases: Int,
    ): RowPhase {
        val n = phases.coerceAtLeast(1)
        var row = floor(y).toInt()
        var phase = ((y - row) * n).roundToInt()
        if (phase >= n) {
            row += 1
            phase = 0
        }
        return RowPhase(row, phase)
    }
}
