package com.gexetr.laneset.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.gexetr.laneset.domain.PaceModel
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The swimmer's two time trials and the threshold pace they imply, as one value.
 *
 * All three numbers are kept, not just the pace, because the time trials are the swimmer's own
 * measurement and the pace is a *derived* number. Keeping the inputs means the pace can be
 * recomputed when the model changes, and the settings screen can show the swimmer the two times
 * they actually swam rather than only the number that was derived from them.
 *
 * A profile is either complete or empty: a half-written record (one trial missing) reads as
 * [hasTrials] `false` rather than as a pace computed from a missing input.
 */
data class PaceProfile(
    val timeTrial400Seconds: Double? = null,
    val timeTrial200Seconds: Double? = null,
    val thresholdPacePer100Seconds: Double? = null,
) {
    /** True only when both trials and the derived pace are present. */
    val hasTrials: Boolean
        get() = timeTrial400Seconds != null &&
            timeTrial200Seconds != null &&
            thresholdPacePer100Seconds != null

    companion object {
        val EMPTY = PaceProfile()
    }
}

/**
 * Persists the threshold pace test so the swimmer never has to swim it twice.
 *
 * The computation is not repeated here: the writer hands both times to [PaceModel] and stores the
 * number the model returns. If the two times are impossible — a 400 that is not slower than the 200
 * — the model's own `require` throws and nothing is written, because a silently wrong target would
 * print on every interval of every session afterwards.
 */
interface PaceStore {
    /** The stored profile, or [PaceProfile.EMPTY] when none has been recorded. */
    val profile: Flow<PaceProfile>

    /** Computes the threshold pace through [PaceModel] and stores both trials and the result. */
    suspend fun setTimeTrials(timeTrial400Seconds: Double, timeTrial200Seconds: Double)

    /** Forgets the test, e.g. after the swimmer's times change. */
    suspend fun clear()
}

/**
 * The single DataStore instance for the pace test.
 *
 * A separate file from the preferences on purpose: the two have different lifetimes (a threshold
 * pace outlives a theme change) and one corrupt file should not take the other down with it.
 */
private val Context.paceDataStore by preferencesDataStore(name = "pace")

/**
 * The DataStore-backed implementation of [PaceStore].
 *
 * The read degrades the same way the settings store does — an [IOException] becomes
 * [emptyPreferences], so a corrupt file reads as "no test recorded" instead of taking the screen
 * down — and the three values are written in a single `edit { }` so a reader never observes a 400 m
 * time without its matching 200 m time.
 */
class DataStorePaceStore(private val context: Context) : PaceStore {

    override val profile: Flow<PaceProfile> = context.paceDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences ->
            val time400 = preferences[TIME_400]
            val time200 = preferences[TIME_200]
            val threshold = preferences[THRESHOLD]
            if (time400 == null || time200 == null || threshold == null) {
                PaceProfile.EMPTY
            } else {
                PaceProfile(
                    timeTrial400Seconds = time400,
                    timeTrial200Seconds = time200,
                    thresholdPacePer100Seconds = threshold,
                )
            }
        }

    override suspend fun setTimeTrials(timeTrial400Seconds: Double, timeTrial200Seconds: Double) {
        // The computation stays in the domain model; duplicating `(t400 - t200) / 2` here would be a
        // second copy of the formula that could drift from the one the tests exercise.
        val threshold = PaceModel.thresholdPacePer100(timeTrial400Seconds, timeTrial200Seconds)
        context.paceDataStore.edit { preferences ->
            preferences[TIME_400] = timeTrial400Seconds
            preferences[TIME_200] = timeTrial200Seconds
            preferences[THRESHOLD] = threshold
        }
    }

    override suspend fun clear() {
        context.paceDataStore.edit { preferences ->
            preferences.remove(TIME_400)
            preferences.remove(TIME_200)
            preferences.remove(THRESHOLD)
        }
    }

    private companion object {
        val TIME_400 = doublePreferencesKey("time_trial_400_seconds")
        val TIME_200 = doublePreferencesKey("time_trial_200_seconds")
        val THRESHOLD = doublePreferencesKey("threshold_pace_per_100_seconds")
    }
}
