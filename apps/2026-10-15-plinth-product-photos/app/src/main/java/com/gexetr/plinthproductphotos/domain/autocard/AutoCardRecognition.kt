package com.gexetr.plinthproductphotos.domain.autocard

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull

enum class AutoCardScaleKind(val wireValue: String) {
    NONE("none"),
    RULER("ruler"),
    COIN("coin"),
    HAND("hand"),
    LABEL("label"),
    ;

    companion object {
        fun fromWireValue(value: String): AutoCardScaleKind? =
            entries.firstOrNull { it.wireValue == value }
    }
}

data class AutoCardRecognition(
    val category: AutoCategory,
    val confidence: Float,
    val attributes: List<String>,
    val visibleText: List<String>,
    val scaleKind: AutoCardScaleKind,
    val scaleValueMm: Double?,
    val sku: String?,
) {
    companion object {
        fun fallback(): AutoCardRecognition = AutoCardRecognition(
            category = AutoCategory.OTHER,
            confidence = 0f,
            attributes = emptyList(),
            visibleText = emptyList(),
            scaleKind = AutoCardScaleKind.NONE,
            scaleValueMm = null,
            sku = null,
        )
    }
}

fun interface AutoCardRecognitionTransport {
    suspend fun recognize(imageJpeg: ByteArray): String
}

class AutoCardRecognitionClient(
    private val transport: AutoCardRecognitionTransport,
) {
    suspend fun recognize(imageJpeg: ByteArray): AutoCardRecognition {
        require(imageJpeg.size >= 2 && imageJpeg[0] == 0xff.toByte() && imageJpeg[1] == 0xd8.toByte()) {
            "Auto-card recognition requires a JPEG image"
        }
        return parse(transport.recognize(imageJpeg)) ?: AutoCardRecognition.fallback()
    }

    private fun parse(response: String): AutoCardRecognition? {
        val root = try {
            Json.parseToJsonElement(response) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: return null
        val categoryId = root.string("category") ?: return null
        val category = AutoCategory.entries.firstOrNull { it.id == categoryId } ?: return null
        val confidence = root.number("confidence")
            ?.takeIf { it.isFinite() && it in 0.0..1.0 }
            ?.toFloat()
            ?: return null
        val attributes = root.stringArray("attributes", maximum = 8) ?: return null
        val visibleText = root.stringArray("visibleText", maximum = 6) ?: return null
        val scaleHint = root["scaleHint"] as? JsonObject ?: return null
        val scaleKind = scaleHint.string("kind")
            ?.let(AutoCardScaleKind::fromWireValue)
            ?: return null
        val scaleValueMm = scaleHint.number("valueMm")
        if (scaleValueMm != null && (!scaleValueMm.isFinite() || scaleValueMm <= 0.0)) return null
        val sku = when (val skuValue = root["sku"]) {
            null -> null
            is JsonPrimitive -> if (skuValue.isString) skuValue.contentOrNull ?: return null else return null
            else -> return null
        }
        return AutoCardRecognition(
            category = category,
            confidence = confidence,
            attributes = attributes,
            visibleText = visibleText,
            scaleKind = scaleKind,
            scaleValueMm = scaleValueMm,
            sku = sku,
        )
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.contentOrNull

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.doubleOrNull

    private fun JsonObject.stringArray(key: String, maximum: Int): List<String>? {
        val array = this[key] as? JsonArray ?: return null
        if (array.size > maximum) return null
        return array.map { value ->
            (value as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.contentOrNull
                ?: return null
        }
    }
}
