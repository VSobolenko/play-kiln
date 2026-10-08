package com.gexetr.unitwalk.ui.review

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.gexetr.unitwalk.data.ReviewPromptStore
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Google Play In-App Review: ReviewManager first, Play Store listing only as fallback.
 *
 * Soft prompts go through [maybeSoftPromptAfterSuccess]. An explicit Settings "Rate app" tap goes
 * through [requestReviewFromSettings], which always attempts the API and falls back to
 * `market://details` / the HTTPS store URL when the flow fails to start.
 *
 * Never show a custom star rating UI before this API — Play owns the dialog.
 */
object InAppReview {
    fun maybeSoftPromptAfterSuccess(
        activity: Activity,
        store: ReviewPromptStore,
        scope: CoroutineScope,
    ) {
        scope.launch {
            if (!store.shouldSoftPrompt()) return@launch
            launchReviewFlow(activity)
            store.markPrompted()
            // Soft prompt: do not bounce the user to the store when Play declines the dialog.
        }
    }

    fun requestReviewFromSettings(
        activity: Activity,
        store: ReviewPromptStore,
        scope: CoroutineScope,
    ) {
        scope.launch {
            val started = launchReviewFlow(activity)
            store.markPrompted()
            if (!started) {
                withContext(Dispatchers.Main) {
                    openPlayStoreListing(activity)
                }
            }
        }
    }

    /**
     * @return true when `launchReviewFlow` completed successfully; false when the request failed
     *   or Play returned no usable ReviewInfo (quota / unavailable).
     */
    private suspend fun launchReviewFlow(activity: Activity): Boolean =
        withContext(Dispatchers.Main) {
            try {
                val manager = ReviewManagerFactory.create(activity)
                val reviewInfo = manager.requestReviewFlow().awaitOrNull() ?: return@withContext false
                manager.launchReviewFlow(activity, reviewInfo).awaitOrNull() != null
            } catch (_: Exception) {
                false
            }
        }

    fun openPlayStoreListing(context: Context) {
        val packageName = context.packageName.removeSuffix(".debug")
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val https = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            runCatching { context.startActivity(https) }
        }
    }
}

private suspend fun <T> Task<T>.awaitOrNull(): T? =
    suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (cont.isActive) {
                cont.resume(if (task.isSuccessful) task.result else null)
            }
        }
    }
