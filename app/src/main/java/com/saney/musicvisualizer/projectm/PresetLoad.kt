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
     * Heavy: the screen drops frames (< 50 a second) or one frame takes ≥ 14 ms of the 16,7 ms, or the
     * app eats ≥ 30 % of the whole processor. Light: full 57+ frames, frame ≤ 8 ms, CPU < 18 %.
     * Everything between — medium.
     */
    fun classify(
        fps: Float,
        gpuMs: Float,
        cpu: Int,
    ): PresetLoadClass =
        when {
            fps < 50f || gpuMs >= 14f || cpu >= 30 -> PresetLoadClass.HEAVY
            fps >= 57f && gpuMs <= 8f && cpu < 18 -> PresetLoadClass.LIGHT
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
    private val prefs = context.applicationContext.getSharedPreferences("projectm_preset_load", Context.MODE_PRIVATE)

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
