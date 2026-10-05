package com.saney.musicvisualizer.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.saney.musicvisualizer.theme.HeroThemeRenderer
import com.saney.musicvisualizer.theme.MusicVideoProject
import com.saney.musicvisualizer.theme.ThemeInput
import java.io.File
import java.io.FileOutputStream

object ExportFrameProof {
    fun render(
        context: Context,
        project: MusicVideoProject,
        input: ThemeInput,
        timeMs: Long,
        cyberSharkConfig:
            CyberSharkExportConfig? = null,
        compositionConfig:
            CompositionExportConfig? = null,
        title: String = input.title,
        artist: String = input.artist,
        durationMs: Long = input.durationMs,
    ): Bitmap {
        val bitmap =
            Bitmap.createBitmap(
                project.aspectRatio.width,
                project.aspectRatio.height,
                Bitmap.Config.ARGB_8888,
            )

        val canvas = Canvas(bitmap)

        val signal =
            com.saney.musicvisualizer.analysis
                .SceneSignal(
                    amplitude = input.amplitude,
                    bass = input.bass,
                    mid = input.mid,
                    high = input.high,
                    beatStrength = input.beat,
                )

        if (compositionConfig != null) {
            CompositionExportRenderer(
                context = context,
                config = compositionConfig,
            ).render(
                canvas = canvas,
                width = bitmap.width,
                height = bitmap.height,
                timeMs = timeMs,
                signal = signal,
                title = title,
                artist = artist,
                durationMs = durationMs,
                playing = true,
            )
        } else if (
            project.themeId ==
                com.saney.musicvisualizer.theme
                    .PlaybackThemeId.CYBER_SHARK &&
            cyberSharkConfig != null
        ) {
            CyberSharkExportRenderer(
                context,
            ).render(
                canvas = canvas,
                width = bitmap.width,
                height = bitmap.height,
                timeMs = timeMs,
                signal = signal,
                config =
                    cyberSharkConfig,
            )
        } else {
            HeroThemeRenderer.render(
                canvas = canvas,
                width = bitmap.width,
                height = bitmap.height,
                timeMs = timeMs,
                themeId = project.themeId,
                input = input,
            )
        }

        return bitmap
    }

    fun savePng(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
    ): Uri? {
        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            val values =
                ContentValues().apply {
                    put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        displayName,
                    )
                    put(
                        MediaStore.Images.Media.MIME_TYPE,
                        "image/png",
                    )
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "Pictures/FARIC",
                    )
                    put(
                        MediaStore.Images.Media.IS_PENDING,
                        1,
                    )
                }

            val resolver =
                context.contentResolver

            val uri =
                resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values,
                )
                    ?: return null

            try {
                resolver
                    .openOutputStream(uri)
                    ?.use { output ->
                        check(
                            bitmap.compress(
                                Bitmap.CompressFormat.PNG,
                                100,
                                output,
                            ),
                        )
                    }

                values.clear()
                values.put(
                    MediaStore.Images.Media.IS_PENDING,
                    0,
                )
                resolver.update(
                    uri,
                    values,
                    null,
                    null,
                )

                uri
            } catch (error: Throwable) {
                resolver.delete(
                    uri,
                    null,
                    null,
                )
                throw error
            }
        } else {
            val dir =
                File(
                    context.getExternalFilesDir(null),
                    "FARIC",
                )
            dir.mkdirs()

            val file =
                File(
                    dir,
                    displayName,
                )

            FileOutputStream(file).use {
                check(
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        it,
                    ),
                )
            }

            Uri.fromFile(file)
        }
    }
}
