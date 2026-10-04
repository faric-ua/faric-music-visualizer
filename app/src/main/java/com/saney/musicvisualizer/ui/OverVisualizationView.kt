package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

/**
 * Layer 1 — over-visualization.
 *
 * Layer 1 — the complete user-approved 447504 artwork.
 *
 * This is a real full-screen image layer, not a transparent/effect overlay.
 * It is drawn normally (SRC_OVER) so the black background and every pixel of
 * the source image belong to Layer 1. The artwork uses center-crop plus 5%
 * overscan on every edge to tolerate different phone aspect ratios without
 * exposing hard image boundaries.
 */
class OverVisualizationView(
    context: Context,
) : View(context) {

    private val paint =
        Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG,
        ).apply {
            alpha = 255
        }

    private val artwork: Bitmap? by lazy {
        loadArtwork()
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)

        val bitmap =
            artwork
                ?: return

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val overscan = 1.10f
        val targetWidth = w * overscan
        val targetHeight = h * overscan
        val target =
            RectF(
                (w - targetWidth) * 0.5f,
                (h - targetHeight) * 0.5f,
                (w + targetWidth) * 0.5f,
                (h + targetHeight) * 0.5f,
            )

        val targetAspect =
            target.width() /
                target.height()
        val sourceAspect =
            bitmap.width.toFloat() /
                bitmap.height.toFloat()

        val source =
            if (sourceAspect > targetAspect) {
                val sourceWidth =
                    bitmap.height *
                        targetAspect
                val left =
                    (
                        bitmap.width -
                            sourceWidth
                        ) *
                        0.5f

                Rect(
                    left.toInt(),
                    0,
                    (left + sourceWidth).toInt(),
                    bitmap.height,
                )
            } else {
                val sourceHeight =
                    bitmap.width /
                        targetAspect
                val top =
                    (
                        bitmap.height -
                            sourceHeight
                        ) *
                        0.5f

                Rect(
                    0,
                    top.toInt(),
                    bitmap.width,
                    (top + sourceHeight).toInt(),
                )
            }

        canvas.drawBitmap(
            bitmap,
            source,
            target,
            paint,
        )
    }

    private fun loadArtwork(): Bitmap? =
        runCatching {
            context.assets
                .open(
                    "pulsedeck_hud/over_visualization/over_visualization_447504.webp",
                )
                .use { input ->
                    BitmapFactory.decodeStream(
                        input,
                    )
                }
        }.getOrNull()
}
