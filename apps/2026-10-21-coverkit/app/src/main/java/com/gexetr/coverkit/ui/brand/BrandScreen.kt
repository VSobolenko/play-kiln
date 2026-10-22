package com.gexetr.coverkit.ui.brand

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gexetr.coverkit.R
import com.gexetr.coverkit.data.BrandKitStore
import com.gexetr.coverkit.domain.BrandKit
import com.gexetr.coverkit.ui.components.StudioScaffold
import com.gexetr.coverkit.ui.theme.Cyan
import com.gexetr.coverkit.ui.theme.InkSoft
import com.gexetr.coverkit.ui.theme.OnCyan
import com.gexetr.coverkit.ui.theme.Paper
import kotlinx.coroutines.launch

private val PRESET_SWATCHES = listOf(
    0xFFE63946.toInt(),
    0xFFF4A261.toInt(),
    0xFFE9C46A.toInt(),
    0xFF2A9D8F.toInt(),
    0xFF264653.toInt(),
    0xFF2DE2E6.toInt(),
    0xFF457B9D.toInt(),
    0xFF9B5DE5.toInt(),
    0xFFF15BB5.toInt(),
    0xFFFF006E.toInt(),
    0xFFEEF2F5.toInt(),
    0xFF12161A.toInt(),
)

private data class FontChoice(val id: String, val labelRes: Int)

private val PRIMARY_FONTS = listOf(
    FontChoice("sans", R.string.brand_font_sans),
    FontChoice("bold_sans", R.string.brand_font_bold_sans),
    FontChoice("display", R.string.brand_font_display),
    FontChoice("rounded", R.string.brand_font_rounded),
)

private val SECONDARY_FONTS = listOf(
    FontChoice("serif", R.string.brand_font_serif),
    FontChoice("sans", R.string.brand_font_sans),
    FontChoice("mono", R.string.brand_font_mono),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandScreen(
    brandKitStore: BrandKitStore,
    unlocked: Boolean,
    onBack: () -> Unit,
    onNeedUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kit by brandKitStore.brandKit.collectAsStateWithLifecycle(initialValue = BrandKit())
    val scope = rememberCoroutineScope()
    var selectedColors by remember(kit.colors) { mutableStateOf(kit.colors.take(BrandKit.MAX_COLORS)) }
    var fontPrimary by remember(kit.fontPrimary) { mutableStateOf(kit.fontPrimary) }
    var fontSecondary by remember(kit.fontSecondary) { mutableStateOf(kit.fontSecondary) }

    val pickLogo = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (!unlocked) {
            onNeedUnlock()
            return@rememberLauncherForActivityResult
        }
        scope.launch { brandKitStore.setLogoUri(uri.toString()) }
    }

    fun gate(action: () -> Unit) {
        if (!unlocked) onNeedUnlock() else action()
    }

    StudioScaffold(title = stringResource(R.string.nav_brand), onBack = onBack) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(R.string.brand_hero),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Cyan,
            )
            Text(
                text = stringResource(R.string.brand_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = Paper.copy(alpha = 0.9f),
            )

            Text(
                stringResource(R.string.brand_logo_header),
                style = MaterialTheme.typography.titleMedium,
                color = Paper,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(InkSoft)
                    .border(1.dp, Cyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable {
                        gate {
                            pickLogo.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val logo = kit.logoUri
                if (!logo.isNullOrBlank()) {
                    AsyncImage(
                        model = logo,
                        contentDescription = stringResource(R.string.brand_logo_set),
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(stringResource(R.string.brand_pick_logo), color = Cyan, fontWeight = FontWeight.Medium)
                }
            }
            if (!kit.logoUri.isNullOrBlank()) {
                TextButton(onClick = { gate { scope.launch { brandKitStore.setLogoUri(null) } } }) {
                    Text(stringResource(R.string.brand_clear_logo), color = Paper)
                }
            }

            Text(
                stringResource(R.string.brand_colors_header),
                style = MaterialTheme.typography.titleMedium,
                color = Paper,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.brand_colors_hint, BrandKit.MAX_COLORS),
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.copy(alpha = 0.75f),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PRESET_SWATCHES.forEach { argb ->
                    val selected = selectedColors.any { it == argb }
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .widthIn(min = 48.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Cyan else Paper.copy(alpha = 0.35f),
                                shape = CircleShape,
                            )
                            .clickable {
                                gate {
                                    selectedColors = when {
                                        selected -> selectedColors.filterNot { it == argb }
                                        selectedColors.size >= BrandKit.MAX_COLORS ->
                                            selectedColors.drop(1) + argb
                                        else -> selectedColors + argb
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Text("✓", color = if (argb == 0xFFEEF2F5.toInt()) Color.Black else Color.White)
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.brand_font_primary),
                style = MaterialTheme.typography.titleMedium,
                color = Paper,
                fontWeight = FontWeight.SemiBold,
            )
            FontChipRow(
                choices = PRIMARY_FONTS,
                selectedId = fontPrimary,
                onSelect = { id -> gate { fontPrimary = id } },
            )
            Text(
                stringResource(R.string.brand_font_secondary),
                style = MaterialTheme.typography.titleMedium,
                color = Paper,
                fontWeight = FontWeight.SemiBold,
            )
            FontChipRow(
                choices = SECONDARY_FONTS,
                selectedId = fontSecondary,
                onSelect = { id -> gate { fontSecondary = id } },
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    gate {
                        scope.launch {
                            brandKitStore.setColors(selectedColors)
                            brandKitStore.setFonts(fontPrimary, fontSecondary)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = OnCyan),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.brand_save), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontChipRow(
    choices: List<FontChoice>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        choices.forEach { choice ->
            val selected = choice.id == selectedId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Cyan else InkSoft)
                    .clickable { onSelect(choice.id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    stringResource(choice.labelRes),
                    color = if (selected) OnCyan else Paper,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
