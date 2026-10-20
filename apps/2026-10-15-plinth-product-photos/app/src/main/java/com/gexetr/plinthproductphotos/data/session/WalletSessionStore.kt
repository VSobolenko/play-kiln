package com.gexetr.plinthproductphotos.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.walletDataStore by preferencesDataStore(name = "wallet")

class WalletSessionStore internal constructor(
    private val dataStore: DataStore<Preferences>,
) : SessionPersistence {
    constructor(context: Context) : this(context.applicationContext.walletDataStore)

    val balance: Flow<Int?> = dataStore.data.map { preferences ->
        if (preferences[DEVICE_TOKEN] != null && preferences[WALLET_ID] != null) {
            preferences[BALANCE_CACHE]
        } else {
            null
        }
    }.distinctUntilChanged()

    override suspend fun installId(): String {
        var result: String? = null
        dataStore.edit { preferences ->
            result = preferences[INSTALL_ID] ?: UUID.randomUUID().toString().also {
                preferences[INSTALL_ID] = it
            }
        }
        return requireNotNull(result)
    }

    override suspend fun current(): WalletSession? {
        val preferences = dataStore.data.first()
        val token = preferences[DEVICE_TOKEN] ?: return null
        val walletId = preferences[WALLET_ID] ?: return null
        return WalletSession(
            deviceToken = token,
            walletId = walletId,
            tokenGeneration = preferences[TOKEN_GENERATION] ?: 0L,
            balanceCache = preferences[BALANCE_CACHE],
        )
    }

    override suspend fun saveSession(deviceToken: String, walletId: String, balance: Int) {
        dataStore.edit { preferences ->
            preferences[DEVICE_TOKEN] = deviceToken
            preferences[WALLET_ID] = walletId
            preferences[BALANCE_CACHE] = balance
            preferences[TOKEN_GENERATION] = preferences[TOKEN_GENERATION] ?: 0L
        }
    }

    suspend fun updateServerBalance(balance: Int) {
        dataStore.edit { preferences -> preferences[BALANCE_CACHE] = balance }
    }

    override suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(DEVICE_TOKEN)
            preferences.remove(WALLET_ID)
            preferences.remove(BALANCE_CACHE)
            preferences.remove(TOKEN_GENERATION)
        }
    }

    private companion object {
        val INSTALL_ID = stringPreferencesKey("installId")
        val DEVICE_TOKEN = stringPreferencesKey("deviceToken")
        val WALLET_ID = stringPreferencesKey("walletId")
        val BALANCE_CACHE = androidx.datastore.preferences.core.intPreferencesKey("balanceCache")
        val TOKEN_GENERATION = longPreferencesKey("tokenGeneration")
    }
}
