package com.saney.musicvisualizer.ui

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * V141 (faric 23.10 00:25–00:35, прев'ю «Г / Д»): an equalizer wrapped around the frame at the top —
 * three layers, each its own: **смужки** (ритм, увесь спектр), **крапки** (високі), **хвиля**
 * (мелодія — середні частоти). Every layer: on / off, colours, density, thickness, reaction; and the
 * whole thing can slowly turn around the tile. Pure Kotlin — EdgeEqTest.
 */
data class EdgeLayer(
    val enabled: Boolean = true,
    val palette: Int = 0,
    /** 1–100: how many bars / dots / wave points around the tile. */
    val density: Int = 60,
    /** 1–100: line / dot thickness. */
    val thickness: Int = 35,
    /** 0–200 %: how strongly it jumps with the music. */
    val reaction: Int = 100,
) {
    fun encode(): String = listOf(if (enabled) 1 else 0, palette, density, thickness, reaction).joinToString(",")

    companion object {
        fun decode(
            text: String?,
            fallback: EdgeLayer,
        ): EdgeLayer {
            val p = text?.split(',')?.map { it.trim().toIntOrNull() } ?: return fallback
            if (p.size != 5 || p.any { it == null }) return fallback
            return EdgeLayer(
                enabled = p[0] == 1,
                palette = p[1]!!.coerceIn(0, EdgePalettes.ALL.lastIndex),
                density = p[2]!!.coerceIn(1, 100),
                thickness = p[3]!!.coerceIn(1, 100),
                reaction = p[4]!!.coerceIn(0, 200),
            )
        }
    }
}

data class EdgeEqSettings(
    val on: Boolean = true,
    /** −100…100: turning around the tile; 100 = one full turn in 10 s, minus = the other way. */
    val scroll: Int = 0,
    val bars: EdgeLayer = DEFAULT_BARS,
    val dots: EdgeLayer = DEFAULT_DOTS,
    val wave: EdgeLayer = DEFAULT_WAVE,
) {
    fun encode(): String = listOf("v1", if (on) "1" else "0", scroll.toString(), bars.encode(), dots.encode(), wave.encode()).joinToString("|")

    companion object {
        val DEFAULT_BARS = EdgeLayer(true, 0, 60, 35, 100)
        val DEFAULT_DOTS = EdgeLayer(true, 4, 50, 30, 100)
        val DEFAULT_WAVE = EdgeLayer(true, 1, 60, 40, 100)
        val DEFAULT = EdgeEqSettings()

        fun decode(text: String?): EdgeEqSettings {
            val p = text?.split('|') ?: return DEFAULT
            if (p.size != 6 || p[0] != "v1") return DEFAULT
            return EdgeEqSettings(
                on = p[1] == "1",
                scroll = (p[2].toIntOrNull() ?: 0).coerceIn(-100, 100),
                bars = EdgeLayer.decode(p[3], DEFAULT_BARS),
                dots = EdgeLayer.decode(p[4], DEFAULT_DOTS),
                wave = EdgeLayer.decode(p[5], DEFAULT_WAVE),
            )
        }
    }
}

/** Colour sets around the tile (ARGB). The first colour repeats at the end, so the ring has no seam. */
object EdgePalettes {
    data class Palette(
        val name: String,
        val colors: IntArray,
    )

    val ALL: List<Palette> =
        listOf(
            // Bottom (bass) blue → top (highs) orange, as in the preview.
            Palette("Лід і вогонь", intArrayOf(0xFF9FA0D0.toInt(), 0xFF40C8FF.toInt(), 0xFF9FA0D0.toInt(), 0xFFFF7838.toInt(), 0xFF9FA0D0.toInt())),
            Palette("Фіолетовий неон", intArrayOf(0xFF8A6CFF.toInt(), 0xFFB98CFF.toInt(), 0xFFE0D2FF.toInt(), 0xFFB98CFF.toInt(), 0xFF8A6CFF.toInt())),
            Palette("Море", intArrayOf(0xFF2EE6C8.toInt(), 0xFF40C8FF.toInt(), 0xFF2A7BFF.toInt(), 0xFF40C8FF.toInt(), 0xFF2EE6C8.toInt())),
            Palette("Вогонь", intArrayOf(0xFFFF3D2E.toInt(), 0xFFFF8A2E.toInt(), 0xFFFFD25A.toInt(), 0xFFFF8A2E.toInt(), 0xFFFF3D2E.toInt())),
            Palette("Іній", intArrayOf(0xFFBFE6FF.toInt(), 0xFFE2F3FF.toInt(), 0xFF9AD4FF.toInt(), 0xFFE2F3FF.toInt(), 0xFFBFE6FF.toInt())),
            Palette(
                "Веселка",
                intArrayOf(
                    0xFFFF4D4D.toInt(),
                    0xFFFFC94D.toInt(),
                    0xFF5CFF7A.toInt(),
                    0xFF4DE1FF.toInt(),
                    0xFF5C6BFF.toInt(),
                    0xFFE15CFF.toInt(),
                    0xFFFF4D4D.toInt(),
                ),
            ),
            Palette("Рожевий і блакитний", intArrayOf(0xFFFF6EC7.toInt(), 0xFF7AD7FF.toInt(), 0xFFFF6EC7.toInt(), 0xFF7AD7FF.toInt(), 0xFFFF6EC7.toInt())),
        )

    fun get(index: Int): Palette = ALL[index.coerceIn(0, ALL.lastIndex)]
}

object EdgeEqMath {
    /** V174: colour steps round the tile (instead of a per-pixel sweep gradient). */
    const val COLOR_BUCKETS = 48

    /**
     * V174: the colour of an evenly spaced, wrapping palette at [t] (0–1 round the tile) — what a
     * SweepGradient with these colours gives at that angle.
     */
    fun colorAt(
        colors: IntArray,
        t: Float,
    ): Int {
        if (colors.isEmpty()) return 0
        if (colors.size == 1) return colors[0]
        val u = (t - kotlin.math.floor(t)) * (colors.size - 1)
        val i = u.toInt().coerceIn(0, colors.size - 2)
        val f = u - i
        val a = colors[i]
        val b = colors[i + 1]
        fun mix(shift: Int): Int {
            val x = (a ushr shift) and 0xFF
            val y = (b ushr shift) and 0xFF
            return (x + (y - x) * f + 0.5f).toInt().coerceIn(0, 255)
        }
        return (mix(24) shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    /** V174: which colour step a point at [t0] (its angle, 0–1) falls into when the ring is turned by [turn]. */
    fun bucket(
        t0: Float,
        turn: Float,
    ): Int {
        val t = t0 - turn
        return ((t - kotlin.math.floor(t)) * COLOR_BUCKETS).toInt().coerceIn(0, COLOR_BUCKETS - 1)
    }

    /** V174: 0–1 angle of (x, y) round (cx, cy), clockwise from the right — as SweepGradient counts. */
    fun angle01(
        x: Float,
        y: Float,
        cx: Float,
        cy: Float,
    ): Float {
        val a = kotlin.math.atan2(y - cy, x - cx) / (2.0 * PI)
        return (if (a < 0) a + 1.0 else a).toFloat()
    }

    /**
     * V154 (faric 23.10 13:20, скріни Розкладки й головного: «квадратик не відповідає габаритам —
     * більше розтягнутий»): the tile itself was the size from «Розкладка», but the equalizer grew
     * OUTWARD from it (up to ~8 % of the screen width), so the whole thing looked bigger and covered
     * the title. Now the equalizer lives inside the frame: the tile is made smaller by this much and
     * every layer stays within it.
     */
    const val REACH = 0.055f

    /** How much the tile gives up to the equalizer (0 when it is off). */
    fun inset(
        settings: EdgeEqSettings,
        unit: Float,
    ): Float = if (settings.on && (settings.bars.enabled || settings.dots.enabled || settings.wave.enabled)) unit * REACH else 0f

    /** How many points around the tile for a density 1–100. */
    fun sampleCount(density: Int): Int = 48 + density.coerceIn(1, 100) * 2

    /**
     * Which spectrum band a point at [u] (0 = bottom centre, going round, 1 = back) listens to:
     * symmetric, bass at the bottom, highs at the top — like the preview. [from]…[to] is the part of
     * the spectrum this layer uses (the wave: the middle — the melody).
     */
    fun band(
        u: Float,
        bands: Int,
        from: Float = 0f,
        to: Float = 1f,
    ): Int {
        val wrapped = ((u % 1f) + 1f) % 1f
        val height = minOf(wrapped, 1f - wrapped) * 2f
        val f = from + (to - from) * height
        return (f * (bands - 1)).toInt().coerceIn(0, bands - 1)
    }

    /** Fast up, slower down — the bars jump with a hit and fall back softly. */
    fun smooth(
        old: Float,
        target: Float,
        fall: Float = 0.15f,
    ): Float = if (target > old) old + (target - old) * 0.6f else old * (1f - fall) + target * fall

    /**
     * V181 (faric 23.10 22:28: «палочки навколо квадрата — щоб реагували так класно, як еквалайзер унизу,
     * а то щось настраюю, а вони ніфіга»): the bars used to grow 0,6 % + 5 % × level and hit the frame's
     * limit already at a medium level — most of the time all of them stood near the top, and «Реакція»
     * changed nothing you could see. Now the same curve as the equalizer under the song: quiet stays
     * short, peaks shoot up (level² × 0,72 + level × 0,28), and the whole room up to the limit is used.
     */
    fun shape(level: Float): Float {
        val x = level.coerceIn(0f, 1f)
        return x * x * 0.72f + x * 0.28f
    }

    /** The turn for the clock: [scroll] 100 → one full turn every 10 s. Result 0…1. */
    fun turn(
        clockMs: Long,
        scroll: Int,
    ): Float {
        if (scroll == 0) return 0f
        val turns = clockMs / 10_000.0 * scroll / 100.0
        return (((turns % 1.0) + 1.0) % 1.0).toFloat()
    }

    /**
     * Points round a rounded rectangle, starting at the bottom centre and going round (left first):
     * x, y, outward normal x, y — [n] points, evenly spaced along the edge.
     */
    fun ring(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        n: Int,
    ): FloatArray {
        val r = radius.coerceIn(0f, minOf(right - left, bottom - top) / 2f)
        val w = right - left - 2f * r
        val h = bottom - top - 2f * r
        val arc = (PI.toFloat() / 2f) * r
        val total = 2f * w + 2f * h + 4f * arc
        val out = FloatArray(n * 4)
        val cx = (left + right) / 2f
        for (i in 0 until n) {
            var d = total * i / n
            var x: Float
            var y: Float
            var nx: Float
            var ny: Float
            // bottom edge, centre → left
            if (d <= w / 2f) {
                x = cx - d; y = bottom; nx = 0f; ny = 1f
            } else {
                d -= w / 2f
                if (d <= arc) {
                    val a = PI / 2 + d / r
                    x = left + r + r * cos(a).toFloat(); y = bottom - r + r * sin(a).toFloat(); nx = cos(a).toFloat(); ny = sin(a).toFloat()
                } else {
                    d -= arc
                    if (d <= h) {
                        x = left; y = bottom - r - d; nx = -1f; ny = 0f
                    } else {
                        d -= h
                        if (d <= arc) {
                            val a = PI + d / r
                            x = left + r + r * cos(a).toFloat(); y = top + r + r * sin(a).toFloat(); nx = cos(a).toFloat(); ny = sin(a).toFloat()
                        } else {
                            d -= arc
                            if (d <= w) {
                                x = left + r + d; y = top; nx = 0f; ny = -1f
                            } else {
                                d -= w
                                if (d <= arc) {
                                    val a = 1.5 * PI + d / r
                                    x = right - r + r * cos(a).toFloat(); y = top + r + r * sin(a).toFloat(); nx = cos(a).toFloat(); ny = sin(a).toFloat()
                                } else {
                                    d -= arc
                                    if (d <= h) {
                                        x = right; y = top + r + d; nx = 1f; ny = 0f
                                    } else {
                                        d -= h
                                        if (d <= arc) {
                                            val a = d / r.toDouble()
                                            x = right - r + r * cos(a).toFloat(); y = bottom - r + r * sin(a).toFloat(); nx = cos(a).toFloat(); ny = sin(a).toFloat()
                                        } else {
                                            d -= arc
                                            x = right - r - d; y = bottom; nx = 0f; ny = 1f
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val len = hypot(nx, ny).takeIf { it > 0f } ?: 1f
            out[i * 4] = x
            out[i * 4 + 1] = y
            out[i * 4 + 2] = nx / len
            out[i * 4 + 3] = ny / len
        }
        return out
    }
}
