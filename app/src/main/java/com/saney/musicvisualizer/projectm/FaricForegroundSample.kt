package com.saney.musicvisualizer.projectm

enum class FaricForegroundSample(
    val nativeId: Int,
    val label: String,
) {
    PULSE_RAYS(0, "Pulse Rays"),
    ORBIT_RINGS(1, "Orbit Rings"),
    SPECTRUM_HALO(2, "Spectrum Halo"),
    ;

    fun next(): FaricForegroundSample {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }
}
