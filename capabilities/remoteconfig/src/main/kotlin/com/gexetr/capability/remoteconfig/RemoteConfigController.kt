package com.gexetr.capability.remoteconfig

import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.flow.StateFlow

/** Remote feature flags and parameters with synchronous, offline-safe reads. */
interface RemoteConfigController {
    val availability: StateFlow<CapabilityAvailability>

    /** Installs in-app defaults and configures the minimum fetch interval. */
    fun initialize(config: RemoteConfigConfig)

    /** Fetches and activates remote values without throwing expected SDK/network failures. */
    suspend fun fetch(): PlayKilnResult<Unit>

    fun getBoolean(key: String, default: Boolean): Boolean

    fun getLong(key: String, default: Long): Long

    fun getString(key: String, default: String): String

    fun shutdown()
}
