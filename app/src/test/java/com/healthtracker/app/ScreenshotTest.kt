package com.healthtracker.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.healthtracker.app.data.ActivityLevel
import com.healthtracker.app.data.DailyActivity
import com.healthtracker.app.data.FoodEntry
import com.healthtracker.app.data.MyFood
import com.healthtracker.app.data.Pace
import com.healthtracker.app.data.PauseEvent
import com.healthtracker.app.data.Sex
import com.healthtracker.app.data.WaistEntry
import com.healthtracker.app.data.WaterEntry
import com.healthtracker.app.data.WeightEntry
import com.healthtracker.app.domain.Calc
import com.healthtracker.app.pause.PauseContent
import com.healthtracker.app.pause.PauseInfo
import com.healthtracker.app.ui.HealthViewModel
import com.healthtracker.app.ui.screens.AddFoodScreen
import com.healthtracker.app.ui.screens.FoodScreen
import com.healthtracker.app.ui.screens.OnboardingScreen
import com.healthtracker.app.ui.screens.PlanScreen
import com.healthtracker.app.ui.screens.ReviewScreen
import com.healthtracker.app.ui.screens.TodayScreen
import com.healthtracker.app.ui.screens.WeightScreen
import com.healthtracker.app.ui.theme.HealthTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private fun Modifier.fillMaxSizeCompat() = this.fillMaxSize()
private fun Modifier.androidxBackground() = this.background(Color(0xFF101418))

