package com.saney.musicvisualizer.export

/**
 * V212 (faric 04.11 16:39, звіт «чистий екран, лише projectM»: 15 797 кадрів — 95 с «GPU overlay» + 64 с «composition»):
 * with every layer over projectM switched off the video still drew a fully transparent 1080 × 1920 picture each
 * frame and laid it over projectM — almost half of the export, for nothing.
 *
 * Skipping is allowed only when (1) the settings say nothing is drawn over projectM and (2) the overlay really came
 * out fully transparent. The overlay is still drawn and checked every [CHECK_EVERY] frames; the first non-transparent
 * pixel turns skipping off for the rest of the video. Pure — tested on JVM.
 */
class EmptyOverlayGuard(
    /** The settings: nothing over projectM (no HUD part, GF, theme, reactive, big EQ, effects). */
    allowed: Boolean,
) {
    private var possible = allowed
    private var confirmedEmpty = false

    /** Draw the overlay on this frame? (false — skip composing it and laying it over projectM). */
    fun mustDraw(frameIndex: Int): Boolean = !possible || !confirmedEmpty || frameIndex % CHECK_EVERY == 0

    /** After the overlay was drawn: [transparent] — not one pixel with alpha. */
    fun observe(transparent: Boolean) {
        if (!possible) return
        if (transparent) {
            confirmedEmpty = true
        } else {
            possible = false
            confirmedEmpty = false
        }
    }

    /** Is a check needed after drawing (only while skipping is still possible). */
    val checking: Boolean get() = possible

    val skippedSoFar: Boolean get() = possible && confirmedEmpty

    companion object {
        const val CHECK_EVERY = 300

        /** ARGB pixels: every alpha byte is zero. */
        fun allTransparent(
            pixels: IntArray,
            count: Int = pixels.size,
        ): Boolean {
            for (i in 0 until count) if (pixels[i] ushr 24 != 0) return false
            return true
        }
    }
}
