package com.gexetr.plinthproductphotos.data.batch

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Entity(tableName = "batch")
data class BatchEntity(
    @PrimaryKey val batchId: String,
    val lookJson: String,
    val workName: String,
    val createdAt: Long,
    val terminalAt: Long? = null,
)

@Entity(
    tableName = "batch_item",
    foreignKeys = [
        ForeignKey(
            entity = BatchEntity::class,
            parentColumns = ["batchId"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["batchId", "sourceId"], unique = true),
        Index(value = ["batchId", "state"]),
    ],
)
data class BatchItemEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val sourceId: String,
    val lookJson: String,
    val state: String,
    val outputPath: String? = null,
    val error: String? = null,
    val attempt: Int = 0,
)

@androidx.room.Dao
interface BatchDao {
    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertBatch(batch: BatchEntity)

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<BatchItemEntity>)

    @Query("SELECT * FROM batch WHERE batchId = :batchId")
    suspend fun batch(batchId: String): BatchEntity?

    @Query("SELECT * FROM batch_item WHERE batchId = :batchId ORDER BY id")
    suspend fun items(batchId: String): List<BatchItemEntity>

    @Query("SELECT * FROM batch_item WHERE batchId = :batchId ORDER BY id")
    fun observeItems(batchId: String): kotlinx.coroutines.flow.Flow<List<BatchItemEntity>>

    @Query("UPDATE batch_item SET state = 'QUEUED' WHERE batchId = :batchId AND state = 'PROCESSING'")
    suspend fun recoverInterrupted(batchId: String)

    @Query(
        "UPDATE batch_item SET state = 'PROCESSING', attempt = attempt + 1 " +
            "WHERE id = :itemId AND state = 'QUEUED'",
    )
    suspend fun claim(itemId: String): Int

    @Query("SELECT * FROM batch_item WHERE id = :itemId")
    suspend fun item(itemId: String): BatchItemEntity?

    @Query(
        "UPDATE batch_item SET state = 'COMPOSED', outputPath = :outputPath, error = NULL " +
            "WHERE id = :itemId AND state = 'PROCESSING'",
    )
    suspend fun complete(itemId: String, outputPath: String): Int

    @Query(
        "UPDATE batch_item SET state = 'FAILED', error = :message " +
            "WHERE id = :itemId AND state = 'PROCESSING'",
    )
    suspend fun fail(itemId: String, message: String): Int

    @Query("UPDATE batch_item SET state = 'QUEUED', error = NULL WHERE id = :itemId AND state = 'FAILED'")
    suspend fun retry(itemId: String): Int

    @Query("UPDATE batch SET terminalAt = NULL WHERE batchId = :batchId")
    suspend fun clearTerminal(batchId: String)

    @Query(
        "UPDATE batch SET terminalAt = :terminalAt " +
            "WHERE batchId = :batchId AND NOT EXISTS " +
            "(SELECT 1 FROM batch_item WHERE batchId = :batchId AND state IN ('QUEUED', 'PROCESSING'))",
    )
    suspend fun markTerminalIfSettled(batchId: String, terminalAt: Long): Int

    @Query(
        "SELECT COUNT(*) FROM batch_item WHERE batchId = :batchId AND state = 'COMPOSED'",
    )
    suspend fun composedCount(batchId: String): Int

    @Query("SELECT COUNT(*) FROM batch_item WHERE batchId = :batchId AND state = 'FAILED'")
    suspend fun failedCount(batchId: String): Int

    @Transaction
    suspend fun insertBatchAndItems(batch: BatchEntity, items: List<BatchItemEntity>) {
        insertBatch(batch)
        insertItems(items)
    }
}

@Database(entities = [BatchEntity::class, BatchItemEntity::class], version = 1, exportSchema = true)
abstract class BatchDatabase : RoomDatabase() {
    abstract fun batches(): BatchDao

    companion object {
        @Volatile
        private var instance: BatchDatabase? = null

        fun get(context: Context): BatchDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BatchDatabase::class.java,
                "batch.db",
            ).build().also { instance = it }
        }
    }
}
