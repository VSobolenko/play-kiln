package com.gexetr.teachergradebook.ui.classroom

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.gexetr.teachergradebook.domain.model.Assignment
import com.gexetr.teachergradebook.domain.model.Category
import com.gexetr.teachergradebook.domain.model.Classroom
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.ui.components.DestructiveConfirmDialog
import com.gexetr.teachergradebook.ui.components.EmptyHint
import com.gexetr.teachergradebook.ui.components.GradeDisplay
import com.gexetr.teachergradebook.ui.components.HeroPanel
import com.gexetr.teachergradebook.ui.components.HeroStat
import com.gexetr.teachergradebook.ui.components.Panel
import com.gexetr.teachergradebook.ui.components.ScreenGutter
import com.gexetr.teachergradebook.ui.components.SectionMark
import com.gexetr.teachergradebook.ui.components.SectionStrip
import com.gexetr.teachergradebook.ui.components.SectionCard
import com.gexetr.teachergradebook.ui.components.StatTile
import com.gexetr.teachergradebook.ui.components.StatusPill
import com.gexetr.teachergradebook.ui.components.Tone
import com.gexetr.teachergradebook.ui.components.TwoFieldDialog
import com.gexetr.teachergradebook.ui.components.trim
import com.gexetr.teachergradebook.ui.recordbook.RecordBookUiState
import com.gexetr.teachergradebook.ui.recordbook.RecordBookViewModel
import java.util.Locale

/** The parts of one class. Order is the order a term runs in: roster, then marks, then admin. */
enum class ClassTab(val mark: SectionMark) {
    ROSTER(SectionMark.Roster),
    GRADES(SectionMark.Grades),
    ATTENDANCE(SectionMark.Attendance),
    NOTES(SectionMark.Notes),
    SEATING(SectionMark.Seating),
    REPORTS(SectionMark.Reports),
    SETUP(SectionMark.Setup),
}

@Composable
private fun classTabLabel(tab: ClassTab): String = stringResource(
    when (tab) {
        ClassTab.ROSTER -> R.string.classroom_tab_roster
        ClassTab.GRADES -> R.string.classroom_tab_grades
        ClassTab.ATTENDANCE -> R.string.classroom_tab_attendance
        ClassTab.NOTES -> R.string.classroom_tab_notes
        ClassTab.SEATING -> R.string.classroom_tab_seating
        ClassTab.REPORTS -> R.string.classroom_tab_reports
        ClassTab.SETUP -> R.string.classroom_tab_setup
    },
)

