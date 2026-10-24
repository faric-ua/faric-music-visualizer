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

    /** V188: where «легка» ends and «важка» begins — in ms of a frame's work. */
    data class Thresholds(
        val lightUpTo: Float,
        val mediumUpTo: Float,
    )

    val DEFAULT_THRESHOLDS = Thresholds(12f, 25f)

    /**
     * V188 (скрін faric 24.10 04:26: 0 легких · 20 середніх · 35 важких — при 62 к/с і 28 мс «роботи
     * кадру»; «вони нормальні, а є ще поважчі»): on this phone the frame-work measure runs high for
     * everything, so fixed limits put most into «важкі». The limits now come from the measured ones
     * themselves (those that keep 50+ frames): the lightest third — легкі, the middle third — середні,
     * the rest — важкі. Fewer than 9 measured — the fixed limits.
     */
    fun thresholds(loads: Collection<PresetLoad>): Thresholds {
        val ms = loads.filter { it.fps >= 50f && it.cpu < 30 }.map { it.gpuMs }.sorted()
        if (ms.size < 9) return DEFAULT_THRESHOLDS
        return Thresholds(ms[ms.size / 3 - 1], ms[ms.size * 2 / 3 - 1])
    }

    fun classify(
        load: PresetLoad,
        t: Thresholds,
    ): PresetLoadClass =
        when {
            load.fps < 50f || load.cpu >= 30 -> PresetLoadClass.HEAVY
            load.gpuMs <= t.lightUpTo -> PresetLoadClass.LIGHT
            load.gpuMs <= t.mediumUpTo -> PresetLoadClass.MEDIUM
            else -> PresetLoadClass.HEAVY
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

    private fun allLoads(): List<PresetLoad> = prefs.all.values.mapNotNull { (it as? String)?.let(PresetLoad::decode) }

    /** V188: the class against everything measured so far (thresholds from the measured themselves). */
    fun classOf(file: File): PresetLoadClass? {
        val load = loadOf(file) ?: return null
        return PresetLoadMath.classify(load, PresetLoadMath.thresholds(allLoads()))
    }

    /** V188: many files at once (one read of the thresholds). */
    fun classifier(): (File) -> PresetLoadClass? {
        val t = PresetLoadMath.thresholds(allLoads())
        return { file -> loadOf(file)?.let { PresetLoadMath.classify(it, t) } }
    }

    fun save(
        file: File,
        load: PresetLoad,
    ) {
        prefs.edit().putString(ProjectMLibraryManager.presetId(file), load.encode()).apply()
    }

    fun counts(): Map<PresetLoadClass, Int> {
        val loads = allLoads()
        val t = PresetLoadMath.thresholds(loads)
        return loads.map { PresetLoadMath.classify(it, t) }.groupingBy { it }.eachCount()
    }
}
