package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AnalysisClockTest {
    private fun driftAtEndMs(sampleRate: Int, songMs: Long, intervalMs: Double): Double {
        val trueStep = AnalysisClock.intervalMs(1024, sampleRate)
        val index = AnalysisClock.indexAt(songMs, intervalMs, size = 1_000_000)
        return index * trueStep - songMs // > 0 — the picture reacts to sound from the future
    }

    @Test fun wholeMsStepRanSecondsAheadAtTheEndOfASong() {
        // The old way: 1024*1000/44100 = 23 (whole ms), 1024*1000/48000 = 21.
        val song = 270_000L // 4:30
        assertTrue(driftAtEndMs(44_100, song, 23.0) > 2_500.0)
        assertTrue(driftAtEndMs(48_000, song, 21.0) > 4_000.0)
    }

    @Test fun trueStepStaysWithinOneSignal() {
        for (rate in listOf(44_100, 48_000)) {
            val step = AnalysisClock.intervalMs(1024, rate)
            for (t in listOf(0L, 1_000L, 60_000L, 270_000L, 600_000L)) {
                assertTrue("rate $rate t $t", abs(driftAtEndMs(rate, t, step)) <= step)
            }
        }
    }

    @Test fun stepsAreTheRealOnes() {
        assertEquals(23.22, AnalysisClock.intervalMs(1024, 44_100), 0.01)
        assertEquals(21.33, AnalysisClock.intervalMs(1024, 48_000), 0.01)
    }

    @Test fun indexStaysInside() {
        assertEquals(0, AnalysisClock.indexAt(-50L, 23.22, 10))
        assertEquals(9, AnalysisClock.indexAt(10_000_000L, 23.22, 10))
        assertEquals(0, AnalysisClock.indexAt(100L, 23.22, 0))
    }
}
