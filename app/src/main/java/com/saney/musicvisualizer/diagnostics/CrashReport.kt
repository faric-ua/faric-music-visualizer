package com.saney.musicvisualizer.diagnostics

/** One ApplicationExitInfo record, copied into plain data (JVM-testable). */
data class ExitRecord(
    val timestampMs: Long,
    val reason: Int,
    val status: Int,
    val importance: Int,
    val description: String?,
    val pssKb: Long,
    val rssKb: Long,
    /** First part of the ANR trace, when Android provides a text trace. */
    val trace: String? = null,
)

/**
 * What the MP4 export was doing — saved while it runs, so that after the
 * process dies we still know how far it got.
 */
data class ExportTrail(
    val running: Boolean,
    val fullSong: Boolean,
    val path: String,
    val track: String,
    val durationMs: Long,
    val startedAtMs: Long,
    val lastPercent: Int,
    val lastStage: String,
    val updatedAtMs: Long,
    val notes: List<String>,
    val outcome: String?,
)

/** Decides when export progress is worth writing to disk (cheap, ~12 writes per export). */
class ProgressThrottle {
    private var lastDecile = -1
    private var lastStageKey: String? = null

    fun shouldPersist(
        percent: Int,
        stage: String,
    ): Boolean {
        val decile = percent.coerceIn(0, 100) / 10
        val stageKey = stage.substringBefore(" · ").trim()
        if (decile == lastDecile && stageKey == lastStageKey) return false
        lastDecile = decile
        lastStageKey = stageKey
        return true
    }
}

object CrashReport {
    const val MAX_NOTES = 8
    const val MAX_TRACE_CHARS = 3_000

    fun appendNote(
        notes: List<String>,
        note: String,
    ): List<String> = (notes + note).takeLast(MAX_NOTES)

    /**
     * Builds the text shown on the next start (and copied to Claude), or null
     * when there is nothing worth reporting.
     *
     * Reported when: a Kotlin crash was saved, OR an export was still running
     * when the process ended, OR Android recorded a notable exit (crash, native
     * crash, freeze, low memory…). Exits are expected newest first.
     */
    fun build(
        appVersion: String,
        exits: List<ExitRecord>,
        trail: ExportTrail?,
        crashText: String?,
        formatTime: (Long) -> String,
    ): String? {
        val exportInterrupted = trail?.running == true
        val notable = exits.filter { ExitReasonText.isNotable(it.reason) }
        val shownExits =
            when {
                notable.isNotEmpty() -> notable
                exportInterrupted -> exits.take(1)
                else -> emptyList()
            }
        if (crashText.isNullOrBlank() && !exportInterrupted && shownExits.isEmpty()) return null

        return buildString {
            append("FARIC Music Visualizer v").append(appVersion).append('\n')
            if (trail != null && trail.running) {
                append("\n■ ЕКСПОРТ ОБІРВАВСЯ\n")
                appendTrail(trail, formatTime)
            }
            shownExits.forEachIndexed { index, exit ->
                append(if (index == 0) "\n■ ЯК ЗАКРИВСЯ ЗАСТОСУНОК (каже Android)\n" else "\n· раніше:\n")
                append(formatTime(exit.timestampMs)).append(" — ")
                append(ExitReasonText.describe(exit.reason)).append('\n')
                append("де був: ").append(ExitReasonText.describeImportance(exit.importance))
                if (exit.status != 0) append(" · код ").append(exit.status)
                if (exit.pssKb > 0 || exit.rssKb > 0) {
                    append(" · пам'ять ").append(exit.pssKb / 1024).append('/').append(exit.rssKb / 1024).append(" МБ")
                }
                append('\n')
                exit.description?.takeIf { it.isNotBlank() }?.let { append("опис: ").append(it).append('\n') }
                exit.trace?.takeIf { it.isNotBlank() }?.let {
                    append("trace:\n").append(it.take(MAX_TRACE_CHARS)).append('\n')
                }
            }
            if (!crashText.isNullOrBlank()) {
                append("\n■ ПОМИЛКА В КОДІ\n").append(crashText.trim()).append('\n')
            }
            if (!exportInterrupted && trail != null && trail.outcome != null) {
                append("\n■ ОСТАННІЙ ЕКСПОРТ\n")
                appendTrail(trail, formatTime)
            }
        }.trimEnd()
    }

    /** Plain description of one export run (used by the report and the full journal). */
    fun trailText(
        trail: ExportTrail,
        formatTime: (Long) -> String,
    ): String = buildString { appendTrail(trail, formatTime) }.trimEnd()

    private fun StringBuilder.appendTrail(
        trail: ExportTrail,
        formatTime: (Long) -> String,
    ) {
        append(if (trail.fullSong) "уся пісня" else "тест 3 с")
        append(" · шлях ").append(trail.path)
        if (trail.track.isNotBlank()) append(" · «").append(trail.track).append('»')
        append('\n')
        append("тривалість відео: ").append(formatDuration(trail.durationMs)).append('\n')
        append("почато: ").append(formatTime(trail.startedAtMs)).append('\n')
        append("дійшло до: ").append(trail.lastPercent).append(" % — ").append(trail.lastStage)
        append(" (").append(formatTime(trail.updatedAtMs)).append(")\n")
        trail.outcome?.let { append("результат: ").append(it).append('\n') }
        if (trail.notes.isNotEmpty()) {
            append("події:\n")
            trail.notes.forEach { append("  · ").append(it).append('\n') }
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) + 500L) / 1000L
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
}
