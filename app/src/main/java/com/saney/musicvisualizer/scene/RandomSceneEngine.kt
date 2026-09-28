package com.saney.musicvisualizer.scene

import kotlin.random.Random

class RandomSceneEngine(
    private val random: Random = Random.Default,
    private val changeAfterMs: Long = DEFAULT_CHANGE_AFTER_MS,
) {
    private var lastVisualizer: VisualizerType? = null
    private var lastBackground: BackgroundType? = null

    fun next(): SceneSpec {
        val visualizer = pickDifferent(VisualizerType.entries, lastVisualizer)
        val background = pickBackgroundDifferent(lastBackground)
        val palette = AccentPalette.entries[random.nextInt(AccentPalette.entries.size)]
        val intensity = (0.82f + random.nextFloat() * 0.18f).coerceIn(0f, 1f)

        lastVisualizer = visualizer
        lastBackground = background

        return SceneSpec(
            visualizerType = visualizer,
            backgroundType = background,
            palette = palette,
            intensity = intensity,
            changeAfterMs = changeAfterMs,
        )
    }

    private fun pickBackgroundDifferent(previous: BackgroundType?): BackgroundType {
        val weighted = listOf(
            BackgroundType.WARP_STARFIELD,
            BackgroundType.WARP_STARFIELD,
            BackgroundType.WARP_STARFIELD,
            BackgroundType.AURORA,
            BackgroundType.NEON_MIST,
            BackgroundType.NIGHT_GRID,
            BackgroundType.EMBER_CLOUD,
        )
        val candidates = if (previous == null) weighted else weighted.filterNot { it == previous }
        return candidates[random.nextInt(candidates.size)]
    }

    private fun <T> pickDifferent(values: List<T>, previous: T?): T {
        if (values.size <= 1 || previous == null) {
            return values[random.nextInt(values.size)]
        }
        val candidates = values.filterNot { it == previous }
        return candidates[random.nextInt(candidates.size)]
    }

    companion object {
        const val DEFAULT_CHANGE_AFTER_MS = 30_000L
    }
}
