package com.gexetr.teachergradebook.domain

/**
 * What the free version allows, as a domain rule rather than a UI condition.
 *
 * The boundary is deliberately generous where it matters and narrow where the product is sold: a
 * teacher can keep one class with a full roster and unlimited grades for free, so real work is
 * possible before paying — and the moments that ask for money are the ones where the teacher is
 * clearly getting value (a second class, more students, a second weighted category, an export, a
 * backup).
 */
sealed interface LimitCheck {
    data object Allowed : LimitCheck

    data class Blocked(val reason: LimitReason) : LimitCheck

    val isBlocked: Boolean get() = this is Blocked
}

/**
 * Why a limit refused an action. A value, not a sentence: the domain decides *that* an action is
 * refused and *why*, and the UI decides how to say it, because only the UI knows the language.
 *
 * [StudentCap] carries the number rather than a sentence, so the rule and the copy that quotes it
 * cannot drift apart, and the UI can pluralise the count in languages English has no form for.
 */
sealed interface LimitReason {
    data object SecondClass : LimitReason

    data class StudentCap(val limit: Int) : LimitReason

    data object SingleCategory : LimitReason

    data object Export : LimitReason

    data object Backup : LimitReason
}

object FreeTier {
    const val MAX_CLASSES = 1
    const val MAX_STUDENTS_PER_CLASS = 20
    const val MAX_CATEGORIES_PER_CLASS = 1

    fun canAddClass(existingClassCount: Int, unlocked: Boolean): LimitCheck =
        if (unlocked || existingClassCount < MAX_CLASSES) {
            LimitCheck.Allowed
        } else {
            LimitCheck.Blocked(LimitReason.SecondClass)
        }

    fun canAddStudent(existingStudentCount: Int, unlocked: Boolean): LimitCheck =
        if (unlocked || existingStudentCount < MAX_STUDENTS_PER_CLASS) {
            LimitCheck.Allowed
        } else {
            LimitCheck.Blocked(LimitReason.StudentCap(MAX_STUDENTS_PER_CLASS))
        }

    fun canAddCategory(existingCategoryCount: Int, unlocked: Boolean): LimitCheck =
        if (unlocked || existingCategoryCount < MAX_CATEGORIES_PER_CLASS) {
            LimitCheck.Allowed
        } else {
            LimitCheck.Blocked(LimitReason.SingleCategory)
        }

    fun canExport(unlocked: Boolean): LimitCheck =
        if (unlocked) LimitCheck.Allowed else LimitCheck.Blocked(LimitReason.Export)

    fun canBackUp(unlocked: Boolean): LimitCheck =
        if (unlocked) {
            LimitCheck.Allowed
        } else {
            LimitCheck.Blocked(LimitReason.Backup)
        }
}
