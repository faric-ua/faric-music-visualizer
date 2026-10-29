package com.saney.musicvisualizer.projectm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetBlendTest {
    @Test fun offByDefaultAndCycles() {
        assertEquals("різко", PresetBlend.label(0))
        assertEquals("плавно 1 с", PresetBlend.label(1000))
        assertEquals("плавно 2 с", PresetBlend.label(2000))
        assertEquals(1000, PresetBlend.next(0))
        assertEquals(2000, PresetBlend.next(1000))
        assertEquals(0, PresetBlend.next(2000))
        assertEquals("unknown saved value → hard cut", 0, PresetBlend.safe(777))
    }

    @Test fun firstFrameOfAVideoIsAlwaysClean() {
        assertNull(PresetBlend.secondsFor(0))
        assertEquals(1.0, PresetBlend.secondsFor(1000)!!, 1e-9)
        assertNull(PresetBlend.secondsFor(2000, firstFrame = true))
    }

    @Test fun exportLevels() {
        assertEquals(ExportProjectMLevel.NORMAL, ExportProjectMLevel.from(-1, oldHigh = false))
        assertEquals("the old «висока» switch is kept", ExportProjectMLevel.HIGH, ExportProjectMLevel.from(-1, oldHigh = true))
        assertEquals(ExportProjectMLevel.LIGHT, ExportProjectMLevel.from(0, oldHigh = true))
        assertEquals(0.5f, ExportProjectMLevel.LIGHT.profile.renderScale, 1e-6f)
        assertEquals(0.78f, ExportProjectMLevel.NORMAL.profile.renderScale, 1e-6f)
        assertEquals(1f, ExportProjectMLevel.HIGH.profile.renderScale, 1e-6f)
        assertEquals("легка 50 %", ExportProjectMLevel.LIGHT.label)
        assertEquals(ExportProjectMLevel.NORMAL, ExportProjectMLevel.LIGHT.next())
        assertEquals(ExportProjectMLevel.LIGHT, ExportProjectMLevel.HIGH.next())
    }
}
