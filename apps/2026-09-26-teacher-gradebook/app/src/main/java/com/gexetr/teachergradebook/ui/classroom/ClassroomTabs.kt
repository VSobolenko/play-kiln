package com.gexetr.teachergradebook.ui.classroom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gexetr.teachergradebook.R
import com.gexetr.teachergradebook.domain.model.AttendanceStatus
import com.gexetr.teachergradebook.domain.model.Classroom
import com.gexetr.teachergradebook.domain.model.GradeBand
import com.gexetr.teachergradebook.domain.model.NoteKind
import com.gexetr.teachergradebook.domain.model.ParentContact
import com.gexetr.teachergradebook.domain.model.RecordNote
import com.gexetr.teachergradebook.domain.model.RoundMode
import com.gexetr.teachergradebook.ui.components.DestructiveConfirmDialog
import com.gexetr.teachergradebook.ui.components.EmptyHint
import com.gexetr.teachergradebook.ui.components.FieldSpec
import com.gexetr.teachergradebook.ui.components.IsoDateField
import com.gexetr.teachergradebook.ui.components.MultiFieldDialog
import com.gexetr.teachergradebook.ui.components.HeroPanel
import com.gexetr.teachergradebook.ui.components.HeroStat
import com.gexetr.teachergradebook.ui.components.Panel
import com.gexetr.teachergradebook.ui.components.ScreenGutter
import com.gexetr.teachergradebook.ui.components.StatTile
import com.gexetr.teachergradebook.ui.components.StatusPill
import com.gexetr.teachergradebook.ui.components.Tone
import com.gexetr.teachergradebook.ui.components.SectionCard
import com.gexetr.teachergradebook.ui.components.isIsoDate
import com.gexetr.teachergradebook.ui.components.todayIso
import com.gexetr.teachergradebook.ui.components.trim
import com.gexetr.teachergradebook.ui.recordbook.RecordBookViewModel
import com.gexetr.teachergradebook.ui.roundingRuleText

/** The words a teacher uses, not the enum names. */
@Composable
internal fun attendanceLabel(status: AttendanceStatus): String = stringResource(when (status) {
    AttendanceStatus.PRESENT -> R.string.classroom_attendance_present
    AttendanceStatus.TARDY -> R.string.classroom_attendance_late
    AttendanceStatus.ABSENT_EXCUSED -> R.string.classroom_attendance_excused
    AttendanceStatus.ABSENT_UNEXCUSED -> R.string.classroom_attendance_absent
})

private fun attendanceTone(status: AttendanceStatus): Tone = when (status) {
    AttendanceStatus.PRESENT -> Tone.Positive
    AttendanceStatus.TARDY -> Tone.Attention
    AttendanceStatus.ABSENT_EXCUSED -> Tone.Neutral
    AttendanceStatus.ABSENT_UNEXCUSED -> Tone.Danger
}

@Composable
internal fun noteKindLabel(kind: NoteKind): String = stringResource(when (kind) {
    NoteKind.PARTICIPATION -> R.string.classroom_note_participation
    NoteKind.BEHAVIOUR -> R.string.classroom_note_behaviour
    NoteKind.MISSING_WORK -> R.string.classroom_note_missing_work
    NoteKind.GENERAL -> R.string.classroom_note_general
})

/**
 * One date's attendance for the whole class.
 *
 * The date is chosen first and everything on the screen is about that date, because attendance is
 * always taken for a register, never for a person. "Everybody present" is one tap, because that is
 * what happens on most days and a register that takes a minute per student does not get taken.
 */
