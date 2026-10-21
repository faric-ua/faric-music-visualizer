package com.saney.musicvisualizer.ui

/**
 * V104 «Утиліти» (faric 21.10 18:35–23:43): faric moves and resizes the parts of the main screen
 * himself and presses «Зберегти» — the app remembers it (not Claude in the code). Every position
 * («Книжка», «Альбом», «Відео 9:16») keeps its own set. These are adjustments ON TOP of the approved
 * template (the locked centres stay as they are): an empty set = the template exactly («Еталон»).
 * Pure Kotlin — tested on JVM.
 */
enum class LayoutProfileId(
    val label: String,
) {
    PORTRAIT("Книжка"),
    LANDSCAPE("Альбом"),
    VIDEO_9X16("Відео 9:16"),
}

/** Shift (share of the screen width / height) and size (1 = as in the template) of one group. */
data class LayoutAdjust(
    val dx: Float = 0f,
    val dy: Float = 0f,
    val scale: Float = 1f,
    /** V123: how much of the part shows (1 = fully, 0.3 = the visualization shines through). */
    val alpha: Float = 1f,
    /** V123: switched off in this position (☰ → Видимість). */
    val hidden: Boolean = false,
) {
    val isIdentity: Boolean
        get() = isPlaceIdentity && kotlin.math.abs(alpha - 1f) < 1e-3f && !hidden

    /** No move and no size change (look settings aside). */
    val isPlaceIdentity: Boolean
        get() = kotlin.math.abs(dx) < 1e-5f && kotlin.math.abs(dy) < 1e-5f && kotlin.math.abs(scale - 1f) < 1e-4f

    fun moved(
        ddx: Float,
        ddy: Float,
    ): LayoutAdjust = copy(dx = (dx + ddx).coerceIn(-MAX_SHIFT, MAX_SHIFT), dy = (dy + ddy).coerceIn(-MAX_SHIFT, MAX_SHIFT))

    /** [percent] steps of size: +1 = one percent bigger. */
    fun resized(percent: Int): LayoutAdjust {
        val next = Math.round((scale * 100f) + percent) / 100f
        return copy(scale = next.coerceIn(MIN_SCALE, MAX_SCALE))
    }

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 6f
        const val MAX_SHIFT = 0.6f
    }
}

/**
 * What faric can pick in the layout editor: parts that move together (a panel with its buttons,
 * the title with ♡ and ⋮ …). [rule] is shown next to the size buttons.
 */
data class LayoutGroup(
    val id: String,
    val label: String,
    val rule: String,
    val members: List<String>,
)

object LayoutGroups {
    private const val PROPORTIONAL = "лише пропорційно"

    val ALL: List<LayoutGroup> =
        listOf(
            LayoutGroup("cover", "Обкладинка", PROPORTIONAL, listOf("cover_box")),
            LayoutGroup("back", "‹ Назад", PROPORTIONAL, listOf("back")),
            LayoutGroup("menu", "⋮ Меню", PROPORTIONAL, listOf("menu")),
            LayoutGroup("header", "Шапка PulseDeck", PROPORTIONAL, listOf("header_title")),
            LayoutGroup(
                "track",
                "Назва з ♡ і ⋮",
                PROPORTIONAL,
                listOf("favorite", "track_more", "track_info", "track_artist", "track_status"),
            ),
            LayoutGroup(
                "progress",
                "Хвиля і смуга часу",
                PROPORTIONAL,
                listOf("waveform", "progress_line", "progress_time", "progress_thumb", "progress_group"),
            ),
            LayoutGroup(
                "transport",
                "Керування",
                PROPORTIONAL,
                listOf("transport_rail", "shuffle", "previous", "play_pause", "next", "repeat"),
            ),
            LayoutGroup(
                "quick",
                "Швидкі дії",
                PROPORTIONAL,
                listOf("quick_rail", "theme", "board", "visualizer", "export"),
            ),
        )

    private val byMember: Map<String, String> =
        ALL.flatMap { group -> group.members.map { it to group.id } }.toMap()

    fun groupOf(objectId: String): String? = byMember[objectId]

