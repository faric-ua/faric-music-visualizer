package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportPresetScheduleTest {
    private val pool = listOf("a", "b", "c", "d", "e")

    @Test
    fun timerSwitchesInsideTheVideoAndBatchContinues() {
        val s = ExportPresetSchedule.createOrNull(pool, "b", autoEnabled = true, autoSeconds = 5, perSong = false)!!
        s.beginSong()
        assertEquals("b", s.presetAt(0))
        assertEquals("b", s.presetAt(4_999))
        assertEquals("c", s.presetAt(5_000))
        assertEquals("d", s.presetAt(12_000))
        assertEquals("d", s.lastUsed)
        // song 2 starts after the last one song 1 used
        s.beginSong()
        assertEquals("e", s.presetAt(0))
        assertEquals("a", s.presetAt(5_000)) // wraps around the list
        assertEquals("a", s.lastUsed)
    }

    @Test
    fun shortTestClipsStillMoveOnFromSongToSong() {
        val s = ExportPresetSchedule.createOrNull(pool, "a", autoEnabled = true, autoSeconds = 10, perSong = false)!!
        s.beginSong(); assertEquals("a", s.presetAt(2_900))
        s.beginSong(); assertEquals("b", s.presetAt(0))
        s.beginSong(); assertEquals("c", s.presetAt(0))
    }

    @Test
    fun perSongOneVisualizationPerSong() {
        val s = ExportPresetSchedule.createOrNull(pool, "c", autoEnabled = false, autoSeconds = 5, perSong = true)!!
        s.beginSong()
        assertEquals("c", s.presetAt(0))
        assertEquals("c", s.presetAt(120_000))
        s.beginSong()
        assertEquals("d", s.presetAt(60_000))
        assertEquals("d", s.lastUsed)
    }

    @Test
    fun autoOffOrEmptyListMeansNoSwitching() {
        assertNull(ExportPresetSchedule.createOrNull(pool, "a", autoEnabled = false, autoSeconds = 5, perSong = false))
        assertNull(ExportPresetSchedule.createOrNull(emptyList(), "a", autoEnabled = true, autoSeconds = 5, perSong = false))
    }

    @Test
    fun currentNotInListStartsAtTheFirst() {
        val s = ExportPresetSchedule.createOrNull(pool, "zzz", autoEnabled = true, autoSeconds = 5, perSong = false)!!
        s.beginSong()
        assertEquals("a", s.presetAt(0))
    }
}
