package com.healthtracker.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

enum class Sex(val label: String) { MALE("Male"), FEMALE("Female") }

enum class ActivityLevel(val label: String, val detail: String, val factor: Double) {
    SEDENTARY("Mostly sitting", "Desk job, little walking", 1.2),
    LIGHT("Lightly active", "Some walking, light exercise 1–3 days a week", 1.375),
    MODERATE("Moderately active", "Exercise 3–5 days a week", 1.55),
    ACTIVE("Very active", "Hard exercise most days", 1.725),
}

enum class Pace(val label: String, val kgPerWeek: Double) {
    GENTLE("Gentle · 0.5 kg a week", 0.5),
    STEADY("Steady · 0.75 kg a week", 0.75),
    FAST("Fast · 1 kg a week", 1.0),
}

enum class PauseMode(val label: String, val detail: String) {
    GENTLE("Gentle", "Wait, then you can continue"),
    STRICT("Strict", "Longer waits each time, type a promise and log the order before you continue"),
    LOCKDOWN("Lockdown", "No ordering, except your planned meal or a 5-minute emergency unlock"),
}

data class FoodApp(val pkg: String, val name: String)

val KnownFoodApps = listOf(
    FoodApp("in.swiggy.android", "Swiggy"),
    FoodApp("com.application.zomato", "Zomato"),
    FoodApp("com.grofers.customerapp", "Blinkit"),
    FoodApp("com.zeptoconsumerapp", "Zepto"),
)

