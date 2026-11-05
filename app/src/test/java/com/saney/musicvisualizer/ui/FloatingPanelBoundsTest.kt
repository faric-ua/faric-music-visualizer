package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingPanelBoundsTest {
    @Test fun fortyPercentBehindLeftRightAndBottomNeverTop() {
        // full-width panel 1080 × 900 on a 1080 × 2400 screen
        val r = FloatingPanelBounds.of(1080, 2400, 1080, 900)
        assertEquals(-432, r.minX)
        assertEquals(432, r.maxX)
        assertEquals(0, r.minY)
        assertEquals(1500 + 360, r.maxY)
        assertEquals(0, r.clampY(-500))
        assertEquals(-432, r.clampX(-2000))
        assertEquals(432, r.clampX(2000))
    }

    @Test fun firstPressStandsStraightSecondTucks() {
        // full-width panel 1080 × 900 on 1080 × 2400: centre (0, 750)
        val r = FloatingPanelBounds.of(1080, 2400, 1080, 900)
        fun snap(s: FloatingPanelBounds.Snap, x: Int, y: Int) = FloatingPanelBounds.snap(r, s, x, y, centerX = 0, centerY = 750)
        // hidden on the right, ← : first all of it visible at the left edge, then 40 % behind it
        assertEquals(0 to 300, snap(FloatingPanelBounds.Snap.LEFT, 432, 300))
        assertEquals(-432 to 300, snap(FloatingPanelBounds.Snap.LEFT, 0, 300))
        assertEquals(0 to 300, snap(FloatingPanelBounds.Snap.RIGHT, -432, 300))
        assertEquals(432 to 300, snap(FloatingPanelBounds.Snap.RIGHT, 0, 300))
        // ↓ : first all visible at the bottom, then 40 % below
        assertEquals(100 to 1500, snap(FloatingPanelBounds.Snap.BOTTOM, 100, 300))
        assertEquals(100 to 1860, snap(FloatingPanelBounds.Snap.BOTTOM, 100, 1500))
        // ↑ never hides
        assertEquals(100 to 0, snap(FloatingPanelBounds.Snap.TOP, 100, 0))
        assertEquals(0 to 750, snap(FloatingPanelBounds.Snap.CENTER, -432, 1860))
    }
}
