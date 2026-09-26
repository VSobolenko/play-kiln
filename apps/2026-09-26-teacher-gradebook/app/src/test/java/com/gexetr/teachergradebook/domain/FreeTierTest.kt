package com.gexetr.teachergradebook.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The free boundary, as a rule rather than a UI condition.
 *
 * Free work must be genuinely usable — one class, 20 students, unlimited grades — and the moments
 * that ask for money must be the ones where the teacher is clearly getting value. The unlock lifts
 * every limit and nothing is left half-blocked.
 */
class FreeTierTest {

    @Test
    fun `one class is free and a second is not`() {
        assertFalse(FreeTier.canAddClass(0, unlocked = false).isBlocked)
        assertTrue(FreeTier.canAddClass(1, unlocked = false).isBlocked)
        assertFalse(FreeTier.canAddClass(9, unlocked = true).isBlocked)
    }

    @Test
    fun `twenty students are free and the twenty-first is not`() {
        assertFalse(FreeTier.canAddStudent(19, unlocked = false).isBlocked)
        assertTrue(FreeTier.canAddStudent(20, unlocked = false).isBlocked)
        assertFalse(FreeTier.canAddStudent(500, unlocked = true).isBlocked)
    }

    @Test
    fun `a second weighted category is part of the unlock`() {
        assertFalse(FreeTier.canAddCategory(0, unlocked = false).isBlocked)
        assertTrue(FreeTier.canAddCategory(1, unlocked = false).isBlocked)
        assertFalse(FreeTier.canAddCategory(8, unlocked = true).isBlocked)
    }

    @Test
    fun `export and backup are part of the unlock`() {
        assertTrue(FreeTier.canExport(unlocked = false).isBlocked)
        assertTrue(FreeTier.canBackUp(unlocked = false).isBlocked)
        assertFalse(FreeTier.canExport(unlocked = true).isBlocked)
        assertFalse(FreeTier.canBackUp(unlocked = true).isBlocked)
    }

    /**
     * A refusal must say *which* limit applied, as a value the UI can translate — not as a sentence
     * the domain wrote in English. The wording now lives in `strings_limits.xml`; this guards the
     * mapping from the rule to the reason, and `check-locales.mjs` guards the wording.
     */
    @Test
    fun `every refusal carries the reason the UI can name`() {
        assertEquals(
            LimitReason.SecondClass,
            (FreeTier.canAddClass(1, unlocked = false) as LimitCheck.Blocked).reason,
        )
        assertEquals(
            LimitReason.StudentCap(FreeTier.MAX_STUDENTS_PER_CLASS),
            (FreeTier.canAddStudent(20, unlocked = false) as LimitCheck.Blocked).reason,
        )
        assertEquals(
            LimitReason.SingleCategory,
            (FreeTier.canAddCategory(1, unlocked = false) as LimitCheck.Blocked).reason,
        )
        assertEquals(
            LimitReason.Export,
            (FreeTier.canExport(unlocked = false) as LimitCheck.Blocked).reason,
        )
        assertEquals(
            LimitReason.Backup,
            (FreeTier.canBackUp(unlocked = false) as LimitCheck.Blocked).reason,
        )
    }

    /** The number the UI prints comes from the rule, so the copy and the limit cannot disagree. */
    @Test
    fun `the student cap reason carries the rule's own limit`() {
        val reason = (FreeTier.canAddStudent(20, unlocked = false) as LimitCheck.Blocked).reason
        assertEquals(FreeTier.MAX_STUDENTS_PER_CLASS, (reason as LimitReason.StudentCap).limit)
    }

    @Test
    fun `allowed carries no reason`() {
        val allowed = FreeTier.canAddClass(0, unlocked = false)
        assertTrue(allowed is LimitCheck.Allowed)
        assertFalse(allowed.isBlocked)
    }

    @Test
    fun `the stated limits are the ones the copy promises`() {
        assertEquals(1, FreeTier.MAX_CLASSES)
        assertEquals(20, FreeTier.MAX_STUDENTS_PER_CLASS)
        assertEquals(1, FreeTier.MAX_CATEGORIES_PER_CLASS)
    }
}
