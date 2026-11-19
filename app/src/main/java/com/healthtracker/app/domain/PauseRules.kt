package com.healthtracker.app.domain

import com.healthtracker.app.data.PauseMode

/** The rules for how hard the food-app pause makes it to continue. */
object PauseRules {
    const val PROMISE = "I am choosing food over my goal"
    const val LOCKDOWN_WAIT = 300

    data class OrderSize(val label: String, val kcal: Int)

    val orderSizes = listOf(
        OrderSize("Snack or dessert", 450),
        OrderSize("Regular meal", 800),
        OrderSize("Big meal", 1200),
        OrderSize("Feast", 1700),
    )

    /** Seconds to wait before anything but closing is allowed. Strict mode gets longer each time today. */
    fun waitSeconds(mode: PauseMode, base: Int, continuesToday: Int): Int = when (mode) {
        PauseMode.GENTLE -> base
        PauseMode.STRICT -> (base * listOf(1, 3, 6, 12)[continuesToday.coerceIn(0, 3)]).coerceAtMost(180)
        PauseMode.LOCKDOWN -> LOCKDOWN_WAIT
    }

    fun needsPromise(mode: PauseMode): Boolean = mode != PauseMode.GENTLE

    fun needsOrderLog(mode: PauseMode): Boolean = mode != PauseMode.GENTLE

    fun normalize(text: String): String =
        text.lowercase().map { if (it.isLetter()) it else ' ' }.joinToString("").split(' ').filter { it.isNotBlank() }.joinToString(" ")

    fun promiseMatches(typed: String): Boolean = normalize(typed) == normalize(PROMISE)

    /** Rupees kept by backing out, using the person's usual order value. */
    fun moneySaved(closedCount: Int, orderValue: Int): Int = closedCount * orderValue
}
