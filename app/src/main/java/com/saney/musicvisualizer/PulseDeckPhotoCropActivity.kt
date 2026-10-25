package com.saney.musicvisualizer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import com.saney.musicvisualizer.ui.NeonHeroMath
import com.saney.musicvisualizer.ui.NeonHeroSettings
import com.saney.musicvisualizer.ui.NeonHeroStore
import com.saney.musicvisualizer.ui.UtilityUi
import com.saney.musicvisualizer.ui.UtilityUi.dp
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

/**
 * V191 (faric 25.10 14:51: «картинка здорова, а бере по центру — хочу пунктирну рамочку телефона, посунути вліво /
 * вправо, вибрати, яку область картинки взяти»): the whole photo with a dashed phone-shaped frame; one finger
 * moves the frame, two fingers make it smaller (closer) or bigger. «Готово» keeps it; the main screen and the
 * video take exactly that part.
 */
class PulseDeckPhotoCropActivity : ComponentActivity() {
    private var s = NeonHeroSettings()
    private lateinit var crop: PhotoCropView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        s = NeonHeroStore.load(this)
        val metrics = resources.displayMetrics
        val aspect = min(metrics.widthPixels, metrics.heightPixels).toFloat() / max(metrics.widthPixels, metrics.heightPixels)
        crop = PhotoCropView(this, aspect, s.photoCx / 1000f, s.photoCy / 1000f, s.photoZoom / 100f)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(UtilityUi.BG) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(4)) }
        header.addView(UtilityUi.button(this, "‹") { finish() }.apply { contentDescription = "Назад" }, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(UtilityUi.text(this, "Область фото", 22f, bold = true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        root.addView(header)
        root.addView(
            UtilityUi.text(this, "Пунктир — це екран телефона. Посунь рамку пальцем; двома пальцями — ближче чи далі.", 13f, UtilityUi.MUTED).apply { setPadding(dp(16), 0, dp(16), dp(6)) },
        )
        root.addView(crop, LinearLayout.LayoutParams(-1, 0, 1f))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(16), dp(8), dp(16), dp(12)) }
        row.addView(UtilityUi.button(this, "По центру") { crop.reset() }, UtilityUi.weighted(context = this))
        row.addView(UtilityUi.button(this, "Готово", UtilityUi.Kind.MAIN) { save(); finish() }, UtilityUi.weighted(context = this))
        root.addView(row)
        UtilityUi.padForSystemBars(root)
        setContentView(root)
        val uri = s.photoUri
        if (uri.isNotBlank()) {
            thread(name = "neon-crop-photo") {
                val bmp = decode(this, uri)
                runOnUiThread { if (!isDestroyed) crop.photo = bmp }
            }
        }
    }

    override fun onPause() {
        save()
        super.onPause()
    }

    private fun save() {
        val now = NeonHeroStore.load(this)
        val next = now.copy(photoCx = (crop.cx * 1000).toInt().coerceIn(0, 1000), photoCy = (crop.cy * 1000).toInt().coerceIn(0, 1000), photoZoom = (crop.zoom * 100).toInt().coerceIn(100, 400))
        if (next != now) NeonHeroStore.save(this, next)
    }

    private fun decode(
        context: Context,
        uri: String,
    ): Bitmap? =
        runCatching {
            val u = Uri.parse(uri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
            context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        }.getOrNull()

    /** The photo fitted in the view, dimmed outside the dashed phone frame. Positions — in 0–1 of the photo. */
    @SuppressLint("ViewConstructor")
    private class PhotoCropView(
        context: Context,
        private val aspect: Float,
        var cx: Float,
        var cy: Float,
        var zoom: Float,
    ) : View(context) {
        var photo: Bitmap? = null
            set(value) {
                field = value
                clampCentre()
                invalidate()
            }
        private val shown = RectF()
        private val frame = RectF()
        private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        private val dim = Paint().apply { color = Color.argb(150, 0, 0, 0) }
        private val dash =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.WHITE
                strokeWidth = context.resources.displayMetrics.density * 2.5f
                pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
            }
        private val outside = Path()
        private var lastX = 0f
        private var lastY = 0f
        private val pinch =
            ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        // fingers apart → a bigger picture on the screen → the frame gets smaller (closer)
                        zoom = (zoom * detector.scaleFactor).coerceIn(1f, 4f)
                        clampCentre()
                        invalidate()
                        return true
                    }
                },
            )

        fun reset() {
            cx = 0.5f
            cy = 0.5f
            zoom = 1f
            invalidate()
        }

        /** Keep the centre where the frame really is (it may not leave the photo). */
        private fun clampCentre() {
            val p = photo ?: return
            val c = NeonHeroMath.cropRect(p.width.toFloat(), p.height.toFloat(), aspect, cx, cy, zoom)
            cx = (c[0] + c[2] / 2f) / p.width
            cy = (c[1] + c[3] / 2f) / p.height
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val p = photo ?: return
            val pad = 12f * resources.displayMetrics.density
            val scale = min((width - 2 * pad) / p.width, (height - 2 * pad) / p.height)
            val w = p.width * scale
            val h = p.height * scale
            shown.set((width - w) / 2f, (height - h) / 2f, (width + w) / 2f, (height + h) / 2f)
            canvas.drawBitmap(p, null, shown, bitmapPaint)
            val c = NeonHeroMath.cropRect(p.width.toFloat(), p.height.toFloat(), aspect, cx, cy, zoom)
            frame.set(shown.left + c[0] * scale, shown.top + c[1] * scale, shown.left + (c[0] + c[2]) * scale, shown.top + (c[1] + c[3]) * scale)
            outside.reset()
            outside.fillType = Path.FillType.EVEN_ODD
            outside.addRect(shown, Path.Direction.CW)
            outside.addRect(frame, Path.Direction.CW)
            canvas.drawPath(outside, dim)
            canvas.drawRect(frame, dash)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            pinch.onTouchEvent(event)
            val p = photo ?: return true
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                }
                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_POINTER_DOWN -> {
                    lastX = event.getX(0)
                    lastY = event.getY(0)
                }
                MotionEvent.ACTION_MOVE ->
                    if (!pinch.isInProgress && event.pointerCount == 1 && shown.width() > 0f) {
                        cx += (event.x - lastX) / shown.width()
                        cy += (event.y - lastY) / shown.height()
                        lastX = event.x
                        lastY = event.y
                        clampCentre()
                        invalidate()
                    } else {
                        lastX = event.getX(0)
                        lastY = event.getY(0)
                    }
            }
            if (p.isRecycled) return true
            return true
        }
    }
}
