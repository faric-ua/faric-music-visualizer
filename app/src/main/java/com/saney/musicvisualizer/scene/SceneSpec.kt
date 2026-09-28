package com.saney.musicvisualizer.scene

enum class VisualizerType {
    RADIAL,
    WAVE_RIBBON,
    SPECTRUM_BARS,
}

enum class BackgroundType {
    AURORA,
    NEON_MIST,
    NIGHT_GRID,
    EMBER_CLOUD,
}

enum class AccentPalette {
    SUNSET_CYAN,
    VIOLET_TEAL,
    LIME_MAGENTA,
}

data class SceneSpec(
    val visualizerType: VisualizerType,
    val backgroundType: BackgroundType,
    val palette: AccentPalette,
    val intensity: Float,
    val changeAfterMs: Long,
)
