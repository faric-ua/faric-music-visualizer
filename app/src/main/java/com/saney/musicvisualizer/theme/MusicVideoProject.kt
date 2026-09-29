package com.saney.musicvisualizer.theme

enum class ExportAspectRatio(
    val width: Int,
    val height: Int,
) {
    VERTICAL_9_16(1080, 1920),
    LANDSCAPE_16_9(1920, 1080),
    SQUARE_1_1(1080, 1080),
    PORTRAIT_4_5(1080, 1350),
}

data class MusicVideoProject(
    val themeId: PlaybackThemeId = PlaybackThemeId.NEON_EMBLEM,
    val aspectRatio: ExportAspectRatio = ExportAspectRatio.VERTICAL_9_16,
    val frameRate: Int = 30,
    val showMetadata: Boolean = true,
    val showTimer: Boolean = true,
    val backgroundIntensity: Float = 1f,
    val reactiveIntensity: Float = 1f,
    val heroIntensity: Float = 1f,
)
