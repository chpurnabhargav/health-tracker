package com.healthtracker.app.domain

import com.healthtracker.app.data.CatalogFood
import com.healthtracker.app.data.FoodCatalog
import com.healthtracker.app.data.FoodEntry
import com.healthtracker.app.data.MyFood
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

enum class Meal(val label: String, val hour: Int, val minute: Int) {
    BREAKFAST("Breakfast", 8, 30),
    LUNCH("Lunch", 13, 30),
    SNACKS("Snacks", 17, 30),
    DINNER("Dinner", 20, 30);

    companion object {
        fun ofHour(h: Int): Meal = when (h) {
            in 4..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 16..18 -> SNACKS
            else -> DINNER
        }

        fun of(time: Long, zone: ZoneId = ZoneId.systemDefault()): Meal = ofHour(Instant.ofEpochMilli(time).atZone(zone).hour)

        fun now(): Meal = ofHour(java.time.LocalTime.now().hour)
    }

    /** Timestamp for logging into this meal on [day]: the real time if it's this meal now, else a typical time. */
    fun timeOn(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
        val nowT = java.time.ZonedDateTime.now(zone)
        return if (day == nowT.toLocalDate() && ofHour(nowT.hour) == this) nowT.toInstant().toEpochMilli()
        else day.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    }
}

/** Fibre, vegetables and sugar aren't stored per entry, so they're looked up from the food list by name. */
object Facts {
    fun catalogFor(e: FoodEntry): CatalogFood? = FoodCatalog.byName(e.name)

    fun fibre(e: FoodEntry): Double = (catalogFor(e)?.fibre ?: 0.0) * e.servings
    fun veg(e: FoodEntry): Double = (catalogFor(e)?.vegServings ?: 0.0) * e.servings
    fun sugary(e: FoodEntry): Boolean = catalogFor(e)?.sugary ?: false
}

data class DayNutrition(
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fibre: Double,
    val vegServings: Double,
    val junkItems: Int,
    val sugaryItems: Int,
    val items: Int,
) {
    val carbShare: Double get() = if (kcal <= 0) 0.0 else carbs * 4 / kcal
    val fatShare: Double get() = if (kcal <= 0) 0.0 else fat * 9 / kcal
    val proteinShare: Double get() = if (kcal <= 0) 0.0 else protein * 4 / kcal

    companion object {
        fun of(entries: List<FoodEntry>) = DayNutrition(
            kcal = entries.sumOf { it.kcal },
            protein = entries.sumOf { it.protein },
            carbs = entries.sumOf { it.carbs },
            fat = entries.sumOf { it.fat },
            fibre = entries.sumOf { Facts.fibre(it) },
            vegServings = entries.sumOf { Facts.veg(it) },
            junkItems = entries.count { it.junk },
            sugaryItems = entries.count { Facts.sugary(it) },
            items = entries.size,
        )
    }
}

data class ScorePart(val label: String, val points: Int, val max: Int, val detail: String)

data class NutritionScore(val total: Int, val parts: List<ScorePart>, val fixes: List<String>)

object Scoring {
    const val FIBRE_TARGET = 30.0
    const val VEG_TARGET = 5.0

