package com.saney.musicvisualizer.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSignalDynamicsTest {
    @Test
    fun bassPulseRemainsVisibleAfterLongWarmup() {
        val dynamics = LiveSignalDynamics()

        var quietSum = 0f
        var quietCount = 0
        var pulse = 0f

        repeat(240) { index ->
            val isPulse = index % 20 == 0
            val shaped =
                dynamics.update(
                    BandEnergy(
                        amplitude = if (isPulse) 0.79f else 0.72f,
                        bass = if (isPulse) 0.78f else 0.71f,
                        mid = if (isPulse) 0.63f else 0.60f,
                        high = if (isPulse) 0.52f else 0.50f,
                    ),
                )

            if (index >= 180) {
                if (isPulse) {
                    pulse = maxOf(pulse, shaped.bass)
                } else {
                    quietSum += shaped.bass
                    quietCount++
                }
            }
        }

        val quietAverage = quietSum / quietCount
        assertTrue(pulse > quietAverage + 0.30f)
        assertTrue(pulse > 0.75f)
    }

    @Test
    fun pauseThenResumeRebasesInsteadOfCreatingFullScaleBurst() {
        val dynamics = LiveSignalDynamics()
        val playing =
            BandEnergy(
                amplitude = 0.72f,
                bass = 0.70f,
                mid = 0.58f,
                high = 0.46f,
            )

        repeat(40) {
            dynamics.update(playing)
        }

        repeat(6) {
            val silent =
                dynamics.update(
                    BandEnergy(
                        amplitude = 0f,
                        bass = 0f,
                        mid = 0f,
                        high = 0f,
                    ),
                )

            assertEquals(0f, silent.amplitude)
            assertEquals(0f, silent.bass)
        }

        val resumed = dynamics.update(playing)

        assertTrue(resumed.bass in 0.25f..0.55f)
        assertTrue(resumed.amplitude in 0.25f..0.55f)
    }

    @Test
    fun smallOngoingBassChangeGetsExpanded() {
        val dynamics = LiveSignalDynamics()
        val base =
            BandEnergy(
                amplitude = 0.70f,
                bass = 0.70f,
                mid = 0.55f,
                high = 0.45f,
            )

        repeat(80) {
            dynamics.update(base)
        }

        val before = dynamics.update(base)
        val after =
            dynamics.update(
                base.copy(
                    amplitude = 0.735f,
                    bass = 0.735f,
                ),
            )

        assertTrue(after.bass > before.bass + 0.25f)
    }
}
