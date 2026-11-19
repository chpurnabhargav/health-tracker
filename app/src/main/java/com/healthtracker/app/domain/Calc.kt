package com.healthtracker.app.domain

import com.healthtracker.app.data.Pace
import com.healthtracker.app.data.Sex
import com.healthtracker.app.data.UserSettings
import com.healthtracker.app.data.WeightEntry
import java.time.LocalDate
import java.time.Period
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class TrendPoint(val day: Long, val kg: Double, val trend: Double)

data class Milestone(val label: String, val kg: Double)

data class NextGoal(val name: String, val kg: Double, val day: Long)

object Calc {
    const val KCAL_PER_KG = 7700.0

    fun roundTo(x: Double, step: Int): Int = ((x / step).roundToInt() * step)

    fun roundHalf(x: Double): Double = (x * 2).roundToInt() / 2.0

    fun age(birthDay: Long?, today: LocalDate): Int? =
        birthDay?.let { Period.between(LocalDate.ofEpochDay(it), today).years }

    /** Mifflin-St Jeor resting energy, kcal a day. */
    fun bmr(sex: Sex, kg: Double, cm: Double, age: Int): Double =
        10 * kg + 6.25 * cm - 5 * age + if (sex == Sex.MALE) 5.0 else -161.0

    fun maintenance(s: UserSettings, today: LocalDate): Int {
        val a = age(s.birthDay, today) ?: 30
        return (bmr(s.sex, s.calcWeight, s.heightCm, a) * s.activity.factor).roundToInt()
    }

    fun dailyDeficit(pace: Pace): Int = (pace.kgPerWeek * KCAL_PER_KG / 7).roundToInt()

    fun minimumCalories(sex: Sex): Int = if (sex == Sex.MALE) 1500 else 1200

    fun computedTarget(s: UserSettings, today: LocalDate): Int {
        val m = maintenance(s, today)
        return roundTo(max(minimumCalories(s.sex).toDouble(), (m - dailyDeficit(s.pace)).toDouble()), 10)
    }

    fun calorieTarget(s: UserSettings, today: LocalDate): Int = s.calorieOverride ?: computedTarget(s, today)

    fun bmi(kg: Double, cm: Double): Double = kg / ((cm / 100) * (cm / 100))

    fun weightForBmi(bmi: Double, cm: Double): Double = bmi * (cm / 100) * (cm / 100)

    /** 1.6 g per kg of the weight you'd have at a BMI of 25. */
    fun proteinTarget(cm: Double): Int = roundTo(1.6 * weightForBmi(25.0, cm), 5)

    fun defaultWaterMl(kg: Double): Int = roundTo((kg * 30).coerceIn(2500.0, 4000.0), 250)

    /** Each reading with the average of all readings in the 7 days ending that day. */
    fun trendSeries(entries: List<WeightEntry>): List<TrendPoint> {
        val sorted = entries.sortedBy { it.day }
        return sorted.map { e ->
            val window = sorted.filter { it.day in (e.day - 6)..e.day }
            TrendPoint(e.day, e.kg, window.map { it.kg }.average())
        }
    }

    /** 7-day trend weight on [day]. Falls back to the latest trend before it. */
    fun trendOn(entries: List<WeightEntry>, day: Long): Double? {
        val window = entries.filter { it.day in (day - 6)..day }
        if (window.isNotEmpty()) return window.map { it.kg }.average()
        val last = entries.filter { it.day <= day }.maxByOrNull { it.day } ?: return null
        return entries.filter { it.day in (last.day - 6)..last.day }.map { it.kg }.average()
    }

    /** Kg lost per week over up to the last 28 days (positive means losing). Needs a week of data. */
    fun weeklyRate(entries: List<WeightEntry>, today: LocalDate): Double? {
        if (entries.size < 2) return null
        val t = today.toEpochDay()
        val now = trendOn(entries, t) ?: return null
        val first = entries.minOf { it.day }
        val from = max(first, t - 28)
        val span = t - from
        if (span < 7) return null
        val then = trendOn(entries, from) ?: return null
        return (then - now) / (span / 7.0)
    }

    fun projectedWeight(trendNow: Double, ratePerWeek: Double, days: Long): Double =
        trendNow - ratePerWeek * days / 7.0

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate =
        if (month == 2 && day == 29 && !java.time.Year.isLeap(year.toLong())) LocalDate.of(year, 2, 28)
        else LocalDate.of(year, month, day)

    /** The next birthday on or after [today]. */
    fun nextBirthday(birthDay: Long, today: LocalDate): LocalDate {
        val b = LocalDate.ofEpochDay(birthDay)
        val thisYear = safeDate(today.year, b.monthValue, b.dayOfMonth)
        return if (thisYear.isBefore(today)) safeDate(today.year + 1, b.monthValue, b.dayOfMonth) else thisYear
    }

    /** A realistic goal weight for a date [days] away, allowing for weight loss slowing down. */
    fun suggestGoalKg(startKg: Double, pace: Pace, days: Long, cm: Double): Double {
        val raw = startKg - pace.kgPerWeek * (days / 7.0) * 0.85
        val floor = weightForBmi(22.0, cm)
        return roundHalf(max(raw, floor)).coerceAtMost(startKg - 0.5)
    }

    fun milestones(startKg: Double, cm: Double): List<Milestone> = listOf(
        Milestone("First 2 kg", startKg - 2),
        Milestone("First 5 kg", startKg - 5),
        Milestone("10% lighter", startKg * 0.90),
        Milestone("BMI under 30", weightForBmi(29.9, cm)),
        Milestone("15% lighter", startKg * 0.85),
        Milestone("BMI under 27.5", weightForBmi(27.4, cm)),
        Milestone("BMI under 25", weightForBmi(24.9, cm)),
    ).filter { it.kg < startKg }
        .map { it.copy(kg = (it.kg * 10).roundToInt() / 10.0) }
        .sortedByDescending { it.kg }
        .fold(mutableListOf<Milestone>()) { acc, m ->
            if (acc.none { kotlin.math.abs(it.kg - m.kg) < 1.0 }) acc.add(m)
            acc
        }

    /** The goal that starts automatically once the current goal's date has passed. */
    fun nextGoal(trendKg: Double, startKg: Double, cm: Double, pace: Pace, today: LocalDate): NextGoal {
        val ahead = milestones(startKg, cm).filter { it.kg < trendKg - 1.0 }
        val m = ahead.firstOrNull { it.kg <= trendKg - 3.0 } ?: ahead.firstOrNull()
            ?: Milestone("Stay at a healthy weight", roundHalf(trendKg))
        val weeks = ceil((trendKg - m.kg) / (pace.kgPerWeek * 0.85)).toLong().coerceAtLeast(4)
        return NextGoal(m.label, m.kg, today.toEpochDay() + weeks * 7)
    }
}
