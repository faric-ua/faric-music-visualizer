package com.saney.musicvisualizer.projectm

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Movable PulseDeck-styled authoring panel for projectM.
 *
 * The panel is intentionally non-modal and draggable so the composition remains
 * visible while FG Center / Edge FX / AUTO settings are tuned.
 */
object ProjectMSettingsPanel {
    enum class Section {
        AUTO,
        CENTER,
        EDGE,
    }

    fun show(
        context: Context,
        stateStore: ProjectMStateStore,
        initialSection: Section = Section.CENTER,
        onAutoChanged: (enabled: Boolean, seconds: Int) -> Unit = { _, _ -> },
        onTuningChanged: (ProjectMForegroundTuning) -> Unit = {},
    ): Dialog {
        val metrics =
            context.resources.displayMetrics
        val screenWidth =
            metrics.widthPixels
        val screenHeight =
            metrics.heightPixels

        val panelWidth =
            (screenWidth * 0.88f)
                .toInt()
                .coerceAtLeast(
                    context.dp(300),
                )
                .coerceAtMost(
                    screenWidth,
                )
        val panelHeight =
            (screenHeight * 0.46f)
                .toInt()
                .coerceAtLeast(
                    context.dp(330),
                )
                .coerceAtMost(
                    (screenHeight * 0.70f)
                        .toInt(),
                )

        val dialog =
            Dialog(
                context,
            ).apply {
                requestWindowFeature(
                    Window.FEATURE_NO_TITLE,
                )
                setCancelable(
                    true,
                )
                setCanceledOnTouchOutside(
                    false,
                )
            }

        var section =
            initialSection
        var tuning =
            stateStore.foregroundTuning()
        var autoEnabled =
            stateStore.autoEnabled
        var autoSeconds =
            stateStore.autoSwitchSeconds

        val prefs =
            context.getSharedPreferences(
                "projectm_authoring_panel",
                Context.MODE_PRIVATE,
            )

        val panel =
            LinearLayout(
                context,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    context.dp(10),
                    context.dp(7),
                    context.dp(10),
                    context.dp(10),
                )
                background =
                    rounded(
                        context = context,
                        fill =
                            Color.argb(
                                248,
                                22,
                                25,
                                29,
                            ),
                        radiusDp = 24,
                        stroke =
                            Color.argb(
                                105,
                                255,
                                255,
                                255,
                            ),
                    )
            }

