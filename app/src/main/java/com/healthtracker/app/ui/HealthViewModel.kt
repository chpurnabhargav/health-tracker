package com.healthtracker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.app.HealthApp
import com.healthtracker.app.data.CatalogFood
import com.healthtracker.app.data.DailyActivity
import com.healthtracker.app.data.FoodEntry
import com.healthtracker.app.data.MinimumDay
import com.healthtracker.app.data.MyFood
import com.healthtracker.app.data.PauseEvent
import com.healthtracker.app.data.UserSettings
import com.healthtracker.app.data.WaistEntry
import com.healthtracker.app.data.WaterEntry
import com.healthtracker.app.data.WeightEntry
import com.healthtracker.app.domain.HealthState
import com.healthtracker.app.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class HealthViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as HealthApp).container
    private val dao = c.db.dao()
    private val today = MutableStateFlow(LocalDate.now())
    private val from = LocalDate.now().minusDays(400).toEpochDay()

    val settings: StateFlow<UserSettings?> =
        c.settings.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<HealthState?> = combine(
        listOf<Flow<Any>>(
            today, c.settings.flow, dao.weights(), dao.waists(), dao.foodSince(from), dao.waterSince(from),
            dao.pausesSince(from), dao.activitySince(from), dao.minimumDaysSince(from), dao.myFoods(),
        )
    ) { a ->
        HealthState(
            today = a[0] as LocalDate,
            s = a[1] as UserSettings,
            weights = a[2] as List<WeightEntry>,
            waists = a[3] as List<WaistEntry>,
            food = a[4] as List<FoodEntry>,
            water = a[5] as List<WaterEntry>,
            pauses = a[6] as List<PauseEvent>,
            activity = a[7] as List<DailyActivity>,
            minimumDays = (a[8] as List<MinimumDay>).map { it.day }.toSet(),
            myFoods = a[9] as List<MyFood>,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun showMessage(text: String) { _message.value = text }
    fun clearMessage() { _message.value = null }

    /** Call when the app comes to the front: refreshes the date, goals and Health Connect data. */
    fun onResume() {
        today.value = LocalDate.now()
        viewModelScope.launch {
            c.repo.maintainGoals(LocalDate.now())
            refreshActivity()
        }
    }

    suspend fun refreshActivity() {
        withContext(Dispatchers.IO) {
            if (!c.health.hasPermissions()) return@withContext
            val days = (0..7).map { LocalDate.now().minusDays(it.toLong()) }
            val list = runCatching { c.health.readDays(days) }.getOrDefault(emptyList())
            if (list.isNotEmpty()) c.repo.saveActivity(list)
        }
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            c.settings.update(transform)
            ReminderScheduler.scheduleAll(getApplication(), c.settings.current())
        }
    }

    fun logWeight(kg: Double) = viewModelScope.launch { c.repo.logWeight(kg) }
    fun deleteWeight(day: Long) = viewModelScope.launch { c.repo.deleteWeight(day) }
    fun logWaist(cm: Double) = viewModelScope.launch { c.repo.logWaist(cm) }
    fun deleteWaist(day: Long) = viewModelScope.launch { c.repo.deleteWaist(day) }
    fun addWater(ml: Int) = viewModelScope.launch { c.repo.addWater(ml) }
    fun undoWater() = viewModelScope.launch { c.repo.undoWater() }
    fun deleteFood(id: Long) = viewModelScope.launch { c.repo.deleteFood(id) }
    fun deleteMyFood(id: Long) = viewModelScope.launch { c.repo.deleteMyFood(id) }
    fun setMinimumDay(on: Boolean) = viewModelScope.launch { c.repo.setMinimumDay(on) }

    fun addCatalogFood(f: CatalogFood, servings: Double) = viewModelScope.launch {
        c.repo.addFood(f.name, servings, f.kcal, f.protein, f.carbs, f.fat, f.junk, serving = f.serving)
    }

    fun addMyFood(f: MyFood, servings: Double) = viewModelScope.launch {
        c.repo.addFood(f.name, servings, f.kcal, f.protein, f.carbs, f.fat, f.junk, serving = f.serving)
    }

    fun addCustomFood(name: String, serving: String, kcal: Int, p: Double, carbs: Double, fat: Double, junk: Boolean, servings: Double) =
        viewModelScope.launch { c.repo.addFood(name, servings, kcal, p, carbs, fat, junk, serving = serving) }

    /** Meal to preselect when opening Add food. */
    val pendingMeal = MutableStateFlow<com.healthtracker.app.domain.Meal?>(null)

    // ---- Coach ----
    private val _coachBusy = MutableStateFlow(false)
    val coachBusy: StateFlow<Boolean> = _coachBusy.asStateFlow()
    private val _coachError = MutableStateFlow<String?>(null)
    val coachError: StateFlow<String?> = _coachError.asStateFlow()

    private suspend fun <T> coachCall(block: suspend () -> T): T? {
        _coachBusy.value = true
        _coachError.value = null
        return try {
            block()
        } catch (e: com.healthtracker.app.coach.CoachError) {
            _coachError.value = e.userMessage; null
        } catch (e: Exception) {
            _coachError.value = "Coach hit a problem. Try again."; null
        } finally {
            _coachBusy.value = false
        }
    }

    fun askCoach(question: String) = viewModelScope.launch {
        val h = state.value ?: return@launch
        coachCall { c.coach.ask(h, question) }
    }

    fun clearCoachError() { _coachError.value = null }
    fun clearCoachChat() = viewModelScope.launch { c.coach.clearHistory() }

    fun loadDailyTip() = viewModelScope.launch {
        val h = state.value ?: return@launch
        if (h.s.coachKey.isBlank() || !h.s.coachEnabled) return@launch
        if (h.s.coachTipDay == h.todayDay && h.s.coachTip.isNotBlank()) return@launch
        runCatching { c.coach.dailyTip(h) }
    }

    fun loadWeeklyReview() = viewModelScope.launch {
        val h = state.value ?: return@launch
        coachCall { c.coach.weeklyReview(h) }
    }

    suspend fun testCoachKey(key: String): String? = try {
        c.coach.testKey(key); null
    } catch (e: com.healthtracker.app.coach.CoachError) {
        e.userMessage
    } catch (e: Exception) {
        "Couldn't check the key."
    }

    suspend fun estimateFoods(items: List<com.healthtracker.app.domain.ParsedItem>): List<com.healthtracker.app.domain.ParsedItem>? {
        val h = state.value ?: return null
        return coachCall { c.coach.estimate(h, items) }
    }

    // ---- Faster logging ----
    fun logItems(items: List<com.healthtracker.app.domain.ParsedItem>, meal: com.healthtracker.app.domain.Meal) = viewModelScope.launch {
        c.repo.logItems(items, meal.timeOn(LocalDate.now()))
        val s = c.settings.current()
        val logged = items.filter { it.name != null }.map {
            FoodEntry(day = 0, time = 0, name = it.name!!, servings = it.servings, kcal = (it.kcal * it.servings).toInt(),
                protein = it.protein * it.servings, carbs = it.carbs * it.servings, fat = it.fat * it.servings, junk = it.junk)
        }
        val fb = com.healthtracker.app.domain.MealFeedback.forMeal(logged, s.displayName, com.healthtracker.app.domain.Calc.proteinTarget(s.heightCm))
        _message.value = "Logged ${logged.size} item${if (logged.size == 1) "" else "s"} · ${logged.sumOf { it.kcal }} kcal" + (fb?.let { ". $it" } ?: "")
    }

    fun repeatYesterday(meal: com.healthtracker.app.domain.Meal) = viewModelScope.launch {
        val n = c.repo.repeatYesterday(meal)
        _message.value = if (n == 0) "Nothing logged for ${meal.label.lowercase()} yesterday." else "Copied yesterday's ${meal.label.lowercase()} ($n items)."
    }

    fun saveCombo(name: String, entries: List<FoodEntry>) = viewModelScope.launch {
        c.repo.saveCombo(name, entries)
        _message.value = "Saved \"$name\" to your foods."
    }

    fun planMeal(day: Long, meal: com.healthtracker.app.domain.Meal, ideaId: String?) = updateSettings { s ->
        val root = runCatching { org.json.JSONObject(s.mealPlan) }.getOrDefault(org.json.JSONObject())
        val today = LocalDate.now().toEpochDay()
        root.keys().asSequence().toList().forEach { k -> if ((k.toLongOrNull() ?: 0) < today) root.remove(k) }
        val d = root.optJSONObject(day.toString()) ?: org.json.JSONObject()
        if (ideaId == null) d.remove(meal.name) else d.put(meal.name, ideaId)
        root.put(day.toString(), d)
        s.copy(mealPlan = root.toString())
    }

    fun cravingBeaten() = updateSettings { it.copy(cravingWins = it.cravingWins + 1) }

    fun dismissRecalcNotice() = updateSettings { it.copy(recalcNotice = null) }
    fun dismissGoalResult() = updateSettings { it.copy(lastGoalResult = null) }

    suspend fun exportJson(): String = c.repo.exportJson()

    fun importJson(text: String) = viewModelScope.launch {
        val result = runCatching { c.repo.importJson(text) }
        _message.value = result.getOrElse { "Couldn't import: ${it.message ?: "the file isn't valid"}" }
        ReminderScheduler.scheduleAll(getApplication(), c.settings.current())
    }

    /** Finishes onboarding and sets up reminders. */
    fun completeOnboarding(s: UserSettings) = viewModelScope.launch {
        c.settings.update { s.copy(onboarded = true) }
        if (dao.weightOn(LocalDate.now().toEpochDay()) == null) c.repo.logWeight(s.startWeight)
        ReminderScheduler.scheduleAll(getApplication(), c.settings.current())
    }
}