    /**
     * 0 to 100 for the day: calories in range (25), protein (25), fibre (15), vegetables and fruit (15),
     * junk (10) and sugar (10). [dayDone] = false means the day is still going, so being under is fine.
     */
    fun score(n: DayNutrition, kcalTarget: Int, proteinTarget: Int, dayDone: Boolean, name: String = "you"): NutritionScore {
        val kcalRatio = n.kcal / kcalTarget.toDouble()
        val kcalPts = when {
            n.items == 0 -> 0
            kcalRatio > 1.15 -> (25 * (1 - (kcalRatio - 1.15) * 3)).coerceIn(0.0, 25.0).roundToInt().coerceAtMost(12)
            kcalRatio > 1.05 -> 18
            !dayDone || kcalRatio >= 0.8 -> 25
            else -> (25 * kcalRatio / 0.8).roundToInt()
        }
        val proteinPts = (25 * (n.protein / proteinTarget)).coerceIn(0.0, 25.0).roundToInt()
        val fibrePts = (15 * (n.fibre / FIBRE_TARGET)).coerceIn(0.0, 15.0).roundToInt()
        val vegPts = (15 * (n.vegServings / VEG_TARGET)).coerceIn(0.0, 15.0).roundToInt()
        val junkPts = if (n.items == 0) 0 else (10 - 5 * n.junkItems).coerceAtLeast(0)
        val sugarPts = if (n.items == 0) 0 else (10 - 4 * n.sugaryItems).coerceAtLeast(0)
        val parts = listOf(
            ScorePart("Calories", kcalPts, 25, "${Fmt.int(n.kcal)} of ${Fmt.int(kcalTarget)} kcal"),
            ScorePart("Protein", proteinPts, 25, "${n.protein.roundToInt()} of $proteinTarget g"),
            ScorePart("Fibre", fibrePts, 15, "${n.fibre.roundToInt()} of ${FIBRE_TARGET.toInt()} g"),
            ScorePart("Veg & fruit", vegPts, 15, "${Fmt.one(n.vegServings).removeSuffix(".0")} of ${VEG_TARGET.toInt()} servings"),
            ScorePart("Junk", junkPts, 10, if (n.junkItems == 0) "None" else "${n.junkItems} item${if (n.junkItems > 1) "s" else ""}"),
            ScorePart("Sugar", sugarPts, 10, if (n.sugaryItems == 0) "None" else "${n.sugaryItems} sugary item${if (n.sugaryItems > 1) "s" else ""}"),
        )
        val fixes = mutableListOf<String>()
        val proteinGap = proteinTarget - n.protein
        if (proteinGap > 15) fixes += "You're ${proteinGap.roundToInt()} g short on protein. Add eggs, chicken, paneer, dal or curd to your next meal."
        if (n.items > 0 && n.carbShare > 0.6) fixes += "${Fmt.pct(n.carbShare)} of your calories are from carbs. Swap one roti or some rice for protein or sabzi."
        if (n.vegServings < 3 && n.items > 0) fixes += if (n.vegServings < 0.5) "No vegetables or fruit yet. Add a salad, sabzi or a fruit."
            else "Only ${Fmt.one(n.vegServings).removeSuffix(".0")} servings of vegetables or fruit. Add a salad, sabzi or a fruit."
        if (n.fibre < 15 && n.items > 1) fixes += "Fibre is low. Dal, chana, sprouts, guava or whole-wheat roti help you stay full."
        if (n.sugaryItems >= 2) fixes += "${n.sugaryItems} sugary items today. Try your next chai without sugar."
        if (n.junkItems >= 2) fixes += "${n.junkItems} junk items today. Keep the rest of the day clean."
        if (kcalRatio > 1.05) fixes += "You're over today's calories. Keep dinner light: dal, salad, curd or grilled protein."
        val total = parts.sumOf { it.points }
        return NutritionScore(total, parts, fixes.take(3))
    }
}

/** Instant feedback after a meal is logged, without the internet. */
object MealFeedback {
    fun forMeal(entries: List<FoodEntry>, name: String, proteinTarget: Int): String? {
        if (entries.isEmpty()) return null
        val n = DayNutrition.of(entries)
        val who = name.ifBlank { "Hey" }
        return when {
            n.kcal >= 900 -> "$who, that meal was ${Fmt.int(n.kcal)} kcal. Keep the next one light."
            n.carbShare > 0.65 && n.protein < 15 -> "$who, that was mostly carbs (${Fmt.pct(n.carbShare)}) with only ${n.protein.roundToInt()} g protein. Add curd, eggs or dal next time."
            n.fatShare > 0.5 -> "$who, that meal was heavy on oil or fat (${Fmt.pct(n.fatShare)} of calories). Ask for less oil next time."
            n.protein >= proteinTarget / 4.0 -> "Nice, ${n.protein.roundToInt()} g protein in that meal."
            n.vegServings >= 1.5 -> "Good, plenty of vegetables in that meal."
            else -> null
        }
    }
}

/** One item found in a typed sentence like "2 roti, dal and half plate rice". */
data class ParsedItem(
    val text: String,
    val servings: Double,
    val name: String?,
    val serving: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val junk: Boolean,
    val estimated: Boolean = false,
) {
    val matched: Boolean get() = name != null
}

