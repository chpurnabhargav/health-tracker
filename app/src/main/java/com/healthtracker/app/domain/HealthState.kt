package com.healthtracker.app.domain

import com.healthtracker.app.data.DailyActivity
import com.healthtracker.app.data.FoodEntry
import com.healthtracker.app.data.MyFood
import com.healthtracker.app.data.PauseEvent
import com.healthtracker.app.data.UserSettings
import com.healthtracker.app.data.WaistEntry
import com.healthtracker.app.data.WaterEntry
import com.healthtracker.app.data.WeightEntry
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

data class DayStat(
    val date: LocalDate,
    val kcal: Int,
    val protein: Double,
    val waterMl: Int,
    val steps: Int?,
    val sleepMin: Int?,
    val lateKcal: Int,
    val foodLogged: Boolean,
    val weightLogged: Boolean,
    val minimumDay: Boolean,
    val topItem: FoodEntry?,
) {
    val logged: Boolean get() = foodLogged || weightLogged || minimumDay
}

data class SourceShare(val name: String, val kcal: Int, val count: Int, val share: Double)

data class WeeklyReview(
    val from: LocalDate,
    val to: LocalDate,
    val days: List<DayStat>,
    val trendFrom: Double?,
    val trendTo: Double?,
    val avgKcal: Int?,
    val overDays: List<DayStat>,
    val waterHitDays: Int,
    val avgWaterMl: Int,
    val avgSteps: Int?,
    val avgSleepMin: Int?,
    val avgProtein: Int?,
    val pauseOpens: Int,
    val pauseClosed: Int,
    val daysLogged: Int,
    val lateNights: Int,
    val lateKcal: Int,
    val headline: String,
    val noticed: List<String>,
    val focus: String,
) {
    val weightChange: Double? get() = if (trendFrom != null && trendTo != null) trendTo - trendFrom else null
}

