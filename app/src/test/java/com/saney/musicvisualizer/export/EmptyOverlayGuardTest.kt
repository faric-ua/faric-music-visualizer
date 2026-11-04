package com.saney.musicvisualizer.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmptyOverlayGuardTest {
    @Test fun visibleLayersNeverSkip() {
        val guard = EmptyOverlayGuard(allowed = false)
        guard.observe(true)
        assertTrue((0 until 1000).all { guard.mustDraw(it) })
    }

    @Test fun cleanProjectMSkipsAfterTheFirstEmptyFrameAndStillChecks() {
        val guard = EmptyOverlayGuard(allowed = true)
        assertTrue(guard.mustDraw(0))
        guard.observe(true)
        assertFalse(guard.mustDraw(1))
        assertFalse(guard.mustDraw(299))
        assertTrue("checked again every 300 frames", guard.mustDraw(300))
    }

    @Test fun somethingVisibleTurnsSkippingOffForGood() {
        val guard = EmptyOverlayGuard(allowed = true)
        guard.observe(true)
        guard.observe(false)
        assertTrue((1 until 1000).all { guard.mustDraw(it) })
        guard.observe(true)
        assertTrue(guard.mustDraw(5))
        assertFalse(guard.checking)
    }

    @Test fun anyAlphaCounts() {
        assertTrue(EmptyOverlayGuard.allTransparent(IntArray(16)))
        assertFalse(EmptyOverlayGuard.allTransparent(IntArray(16).also { it[9] = 0x01000000 }))
        assertTrue("only the first count pixels", EmptyOverlayGuard.allTransparent(IntArray(4).also { it[3] = -1 }, count = 3))
    }
}
