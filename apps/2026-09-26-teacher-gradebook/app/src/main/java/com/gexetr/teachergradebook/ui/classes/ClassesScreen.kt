package com.gexetr.teachergradebook.ui.classes

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gexetr.teachergradebook.R
import com.gexetr.teachergradebook.domain.FreeTier
import com.gexetr.teachergradebook.domain.GradeMath
import com.gexetr.teachergradebook.domain.model.Classroom
import com.gexetr.teachergradebook.ui.components.ClickablePanel
import com.gexetr.teachergradebook.ui.components.DestructiveConfirmDialog
import com.gexetr.teachergradebook.ui.components.EmptyState
import com.gexetr.teachergradebook.ui.components.HeroPanel
import com.gexetr.teachergradebook.ui.components.HeroStat
import com.gexetr.teachergradebook.ui.components.Mark
import com.gexetr.teachergradebook.ui.components.Panel
import com.gexetr.teachergradebook.ui.components.ScreenGutter
import com.gexetr.teachergradebook.ui.components.ScreenShell
import com.gexetr.teachergradebook.ui.components.SectionMark
import com.gexetr.teachergradebook.ui.components.StatTile
import com.gexetr.teachergradebook.ui.components.StatusPill
import com.gexetr.teachergradebook.ui.components.TextInputDialog
import com.gexetr.teachergradebook.ui.components.Tone
import com.gexetr.teachergradebook.ui.recordbook.RecordBookUiState
import java.util.Locale

private const val SubtitleSeparator = " · "

