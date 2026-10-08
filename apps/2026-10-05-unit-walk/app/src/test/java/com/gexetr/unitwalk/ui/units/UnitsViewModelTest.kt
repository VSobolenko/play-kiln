package com.gexetr.unitwalk.ui.units

import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.billing.FakeBillingController
import com.gexetr.unitwalk.data.WalkStore
import com.gexetr.unitwalk.domain.Unit
import com.gexetr.unitwalk.domain.Walk
import com.gexetr.unitwalk.domain.WalkKind
import com.gexetr.unitwalk.domain.WalkState
import com.gexetr.unitwalk.domain.WalkStatus
import com.gexetr.unitwalk.ui.unlock.UNIT_WALK_UNLOCK
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The portfolio brain, driven on the JVM.
 *
 * Temporary directories are created under the repository's disposable `.kiln/scratch/` tree — never
 * the system temp directory (`.cursor/rules/workspace-hygiene.mdc`) — and `kotlinx-coroutines-test`
 * is deliberately not used, because it is not on the classpath: the ViewModel's suspending [handle]
 * is driven directly inside `runBlocking`.
 */
class UnitsViewModelTest {

    private val tempDirs = mutableListOf<File>()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-10-05T10:00:00Z"), ZoneOffset.UTC)

    @After
    fun cleanUp() {
        tempDirs.forEach { dir -> dir.deleteRecursively() }
        tempDirs.clear()
    }

    // --- the `multi-unit-portfolio` promise: two units, two independent walks ----------------
    //
    // Two units are an *owned* portfolio now: the free plan holds one. This test wired no billing
    // before the single-unit cap existed, so that is a finding recorded in the handoff rather than a
    // quietly rewritten assertion — the promise it checks (two units, independent walks) is unchanged,
    // and the entitlement it now needs is supplied by `ownedViewModel`.

