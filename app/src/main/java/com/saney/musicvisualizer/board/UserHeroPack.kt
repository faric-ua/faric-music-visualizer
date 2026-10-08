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
 * User-provided Pictures2.zip artwork is kept outside the APK until the user
 * explicitly imports the archive. Filenames are matched exactly: ZIP entry
 * order must never determine hero identity. Source PNGs are preserved as-is.
 *
 * Initial skin uses the composite emblem area. Independent production layers
 * are NOT inferred from the contact sheet and need separate visual QA.
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

    fun find(id: PlaybackThemeId): Hero? = heroes.firstOrNull { it.id == id }

    private fun directory(context: Context): File =
        File(context.filesDir, "graphic-figure-packs/pictures2")

    private fun source(context: Context, hero: Hero): File =
        File(directory(context), hero.id.name + ".png")

    fun isInstalled(context: Context, id: PlaybackThemeId): Boolean =
        find(id)?.let { source(context, it).isFile } ?: false

    /**
     * Transactional per-file import, no zip-slip paths and no partly installed
     * archive: validate all ten files before moving them into the active pack.
     */
    fun importZip(context: Context, uri: Uri): Int {
        val temporary = File(context.cacheDir, "pictures2-stage-" + System.nanoTime())
        check(temporary.mkdirs()) { "Не вдалося створити тимчасову папку" }
        try {
            val expected = heroes.associateBy { it.sourceFilename }
            val received = mutableSetOf<PlaybackThemeId>()
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val hero = expected[entry.name.substringAfterLast('/')]
                        if (!entry.isDirectory && hero != null) {
                            check(hero.id !in received) { "Повторне зображення: ${hero.title}" }
                            val target = File(temporary, hero.id.name + ".png")
                            FileOutputStream(target).use { output ->
                                val buffer = ByteArray(32 * 1024)
                                var length = 0L
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    length += count
                                    check(length <= 12L * 1024L * 1024L) { "Зображення завелике" }
                                    output.write(buffer, 0, count)
                                }
                            }
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(target.absolutePath, bounds)
                            check(bounds.outWidth >= 800 && bounds.outHeight >= 800) {
                                "Неправильний PNG: ${hero.title}"
                            }
                            received += hero.id
                        }
                        zip.closeEntry()
                    }
                }
            } ?: error("Не вдалося прочитати ZIP")
            check(received.size == heroes.size) {
                "Очікувалося ${heroes.size} героїв, знайдено ${received.size}"
            }
            val installed = directory(context)
            check(installed.isDirectory || installed.mkdirs()) { "Немає доступу до сховища" }
            for (hero in heroes) {
                val staged = File(temporary, hero.id.name + ".png")
                val dest = source(context, hero)
                if (!staged.renameTo(dest)) {
                    staged.copyTo(dest, overwrite = true)
                    staged.delete()
                }
            }
            return received.size
        } finally {
            temporary.deleteRecursively()
        }
    }

    /**
     * Preview-stage single emblem, cropped from the upper-left composite of
     * the original design sheet. Keeps original pixels/alpha and proportions.
     * Full independent frame/FX/creature/wordmark assets require art review.
     */
    fun loadEmblem(context: Context, id: PlaybackThemeId, preview: Boolean = false): Bitmap? {
        val hero = find(id) ?: return null
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = if (preview) 4 else 1
        }
        val original = BitmapFactory.decodeFile(source(context, hero).absolutePath, options)
            ?: return null
        val cropW = (original.width * 0.57f).toInt().coerceAtLeast(1)
        val cropH = (original.height * 0.54f).toInt().coerceAtLeast(1)
        val emblem = Bitmap.createBitmap(original, 0, 0, cropW, cropH)
        if (emblem !== original) original.recycle()
        return emblem
    }
}