object FoodParser {
    private val numberWords = mapOf(
        "a" to 1.0, "an" to 1.0, "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
        "six" to 6.0, "half" to 0.5, "quarter" to 0.25, "double" to 2.0, "ek" to 1.0, "do" to 2.0, "teen" to 3.0,
    )

    /** Common ways of saying a food, mapped to its name in the food list. */
    private val synonyms = linkedMapOf(
        "chapati" to "Roti / chapati", "chapathi" to "Roti / chapati", "roti" to "Roti / chapati", "phulka" to "Phulka",
        "paratha" to "Plain paratha", "aloo paratha" to "Aloo paratha", "naan" to "Naan",
        "brown rice" to "Brown rice", "jeera rice" to "Jeera rice", "curd rice" to "Curd rice", "rice" to "White rice", "chawal" to "White rice",
        "dal makhani" to "Dal makhani", "dal fry" to "Dal fry", "dal" to "Dal tadka", "daal" to "Dal tadka",
        "rajma chawal" to "Rajma chawal", "rajma" to "Rajma", "chole" to "Chole / chana masala", "chana" to "Chole / chana masala",
        "sambar" to "Sambar", "rasam" to "Rasam", "sabzi" to "Mixed veg sabzi", "sabji" to "Mixed veg sabzi",
        "salad" to "Green salad", "raita" to "Raita", "curd" to "Curd / dahi", "dahi" to "Curd / dahi", "yogurt" to "Curd / dahi",
        "boiled egg" to "Boiled egg", "eggs" to "Boiled egg", "egg" to "Boiled egg", "omelette" to "Omelette", "omelet" to "Omelette",
        "egg bhurji" to "Egg bhurji", "chicken biryani" to "Chicken biryani", "biryani" to "Chicken biryani",
        "chicken curry" to "Chicken curry", "butter chicken" to "Butter chicken", "chicken tikka" to "Chicken tikka",
        "grilled chicken" to "Grilled chicken breast", "chicken breast" to "Grilled chicken breast", "chicken" to "Chicken curry",
        "paneer tikka" to "Paneer tikka", "paneer butter masala" to "Paneer butter masala", "palak paneer" to "Palak paneer", "paneer" to "Paneer",
        "masala dosa" to "Masala dosa", "dosa" to "Plain dosa", "idli" to "Idli", "vada" to "Medu vada", "upma" to "Upma", "poha" to "Poha",
        "chai" to "Chai with sugar", "tea" to "Chai with sugar", "coffee" to "Coffee with milk and sugar", "black coffee" to "Black coffee",
        "green tea" to "Green tea", "milk" to "Milk, toned", "buttermilk" to "Buttermilk / chaas", "chaas" to "Buttermilk / chaas", "lassi" to "Sweet lassi",
        "banana" to "Banana", "apple" to "Apple", "mango" to "Mango", "orange" to "Orange", "papaya" to "Papaya", "guava" to "Guava",
        "samosa" to "Samosa", "pakora" to "Pakora / bhajji", "maggi" to "Instant noodles", "noodles" to "Instant noodles",
        "pizza" to "Pizza", "burger" to "Burger", "fries" to "French fries", "momos" to "Steamed momos", "sandwich" to "Veg sandwich",
        "biscuit" to "Marie biscuits", "biscuits" to "Marie biscuits", "gulab jamun" to "Gulab jamun", "ice cream" to "Ice cream", "sweet" to "Ladoo",
        "oats" to "Oats with milk", "sprouts" to "Sprouts salad", "soya" to "Soya chunks, cooked", "whey" to "Whey protein", "protein shake" to "Protein shake with milk",
        "almonds" to "Almonds", "peanuts" to "Roasted peanuts", "khichdi" to "Khichdi", "pulao" to "Veg pulao", "dhokla" to "Dhokla",
    )

    private val portionWords = mapOf("half plate" to 0.5, "full plate" to 1.0, "small" to 0.75, "large" to 1.5, "big" to 1.5)

