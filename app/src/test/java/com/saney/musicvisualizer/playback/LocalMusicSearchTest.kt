package com.saney.musicvisualizer.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMusicSearchTest {
    @Test
    fun searchMatchesTitleArtistAndAlbumCaseInsensitive() {
        assertTrue(LocalMusicSearch.matches("Firestarter", "The Prodigy", "The Fat of the Land", "PRODIGY"))
        assertTrue(LocalMusicSearch.matches("Firestarter", "The Prodigy", "The Fat of the Land", "fire"))
        assertTrue(LocalMusicSearch.matches("Firestarter", "The Prodigy", "The Fat of the Land", "fat of"))
        assertTrue(LocalMusicSearch.matches("Firestarter", "The Prodigy", "The Fat of the Land", "  fire  "))
        assertFalse(LocalMusicSearch.matches("Firestarter", "The Prodigy", "The Fat of the Land", "Daft"))
    }

    @Test
    fun incrementalKeyboardQueryMatchesLatinAndUkrainianWithoutChangingCatalog() {
        val song = LocalMusicTrack(
            id = 101L,
            uri = android.net.Uri.EMPTY,
            title = "Horizon / Горизонт",
            artist = "Night Drive",
            album = "Ambient",
            durationMs = 300_000L,
            folder = "Music/Chill",
            genre = "Electronic",
            year = 2023,
        )
        val catalog = listOf(song)
        for (needle in listOf("H", "HO", "HOR", "HORIZ", "Г", "ГО", "ГОР")) {
            assertTrue(LocalMusicSearch.matches(song, needle))
            assertTrue(catalog.filter { LocalMusicSearch.matches(it, needle) }.size == 1)
        }
        assertFalse(LocalMusicSearch.matches(song, "not-a-song"))
    }

    @Test
    fun blankSearchShowsEntireCatalog() {
        assertTrue(LocalMusicSearch.matches("Song", "Artist", "Album", ""))
        assertTrue(LocalMusicSearch.matches("Song", "Artist", "Album", "  "))
    }
}
