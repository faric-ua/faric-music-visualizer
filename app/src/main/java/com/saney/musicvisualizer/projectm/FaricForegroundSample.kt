package com.saney.musicvisualizer.projectm

enum class FaricForegroundSample(
    val nativeId: Int,
    val label: String,
) {
    PULSE_RAYS(0, "Pulse Rays"),
    ORBIT_RINGS(1, "Orbit Rings"),
    SPECTRUM_HALO(2, "Spectrum Halo"),
    NEON_EMBLEM(3, "Neon Emblem"),
    ENERGY_CORE(4, "Energy Core"),
    ORBITAL_CROWN(5, "Orbital Crown"),
    STAR_SEED(6, "Star Seed"),
    WAVE_IDOL(7, "Wave Idol"),
    ;

    fun next(): FaricForegroundSample {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }
}