@Composable
internal fun AttendanceTab(classroom: Classroom, recordBook: RecordBookViewModel) {
    var date by remember { mutableStateOf(todayIso()) }
    val validDate = isIsoDate(date)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val marks = classroom.attendance.filter { it.date == date }
            Panel {
                Text(
                    stringResource(R.string.classroom_attendance_headline),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        stringResource(R.string.classroom_attendance_present),
                        marks.count { it.status == AttendanceStatus.PRESENT }.toString(),
                        Modifier.weight(1f),
                        Tone.Positive,
                    )
                    StatTile(
                        stringResource(R.string.classroom_attendance_late),
                        marks.count { it.status == AttendanceStatus.TARDY }.toString(),
                        Modifier.weight(1f),
                        Tone.Attention,
                    )
                    StatTile(
                        stringResource(R.string.classroom_attendance_absent),
                        marks.count { it.status == AttendanceStatus.ABSENT_UNEXCUSED }.toString(),
                        Modifier.weight(1f),
                        Tone.Danger,
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AttendanceStatus.entries.forEach { status ->
                        StatusPill(attendanceLabel(status), attendanceTone(status))
                    }
                }
            }
        }
        item {
            IsoDateField(
                value = date,
                onValueChange = { date = it },
                label = stringResource(R.string.classroom_register_for),
            )
        }
        if (!validDate) {
            item { EmptyHint(stringResource(R.string.classroom_date_help, todayIso())) }
        }
        if (validDate) {
            val marks = classroom.attendance.filter { it.date == date }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { recordBook.markAllPresent(classroom.id, date) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.classroom_everyone_present)) }
                    Text(
                        text = stringResource(
                            R.string.classroom_attendance_recorded,
                            marks.size,
                            classroom.students.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (classroom.students.isEmpty()) {
                item { EmptyHint(stringResource(R.string.classroom_add_roster_first)) }
            }
            items(classroom.students, key = { it.id }) { student ->
                val current = marks.firstOrNull { it.studentId == student.id }?.status
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = student.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AttendanceStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = current == status,
                                    onClick = {
                                        if (current == status) {
                                            recordBook.clearAttendance(classroom.id, student.id, date)
                                        } else {
                                            recordBook.setAttendance(classroom.id, student.id, date, status)
                                        }
                                    },
                                    label = { Text(attendanceLabel(status)) },
                                )
                            }
                        }
                        if (current == null) {
                            StatusPill(stringResource(R.string.classroom_attendance_unrecorded), Tone.Attention)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Everything written about students in this class, newest first, plus the follow-ups that are due.
 *
 * Notes are added from a student's own screen, where the teacher is already looking at that
 * student; this tab is for reading them back and for noticing the parent contact that needed a
 * follow-up date.
 */
@Composable
internal fun NotesTab(classroom: Classroom, recordBook: RecordBookViewModel) {
    var deletingNote by remember { mutableStateOf<RecordNote?>(null) }
    var deletingContact by remember { mutableStateOf<ParentContact?>(null) }
    var editingNote by remember { mutableStateOf<RecordNote?>(null) }
    var addingNote by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<ParentContact?>(null) }
    var addingContact by remember { mutableStateOf(false) }

    val notes = classroom.notes.sortedByDescending { it.date }
    val contacts = classroom.contacts.sortedByDescending { it.date }
    val followUps = contacts.filter { it.followUpOn != null }.sortedBy { it.followUpOn }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeroPanel(
                eyebrow = stringResource(R.string.classroom_notes_eyebrow),
                headline = stringResource(R.string.classroom_notes_headline),
                supporting = stringResource(R.string.classroom_notes_supporting),
                stats = listOf(
                    HeroStat(stringResource(R.string.classroom_notes_label), notes.size.toString()),
                    HeroStat(stringResource(R.string.classroom_contacts_label), contacts.size.toString()),
                    HeroStat(stringResource(R.string.classroom_followups_label), followUps.size.toString()),
                ),
            )
        }
        item {
            SectionCard(title = stringResource(R.string.classroom_followups_title)) {
                if (followUps.isEmpty()) {
                    EmptyHint(
                        stringResource(R.string.classroom_followups_empty),
                    )
                } else {
                    followUps.forEach { contact ->
                        val student = classroom.students.firstOrNull { it.id == contact.studentId }
                        Text(
                            text = stringResource(
                                R.string.classroom_followup_summary,
                                contact.followUpOn.orEmpty(),
                                student?.displayName ?: stringResource(R.string.classroom_former_student),
                                contact.channel,
                                contact.summary,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.classroom_parent_contacts), style = MaterialTheme.typography.titleMedium)
            TextButton(
                onClick = { addingContact = true },
                enabled = classroom.students.isNotEmpty(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.records_add_contact)) }
        }
        if (contacts.isEmpty()) {
            item {
                EmptyHint(stringResource(R.string.classroom_contacts_empty))
            }
        }
        items(contacts, key = { it.id }) { contact ->
            val student = classroom.students.firstOrNull { it.id == contact.studentId }
            RowCard(
                title = student?.displayName ?: stringResource(R.string.classroom_former_student),
                subtitle = contact.followUpOn?.let {
                    stringResource(
                        R.string.classroom_contact_with_followup,
                        contact.date,
                        contact.channel,
                        contact.summary,
                        it,
                    )
                } ?: stringResource(
                    R.string.classroom_contact_summary,
                    contact.date,
                    contact.channel,
                    contact.summary,
                ),
                onEdit = { editingContact = contact },
                onDelete = { deletingContact = contact },
            )
        }
        item {
            Text(stringResource(R.string.classroom_notes_label), style = MaterialTheme.typography.titleMedium)
            TextButton(
                onClick = { addingNote = true },
                enabled = classroom.students.isNotEmpty(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.records_add_note)) }
        }
        if (notes.isEmpty()) {
            item { EmptyHint(stringResource(R.string.classroom_notes_empty)) }
        }
        items(notes, key = { it.id }) { note ->
            val student = classroom.students.firstOrNull { it.id == note.studentId }
            RowCard(
                title = student?.displayName ?: stringResource(R.string.classroom_former_student),
                subtitle = stringResource(
                    R.string.classroom_note_summary,
                    note.date,
                    noteKindLabel(note.kind),
                    note.text,
                ),
                onEdit = { editingNote = note },
                onDelete = { deletingNote = note },
            )
        }
    }

    if (addingNote || editingNote != null) {
        val note = editingNote
        NoteEditorDialog(
            classroom = classroom,
            note = note,
            onDismiss = { addingNote = false; editingNote = null },
            onSave = { studentId, date, kind, text ->
                if (note == null) recordBook.addNote(classroom.id, studentId, date, kind, text)
                else recordBook.updateNote(classroom.id, note.id, date, kind, text)
                addingNote = false
                editingNote = null
            },
        )
    }

    if (addingContact || editingContact != null) {
        val contact = editingContact
        ContactEditorDialog(
            classroom = classroom,
            contact = contact,
            onDismiss = { addingContact = false; editingContact = null },
            onSave = { studentId, date, channel, summary, followUp ->
                if (contact == null) recordBook.addContact(classroom.id, studentId, date, channel, summary, followUp)
                else recordBook.updateContact(classroom.id, contact.id, date, channel, summary, followUp)
                addingContact = false
                editingContact = null
            },
        )
    }

    deletingNote?.let { note ->
        DestructiveConfirmDialog(
            title = stringResource(R.string.classroom_delete_note_title),
            body = stringResource(R.string.classroom_delete_note_body, note.text, note.date),
            confirmLabel = stringResource(R.string.classroom_delete_note),
            onConfirm = {
                deletingNote = null
                recordBook.deleteNote(classroom.id, note.id)
            },
            onDismiss = { deletingNote = null },
        )
    }

    deletingContact?.let { contact ->
        DestructiveConfirmDialog(
            title = stringResource(R.string.classroom_delete_contact_title),
            body = stringResource(R.string.classroom_delete_contact_body, contact.date, contact.channel),
            confirmLabel = stringResource(R.string.classroom_delete_contact),
            onConfirm = {
                deletingContact = null
                recordBook.deleteContact(classroom.id, contact.id)
            },
            onDismiss = { deletingContact = null },
        )
    }
}

