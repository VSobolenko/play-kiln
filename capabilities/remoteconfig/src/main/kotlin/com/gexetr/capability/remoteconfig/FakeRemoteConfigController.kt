package com.gexetr.capability.remoteconfig

import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Deterministic Remote Config implementation for tests, previews, and screenshots. */
class FakeRemoteConfigController(
    seededValues: Map<String, Any> = emptyMap(),
    fetchOutcome: PlayKilnResult<Unit> = PlayKilnResult.Success(Unit),
) : RemoteConfigController {
    private val mutableAvailability = MutableStateFlow(CapabilityAvailability.UNINITIALIZED)
    override val availability: StateFlow<CapabilityAvailability> = mutableAvailability.asStateFlow()

    private var config: RemoteConfigConfig? = null
    private var pendingValues: Map<String, Any> = seededValues.toMap()
    private var activeValues: Map<String, Any> = emptyMap()
    private var scriptedFetchOutcome: PlayKilnResult<Unit> = fetchOutcome

    /** Replaces the values that the next successful [fetch] activates. */
    fun seed(values: Map<String, Any>) {
        pendingValues = values.toMap()
    }

    /** Replaces the deterministic result returned by subsequent [fetch] calls. */
    fun scriptFetch(outcome: PlayKilnResult<Unit>) {
        scriptedFetchOutcome = outcome
    }

    override fun initialize(config: RemoteConfigConfig) {
        activeValues = emptyMap()
        if (config.validate().isNotEmpty()) {
            this.config = null
            mutableAvailability.value = CapabilityAvailability.FAILED
            return
        }

        this.config = config
        mutableAvailability.value = CapabilityAvailability.READY
    }

    override suspend fun fetch(): PlayKilnResult<Unit> {
        if (config == null) {
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)
        }

        return when (val outcome = scriptedFetchOutcome) {
            is PlayKilnResult.Success -> {
                activeValues = pendingValues.toMap()
                mutableAvailability.value = CapabilityAvailability.READY
                PlayKilnResult.Success(Unit)
            }

            is PlayKilnResult.Failure -> {
                mutableAvailability.value = CapabilityAvailability.FAILED
                outcome
            }
        }
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        value(key)?.toRemoteBooleanOrNull() ?: default

    override fun getLong(key: String, default: Long): Long =
        value(key)?.toRemoteLongOrNull() ?: default

    override fun getString(key: String, default: String): String =
        value(key)?.toRemoteString() ?: default

    override fun shutdown() {
        config = null
        activeValues = emptyMap()
        mutableAvailability.value = CapabilityAvailability.UNINITIALIZED
    }

    private fun value(key: String): Any? =
        if (activeValues.containsKey(key)) activeValues[key] else config?.defaults?.get(key)
}
