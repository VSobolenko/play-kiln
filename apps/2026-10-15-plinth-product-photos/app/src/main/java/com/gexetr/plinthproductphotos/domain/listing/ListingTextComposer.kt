package com.gexetr.plinthproductphotos.domain.listing

import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.JsonPrimitive

data class ListingAttributes(
    val itemName: String = "",
    val brand: String = "",
    val material: String = "",
    val size: String = "",
    val colour: String = "",
    val quantity: String = "",
)

enum class ListingField {
    TITLE,
    SHORT_DESCRIPTION,
}

enum class RequiredListingAttribute {
    ITEM_NAME,
    QUANTITY,
}

sealed interface ListingBuildResult {
    data class Ready(
        val marketplaceId: String,
        val localeTag: String,
        val title: String,
        val shortDescription: String,
        val titleLimit: Int,
        val descriptionLimit: Int,
    ) : ListingBuildResult

    data class MissingRequiredAttributes(
        val attributes: Set<RequiredListingAttribute>,
    ) : ListingBuildResult

    data class LimitUnavailable(
        val field: ListingField,
        val marketplaceId: String,
        val presetProperty: String,
    ) : ListingBuildResult

    data class Overflow(
        val field: ListingField,
        val actualCharacters: Int,
        val limit: Int,
    ) : ListingBuildResult

    data class BelowMinimum(
        val field: ListingField,
        val actualCharacters: Int,
        val minimum: Int,
    ) : ListingBuildResult

    data class UnsupportedLanguage(val localeTag: String) : ListingBuildResult
}

object ListingTextComposer {
    fun compose(
        marketplace: MarketplacePreset,
        localeTag: String,
        attributes: ListingAttributes,
    ): ListingBuildResult {
        val phrases = ListingPhrases.byLocale[localeTag]
            ?: return ListingBuildResult.UnsupportedLanguage(localeTag)

        val itemName = attributes.itemName.trim()
        val quantity = attributes.quantity.trim().toIntOrNull()
        val missing = buildSet {
            if (itemName.isEmpty()) add(RequiredListingAttribute.ITEM_NAME)
            if (quantity == null || quantity <= 0) add(RequiredListingAttribute.QUANTITY)
        }
        if (missing.isNotEmpty()) return ListingBuildResult.MissingRequiredAttributes(missing)
        val quantityValue = requireNotNull(quantity)

        val titleLimit = marketplace.listing.titleMax
            ?: return ListingBuildResult.LimitUnavailable(
                ListingField.TITLE,
                marketplace.id,
                "listing.titleMax",
            )
        val descriptionLimit = marketplace.listing.descriptionMax
            ?: return ListingBuildResult.LimitUnavailable(
                ListingField.SHORT_DESCRIPTION,
                marketplace.id,
                "listing.descriptionMax",
            )

        val brand = attributes.brand.trim()
        val title = itemName + if (brand.isEmpty()) "" else phrases.brandJoiner + brand
        val descriptionLines = buildList {
            add(phrases.detailsHeading)
            addIfPresent(phrases.brandLabel, attributes.brand)
            addIfPresent(phrases.materialLabel, attributes.material)
            addIfPresent(phrases.sizeLabel, attributes.size)
            addIfPresent(phrases.colourLabel, attributes.colour)
            add("${phrases.quantityLabel}: $quantityValue ${phrases.quantityUnit}")
        }
        val description = descriptionLines.joinToString(separator = "\n")

        val titleCharacters = title.characterCount()
        if (titleCharacters > titleLimit) {
            return ListingBuildResult.Overflow(ListingField.TITLE, titleCharacters, titleLimit)
        }
        val descriptionCharacters = description.characterCount()
        val descriptionMinimum =
            (marketplace.listing.raw["descriptionMin"] as? JsonPrimitive)?.intOrNull
        if (descriptionMinimum != null && descriptionCharacters < descriptionMinimum) {
            return ListingBuildResult.BelowMinimum(
                ListingField.SHORT_DESCRIPTION,
                descriptionCharacters,
                descriptionMinimum,
            )
        }
        if (descriptionCharacters > descriptionLimit) {
            return ListingBuildResult.Overflow(
                ListingField.SHORT_DESCRIPTION,
                descriptionCharacters,
                descriptionLimit,
            )
        }

        return ListingBuildResult.Ready(
            marketplaceId = marketplace.id,
            localeTag = localeTag,
            title = title,
            shortDescription = description,
            titleLimit = titleLimit,
            descriptionLimit = descriptionLimit,
        )
    }

    private fun MutableList<String>.addIfPresent(label: String, value: String) {
        value.trim().takeIf(String::isNotEmpty)?.let { add("$label: $it") }
    }

    private fun String.characterCount(): Int = codePointCount(0, length)
}
