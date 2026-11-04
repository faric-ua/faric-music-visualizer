package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportModesTestTest {
    @Test fun smallestToBiggestThirtyThenSixty() {
        val modes = ExportModesTest.allModes()
        assertEquals(8, modes.size)
        assertEquals(ExportModesTest.Mode(VideoSize.P720, 30), modes.first())
        assertEquals(ExportModesTest.Mode(VideoSize.P2160, 60), modes.last())
        assertEquals("2K · 60 кадр/с", ExportModesTest.Mode(VideoSize.P1440, 60).label)
    }

    @Test fun fileNamesKeepTheOrder() {
        assertEquals("01 — 720p 30 кадр.mp4", ExportModesTest.fileName(0, ExportModesTest.Mode(VideoSize.P720, 30)))
        assertEquals("08 — 4K 60 кадр.mp4", ExportModesTest.fileName(7, ExportModesTest.Mode(VideoSize.P2160, 60)))
    }

    @Test fun tableSaysWhatThePhoneCannot() {
        val text =
            ExportModesTest.summary(
                title = "Тест режимів",
                seconds = 10,
                settings = "9:16",
                rows =
                    listOf(
                        ExportModesTest.Row(ExportModesTest.Mode(VideoSize.P1080, 30), 1080, 1920, totalMs = 7_000, videoMs = 10_000, frames = 300, bytes = 23_068_672, heat = "33 °C"),
                        ExportModesTest.Row(ExportModesTest.Mode(VideoSize.P2160, 60), 2160, 3840, totalMs = null, note = "телефон не вміє"),
                    ),
            )
        assertTrue(text, text.contains("1080p · 30 кадр/с (1080×1920) — 7,0 с · ×0,70 · 23 мс · 22,0 МБ · 33 °C"))
        assertTrue(text, text.contains("4K · 60 кадр/с (2160×3840) — телефон не вміє"))
        assertTrue(text, text.contains("Найшвидше: 1080p · 30 кадр/с"))
    }

    @Test fun estimateGrowsWithSizeAndFrames() {
        val small = ExportModesTest.estimateMinutes(listOf(ExportModesTest.Mode(VideoSize.P720, 30)), 10)
        val big = ExportModesTest.estimateMinutes(listOf(ExportModesTest.Mode(VideoSize.P2160, 60)), 60)
        assertEquals(1, small)
        assertTrue(big > 5)
    }
}
