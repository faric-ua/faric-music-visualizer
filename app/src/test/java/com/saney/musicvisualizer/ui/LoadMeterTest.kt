package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoadMeterTest {
    @Test fun gpuShareOfAFrame() {
        assertEquals(50, LoadMeter.gpuPercent(8.33f))
        assertEquals(100, LoadMeter.gpuPercent(16.67f))
        assertEquals(0, LoadMeter.gpuPercent(-1f))
    }

    @Test fun cpuFromProcStat() {
        val line = "1234 (com.saney.music visualizer) S 1 1234 0 0 -1 4194624 100 0 0 0 250 50 0 0 20 0 30 0 100 0 0"
        assertEquals(300L, LoadMeter.cpuTicks(line))
        assertNull(LoadMeter.cpuTicks("garbage"))
        // 80 ticks of 100 per second in 1 s on 8 cores = 10 %
        assertEquals(10, LoadMeter.cpuPercent(80, 100, 1000, 8))
        assertEquals(0, LoadMeter.cpuPercent(80, 0, 1000, 8))
    }

    @Test fun levels() {
        assertEquals(LoadMeter.Level.LOW, LoadMeter.level(30))
        assertEquals(LoadMeter.Level.MEDIUM, LoadMeter.level(60))
        assertEquals(LoadMeter.Level.HIGH, LoadMeter.level(120))
    }
}
