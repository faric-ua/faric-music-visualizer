package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import com.saney.musicvisualizer.analysis.SceneSignal
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * V191 «Неон-герой» — drawing (rules and numbers — [NeonHeroMath], [RubberRing]). One renderer per
 * [HeroThemeView]: the screen and the MP4 export draw the same view, so they look the same. The motion
 * steps in fixed 1/60 s steps from the clock it is given (the song time in the video), not from frames.
 * Sizes follow the approved mock-up (340 × 520 px, hero radius 74) scaled to the real screen.
 */
class NeonHeroRenderer(private val context: Context) {
    var settings = NeonHeroSettings()
        private set
    private var settingsVersion = -1

    /** Set by the export: the photo is loaded at once (the video must not start without it). */
    var loadPhotoNow = false

    private val ring = RubberRing(96)
    private var px = 0f
    private var py = 0f
    private var pvx = 0f
    private var pvy = 0f
    private var kick = 0f
    private var glitch = 0f
    private var auraV = 0f
    private var auraS = 0f
    private var auraShape = 0
    private var jump = 0f
    private var shake = 0f
    private var slices = 0
    private var lastStepMs = -1L
    private var lastBeatMs = -10_000L
    private var prevBeat = 0f
    private val rng = Random(191)

    // the latest sound
    private var amplitude = 0f
    private var bass = 0f
    private var mid = 0f
    private var high = 0f
    private var stereoPan = 0f
    private var spectrum = FloatArray(0)
    private val bandLevels = FloatArray(13)
    private var meterL = 0f
    private var meterR = 0f

    private class Drop(var x: Float, var y: Float, var len: Float, var speed: Float)

    private class Blob(var x: Float, var y: Float, var vx: Float, var vy: Float, val r: Float, var life: Float, val rot: Float)

    private class Ripple(val x: Float, val y: Float, var r: Float, var fall: Int, var life: Float)

    private val rain = ArrayList<Drop>()
    private val blobs = ArrayList<Blob>()
    private val ripples = ArrayList<Ripple>()
    private var rainW = 0f
    private var rainH = 0f

    private var photo: Bitmap? = null
    private var photoKey = ""
    private var photoLoading = ""
    private var photoScaled: Bitmap? = null
    private var photoScaledKey = ""

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val photoPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val path = Path()
    private val clip = Path()
    private val src = Rect()
    private val dst = RectF()
    private var rainLines = FloatArray(0)
    private var bgShader: Shader? = null
    private var bgShaderH = 0f
    private val bokeh = List(14) { floatArrayOf(Random(it * 7L + 3).nextFloat(), Random(it * 13L + 5).nextFloat(), 0.02f + Random(it * 3L).nextFloat() * 0.05f, 0.03f + Random(it * 11L).nextFloat() * 0.05f) }

    fun onSignal(
        signal: SceneSignal,
        nowMs: Long,
    ) {
        amplitude = signal.amplitude
        bass = signal.bass
        mid = signal.mid
        high = signal.high
        stereoPan = signal.stereoPan
        spectrum = signal.spectrum
        val b = signal.beatStrength
        if (NeonHeroMath.isBeat(prevBeat, b, nowMs - lastBeatMs)) {
            lastBeatMs = nowMs
            beat(max(b, bass).coerceIn(0f, 1.3f), stereoPan)
        }
        prevBeat = b
    }

