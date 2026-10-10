package com.saney.musicvisualizer.board

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

/**
 * Read-only SAF document-tree hero library. The PNG bytes stay in the selected
 * Documents folder: no source artwork is copied into, or bundled with, the APK.
 * Only the selected bitmap is decoded when the renderer requests it.
 *
 * scanFolder and decode must be called on background threads.
 */
class ExternalPngHeroLibrary(private val context: Context) {
    data class Hero(val id: String, val title: String, val uri: String)
    data class ScanResult(val accepted: Int, val rejected: Int, val duplicates: Int)

    private val prefs = context.getSharedPreferences("faric_external_png_heroes_v1", Context.MODE_PRIVATE)

    private fun itemsKey(type: GfArtType) =
        if (type == GfArtType.CREATURE) "items" else "items_${type.fileStem}"

    private fun folderKey(type: GfArtType) =
        if (type == GfArtType.CREATURE) "tree_uri" else "tree_uri_${type.fileStem}"

    fun list(type: GfArtType = GfArtType.CREATURE): List<Hero> = runCatching {
        val arr = JSONArray(prefs.getString(itemsKey(type), "[]"))
        (0 until arr.length()).map { n ->
            arr.getJSONObject(n).let { Hero(it.getString("id"), it.getString("title"), it.getString("uri")) }
        }
    }.getOrDefault(emptyList())

    fun find(id: String, type: GfArtType = GfArtType.CREATURE): Hero? =
        list(type).firstOrNull { it.id == id }

    fun title(id: String, type: GfArtType = GfArtType.CREATURE): String? =
        find(id, type)?.title

    /**
     * Requires the already user-approved ACTION_OPEN_DOCUMENT_TREE Uri.
     * Validation checks actual magic bytes, IHDR alpha color type, and
     * transparent pixels — not a filename ending alone.
     */
    fun scanFolder(
        treeUri: Uri,
        type: GfArtType = GfArtType.CREATURE,
    ): ScanResult {
        require(type in setOf(GfArtType.CREATURE, GfArtType.FRAME, GfArtType.FX, GfArtType.WORDMARK)) {
            "Unsupported external PNG category"
        }
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val docs = mutableListOf<Pair<String, Uri>>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        context.contentResolver.query(children, projection, null, null, null).use { cursor ->
            requireNotNull(cursor) { "Немає доступу до вибраної папки" }
            while (cursor.moveToNext() && docs.size < 300) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val mime = cursor.getString(2) ?: ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                docs.add(name to DocumentsContract.buildDocumentUriUsingTree(treeUri, id))
            }
        }

        var rejected = 0
        var duplicates = 0
        val found = linkedMapOf<String, Hero>()
        for ((name, uri) in docs) {
            if (!name.lowercase(Locale.ROOT).endsWith(".png")) {
                rejected++
                continue
            }
            val digest = hashVerifiedPng(uri)
            if (digest == null) {
                rejected++
                continue
            }
            val id = "png_" + digest.take(40)
            if (id in found) {
                duplicates++
                continue
            }
            val nameWithoutExtension = name.substringBeforeLast(".")
                .replace('_', ' ').trim().take(100)
            found[id] = Hero(id, nameWithoutExtension.ifEmpty { "PNG герой" }, uri.toString())
        }
        // Even an empty folder scan is authoritative, but never overwrite
        // previously indexed heroes on a document-provider exception.
        prefs.edit()
            .putString(folderKey(type), treeUri.toString())
            .putString(itemsKey(type), JSONArray().apply {
                found.values.forEach { hero ->
                    put(JSONObject().apply {
                        put("id", hero.id)
                        put("title", hero.title)
                        put("uri", hero.uri)
                    })
                }
            }.toString())
            .commit()
        return ScanResult(found.size, rejected, duplicates)
    }

    fun activeFolder(type: GfArtType = GfArtType.CREATURE): Uri? =
        prefs.getString(folderKey(type), null)?.let(Uri::parse)

    /** Used only in worker threads, not per frame. Gracefully returns null for inaccessible files. */
    fun decode(
        id: String,
        preferredDimension: Int = 1254,
        type: GfArtType = GfArtType.CREATURE,
    ): Bitmap? {
        val hero = find(id, type) ?: return null
        val uri = Uri.parse(hero.uri)
        return runCatching {
            if (!validPngHeader(uri)) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outMimeType != "image/png" ||
                bounds.outWidth !in 64..4096 || bounds.outHeight !in 64..4096) return null
            var sample = 1
            while (maxOf(bounds.outHeight, bounds.outWidth) / (sample * 2) >= preferredDimension) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)?.takeIf { it.hasAlpha() }
            }
        }.getOrNull()
    }

    private fun validPngHeader(uri: Uri): Boolean {
        return runCatching {
            val bytes = ByteArray(26)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                var at = 0
                while (at < bytes.size) {
                    val n = stream.read(bytes, at, bytes.size - at)
                    if (n < 0) break
                    at += n
                }
                if (at != 26) return false
            } ?: return false
            val signature = byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10)
            signature.indices.all { bytes[it] == signature[it] } &&
                bytes[12] == 73.toByte() && bytes[13] == 72.toByte() &&
                bytes[14] == 68.toByte() && bytes[15] == 82.toByte() &&
                (bytes[25].toInt() and 0xff) in listOf(4, 6)
        }.getOrDefault(false)
    }

    private fun hashVerifiedPng(uri: Uri): String? {
        if (!validPngHeader(uri)) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outMimeType != "image/png" || bounds.outWidth !in 64..4096 ||
            bounds.outHeight !in 64..4096) return null
        val sample = BitmapFactory.Options().apply {
            inSampleSize = 16
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val thumb = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, sample)
        } ?: return null
        var transparent = false
        var opaque = false
        for (y in 0 until thumb.height step 3) {
            for (x in 0 until thumb.width step 3) {
                val alpha = (thumb.getPixel(x,y) ushr 24) and 255
                if (alpha == 0) transparent = true
                if (alpha > 10) opaque = true
            }
        }
        thumb.recycle()
        if (!transparent || !opaque) return null
        val sha = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(32768)
        context.contentResolver.openInputStream(uri)?.use { input ->
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n > 0) sha.update(buffer, 0, n)
            }
        } ?: return null
        return sha.digest().joinToString("") { "%02x".format(it) }
    }
}
