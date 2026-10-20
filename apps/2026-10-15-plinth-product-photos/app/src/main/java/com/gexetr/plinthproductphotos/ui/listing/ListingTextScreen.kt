package com.gexetr.plinthproductphotos.ui.listing

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.domain.listing.ListingAttributes
import com.gexetr.plinthproductphotos.domain.listing.ListingBuildResult
import com.gexetr.plinthproductphotos.domain.listing.ListingField
import com.gexetr.plinthproductphotos.domain.listing.ListingPhrases
import com.gexetr.plinthproductphotos.domain.listing.ListingTextComposer
import com.gexetr.plinthproductphotos.domain.listing.RequiredListingAttribute
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingTextScreen(
    marketplaces: List<MarketplacePreset>,
    selectedMarketplaceId: String?,
    selectedLocale: String,
    attributes: ListingAttributes,
    attributesConfirmed: Boolean,
    isOffline: Boolean,
    onBack: () -> Unit,
    onMarketplaceSelected: (String) -> Unit,
    onLocaleSelected: (String) -> Unit,
    onAttributesChanged: (ListingAttributes) -> Unit,
    onAttributesConfirmedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val marketplace = marketplaces.firstOrNull { it.id == selectedMarketplaceId }
    val result = marketplace?.let {
        ListingTextComposer.compose(it, selectedLocale, attributes)
    }
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.listing_share_chooser)
    val clipboard = LocalClipboardManager.current
    var referencePhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) referencePhoto = uri.toString()
    }
    fun changeAttributes(updated: ListingAttributes) {
        onAttributesChanged(updated)
        onAttributesConfirmedChange(false)
    }

    PlinthScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.listing_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.listing_back))
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.listing_workspace_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.listing_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isOffline) {
                Text(
                    stringResource(R.string.listing_offline),
                    color = PlinthTheme.accentInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text(stringResource(R.string.listing_marketplace_label), style = MaterialTheme.typography.titleSmall)
            if (marketplaces.isEmpty()) {
                Text(stringResource(R.string.listing_no_marketplaces))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(marketplaces, key = MarketplacePreset::id) { item ->
                        FilterChip(
                            selected = item.id == selectedMarketplaceId,
                            onClick = {
                                onMarketplaceSelected(item.id)
                                onAttributesConfirmedChange(false)
                            },
                            label = { Text(item.name) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }

            Text(stringResource(R.string.listing_language_label), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListingPhrases.byLocale.forEach { (locale, phrases) ->
                    FilterChip(
                        selected = locale == selectedLocale,
                        onClick = {
                            onLocaleSelected(locale)
                            onAttributesConfirmedChange(false)
                        },
                        label = { Text(phrases.languageName) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            val editor: @Composable () -> Unit = {
                ListingDetailsEditor(attributes, ::changeAttributes)
            }
            val preview: @Composable () -> Unit = {
                ListingPreview(
                    marketplace = marketplace,
                    result = result,
                    attributes = attributes,
                    attributesConfirmed = attributesConfirmed,
                    referencePhoto = referencePhoto,
                    onChoosePhoto = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemovePhoto = { referencePhoto = null },
                    onConfirm = { onAttributesConfirmedChange(true) },
                    onCopy = { ready ->
                        clipboard.setText(AnnotatedString(ready.title + "\n\n" + ready.shortDescription))
                        Toast.makeText(context, R.string.listing_copied, Toast.LENGTH_SHORT).show()
                    },
                    onShare = { ready ->
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, ready.title)
                            putExtra(Intent.EXTRA_TEXT, ready.title + "\n\n" + ready.shortDescription)
                        }
                        context.startActivity(Intent.createChooser(send, shareChooserTitle))
                    },
                )
            }
            val fontScale = LocalDensity.current.fontScale
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 720.dp * fontScale) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(1f)) { editor() }
                        Column(Modifier.weight(1f)) { preview() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        preview()
                        editor()
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingDetailsEditor(
    attributes: ListingAttributes,
    onChange: (ListingAttributes) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.listing_attributes_label),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        OutlinedTextField(
            value = attributes.itemName,
            onValueChange = { onChange(attributes.copy(itemName = it)) },
            label = { Text(stringResource(R.string.listing_item_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = attributes.brand,
            onValueChange = { onChange(attributes.copy(brand = it)) },
            label = { Text(stringResource(R.string.listing_brand)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = attributes.material,
            onValueChange = { onChange(attributes.copy(material = it)) },
            label = { Text(stringResource(R.string.listing_material)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = attributes.size,
            onValueChange = { onChange(attributes.copy(size = it)) },
            label = { Text(stringResource(R.string.listing_size)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = attributes.colour,
            onValueChange = { onChange(attributes.copy(colour = it)) },
            label = { Text(stringResource(R.string.listing_colour)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = attributes.quantity,
            onValueChange = { onChange(attributes.copy(quantity = it)) },
            label = { Text(stringResource(R.string.listing_quantity)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Composable
private fun ListingPreview(
    marketplace: MarketplacePreset?,
    result: ListingBuildResult?,
    attributes: ListingAttributes,
    attributesConfirmed: Boolean,
    referencePhoto: String?,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onConfirm: () -> Unit,
    onCopy: (ListingBuildResult.Ready) -> Unit,
    onShare: (ListingBuildResult.Ready) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.listing_preview_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                marketplace?.name ?: stringResource(R.string.listing_marketplace_label),
                style = MaterialTheme.typography.labelLarge,
                color = PlinthTheme.accentInk,
            )
            if (referencePhoto != null) {
                key(referencePhoto) {
                    var photoFailed by remember { mutableStateOf(false) }
                    AsyncImage(
                        model = referencePhoto,
                        contentDescription = stringResource(R.string.listing_photo_description),
                        contentScale = ContentScale.Fit,
                        onError = { photoFailed = true },
                        onSuccess = { photoFailed = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.large),
                    )
                    if (photoFailed) {
                        Text(
                            stringResource(R.string.listing_photo_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            attributes.itemName.trim().ifEmpty { stringResource(R.string.listing_product_placeholder) },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            stringResource(R.string.listing_photo_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onChoosePhoto, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(
                        if (referencePhoto == null) R.string.listing_photo_choose else R.string.listing_photo_replace,
                    ))
                }
                if (referencePhoto != null) {
                    TextButton(onClick = onRemovePhoto, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.listing_photo_remove))
                    }
                }
            }
            Text(
                stringResource(R.string.listing_photo_preview_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (result is ListingBuildResult.Ready) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(
                        R.string.listing_character_count,
                        stringResource(R.string.listing_field_title),
                        result.title.codePointCount(0, result.title.length),
                        result.titleLimit,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.listing_field_description),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(result.shortDescription, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(
                        R.string.listing_character_count,
                        stringResource(R.string.listing_field_description),
                        result.shortDescription.codePointCount(0, result.shortDescription.length),
                        result.descriptionLimit,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ListingEditorialFeedback(result, marketplace, attributesConfirmed)
            if (result is ListingBuildResult.Ready) {
                if (attributesConfirmed) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = { onCopy(result) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.listing_copy))
                        }
                        OutlinedButton(onClick = { onShare(result) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.listing_share))
                        }
                    }
                } else {
                    Button(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.listing_confirm_attributes))
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingEditorialFeedback(
    result: ListingBuildResult?,
    marketplace: MarketplacePreset?,
    attributesConfirmed: Boolean,
) {
    val hasError = result != null && result !is ListingBuildResult.Ready
    Surface(
        color = if (hasError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (hasError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(when {
                    hasError -> R.string.listing_feedback_attention
                    result == null -> R.string.listing_feedback_setup
                    attributesConfirmed -> R.string.listing_feedback_confirmed
                    else -> R.string.listing_feedback_review
                }),
                style = MaterialTheme.typography.labelLarge,
            )
            when (result) {
                null -> Text(stringResource(R.string.listing_choose_marketplace))
                is ListingBuildResult.MissingRequiredAttributes -> {
                    if (RequiredListingAttribute.ITEM_NAME in result.attributes) {
                        Text(stringResource(R.string.listing_missing_item_name))
                    }
                    if (RequiredListingAttribute.QUANTITY in result.attributes) {
                        Text(stringResource(R.string.listing_missing_quantity))
                    }
                }
                is ListingBuildResult.LimitUnavailable -> Text(stringResource(
                    R.string.listing_limit_unavailable,
                    stringResource(fieldLabel(result.field)),
                    requireNotNull(marketplace).name,
                    result.presetProperty,
                ))
                is ListingBuildResult.BelowMinimum -> Text(stringResource(
                    R.string.listing_below_minimum,
                    stringResource(fieldLabel(result.field)),
                    result.actualCharacters,
                    result.minimum,
                ))
                is ListingBuildResult.Overflow -> Text(stringResource(
                    R.string.listing_overflow,
                    stringResource(fieldLabel(result.field)),
                    result.actualCharacters,
                    result.limit,
                ))
                is ListingBuildResult.UnsupportedLanguage ->
                    Text(stringResource(R.string.listing_unsupported_language, result.localeTag))
                is ListingBuildResult.Ready -> Text(stringResource(
                    if (attributesConfirmed) R.string.listing_feedback_ready else R.string.listing_feedback_draft,
                ))
            }
        }
    }
}

private fun fieldLabel(field: ListingField): Int = when (field) {
    ListingField.TITLE -> R.string.listing_field_title
    ListingField.SHORT_DESCRIPTION -> R.string.listing_field_description
}
