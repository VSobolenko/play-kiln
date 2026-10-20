package com.gexetr.plinthproductphotos.domain.look

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import org.json.JSONArray
import org.json.JSONObject

object LookJsonCodec {
    private const val VERSION = 1

    fun encode(looks: List<Look>): String = JSONObject()
        .put("version", VERSION)
        .put("looks", JSONArray().apply { looks.forEach { put(encodeLook(it)) } })
        .toString()

    fun decode(encoded: String): List<Look> {
        val root = JSONObject(encoded)
        require(root.getInt("version") == VERSION) { "Unsupported saved Looks version" }
        val looks = root.getJSONArray("looks")
        return List(looks.length()) { index -> decodeLook(looks.getJSONObject(index)) }
    }

    fun encodeOne(look: Look): String = encode(listOf(look))

    fun decodeOne(encoded: String): Look =
        decode(encoded).singleOrNull() ?: throw IllegalArgumentException("Expected exactly one saved Look")

    private fun encodeLook(look: Look): JSONObject = JSONObject()
        .put("id", look.id)
        .put("name", look.name)
        .put("background", JSONObject()
            .put("kind", look.background.kind.name)
            .putNullable("colorArgb", look.background.colorArgb)
            .putNullable("color2Argb", look.background.color2Argb)
            .putNullable("angleDeg", look.background.angleDeg)
            .putNullable("sceneId", look.background.sceneId)
            .putNullable("sceneAssetPath", look.background.sceneAssetPath)
            .putNullable("prompt", look.background.prompt)
            .putNullable("seed", look.background.seed))
        .put("shadow", JSONObject()
            .put("kind", look.shadow.kind.name)
            .put("opacity", look.shadow.opacity.toDouble())
            .put("blurPct", look.shadow.blurPct.toDouble())
            .put("offsetPct", look.shadow.offsetPct.toDouble()))
        .put("marginPct", look.marginPct.toDouble())
        .put("fillTarget", look.fillTarget.toDouble())
        .putNullable("slideStyleId", look.slideStyleId)
        .put("createdAt", look.createdAt)

    private fun decodeLook(value: JSONObject): Look {
        val background = value.getJSONObject("background")
        val shadow = value.getJSONObject("shadow")
        return Look(
            id = value.getString("id"),
            name = value.getString("name"),
            background = BackgroundSpec(
                kind = BackgroundSpec.Kind.valueOf(background.getString("kind")),
                colorArgb = background.optionalInt("colorArgb"),
                color2Argb = background.optionalInt("color2Argb"),
                angleDeg = background.optionalInt("angleDeg"),
                sceneId = background.optionalString("sceneId"),
                sceneAssetPath = background.optionalString("sceneAssetPath"),
                prompt = background.optionalString("prompt"),
                seed = background.optionalLong("seed"),
            ),
            shadow = ShadowSpec(
                kind = ShadowSpec.Kind.valueOf(shadow.getString("kind")),
                opacity = shadow.getDouble("opacity").toFloat(),
                blurPct = shadow.getDouble("blurPct").toFloat(),
                offsetPct = shadow.getDouble("offsetPct").toFloat(),
            ),
            marginPct = value.getDouble("marginPct").toFloat(),
            fillTarget = value.getDouble("fillTarget").toFloat(),
            slideStyleId = value.optionalString("slideStyleId"),
            createdAt = value.getLong("createdAt"),
        ).also { look ->
            require(look.name.isNotBlank()) { "Saved Look name cannot be blank" }
            require(look.marginPct.isFinite() && look.marginPct >= 0f) { "Saved Look margin is invalid" }
            require(look.fillTarget.isFinite() && look.fillTarget > 0f) { "Saved Look fill is invalid" }
            require(
                look.shadow.opacity.isFinite() && look.shadow.blurPct.isFinite() &&
                    look.shadow.offsetPct.isFinite(),
            ) { "Saved Look shadow is invalid" }
        }
    }

    private fun JSONObject.putNullable(name: String, value: Any?): JSONObject =
        put(name, value ?: JSONObject.NULL)

    private fun JSONObject.optionalString(name: String): String? =
        if (isNull(name)) null else getString(name)

    private fun JSONObject.optionalInt(name: String): Int? =
        if (isNull(name)) null else getInt(name)

    private fun JSONObject.optionalLong(name: String): Long? =
        if (isNull(name)) null else getLong(name)
}
