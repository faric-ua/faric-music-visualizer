package com.saney.musicvisualizer.playback

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMusicCatalogTest {
    private fun item(
        id: Long,
        album: String = "Release",
        artist: String = "Artist",
        folder: String = "Music/Albums",
        genre: String = "Rock",
        year: Int = 2025,
        trackNumber: Int = 0,
        time: Long = 0L,
    ) = LocalMusicTrack(
        id = id,
        uri = Uri.parse("content://media/external/audio/media/$id"),
        title = "Song $id",
        artist = artist,
        album = album,
        durationMs = id * 1000,
        folder = folder,
        genre = genre,
        year = year,
        trackNumber = trackNumber,
        addedAtSec = time,
    )

    @Test fun groupedCategoriesKeepAllTracksAndCounts() {
        val songs = listOf(item(1), item(2), item(3, album = "Other", folder = "Downloads", genre = "Pop"))
        assertEquals(2, LocalMusicCatalog.groups(LocalMusicCategory.ALBUMS, songs).size)
        assertEquals(2, LocalMusicCatalog.groups(LocalMusicCategory.FOLDERS, songs).size)
        assertEquals(2, LocalMusicCatalog.groups(LocalMusicCategory.GENRES, songs).size)
        assertEquals(1, LocalMusicCatalog.groups(LocalMusicCategory.ARTISTS, songs).size)
        assertEquals(3, LocalMusicCatalog.groups(LocalMusicCategory.ALBUMS, songs).sumOf { it.tracks.size })
    }

    @Test fun yearsDescAndUnknownLast() {
        val songs = listOf(item(1, year = 1990), item(2, year = 2025), item(3, year = 0))
        assertEquals(listOf("2025", "1990", "Рік невідомий"),
            LocalMusicCatalog.groups(LocalMusicCategory.YEARS, songs).map { it.title })
    }

    @Test fun sortByAlbumTrackThenName() {
        val songs = listOf(item(3, trackNumber = 3), item(1, trackNumber = 1), item(2, trackNumber = 2))
        assertEquals(listOf(1L, 2L, 3L), LocalMusicCatalog.sortTracks(songs, LocalMusicSort.ALBUM_ORDER).map { it.id })
    }

    @Test fun favoriteAndRecentFilterIgnoreMissingUris() {
        val songs = listOf(item(1), item(2))
        val two = songs[1].uri.toString()
        val one = songs[0].uri.toString()
        assertEquals(listOf(2L), LocalMusicCatalog.favoriteTracks(songs, setOf(two)).map { it.id })
        assertEquals(listOf(2L, 1L), LocalMusicCatalog.recentTracks(songs, listOf(two, "missing", one)).map { it.id })
    }

    @Test fun folderGenreAndYearSearch() {
        val song = item(1, folder = "Music/DrumAndBass", genre = "Breakbeat", year = 1997)
        assertTrue(LocalMusicSearch.matches(song, "DrumAndBass"))
        assertTrue(LocalMusicSearch.matches(song, "break"))
        assertTrue(LocalMusicSearch.matches(song, "1997"))
    }
}
