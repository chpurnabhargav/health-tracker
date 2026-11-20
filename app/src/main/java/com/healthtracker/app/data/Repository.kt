package com.healthtracker.app.data

import com.healthtracker.app.domain.Calc
import com.healthtracker.app.domain.Fmt
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.roundToInt

class HealthRepository(val dao: HealthDao, val settings: SettingsRepository, private val onChange: () -> Unit = {}) {

    private fun now() = System.currentTimeMillis()
    private fun today() = LocalDate.now().toEpochDay()

    suspend fun logWeight(kg: Double, day: Long = today()) {
        dao.upsertWeight(WeightEntry(day, (kg * 10).roundToInt() / 10.0, now()))
        maintainGoals(LocalDate.now())
        onChange()
    }

    suspend fun deleteWeight(day: Long) = dao.deleteWeight(day)

    suspend fun logWaist(cm: Double, day: Long = today()) =
        dao.upsertWaist(WaistEntry(day, (cm * 10).roundToInt() / 10.0))

    suspend fun deleteWaist(day: Long) = dao.deleteWaist(day)

    suspend fun addWater(ml: Int) {
        dao.insertWater(WaterEntry(day = today(), time = now(), ml = ml))
        onChange()
    }

    suspend fun undoWater() {
        dao.undoLastWater(today())
        onChange()
    }

    suspend fun waterToday(): Int = dao.waterOn(today())

    suspend fun weightToday(): WeightEntry? = dao.weightOn(today())

    suspend fun addFood(
        name: String, servings: Double, kcalPer: Int, proteinPer: Double, carbsPer: Double, fatPer: Double,
        junk: Boolean, plannedMeal: Boolean = false, serving: String = "1 serving",
    ) {
        dao.insertFood(
            FoodEntry(
                day = today(), time = now(), name = name, servings = servings,
                kcal = (kcalPer * servings).roundToInt(), protein = proteinPer * servings,
                carbs = carbsPer * servings, fat = fatPer * servings, junk = junk, plannedMeal = plannedMeal,
            )
        )
        val existing = dao.myFoodByName(name)
        if (existing != null) dao.upsertMyFood(existing.copy(uses = existing.uses + 1, lastUsed = now()))
        else dao.upsertMyFood(MyFood(name = name, serving = serving, kcal = kcalPer, protein = proteinPer, carbs = carbsPer, fat = fatPer, junk = junk, uses = 1, lastUsed = now()))
    }

    suspend fun deleteFood(id: Long) = dao.deleteFood(id)

    /** Logs several items at once at [time] (used by typed logging, meal ideas and repeat). */
    suspend fun logItems(items: List<com.healthtracker.app.domain.ParsedItem>, time: Long) {
        val day = java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
        items.filter { it.name != null }.forEachIndexed { i, it ->
            dao.insertFood(
                FoodEntry(
                    day = day, time = time + i, name = it.name!!, servings = it.servings,
                    kcal = (it.kcal * it.servings).roundToInt(), protein = it.protein * it.servings,
                    carbs = it.carbs * it.servings, fat = it.fat * it.servings, junk = it.junk,
                )
            )
            val existing = dao.myFoodByName(it.name)
            if (existing != null) dao.upsertMyFood(existing.copy(uses = existing.uses + 1, lastUsed = now()))
            else if (it.estimated) dao.upsertMyFood(MyFood(name = it.name, serving = it.serving, kcal = it.kcal, protein = it.protein, carbs = it.carbs, fat = it.fat, junk = it.junk, uses = 1, lastUsed = now()))
        }
        onChange()
    }

    /** Copies the entries of one meal from yesterday into today's same meal. */
    suspend fun repeatYesterday(meal: com.healthtracker.app.domain.Meal): Int {
        val y = today() - 1
        val src = dao.allFood().filter { it.day == y && com.healthtracker.app.domain.Meal.of(it.time) == meal }
        val t = meal.timeOn(java.time.LocalDate.now())
        src.forEachIndexed { i, e -> dao.insertFood(e.copy(id = 0, day = today(), time = t + i)) }
        return src.size
    }

