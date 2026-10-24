package com.saney.musicvisualizer.ui

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * V191 «Неон-герой» (faric 24.10 14:16 — відео TikTok; макети v1–v4 у чаті 24–25.10; 25.10 02:02 «роби»):
 * a standalone theme — a photo (or dark night) behind, rain, drops with waves that bend the photo, ovals
 * flying in from the edges, and in the middle a rubber «hero» (circle / head / hexagon) with an equalizer
 * and L / R meters inside, an aura on the beat and a pendulum swing towards the side the bass came from.
 *
 * Pure Kotlin (no Android) — tested in `NeonHeroTest`. Drawing — [NeonHeroRenderer].
 */
data class NeonHeroSettings(
    /** 0 circle · 1 head · 2 hexagon. */
    val shape: Int = 0,
    /** 20–100: how tight the rubber is (low — soft and wobbly). */
    val rubber: Int = 55,
    /** 0–150 %: how deep a beat dents the hero. */
    val dent: Int = 100,
    val pendulumOn: Boolean = true,
    val pendulum: Int = 100,
    val auraOn: Boolean = true,
    val aura: Int = 100,
    val eqOn: Boolean = true,
    /** V191 (faric 25.10 02:48: «кота не видно» — відключати героя, підписи L / R, альбом окремо). */
    val heroOn: Boolean = true,
    /** 10–100 %: how see-through the hero is (the photo shows through). */
    val heroAlpha: Int = 100,
    val lrOn: Boolean = true,
    val titleOn: Boolean = true,
    val albumOn: Boolean = true,
    /** The photo behind (content:// from the Android picker) — empty: a dark night. */
    val photoUri: String = "",
    val photoOn: Boolean = true,
    val photoAlpha: Int = 60,
    val fxJump: Boolean = true,
    val fxShake: Boolean = true,
    /** 0–150 %: how hard the strong-beat shake is. */
    val shakePower: Int = 100,
    /**
     * V191 (faric 25.10 02:50: «фото ривками в сторону, вверх, вниз від басів чи ударів… як жива, чи камера
     * трясеться»): 0–100 % — the photo jerks on every beat (down on the bass, to the side the bass leans,
     * up on the highs) and springs back; a little living sway with the bass between beats.
     */
    val photoMove: Int = 40,
    val fxGlitch: Boolean = true,
    val ripplesOn: Boolean = true,
    val rippleCount: Int = 55,
    val rippleSize: Int = 60,
    val rippleSpeed: Int = 45,
    val rippleZoom: Int = 60,
    val rippleRefract: Int = 60,
    val rainOn: Boolean = true,
    val rainCount: Int = 55,
    val rainAlpha: Int = 60,
    val rainSpeed: Int = 100,
    val rainLength: Int = 100,
    val blobsOn: Boolean = true,
    val blobCount: Int = 50,
    val blobAlpha: Int = 70,
) {
    /** «k=v;k=v» — new fields can be added later without breaking what is saved. */
    fun encode(): String =
        listOf(
            "shape" to shape, "rubber" to rubber, "dent" to dent, "pendulumOn" to pendulumOn, "pendulum" to pendulum,
            "auraOn" to auraOn, "aura" to aura, "eqOn" to eqOn, "heroOn" to heroOn, "heroAlpha" to heroAlpha, "lrOn" to lrOn, "titleOn" to titleOn, "albumOn" to albumOn,
            "photoOn" to photoOn, "photoAlpha" to photoAlpha, "fxJump" to fxJump, "fxShake" to fxShake, "shakePower" to shakePower, "photoMove" to photoMove, "fxGlitch" to fxGlitch,
            "ripplesOn" to ripplesOn, "rippleCount" to rippleCount, "rippleSize" to rippleSize, "rippleSpeed" to rippleSpeed,
            "rippleZoom" to rippleZoom, "rippleRefract" to rippleRefract,
            "rainOn" to rainOn, "rainCount" to rainCount, "rainAlpha" to rainAlpha, "rainSpeed" to rainSpeed, "rainLength" to rainLength,
            "blobsOn" to blobsOn, "blobCount" to blobCount, "blobAlpha" to blobAlpha,
        ).joinToString(";") { (k, v) -> "$k=$v" } + ";photoUri=" + photoUri.replace(";", "%3B")

    companion object {
        fun decode(text: String?): NeonHeroSettings {
            if (text.isNullOrBlank()) return NeonHeroSettings()
            val m =
                text.split(';').mapNotNull { part ->
                    val eq = part.indexOf('=')
                    if (eq <= 0) null else part.substring(0, eq) to part.substring(eq + 1)
                }.toMap()
            val d = NeonHeroSettings()
            fun i(k: String, def: Int, lo: Int, hi: Int) = m[k]?.toIntOrNull()?.coerceIn(lo, hi) ?: def
            fun b(k: String, def: Boolean) = m[k]?.toBooleanStrictOrNull() ?: def
            return NeonHeroSettings(
                shape = i("shape", d.shape, 0, 2),
                rubber = i("rubber", d.rubber, 20, 100),
                dent = i("dent", d.dent, 0, 150),
                pendulumOn = b("pendulumOn", d.pendulumOn),
                pendulum = i("pendulum", d.pendulum, 20, 150),
                auraOn = b("auraOn", d.auraOn),
                aura = i("aura", d.aura, 30, 150),
                eqOn = b("eqOn", d.eqOn),
                heroOn = b("heroOn", d.heroOn),
                heroAlpha = i("heroAlpha", d.heroAlpha, 10, 100),
                lrOn = b("lrOn", d.lrOn),
                titleOn = b("titleOn", d.titleOn),
                albumOn = b("albumOn", d.albumOn),
                photoUri = m["photoUri"]?.replace("%3B", ";") ?: "",
                photoOn = b("photoOn", d.photoOn),
                photoAlpha = i("photoAlpha", d.photoAlpha, 0, 100),
                fxJump = b("fxJump", d.fxJump),
                fxShake = b("fxShake", d.fxShake),
                shakePower = i("shakePower", d.shakePower, 0, 150),
                photoMove = i("photoMove", d.photoMove, 0, 100),
                fxGlitch = b("fxGlitch", d.fxGlitch),
                ripplesOn = b("ripplesOn", d.ripplesOn),
                rippleCount = i("rippleCount", d.rippleCount, 10, 100),
                rippleSize = i("rippleSize", d.rippleSize, 20, 100),
                rippleSpeed = i("rippleSpeed", d.rippleSpeed, 20, 100),
                rippleZoom = i("rippleZoom", d.rippleZoom, 0, 100),
                rippleRefract = i("rippleRefract", d.rippleRefract, 0, 100),
                rainOn = b("rainOn", d.rainOn),
                rainCount = i("rainCount", d.rainCount, 10, 100),
                rainAlpha = i("rainAlpha", d.rainAlpha, 10, 100),
                rainSpeed = i("rainSpeed", d.rainSpeed, 20, 150),
                rainLength = i("rainLength", d.rainLength, 30, 200),
                blobsOn = b("blobsOn", d.blobsOn),
                blobCount = i("blobCount", d.blobCount, 10, 100),
                blobAlpha = i("blobAlpha", d.blobAlpha, 10, 100),
            )
        }
    }
}

