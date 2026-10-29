package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportVideoSizeTest {
    @Test fun fullHdStaysAsBefore() {
        assertEquals(1080 to 1920, ExportVideoSize.fit(1080, 1920))
        assertEquals(1920 to 1080, ExportVideoSize.fit(1920, 1080))
        assertEquals(1080 to 1350, ExportVideoSize.fit(1080, 1350))
        assertEquals(1080 to 1080, ExportVideoSize.fit(1080, 1080))
        assertEquals(1f, ExportVideoSize.scaleOf(1080, 1920, false), 1e-6f)
        // bigger than 1920 — shrunk as before
        assertEquals(1080 to 1920, ExportVideoSize.fit(2160, 3840))
    }

    @Test fun hd720ForEveryFormat() {
        assertEquals(720 to 1280, ExportVideoSize.fit(1080, 1920, true))
        assertEquals(1280 to 720, ExportVideoSize.fit(1920, 1080, true))
        assertEquals(720 to 720, ExportVideoSize.fit(1080, 1080, true))
        assertEquals(720 to 900, ExportVideoSize.fit(1080, 1350, true))
        assertEquals(2f / 3f, ExportVideoSize.scaleOf(1080, 1920, true), 1e-4f)
        assertEquals("already small — not grown", 640 to 360, ExportVideoSize.fit(640, 360, true))
    }
}
