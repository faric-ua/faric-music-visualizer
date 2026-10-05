package com.saney.musicvisualizer.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max

/** Deterministic Layer 1 renderer for the approved 447504 artwork. */
class OverVisualizationExportRenderer(
    context: Context,
) {
    private val bitmap: Bitmap? =
        runCatching {
            context.assets
                .open(
                    "pulsedeck_hud/over_visualization/over_visualization_447504.webp",
                )
                .use {
                    BitmapFactory.decodeStream(
                        it,
                    )
                }
        }.getOrNull()

    private val paint =
        Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG,
        ).apply {
            alpha = 255
        }

    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
    ) {
        val source =
            bitmap
                ?: return

        if (
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        val targetWidth =
            width *
                1.10f
        val targetHeight =
            height *
                1.10f

        val scale =
            max(
                targetWidth /
                    source.width,
                targetHeight /
                    source.height,
            )

        val renderedWidth =
            source.width *
                scale
        val renderedHeight =
            source.height *
                scale

        val left =
            (
                width -
                    renderedWidth
                ) *
                0.5f
        val top =
            (
                height -
                    renderedHeight
                ) *
                0.5f

        canvas.drawBitmap(
            source,
            null,
            RectF(
                left,
                top,
                left +
                    renderedWidth,
                top +
                    renderedHeight,
            ),
            paint,
        )
    }
}