    /** Saves a meal's entries as one reusable food, like "My thali". */
    suspend fun saveCombo(name: String, entries: List<FoodEntry>) {
        if (entries.isEmpty()) return
        dao.upsertMyFood(
            (dao.myFoodByName(name) ?: MyFood(name = name, serving = "1 combo", kcal = 0, protein = 0.0, carbs = 0.0, fat = 0.0, junk = false)).copy(
                serving = entries.joinToString(" + ") { e -> (if (e.servings != 1.0) "${Fmt.one(e.servings).removeSuffix(".0")} " else "") + e.name },
                kcal = entries.sumOf { it.kcal }, protein = entries.sumOf { it.protein }, carbs = entries.sumOf { it.carbs },
                fat = entries.sumOf { it.fat }, junk = entries.any { it.junk }, uses = 5, lastUsed = now(),
            )
        )
    }

    suspend fun saveMyFood(f: MyFood) = dao.upsertMyFood(f)

    suspend fun deleteMyFood(id: Long) = dao.deleteMyFood(id)

    suspend fun foodLoggedSince(millis: Long): Boolean = dao.foodCountSince(millis) > 0

    suspend fun logPause(pkg: String, appName: String, outcome: String) =
        dao.insertPause(PauseEvent(time = now(), day = today(), pkg = pkg, appName = appName, outcome = outcome))

    suspend fun pausesSince(day: Long) = dao.pausesFrom(day)

    suspend fun allPauses() = dao.allPauses()

    /** Logs a food-app order as an honest estimate, so it counts toward the day's calories. */
    suspend fun logOrder(appName: String, label: String, kcal: Int, planned: Boolean) {
        dao.insertFood(
            FoodEntry(
                day = today(), time = now(), name = "$appName order · $label", servings = 1.0, kcal = kcal,
                protein = kcal * 0.03, carbs = kcal * 0.11, fat = kcal * 0.045, junk = !planned, plannedMeal = planned,
            )
        )
    }

    suspend fun setMinimumDay(on: Boolean) {
        if (on) dao.upsertMinimumDay(MinimumDay(today())) else dao.deleteMinimumDay(today())
    }

    suspend fun saveActivity(list: List<DailyActivity>) = dao.upsertActivity(list)

    /**
     * Recalculates the calorie target every 5 kg lost, and starts the next goal automatically
     * once the current goal's date has passed.
     */
    suspend fun maintainGoals(today: LocalDate) {
        val s = settings.current()
        if (!s.onboarded) return
        val weights = dao.allWeights()
        val trend = Calc.trendOn(weights, today.toEpochDay()) ?: s.startWeight

        if (s.calcWeight - trend >= 5.0) {
            val next = s.copy(calcWeight = (trend * 10).roundToInt() / 10.0)
            val newTarget = Calc.calorieTarget(next, today)
            settings.update {
                it.copy(
                    calcWeight = next.calcWeight,
                    recalcNotice = "You've lost another 5 kg, so your body needs less energy now. Your daily target is updated to ${Fmt.int(newTarget)} kcal.",
                )
            }
        }

        val goalDay = s.goalDay
        if (goalDay != null && today.toEpochDay() > goalDay) {
            val startKg = s.goalStartKg ?: s.startWeight
            val reached = trend <= s.goalKg + 0.05
            val title = if (s.goalName == "your birthday") "Your birthday goal" else "Your ${s.goalName} goal"
            val result = if (reached) "$title is done: you reached ${Fmt.kg(trend)} kg. That's ${Fmt.kg(startKg - trend)} kg down."
            else "$title date has passed. You're at ${Fmt.kg(trend)} kg, ${Fmt.kg((startKg - trend).coerceAtLeast(0.0))} kg down since it started. That's real progress, so keep going."
            val ng = Calc.nextGoal(trend, s.startWeight, s.heightCm, s.pace, today)
            settings.update {
                it.copy(
                    goalName = ng.name, goalKg = ng.kg, goalDay = ng.day,
                    goalStartDay = today.toEpochDay(), goalStartKg = trend, lastGoalResult = result,
                )
            }
        }
    }

