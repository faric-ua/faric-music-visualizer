package com.saney.musicvisualizer.board

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.saney.musicvisualizer.R
import com.saney.musicvisualizer.theme.PlaybackThemeId

data class GraphicFigureAssets(
    val frame: Bitmap?,
    val creature: Bitmap?,
    val wordmark: Bitmap?,
)

object GraphicFigureCatalog {
    val ids: List<PlaybackThemeId> =
        listOf(
            PlaybackThemeId.CYBER_SHARK,
            PlaybackThemeId.CYBER_PANTHER,
        )

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
        when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                "Cyber Panther"

            else ->
                "Cyber Shark"
        }

    fun creatureLabel(
        themeId: PlaybackThemeId,
    ): String =
        when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                "Panther"

            else ->
                "Shark"
        }

    fun supportedLayers(
        themeId: PlaybackThemeId,
    ): Set<BoardLayerId> =
        when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                setOf(
                    BoardLayerId.BACKGROUND,
                    BoardLayerId.FX,
                    BoardLayerId.CREATURE,
                )

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
        when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                GraphicFigureAssets(
                    frame = null,
                    creature =
                        createCyberPantherCreature(),
                    wordmark = null,
                )

            else ->
                GraphicFigureAssets(
                    frame =
                        decode(
                            context,
                            R.drawable.cyber_shark_frame,
                        ),
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

    /**
     * Procedural static artwork for the first alternate GF.
     *
     * It intentionally has no frame and no wordmark. That makes Cyber Panther
     * a real alternate layer-set test for the generalized exporter instead of
     * another Cyber Shark clone.
     */
    private fun createCyberPantherCreature():
        Bitmap {
        val size = 1024
        val bitmap =
            Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888,
            )
        val canvas =
            Canvas(
                bitmap,
            )

        val fill =
            Paint(
                Paint.ANTI_ALIAS_FLAG,
            )
        val stroke =
            Paint(
                Paint.ANTI_ALIAS_FLAG,
            ).apply {
                style =
                    Paint.Style.STROKE
                strokeJoin =
                    Paint.Join.ROUND
                strokeCap =
                    Paint.Cap.ROUND
            }
        val path =
            Path()

        // Panther head silhouette with angular cyber ears and cheek plates.
        path.moveTo(
            512f,
            154f,
        )
        path.lineTo(
            366f,
            206f,
        )
        path.lineTo(
            236f,
            112f,
        )
        path.lineTo(
            268f,
            318f,
        )
        path.cubicTo(
            190f,
            402f,
            194f,
            585f,
            302f,
            706f,
        )
        path.cubicTo(
            368f,
            780f,
            432f,
            842f,
            512f,
            892f,
        )
        path.cubicTo(
            592f,
            842f,
            656f,
            780f,
            722f,
            706f,
        )
        path.cubicTo(
            830f,
            585f,
            834f,
            402f,
            756f,
            318f,
        )
        path.lineTo(
            788f,
            112f,
        )
        path.lineTo(
            658f,
            206f,
        )
        path.close()

        fill.shader =
            LinearGradient(
                220f,
                180f,
                820f,
                860f,
                intArrayOf(
                    Color.argb(
                        245,
                        9,
                        18,
                        38,
                    ),
                    Color.argb(
                        250,
                        25,
                        17,
                        58,
                    ),
                    Color.argb(
                        245,
                        3,
                        14,
                        28,
                    ),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(
            path,
            fill,
        )
        fill.shader = null

        // Multi-pass neon outline.
        stroke.color =
            Color.argb(
                65,
                194,
                48,
                255,
            )
        stroke.strokeWidth =
            34f
        canvas.drawPath(
            path,
            stroke,
        )

        stroke.color =
            Color.argb(
                120,
                46,
                222,
                255,
            )
        stroke.strokeWidth =
            15f
        canvas.drawPath(
            path,
            stroke,
        )

        stroke.color =
            Color.argb(
                235,
                151,
                73,
                255,
            )
        stroke.strokeWidth =
            4f
        canvas.drawPath(
            path,
            stroke,
        )

        // Forehead plates/circuit spine.
        stroke.strokeWidth =
            9f
        stroke.color =
            Color.argb(
                200,
                68,
                222,
                255,
            )
        path.reset()
        path.moveTo(
            512f,
            194f,
        )
        path.lineTo(
            512f,
            540f,
        )
        path.moveTo(
            512f,
            284f,
        )
        path.lineTo(
            442f,
            350f,
        )
        path.lineTo(
            362f,
            374f,
        )
        path.moveTo(
            512f,
            284f,
        )
        path.lineTo(
            582f,
            350f,
        )
        path.lineTo(
            662f,
            374f,
        )
        canvas.drawPath(
            path,
            stroke,
        )

        stroke.strokeWidth =
            4f
        stroke.color =
            Color.argb(
                185,
                227,
                75,
                255,
            )
        canvas.drawPath(
            path,
            stroke,
        )

        // Eye sockets.
        val leftEye =
            Path().apply {
                moveTo(
                    314f,
                    424f,
                )
                lineTo(
                    458f,
                    398f,
                )
                lineTo(
                    418f,
                    482f,
                )
                lineTo(
                    326f,
                    486f,
                )
                close()
            }
        val rightEye =
            Path().apply {
                moveTo(
                    710f,
                    424f,
                )
                lineTo(
                    566f,
                    398f,
                )
                lineTo(
                    606f,
                    482f,
                )
                lineTo(
                    698f,
                    486f,
                )
                close()
            }

        fill.color =
            Color.argb(
                68,
                0,
                228,
                255,
            )
        canvas.drawPath(
            leftEye,
            fill,
        )
        fill.color =
            Color.argb(
                68,
                230,
                47,
                255,
            )
        canvas.drawPath(
            rightEye,
            fill,
        )

        stroke.style =
            Paint.Style.STROKE
        stroke.strokeWidth =
            11f
        stroke.color =
            Color.rgb(
                69,
                238,
                255,
            )
        canvas.drawPath(
            leftEye,
            stroke,
        )
        stroke.color =
            Color.rgb(
                222,
                78,
                255,
            )
        canvas.drawPath(
            rightEye,
            stroke,
        )

        // Bright pupils.
        fill.color =
            Color.WHITE
        canvas.drawCircle(
            382f,
            447f,
            11f,
            fill,
        )
        canvas.drawCircle(
            642f,
            447f,
            11f,
            fill,
        )

        // Nose and muzzle.
        path.reset()
        path.moveTo(
            444f,
            590f,
        )
        path.lineTo(
            512f,
            622f,
        )
        path.lineTo(
            580f,
            590f,
        )
        path.lineTo(
            548f,
            660f,
        )
        path.lineTo(
            512f,
            676f,
        )
        path.lineTo(
            476f,
            660f,
        )
        path.close()

        fill.color =
            Color.argb(
                235,
                173,
                69,
                255,
            )
        canvas.drawPath(
            path,
            fill,
        )

        stroke.color =
            Color.argb(
                220,
                58,
                224,
                255,
            )
        stroke.strokeWidth =
            7f
        path.reset()
        path.moveTo(
            512f,
            676f,
        )
        path.cubicTo(
            468f,
            700f,
            422f,
            704f,
            374f,
            688f,
        )
        path.moveTo(
            512f,
            676f,
        )
        path.cubicTo(
            556f,
            700f,
            602f,
            704f,
            650f,
            688f,
        )
        canvas.drawPath(
            path,
            stroke,
        )

        // Cyber whiskers.
        stroke.strokeWidth =
            5f
        stroke.color =
            Color.argb(
                160,
                83,
                225,
                255,
            )
        repeat(3) { index ->
            val y =
                612f +
                    index *
                    45f
            canvas.drawLine(
                418f,
                y,
                214f -
                    index *
                    22f,
                y +
                    22f +
                    index *
                    12f,
                stroke,
            )
            canvas.drawLine(
                606f,
                y,
                810f +
                    index *
                    22f,
                y +
                    22f +
                    index *
                    12f,
                stroke,
            )
        }

        // Small cheek circuitry for extra depth.
        stroke.strokeWidth =
            4f
        stroke.color =
            Color.argb(
                145,
                222,
                79,
                255,
            )
        repeat(3) { index ->
            val y =
                525f +
                    index *
                    58f
            canvas.drawLine(
                286f,
                y,
                372f,
                y +
                    24f,
                stroke,
            )
            canvas.drawLine(
                738f,
                y,
                652f,
                y +
                    24f,
                stroke,
            )
        }

        return bitmap
    }
}
