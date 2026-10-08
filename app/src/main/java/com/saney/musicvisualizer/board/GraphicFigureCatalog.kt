package com.saney.musicvisualizer.board

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.saney.musicvisualizer.R
import com.saney.musicvisualizer.theme.PlaybackThemeId

data class GraphicFigureAssets(
    val frame: Bitmap?,
    val fx: Bitmap?,
    val creature: Bitmap?,
    val wordmark: Bitmap?,
)

object GraphicFigureCatalog {
    val ids: List<PlaybackThemeId> =
        listOf(
            PlaybackThemeId.CYBER_SHARK,
            PlaybackThemeId.CYBER_PANTHER,
        ) + UserHeroPack.heroes.map { it.id }

    fun normalize(
        themeId: PlaybackThemeId,
    ): PlaybackThemeId =
        if (themeId in ids) {
            themeId
        } else {
            PlaybackThemeId.CYBER_SHARK
        }

    fun title(
        themeId: PlaybackThemeId,
    ): String =
        UserHeroPack.find(themeId)?.title ?: when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                "Cyber Panther"

            else ->
                "Cyber Shark"
        }

    fun creatureLabel(
        themeId: PlaybackThemeId,
    ): String =
        UserHeroPack.find(themeId)?.title ?: when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                "Panther"

            else ->
                "Shark"
        }

    fun supportedLayers(
        themeId: PlaybackThemeId,
    ): Set<BoardLayerId> =
        if (UserHeroPack.find(themeId) != null) {
            BoardLayerId.entries.toSet()
        } else when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                BoardLayerId.entries.toSet()

            else ->
                BoardLayerId.entries.toSet()
        }

    fun supports(
        themeId: PlaybackThemeId,
        layerId: BoardLayerId,
    ): Boolean =
        layerId in
            supportedLayers(
                themeId,
            )

    fun loadAssets(
        context: Context,
        themeId: PlaybackThemeId,
    ): GraphicFigureAssets =
        if (UserHeroPack.find(themeId) != null) {
            GraphicFigureAssets(
                frame = UserHeroPack.loadLayer(context, themeId, "frame"),
                fx = UserHeroPack.loadLayer(context, themeId, "fx"),
                creature = UserHeroPack.loadLayer(context, themeId, "creature"),
                wordmark = UserHeroPack.loadLayer(context, themeId, "wordmark"),
            )
        } else when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                GraphicFigureAssets(
                    frame =
                        decode(
                            context,
                            R.drawable.cyber_panther_frame,
                        ),
                    fx =
                        decode(
                            context,
                            R.drawable.cyber_panther_fx,
                        ),
                    creature =
                        decodePantherCreature(
                            context,
                        ),
                    wordmark =
                        decode(
                            context,
                            R.drawable.cyber_panther_wordmark,
                        ),
                )

            else ->
                GraphicFigureAssets(
                    frame =
                        decode(
                            context,
                            R.drawable.cyber_shark_frame,
                        ),
                    fx = null,
                    creature =
                        decode(
                            context,
                            R.drawable.cyber_shark_creature,
                        ),
                    wordmark =
                        decode(
                            context,
                            R.drawable.cyber_shark_wordmark,
                        ),
                )
        }

    fun isInstalled(context: Context, themeId: PlaybackThemeId): Boolean =
        UserHeroPack.find(themeId) == null || UserHeroPack.isInstalled(context, themeId)

    fun loadPreview(context: Context, themeId: PlaybackThemeId): Bitmap? {
        if (UserHeroPack.find(themeId) != null) {
            return UserHeroPack.loadEmblem(context, themeId, preview = true)
        }
        val drawable = when (themeId) {
            PlaybackThemeId.CYBER_PANTHER -> R.drawable.cyber_panther_full
            else -> R.drawable.cyber_shark_creature
        }
        return BitmapFactory.decodeResource(
            context.resources,
            drawable,
            BitmapFactory.Options().apply {
                inScaled = false
                inSampleSize = 4
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    }

    private fun decodePantherCreature(
        context: Context,
    ): Bitmap? {
        val creature =
            decode(
                context,
                R.drawable.cyber_panther_creature,
            )
        val coverage =
            alphaCoverage(
                creature,
            )

        Log.i(
            TAG,
            "Cyber Panther creature alpha coverage=" +
                "%.4f".format(
                    coverage,
                ) +
                " size=" +
                (
                    creature
                        ?.let {
                            "${it.width}x${it.height}"
                        }
                        ?: "null"
                    ),
        )

        if (
            creature != null &&
            coverage >=
                MIN_USEFUL_ALPHA_COVERAGE
        ) {
            return creature
        }

        Log.w(
            TAG,
            "Cyber Panther creature asset is effectively empty; using physical full-pack fallback for phone diagnosis.",
        )

        return decode(
            context,
            R.drawable.cyber_panther_full,
        )
    }

    private fun alphaCoverage(
        bitmap: Bitmap?,
    ): Float {
        bitmap ?: return 0f

        val stepX =
            (bitmap.width / 64)
                .coerceAtLeast(
                    1,
                )
        val stepY =
            (bitmap.height / 64)
                .coerceAtLeast(
                    1,
                )

        var sampled =
            0
        var visible =
            0
        var y =
            0

        while (
            y <
            bitmap.height
        ) {
            var x =
                0
            while (
                x <
                bitmap.width
            ) {
                sampled +=
                    1
                if (
                    android.graphics.Color.alpha(
                        bitmap.getPixel(
                            x,
                            y,
                        ),
                    ) >
                    12
                ) {
                    visible +=
                        1
                }
                x +=
                    stepX
            }
            y +=
                stepY
        }

        return if (
            sampled >
            0
        ) {
            visible /
                sampled.toFloat()
        } else {
            0f
        }
    }

    private fun decode(
        context: Context,
        drawable: Int,
    ): Bitmap? =
        runCatching {
            BitmapFactory.decodeResource(
                context.resources,
                drawable,
                BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig =
                        Bitmap.Config.ARGB_8888
                },
            )
        }.getOrNull()

    private const val TAG =
        "GraphicFigureCatalog"
    private const val MIN_USEFUL_ALPHA_COVERAGE =
        0.01f
}
