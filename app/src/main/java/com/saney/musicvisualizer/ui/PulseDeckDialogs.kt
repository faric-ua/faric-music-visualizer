package com.saney.musicvisualizer.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Project-wide app-owned dialog family for PulseDeck.
 *
 * Do not add a stock AlertDialog for PulseDeck-owned UI. Extend this family
 * or inherit from the nearest styled parent/sibling flow instead.
 */
object PulseDeckDialogs {
    data class Action(
        val label: String,
        val accent: Boolean = false,
        val destructive: Boolean = false,
        val dismissOnClick: Boolean = true,
        /**
         * V61 (faric 19.10 23:13): a tile with an icon — the icon lives in its own area
         * on the left (centred), the title and [subtitle] are centred in their own area
         * and wrap to two lines; the tile grows with the text.
         */
        val icon: String? = null,
        val subtitle: String? = null,
        val onClick: () -> Unit,
    )

    private const val PANEL_WIDTH_FRACTION = 0.88f

    private val COLOR_PANEL =
        Color.rgb(
            22,
            25,
            29,
        )
    private val COLOR_PANEL_2 =
        Color.rgb(
            31,
            36,
            43,
        )
    private val COLOR_TEXT =
        Color.WHITE
    private val COLOR_MUTED =
        Color.rgb(
            165,
            178,
            190,
        )
    private val COLOR_ORANGE =
        Color.rgb(
            255,
            177,
            90,
        )
    private val COLOR_CYAN =
        Color.rgb(
            35,
            211,
            238,
        )
    private val COLOR_DANGER =
        Color.rgb(
            225,
            92,
            92,
        )

    fun showActionList(
        context: Context,
        title: String,
        items: List<String>,
        cancelLabel: String? = "Закрити",
        accentFirst: Boolean = false,
        onItem: (Int) -> Unit,
    ): Dialog {
        val actions =
            items.mapIndexed { index, label ->
                Action(
                    label = label,
                    accent =
                        accentFirst &&
                            index == 0,
                ) {
                    onItem(
                        index,
                    )
                }
            }

        return show(
            context = context,
            title = title,
            actions =
                buildList {
                    addAll(
                        actions,
                    )
                    if (
                        !cancelLabel.isNullOrBlank()
                    ) {
                        add(
                            Action(
                                label =
                                    cancelLabel,
                            ) {},
                        )
                    }
                },
            dismissAfterAction = true,
        )
    }


