package com.healthtracker.app.domain

import com.healthtracker.app.data.PauseMode
import com.healthtracker.app.data.UserSettings
import java.time.DayOfWeek
import java.time.LocalDateTime

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

    /** True when [minute] (minute of day) is inside a window that may run past midnight. */
    fun inWindow(minute: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> minute in start until end
        else -> minute >= start || minute < end
    }

    private fun isWeekend(d: DayOfWeek) = d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY

    /** Whether the automatic lockdown schedule is active right now. */
    fun scheduledLockdown(s: UserSettings, now: LocalDateTime): Boolean {
        if (!s.nightLockdown) return false
        if (s.weekendLockdown && isWeekend(now.dayOfWeek)) return true
        return inWindow(now.hour * 60 + now.minute, s.lockStartMin, s.lockEndMin)
    }

    /** The mode that applies right now: the schedule can only make it stricter. */
    fun effectiveMode(s: UserSettings, now: LocalDateTime): PauseMode =
        if (scheduledLockdown(s, now)) PauseMode.LOCKDOWN else s.pauseMode

    /** A short line explaining an active schedule, like "Night lockdown until 6:00 AM". */
    fun scheduleLine(s: UserSettings, now: LocalDateTime): String? {
        if (!scheduledLockdown(s, now)) return null
        if (s.weekendLockdown && isWeekend(now.dayOfWeek)) return "Weekend lockdown until Monday"
        return "Night lockdown until ${Fmt.clock(s.lockEndMin)}"
    }

    /** Rupees kept by backing out, using the person's usual order value. */
    fun moneySaved(closedCount: Int, orderValue: Int): Int = closedCount * orderValue
}