object NeonHeroMath {
    const val SHAPE_CIRCLE = 0
    const val SHAPE_HEAD = 1
    const val SHAPE_HEX = 2
    val SHAPE_NAMES = listOf("Коло", "Голова з вушками", "Шестикутник")

    /** The hero's outline as a radius multiplier at angle [a] (screen angles: 0 → right, π/2 → down). */
    fun baseRadius(
        shape: Int,
        a: Float,
    ): Float {
        val u = a + PI.toFloat() / 2f // 0 at the top
        return when (shape) {
            SHAPE_HEAD -> {
                // a slightly narrower chin and two ears on the top
                var r = 1f - 0.12f * max(0f, -cos(u)).pow(2)
                r += 0.32f * (max(0f, cos(u - 0.62f)).pow(40) + max(0f, cos(u + 0.62f)).pow(40))
                r
            }
            SHAPE_HEX -> {
                val s = (PI / 3).toFloat()
                val t = ((a % s) + s) % s - s / 2f
                cos(PI.toFloat() / 6f) / cos(t)
            }
            else -> 1f
        }
    }

    /**
     * V191: the photo's jerk on a beat, in screen widths: down on the bass, to the side the bass leans, up when
     * the highs are louder than the bass. [move] 0..1.
     */
    fun photoKick(
        strength: Float,
        stereoPan: Float,
        bass: Float,
        high: Float,
        move: Float,
    ): Pair<Float, Float> {
        val side = pushSide(stereoPan)
        val up = high > bass + 0.1f
        val dx = side * 0.012f * strength * move
        val dy = (if (up) -0.008f else 0.010f) * strength * move
        return dx to dy
    }

    /** Where a beat hits: from the left / right when the bass leans there, else from below. */
    fun hitAngle(stereoPan: Float): Float =
        when {
            stereoPan < -0.15f -> (PI * 0.85).toFloat()
            stereoPan > 0.15f -> (PI * 0.15).toFloat()
            else -> (PI / 2).toFloat()
        }

    /** The pendulum's push: −1 left, +1 right (towards the bass), 0 — straight. */
    fun pushSide(stereoPan: Float): Int =
        when {
            stereoPan < -0.15f -> -1
            stereoPan > 0.15f -> 1
            else -> 0
        }

