package com.gexetr.unitwalk.ui.walk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.unitwalk.R
import com.gexetr.unitwalk.data.WalkStore
import com.gexetr.unitwalk.domain.PhotoSource
import com.gexetr.unitwalk.domain.RoomCondition
import com.gexetr.unitwalk.domain.WalkKind
import com.gexetr.unitwalk.domain.WalkStatus
import com.gexetr.unitwalk.ui.components.EmptyState
import com.gexetr.unitwalk.ui.components.HeroPanel
import com.gexetr.unitwalk.ui.components.PhotoThumbRow
import com.gexetr.unitwalk.ui.components.PillKind
import com.gexetr.unitwalk.ui.components.StatusPill
import com.gexetr.unitwalk.ui.theme.UnitWalkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The room-by-room walk — the surface the whole product exists for.
 *
 * The screen is split the way `ui/compare/CompareScreen.kt` is split: [WalkScreen] / [WalkRoute]
 * build the ViewModel, own the two device capabilities, and collect the
 * state, while [WalkContent] is a pure function of [WalkUiState] that emits [WalkEvent]s. Keeping the
 * `ActivityResult` launchers in the screen is deliberate: it is what lets [WalkContent] be previewed
 * and reasoned about without an activity registry, and the stateless half stays a function of state.
 *
 * ## The two capabilities the plan sells
 *
 *  - **Camera capture** through `ActivityResultContracts.TakePicture`. The destination URI must exist
 *    before the camera starts, so [PhotoUris.newPhotoFileName] allocates the name and
 *    [PhotoUris.uriFor] over [PhotoUris.captureFile] hands the camera a `content://` URI under the
 *    declared `reports/` root. After the capture the bytes are read back and dispatched as
 *    [WalkEvent.AttachPhoto] with [PhotoSource.CAMERA], so the bytes reach the store **before** the row
 *    that names them — the ordering [WalkViewModel] documents. The `CAMERA` permission is the app's
 *    only runtime permission and is requested on first use; a device without a camera still imports.
 *  - **Gallery backfill** through `ActivityResultContracts.PickVisualMedia`. The Photo Picker needs
 *    **no** runtime permission, which is why `READ_MEDIA_IMAGES` is deliberately not declared, and the
 *    bytes are dispatched as [WalkEvent.AttachPhoto] with [PhotoSource.PICKER] the same way.
 *
 * ## The ordering the product is built on
 *
 * Every room, note, condition and photo is committed by the brain before the next room opens, so a
 * process killed mid-walk loses at most the step in flight. This screen never batches: each event is
 * dispatched as the user produces it, and the finished room is followed by the room the brain says is
 * current rather than by one the screen computed.
 */
@Composable
fun WalkScreen(
    walkId: String,
    onBack: () -> Unit,
    onFinished: (walkId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: WalkViewModel = viewModel(factory = WalkViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(walkId) { viewModel.onEvent(WalkEvent.RequestWalk(walkId)) }

    // The camera's destination name. Allocated before the launch and reused as the committed row's
    // name, so the file the camera wrote and the row that names it cannot disagree.
    var pendingCaptureName by remember { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val fileName = pendingCaptureName
        pendingCaptureName = null
        val roomId = state.currentRoomId
        if (success && fileName != null && roomId != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    val staging = PhotoUris.captureFile(context, fileName)
                    val read = runCatching { staging.readBytes() }.getOrNull()
                    // The bytes are in memory and the store writes its own durable copy, so the
                    // staging file under `reports/` is disposable rather than a second home.
                    if (read != null && read.isNotEmpty()) runCatching { staging.delete() }
                    read
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    viewModel.onEvent(
                        WalkEvent.AttachPhoto(
                            roomId = roomId,
                            fileName = fileName,
                            bytes = bytes,
                            source = PhotoSource.CAMERA,
                        ),
                    )
                }
            }
        }
    }

    val capturePhoto: () -> Unit = {
        val fileName = PhotoUris.newPhotoFileName()
        val uri = runCatching {
            PhotoUris.uriFor(context, PhotoUris.captureFile(context, fileName))
        }.getOrNull()
        if (uri != null) {
            pendingCaptureName = fileName
            takePicture.launch(uri)
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) capturePhoto()
    }

    val requestCamera: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) capturePhoto() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    val pickGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val roomId = state.currentRoomId
        if (uri != null && roomId != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
                    }.getOrNull()
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    viewModel.onEvent(
                        WalkEvent.AttachPhoto(
                            roomId = roomId,
                            fileName = PhotoUris.newPhotoFileName(),
                            bytes = bytes,
                            source = PhotoSource.PICKER,
                        ),
                    )
                }
            }
        }
    }

    val requestGallery: () -> Unit = {
        pickGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    WalkContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onFinished = onFinished,
        onRequestCamera = requestCamera,
        onRequestGallery = requestGallery,
        onViewPhoto = { fileName -> viewPhoto(context, fileName) },
        modifier = modifier,
    )
}

