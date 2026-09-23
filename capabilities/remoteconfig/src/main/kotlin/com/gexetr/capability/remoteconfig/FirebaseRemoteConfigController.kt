package com.gexetr.capability.remoteconfig

import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Real [RemoteConfigController] backed by the Firebase Remote Config SDK. */
class FirebaseRemoteConfigController : RemoteConfigController {
    private val mutableAvailability = MutableStateFlow(CapabilityAvailability.UNINITIALIZED)
    override val availability: StateFlow<CapabilityAvailability> = mutableAvailability.asStateFlow()

    private var config: RemoteConfigConfig? = null
    private var firebase: FirebaseRemoteConfig? = null
    private var setupTasks: List<Task<Void>> = emptyList()

    override fun initialize(config: RemoteConfigConfig) {
        shutdown()
        if (config.validate().isNotEmpty()) {
            mutableAvailability.value = CapabilityAvailability.FAILED
            return
        }

        // Retain defaults before touching Firebase. Reads remain safe even when getInstance()
        // throws because a debug build has no google-services.json/default FirebaseApp.
        this.config = config
        try {
            val instance = FirebaseRemoteConfig.getInstance()
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(config.minimumFetchIntervalSeconds)
                .build()

            firebase = instance
            setupTasks = listOf(
                instance.setDefaultsAsync(config.defaults),
                instance.setConfigSettingsAsync(settings),
            )
            mutableAvailability.value = CapabilityAvailability.READY
        } catch (_: Exception) {
            firebase = null
            setupTasks = emptyList()
            mutableAvailability.value = CapabilityAvailability.FAILED
        }
    }

    override suspend fun fetch(): PlayKilnResult<Unit> {
        val currentConfig = config
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)
        val instance = firebase
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)

        return try {
            // Defaults and fetch settings must finish before the first network fetch.
            setupTasks.forEach { it.awaitCompletion() }
            instance.fetch(currentConfig.minimumFetchIntervalSeconds).awaitCompletion()
            instance.activate().awaitCompletion()
            mutableAvailability.value = CapabilityAvailability.READY
            PlayKilnResult.Success(Unit)
        } catch (exception: Exception) {
            mutableAvailability.value = CapabilityAvailability.FAILED
            PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK, exception)
        }
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        remoteValue(key) { it.getBoolean(key) }
            ?: config?.defaults?.get(key)?.toRemoteBooleanOrNull()
            ?: default

    override fun getLong(key: String, default: Long): Long =
        remoteValue(key) { it.getLong(key) }
            ?: config?.defaults?.get(key)?.toRemoteLongOrNull()
            ?: default

    override fun getString(key: String, default: String): String =
        remoteValue(key) { it.getString(key) }
            ?: config?.defaults?.get(key)?.toRemoteString()
            ?: default

    override fun shutdown() {
        config = null
        firebase = null
        setupTasks = emptyList()
        mutableAvailability.value = CapabilityAvailability.UNINITIALIZED
    }

    private inline fun <T> remoteValue(
        key: String,
        read: (FirebaseRemoteConfig) -> T,
    ): T? {
        val instance = firebase ?: return null
        return try {
            if (instance.getValue(key).source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
                read(instance)
            } else {
                null
            }
        } catch (_: RuntimeException) {
            null
        }
    }
}

private suspend fun <T> Task<T>.awaitCompletion(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            val exception = task.exception
            if (exception != null) {
                continuation.resumeWithException(exception)
            } else {
                continuation.resume(task.result)
            }
        }
    }
