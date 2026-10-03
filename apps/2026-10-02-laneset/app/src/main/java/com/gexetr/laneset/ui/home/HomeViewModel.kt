package com.gexetr.laneset.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.laneset.data.AppGraph
import com.gexetr.laneset.data.BillingManager
import com.gexetr.laneset.data.PaceStore
import com.gexetr.laneset.data.SessionPosition
import com.gexetr.laneset.data.SessionStore
import com.gexetr.laneset.domain.Focus
import com.gexetr.laneset.domain.Gear
import com.gexetr.laneset.domain.Session
import com.gexetr.laneset.domain.SetGenerator
import com.gexetr.laneset.domain.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The home surface's state holder.
 *
 * Four things live here and nowhere else, because each one is a decision some other layer would get
 * wrong if it made it again:
 *
 * - **The free/paid boundary.** Whether a focus is available, whether the handoff is available and
 *   whether a refusal is a *sale* or an *error* is decided in [onEvent] from `BillingManager.isUnlocked`.
 *   The screen renders one button per action and never decides what it does, so there is exactly one
 *   place a paid feature could leak out of.
 * - **The generator call.** The session is produced by `SetGenerator.generateSession(constraints, pace)`
 *   and by nothing else. No arithmetic is done in the UI: the constraints are the domain's own type,
 *   the pace is the one [HomeUiState.activePacePer100Seconds] resolves, and the result is the domain's
 *   own `Session`. That is what makes the promise's determinism observable — the same constraints and
 *   the same pace always produce this same set list, because the UI cannot contribute to it.
 * - **Durability.** The generated session and the swimmer's place in it are written through
 *   [SessionStore] on every change, and the stored value is the single source of the session on
 *   screen. The ViewModel never keeps a session the store does not have, and it never restores a
 *   position without clamping it against the session it is restored with.
 * - **Navigation that needs an entitlement check.** A locked action does not fail silently and does
 *   not push a screen from the composable: it sets [HomeUiState.navigationRequest] and the route
 *   performs it. The screen cannot navigate to the unlock screen on its own, which is the property
 *   that keeps "explain what the unlock gives" in one place.
 *
 * The three stores are nullable for exactly one reason, stated so it is not read as defensive noise:
 * [HomeViewModel.factory] hands this class the process-wide graph, and `AppGraph` is a `lateinit`
 * object `MainActivity` builds before the first composition — so in the shipped app they are never
 * null. They are null in a plain JVM unit test or a `@Preview`, where there is no Android `Context`
 * to build a `DataStore` from, and the screen must still render its form and its empty state there
 * rather than throw from a `lateinit`.
 */
