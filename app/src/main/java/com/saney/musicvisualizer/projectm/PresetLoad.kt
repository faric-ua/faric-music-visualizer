package com.saney.musicvisualizer.projectm

import android.content.Context
import java.io.File
import java.util.Locale

/**
 * V161 (faric 23.10 17:52: «утиліта: беру з 9 тисяч скільки-то, напр. 50, — аналіз навантаження
 * CPU, GPU, кадрів — і групує: легкі, середні, важкі; важкі телефон не тягне — відсікти»).
 */
enum class PresetLoadClass(
    val label: String,
) {
    LIGHT("легка"),
    MEDIUM("середня"),
    HEAVY("важка"),
}

/** One measured visualization: frames per second, one frame on the graphics (ms), CPU of the app (%). */
data class PresetLoad(
    val fps: Float,
    val gpuMs: Float,
    val cpu: Int,
) {
    val loadClass: PresetLoadClass get() = PresetLoadMath.classify(fps, gpuMs, cpu)

    fun encode(): String = String.format(Locale.ROOT, "%.1f;%.1f;%d", fps, gpuMs, cpu)

    companion object {
        fun decode(text: String?): PresetLoad? {
            val p = text?.split(';') ?: return null
            if (p.size != 3) return null
            val fps = p[0].toFloatOrNull() ?: return null
            val gpu = p[1].toFloatOrNull() ?: return null
            val cpu = p[2].toIntOrNull() ?: return null
            return PresetLoad(fps, gpu, cpu)
        }
    }
}

/** Pure rules — PresetLoadTest. */
object PresetLoadMath {
    /**
     * V185 (faric 24.10 03:54: «50 перевірив — 50 важких, а вони нормальні, 60 кадрів, 16 мс»): since
     * V169 the «graphics ms» was capped by the frame rate — ~16 ms for every visualization that keeps
     * 60 frames, and ≥ 14 ms made all of them heavy. Now [gpuMs] is the frame's whole work (not capped):
     * heavy — the screen drops frames (< 50 a second), or a frame's work ≥ 25 ms, or CPU ≥ 30 %;
     * light — 57+ frames, work ≤ 12 ms, CPU < 18 %; the rest — medium.
     */
    fun classify(
        fps: Float,
        gpuMs: Float,
        cpu: Int,
    ): PresetLoadClass =
        when {
            fps < 50f || gpuMs >= 25f || cpu >= 30 -> PresetLoadClass.HEAVY
            fps >= 57f && gpuMs <= 12f && cpu < 18 -> PresetLoadClass.LIGHT
            else -> PresetLoadClass.MEDIUM
        }

    /** The next [count] visualizations of [pool] not measured yet, in the list's order. */
    fun <T> nextToMeasure(
        pool: List<T>,
        measured: (T) -> Boolean,
        count: Int,
    ): List<T> = pool.asSequence().filterNot(measured).take(count.coerceAtLeast(0)).toList()

    fun average(values: List<Float>): Float = if (values.isEmpty()) 0f else values.sum() / values.size
}

/** Saved results: preset id → «fps;gpu;cpu». */
class PresetLoadStore(context: Context) {
    // V185: a new store — the old results (capped ~16 ms, all «heavy») are not comparable.
    private val prefs = context.applicationContext.getSharedPreferences("projectm_preset_load_v2", Context.MODE_PRIVATE)

    fun loadOf(file: File): PresetLoad? = PresetLoad.decode(prefs.getString(ProjectMLibraryManager.presetId(file), null))

    fun classOf(file: File): PresetLoadClass? = loadOf(file)?.loadClass

    fun save(
        file: File,
        load: PresetLoad,
    ) {
        prefs.edit().putString(ProjectMLibraryManager.presetId(file), load.encode()).apply()
    }

    fun counts(): Map<PresetLoadClass, Int> =
        prefs.all.values
            .mapNotNull { (it as? String)?.let(PresetLoad::decode)?.loadClass }
            .groupingBy { it }
            .eachCount()
}
