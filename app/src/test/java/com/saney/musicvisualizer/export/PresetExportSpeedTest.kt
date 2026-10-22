package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Test

class PresetExportSpeedTest {
    @Test fun marksByAverageFrameTime() {
        assertEquals(PresetSpeedMark.UNKNOWN, PresetExportSpeed.mark(null))
        assertEquals(PresetSpeedMark.UNKNOWN, PresetExportSpeed.mark(PresetSpeed(10, 2000)))
        assertEquals(PresetSpeedMark.OK, PresetExportSpeed.mark(PresetSpeed(300, 9_000)))
        assertEquals(PresetSpeedMark.SLOW, PresetExportSpeed.mark(PresetSpeed(300, 21_000)))
        assertEquals(PresetSpeedMark.TOO_SLOW, PresetExportSpeed.mark(PresetSpeed(300, 30_000)))
    }

    @Test fun codecRoundTripWithOddFileNames() {
        val map =
            mapOf(
                "Dancer/a=b;c.milk" to PresetSpeed(300, 9000),
                "$$$ Royal - Mashup (454).milk" to PresetSpeed(450, 18000),
            )
        assertEquals(map, PresetExportSpeed.decode(PresetExportSpeed.encode(map)))
    }

    @Test fun plusAccumulates() {
        assertEquals(PresetSpeed(600, 30_000), PresetSpeed(300, 9_000).plus(300, 21_000))
        assertEquals(50L, PresetSpeed(600, 30_000).msPerFrame)
    }
}
