package com.healthtracker.app.data

/**
 * A food you can pick from the built-in list. Values are per serving and are typical estimates.
 * [oilTsp] is the cooking oil already included, so portions cooked with less or more oil can be adjusted.
 */
data class CatalogFood(
    val name: String,
    val serving: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val junk: Boolean = false,
    val category: String,
    val oilTsp: Double = 0.0,
    val fibre: Double = 0.0,
    val vegServings: Double = 0.0,
    val sugary: Boolean = false,
)

object FoodCatalog {
    const val KCAL_PER_TSP_OIL = 45
    const val FAT_PER_TSP_OIL = 5.0

    private val fibreByCategory = mapOf(
        "Breads & rice" to 2.5, "Breakfast" to 3.0, "Dal & curries" to 6.0, "Vegetables" to 4.0,
        "Protein" to 0.5, "Drinks" to 0.0, "Fruit" to 3.0, "Snacks & fast food" to 2.0,
        "Sweets" to 0.5, "Oil & ghee" to 0.0, "Packaged" to 1.0, "Restaurant & chains" to 2.0,
    )
    private val oilByCategory = mapOf("Dal & curries" to 1.5, "Vegetables" to 2.0)
    private val vegByCategory = mapOf("Vegetables" to 1.0, "Fruit" to 1.0)

    private fun f(
        name: String, serving: String, kcal: Int, p: Double, c: Double, fat: Double, cat: String,
        junk: Boolean = false, oil: Double? = null, fib: Double? = null, veg: Double? = null, sugary: Boolean = false,
    ) = CatalogFood(
        name, serving, kcal, p, c, fat, junk, cat,
        oilTsp = oil ?: oilByCategory[cat] ?: 0.0,
        fibre = fib ?: fibreByCategory[cat] ?: 0.0,
        vegServings = veg ?: vegByCategory[cat] ?: 0.0,
        sugary = sugary || cat == "Sweets",
    )

    private const val B = "Breads & rice"
    private const val BF = "Breakfast"
    private const val D = "Dal & curries"
    private const val V = "Vegetables"
    private const val P = "Protein"
    private const val DR = "Drinks"
    private const val FR = "Fruit"
    private const val S = "Snacks & fast food"
    private const val SW = "Sweets"
    private const val O = "Oil & ghee"
    private const val PK = "Packaged"
    private const val R = "Restaurant & chains"

