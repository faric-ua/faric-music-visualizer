package com.saney.musicvisualizer.projectm

import android.content.Context
import java.io.File

class ProjectMPresetPerformanceStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            "projectm_preset_performance",
            Context.MODE_PRIVATE,
        )

    fun recordLoad(
        file: File,
        loadMs: Long,
    ) {
        if (loadMs < 0L) return

        val id =
            ProjectMLibraryManager
                .presetId(file)

        val old =
            prefs.getLong(
                key(id),
                -1L,
            )

        val smoothed =
            if (old < 0L) {
                loadMs
            } else {
                (
                    old * 0.65 +
                        loadMs * 0.35
                    )
                    .toLong()
            }

        prefs.edit()
            .putLong(
                key(id),
                smoothed,
            )
            .apply()
    }

    fun estimatedLoadMs(
        file: File,
    ): Long =
        prefs.getLong(
            key(
                ProjectMLibraryManager
                    .presetId(file),
            ),
            -1L,
        )

    fun isHeavy(
        file: File,
    ): Boolean =
        estimatedLoadMs(file) >=
            HEAVY_PRESET_MS

    fun heavyCount(): Int =
        prefs.all.values
            .count { value ->
                (value as? Long)
                    ?.let {
                        it >= HEAVY_PRESET_MS
                    }
                    ?: false
            }

    private fun key(
        id: String,
    ): String =
        "load:$id"

    companion object {
        const val HEAVY_PRESET_MS =
            1_200L
    }
}
