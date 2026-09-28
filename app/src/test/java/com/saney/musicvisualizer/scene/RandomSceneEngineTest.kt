package com.saney.musicvisualizer.scene

import kotlin.random.Random
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomSceneEngineTest {

    @Test
    fun consecutiveScenesDoNotRepeatForegroundOrBackground() {
        val engine = RandomSceneEngine(Random(42), changeAfterMs = 1_000L)
        var previous = engine.next()

        repeat(50) {
            val next = engine.next()
            assertNotEquals(previous.visualizerType, next.visualizerType)
            assertNotEquals(previous.backgroundType, next.backgroundType)
            previous = next
        }
    }

    @Test
    fun intensityStaysWithinExpectedRange() {
        val engine = RandomSceneEngine(Random(7))

        repeat(100) {
            val intensity = engine.next().intensity
            assertTrue(intensity in 0.72f..1.0f)
        }
    }

    @Test
    fun configuredIntervalIsPreserved() {
        val engine = RandomSceneEngine(Random(1), changeAfterMs = 12_345L)
        assertTrue(engine.next().changeAfterMs == 12_345L)
    }
}