@Composable
private fun RowCard(title: String, subtitle: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.records_edit))
            }
            TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.classroom_delete))
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(
    classroom: Classroom,
    note: RecordNote?,
    onDismiss: () -> Unit,
    onSave: (String, String, NoteKind, String) -> Unit,
) {
    var studentId by remember(note) { mutableStateOf(note?.studentId ?: classroom.students.first().id) }
    var date by remember(note) { mutableStateOf(note?.date ?: todayIso()) }
    var kind by remember(note) { mutableStateOf(note?.kind ?: NoteKind.PARTICIPATION) }
    var text by remember(note) { mutableStateOf(note?.text.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (note == null) R.string.records_add_note_title else R.string.records_edit_note_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.records_student), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    classroom.students.forEach { student -> FilterChip(studentId == student.id, { studentId = student.id }, { Text(student.displayName) }) }
                }
                Text(stringResource(R.string.records_note_kind), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteKind.entries.forEach { option -> FilterChip(kind == option, { kind = option }, { Text(noteKindLabel(option)) }) }
                }
                IsoDateField(date, { date = it }, stringResource(R.string.common_date))
                OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.records_note_text)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
                Text(stringResource(R.string.records_note_change_help), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton({ onSave(studentId, date, kind, text.trim()) }, enabled = isIsoDate(date) && text.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.records_save_note)) } },
        dismissButton = { TextButton(onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun ContactEditorDialog(
    classroom: Classroom,
    contact: ParentContact?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String?) -> Unit,
) {
    var studentId by remember(contact) { mutableStateOf(contact?.studentId ?: classroom.students.first().id) }
    var date by remember(contact) { mutableStateOf(contact?.date ?: todayIso()) }
    var channel by remember(contact) { mutableStateOf(contact?.channel.orEmpty()) }
    var summary by remember(contact) { mutableStateOf(contact?.summary.orEmpty()) }
    var followUp by remember(contact) { mutableStateOf(contact?.followUpOn.orEmpty()) }
    val followUpValid = followUp.isBlank() || isIsoDate(followUp)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (contact == null) R.string.records_add_contact_title else R.string.records_edit_contact_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.records_student), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    classroom.students.forEach { student -> FilterChip(studentId == student.id, { studentId = student.id }, { Text(student.displayName) }) }
                }
                IsoDateField(date, { date = it }, stringResource(R.string.records_contact_date))
                OutlinedTextField(channel, { channel = it }, label = { Text(stringResource(R.string.records_contact_method)) }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
                OutlinedTextField(summary, { summary = it }, label = { Text(stringResource(R.string.records_contact_summary)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
                IsoDateField(followUp, { followUp = it }, stringResource(R.string.records_follow_up_date))
                Text(stringResource(R.string.records_contact_change_help), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton({ onSave(studentId, date, channel.trim(), summary.trim(), followUp.trim().ifBlank { null }) }, enabled = isIsoDate(date) && followUpValid && channel.isNotBlank() && summary.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.records_save_contact)) } },
        dismissButton = { TextButton(onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/**
 * The room, as a grid of desks.
 *
 * Desks are addressed by a stable index, so resizing the grid keeps the students who still have a
 * desk instead of silently shuffling the room. Assigning a student who already sits somewhere
 * moves them, because a student cannot be in two places.
 */
@Composable
internal fun SeatingTab(classroom: Classroom, recordBook: RecordBookViewModel) {
    val context = LocalContext.current
    val plan = classroom.seating
    var resizing by remember { mutableStateOf(false) }
    var assigningDesk by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val unseatedCount = classroom.students.count { plan.deskOf(it.id) == null }
            HeroPanel(
                eyebrow = stringResource(R.string.classroom_seating_eyebrow),
                headline = stringResource(R.string.classroom_seating_headline),
                supporting = stringResource(R.string.classroom_seating_supporting),
                stats = listOf(
                    HeroStat(stringResource(R.string.classroom_desks_label), plan.deskCount.toString()),
                    HeroStat(stringResource(R.string.classroom_seated_label), plan.seats.size.toString()),
                    HeroStat(stringResource(R.string.classroom_unseated_label), unseatedCount.toString()),
                ),
            )
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (plan.deskCount == 0) {
                        stringResource(R.string.classroom_seating_none)
                    } else {
                        stringResource(
                            R.string.classroom_seating_summary,
                            plan.rows,
                            plan.columns,
                            plan.seats.size,
                            plan.deskCount,
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { resizing = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        if (plan.deskCount == 0) {
                            stringResource(R.string.classroom_set_up)
                        } else {
                            stringResource(R.string.classroom_resize)
                        },
                    )
                }
            }
        }
        if (plan.deskCount == 0) {
            item {
                EmptyHint(
                    stringResource(R.string.classroom_seating_empty),
                )
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        for (row in 0 until plan.rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (column in 0 until plan.columns) {
                                    val desk = row * plan.columns + column
                                    val studentId = plan.studentAt(desk)
                                    val student = classroom.students.firstOrNull { it.id == studentId }
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp)
                                            .clickable { assigningDesk = desk },
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = student?.displayName ?: stringResource(R.string.classroom_empty_desk),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (student == null) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val unseated = classroom.students.filter { plan.deskOf(it.id) == null }
            if (unseated.isNotEmpty()) {
                item { Text(stringResource(R.string.classroom_not_seated), style = MaterialTheme.typography.titleMedium) }
                items(unseated, key = { it.id }) { student ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = student.displayName, modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { recordBook.moveToNextFreeDesk(classroom.id, student.id) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text(stringResource(R.string.classroom_next_free_desk)) }
                        }
                    }
                }
            }
        }
    }

    if (resizing) {
        MultiFieldDialog(
            title = stringResource(R.string.classroom_seating_grid),
            confirmLabel = stringResource(R.string.classroom_apply),
            fields = listOf(
                FieldSpec("rows", stringResource(R.string.classroom_rows), if (plan.rows == 0) "5" else plan.rows.toString(), numeric = true),
                FieldSpec("columns", stringResource(R.string.classroom_columns), if (plan.columns == 0) "6" else plan.columns.toString(), numeric = true),
            ),
            onDismiss = { resizing = false },
            onConfirm = { values ->
                resizing = false
                recordBook.resizeSeating(
                    classroom.id,
                    values["rows"]?.toDoubleOrNull()?.toInt() ?: 0,
                    values["columns"]?.toDoubleOrNull()?.toInt() ?: 0,
                )
            },
        )
    }

    assigningDesk?.let { desk ->
        val current = plan.studentAt(desk)
        MultiFieldDialog(
            title = stringResource(R.string.classroom_desk_number, desk + 1),
            confirmLabel = if (current == null) {
                stringResource(R.string.classroom_seat)
            } else {
                stringResource(R.string.classroom_move_here)
            },
            fields = listOf(
                FieldSpec(
                    "student",
                    stringResource(R.string.classroom_student_name),
                    classroom.students.firstOrNull { it.id == current }?.displayName ?: "",
                    supporting = stringResource(R.string.classroom_seat_help),
                ),
            ),
            onDismiss = { assigningDesk = null },
            onConfirm = { values ->
                assigningDesk = null
                val typed = values["student"].orEmpty()
                if (typed.isBlank() || typed.equals("clear", ignoreCase = true)) {
                    recordBook.setSeat(classroom.id, desk, null)
                } else {
                    val match = classroom.students.firstOrNull {
                        it.displayName.equals(typed, ignoreCase = true) ||
                            it.firstName.equals(typed, ignoreCase = true) ||
                            it.lastName.equals(typed, ignoreCase = true)
                    }
                    if (match == null) {
                        recordBook.announce(context.getString(R.string.classroom_student_not_found, typed))
                    } else {
                        recordBook.setSeat(classroom.id, desk, match.id)
                    }
                }
            },
        )
    }
}

/** What leaves the app, and what comes back. */
@Composable
internal fun ReportsTab(
    classroom: Classroom,
    unlocked: Boolean,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroPanel(
                eyebrow = stringResource(R.string.classroom_reports_eyebrow),
                headline = stringResource(R.string.classroom_reports_headline),
                supporting = if (unlocked) {
                    stringResource(R.string.classroom_reports_ready)
                } else {
                    stringResource(R.string.classroom_reports_locked)
                },
                stats = listOf(
                    HeroStat(stringResource(R.string.classroom_csv_rows), classroom.students.size.toString()),
                    HeroStat(stringResource(R.string.classroom_assignments_label), classroom.assignments.size.toString()),
                    HeroStat(stringResource(R.string.classroom_backups_label), if (unlocked) "1" else "0"),
                ),
            )
        }
        item {
            SectionCard(title = stringResource(R.string.classroom_csv_title)) {
                Text(
                    text = stringResource(R.string.classroom_csv_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onExportCsv, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.classroom_export_grade_sheet))
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.classroom_pdf_title)) {
                Text(
                    text = stringResource(R.string.classroom_pdf_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onExportPdf, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.classroom_export_class_report))
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.classroom_backup_title)) {
                Text(
                    text = stringResource(R.string.classroom_backup_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onBackup, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.classroom_backup))
                    }
                    TextButton(onClick = onRestore, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.classroom_restore))
                    }
                }
            }
        }
        if (!unlocked) {
            item {
                EmptyHint(
                    stringResource(R.string.classroom_export_locked),
                )
            }
        }
        item {
            Panel {
                Text(
                    stringResource(R.string.classroom_inventory_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                listOf(
                    pluralStringResource(R.plurals.classroom_students_count, classroom.students.size, classroom.students.size),
                    pluralStringResource(R.plurals.classroom_assignments_count, classroom.assignments.size, classroom.assignments.size),
                    pluralStringResource(R.plurals.classroom_marks_count, classroom.grades.size, classroom.grades.size),
                    pluralStringResource(R.plurals.classroom_attendance_records_count, classroom.attendance.size, classroom.attendance.size),
                    pluralStringResource(R.plurals.classroom_notes_count, classroom.notes.size, classroom.notes.size),
                    pluralStringResource(R.plurals.classroom_contacts_count, classroom.contacts.size, classroom.contacts.size),
                ).forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * The rules behind the numbers, and the last place a class can be deleted from.
 *
 * The rounding rule and the missing-work rule are here rather than hidden in a settings menu
 * because they change what every number on every other tab means, and a teacher is entitled to
 * know which one is in force.
 */
@Composable
internal fun SetupTab(classroom: Classroom, recordBook: RecordBookViewModel) {
    val context = LocalContext.current
    var editingDetails by remember { mutableStateOf(false) }
    var addingBand by remember { mutableStateOf(false) }
    var deletingClass by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroPanel(
                eyebrow = stringResource(R.string.classroom_setup_eyebrow),
                headline = stringResource(R.string.classroom_setup_headline),
                supporting = stringResource(R.string.classroom_setup_supporting),
                stats = listOf(
                    HeroStat(stringResource(R.string.classroom_categories_label), classroom.categories.size.toString()),
                    HeroStat(stringResource(R.string.classroom_grade_bands_label), classroom.gradeScale.size.toString()),
                ),
            )
        }
        item {
            SectionCard(title = stringResource(R.string.classroom_class_details)) {
                Text(
                    text = listOfNotNull(
                        classroom.subject.takeIf { it.isNotBlank() } ?: stringResource(R.string.classroom_no_subject),
                        classroom.period.takeIf { it.isNotBlank() }?.let {
                            stringResource(R.string.classroom_period_value, it)
                        },
                        classroom.term.takeIf { it.isNotBlank() } ?: stringResource(R.string.classroom_no_term),
                    ).joinToString(stringResource(R.string.classroom_separator)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { editingDetails = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.classroom_edit))
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.classroom_rounding)) {
                Text(
                    text = roundingRuleText(classroom.rounding),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RoundMode.entries.forEach { mode ->
                        FilterChip(
                            selected = classroom.rounding.mode == mode,
                            onClick = {
                                recordBook.setRounding(
                                    classroom.id,
                                    classroom.rounding.copy(mode = mode),
                                )
                            },
                            label = {
                                Text(
                                    when (mode) {
                                        RoundMode.HALF_UP -> stringResource(R.string.classroom_round_half_up)
                                        RoundMode.HALF_EVEN -> stringResource(R.string.classroom_round_half_even)
                                        RoundMode.FLOOR -> stringResource(R.string.classroom_round_floor)
                                    },
                                )
                            },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..2).forEach { decimals ->
                        FilterChip(
                            selected = classroom.rounding.decimals == decimals,
                            onClick = {
                                recordBook.setRounding(
                                    classroom.id,
                                    classroom.rounding.copy(decimals = decimals),
                                )
                            },
                            label = {
                                Text(pluralStringResource(R.plurals.classroom_decimals, decimals, decimals))
                            },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.classroom_rounding_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(title = stringResource(R.string.classroom_missing_work_title)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (classroom.missingCountsAsZero) {
                                stringResource(R.string.classroom_missing_counts_zero)
                            } else {
                                stringResource(R.string.classroom_missing_excluded)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.classroom_exempt_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = classroom.missingCountsAsZero,
                        onCheckedChange = { counts ->
                            recordBook.setMissingCountsAsZero(classroom.id, counts)
                        },
                    )
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.classroom_letter_grades)) {
                if (classroom.gradeScale.isEmpty()) {
                    EmptyHint(
                        stringResource(R.string.classroom_letter_grades_empty),
                    )
                } else {
                    classroom.gradeScale.sortedByDescending { it.minPercent }.forEach { band ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(
                                    R.string.classroom_grade_band,
                                    band.letter,
                                    trim(band.minPercent),
                                ),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(
                                onClick = {
                                    recordBook.setGradeScale(
                                        classroom.id,
                                        classroom.gradeScale.filterNot { it == band },
                                    )
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text(stringResource(R.string.classroom_remove)) }
                        }
                    }
                }
                TextButton(onClick = { addingBand = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.classroom_add_band))
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.classroom_delete_class_title)) {
                Text(
                    text = stringResource(R.string.classroom_delete_class_warning),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { deletingClass = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        stringResource(R.string.classroom_delete_named_action, classroom.name),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (editingDetails) {
        MultiFieldDialog(
            title = stringResource(R.string.classroom_class_details),
            confirmLabel = stringResource(R.string.classroom_save),
            fields = listOf(
                FieldSpec("name", stringResource(R.string.classroom_class_name), classroom.name),
                FieldSpec("subject", stringResource(R.string.classroom_subject), classroom.subject),
                FieldSpec("period", stringResource(R.string.classroom_period), classroom.period),
                FieldSpec("term", stringResource(R.string.classroom_term), classroom.term),
            ),
            onDismiss = { editingDetails = false },
            onConfirm = { values ->
                editingDetails = false
                recordBook.updateClassDetails(
                    classroom.id,
                    values["subject"].orEmpty(),
                    values["period"].orEmpty(),
                    values["term"].orEmpty(),
                )
                values["name"]?.takeIf { it.isNotBlank() }?.let { name ->
                    recordBook.renameClass(classroom.id, name)
                }
            },
        )
    }

    if (addingBand) {
        MultiFieldDialog(
            title = stringResource(R.string.classroom_add_letter_band),
            confirmLabel = stringResource(R.string.classroom_add),
            fields = listOf(
                FieldSpec(
                    "letter",
                    stringResource(R.string.classroom_letter),
                    "",
                    supporting = stringResource(R.string.classroom_letter_example),
                ),
                FieldSpec("min", stringResource(R.string.classroom_from_percent), "90", numeric = true),
            ),
            onDismiss = { addingBand = false },
            onConfirm = { values ->
                addingBand = false
                val letter = values["letter"].orEmpty().trim()
                val min = values["min"]?.toDoubleOrNull()
                if (letter.isBlank() || min == null) {
                recordBook.announce(context.getString(R.string.classroom_band_invalid))
                } else {
                    recordBook.setGradeScale(
                        classroom.id,
                        classroom.gradeScale + GradeBand(minPercent = min, letter = letter),
                    )
                }
            },
        )
    }

    if (deletingClass) {
        DestructiveConfirmDialog(
            title = stringResource(R.string.classroom_delete_named, classroom.name),
            body = pluralStringResource(
                R.plurals.classroom_delete_class_body,
                classroom.students.size,
                classroom.students.size,
            ),
            confirmLabel = stringResource(R.string.classroom_delete_class),
            onConfirm = {
                deletingClass = false
                recordBook.deleteClass(classroom.id)
            },
            onDismiss = { deletingClass = false },
        )
    }
}
