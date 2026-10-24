package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftwareBlitTest {
    private fun m(
        sx: Float,
        tx: Float,
        sy: Float,
        ty: Float,
        skew: Float = 0f,
    ) = floatArrayOf(sx, skew, tx, 0f, sy, ty, 0f, 0f, 1f)

    @Test fun onlyStretchAndMoveAreUsable() {
        assertTrue(SoftwareBlitMath.isAxisAligned(m(0.82f, 10f, 0.82f, 4f)))
        assertFalse(SoftwareBlitMath.isAxisAligned(m(0.82f, 10f, 0.82f, 4f, skew = 0.3f)))
        assertFalse(SoftwareBlitMath.isAxisAligned(m(-1f, 0f, 1f, 0f))) // mirrored
        assertFalse(SoftwareBlitMath.isAxisAligned(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0.001f, 0f, 1f)))
        assertFalse(SoftwareBlitMath.isAxisAligned(FloatArray(4)))
    }

    @Test fun rectangleLandsOnWholePixels() {
        val r = SoftwareBlitMath.deviceRect(m(0.5f, 10f, 0.5f, 20f), 100.4f, 41f, 300.6f, 141f)!!
        assertEquals(60, r.left) // 50.2 + 10
        assertEquals(41, r.top) // 20.5 + 20 → 41 (round half up)
        assertEquals(100, r.width) // 160.3 → 160, minus 60
        assertEquals(50, r.height)
        assertNull(SoftwareBlitMath.deviceRect(m(0.01f, 0f, 0.01f, 0f), 0f, 0f, 10f, 10f))
    }

    @Test fun crawlKeepsQuarterPixelSteps() {
        assertEquals(SoftwareBlitMath.RowPhase(10, 0), SoftwareBlitMath.rowPhase(10.1f, 4))
        assertEquals(SoftwareBlitMath.RowPhase(10, 1), SoftwareBlitMath.rowPhase(10.25f, 4))
        assertEquals(SoftwareBlitMath.RowPhase(10, 3), SoftwareBlitMath.rowPhase(10.8f, 4))
        assertEquals(SoftwareBlitMath.RowPhase(11, 0), SoftwareBlitMath.rowPhase(10.9f, 4))
        assertEquals(SoftwareBlitMath.RowPhase(-3, 2), SoftwareBlitMath.rowPhase(-2.5f, 4)) // above the top
        assertEquals(SoftwareBlitMath.RowPhase(10, 1), SoftwareBlitMath.rowPhase(10.5f, 2))
        // the error is never more than an eighth of a pixel with 4 copies
        var y = 0f
        while (y < 5f) {
            val p = SoftwareBlitMath.rowPhase(y, 4)
            assertEquals(y, p.row + p.phase / 4f, 0.1251f)
            y += 0.037f
        }
    }

    @Test fun bigTextsKeepTheOldWayInsteadOfWholePixelHops() {
        val mb = 1024L * 1024
        assertEquals(4, SoftwareBlitMath.textPhases(5 * mb))
        assertEquals(2, SoftwareBlitMath.textPhases(10 * mb))
        assertEquals(0, SoftwareBlitMath.textPhases(13 * mb))
        assertEquals(0, SoftwareBlitMath.textPhases(0))
        assertEquals(600L * 400 * 4, SoftwareBlitMath.bytesOf(600, 400, 4))
    }
}
