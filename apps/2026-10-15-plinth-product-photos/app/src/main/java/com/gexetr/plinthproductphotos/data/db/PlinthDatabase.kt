package com.gexetr.plinthproductphotos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.gexetr.plinthproductphotos.domain.model.ItemState
import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.model.JobState

class PlinthConverters {
    @TypeConverter
    fun slotRoleToString(value: SlotRole): String = value.name

    @TypeConverter
    fun stringToSlotRole(value: String): SlotRole = SlotRole.valueOf(value)

    @TypeConverter
    fun itemStateToString(value: ItemState): String = value.name

    @TypeConverter
    fun stringToItemState(value: String): ItemState = ItemState.valueOf(value)

    @TypeConverter
    fun jobStateToString(value: JobState): String = value.name

    @TypeConverter
    fun stringToJobState(value: String): JobState = JobState.valueOf(value)

    @TypeConverter
    fun provenanceToString(value: Provenance): String = value.name

    @TypeConverter
    fun stringToProvenance(value: String): Provenance = Provenance.valueOf(value)
}

@Database(
    entities = [
        ProjectEntity::class,
        ItemEntity::class,
        LookEntity::class,
        JobEntity::class,
        OutputEntity::class,
        ExportRecordEntity::class,
        GroupEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(PlinthConverters::class)
abstract class PlinthDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun itemDao(): ItemDao
    abstract fun groupDao(): GroupDao
    abstract fun lookDao(): LookDao
    abstract fun jobDao(): JobDao
    abstract fun outputDao(): OutputDao
    abstract fun exportRecordDao(): ExportRecordDao

    companion object {
        @Volatile
        private var instance: PlinthDatabase? = null

        fun get(context: Context): PlinthDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlinthDatabase::class.java,
                    "plinth.db",
                ).build().also { instance = it }
            }
    }
}
