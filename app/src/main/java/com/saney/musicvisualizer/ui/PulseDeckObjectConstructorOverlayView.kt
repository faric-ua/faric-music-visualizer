package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.hypot

class PulseDeckObjectConstructorOverlayView(
    context: Context,
    private val sourceView: PulseDeckMainSkinView,
    private val onExportRequested: (String) -> Unit = {},
    private val onImportRequested: () -> Unit = {},
) : View(context) {
    private data class ObjectItem(
        val id: String,
        val name: String,
        val rendererId: String,
        val parentId: String? = null,
    )

    private enum class Action {
        UP, DOWN, LEFT, RIGHT, STEP, PREV, NEXT, SAVE, RESET, RESET_ALL,
        EXPORT, IMPORT, GRID, MAG, ZOOM, SIZE, FOLLOW, FREEZE,
        GUIDE_V, GUIDE_H, GUIDE_CLEAR,
        ACTIONS_TOGGLE, MAGNIFIER_TOGGLE, GUIDES_TOGGLE, NONE,
    }

    private val objects = listOf(
        ObjectItem("C-01", "Back", "back"),
        ObjectItem("C-02", "Header title", "header_title"),
        ObjectItem("C-03", "Menu", "menu"),
        ObjectItem("C-04", "Hero reactor", "hero_frame"),
        ObjectItem("C-05", "Favorite", "favorite"),
        ObjectItem("C-06", "Track info", "track_info"),
        ObjectItem("C-07", "Track more", "track_more"),
        ObjectItem("C-08", "Waveform", "waveform"),
        ObjectItem("C-09", "Progress", "progress_group"),
        ObjectItem("C-10", "Transport rail", "transport_rail"),
        ObjectItem("C-11", "Shuffle", "shuffle", "C-10"),
        ObjectItem("C-12", "Previous", "previous", "C-10"),
        ObjectItem("C-13", "Play/Pause", "play_pause", "C-10"),
        ObjectItem("C-14", "Next", "next", "C-10"),
        ObjectItem("C-15", "Repeat", "repeat", "C-10"),
        ObjectItem("C-16", "Quick-actions rail", "quick_rail"),
        ObjectItem("C-17", "Theme", "theme", "C-16"),
        ObjectItem("C-18", "Board", "board", "C-16"),
        ObjectItem("C-19", "Visualizer", "visualizer", "C-16"),
        ObjectItem("C-20", "Export", "export", "C-16"),
    )

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val offsets = mutableMapOf<String, Pair<Float, Float>>()
    private val selected = linkedSetOf<Int>()

    private var primary = prefs.getInt(KEY_PRIMARY, 0).coerceIn(0, objects.lastIndex)
    private var stepPx = prefs.getInt(KEY_STEP, 2).coerceIn(1, 20)
    private var gridEnabled = prefs.getBoolean(KEY_GRID, false)
    private var actionsExpanded = prefs.getBoolean(KEY_ACTIONS_EXPANDED, true)
    private var magnifierExpanded = prefs.getBoolean(KEY_MAGNIFIER_EXPANDED, true)
    private var guidesExpanded = prefs.getBoolean(KEY_GUIDES_EXPANDED, false)
    private var panelX = prefs.getFloat(KEY_PANEL_X, 0.50f)
    private var panelY = prefs.getFloat(KEY_PANEL_Y, 0.77f)

    private var magnifierEnabled = prefs.getBoolean(KEY_MAG_ENABLED, true)
    private var magnifierZoom = prefs.getFloat(KEY_MAG_ZOOM, 4f).coerceIn(2f, 6f)
    private var magnifierSizeDp = prefs.getInt(KEY_MAG_SIZE, 180).coerceIn(120, 240)
    private var magnifierFollow = prefs.getBoolean(KEY_MAG_FOLLOW, true)
    private var magnifierFreeze = prefs.getBoolean(KEY_MAG_FREEZE, false)
    private var magnifierX = prefs.getFloat(KEY_MAG_X, 0.78f)
    private var magnifierY = prefs.getFloat(KEY_MAG_Y, 0.25f)

    private var guideVEnabled = prefs.getBoolean(KEY_GUIDE_V_ENABLED, false)
    private var guideHEnabled = prefs.getBoolean(KEY_GUIDE_H_ENABLED, false)
    private var guideVX = prefs.getFloat(KEY_GUIDE_V_X, 0.5f)
    private var guideHY = prefs.getFloat(KEY_GUIDE_H_Y, 0.5f)
    private var lastAction = "Готово"

    private val actionRects = mutableMapOf<Action, RectF>()
    private val panelRect = RectF()

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var downObject = -1
    private var draggingPanel = false
    private var draggingMagnifier = false
    private var panelOffsetX = 0f
    private var panelOffsetY = 0f
    private var magnifierOffsetX = 0f
    private var magnifierOffsetY = 0f

    private var frozenBitmap: Bitmap? = null
    private var frozenTargetX = 0f
    private var frozenTargetY = 0f

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(224, 5, 10, 16)
    }
    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(155, 13, 27, 38)
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 15, 28, 39)
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 166, 0)
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
    }
    private val groupedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 255, 220)
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dp(12f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(204, 218, 228)
        textSize = dp(10f)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        pathEffect = DashPathEffect(floatArrayOf(dp(5f), dp(7f)), 0f)
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(225, 255, 166, 0)
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        pathEffect = DashPathEffect(floatArrayOf(dp(9f), dp(7f)), 0f)
    }
    private val loupeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 92, 64)
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
    }
    private val loupeAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(245, 0, 255, 220)
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        pathEffect = DashPathEffect(floatArrayOf(dp(8f), dp(6f)), 0f)
    }
    private val loupeCrossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 92, 64)
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }
    private val loupePath = Path()

    init {
        objects.forEach { item ->
            offsets[item.id] =
                prefs.getFloat("dx_" + item.id, 0f) to
                    prefs.getFloat("dy_" + item.id, 0f)
        }
        selected.add(primary)
        applyOffsets()
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        if (gridEnabled) drawGrid(canvas)
        drawGuides(canvas)
        drawPrimaryObjectAxes(canvas)
        drawSelections(canvas)
        drawMagnifier(canvas)
        drawPanel(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        for (i in 1 until 10) {
            val x = width * i / 10f
            val y = height * i / 10f
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
    }

    private fun drawGuides(canvas: Canvas) {
        if (guideVEnabled) {
            val x = guideVX * width
            canvas.drawLine(x, 0f, x, height.toFloat(), guidePaint)
        }
        if (guideHEnabled) {
            val y = guideHY * height
            canvas.drawLine(0f, y, width.toFloat(), y, guidePaint)
        }
    }

    private fun drawPrimaryObjectAxes(canvas: Canvas) {
        val rect = primaryRect() ?: return
        val x = rect.centerX()
        val y = rect.centerY()
        canvas.drawLine(x, 0f, x, height.toFloat(), loupeAxisPaint)
        canvas.drawLine(0f, y, width.toFloat(), y, loupeAxisPaint)
    }

    private fun drawSelections(canvas: Canvas) {
        val rects = sourceView.editorObjectRects()
        selected.forEach { index ->
            val item = objects[index]
            val rect = rects[item.rendererId] ?: return@forEach
            val paint = if (index == primary) selectedPaint else groupedPaint
            canvas.drawRoundRect(rect, dp(8f), dp(8f), paint)
            canvas.drawText(
                item.id,
                rect.left + dp(4f),
                (rect.top - dp(4f)).coerceAtLeast(dp(12f)),
                textPaint,
            )
        }
    }

    private fun primaryRect(): RectF? =
        sourceView.editorObjectRects()[objects[primary].rendererId]

    private fun primaryCenter(): Pair<Float, Float> {
        val rect = primaryRect()
        return if (rect != null) {
            rect.centerX() to rect.centerY()
        } else {
            width * 0.5f to height * 0.5f
        }
    }

    private fun magnifierRadius(): Float = dp(magnifierSizeDp / 2f)

    private fun magnifierCenter(targetX: Float, targetY: Float): Pair<Float, Float> {
        val radius = magnifierRadius()
        if (magnifierFollow) {
            val offsetX = radius + dp(34f)
            val offsetY = radius + dp(28f)
            val rawX =
                if (targetX < width * 0.55f) targetX + offsetX
                else targetX - offsetX
            val rawY =
                if (targetY < height * 0.45f) targetY + offsetY
                else targetY - offsetY
            return rawX.coerceIn(radius + dp(8f), width - radius - dp(8f)) to
                rawY.coerceIn(radius + dp(8f), height - radius - dp(8f))
        }
        return (magnifierX * width).coerceIn(radius + dp(8f), width - radius - dp(8f)) to
            (magnifierY * height).coerceIn(radius + dp(8f), height - radius - dp(8f))
    }

    private fun drawMagnifier(canvas: Canvas) {
        if (!magnifierEnabled) return

        val liveTarget = primaryCenter()
        val sourceTarget =
            if (magnifierFreeze && frozenBitmap != null) {
                frozenTargetX to frozenTargetY
            } else {
                liveTarget
            }
        val (tx, ty) = sourceTarget
        val (cx, cy) = magnifierCenter(liveTarget.first, liveTarget.second)
        val radius = magnifierRadius()

        loupePath.reset()
        loupePath.addCircle(cx, cy, radius, Path.Direction.CW)

        val save = canvas.save()
        canvas.clipPath(loupePath)

        val bitmap = frozenBitmap
        if (magnifierFreeze && bitmap != null) {
            val srcHalf = radius / magnifierZoom
            val srcLeft = (tx - srcHalf).toInt().coerceIn(0, bitmap.width - 1)
            val srcTop = (ty - srcHalf).toInt().coerceIn(0, bitmap.height - 1)
            val srcRight = (tx + srcHalf).toInt().coerceIn(srcLeft + 1, bitmap.width)
            val srcBottom = (ty + srcHalf).toInt().coerceIn(srcTop + 1, bitmap.height)
            canvas.drawBitmap(
                bitmap,
                Rect(srcLeft, srcTop, srcRight, srcBottom),
                RectF(cx - radius, cy - radius, cx + radius, cy + radius),
                null,
            )
        } else {
            canvas.translate(cx, cy)
            canvas.scale(magnifierZoom, magnifierZoom)
            canvas.translate(-tx, -ty)
            sourceView.draw(canvas)
        }
        canvas.restoreToCount(save)

        val axisSave = canvas.save()
        canvas.clipPath(loupePath)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, loupeAxisPaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, loupeAxisPaint)
        canvas.restoreToCount(axisSave)

        canvas.drawCircle(cx, cy, radius, loupeBorderPaint)
        val arm = radius * 0.32f
        canvas.drawLine(cx - arm, cy, cx + arm, cy, loupeCrossPaint)
        canvas.drawLine(cx, cy - arm, cx, cy + arm, loupeCrossPaint)
        canvas.drawCircle(cx, cy, dp(5f), loupeCrossPaint)
    }

    private fun panelHeight(): Float =
        dp(
            224f +
                (if (actionsExpanded) 92f else 0f) +
                (if (magnifierExpanded) 58f else 0f) +
                (if (guidesExpanded) 54f else 0f),
        )

    private fun drawPanel(canvas: Canvas) {
        actionRects.clear()
        val pw = minOf(width - dp(20f), dp(370f))
        val ph = panelHeight()
        val cx = (panelX * width).coerceIn(pw / 2f, width - pw / 2f)
        val cy = (panelY * height).coerceIn(ph / 2f, height - ph / 2f)

        panelRect.set(cx - pw / 2f, cy - ph / 2f, cx + pw / 2f, cy + ph / 2f)
        canvas.drawRoundRect(panelRect, dp(16f), dp(16f), panelPaint)

        canvas.drawText(
            "OBJECT CONSTRUCTOR  ·  drag",
            panelRect.left + dp(14f),
            panelRect.top + dp(23f),
            textPaint,
        )

        var top = panelRect.top + dp(36f)

        val moveTop = top
        val moveBottom = moveTop + dp(154f)
        drawTile(canvas, moveTop, moveBottom, "MOVE")
        drawMoveControls(canvas, moveTop)
        top = moveBottom + dp(6f)

        val actionsTop = top
        val actionsBottom = actionsTop + dp(if (actionsExpanded) 98f else 34f)
        drawTile(
            canvas,
            actionsTop,
            actionsBottom,
            if (actionsExpanded) "ACTIONS  ▲" else "ACTIONS  ▼",
        )
        actionRects[Action.ACTIONS_TOGGLE] =
            RectF(panelRect.left + dp(10f), actionsTop, panelRect.right - dp(10f), actionsTop + dp(34f))
        if (actionsExpanded) drawActionControls(canvas, actionsTop)
        top = actionsBottom + dp(6f)

        val magTop = top
        val magBottom = magTop + dp(if (magnifierExpanded) 64f else 34f)
        drawTile(
            canvas,
            magTop,
            magBottom,
            if (magnifierExpanded) "MAGNIFIER  ▲" else "MAGNIFIER  ▼",
        )
        actionRects[Action.MAGNIFIER_TOGGLE] =
            RectF(panelRect.left + dp(10f), magTop, panelRect.right - dp(10f), magTop + dp(34f))
        if (magnifierExpanded) drawMagnifierControls(canvas, magTop)
        top = magBottom + dp(6f)

        val guideTop = top
        val guideBottom = guideTop + dp(if (guidesExpanded) 60f else 34f)
        drawTile(
            canvas,
            guideTop,
            guideBottom,
            if (guidesExpanded) "GUIDES  ▲" else "GUIDES  ▼",
        )
        actionRects[Action.GUIDES_TOGGLE] =
            RectF(panelRect.left + dp(10f), guideTop, panelRect.right - dp(10f), guideTop + dp(34f))
        if (guidesExpanded) drawGuideControls(canvas, guideTop)
    }

    private fun drawTile(canvas: Canvas, top: Float, bottom: Float, title: String) {
        val rect = RectF(panelRect.left + dp(10f), top, panelRect.right - dp(10f), bottom)
        canvas.drawRoundRect(rect, dp(12f), dp(12f), tilePaint)
        canvas.drawText(title, rect.left + dp(10f), rect.top + dp(18f), smallPaint)
    }

    private fun drawMoveControls(canvas: Canvas, top: Float) {
        val dpadX = panelRect.left + dp(86f)
        val dpadY = top + dp(82f)
        val size = dp(42f)
        val gap = dp(6f)

        addButton(canvas, Action.UP, dpadX, dpadY - size - gap, size, "↑")
        addButton(canvas, Action.LEFT, dpadX - size - gap, dpadY, size, "←")
        addButton(canvas, Action.RIGHT, dpadX + size + gap, dpadY, size, "→")
        addButton(canvas, Action.DOWN, dpadX, dpadY + size + gap, size, "↓")

        val stepRect =
            RectF(
                panelRect.left + dp(172f),
                top + dp(34f),
                panelRect.right - dp(14f),
                top + dp(75f),
            )
        actionRects[Action.STEP] = stepRect
        canvas.drawRoundRect(stepRect, dp(9f), dp(9f), buttonPaint)
        canvas.drawText(
            "STEP " + stepPx + "px",
            stepRect.left + dp(12f),
            stepRect.centerY() + dp(5f),
            textPaint,
        )

        val item = objects[primary]
        canvas.drawText(
            item.id + "  " + item.name,
            panelRect.left + dp(172f),
            top + dp(101f),
            smallPaint,
        )
        val rect = primaryRect()
        val centerX = rect?.centerX() ?: width * 0.5f
        val centerY = rect?.centerY() ?: height * 0.5f
        val xNorm = centerX / width.toFloat()
        val yNorm = centerY / height.toFloat()

        canvas.drawText(
            "X %.0fpx  ·  %.6f".format(centerX, xNorm),
            panelRect.left + dp(172f),
            top + dp(116f),
            smallPaint,
        )
        canvas.drawText(
            "Y %.0fpx  ·  %.6f".format(centerY, yNorm),
            panelRect.left + dp(172f),
            top + dp(131f),
            smallPaint,
        )
        canvas.drawText(
            "selected " + selected.size + "  ·  " + lastAction,
            panelRect.left + dp(172f),
            top + dp(146f),
            smallPaint,
        )
    }

    private fun drawActionControls(canvas: Canvas, top: Float) {
        val row1 =
            listOf(
                Action.PREV to "PREV",
                Action.SAVE to "SAVE",
                Action.NEXT to "NEXT",
                Action.RESET to "RESET",
                Action.GRID to if (gridEnabled) "GRID ON" else "GRID OFF",
            )
        drawRow(canvas, row1, top + dp(28f), dp(34f))

        val row2 =
            listOf(
                Action.EXPORT to "EXPORT",
                Action.IMPORT to "IMPORT",
                Action.RESET_ALL to "RESET ALL",
            )
        drawRow(canvas, row2, top + dp(64f), dp(28f))
    }

    private fun drawMagnifierControls(canvas: Canvas, top: Float) {
        val labels =
            listOf(
                Action.MAG to if (magnifierEnabled) "MAG ON" else "MAG OFF",
                Action.ZOOM to "ZOOM " + magnifierZoom.toInt() + "x",
                Action.SIZE to when (magnifierSizeDp) {
                    120 -> "SIZE S"
                    180 -> "SIZE M"
                    else -> "SIZE L"
                },
                Action.FOLLOW to if (magnifierFollow) "FOLLOW" else "FREE",
                Action.FREEZE to if (magnifierFreeze) "FROZEN" else "FREEZE",
            )
        drawRow(canvas, labels, top + dp(26f), dp(34f))
    }

    private fun drawGuideControls(canvas: Canvas, top: Float) {
        val labels =
            listOf(
                Action.GUIDE_V to if (guideVEnabled) "V ON" else "V GUIDE",
                Action.GUIDE_H to if (guideHEnabled) "H ON" else "H GUIDE",
                Action.GUIDE_CLEAR to "CLEAR",
            )
        drawRow(canvas, labels, top + dp(24f), dp(32f))
    }

    private fun drawRow(
        canvas: Canvas,
        labels: List<Pair<Action, String>>,
        top: Float,
        height: Float,
    ) {
        val gap = dp(5f)
        val left = panelRect.left + dp(14f)
        val usable = panelRect.width() - dp(28f)
        val bw = (usable - gap * (labels.size - 1)) / labels.size

        labels.forEachIndexed { index, pair ->
            val rect =
                RectF(
                    left + index * (bw + gap),
                    top,
                    left + index * (bw + gap) + bw,
                    top + height,
                )
            actionRects[pair.first] = rect
            canvas.drawRoundRect(rect, dp(8f), dp(8f), buttonPaint)

            val old = smallPaint.textAlign
            smallPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(pair.second, rect.centerX(), rect.centerY() + dp(4f), smallPaint)
            smallPaint.textAlign = old
        }
    }

    private fun addButton(
        canvas: Canvas,
        action: Action,
        cx: Float,
        cy: Float,
        size: Float,
        label: String,
    ) {
        val rect = RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)
        actionRects[action] =
            RectF(rect.left - dp(5f), rect.top - dp(5f), rect.right + dp(5f), rect.bottom + dp(5f))
        canvas.drawRoundRect(rect, dp(9f), dp(9f), buttonPaint)

        val old = textPaint.textAlign
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, cx, cy + dp(5f), textPaint)
        textPaint.textAlign = old
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downTime = event.eventTime

                val action =
                    actionRects.entries
                        .firstOrNull { it.value.contains(event.x, event.y) }
                        ?.key
                        ?: Action.NONE
                if (action != Action.NONE) {
                    perform(action)
                    return true
                }

                val (mx, my) = magnifierCenterForHit()
                if (
                    magnifierEnabled &&
                    hypot(event.x - mx, event.y - my) <= magnifierRadius()
                ) {
                    draggingMagnifier = true
                    magnifierFollow = false
                    magnifierOffsetX = mx - event.x
                    magnifierOffsetY = my - event.y
                    return true
                }

                if (panelRect.contains(event.x, event.y)) {
                    draggingPanel = true
                    panelOffsetX = panelRect.centerX() - event.x
                    panelOffsetY = panelRect.centerY() - event.y
                    return true
                }

                downObject = hitObject(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (draggingMagnifier) {
                    val radius = magnifierRadius()
                    val x = (event.x + magnifierOffsetX).coerceIn(radius, width - radius)
                    val y = (event.y + magnifierOffsetY).coerceIn(radius, height - radius)
                    magnifierX = x / width
                    magnifierY = y / height
                    persistMagnifier()
                    invalidate()
                    return true
                }

                if (draggingPanel) {
                    val cx =
                        (event.x + panelOffsetX)
                            .coerceIn(panelRect.width() / 2f, width - panelRect.width() / 2f)
                    val cy =
                        (event.y + panelOffsetY)
                            .coerceIn(panelRect.height() / 2f, height - panelRect.height() / 2f)
                    panelX = cx / width
                    panelY = cy / height
                    invalidate()
                    return true
                }

            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                if (draggingMagnifier) {
                    draggingMagnifier = false
                    persistMagnifier()
                    return true
                }

                if (draggingPanel) {
                    draggingPanel = false
                    prefs.edit()
                        .putFloat(KEY_PANEL_X, panelX)
                        .putFloat(KEY_PANEL_Y, panelY)
                        .apply()
                    return true
                }

                if (downObject >= 0) {
                    val held = event.eventTime - downTime
                    val distance = hypot(event.x - downX, event.y - downY)
                    val tapTolerance = dp(12f)

                    if (held >= LONG_PRESS_MS && distance <= tapTolerance) {
                        toggleGroup(downObject)
                    } else if (distance <= tapTolerance) {
                        selectOnly(downObject)
                    }
                }

                downObject = -1
                invalidate()
                return true
            }
        }
        return true
    }

    private fun hitObject(x: Float, y: Float): Int {
        val rects = sourceView.editorObjectRects()
        return objects.indices
            .reversed()
            .firstOrNull { index ->
                rects[objects[index].rendererId]?.contains(x, y) == true
            }
            ?: -1
    }

    private fun selectOnly(index: Int) {
        selected.clear()
        selected.add(index)
        primary = index
        prefs.edit().putInt(KEY_PRIMARY, primary).apply()
    }

    private fun toggleGroup(index: Int) {
        if (selected.contains(index)) {
            selected.remove(index)
            if (selected.isEmpty()) selected.add(index)
        } else {
            selected.add(index)
            primary = index
        }

        if (!selected.contains(primary)) {
            primary = selected.lastOrNull() ?: index
        }

        prefs.edit().putInt(KEY_PRIMARY, primary).apply()
        Toast.makeText(
            context,
            "Виділено: " + selected.size,
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun editableSelectedIndices(): List<Int> =
        selected.filter { index ->
            val parentId = objects[index].parentId
            parentId == null ||
                selected.none { objects[it].id == parentId }
        }

    private fun nudge(dxPx: Int, dyPx: Int) {
        val dx = dxPx / width.toFloat()
        val dy = dyPx / height.toFloat()

        editableSelectedIndices().forEach { index ->
            val item = objects[index]
            val old = offsets[item.id] ?: (0f to 0f)
            offsets[item.id] = (old.first + dx) to (old.second + dy)
        }

        applyOffsets()
        saveOffsets()
        val item = objects[primary]
        val rect = primaryRect()
        lastAction =
            if (rect != null) {
                val x = rect.centerX() / width.toFloat()
                val y = rect.centerY() / height.toFloat()
                "%s X %.6f Y %.6f".format(item.id, x, y)
            } else {
                item.id + " переміщено"
            }
        invalidate()
    }

    private fun perform(action: Action) {
        when (action) {
            Action.UP -> nudge(0, -stepPx)
            Action.DOWN -> nudge(0, stepPx)
            Action.LEFT -> nudge(-stepPx, 0)
            Action.RIGHT -> nudge(stepPx, 0)
            Action.STEP -> {
                stepPx =
                    when (stepPx) {
                        1 -> 2
                        2 -> 5
                        5 -> 10
                        10 -> 20
                        else -> 1
                    }
                prefs.edit().putInt(KEY_STEP, stepPx).apply()
            }
            Action.PREV -> selectOnly((primary - 1 + objects.size) % objects.size)
            Action.NEXT -> selectOnly((primary + 1) % objects.size)
            Action.SAVE -> {
                saveOffsets()
                lastAction = "Збережено"
            }
            Action.RESET -> {
                editableSelectedIndices().forEach {
                    offsets[objects[it].id] = 0f to 0f
                }
                applyOffsets()
                saveOffsets()
                lastAction = "Скинуто " + objects[primary].id
            }
            Action.RESET_ALL -> {
                objects.forEach { offsets[it.id] = 0f to 0f }
                applyOffsets()
                saveOffsets()
                lastAction = "Скинуто все"
            }
            Action.EXPORT -> {
                lastAction = "Експорт"
                onExportRequested(exportTemplateJson())
            }
            Action.IMPORT -> {
                lastAction = "Імпорт"
                onImportRequested()
            }
            Action.GRID -> {
                gridEnabled = !gridEnabled
                prefs.edit().putBoolean(KEY_GRID, gridEnabled).apply()
            }
            Action.MAG -> {
                magnifierEnabled = !magnifierEnabled
                persistMagnifier()
            }
            Action.ZOOM -> {
                magnifierZoom =
                    when (magnifierZoom.toInt()) {
                        2 -> 3f
                        3 -> 4f
                        4 -> 6f
                        else -> 2f
                    }
                persistMagnifier()
            }
            Action.SIZE -> {
                magnifierSizeDp =
                    when (magnifierSizeDp) {
                        120 -> 180
                        180 -> 240
                        else -> 120
                    }
                persistMagnifier()
            }
            Action.FOLLOW -> {
                magnifierFollow = !magnifierFollow
                persistMagnifier()
            }
            Action.FREEZE -> {
                magnifierFreeze = !magnifierFreeze
                if (magnifierFreeze) {
                    captureFreeze()
                } else {
                    frozenBitmap?.recycle()
                    frozenBitmap = null
                }
                persistMagnifier()
            }
            Action.GUIDE_V -> {
                guideVEnabled = true
                guideVX = primaryCenter().first / width
                persistGuides()
            }
            Action.GUIDE_H -> {
                guideHEnabled = true
                guideHY = primaryCenter().second / height
                persistGuides()
            }
            Action.GUIDE_CLEAR -> {
                guideVEnabled = false
                guideHEnabled = false
                persistGuides()
            }
            Action.ACTIONS_TOGGLE -> {
                actionsExpanded = !actionsExpanded
                prefs.edit().putBoolean(KEY_ACTIONS_EXPANDED, actionsExpanded).apply()
            }
            Action.MAGNIFIER_TOGGLE -> {
                magnifierExpanded = !magnifierExpanded
                prefs.edit().putBoolean(KEY_MAGNIFIER_EXPANDED, magnifierExpanded).apply()
            }
            Action.GUIDES_TOGGLE -> {
                guidesExpanded = !guidesExpanded
                prefs.edit().putBoolean(KEY_GUIDES_EXPANDED, guidesExpanded).apply()
            }
            Action.NONE -> Unit
        }
        invalidate()
    }

    private fun captureFreeze() {
        if (sourceView.width <= 0 || sourceView.height <= 0) return
        frozenBitmap?.recycle()
        frozenBitmap =
            Bitmap.createBitmap(
                sourceView.width,
                sourceView.height,
                Bitmap.Config.ARGB_8888,
            ).also { bitmap ->
                sourceView.draw(Canvas(bitmap))
            }
        val center = primaryCenter()
        frozenTargetX = center.first
        frozenTargetY = center.second
    }

    private fun magnifierCenterForHit(): Pair<Float, Float> {
        val target = primaryCenter()
        return magnifierCenter(target.first, target.second)
    }

    private fun persistMagnifier() {
        prefs.edit()
            .putBoolean(KEY_MAG_ENABLED, magnifierEnabled)
            .putFloat(KEY_MAG_ZOOM, magnifierZoom)
            .putInt(KEY_MAG_SIZE, magnifierSizeDp)
            .putBoolean(KEY_MAG_FOLLOW, magnifierFollow)
            .putBoolean(KEY_MAG_FREEZE, magnifierFreeze)
            .putFloat(KEY_MAG_X, magnifierX)
            .putFloat(KEY_MAG_Y, magnifierY)
            .apply()
    }

    private fun persistGuides() {
        prefs.edit()
            .putBoolean(KEY_GUIDE_V_ENABLED, guideVEnabled)
            .putBoolean(KEY_GUIDE_H_ENABLED, guideHEnabled)
            .putFloat(KEY_GUIDE_V_X, guideVX)
            .putFloat(KEY_GUIDE_H_Y, guideHY)
            .apply()
    }

    private fun applyOffsets() {
        val rendererOffsets = mutableMapOf<String, Pair<Float, Float>>()
        objects.forEach { item ->
            rendererOffsets[item.rendererId] = offsets[item.id] ?: (0f to 0f)
        }
        sourceView.setEditorObjectOffsets(rendererOffsets)
    }

    private fun saveOffsets() {
        val edit = prefs.edit()
        objects.forEach { item ->
            val pair = offsets[item.id] ?: (0f to 0f)
            edit.putFloat("dx_" + item.id, pair.first)
            edit.putFloat("dy_" + item.id, pair.second)
        }
        edit.apply()
    }

    fun exportTemplateJson(): String {
        val root =
            JSONObject()
                .put("schema", "pulsedeck-object-template-v1")
                .put("mode", "modular-object-offsets")

        val array = JSONArray()
        objects.forEach { item ->
            val pair = offsets[item.id] ?: (0f to 0f)
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("rendererId", item.rendererId)
                    .put("dx", pair.first.toDouble())
                    .put("dy", pair.second.toDouble()),
            )
        }
        root.put("items", array)
        return root.toString(2)
    }

    fun importTemplateJson(raw: String) {
        val root = JSONObject(raw)
        require(root.optString("schema") == "pulsedeck-object-template-v1") {
            "Невідомий формат object template"
        }

        val array = root.getJSONArray("items")
        repeat(array.length()) { index ->
            val obj = array.getJSONObject(index)
            val id = obj.getString("id")
            if (objects.any { it.id == id }) {
                offsets[id] =
                    obj.getDouble("dx").toFloat() to
                        obj.getDouble("dy").toFloat()
            }
        }

        applyOffsets()
        saveOffsets()
        invalidate()
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    companion object {
        private const val PREFS = "pulsedeck_object_constructor"
        private const val KEY_PRIMARY = "primary"
        private const val KEY_STEP = "step"
        private const val KEY_GRID = "grid"
        private const val KEY_PANEL_X = "panel_x"
        private const val KEY_PANEL_Y = "panel_y"
        private const val KEY_ACTIONS_EXPANDED = "actions_expanded"
        private const val KEY_MAGNIFIER_EXPANDED = "magnifier_expanded"
        private const val KEY_GUIDES_EXPANDED = "guides_expanded"
        private const val KEY_MAG_ENABLED = "mag_enabled"
        private const val KEY_MAG_ZOOM = "mag_zoom"
        private const val KEY_MAG_SIZE = "mag_size"
        private const val KEY_MAG_FOLLOW = "mag_follow"
        private const val KEY_MAG_FREEZE = "mag_freeze"
        private const val KEY_MAG_X = "mag_x"
        private const val KEY_MAG_Y = "mag_y"
        private const val KEY_GUIDE_V_ENABLED = "guide_v_enabled"
        private const val KEY_GUIDE_H_ENABLED = "guide_h_enabled"
        private const val KEY_GUIDE_V_X = "guide_v_x"
        private const val KEY_GUIDE_H_Y = "guide_h_y"
        private const val LONG_PRESS_MS = 650L
    }
}