    /**
     * A scoped GF source chooser: numbered entries with 76dp previews.
     * SAF file identifiers never become visible labels. All thumbnail I/O
     * occurs on a worker, not the dialog/UI thread.
     */
    fun showImageChoice(
        context: Context,
        title: String,
        labels: List<String>,
        checkedIndex: Int = -1,
        onPreview: (Int) -> android.graphics.Bitmap?,
        onSelected: (Int) -> Unit,
        onAdd: () -> Unit,
    ): Dialog {
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val images = mutableListOf<Pair<Int, android.widget.ImageView>>()
        lateinit var dialog: Dialog
        labels.forEachIndexed { index, text ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(context.dp(8), context.dp(6), context.dp(8), context.dp(6))
                background = roundedDrawable(
                    context = context,
                    fill = COLOR_PANEL_2,
                    radiusDp = 18,
                    stroke = if (index == checkedIndex) COLOR_CYAN
                        else Color.argb(115, 255, 255, 255),
                    strokeDp = 1,
                )
                isClickable = true
                contentDescription = "Вибрати $text"
                setOnClickListener {
                    dialog.dismiss()
                    onSelected(index)
                }
            }
            val image = android.widget.ImageView(context).apply {
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                contentDescription = "Прев'ю $text"
            }
            row.addView(
                image, LinearLayout.LayoutParams(context.dp(76), context.dp(76))
                    .apply { rightMargin = context.dp(12) },
            )
            row.addView(
                label(context, text, 15f, COLOR_TEXT, true).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    maxLines = 2
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
            list.addView(
                row, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { if (index > 0) topMargin = context.dp(10) },
            )
            images.add(index to image)
        }
        if (labels.isEmpty()) {
            list.addView(label(
                context, "PNG ще немає. Додай папку з файлами.", 13f, COLOR_MUTED, false,
            ))
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = false
            addView(list)
        }
        // The inner scroll has a measured height; show() can safely wrap the
        // outer body without swallowing this limit, even with 100+ sources.
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                scroll, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    minOf(
                        (context.resources.displayMetrics.heightPixels * 0.50f).toInt(),
                        context.dp(labels.size * 96 + 24).coerceAtLeast(context.dp(68)),
                    ),
                ),
            )
        }
        dialog = show(
            context = context,
            title = title,
            body = body,
            actions = listOf(
                Action(
                    label = "＋ Додати PNG із папки",
                    accent = true,
                    dismissOnClick = false,
                ) {
                    dialog.dismiss()
                    onAdd()
                },
                Action("Закрити") {},
            ),
        )
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        kotlin.concurrent.thread(name = "faric-gf-thumbnails") {
            for ((index, image) in images) {
                if (!dialog.isShowing) break
                val bitmap = runCatching { onPreview(index) }.getOrNull()
                handler.post {
                    if (dialog.isShowing && image.parent != null) {
                        image.setImageBitmap(bitmap)
                    } else {
                        bitmap?.recycle()
                    }
                }
            }
        }
        return dialog
    }

    fun showSingleChoice(
        context: Context,
        title: String,
        labels: List<String>,
        checkedIndex: Int = -1,
        cancelLabel: String? = "Скасувати",
        dismissOnSelect: Boolean = true,
        onSelected: (Int) -> Unit,
    ): Dialog {
        if (dismissOnSelect) {
            val display =
                labels.mapIndexed { index, label ->
                    if (
                        index ==
                            checkedIndex
                    ) {
                        "●  $label"
                    } else {
                        "○  $label"
                    }
                }

            return showActionList(
                context = context,
                title = title,
                items = display,
                cancelLabel = cancelLabel,
                accentFirst = false,
            ) { index ->
                // showActionList also includes a Cancel/Close row after labels.
                // That row must not be interpreted as a GF asset index.
                if (index in labels.indices) onSelected(index)
            }
        }

        var selectedIndex =
            checkedIndex
        val rows =
            mutableListOf<TextView>()
        val body =
            LinearLayout(
                context,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        fun refreshRows() {
            rows.forEachIndexed { index, row ->
                row.text =
                    if (
                        index ==
                            selectedIndex
                    ) {
                        "●  ${labels[index]}"
                    } else {
                        "○  ${labels[index]}"
                    }
            }
        }

        labels.forEachIndexed { index, labelText ->
            val row =
                actionButton(
                    context = context,
                    action =
                        Action(
                            label =
                                if (
                                    index ==
                                        selectedIndex
                                ) {
                                    "●  $labelText"
                                } else {
                                    "○  $labelText"
                                },
                        ) {},
                ) {
                    selectedIndex =
                        index
                    refreshRows()
                    onSelected(
                        index,
                    )
                }

            rows.add(
                row,
            )
            body.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (
                        index >
                        0
                    ) {
                        topMargin =
                            context.dp(
                                8,
                            )
                    }
                },
            )
        }

        return show(
            context = context,
            title = title,
            body = body,
            actions =
                if (
                    cancelLabel.isNullOrBlank()
                ) {
                    emptyList()
                } else {
                    listOf(
                        Action(
                            label =
                                cancelLabel,
                        ) {},
                    )
                },
            dismissAfterAction = true,
        )
    }

    fun showTextInput(
        context: Context,
        title: String,
        hint: String? = null,
        initialValue: String? = null,
        positiveLabel: String = "Зберегти",
        negativeLabel: String = "Скасувати",
        onPositive: (String) -> Unit,
    ): Dialog {
        val input =
            EditText(
                context,
            ).apply {
                setSingleLine(
                    true,
                )
                setTextColor(
                    COLOR_TEXT,
                )
                setHintTextColor(
                    COLOR_MUTED,
                )
                textSize =
                    16f
                setPadding(
                    context.dp(
                        14,
                    ),
                    context.dp(
                        12,
                    ),
                    context.dp(
                        14,
                    ),
                    context.dp(
                        12,
                    ),
                )
                background =
                    roundedDrawable(
                        context = context,
                        fill =
                            Color.rgb(
                                10,
                                16,
                                22,
                            ),
                        radiusDp = 18,
                        stroke =
                            Color.argb(
                                130,
                                35,
                                211,
                                238,
                            ),
                        strokeDp = 1,
                    )

                this.hint =
                    hint.orEmpty()

                if (
                    !initialValue.isNullOrEmpty()
                ) {
                    setText(
                        initialValue,
                    )
                    setSelection(
                        text.length,
                    )
                }
            }

        return show(
            context = context,
            title = title,
            body =
                input,
            actions =
                listOf(
                    Action(
                        label =
                            positiveLabel,
                        accent = true,
                    ) {
                        onPositive(
                            input.text
                                ?.toString()
                                .orEmpty(),
                        )
                    },
                    Action(
                        label =
                            negativeLabel,
                    ) {},
                ),
            dismissAfterAction = true,
        )
    }

    fun showConfirm(
        context: Context,
        title: String,
        message: String? = null,
        positiveLabel: String,
        negativeLabel: String = "Скасувати",
        destructive: Boolean = false,
        onPositive: () -> Unit,
    ): Dialog =
        show(
            context = context,
            title = title,
            message = message,
            actions =
                listOf(
                    Action(
                        label =
                            positiveLabel,
                        accent =
                            !destructive,
                        destructive =
                            destructive,
                        onClick =
                            onPositive,
                    ),
                    Action(
                        label =
                            negativeLabel,
                    ) {},
                ),
            dismissAfterAction = true,
        )

    fun showMessage(
        context: Context,
        title: String,
        message: String,
        primaryLabel: String = "OK",
        secondaryLabel: String? = null,
        // The export timing report can exceed the viewport. Opt in to
        // scrolling only its message, not the title or action footer.
        scrollableMessage: Boolean = false,
        keepOpenOnSecondary: Boolean = false,
        onSecondary: (() -> Unit)? = null,
    ): Dialog =
        show(
            context = context,
            title = title,
            message = message,
            actions =
                buildList {
                    if (
                        !secondaryLabel.isNullOrBlank()
                    ) {
                        add(
                            Action(
                                label =
                                    secondaryLabel,
                                dismissOnClick =
                                    !keepOpenOnSecondary,
                            ) {
                                onSecondary
                                    ?.invoke()
                            },
                        )
                    }

                    add(
                        Action(
                            label =
                                primaryLabel,
                            accent = true,
                        ) {},
                    )
                },
            dismissAfterAction = true,
            scrollableMessage = scrollableMessage,
        )

    /**
     * THE canonical PulseDeck window (v0.19.75). Every message / confirm / choice /
     * text-input window goes through here: title → scrollable message → optional body
     * → action buttons. BoundedDialogPanel keeps it within 82 % of the screen, the
     * message scrolls, the buttons are always reachable, Back closes it.
     * `scrollableMessage` is kept for old call sites; scrolling is now always on.
     * New informational windows must use this, not a hand-made Dialog (CI checks).
     */
    fun show(
        context: Context,
        title: String,
        message: String? = null,
        body: View? = null,
        actions: List<Action>,
        dismissAfterAction: Boolean = true,
        scrollableMessage: Boolean = false,
        /** false = Back and outside taps do nothing (progress of a running export). */
        cancelable: Boolean = true,
    ): Dialog {
        lateinit var dialog:
            Dialog

        dialog =
            Dialog(
                context,
            ).apply {
                requestWindowFeature(
                    Window.FEATURE_NO_TITLE,
                )
                setCancelable(
                    cancelable,
                )
                setCanceledOnTouchOutside(
                    false,
                )
            }
        // Canonical window (v0.19.75): never taller than 82 % of the screen; the
        // message scrolls, title and buttons always stay visible. See BoundedDialogPanel.
        val panel =
            BoundedDialogPanel(
                context,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    context.dp(
                        18,
                    ),
                    context.dp(
                        16,
                    ),
                    context.dp(
                        18,
                    ),
                    context.dp(
                        16,
                    ),
                )
                background =
                    roundedDrawable(
                        context = context,
                        fill =
                            Color.argb(
                                250,
                                Color.red(
                                    COLOR_PANEL,
                                ),
                                Color.green(
                                    COLOR_PANEL,
                                ),
                                Color.blue(
                                    COLOR_PANEL,
                                ),
                            ),
                        radiusDp = 24,
                        stroke =
                            Color.argb(
                                95,
                                255,
                                255,
                                255,
                            ),
                        strokeDp = 1,
                    )
            }

        panel.addView(
            label(
                context = context,
                text = title,
                sizeSp = 21f,
                color =
                    COLOR_TEXT,
                bold = true,
            ),
        )

        if (
            !message.isNullOrBlank()
        ) {
            val messageView =
                label(
                    context = context,
                    text = message,
                    sizeSp = 13f,
                    color =
                        COLOR_MUTED,
                    bold = false,
                )

            // Always inside a scroll view; BoundedDialogPanel shrinks it only when the
            // whole window would not fit. Short messages keep their natural height.
            val messageScroll =
                ScrollView(
                    context,
                ).apply {
                    isFillViewport =
                        false
                    addView(
                        messageView,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ),
                    )
                }
            panel.addView(
                messageScroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin =
                        context.dp(
                            8,
                        )
                    bottomMargin =
                        context.dp(
                            12,
                        )
                },
            )
            panel.flexible = messageScroll
        }

        body?.let { bodyView ->
            panel.addView(
                bodyView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin =
                        context.dp(
                            12,
                        )
                    bottomMargin =
                        context.dp(
                            12,
                        )
                },
            )
        }

        // Large GF source lists must remain scrollable instead of growing
        // beyond the screen's bottom navigation bar. Short dialogs retain
        // their natural WRAP_CONTENT height.
        val actionsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        actions.forEachIndexed { index, action ->
            val onActionClick: () -> Unit = {
                action.onClick()
                if (
                    dismissAfterAction &&
                        action.dismissOnClick
                ) {
                    dialog.dismiss()
                }
            }
            val actionView: View =
                if (action.icon != null || action.subtitle != null) {
                    iconTile(context, action, onActionClick)
                } else {
                    actionButton(context, action, onActionClick)
                }
            actionsContainer.addView(
                actionView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (index > 0) topMargin = context.dp(12)
                },
            )
        }
        if (actions.size > 5) {
            panel.addView(
                ScrollView(context).apply {
                    isFillViewport = false
                    clipToPadding = false
                    addView(actionsContainer)
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (context.resources.displayMetrics.heightPixels * 0.58f).toInt(),
                ).apply { topMargin = context.dp(12) },
            )
        } else {
            panel.addView(
                actionsContainer,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = context.dp(12) },
            )
        }

        dialog.setContentView(
            panel,
        )
        dialog.show()

        dialog.window
            ?.apply {
                setBackgroundDrawable(
                    android.graphics.drawable
                        .ColorDrawable(
                            Color.TRANSPARENT,
                        ),
                )
                setLayout(
                    (
                        context.resources
                            .displayMetrics
                            .widthPixels *
                            PANEL_WIDTH_FRACTION
                        )
                        .toInt(),
                    // Height is capped by BoundedDialogPanel (82 % of the screen).
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setGravity(
                    Gravity.CENTER,
                )
            }

        return dialog
    }

    /** V61: icon area · centred two-line text · the same width as the icon area on the right. */
    private fun iconTile(
        context: Context,
        action: Action,
        onClick: () -> Unit,
    ): View {
        val textColor =
            when {
                action.accent -> Color.BLACK
                action.destructive -> COLOR_DANGER
                else -> COLOR_TEXT
            }
        val iconArea = context.dp(44)
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = context.dp(56)
            setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
            background =
                roundedDrawable(
                    context = context,
                    fill = if (action.accent) COLOR_ORANGE else COLOR_PANEL_2,
                    radiusDp = 22,
                    stroke =
                        when {
                            action.accent -> COLOR_ORANGE
                            action.destructive -> Color.argb(170, 225, 92, 92)
                            else -> Color.argb(115, 255, 255, 255)
                        },
                    strokeDp = 1,
                )
            addView(
                label(context, action.icon.orEmpty(), 22f, textColor, true).apply {
                    gravity = Gravity.CENTER
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                },
                LinearLayout.LayoutParams(iconArea, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    addView(
                        label(context, action.label, 15f, textColor, true).apply {
                            gravity = Gravity.CENTER
                            textAlignment = View.TEXT_ALIGNMENT_CENTER
                            maxLines = 2
                            breakStrategy = android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY
                        },
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                    )
                    action.subtitle?.let { sub ->
                        addView(
                            label(context, sub, 12f, if (action.accent) Color.argb(200, 0, 0, 0) else COLOR_MUTED, false).apply {
                                gravity = Gravity.CENTER
                                textAlignment = View.TEXT_ALIGNMENT_CENTER
                                maxLines = 2
                            },
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                topMargin = context.dp(3)
                            },
                        )
                    }
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
            // Same width as the icon area, so the text is centred on the whole tile.
            addView(View(context), LinearLayout.LayoutParams(iconArea, 1))
            setOnClickListener { onClick() }
        }
    }

    private fun actionButton(
        context: Context,
        action: Action,
        onClick: () -> Unit,
    ): TextView =
        label(
            context = context,
            text = action.label,
            sizeSp = 15f,
            color =
                when {
                    action.accent ->
                        Color.BLACK

                    action.destructive ->
                        COLOR_DANGER

                    else ->
                        COLOR_TEXT
                },
            bold = true,
        ).apply {
            gravity =
                Gravity.CENTER
            textAlignment =
                View.TEXT_ALIGNMENT_CENTER
            minimumHeight =
                context.dp(
                    if (
                        action.accent
                    ) {
                        52
                    } else {
                        48
                    },
                )
            setPadding(
                context.dp(
                    16,
                ),
                context.dp(
                    12,
                ),
                context.dp(
                    16,
                ),
                context.dp(
                    12,
                ),
            )
            // Primary action labels must wrap and grow with content, never
            // clip or ellipsize a long imported PNG filename.
            maxLines = Int.MAX_VALUE
            breakStrategy = android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY
            background =
                roundedDrawable(
                    context = context,
                    fill =
                        if (
                            action.accent
                        ) {
                            COLOR_ORANGE
                        } else {
                            COLOR_PANEL_2
                        },
                    radiusDp = 22,
                    stroke =
                        when {
                            action.accent ->
                                COLOR_ORANGE

                            action.destructive ->
                                Color.argb(
                                    170,
                                    225,
                                    92,
                                    92,
                                )

                            else ->
                                Color.argb(
                                    115,
                                    255,
                                    255,
                                    255,
                                )
                        },
                    strokeDp = 1,
                )
            setOnClickListener {
                onClick()
            }
        }

    private fun label(
        context: Context,
        text: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean,
    ): TextView =
        TextView(
            context,
        ).apply {
            this.text =
                text
            textSize =
                sizeSp
            setTextColor(
                color,
            )
            includeFontPadding =
                false
            typeface =
                if (
                    bold
                ) {
                    Typeface.DEFAULT_BOLD
                } else {
                    Typeface.DEFAULT
                }
        }

    private fun roundedDrawable(
        context: Context,
        fill: Int,
        radiusDp: Int,
        stroke: Int,
        strokeDp: Int,
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
                strokeDp >
                0 &&
                Color.alpha(
                    stroke,
                ) >
                0
            ) {
                setStroke(
                    context.dp(
                        strokeDp,
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
                resources
                    .displayMetrics
                    .density
            )
            .toInt()
}
