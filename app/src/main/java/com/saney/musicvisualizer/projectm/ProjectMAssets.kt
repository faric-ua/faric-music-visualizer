package com.saney.musicvisualizer.projectm

import android.content.Context
import java.io.File

object ProjectMAssets {
    private val presetNames = listOf(
        "001-line.milk",
        "100-square.milk",
        "251-wavecode-spectrum.milk",
        "300-beatdetect-bassmidtreb.milk",
    )

    fun prepare(context: Context): File {
        val root = File(context.cacheDir, "projectm-spike")
        val presets = File(root, "presets")
        presets.mkdirs()

        for (name in presetNames) {
            val target = File(presets, name)
            if (!target.exists() || target.length() == 0L) {
                context.assets.open("projectm/presets/$name").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }

        return presets
    }
}
