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

    fun snap(
        range: Range,
        snap: Snap,
        x: Int,
        y: Int,
        centerX: Int,
        centerY: Int,
    ): Pair<Int, Int> =
        when (snap) {
            // top / bottom — right against the edge, the whole panel visible
            Snap.TOP -> x to range.minY
            // the centred panel stands at (screen − panel) / 2, so the fully visible bottom is twice that
            Snap.BOTTOM -> x to range.clampY(centerY * 2)
            // left / right — tucked behind the edge as far as allowed (40 %), the height kept
            Snap.LEFT -> range.minX to y
            Snap.RIGHT -> range.maxX to y
            Snap.CENTER -> centerX to centerY
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
