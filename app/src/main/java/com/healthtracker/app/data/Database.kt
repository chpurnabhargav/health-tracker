package com.healthtracker.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** All `day` columns are LocalDate.toEpochDay(); all `time` columns are epoch millis. */

@Entity(tableName = "weights")
data class WeightEntry(
    @PrimaryKey val day: Long,
    val kg: Double,
    val loggedAt: Long,
)

@Entity(tableName = "waists")
data class WaistEntry(
    @PrimaryKey val day: Long,
    val cm: Double,
)

@Entity(tableName = "water")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val time: Long,
    val ml: Int,
)

@Entity(tableName = "food")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val time: Long,
    val name: String,
    val servings: Double,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val junk: Boolean,
    val plannedMeal: Boolean = false,
)

@Entity(tableName = "my_foods")
data class MyFood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val serving: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val junk: Boolean,
    val uses: Int = 0,
    val lastUsed: Long = 0,
)

/** outcome: "closed", "continued" or "planned". */
@Entity(tableName = "pauses")
data class PauseEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val day: Long,
    val pkg: String,
    val appName: String,
    val outcome: String,
)

@Entity(tableName = "activity")
data class DailyActivity(
    @PrimaryKey val day: Long,
    val steps: Int?,
    val sleepMin: Int?,
)

@Entity(tableName = "minimum_days")
data class MinimumDay(
    @PrimaryKey val day: Long,
)

@Dao
interface HealthDao {
    // Weight
    @Query("SELECT * FROM weights ORDER BY day")
    fun weights(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weights ORDER BY day")
    suspend fun allWeights(): List<WeightEntry>

    @Upsert
    suspend fun upsertWeight(e: WeightEntry)

    @Query("DELETE FROM weights WHERE day = :day")
    suspend fun deleteWeight(day: Long)

    @Query("SELECT * FROM weights WHERE day = :day")
    suspend fun weightOn(day: Long): WeightEntry?

    // Waist
    @Query("SELECT * FROM waists ORDER BY day")
    fun waists(): Flow<List<WaistEntry>>

    @Query("SELECT * FROM waists ORDER BY day")
    suspend fun allWaists(): List<WaistEntry>

    @Upsert
    suspend fun upsertWaist(e: WaistEntry)

    @Query("DELETE FROM waists WHERE day = :day")
    suspend fun deleteWaist(day: Long)

    // Water
    @Query("SELECT * FROM water WHERE day >= :from ORDER BY time")
    fun waterSince(from: Long): Flow<List<WaterEntry>>

    @Query("SELECT * FROM water ORDER BY time")
    suspend fun allWater(): List<WaterEntry>

    @Query("SELECT COALESCE(SUM(ml), 0) FROM water WHERE day = :day")
    suspend fun waterOn(day: Long): Int

    @Insert
    suspend fun insertWater(e: WaterEntry)

    @Query("DELETE FROM water WHERE id = (SELECT id FROM water WHERE day = :day ORDER BY time DESC LIMIT 1)")
    suspend fun undoLastWater(day: Long)

    // Food
    @Query("SELECT * FROM food WHERE day >= :from ORDER BY time")
    fun foodSince(from: Long): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food ORDER BY time")
    suspend fun allFood(): List<FoodEntry>

    @Query("SELECT COUNT(*) FROM food WHERE time >= :since")
    suspend fun foodCountSince(since: Long): Int

    @Insert
    suspend fun insertFood(e: FoodEntry)

    @Query("DELETE FROM food WHERE id = :id")
    suspend fun deleteFood(id: Long)

    // My foods
    @Query("SELECT * FROM my_foods ORDER BY uses DESC, lastUsed DESC")
    fun myFoods(): Flow<List<MyFood>>

    @Query("SELECT * FROM my_foods")
    suspend fun allMyFoods(): List<MyFood>

    @Query("SELECT * FROM my_foods WHERE name = :name LIMIT 1")
    suspend fun myFoodByName(name: String): MyFood?

    @Upsert
    suspend fun upsertMyFood(f: MyFood)

    @Query("DELETE FROM my_foods WHERE id = :id")
    suspend fun deleteMyFood(id: Long)

    // Pauses
    @Query("SELECT * FROM pauses WHERE day >= :from ORDER BY time")
    fun pausesSince(from: Long): Flow<List<PauseEvent>>

    @Query("SELECT * FROM pauses ORDER BY time")
    suspend fun allPauses(): List<PauseEvent>

    @Query("SELECT * FROM pauses WHERE day >= :from ORDER BY time")
    suspend fun pausesFrom(from: Long): List<PauseEvent>

    @Insert
    suspend fun insertPause(e: PauseEvent)

    // Activity (steps and sleep, from Health Connect)
    @Query("SELECT * FROM activity WHERE day >= :from ORDER BY day")
    fun activitySince(from: Long): Flow<List<DailyActivity>>

    @Query("SELECT * FROM activity ORDER BY day")
    suspend fun allActivity(): List<DailyActivity>

    @Upsert
    suspend fun upsertActivity(list: List<DailyActivity>)

    // Minimum days
    @Query("SELECT * FROM minimum_days WHERE day >= :from")
    fun minimumDaysSince(from: Long): Flow<List<MinimumDay>>

    @Query("SELECT * FROM minimum_days")
    suspend fun allMinimumDays(): List<MinimumDay>

    @Upsert
    suspend fun upsertMinimumDay(d: MinimumDay)

    @Query("DELETE FROM minimum_days WHERE day = :day")
    suspend fun deleteMinimumDay(day: Long)

    // Import
    @Query("DELETE FROM weights") suspend fun clearWeights()
    @Query("DELETE FROM waists") suspend fun clearWaists()
    @Query("DELETE FROM water") suspend fun clearWater()
    @Query("DELETE FROM food") suspend fun clearFood()
    @Query("DELETE FROM my_foods") suspend fun clearMyFoods()
    @Query("DELETE FROM pauses") suspend fun clearPauses()
    @Query("DELETE FROM activity") suspend fun clearActivity()
    @Query("DELETE FROM minimum_days") suspend fun clearMinimumDays()

    @Insert suspend fun insertWeights(l: List<WeightEntry>)
    @Insert suspend fun insertWaists(l: List<WaistEntry>)
    @Insert suspend fun insertWaters(l: List<WaterEntry>)
    @Insert suspend fun insertFoods(l: List<FoodEntry>)
    @Insert suspend fun insertMyFoods(l: List<MyFood>)
    @Insert suspend fun insertPauses(l: List<PauseEvent>)
    @Insert suspend fun insertMinimumDays(l: List<MinimumDay>)
}

@Database(
    entities = [
        WeightEntry::class, WaistEntry::class, WaterEntry::class, FoodEntry::class,
        MyFood::class, PauseEvent::class, DailyActivity::class, MinimumDay::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): HealthDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "health.db").build()
    }
}
