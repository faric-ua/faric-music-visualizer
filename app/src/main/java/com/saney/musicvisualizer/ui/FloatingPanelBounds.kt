package com.saney.musicvisualizer.ui

/**
 * V219 (faric 04.11 20:17: «панельку «Шари» — на всю ширину, на 25 % вище; щоб могла сховати 40 % себе за правий край,
 * 40 % за лівий і 40 % униз; вгору не треба — не витягнемо назад»). Where the dragged panel may stand. Pure — JVM tests.
 */
object FloatingPanelBounds {
    /** How much of the panel may go behind the left / right / bottom edge. */
    const val HIDE_FRACTION = 0.4f

    data class Range(val minX: Int, val maxX: Int, val minY: Int, val maxY: Int) {
        fun clampX(x: Int): Int = x.coerceIn(minX, maxX)

        fun clampY(y: Int): Int = y.coerceIn(minY, maxY)
    }

    /** V224: «приліпити» — where the panel goes for each arrow of the ◎ pad (x, y). */
    enum class Snap { TOP, BOTTOM, LEFT, RIGHT, CENTER }

    /**
     * V232 (faric 05.11 20:02: «якщо сховано праворуч і я тисну вліво — притуляється до лівого краю; ще раз — ховається
     * далі»). First press — the panel stands straight against that edge, all of it visible; when it already stands so —
     * it tucks 40 % behind that edge (top — never behind: the header is the handle).
     */
    fun snap(
        range: Range,
        snap: Snap,
        x: Int,
        y: Int,
        centerX: Int,
        centerY: Int,
    ): Pair<Int, Int> {
        // the centred panel stands at (screen − panel) / 2 — so the edges where all of it is visible are 0 and twice that
        val flushLeft = 0
        val flushRight = range.clampX(centerX * 2)
        val flushBottom = range.clampY(centerY * 2)
        return when (snap) {
            Snap.TOP -> x to range.minY
            Snap.BOTTOM -> x to (if (y == flushBottom) range.maxY else flushBottom)
            Snap.LEFT -> (if (x == flushLeft) range.minX else flushLeft) to y
            Snap.RIGHT -> (if (x == flushRight) range.maxX else flushRight) to y
            Snap.CENTER -> centerX to centerY
        }
    }

    fun of(
        screenWidth: Int,
        screenHeight: Int,
        panelWidth: Int,
        panelHeight: Int,
    ): Range {
        val sideHide = (panelWidth * HIDE_FRACTION).toInt()
        val bottomHide = (panelHeight * HIDE_FRACTION).toInt()
        return Range(
            minX = -sideHide,
            maxX = (screenWidth - panelWidth).coerceAtLeast(0) + sideHide,
            // never above the top edge: the header (the handle) must stay reachable
            minY = 0,
            maxY = (screenHeight - panelHeight).coerceAtLeast(0) + bottomHide,
        )
    }
}
