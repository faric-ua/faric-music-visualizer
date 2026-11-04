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

    @Test fun twoKAndFourKGrowTheFullHdFrame() {
        assertEquals(1440 to 2560, ExportVideoSize.fit(1080, 1920, VideoSize.P1440))
        assertEquals(2560 to 1440, ExportVideoSize.fit(1920, 1080, VideoSize.P1440))
        assertEquals(2160 to 3840, ExportVideoSize.fit(1080, 1920, VideoSize.P2160))
        assertEquals(3840 to 2160, ExportVideoSize.fit(1920, 1080, VideoSize.P2160))
        assertEquals(2160 to 2160, ExportVideoSize.fit(1080, 1080, VideoSize.P2160))
        assertEquals(2160 to 2700, ExportVideoSize.fit(1080, 1350, VideoSize.P2160))
        assertEquals(2f, ExportVideoSize.scaleOf(1080, 1920, VideoSize.P2160), 1e-6f)
    }

    @Test fun oldSavedChoiceStillWorks() {
        assertEquals(VideoSize.P720, VideoSize.from(null, old720 = true))
        assertEquals(VideoSize.P1080, VideoSize.from(null, old720 = false))
        assertEquals(VideoSize.P2160, VideoSize.from("P2160", old720 = true))
        assertEquals(VideoSize.P720, VideoSize.P2160.next())
        assertEquals(ExportVideoSize.fit(1080, 1920, true), ExportVideoSize.fit(1080, 1920, VideoSize.P720))
    }
}
