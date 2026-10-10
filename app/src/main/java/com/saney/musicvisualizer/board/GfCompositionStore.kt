package com.saney.musicvisualizer.board

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Saves only references and per-instance transforms, NEVER copied artwork.
 * Missing external assets retain their original sourcePackId for repair.
 * Existing BoardLayerTransformStore and current GF selections are untouched.
 */
class GfCompositionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun listAll(): List<GfComposition> {
        val raw = prefs.getString(KEY, "[]") ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            runCatching { decode(array.getJSONObject(index)) }.getOrNull()
        }
    }

    fun load(compositionId: String): GfComposition? =
        listAll().firstOrNull { it.compositionId == compositionId }

    @Synchronized
    fun save(composition: GfComposition) {
        val safe = composition.sanitized()
        val current = listAll()
        val next = if (current.any { it.compositionId == safe.compositionId }) {
            current.map { if (it.compositionId == safe.compositionId) safe else it }
        } else {
            current + safe
        }
        prefs.edit().putString(KEY, encodeAll(next).toString()).apply()
    }

    @Synchronized
    fun remove(compositionId: String) {
        val next = listAll().filterNot { it.compositionId == compositionId }
        prefs.edit().putString(KEY, encodeAll(next).toString()).apply()
    }

    private fun encodeAll(items: List<GfComposition>): JSONArray =
        JSONArray().apply { items.forEach { put(encode(it)) } }

    private fun encode(model: GfComposition): JSONObject =
        JSONObject().apply {
            put("schema", 1)
            put("id", model.compositionId)
            put("title", model.title)
            put("instances", JSONArray().apply {
                model.instances.forEach { layer ->
                    put(JSONObject().apply {
                        put("id", layer.instanceId)
                        put("pack", layer.asset.sourcePackId)
                        put("type", layer.asset.type.name)
                        put("visible", layer.visible)
                        put("x", layer.x.toDouble())
                        put("y", layer.y.toDouble())
                        put("scale", layer.scale.toDouble())
                        put("rotation", layer.rotationDegrees.toDouble())
                        put("opacity", layer.opacity.toDouble())
                        put("z", layer.zOrder)
                    })
                }
            })
        }

    private fun decode(json: JSONObject): GfComposition {
        require(json.getInt("schema") == 1) { "Unsupported GF composition version" }
        val objects = json.getJSONArray("instances")
        require(objects.length() <= 64) { "Too many GF layers" }
        val instances = (0 until objects.length()).map { index ->
            val raw = objects.getJSONObject(index)
            GfLayerInstance(
                instanceId = raw.getString("id"),
                asset = GfAssetRef(
                    sourcePackId = raw.getString("pack"),
                    type = GfArtType.valueOf(raw.getString("type")),
                ),
                visible = raw.getBoolean("visible"),
                x = raw.getDouble("x").toFloat(),
                y = raw.getDouble("y").toFloat(),
                scale = raw.getDouble("scale").toFloat(),
                rotationDegrees = raw.getDouble("rotation").toFloat(),
                opacity = raw.getDouble("opacity").toFloat(),
                zOrder = raw.getInt("z"),
            ).sanitized()
        }
        return GfComposition(
            compositionId = json.getString("id"),
            title = json.getString("title"),
            instances = instances,
        )
    }

    companion object {
        private const val PREF_NAME = "faric_gf_compositions_v1"
        private const val KEY = "user_compositions"
    }
}
