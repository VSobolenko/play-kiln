package com.gexetr.plinthproductphotos.ui.group

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.db.GroupEntity
import com.gexetr.plinthproductphotos.data.db.ItemEntity
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import java.io.File

@Composable
fun GroupScreen(viewModel: GroupViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val groupedPhotos = remember(state.photos) {
        state.photos.sortedWith(compareBy(ItemEntity::photoOrder, ItemEntity::id)).groupBy(ItemEntity::groupId)
    }
    val ungrouped = remember(groupedPhotos, state.groups) {
        val groupIds = state.groups.map(GroupEntity::id).toSet()
        groupedPhotos.filterKeys { it !in groupIds }.values.flatten()
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "workspace") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.group_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.w30_workspace_summary, state.groups.size, state.photos.size, ungrouped.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = PlinthTheme.accentInk,
                )
                Text(
                    stringResource(R.string.w30_workspace_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "naming") {
            NamingTools(state, viewModel::setPattern, viewModel::setSkuBase, viewModel::regroup, viewModel::createGroup)
        }
        state.error?.let { error ->
            item(key = "error") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Text(
                        stringResource(R.string.group_error, error),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        if (state.photos.isEmpty()) {
            item(key = "empty") {
                Text(stringResource(R.string.group_no_photos), modifier = Modifier.padding(vertical = 12.dp))
            }
        }
        state.groups.forEach { group ->
            val photos = groupedPhotos[group.id].orEmpty()
            item(key = "group:${group.id}", contentType = "group") {
                GroupHeader(
                    group = group,
                    photoCount = photos.size,
                    pattern = state.pattern,
                    onRename = { skuBase -> viewModel.renameGroup(group.id, skuBase) },
                )
            }
            items(photos, key = { "photo:${it.id}" }, contentType = { "photo" }) { photo ->
                WorkspacePhoto(photo, state.groups, viewModel)
            }
        }
        if (ungrouped.isNotEmpty()) {
            item(key = "unassigned") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.w30_ungrouped, ungrouped.size),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.w30_ungrouped_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(ungrouped, key = { "photo:${it.id}" }, contentType = { "photo" }) { photo ->
                WorkspacePhoto(photo, state.groups, viewModel)
            }
        }
    }
}

@Composable
private fun NamingTools(
    state: GroupUiState,
    onPatternChange: (String) -> Unit,
    onSkuBaseChange: (String) -> Unit,
    onRegroup: () -> Unit,
    onCreate: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.w30_naming_title), style = MaterialTheme.typography.titleMedium)
            Text(state.pattern, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.w30_hide_naming else R.string.w30_edit_naming))
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.pattern,
                        onValueChange = onPatternChange,
                        label = { Text(stringResource(R.string.group_pattern)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.skuBase,
                        onValueChange = onSkuBaseChange,
                        label = { Text(stringResource(R.string.w30_sku_base)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
            Text(
                stringResource(R.string.w30_naming_base, state.skuBase),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.w30_regroup_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRegroup,
                enabled = state.photos.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.group_apply_pattern)) }
            OutlinedButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.group_create_group))
            }
        }
    }
}

