package com.saney.musicvisualizer.projectm

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.math.floor

object ProjectMLibraryManager {
    private const val CREAM_COMMIT = "0180df21f5e0bd39b9060cc5de420ed2f1f9e509"
    private const val TEXTURE_COMMIT = "6368812f27bc747b517218fbf89d21d59afce4d9"
    private const val EXPECTED_PRESET_COUNT = 9_795
    private const val FAST_PRESET_TARGET = 1_200
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
        val topPresetCount: Int,
        val testPresetCount: Int,
        val indexed: Boolean,
    )

    fun root(context: Context): File =
        File(context.filesDir, "projectm-library")

    fun fullPresetDir(context: Context): File =
        File(root(context), "presets/cream-of-the-crop")

    /**
     * Legacy v0.5.x directory. New versions no longer copy thousands of presets here.
     * It is retained only for migration cleanup.
     */
    fun topPresetDir(context: Context): File =
        File(root(context), "presets/faric-top-half")

    fun textureDir(context: Context): File =
        File(root(context), "textures")

    fun testPresetDir(context: Context): File =
        File(root(context), "presets/faric-test-40")

    private fun indexDir(context: Context): File =
        File(root(context), "index")

    private fun fullIndexFile(context: Context): File =
        File(indexDir(context), "cream-$CREAM_COMMIT-all.txt")

    private fun fastIndexFile(context: Context): File =
        File(indexDir(context), "cream-$CREAM_COMMIT-fast-$FAST_PRESET_TARGET.txt")

    private fun marker(context: Context): File =
        File(root(context), ".full-library-$CREAM_COMMIT")

    fun expectedTopCount(): Int = FAST_PRESET_TARGET

    fun presetId(file: File): String {
        val normalized =
            file.absolutePath.replace('\\', '/')

        val roots =
            listOf(
                "/cream-of-the-crop/",
                "/faric-top-half/",
                "/faric-test-40/",
            )

        for (root in roots) {
            val index = normalized.indexOf(root)
            if (index >= 0) {
                return normalized
                    .substring(index + root.length)
                    .lowercase()
            }
        }

        return file.name.lowercase()
    }

    /**
     * Cheap main-thread-safe state read: no recursive traversal of 9k files.
     */
    fun state(context: Context): LibraryState {
        val markerInfo = readMarker(context)
        val fullCount =
            readIndexCount(fullIndexFile(context))
                .takeIf { it > 0 }
                ?: markerInfo["presets"]?.toIntOrNull()
                ?: 0

        val fastCount = readIndexCount(fastIndexFile(context))
        val testCount =
            markerInfo["test"]?.toIntOrNull()
                ?: if (testPresetDir(context).isDirectory) 40 else 0

        return LibraryState(
            installed =
                marker(context).exists() &&
                    fullPresetDir(context).isDirectory &&
                    fullCount >= EXPECTED_PRESET_COUNT,
            presetCount = fullCount,
            topPresetCount = fastCount,
            testPresetCount = testCount,
            indexed =
                fullIndexFile(context).isFile &&
                    fastIndexFile(context).isFile,
        )
    }

    fun ensureDerivedPacks(context: Context): LibraryState {
        val before = state(context)
        if (!before.installed) return before

        if (!before.indexed || before.topPresetCount != FAST_PRESET_TARGET) {
            buildIndexes(context)
        }

        if (before.testPresetCount < 40) {
            buildTestPack(context)
        }

        // v0.5.x physically duplicated roughly half the library.
        topPresetDir(context).deleteRecursively()

        val after = state(context)
        writeMarker(
            context = context,
            presetCount = after.presetCount,
            testCount = after.testPresetCount,
        )
        return state(context)
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
        topPresetDir(context).deleteRecursively()
        testPresetDir(context).deleteRecursively()
        indexDir(context).deleteRecursively()
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

        onProgress("Будую індекс бібліотеки…")
        buildIndexes(context)

        onProgress("Формую TEST 40…")
        val testCount = buildTestPack(context)

        writeMarker(
            context = context,
            presetCount = total,
            testCount = testCount,
        )

        creamZip.delete()
        textureZip.delete()

        return state(context)
    }

    /**
     * ALL and FAST are indexes of the same original files.
     * No duplicate 1,200/4,898 .milk files are created.
     */
    fun buildIndexes(context: Context): Int {
        val source = fullPresetDir(context)
        val indexRoot = indexDir(context)
        indexRoot.mkdirs()

        val relativePaths =
            source
                .walkTopDown()
                .filter {
                    it.isFile &&
                        it.extension.equals(
                            "milk",
                            ignoreCase = true,
                        )
                }
                .map {
                    it.relativeTo(source)
                        .invariantSeparatorsPath
                }
                .sortedBy { it.lowercase() }
                .toList()

        require(relativePaths.size >= EXPECTED_PRESET_COUNT) {
            "Недостатньо projectM preset-ів для індексації: ${relativePaths.size}"
        }

        writeIndex(
            fullIndexFile(context),
            relativePaths,
        )

        val fast =
            evenlyDistributedSample(
                values = relativePaths,
                target = FAST_PRESET_TARGET,
            )

        writeIndex(
            fastIndexFile(context),
            fast,
        )

        return fast.size
    }

    fun allPresetFiles(context: Context): List<File> =
        readIndexedFiles(
            base = fullPresetDir(context),
            index = fullIndexFile(context),
        )

    fun fastPresetFiles(context: Context): List<File> =
        readIndexedFiles(
            base = fullPresetDir(context),
            index = fastIndexFile(context),
        )

    fun firstIndexedPreset(context: Context): File? {
        val base = fullPresetDir(context)
        val first =
            fullIndexFile(context)
                .takeIf { it.isFile }
                ?.useLines { lines ->
                    lines.firstOrNull { it.isNotBlank() }
                }
                ?: return null

        return File(base, first)
            .takeIf { it.isFile }
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

            val candidates =
                categoryDir
                    .walkTopDown()
                    .filter {
                        it.isFile &&
                            it.extension.equals(
                                "milk",
                                ignoreCase = true,
                            )
                    }
                    .sortedBy {
                        it.relativeTo(categoryDir)
                            .invariantSeparatorsPath
                            .lowercase()
                    }
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

    private fun evenlyDistributedSample(
        values: List<String>,
        target: Int,
    ): List<String> {
        if (values.size <= target) return values

        val step = values.size.toDouble() / target.toDouble()

        return List(target) { index ->
            values[
                floor(index * step)
                    .toInt()
                    .coerceIn(0, values.lastIndex)
            ]
        }.distinct()
    }

    private fun writeIndex(
        file: File,
        values: List<String>,
    ) {
        file.parentFile?.mkdirs()
        file.writeText(
            values.joinToString(
                separator = "\n",
                postfix = "\n",
            ),
        )
    }

    private fun readIndexedFiles(
        base: File,
        index: File,
    ): List<File> {
        if (!index.isFile) return emptyList()

        return index
            .readLines()
            .asSequence()
            .filter { it.isNotBlank() }
            .map { File(base, it) }
            .filter { it.isFile }
            .toList()
    }

    private fun readIndexCount(file: File): Int {
        if (!file.isFile) return 0

        return file.useLines { lines ->
            lines.count { it.isNotBlank() }
        }
    }

    private fun readMarker(context: Context): Map<String, String> {
        val file = marker(context)
        if (!file.isFile) return emptyMap()

        return file
            .readLines()
            .mapNotNull { line ->
                val split = line.indexOf('=')
                if (split <= 0) return@mapNotNull null

                line.substring(0, split) to
                    line.substring(split + 1)
            }
            .toMap()
    }

    private fun writeMarker(
        context: Context,
        presetCount: Int,
        testCount: Int,
    ) {
        marker(context).writeText(
            buildString {
                append("cream=$CREAM_COMMIT\n")
                append("textures=$TEXTURE_COMMIT\n")
                append("presets=$presetCount\n")
                append("fast=$FAST_PRESET_TARGET\n")
                append("test=$testCount\n")
                append("indexVersion=2\n")
            },
        )
    }

    private fun download(
        sourceUrl: String,
        destination: File,
        onProgress: (Int) -> Unit,
    ) {
        val connection =
            (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 20_000
                readTimeout = 60_000
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "FARIC-Music-Visualizer",
                )
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

                    val mb =
                        (bytes / (1024L * 1024L))
                            .toInt()

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
        onFile: (
            relativePath: String,
            input: ZipInputStream,
        ) -> Unit,
    ) {
        ZipInputStream(
            BufferedInputStream(
                zipFile.inputStream(),
            ),
        ).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break

                if (!entry.isDirectory) {
                    val relative =
                        stripArchiveRoot(
                            entry.name,
                        )

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

        return if (
            slash >= 0 &&
            slash + 1 < normalized.length
        ) {
            normalized.substring(slash + 1)
        } else {
            ""
        }
    }

    private fun safeTarget(
        root: File,
        relative: String,
    ): File {
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

        return root
            .walkTopDown()
            .count {
                it.isFile &&
                    it.extension.equals(
                        "milk",
                        ignoreCase = true,
                    )
            }
    }
}