    val all: List<CatalogFood> = listOf(
        // Breads and rice
        f("Roti / chapati", "1 medium, no ghee", 100, 3.0, 18.0, 2.0, B, fib = 3.0),
        f("Roti with ghee", "1 medium", 145, 3.0, 18.0, 6.5, B, fib = 3.0),
        f("Phulka", "1 small", 70, 2.0, 14.0, 0.5, B, fib = 2.0),
        f("Multigrain roti", "1 medium", 110, 4.0, 18.0, 2.5, B, fib = 4.0),
        f("Bajra roti", "1 medium", 115, 3.5, 20.0, 2.5, B, fib = 3.5),
        f("Jowar roti", "1 medium", 105, 3.5, 21.0, 1.0, B, fib = 3.5),
        f("Makki roti", "1 medium", 150, 3.0, 24.0, 5.0, B, fib = 3.0),
        f("Plain paratha", "1", 230, 5.0, 32.0, 9.0, B, fib = 3.5),
        f("Aloo paratha", "1", 300, 6.0, 42.0, 12.0, B, fib = 4.0),
        f("Paneer paratha", "1", 330, 12.0, 36.0, 15.0, B, fib = 3.5),
        f("Gobi paratha", "1", 270, 6.0, 38.0, 10.0, B, fib = 4.5),
        f("Thepla", "1", 120, 3.0, 16.0, 5.0, B, fib = 2.5),
        f("Puri", "1 piece", 100, 1.5, 11.0, 5.5, B, junk = true, fib = 0.8),
        f("Bhatura", "1", 300, 6.0, 40.0, 13.0, B, junk = true, fib = 1.5),
        f("Kulcha", "1", 250, 7.0, 42.0, 6.0, B, fib = 1.5),
        f("Naan", "1", 260, 8.0, 45.0, 5.0, B, fib = 2.0),
        f("Butter naan", "1", 330, 8.0, 45.0, 12.0, B, fib = 2.0),
        f("Garlic naan", "1", 340, 8.0, 46.0, 13.0, B, fib = 2.0),
        f("White rice", "1 cup cooked", 210, 4.0, 46.0, 0.5, B, fib = 0.6),
        f("Brown rice", "1 cup cooked", 215, 5.0, 45.0, 1.8, B, fib = 3.5),
        f("Jeera rice", "1 cup", 260, 5.0, 45.0, 6.0, B, fib = 0.8),
        f("Curd rice", "1 bowl", 250, 7.0, 38.0, 7.0, B, fib = 1.0),
        f("Lemon rice", "1 cup", 280, 5.0, 45.0, 8.0, B, fib = 1.5),
        f("Veg pulao", "1 cup", 280, 6.0, 46.0, 8.0, B, fib = 2.5),
        f("Khichdi", "1 bowl", 250, 9.0, 40.0, 6.0, B, fib = 4.0),
        f("Bisi bele bath", "1 bowl", 320, 9.0, 50.0, 9.0, B, fib = 5.0),
        f("Rajma chawal", "1 plate", 480, 16.0, 80.0, 10.0, B, fib = 10.0, oil = 2.0),
        f("Chole chawal", "1 plate", 500, 15.0, 82.0, 12.0, B, fib = 10.0, oil = 2.0),
        f("Quinoa", "1 cup cooked", 220, 8.0, 39.0, 3.5, B, fib = 5.0),
        f("Bread, white", "1 slice", 75, 2.5, 14.0, 1.0, B, fib = 0.7),
        f("Bread, brown", "1 slice", 70, 3.0, 12.0, 1.0, B, fib = 2.0),

        // Breakfast
        f("Poha", "1 plate", 270, 5.0, 45.0, 8.0, BF, fib = 2.0, oil = 1.5),
        f("Upma", "1 plate", 250, 6.0, 38.0, 8.0, BF, fib = 3.0, oil = 1.5),
        f("Idli", "1 piece", 60, 2.0, 12.0, 0.3, BF, fib = 0.8),
        f("Plain dosa", "1", 170, 4.0, 28.0, 5.0, BF, fib = 1.5),
        f("Masala dosa", "1", 370, 7.0, 52.0, 15.0, BF, fib = 4.0, oil = 2.0),
        f("Rava dosa", "1", 220, 4.0, 30.0, 9.0, BF, fib = 1.0),
        f("Uttapam", "1", 220, 6.0, 35.0, 6.0, BF, fib = 2.5),
        f("Pesarattu", "1", 180, 9.0, 25.0, 5.0, BF, fib = 5.0),
        f("Appam", "1", 120, 2.0, 24.0, 1.5, BF, fib = 0.8),
        f("Puttu", "1 piece", 200, 4.0, 40.0, 3.0, BF, fib = 2.5),
        f("Ven pongal", "1 bowl", 300, 8.0, 42.0, 11.0, BF, fib = 2.5),
        f("Medu vada", "1 piece", 140, 4.0, 15.0, 7.0, BF, junk = true, fib = 2.0),
        f("Moong dal chilla", "1", 120, 7.0, 15.0, 4.0, BF, fib = 3.0),
        f("Besan chilla", "1", 150, 7.0, 16.0, 6.0, BF, fib = 3.0),
        f("Sabudana khichdi", "1 plate", 350, 4.0, 60.0, 11.0, BF, fib = 1.0),
        f("Thalipeeth", "1", 200, 6.0, 28.0, 7.0, BF, fib = 4.0),
        f("Dhokla", "4 pieces", 160, 7.0, 24.0, 4.0, BF, fib = 2.0),
        f("Oats with milk", "1 bowl", 250, 10.0, 38.0, 6.0, BF, fib = 4.0),
        f("Masala oats", "1 bowl", 200, 6.0, 32.0, 5.0, BF, fib = 4.0),
        f("Muesli with milk", "1 bowl", 280, 10.0, 45.0, 7.0, BF, fib = 5.0, sugary = true),
        f("Cornflakes with milk", "1 bowl", 250, 8.0, 44.0, 5.0, BF, fib = 1.0, sugary = true),
        f("Bread omelette", "2 slices + 2 eggs", 330, 16.0, 28.0, 17.0, BF, fib = 1.5),
        f("Peanut butter toast", "1 slice", 170, 7.0, 15.0, 9.0, BF, fib = 2.5),

        // Dal and legumes
        f("Dal tadka", "1 bowl", 180, 9.0, 24.0, 5.0, D),
        f("Dal fry", "1 bowl", 200, 9.0, 24.0, 7.0, D, oil = 2.0),
        f("Moong dal", "1 bowl", 150, 10.0, 22.0, 3.0, D, oil = 1.0),
        f("Masoor dal", "1 bowl", 170, 10.0, 24.0, 4.0, D),
        f("Dal makhani", "1 bowl", 330, 12.0, 30.0, 18.0, D, oil = 3.0),
        f("Rajma", "1 bowl", 240, 12.0, 34.0, 6.0, D, fib = 9.0),
        f("Chole / chana masala", "1 bowl", 270, 12.0, 36.0, 9.0, D, fib = 9.0, oil = 2.0),
        f("Kadhi", "1 bowl", 180, 6.0, 14.0, 11.0, D, fib = 1.0),
        f("Sambar", "1 bowl", 130, 6.0, 18.0, 4.0, D, fib = 5.0, veg = 0.5),
        f("Rasam", "1 bowl", 70, 2.0, 10.0, 2.5, D, fib = 1.5, oil = 0.5),
        f("Sprouts salad", "1 bowl", 120, 8.0, 20.0, 1.0, D, fib = 6.0, oil = 0.0, veg = 0.5),
        f("Boiled chana", "1 bowl", 210, 11.0, 35.0, 3.5, D, fib = 10.0, oil = 0.0),
        f("Misal", "1 bowl, no pav", 330, 13.0, 35.0, 15.0, D, fib = 9.0, oil = 3.0),

        // Vegetables
        f("Mixed veg sabzi", "1 bowl", 150, 4.0, 15.0, 9.0, V),
        f("Aloo sabzi", "1 bowl", 200, 3.0, 28.0, 9.0, V, veg = 0.5),
        f("Aloo gobi", "1 bowl", 170, 4.0, 20.0, 9.0, V),
        f("Bhindi fry", "1 bowl", 160, 3.0, 12.0, 11.0, V, oil = 2.5),
        f("Baingan bharta", "1 bowl", 140, 3.0, 12.0, 9.0, V),
        f("Palak paneer", "1 bowl", 280, 14.0, 10.0, 21.0, V, oil = 2.5),
        f("Paneer butter masala", "1 bowl", 400, 16.0, 14.0, 32.0, V, oil = 3.0, veg = 0.5),
        f("Kadai paneer", "1 bowl", 330, 15.0, 12.0, 25.0, V, oil = 3.0),
        f("Matar paneer", "1 bowl", 300, 14.0, 16.0, 20.0, V, oil = 2.5),
        f("Malai kofta", "1 bowl", 450, 10.0, 25.0, 35.0, V, junk = true, oil = 4.0, veg = 0.5),
        f("Mushroom masala", "1 bowl", 160, 5.0, 12.0, 10.0, V),
        f("Cabbage poriyal", "1 bowl", 110, 3.0, 10.0, 7.0, V, oil = 1.5),
        f("Beans poriyal", "1 bowl", 110, 3.0, 11.0, 6.5, V, oil = 1.5),
        f("Lauki sabzi", "1 bowl", 90, 2.0, 10.0, 5.0, V, oil = 1.0),
        f("Undhiyu", "1 bowl", 280, 6.0, 25.0, 17.0, V, oil = 3.0),
        f("Avial", "1 bowl", 180, 4.0, 14.0, 12.0, V, oil = 1.0),
        f("Green salad", "1 bowl", 40, 2.0, 8.0, 0.3, V, oil = 0.0),
        f("Kachumber salad", "1 bowl", 50, 2.0, 10.0, 0.5, V, oil = 0.0),
        f("Raita", "1 bowl", 110, 5.0, 9.0, 5.0, V, oil = 0.0, fib = 1.0, veg = 0.5),
        f("Vegetable soup", "1 bowl", 80, 3.0, 12.0, 2.0, V, oil = 0.5),

        // Protein
        f("Boiled egg", "1 egg", 78, 6.3, 0.6, 5.3, P, fib = 0.0),
        f("Omelette", "2 eggs", 200, 13.0, 2.0, 15.0, P, oil = 1.0, fib = 0.0),
        f("Egg bhurji", "2 eggs", 220, 13.0, 4.0, 17.0, P, oil = 1.5),
        f("Egg curry", "2 eggs + gravy", 280, 14.0, 9.0, 21.0, P, oil = 2.5),
        f("Egg white omelette", "3 whites", 70, 11.0, 2.0, 2.0, P, fib = 0.0),
        f("Grilled chicken breast", "150 g", 250, 46.0, 0.0, 6.0, P, fib = 0.0),
        f("Boiled chicken breast", "100 g", 165, 31.0, 0.0, 3.6, P, fib = 0.0),
        f("Chicken curry", "1 bowl", 300, 25.0, 8.0, 19.0, P, oil = 2.5),
        f("Chicken tikka", "6 pieces", 260, 32.0, 5.0, 12.0, P, oil = 1.0),
        f("Tandoori chicken", "2 pieces", 260, 30.0, 4.0, 13.0, P, oil = 1.0),
        f("Butter chicken", "1 bowl", 440, 28.0, 12.0, 31.0, P, oil = 3.5),
        f("Chicken 65", "1 plate", 420, 28.0, 18.0, 26.0, P, junk = true, oil = 4.0),
        f("Chicken biryani", "1 plate", 730, 30.0, 85.0, 30.0, P, fib = 3.0, oil = 3.0),
        f("Mutton biryani", "1 plate", 800, 32.0, 85.0, 36.0, P, fib = 3.0, oil = 3.5),
        f("Veg biryani", "1 plate", 500, 10.0, 75.0, 17.0, B, fib = 5.0, oil = 3.0, veg = 0.5),
        f("Mutton curry", "1 bowl", 380, 26.0, 7.0, 28.0, P, oil = 3.0),
        f("Keema", "1 bowl", 350, 24.0, 8.0, 25.0, P, oil = 2.5),
        f("Haleem", "1 bowl", 400, 24.0, 30.0, 20.0, P, fib = 4.0, oil = 3.0),
        f("Fish curry", "1 bowl", 250, 22.0, 6.0, 15.0, P, oil = 2.0),
        f("Fish tikka / grilled fish", "150 g", 220, 32.0, 3.0, 9.0, P, oil = 1.0),
        f("Fish fry", "2 pieces", 320, 24.0, 10.0, 20.0, P, junk = true, oil = 3.0),
        f("Prawn curry", "1 bowl", 260, 22.0, 8.0, 15.0, P, oil = 2.0),
        f("Paneer", "100 g", 290, 18.0, 3.0, 22.0, P, fib = 0.0),
        f("Low-fat paneer", "100 g", 180, 20.0, 4.0, 9.0, P, fib = 0.0),
        f("Paneer tikka", "6 pieces", 300, 20.0, 8.0, 21.0, P, oil = 1.0),
        f("Paneer bhurji", "1 bowl", 300, 18.0, 6.0, 23.0, P, oil = 1.5),
        f("Soya chunks, cooked", "1 bowl", 180, 25.0, 15.0, 1.0, P, fib = 6.0),
        f("Soya chunk curry", "1 bowl", 230, 24.0, 16.0, 8.0, P, fib = 6.0, oil = 1.5),
        f("Tofu", "100 g", 145, 15.0, 3.0, 8.0, P, fib = 1.0),
        f("Curd / dahi", "1 bowl", 95, 5.0, 7.0, 5.0, P, fib = 0.0),
        f("Hung curd / Greek yogurt", "1 cup", 130, 12.0, 8.0, 5.0, P, fib = 0.0),
        f("Milk, toned", "1 glass, 250 ml", 150, 8.0, 12.0, 7.5, P, fib = 0.0),
        f("Milk, full cream", "1 glass, 250 ml", 200, 8.0, 12.0, 13.0, P, fib = 0.0),
        f("Soy milk", "1 glass, 250 ml", 100, 7.0, 6.0, 4.0, P, fib = 1.0),
        f("Whey protein", "1 scoop", 120, 24.0, 3.0, 1.5, P, fib = 0.0),
        f("Peanut butter", "1 tbsp", 95, 4.0, 3.0, 8.0, P, fib = 1.0),
        f("Almonds", "10 almonds", 70, 2.5, 2.5, 6.0, P, fib = 1.5),
        f("Walnuts", "5 halves", 65, 1.5, 1.4, 6.5, P, fib = 0.7),
        f("Cashews", "10 cashews", 90, 3.0, 5.0, 7.0, P, fib = 0.5),
        f("Roasted peanuts", "1 handful, 30 g", 170, 7.5, 5.0, 14.0, P, fib = 2.5),
        f("Roasted chana", "1 handful, 30 g", 110, 6.0, 18.0, 2.0, P, fib = 5.0),
        f("Makhana, roasted", "1 cup", 100, 3.0, 20.0, 1.0, P, fib = 2.0),

        // Drinks
        f("Chai with sugar", "1 cup", 80, 2.0, 12.0, 2.5, DR, sugary = true),
        f("Chai without sugar", "1 cup", 45, 2.0, 4.0, 2.5, DR),
        f("Masala chai with sugar", "1 cup", 90, 2.0, 13.0, 3.0, DR, sugary = true),
        f("Green tea", "1 cup", 2, 0.0, 0.0, 0.0, DR),
        f("Black coffee", "1 cup", 5, 0.3, 0.0, 0.0, DR),
        f("Coffee with milk and sugar", "1 cup", 90, 3.0, 13.0, 3.0, DR, sugary = true),
        f("Filter coffee", "1 cup", 110, 3.0, 14.0, 4.0, DR, sugary = true),
        f("Cold coffee", "1 glass", 250, 7.0, 35.0, 9.0, DR, junk = true, sugary = true),
        f("Buttermilk / chaas", "1 glass", 40, 2.0, 4.0, 1.5, DR),
        f("Nimbu pani with sugar", "1 glass", 90, 0.0, 22.0, 0.0, DR, sugary = true),
        f("Nimbu pani, no sugar", "1 glass", 10, 0.0, 2.0, 0.0, DR),
        f("Coconut water", "1 coconut", 45, 0.5, 9.0, 0.0, DR),
        f("Fresh fruit juice", "1 glass", 120, 1.0, 28.0, 0.3, DR, sugary = true),
        f("Sugarcane juice", "1 glass", 180, 0.0, 45.0, 0.0, DR, sugary = true),
        f("Sweet lassi", "1 glass", 250, 8.0, 38.0, 7.0, DR, junk = true, sugary = true),
        f("Salted lassi", "1 glass", 120, 7.0, 9.0, 6.0, DR),
        f("Milkshake", "1 glass", 320, 9.0, 45.0, 11.0, DR, junk = true, sugary = true),
        f("Protein shake with milk", "1 glass", 270, 32.0, 15.0, 9.0, DR),
        f("Cola / soft drink", "330 ml can", 140, 0.0, 35.0, 0.0, DR, junk = true, sugary = true),
        f("Diet cola", "330 ml can", 1, 0.0, 0.0, 0.0, DR),
        f("Packaged mango drink", "250 ml", 160, 0.0, 40.0, 0.0, DR, junk = true, sugary = true),
        f("Energy drink", "250 ml", 115, 0.0, 28.0, 0.0, DR, junk = true, sugary = true),
        f("Beer", "500 ml", 215, 2.0, 18.0, 0.0, DR, junk = true),
        f("Whisky / rum", "60 ml", 140, 0.0, 0.0, 0.0, DR, junk = true),
        f("Wine", "1 glass, 150 ml", 125, 0.0, 4.0, 0.0, DR, junk = true),

        // Fruit
        f("Banana", "1 medium", 105, 1.3, 27.0, 0.4, FR),
        f("Apple", "1 medium", 95, 0.5, 25.0, 0.3, FR, fib = 4.4),
        f("Orange", "1 medium", 60, 1.2, 15.0, 0.2, FR),
        f("Mango", "1 cup", 100, 1.4, 25.0, 0.6, FR),
        f("Papaya", "1 cup", 60, 0.7, 15.0, 0.4, FR),
        f("Watermelon", "1 cup", 45, 0.9, 11.0, 0.2, FR, fib = 0.6),
        f("Guava", "1 medium", 70, 2.6, 14.0, 1.0, FR, fib = 5.4),
        f("Pomegranate", "1 cup seeds", 145, 3.0, 33.0, 2.0, FR, fib = 7.0),
        f("Grapes", "1 cup", 105, 1.1, 27.0, 0.2, FR, fib = 1.4),
        f("Pineapple", "1 cup", 80, 0.9, 22.0, 0.2, FR, fib = 2.3),
        f("Pear", "1 medium", 100, 0.6, 27.0, 0.2, FR, fib = 5.5),
        f("Chikoo", "1 medium", 85, 0.5, 20.0, 1.1, FR, fib = 5.0),
        f("Muskmelon", "1 cup", 55, 1.3, 13.0, 0.3, FR, fib = 1.4),
        f("Dates", "2 dates", 130, 0.8, 36.0, 0.1, FR, fib = 3.2, sugary = true, veg = 0.0),
        f("Raisins", "1 tbsp", 30, 0.3, 8.0, 0.0, FR, fib = 0.4, veg = 0.0),

        // Snacks and street food
        f("Samosa", "1 piece", 260, 4.0, 30.0, 14.0, S, junk = true),
        f("Kachori", "1 piece", 280, 5.0, 28.0, 17.0, S, junk = true),
        f("Pakora / bhajji", "5 pieces", 300, 6.0, 28.0, 18.0, S, junk = true),
        f("Aloo tikki", "2 pieces", 280, 5.0, 34.0, 14.0, S, junk = true),
        f("Vada pav", "1", 300, 7.0, 42.0, 12.0, S, junk = true),
        f("Dabeli", "1", 280, 6.0, 40.0, 11.0, S, junk = true),
        f("Pav bhaji", "1 plate", 500, 11.0, 65.0, 22.0, S, junk = true, veg = 1.0),
        f("Misal pav", "1 plate", 480, 15.0, 60.0, 20.0, S, junk = true, fib = 9.0),
        f("Chole bhature", "1 plate", 650, 18.0, 75.0, 30.0, S, junk = true, fib = 9.0),
        f("Pani puri", "6 pieces", 200, 4.0, 30.0, 7.0, S, junk = true),
        f("Bhel puri", "1 plate", 250, 6.0, 40.0, 8.0, S, junk = true),
        f("Sev puri", "6 pieces", 300, 6.0, 38.0, 14.0, S, junk = true),
        f("Dahi puri", "6 pieces", 280, 7.0, 40.0, 10.0, S, junk = true),
        f("Papdi chaat", "1 plate", 330, 8.0, 42.0, 15.0, S, junk = true),
        f("Chips", "50 g pack", 270, 3.0, 27.0, 17.0, S, junk = true),
        f("Namkeen / bhujia", "1 handful, 30 g", 160, 4.0, 14.0, 10.0, S, junk = true),
        f("Kurkure", "50 g pack", 270, 3.0, 30.0, 16.0, S, junk = true),
        f("Cream biscuits", "4 biscuits", 200, 2.0, 28.0, 9.0, S, junk = true, sugary = true),
        f("Glucose biscuits", "4 biscuits", 110, 1.5, 19.0, 3.0, S, junk = true, sugary = true),
        f("Marie biscuits", "4 biscuits", 110, 2.0, 18.0, 3.0, S),
        f("Rusk", "2 pieces", 120, 3.0, 22.0, 2.0, S, sugary = true),
        f("Khakhra", "2 pieces", 120, 4.0, 18.0, 3.5, S, fib = 3.0),
        f("Instant noodles", "1 pack", 350, 8.0, 48.0, 14.0, S, junk = true),
        f("Hakka noodles", "1 plate", 450, 9.0, 60.0, 19.0, S, junk = true),
        f("Veg fried rice", "1 plate", 420, 8.0, 65.0, 13.0, S, junk = true),
        f("Gobi manchurian", "1 plate", 380, 6.0, 40.0, 22.0, S, junk = true),
        f("Fried momos", "6 pieces", 400, 12.0, 42.0, 20.0, S, junk = true),
        f("Steamed momos", "6 pieces", 250, 12.0, 36.0, 6.0, S),
        f("Egg roll", "1 roll", 380, 14.0, 40.0, 18.0, S, junk = true),
        f("Kathi roll", "1 roll", 450, 18.0, 48.0, 20.0, S, junk = true),
        f("Chicken shawarma", "1 roll", 500, 28.0, 45.0, 23.0, S, junk = true),
        f("Veg sandwich", "1 sandwich", 250, 7.0, 34.0, 9.0, S, veg = 0.5),
        f("Grilled chicken sandwich", "1 sandwich", 350, 25.0, 34.0, 12.0, S),
        f("Popcorn, salted", "1 cup", 55, 2.0, 6.0, 3.0, S),

        // Restaurant and chains (typical values)
        f("Pizza", "2 slices", 560, 24.0, 66.0, 22.0, R, junk = true),
        f("Burger", "1", 450, 20.0, 45.0, 21.0, R, junk = true),
        f("Veg burger (chain)", "1", 400, 10.0, 52.0, 17.0, R, junk = true),
        f("Chicken burger (chain)", "1", 450, 18.0, 45.0, 22.0, R, junk = true),
        f("French fries", "medium", 330, 4.0, 43.0, 16.0, R, junk = true),
        f("Fried chicken", "2 pieces", 500, 36.0, 18.0, 31.0, R, junk = true),
        f("Sub sandwich, veg", "6 inch", 300, 10.0, 48.0, 6.0, R, fib = 4.0),
        f("Sub sandwich, chicken", "6 inch", 340, 24.0, 44.0, 7.0, R, fib = 4.0),
        f("Pasta in white sauce", "1 plate", 600, 16.0, 70.0, 28.0, R, junk = true),
        f("Pasta in red sauce", "1 plate", 450, 14.0, 70.0, 12.0, R, veg = 0.5),
        f("Thali, veg (restaurant)", "1 thali", 900, 25.0, 120.0, 35.0, R, fib = 12.0, veg = 1.0),
        f("Chicken thali (restaurant)", "1 thali", 1050, 45.0, 115.0, 45.0, R, fib = 9.0),

        // Packaged
        f("Protein bar", "1 bar, 60 g", 230, 20.0, 22.0, 8.0, PK, fib = 5.0),
        f("Oats, plain", "40 g dry", 150, 5.0, 26.0, 3.0, PK, fib = 4.0),
        f("Peanut chikki", "1 piece, 25 g", 120, 3.0, 13.0, 6.0, PK, sugary = true),
        f("Butter", "1 tsp", 36, 0.0, 0.0, 4.0, PK, fib = 0.0),
        f("Cheese slice", "1 slice", 60, 4.0, 1.0, 4.5, PK, fib = 0.0),
        f("Packaged dahi / yogurt cup", "1 cup", 100, 5.0, 9.0, 4.5, PK, fib = 0.0),
        f("Flavoured yogurt", "1 cup", 150, 5.0, 24.0, 3.5, PK, fib = 0.0, sugary = true),
        f("Ready-to-eat poha / upma cup", "1 cup", 250, 5.0, 40.0, 8.0, PK, fib = 2.0),
        f("Chocolate cookies", "3 cookies", 210, 2.5, 28.0, 10.0, PK, junk = true, sugary = true),
        f("Dark chocolate", "2 squares, 20 g", 110, 1.5, 9.0, 8.0, PK, fib = 2.0),

        // Sweets
        f("Gulab jamun", "1 piece", 150, 2.0, 22.0, 6.0, SW, junk = true),
        f("Rasgulla", "1 piece", 110, 2.0, 22.0, 1.5, SW, junk = true),
        f("Rasmalai", "1 piece", 180, 5.0, 22.0, 8.0, SW, junk = true),
        f("Jalebi", "100 g", 380, 3.0, 60.0, 15.0, SW, junk = true),
        f("Ladoo", "1 piece", 180, 3.0, 22.0, 9.0, SW, junk = true),
        f("Kaju katli", "2 pieces", 150, 3.0, 18.0, 7.5, SW, junk = true),
        f("Barfi", "1 piece", 160, 3.0, 22.0, 7.0, SW, junk = true),
        f("Mysore pak", "1 piece", 200, 2.0, 18.0, 14.0, SW, junk = true),
        f("Gajar halwa", "1 bowl", 330, 6.0, 42.0, 16.0, SW, junk = true, fib = 3.0),
        f("Sooji halwa", "1 bowl", 350, 4.0, 48.0, 16.0, SW, junk = true),
        f("Kheer", "1 bowl", 250, 7.0, 38.0, 8.0, SW, junk = true),
        f("Payasam", "1 bowl", 280, 6.0, 42.0, 10.0, SW, junk = true),
        f("Shrikhand", "1 bowl", 300, 8.0, 45.0, 10.0, SW, junk = true),
        f("Kulfi", "1 stick", 200, 5.0, 22.0, 10.0, SW, junk = true),
        f("Ice cream", "1 scoop", 140, 2.5, 17.0, 7.0, SW, junk = true),
        f("Chocolate bar", "40 g", 210, 3.0, 24.0, 12.0, SW, junk = true),
        f("Cake / pastry", "1 slice", 350, 4.0, 45.0, 17.0, SW, junk = true),
        f("Donut", "1", 300, 4.0, 35.0, 16.0, SW, junk = true),

        // Fats
        f("Ghee", "1 tsp", 45, 0.0, 0.0, 5.0, O),
        f("Cooking oil", "1 tbsp", 120, 0.0, 0.0, 14.0, O),
    ).distinctBy { it.name }

    private val byName = all.associateBy { it.name.lowercase() }

    fun byName(name: String): CatalogFood? = byName[name.lowercase()]

    fun search(query: String): List<CatalogFood> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) }
            .sortedBy { if (it.name.lowercase().startsWith(q)) 0 else 1 }
    }
}
