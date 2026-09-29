package com.saney.musicvisualizer.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveBeatDetectorTest {
    @Test
    fun spikeAfterBaselineCreatesBeatAndCooldownBlocksImmediateRepeat() {
        val detector = AdaptiveBeatDetector()
        var now = 1_000L

        repeat(20) {
            detector.update(0.08f, now)
            now += 30L
        }

        val beat =
            detector.update(
                0.55f,
                now,
            )

        val immediateRepeat =
            detector.update(
                0.60f,
                now + 30L,
            )

        assertTrue(beat > 0f)
        assertEquals(
            0f,
            immediateRepeat,
        )
    }

    @Test
    fun broadbandTransientCanTriggerBeatEvenWithoutLargeBassSpike() {
        val detector = AdaptiveBeatDetector()
        var now = 1_000L

        val baseline =
            BandEnergy(
                amplitude = 0.30f,
                bass = 0.28f,
                mid = 0.26f,
                high = 0.22f,
            )

        repeat(20) {
            detector.update(
                baseline,
                now,
            )
            now += 30L
        }

        val transient =
            detector.update(
                BandEnergy(
                    amplitude = 0.78f,
                    bass = 0.30f,
                    mid = 0.82f,
                    high = 0.68f,
                ),
                now,
            )

        assertTrue(transient > 0f)
    }

    @Test
    fun separatedRhythmicHitsContinueToFire() {
        val detector = AdaptiveBeatDetector()
        var now = 1_000L

        val rest =
            BandEnergy(
                amplitude = 0.30f,
                bass = 0.28f,
                mid = 0.25f,
                high = 0.20f,
            )

        val hit =
            BandEnergy(
                amplitude = 0.72f,
                bass = 0.74f,
                mid = 0.48f,
                high = 0.32f,
            )

        repeat(12) {
            detector.update(
                rest,
                now,
            )
            now += 30L
        }

        var fired = 0

        repeat(6) {
            if (
                detector.update(
                    hit,
                    now,
                ) > 0f
            ) {
                fired++
            }

            now += 150L

            detector.update(
                rest,
                now,
            )

            now += 60L
        }

        assertTrue(fired >= 5)
    }
}
