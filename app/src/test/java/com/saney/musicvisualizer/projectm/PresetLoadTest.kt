package com.saney.musicvisualizer.projectm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetLoadTest {
    @Test fun classes() {
        assertEquals(PresetLoadClass.LIGHT, PresetLoadMath.classify(60f, 5f, 10))
        assertEquals(PresetLoadClass.LIGHT, PresetLoadMath.classify(61f, 11f, 12))
        // V185: 60 frames and a frame's work of 16–19 ms is fine — medium, not heavy
        assertEquals(PresetLoadClass.MEDIUM, PresetLoadMath.classify(62f, 16.1f, 10))
        assertEquals(PresetLoadClass.MEDIUM, PresetLoadMath.classify(61f, 18.8f, 10))
        assertEquals(PresetLoadClass.MEDIUM, PresetLoadMath.classify(60f, 6f, 22))
        assertEquals(PresetLoadClass.HEAVY, PresetLoadMath.classify(42f, 6f, 10))
        assertEquals(PresetLoadClass.HEAVY, PresetLoadMath.classify(60f, 26f, 10))
        assertEquals(PresetLoadClass.HEAVY, PresetLoadMath.classify(60f, 6f, 31))
    }

    @Test fun limitsFromTheMeasuredOnes() {
        // fewer than 9 — the fixed limits
        assertEquals(PresetLoadMath.DEFAULT_THRESHOLDS, PresetLoadMath.thresholds(listOf(PresetLoad(60f, 20f, 10))))
        val loads = (1..9).map { PresetLoad(62f, 18f + it * 2f, 10) } + PresetLoad(30f, 5f, 10)
        val t = PresetLoadMath.thresholds(loads)
        assertEquals(24f, t.lightUpTo, 1e-4f) // 20 22 24 | 26 28 30 | 32 34 36
        assertEquals(30f, t.mediumUpTo, 1e-4f)
        assertEquals(PresetLoadClass.LIGHT, PresetLoadMath.classify(PresetLoad(62f, 22f, 10), t))
        assertEquals(PresetLoadClass.MEDIUM, PresetLoadMath.classify(PresetLoad(62f, 28.6f, 10), t))
        assertEquals(PresetLoadClass.HEAVY, PresetLoadMath.classify(PresetLoad(62f, 35f, 10), t))
        // dropping frames is always heavy, whatever its ms
        assertEquals(PresetLoadClass.HEAVY, PresetLoadMath.classify(PresetLoad(30f, 5f, 10), t))
    }

    @Test fun codecAndQueue() {
        val l = PresetLoad(58.3f, 7.42f, 13)
        assertEquals("58.3;7.4;13", l.encode())
        assertEquals(PresetLoad(58.3f, 7.4f, 13), PresetLoad.decode(l.encode()))
        assertNull(PresetLoad.decode("bad"))
        val pool = (1..10).toList()
        assertEquals(listOf(2, 4, 6), PresetLoadMath.nextToMeasure(pool, { it % 2 == 1 }, 3))
        assertEquals(2f, PresetLoadMath.average(listOf(1f, 3f)), 1e-6f)
    }
}
