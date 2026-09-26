package com.gexetr.teachergradebook.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.gexetr.teachergradebook.R
import com.gexetr.teachergradebook.data.DocumentFailure
import com.gexetr.teachergradebook.data.DocumentStoreException
import com.gexetr.teachergradebook.domain.BackupRejection
import com.gexetr.teachergradebook.domain.CsvLabels
import com.gexetr.teachergradebook.domain.EditRejection
import com.gexetr.teachergradebook.domain.RosterSkipReason
import com.gexetr.teachergradebook.domain.model.RoundMode
import com.gexetr.teachergradebook.domain.model.RoundingRule

@PluralsRes
private fun roundingPlural(mode: RoundMode): Int = when (mode) {
    RoundMode.HALF_UP -> R.plurals.rounding_half_up
    RoundMode.HALF_EVEN -> R.plurals.rounding_half_even
    RoundMode.FLOOR -> R.plurals.rounding_floor
}

@Composable
fun roundingRuleText(rule: RoundingRule): String =
    pluralStringResource(roundingPlural(rule.mode), rule.decimals, rule.decimals)

fun roundingRuleText(context: Context, rule: RoundingRule): String =
    context.resources.getQuantityString(roundingPlural(rule.mode), rule.decimals, rule.decimals)

fun editRejectionText(context: Context, reason: EditRejection): String = when (reason) {
    EditRejection.ClassNeedsName -> context.getString(R.string.edit_class_needs_name)
    EditRejection.ClassGone -> context.getString(R.string.edit_class_gone)
    EditRejection.StudentNeedsName -> context.getString(R.string.edit_student_needs_name)
    EditRejection.StudentGone -> context.getString(R.string.edit_student_gone)
    EditRejection.CategoryNeedsName -> context.getString(R.string.edit_category_needs_name)
    EditRejection.CategoryWeightNotPositive -> context.getString(R.string.edit_category_weight_not_positive)
    EditRejection.DropLowestNegative -> context.getString(R.string.edit_drop_lowest_negative)
    EditRejection.CategoryGone -> context.getString(R.string.edit_category_gone)
    EditRejection.CategoryAlreadyAtEnd -> context.getString(R.string.edit_category_already_at_end)
    EditRejection.AssignmentNeedsName -> context.getString(R.string.edit_assignment_needs_name)
    EditRejection.AssignmentPointsNotPositive -> context.getString(R.string.edit_assignment_points_not_positive)
    EditRejection.AssignmentGone -> context.getString(R.string.edit_assignment_gone)
    EditRejection.EarnedPointsNegative -> context.getString(R.string.edit_earned_points_negative)
    is EditRejection.EarnedPointsAboveMaximum -> context.getString(
        R.string.edit_earned_points_above_maximum,
        trimNumber(reason.maximum),
    )
    EditRejection.NoteNeedsText -> context.getString(R.string.edit_note_needs_text)
    EditRejection.NoteGone -> context.getString(R.string.edit_note_gone)
    EditRejection.ContactNeedsChannel -> context.getString(R.string.edit_contact_needs_channel)
    EditRejection.ContactNeedsSummary -> context.getString(R.string.edit_contact_needs_summary)
    EditRejection.ContactGone -> context.getString(R.string.edit_contact_gone)
    EditRejection.SeatingGridNegative -> context.getString(R.string.edit_seating_grid_negative)
    EditRejection.SeatingGridTooLarge -> context.getString(R.string.edit_seating_grid_too_large)
    EditRejection.DeskOutsideGrid -> context.getString(R.string.edit_desk_outside_grid)
    EditRejection.SeatingGridFull -> context.getString(R.string.edit_seating_grid_full)
}

fun backupRejectionText(context: Context, reason: BackupRejection): String = when (reason) {
    BackupRejection.Empty -> context.getString(R.string.backup_empty)
    BackupRejection.NotMarkBookBackup -> context.getString(R.string.backup_not_mark_book)
    BackupRejection.MissingSchemaVersion -> context.getString(R.string.backup_missing_schema)
    is BackupRejection.UnsupportedVersion -> context.getString(
        R.string.backup_unsupported_version,
        reason.fileVersion,
        reason.supportedVersion,
    )
    is BackupRejection.Damaged -> context.getString(
        R.string.backup_damaged,
        reason.detail?.takeIf { it.isNotBlank() } ?: context.getString(R.string.app_error_unknown_reason),
    )
}

fun csvLabels(context: Context, rule: RoundingRule): CsvLabels = CsvLabels(
    student = context.getString(R.string.csv_student),
    externalId = context.getString(R.string.csv_external_id),
    overallPercent = context.getString(R.string.csv_overall_percent),
    letter = context.getString(R.string.csv_letter),
    missingCountedAsZero = context.getString(R.string.csv_missing_counted_as_zero),
    rounding = context.getString(R.string.csv_rounding),
    missing = context.getString(R.string.csv_missing),
    exempt = context.getString(R.string.csv_exempt),
    yes = context.getString(R.string.csv_yes),
    no = context.getString(R.string.csv_no),
    roundingRule = roundingRuleText(context, rule),
)

fun rosterSkipReasonText(context: Context, reason: RosterSkipReason): String = when (reason) {
    RosterSkipReason.NoNameInFirstColumn -> context.getString(R.string.import_no_name_first_column)
}

fun documentFailureText(context: Context, throwable: Throwable): String? =
    (throwable as? DocumentStoreException)?.let { failure ->
        when (failure.reason) {
            DocumentFailure.CouldNotOpen -> context.getString(R.string.document_could_not_open)
            DocumentFailure.CouldNotOpenForWriting -> context.getString(R.string.document_could_not_open_for_writing)
        }
    }

private fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
