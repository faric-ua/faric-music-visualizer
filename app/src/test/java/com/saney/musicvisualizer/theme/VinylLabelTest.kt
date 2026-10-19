package com.saney.musicvisualizer.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VinylLabelTest {
    @Test
    fun fileNameJunkIsRemoved() {
        assertEquals("Ed Sheeran - Shivers", VinylLabel.cleanTitle("Ed_Sheeran_-_Shivers_(www.muzofan.net).mp3"))
        assertEquals("ed-sheeran-alok-shivers-remix", VinylLabel.cleanTitle("ed-sheeran-alok-shivers-remix(ukrtune.com)"))
        assertTrue(VinylLabel.cleanTitle("Foiz.PRO_Lil Jon _ Eminem - Whispers In The Dark").startsWith("Lil Jon"))
        assertEquals("Neon Heartbeat (3)", VinylLabel.cleanTitle("Neon Heartbeat (3).mp3"))
    }

    @Test
    fun artistSongAndBracketsGoOnTheirOwnLines() {
        assertEquals(listOf("Ed Sheeran", "- Shivers"), VinylLabel.titleLines("Ed_Sheeran_-_Shivers_(www.muzofan.net).mp3"))
        assertEquals(
            listOf("Ed Sheeran", "- Shivers", "(Radio Edit)"),
            VinylLabel.titleLines("Ed Sheeran - Shivers (Radio Edit).flac"),
        )
    }

    @Test
    fun longNamesAreWrappedAndCapped() {
        val lines = VinylLabel.titleLines("Foiz.PRO_Lil Jon _ Eminem - Whispers In The Dark _ D MixX Remix __0.0-197.0")
        assertEquals(3, lines.size)
        assertTrue(lines.all { it.length <= VinylLabel.MAX_CHARS_PER_LINE })
        assertTrue(lines.last().endsWith("…"))
        val oneWord = VinylLabel.titleLines("ed-sheeran-alok-shivers-remix(ukrtune.com)")
        assertTrue(oneWord.all { it.length <= VinylLabel.MAX_CHARS_PER_LINE })
    }

    @Test
    fun bottomLineIsArtistOrVinylNeverFaric() {
        assertEquals("VINYL", VinylLabel.bottomLine(null))
        assertEquals("VINYL", VinylLabel.bottomLine("  "))
        assertEquals("VINYL", VinylLabel.bottomLine("Невідомий виконавець"))
        assertEquals("Ed Sheeran", VinylLabel.bottomLine("Ed Sheeran"))
    }
}
