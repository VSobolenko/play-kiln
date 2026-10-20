package com.gexetr.plinthproductphotos.ui.profile

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.components.AuroraField
import com.gexetr.plinthproductphotos.ui.components.PlinthCostChip
import com.gexetr.plinthproductphotos.ui.components.PlinthStage
import com.gexetr.plinthproductphotos.ui.theme.PlinthMotion

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onOpenWallet: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSavedResult: (photoId: String, outputId: String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "profile-identity") {
            ProfileEntrance(index = 0) { ProfileIdentity(state, onOpenWallet, onRefresh) }
        }
        item(key = "profile-stats") {
            ProfileEntrance(index = 1) { ProfileStats(state) }
        }
        item(key = "profile-recent-work") {
            ProfileEntrance(index = 2) {
                ProfileRecentWork(state, onOpenLibrary, onOpenSavedResult, onRefresh)
            }
        }
        item(key = "profile-settings") {
            ProfileEntrance(index = 4) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
                        Column {
                            ProfileSettingsLane(
                                icon = { Icon(painterResource(R.drawable.ic_lucide_folder), contentDescription = null) },
                                title = stringResource(R.string.profile_w76_library_title),
                                supporting = stringResource(R.string.profile_w76_library_body),
                                onClick = onOpenLibrary,
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            ProfileSettingsLane(
                                icon = { Icon(painterResource(R.drawable.ic_lucide_wallet), contentDescription = null) },
                                title = stringResource(R.string.profile_w76_credits_title),
                                supporting = stringResource(state.balanceStatus.statusTextRes()),
                                onClick = onOpenWallet,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEntrance(index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = true, enter = androidx.compose.animation.fadeIn(PlinthMotion.enterStagger(index))) {
        content()
    }
}

@Composable
private fun ProfileIdentity(state: ProfileUiState, onOpenWallet: () -> Unit, onRefresh: () -> Unit) {
    val animatedCredits by animateIntAsState(
        targetValue = state.credits ?: 0,
        animationSpec = tween<Int>(durationMillis = 260, delayMillis = PlinthMotion.EnterStaggerMillis),
        label = "profile-credits",
    )
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 236.dp).clip(MaterialTheme.shapes.extraLarge)) {
        AuroraField(Modifier.fillMaxSize(), intensity = 0.8f)
        PlinthStage(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.profile_w76_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.profile_w76_credits_title),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            PlinthCostChip(label = stringResource(state.balanceStatus.statusTextRes()))
                        }
                        Text(
                            text = state.credits?.let {
                                pluralStringResource(R.plurals.profile_w76_credits_amount, animatedCredits, animatedCredits)
                            } ?: stringResource(R.string.profile_w76_credits_unavailable),
                            style = MaterialTheme.typography.displaySmall,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onOpenWallet, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.profile_w76_open_wallet))
                    }
                    TextButton(onClick = onRefresh, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.profile_w76_refresh_balance))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStats(state: ProfileUiState) {
    val stats = buildList {
        state.credits?.let { add(it to R.string.profile_w76_credits_title) }
        if (state.historyStatus == ProfileHistoryStatus.READY) {
            add(state.projectCount to R.string.profile_w76_library_title)
            add(state.savedResultCount to R.string.profile_w76_history_title)
        }
    }
    if (stats.isEmpty()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.take(3).forEach { (value, label) ->
            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = value.toString(), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRecentWork(
    state: ProfileUiState,
    onOpenLibrary: () -> Unit,
    onOpenSavedResult: (photoId: String, outputId: String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = stringResource(R.string.profile_w76_history_title), style = MaterialTheme.typography.titleLarge)
        when (state.historyStatus) {
            ProfileHistoryStatus.LOADING -> ProfileNotice(
                stringResource(R.string.profile_w76_history_loading_title),
                stringResource(R.string.profile_w76_history_loading),
            )
            ProfileHistoryStatus.ERROR -> ProfileNotice(
                stringResource(R.string.profile_w76_history_error_title),
                stringResource(R.string.profile_w76_history_error_body),
                stringResource(R.string.profile_w76_retry),
                onRefresh,
            )
            ProfileHistoryStatus.READY -> if (state.history.isEmpty()) {
                ProfileNotice(
                    stringResource(R.string.profile_w76_history_empty_title),
                    stringResource(R.string.profile_w76_history_empty_body),
                    stringResource(R.string.profile_w76_open_library),
                    onOpenLibrary,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 8.dp)) {
                    itemsIndexed(state.history, key = { _, item -> item.outputId.ifBlank { "${item.photoId}:${item.imagePath}" } }) { _, result ->
                        ProfileThumbnail(result, onClick = { onOpenSavedResult(result.photoId, result.outputId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileThumbnail(result: ProfileResult, onClick: () -> Unit) {
    val thumbnail by rememberProfileThumbnail(result.imagePath)
    val pressed = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (pressed.collectIsPressedAsState().value) 0.96f else 1f,
        animationSpec = PlinthMotion.pressScale,
        label = "profile-thumbnail-press",
    )
    val description = stringResource(R.string.profile_w76_result_image, result.title)
    Surface(
        modifier = Modifier
            .widthIn(min = 144.dp).heightIn(min = 176.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(MaterialTheme.shapes.large)
            .clickable(interactionSource = pressed, indication = null, onClick = onClick)
            .semantics { contentDescription = description },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
    ) {
        Box {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = profileRelativeDate(result.createdAt),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ProfileNotice(title: String, body: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null && onAction != null) {
                TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
            }
        }
    }
}

@Composable
private fun ProfileSettingsLane(
    icon: @Composable () -> Unit,
    title: String,
    supporting: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (interaction.collectIsPressedAsState().value) 0.985f else 1f,
        animationSpec = PlinthMotion.pressScale,
        label = "profile-lane-press",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(MaterialTheme.shapes.large)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) { Box(contentAlignment = Alignment.Center) { icon() } }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(painterResource(R.drawable.ic_lucide_chevron_right), contentDescription = null)
        }
    }
}

@Composable
private fun profileRelativeDate(createdAt: Long): String {
    val age = (System.currentTimeMillis() - createdAt).coerceAtLeast(0L)
    return when {
        age < DateUtils.MINUTE_IN_MILLIS -> stringResource(R.string.profile_w76_date_now)
        age < DateUtils.HOUR_IN_MILLIS -> {
            val minutes = (age / DateUtils.MINUTE_IN_MILLIS).toInt()
            pluralStringResource(R.plurals.profile_w76_date_minutes_ago, minutes, minutes)
        }
        age < DateUtils.DAY_IN_MILLIS -> {
            val hours = (age / DateUtils.HOUR_IN_MILLIS).toInt()
            pluralStringResource(R.plurals.profile_w76_date_hours_ago, hours, hours)
        }
        age < DateUtils.DAY_IN_MILLIS * 2 -> stringResource(R.string.profile_w76_date_today)
        else -> {
            val days = (age / DateUtils.DAY_IN_MILLIS).toInt()
            pluralStringResource(R.plurals.profile_w76_date_days_ago, days, days)
        }
    }
}

private fun ProfileBalanceStatus.statusTextRes(): Int = when (this) {
    ProfileBalanceStatus.LOADING -> R.string.profile_w76_balance_loading
    ProfileBalanceStatus.CURRENT -> R.string.profile_w76_balance_current
    ProfileBalanceStatus.CACHED -> R.string.profile_w76_balance_cached
    ProfileBalanceStatus.UNAVAILABLE -> R.string.profile_w76_balance_unavailable
    ProfileBalanceStatus.ERROR -> R.string.profile_w76_balance_error
}
