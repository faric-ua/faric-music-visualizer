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
}
