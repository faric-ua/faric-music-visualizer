package com.saney.musicvisualizer.projectm

import android.content.Context
import java.io.File
import java.security.MessageDigest

enum class ProjectMPresetRating {
    NONE,
    UP,
    DOWN,
    HIDDEN,
}

class ProjectMPresetRatingsStore(context: Context) {
    private val prefs =
        context.getSharedPreferences("projectm_preset_ratings", Context.MODE_PRIVATE)

    fun ratingFor(file: File): ProjectMPresetRating =
        ratingForId(ProjectMLibraryManager.presetId(file))

    fun setRating(
        file: File,
        rating: ProjectMPresetRating,
    ) {
        val key = ratingKey(ProjectMLibraryManager.presetId(file))

        prefs.edit().apply {
            if (rating == ProjectMPresetRating.NONE) {
                remove(key)
            } else {
                putString(key, rating.name)
            }
        }.apply()
    }

    fun counts(): Map<ProjectMPresetRating, Int> {
        val result =
            ProjectMPresetRating.entries
                .associateWith { 0 }
                .toMutableMap()

        for ((key, value) in prefs.all) {
            if (!key.startsWith(KEY_PREFIX)) continue

            val rating =
                runCatching {
                    ProjectMPresetRating.valueOf(value as String)
                }.getOrNull()
                ?: continue

            result[rating] = (result[rating] ?: 0) + 1
        }

        return result
    }

    private fun ratingForId(id: String): ProjectMPresetRating {
        val stored =
            prefs.getString(
                ratingKey(id),
                null,
            )
            ?: return ProjectMPresetRating.NONE

        return runCatching {
            ProjectMPresetRating.valueOf(stored)
        }.getOrDefault(ProjectMPresetRating.NONE)
    }

    private fun ratingKey(id: String): String =
        KEY_PREFIX + sha256(id)

    private fun sha256(value: String): String {
        val bytes =
            MessageDigest
                .getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }

    companion object {
        private const val KEY_PREFIX = "rating:"
    }
}