    fun byId(id: String): LayoutGroup? = ALL.firstOrNull { it.id == id }
}

/** One profile as text for SharedPreferences: «id:dx:dy:scale;…» (no JSON — tested on JVM). */
object LayoutProfileCodec {
    fun encode(adjusts: Map<String, LayoutAdjust>): String =
        adjusts
            .filterValues { !it.isIdentity }
            .toSortedMap()
            .entries
            .joinToString(";") { (id, a) -> "$id:${a.dx}:${a.dy}:${a.scale}:${a.alpha}:${if (a.hidden) 1 else 0}" }

    fun decode(raw: String?): Map<String, LayoutAdjust> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(';').mapNotNull { part ->
            val bits = part.split(':')
            // v0.21.x wrote 4 fields; from v0.21.4 — 6 (+ alpha, hidden).
            if ((bits.size != 4 && bits.size != 6) || bits[0].isBlank()) return@mapNotNull null
            val dx = bits[1].toFloatOrNull() ?: return@mapNotNull null
            val dy = bits[2].toFloatOrNull() ?: return@mapNotNull null
            val scale = bits[3].toFloatOrNull() ?: return@mapNotNull null
            val alpha = if (bits.size == 6) bits[4].toFloatOrNull() ?: return@mapNotNull null else 1f
            val hidden = bits.size == 6 && bits[5] == "1"
            if (!dx.isFinite() || !dy.isFinite() || !scale.isFinite() || !alpha.isFinite()) return@mapNotNull null
            bits[0] to
                LayoutAdjust(
                    dx.coerceIn(-LayoutAdjust.MAX_SHIFT, LayoutAdjust.MAX_SHIFT),
                    dy.coerceIn(-LayoutAdjust.MAX_SHIFT, LayoutAdjust.MAX_SHIFT),
                    scale.coerceIn(LayoutAdjust.MIN_SCALE, LayoutAdjust.MAX_SCALE),
                    alpha.coerceIn(0f, 1f),
                    hidden,
                )
        }.toMap()
    }
}

/** Which set the screen uses: a wide view is «Альбом»; the MP4 uses «Відео 9:16», if faric made one. */
object LayoutProfileChoice {
    fun forView(
        width: Int,
        height: Int,
        forced: LayoutProfileId?,
        saved: Map<LayoutProfileId, Map<String, LayoutAdjust>>,
    ): Map<String, LayoutAdjust> {
        if (forced == LayoutProfileId.VIDEO_9X16) {
            return saved[LayoutProfileId.VIDEO_9X16]?.takeIf { it.isNotEmpty() }
                ?: saved[LayoutProfileId.PORTRAIT].orEmpty()
        }
        val id = forced ?: if (width > height) LayoutProfileId.LANDSCAPE else LayoutProfileId.PORTRAIT
        return saved[id].orEmpty()
    }
}

/** A rectangle in screen pixels (pure — the editor's guides are tested on JVM). */
data class LayoutBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    /** Where a group's template box ends up with [adjust] (size from its centre, then the shift). */
    fun adjusted(
        adjust: LayoutAdjust,
        screenW: Float,
        screenH: Float,
    ): LayoutBox {
        val halfW = (right - left) / 2f * adjust.scale
        val halfH = (bottom - top) / 2f * adjust.scale
        val cx = centerX + adjust.dx * screenW
        val cy = centerY + adjust.dy * screenH
        return LayoutBox(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
    }
}

/**
 * V121 (faric 22.10 00:45: «прив'язка по центрах — веду кружочок, і коли він став по центру екрана
 * або по центру іншого, підсвічується лінія»). While a part is dragged, its centre «sticks» to the
 * screen's centre lines and to the centres of the other parts when it comes within [threshold] px.
 */
object LayoutSnap {
    data class Result(
        val adjust: LayoutAdjust,
        /** Vertical guide (x in screen px) the centre sits on, or null. */
        val guideX: Float?,
        /** Horizontal guide (y in screen px), or null. */
        val guideY: Float?,
    )

