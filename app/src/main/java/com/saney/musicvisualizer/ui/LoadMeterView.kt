package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

/**
 * V157 (faric 23.10 15:27: «індикатор, як запис у Track Catcher — наскільки навантажує телефон ця
 * візуалізація; GPU, CPU»): two thin level bars above the bottom buttons of the right page.
 * Green → yellow → red; the text says the number and what it means.
 */
class LoadMeterView(context: Context) : View(context) {
    private var gpu = 0
    private var cpu = 0
    private var info = "Міряю…"
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 20, 30, 40) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(176, 216, 242) }
    private val bar = RectF()

    fun update(
        gpuPercent: Int,
        cpuPercent: Int,
        line: String,
    ) {
        gpu = gpuPercent
        cpu = cpuPercent
        info = line
        invalidate()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val d = resources.displayMetrics.density
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (d * 62).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        canvas.drawColor(Color.argb(155, 0, 0, 0))
        label.textSize = 12f * d
        val left = 8f * d
        val labelW = 42f * d
        val right = width - 8f * d
        // V185: a frame's whole work (projectM sending + the graphics), not the graphics alone.
        drawRow(canvas, "Кадр", gpu, 6f * d, left, labelW, right, d)
        drawRow(canvas, "CPU", cpu, 24f * d, left, labelW, right, d)
        label.textSize = 11f * d
        canvas.drawText(info, left, 56f * d, label)
    }

    private fun drawRow(
        canvas: Canvas,
        name: String,
        percent: Int,
        top: Float,
        left: Float,
        labelW: Float,
        right: Float,
        d: Float,
    ) {
        canvas.drawText(name, left, top + 11f * d, label)
        val valueW = 46f * d
        val x0 = left + labelW
        val x1 = right - valueW
        bar.set(x0, top + 2f * d, x1, top + 12f * d)
        canvas.drawRoundRect(bar, 5f * d, 5f * d, track)
        val share = (percent.coerceIn(0, 100) / 100f)
        if (share > 0f) {
            fill.shader =
                LinearGradient(
                    x0, 0f, x1, 0f,
                    intArrayOf(Color.rgb(61, 220, 132), Color.rgb(255, 210, 80), Color.rgb(255, 90, 78)),
                    floatArrayOf(0f, 0.6f, 1f),
                    Shader.TileMode.CLAMP,
                )
            bar.right = x0 + (x1 - x0) * share
            canvas.drawRoundRect(bar, 5f * d, 5f * d, fill)
        }
        canvas.drawText(if (percent > 100) "100+ %" else "$percent %", x1 + 6f * d, top + 11f * d, label)
    }
}