/** Everything the screens show, worked out from the stored data. */
class HealthState(
    val today: LocalDate,
    val s: UserSettings,
    val weights: List<WeightEntry>,
    val waists: List<WaistEntry>,
    val food: List<FoodEntry>,
    val water: List<WaterEntry>,
    val pauses: List<PauseEvent>,
    val activity: List<DailyActivity>,
    val minimumDays: Set<Long>,
    val myFoods: List<MyFood>,
) {
    private val zone: ZoneId = ZoneId.systemDefault()
    val todayDay: Long = today.toEpochDay()

    val calorieTarget: Int by lazy { Calc.calorieTarget(s, today) }
    val maintenance: Int by lazy { Calc.maintenance(s, today) }
    val proteinTarget: Int by lazy { Calc.proteinTarget(s.heightCm) }

    val todayFood: List<FoodEntry> by lazy { food.filter { it.day == todayDay } }
    val todayKcal: Int by lazy { todayFood.sumOf { it.kcal } }
    val todayProtein: Double by lazy { todayFood.sumOf { it.protein } }
    val todayWaterMl: Int by lazy { water.filter { it.day == todayDay }.sumOf { it.ml } }

    val todayWeight: WeightEntry? by lazy { weights.find { it.day == todayDay } }
    val previousWeight: WeightEntry? by lazy { weights.filter { it.day < todayDay }.maxByOrNull { it.day } }
    val latestWeight: WeightEntry? by lazy { weights.maxByOrNull { it.day } }

    val trendNow: Double? by lazy { Calc.trendOn(weights, todayDay) }
    val trendWeekAgo: Double? by lazy {
        val before = weights.filter { it.day <= todayDay - 7 }
        Calc.trendOn(before, todayDay - 7)
    }
    val weekChange: Double? by lazy { trendNow?.let { n -> trendWeekAgo?.let { n - it } } }
    val currentKg: Double by lazy { trendNow ?: s.startWeight }
    val totalLost: Double by lazy { s.startWeight - currentKg }
    val weeklyRate: Double? by lazy { Calc.weeklyRate(weights, today) }
    val bmiNow: Double by lazy { Calc.bmi(currentKg, s.heightCm) }
    val bmiStart: Double by lazy { Calc.bmi(s.startWeight, s.heightCm) }
    val trendSeries: List<TrendPoint> by lazy { Calc.trendSeries(weights) }

    // Goal
    val goalDate: LocalDate? by lazy { s.goalDay?.let { LocalDate.ofEpochDay(it) } }
    val daysLeft: Long? by lazy { s.goalDay?.let { it - todayDay } }
    val goalStartKg: Double by lazy { s.goalStartKg ?: s.startWeight }
    val goalToLose: Double by lazy { (goalStartKg - s.goalKg).coerceAtLeast(0.1) }
    val goalLost: Double by lazy { (goalStartKg - currentKg).coerceAtLeast(0.0) }
    val goalProgress: Float by lazy { (goalLost / goalToLose).coerceIn(0.0, 1.0).toFloat() }
    val isBirthdayGoal: Boolean get() = s.goalName == "your birthday"
    val projectedAtGoal: Double? by lazy {
        val r = weeklyRate ?: return@lazy null
        val d = daysLeft ?: return@lazy null
        Calc.projectedWeight(currentKg, r.coerceAtLeast(0.0), d)
    }

    // Activity
    val stepsToday: Int? by lazy { activity.find { it.day == todayDay }?.steps }
    val sleepLastNight: Int? by lazy { activity.find { it.day == todayDay }?.sleepMin }

    val isMinimumDay: Boolean get() = todayDay in minimumDays

    fun isLogged(day: Long): Boolean =
        weights.any { it.day == day } || food.any { it.day == day } || day in minimumDays

    val loggedLast7: Int by lazy { (0..6).count { isLogged(todayDay - it) } }
    val missedYesterday: Boolean by lazy {
        todayDay - 1 >= s.startDay && !isLogged(todayDay - 1) && !isLogged(todayDay)
    }

    // Waist
    val waistLatest: WaistEntry? by lazy { waists.maxByOrNull { it.day } }
    val waistFirst: WaistEntry? by lazy { waists.minByOrNull { it.day } }
    val waistDue: Boolean by lazy { waistLatest?.let { it.day <= todayDay - 7 } ?: true }

    // Food analytics over the last 7 days, today included
    val last7Food: List<FoodEntry> by lazy { food.filter { it.day in (todayDay - 6)..todayDay } }

    val topSources: List<SourceShare> by lazy {
        val total = last7Food.sumOf { it.kcal }.coerceAtLeast(1)
        last7Food.groupBy { it.name }
            .map { (name, list) -> SourceShare(name, list.sumOf { it.kcal }, list.size, list.sumOf { it.kcal }.toDouble() / total) }
            .sortedByDescending { it.kcal }
            .take(5)
    }
    val last7Kcal: Int by lazy { last7Food.sumOf { it.kcal } }

    val macroKcal: Triple<Double, Double, Double> by lazy {
        Triple(last7Food.sumOf { it.protein } * 4, last7Food.sumOf { it.carbs } * 4, last7Food.sumOf { it.fat } * 9)
    }
    val macroGramsPerDay: Triple<Int, Int, Int> by lazy {
        val days = last7Food.map { it.day }.distinct().size.coerceAtLeast(1)
        Triple(
            (last7Food.sumOf { it.protein } / days).roundToInt(),
            (last7Food.sumOf { it.carbs } / days).roundToInt(),
            (last7Food.sumOf { it.fat } / days).roundToInt(),
        )
    }

    private fun hourOf(time: Long): Int = Instant.ofEpochMilli(time).atZone(zone).hour

    val lateEntries: List<FoodEntry> by lazy { last7Food.filter { hourOf(it.time) >= 21 } }

    // Pauses
    val monthPauses: List<PauseEvent> by lazy {
        val first = today.withDayOfMonth(1).toEpochDay()
        pauses.filter { it.day >= first }
    }
    val monthClosed: Int by lazy { monthPauses.count { it.outcome == "closed" } }
    val closedAll: Int by lazy { pauses.count { it.outcome == "closed" } }
    val moneySaved: Int by lazy { closedAll * s.orderValue }

    val weekStart: LocalDate by lazy { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val plannedUsedThisWeek: Int by lazy {
        pauses.count { it.outcome == "planned" && it.day >= weekStart.toEpochDay() } +
            food.count { it.plannedMeal && it.day >= weekStart.toEpochDay() }
    }
    val plannedLeftThisWeek: Int get() = (s.plannedMealsPerWeek - plannedUsedThisWeek).coerceAtLeast(0)

    fun dayStat(d: LocalDate): DayStat {
        val day = d.toEpochDay()
        val f = food.filter { it.day == day }
        val a = activity.find { it.day == day }
        return DayStat(
            date = d,
            kcal = f.sumOf { it.kcal },
            protein = f.sumOf { it.protein },
            waterMl = water.filter { it.day == day }.sumOf { it.ml },
            steps = a?.steps,
            sleepMin = a?.sleepMin,
            lateKcal = f.filter { hourOf(it.time) >= 21 }.sumOf { it.kcal },
            foodLogged = f.isNotEmpty(),
            weightLogged = weights.any { it.day == day },
            minimumDay = day in minimumDays,
            topItem = f.maxByOrNull { it.kcal },
        )
    }

    /** The 7 days ending today, oldest first. */
    val last7Days: List<DayStat> by lazy { (6 downTo 0).map { dayStat(today.minusDays(it.toLong())) } }

    /** Review of the last 7 complete days (today not included). */
    val review: WeeklyReview by lazy { buildReview() }

    private fun buildReview(): WeeklyReview {
        val to = today.minusDays(1)
        val from = today.minusDays(7)
        val days = (7 downTo 1).map { dayStat(today.minusDays(it.toLong())) }
        val foodDays = days.filter { it.foodLogged }
        val avgKcal = if (foodDays.isEmpty()) null else foodDays.sumOf { it.kcal } / foodDays.size
        val over = foodDays.filter { it.kcal > calorieTarget }
        val waterHit = days.count { it.waterMl >= s.waterGoalMl }
        val waterDays = days.filter { it.waterMl > 0 }
        val avgWater = if (waterDays.isEmpty()) 0 else waterDays.sumOf { it.waterMl } / waterDays.size
        val stepDays = days.mapNotNull { it.steps }
        val sleepDays = days.mapNotNull { it.sleepMin }.filter { it > 0 }
        val avgProtein = if (foodDays.isEmpty()) null else (foodDays.sumOf { it.protein } / foodDays.size).roundToInt()
        val windowPauses = pauses.filter { it.day in from.toEpochDay()..to.toEpochDay() }
        val closed = windowPauses.count { it.outcome == "closed" }
        val lateNights = days.count { it.lateKcal > 0 }
        val lateKcal = days.sumOf { it.lateKcal }
        val before = weights.filter { it.day <= from.toEpochDay() - 1 }
        val trendFrom = Calc.trendOn(before, from.toEpochDay() - 1) ?: weights.filter { it.day <= to.toEpochDay() }.minByOrNull { it.day }?.kg
        val trendTo = Calc.trendOn(weights.filter { it.day <= to.toEpochDay() }, to.toEpochDay())
        val change = if (trendFrom != null && trendTo != null) trendTo - trendFrom else null
        val avgSteps = if (stepDays.isEmpty()) null else stepDays.sum() / stepDays.size
        val avgSleep = if (sleepDays.isEmpty()) null else sleepDays.sum() / sleepDays.size

        val headline = when {
            foodDays.isEmpty() -> "Not enough data yet. Log your weight and meals every day, and next week's review will have real numbers."
            avgKcal!! <= calorieTarget && (change ?: 0.0) < 0 ->
                if (over.isEmpty()) "A strong week. Every day you logged landed at or under target, and your trend went down."
                else "A strong week. You had ${over.size} big ${if (over.size == 1) "day" else "days"}, but your weekly average still stayed under target."
            avgKcal <= calorieTarget -> "Calories were on target. If the scale hasn't moved yet, that's normal: water and salt can hide fat loss for a week or two."
            (change ?: 0.0) < 0 -> "You lost weight, but calories averaged ${Fmt.int(avgKcal - calorieTarget)} over target. Tighten up one meal a day."
            else -> "A tough week, and that's okay. One week doesn't undo your progress. Pick the one focus below and start there."
        }

        val noticed = mutableListOf<String>()
        val weekendOver = over.filter { it.date.dayOfWeek == DayOfWeek.SATURDAY || it.date.dayOfWeek == DayOfWeek.SUNDAY }
        if (over.size >= 2 && weekendOver.size == over.size) {
            noticed += "${if (over.size == 2) "Both" else "All"} of your over-target days were on the weekend."
        } else if (over.isNotEmpty()) {
            noticed += "Over target on ${over.joinToString(", ") { Fmt.weekday(it.date) }}."
        }
        if (waterDays.size >= 3) {
            val low = waterDays.minByOrNull { it.waterMl }!!
            if (low.waterMl < s.waterGoalMl) noticed += "Your lowest water day was ${Fmt.weekdayLong(low.date)}, at ${Fmt.litres(low.waterMl)} L."
        }
        if (avgProtein != null && avgProtein < proteinTarget * 0.8) {
            noticed += "Protein averaged $avgProtein g a day, below your $proteinTarget g target."
        }
        if (lateNights >= 2) noticed += "You ate after 9 PM on $lateNights nights (${Fmt.int(lateKcal)} kcal in total)."
        if (avgSleep != null && avgSleep < 390) noticed += "You slept ${Fmt.hours(avgSleep)} a night on average. Short sleep makes you hungrier the next day."

        val proteinLow = avgProtein != null && avgProtein < proteinTarget * 0.8
        val focus = when {
            foodDays.size < 4 -> "Log every meal for 7 days straight, even the messy ones."
            weekendOver.size >= 2 -> "Plan Saturday and Sunday meals on Friday night."
            proteinLow -> "Add a protein source to every meal: eggs, dal, chicken, paneer or curd."
            waterHit < 4 -> "Drink a glass of water before each meal."
            avgSteps != null && avgSteps < s.stepGoal -> "Add a 10-minute walk after dinner."
            lateNights >= 3 -> "Close the kitchen at 9 PM."
            avgSleep != null && avgSleep < 390 -> "Be in bed by the same time every night."
            else -> "Keep doing exactly what you did this week."
        }

        return WeeklyReview(
            from = from, to = to, days = days, trendFrom = trendFrom, trendTo = trendTo, avgKcal = avgKcal,
            overDays = over, waterHitDays = waterHit, avgWaterMl = avgWater, avgSteps = avgSteps,
            avgSleepMin = avgSleep, avgProtein = avgProtein, pauseOpens = windowPauses.size,
            pauseClosed = closed, daysLogged = days.count { it.logged }, lateNights = lateNights,
            lateKcal = lateKcal, headline = headline, noticed = noticed, focus = focus,
        )
    }

    val milestones: List<Milestone> by lazy { Calc.milestones(s.startWeight, s.heightCm) }

    /** Day the trend first reached each milestone, if it has. */
    fun milestoneReachedOn(m: Milestone): LocalDate? =
        trendSeries.firstOrNull { it.trend <= m.kg }?.let { LocalDate.ofEpochDay(it.day) }
}
