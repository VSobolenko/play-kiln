package com.gexetr.plinthproductphotos.domain.presets

import java.net.URI
import java.net.URISyntaxException
import java.time.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

object MarketplacePresetParser {
    private val supportedLocales = setOf("en", "ru", "de", "fr", "es", "pt-BR")
    private val bcp47 = Regex("[a-zA-Z]{2,8}(?:-[a-zA-Z0-9]{1,8})*")

    fun parseDocument(source: String): PresetParseResult = try {
        val root = Json.parseToJsonElement(source).asObject("\$")
        val marketplaces = root.requiredArray("marketplaces").mapIndexed { index, element ->
            parsePreset(element.asObject("$.marketplaces[$index]"), "$.marketplaces[$index]")
        }
        val unreadStubs = root.requiredArray("unreadStubs").mapIndexed { index, element ->
            parseUnreadStub(element.asObject("$.unreadStubs[$index]"), "$.unreadStubs[$index]")
        }
        val ids = marketplaces.map { it.id }
        if (ids.distinct().size != ids.size) invalid("$.marketplaces", "marketplace ids must be unique")
        val unreadIds = unreadStubs.map { it.id }
        if (unreadIds.distinct().size != unreadIds.size) invalid("$.unreadStubs", "unread ids must be unique")
        if ((ids.toSet() intersect unreadIds.toSet()).isNotEmpty()) {
            invalid("$.unreadStubs", "an id cannot be both a shown preset and an unread stub")
        }
        val localeDefaults = parseLocaleDefaults(root.requiredObject("localeDefaults"), ids.toSet(), "$.localeDefaults")
        val schemaVersion = root.requiredInt("schemaVersion")
        if (schemaVersion != 1) invalid("$.schemaVersion", "unsupported schema version $schemaVersion")
        val textCategories = root.requiredStringSet("textCategories")
        if (textCategories.isEmpty()) invalid("$.textCategories", "must not be empty")
        PresetParseResult.Success(
            MarketplaceDocument(
                schemaVersion = schemaVersion,
                seededAt = root.optionalString("seededAt", "$"),
                textCategories = textCategories,
                localeDefaults = localeDefaults,
                marketplaces = marketplaces,
                unreadStubs = unreadStubs,
            ),
        )
    } catch (input: InvalidPresetInput) {
        PresetParseResult.Invalid(input.error)
    } catch (syntax: SerializationException) {
        PresetParseResult.Invalid(PresetError("$", "invalid JSON: ${syntax.message ?: "syntax error"}"))
    }

    private fun parsePreset(value: JsonObject, path: String): MarketplacePreset {
        val id = value.requiredString("id", path)
        val name = value.requiredString("name", path)
        val regions = value.requiredStringList("regions", path)
        val defaultLocale = value.requiredString("defaultLocale", path)
        if (!bcp47.matches(defaultLocale)) invalid("$path.defaultLocale", "must be a BCP-47 language tag")
        val queue = value.requiredInt("queue", path)
        if (queue !in 1..2) invalid("$path.queue", "must be 1 or 2")
        val editPolicy = parseEditPolicy(value.requiredString("editPolicy", path), "$path.editPolicy")
        val status = parseStatus(value.requiredString("status", path), "$path.status")
        if (status == PresetStatus.UNREAD) invalid("$path.status", "unread entries belong in unreadStubs")
        val verifiedAt = value.requiredString("verifiedAt", path)
        try {
            LocalDate.parse(verifiedAt)
        } catch (_: java.time.format.DateTimeParseException) {
            invalid("$path.verifiedAt", "must be an ISO date")
        }
        val sources = value.requiredArray("sources", path).mapIndexed { index, source ->
            parseSource(source.asObject("$path.sources[$index]"), "$path.sources[$index]")
        }
        if (sources.isEmpty()) invalid("$path.sources", "at least one source is required")
        val allowedOverlays = value.optionalStringSet("allowedOverlays", path)
        if (editPolicy == EditPolicy.LIMITED_OVERLAY && allowedOverlays.isNullOrEmpty()) {
            invalid("$path.allowedOverlays", "limited-overlay presets require a non-empty allow-list")
        }
        return MarketplacePreset(
            id = id,
            name = name,
            regions = regions,
            defaultLocale = defaultLocale,
            queue = queue,
            editPolicy = editPolicy,
            allowedOverlays = allowedOverlays,
            status = status,
            verifiedAt = verifiedAt,
            sourceUrl = sources.first().url,
            sources = sources,
            image = parseImage(value.requiredObject("image", path), "$path.image"),
            main = parseMain(value.requiredObject("main", path), "$path.main"),
            text = parseText(value.requiredObject("text", path), "$path.text"),
            listing = parseListing(value.requiredObject("listing", path), "$path.listing"),
            notes = value.requiredStringList("notes", path),
            raw = value,
        )
    }

