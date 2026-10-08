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
    fun blankSearchShowsEntireCatalog() {
        assertTrue(LocalMusicSearch.matches("Song", "Artist", "Album", ""))
        assertTrue(LocalMusicSearch.matches("Song", "Artist", "Album", "  "))
    }
}
