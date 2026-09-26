package com.gexetr.teachergradebook.domain

import com.gexetr.teachergradebook.domain.model.Assignment
import com.gexetr.teachergradebook.domain.model.Category
import com.gexetr.teachergradebook.domain.model.Classroom
import com.gexetr.teachergradebook.domain.model.GradeBand
import com.gexetr.teachergradebook.domain.model.GradeCell
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.domain.model.RoundMode
import com.gexetr.teachergradebook.domain.model.RoundingRule
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The running average, specified so that no number is ever invented.
 *
 * The contract, in the order the app applies it:
 *
 * 1. per-assignment percent = earned points ÷ the assignment's points; a non-positive point value
 *    yields no percent rather than an infinity;
 * 2. `Exempt` never counts; `Missing` counts as zero only while the classroom's rule says so;
 * 3. at most `dropLowest` of the lowest percentages in a category are removed, and never so many
 *    that the category is left with nothing to average;
 * 4. a category average is the mean of its surviving percentages;
 * 5. the overall value is the weight-weighted mean over categories that have at least one
 *    surviving grade — a category with no grade is excluded and named, never treated as zero;
 * 6. when nothing survives anywhere the answer is `null`, which the UI renders as a
 *    not-enough-data state. It is never `0` and never `NaN`.
 */
object GradeMath {

    /** The percentage a single cell contributes, or `null` when it contributes nothing. */
    fun assignmentPercent(
        cell: GradeCell?,
        assignment: Assignment,
        missingCountsAsZero: Boolean,
    ): Double? {
        if (cell == null) return null
        return when (cell.kind) {
            GradeKind.EXEMPT -> null
            GradeKind.MISSING -> if (missingCountsAsZero) 0.0 else null
            GradeKind.SCORE -> if (assignment.points > 0.0) {
                cell.points / assignment.points * 100.0
            } else {
                // An assignment worth nothing cannot express a percentage. Report nothing rather
                // than dividing by zero and producing an infinity the UI would have to hide.
                null
            }
        }
    }

    /** One category's contribution to one student's average. */
    data class CategoryOutcome(
        val categoryId: String,
        val categoryName: String,
        val weight: Double,
        val average: Double?,
        val usedCount: Int,
        val droppedCount: Int,
        val exemptCount: Int,
        val missingCount: Int,
    ) {
        val isExcluded: Boolean get() = average == null
    }

    /** One student's explainable average. */
    data class StudentAverage(
        val studentId: String,
        /** `null` means "not enough data yet" — the UI must not show a number. */
        val overall: Double?,
        val categories: List<CategoryOutcome>,
        val excludedCategoryIds: List<String>,
        val rounding: RoundingRule,
        val missingCountsAsZero: Boolean,
        val fellBackToUnweighted: Boolean,
    ) {
        val hasAnyData: Boolean get() = overall != null
    }

    fun categoryOutcome(
        classroom: Classroom,
        category: Category,
        studentId: String,
    ): CategoryOutcome {
        val assignments = classroom.assignments.filter { it.categoryId == category.id }
        val cells = classroom.grades.associateBy { it.studentId to it.assignmentId }

        val percents = ArrayList<Double>(assignments.size)
        var exempt = 0
        var missing = 0

        for (assignment in assignments) {
            val cell = cells[studentId to assignment.id]
            if (cell?.kind == GradeKind.EXEMPT) exempt++
            if (cell?.kind == GradeKind.MISSING) missing++
            assignmentPercent(cell, assignment, classroom.missingCountsAsZero)?.let(percents::add)
        }

        if (percents.isEmpty()) {
            return CategoryOutcome(
                categoryId = category.id,
                categoryName = category.name,
                weight = category.weight,
                average = null,
                usedCount = 0,
                droppedCount = 0,
                exemptCount = exempt,
                missingCount = missing,
            )
        }

        // Never drop the last remaining grade: a category that has any data keeps at least one
        // value, so "drop the lowest" can never empty a category and change its meaning.
        val requestedDrop = if (category.dropLowest > 0) category.dropLowest else 0
        val droppedCount = minOf(requestedDrop, percents.size - 1)
        val surviving = percents.sorted().drop(droppedCount)

        return CategoryOutcome(
            categoryId = category.id,
            categoryName = category.name,
            weight = category.weight,
            average = surviving.average(),
            usedCount = surviving.size,
            droppedCount = droppedCount,
            exemptCount = exempt,
            missingCount = missing,
        )
    }

    fun studentAverage(classroom: Classroom, studentId: String): StudentAverage {
        val outcomes = classroom.categories.map { categoryOutcome(classroom, it, studentId) }
        val usable = outcomes.filter { it.average != null }

        if (usable.isEmpty()) {
            return StudentAverage(
                studentId = studentId,
                overall = null,
                categories = outcomes,
                excludedCategoryIds = outcomes.map { it.categoryId },
                rounding = classroom.rounding,
                missingCountsAsZero = classroom.missingCountsAsZero,
                fellBackToUnweighted = false,
            )
        }

        val totalWeight = usable.sumOf { if (it.weight > 0.0) it.weight else 0.0 }
        val fellBack = totalWeight <= 0.0
        val weighted = if (fellBack) {
            usable.sumOf { it.average!! } / usable.size
        } else {
            usable.sumOf { (if (it.weight > 0.0) it.weight else 0.0) * it.average!! } / totalWeight
        }

        return StudentAverage(
            studentId = studentId,
            overall = weighted,
            categories = outcomes,
            excludedCategoryIds = outcomes.filter { it.average == null }.map { it.categoryId },
            rounding = classroom.rounding,
            missingCountsAsZero = classroom.missingCountsAsZero,
            fellBackToUnweighted = fellBack,
        )
    }

    /** The class average over the students who actually have data, or `null` when none do. */
    fun classAverage(classroom: Classroom): Double? {
        val averages = classroom.students
            .filter { it.active }
            .mapNotNull { studentAverage(classroom, it.id).overall }
        return if (averages.isEmpty()) null else averages.average()
    }

    /** Applies the classroom's stated rounding rule. */
    fun round(value: Double, rule: RoundingRule): Double {
        if (value.isNaN() || value.isInfinite()) return value
        val mode = when (rule.mode) {
            RoundMode.HALF_UP -> RoundingMode.HALF_UP
            RoundMode.HALF_EVEN -> RoundingMode.HALF_EVEN
            RoundMode.FLOOR -> RoundingMode.FLOOR
        }
        val decimals = rule.decimals.coerceIn(0, 2)
        return BigDecimal(value).setScale(decimals, mode).toDouble()
    }

    /** The letter for a percentage, or `null` when the teacher has defined no scale. */
    fun letterFor(percent: Double, scale: List<GradeBand>): String? =
        scale.sortedByDescending { it.minPercent }.firstOrNull { percent >= it.minPercent }?.letter
}