    /** A beat of strength [st] (0..1.3), [pan] where the bass leans (−1 left … 1 right). */
    fun beat(
        st: Float,
        pan: Float,
    ) {
        val s = settings
        kick = st
        glitch = if (st > 0.6f) 1f else 0.4f
        auraV = 1f
        auraS = st * s.aura / 100f
        auraShape = if (st > 0.75f) 1 + rng.nextInt(3) else if (st > 0.5f) 4 else 0
        val side = NeonHeroMath.pushSide(pan)
        ring.hit(if (st > 0.75f && side != 0) NeonHeroMath.hitAngle(pan) else (PI / 2).toFloat(), st * s.dent / 100f * 0.19f)
        if (s.pendulumOn) {
            val p = s.pendulum / 100f
            pvx += side * 0.068f * st * p
            pvy += (if (st > 0.75f) -0.04f else 0.0135f) * st * p
        }
        jump = st
        shake = if (st > 0.75f) st else 0f
        slices = if (st > 0.75f) 8 else 0
        if (s.ripplesOn && rainW > 0f) {
            val n = max(1, ((if (st > 0.75f) 3f else 1.4f) * s.rippleCount / 100f * 1.8f).roundToInt())
            repeat(n) { ripples += Ripple(rainW * (0.06f + rng.nextFloat() * 0.88f), rainH * (0.17f + rng.nextFloat() * 0.8f), 0f, 8, 1f) }
            while (ripples.size > 24) ripples.removeAt(0)
        }
        if (s.blobsOn && rainW > 0f) {
            val n = ((if (st > 0.75f) 3f else 1f) * s.blobCount / 100f * 1.6f).roundToInt()
            val u = rainW / 340f
            repeat(n) {
                val (bx, by) =
                    when (rng.nextInt(4)) {
                        0 -> -30f * u to rng.nextFloat() * rainH
                        1 -> rainW + 30f * u to rng.nextFloat() * rainH
                        2 -> rng.nextFloat() * rainW to -30f * u
                        else -> rng.nextFloat() * rainW to rainH + 30f * u
                    }
                val k = 0.022f * (0.6f + rng.nextFloat() * 0.6f)
                blobs += Blob(bx, by, (rainW * 0.5f - bx) * k, (rainH * 0.43f - by) * k, (14f + rng.nextFloat() * 24f * st) * u, 1f, rng.nextFloat() * 6f)
            }
            while (blobs.size > 30) blobs.removeAt(0)
        }
    }

    private fun reloadSettings() {
        if (NeonHeroStore.version == settingsVersion) return
        settingsVersion = NeonHeroStore.version
        settings = NeonHeroStore.load(context)
    }

