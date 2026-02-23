package com.example.stepquest.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val date: String,   // yyyy-MM-dd
    val steps: Long
)

@Dao
interface DailyStepsDao {
    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    suspend fun getAll(): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getByDate(date: String): DailyStepsEntity?

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_steps WHERE date BETWEEN :startDate AND :endDate")
    suspend fun sumStepsInRange(startDate: String, endDate: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyStepsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DailyStepsEntity>)
}

@Database(entities = [DailyStepsEntity::class], version = 1, exportSchema = false)
abstract class StepsDatabase : RoomDatabase() {
    abstract fun dailyStepsDao(): DailyStepsDao

    companion object {
        @Volatile
        private var INSTANCE: StepsDatabase? = null

        fun getInstance(context: Context): StepsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StepsDatabase::class.java,
                    "steps_database"
                ).build().also { INSTANCE = it }
            }
    }
}
