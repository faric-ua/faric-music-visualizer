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
}
