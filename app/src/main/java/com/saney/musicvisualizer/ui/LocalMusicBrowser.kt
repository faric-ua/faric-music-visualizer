package com.saney.musicvisualizer.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextUtils
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
import com.saney.musicvisualizer.playback.LocalMusicCatalog
import com.saney.musicvisualizer.playback.LocalMusicCategory
import com.saney.musicvisualizer.playback.LocalMusicGroup
import com.saney.musicvisualizer.playback.LocalMusicLibrary
import com.saney.musicvisualizer.playback.LocalMusicSearch
import com.saney.musicvisualizer.playback.LocalMusicSort
import com.saney.musicvisualizer.playback.LocalMusicTrack
import com.saney.musicvisualizer.playback.LocalMusicUserState
import kotlin.concurrent.thread

/**
 * All device-music categories in one read-only browser with shared state.
 * Group -> tracks -> player; favorites and app-local playback recents are local.
 * Keeps the existing Player/Scene Host owned by MainActivity.
 */
class LocalMusicBrowser(
    private val activity: Activity,
    private val canReadMusic: Boolean,
    private val category: LocalMusicCategory,
    private val onBack: () -> Unit,
    private val onRequestPermission: () -> Unit,
    private val onPickFiles: () -> Unit,
    private val onSelect: (List<LocalMusicTrack>, Int) -> Unit,
    private val initialGroup: String? = null,
    private val initialQuery: String = "",
    private val onStateChange: (String?, String) -> Unit = { _, _ -> },
) {
    private val density get() = activity.resources.displayMetrics.density
    private fun dp(v: Int) = (density * v + 0.5f).toInt()
    private val background = Color.rgb(2, 6, 10)
    private val panel = Color.rgb(13, 20, 27)
    private val muted = Color.rgb(165, 178, 190)
    private val accent = Color.rgb(43, 196, 228)
    private val state = LocalMusicUserState(activity)
    private var allTracks: List<LocalMusicTrack> = emptyList()
    private var openedGroup: String? = initialGroup
    private var query = initialQuery
    private var sort = if (category == LocalMusicCategory.RECENT) LocalMusicSort.NEWEST else LocalMusicSort.TITLE
    private var render: (() -> Unit)? = null
    private var scanToken = 0
    private var root: FrameLayout? = null

    /** System Back first closes a group, then MainActivity returns to Home. */
    fun navigateBack(): Boolean {
        if (openedGroup == null || !category.grouped) return false
        openedGroup = null
        onStateChange(null, query)
        render?.invoke()
        return true
    }

    private fun box(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(15).toFloat()
        setColor(panel)
        setStroke(dp(1), Color.argb(100, 52, 151, 181))
    }

    private fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(activity).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun button(value: String, action: () -> Unit): TextView =
        text(value, 14f, accent, true).apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(48)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = box()
            setOnClickListener { action() }
        }

    private fun row(title: String, subtitle: String, icon: String, click: () -> Unit, action: (() -> Unit)? = null): View {
        val line = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(72)
            setPadding(dp(14), dp(10), dp(10), dp(10))
            background = box()
            setOnClickListener { click() }
        }
        val copy = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(text(title, 15f, Color.WHITE, true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        copy.addView(text(subtitle, 12f, muted).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
        line.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(text(icon, 22f, accent, true).apply {
            gravity = Gravity.CENTER
            minimumWidth = dp(44)
            minimumHeight = dp(48)
            if (action != null) setOnClickListener { action() }
        }, LinearLayout.LayoutParams(dp(44), dp(50)))
        return line
    }

    fun create(focusSearch: Boolean = false, footer: View? = null): FrameLayout {
        val viewRoot = FrameLayout(activity).apply { setBackgroundColor(background) }
        root = viewRoot
        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        viewRoot.addView(body, FrameLayout.LayoutParams(-1, -1))
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(button("‹") {
            if (!navigateBack()) onBack()
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        val heading = text(category.title, 23f, Color.WHITE, true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        val refresh = button("↻") { scan(force = true) }
        header.addView(refresh, LinearLayout.LayoutParams(dp(50), dp(50)))
        body.addView(header)

        val status = text("Завантаження медіатеки…", 12f, muted)
        body.addView(status, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(10)
            bottomMargin = dp(10)
        })

        val search = EditText(activity).apply {
            hint = if (category.grouped) "Пошук групи або треку" else "Назва, виконавець, альбом, папка"
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = box()
            setText(query)
        }
        body.addView(search, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(8) })

        val tools = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val sortButton = button("Сортувати: ${sort.label}") {
            sort = sort.next()
            render?.invoke()
        }
        tools.addView(sortButton, LinearLayout.LayoutParams(0, dp(48), 1f))
        val countLabel = text("", 12f, muted).apply { gravity = Gravity.CENTER }
        tools.addView(countLabel, LinearLayout.LayoutParams(dp(85), dp(48)).apply { marginStart = dp(8) })
        body.addView(tools, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })

        val list = ListView(activity).apply {
            divider = null
            dividerHeight = dp(6)
            selector = ColorDrawable(Color.TRANSPARENT)
            cacheColorHint = Color.TRANSPARENT
        }
        var visibleGroups: List<LocalMusicGroup> = emptyList()
        var visibleTracks: List<LocalMusicTrack> = emptyList()
        var groupedView = category.grouped
        val adapter = object : BaseAdapter() {
            override fun getCount() = if (groupedView) visibleGroups.size else visibleTracks.size
            override fun getItem(position: Int): Any = if (groupedView) visibleGroups[position] else visibleTracks[position]
            override fun getItemId(position: Int): Long = if (groupedView) position.toLong() else visibleTracks[position].id

            override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
                // Use a simple view-holder: do not re-inflate a 10k-row music list on scroll.
                val cell = if (recycled is LinearLayout && recycled.tag is CellHolder) {
                    recycled
                } else {
                    LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        minimumHeight = dp(70)
                        setPadding(dp(14), dp(8), dp(10), dp(8))
                        background = box()
                        val labels = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                        val primary = text("", 15f, Color.WHITE, true).apply {
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                        }
                        val secondary = text("", 12f, muted).apply {
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                        }
                        labels.addView(primary)
                        labels.addView(secondary, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
                        addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
                        val icon = text("", 23f, accent).apply {
                            gravity = Gravity.CENTER
                        }
                        addView(icon, LinearLayout.LayoutParams(dp(48), dp(50)))
                        tag = CellHolder(primary, secondary, icon)
                    }
                }
                val holder = cell.tag as CellHolder
                if (groupedView) {
                    val group = visibleGroups[position]
                    holder.title.text = group.title
                    val artists = group.tracks.map { it.artist }.distinct().size
                    holder.subtitle.text = "${group.tracks.size} треків · $artists виконавців"
                    holder.action.text = "›"
                    holder.action.setOnClickListener { openedGroup = group.title; onStateChange(openedGroup, query); render?.invoke() }
                    cell.setOnClickListener { openedGroup = group.title; onStateChange(openedGroup, query); render?.invoke() }
                } else {
                    val track = visibleTracks[position]
                    holder.title.text = track.title
                    holder.subtitle.text = "${track.artist} · ${track.durationMs / 60_000}:${((track.durationMs / 1000) % 60).toString().padStart(2, '0')}"
                    holder.action.text = if (state.isFavorite(track.uri.toString())) "♥" else "♡"
                    holder.action.setOnClickListener {
                        state.toggleFavorite(track.uri.toString())
                        render?.invoke()
                    }
                    cell.setOnClickListener {
                        val queue = visibleTracks
                        if (position in queue.indices) onSelect(queue, position)
                    }
                }
                return cell
            }
        }
        list.adapter = adapter
        body.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))

        val emptyHint = text("", 13f, muted).apply {
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        body.addView(emptyHint, LinearLayout.LayoutParams(-1, -2))

        fun visibleSource(): List<LocalMusicTrack> = when (category) {
            LocalMusicCategory.FAVORITES -> LocalMusicCatalog.favoriteTracks(allTracks, state.favorites())
            LocalMusicCategory.RECENT -> LocalMusicCatalog.recentTracks(allTracks, state.recentlyPlayed())
            else -> allTracks
        }

        render = {
            val base = visibleSource()
            val group = if (category.grouped) {
                LocalMusicCatalog.groups(category, base).firstOrNull { it.title == openedGroup }
            } else null
            groupedView = category.grouped && openedGroup == null
            heading.text = if (groupedView || !category.grouped) category.title else group?.title ?: category.title
            sortButton.visibility = if (groupedView) View.GONE else View.VISIBLE
            val needle = query.trim()
            if (groupedView) {
                visibleGroups = LocalMusicCatalog.groups(category, base).filter {
                    needle.isBlank() || it.title.contains(needle, ignoreCase = true) ||
                        it.tracks.any { t -> LocalMusicSearch.matches(t, needle) }
                }
                visibleTracks = emptyList()
                countLabel.text = "${visibleGroups.size} груп"
                status.text = "${base.size} треків · ${visibleGroups.size} груп"
            } else {
                val source = group?.tracks ?: if (category.grouped) emptyList() else base
                val filtered = source.filter { LocalMusicSearch.matches(it, needle) }
                visibleTracks = if (category == LocalMusicCategory.RECENT && sort == LocalMusicSort.NEWEST) {
                    filtered
                } else {
                    LocalMusicCatalog.sortTracks(filtered, sort)
                }
                visibleGroups = emptyList()
                countLabel.text = "${visibleTracks.size} треків"
                status.text = if (category == LocalMusicCategory.FAVORITES) {
                    "♥ Улюблені · натисни ♡ біля треку, щоб додати або прибрати"
                } else if (category == LocalMusicCategory.RECENT) {
                    "Нещодавно відтворені в FARIC"
                } else "${visibleTracks.size} із ${source.size} треків"
            }
            sortButton.text = "Сортувати: ${sort.label}"
            adapter.notifyDataSetChanged()
            list.setSelection(0)
            emptyHint.visibility = if (adapter.count == 0 && canReadMusic) View.VISIBLE else View.GONE
            emptyHint.text = when (category) {
                LocalMusicCategory.FAVORITES -> "Поки немає улюблених. Відкрий «Усі треки» та натисни ♡."
                LocalMusicCategory.RECENT -> "Тут з'являться треки, які ти відтвориш у FARIC."
                else -> if (query.isNotBlank()) "За запитом нічого не знайдено" else "Музики в цій категорії немає"
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s?.toString().orEmpty()
                onStateChange(openedGroup, query)
                render?.invoke()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        if (footer != null) body.addView(footer, LinearLayout.LayoutParams(-1, -2))
        if (!canReadMusic) {
            status.text = "Потрібен дозвіл на читання аудіо"
            body.addView(button("Дозволити доступ до музики") { onRequestPermission() },
                LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })
            body.addView(button("Обрати файли вручну") { onPickFiles() },
                LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(7) })
            refresh.visibility = View.GONE
            sortButton.visibility = View.GONE
        } else {
            if (focusSearch) search.requestFocus()
            scan(force = false)
        }
        return viewRoot
    }

    private fun scan(force: Boolean) {
        val token = ++scanToken
        val view = root ?: return
        thread(name = "faric-library-category-index") {
            val result = runCatching { LocalMusicLibrary.scan(activity.applicationContext, force) }
            activity.runOnUiThread {
                if (token != scanToken || !view.isAttachedToWindow || activity.isFinishing || activity.isDestroyed) {
                    return@runOnUiThread
                }
                result.onSuccess {
                    allTracks = it
                    render?.invoke()
                }.onFailure {
                    android.widget.Toast.makeText(activity, "Помилка медіатеки: ${it.message ?: "невідома"}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private data class CellHolder(val title: TextView, val subtitle: TextView, val action: TextView)
}
