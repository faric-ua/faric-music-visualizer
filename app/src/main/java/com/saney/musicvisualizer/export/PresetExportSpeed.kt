package com.saney.musicvisualizer.export

/**
 * V131 (faric 22.10 03:26: «візуалізація пройшла 10 с нормально — ставимо галочку; якщо на ній
 * 20–30 с — знак питання / оклику, щоб не брати її в пакетне конвертування»).
 * Every export books each visualization's whole-frame time; the app remembers an average per
 * visualization and gives it a mark. Pure Kotlin — tested on JVM.
 */
enum class PresetSpeedMark(
    val sign: String,
    val label: String,
) {
    /** Not measured yet (fewer than [PresetExportSpeed.MIN_FRAMES] frames in videos). */
    UNKNOWN("·", "ще не мірялась"),
    OK("✓", "нормальна"),
    SLOW("?", "повільна"),
    TOO_SLOW("!", "дуже повільна — у пакеті пропускається"),
}

data class PresetSpeed(
    val frames: Int,
    val totalMs: Long,
) {
    val msPerFrame: Long get() = if (frames > 0) totalMs / frames else 0L

    fun plus(
        moreFrames: Int,
        moreMs: Long,
    ): PresetSpeed = PresetSpeed(frames + moreFrames, totalMs + moreMs)
}

object PresetExportSpeed {
    /** One second of video — less is not a fair measure (the first frame loads the visualization). */
    const val MIN_FRAMES = 30

    /** A whole frame up to this is fine (the export runs ~1.2–1.4× the song length). */
    const val OK_MS = 50L

    /** From here the visualization alone makes a song take 3× its length or more. */
    const val TOO_SLOW_MS = 90L

    fun mark(speed: PresetSpeed?): PresetSpeedMark =
        when {
            speed == null || speed.frames < MIN_FRAMES -> PresetSpeedMark.UNKNOWN
            speed.msPerFrame <= OK_MS -> PresetSpeedMark.OK
            speed.msPerFrame < TOO_SLOW_MS -> PresetSpeedMark.SLOW
            else -> PresetSpeedMark.TOO_SLOW
        }

    /** «id=frames,ms;…» for SharedPreferences. Ids are file paths: «=» and «;» are escaped. */
    fun encode(map: Map<String, PresetSpeed>): String =
        map.entries.joinToString(";") { (id, s) -> escape(id) + "=" + s.frames + "," + s.totalMs }

    fun decode(raw: String?): Map<String, PresetSpeed> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(';').mapNotNull { part ->
            val at = part.lastIndexOf('=')
            if (at <= 0) return@mapNotNull null
            val nums = part.substring(at + 1).split(',')
            val frames = nums.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val ms = nums.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
            unescape(part.substring(0, at)) to PresetSpeed(frames, ms)
        }.toMap()
    }

    private fun escape(id: String) = id.replace("%", "%25").replace("=", "%3D").replace(";", "%3B")

    private fun unescape(id: String) = id.replace("%3B", ";").replace("%3D", "=").replace("%25", "%")
}
