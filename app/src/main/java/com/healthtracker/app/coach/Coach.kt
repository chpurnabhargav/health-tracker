package com.healthtracker.app.coach

import com.healthtracker.app.data.SettingsRepository
import com.healthtracker.app.data.UserSettings
import com.healthtracker.app.domain.Calc
import com.healthtracker.app.domain.DayNutrition
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.domain.HealthState
import com.healthtracker.app.domain.Meal
import com.healthtracker.app.domain.ParsedItem
import com.healthtracker.app.domain.Scoring
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Decides what Coach is allowed to talk about, and what app data it gets to see. */
object CoachContext {
    const val OFF_TOPIC = "I can only help with your health, food and fitness in this app."

    /** Words that mark a question as on-topic when a word in the question starts with them. */
    private val topicPrefixes = listOf(
        "eat", "ate", "food", "meal", "breakfast", "lunch", "dinner", "snack", "diet", "calorie", "kcal", "protein",
        "carb", "fibre", "fiber", "sugar", "salt", "sodium", "vitamin", "nutri", "macro", "portion", "ghee",
        "water", "drink", "hydrat", "thirst", "weigh", "kg", "lose", "losing", "lost", "gain", "bmi", "waist", "belly",
        "sleep", "tired", "energy", "steps", "walk", "running", "exercis", "workout", "gym", "muscle", "strength", "cardio", "yoga",
        "craving", "crave", "hungry", "hunger", "swiggy", "zomato", "order", "restaurant", "junk", "sweet", "dessert",
        "roti", "chapati", "rice", "dal", "chicken", "paneer", "eggs", "fruit", "veg", "salad", "chai", "coffee", "milk", "curd",
        "biryani", "pizza", "burger", "alcohol", "beer", "healthy", "health", "fitness", "goal", "birthday", "target", "progress",
        "plateau", "stuck", "motivat", "habit", "streak", "pause", "lockdown", "nutrition", "obes", "overweight", "slim",
    )
    /** Short words that only count as a whole word, so "teach" or "explain" don't match "tea" or "plan". */
    private val topicWords = setOf(
        "fat", "fats", "oil", "tea", "egg", "app", "fit", "run", "log", "plan", "track", "week", "score", "trend", "body", "full", "diet",
    )
    private val topicPhrases = listOf("how am i doing", "how did i do", "am i on track", "this week", "last week", "my weight")

    private val greetings = setOf("hi", "hii", "hello", "hey", "yo", "hola", "namaste", "thanks", "thank you", "thx", "ok", "okay", "cool")

    /** Free local reply for greetings, so they don't use the daily quota. */
    fun localReply(q: String, name: String): String? {
        val t = q.trim().lowercase().trimEnd('!', '.', '?')
        return if (t in greetings) {
            if (t.startsWith("thank") || t == "thx") "Anytime, $name. Keep going!"
            else "Hi $name! Ask me about your food, calories, weight, water, sleep or cravings."
        } else null
    }

    /** True when a question is about health, food, fitness or this app. Checked before any call to Gemini. */
    fun onTopic(q: String, hasHistory: Boolean): Boolean {
        val t = q.lowercase()
        val words = t.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        if (words.any { w -> w in topicWords || topicPrefixes.any { w.startsWith(it) } }) return true
        if (topicPhrases.any { t.contains(it) }) return true
        return hasHistory && words.size <= 4
    }

