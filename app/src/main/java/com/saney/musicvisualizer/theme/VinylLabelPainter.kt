package com.saney.musicvisualizer.theme

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.min

/**
 * Draws [VinylLabel] text on the record label — one painter for the live screen
 * (HeroThemeView) and MP4 export (HeroThemeRenderer), so both look the same.
 * Title lines sit above the centre hole, the artist / «VINYL» line below it.
 * Call inside the canvas rotation of the record.
 */
object VinylLabelPainter {
    fun draw(
        canvas: Canvas,
        paint: Paint,
        cx: Float,
        cy: Float,
        labelRadius: Float,
        rawTitle: String,
        artist: String?,
        titleColor: Int,
        titleAlpha: Int,
        bottomColor: Int,
        bottomAlpha: Int,
    ) {
        val savedAlign = paint.textAlign
        val savedSize = paint.textSize
        val savedBold = paint.isFakeBoldText
        paint.textAlign = Paint.Align.CENTER

        val lines = VinylLabel.titleLines(rawTitle)
        if (lines.isNotEmpty()) {
            paint.isFakeBoldText = true
            paint.color = titleColor
            paint.alpha = titleAlpha
            var size = labelRadius * 0.20f
            paint.textSize = size
            val widest = lines.maxOf { paint.measureText(it) }.coerceAtLeast(1f)
            val maxWidth = labelRadius * 1.45f
            size = min(size, size * maxWidth / widest)
            paint.textSize = size
            val lineHeight = size * 1.12f
            // The last title line ends just above the centre hole.
            val lastBaseline = cy - labelRadius * 0.13f
            lines.forEachIndexed { index, line ->
                val y = lastBaseline - (lines.size - 1 - index) * lineHeight
                canvas.drawText(line, cx, y, paint)
            }
        }

        paint.isFakeBoldText = false
        paint.color = bottomColor
        paint.alpha = bottomAlpha
        var bottomSize = labelRadius * 0.13f
        paint.textSize = bottomSize
        val bottom = VinylLabel.bottomLine(artist)
        val bottomWidth = paint.measureText(bottom).coerceAtLeast(1f)
        bottomSize = min(bottomSize, bottomSize * labelRadius * 1.3f / bottomWidth)
        paint.textSize = bottomSize
        canvas.drawText(bottom, cx, cy + labelRadius * 0.30f, paint)

        paint.textAlign = savedAlign
        paint.textSize = savedSize
        paint.isFakeBoldText = savedBold
    }
}
