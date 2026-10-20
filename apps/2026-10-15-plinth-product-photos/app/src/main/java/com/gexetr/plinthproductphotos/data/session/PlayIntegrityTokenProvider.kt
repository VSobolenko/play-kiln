package com.gexetr.plinthproductphotos.data.session

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class PlayIntegrityTokenProvider(
    context: Context,
    private val cloudProjectNumber: Long,
) : IntegrityTokenProvider {
    private val manager = IntegrityManagerFactory.createStandard(context.applicationContext)

    override suspend fun token(requestHash: String): String {
        if (cloudProjectNumber <= 0L) throw IntegrityTokenException(SessionFailure.CONFIGURATION)
        return withTimeoutOrNull(60_000L) {
            val provider = manager.prepareIntegrityToken(
                PrepareIntegrityTokenRequest.builder()
                    .setCloudProjectNumber(cloudProjectNumber)
                    .build(),
            ).awaitIntegrity()
            provider.request(
                StandardIntegrityTokenRequest.builder().setRequestHash(requestHash).build(),
            ).awaitIntegrity().token()
        } ?: throw IntegrityTokenException(SessionFailure.INTEGRITY)
    }

    private suspend fun <T> Task<T>.awaitIntegrity(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value ->
            if (continuation.isActive) continuation.resume(value)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(IntegrityTokenException(SessionFailure.INTEGRITY, error))
            }
        }
        addOnCanceledListener {
            if (continuation.isActive) {
                continuation.resumeWithException(IntegrityTokenException(SessionFailure.INTEGRITY))
            }
        }
    }
}
