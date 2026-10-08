package com.saney.musicvisualizer.board

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.saney.musicvisualizer.theme.PlaybackThemeId
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Prepared 1024x1024 aligned artwork layers. Can be bundled in
 * skin/hero_packs/<hero-slug> (automatically available offline) or installed
 * using an already-prepared ZIP. Raw Pictures2 design sheets must NOT be
 * treated as separated production assets.
 */
object UserHeroPack {
    data class Hero(
        val id: PlaybackThemeId,
        val title: String,
        val description: String,
        val sourceFilename: String,
    )

    val heroes: List<Hero> = listOf(
        Hero(PlaybackThemeId.HERO_SABER_TIGER, "Saber Tiger", "Крижаний тигр · FARIC", "file_0000000059388210b188ddfa87ec13b0.png"),
        Hero(PlaybackThemeId.HERO_THUNDER_WOLF, "Thunder Wolf", "Електричний вовк · FMV", "file_00000000acbc8210bab7cfe485fb770e.png"),
        Hero(PlaybackThemeId.HERO_ARCANE_SPECTER, "Arcane Specter", "Блакитний містичний вартовий · FARIC", "file_00000000c36c81f4b3f55d260ac33c38.png"),
        Hero(PlaybackThemeId.HERO_INFERNO_PHOENIX, "Inferno Phoenix", "Вогняний фенікс · FARIC", "file_000000004d088210869b6db73ddbb873.png"),
        Hero(PlaybackThemeId.HERO_PLASMA_COBRA, "Plasma Cobra", "Фіолетова кобра · FVMP", "file_0000000065548210b363e525ba124732.png"),
        Hero(PlaybackThemeId.HERO_INFERNO_WARLOCK, "Inferno Warlock", "Вогняний чаклун · FARIC", "file_0000000091e481f487508f382a705d3e.png"),
        Hero(PlaybackThemeId.HERO_NEON_GRIFFIN, "Neon Griffin", "Синьо-золотий грифон · FMV", "file_00000000d67c81f49f7a0c7da3555a3e.png"),
        Hero(PlaybackThemeId.HERO_MECHA_PANTHER, "Mecha Panther", "Броньована пантера · FARIC", "file_0000000034f082108ffe5b7ce8438b11.png"),
        Hero(PlaybackThemeId.HERO_VOID_DRAGON, "Void Dragon", "Фіолетовий дракон · FARIC", "file_00000000e734824681b61d80ec790410.png"),
        Hero(PlaybackThemeId.HERO_TITAN_SCORPION, "Titan Scorpion", "Кіберскорпіон · FARIC", "file_00000000970c81f49b75c4098058b512.png"),
    )

    private val layers = listOf("frame", "fx", "creature", "wordmark", "full", "preview")

    fun find(id: PlaybackThemeId): Hero? = heroes.firstOrNull { it.id == id }

    private fun slug(id: PlaybackThemeId): String =
        id.name.removePrefix("HERO_").lowercase().replace('_', '-')

    private fun localFolder(context: Context, id: PlaybackThemeId): File =
        File(context.filesDir, "graphic-figure-packs/prepared/" + slug(id))

    private fun packagedPath(id: PlaybackThemeId, layer: String): String =
        "hero_packs/" + slug(id) + "/$layer.webp"

    private fun packaged(context: Context, id: PlaybackThemeId): Boolean =
        runCatching {
            context.assets.open(packagedPath(id, "preview")).use { true }
        }.getOrDefault(false)

    fun isInstalled(context: Context, id: PlaybackThemeId): Boolean {
        if (find(id) == null) return false
        if (packaged(context, id)) return true
        val folder = localFolder(context, id)
        return layers.all { File(folder, "$it.webp").isFile }
    }

    fun loadLayer(
        context: Context,
        id: PlaybackThemeId,
        layer: String,
        previewOnly: Boolean = false,
    ): Bitmap? {
        if (find(id) == null || layer !in layers) return null
        val options = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = if (previewOnly && layer != "preview") 4 else 1
        }
        if (packaged(context, id)) {
            return runCatching {
                context.assets.open(packagedPath(id, layer)).use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                }
            }.getOrNull()
        }
        return BitmapFactory.decodeFile(
            File(localFolder(context, id), "$layer.webp").absolutePath,
            options,
        )
    }

    fun loadEmblem(
        context: Context,
        id: PlaybackThemeId,
        preview: Boolean = false,
    ): Bitmap? = loadLayer(context, id, if (preview) "preview" else "full", previewOnly = preview)

    /**
     * Optional offline fallback if artwork has not been committed to app assets.
     * Only accepts a PREPARED pack (full + four 1024px layers + preview).
     * An original Pictures2.zip intentionally fails validation.
     */
    fun importPreparedZip(context: Context, uri: Uri): Int {
        val staging = File(context.cacheDir, "faric-hero-stage-" + System.nanoTime())
        check(staging.mkdirs()) { "Не вдалося підготувати тимчасове сховище" }
        try {
            val expectedFolders = heroes.associateBy { slug(it.id) }
            val received = mutableSetOf<String>()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val segments = entry.name.split('/')
                        if (!entry.isDirectory &&
                            segments.size == 2 &&
                            segments[0] in expectedFolders &&
                            segments[1] in layers.map { "$it.webp" }
                        ) {
                            val folder = File(staging, segments[0])
                            check(folder.isDirectory || folder.mkdirs()) {
                                "Не вдалося створити каталог героя"
                            }
                            val dest = File(folder, segments[1])
                            check(!dest.exists()) { "Знайдено повтор: ${entry.name}" }
                            FileOutputStream(dest).use { output ->
                                val buffer = ByteArray(32 * 1024)
                                var total = 0L
                                while (true) {
                                    val n = zip.read(buffer)
                                    if (n < 0) break
                                    total += n
                                    check(total <= 12L * 1024L * 1024L) {
                                        "Завеликий файл: ${entry.name}"
                                    }
                                    output.write(buffer, 0, n)
                                }
                            }
                            received.add(entry.name)
                        }
                        zip.closeEntry()
                    }
                }
            } ?: error("Не вдалося відкрити підготовлений ZIP")

            for (hero in heroes) {
                val folder = File(staging, slug(hero.id))
                check(layers.all { File(folder, "$it.webp").exists() }) {
                    "Неповний пакет: ${hero.title}. Потрібен FARIC-Heroes-Prepared ZIP."
                }
                for (layer in listOf("frame", "fx", "creature", "wordmark", "full")) {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(File(folder, "$layer.webp").absolutePath, opts)
                    check(opts.outWidth == 1024 && opts.outHeight == 1024) {
                        "Некоректний шар ${hero.title}/$layer"
                    }
                }
            }
            // Do not remove existing install until the entire archive validates.
            for (hero in heroes) {
                val source = File(staging, slug(hero.id))
                val target = localFolder(context, hero.id)
                check(target.parentFile?.isDirectory == true ||
                    target.parentFile?.mkdirs() == true) { "Немає доступу до сховища" }
                // Copying all assets before touching existing complete packs.
                val tmp = File(target.parentFile, target.name + ".incoming")
                tmp.deleteRecursively()
                check(source.copyRecursively(tmp, overwrite = true))
                target.deleteRecursively()
                check(tmp.renameTo(target)) { "Не вдалося встановити ${hero.title}" }
            }
            return heroes.size
        } finally {
            staging.deleteRecursively()
        }
    }
}
