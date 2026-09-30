package com.saney.musicvisualizer.analysis

data class SceneSignal(
    val amplitude: Float,
    val bass: Float,
    val mid: Float,
    val high: Float,
    val beatStrength: Float,
    val stereoPan: Float = 0f,
)