    private val day = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)

    /** A compact summary of the person's data. This is everything Coach knows. */
    fun summary(h: HealthState): String {
        val s = h.s
        val today = h.today
        val n = DayNutrition.of(h.todayFood)
        val score = Scoring.score(n, h.calorieTarget, h.proteinTarget, dayDone = false)
        val age = Calc.age(s.birthDay, today)
        val sb = StringBuilder()
        sb.appendLine("Today: ${today.format(day)} ${today.year}, time ${LocalTime.now().withNano(0).withSecond(0)}.")
        sb.appendLine(
            "Profile: ${s.displayName}, ${s.sex.label.lowercase()}${age?.let { ", $it y" } ?: ""}, ${Fmt.int(s.heightCm)} cm, ${s.activity.label.lowercase()}. " +
                "Started ${Fmt.kg(s.startWeight)} kg on ${LocalDate.ofEpochDay(s.startDay).format(day)}. " +
                "Trend now ${h.trendNow?.let { Fmt.kg(it) + " kg" } ?: "unknown"} (lost ${Fmt.kg(h.totalLost)} kg), " +
                "weekly rate ${h.weeklyRate?.let { Fmt.kg(it) + " kg/week" } ?: "not enough data"}, BMI ${Fmt.one(h.bmiNow)}.",
        )
        val goal = if (h.isBirthdayGoal) "birthday" else s.goalName
        sb.appendLine(
            "Goal: ${Fmt.kg(s.goalKg)} kg by $goal${h.goalDate?.let { " (${it.format(day)}, ${h.daysLeft} days left)" } ?: ""}" +
                (h.projectedAtGoal?.let { ", projected ${Fmt.kg(it)} kg" } ?: "") + ".",
        )
        sb.appendLine(
            "Targets: ${Fmt.int(h.calorieTarget)} kcal (maintenance about ${Fmt.int(h.maintenance)}), protein ${h.proteinTarget} g, " +
                "water ${Fmt.litres(s.waterGoalMl)} L, steps ${Fmt.int(s.stepGoal)}, pace ${s.pace.label}.",
        )
        sb.appendLine(
            "Today so far: ${Fmt.int(n.kcal)} kcal, protein ${n.protein.roundToInt()} g, carbs ${n.carbs.roundToInt()} g, fat ${n.fat.roundToInt()} g, " +
                "fibre ${n.fibre.roundToInt()} g, veg/fruit ${Fmt.one(n.vegServings)} servings, junk ${n.junkItems}, sugary ${n.sugaryItems}. " +
                "Water ${Fmt.litres(h.todayWaterMl)} L. Steps ${h.stepsToday?.let { Fmt.int(it) } ?: "unknown"}. " +
                "Sleep ${h.sleepLastNight?.let { Fmt.hours(it) } ?: "unknown"}. Nutrition score ${score.total}/100.",
        )
        val meals = h.todayFood.groupBy { Meal.of(it.time) }
        if (meals.isNotEmpty()) {
            sb.appendLine("Today's food: " + Meal.entries.filter { it in meals }.joinToString("; ") { m ->
                m.label + ": " + meals[m]!!.joinToString(", ") { "${it.name}${if (it.servings != 1.0) " ×" + Fmt.one(it.servings) else ""} ${it.kcal}" }
            })
        }
        sb.appendLine("Last 7 days (day: kcal, protein g, carbs %, water L, steps, sleep, weight):")
        h.last7Days.forEach { d ->
            val f = h.food.filter { it.day == d.date.toEpochDay() }
            val carbs = f.sumOf { it.carbs }
            val carbPct = if (d.kcal > 0) (carbs * 4 * 100 / d.kcal).roundToInt() else 0
            val w = h.weights.find { it.day == d.date.toEpochDay() }?.kg
            sb.appendLine(
                "- ${d.date.format(day)}: ${if (d.foodLogged) "${d.kcal}, ${d.protein.roundToInt()}, $carbPct%" else "not logged"}, " +
                    "${Fmt.litres(d.waterMl)}, ${d.steps ?: "-"}, ${d.sleepMin?.let { Fmt.hours(it) } ?: "-"}, ${w?.let { Fmt.kg(it) } ?: "-"}",
            )
        }
        val r = h.review
        sb.appendLine(
            "Last week: avg ${r.avgKcal?.let { Fmt.int(it) } ?: "-"} kcal, protein ${r.avgProtein ?: "-"} g, weight change ${r.weightChange?.let { Fmt.signedKg(it) } ?: "-"} kg, " +
                "over target on ${r.overDays.size} days, water goal hit ${r.waterHitDays}/7, late-night eating ${r.lateNights} nights.",
        )
        val top = h.topSources.take(3).joinToString(", ") { "${it.name} ${Fmt.pct(it.share)}" }
        if (top.isNotEmpty()) sb.appendLine("Top calorie sources this week: $top.")
        sb.appendLine(
            "Food apps: closed ${h.monthClosed} of ${h.monthPauses.size} times this month, money saved ₹${Fmt.int(h.moneySaved)}. " +
                "Cravings beaten with SOS: ${s.cravingWins}. Pause mode ${s.pauseMode.label}" +
                (if (s.nightLockdown) ", night lockdown ${Fmt.clock(s.lockStartMin)}–${Fmt.clock(s.lockEndMin)}" else "") + ".",
        )
        h.waistLatest?.let { w -> sb.appendLine("Waist ${Fmt.one(w.cm)} cm" + (h.waistFirst?.let { f -> " (started ${Fmt.one(f.cm)})" } ?: "") + ".") }
        return sb.toString()
    }

    fun systemPrompt(h: HealthState, task: String? = null): String {
        val s = h.s
        val goal = if (h.isBirthdayGoal) "their birthday" else s.goalName
        return """
            You are Coach, the assistant inside ${s.displayName}'s personal Health Tracker app. ${s.displayName} is losing weight towards ${Fmt.kg(s.goalKg)} kg by $goal.
            Scope: only help with ${s.displayName}'s food, nutrition, calories, weight, water, sleep, steps, exercise, cravings, habits, motivation, and how to use this app.
            If a question is outside that scope (for example coding, news, studies, trivia, other people, or anything not about their health), reply exactly: "$OFF_TOPIC"
            Use the APP DATA below. Never invent numbers that aren't in it; if something isn't logged, say so.
            Style: talk to ${s.displayName} directly and use their name now and then. Be warm but honest and specific. Prefer Indian food examples, metric units and ₹.
            Keep answers under 120 words unless asked for detail. Short paragraphs or up to 4 bullet points with "-". No headings, no tables, no bold.
            Safety: you're not a doctor. Don't diagnose or suggest medicines. For pain, dizziness, fainting, eating under 1,200 kcal, or blood test results, suggest seeing a doctor. Never suggest crash diets or skipping meals.
            ${task ?: ""}
            APP DATA:
            ${summary(h)}
        """.trimIndent()
    }
}

