package com.gexetr.plinthproductphotos.data.session

import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.IntegrityChallengeRequest
import com.gexetr.plinthproductphotos.data.api.IntegrityChallengeResponse
import com.gexetr.plinthproductphotos.data.api.SessionRequest
import com.gexetr.plinthproductphotos.data.api.SessionResponse
import java.io.IOException
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface IntegrityTokenProvider {
    suspend fun token(requestHash: String): String
}

enum class SessionFailure {
    CONFIGURATION,
    INTEGRITY,
    NETWORK,
    SERVER,
    STORAGE,
}

class IntegrityTokenException(val reason: SessionFailure, cause: Throwable? = null) :
    Exception(reason.name, cause)

sealed interface SessionConnectionState {
    data object Connecting : SessionConnectionState
    data class Ready(val session: WalletSession) : SessionConnectionState
    data class Failed(val reason: SessionFailure) : SessionConnectionState
}

internal fun sessionRequestHash(request: SessionRequest): String {
    val canonical = listOf(
        "session",
        request.challengeId,
        request.challenge,
        request.installId,
        request.appVersion,
        request.locale,
    ).joinToString("\n")
    return Base64.getUrlEncoder().withoutPadding().encodeToString(
        MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8)),
    )
}

class SessionBootstrap(
    private val store: SessionPersistence,
    private val getChallenge: (IntegrityChallengeRequest) -> ApiResult<IntegrityChallengeResponse>,
    private val createSession: (SessionRequest) -> ApiResult<SessionResponse>,
) {
    suspend fun connect(
        appVersion: String,
        locale: String,
        forceReattestation: Boolean = false,
        integrity: IntegrityTokenProvider,
    ): SessionConnectionState = creationLock.withLock {
        try {
            if (forceReattestation) store.clearSession()
            store.current()?.let { return@withLock SessionConnectionState.Ready(it) }
            val installId = store.installId()
            val challenge = when (val result = getChallenge(
                IntegrityChallengeRequest(installId, appVersion, locale),
            )) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure -> return@withLock failed(result.failure)
            }
            val unsigned = SessionRequest(
                installId = installId,
                appVersion = appVersion,
                locale = locale,
                integrityToken = "",
                challengeId = challenge.challengeId,
                challenge = challenge.challenge,
            )
            val token = integrity.token(sessionRequestHash(unsigned))
            when (val result = createSession(unsigned.copy(integrityToken = token))) {
                is ApiResult.Failure -> failed(result.failure)
                is ApiResult.Success -> {
                    val session = result.value
                    store.saveSession(session.deviceToken, session.walletId, session.balance)
                    SessionConnectionState.Ready(
                        WalletSession(session.deviceToken, session.walletId, 0L, session.balance),
                    )
                }
            }
        } catch (error: IntegrityTokenException) {
            SessionConnectionState.Failed(error.reason)
        } catch (_: IOException) {
            SessionConnectionState.Failed(SessionFailure.STORAGE)
        }
    }

    private fun failed(failure: ApiFailure): SessionConnectionState.Failed =
        SessionConnectionState.Failed(
            when (failure) {
                ApiFailure.NotConfigured -> SessionFailure.CONFIGURATION
                is ApiFailure.Network -> SessionFailure.NETWORK
                is ApiFailure.Unknown -> if (failure.code == "INTEGRITY_INVALID") {
                    SessionFailure.INTEGRITY
                } else {
                    SessionFailure.SERVER
                }
                else -> SessionFailure.SERVER
            },
        )

    private companion object {
        // Activities can be recreated while an attestation is in flight.
        val creationLock = Mutex()
    }
}
