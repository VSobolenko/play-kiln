package com.gexetr.plinthproductphotos.ui.editor

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

fun CompositionSettings.toJsonObject(): JsonObject = buildJsonObject {
    put("background", buildJsonObject {
        put("kind", background.kind.name)
        background.colorArgb?.let { put("colorArgb", it) }
        background.color2Argb?.let { put("color2Argb", it) }
        background.angleDeg?.let { put("angleDeg", it) }
        background.sceneId?.let { put("sceneId", it) }
        background.sceneAssetPath?.let { put("sceneAssetPath", it) }
        background.prompt?.let { put("prompt", it) }
        background.seed?.let { put("seed", it) }
    })
    put("shadow", buildJsonObject {
        put("kind", shadow.kind.name)
        put("opacity", shadow.opacity)
        put("blurPct", shadow.blurPct)
        put("offsetPct", shadow.offsetPct)
    })
    put("marginPct", marginPct)
    put("fillTarget", fillTarget)
    put("itemScale", itemScale)
    put("horizontalPosition", horizontalPosition)
    put("verticalPosition", verticalPosition)
}

fun compositionSettingsFromJson(source: JsonObject?): CompositionSettings? = try {
    source ?: return null
    val background = source.objectValue("background") ?: return null
    val backgroundKind = background.stringValue("kind")
        ?.let { value -> BackgroundSpec.Kind.entries.firstOrNull { it.name == value } }
        ?: return null
    val shadow = source.objectValue("shadow")?.let { value ->
        val kind = value.stringValue("kind")
            ?.let { name -> ShadowSpec.Kind.entries.firstOrNull { it.name == name } }
            ?: return null
        val defaults = ShadowSpec()
        ShadowSpec(
            kind = kind,
            opacity = value.floatValue("opacity") ?: if ("opacity" in value) return null else defaults.opacity,
            blurPct = value.floatValue("blurPct") ?: if ("blurPct" in value) return null else defaults.blurPct,
            offsetPct = value.floatValue("offsetPct") ?: if ("offsetPct" in value) return null else defaults.offsetPct,
        )
    } ?: ShadowSpec()
    val defaultBackground = BackgroundSpec(backgroundKind, null, null, null, null, null, null, null)
    val defaults = CompositionSettings(background = defaultBackground)
    CompositionSettings(
        background = BackgroundSpec(
            kind = backgroundKind,
            colorArgb = background.intValue("colorArgb").orNullUnlessAbsent(background, "colorArgb"),
            color2Argb = background.intValue("color2Argb").orNullUnlessAbsent(background, "color2Argb"),
            angleDeg = background.intValue("angleDeg").orNullUnlessAbsent(background, "angleDeg"),
            sceneId = background.stringValue("sceneId").orNullUnlessAbsent(background, "sceneId"),
            sceneAssetPath = background.stringValue("sceneAssetPath").orNullUnlessAbsent(background, "sceneAssetPath"),
            prompt = background.stringValue("prompt").orNullUnlessAbsent(background, "prompt"),
            seed = background.longValue("seed").orNullUnlessAbsent(background, "seed"),
        ),
        shadow = shadow,
        marginPct = source.floatValue("marginPct") ?: if ("marginPct" in source) return null else defaults.marginPct,
        fillTarget = source.floatValue("fillTarget") ?: if ("fillTarget" in source) return null else defaults.fillTarget,
        itemScale = source.floatValue("itemScale") ?: if ("itemScale" in source) return null else defaults.itemScale,
        horizontalPosition = source.floatValue("horizontalPosition") ?: if ("horizontalPosition" in source) return null else defaults.horizontalPosition,
        verticalPosition = source.floatValue("verticalPosition") ?: if ("verticalPosition" in source) return null else defaults.verticalPosition,
    )
} catch (_: Exception) {
    null
}

private fun JsonObject.objectValue(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.stringValue(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.floatValue(key: String): Float? {
    val value = this[key] ?: return null
    return (value as? JsonPrimitive)?.floatOrNull
}

private fun JsonObject.intValue(key: String): Int? {
    val value = this[key] ?: return null
    return (value as? JsonPrimitive)?.intOrNull
}

private fun JsonObject.longValue(key: String): Long? {
    val value = this[key] ?: return null
    return (value as? JsonPrimitive)?.longOrNull
}

private fun <T> T?.orNullUnlessAbsent(source: JsonObject, key: String): T? {
    if (this == null && key in source) throw IllegalArgumentException("Invalid '$key'")
    return this
}
