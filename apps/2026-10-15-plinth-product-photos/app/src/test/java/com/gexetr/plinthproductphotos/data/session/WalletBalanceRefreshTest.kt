package com.gexetr.plinthproductphotos.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletBalanceRefreshTest {
    @Test
    fun chipBalanceFollowsStudioChargeAndWalletRefreshWithoutResubscribing() = runBlocking {
        val dataStore = MemoryPreferencesDataStore()
        val shellStore = WalletSessionStore(dataStore)
        val studioStore = WalletSessionStore(dataStore)
        shellStore.saveSession("test-token", "test-wallet", 9)
        val balances = Channel<Int?>(Channel.UNLIMITED)
        val collector = launch {
            shellStore.balance.collect { balances.send(it) }
        }
        suspend fun nextBalance(): Int? = withTimeout(5_000) { balances.receive() }
        try {
            assertEquals(9, nextBalance())

            studioStore.updateServerBalance(4)
            assertEquals(4, nextBalance())
            assertEquals(4, shellStore.current()?.balanceCache)

            shellStore.updateServerBalance(24)
            assertEquals(24, nextBalance())
            shellStore.updateServerBalance(0)
            assertEquals(0, nextBalance())

            shellStore.clearSession()
            assertEquals(null, nextBalance())
        } finally {
            collector.cancel()
        }
    }
}

private class MemoryPreferencesDataStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = mutex.withLock {
        transform(data.value).toPreferences().also { data.value = it }
    }
}
