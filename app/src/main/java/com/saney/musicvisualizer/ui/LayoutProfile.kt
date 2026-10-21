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
) {
    val isIdentity: Boolean
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
            .joinToString(";") { (id, a) -> "$id:${a.dx}:${a.dy}:${a.scale}" }

    fun decode(raw: String?): Map<String, LayoutAdjust> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(';').mapNotNull { part ->
            val bits = part.split(':')
            if (bits.size != 4 || bits[0].isBlank()) return@mapNotNull null
            val dx = bits[1].toFloatOrNull() ?: return@mapNotNull null
            val dy = bits[2].toFloatOrNull() ?: return@mapNotNull null
            val scale = bits[3].toFloatOrNull() ?: return@mapNotNull null
            if (!dx.isFinite() || !dy.isFinite() || !scale.isFinite()) return@mapNotNull null
            bits[0] to
                LayoutAdjust(
                    dx.coerceIn(-LayoutAdjust.MAX_SHIFT, LayoutAdjust.MAX_SHIFT),
                    dy.coerceIn(-LayoutAdjust.MAX_SHIFT, LayoutAdjust.MAX_SHIFT),
                    scale.coerceIn(LayoutAdjust.MIN_SCALE, LayoutAdjust.MAX_SCALE),
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
