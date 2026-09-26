package com.gexetr.teachergradebook.ui.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.teachergradebook.R
import com.gexetr.teachergradebook.domain.GradeMath
import com.gexetr.teachergradebook.domain.model.AttendanceStatus
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.domain.model.NoteKind
import com.gexetr.teachergradebook.ui.components.DestructiveConfirmDialog
import com.gexetr.teachergradebook.ui.components.EmptyState
import com.gexetr.teachergradebook.ui.components.GradeDisplay
import com.gexetr.teachergradebook.ui.components.GradeEntryDialog
import com.gexetr.teachergradebook.ui.components.HeroPanel
import com.gexetr.teachergradebook.ui.components.HeroStat
import com.gexetr.teachergradebook.ui.components.Panel
import com.gexetr.teachergradebook.ui.components.ScreenGutter
import com.gexetr.teachergradebook.ui.components.ScreenShell
import com.gexetr.teachergradebook.ui.components.SectionMark
import com.gexetr.teachergradebook.ui.components.StatTile
import com.gexetr.teachergradebook.ui.components.StatusPill
import com.gexetr.teachergradebook.ui.components.Tone
import com.gexetr.teachergradebook.ui.components.trim
import com.gexetr.teachergradebook.ui.recordbook.RecordBookUiState
import com.gexetr.teachergradebook.ui.recordbook.RecordBookViewModel
import java.text.NumberFormat

