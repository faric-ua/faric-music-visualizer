package com.saney.musicvisualizer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LastSessionCodecTest {
    @Test
    fun queueSurvivesRoundTrip() {
        val tracks =
            listOf(
                LastSessionCodec.Track("content://media/external/audio/media/42", "Foiz.PRO_Lil Jon _ Eminem - Whispers", "Lil Jon", "Album"),
                LastSessionCodec.Track("content://media/external/audio/media/7", "Пісня faric", null, null),
            )
        assertEquals(tracks, LastSessionCodec.decode(LastSessionCodec.encode(tracks)))
    }

    @Test
    fun brokenOrEmptyTextGivesEmptyQueue() {
        assertTrue(LastSessionCodec.decode(null).isEmpty())
        assertTrue(LastSessionCodec.decode("").isEmpty())
        assertEquals(1, LastSessionCodec.decode("\u001Econtent://x/1\u001F\u001E").size)
    }

    @Test
    fun missingNameFallsBackToUriEnd() {
        assertEquals("song.mp3", LastSessionCodec.decode("file:///Music/song.mp3").single().name)
    }

    @Test
    fun indexAndPositionAreSafe() {
        assertEquals(0, LastSessionCodec.safeIndex(5, 0))
        assertEquals(2, LastSessionCodec.safeIndex(9, 3))
        assertEquals(82_000L, LastSessionCodec.resumePosition(82_000L, 196_000L))
        assertEquals(0L, LastSessionCodec.resumePosition(194_500L, 196_000L))
        assertEquals(0L, LastSessionCodec.resumePosition(-5L, 0L))
    }
}
