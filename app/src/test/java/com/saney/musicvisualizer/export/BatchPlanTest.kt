package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchPlanTest {
    @Test
    fun audioIsRecognisedByMimeOrExtension() {
        assertTrue(BatchPlan.isAudio("song.MP3", null))
        assertTrue(BatchPlan.isAudio("x.flac", "application/octet-stream"))
        assertTrue(BatchPlan.isAudio("no-extension", "audio/mpeg"))
        assertFalse(BatchPlan.isAudio("cover.jpg", "image/jpeg"))
        assertFalse(BatchPlan.isAudio("video.mp4", "video/mp4"))
    }

    @Test
    fun videoNameKeepsTheSongName() {
        assertEquals("01 - Neon Horizon.mp4", BatchPlan.videoFileName("01 - Neon Horizon.mp3"))
        assertEquals("Пісня faric.mp4", BatchPlan.videoFileName("Пісня faric.flac"))
    }

    @Test
    fun wholeSongVideoIsNamedTrackNumberAndTitle() {
        // V196 (faric 28.10 17:04): «TRACK 01 — назва пісні», not «PulseDeck-Full-…»
        assertEquals("TRACK 01 — IGNITION POINT.mp4", BatchPlan.songVideoFileName("IGNITION POINT", "1/12", "IGNITION POINT.mp3"))
        assertEquals("TRACK 07 — Neon Horizon.mp4", BatchPlan.songVideoFileName("Neon Horizon", "07", "x.flac"))
        // no tags — number and name from the file, the number not twice
        assertEquals("TRACK 03 — Midnight Pulse.mp4", BatchPlan.songVideoFileName(null, null, "03 - Midnight Pulse.mp3"))
        assertEquals("TRACK 03 — Midnight Pulse.mp4", BatchPlan.songVideoFileName("03 - Midnight Pulse", null, "03 - Midnight Pulse.mp3"))
        assertEquals("TRACK 12 — Afterglow.mp4", BatchPlan.songVideoFileName("TRACK 12 — Afterglow", "12", "a.mp3"))
        // no number anywhere — just the song's name
        assertEquals("Пісня faric.mp4", BatchPlan.songVideoFileName("Пісня faric", null, "Пісня faric.flac"))
        assertEquals("Song.mp4", BatchPlan.songVideoFileName(null, "", "Song.wav"))
        // a year-like name is not a track number
        assertEquals("1979.mp4", BatchPlan.songVideoFileName(null, null, "1979.mp3"))
        // the title's own number stays when it is not the track number
        assertEquals("TRACK 05 — 2 Become 1.mp4", BatchPlan.songVideoFileName("2 Become 1", "5", "x.mp3"))
        assertEquals("TRACK 02 — AC DC Back.mp4", BatchPlan.songVideoFileName("AC/DC: Back?", "2/9", "x.mp3"))
        assertEquals(3, BatchPlan.trackNumber("3/12", "x.mp3"))
        assertEquals(null, BatchPlan.trackNumber("0", "x.mp3"))
    }

    @Test
    fun illegalCharactersAreReplaced() {
        assertEquals("AC DC Back.mp4", BatchPlan.videoFileName("AC/DC: Back?.mp3"))
        assertEquals("track.mp4", BatchPlan.videoFileName("???.mp3"))
        assertTrue(BatchPlan.videoFileName("a".repeat(300) + ".mp3").length <= BatchPlan.MAX_NAME_CHARS + 4)
    }

    @Test
    fun outputFolderIsInsideMoviesFaric() {
        assertEquals("Movies/PulseDeck/Suno album 1/", BatchPlan.relativePath("Suno album 1"))
        assertEquals("Movies/PulseDeck/Batch/", BatchPlan.relativePath("  "))
    }

    @Test
    fun testClipIsTakenFromTheMiddle() {
        assertEquals(118_500L to 3_000L, BatchPlan.testWindow(240_000L))
        assertEquals(0L to 2_000L, BatchPlan.testWindow(2_000L))
        assertEquals(0L to 500L, BatchPlan.testWindow(0L))
        val (start, length) = BatchPlan.testWindow(3_100L)
        assertTrue(start + length <= 3_100L)
    }

    @Test
    fun testRunsGoToTheirOwnFolder() {
        assertEquals("Movies/PulseDeck/Album/Тест 3с 19.10 21-05/", BatchPlan.testRelativePath("Album", "19.10 21-05"))
        assertEquals("Movies/PulseDeck/Album/Тест 3с 19.10 21 05/", BatchPlan.testRelativePath("Album", "19.10 21:05"))
        assertEquals("Movies/PulseDeck/Album/Тест 10с 21.10 01-10/", BatchPlan.testRelativePath("Album", "21.10 01-10", seconds = 10))
        assertEquals(10_000L, BatchPlan.testWindow(200_000L, BatchPlan.TEST_CLIP_LONG_MS).second)
    }

    @Test
    fun songsAreOrderedByNameIgnoringCase() {
        val ordered = BatchPlan.ordered(listOf("b.mp3", "A.mp3", "02.mp3", "01.mp3")) { it }
        assertEquals(listOf("01.mp3", "02.mp3", "A.mp3", "b.mp3"), ordered)
    }

    @Test
    fun estimateIsRoughlyTwiceTheMusic() {
        assertEquals(9, BatchPlan.estimateMinutes(4 * 60_000L))
        assertEquals(0, BatchPlan.estimateMinutes(0L))
    }

    @Test
    fun summaryCountsEveryOutcome() {
        val text =
            BatchPlan.summary(
                folder = "Album",
                results =
                    listOf(
                        BatchPlan.SongResult("a", BatchPlan.Outcome.DONE, elapsedMs = 61_000L),
                        BatchPlan.SongResult("b", BatchPlan.Outcome.SKIPPED_EXISTS),
                        BatchPlan.SongResult("c", BatchPlan.Outcome.FAILED, "IllegalStateException: x"),
                    ),
                total = 4,
            )
        assertTrue(text.contains("Зроблено: 1 з 4"))
        assertTrue(text.contains("уже були: 1"))
        assertTrue(text.contains("помилки: 1"))
        assertTrue(text.contains("не дійшли: 1"))
        assertTrue(text.contains("✔ a · 1:01"))
        assertTrue(text.contains("Movies/PulseDeck/Album/"))
    }

    @Test
    fun detailedRunReport() {
        // V201 (faric 30.10 19:59): time against the music, fastest / slowest, warmth, settings
        val text =
            BatchPlan.summary(
                folder = "White Side",
                results =
                    listOf(
                        BatchPlan.SongResult("TRACK 01 — ORBITAL AWAKENING", BatchPlan.Outcome.DONE, elapsedMs = 340_000L, songMs = 312_000L, analysisMs = 54_000L, heatC = 33.4f),
                        BatchPlan.SongResult("BONUS 03 — BEYOND INFINITY", BatchPlan.Outcome.DONE, elapsedMs = 453_000L, songMs = 360_000L, analysisMs = 47_000L, heatC = 35.9f),
                    ),
                total = 2,
                settings = "720×1280 · візуалізація легка 50 %",
                heatStart = "30.1 °C · норма",
                heatEnd = "35.2 °C · теплий",
            )
        assertTrue(text, text.contains("Час: 13:13 на 11:12 музики · ×1,18 · з них аналіз звуку 1:41"))
        assertTrue(text, text.contains("Найшвидше: ×1,09 · найповільніше: ×1,26 (BONUS 03 — BEYOND INFINITY)"))
        assertTrue(text, text.contains("Нагрів: старт 30.1 °C · норма → кінець 35.2 °C · теплий · найгарячіше 35,9 °C"))
        assertTrue(text, text.contains("Як: 720×1280 · візуалізація легка 50 %"))
        assertTrue(text, text.contains("✔ TRACK 01 — ORBITAL AWAKENING · 5:40 на 5:12 · ×1,09 · аналіз 0:54 · 33,4 °C"))
        assertEquals("1:22:24", BatchPlan.formatMinutes(4_944_000L))
    }
}