@Composable
fun StudentScreen(
    classId: String,
    studentId: String,
    state: RecordBookUiState,
    onBack: () -> Unit,
    recordBook: RecordBookViewModel,
    onExportPdf: () -> Unit,
) {
    val classroom = state.book.classes.firstOrNull { it.id == classId }
    val student = classroom?.students?.firstOrNull { it.id == studentId }
    var deletingStudent by remember { mutableStateOf(false) }
    var editingGrade by remember { mutableStateOf<String?>(null) }
    var editingStudent by remember { mutableStateOf(false) }

    ScreenShell(
        title = student?.displayName ?: stringResource(R.string.student_fallback_title),
        subtitle = classroom?.name,
        onBack = onBack,
        actions = {
            if (student != null) {
                TextButton(onClick = { editingStudent = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.records_edit_student))
                }
                TextButton(onClick = onExportPdf, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.student_report))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (classroom == null || student == null) {
                EmptyState(
                    mark = SectionMark.Roster,
                    title = stringResource(R.string.student_missing_title),
                    body = stringResource(R.string.student_missing_body),
                    modifier = Modifier.padding(ScreenGutter),
                )
                return@Box
            }

            val average = GradeMath.studentAverage(classroom, student.id)
            val cells = classroom.grades.filter { it.studentId == student.id }
            val gradedCount = cells.count { it.kind == GradeKind.SCORE }
            val ungradedCount = classroom.assignments.size - cells.size
            val attendance = classroom.attendance.filter { it.studentId == student.id }
            val present = attendance.count { it.status == AttendanceStatus.PRESENT }
            val late = attendance.count { it.status == AttendanceStatus.TARDY }
            val absent = attendance.count {
                it.status == AttendanceStatus.ABSENT_EXCUSED || it.status == AttendanceStatus.ABSENT_UNEXCUSED
            }
            val attendanceRate = if (attendance.isEmpty()) null else (present + late).toDouble() / attendance.size * 100.0
            val percentFormat = NumberFormat.getNumberInstance().apply {
                minimumFractionDigits = average.rounding.decimals.coerceIn(0, 2)
                maximumFractionDigits = average.rounding.decimals.coerceIn(0, 2)
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    HeroPanel(
                        eyebrow = stringResource(R.string.student_overview),
                        headline = student.displayName,
                        stats = listOf(
                            HeroStat(stringResource(R.string.student_attendance_rate), attendanceRate?.let { "${percentFormat.format(it)}%" } ?: "—"),
                            HeroStat(stringResource(R.string.student_graded), gradedCount.toString()),
                            HeroStat(stringResource(R.string.student_ungraded), ungradedCount.toString()),
                        ),
                    )
                    StatusPill(
                        text = stringResource(if (student.active) R.string.records_status_active else R.string.records_status_inactive),
                        tone = if (student.active) Tone.Positive else Tone.Neutral,
                    )
                }
                item {
                    val overall = average.overall
                    GradeDisplay(
                        percentText = overall?.let { "${percentFormat.format(GradeMath.round(it, average.rounding))}%" }
                            ?: stringResource(R.string.student_average_unavailable),
                        letter = overall?.let { GradeMath.letterFor(it, classroom.gradeScale) },
                        caption = if (overall == null) stringResource(R.string.student_average_unavailable_caption)
                        else pluralStringResource(R.plurals.student_average_item_count, gradedCount, gradedCount),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item { Text(stringResource(R.string.student_marks), style = MaterialTheme.typography.titleMedium) }
                if (classroom.assignments.isEmpty()) item {
                    EmptyState(SectionMark.Grades, stringResource(R.string.student_marks), stringResource(R.string.student_no_assignments))
                }
                classroom.assignments.forEach { assignment ->
                    item(key = assignment.id) {
                        val cell = cells.firstOrNull { it.assignmentId == assignment.id }
                        val category = classroom.categories.firstOrNull { it.id == assignment.categoryId }
                        val percent = GradeMath.assignmentPercent(cell, assignment, classroom.missingCountsAsZero)
                        Panel(Modifier.clickable { editingGrade = assignment.id }) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(assignment.name, style = MaterialTheme.typography.titleSmall)
                                    Text(category?.name ?: stringResource(R.string.student_no_category), style = MaterialTheme.typography.bodySmall)
                                    if (cell?.kind == GradeKind.SCORE) Text(
                                        stringResource(R.string.student_score_format, trim(cell.points), trim(assignment.points)),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    percent?.let { Text(stringResource(R.string.student_percent_format, percentFormat.format(it))) }
                                    when (cell?.kind) {
                                        GradeKind.MISSING -> StatusPill(stringResource(R.string.student_missing), Tone.Attention)
                                        GradeKind.EXEMPT -> StatusPill(stringResource(R.string.student_exempt), Tone.Neutral)
                                        null -> StatusPill(stringResource(R.string.student_unmarked), Tone.Attention)
                                        GradeKind.SCORE -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
                item { Text(stringResource(R.string.student_attendance), style = MaterialTheme.typography.titleMedium) }
                item {
                    if (attendance.isEmpty()) EmptyState(SectionMark.Attendance, stringResource(R.string.student_attendance), stringResource(R.string.student_no_attendance))
                    else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(stringResource(R.string.student_present), present.toString(), Modifier.weight(1f), Tone.Positive)
                        StatTile(stringResource(R.string.student_late), late.toString(), Modifier.weight(1f), Tone.Attention)
                        StatTile(stringResource(R.string.student_absent), absent.toString(), Modifier.weight(1f), Tone.Danger)
                    }
                }
                item { Text(stringResource(R.string.student_notes), style = MaterialTheme.typography.titleMedium) }
                item {
                    val notes = classroom.notes.filter { it.studentId == student.id }.sortedByDescending { it.date }
                    if (notes.isEmpty()) EmptyState(SectionMark.Notes, stringResource(R.string.student_notes), stringResource(R.string.student_no_notes))
                    else Panel { notes.forEach { note ->
                        Text(stringResource(R.string.student_dated_kind, note.date, noteKind(note.kind)), style = MaterialTheme.typography.labelMedium)
                        Text(note.text)
                    } }
                }
                item { Text(stringResource(R.string.student_contacts), style = MaterialTheme.typography.titleMedium) }
                item {
                    val contacts = classroom.contacts.filter { it.studentId == student.id }.sortedByDescending { it.date }
                    if (contacts.isEmpty()) EmptyState(SectionMark.Notes, stringResource(R.string.student_contacts), stringResource(R.string.student_no_contacts))
                    else Panel { contacts.forEach { contact ->
                        Text(stringResource(R.string.student_dated_kind, contact.date, contact.channel), style = MaterialTheme.typography.labelMedium)
                        Text(contact.summary)
                        contact.followUpOn?.let { Text(stringResource(R.string.student_follow_up, it), style = MaterialTheme.typography.bodySmall) }
                    } }
                }
                item {
                    Panel(tone = Tone.Danger) {
                        Text(stringResource(R.string.student_remove_section), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.student_remove_explanation))
                        TextButton(onClick = { deletingStudent = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.student_remove_action, student.displayName))
                        }
                    }
                }
            }
        }
    }

    if (editingStudent && student != null) {
        StudentEditorDialog(
            firstName = student.firstName,
            lastName = student.lastName,
            externalId = student.externalId,
            active = student.active,
            onDismiss = { editingStudent = false },
            onSave = { first, last, external, active ->
                recordBook.updateStudent(classId, student.id, first, last, external, active)
                editingStudent = false
            },
        )
    }

    if (deletingStudent && classroom != null && student != null) {
        val marks = classroom.grades.count { it.studentId == student.id }
        DestructiveConfirmDialog(
            title = stringResource(R.string.student_remove_title, student.displayName),
            body = pluralStringResource(R.plurals.student_remove_body, marks, marks),
            confirmLabel = stringResource(R.string.student_remove_confirm),
            onConfirm = { deletingStudent = false; recordBook.deleteStudent(classId, student.id); onBack() },
            onDismiss = { deletingStudent = false },
        )
    }

    editingGrade?.let { assignmentId ->
        if (classroom == null || student == null) return@let
        val assignment = classroom.assignments.firstOrNull { it.id == assignmentId } ?: return@let
        val existing = classroom.grades.firstOrNull { it.studentId == student.id && it.assignmentId == assignment.id }
        GradeEntryDialog(
            assignment.name, assignment.points, existing,
            onDismiss = { editingGrade = null },
            onSet = { kind, points -> editingGrade = null; recordBook.setGrade(classId, student.id, assignment.id, kind, points) },
            onClear = { editingGrade = null; recordBook.clearGrade(classId, student.id, assignment.id) },
        )
    }
}

@Composable
private fun StudentEditorDialog(
    firstName: String,
    lastName: String,
    externalId: String,
    active: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit,
) {
    var first by remember { mutableStateOf(firstName) }
    var last by remember { mutableStateOf(lastName) }
    var external by remember { mutableStateOf(externalId) }
    var isActive by remember { mutableStateOf(active) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.records_edit_student_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(first, { first = it }, label = { Text(stringResource(R.string.records_first_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
                OutlinedTextField(last, { last = it }, label = { Text(stringResource(R.string.records_last_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
                OutlinedTextField(external, { external = it }, label = { Text(stringResource(R.string.records_external_id)) }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.records_active_student), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.records_active_help), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
                StatusPill(stringResource(if (isActive) R.string.records_status_active else R.string.records_status_inactive), if (isActive) Tone.Positive else Tone.Neutral)
                Text(stringResource(R.string.records_student_change_help), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton({ onSave(first.trim(), last.trim(), external.trim(), isActive) }, enabled = first.isNotBlank() || last.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.records_save_student)) } },
        dismissButton = { TextButton(onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun noteKind(kind: NoteKind): String = stringResource(when (kind) {
    NoteKind.PARTICIPATION -> R.string.student_note_participation
    NoteKind.BEHAVIOUR -> R.string.student_note_behaviour
    NoteKind.MISSING_WORK -> R.string.student_note_missing_work
    NoteKind.GENERAL -> R.string.student_note_general
})
