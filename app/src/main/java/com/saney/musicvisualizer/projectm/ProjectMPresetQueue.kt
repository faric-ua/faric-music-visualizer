package com.saney.musicvisualizer.projectm

import java.io.File
import java.util.ArrayDeque
import kotlin.random.Random

class ProjectMPresetQueue(
    pool: List<File>,
    current: File?,
    private val ratingOf: (File) -> ProjectMPresetRating = {
        ProjectMPresetRating.NONE
    },
    private val random: Random = Random.Default,
    private val aheadCount: Int = 3,
) {
    private val candidates =
        pool
            .asSequence()
            .filter {
                it.isFile &&
                    it.extension.equals(
                        "milk",
                        ignoreCase = true,
                    )
            }
            .filter {
                ratingOf(it) != ProjectMPresetRating.HIDDEN
            }
            .distinctBy { it.absolutePath }
            .toList()

    private val next = ArrayDeque<File>()
    private val recent = ArrayDeque<String>()

    val availableCount: Int
        get() = candidates.size

    var current: File? =
        current?.takeIf { candidate ->
            candidates.any {
                it.absolutePath == candidate.absolutePath
            }
        }
            ?: pickWeighted(candidates)
        private set

    init {
        current?.let(::remember)
        refill()
    }

    fun advance(): File? {
        if (candidates.isEmpty()) return null

        if (next.isEmpty()) {
            refill()
        }

        val selected =
            if (next.isNotEmpty()) {
                next.removeFirst()
            } else {
                pickCandidate()
            }

        current = selected
        selected?.let(::remember)
        refill()

        return selected
    }

    fun nextAhead(): List<File> =
        next.toList()

    private fun refill() {
        var attempts = 0
        val maxAttempts = (aheadCount * 20).coerceAtLeast(30)

        while (
            next.size < aheadCount &&
            candidates.isNotEmpty() &&
            attempts < maxAttempts
        ) {
            attempts++

            val candidate =
                pickCandidate()
                    ?: break

            val duplicate =
                next.any {
                    it.absolutePath == candidate.absolutePath
                } ||
                    current?.absolutePath == candidate.absolutePath

            if (!duplicate) {
                next.addLast(candidate)
            }

            if (candidates.size <= next.size + 1) {
                break
            }
        }
    }

    private fun pickCandidate(): File? {
        if (candidates.isEmpty()) return null

        val recentSet = recent.toHashSet()

        val preferred =
            candidates.filterNot {
                it.absolutePath in recentSet
            }

        return pickWeighted(
            if (preferred.isNotEmpty()) {
                preferred
            } else {
                candidates
            },
        )
    }

    private fun pickWeighted(source: List<File>): File? {
        if (source.isEmpty()) return null

        val totalWeight =
            source.sumOf {
                weightFor(ratingOf(it))
            }

        if (totalWeight <= 0) {
            return source[random.nextInt(source.size)]
        }

        var roll = random.nextInt(totalWeight)

        for (file in source) {
            roll -= weightFor(ratingOf(file))

            if (roll < 0) {
                return file
            }
        }

        return source.last()
    }

    private fun weightFor(
        rating: ProjectMPresetRating,
    ): Int =
        when (rating) {
            ProjectMPresetRating.UP -> 6
            ProjectMPresetRating.NONE -> 3
            ProjectMPresetRating.DOWN -> 1
            ProjectMPresetRating.HIDDEN -> 0
        }

    private fun remember(file: File) {
        val path = file.absolutePath

        recent.remove(path)
        recent.addLast(path)

        while (recent.size > RECENT_LIMIT) {
            recent.removeFirst()
        }
    }

    companion object {
        private const val RECENT_LIMIT = 18

        fun warmFiles(files: List<File>) {
            val buffer = ByteArray(32 * 1024)

            for (file in files.take(3)) {
                runCatching {
                    file
                        .inputStream()
                        .buffered()
                        .use { input ->
                            while (input.read(buffer) > 0) {
                                // Read-through intentionally warms filesystem cache.
                            }
                        }
                }
            }
        }
    }
}
