package com.gexetr.unitwalk.ui.units

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.capability.billing.BillingController
import com.gexetr.unitwalk.data.WalkStore
import com.gexetr.unitwalk.domain.Unit
import com.gexetr.unitwalk.domain.WalkKind
import com.gexetr.unitwalk.domain.WalkState
import com.gexetr.unitwalk.domain.WalkStatus
import com.gexetr.unitwalk.domain.startWalk
import com.gexetr.unitwalk.ui.unlock.UNIT_WALK_CATALOG
import com.gexetr.unitwalk.ui.unlock.UnlockState
import com.gexetr.unitwalk.ui.unlock.isOwned
import com.gexetr.unitwalk.ui.unlock.unlockState
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The portfolio brain: it lists the landlord's named units, creates, renames and deletes them, and
 * starts a walk from a named one.
 *
 * It owns the durable [WalkState] and writes it through [WalkStore]. Every mutation is persisted
 * **before** it is published into [uiState], so the screen can never show a unit that is not on disk
 * and a state update after a failed write is the previous good state rather than an optimistic lie.
 *
 * ## The free-plan single-unit cap
 *
 * The one-time unlock this app sells is unlimited units, and the free plan keeps **exactly one unit
 * usable**. The rule is stated once and applied everywhere: when the unlock is not owned the free
 * unit is `units.first()` — the oldest unit in the durable portfolio — and every other unit is
 * *locked*, never hidden and never deleted. A locked unit's record stays on disk and its row stays
 * on screen with its name and its walk count; what it loses is the way in.
 *
 * The cap is therefore enforced at **load**, not only at creation: the row projection `toRows`
 * derives each row's `locked` flag and both [publish] and [applyUnlock] re-run it, so a restored
 * document is marked the moment it is read and re-marked when the entitlement changes. Every route
 * that would enter a unit — [createUnit] (which refuses a second unit), [startWalkForUnit] and
 * [resumeWalk] — refuses a locked one with the same typed [UnitsMessage.UnitLimitReached] and arms
 * [UnitsUiState.openUnlock]. Enforcing it at creation alone let a restored multi-unit document hand
 * out the paid product, because [reload] and the row projection trusted whatever was on disk.
 *
 * [UnlockState.Unknown] — billing has not answered yet — is **not** owned, so an unresolved
 * entitlement is the locked direction rather than the generous one; the shared question itself is
 * [unlockState], the `Entitlements` extension in `ui/unlock/UnlockProducts.kt`. The refusal hands the
 * user [UnitsUiState.openUnlock], a one-shot that arms the Unlock action on the refusal's message;
 * the destination opens only when that action is tapped. Nothing the user already has is removed, and
 * export is never gated by this cap.
 *
 * ## Why the store and the clock are constructor parameters
 *
 * [WalkStore] takes the directory it writes to (the app passes `context.filesDir`, a JVM test passes
 * a `.kiln/scratch/` directory) and [clock] makes `createdAt`/`startedAtIso` deterministic in a test.
 * Neither the store nor the clock is discovered inside the class, which is what makes the tests real
 * rather than mocked: they run the same code path the app runs, against a real directory.
 *
 * ## The orchestrator's pattern
 *
 * A `StateFlow` of an [Immutable][UnitsUiState] value, a sealed [UnitsEvent] for input and a sealed
 * [UnitsMessage] for output, plus a `factory(context)` companion — the shape
 * `ui/settings/SettingsViewModel.kt` already uses. [onEvent] is the
 * non-suspending entry point a composable calls; [handle] is the same logic as a suspending function
 * so a JVM test can drive it inside `runBlocking` with no coroutine test runner on the classpath.
 */