    /** One 1/60 s of motion. */
    private fun step() {
        val s = settings
        kick *= 0.85f
        glitch *= 0.88f
        auraV *= 0.9f
        jump *= 0.82f
        shake *= 0.8f
        if (slices > 0) slices--
        ring.step(s.rubber / 100f)
        pvx += -px * 0.08f
        pvy += -py * 0.08f
        pvx *= 0.9f
        pvy *= 0.9f
        px += pvx
        py += pvy
        val levels = NeonHeroMath.bands(spectrum, 13, bass, mid, high)
        for (i in 0 until 13) {
            val t = levels[i]
            bandLevels[i] = if (t > bandLevels[i]) bandLevels[i] + (t - bandLevels[i]) * 0.6f else bandLevels[i] * 0.9f
        }
        val (l, r) = NeonHeroMath.meters(max(amplitude, kick * 0.9f), stereoPan)
        meterL = max(meterL * 0.92f, l)
        meterR = max(meterR * 0.92f, r)
        // rain: from the top and the right edge, falling down-left
        val u = rainW / 340f
        val speed = s.rainSpeed / 100f * u
        for (d in rain) {
            val sp = d.speed * speed * (1f + kick * 3f)
            d.x -= sp * 1.1f
            d.y += sp * 1.7f
            if (d.y > rainH + 10f * u || d.x < -20f * u) respawn(d, false)
        }
        val it = blobs.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.x += b.vx
            b.y += b.vy
            b.vx *= 0.95f
            b.vy *= 0.95f
            b.life -= 0.018f
            if (b.life <= 0f) it.remove()
        }
        val maxR = min(rainW, rainH) * (0.088f + s.rippleSize / 100f * 0.38f)
        val grow = rainW * (0.0018f + s.rippleSpeed / 100f * 0.01f)
        val rit = ripples.iterator()
        while (rit.hasNext()) {
            val r = rit.next()
            if (r.fall > 0) {
                r.fall--
                continue
            }
            r.r += grow
            r.life = (1f - r.r / maxR).coerceAtLeast(0f)
            if (r.life <= 0f) rit.remove()
        }
    }

    private fun respawn(
        d: Drop,
        anywhere: Boolean,
    ) {
        val u = rainW / 340f
        val top = rng.nextFloat() < rainW / (rainW + rainH)
        d.x = if (anywhere || top) rng.nextFloat() * (rainW + 140f * u) else rainW + 10f * u
        d.y = if (anywhere) rng.nextFloat() * rainH else if (top) -10f * u else rng.nextFloat() * rainH
        d.len = (5f + rng.nextFloat() * 10f) * u
        d.speed = 1.2f + rng.nextFloat() * 1.6f
    }

    fun draw(
        canvas: Canvas,
        w: Float,
        h: Float,
        nowMs: Long,
        title: String,
        album: String,
        playing: Boolean,
    ) {
        reloadSettings()
        val s = settings
        if (rainW != w || rainH != h) {
            rainW = w
            rainH = h
            val n = (180 * w / 340f).roundToInt().coerceIn(60, 800)
            rain.clear()
            repeat(n) { rain += Drop(0f, 0f, 0f, 0f).also { d -> respawn(d, true) } }
        }
        if (lastStepMs < 0L) lastStepMs = nowMs
        var steps = ((nowMs - lastStepMs) / 16.667f).toInt()
        if (steps > 6 || steps < 0) {
            steps = if (steps < 0) 0 else 6
            lastStepMs = nowMs
        } else {
            lastStepMs += (steps * 16.667f).toLong()
        }
        if (!playing) steps = min(steps, 1)
        repeat(steps) { step() }

        val u = w / 340f
        val minSide = min(w, h)
        val heroR = minSide * 0.25f
        val f = heroR / 74f
        val photoBmp = if (s.photoOn) photoFor(s.photoUri, w, h) else null
        val alpha = s.photoAlpha / 100f

        drawBackground(canvas, w, h, photoBmp, alpha)
        drawRipples(canvas, photoBmp, alpha, u)
        if (s.rainOn) drawRain(canvas, s, u)
        if (s.blobsOn) {
            for (b in blobs) {
                fill.color = Color.argb((b.life * s.blobAlpha / 100f * 255f).toInt().coerceIn(0, 255), 120, 235, 255)
                canvas.save()
                canvas.rotate(b.rot * 57.2958f, b.x, b.y)
                dst.set(b.x - b.r * 1.5f, b.y - b.r * 0.8f, b.x + b.r * 1.5f, b.y + b.r * 0.8f)
                canvas.drawOval(dst, fill)
                canvas.restore()
            }
        }
        val cx = w * 0.5f + px * heroR
        val cy = h * 0.40f + py * heroR
        val k = 1f + kick * 0.04f
        drawHero(canvas, s, cx, cy, heroR * k, f, nowMs / 1000f, title, album)
    }

    private fun drawBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        photoBmp: Bitmap?,
        alpha: Float,
    ) {
        if (bgShader == null || bgShaderH != h) {
            bgShader = LinearGradient(0f, 0f, 0f, h, intArrayOf(Color.rgb(6, 9, 26), Color.rgb(20, 10, 36), Color.rgb(4, 5, 10)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            bgShaderH = h
        }
        fill.shader = bgShader
        canvas.drawRect(0f, 0f, w, h, fill)
        fill.shader = null
        for (b in bokeh) {
            fill.color = Color.argb((b[3] * 255).toInt(), 90, 140, 200)
            canvas.drawCircle(b[0] * w, b[1] * h, b[2] * w, fill)
        }
        if (photoBmp == null) return
        val s = settings
        photoPaint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        canvas.save()
        if (s.fxShake && shake > 0.01f) canvas.translate((rng.nextFloat() - 0.5f) * 12f * shake * w / 340f, (rng.nextFloat() - 0.5f) * 8f * shake * w / 340f)
        if (s.fxJump && jump > 0.01f) canvas.scale(1f + jump * 0.05f, 1f + jump * 0.05f, w / 2f, h / 2f)
        if (s.fxGlitch && slices > 0) {
            val strip = (h / 26f).coerceAtLeast(4f)
            var y = 0f
            while (y < h) {
                val off = (rng.nextFloat() - 0.5f) * 30f * w / 340f * slices / 8f
                src.set(0, y.toInt(), photoBmp.width, min(photoBmp.height, (y + strip).toInt()))
                dst.set(off, y, off + w, y + strip)
                canvas.drawBitmap(photoBmp, src, dst, photoPaint)
                y += strip
            }
        } else {
            canvas.drawBitmap(photoBmp, 0f, 0f, photoPaint)
        }
        canvas.restore()
    }

    private fun drawRipples(
        canvas: Canvas,
        photoBmp: Bitmap?,
        alpha: Float,
        u: Float,
    ) {
        val s = settings
        if (!s.ripplesOn) return
        for (r in ripples) {
            if (r.fall > 0) {
                stroke.strokeWidth = 2.4f * u
                stroke.color = Color.argb(230, 225, 250, 255)
                canvas.drawLine(r.x + r.fall * 2.5f * u, r.y - r.fall * 7f * u, r.x + r.fall * 2.5f * u + 5f * u, r.y - r.fall * 7f * u - 14f * u, stroke)
                continue
            }
            if (r.r < 1f) continue
            if (photoBmp != null) {
                // the photo under the wave: magnified in the middle (a lens), bent at the crest
                val mag = 1f + s.rippleZoom / 100f * 0.35f * r.life
                val bend = s.rippleRefract / 100f * 0.25f * r.life
                val bands = 5
                photoPaint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
                for (b in bands - 1 downTo 0) {
                    val r0 = r.r * b / bands
                    val r1 = r.r * (b + 1) / bands
                    val m = if (b < bands - 2) mag else if (b == bands - 2) 1f + bend else 1f - bend
                    canvas.save()
                    clip.reset()
                    clip.fillType = Path.FillType.EVEN_ODD
                    clip.addCircle(r.x, r.y, r1, Path.Direction.CW)
                    if (r0 > 0f) clip.addCircle(r.x, r.y, r0, Path.Direction.CW)
                    canvas.clipPath(clip)
                    val half = r.r / m
                    src.set((r.x - half).toInt(), (r.y - half).toInt(), (r.x + half).toInt(), (r.y + half).toInt())
                    dst.set(r.x - r.r, r.y - r.r, r.x + r.r, r.y + r.r)
                    canvas.drawBitmap(photoBmp, src, dst, photoPaint)
                    canvas.restore()
                }
            }
            stroke.strokeWidth = 1.6f * u
            stroke.color = Color.argb((0.6f * r.life * 255).toInt(), 215, 245, 255)
            canvas.drawCircle(r.x, r.y, r.r, stroke)
            stroke.strokeWidth = 1f * u
            stroke.color = Color.argb((0.3f * r.life * 255).toInt(), 215, 245, 255)
            canvas.drawCircle(r.x, r.y, r.r * 0.62f, stroke)
        }
    }

    private fun drawRain(
        canvas: Canvas,
        s: NeonHeroSettings,
        u: Float,
    ) {
        val n = (rain.size * s.rainCount / 100f).roundToInt().coerceIn(0, rain.size)
        if (rainLines.size < n * 4) rainLines = FloatArray(n * 4)
        val ln = s.rainLength / 100f
        for (i in 0 until n) {
            val d = rain[i]
            rainLines[i * 4] = d.x
            rainLines[i * 4 + 1] = d.y
            rainLines[i * 4 + 2] = d.x + d.len * ln * 0.55f
            rainLines[i * 4 + 3] = d.y - d.len * ln * 0.85f
        }
        rainPaint.strokeWidth = 1.1f * u
        rainPaint.color = Color.argb((0.7f * s.rainAlpha / 100f * 255f).toInt().coerceIn(0, 255), 185, 240, 255)
        canvas.drawLines(rainLines, 0, n * 4, rainPaint)
    }

    private fun shapePath(
        cx: Float,
        cy: Float,
        r: Float,
        shape: Int,
        extra: ((Float) -> Float)?,
    ): Path {
        path.reset()
        val n = ring.n
        for (i in 0..n) {
            val j = i % n
            val a = (j.toFloat() / n * 2.0 * PI).toFloat()
            val rr = r * NeonHeroMath.baseRadius(shape, a) + ring.offset[j] * r + (extra?.invoke(a) ?: 0f)
            val x = cx + cos(a) * rr
            val y = cy + sin(a) * rr
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun drawHero(
        canvas: Canvas,
        s: NeonHeroSettings,
        cx: Float,
        cy: Float,
        r: Float,
        f: Float,
        tm: Float,
        title: String,
        album: String,
    ) {
        fill.color = Color.rgb(7, 10, 26)
        canvas.drawPath(shapePath(cx, cy, r, s.shape, null), fill)
        if (s.eqOn) {
            canvas.save()
            canvas.clipPath(shapePath(cx, cy, r, s.shape, null))
            val ext = r * 1.45f
            val cols = 15
            val cw = 2f * r / cols
            val segs = 14
            val sh = 2f * ext / segs
            val gap = 1.2f * f
            for (c in 0 until cols) {
                val colx = cx - r + c * cw
                val meter = c == 0 || c == cols - 1
                val lv = if (c == 0) meterL else if (c == cols - 1) meterR else bandLevels[NeonHeroMath.BAND_ORDER[c - 1]]
                val lit = (lv * segs).roundToInt()
                for (sg in 0 until segs) {
                    val y0 = cy + ext * 0.9f - (sg + 1) * sh
                    val on = sg < lit
                    val rgb =
                        if (meter) {
                            if (sg >= segs - 2) Color.rgb(255, 80, 90) else if (sg >= segs - 4) Color.rgb(255, 200, 80) else Color.rgb(90, 230, 140)
                        } else {
                            if (sg >= segs - 2) Color.rgb(255, 140, 220) else if (c < 8) Color.rgb(140, 100, 255) else Color.rgb(80, 180, 255)
                        }
                    fill.color = rgb
                    fill.alpha = if (on) 242 else 20
                    canvas.drawRect(colx + gap, y0 + gap, colx + cw - gap, y0 + sh - gap, fill)
                }
            }
            canvas.restore()
            textPaint.textSize = 12f * f
            textPaint.color = Color.argb(204, 200, 230, 255)
            canvas.drawText("L", cx - r + cw / 2f, cy + r + 18f * f, textPaint)
            canvas.drawText("R", cx + r - cw / 2f, cy + r + 18f * f, textPaint)
        }
        stroke.strokeWidth = 1.2f * f
        stroke.color = Color.argb(90, 150, 230, 255)
        canvas.drawPath(shapePath(cx, cy, r, s.shape) { 5f * f }, stroke)

        // the title: one word per line inside; does not fit — below the hero with the album
        var belowY = cy + r * 1.25f + 24f * f
        if (s.titleOn && title.isNotBlank()) {
            val lines = NeonHeroMath.titleLines(title)
            val inside = lines?.let { fitTitle(it, r) }
            if (lines != null && inside != null) {
                drawTitleInside(canvas, lines, cx, cy, inside)
            } else {
                textPaint.textSize = 17f * f
                textPaint.color = Color.argb(235, 225, 245, 255)
                canvas.drawText(ellipsize(title, 2.6f * r), cx, belowY, textPaint)
                belowY += 20f * f
            }
        }
        if (s.albumOn && album.isNotBlank()) {
            textPaint.textSize = 13f * f
            textPaint.color = Color.argb(200, 170, 215, 245)
            canvas.drawText(ellipsize(album, 2.6f * r), cx, belowY, textPaint)
        }
        if (s.auraOn && auraV > 0.03f) drawAura(canvas, s, cx, cy, r, f, tm)
    }

    /** The letter size so the longest word fits the hero's width and all lines its height; null — too small. */
    private fun fitTitle(
        lines: List<String>,
        r: Float,
    ): Float? {
        textPaint.textSize = 100f
        val widest = lines.maxOf { textPaint.measureText(it) }.coerceAtLeast(1f)
        val byWidth = 100f * (1.45f * r) / widest
        val byHeight = (1.25f * r) / (lines.size * 1.1f)
        val size = min(min(byWidth, byHeight), r * 0.42f)
        return size.takeIf { it >= r * 0.15f }
    }

    private fun drawTitleInside(
        canvas: Canvas,
        lines: List<String>,
        cx: Float,
        cy: Float,
        size: Float,
    ) {
        textPaint.textSize = size
        val lh = size * 1.1f
        val top = cy - lh * lines.size / 2f + size * 0.82f
        textPaint.color = Color.argb(150, 225, 245, 255)
        lines.forEachIndexed { i, line -> canvas.drawText(line, cx, top + i * lh, textPaint) }
        if (settings.fxGlitch && glitch > 0.1f) {
            // the glitch: the letters break into horizontal strips that slide apart and fade
            val y0 = top - size
            val total = lh * lines.size + size * 0.4f
            val strips = 10
            for (sl in 0 until strips) {
                val ys = y0 + total * sl / strips
                canvas.save()
                canvas.clipRect(cx - size * 8f, ys, cx + size * 8f, ys + total / strips)
                val off = (rng.nextFloat() - 0.5f) * size * 1.2f * glitch
                textPaint.color = if (sl % 2 == 0) Color.argb((glitch * 255).toInt(), 220, 245, 255) else Color.argb((glitch * 255).toInt(), 120, 255, 230)
                lines.forEachIndexed { i, line -> canvas.drawText(line, cx + off, top + i * lh, textPaint) }
                canvas.restore()
            }
        }
    }

    private fun ellipsize(
        text: String,
        width: Float,
    ): String {
        if (textPaint.measureText(text) <= width) return text
        var t = text
        while (t.length > 1 && textPaint.measureText("$t…") > width) t = t.dropLast(1)
        return "$t…"
    }

    private fun drawAura(
        canvas: Canvas,
        s: NeonHeroSettings,
        cx: Float,
        cy: Float,
        r: Float,
        f: Float,
        tm: Float,
    ) {
        if (auraShape == 0) {
            stroke.strokeWidth = 7f * auraV * f
            stroke.color = Color.argb((auraV * 255).toInt().coerceIn(0, 255), 235, 250, 255)
            dst.set(cx - (r + 12f * f), cy + 4f * f - (r + 12f * f), cx + r + 12f * f, cy + 4f * f + r + 12f * f)
            canvas.drawArc(dst, 36f, 108f, false, stroke)
            return
        }
        val amp = auraV * auraS
        val extra = { a: Float ->
            val uu = a + (PI / 2).toFloat()
            var e = 10f + amp * 12f
            when (auraShape) {
                1 -> e += amp * 44f * max(0f, cos(uu * 2f)).pow(8) * (if (cos(uu) > 0f) 1f else 0f)
                2 -> e += amp * 48f * (max(0f, cos(uu - 0.75f)).pow(14) + max(0f, cos(uu + 0.75f)).pow(14))
                3 -> e += amp * 30f * max(0f, cos(uu * 3f)).pow(6) * (if (cos(uu) > 0f) 1f else 0.2f)
                4 -> e += amp * 22f * (max(0f, cos(a)).pow(18) + max(0f, cos(a + PI.toFloat())).pow(18))
            }
            (e + sin(a * 7f + tm * 6f) * amp * 4f + sin(a * 3f - tm * 4f) * amp * 3f) * f
        }
        val p = shapePath(cx, cy, r, s.shape, extra)
        for ((width, argb) in listOf(16f to Color.argb((0.25f * auraV * 255).toInt(), 60, 200, 255), 9f to Color.argb((0.5f * auraV * 255).toInt(), 140, 230, 255), 4f to Color.argb((auraV * 255).toInt().coerceIn(0, 255), 245, 252, 255))) {
            stroke.strokeWidth = width * f
            stroke.color = argb
            canvas.drawPath(p, stroke)
        }
    }

    /** The photo, cut to fill the screen (centre crop), made once per size. Loads in the background on screen. */
    private fun photoFor(
        uri: String,
        w: Float,
        h: Float,
    ): Bitmap? {
        if (uri.isBlank()) return null
        if (photoKey != uri) {
            if (loadPhotoNow) {
                photo = decode(uri)
                photoKey = uri
                photoScaled = null
            } else if (photoLoading != uri) {
                photoLoading = uri
                Thread({
                    val bmp = decode(uri)
                    synchronized(this) {
                        photo = bmp
                        photoKey = uri
                        photoScaled = null
                    }
                }, "neon-hero-photo").start()
            }
            if (photoKey != uri) return null
        }
        val p = photo ?: return null
        val key = "$uri|${w.toInt()}|${h.toInt()}"
        photoScaled?.takeIf { photoScaledKey == key && !it.isRecycled }?.let { return it }
        val out =
            runCatching {
                Bitmap.createBitmap(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).also { b ->
                    val scale = max(w / p.width, h / p.height)
                    val m = Matrix()
                    m.setScale(scale, scale)
                    m.postTranslate((w - p.width * scale) / 2f, (h - p.height * scale) / 2f)
                    Canvas(b).drawBitmap(p, m, Paint(Paint.FILTER_BITMAP_FLAG))
                }
            }.getOrNull()
        photoScaled = out
        photoScaledKey = key
        return out
    }

    private fun decode(uri: String): Bitmap? =
        runCatching {
            val u = Uri.parse(uri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
            context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        }.getOrNull()
}

/** V191: the «Неон-герой» settings — same file as MainActivity.layerPrefs(); [version] tells views to re-read. */
object NeonHeroStore {
    const val PREFS = "pulsedeck_layers"
    const val KEY = "pulsedeck.neon_hero"

    @Volatile
    var version = 0
        private set

    fun load(context: Context): NeonHeroSettings =
        NeonHeroSettings.decode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))

    fun save(
        context: Context,
        settings: NeonHeroSettings,
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, settings.encode()).apply()
        version++
    }
}
