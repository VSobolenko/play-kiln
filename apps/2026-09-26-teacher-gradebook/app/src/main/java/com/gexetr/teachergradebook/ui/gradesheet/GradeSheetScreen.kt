package com.gexetr.teachergradebook.ui.gradesheet

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.teachergradebook.R
import com.gexetr.teachergradebook.ui.components.EmptyHint
import com.gexetr.teachergradebook.ui.components.GradeCellLabel
import com.gexetr.teachergradebook.ui.components.GradeEntryDialog
import com.gexetr.teachergradebook.ui.components.HeroPanel
import com.gexetr.teachergradebook.ui.components.HeroStat
import com.gexetr.teachergradebook.ui.components.Panel
import com.gexetr.teachergradebook.ui.components.ScreenGutter
import com.gexetr.teachergradebook.ui.components.ScreenShell
import com.gexetr.teachergradebook.ui.components.StatusPill
import com.gexetr.teachergradebook.ui.components.Tone
import com.gexetr.teachergradebook.ui.components.trim
import com.gexetr.teachergradebook.ui.recordbook.RecordBookUiState
import com.gexetr.teachergradebook.ui.recordbook.RecordBookViewModel

@Composable
fun GradeSheetScreen(
    classId: String,
    assignmentId: String,
    state: RecordBookUiState,
    onBack: () -> Unit,
    recordBook: RecordBookViewModel,
) {
    val classroom = state.book.classes.firstOrNull { it.id == classId }
    val assignment = classroom?.assignments?.firstOrNull { it.id == assignmentId }
    val category = classroom?.categories?.firstOrNull { it.id == assignment?.categoryId }
    var unmarkedOnly by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<String?>(null) }

    ScreenShell(
        title = assignment?.name ?: stringResource(R.string.grades_assignment),
        subtitle = classroom?.name,
        onBack = onBack,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (classroom == null || assignment == null) {
                Column(Modifier.padding(ScreenGutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.grades_assignment_gone), style = MaterialTheme.typography.titleMedium)
                    EmptyHint(stringResource(R.string.grades_assignment_gone_body))
                }
                return@Box
            }

            val recorded = classroom.grades.filter { it.assignmentId == assignment.id }
            val visible = if (unmarkedOnly) classroom.students.filter { student ->
                recorded.none { it.studentId == student.id }
            } else classroom.students

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    HeroPanel(
                        eyebrow = stringResource(R.string.grades_entry_eyebrow),
                        headline = stringResource(R.string.grades_points_max, trim(assignment.points)),
                        supporting = category?.let { chosen ->
                            stringResource(
                                R.string.grades_assignment_context,
                                chosen.name,
                                trim(chosen.weight),
                            )
                        } ?: stringResource(R.string.grades_no_category),
                        stats = listOf(
                            HeroStat(stringResource(R.string.grades_marked), recorded.size.toString()),
                            HeroStat(stringResource(R.string.grades_unmarked), (classroom.students.size - recorded.size).toString()),
                        ),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !unmarkedOnly,
                            onClick = { unmarkedOnly = false },
                            label = { Text(stringResource(R.string.grades_everyone)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                        FilterChip(
                            selected = unmarkedOnly,
                            onClick = { unmarkedOnly = true },
                            label = { Text(stringResource(R.string.grades_still_to_mark)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                if (classroom.students.isEmpty()) item { EmptyHint(stringResource(R.string.grades_no_students)) }
                if (unmarkedOnly && visible.isEmpty() && classroom.students.isNotEmpty()) {
                    item { EmptyHint(stringResource(R.string.grades_all_recorded)) }
                }
                items(visible, key = { it.id }) { student ->
                    val cell = recorded.firstOrNull { it.studentId == student.id }
                    Panel(Modifier.fillMaxWidth().clickable { editingStudent = student.id }) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(student.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            if (cell == null) StatusPill(stringResource(R.string.grades_unmarked), Tone.Attention)
                            else GradeCellLabel(cell, assignment.points)
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.grades_entry_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    editingStudent?.let { studentId ->
        if (classroom == null || assignment == null) return@let
        val existing = classroom.grades.firstOrNull { it.studentId == studentId && it.assignmentId == assignment.id }
        GradeEntryDialog(
            assignmentName = classroom.students.firstOrNull { it.id == studentId }?.displayName ?: assignment.name,
            assignmentPoints = assignment.points,
            existing = existing,
            onDismiss = { editingStudent = null },
            onSet = { kind, points ->
                editingStudent = null
                recordBook.setGrade(classId, studentId, assignment.id, kind, points)
            },
            onClear = {
                editingStudent = null
                recordBook.clearGrade(classId, studentId, assignment.id)
            },
        )
    }
}