@Composable
private fun GroupHeader(group: GroupEntity, photoCount: Int, pattern: String, onRename: (String) -> Unit) {
    var skuBase by rememberSaveable(group.id, group.skuBase) { mutableStateOf(group.skuBase) }
    var editing by rememberSaveable(group.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                group.skuPattern.replace("{base}", group.skuBase).replace("{n}", group.ordinal.toString()),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.w30_photo_count, photoCount), style = MaterialTheme.typography.labelLarge)
            if (photoCount == 0) {
                Text(stringResource(R.string.w30_empty_group), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = { editing = !editing }) {
                Text(stringResource(if (editing) R.string.w30_close_rename else R.string.w30_edit_group))
            }
            AnimatedVisibility(visible = editing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = skuBase,
                        onValueChange = { skuBase = it },
                        label = { Text(stringResource(R.string.w30_sku_base)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text(
                        stringResource(R.string.w30_rename_pattern, pattern),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { onRename(skuBase) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.group_rename))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspacePhoto(photo: ItemEntity, groups: List<GroupEntity>, viewModel: GroupViewModel) {
    GroupPhotoRow(
        photo = photo,
        groups = groups,
        onMove = { viewModel.moveItem(photo.id, it) },
        onReferenceChange = { viewModel.setReference(photo.id, it) },
        onSaveCategory = { viewModel.saveCategory(photo.id, it) },
        onConfirmSku = { viewModel.confirmSku(photo.id) },
        onConfirmScale = { viewModel.confirmScale(photo.id) },
    )
}

@Composable
private fun GroupPhotoRow(
    photo: ItemEntity,
    groups: List<GroupEntity>,
    onMove: (String) -> Unit,
    onReferenceChange: (Boolean) -> Unit,
    onSaveCategory: (String) -> Unit,
    onConfirmSku: () -> Unit,
    onConfirmScale: () -> Unit,
) {
    var category by rememberSaveable(photo.id, photo.category) { mutableStateOf(photo.category.orEmpty()) }
    var expanded by rememberSaveable(photo.id) { mutableStateOf(false) }
    var moveMenuOpen by remember(photo.id) { mutableStateOf(false) }
    val filename = photo.sourcePath.substringAfterLast('/').substringAfterLast('\\')
    val role = stringResource(
        when (photo.role) {
            SlotRole.REFERENCE -> R.string.group_role_reference
            SlotRole.EXTRA -> R.string.group_role_extra
            SlotRole.SLIDE -> R.string.group_role_slide
            else -> R.string.group_role_main
        },
    )
    val needsConfirmation = (photo.suggestedSku != null && !photo.skuConfirmed) ||
        (photo.scaleHintJson != null && !photo.scaleHintConfirmed)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProductThumbnail(photo, filename)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(photo.sku ?: filename, style = MaterialTheme.typography.titleMedium)
                    if (photo.sku != null) {
                        Text(
                            filename,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(stringResource(R.string.group_photo_role, role), style = MaterialTheme.typography.labelLarge)
                    photo.category?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (needsConfirmation) {
                Text(
                    stringResource(R.string.group_suggestion_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = PlinthTheme.accentInk,
                )
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.w30_hide_details else R.string.w30_photo_details))
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(stringResource(R.string.group_category)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    TextButton(onClick = { onSaveCategory(category) }) {
                        Text(stringResource(R.string.w30_save_category))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(
                                value = photo.role == SlotRole.REFERENCE,
                                role = Role.Checkbox,
                                onValueChange = onReferenceChange,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = photo.role == SlotRole.REFERENCE, onCheckedChange = null)
                        Text(stringResource(R.string.group_reference), modifier = Modifier.weight(1f))
                    }
                    Box {
                        OutlinedButton(
                            onClick = { moveMenuOpen = true },
                            enabled = groups.any { it.id != photo.groupId },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.group_move_to)) }
                        DropdownMenu(expanded = moveMenuOpen, onDismissRequest = { moveMenuOpen = false }) {
                            groups.filterNot { it.id == photo.groupId }.forEach { group ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            group.skuPattern.replace("{base}", group.skuBase)
                                                .replace("{n}", group.ordinal.toString()),
                                        )
                                    },
                                    onClick = {
                                        moveMenuOpen = false
                                        onMove(group.id)
                                    },
                                )
                            }
                        }
                    }
                    if (groups.none { it.id != photo.groupId }) {
                        Text(
                            stringResource(R.string.w30_no_move_target),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (needsConfirmation) {
                        Text(stringResource(R.string.group_suggestion_unverified), style = MaterialTheme.typography.bodySmall)
                    }
                    if (photo.suggestedSku != null && !photo.skuConfirmed) {
                        Text(stringResource(R.string.group_sku_suggestion, photo.suggestedSku))
                        Button(onClick = onConfirmSku, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.group_confirm_sku))
                        }
                    }
                    if (photo.scaleHintJson != null && !photo.scaleHintConfirmed) {
                        Text(stringResource(R.string.group_scale_suggestion, photo.scaleHintJson))
                        Button(onClick = onConfirmScale, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.group_confirm_scale))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductThumbnail(photo: ItemEntity, filename: String) {
    val context = LocalContext.current
    val file = remember(photo.sourcePath, context) {
        File(photo.sourcePath).let { if (it.isAbsolute) it else File(context.filesDir, photo.sourcePath) }
    }
    SubcomposeAsyncImage(
        model = file,
        contentDescription = stringResource(R.string.w30_photo_description, filename),
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(88.dp).clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        },
        error = {
            Box(Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.w30_preview_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