@Composable
fun ClassesScreen(
    state: RecordBookUiState,
    unlocked: Boolean,
    onOpenClass: (String) -> Unit,
    onOpenAbout: () -> Unit,
    onShowUnlock: () -> Unit,
    onAddClass: (String) -> Unit,
    onRenameClass: (String, String) -> Unit,
    onDeleteClass: (String) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Classroom?>(null) }
    var deleting by remember { mutableStateOf<Classroom?>(null) }

    ScreenShell(
        title = stringResource(if (unlocked) R.string.classes_title_unlocked else R.string.classes_title_free),
        actions = {
            TextButton(onClick = onOpenAbout, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.classes_about))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = {
                    Mark(SectionMark.Roster, MaterialTheme.colorScheme.onPrimaryContainer, 20.dp)
                },
                text = { Text(stringResource(R.string.classes_new_class)) },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.loaded -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                state.book.classes.isEmpty() -> EmptyState(
                    mark = SectionMark.Roster,
                    title = stringResource(R.string.classes_empty_title),
                    body = stringResource(R.string.classes_empty_body),
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> ClassesList(
                    state, unlocked, onOpenClass, onShowUnlock,
                    onRename = { renaming = it },
                    onDelete = { deleting = it },
                )
            }
        }
    }

    if (adding) {
        TextInputDialog(
            title = stringResource(R.string.classes_new_class),
            confirmLabel = stringResource(R.string.classes_create),
            label = stringResource(R.string.classes_name_label),
            supporting = if (!unlocked && state.book.classes.size >= FreeTier.MAX_CLASSES) {
                pluralStringResource(
                    R.plurals.classes_free_add_warning,
                    FreeTier.MAX_CLASSES,
                    FreeTier.MAX_CLASSES,
                )
            } else null,
            onDismiss = { adding = false },
            onConfirm = { name -> adding = false; onAddClass(name) },
        )
    }

    renaming?.let { classroom ->
        TextInputDialog(
            title = stringResource(R.string.classes_rename_title),
            confirmLabel = stringResource(R.string.classes_save),
            label = stringResource(R.string.classes_name_label),
            initial = classroom.name,
            onDismiss = { renaming = null },
            onConfirm = { name -> renaming = null; onRenameClass(classroom.id, name) },
        )
    }

    deleting?.let { classroom ->
        val students = pluralStringResource(R.plurals.classes_student_count, classroom.students.size, classroom.students.size)
        val assignments = pluralStringResource(R.plurals.classes_assignment_count, classroom.assignments.size, classroom.assignments.size)
        val grades = pluralStringResource(R.plurals.classes_grade_count, classroom.grades.size, classroom.grades.size)
        val attendance = pluralStringResource(
            R.plurals.classes_attendance_count,
            classroom.attendance.size,
            classroom.attendance.size,
        )
        val notes = pluralStringResource(R.plurals.classes_note_count, classroom.notes.size, classroom.notes.size)
        val contacts = pluralStringResource(R.plurals.classes_contact_count, classroom.contacts.size, classroom.contacts.size)
        DestructiveConfirmDialog(
            title = stringResource(R.string.classes_delete_title, classroom.name),
            body = stringResource(
                R.string.classes_delete_body,
                students,
                assignments,
                grades,
                attendance,
                notes,
                contacts,
            ),
            confirmLabel = stringResource(R.string.classes_delete_confirm),
            onConfirm = { deleting = null; onDeleteClass(classroom.id) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun ClassesList(
    state: RecordBookUiState,
    unlocked: Boolean,
    onOpenClass: (String) -> Unit,
    onShowUnlock: () -> Unit,
    onRename: (Classroom) -> Unit,
    onDelete: (Classroom) -> Unit,
) {
    val classrooms = state.book.classes
    val averages = classrooms.mapNotNull(GradeMath::classAverage)
    val overall = averages.takeIf { it.isNotEmpty() }?.average()
    val overallText = overall?.let {
        stringResource(R.string.classes_percent_format, String.format(Locale.getDefault(), "%.1f", it))
    } ?: stringResource(R.string.classes_not_enough_data)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroPanel(
                eyebrow = stringResource(R.string.classes_hero_eyebrow),
                headline = stringResource(R.string.classes_hero_headline),
                supporting = stringResource(R.string.classes_hero_supporting),
                stats = listOf(
                    HeroStat(stringResource(R.string.classes_stat_classes), classrooms.size.toString()),
                    HeroStat(stringResource(R.string.classes_stat_students), classrooms.sumOf { it.students.size }.toString()),
                    HeroStat(stringResource(R.string.classes_stat_overall_average), overallText),
                ),
            )
        }
        if (!unlocked) item { LimitCard(onShowUnlock, classrooms.size) }
        items(classrooms, key = { it.id }) { classroom ->
            ClassRow(classroom, { onOpenClass(classroom.id) }, { onRename(classroom) }, { onDelete(classroom) })
        }
        item {
            Text(
                stringResource(R.string.classes_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ClassRow(classroom: Classroom, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val average = GradeMath.classAverage(classroom)
    val subtitle = listOfNotNull(
        classroom.subject.takeIf(String::isNotBlank),
        classroom.period.takeIf(String::isNotBlank)?.let { stringResource(R.string.classes_period_format, it) },
        classroom.term.takeIf(String::isNotBlank),
    ).joinToString(SubtitleSeparator)

    ClickablePanel(onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(classroom.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle.isNotBlank()) Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        pluralStringResource(R.plurals.classes_student_count, classroom.students.size, classroom.students.size),
                        Tone.Neutral,
                    )
                    StatusPill(
                        pluralStringResource(R.plurals.classes_assignment_count, classroom.assignments.size, classroom.assignments.size),
                        Tone.Neutral,
                    )
                }
                StatusPill(
                    text = stringResource(R.string.classes_average_value, average?.let {
                        val decimals = classroom.rounding.decimals.coerceIn(0, 2)
                        val number = String.format(Locale.getDefault(), "%.${decimals}f", it)
                        stringResource(R.string.classes_percent_format, number)
                    } ?: stringResource(R.string.classes_not_enough_data)),
                    tone = if (average == null) Tone.Attention else Tone.Brand,
                )
            }
            Box {
                TextButton(onClick = { menuOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.classes_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.classes_rename)) },
                        onClick = { menuOpen = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.classes_delete)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun LimitCard(onShowUnlock: () -> Unit, usedClasses: Int) {
    val classAllowance = pluralStringResource(
        R.plurals.classes_free_allowance,
        FreeTier.MAX_CLASSES,
        FreeTier.MAX_CLASSES,
    )
    val rosterAllowance = pluralStringResource(
        R.plurals.classes_free_roster,
        FreeTier.MAX_STUDENTS_PER_CLASS,
        FreeTier.MAX_STUDENTS_PER_CLASS,
    )
    val rosterTileValue = pluralStringResource(
        R.plurals.classes_free_roster_tile,
        FreeTier.MAX_STUDENTS_PER_CLASS,
        FreeTier.MAX_STUDENTS_PER_CLASS,
    )
    Panel(tone = Tone.Brand) {
        Text(stringResource(R.string.classes_free_title), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                stringResource(R.string.classes_free_allowance_label),
                classAllowance,
                Modifier.weight(1f),
                Tone.Brand,
            )
            StatTile(
                stringResource(R.string.classes_free_roster_label),
                rosterTileValue,
                Modifier.weight(1f),
                Tone.Brand,
            )
        }
        Text(
            stringResource(
                if (usedClasses >= FreeTier.MAX_CLASSES) R.string.classes_free_spent else R.string.classes_free_available,
                classAllowance,
                rosterAllowance,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(stringResource(R.string.classes_unlock_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.classes_unlock_body), style = MaterialTheme.typography.bodySmall)
        Button(onClick = onShowUnlock, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.classes_unlock_action))
        }
    }
}
