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

    @Test fun snapsStickToTheEdges() {
        // full-width panel 1080 × 900 on 1080 × 2400: centre (0, 750)
        val r = FloatingPanelBounds.of(1080, 2400, 1080, 900)
        val s = FloatingPanelBounds.Snap.entries.associateWith { FloatingPanelBounds.snap(r, it, x = 100, y = 300, centerX = 0, centerY = 750) }
        assertEquals(100 to 0, s[FloatingPanelBounds.Snap.TOP])
        assertEquals("bottom, whole panel visible", 100 to 1500, s[FloatingPanelBounds.Snap.BOTTOM])
        assertEquals("40 % behind the left edge", -432 to 300, s[FloatingPanelBounds.Snap.LEFT])
        assertEquals(432 to 300, s[FloatingPanelBounds.Snap.RIGHT])
        assertEquals(0 to 750, s[FloatingPanelBounds.Snap.CENTER])
    }
}
