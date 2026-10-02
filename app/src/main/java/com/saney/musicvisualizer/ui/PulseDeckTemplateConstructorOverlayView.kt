package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.hypot

class PulseDeckTemplateConstructorOverlayView(
    context: Context,
    private val sourceView: View,
    private val onExportRequested: (String) -> Unit = {},
    private val onImportRequested: () -> Unit = {},
) : View(context) {
    private data class Item(val id: String, val name: String, val x: Float, val y: Float)
    private enum class Action {
        UP, DOWN, LEFT, RIGHT, STEP, PREV, NEXT, RESET, RESET_ALL,
        SAVE, EXPORT, IMPORT, GRID, LOUPE, NONE,
    }

    private val reference = listOf(
        Item("C-01", "Back", 0.076036f, 0.044982f),
        Item("C-02", "Header title", 0.498148f, 0.044872f),
        Item("C-03", "Menu", 0.924074f, 0.044872f),
        Item("C-04", "Hero reactor", 0.498148f, 0.228205f),
        Item("C-05", "Favorite", 0.075926f, 0.463675f),
        Item("C-06", "Track info", 0.498148f, 0.480769f),
        Item("C-07", "Track more", 0.924074f, 0.463675f),
        Item("C-08", "Waveform", 0.498148f, 0.552137f),
        Item("C-09", "Progress", 0.498148f, 0.582479f),
        Item("C-10", "Transport rail", 0.500000f, 0.721367f),
        Item("C-11", "Shuffle", 0.107407f, 0.720940f),
        Item("C-12", "Previous", 0.285185f, 0.720940f),
        Item("C-13", "Play/Pause", 0.500926f, 0.720940f),
        Item("C-14", "Next", 0.714815f, 0.720940f),
        Item("C-15", "Repeat", 0.892593f, 0.720940f),
        Item("C-16", "Quick-actions rail", 0.501852f, 0.876068f),
        Item("C-17", "Theme", 0.157407f, 0.876068f),
        Item("C-18", "Board", 0.385185f, 0.876923f),
        Item("C-19", "Visualizer", 0.618519f, 0.876068f),
        Item("C-20", "Export", 0.850000f, 0.876068f),
    )

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val positions = mutableMapOf<String, Pair<Float, Float>>()
    private var selected = prefs.getInt(KEY_SELECTED, 0).coerceIn(0, reference.lastIndex)
    private var stepPx = prefs.getInt(KEY_STEP, 2).coerceIn(1, 20)
    private var grid = prefs.getBoolean(KEY_GRID, true)
    private var loupe = prefs.getBoolean(KEY_LOUPE, true)
    private var draggingItem = false
    private var draggingPanel = false
    private var panelX = prefs.getFloat(KEY_PANEL_X, 0.50f)
    private var panelY = prefs.getFloat(KEY_PANEL_Y, 0.77f)
    private val actionRects = mutableMapOf<Action, RectF>()
    private val panelRect = RectF()

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        color = Color.argb(150, 0, 220, 255)
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        color = Color.rgb(255, 166, 0)
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(1.5f)
        color = Color.WHITE
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(1f)
        color = Color.argb(65, 255, 255, 255)
        pathEffect = DashPathEffect(floatArrayOf(dp(4f), dp(6f)), 0f)
    }
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 5, 10, 16)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dp(12f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(185, 205, 220)
        textSize = dp(10f)
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 18, 34, 44)
    }

    init {
        reference.forEach { item ->
            positions[item.id] =
                prefs.getFloat("x_" + item.id, item.x) to
                    prefs.getFloat("y_" + item.id, item.y)
        }
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        if (grid) drawGrid(canvas)
        reference.forEachIndexed { index, item -> drawItem(canvas, index, item) }
        if (loupe) drawLoupe(canvas)
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

    private fun drawItem(canvas: Canvas, index: Int, item: Item) {
        val p = positions.getValue(item.id)
        val x = p.first * width
        val y = p.second * height
        val w = itemWidth(item) * width
        val h = itemHeight(item) * height
        val rect = RectF(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f)
        canvas.drawRoundRect(rect, dp(8f), dp(8f), if (index == selected) selectedPaint else boxPaint)
        canvas.drawLine(x - dp(7f), y, x + dp(7f), y, centerPaint)
        canvas.drawLine(x, y - dp(7f), x, y + dp(7f), centerPaint)
        textPaint.color = if (index == selected) Color.rgb(255, 190, 60) else Color.WHITE
        canvas.drawText(item.id, rect.left + dp(4f), rect.top - dp(4f), textPaint)
    }

    private fun drawLoupe(canvas: Canvas) {
        val item = reference[selected]
        val p = positions.getValue(item.id)
        val sx = p.first * width
        val sy = p.second * height
        val radius = dp(62f)
        val cx = width - radius - dp(12f)
        val cy = radius + dp(18f)
        val path = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
        val save = canvas.save()
        canvas.clipPath(path)
        canvas.drawColor(Color.rgb(4, 8, 12))
        val scale = 3f
        canvas.save()
        canvas.translate(cx - sx * scale, cy - sy * scale)
        canvas.scale(scale, scale)
        sourceView.draw(canvas)
        canvas.restore()
        canvas.restoreToCount(save)
        canvas.drawCircle(cx, cy, radius, selectedPaint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, centerPaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, centerPaint)
    }

    private fun drawPanel(canvas: Canvas) {
        actionRects.clear()
        val pw = minOf(width * 0.94f, dp(420f))
        val ph = dp(244f)
        val cx = (panelX * width).coerceIn(pw / 2f, width - pw / 2f)
        val cy = (panelY * height).coerceIn(ph / 2f, height - ph / 2f)
        panelRect.set(cx - pw / 2f, cy - ph / 2f, cx + pw / 2f, cy + ph / 2f)
        canvas.drawRoundRect(panelRect, dp(16f), dp(16f), panelPaint)

        val item = reference[selected]
        val pos = positions.getValue(item.id)
        textPaint.color = Color.WHITE
        canvas.drawText("TEMPLATE  •  " + item.id + " " + item.name, panelRect.left + dp(12f), panelRect.top + dp(22f), textPaint)
        canvas.drawText(
            String.format(java.util.Locale.US, "x %.6f   y %.6f   step %dpx", pos.first, pos.second, stepPx),
            panelRect.left + dp(12f),
            panelRect.top + dp(42f),
            smallPaint,
        )

        val labels = listOf(
            Action.UP to "UP", Action.LEFT to "LEFT", Action.DOWN to "DOWN", Action.RIGHT to "RIGHT", Action.STEP to (stepPx.toString() + "px"),
            Action.PREV to "PREV", Action.NEXT to "NEXT", Action.RESET to "RESET", Action.SAVE to "SAVE",
            Action.GRID to if (grid) "GRID ON" else "GRID OFF",
            Action.LOUPE to if (loupe) "MAG ON" else "MAG OFF",
            Action.IMPORT to "IMPORT", Action.EXPORT to "EXPORT", Action.RESET_ALL to "RESET ALL",
        )
        val cols = 5
        val gap = dp(6f)
        val bw = (pw - dp(24f) - gap * (cols - 1)) / cols
        val bh = dp(46f)
        labels.forEachIndexed { i, pair ->
            val row = i / cols
            val col = i % cols
            val left = panelRect.left + dp(12f) + col * (bw + gap)
            val top = panelRect.top + dp(56f) + row * (bh + gap)
            val rect = RectF(left, top, left + bw, top + bh)
            actionRects[pair.first] = rect
            canvas.drawRoundRect(rect, dp(8f), dp(8f), buttonPaint)
            val tw = textPaint.measureText(pair.second)
            canvas.drawText(pair.second, rect.centerX() - tw / 2f, rect.centerY() + dp(4f), textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val action = actionAt(event.x, event.y)
                if (action != Action.NONE) {
                    perform(action)
                    return true
                }
                if (panelRect.contains(event.x, event.y)) {
                    draggingPanel = true
                    return true
                }
                val hit = nearestItem(event.x, event.y)
                if (hit >= 0) {
                    select(hit)
                    draggingItem = true
                    setSelectedPosition(event.x / width, event.y / height)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggingItem) {
                    setSelectedPosition(event.x / width, event.y / height)
                    invalidate()
                    return true
                }
                if (draggingPanel) {
                    panelX = (event.x / width).coerceIn(0f, 1f)
                    panelY = (event.y / height).coerceIn(0f, 1f)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (draggingItem) saveSelected()
                if (draggingPanel) {
                    prefs.edit().putFloat(KEY_PANEL_X, panelX).putFloat(KEY_PANEL_Y, panelY).apply()
                }
                draggingItem = false
                draggingPanel = false
                return true
            }
        }
        return true
    }

    private fun nearestItem(x: Float, y: Float): Int {
        var best = -1
        var bestDistance = dp(54f)
        reference.forEachIndexed { index, item ->
            val p = positions.getValue(item.id)
            val d = hypot(x - p.first * width, y - p.second * height)
            if (d < bestDistance) {
                bestDistance = d
                best = index
            }
        }
        return best
    }

    private fun actionAt(x: Float, y: Float): Action =
        actionRects.entries.firstOrNull { it.value.contains(x, y) }?.key ?: Action.NONE

    private fun perform(action: Action) {
        when (action) {
            Action.UP -> nudge(0, -stepPx)
            Action.DOWN -> nudge(0, stepPx)
            Action.LEFT -> nudge(-stepPx, 0)
            Action.RIGHT -> nudge(stepPx, 0)
            Action.STEP -> {
                stepPx = when (stepPx) { 1 -> 2; 2 -> 5; 5 -> 10; 10 -> 20; else -> 1 }
                prefs.edit().putInt(KEY_STEP, stepPx).apply()
            }
            Action.PREV -> select((selected - 1 + reference.size) % reference.size)
            Action.NEXT -> select((selected + 1) % reference.size)
            Action.RESET -> {
                val item = reference[selected]
                positions[item.id] = item.x to item.y
                saveSelected()
            }
            Action.RESET_ALL -> {
                reference.forEach { positions[it.id] = it.x to it.y }
                saveAll()
            }
            Action.SAVE -> saveAll()
            Action.EXPORT -> onExportRequested(exportTemplateJson())
            Action.IMPORT -> onImportRequested()
            Action.GRID -> {
                grid = !grid
                prefs.edit().putBoolean(KEY_GRID, grid).apply()
            }
            Action.LOUPE -> {
                loupe = !loupe
                prefs.edit().putBoolean(KEY_LOUPE, loupe).apply()
            }
            Action.NONE -> Unit
        }
        invalidate()
    }

    private fun select(index: Int) {
        selected = index.coerceIn(0, reference.lastIndex)
        prefs.edit().putInt(KEY_SELECTED, selected).apply()
    }

    private fun nudge(dx: Int, dy: Int) {
        val item = reference[selected]
        val p = positions.getValue(item.id)
        setSelectedPosition(p.first + dx / width.toFloat(), p.second + dy / height.toFloat())
        saveSelected()
    }

    private fun setSelectedPosition(x: Float, y: Float) {
        positions[reference[selected].id] = x.coerceIn(0f, 1f) to y.coerceIn(0f, 1f)
    }

    private fun saveSelected() {
        val item = reference[selected]
        val p = positions.getValue(item.id)
        prefs.edit().putFloat("x_" + item.id, p.first).putFloat("y_" + item.id, p.second).apply()
    }

    private fun saveAll() {
        val edit = prefs.edit()
        reference.forEach { item ->
            val p = positions.getValue(item.id)
            edit.putFloat("x_" + item.id, p.first).putFloat("y_" + item.id, p.second)
        }
        edit.apply()
    }

    fun exportTemplateJson(): String {
        val root = JSONObject()
        root.put("schema", "pulsedeck-template-v1")
        root.put("reference", "PULSEDECK_CENTER_CALIBRATION")
        val items = JSONArray()
        reference.forEach { item ->
            val p = positions.getValue(item.id)
            items.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("x", p.first.toDouble())
                    .put("y", p.second.toDouble()),
            )
        }
        root.put("items", items)
        return root.toString(2)
    }

    fun importTemplateJson(raw: String) {
        val root = JSONObject(raw)
        require(root.optString("schema") == "pulsedeck-template-v1") { "Невідомий формат шаблона" }
        val array = root.getJSONArray("items")
        repeat(array.length()) { i ->
            val obj = array.getJSONObject(i)
            val id = obj.getString("id")
            if (reference.any { it.id == id }) {
                positions[id] =
                    obj.getDouble("x").toFloat().coerceIn(0f, 1f) to
                        obj.getDouble("y").toFloat().coerceIn(0f, 1f)
            }
        }
        saveAll()
        invalidate()
    }

    private fun itemWidth(item: Item): Float = when (item.id) {
        "C-02", "C-06", "C-08", "C-09" -> 0.46f
        "C-04" -> 0.42f
        "C-10", "C-16" -> 0.88f
        "C-13" -> 0.16f
        else -> 0.12f
    }

    private fun itemHeight(item: Item): Float = when (item.id) {
        "C-04" -> 0.22f
        "C-06" -> 0.075f
        "C-08", "C-09" -> 0.045f
        "C-10" -> 0.12f
        "C-16" -> 0.10f
        else -> 0.065f
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        private const val PREFS = "pulsedeck_template_constructor"
        private const val KEY_SELECTED = "selected"
        private const val KEY_STEP = "step"
        private const val KEY_GRID = "grid"
        private const val KEY_LOUPE = "loupe"
        private const val KEY_PANEL_X = "panel_x"
        private const val KEY_PANEL_Y = "panel_y"
    }
}
