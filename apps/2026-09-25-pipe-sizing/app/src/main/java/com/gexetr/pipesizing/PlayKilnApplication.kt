package com.gexetr.pipesizing

import android.app.Application
import com.gexetr.pipesizing.monetization.PlayKilnContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Owns the ads and billing clients for the process.
 *
 * The container is built here rather than in an Activity so a configuration change does not
 * reconnect to Play or re-initialise the Ads SDK, and hydrating the persisted entitlement starts
 * before any Activity can ask whether the user owns Pro.
 */
class PlayKilnApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val container: PlayKilnContainer by lazy { PlayKilnContainer.create(this, applicationScope) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { container.prepare() }
    }
}