        val header =
            LinearLayout(
                context,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    context.dp(7),
                    context.dp(2),
                    context.dp(2),
                    context.dp(2),
                )
            }

        val title =
            TextView(
                context,
            ).apply {
                text =
                    "☰  projectM · налаштування · тягни"
                textSize =
                    15f
                setTextColor(
                    Color.WHITE,
                )
                typeface =
                    Typeface.DEFAULT_BOLD
                includeFontPadding =
                    false
            }

        val resetPosition =
            iconButton(
                context = context,
                text = "◎",
                description =
                    "Повернути панель у центр",
            )

        val close =
            iconButton(
                context = context,
                text = "✕",
                description =
                    "Закрити панель",
            ).apply {
                setOnClickListener {
                    dialog.dismiss()
                }
            }

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        header.addView(
            resetPosition,
        )
        header.addView(
            close,
        )
        panel.addView(
            header,
        )

        val tabs =
            LinearLayout(
                context,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
                setPadding(
                    context.dp(2),
                    context.dp(5),
                    context.dp(2),
                    context.dp(5),
                )
            }

        val autoTab =
            tabButton(
                context,
                "AUTO",
            )
        val centerTab =
            tabButton(
                context,
                "CENTER",
            )
        val edgeTab =
            tabButton(
                context,
                "EDGE FX",
            )

        listOf(
            autoTab,
            centerTab,
            edgeTab,
        ).forEachIndexed { index, view ->
            tabs.addView(
                view,
                LinearLayout.LayoutParams(
                    0,
                    context.dp(44),
                    1f,
                ).apply {
                    if (index > 0) {
                        marginStart =
                            context.dp(5)
                    }
                },
            )
        }

        panel.addView(
            tabs,
        )

        val body =
            LinearLayout(
                context,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    context.dp(4),
                    context.dp(2),
                    context.dp(4),
                    context.dp(8),
                )
            }

        val scroll =
            ScrollView(
                context,
            ).apply {
                isFillViewport =
                    false
                addView(
                    body,
                )
            }

        panel.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        fun persistTuning(
            next: ProjectMForegroundTuning,
        ) {
            tuning =
                next.sanitized()
            stateStore.saveForegroundTuning(
                tuning,
            )
            onTuningChanged(
                tuning,
            )
        }

        fun persistAuto(
            enabled: Boolean,
            seconds: Int,
        ) {
            autoEnabled =
                enabled
            autoSeconds =
                seconds
            stateStore.autoEnabled =
                enabled
            stateStore.autoSwitchSeconds =
                seconds
            onAutoChanged(
                enabled,
                seconds,
            )
        }

        fun sectionButtonState(
            view: TextView,
            selected: Boolean,
        ) {
            view.setTextColor(
                if (selected) {
                    Color.BLACK
                } else {
                    Color.WHITE
                },
            )
            view.background =
                rounded(
                    context = context,
                    fill =
                        if (selected) {
                            COLOR_ORANGE
                        } else {
                            COLOR_TILE
                        },
                    radiusDp = 18,
                    stroke =
                        if (selected) {
                            COLOR_ORANGE
                        } else {
                            Color.argb(
                                105,
                                35,
                                211,
                                238,
                            )
                        },
                )
        }

        fun addHint(
            text: String,
        ) {
            body.addView(
                TextView(
                    context,
                ).apply {
                    this.text =
                        text
                    textSize =
                        12f
                    setTextColor(
                        COLOR_MUTED,
                    )
                    includeFontPadding =
                        false
                    setPadding(
                        context.dp(5),
                        context.dp(3),
                        context.dp(5),
                        context.dp(8),
                    )
                },
            )
        }

        fun addValueRow(
            label: String,
            value: () -> Float,
            step: Float,
            format: (Float) -> String,
            update: (Float) -> Unit,
        ) {
            val row =
                LinearLayout(
                    context,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                    setPadding(
                        context.dp(4),
                        context.dp(2),
                        context.dp(4),
                        context.dp(2),
                    )
                }

            row.addView(
                TextView(
                    context,
                ).apply {
                    text =
                        label
                    textSize =
                        13.5f
                    setTextColor(
                        Color.WHITE,
                    )
                    includeFontPadding =
                        false
                },
                LinearLayout.LayoutParams(
                    0,
                    context.dp(43),
                    1f,
                ).apply {
                    gravity =
                        Gravity.CENTER_VERTICAL
                },
            )

            val valueView =
                TextView(
                    context,
                ).apply {
                    text =
                        format(
                            value(),
                        )
                    textSize =
                        13.5f
                    setTextColor(
                        Color.WHITE,
                    )
                    gravity =
                        Gravity.CENTER
                    includeFontPadding =
                        false
                }

            fun applyDelta(
                delta: Float,
            ) {
                update(
                    value() +
                        delta,
                )
                valueView.text =
                    format(
                        value(),
                    )
            }

            row.addView(
                smallButton(
                    context,
                    "−",
                ) {
                    applyDelta(
                        -step,
                    )
                },
            )
            row.addView(
                valueView,
                LinearLayout.LayoutParams(
                    context.dp(76),
                    context.dp(43),
                ),
            )
            row.addView(
                smallButton(
                    context,
                    "+",
                ) {
                    applyDelta(
                        step,
                    )
                },
            )

            body.addView(
                row,
            )
        }

        lateinit var rebuild: () -> Unit

        fun buildAuto() {
            addHint(
                "Автозміна projectM preset. Вибір 5 / 10 / 15 секунд одразу вмикає AUTO.",
            )

            val state =
                TextView(
                    context,
                ).apply {
                    textSize =
                        14f
                    gravity =
                        Gravity.CENTER
                    typeface =
                        Typeface.DEFAULT_BOLD
                    includeFontPadding =
                        false
                    setPadding(
                        context.dp(10),
                        context.dp(9),
                        context.dp(10),
                        context.dp(9),
                    )
                }

            fun refreshState() {
                state.text =
                    if (autoEnabled) {
                        "● AUTO · ${autoSeconds}s"
                    } else {
                        "○ AUTO вимкнено"
                    }
                state.setTextColor(
                    if (autoEnabled) {
                        COLOR_CYAN
                    } else {
                        COLOR_MUTED
                    },
                )
            }

            refreshState()
            state.background =
                rounded(
                    context,
                    COLOR_TILE,
                    18,
                    Color.argb(
                        100,
                        255,
                        255,
                        255,
                    ),
                )
            state.setOnClickListener {
                persistAuto(
                    enabled =
                        !autoEnabled,
                    seconds =
                        autoSeconds,
                )
                refreshState()
            }
            body.addView(
                state,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    context.dp(48),
                ).apply {
                    bottomMargin =
                        context.dp(8)
                },
            )

            val row =
                LinearLayout(
                    context,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER
                }

            intArrayOf(
                5,
                10,
                15,
            ).forEachIndexed { index, seconds ->
                row.addView(
                    TextView(
                        context,
                    ).apply {
                        text =
                            "${seconds}s"
                        textSize =
                            14f
                        gravity =
                            Gravity.CENTER
                        typeface =
                            Typeface.DEFAULT_BOLD
                        setTextColor(
                            if (
                                autoEnabled &&
                                autoSeconds ==
                                    seconds
                            ) {
                                Color.BLACK
                            } else {
                                Color.WHITE
                            },
                        )
                        background =
                            rounded(
                                context,
                                if (
                                    autoEnabled &&
                                    autoSeconds ==
                                        seconds
                                ) {
                                    COLOR_ORANGE
                                } else {
                                    COLOR_TILE
                                },
                                18,
                                Color.argb(
                                    100,
                                    35,
                                    211,
                                    238,
                                ),
                            )
                        setOnClickListener {
                            persistAuto(
                                enabled = true,
                                seconds = seconds,
                            )
                            rebuild()
                        }
                    },
                    LinearLayout.LayoutParams(
                        0,
                        context.dp(48),
                        1f,
                    ).apply {
                        if (index > 0) {
                            marginStart =
                                context.dp(6)
                        }
                    },
                )
            }

            body.addView(
                row,
            )
        }

        fun buildCenter() {
            addHint(
                "FG Center змінюється наживо. Панель можна відтягнути вбік, щоб бачити результат.",
            )

            addValueRow(
                label = "Масштаб",
                value = {
                    tuning.centerScale
                },
                step = 0.05f,
                format = {
                    "%.2fx".format(
                        it,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            centerScale =
                                next,
                        ),
                    )
                },
            )
            addValueRow(
                label = "Поворот",
                value = {
                    tuning.centerRotationDegrees
                },
                step = 5f,
                format = {
                    "%.0f°".format(
                        it,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            centerRotationDegrees =
                                next,
                        ),
                    )
                },
            )
            addValueRow(
                label = "Прозорість",
                value = {
                    tuning.centerOpacity
                },
                step = 0.05f,
                format = {
                    "%.0f%%".format(
                        it *
                            100f,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            centerOpacity =
                                next,
                        ),
                    )
                },
            )
            addValueRow(
                label = "Реакція Bass",
                value = {
                    tuning.centerBassGain
                },
                step = 0.10f,
                format = {
                    "%.1fx".format(
                        it,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            centerBassGain =
                                next,
                        ),
                    )
                },
            )
            addValueRow(
                label = "Реакція Mid",
                value = {
                    tuning.centerMidGain
                },
                step = 0.10f,
                format = {
                    "%.1fx".format(
                        it,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            centerMidGain =
                                next,
                        ),
                    )
                },
            )
            addValueRow(
                label = "Реакція High",
                value = {
                    tuning.centerHighGain
                },
                step = 0.10f,
                format = {
                    "%.1fx".format(
                        it,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            centerHighGain =
                                next,
                        ),
                    )
                },
            )
            addValueRow(
                label = "Реакція Beat",
                value = {
                    tuning.centerBeatGain
                },
                step = 0.10f,
                format = {
                    "%.1fx".format(
                        it,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            centerBeatGain =
                                next,
                        ),
                    )
                },
            )

            body.addView(
                resetButton(
                    context,
                    "Скинути Center",
                ) {
                    persistTuning(
                        tuning.copy(
                            centerScale = 1f,
                            centerRotationDegrees = 0f,
                            centerOpacity = 1f,
                            centerBassGain = 1f,
                            centerMidGain = 1f,
                            centerHighGain = 1f,
                            centerBeatGain = 1f,
                        ),
                    )
                    rebuild()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    context.dp(48),
                ).apply {
                    topMargin =
                        context.dp(7)
                },
            )
        }

        fun buildEdge() {
            addHint(
                "FG Edge FX — окремий боковий/крайовий шар. Налаштовується незалежно від Center.",
            )

            addValueRow(
                label = "Прозорість",
                value = {
                    tuning.edgeOpacity
                },
                step = 0.05f,
                format = {
                    "%.0f%%".format(
                        it *
                            100f,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            edgeOpacity =
                                next,
                        ),
                    )
                },
            )
            addValueRow(
                label = "Реакція Bass",
                value = {
                    tuning.edgeBassGain
                },
                step = 0.10f,
                format = {
                    "%.1fx".format(
                        it,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            edgeBassGain =
                                next,
                        ),
                    )
                },
            )
            addValueRow(
                label = "Реакція High",
                value = {
                    tuning.edgeHighGain
                },
                step = 0.10f,
                format = {
                    "%.1fx".format(
                        it,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            edgeHighGain =
                                next,
                        ),
                    )
                },
            )
            addValueRow(
                label = "Реакція Beat",
                value = {
                    tuning.edgeBeatGain
                },
                step = 0.10f,
                format = {
                    "%.1fx".format(
                        it,
                    )
                },
                update = { next ->
                    persistTuning(
                        tuning.copy(
                            edgeBeatGain =
                                next,
                        ),
                    )
                },
            )

            body.addView(
                resetButton(
                    context,
                    "Скинути Edge FX",
                ) {
                    persistTuning(
                        tuning.copy(
                            edgeOpacity = 1f,
                            edgeBassGain = 1f,
                            edgeHighGain = 1f,
                            edgeBeatGain = 1f,
                        ),
                    )
                    rebuild()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    context.dp(48),
                ).apply {
                    topMargin =
                        context.dp(7)
                },
            )
        }

        rebuild = {
            body.removeAllViews()

            sectionButtonState(
                autoTab,
                section ==
                    Section.AUTO,
            )
            sectionButtonState(
                centerTab,
                section ==
                    Section.CENTER,
            )
            sectionButtonState(
                edgeTab,
                section ==
                    Section.EDGE,
            )

            when (section) {
                Section.AUTO ->
                    buildAuto()

                Section.CENTER ->
                    buildCenter()

                Section.EDGE ->
                    buildEdge()
            }
        }

        autoTab.setOnClickListener {
            section =
                Section.AUTO
            rebuild()
        }
        centerTab.setOnClickListener {
            section =
                Section.CENTER
            rebuild()
        }
        edgeTab.setOnClickListener {
            section =
                Section.EDGE
            rebuild()
        }

        rebuild()

        dialog.setContentView(
            panel,
        )
        dialog.show()

        val window =
            dialog.window
                ?: return dialog

        window.setBackgroundDrawable(
            android.graphics.drawable
                .ColorDrawable(
                    Color.TRANSPARENT,
                ),
        )
        window.clearFlags(
            WindowManager.LayoutParams
                .FLAG_DIM_BEHIND,
        )
        window.addFlags(
            WindowManager.LayoutParams
                .FLAG_NOT_TOUCH_MODAL,
        )
        window.setGravity(
            Gravity.TOP or
                Gravity.START,
        )
        window.setLayout(
            panelWidth,
            panelHeight,
        )

        val maxX =
            (
                screenWidth -
                    panelWidth
                )
                .coerceAtLeast(
                    0,
                )
        val maxY =
            (
                screenHeight -
                    panelHeight
                )
                .coerceAtLeast(
                    0,
                )

        fun setPosition(
            x: Int,
            y: Int,
            persist: Boolean,
        ) {
            val safeX =
                x.coerceIn(
                    0,
                    maxX,
                )
            val safeY =
                y.coerceIn(
                    0,
                    maxY,
                )

            val attributes =
                window.attributes
            attributes.x =
                safeX
            attributes.y =
                safeY
            window.attributes =
                attributes

            if (persist) {
                prefs.edit()
                    .putInt(
                        "x",
                        safeX,
                    )
                    .putInt(
                        "y",
                        safeY,
                    )
                    .apply()
            }
        }

        val defaultX =
            (
                maxX /
                    2
                )
        val defaultY =
            (
                screenHeight *
                    0.16f
                )
                .toInt()
                .coerceIn(
                    0,
                    maxY,
                )

        setPosition(
            x =
                prefs.getInt(
                    "x",
                    defaultX,
                ),
            y =
                prefs.getInt(
                    "y",
                    defaultY,
                ),
            persist = false,
        )

        resetPosition.setOnClickListener {
            setPosition(
                x = defaultX,
                y = defaultY,
                persist = true,
            )
        }

        var dragging =
            false
        var startRawX =
            0f
        var startRawY =
            0f
        var startX =
            0
        var startY =
            0

        header.setOnTouchListener { _, event ->
            when (
                event.actionMasked
            ) {
                MotionEvent.ACTION_DOWN -> {
                    dragging =
                        true
                    startRawX =
                        event.rawX
                    startRawY =
                        event.rawY
                    startX =
                        window.attributes.x
                    startY =
                        window.attributes.y
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (
                        !dragging
                    ) {
                        false
                    } else {
                        setPosition(
                            x =
                                startX +
                                    (
                                        event.rawX -
                                            startRawX
                                        )
                                        .toInt(),
                            y =
                                startY +
                                    (
                                        event.rawY -
                                            startRawY
                                        )
                                        .toInt(),
                            persist = false,
                        )
                        true
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (
                        dragging
                    ) {
                        dragging =
                            false
                        setPosition(
                            x =
                                window.attributes.x,
                            y =
                                window.attributes.y,
                            persist = true,
                        )
                    }
                    true
                }

                else ->
                    false
            }
        }

        return dialog
    }

    private fun iconButton(
        context: Context,
        text: String,
        description: String,
    ): TextView =
        TextView(
            context,
        ).apply {
            this.text =
                text
            textSize =
                20f
            gravity =
                Gravity.CENTER
            contentDescription =
                description
            setTextColor(
                Color.WHITE,
            )
            setPadding(
                context.dp(10),
                context.dp(4),
                context.dp(10),
                context.dp(4),
            )
            background =
                rounded(
                    context,
                    Color.argb(
                        120,
                        16,
                        22,
                        30,
                    ),
                    18,
                    Color.argb(
                        100,
                        35,
                        211,
                        238,
                    ),
                )
        }

    private fun tabButton(
        context: Context,
        text: String,
    ): TextView =
        TextView(
            context,
        ).apply {
            this.text =
                text
            textSize =
                13f
            gravity =
                Gravity.CENTER
            typeface =
                Typeface.DEFAULT_BOLD
            includeFontPadding =
                false
        }

    private fun smallButton(
        context: Context,
        text: String,
        onClick: () -> Unit,
    ): TextView =
        TextView(
            context,
        ).apply {
            this.text =
                text
            textSize =
                18f
            gravity =
                Gravity.CENTER
            typeface =
                Typeface.DEFAULT_BOLD
            setTextColor(
                Color.WHITE,
            )
            background =
                rounded(
                    context,
                    COLOR_TILE,
                    16,
                    Color.argb(
                        90,
                        35,
                        211,
                        238,
                    ),
                )
            setOnClickListener {
                onClick()
            }
            layoutParams =
                LinearLayout.LayoutParams(
                    context.dp(43),
                    context.dp(43),
                ).apply {
                    marginStart =
                        context.dp(3)
                    marginEnd =
                        context.dp(3)
                }
        }

    private fun resetButton(
        context: Context,
        text: String,
        onClick: () -> Unit,
    ): TextView =
        TextView(
            context,
        ).apply {
            this.text =
                text
            textSize =
                14f
            gravity =
                Gravity.CENTER
            typeface =
                Typeface.DEFAULT_BOLD
            setTextColor(
                Color.WHITE,
            )
            background =
                rounded(
                    context,
                    COLOR_TILE,
                    20,
                    Color.argb(
                        115,
                        255,
                        255,
                        255,
                    ),
                )
            setOnClickListener {
                onClick()
            }
        }

    private fun rounded(
        context: Context,
        fill: Int,
        radiusDp: Int,
        stroke: Int,
    ): GradientDrawable =
        GradientDrawable().apply {
            shape =
                GradientDrawable.RECTANGLE
            cornerRadius =
                context.dp(
                    radiusDp,
                )
                    .toFloat()
            setColor(
                fill,
            )
            if (
                Color.alpha(
                    stroke,
                ) >
                0
            ) {
                setStroke(
                    context.dp(
                        1,
                    ),
                    stroke,
                )
            }
        }

    private fun Context.dp(
        value: Int,
    ): Int =
        (
            value *
                resources.displayMetrics
                    .density
            )
            .toInt()

    private const val COLOR_TILE =
        0xFF1F242B.toInt()
    private const val COLOR_MUTED =
        0xFFA5B2BE.toInt()
    private const val COLOR_ORANGE =
        0xFFFFB15A.toInt()
    private const val COLOR_CYAN =
        0xFF23D3EE.toInt()
}
