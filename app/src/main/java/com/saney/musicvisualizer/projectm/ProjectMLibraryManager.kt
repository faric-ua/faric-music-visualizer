package com.saney.musicvisualizer.projectm

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

object ProjectMLibraryManager {
    // Keep upstream revisions pinned so the installed library is reproducible.
    private const val CREAM_COMMIT = "0180df21f5e0bd39b9060cc5de420ed2f1f9e509"
    private const val TEXTURE_COMMIT = "6368812f27bc747b517218fbf89d21d59afce4d9"
    private const val EXPECTED_PRESET_COUNT = 9_795
    private const val TEST_PER_CATEGORY = 8

    private val testCategories = listOf(
        "Geometric",
        "Particles",
        "Supernova",
        "Waveform",
        "Hypnotic",
    )

    private val creamUrl =
        "https://github.com/projectM-visualizer/presets-cream-of-the-crop/archive/$CREAM_COMMIT.zip"

    private val textureUrl =
        "https://github.com/projectM-visualizer/presets-milkdrop-texture-pack/archive/$TEXTURE_COMMIT.zip"

    data class LibraryState(
        val installed: Boolean,
        val presetCount: Int,
        val testPresetCount: Int,
    )

    fun root(context: Context): File =
        File(context.filesDir, "projectm-library")

    fun fullPresetDir(context: Context): File =
        File(root(context), "presets/cream-of-the-crop")

    fun textureDir(context: Context): File =
        File(root(context), "textures")

    fun testPresetDir(context: Context): File =
        File(root(context), "presets/faric-test-40")

    private fun marker(context: Context): File =
        File(root(context), ".full-library-$CREAM_COMMIT")

    fun state(context: Context): LibraryState {
        val presetCount = countMilk(fullPresetDir(context))
        val testCount = countMilk(testPresetDir(context))
        return LibraryState(
            installed = marker(context).exists() && presetCount >= EXPECTED_PRESET_COUNT,
            presetCount = presetCount,
            testPresetCount = testCount,
        )
    }

    fun installAll(
        context: Context,
        onProgress: (String) -> Unit,
    ): LibraryState {
        val root = root(context)
        val presets = fullPresetDir(context)
        val textures = textureDir(context)
        val cache = File(context.cacheDir, "projectm-library-downloads")
        val creamZip = File(cache, "cream-$CREAM_COMMIT.zip")
        val textureZip = File(cache, "textures-$TEXTURE_COMMIT.zip")

        root.mkdirs()
        cache.mkdirs()
        presets.deleteRecursively()
        textures.deleteRecursively()
        testPresetDir(context).deleteRecursively()
        marker(context).delete()

        onProgress("Завантаження Cream of the Crop…")
        download(creamUrl, creamZip) { downloadedMb ->
            onProgress("Presets: $downloadedMb MB")
        }

        onProgress("Розпаковка 9 795 preset-ів…")
        var extracted = 0
        unzip(creamZip) { relative, input ->
            if (!relative.endsWith(".milk", ignoreCase = true)) return@unzip

            val target = safeTarget(presets, relative)
            target.parentFile?.mkdirs()
            FileOutputStream(target).use { output -> input.copyTo(output) }

            extracted++
            if (extracted % 250 == 0) {
                onProgress("Розпаковано $extracted preset-ів…")
            }
        }

        onProgress("Завантаження texture pack…")
        download(textureUrl, textureZip) { downloadedMb ->
            onProgress("Textures: $downloadedMb MB")
        }

        onProgress("Розпаковка текстур…")
        unzip(textureZip) { relative, input ->
            if (!relative.startsWith("textures/")) return@unzip
            val normalized = relative.removePrefix("textures/")
            if (normalized.isBlank()) return@unzip

            val target = safeTarget(textures, normalized)
            target.parentFile?.mkdirs()
            FileOutputStream(target).use { output -> input.copyTo(output) }
        }

        val total = countMilk(presets)
        require(total >= EXPECTED_PRESET_COUNT) {
            "Очікувалось щонайменше $EXPECTED_PRESET_COUNT preset-ів, отримано $total"
        }

        buildTestPack(context)

        marker(context).writeText(
            "cream=$CREAM_COMMIT\ntextures=$TEXTURE_COMMIT\npresets=$total\n",
        )

        creamZip.delete()
        textureZip.delete()

        return state(context)
    }

    fun buildTestPack(context: Context): Int {
        val source = fullPresetDir(context)
        val target = testPresetDir(context)

        target.deleteRecursively()
        target.mkdirs()

        var copied = 0

        for (category in testCategories) {
            val categoryDir = File(source, category)
            if (!categoryDir.isDirectory) continue

            val candidates = categoryDir
                .walkTopDown()
                .filter { it.isFile && it.extension.equals("milk", ignoreCase = true) }
                .sortedBy { it.relativeTo(categoryDir).invariantSeparatorsPath.lowercase() }
                .take(TEST_PER_CATEGORY)
                .toList()

            for (preset in candidates) {
                val relative = preset.relativeTo(categoryDir)
                val destination = File(File(target, category), relative.path)
                destination.parentFile?.mkdirs()
                preset.copyTo(destination, overwrite = true)
                copied++
            }
        }

        return copied
    }

    private fun download(
        sourceUrl: String,
        destination: File,
        onProgress: (Int) -> Unit,
    ) {
        val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 60_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "FARIC-Music-Visualizer")
        }

        connection.connect()

        require(connection.responseCode in 200..299) {
            "HTTP ${connection.responseCode} для $sourceUrl"
        }

        var bytes = 0L
        var lastMb = -1

        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)

                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break

                    output.write(buffer, 0, read)
                    bytes += read

                    val mb = (bytes / (1024L * 1024L)).toInt()
                    if (mb != lastMb) {
                        lastMb = mb
                        onProgress(mb)
                    }
                }
            }
        }

        connection.disconnect()
    }

    private fun unzip(
        zipFile: File,
        onFile: (relativePath: String, input: ZipInputStream) -> Unit,
    ) {
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break

                if (!entry.isDirectory) {
                    val relative = stripArchiveRoot(entry.name)

                    if (relative.isNotBlank()) {
                        onFile(relative, zip)
                    }
                }

                zip.closeEntry()
            }
        }
    }

    private fun stripArchiveRoot(path: String): String {
        val normalized = path.replace('\\', '/')
        val slash = normalized.indexOf('/')

        return if (slash >= 0 && slash + 1 < normalized.length) {
            normalized.substring(slash + 1)
        } else {
            ""
        }
    }

    private fun safeTarget(root: File, relative: String): File {
        val target = File(root, relative)
        val rootPath = root.canonicalFile.toPath()
        val targetPath = target.canonicalFile.toPath()

        require(targetPath.startsWith(rootPath)) {
            "Unsafe zip entry: $relative"
        }

        return target
    }

    private fun countMilk(root: File): Int {
        if (!root.isDirectory) return 0

        return root.walkTopDown()
            .count { it.isFile && it.extension.equals("milk", ignoreCase = true) }
    }
}
