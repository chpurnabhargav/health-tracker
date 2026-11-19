package com.healthtracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.ui.HealthViewModel
import com.healthtracker.app.ui.components.AppCard
import com.healthtracker.app.ui.components.BarDatum
import com.healthtracker.app.ui.components.DayBarChart
import com.healthtracker.app.ui.components.HeroCard
import com.healthtracker.app.ui.components.LegendLine
import com.healthtracker.app.ui.components.LegendSwatch
import com.healthtracker.app.ui.components.Overline
import com.healthtracker.app.ui.components.ScreenTitle
import com.healthtracker.app.ui.components.SectionHeader
import com.healthtracker.app.ui.components.TileGrid
import com.healthtracker.app.ui.theme.App

@Composable
fun ReviewScreen(vm: HealthViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    val h = st ?: return
    val c = App.colors
    val r = h.review
    val s = h.s

    ScreenScroll {
        ScreenTitle("Weekly review", "${Fmt.dayMonth(r.from)} – ${Fmt.dayMonth(r.to)}")
        Text(r.headline, style = MaterialTheme.typography.titleLarge, color = c.ink)

        val tiles = mutableListOf(
            Triple("Weight trend", r.weightChange?.let { "${Fmt.signedKg(it)} kg" } ?: "—",
                if (r.trendFrom != null && r.trendTo != null) "${Fmt.kg(r.trendFrom)} → ${Fmt.kg(r.trendTo)} kg" else "Needs weigh-ins"),
            Triple("Average calories", r.avgKcal?.let { Fmt.int(it) } ?: "—",
                r.avgKcal?.let { val d = it - h.calorieTarget; if (d <= 0) "${Fmt.int(-d)} under target" else "${Fmt.int(d)} over target" } ?: "Log meals to see this"),
            Triple("Water goal hit", "${r.waterHitDays} of 7 days", if (r.avgWaterMl > 0) "Average ${Fmt.one(r.avgWaterMl / 1000.0)} L" else "Log water to see this"),
            Triple("Food-app pauses", if (r.pauseOpens == 0) "None" else "${r.pauseClosed} of ${r.pauseOpens}", if (r.pauseOpens == 0) "No food apps opened" else "times you backed out"),
            Triple("Days logged", "${r.daysLogged} of 7", "Weight, food or minimum day"),
            Triple("Average steps", r.avgSteps?.let { Fmt.int(it) } ?: "—", "Goal ${Fmt.int(s.stepGoal)}"),
        )
        r.avgProtein?.let { tiles += Triple("Average protein", "$it g", "Target ${h.proteinTarget} g") }
        r.avgSleepMin?.let { tiles += Triple("Average sleep", Fmt.hours(it), "A night") }
        TileGrid(tiles)

        AppCard {
            SectionHeader("Water by day")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendSwatch(c.data, "Goal hit")
                LegendSwatch(c.track, "Under goal")
                LegendLine(c.ink2, "Goal ${Fmt.litres(s.waterGoalMl)} L")
            }
            val maxV = maxOf(s.waterGoalMl * 1.3f, r.days.maxOf { it.waterMl }.toFloat())
            DayBarChart(
                bars = r.days.map { d ->
                    val hit = d.waterMl >= s.waterGoalMl
                    BarDatum(
                        label = Fmt.weekday(d.date),
                        value = d.waterMl.toFloat(),
                        color = if (hit) c.data else c.track,
                        valueLabel = Fmt.litres(d.waterMl),
                        description = "${Fmt.weekdayLong(d.date)}: ${Fmt.litres(d.waterMl)} litres${if (hit) ", goal hit" else ""}",
                    )
                },
                maxValue = maxV,
                target = s.waterGoalMl.toFloat(),
                selected = null,
                onSelect = null,
                showValues = { i -> r.days[i].waterMl in 1 until s.waterGoalMl },
            )
        }

        if (r.pauseOpens > 0) {
            AppCard {
                SectionHeader("\"No ${s.displayName}\" pauses")
                val window = h.pauses.filter { it.day in r.from.toEpochDay()..r.to.toEpochDay() }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.semantics { contentDescription = "${r.pauseOpens} food-app opens, backed out ${r.pauseClosed} times" },
                ) {
                    window.take(14).forEach { p ->
                        val col = when (p.outcome) { "closed" -> c.data; "planned" -> c.aqua; else -> c.over }
                        Box(Modifier.size(22.dp).clip(CircleShape).background(col))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendSwatch(c.data, "Backed out", round = true)
                    LegendSwatch(c.over, "Ordered", round = true)
                    if (window.any { it.outcome == "planned" }) LegendSwatch(c.aqua, "Planned meal", round = true)
                }
                Text(
                    "You opened a food app ${r.pauseOpens} times and closed it ${r.pauseClosed} times. This month: ${h.monthClosed} of ${h.monthPauses.size}.",
                    style = MaterialTheme.typography.bodyMedium, color = c.ink,
                )
            }
        }

        if (r.noticed.isNotEmpty()) {
            AppCard {
                SectionHeader("What we noticed")
                r.noticed.forEach { n ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 9.dp).size(6.dp).clip(CircleShape).background(c.primary))
                        Text(n, style = MaterialTheme.typography.bodyLarge, color = c.ink)
                    }
                }
                if ((h.todayDay - s.startDay) < 28) {
                    Text("Patterns get more reliable after about 4 weeks of data.", style = MaterialTheme.typography.bodySmall, color = c.ink2)
                }
            }
        }

        HeroCard {
            Overline("One focus for next week", c.heroMuted)
            Text(r.focus, style = MaterialTheme.typography.titleLarge, color = c.onHero)
            Text(
                "Just one. Small changes you keep beat big changes you drop.",
                style = MaterialTheme.typography.bodyMedium, color = c.heroMuted,
            )
        }
        if (s.weeklyRemind) {
            Text("A reminder to check your review arrives every Sunday at 9 AM.", style = MaterialTheme.typography.bodySmall, color = c.ink2)
        }
    }
}

