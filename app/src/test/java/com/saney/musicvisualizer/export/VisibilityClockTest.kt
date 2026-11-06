package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibilityClockTest {
    @Test fun countsEachPlaceAndTheRunningOne() {
        val c = VisibilityClock(VisibilityClock.State.ON_SCREEN, 0L)
        c.mark(VisibilityClock.State.HIDDEN, 1_000L)
        c.mark(VisibilityClock.State.HIDDEN, 2_000L) // same state — nothing changes
        c.mark(VisibilityClock.State.SCREEN_OFF, 8_000L)
        val s = c.snapshot(10_000L)
        assertEquals(1_000L, s.onScreenMs)
        assertEquals(7_000L, s.hiddenMs)
        assertEquals(2_000L, s.screenOffMs)
        assertEquals("на екрані 10 % · згорнуто 70 % · екран вимкнено 20 %", s.label())
    }

    @Test fun oneSongIsTheDifferenceOfTwoSnapshots() {
        val c = VisibilityClock(VisibilityClock.State.HIDDEN, 0L)
        val start = c.snapshot(5_000L)
        c.mark(VisibilityClock.State.ON_SCREEN, 6_000L)
        val song = c.snapshot(9_000L) - start
        assertEquals(1_000L, song.hiddenMs)
        assertEquals(3_000L, song.onScreenMs)
        assertEquals("на екрані 75 % · згорнуто 25 %", song.label())
    }

    @Test fun onePlaceSaysAllTheTime() {
        assertEquals("згорнуто весь час", VisibilityClock.Share(hiddenMs = 60_000L).label())
        assertNull(VisibilityClock.Share().label())
    }

    @Test fun percentsAddUpTo100() {
        val l = VisibilityClock.Share(1L, 1L, 1L).label()!!
        val sum = Regex("(\\d+) %").findAll(l).sumOf { it.groupValues[1].toInt() }
        assertEquals(100, sum)
    }

    @Test fun batchSummaryExplainsASlowSong() {
        val slow = BatchPlan.slowestOf(
            listOf(
                Triple("/x/blink.milk", 60, 400L), // 1 s — a blink, not counted
                Triple("/x/glowsticks v2 05.milk", 2_400, 85L),
                Triple("/x/skylight a3.milk", 6_000, 30L),
            ),
            fps = 60,
        )
        assertEquals(BatchPlan.Slowest("glowsticks v2 05", 85L, 40), slow)
        val text =
            BatchPlan.summary(
                folder = "Black Side",
                results =
                    listOf(
                        BatchPlan.SongResult(
                            "TRACK 05", BatchPlan.Outcome.DONE, elapsedMs = 2_001_000L, songMs = 257_000L,
                            visibility = VisibilityClock.Share(onScreenMs = 200_000L, hiddenMs = 1_400_000L, screenOffMs = 400_000L),
                            slowest = slow,
                        ),
                    ),
                total = 1,
            )
        assertTrue(text, text.contains("PulseDeck: на екрані 10 % · згорнуто 70 % · екран вимкнено 20 %\n"))
        assertTrue(text, text.contains("\n   на екрані 10 % · згорнуто 70 % · екран вимкнено 20 % · найповільніша: glowsticks v2 05 · 85 мс/кадр · 40 с"))
    }
}
