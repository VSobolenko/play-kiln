package com.gexetr.plinthproductphotos.data.session

import com.gexetr.plinthproductphotos.data.api.ApiConfigResponse
import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.IntegrityChallengeResponse
import com.gexetr.plinthproductphotos.data.api.SessionResponse
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBootstrapTest {
    private val challengeId = "12345678-1234-1234-1234-123456789abc"
    private val response = SessionResponse(
        "signed-device-token", "wallet-id", 10, true,
        ApiConfigResponse(emptyMap(), 10, false, "0", false),
    )

    @Test
    fun cleanInstallAttestsExactServerFieldsAndPersistsServerBalance() = runBlocking {
        val store = MemorySessionStore()
        var hash: String? = null
        val bootstrap = SessionBootstrap(
            store,
            getChallenge = { request ->
                assertEquals("install-1", request.installId)
                assertEquals("0.1.0", request.appVersion)
                assertEquals("ru-RU", request.locale)
                ApiResult.Success(IntegrityChallengeResponse(challengeId, "server-nonce", Long.MAX_VALUE))
            },
            createSession = { request ->
                assertEquals("ru-RU", request.locale)
                assertEquals(challengeId, request.challengeId)
                assertEquals("server-nonce", request.challenge)
                assertEquals("test-attestation", request.integrityToken)
                ApiResult.Success(response)
            },
        )
        val result = bootstrap.connect("0.1.0", "ru-RU") {
            hash = it
            "test-attestation"
        }
        assertEquals("k5zfdib4y8jP6BbPX5-_tBxb3oTTz6K6WfiVyzZLXIc", hash)
        assertEquals(SessionConnectionState.Ready(store.session!!), result)
        assertEquals(10, store.session?.balanceCache)
        assertEquals("signed-device-token", store.session?.deviceToken)
        assertEquals(1, store.saves)
    }

    @Test
    fun persistedSessionSkipsChallengeAndIntegrityOnSubsequentForegrounds() = runBlocking {
        val store = MemorySessionStore()
        store.session = WalletSession("existing-token", "existing-wallet", 3L, 42)
        val bootstrap = SessionBootstrap(
            store,
            getChallenge = { error("Existing sessions must not create another wallet") },
            createSession = { error("Existing sessions must not create another wallet") },
        )
        repeat(2) {
            assertEquals(
                SessionConnectionState.Ready(store.session!!),
                bootstrap.connect("0.1.0", "ru-RU") { error("No attestation needed") },
            )
        }
        assertEquals(0, store.saves)
    }

    @Test
    fun failedAttestationDoesNotCreateOrSaveAndCanRetryWithAFreshChallenge() = runBlocking {
        val store = MemorySessionStore()
        var challengeCount = 0
        var sessionCount = 0
        val bootstrap = SessionBootstrap(
            store,
            getChallenge = {
                challengeCount++
                ApiResult.Success(IntegrityChallengeResponse(challengeId, "nonce-$challengeCount", Long.MAX_VALUE))
            },
            createSession = {
                sessionCount++
                assertEquals("nonce-2", it.challenge)
                ApiResult.Success(response)
            },
        )
        assertEquals(
            SessionConnectionState.Failed(SessionFailure.INTEGRITY),
            bootstrap.connect("0.1.0", "ru-RU") {
                throw IntegrityTokenException(SessionFailure.INTEGRITY)
            },
        )
        assertEquals(null, store.session)
        assertEquals(0, sessionCount)
        assertTrue(bootstrap.connect("0.1.0", "ru-RU") { "test-attestation" } is SessionConnectionState.Ready)
        assertEquals(2, challengeCount)
        assertEquals(1, sessionCount)
    }

    @Test
    fun unavailableChallengeIsAnExplicitNetworkFailureNotAProviderFailure() = runBlocking {
        val store = MemorySessionStore()
        val bootstrap = SessionBootstrap(
            store,
            getChallenge = { ApiResult.Failure(ApiFailure.Network("unreachable")) },
            createSession = { error("No challenge means no session request") },
        )
        assertEquals(
            SessionConnectionState.Failed(SessionFailure.NETWORK),
            bootstrap.connect("0.1.0", "ru-RU") { error("No challenge means no token") },
        )
        assertEquals(null, store.session)
    }

    @Test
    fun missingCloudConfigurationNeverPostsASessionOrInventsAToken() = runBlocking {
        val store = MemorySessionStore()
        val bootstrap = SessionBootstrap(
            store,
            getChallenge = {
                ApiResult.Success(IntegrityChallengeResponse(challengeId, "server-nonce", Long.MAX_VALUE))
            },
            createSession = { error("Missing Cloud project must not submit a session") },
        )
        assertEquals(
            SessionConnectionState.Failed(SessionFailure.CONFIGURATION),
            bootstrap.connect("0.1.0", "ru-RU") {
                throw IntegrityTokenException(SessionFailure.CONFIGURATION)
            },
        )
        assertEquals(null, store.session)
    }

    @Test
    fun declinedSessionNeverPersistsAWallet() = runBlocking {
        val store = MemorySessionStore()
        val bootstrap = SessionBootstrap(
            store,
            getChallenge = {
                ApiResult.Success(IntegrityChallengeResponse(challengeId, "server-nonce", Long.MAX_VALUE))
            },
            createSession = { ApiResult.Failure(ApiFailure.NotConfigured) },
        )
        assertEquals(
            SessionConnectionState.Failed(SessionFailure.CONFIGURATION),
            bootstrap.connect("0.1.0", "ru-RU") { "test-attestation" },
        )
        assertEquals(0, store.saves)
    }

    @Test
    fun zeroServerGrantIsNotReplacedWithPromisedStarterCredits() = runBlocking {
        val store = MemorySessionStore()
        val bootstrap = SessionBootstrap(
            store,
            getChallenge = {
                ApiResult.Success(IntegrityChallengeResponse(challengeId, "server-nonce", Long.MAX_VALUE))
            },
            createSession = { ApiResult.Success(response.copy(balance = 0, starterGranted = false)) },
        )
        assertTrue(bootstrap.connect("0.1.0", "ru-RU") { "test-attestation" } is SessionConnectionState.Ready)
        assertEquals(0, store.session?.balanceCache)
    }

    @Test
    fun concurrentBootstrapsCreateOnlyOneWallet() = runBlocking {
        val store = MemorySessionStore()
        var calls = 0
        val bootstrap = SessionBootstrap(
            store,
            getChallenge = {
                ApiResult.Success(IntegrityChallengeResponse(challengeId, "server-nonce", Long.MAX_VALUE))
            },
            createSession = { calls++; ApiResult.Success(response) },
        )
        val first = async { bootstrap.connect("0.1.0", "ru-RU") { delay(10); "test-attestation" } }
        val second = async { bootstrap.connect("0.1.0", "ru-RU") { "test-attestation" } }
        assertTrue(first.await() is SessionConnectionState.Ready)
        assertTrue(second.await() is SessionConnectionState.Ready)
        assertEquals(1, calls)
        assertEquals(1, store.saves)
    }

    @Test
    fun cancellationIsNotConvertedToAConnectionFailure() = runBlocking {
        val bootstrap = SessionBootstrap(
            MemorySessionStore(),
            getChallenge = {
                ApiResult.Success(IntegrityChallengeResponse(challengeId, "server-nonce", Long.MAX_VALUE))
            },
            createSession = { error("Cancelled attestation cannot submit") },
        )
        var cancelled = false
        try {
            bootstrap.connect("0.1.0", "ru-RU") { throw CancellationException("test cancellation") }
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }

    @Test
    fun failedPersistenceDoesNotReportAReadySession() = runBlocking {
        val store = object : SessionPersistence {
            override suspend fun current(): WalletSession? = null
            override suspend fun installId(): String = "install-1"
            override suspend fun saveSession(deviceToken: String, walletId: String, balance: Int) {
                throw IOException("disk full")
            }
            override suspend fun clearSession() = Unit
        }
        val bootstrap = SessionBootstrap(
            store,
            getChallenge = {
                ApiResult.Success(IntegrityChallengeResponse(challengeId, "server-nonce", Long.MAX_VALUE))
            },
            createSession = { ApiResult.Success(response) },
        )
        val result = bootstrap.connect("0.1.0", "ru-RU") { "test-attestation" }
        assertFalse(result is SessionConnectionState.Ready)
        assertEquals(SessionConnectionState.Failed(SessionFailure.STORAGE), result)
    }

    @Test
    fun forcedReattestationDropsTheOldTokenAndBindsTheInstalledVersion() = runBlocking {
        val store = MemorySessionStore()
        store.session = WalletSession("stale-token", "stale-wallet", 1L, 10)
        var attestedVersion: String? = null
        val bootstrap = SessionBootstrap(
            store,
            getChallenge = { request ->
                attestedVersion = request.appVersion
                ApiResult.Success(IntegrityChallengeResponse(challengeId, "server-nonce", Long.MAX_VALUE))
            },
            createSession = { ApiResult.Success(response) },
        )
        val result = bootstrap.connect(
            appVersion = "1.4.0",
            locale = "ru-RU",
            integrity = { "test-attestation" },
            forceReattestation = true,
        )
        assertTrue(result is SessionConnectionState.Ready)
        assertEquals("1.4.0", attestedVersion)
        assertEquals(1, store.clears)
        assertEquals("signed-device-token", store.session?.deviceToken)
    }

    private class MemorySessionStore : SessionPersistence {
        var session: WalletSession? = null
        var saves = 0
        var clears = 0
        override suspend fun installId(): String = "install-1"
        override suspend fun current(): WalletSession? = session
        override suspend fun saveSession(deviceToken: String, walletId: String, balance: Int) {
            saves++
            session = WalletSession(deviceToken, walletId, 0L, balance)
        }

        override suspend fun clearSession() {
            clears++
            session = null
        }
    }
}
