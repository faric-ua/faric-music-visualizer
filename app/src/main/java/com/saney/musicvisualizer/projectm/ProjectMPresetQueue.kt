package com.saney.musicvisualizer.projectm

import java.io.File
import java.util.ArrayDeque
import kotlin.random.Random

class ProjectMPresetQueue(
    pool: List<File>,
    current: File?,
    ratingOf: (File) -> ProjectMPresetRating = {
        ProjectMPresetRating.NONE
    },
    private val random: Random = Random.Default,
    private val aheadCount: Int = 3,
) {
    private data class Candidate(
        val file: File,
        var rating: ProjectMPresetRating,
    )

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
            .distinctBy { it.absolutePath }
            .mapNotNull { file ->
                val rating = ratingOf(file)

                if (rating == ProjectMPresetRating.HIDDEN) {
                    null
                } else {
                    Candidate(file, rating)
                }
            }
            .toMutableList()

    private val next = ArrayDeque<Candidate>()
    private val recent = ArrayDeque<String>()

    val availableCount: Int
        get() = candidates.size

    var current: File? =
        current
            ?.takeIf { candidate ->
                candidates.any {
                    it.file.absolutePath == candidate.absolutePath
                }
            }
            ?: pickWeighted(candidates)?.file
        private set

    init {
        current?.let(::remember)
        refill()
    }

    fun advance(): File? {
        if (candidates.isEmpty()) {
            current = null
            return null
        }

        if (next.isEmpty()) {
            refill()
        }

        val selected =
            if (next.isNotEmpty()) {
                next.removeFirst()
            } else {
                pickCandidate()
            }

        current = selected?.file
        current?.let(::remember)
        refill()

        return current
    }

    fun updateRating(
        file: File,
        rating: ProjectMPresetRating,
    ): Boolean {
        val path = file.absolutePath
        val candidate =
            candidates.firstOrNull {
                it.file.absolutePath == path
            }

        if (rating == ProjectMPresetRating.HIDDEN) {
            candidates.removeAll {
                it.file.absolutePath == path
            }

            val retained =
                next.filterNot {
                    it.file.absolutePath == path
                }

            next.clear()
            retained.forEach(next::addLast)

            val hidCurrent =
                current?.absolutePath == path

            if (hidCurrent) {
                current = null
            }

            refill()
            return hidCurrent
        }

        candidate?.rating = rating
        return false
    }

    fun nextAhead(): List<File> =
        next.map { it.file }

    private fun refill() {
        var attempts = 0
        val maxAttempts =
            (aheadCount * 20).coerceAtLeast(30)

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
                    it.file.absolutePath ==
                        candidate.file.absolutePath
                } ||
                    current?.absolutePath ==
                        candidate.file.absolutePath

            if (!duplicate) {
                next.addLast(candidate)
            }

            if (candidates.size <= next.size + 1) {
                break
            }
        }
    }

    private fun pickCandidate(): Candidate? {
        if (candidates.isEmpty()) return null

        val recentSet = recent.toHashSet()

        val preferred =
            candidates.filterNot {
                it.file.absolutePath in recentSet
            }

        return pickWeighted(
            if (preferred.isNotEmpty()) {
                preferred
            } else {
                candidates
            },
        )
    }

    private fun pickWeighted(
        source: List<Candidate>,
    ): Candidate? {
        if (source.isEmpty()) return null

        val totalWeight =
            source.sumOf {
                weightFor(it.rating)
            }

        if (totalWeight <= 0) {
            return source[
                random.nextInt(source.size)
            ]
        }

        var roll = random.nextInt(totalWeight)

        for (candidate in source) {
            roll -= weightFor(candidate.rating)

            if (roll < 0) {
                return candidate
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
                                // Read-through warms filesystem page cache.
                            }
                        }
                }
            }
        }
    }
}
