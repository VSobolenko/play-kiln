package com.gexetr.plinthproductphotos.data.session

data class WalletSession(
    val deviceToken: String,
    val walletId: String,
    val tokenGeneration: Long,
    val balanceCache: Int?,
)

interface SessionPersistence {
    suspend fun installId(): String
    suspend fun current(): WalletSession?
    suspend fun saveSession(deviceToken: String, walletId: String, balance: Int)

    /**
     * Drops the stored token, wallet id and cached balance.
     *
     * Called only when re-attesting after `426 UPDATE_REQUIRED`, because the version the server
     * compares lives inside the signed token and an app update does not change an existing one.
     */
    suspend fun clearSession()
}