    @Test
    fun twoUnitsKeepIndependentWalksAndEachStartsItsOwn() = runBlocking {
        val dir = newTempDir()
        val viewModel = ownedViewModel(dir)

        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))
        viewModel.handle(UnitsEvent.CreateUnit("Flat 4A"))

        val unitIds = viewModel.uiState.value.units.map { row -> row.id }
        assertEquals(2, unitIds.size)

        viewModel.handle(UnitsEvent.StartWalk(unitIds[0], WalkKind.MOVE_IN))
        viewModel.handle(UnitsEvent.StartWalk(unitIds[1], WalkKind.MOVE_OUT))

        // The walks are durable and each is attached to the unit it started from.
        val durable = WalkStore(dir).load()
        assertEquals(2, durable.walks.size)
        assertEquals(unitIds[0], durable.walks.first { it.kind == WalkKind.MOVE_IN }.unitId)
        assertEquals(unitIds[1], durable.walks.first { it.kind == WalkKind.MOVE_OUT }.unitId)

        // Each row shows exactly its own walk, and neither unit shows the other's.
        val rows = viewModel.uiState.value.units.associateBy { row -> row.id }
        assertEquals(1, rows.getValue(unitIds[0]).walkCount)
        assertEquals(1, rows.getValue(unitIds[1]).walkCount)
        assertEquals(unitIds[0], rows.getValue(unitIds[0]).resumableDraft?.unitId)
        assertEquals(unitIds[1], rows.getValue(unitIds[1]).resumableDraft?.unitId)
        assertTrue(rows.getValue(unitIds[0]).resumableDraft?.walkId != rows.getValue(unitIds[1]).resumableDraft?.walkId)
    }

    // --- the one-time unlock: one free unit, the rest behind the purchase --------------------

    /**
     * The observation that fails on the pre-fix brain: `createUnit` validated only a blank name, so
     * the second create succeeded and the portfolio held two units. Now the second create is refused,
     * the stored document still holds exactly the free unit, and the user is routed to the unlock.
     */
    @Test
    fun aSecondUnitIsRefusedWhileTheUnlockIsNotOwned() = runBlocking {
        val dir = newTempDir()
        val viewModel = unownedViewModel(dir)

        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))
        viewModel.handle(UnitsEvent.CreateUnit("Flat 4A"))

        assertEquals(UnitsMessage.UnitLimitReached, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.openUnlock)
        assertEquals(1, viewModel.uiState.value.units.size)
        assertEquals("Flat 3B", viewModel.uiState.value.units.single().name)
        // Nothing was destroyed to enforce the cap: the unit the user owns is still on disk.
        assertEquals(1, WalkStore(dir).load().units.size)
    }

    /**
     * The observation that fails on the pre-fix brain: a second unit was created with no entitlement
     * source at all. Now an unresolved entitlement (`Unknown`, which is what a brain with no billing
     * controller can answer) is treated as not owned, so the paid perk is not handed out before Play
     * has answered.
     */
    @Test
    fun aSecondUnitIsRefusedWhileTheEntitlementIsUnresolved() = runBlocking {
        val dir = newTempDir()
        // No controller: the state stays `UnlockState.Unknown` for the whole test.
        val viewModel = UnitsViewModel(WalkStore(dir), clock)

        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))
        viewModel.handle(UnitsEvent.CreateUnit("Flat 4A"))

        assertEquals(UnitsMessage.UnitLimitReached, viewModel.uiState.value.message)
        assertEquals(1, viewModel.uiState.value.units.size)
        assertEquals(1, WalkStore(dir).load().units.size)
    }

    /**
     * The observation that fails on the pre-fix brain: `createUnit` was the only place the cap
     * existed, so a multi-unit document restored from cloud backup or device transfer was projected
     * row for row and every unit stayed fully usable. Now the entitlement is applied at load: exactly
     * the portfolio's first unit — the oldest — is usable and the rest are marked locked, with none
     * of them lost.
     */
    @Test
    fun aRestoredPortfolioBeyondTheCapMarksTheExtraUnitsLockedWithoutLosingOne() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(
            WalkState(
                units = listOf(
                    Unit(id = "u1", name = "Flat 3B"),
                    Unit(id = "u2", name = "Flat 4A"),
                    Unit(id = "u3", name = "Studio 5"),
                ),
            ),
        )

        // No controller: the entitlement is `Unknown`, which is not owned, so the free plan's rules
        // apply rather than the paid ones.
        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)

        val rows = viewModel.uiState.value.units
        // Every unit is still listed, with its identity intact: the cap marks, it does not hide.
        assertEquals(3, rows.size)
        assertEquals(listOf("u1", "u2", "u3"), rows.map { row -> row.id })
        assertEquals(listOf("Flat 3B", "Flat 4A", "Studio 5"), rows.map { row -> row.name })
        // The first, and only the first, is usable.
        assertFalse(rows[0].locked)
        assertTrue(rows[1].locked)
        assertTrue(rows[2].locked)
        // The cap is a reading, not a write: the document still holds every unit.
        assertEquals(3, WalkStore(dir).load().units.size)
    }

    /** The other side of the same rule: an owned entitlement locks nothing. */
    @Test
    fun anOwnedPortfolioMarksNoUnitLocked() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(
            WalkState(
                units = listOf(
                    Unit(id = "u1", name = "Flat 3B"),
                    Unit(id = "u2", name = "Flat 4A"),
                ),
            ),
        )

        val viewModel = ownedViewModel(dir)
        viewModel.handle(UnitsEvent.ScreenShown)

        assertEquals(2, viewModel.uiState.value.units.size)
        assertTrue(viewModel.uiState.value.units.none { row -> row.locked })
    }

    /**
     * The observation that fails on the pre-fix brain: `startWalkForUnit` checked only that the unit
     * existed, so a restored locked unit could still start a walk. Now a start on a locked unit is
     * refused with the create refusal's own message and arms the same one-shot, while the free unit
     * stays startable so the guard did not close the whole route.
     */
    @Test
    fun aWalkCannotBeStartedOnALockedUnit() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(
            WalkState(
                units = listOf(
                    Unit(id = "u1", name = "Flat 3B"),
                    Unit(id = "u2", name = "Flat 4A"),
                ),
            ),
        )
        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)

        // `u2` is not the first unit, so it is locked: the walk is refused and nothing is written.
        viewModel.handle(UnitsEvent.StartWalk("u2", WalkKind.MOVE_IN))

        assertEquals(UnitsMessage.UnitLimitReached, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.openUnlock)
        assertNull(viewModel.uiState.value.openWalk)
        assertTrue(WalkStore(dir).load().walks.isEmpty())

        // `u1` is the free unit, so its walk still starts and is durable.
        viewModel.handle(UnitsEvent.StartWalk("u1", WalkKind.MOVE_IN))
        val durable = WalkStore(dir).load()
        assertEquals(1, durable.walks.size)
        assertEquals("u1", durable.walks.single().unitId)
    }

    /**
     * The second route into a unit: "continue" on a restored draft. The pre-fix `resumeWalk` looked
     * only at the walk id, so a locked unit's draft opened anyway; now it passes the same check as a
     * new walk, while the free unit's draft still opens.
     */
    @Test
    fun aWalkCannotBeResumedOnALockedUnit() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(
            WalkState(
                units = listOf(
                    Unit(id = "u1", name = "Flat 3B"),
                    Unit(id = "u2", name = "Flat 4A"),
                ),
                walks = listOf(
                    Walk(id = "w1", unitId = "u1", kind = WalkKind.MOVE_IN, status = WalkStatus.DRAFT),
                    Walk(id = "w2", unitId = "u2", kind = WalkKind.MOVE_IN, status = WalkStatus.DRAFT),
                ),
            ),
        )
        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)

        // The locked unit's draft is refused by the shared message...
        viewModel.handle(UnitsEvent.ResumeWalk("w2"))

        assertEquals(UnitsMessage.UnitLimitReached, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.openUnlock)
        assertNull(viewModel.uiState.value.openWalk)

        // ...and the free unit's draft still opens.
        viewModel.handle(UnitsEvent.ResumeWalk("w1"))
        assertEquals("w1", viewModel.uiState.value.openWalk?.walkId)
    }

    /**
     * The observation that fails on the pre-fix brain: the snackbar effect was keyed on `message`,
     * and `UnitLimitReached` is a `data object`, so a second refusal while the first snackbar was
     * still up produced an equal key, no feedback, and a `true` `openUnlock` nobody consumed. Now
     * every publication carries a new marker.
     */
    @Test
    fun aRepeatedRefusalPublishesANewMessageMarker() = runBlocking {
        val dir = newTempDir()
        val viewModel = unownedViewModel(dir)
        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))

        viewModel.handle(UnitsEvent.CreateUnit("Flat 4A"))
        val firstRefusalId = viewModel.uiState.value.messageId
        assertEquals(UnitsMessage.UnitLimitReached, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.openUnlock)

        // The same refusal again, with `MessageShown` deliberately not dispatched — the message is an
        // equal value, so only the marker can tell the screen to show it again.
        viewModel.handle(UnitsEvent.CreateUnit("Flat 4A"))

        assertEquals(UnitsMessage.UnitLimitReached, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.openUnlock)
        assertTrue(viewModel.uiState.value.messageId > firstRefusalId)
        // The refusal still changed nothing the user owns.
        assertEquals(1, viewModel.uiState.value.units.size)
        assertEquals(1, WalkStore(dir).load().units.size)
    }

    /**
     * The one-shot's lifecycle, as the screen consumes it. The screen clears
     * [UnitsUiState.openUnlock] the moment it reads the refusal's message, and this asserts both
     * halves of what that consumption depends on: clearing the flag must **not** clear the message
     * (otherwise the snackbar being drawn would be cancelled by the screen's own bookkeeping), and
     * the cleared flag must not come back, so a later, unrelated message cannot inherit the Unlock
     * action.
     */
    @Test
    fun theRefusalOneShotIsConsumedAsReadAndCannotLeakIntoALaterMessage() = runBlocking {
        val dir = newTempDir()
        val viewModel = unownedViewModel(dir)
        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))
        viewModel.handle(UnitsEvent.CreateUnit("Flat 4A"))

        // The refusal arms the message and the one-shot in one state update.
        assertEquals(UnitsMessage.UnitLimitReached, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.openUnlock)

        // What the screen does as it reads the flag: consume it, then show the message.
        viewModel.handle(UnitsEvent.UnlockOpened)
        assertFalse(viewModel.uiState.value.openUnlock)
        // Consuming the flag leaves the message standing, so the snackbar is not cancelled mid-draw.
        assertEquals(UnitsMessage.UnitLimitReached, viewModel.uiState.value.message)
        viewModel.handle(UnitsEvent.MessageShown)
        assertNull(viewModel.uiState.value.message)

        // A later, successful create starts from a cleared one-shot — the leak the fix closes.
        val unitId = viewModel.uiState.value.units.single().id
        viewModel.handle(UnitsEvent.RequestDeleteUnit(unitId))
        viewModel.handle(UnitsEvent.ConfirmDeleteUnit)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 5C"))

        assertEquals(UnitsMessage.UnitCreated, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.openUnlock)
        assertEquals("Flat 5C", viewModel.uiState.value.units.single().name)
    }

    /**
     * A guard for the new cap rather than a proof of it: the owned path must stay open, and this
     * fails if the cap ever becomes unconditional (which would also fail the free plan's own promise).
     */
    @Test
    fun aSecondUnitIsAllowedWhenTheUnlockIsOwned() = runBlocking {
        val dir = newTempDir()
        val viewModel = ownedViewModel(dir)

        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))
        viewModel.handle(UnitsEvent.CreateUnit("Flat 4A"))

        assertEquals(UnitsMessage.UnitCreated, viewModel.uiState.value.message)
        assertEquals(2, viewModel.uiState.value.units.size)
        assertEquals(2, WalkStore(dir).load().units.size)
    }

    /**
     * A guard for the free path: the first unit must be creatable with no entitlement at all. It fails
     * against the naive fix that reads the entitlement for *every* create — that version refuses the
     * free plan's own first unit while the unlock is unowned.
     */
    @Test
    fun theFirstUnitIsCreatedWithNoEntitlementAtAll() = runBlocking {
        val dir = newTempDir()
        // The free path is given no controller, and the portfolio is empty: nothing to ask about.
        val viewModel = UnitsViewModel(WalkStore(dir), clock)

        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))

        assertEquals(UnitsMessage.UnitCreated, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.openUnlock)
        assertEquals(1, viewModel.uiState.value.units.size)
        assertEquals(1, WalkStore(dir).load().units.size)
    }

    /**
     * A guard for the free path through a *present* controller: an unowned entitlement must not stop
     * the first unit either, which is the case where the entitlement genuinely exists and is `false`.
     */
    @Test
    fun theFirstUnitIsCreatedEvenWhenTheUnlockIsNotOwned() = runBlocking {
        val dir = newTempDir()
        val viewModel = unownedViewModel(dir)

        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))

        assertEquals(UnitsMessage.UnitCreated, viewModel.uiState.value.message)
        assertEquals(1, viewModel.uiState.value.units.size)
    }

    // --- deletion is two-step ----------------------------------------------------------------

    @Test
    fun deletingAUnitRequiresTheConfirmationStep() = runBlocking {
        val dir = newTempDir()
        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))
        val unitId = viewModel.uiState.value.units.single().id
        viewModel.handle(UnitsEvent.StartWalk(unitId, WalkKind.SINGLE))

        viewModel.handle(UnitsEvent.RequestDeleteUnit(unitId))

        // The request only stages the prompt; it names what would be removed and removes nothing.
        assertEquals(unitId, viewModel.uiState.value.pendingDelete?.unitId)
        assertEquals("Flat 3B", viewModel.uiState.value.pendingDelete?.unitName)
        assertEquals(1, viewModel.uiState.value.pendingDelete?.walkCount)
        assertTrue(viewModel.uiState.value.units.any { row -> row.id == unitId })
        assertTrue(WalkStore(dir).load().units.any { unit -> unit.id == unitId })

        viewModel.handle(UnitsEvent.ConfirmDeleteUnit)

        assertNull(viewModel.uiState.value.pendingDelete)
        assertTrue(viewModel.uiState.value.units.isEmpty())
        val durable = WalkStore(dir).load()
        assertTrue(durable.units.none { unit -> unit.id == unitId })
        assertTrue(durable.walks.none { walk -> walk.unitId == unitId })
    }

    @Test
    fun cancellingTheConfirmationKeepsTheUnitAndItsWalks() = runBlocking {
        val dir = newTempDir()
        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))
        val unitId = viewModel.uiState.value.units.single().id
        viewModel.handle(UnitsEvent.StartWalk(unitId, WalkKind.MOVE_IN))

        viewModel.handle(UnitsEvent.RequestDeleteUnit(unitId))
        viewModel.handle(UnitsEvent.CancelDeleteUnit)

        assertNull(viewModel.uiState.value.pendingDelete)
        assertEquals(1, viewModel.uiState.value.units.size)
        assertEquals(1, WalkStore(dir).load().walks.size)
    }

    // --- honest empty states, never a fabricated row -----------------------------------------

    @Test
    fun aUnitWithNoWalksIsAnHonestEmptyState() = runBlocking {
        val dir = newTempDir()
        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))

        val row = viewModel.uiState.value.units.single()

        assertEquals(0, row.walkCount)
        assertNull(row.resumableDraft)
        assertFalse(row.hasPriorMoveIn)
    }

    @Test
    fun aMoveOutWithoutAPriorMoveInIsReportedRatherThanInvented() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(
            WalkState(
                units = listOf(Unit(id = "u1", name = "Flat 3B")),
                walks = listOf(
                    Walk(
                        id = "w1",
                        unitId = "u1",
                        kind = WalkKind.MOVE_OUT,
                        status = WalkStatus.FINISHED,
                    ),
                    // A draft move-in is still being written, so it is not a baseline.
                    Walk(
                        id = "w2",
                        unitId = "u1",
                        kind = WalkKind.MOVE_IN,
                        status = WalkStatus.DRAFT,
                    ),
                ),
            ),
        )

        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)

        val row = viewModel.uiState.value.units.single()
        assertEquals(2, row.walkCount)
        assertFalse(row.hasPriorMoveIn)
    }

    @Test
    fun aFinishedMoveInIsTheOnlyThingThatCountsAsAPriorReport() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(
            WalkState(
                units = listOf(Unit(id = "u1", name = "Flat 3B")),
                walks = listOf(
                    Walk(
                        id = "w1",
                        unitId = "u1",
                        kind = WalkKind.MOVE_IN,
                        status = WalkStatus.FINISHED,
                    ),
                ),
            ),
        )

        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)

        assertTrue(viewModel.uiState.value.units.single().hasPriorMoveIn)
    }

    // --- create / rename / blank input -------------------------------------------------------

    @Test
    fun renamingAUnitKeepsItsWalksAndSurvivesAReload() = runBlocking {
        val dir = newTempDir()
        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)
        viewModel.handle(UnitsEvent.CreateUnit("Flat 3B"))
        val unitId = viewModel.uiState.value.units.single().id
        viewModel.handle(UnitsEvent.StartWalk(unitId, WalkKind.MOVE_IN))

        viewModel.handle(UnitsEvent.RenameUnit(unitId, "Flat 3B (rear)"))

        assertEquals("Flat 3B (rear)", viewModel.uiState.value.units.single().name)

        // A fresh store and brain see the new name; the walk stayed with its unit.
        val reopened = UnitsViewModel(WalkStore(dir), clock)
        reopened.handle(UnitsEvent.ScreenShown)
        val row = reopened.uiState.value.units.single()
        assertEquals("Flat 3B (rear)", row.name)
        assertEquals(1, row.walkCount)
        assertEquals(unitId, WalkStore(dir).load().walks.single().unitId)
    }

    @Test
    fun aBlankUnitNameIsRejectedRatherThanStored() = runBlocking {
        val dir = newTempDir()
        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)

        viewModel.handle(UnitsEvent.CreateUnit("   "))

        assertEquals(UnitsMessage.NameRequired, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.units.isEmpty())
        assertTrue(WalkStore(dir).load().units.isEmpty())
    }

    // --- a half-written document costs the document, not the brain ---------------------------

    @Test
    fun aHalfWrittenPortfolioReadsAsEmptyInsteadOfThrowing() = runBlocking {
        val dir = newTempDir()
        File(dir, WalkStore.DOCUMENT_NAME).writeText("{\"units\":[{\"id\":", Charsets.UTF_8)

        val viewModel = UnitsViewModel(WalkStore(dir), clock)
        viewModel.handle(UnitsEvent.ScreenShown)

        assertTrue(viewModel.uiState.value.units.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // --- helpers -----------------------------------------------------------------------------

    /**
     * A brain with no billing controller: the state it publishes is `UnlockState.Unknown`, and it is
     * never touched for the first unit. `externalScope` is what keeps `Dispatchers.Main` out of a JVM
     * test while the entitlement mirror is launched.
     */
    private fun unownedViewModel(dir: File): UnitsViewModel = UnitsViewModel(
        store = WalkStore(dir),
        clock = clock,
        billing = FakeBillingController(),
        externalScope = CoroutineScope(Dispatchers.Unconfined),
    )

    /** A brain that sees the one-time unlock as confirmed owned, so the portfolio is unlimited. */
    private fun ownedViewModel(dir: File): UnitsViewModel = UnitsViewModel(
        store = WalkStore(dir),
        clock = clock,
        billing = FakeBillingController(
            seedEntitlements = Entitlements(owned = setOf(UNIT_WALK_UNLOCK.key())),
        ),
        externalScope = CoroutineScope(Dispatchers.Unconfined),
    )

    private fun newTempDir(): File {
        val dir = File.createTempFile("unitwalk-units-", "", scratchRoot())
        assertTrue(dir.delete())
        assertTrue(dir.mkdirs())
        tempDirs += dir
        return dir
    }

    /**
     * The repository's `.kiln/scratch/`, found by walking up from the test working directory, so a
     * test never touches the machine's system temp directory. The fallback is still inside the
     * checked-out tree.
     */
    private fun scratchRoot(): File {
        var candidate: File? = File(".").absoluteFile
        var hops = 0
        while (candidate != null && hops < 12) {
            if (File(candidate, ".kiln").isDirectory) {
                val scratch = File(candidate, ".kiln/scratch")
                scratch.mkdirs()
                return scratch
            }
            candidate = candidate.parentFile
            hops++
        }
        val fallback = File(".kiln/scratch")
        fallback.mkdirs()
        return fallback
    }
}
