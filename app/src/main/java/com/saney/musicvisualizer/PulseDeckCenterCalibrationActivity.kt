package com.saney.musicvisualizer

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.saney.musicvisualizer.ui.PulseDeckCenterCalibrationOverlayView
import com.saney.musicvisualizer.ui.PulseDeckMainSkinView

/**
 * Temporary engineering page used to calibrate the exact centers of the
 * permanent PulseDeck main-page objects on the real phone viewport.
 */
class PulseDeckCenterCalibrationActivity :
    ComponentActivity() {

    private lateinit var calibrationOverlay:
        PulseDeckCenterCalibrationOverlayView

    private var pendingExportJson:
        String? = null

    private val createSnapshotDocument =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/json",
            ),
        ) { uri: Uri? ->
            val json =
                pendingExportJson
            pendingExportJson = null

            if (
                uri == null ||
                json == null
            ) {
                return@registerForActivityResult
            }

            runCatching {
                contentResolver
                    .openOutputStream(
                        uri,
                        "wt",
                    )
                    ?.bufferedWriter()
                    ?.use { writer ->
                        writer.write(
                            json,
                        )
                    }
                    ?: error(
                        "Не вдалося відкрити файл для запису",
                    )
            }
                .onSuccess {
                    Toast.makeText(
                        this,
                        "Calibration snapshot збережено",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        this,
                        "EXPORT помилка: " +
                            (
                                error.message
                                    ?: "невідома"
                                ),
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }

    private val openSnapshotDocument =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (
                uri == null
            ) {
                return@registerForActivityResult
            }

            runCatching {
                val json =
                    contentResolver
                        .openInputStream(
                            uri,
                        )
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        ?: error(
                            "Не вдалося прочитати snapshot",
                        )

                calibrationOverlay
                    .importCalibrationSnapshotJson(
                        json,
                    )
            }
                .onSuccess { imported ->
                    Toast.makeText(
                        this,
                        "IMPORT: відновлено " +
                            imported +
                            " збережених центрів",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        this,
                        "IMPORT помилка: " +
                            (
                                error.message
                                    ?: "невідома"
                                ),
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )

        enableImmersiveFullscreen()

        val root =
            FrameLayout(this)

        val skin =
            PulseDeckMainSkinView(this).apply {
                setPlaying(true)
                setPlaybackContent(
                    title =
                        "Neon Heartbeat (3).mp3",
                    artist =
                        "Невідомий виконавець",
                    status =
                        "Відтворення · reactive ON · PulseDeck HUD",
                    elapsed =
                        "0:39",
                    total =
                        "3:39",
                    progressFraction =
                        0.18f,
                )
                setControlsVisible(
                    visible = true,
                    animate = false,
                )
            }

        root.addView(
            skin,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        calibrationOverlay =
            PulseDeckCenterCalibrationOverlayView(
                context = this,
                sourceView = skin,
                onExportRequested = { json ->
                    pendingExportJson =
                        json

                    createSnapshotDocument.launch(
                        manualSnapshotFileName(),
                    )
                },
                onImportRequested = {
                    openSnapshotDocument.launch(
                        arrayOf(
                            "application/json",
                            "text/plain",
                        ),
                    )
                },
                onAutoSnapshotRequested = { json ->
                    writeAutomaticSnapshot(
                        json,
                    )
                },
            )

        root.addView(
            calibrationOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        setContentView(root)
    }

    private fun manualSnapshotFileName(): String =
        "PulseDeck_calibration_" +
            System.currentTimeMillis() +
            ".json"

    private fun writeAutomaticSnapshot(
        json: String,
    ) {
        runCatching {
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                writeAutomaticSnapshotMediaStore(
                    json,
                )
            } else {
                val dir =
                    java.io.File(
                        getExternalFilesDir(
                            Environment.DIRECTORY_DOCUMENTS,
                        ),
                        "FARIC/PulseDeck",
                    )

                dir.mkdirs()

                java.io.File(
                    dir,
                    AUTO_SNAPSHOT_FILE,
                )
                    .writeText(
                        json,
                    )
            }
        }
            .onFailure { error ->
                Toast.makeText(
                    this,
                    "Auto-backup помилка: " +
                        (
                            error.message
                                ?: "невідома"
                            ),
                    Toast.LENGTH_LONG,
                ).show()
            }
    }

    private fun writeAutomaticSnapshotMediaStore(
        json: String,
    ) {
        val collection =
            MediaStore.Downloads
                .getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY,
                )

        val relativePath =
            Environment.DIRECTORY_DOWNLOADS +
                "/FARIC/PulseDeck/"

        var targetUri:
            Uri? = null

        val projection =
            arrayOf(
                MediaStore.Downloads._ID,
            )

        val selection =
            MediaStore.Downloads.DISPLAY_NAME +
                "=? AND " +
                MediaStore.Downloads.RELATIVE_PATH +
                "=?"

        val args =
            arrayOf(
                AUTO_SNAPSHOT_FILE,
                relativePath,
            )

        contentResolver
            .query(
                collection,
                projection,
                selection,
                args,
                null,
            )
            ?.use { cursor ->
                if (
                    cursor.moveToFirst()
                ) {
                    val id =
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Downloads._ID,
                            ),
                        )

                    targetUri =
                        Uri.withAppendedPath(
                            collection,
                            id.toString(),
                        )
                }
            }

        if (
            targetUri == null
        ) {
            val values =
                ContentValues().apply {
                    put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        AUTO_SNAPSHOT_FILE,
                    )
                    put(
                        MediaStore.Downloads.MIME_TYPE,
                        "application/json",
                    )
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        relativePath,
                    )
                    put(
                        MediaStore.Downloads.IS_PENDING,
                        1,
                    )
                }

            targetUri =
                contentResolver.insert(
                    collection,
                    values,
                )
                    ?: error(
                        "Не вдалося створити auto-backup",
                    )
        }

        contentResolver
            .openOutputStream(
                targetUri!!,
                "wt",
            )
            ?.bufferedWriter()
            ?.use { writer ->
                writer.write(
                    json,
                )
            }
            ?: error(
                "Не вдалося записати auto-backup",
            )

        val ready =
            ContentValues().apply {
                put(
                    MediaStore.Downloads.IS_PENDING,
                    0,
                )
            }

        contentResolver.update(
            targetUri!!,
            ready,
            null,
            null,
        )
    }

    private fun enableImmersiveFullscreen() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }
    companion object {
        private const val AUTO_SNAPSHOT_FILE =
            "PulseDeck_calibration_latest.json"
    }

}
