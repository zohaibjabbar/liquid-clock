package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter
    fun fromString(value: String?): Set<Int> {
        if (value.isNullOrBlank()) return emptySet()
        val mappedList = value.split(",").mapNotNull { part ->
            val cleanPart = part.trim()
            val intVal = cleanPart.toIntOrNull()
            if (intVal != null) {
                intVal
            } else {
                when (cleanPart.lowercase()) {
                    "m", "mon", "monday" -> 0
                    "t", "tue", "tuesday" -> 1
                    "w", "wed", "wednesday" -> 2
                    "thu", "thursday" -> 3
                    "f", "fri", "friday" -> 4
                    "s", "sat", "saturday" -> 5
                    "sun", "sunday" -> 6
                    else -> null
                }
            }
        }
        return mappedList.toSet()
    }

    @TypeConverter
    fun fromSet(set: Set<Int>?): String {
        if (set == null) return ""
        return set.joinToString(",")
    }
}

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int, // 1 - 12
    val minute: Int, // 0 - 59
    val isAm: Boolean,
    val repeatDays: Set<Int> = emptySet(),
    val label: String,
    val sound: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "world_clocks")
data class WorldClockEntity(
    @PrimaryKey val cityId: String,
    val cityName: String,
    val timezoneId: String,
    val offsetHours: Int,
    val offsetMinutes: Int = 0
)

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY isAm ASC, hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Query("DELETE FROM alarms")
    suspend fun deleteAllAlarms()
}

@Dao
interface WorldClockDao {
    @Query("SELECT * FROM world_clocks")
    fun getAllWorldClocks(): Flow<List<WorldClockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorldClock(worldClock: WorldClockEntity)

    @Delete
    suspend fun deleteWorldClock(worldClock: WorldClockEntity)

    @Query("DELETE FROM world_clocks")
    suspend fun deleteAllWorldClocks()
}

@Database(entities = [AlarmEntity::class, WorldClockEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ClockDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun worldClockDao(): WorldClockDao

    companion object {
        @Volatile
        private var INSTANCE: ClockDatabase? = null

        fun getDatabase(context: Context): ClockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClockDatabase::class.java,
                    "liquid_clock_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
