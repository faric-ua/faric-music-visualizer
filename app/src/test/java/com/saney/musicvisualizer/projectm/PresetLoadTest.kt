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