    fun snap(
        raw: LayoutAdjust,
        base: LayoutBox,
        screenW: Float,
        screenH: Float,
        targetsX: List<Float>,
        targetsY: List<Float>,
        threshold: Float,
    ): Result {
        val box = base.adjusted(raw, screenW, screenH)
        val nearX = targetsX.minByOrNull { kotlin.math.abs(it - box.centerX) }?.takeIf { kotlin.math.abs(it - box.centerX) <= threshold }
        val nearY = targetsY.minByOrNull { kotlin.math.abs(it - box.centerY) }?.takeIf { kotlin.math.abs(it - box.centerY) <= threshold }
        var adjust = raw
        if (nearX != null) adjust = adjust.copy(dx = raw.dx + (nearX - box.centerX) / screenW)
        if (nearY != null) adjust = adjust.copy(dy = raw.dy + (nearY - box.centerY) / screenH)
        return Result(adjust, nearX, nearY)
    }
}

/** V124 (faric 22.10 01:45): what the dragged part sticks to. */
enum class SnapMode(
    val label: String,
) {
    OFF("Вимкнено"),
    CENTERS("По центрах"),
    EDGES("По краях (усі 4)"),
    LEFT("По лівому краю"),
    RIGHT("По правому краю"),
    TOP("По верхньому краю"),
    BOTTOM("По нижньому краю"),
    ALL("Центри й краї"),
}

/**
 * V124: snapping by centres and / or edges. The moving part's chosen lines (left / centre / right,
 * top / centre / bottom) stick to the same kind of lines of the screen and of the other parts.
 */
object LayoutSnapEdges {
    private fun xFeatures(
        box: LayoutBox,
        mode: SnapMode,
    ): List<Float> =
        when (mode) {
            SnapMode.OFF, SnapMode.TOP, SnapMode.BOTTOM -> emptyList()
            SnapMode.CENTERS -> listOf(box.centerX)
            SnapMode.EDGES -> listOf(box.left, box.right)
            SnapMode.LEFT -> listOf(box.left)
            SnapMode.RIGHT -> listOf(box.right)
            SnapMode.ALL -> listOf(box.left, box.centerX, box.right)
        }

    private fun yFeatures(
        box: LayoutBox,
        mode: SnapMode,
    ): List<Float> =
        when (mode) {
            SnapMode.OFF, SnapMode.LEFT, SnapMode.RIGHT -> emptyList()
            SnapMode.CENTERS -> listOf(box.centerY)
            SnapMode.EDGES -> listOf(box.top, box.bottom)
            SnapMode.TOP -> listOf(box.top)
            SnapMode.BOTTOM -> listOf(box.bottom)
            SnapMode.ALL -> listOf(box.top, box.centerY, box.bottom)
        }

    fun snap(
        raw: LayoutAdjust,
        base: LayoutBox,
        screenW: Float,
        screenH: Float,
        others: List<LayoutBox>,
        mode: SnapMode,
        threshold: Float,
    ): LayoutSnap.Result {
        if (mode == SnapMode.OFF) return LayoutSnap.Result(raw, null, null)
        val box = base.adjusted(raw, screenW, screenH)
        val screen = LayoutBox(0f, 0f, screenW, screenH)
        val all = others + screen
        val targetsX = all.flatMap { xFeatures(it, mode) }
        val targetsY = all.flatMap { yFeatures(it, mode) }
        val bestX =
            xFeatures(box, mode)
                .flatMap { f -> targetsX.map { t -> Triple(f, t, kotlin.math.abs(t - f)) } }
                .filter { it.third <= threshold }
                .minByOrNull { it.third }
        val bestY =
            yFeatures(box, mode)
                .flatMap { f -> targetsY.map { t -> Triple(f, t, kotlin.math.abs(t - f)) } }
                .filter { it.third <= threshold }
                .minByOrNull { it.third }
        var adjust = raw
        if (bestX != null) adjust = adjust.copy(dx = raw.dx + (bestX.second - bestX.first) / screenW)
        if (bestY != null) adjust = adjust.copy(dy = raw.dy + (bestY.second - bestY.first) / screenH)
        return LayoutSnap.Result(adjust, bestX?.second, bestY?.second)
    }
}
