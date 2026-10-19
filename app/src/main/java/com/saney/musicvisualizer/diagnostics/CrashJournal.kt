package com.saney.musicvisualizer.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * V24 — journal of crashes and of how the process ended (TODO.md).
 *
 * Until v0.19.72 the app kept no record at all, so a full-song export that
 * "just closed" could never be explained. Now:
 *  - an uncaught Kotlin/Java exception is written to a file before the process dies;
 *  - the MP4 export leaves a trail (stage, percent, events) while it runs;
 *  - on the next start Android's own ApplicationExitInfo (Android 11+) says why
 *    the process ended: crash, native crash, freeze, low memory, killed…
 *
 * Everything stays in app-private storage; nothing leaves the phone unless the
 * owner taps «Копіювати» and sends it.
 */
object CrashJournal {
    private const val TAG = "FARIC-crash"
    private const val PREFS = "faric_diagnostics"
    private const val DIR = "diagnostics"
    private const val CRASH_FILE = "last-crash.txt"
    private const val LAST_REPORT_FILE = "last-report.txt"
    private const val MAX_CRASH_CHARS = 12_000

    private const val KEY_LAST_SEEN_EXIT_MS = "last_seen_exit_ms"
    private const val KEY_RUNNING = "export_running"
    private const val KEY_FULL = "export_full"
    private const val KEY_PATH = "export_path"
    private const val KEY_TRACK = "export_track"
    private const val KEY_DURATION = "export_duration_ms"
    private const val KEY_STARTED = "export_started_ms"
    private const val KEY_PERCENT = "export_percent"
    private const val KEY_STAGE = "export_stage"
    private const val KEY_UPDATED = "export_updated_ms"
    private const val KEY_NOTES = "export_notes"
    private const val KEY_OUTCOME = "export_outcome"
    private const val NOTE_SEPARATOR = "\u001F"

    @Volatile private var installed = false
    @Volatile private var appVersion = "?"
    private val throttle = ProgressThrottle()

