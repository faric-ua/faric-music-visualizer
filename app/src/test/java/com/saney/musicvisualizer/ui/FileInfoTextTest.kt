package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileInfoTextTest {
    @Test fun sourceLine() {
        assertEquals("Пісня · MP3 · 48 кГц · 320 кбіт/с", FileInfoText.source("MP3", 48_000, 320_000L))
        assertEquals("Пісня · FLAC · 44,1 кГц", FileInfoText.source("FLAC", 44_100, null))
        assertNull(FileInfoText.source(null, 0, 0L))
    }

    @Test fun formats() {
        assertEquals("MP3", FileInfoText.formatOf("audio/mpeg", "a.mp3"))
        assertEquals("FLAC", FileInfoText.formatOf(null, "Song.FLAC"))
        assertEquals("M4A", FileInfoText.formatOf("audio/mp4a-latm", "a.m4a"))
        assertNull(FileInfoText.formatOf(null, null))
    }

    @Test fun videoLine() {
        assertEquals("Відео · 1080×1920 · 30 к/с · 18,7 Мбіт/с · AAC 160 кбіт/с", FileInfoText.video(1080, 1920, 30, 18_662_400L, 160_000L))
    }
}
