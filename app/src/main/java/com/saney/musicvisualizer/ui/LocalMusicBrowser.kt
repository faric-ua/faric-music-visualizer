package com.saney.musicvisualizer.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.saney.musicvisualizer.playback.LocalMusicLibrary
import com.saney.musicvisualizer.playback.LocalMusicSearch
import com.saney.musicvisualizer.playback.LocalMusicTrack
import kotlin.concurrent.thread

/**
 * One read-only MediaStore browser. All scanning is off the main thread.
 * Navigation is owned by MainActivity. Selecting a row passes the filtered
 * queue to the existing player without introducing another playback engine.
 */
class LocalMusicBrowser(
    private val activity: Activity,
    private val canReadMusic: Boolean,
    private val onBack: () -> Unit,
    private val onRequestPermission: () -> Unit,
    private val onPickFiles: () -> Unit,
    private val onSelect: (List<LocalMusicTrack>, Int) -> Unit,
) {
    private val density get() = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (density * value + 0.5f).toInt()
    private val bg = Color.rgb(2, 6, 10)
    private val panel = Color.rgb(13, 20, 27)
    private val muted = Color.rgb(165, 178, 190)
    private val cyan = Color.rgb(43, 196, 228)
    private val orange = Color.rgb(255, 153, 24)

    private fun box(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(16).toFloat()
        setColor(panel)
        setStroke(dp(1), Color.argb(100, 52, 151, 181))
    }

    private fun text(value: String, size: Float, color: Int = Color.WHITE, strong: Boolean = false): TextView =
        TextView(activity).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (strong) setTypeface(null, android.graphics.Typeface.BOLD)
        }

    private fun button(value: String, action: () -> Unit): TextView =
        text(value, 15f, cyan, true).apply {
            gravity = Gravity.CENTER
            background = box()
            minimumHeight = dp(52)
            setOnClickListener { action() }
        }

    fun create(focusSearch: Boolean = false, footer: View? = null): FrameLayout {
        val root = FrameLayout(activity).apply { setBackgroundColor(bg) }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        val top = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(button("‹") { onBack() }, LinearLayout.LayoutParams(dp(52), dp(52)))
        top.addView(text("Усі треки", 23f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        content.addView(top)

        val status = text("Завантаження медіатеки…", 12f, muted)
        content.addView(status, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(10)
            bottomMargin = dp(10)
        })

        val search = EditText(activity).apply {
            hint = "Назва, виконавець або альбом"
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = box()
        }
        content.addView(search, LinearLayout.LayoutParams(-1, dp(50)).apply {
            bottomMargin = dp(8)
        })

        val list = ListView(activity).apply {
            divider = null
            dividerHeight = dp(5)
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            cacheColorHint = Color.TRANSPARENT
        }
        var tracks: List<LocalMusicTrack> = emptyList()
        var visible: List<LocalMusicTrack> = emptyList()
        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = visible.size
            override fun getItem(position: Int): LocalMusicTrack = visible[position]
            override fun getItemId(position: Int): Long = visible[position].id
            override fun hasStableIds(): Boolean = true

            override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
                val row = (recycled as? LinearLayout) ?: LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    minimumHeight = dp(66)
                    setPadding(dp(14), dp(9), dp(14), dp(9))
                    background = box()
                    addView(text("", 15f, Color.WHITE, true).apply {
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                    addView(text("", 12f, muted).apply {
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                }
                val item = getItem(position)
                (row.getChildAt(0) as TextView).text = item.title
                (row.getChildAt(1) as TextView).text =
                    "${item.artist} · ${item.durationMs / 60000}:${((item.durationMs / 1000) % 60).toString().padStart(2, '0')}"
                return row
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, index, _ ->
            if (index in visible.indices) onSelect(visible, index)
        }
        content.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))

        fun filter() {
            visible = tracks.filter {
                LocalMusicSearch.matches(it.title, it.artist, it.album, search.text.toString())
            }
            status.text = if (tracks.isEmpty()) {
                "Музичних файлів не знайдено"
            } else {
                "Знайдено ${visible.size} із ${tracks.size} треків"
            }
            adapter.notifyDataSetChanged()
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filter() }
            override fun afterTextChanged(s: Editable?) {}
        })

        if (footer != null) content.addView(footer, LinearLayout.LayoutParams(-1, -2))
        if (!canReadMusic) {
            status.text = "Для пошуку музики на телефоні потрібен дозвіл"
            content.addView(button("Дозволити доступ до музики") { onRequestPermission() },
                LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })
            content.addView(button("Обрати аудіофайли вручну") { onPickFiles() },
                LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(7) })
        } else {
            if (focusSearch) search.requestFocus()
            thread(name = "faric-local-media-index") {
                val result = runCatching { LocalMusicLibrary.scan(activity.applicationContext) }
                activity.runOnUiThread {
                    if (!root.isAttachedToWindow || activity.isFinishing || activity.isDestroyed) {
                        return@runOnUiThread
                    }
                    result.onSuccess { newTracks ->
                        tracks = newTracks
                        filter()
                    }.onFailure { error ->
                        status.text = "Не вдалося прочитати музику: ${error.message ?: "помилка"}"
                    }
                }
            }
        }
        return root
    }
}
