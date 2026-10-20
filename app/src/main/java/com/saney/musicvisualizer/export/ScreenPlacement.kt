package com.saney.musicvisualizer.export

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * V46 / V82 «відео як екран» (faric 19.10 16:07, 20.10 05:36): the hero (GF) and the buttons
 * (HUD) are laid out exactly as on the phone screen — a virtual portrait canvas with the
 * screen's proportions — and that canvas is scaled into the video frame ONE uniform scale:
 *  · 9:16 — fills the frame («на весь кадр»), the little that sticks out is cut off;
 *  · 16:9, 1:1, 4:5 — fits by height («пропорційно менший, по центру»), because filling
 *    would cut away most of the screen.
 * The background (projectM, reactions, equalizer, effects) still fills the whole frame.
 * Pure math — tested on JVM.
 */
data class ScreenPlacement(
    val virtualWidth: Int,
    val virtualHeight: Int,
    val scale: Float,
    val dx: Float,
    val dy: Float,
) {
    fun mapX(x: Float): Float = dx + x * scale

    fun mapY(y: Float): Float = dy + y * scale

    fun mapSize(size: Float): Float = size * scale

    companion object {
        /** Fill the frame only if that cuts at most this share of the screen. */
        const val MAX_FILL_CROP = 0.25f

        /**
         * @param screenAspect phone screen height / width in portrait (e.g. 2340 / 1080 ≈ 2.17).
         * @param fitWholeScreen «вміщається все» — never cut anything.
         */
        fun compute(
            frameWidth: Int,
            frameHeight: Int,
            screenAspect: Float,
            fitWholeScreen: Boolean,
        ): ScreenPlacement {
            val w = frameWidth.coerceAtLeast(1)
            val h = frameHeight.coerceAtLeast(1)
            val aspect = screenAspect.takeIf { it.isFinite() && it > 0.2f } ?: (16f / 9f)
            val vw = min(w, h)
            val vh = (vw * aspect).roundToInt().coerceAtLeast(1)
            val fill = max(w.toFloat() / vw, h.toFloat() / vh)
            val fit = min(w.toFloat() / vw, h.toFloat() / vh)
            val visibleWithFill = min(w / (vw * fill), h / (vh * fill))
            val useFill = !fitWholeScreen && 1f - visibleWithFill <= MAX_FILL_CROP + 1e-4f
            val s = if (useFill) fill else fit
            return ScreenPlacement(
                virtualWidth = vw,
                virtualHeight = vh,
                scale = s,
                dx = (w - vw * s) / 2f,
                dy = (h - vh * s) / 2f,
            )
        }
    }
}
