package com.saney.musicvisualizer.export

/**
 * Pure rules of the folder batch export (TODO.md V22, v0.19.73).
 * No android.* here, so everything is covered by JVM tests.
 */
object BatchPlan {
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "opus", "wma", "amr", "3gp", "mka")
    private val ILLEGAL = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private val SPACES = Regex("\\s+")
    const val MAX_NAME_CHARS = 120

    /**
     * Measured on the owner's A26 (EXPORT_ETALON.md): ~1.8–2.4× the song length for
     * rendering, plus audio and mux. Used only for the «приблизно N хв» estimate.
     */
    const val TIME_FACTOR = 2.2

    fun isAudio(
        displayName: String,
        mimeType: String?,
    ): Boolean {
        if (mimeType != null && mimeType.startsWith("audio/")) return true
        val ext = displayName.substringAfterLast('.', "").lowercase()
        return ext in AUDIO_EXTENSIONS
    }

    /** Song file name → safe MP4 name: «01 - Song.mp3» → «01 - Song.mp4». */
    fun videoFileName(songDisplayName: String): String {
        val base = songDisplayName.substringBeforeLast('.').ifBlank { songDisplayName }
        return clean(base, fallback = "track") + ".mp4"
    }

    /** Folder name → Movies/PulseDeck/<folder>/ (MediaStore relative path, trailing slash). */
    fun relativePath(folderName: String): String = ExportFolders.VIDEO + "/" + clean(folderName, fallback = "Batch") + "/"

    /**
     * Batch test (v0.19.74, faric 19.10 20:52: «експорт усіх пісень тільки по 3 секунди»).
     * Each run goes to its own folder, so runs before / after a change can be compared:
     * Movies/PulseDeck/<folder>/Тест 3с 19.10 21-05/
     */
    fun testRelativePath(
        folderName: String,
        stamp: String,
    ): String = relativePath(folderName) + clean("Тест 3с $stamp", fallback = "Тест 3с") + "/"

    const val TEST_CLIP_MS = 3_000L

    /**
     * Which 3 seconds of a song the test takes: the middle of the song (music is
     * usually playing there, unlike the quiet intro). Returns start and length in ms.
     */
    fun testWindow(
        songDurationMs: Long,
        clipMs: Long = TEST_CLIP_MS,
    ): Pair<Long, Long> {
        val duration = songDurationMs.coerceAtLeast(0L)
        if (duration <= clipMs) return 0L to duration.coerceAtLeast(500L)
        val start = (duration / 2 - clipMs / 2).coerceIn(0L, duration - clipMs)
        return start to clipMs
    }

    fun songTitle(songDisplayName: String): String = songDisplayName.substringBeforeLast('.').ifBlank { songDisplayName }.trim()

    /** Natural-ish order: by name, ignoring case, so «01 …», «02 …» stay in album order. */
    fun <T> ordered(
        items: List<T>,
        name: (T) -> String,
    ): List<T> = items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { name(it) })

    fun estimateMinutes(totalAudioMs: Long): Int = Math.ceil(totalAudioMs.coerceAtLeast(0L) * TIME_FACTOR / 60_000.0).toInt()

    private fun clean(
        raw: String,
        fallback: String,
    ): String {
        val cleaned =
            raw.replace(ILLEGAL, " ")
                .replace(SPACES, " ")
                .trim()
                .trim('.')
                .trim()
        return cleaned.take(MAX_NAME_CHARS).trim().ifBlank { fallback }
    }

    enum class Outcome { DONE, SKIPPED_EXISTS, FAILED, CANCELLED }

    data class SongResult(
        val name: String,
        val outcome: Outcome,
        val detail: String = "",
        val elapsedMs: Long = 0L,
    )

    fun summary(
        folder: String,
        results: List<SongResult>,
        total: Int,
    ): String =
        buildString {
            val done = results.count { it.outcome == Outcome.DONE }
            val skipped = results.count { it.outcome == Outcome.SKIPPED_EXISTS }
            val failed = results.count { it.outcome == Outcome.FAILED }
            val notReached = total - results.size
            append("Папка: ").append(folder).append('\n')
            append("Зроблено: ").append(done).append(" з ").append(total)
            if (skipped > 0) append(" · уже були: ").append(skipped)
            if (failed > 0) append(" · помилки: ").append(failed)
            if (notReached > 0) append(" · не дійшли: ").append(notReached)
            append('\n')
            append("Відео: ").append(relativePath(folder)).append('\n')
            results.forEachIndexed { index, r ->
                append('\n').append(index + 1).append(". ")
                append(
                    when (r.outcome) {
                        Outcome.DONE -> "✔ "
                        Outcome.SKIPPED_EXISTS -> "↷ "
                        Outcome.FAILED -> "✖ "
                        Outcome.CANCELLED -> "■ "
                    },
                )
                append(r.name)
                if (r.elapsedMs > 0) append(" · ").append(formatMinutes(r.elapsedMs))
                if (r.detail.isNotBlank()) append(" · ").append(r.detail)
            }
        }

    fun formatMinutes(ms: Long): String {
        val s = (ms.coerceAtLeast(0L) + 500L) / 1000L
        return "%d:%02d".format(s / 60, s % 60)
    }
}
