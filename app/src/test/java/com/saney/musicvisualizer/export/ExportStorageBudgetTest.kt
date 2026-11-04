package com.saney.musicvisualizer.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportStorageBudgetTest {
    @Test fun longSongRequiresMoreSpaceThanShortProof() {
        val proof = ExportStorageBudget.estimateRequiredBytes(3_000, 1080, 1920, true)
        val full = ExportStorageBudget.estimateRequiredBytes(240_000, 1080, 1920, true)
        assertTrue(full > proof)
        assertTrue(full > 700L * 1024L * 1024L)
    }

    @Test fun requiringPcmCostsMoreSpace() {
        val one = ExportStorageBudget.estimateRequiredBytes(180_000, 720, 1280, false)
        val pcm = ExportStorageBudget.estimateRequiredBytes(180_000, 720, 1280, true)
        assertTrue(pcm > one)
    }

    @Test fun boundaryCapacityAndUnitDisplay() {
        assertTrue(ExportStorageBudget.hasCapacity(2_000, 2_000))
        assertFalse(ExportStorageBudget.hasCapacity(1_999, 2_000))
        assertFalse(ExportStorageBudget.hasCapacity(0, 0))
        assertEquals(2L, ExportStorageBudget.formatMiB(1_048_577L))
    }

    @Test fun videoBitRateIsHigherButCapped() {
        // V113: 1080×1920 → 9 bits per pixel-second ≈ 18.7 Mbit/s (was 6 → 12.4).
        assertEquals(18_662_400L, ExportStorageBudget.videoBitRate(1080, 1920))
        assertEquals(8_000_000L, ExportStorageBudget.videoBitRate(320, 240))
        assertEquals(60_000_000L, ExportStorageBudget.videoBitRate(3840, 2160)) // V213: big frames — 60 Mbit/s ceiling (was 24)
        // V210: 60 fps — ×1,5 (720p 8,3 → 12,4 Mbit/s; 1080p hits the 24 Mbit/s cap).
        assertEquals(12_441_600L, ExportStorageBudget.videoBitRate(720, 1280, fps = 60))
        assertEquals(24_000_000L, ExportStorageBudget.videoBitRate(1080, 1920, fps = 60)) // 1080p ceiling stays 24
        assertEquals(18_662_400L, ExportStorageBudget.videoBitRate(1080, 1920, fps = 30))
        // V213: 2K / 4K — own ceiling 60 Mbit/s.
        assertEquals(33_177_600L, ExportStorageBudget.videoBitRate(1440, 2560))
        assertEquals(60_000_000L, ExportStorageBudget.videoBitRate(2160, 3840))
    }
}
