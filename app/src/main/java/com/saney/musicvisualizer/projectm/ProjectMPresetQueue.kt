package com.saney.musicvisualizer.projectm

import java.io.File
import java.util.ArrayDeque
import kotlin.random.Random

class ProjectMPresetQueue(
    pool: List<File>,
    current: File?,
    private val random: Random = Random.Default,
    private val aheadCount: Int = 3,
) {
    private val candidates =
        pool
            .asSequence()
            .filter { it.isFile && it.extension.equals("milk", ignoreCase = true) }
            .distinctBy { it.absolutePath }
            .toList()

    private val next = ArrayDeque<File>()
    private val recent = ArrayDeque<String>()

    var current: File? =
        current?.takeIf { candidate ->
            candidates.any { it.absolutePath == candidate.absolutePath }
        }
        ?: candidates.randomOrNull(random)
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
            if (next.isNotEmpty()) next.removeFirst()
            else pickCandidate()

        current = selected
        selected?.let(::remember)
        refill()
        return selected
    }

    fun nextAhead(): List<File> =
        next.toList()

    private fun refill() {
        while (next.size < aheadCount && candidates.isNotEmpty()) {
            val candidate = pickCandidate() ?: break

            if (
                next.none { it.absolutePath == candidate.absolutePath } &&
                current?.absolutePath != candidate.absolutePath
            ) {
                next.addLast(candidate)
            } else if (candidates.size <= aheadCount + 1) {
                break
            }
        }
    }

    private fun pickCandidate(): File? {
        if (candidates.isEmpty()) return null

        val recentSet = recent.toHashSet()
        val preferred = candidates.filterNot { it.absolutePath in recentSet }

        return when {
            preferred.isNotEmpty() -> preferred[random.nextInt(preferred.size)]
            else -> candidates[random.nextInt(candidates.size)]
        }
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
                    file.inputStream().buffered().use { input ->
                        while (input.read(buffer) > 0) {
                            // Read-through intentionally warms the filesystem page cache.
                        }
                    }
                }
            }
        }
    }
}
