package com.saney.musicvisualizer.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackTitlesTest {
    @Test
    fun emojiLostInTagComesFromFileName() {
        assertEquals(
            "🔥🔥🔥🔥 TRACK 06 ⚡⚡⚡ SKYBREAKER",
            TrackTitles.pick("???? TRACK 06 ??? SKYBREAKER", "🔥🔥🔥🔥 TRACK 06 ⚡⚡⚡ SKYBREAKER.mp3"),
        )
    }

    @Test
    fun normalTagIsKept() {
        assertEquals("Shivers", TrackTitles.pick("Shivers", "Ed_Sheeran_-_Shivers.mp3"))
        assertEquals("What?", TrackTitles.pick("What?", "What?.mp3"))
    }

    @Test
    fun emptyTagUsesFileNameWithoutExtension() {
        assertEquals("song", TrackTitles.pick("", "song.wav"))
        assertEquals("Без назви", TrackTitles.pick(null, null))
    }
}
