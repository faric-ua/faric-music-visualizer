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

    @Test fun displayPrefersTagThenFileNameWithoutExtension() {
        assertEquals("IGNITION POINT", TrackTitles.display("IGNITION POINT", "IGNITION POINT.mp3"))
        assertEquals("IGNITION POINT", TrackTitles.display(null, "IGNITION POINT.mp3"))
        assertEquals("VOL. 1 — MIDNIGHT PULSE (2)", TrackTitles.display("", "VOL. 1 — MIDNIGHT PULSE (2).FLAC"))
        // no audio extension → other dots stay
        assertEquals("VOL. 1 — MIDNIGHT PULSE", TrackTitles.withoutAudioExtension("VOL. 1 — MIDNIGHT PULSE"))
        assertEquals("Без назви", TrackTitles.display(null, null))
    }
}
