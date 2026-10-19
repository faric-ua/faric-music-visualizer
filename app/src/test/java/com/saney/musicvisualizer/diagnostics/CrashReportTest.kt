package com.saney.musicvisualizer.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {
    private val time: (Long) -> String = { "t$it" }

    private fun exit(
        reason: Int,
        ts: Long = 100L,
    ) = ExitRecord(
        timestampMs = ts,
        reason = reason,
        status = 0,
        importance = 100,
        description = null,
        pssKb = 0,
        rssKb = 0,
    )

    private fun trail(running: Boolean) =
        ExportTrail(
            running = running,
            fullSong = true,
            path = "А (повний GPU)",
            track = "Neon Horizon",
            durationMs = 241_000L,
            startedAtMs = 10L,
            lastPercent = 48,
            lastStage = "Рендерю відео · 48%",
            updatedAtMs = 20L,
            notes = listOf("12:00:00 екран сховано під час експорту (onStop)"),
            outcome = null,
        )

    @Test
    fun nothingToReportAfterNormalExitsOnly() {
        val report =
            CrashReport.build(
                appVersion = "0.19.72",
                exits = listOf(exit(ExitReasonText.REASON_PACKAGE_UPDATED), exit(ExitReasonText.REASON_USER_REQUESTED)),
                trail = null,
                crashText = null,
                formatTime = time,
            )
        assertNull(report)
    }

    @Test
    fun routineBackgroundKillsAreNotShownAtStart() {
        // Owner's phone 18.10: Samsung «AL_Kill : over 1 slots» while cached, low memory in background.
        val report =
            CrashReport.build(
                appVersion = "0.19.73",
                exits =
                    listOf(
                        exit(ExitReasonText.REASON_OTHER).copy(importance = 400, description = "AL_Kill : over 1 slots"),
                        exit(ExitReasonText.REASON_LOW_MEMORY).copy(importance = 400),
                    ),
                trail = null,
                crashText = null,
                formatTime = time,
            )
        assertNull(report)
    }

    @Test
    fun killWhilePlayingIsStillShown() {
        val report =
            CrashReport.build(
                appVersion = "0.19.73",
                exits = listOf(exit(ExitReasonText.REASON_SIGNALED).copy(importance = 230, status = 9)),
                trail = null,
                crashText = null,
                formatTime = time,
            )
        assertNotNull(report)
    }

    @Test
    fun nativeCrashIsReportedInPlainWords() {
        val report =
            CrashReport.build(
                appVersion = "0.19.72",
                exits = listOf(exit(ExitReasonText.REASON_CRASH_NATIVE)),
                trail = null,
                crashText = null,
                formatTime = time,
            )
        assertNotNull(report)
        assertTrue(report!!.contains("падіння в C++"))
        assertTrue(report.contains("на екрані"))
    }

    @Test
    fun interruptedExportIsReportedEvenAfterAnOrdinaryExit() {
        val report =
            CrashReport.build(
                appVersion = "0.19.72",
                exits = listOf(exit(ExitReasonText.REASON_USER_REQUESTED)),
                trail = trail(running = true),
                crashText = null,
                formatTime = time,
            )
        assertNotNull(report)
        assertTrue(report!!.contains("ЕКСПОРТ ОБІРВАВСЯ"))
        assertTrue(report.contains("48 %"))
        assertTrue(report.contains("4:01"))
        assertTrue(report.contains("onStop"))
        // The ordinary exit is still shown, because it explains the interruption.
        assertTrue(report.contains("закрито вручну"))
    }

    @Test
    fun kotlinCrashTextIsIncluded() {
        val report =
            CrashReport.build(
                appVersion = "0.19.72",
                exits = emptyList(),
                trail = null,
                crashText = "java.lang.IllegalStateException: boom",
                formatTime = time,
            )
        assertTrue(report!!.contains("ПОМИЛКА В КОДІ"))
        assertTrue(report.contains("boom"))
    }

    @Test
    fun notesKeepOnlyTheNewest() {
        var notes = emptyList<String>()
        repeat(CrashReport.MAX_NOTES + 3) { notes = CrashReport.appendNote(notes, "n$it") }
        assertEquals(CrashReport.MAX_NOTES, notes.size)
        assertEquals("n${CrashReport.MAX_NOTES + 2}", notes.last())
    }

    @Test
    fun progressIsPersistedOncePerTenPercentOrStage() {
        val throttle = ProgressThrottle()
        assertTrue(throttle.shouldPersist(0, "Рендерю відео · 0%"))
        assertFalse(throttle.shouldPersist(5, "Рендерю відео · 5%"))
        assertTrue(throttle.shouldPersist(10, "Рендерю відео · 10%"))
        assertTrue(throttle.shouldPersist(19, "Кодую аудіо · 19%"))
        assertFalse(throttle.shouldPersist(19, "Кодую аудіо · 19%"))
    }

    @Test
    fun everyKnownReasonHasText() {
        (0..16).forEach { reason ->
            assertFalse(ExitReasonText.describe(reason).startsWith("невідома причина (код"))
        }
        assertTrue(ExitReasonText.isNotable(ExitReasonText.REASON_LOW_MEMORY))
        assertFalse(ExitReasonText.isNotable(ExitReasonText.REASON_PACKAGE_UPDATED))
    }

    @Test
    fun durationIsMinutesAndSeconds() {
        assertEquals("0:03", CrashReport.formatDuration(3_000L))
        assertEquals("4:01", CrashReport.formatDuration(241_000L))
    }
}
