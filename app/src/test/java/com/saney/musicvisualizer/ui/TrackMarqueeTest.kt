package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMarqueeTest {
    private val d = 2.75f

    @Test
    fun linesThatFitStayCentred() {
        assertEquals(listOf<Float?>(null, null, null), TrackMarquee.offsets(listOf(300f, 200f, 100f), 600f, d, 5_000L))
    }

    @Test
    fun onlyOneLineMovesAtATime() {
        val widths = listOf(1000f, 900f, 100f)
        for (t in 0L..120_000L step 53L) {
            val o = TrackMarquee.offsets(widths, 600f, d, t)
            assertNull(o[2])
            val moving = o.count { it != null && it > 0.01f }
            assertTrue("t=$t $o", moving <= 1)
        }
    }

    @Test
    fun titleScrollsToItsEndThenReturnsThenArtistGoes() {
        val widths = listOf(1000f, 900f, 100f)
        val speed = TrackMarquee.SPEED_DP_PER_S * d / 1000f
        val titleScroll = (400f / speed).toLong()
        // waiting at the start
        assertEquals(0f, TrackMarquee.offsets(widths, 600f, d, 1_000L)[0]!!, 0.01f)
        // end of the title visible during the hold
        val hold = TrackMarquee.PAUSE_MS + titleScroll + 500L
        assertEquals(400f, TrackMarquee.offsets(widths, 600f, d, hold)[0]!!, 0.5f)
        // back home, then the artist line starts moving
        val titleSpan = TrackMarquee.PAUSE_MS + titleScroll + TrackMarquee.END_HOLD_MS + TrackMarquee.RETURN_MS
        val artistMoving = titleSpan + TrackMarquee.PAUSE_MS + 1_000L
        val o = TrackMarquee.offsets(widths, 600f, d, artistMoving)
        assertEquals(0f, o[0]!!, 0.01f)
        assertTrue(o[1]!! > 50f)
    }

    @Test
    fun sameClockSameFrame() {
        val w = listOf(1500f, 700f, 300f)
        assertEquals(TrackMarquee.offsets(w, 600f, 3f, 12_345L), TrackMarquee.offsets(w, 600f, 3f, 12_345L))
    }
}