    fun splitItems(text: String): List<String> =
        text.lowercase()
            .replace(Regex("\\s+(and|with|aur|plus)\\s+"), ",")
            .split(',', '+', '&', ';', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** Finds each item and its amount. Unknown items are returned unmatched so the person (or Coach) can fill them in. */
    fun parse(text: String, myFoods: List<MyFood> = emptyList()): List<ParsedItem> = splitItems(text).map { raw ->
        var s = raw
        var qty = 1.0
        portionWords.forEach { (w, m) -> if (s.contains(w)) { qty *= m; s = s.replace(w, " ") } }
        val num = Regex("^(\\d+(?:[.,]\\d+)?|½)\\s*(?:x|×)?\\s*").find(s.trim())
        if (num != null) {
            qty *= if (num.groupValues[1] == "½") 0.5 else num.groupValues[1].replace(',', '.').toDouble()
            s = s.trim().removeRange(num.range)
        } else {
            val first = s.trim().substringBefore(' ')
            numberWords[first]?.let { qty *= it; s = s.trim().removePrefix(first) }
        }
        s = s.replace(Regex("\\b(cups?|bowls?|plates?|pieces?|pcs|glass(es)?|katori|of)\\b"), " ").replace(Regex("\\s+"), " ").trim()

        val mine = myFoods.firstOrNull { it.name.lowercase() == s } ?: myFoods.firstOrNull { s.length >= 4 && it.name.lowercase().contains(s) }
        if (mine != null) {
            return@map ParsedItem(raw, qty, mine.name, mine.serving, mine.kcal, mine.protein, mine.carbs, mine.fat, mine.junk)
        }
        val exact = FoodCatalog.byName(s)
        val syn = synonyms.entries.firstOrNull { (k, _) -> Regex("\\b${Regex.escape(k)}\\b").containsMatchIn(s) }?.value?.let { FoodCatalog.byName(it) }
        val loose = if (s.length >= 4) FoodCatalog.all.firstOrNull { it.name.lowercase().contains(s) } else null
        val food = exact ?: syn ?: loose
        if (food != null) ParsedItem(raw, qty, food.name, food.serving, food.kcal, food.protein, food.carbs, food.fat, food.junk)
        else ParsedItem(raw, qty, null, "1 serving", 0, 0.0, 0.0, 0.0, false)
    }
}

/** A ready meal idea built from foods in the list. */
data class MealIdea(val id: String, val title: String, val parts: List<Pair<String, Double>>, val meals: Set<Meal>) {
    private val foods get() = parts.mapNotNull { (n, q) -> FoodCatalog.byName(n)?.let { it to q } }
    val kcal: Int get() = foods.sumOf { (f, q) -> f.kcal * q }.roundToInt()
    val protein: Double get() = foods.sumOf { (f, q) -> f.protein * q }
    val fibre: Double get() = foods.sumOf { (f, q) -> f.fibre * q }
    fun items(): List<Pair<CatalogFood, Double>> = foods
}

object MealIdeas {
    private val B = Meal.BREAKFAST
    private val L = Meal.LUNCH
    private val S = Meal.SNACKS
    private val D = Meal.DINNER