class UnitsViewModel(
    private val store: WalkStore,
    private val clock: Clock = Clock.systemUTC(),
    /**
     * The entitlement source, or `null` in a test that does not exercise the cap. It is read to
     * decide which units are usable — at load and again whenever the entitlement changes — and to
     * refuse a second unit. With no controller the state stays [UnlockState.Unknown], which is not
     * owned, so the free plan's own rules apply. The app hands in the shell's one controller; the
     * same instance backs the report screen, so the answer does not have to be fetched twice.
     */
    private val billing: BillingController? = null,
    externalScope: CoroutineScope? = null,
) : ViewModel() {

    // Resolved lazily so a unit test that never calls `onEvent` never touches `Dispatchers.Main`.
    private val scope: CoroutineScope by lazy { externalScope ?: viewModelScope }

    /** Guards the one-time connect/mirror, so a reload does not open a second Play connection. */
    private var watchingUnlock: Boolean = false

    private var state: WalkState = WalkState()

    private val _uiState = MutableStateFlow(UnitsUiState())
    val uiState: StateFlow<UnitsUiState> = _uiState.asStateFlow()

    fun onEvent(event: UnitsEvent) {
        scope.launch { handle(event) }
    }

    /**
     * Mirrors the entitlement into [UnitsUiState.unlock] and connects once.
     *
     * The connect is launched, never awaited: the portfolio must render from disk immediately rather
     * than wait on Play, and while the answer is in flight the state stays [UnlockState.Unknown],
     * which [isOwned] treats as "not owned" — the safe direction, so a restored document does not
     * hand out the paid product before Play has answered. Every mirror goes through [applyUnlock], so
     * a purchase, or a `restore()` that finds one, re-derives the rows and the locked units become
     * usable in the same session rather than after a restart.
     */
    private fun watchUnlock() {
        val controller = billing ?: return
        applyUnlock(controller.entitlements.value.unlockState())
        if (watchingUnlock) return
        watchingUnlock = true
        scope.launch {
            controller.connect(UNIT_WALK_CATALOG)
            applyUnlock(controller.entitlements.value.unlockState())
        }
        scope.launch {
            controller.entitlements.collect { entitlements ->
                applyUnlock(entitlements.unlockState())
            }
        }
    }

    /**
     * Mirrors [unlock] and re-derives the rows in the same update, so [UnitRow.locked] always agrees
     * with the entitlement that decides it. Without the second half, a purchase would raise
     * [UnitsUiState.unlock] and leave every restored row locked until the next reload.
     */
    private fun applyUnlock(unlock: UnlockState) {
        _uiState.update {
            it.copy(unlock = unlock, units = state.toRows(unlock.isOwned))
        }
    }

    /**
     * The same logic as a suspending function, so a JVM test can drive one event at a time inside
     * `runBlocking` with no coroutine test runner on the classpath. The app calls [onEvent].
     */
    suspend fun handle(event: UnitsEvent) {
        when (event) {
            UnitsEvent.ScreenShown -> reload()
            is UnitsEvent.CreateUnit -> createUnit(event.name)
            is UnitsEvent.RenameUnit -> renameUnit(event.unitId, event.name)
            is UnitsEvent.RequestDeleteUnit -> requestDelete(event.unitId)
            UnitsEvent.ConfirmDeleteUnit -> confirmDelete()
            UnitsEvent.CancelDeleteUnit -> _uiState.update { it.copy(pendingDelete = null) }
            is UnitsEvent.StartWalk -> startWalkForUnit(event.unitId, event.kind)
            is UnitsEvent.ResumeWalk -> resumeWalk(event.walkId)
            UnitsEvent.WalkOpened -> _uiState.update { it.copy(openWalk = null) }
            UnitsEvent.UnlockOpened -> _uiState.update { it.copy(openUnlock = false) }
            UnitsEvent.MessageShown -> _uiState.update { it.copy(message = null) }
        }
    }

    private suspend fun reload() {
        state = store.load()
        _uiState.update { it.copy(pendingDelete = null, openWalk = null, openUnlock = false) }
        // The entitlement mirror is started on the first load, not in `init`, so a JVM test that
        // never shows the screen never touches `Dispatchers.Main` — the same reason `scope` is lazy.
        watchUnlock()
        publish(message = null)
    }

    private suspend fun createUnit(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) {
            showMessage(UnitsMessage.NameRequired)
            return
        }
        // The free plan's single unit. The first unit is created without ever reading the
        // entitlement, so there is no "billing has not answered yet" window in the free path; a
        // *second* create asks, and while the answer is unknown the cap is closed rather than given
        // away. This is the same typed refusal and the same one-shot a locked unit's StartWalk and
        // ResumeWalk produce, so creation and re-entry cannot drift apart.
        if (state.units.isNotEmpty() && !_uiState.value.unlock.isOwned) {
            showMessage(UnitsMessage.UnitLimitReached, armUnlock = true)
            return
        }
        val unit = Unit(
            id = "unit-${UUID.randomUUID()}",
            name = name,
            createdAt = Instant.now(clock).toString(),
        )
        commit(state.copy(units = state.units + unit), UnitsMessage.UnitCreated)
    }

    private suspend fun renameUnit(unitId: String, rawName: String) {
        if (state.units.none { unit -> unit.id == unitId }) {
            showMessage(UnitsMessage.UnitNotFound)
            return
        }
        val name = rawName.trim()
        if (name.isEmpty()) {
            showMessage(UnitsMessage.NameRequired)
            return
        }
        val renamed = state.units.map { unit -> if (unit.id == unitId) unit.copy(name = name) else unit }
        commit(state.copy(units = renamed), UnitsMessage.UnitRenamed)
    }

    /**
     * Stages a deletion. Nothing is removed and nothing is written here: the unit survives until
     * [confirmDelete] runs, which is what makes "delete only after the caller confirms" true rather
     * than a convention the screen is trusted to follow.
     */
    private fun requestDelete(unitId: String) {
        val unit = state.units.firstOrNull { candidate -> candidate.id == unitId }
        if (unit == null) {
            showMessage(UnitsMessage.UnitNotFound)
            return
        }
        val walkCount = state.walks.count { walk -> walk.unitId == unitId }
        _uiState.update {
            it.copy(
                pendingDelete = PendingUnitDelete(
                    unitId = unit.id,
                    unitName = unit.name,
                    walkCount = walkCount,
                ),
                message = null,
            )
        }
    }

    private suspend fun confirmDelete() {
        val pending = _uiState.value.pendingDelete ?: return
        val next = state.copy(
            units = state.units.filterNot { unit -> unit.id == pending.unitId },
            // A walk never outlives the unit it belongs to, or it would show under no unit.
            walks = state.walks.filterNot { walk -> walk.unitId == pending.unitId },
        )
        if (commit(next, UnitsMessage.UnitDeleted)) {
            _uiState.update { it.copy(pendingDelete = null) }
        }
    }

    private suspend fun startWalkForUnit(unitId: String, kind: WalkKind) {
        if (state.units.none { unit -> unit.id == unitId }) {
            showMessage(UnitsMessage.UnitNotFound)
            return
        }
        // A locked unit is refused by the same typed message a refused create uses, and the refusal
        // arms the Unlock action. The screen also replaces a locked row's walk buttons with the lock
        // affordance, but the brain is the boundary: this route is gated here, not only there.
        if (isUnitLocked(unitId)) {
            showMessage(UnitsMessage.UnitLimitReached, armUnlock = true)
            return
        }
        val walk = startWalk(state, unitId, kind, Instant.now(clock).toString())
        val saved = commit(state.copy(walks = state.walks + walk), UnitsMessage.WalkStarted)
        if (saved) {
            // The walk is opened only after its snapshot is on disk.
            _uiState.update {
                it.copy(openWalk = WalkRef(walkId = walk.id, unitId = unitId, kind = kind))
            }
        }
    }

    private fun resumeWalk(walkId: String) {
        val walk = state.walks.firstOrNull { candidate -> candidate.id == walkId }
        if (walk == null) {
            showMessage(UnitsMessage.UnitNotFound)
            return
        }
        // "Continue" is the second route into a unit, and it must pass the same check as starting a
        // new walk: the walk carries its unit's id, and that unit can be locked — otherwise a
        // restored draft would be a way back into a unit the cap has closed.
        if (isUnitLocked(walk.unitId)) {
            showMessage(UnitsMessage.UnitLimitReached, armUnlock = true)
            return
        }
        _uiState.update {
            it.copy(
                openWalk = WalkRef(walkId = walk.id, unitId = walk.unitId, kind = walk.kind),
                message = null,
            )
        }
    }

    /**
     * Writes [next], and only then makes it the in-memory state and the published [uiState].
     *
     * The order matters: a failed write must leave the brain on the last durable state rather than
     * reporting a unit that a restart would forget. Returns whether the write landed.
     */
    private suspend fun commit(next: WalkState, message: UnitsMessage): Boolean {
        return try {
            store.save(next)
            state = next
            publish(message)
            true
        } catch (failure: IOException) {
            showMessage(UnitsMessage.NotSaved)
            false
        }
    }

    /**
     * Publishes [message] as a fresh publication: [UnitsUiState.messageId] is bumped so the screen's
     * snackbar effect restarts even when this is an equal `data object` to the message already on
     * screen. Without the marker a repeated refusal — the same [UnitsMessage.UnitLimitReached] while
     * its snackbar is still up — shares the effect's key and produces no feedback at all.
     *
     * [armUnlock] sets [UnitsUiState.openUnlock] without ever clearing it, so an ordinary message
     * cannot silently disarm a refusal the screen has not consumed yet.
     */
    private fun showMessage(message: UnitsMessage, armUnlock: Boolean = false) {
        _uiState.update {
            it.copy(
                message = message,
                messageId = it.messageId + 1,
                openUnlock = it.openUnlock || armUnlock,
            )
        }
    }

    /**
     * Whether [unitId] is behind the unlock right now, by the one rule [toRows] draws with: while the
     * unlock is not owned the free unit is `units.first()` — the oldest — and every other id is
     * locked. A unit that is not in the portfolio is not locked; the caller reports it missing.
     */
    private fun isUnitLocked(unitId: String): Boolean {
        if (_uiState.value.unlock.isOwned) return false
        val freeUnitId = state.units.firstOrNull()?.id ?: return false
        return unitId != freeUnitId
    }

    private fun publish(message: UnitsMessage?) {
        val unlock = _uiState.value.unlock
        _uiState.update {
            it.copy(
                isLoading = false,
                units = state.toRows(unlock.isOwned),
                message = message,
                // A non-null message is a publication and gets a fresh marker; a null one only clears
                // the slot and leaves the marker alone.
                messageId = if (message != null) it.messageId + 1 else it.messageId,
            )
        }
    }

    /**
     * Projects the durable portfolio into the rows the screen draws. Nothing is invented.
     *
     * The free unit is `units.first()` — the **oldest** unit, in the document's own insertion order —
     * and it is the only row that is not [UnitRow.locked] while the unlock is unowned. The others keep
     * every fact they had; they are marked, not hidden and not deleted.
     */
    private fun WalkState.toRows(unlockOwned: Boolean): List<UnitRow> {
        val freeUnitId = if (unlockOwned) null else units.firstOrNull()?.id
        return units.map { unit ->
            val unitWalks = walks.filter { walk -> walk.unitId == unit.id }
            UnitRow(
                id = unit.id,
                name = unit.name,
                address = unit.address,
                walkCount = unitWalks.size,
                resumableDraft = unitWalks
                    .filter { walk -> walk.status == WalkStatus.DRAFT }
                    .maxByOrNull { walk -> walk.startedAtIso }
                    ?.let { walk -> WalkRef(walkId = walk.id, unitId = unit.id, kind = walk.kind) },
                // Only a *finished* move-in is a baseline: a draft is still being written.
                hasPriorMoveIn = unitWalks.any { walk ->
                    walk.kind == WalkKind.MOVE_IN && walk.status == WalkStatus.FINISHED
                },
                locked = freeUnitId != null && unit.id != freeUnitId,
            )
        }
    }

    companion object {
        /**
         * Builds the ViewModel with the app's real store.
         *
         * The application context is taken here rather than remembered in the composition: the walk
         * document lives in `filesDir`, which is per application, not per screen. [billing] is the
         * shell's one entitlement source, or `null` when nothing needs the cap read (a preview).
         */
        fun factory(
            context: Context,
            billing: BillingController? = null,
        ): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    UnitsViewModel(
                        store = WalkStore(applicationContext.filesDir),
                        billing = billing,
                    ) as T
            }
        }
    }
}