    private fun parseSource(value: JsonObject, path: String): PresetSource {
        val url = value.requiredString("url", path)
        val uri = try {
            URI(url)
        } catch (_: URISyntaxException) {
            invalid("$path.url", "must be an absolute HTTPS page URL")
        }
        if (!uri.isAbsolute || !uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
            invalid("$path.url", "must be an absolute HTTPS page URL")
        }
        return PresetSource(
            title = value.requiredString("title", path),
            url = url,
            site = value.requiredString("site", path),
            printedDate = value.requiredString("printedDate", path),
            label = value.requiredString("label", path),
            firstParty = value.requiredBoolean("firstParty", path),
            quotes = value.requiredString("quotes", path),
        )
    }

    private fun parseImage(value: JsonObject, path: String): ImageRules {
        val alternative = value.optionalObject("minHeightAlternative", path)?.let {
            AlternativeLimit(
                value = it.requiredInt("value", "$path.minHeightAlternative"),
                note = it.requiredString("note", "$path.minHeightAlternative"),
            )
        }
        val recommended = value.optionalObject("recommended", path)?.let {
            RecommendedDimensions(
                width = it.requiredInt("w", "$path.recommended"),
                height = it.requiredInt("h", "$path.recommended"),
                basis = it.requiredString("basis", "$path.recommended"),
            )
        }
        val aspect = value.optionalString("aspect", path)
        if (aspect != null) {
            val parts = aspect.split(':')
            val width = parts.getOrNull(0)?.toDoubleOrNull()
            val height = parts.getOrNull(1)?.toDoubleOrNull()
            if (parts.size != 2 || width == null || height == null || width <= 0.0 || height <= 0.0) {
                invalid("$path.aspect", "must be a positive width:height ratio")
            }
        }
        return ImageRules(
            formats = value.requiredStringSet("formats", path),
            maxBytes = value.optionalLong("maxBytes", path),
            minWidth = value.optionalInt("minWidth", path),
            minHeight = value.optionalInt("minHeight", path),
            minHeightAlternative = alternative,
            minLongSide = value.optionalInt("minLongSide", path),
            maxLongSide = value.optionalInt("maxLongSide", path),
            maxSide = value.optionalInt("maxSide", path),
            maxSidesSum = value.optionalInt("maxSidesSum", path),
            maxCount = value.optionalInt("maxCount", path),
            aspect = aspect,
            recommended = recommended,
            minJpegQuality = value.optionalInt("minJpegQuality", path),
            noTransparentPng = value.optionalBoolean("noTransparentPng", path),
            raw = value,
        )
    }

    private fun parseMain(value: JsonObject, path: String) = MainRules(
        backgroundPolicy = value.optionalString("backgroundPolicy", path),
        minFillRatio = value.optionalDouble("minFillRatio", path),
        textOnMain = value.optionalString("textOnMain", path),
        originalOnlyAllowed = value.optionalStringSet("originalOnlyAllowed", path).orEmpty(),
        raw = value,
    )

    private fun parseText(value: JsonObject, path: String): TextRules {
        val contrastElement = value["contrastMin"]
        val contrast = when (contrastElement) {
            null, JsonNull -> null
            is JsonObject -> ContrastThresholds(
                small = contrastElement.requiredDouble("small", "$path.contrastMin"),
                large = contrastElement.requiredDouble("large", "$path.contrastMin"),
            )
            else -> invalid("$path.contrastMin", "must be null or an object with small and large limits")
        }
        return TextRules(
            prohibited = value.requiredStringSet("prohibited", path),
            contrastMin = contrast,
            avoidColourPairs = value.optionalStringList("avoidColourPairs", path).orEmpty(),
            raw = value,
        )
    }

    private fun parseListing(value: JsonObject, path: String) = ListingRules(
        titleMax = value.optionalInt("titleMax", path),
        descriptionMax = value.optionalInt("descriptionMax", path),
        raw = value,
    )

    private fun parseUnreadStub(value: JsonObject, path: String): UnreadMarketplaceStub {
        val status = parseStatus(value.requiredString("status", path), "$path.status")
        if (status != PresetStatus.UNREAD) invalid("$path.status", "unread stub status must be unread")
        val queue = value.requiredInt("queue", path)
        if (queue !in 1..2) invalid("$path.queue", "must be 1 or 2")
        return UnreadMarketplaceStub(value.requiredString("id", path), queue)
    }

    private fun parseLocaleDefaults(
        value: JsonObject,
        shownIds: Set<String>,
        path: String,
    ): Map<String, List<String>> {
        if (value.keys != supportedLocales) {
            invalid(path, "must declare exactly the six supported locales: ${supportedLocales.joinToString()}")
        }
        return supportedLocales.associateWith { locale ->
            val ids = value.requiredStringList(locale, path)
            if (ids.distinct().size != ids.size) invalid("$path.$locale", "marketplace ids must not repeat")
            val unknown = ids.filterNot(shownIds::contains)
            if (unknown.isNotEmpty()) {
                invalid("$path.$locale", "references ids that are not shown presets: ${unknown.joinToString()}")
            }
            ids
        }
    }

    private fun parseEditPolicy(value: String, path: String): EditPolicy = EditPolicy.entries
        .firstOrNull { it.wireValue == value }
        ?: invalid(path, "unsupported edit policy '$value'")

