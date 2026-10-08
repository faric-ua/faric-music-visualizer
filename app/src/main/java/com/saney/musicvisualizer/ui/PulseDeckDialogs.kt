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
                onSelected(
                    index,
                )
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

    fun show(
        context: Context,
        title: String,
        message: String? = null,
        body: View? = null,
        actions: List<Action>,
        dismissAfterAction: Boolean = true,
        scrollableMessage: Boolean = false,
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
                    true,
                )
                setCanceledOnTouchOutside(
                    false,
                )
            }

        val panel =
            LinearLayout(
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

            if (
                scrollableMessage ||
                message.length >
                    900
            ) {
                panel.addView(
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
                    },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        // Give a tall report only the remaining space;
                        // its action buttons stay pinned below the scroll.
                        if (scrollableMessage) 0 else context.dp(420),
                    ).apply {
                        if (scrollableMessage) {
                            weight = 1f
                        }
                        topMargin =
                            context.dp(
                                10,
                            )
                        bottomMargin =
                            context.dp(
                                12,
                            )
                    },
                )
            } else {
                panel.addView(
                    messageView,
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
            }
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

        actions.forEachIndexed { index, action ->
            panel.addView(
                actionButton(
                    context = context,
                    action = action,
                ) {
                    action.onClick()

                    if (
                        dismissAfterAction &&
                            action.dismissOnClick
                    ) {
                        dialog.dismiss()
                    }
                },
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

        if (scrollableMessage) {
            dialog.setContentView(
                panel,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        } else {
            dialog.setContentView(
                panel,
            )
        }
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
                    if (scrollableMessage) {
                        // Respect the visible screen: report body scrolls
                        // while Copy / OK remain reachable at all times.
                        (context.resources.displayMetrics.heightPixels * 0.82f)
                            .toInt()
                    } else {
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    },
                )
                setGravity(
                    Gravity.CENTER,
                )
            }

        return dialog
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
            maxLines =
                3
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
