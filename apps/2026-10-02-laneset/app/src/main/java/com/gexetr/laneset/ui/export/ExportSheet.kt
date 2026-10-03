package com.gexetr.laneset.ui.export

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gexetr.laneset.R
import com.gexetr.laneset.domain.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The two ways a finished set leaves the phone.
 *
 * This is the body of the handoff sheet — `ui/home/HomeScreen.kt` hosts it inside a
 * `ModalBottomSheet` and owns the dismissal gesture, which is why this composable is content rather
 * than the sheet itself. Both actions are one tap, labelled, and at least 48 dp tall.
 *
 * Accessibility decisions, stated because they shape the code:
 * - each action is a single focus target built on `ListItem`, with the whole row clickable and
 *   `Role.Button`, so a screen reader announces one labelled button rather than an unlabelled icon;
 * - the leading glyph is decorative and passes `contentDescription = null`; the text carries meaning;
 * - the close control is an `IconButton`, which is a 48 dp target by construction, and its glyph has
 *   an accessible name.
 *
 * A failure is reported inline rather than as a crash or a silent no-op: a device with no app that
 * accepts a text share says so, and a card that could not be written says so.
 */
@Composable
fun ExportSheet(session: Session, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var messageRes by remember { mutableStateOf(0) }

    val chooserTitle = stringResource(R.string.export_share_chooser_title)
    val fileName = stringResource(R.string.export_pdf_file_name)

    // The Storage Access Framework route: the swimmer picks where the card goes and the app writes
    // into the URI that comes back. No storage permission and no FileProvider are involved.
    val printLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri: Uri? = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            coroutineScope.launch {
                val written = withContext(Dispatchers.IO) {
                    runCatching { writeCard(context, session, uri) }.isSuccess
                }
                messageRes = if (written) R.string.export_print_saved else R.string.export_print_failed
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.export_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.export_sheet_supporting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.export_close),
                )
            }
        }

        ExportAction(
            icon = R.drawable.ic_share,
            title = stringResource(R.string.export_share_action),
            supporting = stringResource(R.string.export_share_supporting),
            onClick = {
                if (SessionExport.launchShare(context, session, chooserTitle)) {
                    onDismiss()
                } else {
                    messageRes = R.string.export_share_failed
                }
            },
        )

        ExportAction(
            icon = R.drawable.ic_print,
            title = stringResource(R.string.export_print_action),
            supporting = stringResource(R.string.export_print_supporting),
            onClick = { printLauncher.launch(SessionExport.createDocumentIntent(fileName)) },
        )

        if (messageRes != 0) {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}

/** One handoff action: a labelled, 48 dp, single-focus row with a decorative leading glyph. */
@Composable
private fun ExportAction(
    icon: Int,
    title: String,
    supporting: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
    )
}

/** Opens the URI the swimmer chose and lets [SessionExport] write the card into it. */
private fun writeCard(context: Context, session: Session, uri: Uri) {
    val output = context.contentResolver.openOutputStream(uri)
        ?: error("the create-document URI has no writable stream")
    output.use { SessionExport.writePdfCard(session, it) }
}
