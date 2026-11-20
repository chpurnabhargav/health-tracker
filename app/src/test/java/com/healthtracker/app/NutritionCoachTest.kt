package com.healthtracker.app

import com.healthtracker.app.coach.CoachContext
import com.healthtracker.app.coach.CoachError
import com.healthtracker.app.coach.Gemini
import com.healthtracker.app.coach.Turn
import com.healthtracker.app.data.FoodCatalog
import com.healthtracker.app.data.FoodEntry
import com.healthtracker.app.domain.DayNutrition
import com.healthtracker.app.domain.FoodParser
import com.healthtracker.app.domain.Meal
import com.healthtracker.app.domain.MealIdeas
import com.healthtracker.app.domain.Scoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NutritionCoachTest {

    @Test
    fun foodListIsBigUniqueAndComplete() {
        assertTrue("only ${FoodCatalog.all.size} foods", FoodCatalog.all.size >= 230)
        assertEquals(FoodCatalog.all.size, FoodCatalog.all.map { it.name.lowercase() }.distinct().size)
        assertTrue(FoodCatalog.byName("Dal tadka")!!.oilTsp > 0)
        assertEquals(0.0, FoodCatalog.byName("Green salad")!!.oilTsp, 0.0)
        assertTrue(FoodCatalog.byName("Gulab jamun")!!.sugary)
        assertEquals(1.0, FoodCatalog.byName("Apple")!!.vegServings, 0.0)
        // Macros should roughly add up to the calories (within 25%), so the data isn't nonsense.
        FoodCatalog.all.filter { it.kcal >= 60 && it.category != "Drinks" }.forEach {
            val fromMacros = it.protein * 4 + it.carbs * 4 + it.fat * 9
            assertTrue("${it.name}: ${it.kcal} vs $fromMacros", fromMacros in it.kcal * 0.7..it.kcal * 1.3)
        }
    }

    @Test
    fun typedMealIsSplitIntoItemsAndAmounts() {
        val items = FoodParser.parse("2 roti, dal and half plate rice")
        assertEquals(3, items.size)
        assertEquals("Roti / chapati", items[0].name); assertEquals(2.0, items[0].servings, 0.0)
        assertEquals("Dal tadka", items[1].name); assertEquals(1.0, items[1].servings, 0.0)
        assertEquals("White rice", items[2].name); assertEquals(0.5, items[2].servings, 0.0)

        val more = FoodParser.parse("3 idli with sambar + 1 cup chai")
        assertEquals(listOf("Idli", "Sambar", "Chai with sugar"), more.map { it.name })
        assertEquals(3.0, more[0].servings, 0.0)

        val two = FoodParser.parse("two eggs, black coffee")
        assertEquals("Boiled egg", two[0].name); assertEquals(2.0, two[0].servings, 0.0)
        assertEquals("Black coffee", two[1].name)

        val unknown = FoodParser.parse("dragon fruit smoothie")
        assertFalse(unknown[0].matched)
    }

    private fun entry(name: String, servings: Double = 1.0): FoodEntry {
        val f = FoodCatalog.byName(name)!!
        return FoodEntry(day = 0, time = 0, name = f.name, servings = servings, kcal = (f.kcal * servings).toInt(),
            protein = f.protein * servings, carbs = f.carbs * servings, fat = f.fat * servings, junk = f.junk)
    }

    @Test
    fun scoreRewardsProteinVegAndPunishesJunk() {
        val good = DayNutrition.of(listOf(
            entry("Omelette"), entry("Bread, brown"), entry("Grilled chicken breast"), entry("Roti / chapati", 2.0),
            entry("Mixed veg sabzi"), entry("Green salad"), entry("Dal tadka"), entry("Apple"), entry("Guava"), entry("Curd / dahi"),
        ))
        val bad = DayNutrition.of(listOf(entry("Samosa"), entry("Chai with sugar", 3.0), entry("Gulab jamun", 2.0), entry("Pizza"), entry("Cola / soft drink")))
        val g = Scoring.score(good, 2000, 140, dayDone = true)
        val b = Scoring.score(bad, 2000, 140, dayDone = true)
        assertTrue("good ${g.total}", g.total >= 70)
        assertTrue("bad ${b.total}", b.total <= 35)
        assertTrue(b.fixes.any { it.contains("protein") })
        assertEquals(100, g.parts.sumOf { it.max })
    }

    @Test
    fun mealIdeasFitTheBudget() {
        val ideas = MealIdeas.suggest(500, 60.0, Meal.DINNER)
        assertEquals(3, ideas.size)
        ideas.forEach { assertTrue("${it.title} ${it.kcal}", it.kcal <= 560) }
        MealIdeas.all.forEach { idea -> assertEquals("${idea.id} has unknown foods", idea.parts.size, idea.items().size) }
        assertEquals(Meal.BREAKFAST, Meal.ofHour(8))
        assertEquals(Meal.DINNER, Meal.ofHour(22))
        assertEquals(Meal.DINNER, Meal.ofHour(2))
    }

    @Test
    fun coachOnlyTalksAboutHealthAndTheApp() {
        assertTrue(CoachContext.onTopic("What should I eat for dinner?", false))
        assertTrue(CoachContext.onTopic("Am I eating too many carbs", false))
        assertTrue(CoachContext.onTopic("why is my weight stuck", false))
        assertTrue(CoachContext.onTopic("how am I doing this week", false))
        assertFalse(CoachContext.onTopic("Write a python script to sort a list", false))
        assertFalse(CoachContext.onTopic("Who won the cricket match yesterday evening", false))
        assertFalse(CoachContext.onTopic("Explain quantum physics to me", false))
        assertTrue(CoachContext.onTopic("why?", true))
        assertNotNull(CoachContext.localReply("Hi", "Bhargav"))
        assertNull(CoachContext.localReply("how much protein today", "Bhargav"))
    }

    @Test
    fun geminiRequestAndResponse() {
        val req = Gemini.buildRequest("system rules", listOf(Turn("user", "hi"), Turn("model", "hello"), Turn("user", "carbs?")), 900, noThinking = true)
        assertEquals("system rules", req.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text"))
        val contents = req.getJSONArray("contents")
        assertEquals(3, contents.length())
        assertEquals("model", contents.getJSONObject(1).getString("role"))
        assertEquals(900, req.getJSONObject("generationConfig").getInt("maxOutputTokens"))
        assertEquals(0, req.getJSONObject("generationConfig").getJSONObject("thinkingConfig").getInt("thinkingBudget"))

        val body = """{"candidates":[{"content":{"parts":[{"text":"thinking...","thought":true},{"text":"Bhargav, add curd."}],"role":"model"},"finishReason":"STOP"}]}"""
        assertEquals("Bhargav, add curd.", Gemini.parseText(body))

        val cut = """{"candidates":[{"content":{"parts":[]},"finishReason":"MAX_TOKENS"}]}"""
        try { Gemini.parseText(cut); fail("expected an error") } catch (e: CoachError) { assertTrue(e.userMessage.contains("cut off")) }

        assertTrue(Gemini.errorFor(429, "{}").userMessage.contains("limit"))
        assertTrue(Gemini.errorFor(400, """{"error":{"message":"API key not valid. Please pass a valid API key."}}""").userMessage.contains("isn't valid"))
        assertEquals(404, Gemini.errorFor(404, "{}").httpCode)
    }
}
