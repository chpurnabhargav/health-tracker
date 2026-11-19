package com.healthtracker.app.data

/** A food you can pick from the built-in list. Values are per serving and are typical estimates. */
data class CatalogFood(
    val name: String,
    val serving: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val junk: Boolean = false,
    val category: String,
)

object FoodCatalog {
    private fun f(name: String, serving: String, kcal: Int, p: Double, c: Double, fat: Double, cat: String, junk: Boolean = false) =
        CatalogFood(name, serving, kcal, p, c, fat, junk, cat)

    val all: List<CatalogFood> = listOf(
        // Breads and grains
        f("Roti / chapati", "1 medium, no ghee", 100, 3.0, 18.0, 2.0, "Breads & rice"),
        f("Roti with ghee", "1 medium", 145, 3.0, 18.0, 6.5, "Breads & rice"),
        f("Phulka", "1 small", 70, 2.0, 14.0, 0.5, "Breads & rice"),
        f("Plain paratha", "1", 230, 5.0, 32.0, 9.0, "Breads & rice"),
        f("Aloo paratha", "1", 300, 6.0, 42.0, 12.0, "Breads & rice"),
        f("Naan", "1", 260, 8.0, 45.0, 5.0, "Breads & rice"),
        f("Butter naan", "1", 330, 8.0, 45.0, 12.0, "Breads & rice"),
        f("White rice", "1 cup cooked", 210, 4.0, 46.0, 0.5, "Breads & rice"),
        f("Brown rice", "1 cup cooked", 215, 5.0, 45.0, 1.8, "Breads & rice"),
        f("Jeera rice", "1 cup", 260, 5.0, 45.0, 6.0, "Breads & rice"),
        f("Bread, white", "1 slice", 75, 2.5, 14.0, 1.0, "Breads & rice"),
        f("Bread, brown", "1 slice", 70, 3.0, 12.0, 1.0, "Breads & rice"),

        // Breakfast
        f("Poha", "1 plate", 270, 5.0, 45.0, 8.0, "Breakfast"),
        f("Upma", "1 plate", 250, 6.0, 38.0, 8.0, "Breakfast"),
        f("Idli", "1 piece", 60, 2.0, 12.0, 0.3, "Breakfast"),
        f("Plain dosa", "1", 170, 4.0, 28.0, 5.0, "Breakfast"),
        f("Masala dosa", "1", 370, 7.0, 52.0, 15.0, "Breakfast"),
        f("Uttapam", "1", 220, 6.0, 35.0, 6.0, "Breakfast"),
        f("Medu vada", "1 piece", 140, 4.0, 15.0, 7.0, "Breakfast", junk = true),
        f("Moong dal chilla", "1", 120, 7.0, 15.0, 4.0, "Breakfast"),
        f("Oats with milk", "1 bowl", 250, 10.0, 38.0, 6.0, "Breakfast"),
        f("Cornflakes with milk", "1 bowl", 250, 8.0, 44.0, 5.0, "Breakfast"),

        // Dal and legumes
        f("Dal tadka", "1 bowl", 180, 9.0, 24.0, 5.0, "Dal & curries"),
        f("Dal makhani", "1 bowl", 330, 12.0, 30.0, 18.0, "Dal & curries"),
        f("Rajma", "1 bowl", 240, 12.0, 34.0, 6.0, "Dal & curries"),
        f("Chole / chana masala", "1 bowl", 270, 12.0, 36.0, 9.0, "Dal & curries"),
        f("Sambar", "1 bowl", 130, 6.0, 18.0, 4.0, "Dal & curries"),
        f("Sprouts salad", "1 bowl", 120, 8.0, 20.0, 1.0, "Dal & curries"),

        // Vegetables
        f("Mixed veg sabzi", "1 bowl", 150, 4.0, 15.0, 9.0, "Vegetables"),
        f("Aloo sabzi", "1 bowl", 200, 3.0, 28.0, 9.0, "Vegetables"),
        f("Bhindi fry", "1 bowl", 160, 3.0, 12.0, 11.0, "Vegetables"),
        f("Palak paneer", "1 bowl", 280, 14.0, 10.0, 21.0, "Vegetables"),
        f("Paneer butter masala", "1 bowl", 400, 16.0, 14.0, 32.0, "Vegetables"),
        f("Green salad", "1 bowl", 40, 2.0, 8.0, 0.3, "Vegetables"),
        f("Raita", "1 bowl", 110, 5.0, 9.0, 5.0, "Vegetables"),

        // Protein
        f("Boiled egg", "1 egg", 78, 6.3, 0.6, 5.3, "Protein"),
        f("Omelette", "2 eggs", 200, 13.0, 2.0, 15.0, "Protein"),
        f("Egg bhurji", "2 eggs", 220, 13.0, 4.0, 17.0, "Protein"),
        f("Grilled chicken breast", "150 g", 250, 46.0, 0.0, 6.0, "Protein"),
        f("Chicken curry", "1 bowl", 300, 25.0, 8.0, 19.0, "Protein"),
        f("Tandoori chicken", "2 pieces", 260, 30.0, 4.0, 13.0, "Protein"),
        f("Butter chicken", "1 bowl", 440, 28.0, 12.0, 31.0, "Protein"),
        f("Chicken biryani", "1 plate", 730, 30.0, 85.0, 30.0, "Protein"),
        f("Veg biryani", "1 plate", 500, 10.0, 75.0, 17.0, "Breads & rice"),
        f("Fish curry", "1 bowl", 250, 22.0, 6.0, 15.0, "Protein"),
        f("Fish fry", "2 pieces", 320, 24.0, 10.0, 20.0, "Protein", junk = true),
        f("Paneer", "100 g", 290, 18.0, 3.0, 22.0, "Protein"),
        f("Paneer tikka", "6 pieces", 300, 20.0, 8.0, 21.0, "Protein"),
        f("Soya chunks, cooked", "1 bowl", 180, 25.0, 15.0, 1.0, "Protein"),
        f("Tofu", "100 g", 145, 15.0, 3.0, 8.0, "Protein"),
        f("Curd / dahi", "1 bowl", 95, 5.0, 7.0, 5.0, "Protein"),
        f("Hung curd / Greek yogurt", "1 cup", 130, 12.0, 8.0, 5.0, "Protein"),
        f("Milk, toned", "1 glass, 250 ml", 150, 8.0, 12.0, 7.5, "Protein"),
        f("Whey protein", "1 scoop", 120, 24.0, 3.0, 1.5, "Protein"),
        f("Peanut butter", "1 tbsp", 95, 4.0, 3.0, 8.0, "Protein"),
        f("Almonds", "10 almonds", 70, 2.5, 2.5, 6.0, "Protein"),
        f("Roasted peanuts", "1 handful, 30 g", 170, 7.5, 5.0, 14.0, "Protein"),

        // Drinks
        f("Chai with sugar", "1 cup", 80, 2.0, 12.0, 2.5, "Drinks"),
        f("Chai without sugar", "1 cup", 45, 2.0, 4.0, 2.5, "Drinks"),
        f("Black coffee", "1 cup", 5, 0.3, 0.0, 0.0, "Drinks"),
        f("Coffee with milk and sugar", "1 cup", 90, 3.0, 13.0, 3.0, "Drinks"),
        f("Buttermilk / chaas", "1 glass", 40, 2.0, 4.0, 1.5, "Drinks"),
        f("Coconut water", "1 coconut", 45, 0.5, 9.0, 0.0, "Drinks"),
        f("Fresh fruit juice", "1 glass", 120, 1.0, 28.0, 0.3, "Drinks"),
        f("Sweet lassi", "1 glass", 250, 8.0, 38.0, 7.0, "Drinks", junk = true),
        f("Cola / soft drink", "330 ml can", 140, 0.0, 35.0, 0.0, "Drinks", junk = true),
        f("Beer", "500 ml", 215, 2.0, 18.0, 0.0, "Drinks", junk = true),
        f("Whisky / rum", "60 ml", 140, 0.0, 0.0, 0.0, "Drinks", junk = true),

        // Fruit
        f("Banana", "1 medium", 105, 1.3, 27.0, 0.4, "Fruit"),
        f("Apple", "1 medium", 95, 0.5, 25.0, 0.3, "Fruit"),
        f("Mango", "1 cup", 100, 1.4, 25.0, 0.6, "Fruit"),
        f("Papaya", "1 cup", 60, 0.7, 15.0, 0.4, "Fruit"),
        f("Orange", "1 medium", 60, 1.2, 15.0, 0.2, "Fruit"),
        f("Watermelon", "1 cup", 45, 0.9, 11.0, 0.2, "Fruit"),

        // Snacks and fast food
        f("Samosa", "1 piece", 260, 4.0, 30.0, 14.0, "Snacks & fast food", junk = true),
        f("Pakora / bhajji", "5 pieces", 300, 6.0, 28.0, 18.0, "Snacks & fast food", junk = true),
        f("Vada pav", "1", 300, 7.0, 42.0, 12.0, "Snacks & fast food", junk = true),
        f("Pav bhaji", "1 plate", 500, 11.0, 65.0, 22.0, "Snacks & fast food", junk = true),
        f("Pani puri", "6 pieces", 200, 4.0, 30.0, 7.0, "Snacks & fast food", junk = true),
        f("Bhel puri", "1 plate", 250, 6.0, 40.0, 8.0, "Snacks & fast food", junk = true),
        f("Chips", "50 g pack", 270, 3.0, 27.0, 17.0, "Snacks & fast food", junk = true),
        f("Namkeen / bhujia", "1 handful, 30 g", 160, 4.0, 14.0, 10.0, "Snacks & fast food", junk = true),
        f("Cream biscuits", "4 biscuits", 200, 2.0, 28.0, 9.0, "Snacks & fast food", junk = true),
        f("Instant noodles", "1 pack", 350, 8.0, 48.0, 14.0, "Snacks & fast food", junk = true),
        f("Pizza", "2 slices", 560, 24.0, 66.0, 22.0, "Snacks & fast food", junk = true),
        f("Burger", "1", 450, 20.0, 45.0, 21.0, "Snacks & fast food", junk = true),
        f("French fries", "medium", 330, 4.0, 43.0, 16.0, "Snacks & fast food", junk = true),
        f("Fried momos", "6 pieces", 400, 12.0, 42.0, 20.0, "Snacks & fast food", junk = true),
        f("Steamed momos", "6 pieces", 250, 12.0, 36.0, 6.0, "Snacks & fast food"),
        f("Chicken shawarma", "1 roll", 500, 28.0, 45.0, 23.0, "Snacks & fast food", junk = true),
        f("Kathi roll", "1 roll", 450, 18.0, 48.0, 20.0, "Snacks & fast food", junk = true),

        // Sweets
        f("Gulab jamun", "1 piece", 150, 2.0, 22.0, 6.0, "Sweets", junk = true),
        f("Rasgulla", "1 piece", 110, 2.0, 22.0, 1.5, "Sweets", junk = true),
        f("Jalebi", "100 g", 380, 3.0, 60.0, 15.0, "Sweets", junk = true),
        f("Ladoo", "1 piece", 180, 3.0, 22.0, 9.0, "Sweets", junk = true),
        f("Kheer", "1 bowl", 250, 7.0, 38.0, 8.0, "Sweets", junk = true),
        f("Ice cream", "1 scoop", 140, 2.5, 17.0, 7.0, "Sweets", junk = true),
        f("Chocolate bar", "40 g", 210, 3.0, 24.0, 12.0, "Sweets", junk = true),
        f("Cake / pastry", "1 slice", 350, 4.0, 45.0, 17.0, "Sweets", junk = true),

        // Fats
        f("Ghee", "1 tsp", 45, 0.0, 0.0, 5.0, "Oil & ghee"),
        f("Cooking oil", "1 tbsp", 120, 0.0, 0.0, 14.0, "Oil & ghee"),
    )

    fun search(query: String): List<CatalogFood> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) }
            .sortedBy { if (it.name.lowercase().startsWith(q)) 0 else 1 }
    }
}
