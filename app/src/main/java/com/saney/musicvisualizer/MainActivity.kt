package com.saney.musicvisualizer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.playback.PlaybackController
import com.saney.musicvisualizer.playback.PlaybackSnapshot
import com.saney.musicvisualizer.ui.ReactiveSceneView
import java.util.Locale

@UnstableApi
class MainActivity : ComponentActivity(), PlaybackController.Listener {
    private lateinit var controller: PlaybackController
    private lateinit var sceneView: ReactiveSceneView
    private lateinit var titleView: TextView
    private lateinit var statusView: TextView
    private lateinit var playButton: Button
    private var pendingOpenAfterPermission = false

    private val openAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            controller.load(uri, resolveDisplayName(uri))
            controller.play()
        }
    }
    private val requestAudioAnalysisPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            controller.setAnalysisPermissionGranted(granted)
            if (pendingOpenAfterPermission) {
                pendingOpenAfterPermission = false
                openAudio.launch(arrayOf("audio/*"))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = ViewModelProvider(this).get(PlaybackController::class.java)
        controller.setAnalysisPermissionGranted(hasAnalysisPermission())

        sceneView = ReactiveSceneView(this)
        titleView = textView(20f, Color.WHITE).apply { text = "FARIC Music Visualizer"; maxLines = 2 }
        statusView = textView(13f, Color.rgb(205, 207, 218)).apply { text = "Оберіть локальний аудіофайл"; maxLines = 3 }

        val chooseButton = Button(this).apply { text = "Обрати музику"; setOnClickListener { chooseTrack() } }
        playButton = Button(this).apply { text = "▶"; isEnabled = false; setOnClickListener { controller.togglePlayPause() } }

        val root = FrameLayout(this)
        root.addView(sceneView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(18))
            setBackgroundColor(Color.argb(205, 12, 13, 20))
        }
        panel.addView(titleView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        panel.addView(statusView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(5) })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        buttons.addView(chooseButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        buttons.addView(playButton, LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8) })
        panel.addView(buttons, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        root.addView(panel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        setContentView(root)
    }

    override fun onStart() { super.onStart(); controller.listener = this; controller.emitCurrentState() }
    override fun onStop() {
        controller.listener = null
        if (!isChangingConfigurations) controller.pause()
        super.onStop()
    }

    override fun onPlaybackSnapshot(snapshot: PlaybackSnapshot) {
        titleView.text = snapshot.trackName ?: "FARIC Music Visualizer"
        playButton.isEnabled = snapshot.trackName != null
        playButton.text = if (snapshot.isPlaying) "Ⅱ" else "▶"
        sceneView.setPlaying(snapshot.isPlaying)
        val timeline = if (snapshot.trackName == null) "" else " · ${formatTime(snapshot.positionMs)} / ${formatTime(snapshot.durationMs)}"
        val analyzer = when {
            snapshot.trackName == null -> ""
            snapshot.analysisActive -> " · audio-reactive ON"
            else -> " · audio-reactive OFF"
        }
        statusView.text = snapshot.status + timeline + analyzer
    }

    override fun onSceneSignal(signal: SceneSignal) = sceneView.updateSignal(signal)

    private fun chooseTrack() {
        if (hasAnalysisPermission()) openAudio.launch(arrayOf("audio/*"))
        else {
            pendingOpenAfterPermission = true
            requestAudioAnalysisPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun hasAnalysisPermission() =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun resolveDisplayName(uri: Uri): String {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else uri.lastPathSegment.orEmpty()
            } else uri.lastPathSegment.orEmpty()
        } catch (_: Throwable) {
            uri.lastPathSegment ?: "Аудіофайл"
        } finally { cursor?.close() }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val seconds = ms / 1000L
        return String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L)
    }
    private fun textView(sizeSp: Float, color: Int) = TextView(this).apply { textSize = sizeSp; setTextColor(color) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