/** Talks to Gemini on the person's behalf, within a daily limit, and caches what it can. */
class Coach(private val settings: SettingsRepository) {

    data class Msg(val role: String, val text: String)

    fun history(s: UserSettings): List<Msg> = runCatching {
        val a = JSONArray(s.coachChat)
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Msg(o.getString("role"), o.getString("text")) } }
    }.getOrDefault(emptyList())

    private suspend fun saveHistory(msgs: List<Msg>) {
        val a = JSONArray()
        msgs.takeLast(30).forEach { a.put(JSONObject().put("role", it.role).put("text", it.text)) }
        settings.update { it.copy(coachChat = a.toString()) }
    }

    suspend fun clearHistory() = settings.update { it.copy(coachChat = "[]") }

    fun callsLeft(s: UserSettings): Int {
        val today = LocalDate.now().toEpochDay()
        val used = if (s.coachCallsDay == today) s.coachCalls else 0
        return (s.coachDailyLimit - used).coerceAtLeast(0)
    }

    /** One call to Gemini, trying models until one works, and counting it toward today's limit. */
    private suspend fun call(system: String, turns: List<Turn>, maxTokens: Int, schema: JSONObject? = null, keyOverride: String? = null): String {
        val s = settings.current()
        val key = keyOverride ?: s.coachKey
        if (key.isBlank()) throw CoachError("Add your Gemini API key in Coach first.")
        if (keyOverride == null && callsLeft(s) <= 0) {
            throw CoachError("You've used today's ${s.coachDailyLimit} Coach questions. They reset at midnight.")
        }
        val models = if (s.coachModel.isNotBlank()) listOf(s.coachModel)
        else (listOf(s.coachWorkingModel) + Gemini.fallbackModels).filter { it.isNotBlank() }.distinct()
        var last: CoachError? = null
        for (m in models) {
            val noThinking = m.contains("2.5-flash")
            for (attempt in 0..1) {
                try {
                    val text = try {
                        Gemini.parseText(Gemini.post(key, m, Gemini.buildRequest(system, turns, maxTokens, schema, noThinking)))
                    } catch (e: CoachError) {
                        if (e.httpCode == 400 && noThinking) Gemini.parseText(Gemini.post(key, m, Gemini.buildRequest(system, turns, maxTokens, schema, false)))
                        else throw e
                    }
                    val today = LocalDate.now().toEpochDay()
                    settings.update {
                        if (keyOverride != null) it.copy(coachWorkingModel = m)
                        else it.copy(
                            coachWorkingModel = m,
                            coachCallsDay = today,
                            coachCalls = (if (it.coachCallsDay == today) it.coachCalls else 0) + 1,
                        )
                    }
                    return text
                } catch (e: CoachError) {
                    last = e
                    when {
                        // Busy server: wait a moment and try the same model once more.
                        e.httpCode >= 500 && attempt == 0 -> kotlinx.coroutines.delay(1500)
                        // Busy, out of quota or missing: try the next model (free limits are per model).
                        e.httpCode >= 500 || e.httpCode == 429 || e.httpCode == 404 -> break
                        else -> throw e
                    }
                }
            }
        }
        val l = last
        throw when {
            l == null -> CoachError("Coach hit a problem.")
            l.httpCode == 404 -> CoachError("None of the Gemini models answered. Set a model name in Plan → Coach.")
            else -> l
        }
    }

    /** Checks a key with one tiny request (doesn't count toward the daily limit). */
    suspend fun testKey(key: String): String =
        call("Reply with the single word OK.", listOf(Turn("user", "Say OK")), 50, keyOverride = key.trim())

    /** Answers a question about the person's data. Off-topic questions are refused without using the quota. */
    suspend fun ask(h: HealthState, question: String): String {
        val q = question.trim()
        val past = history(h.s)
        val reply = CoachContext.localReply(q, h.s.displayName)
            ?: if (!CoachContext.onTopic(q, past.isNotEmpty())) CoachContext.OFF_TOPIC
            else {
                val turns = past.takeLast(6).map { Turn(it.role, it.text) } + Turn("user", q)
                call(CoachContext.systemPrompt(h), turns, 900)
            }
        saveHistory(past + Msg("user", q) + Msg("model", reply))
        return reply
    }

    /** One short tip for today, generated at most once a day. */
    suspend fun dailyTip(h: HealthState): String {
        val today = h.todayDay
        if (h.s.coachTipDay == today && h.s.coachTip.isNotBlank()) return h.s.coachTip
        val tip = call(
            CoachContext.systemPrompt(h, "Task: give one specific, practical tip for today based on the data. Max 30 words. No greeting."),
            listOf(Turn("user", "What's my tip for today?")), 400,
        )
        settings.update { it.copy(coachTipDay = today, coachTip = tip) }
        return tip
    }

    /** Coach's take on the last 7 days, generated at most once per review week. */
    suspend fun weeklyReview(h: HealthState): String {
        val key = h.review.to.toEpochDay()
        if (h.s.coachReviewDay == key && h.s.coachReview.isNotBlank()) return h.s.coachReview
        val text = call(
            CoachContext.systemPrompt(
                h,
                "Task: write the weekly review for ${h.review.from}–${h.review.to}. Format: one-line verdict; then 'Went well:' with 2 bullets; " +
                    "'To fix:' with 2 bullets; then 'Next week:' with one focus. Max 130 words.",
            ),
            listOf(Turn("user", "Write my weekly review.")), 1000,
        )
        settings.update { it.copy(coachReviewDay = key, coachReview = text) }
        return text
    }

    /** Estimates calories for foods the app doesn't know, in one request. */
    suspend fun estimate(h: HealthState, items: List<ParsedItem>): List<ParsedItem> {
        if (items.isEmpty()) return emptyList()
        val schema = JSONObject()
            .put("type", "ARRAY")
            .put(
                "items", JSONObject().put("type", "OBJECT").put(
                    "properties", JSONObject()
                        .put("name", JSONObject().put("type", "STRING"))
                        .put("serving", JSONObject().put("type", "STRING"))
                        .put("kcal", JSONObject().put("type", "INTEGER"))
                        .put("protein", JSONObject().put("type", "NUMBER"))
                        .put("carbs", JSONObject().put("type", "NUMBER"))
                        .put("fat", JSONObject().put("type", "NUMBER"))
                        .put("junk", JSONObject().put("type", "BOOLEAN")),
                ).put("required", JSONArray(listOf("name", "serving", "kcal", "protein", "carbs", "fat", "junk"))),
            )
        val list = items.joinToString("\n") { "- ${it.text}" }
        val system = "You estimate nutrition for Indian and everyday foods as typically served in India. " +
            "For each line, return one object for ONE serving of that food as described (ignore any number of servings at the start). " +
            "Use a short, clear food name. junk = true for fried food, sweets, sugary drinks and fast food. Return items in the same order."
        val text = call(system, listOf(Turn("user", list)), 1200, schema)
        val arr = runCatching { JSONArray(text) }.getOrElse { throw CoachError("Coach couldn't read those foods. Try adding them one by one.") }
        return items.mapIndexed { i, it ->
            val o = arr.optJSONObject(i) ?: return@mapIndexed it
            it.copy(
                name = o.optString("name").ifBlank { it.text }.replaceFirstChar { c -> c.uppercase() },
                serving = o.optString("serving", "1 serving"),
                kcal = o.optInt("kcal").coerceIn(0, 3000),
                protein = o.optDouble("protein", 0.0).coerceIn(0.0, 200.0),
                carbs = o.optDouble("carbs", 0.0).coerceIn(0.0, 400.0),
                fat = o.optDouble("fat", 0.0).coerceIn(0.0, 200.0),
                junk = o.optBoolean("junk"),
                estimated = true,
            )
        }
    }
}
