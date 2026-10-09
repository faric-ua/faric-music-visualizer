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
}
