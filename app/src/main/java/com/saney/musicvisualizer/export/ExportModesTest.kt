package com.saney.musicvisualizer.export

/**
 * V218 (faric 04.11 20:14: «пакетний тест від мінімального до максимального розширення, яке телефон підтримує, і від
 * мінімального до максимального кадрів — натиснув, поставив скільки секунд, і воно поперло тестувати»). The same piece
 * of the song in every size × frames a second; what the phone's encoder cannot make is listed as «не вміє» and skipped;
 * at the end one table. Pure — JVM tests; MainActivity runs the exports one after another.
 */
object ExportModesTest {
    val FPS: List<Int> = listOf(30, 60)

    data class Mode(
        val size: VideoSize,
        val fps: Int,
    ) {
        val label: String get() = shortLabel(size) + " · " + fps + " кадр/с"
    }

    data class Row(
        val mode: Mode,
        val width: Int,
        val height: Int,
        /** null — not made (the phone cannot, or it failed: see [note]). */
        val totalMs: Long?,
        val videoMs: Long = 0L,
        val frames: Int = 0,
        val bytes: Long? = null,
        val heat: String? = null,
        val note: String? = null,
    )

    fun shortLabel(size: VideoSize): String =
        when (size) {
            VideoSize.P720 -> "720p"
            VideoSize.P1080 -> "1080p"
            VideoSize.P1440 -> "2K"
            VideoSize.P2160 -> "4K"
        }

    /** Every size from the smallest to the biggest, 30 then 60 for each. */
    fun allModes(): List<Mode> = VideoSize.entries.flatMap { size -> FPS.map { fps -> Mode(size, fps) } }

    /** «~N хв» for [modes] of [seconds] each: sound analysis + roughly how slow each size is per video second. */
    fun estimateMinutes(
        modes: List<Mode>,
        seconds: Int,
    ): Int {
        val work =
            modes.sumOf { mode ->
                val perSecond =
                    when (mode.size) {
                        VideoSize.P720 -> 0.8
                        VideoSize.P1080 -> 1.2
                        VideoSize.P1440 -> 2.0
                        VideoSize.P2160 -> 4.0
                    } * (if (mode.fps > 30) 1.7 else 1.0)
                15.0 + seconds * perSecond
            }
        return ((work + 59) / 60).toInt().coerceAtLeast(1)
    }

    fun fileName(
        index: Int,
        mode: Mode,
    ): String = "%02d — %s %d кадр.mp4".format(index + 1, shortLabel(mode.size), mode.fps)

    fun summary(
        title: String,
        seconds: Int,
        settings: String,
        rows: List<Row>,
    ): String =
        buildString {
            append(title).append('\n')
            append("Шматок: ").append(seconds).append(" с · ").append(settings).append("\n\n")
            append("Режим — час роботи · ×музики · мс/кадр · файл · нагрів\n")
            rows.forEach { row ->
                append(row.mode.label).append(" (").append(row.width).append('×').append(row.height).append(") — ")
                if (row.totalMs == null) {
                    append(row.note ?: "не зроблено")
                } else {
                    append(formatSeconds(row.totalMs))
                    if (row.videoMs > 0) append(" · ").append(BatchPlan.formatRatio(row.totalMs.toDouble() / row.videoMs))
                    if (row.frames > 0) append(" · ").append(row.totalMs / row.frames).append(" мс")
                    row.bytes?.let { append(" · ").append(formatMb(it)) }
                    row.heat?.let { append(" · ").append(it) }
                    row.note?.let { append(" · ").append(it) }
                }
                append('\n')
            }
            val made = rows.filter { it.totalMs != null && it.videoMs > 0 }
            made.minByOrNull { it.totalMs!!.toDouble() / it.videoMs }?.let { fastest ->
                append("\nНайшвидше: ").append(fastest.mode.label)
            }
            made.maxByOrNull { it.totalMs!!.toDouble() / it.videoMs }?.let { slowest ->
                append("\nНайдовше: ").append(slowest.mode.label)
            }
        }.trimEnd()

    fun formatSeconds(ms: Long): String = String.format(java.util.Locale.ROOT, "%.1f с", ms / 1000.0).replace('.', ',')

    fun formatMb(bytes: Long): String = String.format(java.util.Locale.ROOT, "%.1f МБ", bytes / 1_048_576.0).replace('.', ',')
}
