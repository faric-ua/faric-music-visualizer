package com.saney.musicvisualizer.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveBeatDetectorTest {
    @Test
    fun spikeAfterBaselineCreatesBeatAndCooldownBlocksImmediateRepeat() {
        val detector = AdaptiveBeatDetector()
        var now = 1_000L
        repeat(20) { detector.update(0.08f, now); now += 30L }
        val beat = detector.update(0.55f, now)
        val immediateRepeat = detector.update(0.60f, now + 30L)
        assertTrue(beat > 0f)
        assertEquals(0f, immediateRepeat)
    }
}