/**
 * One class, everything about it.
 *
 * A tab per job rather than one long scroll, because these are different tasks done at different
 * times: marking at a desk, attendance at the door, setup once at the start of term.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScreen(
    classId: String,
    initialTab: ClassTab,
    state: RecordBookUiState,
    unlocked: Boolean,
    onBack: () -> Unit,
    onOpenStudent: (String) -> Unit,
    onOpenGradeSheet: (String) -> Unit,
    recordBook: RecordBookViewModel,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onImportRoster: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val classroom = state.book.classes.firstOrNull { it.id == classId }
    var selected by remember { mutableIntStateOf(initialTab.ordinal) }

    if (classroom == null) {
        // A stale back-stack entry is a real case: deleting the open class leaves this destination
        // behind, and saying so is better than an empty screen the teacher cannot explain.
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.classroom_class_title)) },
                    navigationIcon = {
                        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.classroom_back))
                        }
                    },
                )
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenGutter)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.classroom_missing_title), style = MaterialTheme.typography.titleMedium)
                    EmptyHint(
                        stringResource(R.string.classroom_missing_body),
                    )
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.classroom_back_to_classes))
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(classroom.name, maxLines = 1) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.classroom_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val tabs = ClassTab.entries
            SectionStrip(
                sections = tabs.map { it.mark to classTabLabel(it) },
                selected = tabs[selected].mark,
                onSelect = { mark -> selected = tabs.indexOfFirst { it.mark == mark } },
            )

            when (ClassTab.entries[selected]) {
                ClassTab.ROSTER -> RosterTab(
                    classroom = classroom,
                    unlocked = unlocked,
                    recordBook = recordBook,
                    onOpenStudent = onOpenStudent,
                    onImportRoster = onImportRoster,
                )

                ClassTab.GRADES -> GradesTab(
                    classroom = classroom,
                    unlocked = unlocked,
                    recordBook = recordBook,
                    onOpenGradeSheet = onOpenGradeSheet,
                )

                ClassTab.ATTENDANCE -> AttendanceTab(classroom = classroom, recordBook = recordBook)
                ClassTab.NOTES -> NotesTab(classroom = classroom, recordBook = recordBook)
                ClassTab.SEATING -> SeatingTab(classroom = classroom, recordBook = recordBook)
                ClassTab.REPORTS -> ReportsTab(
                    classroom = classroom,
                    unlocked = unlocked,
                    onExportCsv = onExportCsv,
                    onExportPdf = onExportPdf,
                    onBackup = onBackup,
                    onRestore = onRestore,
                )

                ClassTab.SETUP -> SetupTab(classroom = classroom, recordBook = recordBook)
            }
        }
    }
}

@Composable
private fun RosterTab(
    classroom: Classroom,
    unlocked: Boolean,
    recordBook: RecordBookViewModel,
    onOpenStudent: (String) -> Unit,
    onImportRoster: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeroPanel(
                eyebrow = stringResource(R.string.classroom_roster_eyebrow),
                headline = stringResource(R.string.classroom_roster_headline),
                supporting = stringResource(R.string.classroom_roster_supporting),
                stats = listOf(
                    HeroStat(stringResource(R.string.classroom_students_label), classroom.students.size.toString()),
                    HeroStat(
                        stringResource(R.string.classroom_active_label),
                        classroom.students.count { it.active }.toString(),
                    ),
                ),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { adding = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.classroom_add_student))
                }
                TextButton(onClick = onImportRoster, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.classroom_import_csv))
                }
            }
        }
        if (!unlocked && classroom.students.size >= FreeTier.MAX_STUDENTS_PER_CLASS) {
            item {
                SectionCard(title = stringResource(R.string.classroom_roster_limit_title)) {
                    Text(
                        text = stringResource(
                            R.string.classroom_roster_limit_body,
                            FreeTier.MAX_STUDENTS_PER_CLASS,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (classroom.students.isEmpty()) {
            item {
                EmptyHint(
                    stringResource(R.string.classroom_roster_empty),
                )
            }
        }
        items(classroom.students, key = { it.id }) { student ->
            val average = GradeMath.studentAverage(classroom, student.id)
            val decimals = classroom.rounding.decimals.coerceIn(0, 2)
            val marks = classroom.attendance.count { it.studentId == student.id }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenStudent(student.id) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (student.active) student.displayName else stringResource(
                                R.string.classroom_student_inactive,
                                student.displayName,
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (student.externalId.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.classroom_school_id, student.externalId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val averageText = average.overall?.let {
                            val percent = String.format(Locale.US, "%.${decimals}f%%", it)
                            GradeMath.letterFor(it, classroom.gradeScale)?.let { letter ->
                                stringResource(R.string.classroom_average_with_letter, percent, letter)
                            } ?: percent
                        } ?: stringResource(R.string.classroom_no_average)
                        Text(
                            text = if (marks > 0) {
                                stringResource(
                                    R.string.classroom_student_summary_with_attendance,
                                    averageText,
                                    pluralStringResource(R.plurals.classroom_attendance_marks, marks, marks),
                                )
                            } else {
                                stringResource(R.string.classroom_student_summary, averageText)
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = stringResource(R.string.classroom_open),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    if (adding) {
        TwoFieldDialog(
            title = stringResource(R.string.classroom_add_student),
            confirmLabel = stringResource(R.string.classroom_add),
            firstLabel = stringResource(R.string.classroom_first_name),
            initialFirst = firstName,
            secondLabel = stringResource(R.string.classroom_last_name),
            initialSecond = "",
            secondSupporting = stringResource(R.string.classroom_name_supporting),
            onDismiss = { adding = false },
            onConfirm = { first, last ->
                adding = false
                firstName = ""
                recordBook.addStudent(classroom.id, first, last)
            },
        )
    }
}

@Composable
private fun GradesTab(
    classroom: Classroom,
    unlocked: Boolean,
    recordBook: RecordBookViewModel,
    onOpenGradeSheet: (String) -> Unit,
) {
    var addingCategory by remember { mutableStateOf(false) }
    var addingAssignmentTo by remember { mutableStateOf<Category?>(null) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editingAssignment by remember { mutableStateOf<Assignment?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingAssignment by remember { mutableStateOf<Assignment?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val classAverage = GradeMath.classAverage(classroom)
            val decimals = classroom.rounding.decimals.coerceIn(0, 2)
            val totalCells = classroom.students.size * classroom.assignments.size
            val recordedCells = classroom.grades.map { it.studentId to it.assignmentId }.distinct().size
            val unmarked = (totalCells - recordedCells).coerceAtLeast(0)
            Panel {
                Text(
                    text = stringResource(R.string.classroom_grades_headline),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradeDisplay(
                        percentText = classAverage?.let { String.format(Locale.US, "%.${decimals}f%%", it) }
                            ?: stringResource(R.string.classroom_no_average_short),
                        letter = classAverage?.let { GradeMath.letterFor(it, classroom.gradeScale) },
                        caption = stringResource(R.string.classroom_class_average),
                        modifier = Modifier.weight(1f),
                        tone = if (classAverage == null) Tone.Neutral else Tone.Brand,
                    )
                    StatTile(
                        label = stringResource(R.string.classroom_unmarked_cells),
                        value = unmarked.toString(),
                        modifier = Modifier.weight(1f),
                        tone = if (unmarked > 0) Tone.Attention else Tone.Positive,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(stringResource(R.string.classroom_unmarked), Tone.Attention)
                    Text(
                        stringResource(R.string.classroom_unmarked_legend),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { addingCategory = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.classroom_add_category))
                }
            }
        }
        if (classroom.categories.isEmpty()) {
            item {
                EmptyHint(
                    stringResource(R.string.classroom_categories_empty),
                )
            }
        }
        if (classroom.students.isEmpty()) {
            item {
                EmptyHint(stringResource(R.string.classroom_grades_no_students))
            }
        }
        classroom.categories.forEachIndexed { index, category ->
            item(key = category.id) {
                CategoryCard(
                    classroom = classroom,
                    category = category,
                    isFirst = index == 0,
                    isLast = index == classroom.categories.lastIndex,
                    onMove = { delta -> recordBook.moveCategory(classroom.id, category.id, delta) },
                    onEdit = { editingCategory = category },
                    onDelete = { deletingCategory = category },
                    onAddAssignment = { addingAssignmentTo = category },
                )
            }
            val assignments = classroom.assignments.filter { it.categoryId == category.id }
            if (assignments.isEmpty()) {
                item(key = "${category.id}-none") {
                    Text(
                        text = stringResource(R.string.classroom_assignments_empty, category.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            items(assignments, key = { it.id }) { assignment ->
                AssignmentRow(
                    classroom = classroom,
                    assignment = assignment,
                    onOpen = { onOpenGradeSheet(assignment.id) },
                    onEdit = { editingAssignment = assignment },
                    onDelete = { deletingAssignment = assignment },
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.classroom_grades_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (addingCategory) {
        TwoFieldDialog(
            title = stringResource(R.string.classroom_add_category),
            confirmLabel = stringResource(R.string.classroom_add),
            firstLabel = stringResource(R.string.classroom_category_name),
            initialFirst = "",
            secondLabel = stringResource(R.string.classroom_weight_percent),
            initialSecond = "100",
            secondNumeric = true,
            secondSupporting = if (!unlocked && classroom.categories.size >= FreeTier.MAX_CATEGORIES_PER_CLASS) {
                stringResource(R.string.classroom_category_free_limit)
            } else {
                stringResource(R.string.classroom_weight_help)
            },
            onDismiss = { addingCategory = false },
            onConfirm = { name, weight ->
                addingCategory = false
                recordBook.addCategory(classroom.id, name, weight.toDoubleOrNull() ?: 0.0)
            },
        )
    }

    editingCategory?.let { category ->
        TwoFieldDialog(
            title = stringResource(R.string.classroom_edit_named, category.name),
            confirmLabel = stringResource(R.string.classroom_save),
            firstLabel = stringResource(R.string.classroom_category_name),
            initialFirst = category.name,
            secondLabel = stringResource(R.string.classroom_weight_percent),
            initialSecond = trim(category.weight),
            secondNumeric = true,
            onDismiss = { editingCategory = null },
            onConfirm = { name, weight ->
                editingCategory = null
                recordBook.updateCategory(
                    classroom.id,
                    category.id,
                    name,
                    weight.toDoubleOrNull() ?: 0.0,
                    category.dropLowest,
                )
            },
        )
    }

    addingAssignmentTo?.let { category ->
        TwoFieldDialog(
            title = stringResource(R.string.classroom_new_assignment_in, category.name),
            confirmLabel = stringResource(R.string.classroom_add),
            firstLabel = stringResource(R.string.classroom_assignment_name),
            initialFirst = "",
            secondLabel = stringResource(R.string.classroom_points_worth),
            initialSecond = "100",
            secondNumeric = true,
            secondSupporting = stringResource(R.string.classroom_points_positive),
            onDismiss = { addingAssignmentTo = null },
            onConfirm = { name, points ->
                addingAssignmentTo = null
                recordBook.addAssignment(classroom.id, category.id, name, points.toDoubleOrNull() ?: 0.0)
            },
        )
    }

    editingAssignment?.let { assignment ->
        TwoFieldDialog(
            title = stringResource(R.string.classroom_edit_named, assignment.name),
            confirmLabel = stringResource(R.string.classroom_save),
            firstLabel = stringResource(R.string.classroom_assignment_name),
            initialFirst = assignment.name,
            secondLabel = stringResource(R.string.classroom_points_worth),
            initialSecond = trim(assignment.points),
            secondNumeric = true,
            secondSupporting = stringResource(R.string.classroom_points_change_warning),
            onDismiss = { editingAssignment = null },
            onConfirm = { name, points ->
                editingAssignment = null
                recordBook.updateAssignment(
                    classroom.id,
                    assignment.id,
                    name,
                    points.toDoubleOrNull() ?: 0.0,
                    assignment.categoryId,
                )
            },
        )
    }

    deletingCategory?.let { category ->
        val assignments = classroom.assignments.count { it.categoryId == category.id }
        DestructiveConfirmDialog(
            title = stringResource(R.string.classroom_delete_named, category.name),
            body = pluralStringResource(
                R.plurals.classroom_delete_category_body,
                assignments,
                assignments,
            ),
            confirmLabel = stringResource(R.string.classroom_delete_category),
            onConfirm = {
                deletingCategory = null
                recordBook.deleteCategory(classroom.id, category.id)
            },
            onDismiss = { deletingCategory = null },
        )
    }

    deletingAssignment?.let { assignment ->
        val marks = classroom.grades.count { it.assignmentId == assignment.id }
        DestructiveConfirmDialog(
            title = stringResource(R.string.classroom_delete_named, assignment.name),
            body = pluralStringResource(R.plurals.classroom_delete_assignment_body, marks, marks),
            confirmLabel = stringResource(R.string.classroom_delete_assignment),
            onConfirm = {
                deletingAssignment = null
                recordBook.deleteAssignment(classroom.id, assignment.id)
            },
            onDismiss = { deletingAssignment = null },
        )
    }
}

@Composable
private fun CategoryCard(
    classroom: Classroom,
    category: Category,
    isFirst: Boolean,
    isLast: Boolean,
    onMove: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddAssignment: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (category.dropLowest > 0) {
                        stringResource(
                            R.string.classroom_category_weight_drop,
                            trim(category.weight),
                            category.dropLowest,
                        )
                    } else {
                        stringResource(R.string.classroom_category_weight, trim(category.weight))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onMove(-1) }, enabled = !isFirst, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.classroom_up))
            }
            TextButton(onClick = { onMove(1) }, enabled = !isLast, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.classroom_down))
            }
            Box {
                TextButton(onClick = { menuOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.classroom_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.classroom_add_assignment)) },
                        onClick = {
                            menuOpen = false
                            onAddAssignment()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.classroom_edit_name_weight)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.classroom_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AssignmentRow(
    classroom: Classroom,
    assignment: Assignment,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val marks = classroom.grades.count { it.assignmentId == assignment.id }
    val scored = classroom.grades.count {
        it.assignmentId == assignment.id && it.kind == GradeKind.SCORE
    }
    val missing = classroom.grades.count {
        it.assignmentId == assignment.id && it.kind == GradeKind.MISSING
    }
    val exempt = classroom.grades.count {
        it.assignmentId == assignment.id && it.kind == GradeKind.EXEMPT
    }

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = assignment.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.classroom_assignment_points, trim(assignment.points)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusPill(
                        pluralStringResource(
                            R.plurals.classroom_marked_count,
                            scored,
                            scored,
                            classroom.students.size,
                        ),
                        Tone.Positive,
                    )
                    if (missing > 0) {
                        StatusPill(
                            pluralStringResource(R.plurals.classroom_missing_count, missing, missing),
                            Tone.Attention,
                        )
                    }
                    if (exempt > 0) {
                        StatusPill(
                            pluralStringResource(R.plurals.classroom_exempt_count, exempt, exempt),
                            Tone.Neutral,
                        )
                    }
                    if (marks == 0) {
                        StatusPill(stringResource(R.string.classroom_unmarked), Tone.Attention)
                    }
                }
            }
            Box {
                TextButton(onClick = { menuOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.classroom_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.classroom_mark_class)) },
                        onClick = {
                            menuOpen = false
                            onOpen()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.classroom_edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.classroom_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
