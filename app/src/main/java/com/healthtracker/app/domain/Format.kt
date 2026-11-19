package com.healthtracker.app.domain

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Fmt {
    private val intFmt = NumberFormat.getIntegerInstance(Locale.US)

    fun int(n: Int): String = intFmt.format(n)
    fun int(n: Double): String = intFmt.format(n.roundToInt())
    fun kg(x: Double): String = String.format(Locale.US, "%.1f", x)
    fun one(x: Double): String = String.format(Locale.US, "%.1f", x)

    /** "−1.2" for losses, "+0.4" for gains. */
    fun signedKg(x: Double): String {
        val v = (x * 10).roundToInt() / 10.0
        return when {
            v < 0 -> "−" + kg(abs(v))
            v > 0 -> "+" + kg(v)
            else -> "0.0"
        }
    }

    fun litres(ml: Int): String {
        val l = ml / 1000.0
        return if (ml % 1000 == 0) String.format(Locale.US, "%.1f", l)
        else if (ml % 100 == 0) String.format(Locale.US, "%.1f", l)
        else String.format(Locale.US, "%.2f", l)
    }

    fun hours(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h} h" else "${h} h ${m} min"
    }

    fun clock(minOfDay: Int): String {
        val h = minOfDay / 60
        val m = minOfDay % 60
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = when (val x = h % 12) { 0 -> 12; else -> x }
        return String.format(Locale.US, "%d:%02d %s", h12, m, ampm)
    }

    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    private val longDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)
    private val fullDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US)
    private val weekday = DateTimeFormatter.ofPattern("EEE", Locale.US)
    private val weekdayLong = DateTimeFormatter.ofPattern("EEEE", Locale.US)

    fun dayMonth(d: LocalDate): String = d.format(dayMonth)
    fun longDate(d: LocalDate): String = d.format(longDate)
    fun fullDate(d: LocalDate): String = d.format(fullDate)
    fun weekday(d: LocalDate): String = d.format(weekday)
    fun weekdayLong(d: LocalDate): String = d.format(weekdayLong)

    fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
}
