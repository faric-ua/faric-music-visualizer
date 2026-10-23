package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportProgressTest {
    @Test fun stagesTakeTheirShareOfTime() {
        assertEquals(0f, ExportProgress.render(0, 6719), 1e-3f)
        assertEquals(45.5f, ExportProgress.render(6719 / 2, 6719), 0.1f)
        assertEquals(91f, ExportProgress.render(6719, 6719), 1e-3f)
        assertEquals(94f, ExportProgress.audio(0.5f), 1e-3f)
        assertEquals(97f, ExportProgress.audio(2f), 1e-3f)
        assertEquals(99.5f, ExportProgress.mux(1f), 1e-3f)
        assertEquals("Рендерю відео", ExportProgress.stage(90.9f))
        assertEquals("Кодую аудіо", ExportProgress.stage(93f))
        assertEquals("Збираю та зберігаю MP4", ExportProgress.stage(98f))
        assertEquals("MP4 успішно збережено", ExportProgress.stage(100f))
    }

    @Test fun timeLeft() {
        assertNull(ExportProgress.remaining(60_000, 2f))
        assertNull(ExportProgress.remaining(2_000, 50f))
        // half done in 4 min → ~4 min left
        assertEquals("лишилось ~4 хв", ExportProgress.remaining(240_000, 50f))
        // PCM took the first 10 %: 40 % of own work in 100 s → 50 % left ≈ 125 s
        assertEquals("лишилось ~2 хв 10 с", ExportProgress.remaining(100_000, 50f, startPercent = 10f))
        assertEquals("лишилось ~10 с", ExportProgress.remaining(90_000, 90f))
        assertEquals("12 хв", ExportProgress.duration(725))
    }
}
