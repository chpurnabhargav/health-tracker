package com.healthtracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SouthEast
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.ui.HealthViewModel
import com.healthtracker.app.ui.components.AppCard
import com.healthtracker.app.ui.components.HairlineDivider
import com.healthtracker.app.ui.components.HeroCard
import com.healthtracker.app.ui.components.LegendLine
import com.healthtracker.app.ui.components.LegendSwatch
import com.healthtracker.app.ui.components.NumberDialog
import com.healthtracker.app.ui.components.Overline
import com.healthtracker.app.ui.components.ScreenTitle
import com.healthtracker.app.ui.components.SecondaryButton
import com.healthtracker.app.ui.components.SectionHeader
import com.healthtracker.app.ui.components.TileGrid
import com.healthtracker.app.ui.components.WeightChart
import com.healthtracker.app.ui.theme.App
import java.time.LocalDate

private val ranges = listOf("2W" to 14L, "1M" to 30L, "3M" to 90L, "All" to 100_000L)

@Composable
fun WeightScreen(vm: HealthViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    val h = st ?: return
    val c = App.colors
    val s = h.s
    var range by rememberSaveable { mutableIntStateOf(1) }
    var waistOpen by remember { mutableStateOf(false) }
    var deleteDay by remember { mutableStateOf<Long?>(null) }

    ScreenScroll {
        val dayN = h.todayDay - s.startDay + 1
        ScreenTitle("Weight", "Day $dayN · started ${Fmt.dayMonth(LocalDate.ofEpochDay(s.startDay))} at ${Fmt.kg(s.startWeight)} kg")

        AppCard {
            Text("7-day trend weight", style = MaterialTheme.typography.bodySmall, color = c.ink2)
            Text(h.trendNow?.let { "${Fmt.kg(it)} kg" } ?: "No weigh-ins yet", style = MaterialTheme.typography.displaySmall, color = c.ink)
            if (h.totalLost >= 0.1) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.SouthEast, contentDescription = null, tint = c.good, modifier = Modifier.size(18.dp))
                    Text("${Fmt.kg(h.totalLost)} kg lost since you started", style = MaterialTheme.typography.titleSmall, color = c.good)
                }
            }
        }

        AppCard {
            SectionHeader("Your trend")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ranges.forEachIndexed { i, (label, _) ->
                    FilterChip(
                        selected = range == i,
                        onClick = { range = i },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.soft, selectedLabelColor = c.primary),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendSwatch(c.dot, "Daily reading", round = true)
                LegendLine(c.data, "7-day trend")
            }
            val from = h.todayDay - ranges[range].second
            WeightChart(h.trendSeries.filter { it.day > from })
            Text(
                "Tap or drag across the chart to see a day. Daily readings jump around by up to a kilo from water and salt; the trend line is the number to watch.",
                style = MaterialTheme.typography.bodySmall, color = c.ink2,
            )
        }

        val rate = h.weeklyRate
        TileGrid(
            listOf(
                Triple("Weekly rate", rate?.let { if (it >= 0) "−${Fmt.kg(it)} kg" else "+${Fmt.kg(-it)} kg" } ?: "—", if (rate == null) "After 1 week of data" else "Average, last 4 weeks"),
                Triple("This week", h.weekChange?.let { "${Fmt.signedKg(it)} kg" } ?: "—", "vs 7 days ago"),
                Triple("BMI", Fmt.one(h.bmiNow), "Started at ${Fmt.one(h.bmiStart)}"),
                Triple("Days logged", "${h.loggedLast7} of 7", "Last 7 days"),
            )
        )

        HeroCard {
            val d = h.daysLeft
            Overline(if (d != null && d >= 0) "${if (h.isBirthdayGoal) "On your birthday" else "On goal day"} · $d days" else "Projection", c.heroMuted)
            val proj = h.projectedAtGoal
            Text(
                if (proj != null) "About ${Fmt.kg(proj)} kg" else "Not enough data yet",
                style = MaterialTheme.typography.headlineLarge, color = c.onHero,
            )
            Text(
                if (proj != null && rate != null) {
                    val reach = if (proj <= s.goalKg + 0.2) "you'll reach your ${Fmt.kg(s.goalKg)} kg goal." else "you'll be ${Fmt.kg(proj - s.goalKg)} kg from your ${Fmt.kg(s.goalKg)} kg goal."
                    "If you keep losing ${Fmt.kg(rate.coerceAtLeast(0.0))} kg a week, $reach Weight loss usually slows a little over time, so treat this as a best case."
                } else "Weigh in every morning for a week and your projection will appear here.",
                style = MaterialTheme.typography.bodyMedium, color = c.heroMuted,
            )
        }

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Straighten, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
                Text("Waist", style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.weight(1f))
            }
            val latest = h.waistLatest
            val first = h.waistFirst
            if (latest == null) {
                Text("Measure once a week. When the scale stalls, your waist often keeps shrinking.", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
            } else {
                Text("${Fmt.one(latest.cm)} cm", style = MaterialTheme.typography.headlineMedium, color = c.ink)
                if (first != null && first.day != latest.day) {
                    val d = latest.cm - first.cm
                    Text(
                        "${if (d <= 0) "−" else "+"}${Fmt.one(kotlin.math.abs(d))} cm since ${Fmt.dayMonth(LocalDate.ofEpochDay(first.day))}",
                        style = MaterialTheme.typography.titleSmall, color = if (d <= 0) c.good else c.ink2,
                    )
                }
                h.waists.sortedByDescending { it.day }.take(4).forEach { w ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(Fmt.dayMonth(LocalDate.ofEpochDay(w.day)), style = MaterialTheme.typography.bodyMedium, color = c.ink2, modifier = Modifier.weight(1f))
                        Text("${Fmt.one(w.cm)} cm", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                    }
                }
            }
            SecondaryButton("Add waist measurement", onClick = { waistOpen = true })
        }

        AppCard(spacing = 0.dp) {
            SectionHeader("Milestones")
            h.milestones.forEachIndexed { i, m ->
                val reached = h.milestoneReachedOn(m)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (reached != null) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, contentDescription = "Reached", tint = c.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Box(Modifier.size(28.dp).clip(CircleShape).border(2.dp, c.data, CircleShape))
                    }
                    Text("${m.label} · ${Fmt.kg(m.kg)} kg", style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f))
                    Text(
                        if (reached != null) "Done ${Fmt.dayMonth(reached)}" else "${Fmt.kg((h.currentKg - m.kg).coerceAtLeast(0.0))} kg to go",
                        style = MaterialTheme.typography.bodyMedium, color = c.ink2,
                    )
                }
                if (i < h.milestones.lastIndex) HairlineDivider()
            }
        }

        AppCard(spacing = 0.dp) {
            SectionHeader("Recent weigh-ins")
            val recent = h.weights.sortedByDescending { it.day }.take(14)
            if (recent.isEmpty()) Text("No weigh-ins yet.", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
            recent.forEach { w ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val d = LocalDate.ofEpochDay(w.day)
                    Text("${Fmt.weekday(d)} ${Fmt.dayMonth(d)}", style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f))
                    Text("${Fmt.kg(w.kg)} kg", style = MaterialTheme.typography.titleSmall, color = c.ink)
                    IconButton(onClick = { deleteDay = w.day }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete weigh-in for ${Fmt.dayMonth(d)}", tint = c.ink2)
                    }
                }
            }
        }
    }

    if (waistOpen) {
        NumberDialog(
            title = "Waist", label = "Waist", initial = h.waistLatest?.cm, suffix = "cm", range = 40.0..250.0,
            help = "Measure around your belly button, relaxed, after breathing out.",
            onDismiss = { waistOpen = false },
        ) { vm.logWaist(it); waistOpen = false }
    }
    deleteDay?.let { d ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteDay = null },
            title = { Text("Delete this weigh-in?") },
            text = { Text("${Fmt.fullDate(LocalDate.ofEpochDay(d))} will be removed from your trend.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { vm.deleteWeight(d); deleteDay = null }) { Text("Delete") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { deleteDay = null }) { Text("Cancel") } },
        )
    }
}
