package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodeRangeTest {
    @Test
    fun wholeSongIsNotAWindow() {
        val r = DecodeRange.of(0L, Long.MAX_VALUE)
        assertFalse(r.isWindow)
        assertFalse(r.isPastEnd(999_000_000L))
        assertEquals(50, r.progressPercent(100_000_000L, 200_000_000L))
    }

    @Test
    fun clipWindowStopsAtItsEnd() {
        val (from, to) = DecodeRange.forClip(118_500L, 3_000L)
        assertEquals(117_500L, from)
        assertEquals(121_750L, to)
        val r = DecodeRange.of(from, to)
        assertTrue(r.isWindow)
        assertFalse(r.isPastEnd(121_000_000L))
        assertTrue(r.isPastEnd(121_750_000L))
        assertEquals(0, r.progressPercent(117_000_000L, 240_000_000L))
        assertEquals(50, r.progressPercent(119_625_000L, 240_000_000L))
    }

    @Test
    fun clipAtSongStartHasNoNegativeLeadIn() {
        assertEquals(0L to 3_250L, DecodeRange.forClip(0L, 3_000L))
    }
}