    val all = listOf(
        MealIdea("eggs_toast", "2-egg omelette, brown toast and curd", listOf("Omelette" to 1.0, "Bread, brown" to 1.0, "Curd / dahi" to 1.0), setOf(B)),
        MealIdea("chilla_curd", "2 moong dal chillas with curd", listOf("Moong dal chilla" to 2.0, "Curd / dahi" to 1.0), setOf(B, D)),
        MealIdea("oats_milk", "Oats with milk and a banana", listOf("Oats with milk" to 1.0, "Banana" to 1.0), setOf(B)),
        MealIdea("poha_eggs", "Poha with 2 boiled eggs", listOf("Poha" to 1.0, "Boiled egg" to 2.0), setOf(B)),
        MealIdea("idli_sambar", "3 idlis with sambar", listOf("Idli" to 3.0, "Sambar" to 1.0), setOf(B, D)),
        MealIdea("paneer_paratha", "Paneer paratha with curd", listOf("Paneer paratha" to 1.0, "Curd / dahi" to 1.0), setOf(B)),
        MealIdea("greek_fruit", "Greek yogurt with apple and almonds", listOf("Hung curd / Greek yogurt" to 1.0, "Apple" to 1.0, "Almonds" to 1.0), setOf(B, S)),
        MealIdea("dal_roti_sabzi", "Dal, 2 rotis and sabzi", listOf("Dal tadka" to 1.0, "Roti / chapati" to 2.0, "Mixed veg sabzi" to 1.0), setOf(L, D)),
        MealIdea("chicken_roti_salad", "Grilled chicken, 1 roti and salad", listOf("Grilled chicken breast" to 1.0, "Roti / chapati" to 1.0, "Green salad" to 1.0), setOf(L, D)),
        MealIdea("chicken_curry_rice", "Chicken curry, 1 cup rice and salad", listOf("Chicken curry" to 1.0, "White rice" to 1.0, "Green salad" to 1.0), setOf(L, D)),
        MealIdea("rajma_rice_salad", "Rajma, half cup rice and salad", listOf("Rajma" to 1.0, "White rice" to 0.5, "Green salad" to 1.0), setOf(L)),
        MealIdea("paneer_tikka_salad", "Paneer tikka, 1 roti and salad", listOf("Paneer tikka" to 1.0, "Roti / chapati" to 1.0, "Green salad" to 1.0), setOf(L, D)),
        MealIdea("fish_rice", "Fish curry, 1 cup rice and salad", listOf("Fish curry" to 1.0, "White rice" to 1.0, "Kachumber salad" to 1.0), setOf(L, D)),
        MealIdea("egg_curry_roti", "Egg curry with 2 rotis", listOf("Egg curry" to 1.0, "Roti / chapati" to 2.0), setOf(L, D)),
        MealIdea("soya_roti", "Soya chunk curry, 2 rotis and salad", listOf("Soya chunk curry" to 1.0, "Roti / chapati" to 2.0, "Green salad" to 1.0), setOf(L, D)),
        MealIdea("chole_roti", "Chole, 1 roti and raita", listOf("Chole / chana masala" to 1.0, "Roti / chapati" to 1.0, "Raita" to 1.0), setOf(L)),
        MealIdea("khichdi_curd", "Khichdi with curd and salad", listOf("Khichdi" to 1.0, "Curd / dahi" to 1.0, "Green salad" to 1.0), setOf(L, D)),
        MealIdea("tikka_soup", "Chicken tikka and vegetable soup", listOf("Chicken tikka" to 1.0, "Vegetable soup" to 1.0), setOf(D)),
        MealIdea("dosa_sambar", "Plain dosa with sambar", listOf("Plain dosa" to 1.0, "Sambar" to 1.0), setOf(B, L, D)),
        MealIdea("sprouts_chaas", "Sprouts salad and buttermilk", listOf("Sprouts salad" to 1.0, "Buttermilk / chaas" to 1.0), setOf(S, D)),
        MealIdea("chana_fruit", "Roasted chana and a guava", listOf("Roasted chana" to 1.0, "Guava" to 1.0), setOf(S)),
        MealIdea("egg_snack", "2 boiled eggs and a fruit", listOf("Boiled egg" to 2.0, "Apple" to 1.0), setOf(S)),
        MealIdea("makhana_chai", "Roasted makhana with unsweetened chai", listOf("Makhana, roasted" to 1.0, "Chai without sugar" to 1.0), setOf(S)),
        MealIdea("shake", "Protein shake and a banana", listOf("Protein shake with milk" to 1.0, "Banana" to 1.0), setOf(B, S)),
        MealIdea("light_dinner", "Moong dal, salad and curd", listOf("Moong dal" to 1.0, "Green salad" to 1.0, "Curd / dahi" to 1.0), setOf(D)),
    )

    fun byId(id: String): MealIdea? = all.find { it.id == id }

    /**
     * Ideas that fit the calories left, ranked by protein for the calories (and fibre),
     * preferring ones that suit the meal.
     */
    fun suggest(kcalLeft: Int, proteinLeft: Double, meal: Meal, count: Int = 3): List<MealIdea> {
        val budget = kcalLeft.coerceAtLeast(150)
        val fitting = all.filter { it.kcal <= budget + 60 }
        val forMeal = fitting.filter { meal in it.meals }
        return (if (forMeal.size >= count) forMeal else fitting)
            .sortedByDescending { idea ->
                val fitsMeal = if (meal in idea.meals) 2.0 else 0.0
                val proteinValue = (minOf(idea.protein, proteinLeft.coerceAtLeast(10.0)) / idea.kcal.coerceAtLeast(1)) * 40
                val fibre = idea.fibre / 10
                val useBudget = 1.0 - kotlin.math.abs(budget * 0.7 - idea.kcal) / budget
                fitsMeal + proteinValue + fibre + useBudget
            }
            .take(count)
    }
}
