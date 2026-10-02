package com.saney.musicvisualizer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.hypot

class PulseDeckCenterCalibrationOverlayView(
    context: Context,
) : View(context) {

    private enum class ReviewState {
        WRONG,
        SEEMS_GOOD,
        UNCHECKED,
    }

    private enum class PanelAction {
        MOVE_UP,
        MOVE_DOWN,
        MOVE_LEFT,
        MOVE_RIGHT,
        STEP,
        PREV,
        SAVE,
        NEXT,
        RESET,
        COPY,
        NONE,
    }

    private data class CalibrationPoint(
        val id: String,
        val name: String,
        val baselineX: Float,
        val baselineY: Float,
        val reviewState: ReviewState,
    )

    private val points =
        listOf(
            CalibrationPoint("C-01", "Back", 44f / 708f, 75f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-02", "Header title", 354f / 708f, 78f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-03", "Menu", 659f / 708f, 77f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-04", "Hero reactor", 353f / 708f, 362f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-05", "Favorite", 51f / 708f, 674f / 1536f, ReviewState.SEEMS_GOOD),
            CalibrationPoint("C-06", "Track info", 354f / 708f, 704f / 1536f, ReviewState.SEEMS_GOOD),
            CalibrationPoint("C-07", "Track more", 657f / 708f, 673f / 1536f, ReviewState.SEEMS_GOOD),
            CalibrationPoint("C-08", "Waveform", 354f / 708f, 801f / 1536f, ReviewState.SEEMS_GOOD),
            CalibrationPoint("C-09", "Progress", 354f / 708f, 839f / 1536f, ReviewState.SEEMS_GOOD),
            CalibrationPoint("C-10", "Transport rail", 354f / 708f, 1029f / 1536f, ReviewState.SEEMS_GOOD),
            CalibrationPoint("C-11", "Shuffle", 85f / 708f, 1040f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-12", "Previous", 196f / 708f, 1029f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-13", "Play/Pause", 350f / 708f, 1028f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-14", "Next", 515f / 708f, 1029f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-15", "Repeat", 649f / 708f, 1028f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-16", "Quick-actions rail", 354f / 708f, 1250f / 1536f, ReviewState.SEEMS_GOOD),
            CalibrationPoint("C-17", "Theme", 93f / 708f, 1249f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-18", "Board", 269f / 708f, 1249f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-19", "Visualizer", 446f / 708f, 1250f / 1536f, ReviewState.WRONG),
            CalibrationPoint("C-20", "Export", 622f / 708f, 1250f / 1536f, ReviewState.WRONG),
        )

    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    private val targetPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 58, 58)
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
        }

    private val axisPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                180,
                0,
                255,
                220,
            )
            style = Paint.Style.STROKE
            strokeWidth = dp(1.5f)
        }

    private val labelPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(15f)
            typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD,
                )
        }

    private val smallPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(
                204,
                218,
                228,
            )
            textSize = dp(12f)
        }

    private val panelPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color =
                Color.argb(
                    220,
                    5,
                    10,
                    16,
                )
            style =
                Paint.Style.FILL
        }

    private val savedPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color =
                Color.rgb(
                    83,
                    255,
                    154,
                )
            textSize =
                dp(12f)
            typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD,
                )
        }

    private var currentIndex =
        prefs
            .getInt(
                KEY_CURRENT_INDEX,
                0,
            )
            .coerceIn(
                0,
                points.lastIndex,
            )

    private var targetXNorm = 0.5f
    private var targetYNorm = 0.5f

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var downWasOnTarget = false
    private var draggingTarget = false

    private var stepPx =
        prefs.getInt(
            KEY_STEP_PX,
            5,
        )
            .coerceIn(
                1,
                20,
            )

    private var panelXNorm =
        prefs.getFloat(
            KEY_PANEL_X,
            0.50f,
        )
    private var panelYNorm =
        prefs.getFloat(
            KEY_PANEL_Y,
            0.78f,
        )

    private var panelDragging = false
    private var panelTouchCaptured = false
    private var panelDragOffsetX = 0f
    private var panelDragOffsetY = 0f
    private var activePanelAction =
        PanelAction.NONE

    private val panelActionRects =
        mutableMapOf<PanelAction, RectF>()

    init {
        isClickable = true
        isFocusable = true
        loadCurrentTarget()
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)

        if (
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        val x =
            targetXNorm *
                width
        val y =
            targetYNorm *
                height

        drawMainAxes(
            canvas,
            x,
            y,
        )
        drawCenterTarget(
            canvas,
            x,
            y,
        )
        drawInfoPanel(
            canvas,
        )
        drawControlPanel(
            canvas,
        )
    }

    private fun drawMainAxes(
        canvas: Canvas,
        x: Float,
        y: Float,
    ) {
        val dash =
            dp(10f)
        val gap =
            dp(8f)

        var yy = 0f
        while (yy < height) {
            canvas.drawLine(
                x,
                yy,
                x,
                (yy + dash)
                    .coerceAtMost(
                        height.toFloat(),
                    ),
                axisPaint,
            )
            yy +=
                dash +
                    gap
        }

        var xx = 0f
        while (xx < width) {
            canvas.drawLine(
                xx,
                y,
                (xx + dash)
                    .coerceAtMost(
                        width.toFloat(),
                    ),
                y,
                axisPaint,
            )
            xx +=
                dash +
                    gap
        }
    }

    private fun drawCenterTarget(
        canvas: Canvas,
        x: Float,
        y: Float,
    ) {
        val outer =
            dp(24f)
        val inner =
            dp(7f)
        val arm =
            dp(42f)

        canvas.drawCircle(
            x,
            y,
            outer,
            targetPaint,
        )
        canvas.drawCircle(
            x,
            y,
            inner,
            targetPaint,
        )

        canvas.drawLine(
            x - arm,
            y,
            x - inner - dp(3f),
            y,
            targetPaint,
        )
        canvas.drawLine(
            x + inner + dp(3f),
            y,
            x + arm,
            y,
            targetPaint,
        )
        canvas.drawLine(
            x,
            y - arm,
            x,
            y - inner - dp(3f),
            targetPaint,
        )
        canvas.drawLine(
            x,
            y + inner + dp(3f),
            x,
            y + arm,
            targetPaint,
        )

        val point =
            points[currentIndex]
        val label =
            point.id +
                "  " +
                point.name

        val labelX =
            (
                x +
                    dp(31f)
                )
                .coerceAtMost(
                    width -
                        dp(190f),
                )
                .coerceAtLeast(
                    dp(8f),
                )
        val labelY =
            (
                y -
                    dp(33f)
                )
                .coerceAtLeast(
                    dp(22f),
                )

        val labelBounds =
            RectF(
                labelX -
                    dp(6f),
                labelY -
                    dp(17f),
                labelX +
                    labelPaint
                        .measureText(
                            label,
                        ) +
                    dp(7f),
                labelY +
                    dp(7f),
            )

        canvas.drawRoundRect(
            labelBounds,
            dp(5f),
            dp(5f),
            panelPaint,
        )
        canvas.drawText(
            label,
            labelX,
            labelY,
            labelPaint,
        )
    }

    private fun drawInfoPanel(
        canvas: Canvas,
    ) {
        val point =
            points[currentIndex]

        val top =
            height -
                dp(105f)

        canvas.drawRoundRect(
            RectF(
                dp(8f),
                top,
                width -
                    dp(8f),
                height -
                    dp(8f),
            ),
            dp(13f),
            dp(13f),
            panelPaint,
        )

        val review =
            when (
                point.reviewState
            ) {
                ReviewState.WRONG ->
                    "позначено: ХИБНИЙ"

                ReviewState.SEEMS_GOOD ->
                    "позначено: НАЧЕ ДОБРИЙ"

                ReviewState.UNCHECKED ->
                    "ще не оцінено"
            }

        val px =
            (
                targetXNorm *
                    width
                )
                .toInt()
        val py =
            (
                targetYNorm *
                    height
                )
                .toInt()

        val saved =
            isPointSaved(
                point,
            )

        canvas.drawText(
            point.id +
                " / " +
                points.size +
                "  " +
                point.name +
                "  ·  " +
                review,
            dp(18f),
            top +
                dp(24f),
            labelPaint,
        )

        canvas.drawText(
            "px: " +
                px +
                ", " +
                py +
                "   norm: " +
                formatNorm(
                    targetXNorm,
                ) +
                ", " +
                formatNorm(
                    targetYNorm,
                ),
            dp(18f),
            top +
                dp(48f),
            smallPaint,
        )

        canvas.drawText(
            "Перетягни приціл · тап по прицілу = зберегти → далі · свайп = C←/C→",
            dp(18f),
            top +
                dp(72f),
            smallPaint,
        )

        canvas.drawText(
            "довгий тап по прицілу = скопіювати всі координати",
            dp(18f),
            top +
                dp(92f),
            if (saved) {
                savedPaint
            } else {
                smallPaint
            },
        )
    }

    private fun panelRect(): RectF {
        val panelWidth =
            minOf(
                width -
                    dp(20f),
                dp(360f),
            )
        val panelHeight =
            dp(225f)

        val centerX =
            (
                panelXNorm *
                    width
                )
                .coerceIn(
                    panelWidth *
                        0.5f,
                    width -
                        panelWidth *
                        0.5f,
                )
        val centerY =
            (
                panelYNorm *
                    height
                )
                .coerceIn(
                    panelHeight *
                        0.5f,
                    height -
                        panelHeight *
                        0.5f,
                )

        return RectF(
            centerX -
                panelWidth *
                0.5f,
            centerY -
                panelHeight *
                0.5f,
            centerX +
                panelWidth *
                0.5f,
            centerY +
                panelHeight *
                0.5f,
        )
    }

    private fun drawControlPanel(
        canvas: Canvas,
    ) {
        val panel =
            panelRect()

        panelActionRects.clear()

        canvas.drawRoundRect(
            panel,
            dp(16f),
            dp(16f),
            panelPaint,
        )

        val headerHeight =
            dp(34f)

        canvas.drawText(
            "CENTER REMOTE  ·  drag",
            panel.left +
                dp(14f),
            panel.top +
                dp(23f),
            labelPaint,
        )

        val dpadCx =
            panel.left +
                dp(86f)
        val dpadCy =
            panel.top +
                dp(105f)
        val button =
            dp(42f)
        val gap =
            dp(6f)

        fun addButton(
            action: PanelAction,
            cx: Float,
            cy: Float,
            label: String,
        ) {
            val rect =
                RectF(
                    cx -
                        button *
                        0.5f,
                    cy -
                        button *
                        0.5f,
                    cx +
                        button *
                        0.5f,
                    cy +
                        button *
                        0.5f,
                )

            panelActionRects[action] =
                RectF(
                    rect.left - dp(6f),
                    rect.top - dp(6f),
                    rect.right + dp(6f),
                    rect.bottom + dp(6f),
                )

            val pressed =
                activePanelAction ==
                    action

            val fill =
                Paint(
                    Paint.ANTI_ALIAS_FLAG,
                ).apply {
                    color =
                        if (pressed) {
                            Color.argb(
                                245,
                                20,
                                122,
                                148,
                            )
                        } else {
                            Color.argb(
                                230,
                                15,
                                28,
                                39,
                            )
                        }
                    style =
                        Paint.Style.FILL
                }

            canvas.drawRoundRect(
                rect,
                dp(9f),
                dp(9f),
                fill,
            )

            canvas.drawRoundRect(
                rect,
                dp(9f),
                dp(9f),
                axisPaint,
            )

            val oldAlign =
                labelPaint.textAlign
            labelPaint.textAlign =
                Paint.Align.CENTER

            canvas.drawText(
                label,
                cx,
                cy +
                    dp(6f),
                labelPaint,
            )

            labelPaint.textAlign =
                oldAlign
        }

        addButton(
            PanelAction.MOVE_UP,
            dpadCx,
            dpadCy -
                button -
                gap,
            "↑",
        )
        addButton(
            PanelAction.MOVE_LEFT,
            dpadCx -
                button -
                gap,
            dpadCy,
            "←",
        )
        addButton(
            PanelAction.MOVE_RIGHT,
            dpadCx +
                button +
                gap,
            dpadCy,
            "→",
        )
        addButton(
            PanelAction.MOVE_DOWN,
            dpadCx,
            dpadCy +
                button +
                gap,
            "↓",
        )

        val stepRect =
            RectF(
                panel.left +
                    dp(172f),
                panel.top +
                    dp(55f),
                panel.right -
                    dp(14f),
                panel.top +
                    dp(96f),
            )
        panelActionRects[
            PanelAction.STEP
        ] =
            stepRect

        canvas.drawRoundRect(
            stepRect,
            dp(10f),
            dp(10f),
            Paint(
                Paint.ANTI_ALIAS_FLAG,
            ).apply {
                color =
                    Color.argb(
                        230,
                        20,
                        36,
                        48,
                    )
            },
        )
        canvas.drawText(
            "STEP  " +
                stepPx +
                " px",
            stepRect.left +
                dp(14f),
            stepRect.centerY() +
                dp(6f),
            labelPaint,
        )

        val point =
            points[
                currentIndex
            ]
        val px =
            (
                targetXNorm *
                    width
                )
                .toInt()
        val py =
            (
                targetYNorm *
                    height
                )
                .toInt()

        canvas.drawText(
            point.id +
                "  " +
                point.name,
            panel.left +
                dp(172f),
            panel.top +
                dp(122f),
            smallPaint,
        )
        canvas.drawText(
            "X=" +
                px +
                "  Y=" +
                py,
            panel.left +
                dp(172f),
            panel.top +
                dp(145f),
            savedPaint,
        )

        val actionY =
            panel.bottom -
                dp(31f)
        val actionWidth =
            (
                panel.width() -
                    dp(28f)
                ) /
                5f

        fun addFooterAction(
            action: PanelAction,
            index: Int,
            label: String,
        ) {
            val left =
                panel.left +
                    dp(14f) +
                    actionWidth *
                    index
            val rect =
                RectF(
                    left,
                    actionY -
                        dp(22f),
                    left +
                        actionWidth -
                        dp(4f),
                    actionY +
                        dp(18f),
                )

            panelActionRects[action] =
                rect

            canvas.drawRoundRect(
                rect,
                dp(8f),
                dp(8f),
                Paint(
                    Paint.ANTI_ALIAS_FLAG,
                ).apply {
                    color =
                        Color.argb(
                            225,
                            12,
                            25,
                            35,
                        )
                },
            )

            val oldAlign =
                smallPaint.textAlign
            smallPaint.textAlign =
                Paint.Align.CENTER

            canvas.drawText(
                label,
                rect.centerX(),
                rect.centerY() +
                    dp(5f),
                smallPaint,
            )

            smallPaint.textAlign =
                oldAlign
        }

        addFooterAction(
            PanelAction.PREV,
            0,
            "PREV",
        )
        addFooterAction(
            PanelAction.SAVE,
            1,
            "SAVE",
        )
        addFooterAction(
            PanelAction.NEXT,
            2,
            "NEXT",
        )
        addFooterAction(
            PanelAction.RESET,
            3,
            "RESET",
        )
        addFooterAction(
            PanelAction.COPY,
            4,
            "COPY",
        )

        // Header itself is the drag handle; no action rect is needed.
        panelActionRects[
            PanelAction.NONE
        ] =
            RectF(
                panel.left,
                panel.top,
                panel.right,
                panel.top +
                    headerHeight,
            )
    }

    private fun findPanelAction(
        x: Float,
        y: Float,
    ): PanelAction? =
        panelActionRects
            .entries
            .firstOrNull { (
                action,
                rect,
            ) ->
                action !=
                    PanelAction.NONE &&
                    rect.contains(
                        x,
                        y,
                    )
            }
            ?.key

    private fun moveTargetByPixels(
        dx: Int,
        dy: Int,
    ) {
        if (
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        val x =
            targetXNorm *
                width +
                dx
        val y =
            targetYNorm *
                height +
                dy

        setTargetFromPx(
            x,
            y,
        )
    }

    private fun performPanelAction(
        action: PanelAction,
    ) {
        when (action) {
            PanelAction.MOVE_UP ->
                moveTargetByPixels(
                    0,
                    -stepPx,
                )

            PanelAction.MOVE_DOWN ->
                moveTargetByPixels(
                    0,
                    stepPx,
                )

            PanelAction.MOVE_LEFT ->
                moveTargetByPixels(
                    -stepPx,
                    0,
                )

            PanelAction.MOVE_RIGHT ->
                moveTargetByPixels(
                    stepPx,
                    0,
                )

            PanelAction.STEP -> {
                stepPx =
                    when (stepPx) {
                        1 ->
                            2

                        2 ->
                            5

                        5 ->
                            10

                        10 ->
                            20

                        else ->
                            1
                    }

                prefs.edit()
                    .putInt(
                        KEY_STEP_PX,
                        stepPx,
                    )
                    .apply()

                invalidate()
            }

            PanelAction.PREV ->
                moveIndex(
                    -1,
                )

            PanelAction.SAVE ->
                saveCurrentAndAdvance()

            PanelAction.NEXT ->
                moveIndex(
                    1,
                )

            PanelAction.RESET ->
                resetCurrentPoint()

            PanelAction.COPY ->
                copyAllSaved()

            PanelAction.NONE ->
                Unit
        }
    }

    private fun resetCurrentPoint() {
        val point =
            points[
                currentIndex
            ]

        prefs.edit()
            .remove(
                keyX(
                    point,
                ),
            )
            .remove(
                keyY(
                    point,
                ),
            )
            .remove(
                keySaved(
                    point,
                ),
            )
            .apply()

        targetXNorm =
            point.baselineX
        targetYNorm =
            point.baselineY

        Toast.makeText(
            context,
            point.id +
                " скинуто до стартової точки",
            Toast.LENGTH_SHORT,
        ).show()

        invalidate()
    }

    override fun onTouchEvent(
        event: MotionEvent,
    ): Boolean {
        val targetX =
            targetXNorm *
                width
        val targetY =
            targetYNorm *
                height

        when (
            event.actionMasked
        ) {
            MotionEvent.ACTION_DOWN -> {
                downX =
                    event.x
                downY =
                    event.y
                downTime =
                    event.eventTime

                val panel =
                    panelRect()
                val panelHeader =
                    panelActionRects[
                        PanelAction.NONE
                    ]

                panelTouchCaptured =
                    panel.contains(
                        event.x,
                        event.y,
                    )

                activePanelAction =
                    findPanelAction(
                        event.x,
                        event.y,
                    )
                        ?: PanelAction.NONE

                if (
                    activePanelAction !=
                    PanelAction.NONE
                ) {
                    invalidate()
                    return true
                }

                if (
                    panelHeader
                        ?.contains(
                            event.x,
                            event.y,
                        ) ==
                        true
                ) {
                    panelDragging = true
                    panelDragOffsetX =
                        event.x -
                            panel.centerX()
                    panelDragOffsetY =
                        event.y -
                            panel.centerY()
                    return true
                }

                // Any touch inside the floating remote belongs to the remote.
                // Never let a tap on empty panel space relocate the crosshair.
                if (
                    panelTouchCaptured
                ) {
                    return true
                }

                downWasOnTarget =
                    hypot(
                        event.x -
                            targetX,
                        event.y -
                            targetY,
                    ) <=
                    dp(54f)

                draggingTarget = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (
                    panelDragging
                ) {
                    val centerX =
                        event.x -
                            panelDragOffsetX
                    val centerY =
                        event.y -
                            panelDragOffsetY

                    panelXNorm =
                        (
                            centerX /
                                width
                            )
                            .coerceIn(
                                0f,
                                1f,
                            )
                    panelYNorm =
                        (
                            centerY /
                                height
                            )
                            .coerceIn(
                                0f,
                                1f,
                            )

                    invalidate()
                    return true
                }

                if (
                    downWasOnTarget
                ) {
                    val moved =
                        hypot(
                            event.x -
                                downX,
                            event.y -
                                downY,
                        )

                    if (
                        moved >
                        dp(5f)
                    ) {
                        draggingTarget = true
                    }

                    if (
                        draggingTarget
                    ) {
                        setTargetFromPx(
                            event.x,
                            event.y,
                        )
                    }
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                if (
                    panelDragging
                ) {
                    panelDragging = false
                    panelTouchCaptured = false

                    prefs.edit()
                        .putFloat(
                            KEY_PANEL_X,
                            panelXNorm,
                        )
                        .putFloat(
                            KEY_PANEL_Y,
                            panelYNorm,
                        )
                        .apply()

                    invalidate()
                    return true
                }

                if (
                    activePanelAction !=
                    PanelAction.NONE
                ) {
                    val action =
                        activePanelAction
                    activePanelAction =
                        PanelAction.NONE
                    panelTouchCaptured = false

                    performPanelAction(
                        action,
                    )

                    invalidate()
                    return true
                }

                if (
                    panelTouchCaptured
                ) {
                    panelTouchCaptured = false
                    invalidate()
                    return true
                }

                val dx =
                    event.x -
                        downX
                val dy =
                    event.y -
                        downY
                val held =
                    event.eventTime -
                        downTime

                if (
                    downWasOnTarget
                ) {
                    if (
                        draggingTarget
                    ) {
                        setTargetFromPx(
                            event.x,
                            event.y,
                        )
                    } else if (
                        held >=
                        LONG_PRESS_MS
                    ) {
                        copyAllSaved()
                    } else {
                        saveCurrentAndAdvance()
                    }

                    return true
                }

                if (
                    abs(dx) >=
                        dp(82f) &&
                    abs(dx) >
                        abs(dy) *
                        1.35f
                ) {
                    moveIndex(
                        if (
                            dx > 0f
                        ) {
                            -1
                        } else {
                            1
                        },
                    )
                    return true
                }

                if (
                    abs(dx) <
                        dp(10f) &&
                    abs(dy) <
                        dp(10f)
                ) {
                    setTargetFromPx(
                        event.x,
                        event.y,
                    )
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                draggingTarget = false
                panelDragging = false
                panelTouchCaptured = false
                activePanelAction =
                    PanelAction.NONE
                invalidate()
                return true
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun saveCurrentAndAdvance() {
        val point =
            points[currentIndex]

        prefs.edit()
            .putFloat(
                keyX(
                    point,
                ),
                targetXNorm,
            )
            .putFloat(
                keyY(
                    point,
                ),
                targetYNorm,
            )
            .putBoolean(
                keySaved(
                    point,
                ),
                true,
            )
            .apply()

        val message =
            point.id +
                " = " +
                formatNorm(
                    targetXNorm,
                ) +
                ", " +
                formatNorm(
                    targetYNorm,
                )

        Toast.makeText(
            context,
            "Збережено: " +
                message,
            Toast.LENGTH_SHORT,
        ).show()

        if (
            currentIndex ==
            points.lastIndex
        ) {
            copyAllSaved()
        } else {
            moveIndex(
                1,
            )
        }

        performClick()
    }

    private fun moveIndex(
        delta: Int,
    ) {
        currentIndex =
            (
                currentIndex +
                    delta
                )
                .coerceIn(
                    0,
                    points.lastIndex,
                )

        prefs.edit()
            .putInt(
                KEY_CURRENT_INDEX,
                currentIndex,
            )
            .apply()

        loadCurrentTarget()
        invalidate()
    }

    private fun loadCurrentTarget() {
        val point =
            points[currentIndex]

        targetXNorm =
            if (
                isPointSaved(
                    point,
                )
            ) {
                prefs.getFloat(
                    keyX(
                        point,
                    ),
                    point.baselineX,
                )
            } else {
                point.baselineX
            }

        targetYNorm =
            if (
                isPointSaved(
                    point,
                )
            ) {
                prefs.getFloat(
                    keyY(
                        point,
                    ),
                    point.baselineY,
                )
            } else {
                point.baselineY
            }
    }

    private fun setTargetFromPx(
        x: Float,
        y: Float,
    ) {
        if (
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        targetXNorm =
            (
                x /
                    width
                )
                .coerceIn(
                    0f,
                    1f,
                )

        targetYNorm =
            (
                y /
                    height
                )
                .coerceIn(
                    0f,
                    1f,
                )

        invalidate()
    }

    private fun copyAllSaved() {
        val lines =
            buildList {
                add(
                    "PULSEDECK_CENTER_CALIBRATION",
                )

                points.forEach { point ->
                    if (
                        isPointSaved(
                            point,
                        )
                    ) {
                        val x =
                            prefs.getFloat(
                                keyX(
                                    point,
                                ),
                                point.baselineX,
                            )
                        val y =
                            prefs.getFloat(
                                keyY(
                                    point,
                                ),
                                point.baselineY,
                            )

                        add(
                            point.id +
                                " " +
                                point.name +
                                " = " +
                                formatNorm(
                                    x,
                                ) +
                                ", " +
                                formatNorm(
                                    y,
                                ),
                        )
                    }
                }
            }

        val clipboard =
            context.getSystemService(
                Context.CLIPBOARD_SERVICE,
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "PulseDeck centers",
                lines.joinToString(
                    "\n",
                ),
            ),
        )

        Toast.makeText(
            context,
            "Координати скопійовано: " +
                savedCount() +
                "/" +
                points.size,
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun isPointSaved(
        point: CalibrationPoint,
    ): Boolean =
        prefs.getBoolean(
            keySaved(
                point,
            ),
            false,
        )

    private fun savedCount(): Int =
        points.count(
            ::isPointSaved,
        )

    private fun keyX(
        point: CalibrationPoint,
    ): String =
        point.id +
            "_x"

    private fun keyY(
        point: CalibrationPoint,
    ): String =
        point.id +
            "_y"

    private fun keySaved(
        point: CalibrationPoint,
    ): String =
        point.id +
            "_saved"

    private fun formatNorm(
        value: Float,
    ): String =
        String.format(
            java.util.Locale.US,
            "%.6f",
            value,
        )

    private fun dp(
        value: Float,
    ): Float =
        value *
            resources.displayMetrics
                .density

    companion object {
        private const val PREFS_NAME =
            "pulsedeck_center_calibration"

        private const val KEY_CURRENT_INDEX =
            "current_index"

        private const val KEY_STEP_PX =
            "step_px"
        private const val KEY_PANEL_X =
            "panel_x"
        private const val KEY_PANEL_Y =
            "panel_y"

        private const val LONG_PRESS_MS =
            650L
    }
}