    /** [bands] levels from a 0..1 spectrum (log bands, low → high); empty spectrum → from bass/mid/high. */
    fun bands(
        spectrum: FloatArray,
        bands: Int,
        bass: Float,
        mid: Float,
        high: Float,
    ): FloatArray {
        val out = FloatArray(bands)
        if (spectrum.isEmpty()) {
            for (i in 0 until bands) {
                val f = i / (bands - 1f).coerceAtLeast(1f)
                out[i] = (bass * max(0f, 1f - f * 2.5f) + mid * exp(-((f - 0.5f) / 0.22f).let { it * it }) + high * max(0f, f * 1.6f - 0.6f)).coerceIn(0f, 1f)
            }
            return out
        }
        for (i in 0 until bands) {
            val from = i * spectrum.size / bands
            val to = max(from + 1, (i + 1) * spectrum.size / bands)
            var m = 0f
            for (j in from until min(to, spectrum.size)) m = max(m, spectrum[j])
            out[i] = m.coerceIn(0f, 1f)
        }
        return out
    }

    /** «Хаотичний» order of the bands across the hero (always the same). */
    val BAND_ORDER = intArrayOf(7, 2, 11, 0, 9, 4, 12, 1, 6, 10, 3, 8, 5)

    /** L / R meter levels from the loudness and the stereo lean. */
    fun meters(
        amplitude: Float,
        stereoPan: Float,
    ): Pair<Float, Float> {
        val a = amplitude.coerceIn(0f, 1f)
        val l = a * (1f - max(0f, stereoPan) * 0.8f)
        val r = a * (1f + min(0f, stereoPan) * 0.8f)
        return l.coerceIn(0f, 1f) to r.coerceIn(0f, 1f)
    }

    /**
     * V191 (faric 25.10 01:36: «кожне слово з нової строчки, по центру; не влазить — окремо внизу»): the words
     * of the title, one per line. Null → too many / too long to fit inside: draw below the hero instead.
     */
    fun titleLines(
        title: String,
        maxLines: Int = 4,
        maxChars: Int = 14,
    ): List<String>? {
        val words = title.trim().split(Regex("\\s+")).filter { it.isNotEmpty() && it != "—" && it != "-" }
        if (words.isEmpty() || words.size > maxLines || words.any { it.length > maxChars }) return null
        return words
    }

    /** A beat starts when the beat strength jumps up (not every frame of a loud part). */
    fun isBeat(
        previous: Float,
        now: Float,
        msSinceLast: Long,
    ): Boolean = now >= 0.32f && now - previous >= 0.12f && msSinceLast >= 140L

    /** Shortest angle between two angles, −π..π. */
    fun angleDiff(
        a: Float,
        b: Float,
    ): Float = atan2(sin(a - b), cos(a - b))
}

/**
 * V191: the rubber edge of the hero — [n] points round it, each pulled back to its place and to its
 * neighbours. A beat pushes the points near the hit inwards and the far side out; the wave runs round,
 * overshoots and settles (faric 25.10 01:26: «ефект резинки… по інерції перетягнулась вгору й зайшла назад»).
 * Offsets are in hero radii.
 */
class RubberRing(val n: Int = 96) {
    val offset = FloatArray(n)
    private val speed = FloatArray(n)

    fun hit(
        angle: Float,
        strength: Float,
    ) {
        for (i in 0 until n) {
            val a = (i.toFloat() / n * 2.0 * PI).toFloat()
            val da = NeonHeroMath.angleDiff(a, angle)
            speed[i] -= strength * exp(-da * da / 0.18f)
            val far = PI.toFloat() - abs(da)
            speed[i] += strength * 0.45f * exp(-far * far / 0.3f)
        }
    }

    /** One 1/60 s step. [rubber] 0.2–1: tight and quick at 1, soft and wobbly at 0.2. */
    fun step(rubber: Float) {
        val k = 0.02f + rubber * 0.10f
        val c = 0.25f
        val damp = 0.86f + rubber * 0.08f
        for (i in 0 until n) {
            val prev = offset[(i + n - 1) % n]
            val next = offset[(i + 1) % n]
            val a = -k * offset[i] + c * (prev + next - 2f * offset[i])
            speed[i] = (speed[i] + a) * damp
        }
        for (i in 0 until n) offset[i] = (offset[i] + speed[i]).coerceIn(-0.45f, 0.45f)
    }

    /** The offset at any angle (between two points — mixed). */
    fun at(angle: Float): Float {
        val f = ((angle / (2.0 * PI)).toFloat() % 1f + 1f) % 1f * n
        val i = f.toInt() % n
        val t = f - f.toInt()
        return offset[i] * (1f - t) + offset[(i + 1) % n] * t
    }

    fun energy(): Float = offset.sumOf { abs(it).toDouble() }.toFloat() + speed.sumOf { abs(it).toDouble() }.toFloat()
}
