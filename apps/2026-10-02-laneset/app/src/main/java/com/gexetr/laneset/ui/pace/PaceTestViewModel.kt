package com.gexetr.laneset.ui.pace

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.laneset.data.AppGraph
import com.gexetr.laneset.data.DataStorePaceStore
import com.gexetr.laneset.data.PaceProfile
import com.gexetr.laneset.data.PaceStore
import com.gexetr.laneset.domain.PaceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The pace screen's state holder.
 *
 * Two sources make one state. The persisted [PaceStore.profile] is the truth the app runs on — the
 * two trials and the pace the store derived from them — and is forwarded rather than mirrored, for
 * the reason `SettingsViewModel` already states: a `MutableStateFlow` copy of a persisted value is a
 * second truth that drifts. The local draft is the *editing session*: what is typed, which step is
 * open, and what is wrong with the entry. Neither can be derived from the other, so both are held,
 * and `combine` is the one place they meet.
 *
 * The arithmetic is never re-derived here. Both validation and the write go through
 * [PaceModel.thresholdPacePer100] — called directly so an impossible pair is reported to the swimmer
 * before anything is written, and again inside `PaceStore.setTimeTrials`, which is where the number
 * that is stored comes from. There is one formula and the domain owns it
 * (`.cursor/rules/verification-discipline.mdc`, "one source of truth, imported — never restated").
 */
class PaceTestViewModel(private val store: PaceStore) : ViewModel() {

    private val draft = MutableStateFlow(PaceTestDraft())

    private val stored: StateFlow<PaceProfile> = store.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = PaceProfile.EMPTY,
    )

    val uiState: StateFlow<PaceTestUiState> = combine(stored, draft) { profile, local ->
        PaceTestUiState(
            profile = profile,
            savedTrials = local.savedTrials,
            // A null step means "wherever the stored profile says to start": the result when the
            // swimmer already has a pace, the first trial when they do not.
            step = local.step ?: defaultStep(profile),
            time400Input = local.time400Input,
            time200Input = local.time200Input,
            error = local.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = PaceTestUiState(),
    )

    fun onEvent(event: PaceTestEvent) {
        when (event) {
            is PaceTestEvent.Time400Changed ->
                draft.update { it.copy(time400Input = event.value, error = null) }

            is PaceTestEvent.Time200Changed ->
                draft.update { it.copy(time200Input = event.value, error = null) }

            PaceTestEvent.SubmitTime400 -> submitTime400()

            PaceTestEvent.SubmitTime200 -> submitTime200()

            PaceTestEvent.BackToTime400 ->
                draft.update { it.copy(step = PaceTestStep.FOUR_HUNDRED, error = null) }

            PaceTestEvent.Retry -> retry()

            PaceTestEvent.Reset -> reset()
        }
    }

    /** Step 1: keep the parsed 400 m time for step 2, or say why the entry cannot be used. */
    private fun submitTime400() {
        val seconds = parsePaceTime(draft.value.time400Input)
        when {
            seconds == null -> draft.update { it.copy(error = PaceTestError.Unreadable) }
            seconds <= 0.0 -> draft.update { it.copy(error = PaceTestError.NotPositive) }
            else -> draft.update {
                it.copy(
                    step = PaceTestStep.TWO_HUNDRED,
                    time400Seconds = seconds,
                    error = null,
                )
            }
        }
    }

    /** Step 2: compute and save the pair, or say why the pair cannot produce a pace. */
    private fun submitTime200() {
        val time400 = draft.value.time400Seconds ?: return
        val time200 = parsePaceTime(draft.value.time200Input)
        when {
            time200 == null -> draft.update { it.copy(error = PaceTestError.Unreadable) }
            time200 <= 0.0 -> draft.update { it.copy(error = PaceTestError.NotPositive) }
            else -> save(time400, time200)
        }
    }

    private fun save(time400: Double, time200: Double) {
        // The domain's own guard decides whether this pair can yield a positive pace. A pair it
        // refuses is reported to the swimmer — and nothing is written — instead of crashing the
        // screen or silently keeping the old pace (`domain/PaceModel.kt`:41).
        val threshold = try {
            PaceModel.thresholdPacePer100(time400, time200)
        } catch (_: IllegalArgumentException) {
            draft.update { it.copy(error = PaceTestError.NotSlower(time400, time200)) }
            return
        }
        viewModelScope.launch {
            store.setTimeTrials(time400, time200)
            draft.update {
                it.copy(
                    step = PaceTestStep.RESULT,
                    savedTrials = PaceProfile(
                        timeTrial400Seconds = time400,
                        timeTrial200Seconds = time200,
                        thresholdPacePer100Seconds = threshold,
                    ),
                    error = null,
                )
            }
        }
    }

    /** Re-run the flow with the stored trials already in the fields, so a retry is an edit. */
    private fun retry() {
        val current = draft.value.savedTrials ?: stored.value
        draft.update {
            it.copy(
                step = PaceTestStep.FOUR_HUNDRED,
                time400Input = current.timeTrial400Seconds?.let(::formatPaceTime).orEmpty(),
                time200Input = current.timeTrial200Seconds?.let(::formatPaceTime).orEmpty(),
                time400Seconds = current.timeTrial400Seconds,
                savedTrials = null,
                error = null,
            )
        }
    }

    /** Forget the test. The step is pinned to the first trial so the cleared state shows at once. */
    private fun reset() {
        draft.value = PaceTestDraft(step = PaceTestStep.FOUR_HUNDRED)
        viewModelScope.launch { store.clear() }
    }

    private fun defaultStep(profile: PaceProfile): PaceTestStep =
        if (profile.hasTrials) PaceTestStep.RESULT else PaceTestStep.FOUR_HUNDRED

    companion object {
        /**
         * Builds the ViewModel over the app's own pace store.
         *
         * The shared graph is read rather than a second store constructed, because `AppGraph` is
         * where the host builds the stores once. `AppGraph.init` is deliberately **not** called here
         * — the host owns the graph's lifetime, and a ViewModel that initialised it would be a second
         * initialiser racing the first. When the graph has not been built yet (a preview, or a screen
         * reached before `onCreate` finished), the fallback wraps the *same* DataStore file:
         * `preferencesDataStore` caches one instance per file, so this is one writer and not two.
         */
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PaceTestViewModel(paceStore(applicationContext)) as T
            }
        }

        private fun paceStore(applicationContext: Context): PaceStore =
            if (AppGraph.isReady) AppGraph.pace else DataStorePaceStore(applicationContext)
    }
}

/**
 * The editing session, which is not persisted: what is typed, which step is open, and whether an
 * impossible pair has been refused. [step] is null while the screen is following the stored profile.
 */
private data class PaceTestDraft(
    val step: PaceTestStep? = null,
    val time400Input: String = "",
    val time200Input: String = "",
    val time400Seconds: Double? = null,
    val savedTrials: PaceProfile? = null,
    val error: PaceTestError? = null,
)
