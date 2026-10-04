package com.saney.musicvisualizer.board

enum class BoardLayerId {
    BACKGROUND,
    FRAME,
    FX,
    CREATURE,
    WORDMARK,
}

data class BoardLayerTransform(
    val offsetXFraction: Float = 0f,
    val offsetYFraction: Float = 0f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val opacity: Float = 1f,
) {
    fun sanitized(): BoardLayerTransform =
        copy(
            offsetXFraction =
                offsetXFraction.coerceIn(
                    -0.35f,
                    0.35f,
                ),
            offsetYFraction =
                offsetYFraction.coerceIn(
                    -0.35f,
                    0.35f,
                ),
            scale =
                scale.coerceIn(
                    0.40f,
                    1.80f,
                ),
            rotationDegrees =
                rotationDegrees.coerceIn(
                    -90f,
                    90f,
                ),
            opacity =
                opacity.coerceIn(
                    0f,
                    1f,
                ),
        )

    companion object {
        fun default(): BoardLayerTransform =
            BoardLayerTransform()
    }
}