/**
 * The route form, mirroring `CompareRoute`: the shell may call either name, and both build the same
 * ViewModel over the walk the caller navigated with.
 */
@Composable
fun WalkRoute(
    walkId: String,
    onBack: () -> Unit,
    onFinished: (walkId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    WalkScreen(walkId = walkId, onBack = onBack, onFinished = onFinished, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalkContent(
    state: WalkUiState,
    onEvent: (WalkEvent) -> Unit,
    onBack: () -> Unit,
    onFinished: (String) -> Unit,
    onRequestCamera: () -> Unit,
    onRequestGallery: () -> Unit,
    onViewPhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val currentRoom = state.currentRoom
    val hasWalk = state.walkId.isNotBlank()

    // The keyboard's height, read *above* the scrolling column: `imePadding()` consumes the inset for
    // everything below it, so a read inside the column would always report zero. Measured on the test
    // phone (SM-A405FM, Android 11) the window frame stays [0,0][1080,2340] while the IME owns
    // [0,1416][1080,2340] — `adjustResize` does not resize a window whose decor does not fit the
    // system windows — so the inset has to be consumed by the layout itself and the focused field has
    // to be scrolled into the space the keyboard leaves.
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // A note and a condition are saved on every keystroke, and a stored photo is its own visible
    // thumbnail: announcing each of those would be noise, so only the events a user cannot see are
    // spoken, and the finished walk is carried by navigation rather than by a transient banner.
    val messageText: String? = state.message?.let { message ->
        when (message) {
            WalkMessage.RoomAdded -> stringResource(R.string.walk_room_added_message)
            WalkMessage.RoomFinished -> stringResource(R.string.walk_room_finished_message)
            WalkMessage.RoomNameRequired -> stringResource(R.string.walk_room_name_required_message)
            WalkMessage.RoomNotFound -> stringResource(R.string.walk_room_not_found_message)
            WalkMessage.NothingToResume -> stringResource(R.string.walk_nothing_to_resume_message)
            WalkMessage.NotSaved -> stringResource(R.string.walk_not_saved_message)
            WalkMessage.NoteSaved, WalkMessage.ConditionSaved, WalkMessage.PhotoStored -> null
            WalkMessage.WalkFinished -> null
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        if (message == WalkMessage.WalkFinished) {
            onFinished(state.walkId)
        } else if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
        }
        onEvent(WalkEvent.MessageShown)
    }

    val currentIndex = state.rooms.indexOfFirst { room -> room.id == state.currentRoomId }
    val heroSubtitle = if (currentRoom != null && currentIndex >= 0) {
        stringResource(R.string.walk_hero_room_position, currentIndex + 1, state.rooms.size)
    } else {
        stringResource(kindLabelRes(state.kind))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.walk_title))
                        if (currentRoom != null) {
                            Text(
                                text = currentRoom.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.walk_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isLoading) {
                // Nothing is claimed while the document is still being read: the walk's kind, its
                // status and its photo count are all defaults until the store answers, and a hero
                // panel or a status pill built from a default is a fabricated reading rather than an
                // empty state.
            } else if (!hasWalk) {
                EmptyState(
                    title = stringResource(R.string.walk_no_walk_title),
                    body = stringResource(R.string.walk_nothing_to_resume_message),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                HeroPanel(
                    title = currentRoom?.name ?: stringResource(R.string.walk_title),
                    subtitle = heroSubtitle,
                    stat = state.photoCount.toString(),
                    statLabel = stringResource(R.string.walk_hero_photos_label),
                )

                StatusPill(
                    label = stringResource(
                        if (state.status == WalkStatus.FINISHED) {
                            R.string.walk_status_finished
                        } else {
                            R.string.walk_status_draft
                        },
                    ),
                    kind = if (state.status == WalkStatus.FINISHED) PillKind.OK else PillKind.NEUTRAL,
                )

                Text(
                    text = stringResource(R.string.walk_rooms_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (state.rooms.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.walk_no_rooms_title),
                        body = stringResource(R.string.walk_no_rooms_body),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    state.rooms.forEach { room ->
                        RoomRowView(
                            room = room,
                            selected = room.id == state.currentRoomId,
                            onClick = { onEvent(WalkEvent.OpenRoom(room.id)) },
                        )
                    }
                }

                var roomNameDraft by remember { mutableStateOf("") }
                // The field and the control that submits it are one group: the tap that failed in the
                // first run was the confirm button sitting in the same IME band as the field.
                ImeRevealGroup(imeVisible = imeVisible) { fieldModifier ->
                    OutlinedTextField(
                        value = roomNameDraft,
                        onValueChange = { roomNameDraft = it },
                        label = { Text(stringResource(R.string.walk_add_room_label)) },
                        placeholder = { Text(stringResource(R.string.walk_add_room_hint)) },
                        singleLine = true,
                        modifier = fieldModifier,
                    )
                    Button(
                        onClick = {
                            val trimmed = roomNameDraft.trim()
                            onEvent(WalkEvent.AddRoom(roomNameDraft))
                            if (trimmed.isNotEmpty()) roomNameDraft = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.walk_add_room_action))
                    }
                }

                if (currentRoom != null) {
                    RoomDetail(
                        room = currentRoom,
                        isLastRoom = state.rooms.lastOrNull()?.id == currentRoom.id,
                        imeVisible = imeVisible,
                        onEvent = onEvent,
                        onRequestCamera = onRequestCamera,
                        onRequestGallery = onRequestGallery,
                        onViewPhoto = onViewPhoto,
                    )
                } else if (state.rooms.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.walk_current_room_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.status == WalkStatus.DRAFT) {
                    OutlinedButton(
                        onClick = { onEvent(WalkEvent.FinishWalk) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.walk_finish_walk_action))
                    }
                }
            }
        }
    }
}

/**
 * One room as the walk lists it: its name, a summary of its note, how many photos it holds, and the
 * condition recorded for it. A room whose condition was never recorded says so — it is never rounded
 * up to "good" — and the condition is carried by a word and a silhouette as well as a colour.
 */
@Composable
private fun RoomRowView(
    room: RoomRow,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photoLabel = stringResource(R.string.walk_room_photos_label)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (room.note.isBlank()) {
                        stringResource(R.string.walk_room_note_absent)
                    } else {
                        room.note
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (room.photoFileNames.isNotEmpty()) {
                    Text(
                        text = "${room.photoFileNames.size} $photoLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (room.condition != null) {
                StatusPill(
                    label = conditionLabel(room.condition),
                    kind = conditionPill(room.condition),
                )
            } else {
                StatusPill(
                    label = stringResource(R.string.walk_condition_not_recorded),
                    kind = PillKind.NEUTRAL,
                )
            }
        }
    }
}

/**
 * The room being filled: its note, its condition, its photos and the two ways to advance.
 *
 * The note field is the slot's first text input, and the first device run measured that
 * `adjustResize` alone does **not** keep it clear of the keyboard on Android 11: with the window's
 * decor not fitting the system windows, the app frame stays `[0,0][1080,2340]` while the IME owns
 * `[0,1416][1080,2340]`, and the field's own bounds (`y 1460..1657`) are inside that band. The fix
 * is two-part and both parts are visible in this file: the scrolling column consumes the IME inset
 * with `imePadding()` *before* it scrolls, and [ImeRevealGroup] scrolls the focused field into the
 * smaller viewport. The note is dispatched on every change — the brain persists each one — and the
 * draft the user typed is held locally so a recomposition cannot discard a keystroke.
 */
@Composable
private fun RoomDetail(
    room: RoomRow,
    isLastRoom: Boolean,
    imeVisible: Boolean,
    onEvent: (WalkEvent) -> Unit,
    onRequestCamera: () -> Unit,
    onRequestGallery: () -> Unit,
    onViewPhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var noteDraft by remember(room.id) { mutableStateOf(room.note) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.walk_current_room_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = room.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(R.string.walk_note_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ImeRevealGroup(imeVisible = imeVisible) { fieldModifier ->
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { updated ->
                        noteDraft = updated
                        onEvent(WalkEvent.NoteChanged(room.id, updated))
                    },
                    label = { Text(stringResource(R.string.walk_note_label)) },
                    placeholder = { Text(stringResource(R.string.walk_note_hint)) },
                    minLines = 3,
                    modifier = fieldModifier,
                )
            }

            Text(
                text = stringResource(R.string.walk_condition_header),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoomCondition.entries.forEach { condition ->
                    FilterChip(
                        selected = room.condition == condition,
                        onClick = { onEvent(WalkEvent.ConditionChanged(room.id, condition)) },
                        label = { Text(conditionLabel(condition)) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.walk_photos_header),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (room.photoFileNames.isEmpty()) {
                Text(
                    text = stringResource(R.string.walk_photos_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PhotoThumbRow(
                // The domain names a photo with a bare name inside the store's own `photos/`
                // directory (`domain/Model.kt`: "[fileName] is a name inside the store's own
                // `photos/` directory, never a path"), while `PhotoThumbRow` resolves what it is
                // given relative to the app's private files directory (`Brand.kt`: "named by the
                // caller either in full or relative to the app's private files directory"). This
                // screen is where the two meet, so it supplies the one directory component the
                // component's own contract asks for, from `WalkStore.PHOTOS_DIR` rather than from a
                // restated literal. The row itself keeps the bare name: nothing here is stored.
                fileNames = room.photoFileNames.map { fileName ->
                    "${WalkStore.PHOTOS_DIR}/$fileName"
                },
                onOpen = onViewPhoto,
                onAdd = onRequestCamera,
                addLabel = stringResource(R.string.walk_photo_add_camera),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = onRequestGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_gallery),
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.walk_photo_add_gallery))
            }

            Button(
                onClick = { onEvent(WalkEvent.FinishRoom(room.id)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    stringResource(
                        if (isLastRoom) {
                            R.string.walk_finish_room_action
                        } else {
                            R.string.walk_finish_room_and_next_action
                        },
                    ),
                )
            }
        }
    }
}

/**
 * A group of controls that is scrolled clear of the keyboard once its field is focused.
 *
 * `imePadding()` on the scrolling column is only half of the fix. On Android 11 the window is not
 * resized — measured on SM-A405FM: the app frame stays `[0,0][1080,2340]` while the IME owns
 * `[0,1416][1080,2340]` — so the inset is consumed by the layout and the focused field still has to
 * be *moved* into the space the keyboard leaves. Nothing asks the container to do that by itself, so
 * the group requests it through the responder chain `Modifier.verticalScroll` already provides, one
 * frame after the inset has shrunk the viewport, so the measurement is of the viewport the keyboard
 * leaves rather than the one it is about to cover.
 *
 * [content] receives the modifier its field must carry: applying it is what reports focus, and a
 * group whose field never takes it simply never scrolls.
 */
@Composable
private fun ImeRevealGroup(
    imeVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (fieldModifier: Modifier) -> Unit,
) {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused, imeVisible) {
        if (focused && imeVisible) {
            withFrameNanos { }
            requester.bringIntoView()
        }
    }
    Column(
        modifier = modifier.bringIntoViewRequester(requester),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content(Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused })
    }
}

/** A condition's word. The `when` is exhaustive and carries no `else`, so a new condition is a build error. */
@Composable
private fun conditionLabel(condition: RoomCondition): String = stringResource(
    when (condition) {
        RoomCondition.GOOD -> R.string.walk_condition_good
        RoomCondition.WORN -> R.string.walk_condition_worn
        RoomCondition.DAMAGED -> R.string.walk_condition_damaged
        RoomCondition.MISSING -> R.string.walk_condition_missing
    },
)

/** The silhouette a condition is drawn with, so the state never depends on colour alone. */
private fun conditionPill(condition: RoomCondition): PillKind = when (condition) {
    RoomCondition.GOOD -> PillKind.OK
    RoomCondition.WORN -> PillKind.WARN
    RoomCondition.DAMAGED -> PillKind.DANGER
    RoomCondition.MISSING -> PillKind.NEUTRAL
}

/** A walk kind's word. Exhaustive and without `else`, for the same reason as [conditionLabel]. */
private fun kindLabelRes(kind: WalkKind): Int = when (kind) {
    WalkKind.MOVE_IN -> R.string.walk_kind_move_in
    WalkKind.MOVE_OUT -> R.string.walk_kind_move_out
    WalkKind.SINGLE -> R.string.walk_kind_single
}

/**
 * Shows one committed photo in whatever viewer the device offers.
 *
 * The path is the store's own — [PhotoUris.durablePhotoFile], imported rather than rebuilt — and the
 * URI goes through the app's FileProvider, so nothing outside the declared `photos/` root is exposed.
 * A device with no viewer, or a photo whose bytes escaped, is not an exception here: the tap simply
 * does nothing rather than crashing the walk.
 */
private fun viewPhoto(context: Context, fileName: String) {
    val uri = runCatching {
        PhotoUris.uriFor(context, PhotoUris.durablePhotoFile(context, fileName))
    }.getOrNull() ?: return
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "image/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(intent) }
}

@Preview(name = "Walk light", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun WalkContentLightPreview() {
    UnitWalkTheme(darkTheme = false, dynamicColor = false) {
        WalkContent(
            state = WalkPreviewState,
            onEvent = {},
            onBack = {},
            onFinished = {},
            onRequestCamera = {},
            onRequestGallery = {},
            onViewPhoto = {},
        )
    }
}

@Preview(
    name = "Walk dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WalkContentDarkPreview() {
    UnitWalkTheme(darkTheme = true, dynamicColor = false) {
        WalkContent(
            state = WalkPreviewState,
            onEvent = {},
            onBack = {},
            onFinished = {},
            onRequestCamera = {},
            onRequestGallery = {},
            onViewPhoto = {},
        )
    }
}

private val WalkPreviewState: WalkUiState = WalkUiState(
    isLoading = false,
    walkId = "walk-1",
    unitId = "unit-1",
    kind = WalkKind.MOVE_OUT,
    status = WalkStatus.DRAFT,
    rooms = listOf(
        RoomRow(
            id = "room-1",
            name = "Kitchen",
            note = "Chip in the worktop by the hob.",
            photoFileNames = listOf("photo-1.jpg", "photo-2.jpg"),
            condition = RoomCondition.WORN,
        ),
        RoomRow(
            id = "room-2",
            name = "Hallway",
            note = "",
            photoFileNames = emptyList(),
            condition = null,
        ),
    ),
    currentRoomId = "room-1",
    photoCount = 2,
)
