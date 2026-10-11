package com.saney.musicvisualizer.board

/**
 * Independent additive audio response for ONE GF object. Zero preserves the
 * original built-in layer motion and all existing presets unchanged.
 */
data class BoardLayerReactionTuning(
    val rotationSwayDegrees: Float = 0f,
    val stereoShiftFraction: Float = 0f,
    val bassFloatFraction: Float = 0f,
) {
    fun sanitized(): BoardLayerReactionTuning = copy(
        rotationSwayDegrees = rotationSwayDegrees.takeIf { it.isFinite() }?.coerceIn(0f, 6f) ?: 0f,
        stereoShiftFraction = stereoShiftFraction.takeIf { it.isFinite() }?.coerceIn(0f, 0.10f) ?: 0f,
        bassFloatFraction = bassFloatFraction.takeIf { it.isFinite() }?.coerceIn(0f, 0.06f) ?: 0f,
    )

    fun apply(base: BoardLayerReaction): BoardLayerReaction = sanitized().let { value ->
        base.copy(
            rotationSwayDegrees = value.rotationSwayDegrees,
            stereoShiftFraction = value.stereoShiftFraction,
            bassFloatFraction = value.bassFloatFraction,
        )
    }
}