    fun install(context: Context, versionName: String) {
        appVersion = versionName
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { writeCrash(appContext, thread, error) }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    // ---- Export trail ---------------------------------------------------

    fun exportStarted(
        context: Context,
        fullSong: Boolean,
        path: String,
        track: String,
        durationMs: Long,
    ) {
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .putBoolean(KEY_RUNNING, true)
            .putBoolean(KEY_FULL, fullSong)
            .putString(KEY_PATH, path)
            .putString(KEY_TRACK, track)
            .putLong(KEY_DURATION, durationMs)
            .putLong(KEY_STARTED, now)
            .putInt(KEY_PERCENT, 0)
            .putString(KEY_STAGE, "підготовка")
            .putLong(KEY_UPDATED, now)
            .putString(KEY_NOTES, "")
            .remove(KEY_OUTCOME)
            .commit()
        Log.i(TAG, "export trail started full=$fullSong path=$path")
    }

    fun exportProgress(
        context: Context,
        percent: Int,
        stage: String,
    ) {
        val persist = synchronized(throttle) { throttle.shouldPersist(percent, stage) }
        if (!persist) return
        prefs(context).edit()
            .putInt(KEY_PERCENT, percent.coerceIn(0, 100))
            .putString(KEY_STAGE, stage)
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .commit()
    }

    fun exportNote(
        context: Context,
        note: String,
    ) {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_RUNNING, false)) return
        val stamped = timeOnly(System.currentTimeMillis()) + " " + note
        val notes = CrashReport.appendNote(readNotes(prefs.getString(KEY_NOTES, "")), stamped)
        prefs.edit().putString(KEY_NOTES, notes.joinToString(NOTE_SEPARATOR)).commit()
        Log.i(TAG, "export note: $note")
    }

    fun exportFinished(
        context: Context,
        outcome: String,
    ) {
        prefs(context).edit()
            .putBoolean(KEY_RUNNING, false)
            .putString(KEY_OUTCOME, outcome)
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .commit()
        Log.i(TAG, "export trail finished: $outcome")
    }

    // ---- Reports ----------------------------------------------------------

    /**
     * Report for the start-up dialog, or null. Call [markReported] after showing
     * it so the same event is not shown twice.
     */
    fun pendingReport(context: Context): String? {
        val prefs = prefs(context)
        val lastSeen = prefs.getLong(KEY_LAST_SEEN_EXIT_MS, 0L)
        val exits = readExits(context).filter { it.timestampMs > lastSeen }
        val report =
            CrashReport.build(
                appVersion = appVersion,
                exits = exits,
                trail = readTrail(context)?.takeIf { it.running },
                crashText = crashFile(context).takeIf { it.isFile }?.readText(),
                formatTime = ::dateTime,
            )
        if (report != null) runCatching { lastReportFile(context).writeText(report) }
        return report
    }

    fun markReported(context: Context) {
        val newest = readExits(context).maxOfOrNull { it.timestampMs } ?: 0L
        val prefs = prefs(context)
        val editor = prefs.edit()
        if (newest > prefs.getLong(KEY_LAST_SEEN_EXIT_MS, 0L)) editor.putLong(KEY_LAST_SEEN_EXIT_MS, newest)
        if (prefs.getBoolean(KEY_RUNNING, false)) {
            editor.putBoolean(KEY_RUNNING, false)
            editor.putString(KEY_OUTCOME, "обірвався (застосунок закрився)")
        }
        editor.commit()
        val crash = crashFile(context)
        if (crash.isFile) crash.renameTo(File(crash.parentFile, "shown-crash.txt"))
    }

    /** Full journal for «PulseDeck tools → Журнал збоїв». */
    /**
     * Newest first (faric 19.10 23:20: «відкриваю — і зразу перша строчка, що сталось
     * останнє, а не листати кілометри стрічки»): Android closes → code error →
     * last export → the old report that was already shown, at the very bottom.
     */
    fun fullReport(context: Context): String =
        buildString {
            append("FARIC Music Visualizer v").append(appVersion).append(" · журнал збоїв\n")
            append("(найсвіжіше — вгорі)\n")
            val exits = readExits(context)
            append("\n=== Як закривався застосунок (Android, останні ").append(exits.size).append(") ===\n")
            if (exits.isEmpty()) {
                append(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "записів нема\n" else "Android до 11 цього не зберігає\n")
            }
            exits.sortedByDescending { it.timestampMs }.forEach { exit ->
                append(dateTime(exit.timestampMs)).append(" — ").append(ExitReasonText.describe(exit.reason))
                append(" · ").append(ExitReasonText.describeImportance(exit.importance)).append('\n')
            }
            val shown = File(crashFile(context).parentFile, "shown-crash.txt")
            val crash = crashFile(context).takeIf { it.isFile } ?: shown.takeIf { it.isFile }
            crash?.let { append("\n=== Остання помилка в коді ===\n").append(it.readText().trim()).append('\n') }
            readTrail(context)?.let { trail ->
                append("\n=== Останній експорт ===\n")
                append(CrashReport.trailText(trail.copy(outcome = trail.outcome ?: if (trail.running) "ще йде або обірвався" else "—"), ::dateTime))
                append('\n')
            }
            val last = lastReportFile(context)
            if (last.isFile) {
                append("\n=== Раніше показаний звіт (старе) ===\n").append(last.readText().trim()).append('\n')
            }
        }.trimEnd()

    // ---- Internals --------------------------------------------------------

    private fun writeCrash(
        context: Context,
        thread: Thread,
        error: Throwable,
    ) {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val text =
            buildString {
                append(dateTime(System.currentTimeMillis()))
                append(" · потік ").append(thread.name).append('\n')
                append(trace.take(MAX_CRASH_CHARS))
            }
        val file = crashFile(context)
        file.parentFile?.mkdirs()
        file.writeText(text)
        if (prefs(context).getBoolean(KEY_RUNNING, false)) {
            exportNote(context, "падіння: " + (error.javaClass.simpleName + ": " + (error.message ?: "")).take(160))
        }
        Log.e(TAG, "uncaught exception saved", error)
    }

    private fun readExits(context: Context): List<ExitRecord> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val manager = context.getSystemService(ActivityManager::class.java) ?: return emptyList()
        return runCatching {
            manager.getHistoricalProcessExitReasons(null, 0, 8).map { info ->
                ExitRecord(
                    timestampMs = info.timestamp,
                    reason = info.reason,
                    status = info.status,
                    importance = info.importance,
                    description = info.description,
                    pssKb = info.pss,
                    rssKb = info.rss,
                    trace = if (info.reason == ApplicationExitInfo.REASON_ANR) readTrace(info) else null,
                )
            }
        }.getOrElse {
            Log.w(TAG, "exit reasons unavailable", it)
            emptyList()
        }
    }

    private fun readTrace(info: ApplicationExitInfo): String? =
        runCatching {
            info.traceInputStream?.use { stream ->
                val bytes = ByteArray(CrashReport.MAX_TRACE_CHARS)
                val read = stream.read(bytes)
                if (read > 0) String(bytes, 0, read, Charsets.UTF_8) else null
            }
        }.getOrNull()

    private fun readTrail(context: Context): ExportTrail? {
        val prefs = prefs(context)
        if (!prefs.contains(KEY_STARTED)) return null
        return ExportTrail(
            running = prefs.getBoolean(KEY_RUNNING, false),
            fullSong = prefs.getBoolean(KEY_FULL, false),
            path = prefs.getString(KEY_PATH, "?").orEmpty(),
            track = prefs.getString(KEY_TRACK, "").orEmpty(),
            durationMs = prefs.getLong(KEY_DURATION, 0L),
            startedAtMs = prefs.getLong(KEY_STARTED, 0L),
            lastPercent = prefs.getInt(KEY_PERCENT, 0),
            lastStage = prefs.getString(KEY_STAGE, "").orEmpty(),
            updatedAtMs = prefs.getLong(KEY_UPDATED, 0L),
            notes = readNotes(prefs.getString(KEY_NOTES, "")),
            outcome = prefs.getString(KEY_OUTCOME, null),
        )
    }

    private fun readNotes(raw: String?): List<String> =
        raw.orEmpty().split(NOTE_SEPARATOR).filter { it.isNotBlank() }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun crashFile(context: Context) = File(File(context.applicationContext.filesDir, DIR), CRASH_FILE)

    private fun lastReportFile(context: Context): File =
        File(File(context.applicationContext.filesDir, DIR), LAST_REPORT_FILE).also { it.parentFile?.mkdirs() }

    private fun dateTime(ms: Long): String =
        if (ms <= 0L) "—" else SimpleDateFormat("dd.MM HH:mm:ss", Locale.ROOT).format(Date(ms))

    private fun timeOnly(ms: Long): String = SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date(ms))
}
