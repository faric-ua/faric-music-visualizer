package com.saney.musicvisualizer.board

data class BoardAudioState(
    val amplitude: Float,
    val bass: Float,
    val mid: Float,
    val high: Float,
    val beat: Float,
)

data class BoardLayerReaction(
    val baseScale: Float = 1f,
    val amplitudeScale: Float = 0f,
    val bassScale: Float = 0f,
    val midScale: Float = 0f,
    val highScale: Float = 0f,
    val beatScale: Float = 0f,
    val baseAlpha: Float = 1f,
    val amplitudeAlpha: Float = 0f,
    val bassAlpha: Float = 0f,
    val midAlpha: Float = 0f,
    val highAlpha: Float = 0f,
    val beatAlpha: Float = 0f,
    val rotationDegPerSecond: Float = 0f,
    val beatRotationDeg: Float = 0f,
    val beatTranslateYFraction: Float = 0f,
)

data class BoardLayerMotion(
    val scale: Float,
    val alpha: Float,
    val rotationDegrees: Float,
    val translateYFraction: Float,
)

object BoardLayerMotionEvaluator {
    fun evaluate(
        reaction: BoardLayerReaction,
        audio: BoardAudioState,
        timeSeconds: Float,
    ): BoardLayerMotion {
        val scale =
            reaction.baseScale +
                audio.amplitude * reaction.amplitudeScale +
                audio.bass * reaction.bassScale +
                audio.mid * reaction.midScale +
                audio.high * reaction.highScale +
                audio.beat * reaction.beatScale

        val alpha =
            reaction.baseAlpha +
                audio.amplitude * reaction.amplitudeAlpha +
                audio.bass * reaction.bassAlpha +
                audio.mid * reaction.midAlpha +
                audio.high * reaction.highAlpha +
                audio.beat * reaction.beatAlpha

        return BoardLayerMotion(
            scale = scale.coerceAtLeast(0f),
            alpha = alpha.coerceIn(0f, 1f),
            rotationDegrees =
                timeSeconds * reaction.rotationDegPerSecond +
                    audio.beat * reaction.beatRotationDeg,
            translateYFraction =
                audio.beat * reaction.beatTranslateYFraction,
        )
    }
}