    private fun parseStatus(value: String, path: String): PresetStatus = PresetStatus.entries
        .firstOrNull { it.wireValue == value }
        ?: invalid(path, "unsupported preset status '$value'")

    private fun JsonElement.asObject(path: String): JsonObject = this as? JsonObject
        ?: invalid(path, "must be an object")

    private fun JsonObject.requiredObject(name: String, parent: String = "\$"): JsonObject =
        requiredElement(name, parent).asObject("$parent.$name")

    private fun JsonObject.optionalObject(name: String, parent: String): JsonObject? =
        optionalElement(name)?.let { it.asObject("$parent.$name") }

    private fun JsonObject.requiredArray(name: String, parent: String = "\$"): JsonArray =
        requiredElement(name, parent) as? JsonArray ?: invalid("$parent.$name", "must be an array")

    private fun JsonObject.requiredString(name: String, parent: String = "\$"): String =
        requiredElement(name, parent).stringValue("$parent.$name")

    private fun JsonObject.requiredInt(name: String, parent: String = "\$"): Int =
        requiredElement(name, parent).intValue("$parent.$name")

    private fun JsonObject.requiredDouble(name: String, parent: String): Double =
        requiredElement(name, parent).doubleValue("$parent.$name")

    private fun JsonObject.requiredBoolean(name: String, parent: String): Boolean =
        requiredElement(name, parent).booleanValue("$parent.$name")

    private fun JsonObject.requiredStringList(name: String, parent: String = "\$"): List<String> =
        requiredArray(name, parent).mapIndexed { index, value -> value.stringValue("$parent.$name[$index]") }

    private fun JsonObject.requiredStringSet(name: String, parent: String = "\$"): Set<String> {
        val values = requiredStringList(name, parent)
        if (values.distinct().size != values.size) invalid("$parent.$name", "values must not repeat")
        return values.toSet()
    }

    private fun JsonObject.optionalStringList(name: String, parent: String): List<String>? =
        optionalElement(name)?.let { element ->
            val array = element as? JsonArray ?: invalid("$parent.$name", "must be an array")
            array.mapIndexed { index, value -> value.stringValue("$parent.$name[$index]") }
        }

    private fun JsonObject.optionalStringSet(name: String, parent: String): Set<String>? =
        optionalStringList(name, parent)?.let { values ->
            if (values.distinct().size != values.size) invalid("$parent.$name", "values must not repeat")
            values.toSet()
        }

    private fun JsonObject.optionalString(name: String, parent: String): String? =
        optionalElement(name)?.let { it.stringValue("$parent.$name") }

    private fun JsonObject.optionalInt(name: String, parent: String): Int? =
        optionalElement(name)?.intValue("$parent.$name")

    private fun JsonObject.optionalLong(name: String, parent: String): Long? =
        optionalElement(name)?.longValue("$parent.$name")

    private fun JsonObject.optionalDouble(name: String, parent: String): Double? =
        optionalElement(name)?.doubleValue("$parent.$name")

    private fun JsonObject.optionalBoolean(name: String, parent: String): Boolean? =
        optionalElement(name)?.booleanValue("$parent.$name")

    private fun JsonObject.requiredElement(name: String, parent: String): JsonElement =
        this[name] ?: invalid("$parent.$name", "is required")

    private fun JsonObject.optionalElement(name: String): JsonElement? =
        this[name]?.takeUnless { it == JsonNull }

    private fun JsonElement.stringValue(path: String): String {
        val primitive = this as? JsonPrimitive
        if (primitive == null || !primitive.isString) invalid(path, "must be a string")
        return primitive.content
    }

    private fun JsonElement.intValue(path: String): Int {
        val primitive = this as? JsonPrimitive
        if (primitive == null || primitive.isString || primitive.intOrNull == null) {
            invalid(path, "must be an integer")
        }
        return primitive.intOrNull ?: invalid(path, "must be an integer")
    }

    private fun JsonElement.longValue(path: String): Long {
        val primitive = this as? JsonPrimitive
        if (primitive == null || primitive.isString || primitive.longOrNull == null) {
            invalid(path, "must be an integer")
        }
        return primitive.longOrNull ?: invalid(path, "must be an integer")
    }

    private fun JsonElement.doubleValue(path: String): Double {
        val primitive = this as? JsonPrimitive
        val value = if (primitive == null || primitive.isString) null else primitive.doubleOrNull
        if (value == null || !value.isFinite()) invalid(path, "must be a finite number")
        return value
    }

    private fun JsonElement.booleanValue(path: String): Boolean {
        val primitive = this as? JsonPrimitive
        if (primitive == null || primitive.isString || primitive.booleanOrNull == null) {
            invalid(path, "must be a boolean")
        }
        return primitive.booleanOrNull ?: invalid(path, "must be a boolean")
    }

    private fun invalid(path: String, message: String): Nothing =
        throw InvalidPresetInput(PresetError(path, message))

    private class InvalidPresetInput(val error: PresetError) : RuntimeException(error.message)
}
