package com.healthtracker.app

import com.healthtracker.app.data.ActivityLevel
import com.healthtracker.app.data.Pace
import com.healthtracker.app.data.Sex
import com.healthtracker.app.data.UserSettings
import com.healthtracker.app.data.WeightEntry
import com.healthtracker.app.domain.Calc
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.reminders.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class CalcTest {
    private val today = LocalDate.of(2026, 11, 19)

    private fun settings(kg: Double = 120.0, cm: Double = 188.0, sex: Sex = Sex.MALE, age: Int = 30) = UserSettings(
        onboarded = true, birthDay = today.minusYears(age.toLong()).minusDays(10).toEpochDay(), sex = sex,
        heightCm = cm, startWeight = kg, calcWeight = kg, activity = ActivityLevel.SEDENTARY, pace = Pace.STEADY,
    )

    @Test
    fun bmrMatchesMifflinStJeor() {
        assertEquals(2230.0, Calc.bmr(Sex.MALE, 120.0, 188.0, 30), 0.01)
        assertEquals(1420.25, Calc.bmr(Sex.FEMALE, 70.0, 165.0, 30), 0.01)
    }

    @Test
    fun calorieTargetIsMaintenanceMinusPaceDeficit() {
        val s = settings()
        assertEquals(2676, Calc.maintenance(s, today))
        assertEquals(825, Calc.dailyDeficit(Pace.STEADY))
        assertEquals(1850, Calc.computedTarget(s, today))
        assertEquals(2000, Calc.calorieTarget(s.copy(calorieOverride = 2000), today))
    }

    @Test
    fun calorieTargetNeverGoesBelowTheSafeFloor() {
        val small = settings(kg = 55.0, cm = 155.0, sex = Sex.FEMALE).copy(pace = Pace.FAST)
        assertEquals(1200, Calc.computedTarget(small, today))
    }

    @Test
    fun bmiAndHealthyWeights() {
        assertEquals(33.95, Calc.bmi(120.0, 188.0), 0.01)
        assertEquals(88.36, Calc.weightForBmi(25.0, 188.0), 0.01)
        assertEquals(140, Calc.proteinTarget(188.0))
        assertEquals(3500, Calc.defaultWaterMl(120.0))
        assertEquals(4000, Calc.defaultWaterMl(140.0))
        assertEquals(2500, Calc.defaultWaterMl(60.0))
    }

    @Test
    fun trendIsTheSevenDayAverage() {
        val base = today.toEpochDay()
        val w = (0..9).map { WeightEntry(base - 9 + it, 100.0 - it * 0.2, 0) }
        val series = Calc.trendSeries(w)
        assertEquals(100.0, series.first().trend, 0.001)
        // Last 7 readings: 99.4 .. 98.2, average 98.8
        assertEquals(98.8, series.last().trend, 0.001)
        assertEquals(98.8, Calc.trendOn(w, base)!!, 0.001)
    }

    @Test
    fun trendFallsBackToTheLastKnownWhenThereIsAGap() {
        val base = today.toEpochDay()
        val w = listOf(WeightEntry(base - 20, 101.0, 0), WeightEntry(base - 19, 100.0, 0))
        assertEquals(100.5, Calc.trendOn(w, base)!!, 0.001)
        assertNull(Calc.trendOn(emptyList(), base))
    }

    @Test
    fun weeklyRateNeedsAWeekOfData() {
        val base = today.toEpochDay()
        assertNull(Calc.weeklyRate(listOf(WeightEntry(base - 2, 100.0, 0), WeightEntry(base, 99.8, 0)), today))
        val w = (0..21).map { WeightEntry(base - 21 + it, 120.0 - it * 0.1, 0) }
        val rate = Calc.weeklyRate(w, today)!!
        assertEquals(0.6, rate, 0.001)
    }

    @Test
    fun nextBirthdayHandlesPastFutureTodayAndLeapDays() {
        val birth = LocalDate.of(1998, 1, 5).toEpochDay()
        assertEquals(LocalDate.of(2027, 1, 5), Calc.nextBirthday(birth, today))
        val later = LocalDate.of(1998, 12, 25).toEpochDay()
        assertEquals(LocalDate.of(2026, 12, 25), Calc.nextBirthday(later, today))
        val sameDay = LocalDate.of(1998, 11, 19).toEpochDay()
        assertEquals(today, Calc.nextBirthday(sameDay, today))
        val leap = LocalDate.of(2000, 2, 29).toEpochDay()
        assertEquals(LocalDate.of(2027, 2, 28), Calc.nextBirthday(leap, today))
    }

    @Test
    fun suggestedGoalIsRealisticAndBelowStart() {
        val g = Calc.suggestGoalKg(120.0, Pace.STEADY, 47, 188.0)
        assertTrue("goal was $g", g in 115.0..117.0)
        val far = Calc.suggestGoalKg(80.0, Pace.FAST, 2000, 175.0)
        assertEquals(Calc.roundHalf(Calc.weightForBmi(22.0, 175.0)), far, 0.01)
    }

    @Test
    fun milestonesAreOrderedAndNotTooClose() {
        val m = Calc.milestones(120.0, 188.0)
        assertEquals("First 2 kg", m.first().label)
        for (i in 1 until m.size) assertTrue(m[i - 1].kg - m[i].kg >= 1.0)
        assertTrue(m.any { it.label == "BMI under 30" })
    }

    @Test
    fun nextGoalPicksAMilestoneAtLeastThreeKgAway() {
        val g = Calc.nextGoal(112.0, 120.0, 188.0, Pace.STEADY, today)
        assertEquals("10% lighter", g.label())
        assertTrue(g.day > today.toEpochDay() + 27)
    }

    private fun com.healthtracker.app.domain.NextGoal.label() = name

    @Test
    fun formatting() {
        assertEquals("−1.2", Fmt.signedKg(-1.24))
        assertEquals("+0.4", Fmt.signedKg(0.4))
        assertEquals("3.5", Fmt.litres(3500))
        assertEquals("2.25", Fmt.litres(2250))
        assertEquals("7:00 AM", Fmt.clock(7 * 60))
        assertEquals("12:30 PM", Fmt.clock(12 * 60 + 30))
        assertEquals(117.6, Fmt.parseNumber("117,6")!!, 0.0001)
    }

    @Test
    fun waterRemindersStayInsideTheWindow() {
        val zone = ZoneId.of("Asia/Kolkata")
        val s = UserSettings(onboarded = true, waterStartMin = 8 * 60, waterEndMin = 22 * 60, waterEveryMin = 90)
        val morning = ZonedDateTime.of(2026, 11, 19, 9, 0, 0, 0, zone)
        val next = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(ReminderScheduler.nextWater(s, morning)), zone)
        assertEquals(9, next.hour); assertEquals(30, next.minute)
        val late = ZonedDateTime.of(2026, 11, 19, 22, 30, 0, 0, zone)
        val tomorrow = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(ReminderScheduler.nextWater(s, late)), zone)
        assertEquals(20, tomorrow.dayOfMonth); assertEquals(9, tomorrow.hour); assertEquals(30, tomorrow.minute)
    }

    @Test
    fun weeklyReviewFiresOnSundayMorning() {
        val zone = ZoneId.of("Asia/Kolkata")
        val thu = ZonedDateTime.of(2026, 11, 19, 12, 0, 0, 0, zone)
        val t = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(ReminderScheduler.nextWeekly(thu)), zone)
        assertEquals(java.time.DayOfWeek.SUNDAY, t.dayOfWeek); assertEquals(22, t.dayOfMonth); assertEquals(9, t.hour)
    }
}