    // ---- Export / import, so data survives a reinstall ----

    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "health-tracker")
        root.put("version", 1)
        root.put("exportedAt", now())
        root.put("weights", JSONArray(dao.allWeights().map { JSONObject().put("day", it.day).put("kg", it.kg).put("loggedAt", it.loggedAt) }))
        root.put("waists", JSONArray(dao.allWaists().map { JSONObject().put("day", it.day).put("cm", it.cm) }))
        root.put("water", JSONArray(dao.allWater().map { JSONObject().put("day", it.day).put("time", it.time).put("ml", it.ml) }))
        root.put("food", JSONArray(dao.allFood().map {
            JSONObject().put("day", it.day).put("time", it.time).put("name", it.name).put("servings", it.servings)
                .put("kcal", it.kcal).put("protein", it.protein).put("carbs", it.carbs).put("fat", it.fat)
                .put("junk", it.junk).put("plannedMeal", it.plannedMeal)
        }))
        root.put("myFoods", JSONArray(dao.allMyFoods().map {
            JSONObject().put("name", it.name).put("serving", it.serving).put("kcal", it.kcal).put("protein", it.protein)
                .put("carbs", it.carbs).put("fat", it.fat).put("junk", it.junk).put("uses", it.uses).put("lastUsed", it.lastUsed)
        }))
        root.put("pauses", JSONArray(dao.allPauses().map {
            JSONObject().put("time", it.time).put("day", it.day).put("pkg", it.pkg).put("appName", it.appName).put("outcome", it.outcome)
        }))
        root.put("minimumDays", JSONArray(dao.allMinimumDays().map { it.day }))
        val s = settings.current()
        root.put("settings", JSONObject().apply {
            put("name", s.name); put("birthDay", s.birthDay ?: JSONObject.NULL); put("sex", s.sex.name)
            put("heightCm", s.heightCm); put("startWeight", s.startWeight); put("startDay", s.startDay)
            put("activity", s.activity.name); put("pace", s.pace.name); put("calcWeight", s.calcWeight)
            put("calorieOverride", s.calorieOverride ?: JSONObject.NULL)
            put("goalName", s.goalName); put("goalKg", s.goalKg); put("goalDay", s.goalDay ?: JSONObject.NULL)
            put("goalStartDay", s.goalStartDay ?: JSONObject.NULL); put("goalStartKg", s.goalStartKg ?: JSONObject.NULL)
            put("waterGoalMl", s.waterGoalMl); put("stepGoal", s.stepGoal)
            put("pauseApps", JSONArray(s.pauseApps.toList())); put("customPauseMessage", s.customPauseMessage ?: JSONObject.NULL)
            put("pauseWaitSec", s.pauseWaitSec); put("plannedMealsPerWeek", s.plannedMealsPerWeek)
        })
        return root.toString(2)
    }

    /** Replaces everything with the contents of an export. Returns a short summary. */
    suspend fun importJson(text: String): String {
        val root = JSONObject(text)
        require(root.optString("app") == "health-tracker") { "This file isn't a Health Tracker export." }

        fun arr(name: String): JSONArray = root.optJSONArray(name) ?: JSONArray()
        fun <T> JSONArray.mapObj(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }
        fun JSONObject.optLongOrNull(k: String): Long? = if (isNull(k) || !has(k)) null else getLong(k)
        fun JSONObject.optDoubleOrNull(k: String): Double? = if (isNull(k) || !has(k)) null else getDouble(k)
        fun JSONObject.optIntOrNull(k: String): Int? = if (isNull(k) || !has(k)) null else getInt(k)

        val weights = arr("weights").mapObj { WeightEntry(it.getLong("day"), it.getDouble("kg"), it.optLong("loggedAt")) }
        val waists = arr("waists").mapObj { WaistEntry(it.getLong("day"), it.getDouble("cm")) }
        val water = arr("water").mapObj { WaterEntry(day = it.getLong("day"), time = it.getLong("time"), ml = it.getInt("ml")) }
        val food = arr("food").mapObj {
            FoodEntry(day = it.getLong("day"), time = it.getLong("time"), name = it.getString("name"), servings = it.optDouble("servings", 1.0),
                kcal = it.getInt("kcal"), protein = it.optDouble("protein", 0.0), carbs = it.optDouble("carbs", 0.0), fat = it.optDouble("fat", 0.0),
                junk = it.optBoolean("junk"), plannedMeal = it.optBoolean("plannedMeal"))
        }
        val myFoods = arr("myFoods").mapObj {
            MyFood(name = it.getString("name"), serving = it.optString("serving", "1 serving"), kcal = it.getInt("kcal"),
                protein = it.optDouble("protein", 0.0), carbs = it.optDouble("carbs", 0.0), fat = it.optDouble("fat", 0.0),
                junk = it.optBoolean("junk"), uses = it.optInt("uses"), lastUsed = it.optLong("lastUsed"))
        }
        val pauses = arr("pauses").mapObj {
            PauseEvent(time = it.getLong("time"), day = it.getLong("day"), pkg = it.getString("pkg"), appName = it.optString("appName"), outcome = it.getString("outcome"))
        }
        val minDays = arr("minimumDays").let { a -> (0 until a.length()).map { MinimumDay(a.getLong(it)) } }

        dao.clearWeights(); dao.clearWaists(); dao.clearWater(); dao.clearFood()
        dao.clearMyFoods(); dao.clearPauses(); dao.clearMinimumDays()
        dao.insertWeights(weights); dao.insertWaists(waists); dao.insertWaters(water); dao.insertFoods(food)
        dao.insertMyFoods(myFoods); dao.insertPauses(pauses); dao.insertMinimumDays(minDays)

        root.optJSONObject("settings")?.let { j ->
            settings.update { s ->
                s.copy(
                    onboarded = true,
                    name = j.optString("name", s.name),
                    birthDay = j.optLongOrNull("birthDay"),
                    sex = runCatching { Sex.valueOf(j.getString("sex")) }.getOrDefault(s.sex),
                    heightCm = j.optDouble("heightCm", s.heightCm),
                    startWeight = j.optDouble("startWeight", s.startWeight),
                    startDay = j.optLong("startDay", s.startDay),
                    activity = runCatching { ActivityLevel.valueOf(j.getString("activity")) }.getOrDefault(s.activity),
                    pace = runCatching { Pace.valueOf(j.getString("pace")) }.getOrDefault(s.pace),
                    calcWeight = j.optDouble("calcWeight", s.calcWeight),
                    calorieOverride = j.optIntOrNull("calorieOverride"),
                    goalName = j.optString("goalName", s.goalName),
                    goalKg = j.optDouble("goalKg", s.goalKg),
                    goalDay = j.optLongOrNull("goalDay"),
                    goalStartDay = j.optLongOrNull("goalStartDay"),
                    goalStartKg = j.optDoubleOrNull("goalStartKg"),
                    waterGoalMl = j.optInt("waterGoalMl", s.waterGoalMl),
                    stepGoal = j.optInt("stepGoal", s.stepGoal),
                    pauseApps = j.optJSONArray("pauseApps")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: s.pauseApps,
                    customPauseMessage = if (j.isNull("customPauseMessage")) null else j.optString("customPauseMessage"),
                    pauseWaitSec = j.optInt("pauseWaitSec", s.pauseWaitSec),
                    plannedMealsPerWeek = j.optInt("plannedMealsPerWeek", s.plannedMealsPerWeek),
                )
            }
        }
        return "Restored ${weights.size} weigh-ins and ${food.size} food entries."
    }
}
