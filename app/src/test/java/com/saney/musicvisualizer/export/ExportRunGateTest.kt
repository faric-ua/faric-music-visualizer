package com.saney.musicvisualizer.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportRunGateTest {
    @Test
    fun duplicateStartRejectedUntilCompletion() {
        val gate = ExportRunGate()
        assertTrue(gate.tryStart())
        assertFalse(gate.tryStart())
        assertTrue(gate.isActive())
        gate.finish()
        assertFalse(gate.isActive())
        assertTrue(gate.tryStart())
    }

    @Test
    fun finishIsIdempotent() {
        val gate = ExportRunGate()
        gate.finish()
        assertFalse(gate.isActive())
        assertTrue(gate.tryStart())
        gate.finish()
        gate.finish()
        assertFalse(gate.isActive())
    }
}
