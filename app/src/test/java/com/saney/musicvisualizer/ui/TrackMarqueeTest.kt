package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMarqueeTest {
    @Test
    fun shortLineStaysCentred() {
        assertNull(TrackMarquee.offsetPx(300f, 600f, 2.75f, 10_000L))
    }

    @Test
    fun longLineWaitsThenScrolls() {
        assertEquals(0f, TrackMarquee.offsetPx(1200f, 600f, 2.75f, 0L)!!, 0.001f)
        assertEquals(0f, TrackMarquee.offsetPx(1200f, 600f, 2.75f, 1_400L)!!, 0.001f)
        val later = TrackMarquee.offsetPx(1200f, 600f, 2.75f, 2_500L)!!
        assertEquals(36f * 2.75f, later, 0.5f) // one second of scrolling
    }

    @Test
    fun loopIsSeamlessAndBounded() {
        val density = 2.75f
        val cycle = 1200f + TrackMarquee.gapPx(density)
        for (t in 0L..60_000L step 37L) {
            val o = TrackMarquee.offsetPx(1200f, 600f, density, t)!!
            assertTrue(o in 0f..cycle)
        }
    }

    @Test
    fun sameClockGivesSameFrame() {
        assertEquals(
            TrackMarquee.offsetPx(1500f, 600f, 3f, 12_345L),
            TrackMarquee.offsetPx(1500f, 600f, 3f, 12_345L),
        )
    }
}
