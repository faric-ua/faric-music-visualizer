package com.saney.musicvisualizer.projectm

data class ProjectMForegroundTuning(
    val centerScale: Float = 1f,
    val centerRotationDegrees: Float = 0f,
    val centerOpacity: Float = 1f,
    val centerBassGain: Float = 1f,
    val centerMidGain: Float = 1f,
    val centerHighGain: Float = 1f,
    val centerBeatGain: Float = 1f,
    val edgeOpacity: Float = 1f,
    val edgeBassGain: Float = 1f,
    val edgeHighGain: Float = 1f,
    val edgeBeatGain: Float = 1f,
) {
    fun sanitized():
        ProjectMForegroundTuning =
        copy(
            centerScale =
                centerScale.coerceIn(
                    0.50f,
                    1.80f,
                ),
            centerRotationDegrees =
                centerRotationDegrees.coerceIn(
                    -180f,
                    180f,
                ),
            centerOpacity =
                centerOpacity.coerceIn(
                    0f,
                    1f,
                ),
            centerBassGain =
                centerBassGain.coerceIn(
                    0f,
                    2f,
                ),
            centerMidGain =
                centerMidGain.coerceIn(
                    0f,
                    2f,
                ),
            centerHighGain =
                centerHighGain.coerceIn(
                    0f,
                    2f,
                ),
            centerBeatGain =
                centerBeatGain.coerceIn(
                    0f,
                    2f,
                ),
            edgeOpacity =
                edgeOpacity.coerceIn(
                    0f,
                    1f,
                ),
            edgeBassGain =
                edgeBassGain.coerceIn(
                    0f,
                    2f,
                ),
            edgeHighGain =
                edgeHighGain.coerceIn(
                    0f,
                    2f,
                ),
            edgeBeatGain =
                edgeBeatGain.coerceIn(
                    0f,
                    2f,
                ),
        )

    companion object {
        fun default():
            ProjectMForegroundTuning =
            ProjectMForegroundTuning()
    }
}
