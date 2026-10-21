package com.saney.musicvisualizer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.saney.musicvisualizer.ui.LayoutProfileId
import com.saney.musicvisualizer.ui.LayoutProfileStore
import com.saney.musicvisualizer.ui.PulseDeckDialogs
import com.saney.musicvisualizer.ui.UtilityUi
import com.saney.musicvisualizer.ui.UtilityUi.dp

/**
 * V104 «Утиліти» (faric 21.10 23:43: «поки просто додамо меню Утиліти в PulseDeck tools»; макет
 * «Утиліти PulseDeck», екран 1). Pick the position (Книжка / Альбом / Відео 9:16), then the tool.
 */
class PulseDeckUtilitiesActivity : ComponentActivity() {
    private var profile = LayoutProfileId.PORTRAIT
    private val tabs = mutableMapOf<LayoutProfileId, TextView>()
    private lateinit var savedNote: TextView
    private lateinit var resetTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile =
            if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                LayoutProfileId.LANDSCAPE
            } else {
                LayoutProfileId.PORTRAIT
            }
        val content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(24))
            }
        val scroll =
            ScrollView(this).apply {
                setBackgroundColor(UtilityUi.BG)
                addView(content)
            }
        UtilityUi.padForSystemBars(scroll)

        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
        header.addView(
            UtilityUi.button(this, "‹") { finish() }.apply { contentDescription = "Назад" },
            LinearLayout.LayoutParams(dp(48), dp(48)),
        )
        header.addView(
            UtilityUi.text(this, "Утиліти", 24f, bold = true),
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) },
        )
        content.addView(header)
        content.addView(
            UtilityUi.text(
                this,
                "Розкладка головного екрана. У кожного положення — свої збережені місця й розміри. " +
                    "Повернув телефон — береться своє. «Відео 9:16» — лише для MP4.",
                13f,
                UtilityUi.MUTED,
            ),
            block(12),
        )

        val tabRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        LayoutProfileId.entries.forEach { id ->
            val tab = UtilityUi.button(this, id.label) { choose(id) }
            tabs[id] = tab
            tabRow.addView(tab, UtilityUi.weighted(context = this))
        }
        content.addView(tabRow, block(14))
        savedNote = UtilityUi.text(this, "", 12f, UtilityUi.MUTED)
        content.addView(savedNote, block(6))

        content.addView(
            entry("Розкладка екрана", "Пересунути й змінити розмір: обкладинка, ‹ ⋮, шапка, назва, хвиля, панелі") {
                startActivity(
                    Intent(this, PulseDeckLayoutEditorActivity::class.java)
                        .putExtra(PulseDeckLayoutEditorActivity.EXTRA_PROFILE, profile.name),
                )
            },
            block(16),
        )
        content.addView(
            entry("Центри (калібрування)", "Колишнє «Center Calibration» — точки C-01…C-20 (книжка)") {
                startActivity(Intent(this, PulseDeckCenterCalibrationActivity::class.java))
            },
            block(10),
        )
        content.addView(
            entry("Шаблон", "Колишній «Template Constructor» — збереження у файл") {
                startActivity(Intent(this, PulseDeckTemplateConstructorActivity::class.java))
            },
            block(10),
        )
        content.addView(
            entry("Об'єкти", "Колишній «Object Constructor»") {
                startActivity(Intent(this, PulseDeckObjectConstructorActivity::class.java))
            },
            block(10),
        )

        resetTitle = UtilityUi.text(this, "", 13f, UtilityUi.MUTED)
        content.addView(resetTitle, block(24))
        content.addView(
            UtilityUi.button(this, "Скинути весь екран (назад до еталона)", UtilityUi.Kind.WARNING) {
                PulseDeckDialogs.showConfirm(
                    context = this,
                    title = "Скинути «${profile.label}»?",
                    message =
                        "Усі твої пересування й розміри для положення «${profile.label}» зітруться — " +
                            "екран стане як у затвердженому шаблоні. Інші положення не зміняться.",
                    positiveLabel = "Так, скинути",
                    negativeLabel = "Скасувати",
                    destructive = true,
                ) {
                    LayoutProfileStore(this).clear(profile)
                    refresh()
                }
            },
            block(8),
        )
        setContentView(scroll)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun choose(id: LayoutProfileId) {
        profile = id
        refresh()
    }

    private fun refresh() {
        if (!::savedNote.isInitialized) return
        tabs.forEach { (id, tab) ->
            tab.text = (if (id == profile) "✓ " else "") + id.label
            UtilityUi.style(tab, if (id == profile) UtilityUi.Kind.CHOSEN else UtilityUi.Kind.NORMAL)
        }
        val count = LayoutProfileStore(this).load(profile).size
        savedNote.text =
            if (count == 0) {
                "«${profile.label}»: як у шаблоні (еталон)."
            } else {
                "«${profile.label}»: змінено частин — $count."
            }
        resetTitle.text = "Для положення «${profile.label}»:"
    }

    private fun entry(
        title: String,
        subtitle: String,
        onClick: () -> Unit,
    ): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            minimumHeight = dp(56)
            background = UtilityUi.frame(UtilityUi.PANEL, UtilityUi.CYAN, 1, 14, context)
            isClickable = true
            isFocusable = true
            contentDescription = title
            setOnClickListener { onClick() }
            addView(UtilityUi.text(context, title, 16f, bold = true))
            addView(UtilityUi.text(context, subtitle, 12f, UtilityUi.MUTED), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
        }

    private fun block(topDp: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(topDp) }
}
