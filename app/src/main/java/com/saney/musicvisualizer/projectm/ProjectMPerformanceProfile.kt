package com.saney.musicvisualizer.projectm

data class ProjectMPerformanceProfile(
    val name: String,
    val renderScale: Float,
    val meshX: Int,
    val meshY: Int,
    val targetFps: Int,
    val softCutSeconds: Double,
) {
    companion object {
        val BALANCED_BACKGROUND = ProjectMPerformanceProfile(
            name = "BALANCED",
            renderScale = 0.78f,
            meshX = 72,
            meshY = 40,
            targetFps = 60,
            softCutSeconds = 0.70,
        )

        val QUALITY = ProjectMPerformanceProfile(
            name = "QUALITY",
            renderScale = 1.0f,
            meshX = 96,
            meshY = 54,
            targetFps = 60,
            softCutSeconds = 1.2,
        )

        val ECO = ProjectMPerformanceProfile(
            name = "ECO",
            renderScale = 0.62f,
            meshX = 48,
            meshY = 32,
            targetFps = 30,
            softCutSeconds = 0.45,
        )
    }
}
