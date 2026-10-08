package com.gexetr.unitwalk.ui.report

import com.gexetr.unitwalk.ui.unlock.UnlockState
import com.gexetr.unitwalk.ui.unlock.isOwned
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The report surface's paid/not-paid reading, pinned on the JVM.
 *
 * `ReportUiState` is the one place the report screen asks whether the unlock is owned, and
 * [ReportUiState.isOwned] / [ReportUiState.watermarked] are **derived** from its single [ReportUiState.unlock]
 * field rather than stored beside it — so this file is the proof that the two derived answers cannot
 * disagree with the shared question, and that an unresolved entitlement takes the free direction.
 *
 * **The observation that fails against the code before this fix.** `ReportUiState` had no `unlock`
 * field, no `isOwned` and no `watermarked`; `ReportScreen.kt` therefore showed the upsell to everybody,
 * including an owner, and `ReportViewModel` had nothing to hand the PDF builder. These assertions
 * cannot compile against that revision, and `anUnresolvedEntitlementIsNotOwnedAndIsWatermarked` is the
 * row that a naive `unlock != UnlockState.NotOwned` (which would call `Unknown` paid) fails.
 */
class ReportOwnershipTest {

    /**
     * The dangerous direction: while billing has not answered, the report must be stamped rather than
     * handed out as the paid perk — and it must still be written and still be shareable.
     */
    @Test
    fun anUnresolvedEntitlementIsNotOwnedAndIsWatermarked() {
        val state = ReportUiState(unlock = UnlockState.Unknown)

        assertFalse(state.isOwned)
        assertTrue(state.watermarked)
    }

    @Test
    fun aConfirmedOwnedEntitlementIsNotWatermarked() {
        val state = ReportUiState(unlock = UnlockState.Owned)

        assertTrue(state.isOwned)
        assertFalse(state.watermarked)
    }

    @Test
    fun aConfirmedNotOwnedEntitlementIsWatermarked() {
        val state = ReportUiState(unlock = UnlockState.NotOwned)

        assertFalse(state.isOwned)
        assertTrue(state.watermarked)
    }

    /**
     * The two derived answers are the same answer, for every state — including one added later, which
     * is why this iterates `UnlockState.entries` instead of listing the three by hand.
     */
    @Test
    fun theDerivedOwnershipIsTheSharedQuestionForEveryState() {
        UnlockState.entries.forEach { unlock ->
            val state = ReportUiState(unlock = unlock)

            assertEquals("isOwned drifted from UnlockState.isOwned for $unlock", unlock.isOwned, state.isOwned)
            assertEquals("watermarked is not the negation of owned for $unlock", !unlock.isOwned, state.watermarked)
        }
        // A guard that the iteration measured something rather than nothing.
        assertTrue("UnlockState.entries is empty", UnlockState.entries.isNotEmpty())
    }
}