/**
 * Renders every screen with three weeks of sample data and saves a PNG of each to
 * app/build/outputs/roborazzi. Also checks a few of the numbers the screens show.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h2600dp-xhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var app: HealthApp
    private lateinit var vm: HealthViewModel
    private val today = LocalDate.now()

    @Before
    fun seed() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        val dao = app.container.db.dao()
        val zone = ZoneId.systemDefault()
        fun at(d: LocalDate, h: Int, m: Int = 0) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

        dao.clearWeights(); dao.clearWaists(); dao.clearWater(); dao.clearFood()
        dao.clearMyFoods(); dao.clearPauses(); dao.clearActivity(); dao.clearMinimumDays()

        val start = today.minusDays(21)
        val birthday = today.plusDays(47).minusYears(27)
        app.container.settings.update {
            it.copy(
                onboarded = true, name = "Bhargav", birthDay = birthday.toEpochDay(), sex = Sex.MALE,
                heightCm = 180.0, startWeight = 104.0, startDay = start.toEpochDay(), activity = ActivityLevel.SEDENTARY,
                pace = Pace.STEADY, calcWeight = 104.0, goalName = "your birthday", goalKg = 98.0,
                goalDay = Calc.nextBirthday(birthday.toEpochDay(), today).toEpochDay(),
                goalStartDay = start.toEpochDay(), goalStartKg = 104.0, waterGoalMl = 3500, stepGoal = 8000,
                plannedMealsPerWeek = 1,
            )
        }

        val noise = listOf(0.0, 0.4, -0.2, 0.1, -0.3, 0.2, -0.5, 0.0, -0.4, 0.3, -0.2, -0.4, 0.3, -0.3, -0.2, 0.0, -0.1, -0.2, -0.1, -0.3, -0.5, 0.4)
        noise.forEachIndexed { i, n ->
            dao.upsertWeight(WeightEntry(start.plusDays(i.toLong()).toEpochDay(), 104.0 - i * 0.12 + n, at(start.plusDays(i.toLong()), 7, 4)))
        }
        listOf(21L to 112.0, 14L to 111.0, 7L to 110.0).forEach { (ago, cm) -> dao.upsertWaist(WaistEntry(today.minusDays(ago).toEpochDay(), cm)) }

        fun food(d: LocalDate, h: Int, name: String, kcal: Int, p: Double, c: Double, f: Double, junk: Boolean = false) =
            FoodEntry(day = d.toEpochDay(), time = at(d, h), name = name, servings = 1.0, kcal = kcal, protein = p, carbs = c, fat = f, junk = junk)

        for (ago in 7 downTo 1) {
            val d = today.minusDays(ago.toLong())
            val weekend = d.dayOfWeek.value >= 6
            dao.insertFood(food(d, 8, "Poha", 270, 5.0, 45.0, 8.0))
            dao.insertFood(food(d, 11, "Chai with sugar", 80, 2.0, 12.0, 2.5))
            dao.insertFood(food(d, 13, "Dal tadka", 180, 9.0, 24.0, 5.0))
            dao.insertFood(food(d, 13, "Roti / chapati", 200, 6.0, 36.0, 4.0))
            dao.insertFood(food(d, 17, "Chai with sugar", 80, 2.0, 12.0, 2.5))
            if (weekend) {
                dao.insertFood(food(d, 21, "Chicken biryani", 730, 30.0, 85.0, 30.0))
                dao.insertFood(food(d, 22, "Gulab jamun", 300, 4.0, 44.0, 12.0, junk = true))
            } else {
                dao.insertFood(food(d, 20, "Grilled chicken breast", 250, 46.0, 0.0, 6.0))
                dao.insertFood(food(d, 20, "White rice", 210, 4.0, 46.0, 0.5))
                dao.insertFood(food(d, 20, "Mixed veg sabzi", 150, 4.0, 15.0, 9.0))
            }
            dao.insertWater(WaterEntry(day = d.toEpochDay(), time = at(d, 12), ml = if (weekend) 2400 else 3500 + (ago % 3) * 250))
            dao.upsertActivity(listOf(DailyActivity(d.toEpochDay(), 5200 + ago * 450, 380 + ago * 9)))
        }
        dao.insertFood(food(today, 8, "Boiled egg", 156, 12.6, 1.2, 10.6))
        dao.insertFood(food(today, 8, "Chai with sugar", 80, 2.0, 12.0, 2.5))
        dao.insertFood(food(today, 13, "Dal tadka", 180, 9.0, 24.0, 5.0))
        dao.insertFood(food(today, 13, "Roti / chapati", 200, 6.0, 36.0, 4.0))
        dao.insertWater(WaterEntry(day = today.toEpochDay(), time = at(today, 10), ml = 1750))
        dao.upsertActivity(listOf(DailyActivity(today.toEpochDay(), 4300, 412)))
        listOf("Poha" to 270, "Dal tadka" to 180, "Roti / chapati" to 100, "Chai with sugar" to 80, "Grilled chicken breast" to 250)
            .forEachIndexed { i, (n, k) -> dao.upsertMyFood(MyFood(name = n, serving = "1 serving", kcal = k, protein = 6.0, carbs = 20.0, fat = 4.0, junk = false, uses = 10 - i, lastUsed = 0)) }

        listOf(6L to "closed", 5L to "closed", 3L to "continued", 2L to "closed", 1L to "closed").forEach { (ago, o) ->
            val d = today.minusDays(ago)
            dao.insertPause(PauseEvent(time = at(d, 20), day = d.toEpochDay(), pkg = "in.swiggy.android", appName = "Swiggy", outcome = o))
        }

        vm = HealthViewModel(app)
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            HealthTheme {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(Color(0xFF07080D))) {
                    com.healthtracker.app.ui.components.AuroraBackground(listOf(Color(0xFF8E7CFF), Color(0xFF34D3F6), Color(0xFFC8FF4D)))
                    content()
                }
            }
        }
        compose.waitUntil(20_000) { vm.state.value?.weights?.size == 22 }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun numbersAddUp() {
        compose.setContent { }
        compose.waitUntil(20_000) { vm.state.value?.weights?.size == 22 }
        val h = vm.state.value!!
        assertEquals(47L, h.daysLeft)
        assertTrue("trend ${h.trendNow}", h.trendNow!! in 100.5..102.0)
        assertEquals(616, h.todayKcal)
        assertEquals(1750, h.todayWaterMl)
        assertEquals(5, h.review.pauseOpens)
        assertEquals(4, h.review.pauseClosed)
        assertEquals(7, h.review.daysLogged)
        assertTrue(h.review.noticed.any { it.contains("weekend") })
        assertEquals("Plan Saturday and Sunday meals on Friday night.", h.review.focus)
    }

    @Test fun s1_today() = shot("1-today") { TodayScreen(vm, onAddFood = {}, onOpenTab = {}) }
    @Test fun s2_weight() = shot("2-weight") { WeightScreen(vm) }
    @Test fun s3_food() = shot("3-food") { FoodScreen(vm, onAddFood = {}) }
    @Test fun s4_addFood() = shot("4-add-food") { AddFoodScreen(vm, onDone = {}) }
    @Test fun s5_review() = shot("5-review") { ReviewScreen(vm) }
    @Test fun s6_plan() = shot("6-plan") { PlanScreen(vm) }

    @Test
    @Config(qualifiers = "+night")
    fun s7_todayDark() = shot("7-today-dark") { TodayScreen(vm, onAddFood = {}, onOpenTab = {}) }

    @Test
    @Config(qualifiers = "w412dp-h915dp-xhdpi")
    fun s8_pause() {
        val info = PauseInfo(
            headline = "No Bhargav.", sub = "Not till your birthday.", daysLeft = 47,
            goalLine = "Birthday goal 98.0 kg", lostLine = "You've lost 2.6 kg so far.",
            monthLine = "This month you closed this screen 9 of 11 times.", waitSec = 30, plannedLeft = 1,
            mode = com.healthtracker.app.data.PauseMode.STRICT, continuesToday = 1, moneySaved = 3150, kcalLeft = 1004,
        )
        compose.mainClock.autoAdvance = false
        compose.setContent { HealthTheme { PauseContent(info, "Swiggy", false, {}, { _, _ -> }) } }
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/8-pause.png")
        compose.mainClock.advanceTimeBy(31_000)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/9-pause-unlocked.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp-xhdpi")
    fun s11_waterAlert() {
        compose.setContent {
            HealthTheme {
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.fillMaxSizeCompat().androidxBackground(),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
                ) {
                    com.healthtracker.app.reminders.WaterAlertCard(1750, 3500, 250, 8, 12, {}, {}, {},
                        androidx.compose.ui.Modifier.padding(12.dp))
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/11-water-alert.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp-xhdpi")
    fun s10_onboarding() {
        runBlocking { app.container.settings.update { it.copy(onboarded = false) } }
        compose.setContent { HealthTheme { OnboardingScreen(vm) } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/10-onboarding.png")
    }
}
