package com.gexetr.plinthproductphotos.data.outbox

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gexetr.plinthproductphotos.data.wallet.AndroidKeystoreVault
import com.gexetr.plinthproductphotos.data.wallet.sha256Hex

@Entity(
    tableName = "pending_purchases",
    indices = [Index(value = ["tokenHash"], unique = true)],
)
data class PurchaseOutboxEntity(
    @PrimaryKey val tokenHash: String,
    val productId: String,
    val intentId: String,
    val tokenCiphertext: ByteArray?,
    val state: String,
    val createdAt: Long,
)

@Dao
interface PurchaseOutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PurchaseOutboxEntity): Long

    @Query("SELECT * FROM pending_purchases WHERE state = 'PENDING' ORDER BY createdAt")
    suspend fun pending(): List<PurchaseOutboxEntity>

    @Query(
        "UPDATE pending_purchases SET state = :state, tokenCiphertext = NULL " +
            "WHERE tokenHash = :tokenHash AND state = 'PENDING'",
    )
    suspend fun markTerminal(tokenHash: String, state: String): Int
}

@Database(entities = [PurchaseOutboxEntity::class], version = 1, exportSchema = true)
abstract class PurchaseOutboxDatabase : RoomDatabase() {
    abstract fun purchases(): PurchaseOutboxDao

    companion object {
        @Volatile
        private var instance: PurchaseOutboxDatabase? = null

        fun get(context: Context): PurchaseOutboxDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PurchaseOutboxDatabase::class.java,
                "wallet_outbox.db",
            ).build().also { instance = it }
        }
    }
}

data class PendingPurchase(
    val tokenHash: String,
    val productId: String,
    val intentId: String,
    val purchaseToken: String,
)

class PurchaseOutbox(
    context: Context,
    private val database: PurchaseOutboxDatabase = PurchaseOutboxDatabase.get(context),
    private val vault: AndroidKeystoreVault = AndroidKeystoreVault(TOKEN_KEY_ALIAS),
) {
    private val appContext = context.applicationContext

    init {
        enqueueUniqueRetry(appContext)
    }

    suspend fun enqueue(productId: String, intentId: String, purchaseToken: String) {
        require(productId.isNotBlank() && intentId.isNotBlank() && purchaseToken.isNotBlank()) {
            "Purchase outbox requires product, intent, and token"
        }
        val tokenHash = sha256Hex(purchaseToken)
        val inserted = database.purchases().insert(
            PurchaseOutboxEntity(
                tokenHash = tokenHash,
                productId = productId,
                intentId = intentId,
                tokenCiphertext = vault.encrypt(purchaseToken.toByteArray(Charsets.UTF_8)),
                state = STATE_PENDING,
                createdAt = System.currentTimeMillis(),
            ),
        )
        if (inserted != -1L) enqueueUniqueRetry(appContext)
    }

    suspend fun pending(): List<PendingPurchase> =
        database.purchases().pending().map { row ->
            val ciphertext = requireNotNull(row.tokenCiphertext) {
                "Pending purchase ${row.tokenHash} has no encrypted token"
            }
            PendingPurchase(
                tokenHash = row.tokenHash,
                productId = row.productId,
                intentId = row.intentId,
                purchaseToken = vault.decrypt(ciphertext).toString(Charsets.UTF_8),
            )
        }

    suspend fun markConfirmed(tokenHash: String) {
        database.purchases().markTerminal(tokenHash, STATE_CONFIRMED)
    }

    suspend fun markRejected(tokenHash: String) {
        database.purchases().markTerminal(tokenHash, STATE_REJECTED)
    }

    private fun enqueueUniqueRetry(context: Context) {
        val request = OneTimeWorkRequestBuilder<PurchaseFulfilmentWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val TOKEN_KEY_ALIAS = "plinth_purchase_outbox_v1"
        const val STATE_PENDING = "PENDING"
        const val STATE_CONFIRMED = "CONFIRMED"
        const val STATE_REJECTED = "REJECTED"
        const val UNIQUE_WORK_NAME = "plinth-purchase-fulfilment"
    }
}
