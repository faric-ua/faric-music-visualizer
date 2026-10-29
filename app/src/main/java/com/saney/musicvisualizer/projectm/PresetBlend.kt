package com.saney.musicvisualizer.projectm

/**
 * V198 (faric 29.10 14:43: «візуалізація переключається дуже різко — хоча б секунду наплива, одна ще не
 * закінчилась, а друга почалась; але щось тестове, щоб не зіпсувати»). Off by default — the old hard cut.
 * projectM blends two visualizations itself («soft cut»); here — only how long. Pure, tested on JVM.
 */
object PresetBlend {
    /** Blend lengths to choose from, ms; 0 = hard cut (as before). */
    val OPTIONS_MS = listOf(0, 1000, 2000)

    fun safe(ms: Int): Int = if (ms in OPTIONS_MS) ms else 0

    fun next(ms: Int): Int = OPTIONS_MS[(OPTIONS_MS.indexOf(safe(ms)) + 1) % OPTIONS_MS.size]

    fun label(ms: Int): String =
        when (safe(ms)) {
            0 -> "різко"
            else -> "плавно ${safe(ms) / 1000} с"
        }

    /** Seconds for projectM, or null — load with a hard cut. The first frame of a video is always hard. */
    fun secondsFor(
        ms: Int,
        firstFrame: Boolean = false,
    ): Double? = if (firstFrame || safe(ms) == 0) null else safe(ms) / 1000.0
}

/**
 * V198 (faric 29.10 14:43: «78 % і 100 % — можна 50 %? Довго конвертується, а TikTok однаково якість ріже»).
 * projectM in the video drawn smaller and stretched: 50 % / 78 % / 100 % of the frame.
 */
enum class ExportProjectMLevel(
    val percent: Int,
    val word: String,
) {
    LIGHT(50, "легка"),
    NORMAL(78, "звичайна"),
    HIGH(100, "висока"),
    ;

    fun next(): ExportProjectMLevel = entries[(ordinal + 1) % entries.size]

    val label: String get() = "$word $percent %"

    val profile: ProjectMPerformanceProfile
        get() =
            when (this) {
                LIGHT -> ProjectMPerformanceProfile.LIGHT_EXPORT
                NORMAL -> ProjectMPerformanceProfile.BALANCED_BACKGROUND
                HIGH -> ProjectMPerformanceProfile.QUALITY
            }

    companion object {
        /** [stored] — the new setting (ordinal) or −1; [oldHigh] — the V103 «висока» switch. */
        fun from(
            stored: Int,
            oldHigh: Boolean,
        ): ExportProjectMLevel = entries.getOrNull(stored) ?: if (oldHigh) HIGH else NORMAL
    }
}