class HomeViewModel(
    private val paceStore: PaceStore? = null,
    private val sessionStore: SessionStore? = null,
    private val billing: BillingManager? = null,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(HomeUiState())

    /** The screen's single input. Read by `HomeRoute` and rendered by `HomeScreen`. */
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    /**
     * Whether a stored session has already been taken into the state.
     *
     * The store's flow emits once on subscribe and again after every write, so the *first* emission is
     * the restore and every later one is this class's own write coming back. Only the first may move
     * the form's constraints — otherwise a swimmer's edit in the form, which is deliberately not
     * persisted, would be overwritten by the constraints of the session they already have.
     */
    private var hasAdoptedStoredSession = false

    init {
        viewModelScope.launch {
            val store = paceStore ?: return@launch
            store.profile.collect { profile ->
                mutableUiState.update { state ->
                    state.copy(
                        hasTrials = profile.hasTrials,
                        thresholdPacePer100Seconds = profile.thresholdPacePer100Seconds,
                    )
                }
            }
        }
        viewModelScope.launch {
            val manager = billing ?: return@launch
            manager.isUnlocked.collect { unlocked ->
                mutableUiState.update { state -> state.copy(isUnlocked = unlocked) }
            }
        }
        viewModelScope.launch {
            val store = sessionStore ?: return@launch
            store.saved.collect { persisted ->
                val isRestore = !hasAdoptedStoredSession
                hasAdoptedStoredSession = true
                if (persisted == null) {
                    mutableUiState.update { state ->
                        state.copy(
                            session = null,
                            position = SessionPosition.Start,
                            isExportSheetVisible = false,
                        )
                    }
                    return@collect
                }
                val session = persisted.session
                val position = persisted.position.clampedTo(session)
                mutableUiState.update { state ->
                    if (!isRestore) {
                        // A write of our own coming back: the form is already what the swimmer chose,
                        // so only the session and the position are taken.
                        state.copy(session = session, position = position)
                    } else {
                        state.copy(
                            poolLengthMeters = session.constraints.poolLengthMeters,
                            minutesAvailable = session.constraints.minutesAvailable,
                            // A focus that was unlocked when the session was built and is not unlocked
                            // now stays where it is in the *form*: the session on screen is history and
                            // is shown as it was swam, but a new build would have to be free.
                            focus = session.constraints.focus.takeIf { it == FREE_FOCUS || state.isUnlocked }
                                ?: state.focus,
                            strokesToAvoid = session.constraints.strokesToAvoid,
                            gearOnDeck = session.constraints.gearOnDeck,
                            session = session,
                            position = position,
                        )
                    }
                }
            }
        }
    }

    /**
     * The one entry point for every control on the screen.
     *
     * Exhaustive over [HomeEvent] with no `else`, so a new event is a compile error here rather than a
     * tap that does nothing.
     */
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.PoolLengthSelected -> updateConstraints { it.copy(poolLengthMeters = event.meters) }
            is HomeEvent.MinutesSelected -> updateConstraints { it.copy(minutesAvailable = event.minutes) }
            is HomeEvent.FocusSelected -> selectFocus(event.focus)
            is HomeEvent.StrokeAvoidToggled -> toggleStrokeToAvoid(event.stroke)
            is HomeEvent.GearToggled -> toggleGear(event.gear)
            HomeEvent.GenerateRequested -> generate()
            HomeEvent.AdvanceRequested -> advance()
            HomeEvent.RetreatRequested -> retreat()
            HomeEvent.SessionDiscarded -> discard()
            HomeEvent.ExportRequested -> requestHandoff { it.copy(isExportSheetVisible = true) }
            HomeEvent.ExportDismissed -> mutableUiState.update { it.copy(isExportSheetVisible = false) }
            HomeEvent.ImportRequested -> navigateIfUnlocked(HomeNavigation.CoachImport)
            HomeEvent.PaceTestRequested -> navigateIfUnlocked(HomeNavigation.PaceTest)
            HomeEvent.UnlockRequested -> mutableUiState.update { it.copy(navigationRequest = HomeNavigation.Unlock) }
            HomeEvent.NavigationHandled -> mutableUiState.update { it.copy(navigationRequest = null) }
            HomeEvent.NoticeDismissed -> mutableUiState.update { it.copy(notice = null) }
        }
    }

    // -----------------------------------------------------------------------------------------
    // The form. A change to a constraint is not a reason to regenerate: the swimmer chooses when.
    // -----------------------------------------------------------------------------------------

    /**
     * Applies a constraint change and drops any notice, because both notices are about input the
     * swimmer is now changing. The session on screen is deliberately left alone — it is what they
     * already swam, and [HomeUiState.isSessionCurrent] is what tells them it no longer matches.
     */
    private fun updateConstraints(transform: (HomeUiState) -> HomeUiState) {
        mutableUiState.update { state -> transform(state).copy(notice = null) }
    }

    /**
     * Selects a focus, or asks for the unlock when the focus is not the free one.
     *
     * The swimmer is never told "no" without being told what to do about it: a locked focus is a route
     * to the screen that explains the trials, which is the whole reason the free tier stays useful.
     */
    private fun selectFocus(focus: Focus) {
        val state = mutableUiState.value
        if (focus != FREE_FOCUS && !state.isUnlocked) {
            mutableUiState.update { it.copy(navigationRequest = HomeNavigation.Unlock) }
            return
        }
        updateConstraints { it.copy(focus = focus) }
    }

    /**
     * Switches one stroke out of the session.
     *
     * Refusing the last allowed stroke is not politeness: `SetGenerator.generateSession` requires at
     * least one stroke and would throw, which is a crash bought with a checkbox. The refusal is what
     * keeps the free tier's only session-producing path safe.
     */
    private fun toggleStrokeToAvoid(stroke: Stroke) {
        val avoid = mutableUiState.value.strokesToAvoid
        val next = if (stroke in avoid) avoid - stroke else avoid + stroke
        if (next.size == Stroke.entries.size) {
            mutableUiState.update { it.copy(notice = HomeNotice.KeepOneStroke) }
            return
        }
        updateConstraints { it.copy(strokesToAvoid = next) }
    }

    private fun toggleGear(gear: Gear) {
        val onDeck = mutableUiState.value.gearOnDeck
        val next = if (gear in onDeck) onDeck - gear else onDeck + gear
        updateConstraints { it.copy(gearOnDeck = next) }
    }

    // -----------------------------------------------------------------------------------------
    // Generating, and staying in step with the store
    // -----------------------------------------------------------------------------------------

    /**
     * Builds the session from the constraints as they stand.
     *
     * The session and the position are written together, because a new session with the old position
     * is a position in a different set list. When the generator returns a session equal to the one on
     * screen — same constraints, same pace — the swimmer's place in it is kept rather than reset: the
     * generator is deterministic, so the set list they are halfway through is the one being rebuilt.
     *
     * A refusal from the generator is a notice and not a crash, even though the form is built so the
     * only reachable refusal is "every stroke avoided" (which [toggleStrokeToAvoid] already refuses).
     * The generator is right to throw when it is handed constraints no session can be invented from,
     * and a `require` failing inside a `when` branch would take the screen down with it.
     */
    private fun generate() {
        val state = mutableUiState.value
        val constraints = state.constraints
        val pace = state.activePacePer100Seconds

        val generated = try {
            SetGenerator.generateSession(constraints, pace)
        } catch (_: IllegalArgumentException) {
            mutableUiState.update { current -> current.copy(notice = HomeNotice.GenerationFailed) }
            return
        }

        val keptPosition = state.session
            ?.takeIf { it.constraints == constraints && it.pacePer100Seconds == pace }
            ?.let { state.position.clampedTo(generated) }
            ?: SessionPosition.Start

        mutableUiState.update { current ->
            current.copy(
                session = generated,
                position = keptPosition,
                isExportSheetVisible = false,
                notice = null,
            )
        }
        persistSession(generated, keptPosition)
    }

    /** One repetition further into the session on screen. */
    private fun advance() {
        val state = mutableUiState.value
        val session = state.session ?: return
        val next = session.advance(state.position)
        if (next == state.position) return
        mutableUiState.update { it.copy(position = next) }
        persistPosition(next)
    }

    /** One repetition back. */
    private fun retreat() {
        val state = mutableUiState.value
        val session = state.session ?: return
        val next = session.retreat(state.position)
        if (next == state.position) return
        mutableUiState.update { it.copy(position = next) }
        persistPosition(next)
    }

    /**
     * Throws the session away.
     *
     * The store's own flow drives the state here rather than a local edit: the store is cleared first
     * and its `null` emission is what removes the session, so the screen can never show a session the
     * store has already forgotten.
     */
    private fun discard() {
        mutableUiState.update {
            it.copy(session = null, position = SessionPosition.Start, isExportSheetVisible = false, notice = null)
        }
        val store = sessionStore ?: return
        viewModelScope.launch { runCatching { store.clear() } }
    }

    /**
     * Writes the session and the position.
     *
     * A failed write is swallowed rather than rethrown, and the reason is not convenience: a
     * `DataStore` write can fail with an `IOException`, and an exception thrown out of `viewModelScope`
     * takes the app down — losing a session is recoverable, a crash mid-session is not. The in-memory
     * session is untouched, so the swimmer keeps swimming and the next write is a fresh attempt at the
     * same payload.
     */
    private fun persistSession(session: Session, position: SessionPosition) {
        val store = sessionStore ?: return
        viewModelScope.launch { runCatching { store.save(session, position) } }
    }

    /** Writes the position without touching the stored session. */
    private fun persistPosition(position: SessionPosition) {
        val store = sessionStore ?: return
        viewModelScope.launch { runCatching { store.savePosition(position) } }
    }

    // -----------------------------------------------------------------------------------------
    // The paid surface. One question — is the unlock owned? — asked in one place.
    // -----------------------------------------------------------------------------------------

    /**
     * Runs [action] when the unlock is owned, and asks for the unlock when it is not.
     *
     * Both callers are handoffs the swimmer can see: the export button and the import button are on
     * screen whether or not they are owned, so a locked tap is a route to the explanation rather than
     * a control that is not there.
     */
    private fun requestHandoff(action: (HomeUiState) -> HomeUiState) {
        val state = mutableUiState.value
        if (state.session == null) return
        if (!state.isUnlocked) {
            mutableUiState.update { it.copy(navigationRequest = HomeNavigation.Unlock) }
            return
        }
        mutableUiState.update(action)
    }

    /** Routes to [destination] when the unlock is owned, and to the unlock screen when it is not. */
    private fun navigateIfUnlocked(destination: HomeNavigation) {
        val unlocked = mutableUiState.value.isUnlocked
        mutableUiState.update {
            it.copy(navigationRequest = if (unlocked) destination else HomeNavigation.Unlock)
        }
    }

    companion object {
        /**
         * Builds the ViewModel against the process-wide graph.
         *
         * The stores are the same instances every other screen reads — `MainActivity` builds them in
         * `onCreate`, before the first composition, and `AppGraph.init` is idempotent — so there is one
         * `DataStore` per file behind one wrapper and no second source of a session or a pace. There is
         * no `Context` parameter because there is nothing here to scope to an activity: the graph is
         * built from the application context already, and taking an activity context here would be the
         * leak this factory exists to avoid.
         *
         * `AppGraph.isReady` is read rather than assumed: the graph is `lateinit`, and reading it before
         * `init` throws. A graph that is not ready yields a ViewModel with no stores — the form and the
         * empty state still render, which is a usable screen instead of a crash on a cold start that a
         * future caller has reordered.
         */
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val graph = AppGraph.takeIf { it.isReady }
                return HomeViewModel(
                    paceStore = graph?.pace,
                    sessionStore = graph?.session,
                    billing = graph?.billing,
                ) as T
            }
        }
    }
}
