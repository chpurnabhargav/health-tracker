package com.healthtracker.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.healthtracker.app.data.UserSettings
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object ReminderScheduler {
    const val T_WATER = 1
    const val T_WEIGH = 2
    const val T_WEIGH_FOLLOW = 3
    const val T_MEAL = 4
    const val T_WEEKLY = 5

    /** Minute of day and meal name. */
    val mealTimes = listOf(10 * 60 to "breakfast", 14 * 60 + 30 to "lunch", 21 * 60 + 30 to "dinner")

    fun scheduleAll(ctx: Context, s: UserSettings, now: ZonedDateTime = ZonedDateTime.now()) {
        val on = s.onboarded
        set(ctx, T_WATER, if (on && s.waterRemind) nextWater(s, now) else null)
        set(ctx, T_WEIGH, if (on && s.weighRemind) nextAt(now, s.weighMin) else null)
        set(ctx, T_WEIGH_FOLLOW, if (on && s.weighRemind) nextAt(now, (s.weighMin + 150).coerceAtMost(23 * 60)) else null)
        set(ctx, T_MEAL, if (on && s.mealRemind) nextMeal(now) else null)
        set(ctx, T_WEEKLY, if (on && s.weeklyRemind) nextWeekly(now) else null)
    }

    /** Moves the next water reminder to [minutes] from now. */
    fun snoozeWater(ctx: Context, minutes: Int) {
        set(ctx, T_WATER, System.currentTimeMillis() + minutes * 60_000L)
    }

    private fun pending(ctx: Context, type: Int): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE + type)
            .putExtra(ReminderReceiver.EXTRA_TYPE, type)
        return PendingIntent.getBroadcast(ctx, type, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun set(ctx: Context, type: Int, at: Long?) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(ctx, type)
        if (at == null) {
            am.cancel(pi)
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun atMinute(base: ZonedDateTime, minOfDay: Int): ZonedDateTime =
        base.toLocalDate().atTime(minOfDay / 60, minOfDay % 60).atZone(base.zone)

    /** Next time at [minOfDay], today if still ahead, otherwise tomorrow. */
    fun nextAt(now: ZonedDateTime, minOfDay: Int): Long {
        var t = atMinute(now, minOfDay)
        if (!t.isAfter(now.plusSeconds(30))) t = t.plusDays(1)
        return t.toInstant().toEpochMilli()
    }

    fun nextWater(s: UserSettings, now: ZonedDateTime): Long {
        val every = s.waterEveryMin.coerceAtLeast(30)
        val start = s.waterStartMin
        val end = s.waterEndMin.coerceAtLeast(start + every)
        var m = start + every
        while (m <= end) {
            val t = atMinute(now, m)
            if (t.isAfter(now.plusSeconds(30))) return t.toInstant().toEpochMilli()
            m += every
        }
        return atMinute(now.plusDays(1), start + every).toInstant().toEpochMilli()
    }

    fun nextMeal(now: ZonedDateTime): Long =
        mealTimes.map { nextAt(now, it.first) }.min()

    fun mealFor(now: ZonedDateTime): String {
        val m = now.hour * 60 + now.minute
        return mealTimes.minByOrNull { kotlin.math.abs(it.first - m) }!!.second
    }

    fun nextWeekly(now: ZonedDateTime): Long {
        var t = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(9, 0).atZone(now.zone)
        if (!t.isAfter(now.plusSeconds(30))) t = t.plusWeeks(1)
        return t.toInstant().toEpochMilli()
    }
}