data class UserSettings(
    val onboarded: Boolean = false,
    val name: String = "",
    val birthDay: Long? = null,
    val sex: Sex = Sex.MALE,
    val heightCm: Double = 175.0,
    val startWeight: Double = 80.0,
    val startDay: Long = LocalDate.now().toEpochDay(),
    val activity: ActivityLevel = ActivityLevel.SEDENTARY,
    val pace: Pace = Pace.STEADY,
    /** Weight the calorie target is calculated from. Updated every 5 kg lost. */
    val calcWeight: Double = 80.0,
    val calorieOverride: Int? = null,
    val goalName: String = "your birthday",
    val goalKg: Double = 75.0,
    val goalDay: Long? = null,
    val goalStartDay: Long? = null,
    val goalStartKg: Double? = null,
    val lastGoalResult: String? = null,
    val waterGoalMl: Int = 3500,
    val stepGoal: Int = 8000,
    val waterRemind: Boolean = true,
    val waterStartMin: Int = 8 * 60,
    val waterEndMin: Int = 22 * 60,
    val waterEveryMin: Int = 90,
    val weighRemind: Boolean = true,
    val weighMin: Int = 7 * 60,
    val mealRemind: Boolean = true,
    val weeklyRemind: Boolean = true,
    val pauseEnabled: Boolean = true,
    val pauseApps: Set<String> = setOf("in.swiggy.android", "com.application.zomato"),
    val customPauseMessage: String? = null,
    val pauseWaitSec: Int = 10,
    val junkCheck: Boolean = true,
    val plannedMealsPerWeek: Int = 0,
    val recalcNotice: String? = null,
    val pauseMode: PauseMode = PauseMode.STRICT,
    val orderValue: Int = 350,
    val waterPopup: Boolean = true,
    val lastPauseOffWarnDay: Long? = null,
) {
    val displayName: String get() = name.ifBlank { "friend" }
    val pauseMessage: String
        get() = customPauseMessage?.takeIf { it.isNotBlank() }
            ?: "No ${displayName}, not till ${goalName}."
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private object K {
    val onboarded = booleanPreferencesKey("onboarded")
    val name = stringPreferencesKey("name")
    val birthDay = longPreferencesKey("birth_day")
    val sex = stringPreferencesKey("sex")
    val height = doublePreferencesKey("height_cm")
    val startWeight = doublePreferencesKey("start_weight")
    val startDay = longPreferencesKey("start_day")
    val activity = stringPreferencesKey("activity")
    val pace = stringPreferencesKey("pace")
    val calcWeight = doublePreferencesKey("calc_weight")
    val calorieOverride = intPreferencesKey("calorie_override")
    val goalName = stringPreferencesKey("goal_name")
    val goalKg = doublePreferencesKey("goal_kg")
    val goalDay = longPreferencesKey("goal_day")
    val goalStartDay = longPreferencesKey("goal_start_day")
    val goalStartKg = doublePreferencesKey("goal_start_kg")
    val lastGoalResult = stringPreferencesKey("last_goal_result")
    val waterGoal = intPreferencesKey("water_goal_ml")
    val stepGoal = intPreferencesKey("step_goal")
    val waterRemind = booleanPreferencesKey("water_remind")
    val waterStart = intPreferencesKey("water_start_min")
    val waterEnd = intPreferencesKey("water_end_min")
    val waterEvery = intPreferencesKey("water_every_min")
    val weighRemind = booleanPreferencesKey("weigh_remind")
    val weighMin = intPreferencesKey("weigh_min")
    val mealRemind = booleanPreferencesKey("meal_remind")
    val weeklyRemind = booleanPreferencesKey("weekly_remind")
    val pauseEnabled = booleanPreferencesKey("pause_enabled")
    val pauseApps = stringSetPreferencesKey("pause_apps")
    val pauseMessage = stringPreferencesKey("pause_message")
    val pauseWait = intPreferencesKey("pause_wait_sec")
    val junkCheck = booleanPreferencesKey("junk_check")
    val plannedMeals = intPreferencesKey("planned_meals_per_week")
    val recalcNotice = stringPreferencesKey("recalc_notice")
    val pauseMode = stringPreferencesKey("pause_mode")
    val orderValue = intPreferencesKey("order_value")
    val waterPopup = booleanPreferencesKey("water_popup")
    val lastPauseOffWarn = longPreferencesKey("last_pause_off_warn")
}

class SettingsRepository(private val context: Context) {

    val flow: Flow<UserSettings> = context.dataStore.data.map { p -> read(p) }

    suspend fun current(): UserSettings = flow.first()

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.dataStore.edit { p ->
            val next = transform(read(p))
            write(p, next)
        }
    }

    private fun read(p: Preferences): UserSettings {
        val d = UserSettings()
        return UserSettings(
            onboarded = p[K.onboarded] ?: d.onboarded,
            name = p[K.name] ?: d.name,
            birthDay = p[K.birthDay],
            sex = p[K.sex]?.let { runCatching { Sex.valueOf(it) }.getOrNull() } ?: d.sex,
            heightCm = p[K.height] ?: d.heightCm,
            startWeight = p[K.startWeight] ?: d.startWeight,
            startDay = p[K.startDay] ?: d.startDay,
            activity = p[K.activity]?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() } ?: d.activity,
            pace = p[K.pace]?.let { runCatching { Pace.valueOf(it) }.getOrNull() } ?: d.pace,
            calcWeight = p[K.calcWeight] ?: p[K.startWeight] ?: d.calcWeight,
            calorieOverride = p[K.calorieOverride],
            goalName = p[K.goalName] ?: d.goalName,
            goalKg = p[K.goalKg] ?: d.goalKg,
            goalDay = p[K.goalDay],
            goalStartDay = p[K.goalStartDay],
            goalStartKg = p[K.goalStartKg],
            lastGoalResult = p[K.lastGoalResult],
            waterGoalMl = p[K.waterGoal] ?: d.waterGoalMl,
            stepGoal = p[K.stepGoal] ?: d.stepGoal,
            waterRemind = p[K.waterRemind] ?: d.waterRemind,
            waterStartMin = p[K.waterStart] ?: d.waterStartMin,
            waterEndMin = p[K.waterEnd] ?: d.waterEndMin,
            waterEveryMin = p[K.waterEvery] ?: d.waterEveryMin,
            weighRemind = p[K.weighRemind] ?: d.weighRemind,
            weighMin = p[K.weighMin] ?: d.weighMin,
            mealRemind = p[K.mealRemind] ?: d.mealRemind,
            weeklyRemind = p[K.weeklyRemind] ?: d.weeklyRemind,
            pauseEnabled = p[K.pauseEnabled] ?: d.pauseEnabled,
            pauseApps = p[K.pauseApps] ?: d.pauseApps,
            customPauseMessage = p[K.pauseMessage],
            pauseWaitSec = p[K.pauseWait] ?: d.pauseWaitSec,
            junkCheck = p[K.junkCheck] ?: d.junkCheck,
            plannedMealsPerWeek = p[K.plannedMeals] ?: d.plannedMealsPerWeek,
            recalcNotice = p[K.recalcNotice],
            pauseMode = p[K.pauseMode]?.let { runCatching { PauseMode.valueOf(it) }.getOrNull() } ?: d.pauseMode,
            orderValue = p[K.orderValue] ?: d.orderValue,
            waterPopup = p[K.waterPopup] ?: d.waterPopup,
            lastPauseOffWarnDay = p[K.lastPauseOffWarn],
        )
    }

    private fun write(p: MutablePreferences, s: UserSettings) {
        p[K.onboarded] = s.onboarded
        p[K.name] = s.name
        p.setOrRemove(K.birthDay, s.birthDay)
        p[K.sex] = s.sex.name
        p[K.height] = s.heightCm
        p[K.startWeight] = s.startWeight
        p[K.startDay] = s.startDay
        p[K.activity] = s.activity.name
        p[K.pace] = s.pace.name
        p[K.calcWeight] = s.calcWeight
        p.setOrRemove(K.calorieOverride, s.calorieOverride)
        p[K.goalName] = s.goalName
        p[K.goalKg] = s.goalKg
        p.setOrRemove(K.goalDay, s.goalDay)
        p.setOrRemove(K.goalStartDay, s.goalStartDay)
        p.setOrRemove(K.goalStartKg, s.goalStartKg)
        p.setOrRemove(K.lastGoalResult, s.lastGoalResult)
        p[K.waterGoal] = s.waterGoalMl
        p[K.stepGoal] = s.stepGoal
        p[K.waterRemind] = s.waterRemind
        p[K.waterStart] = s.waterStartMin
        p[K.waterEnd] = s.waterEndMin
        p[K.waterEvery] = s.waterEveryMin
        p[K.weighRemind] = s.weighRemind
        p[K.weighMin] = s.weighMin
        p[K.mealRemind] = s.mealRemind
        p[K.weeklyRemind] = s.weeklyRemind
        p[K.pauseEnabled] = s.pauseEnabled
        p[K.pauseApps] = s.pauseApps
        p.setOrRemove(K.pauseMessage, s.customPauseMessage)
        p[K.pauseWait] = s.pauseWaitSec
        p[K.junkCheck] = s.junkCheck
        p[K.plannedMeals] = s.plannedMealsPerWeek
        p.setOrRemove(K.recalcNotice, s.recalcNotice)
        p[K.pauseMode] = s.pauseMode.name
        p[K.orderValue] = s.orderValue
        p[K.waterPopup] = s.waterPopup
        p.setOrRemove(K.lastPauseOffWarn, s.lastPauseOffWarnDay)
    }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }
}
