package com.gexetr.unitwalk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.days

private val Context.reviewPromptStore by preferencesDataStore(name = "review_prompts")

/**
 * Persists soft In-App Review throttle state so prompts stay moderate.
 *
 * Policy (also in `.cursor/rules/in-app-review.mdc`): soft-prompt after the first successful
 * primary action when the user has not completed a review flow and was not prompted in the last
 * [SOFT_PROMPT_COOLDOWN]. Settings "Rate app" always attempts the Review API and records a prompt
 * even inside the cooldown.
 */
class ReviewPromptStore(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.reviewPromptStore)

    suspend fun shouldSoftPrompt(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val prefs = dataStore.data.first()
        if (prefs[REVIEW_COMPLETED] == true) return false
        val last = prefs[LAST_PROMPTED_AT] ?: 0L
        if (last == 0L) return true
        return nowMillis - last >= SOFT_PROMPT_COOLDOWN.inWholeMilliseconds
    }

    suspend fun markPrompted(nowMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            prefs[LAST_PROMPTED_AT] = nowMillis
        }
    }

    suspend fun markCompleted() {
        dataStore.edit { prefs ->
            prefs[REVIEW_COMPLETED] = true
        }
    }

    companion object {
        /** Soft prompts at most once per 90 days unless the user already completed a flow. */
        val SOFT_PROMPT_COOLDOWN = 90.days

        private val LAST_PROMPTED_AT = longPreferencesKey("last_prompted_at")
        private val REVIEW_COMPLETED = booleanPreferencesKey("review_completed")
    }
}
