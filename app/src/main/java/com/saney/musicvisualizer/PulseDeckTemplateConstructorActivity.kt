package com.saney.musicvisualizer

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.saney.musicvisualizer.ui.PulseDeckMainSkinView
import com.saney.musicvisualizer.ui.PulseDeckTemplateConstructorOverlayView

class PulseDeckTemplateConstructorActivity : ComponentActivity() {
    private lateinit var overlay: PulseDeckTemplateConstructorOverlayView
    private var pendingExport: String? = null

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        val json = pendingExport
        pendingExport = null
        if (uri == null || json == null) return@registerForActivityResult
        runCatching {
            contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(json) }
                ?: error("Не вдалося відкрити файл")
        }.onSuccess {
            Toast.makeText(this, "Шаблон експортовано", Toast.LENGTH_LONG).show()
        }.onFailure { error ->
            Toast.makeText(this, "EXPORT: " + (error.message ?: "помилка"), Toast.LENGTH_LONG).show()
        }
    }

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Не вдалося прочитати файл")
            overlay.importTemplateJson(json)
        }.onSuccess {
            Toast.makeText(this, "Шаблон імпортовано", Toast.LENGTH_LONG).show()
        }.onFailure { error ->
            Toast.makeText(this, "IMPORT: " + (error.message ?: "помилка"), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()
        val root = FrameLayout(this)
        val skin = PulseDeckMainSkinView(this).apply {
            setPlaying(true)
            setPlaybackContent(
                title = "Neon Heartbeat (3).mp3",
                artist = "Невідомий виконавець",
                status = "Template Constructor · PulseDeck",
                elapsed = "0:39",
                total = "3:39",
                progressFraction = 0.18f,
            )
            setControlsVisible(true, false)
        }
        root.addView(skin, FrameLayout.LayoutParams(-1, -1))
        overlay = PulseDeckTemplateConstructorOverlayView(
            context = this,
            sourceView = skin,
            onExportRequested = { json ->
                pendingExport = json
                createDocument.launch("PulseDeck_template.json")
            },
            onImportRequested = {
                openDocument.launch(arrayOf("application/json", "text/plain"))
            },
        )
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    private fun enableImmersiveFullscreen() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}
