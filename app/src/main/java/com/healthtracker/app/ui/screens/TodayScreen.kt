package com.healthtracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.domain.HealthState
import com.healthtracker.app.reminders.ReminderScheduler
import com.healthtracker.app.ui.HealthViewModel
import com.healthtracker.app.ui.components.AppCard
import com.healthtracker.app.ui.components.ButtonShape
import com.healthtracker.app.ui.components.HeroCard
import com.healthtracker.app.ui.components.InfoNote
import com.healthtracker.app.ui.components.Meter
import com.healthtracker.app.ui.components.NumberDialog
import com.healthtracker.app.ui.components.Overline
import com.healthtracker.app.ui.components.PrimaryButton
import com.healthtracker.app.ui.components.Ring
import com.healthtracker.app.ui.components.ScreenTitle
import com.healthtracker.app.ui.components.SecondaryButton
import com.healthtracker.app.ui.components.SectionHeader
import com.healthtracker.app.ui.theme.App
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

@Composable
fun TodayScreen(vm: HealthViewModel, onAddFood: () -> Unit, onOpenTab: (String) -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val h = st ?: return
    val s = h.s

    ScreenScroll {
        val hour = LocalTime.now().hour
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        ScreenTitle("$greeting, ${s.displayName}", Fmt.longDate(h.today))

        s.lastGoalResult?.let { r ->
            DismissibleNote(Icons.Rounded.EmojiEvents, r + " Your next goal has started: ${s.goalName}, ${Fmt.kg(s.goalKg)} kg.") { vm.dismissGoalResult() }
        }
        s.recalcNotice?.let { r -> DismissibleNote(Icons.Rounded.Info, r) { vm.dismissRecalcNotice() } }
        if (h.missedYesterday) {
            InfoNote("Never miss twice. Yesterday is done. Log one thing today and you're back on track.", Icons.Rounded.Info)
        }

        GoalCard(h, onOpenTab)
        MinimumDayCard(h, vm)
        WeightCard(h, vm, onOpenTab)
        CaloriesCard(h, onAddFood, onOpenTab)
        WaterCard(h, vm)
        ActivityCard(h, onOpenTab)
        if (h.waistDue) WaistCard(h, vm)
    }
}

@Composable
private fun DismissibleNote(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onDismiss: () -> Unit) {
    val c = App.colors
    AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.padding(top = 2.dp).size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = c.ink, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = c.ink2) }
        }
    }
}

@Composable
private fun GoalCard(h: HealthState, onOpenTab: (String) -> Unit) {
    val c = App.colors
    val s = h.s
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (h.isBirthdayGoal) Icon(Icons.Rounded.Cake, contentDescription = null, tint = c.heroMuted, modifier = Modifier.size(18.dp))
            Overline(if (h.isBirthdayGoal) "Birthday goal" else "Goal · ${s.goalName}", c.heroMuted)
        }
        val days = h.daysLeft
        if (days == null) {
            Text("Add your birthday to start the countdown.", style = MaterialTheme.typography.titleMedium, color = c.onHero)
            PrimaryButton("Add birthday in Plan", onClick = { onOpenTab("plan") }, container = c.onHero, content = c.hero)
            return@HeroCard
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (days == 0L) {
                Text(if (h.isBirthdayGoal) "Happy birthday!" else "It's goal day!", style = MaterialTheme.typography.headlineLarge, color = c.onHero)
            } else {
                Text("$days", style = MaterialTheme.typography.displaySmall, color = c.onHero)
                Text(if (days == 1L) "day to go" else "days to go", style = MaterialTheme.typography.titleLarge, color = c.onHero, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        Meter(h.goalProgress, color = c.heroFill, track = c.heroTrack)
        Row(Modifier.fillMaxWidth()) {
            Text(
                "${Fmt.kg(h.goalLost)} of ${Fmt.kg(h.goalToLose)} kg lost",
                style = MaterialTheme.typography.titleSmall, color = c.onHero, modifier = Modifier.weight(1f),
            )
            Text("Goal ${Fmt.kg(s.goalKg)} kg", style = MaterialTheme.typography.bodyMedium, color = c.heroMuted)
        }
        val proj = h.projectedAtGoal
        Text(
            when {
                h.currentKg <= s.goalKg -> "You've already reached this goal. Anything more is a bonus."
                proj != null && days > 0 -> "At your current pace you'll be about ${Fmt.kg(proj)} kg on ${if (h.isBirthdayGoal) "your birthday" else Fmt.dayMonth(h.goalDate!!)}."
                else -> "Your projection appears after a week of weigh-ins."
            },
            style = MaterialTheme.typography.bodyMedium, color = c.heroMuted,
        )
    }
}

@Composable
private fun MinimumDayCard(h: HealthState, vm: HealthViewModel) {
    val c = App.colors
    if (!h.isMinimumDay) {
        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Rough day?", style = MaterialTheme.typography.titleSmall, color = c.ink)
                    Text("Switch to a minimum day: 3 small things are enough.", style = MaterialTheme.typography.bodySmall, color = c.ink2)
                }
                TextButton(onClick = { vm.setMinimumDay(true) }) { Text("Minimum day", style = MaterialTheme.typography.titleSmall) }
            }
        }
        return
    }
    val steps = h.stepsToday
    AppCard {
        SectionHeader("Minimum day")
        Text("Do just these three. That counts as a full day.", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
        CheckLine("Weigh in", h.todayWeight != null)
        CheckLine("Log what you eat, honestly", h.todayFood.isNotEmpty())
        CheckLine(if (steps != null) "Walk 5,000 steps (${Fmt.int(steps)} so far)" else "Walk 5,000 steps", (steps ?: 0) >= 5000)
        TextButton(onClick = { vm.setMinimumDay(false) }) { Text("Back to a normal day") }
    }
}

@Composable
private fun CheckLine(text: String, done: Boolean) {
    val c = App.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (done) c.primary else c.soft),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Rounded.Check, contentDescription = "Done", tint = c.onPrimary, modifier = Modifier.size(16.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, color = c.ink)
    }
}

@Composable
private fun WeightCard(h: HealthState, vm: HealthViewModel, onOpenTab: (String) -> Unit) {
    val c = App.colors
    var editing by remember { mutableStateOf(false) }
    AppCard {
        SectionHeader("Weight", "Trends") { onOpenTab("weight") }
        val w = h.todayWeight
        if (w == null) {
            var text by remember { mutableStateOf("") }
            val focus = LocalFocusManager.current
            val value = Fmt.parseNumber(text)
            val valid = value != null && value in 30.0..350.0
            Text("Weigh in before breakfast for the most accurate trend.", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6) },
                    placeholder = { Text(h.latestWeight?.let { Fmt.kg(it.kg) } ?: "kg") },
                    suffix = { Text("kg") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (valid) { vm.logWeight(value!!); focus.clearFocus() } }),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { vm.logWeight(value!!); focus.clearFocus() },
                    enabled = valid,
                    shape = ButtonShape,
                    modifier = Modifier.heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
                ) { Text("Save") }
            }
            h.trendNow?.let { Text("Current trend: ${Fmt.kg(it)} kg", style = MaterialTheme.typography.bodySmall, color = c.ink2) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigStat("Today", "${Fmt.kg(w.kg)} kg", Modifier.weight(1f))
                BigStat("7-day trend", h.trendNow?.let { "${Fmt.kg(it)} kg" } ?: "—", Modifier.weight(1f))
            }
            val prev = h.previousWeight
            val logged = Instant.ofEpochMilli(w.loggedAt).atZone(ZoneId.systemDefault()).toLocalTime()
            val note = buildString {
                append("Logged at ${Fmt.clock(logged.hour * 60 + logged.minute)}.")
                if (prev != null) {
                    val d = w.kg - prev.kg
                    if (abs(d) >= 0.1) {
                        append(if (d > 0) " Up ${Fmt.kg(d)} kg from your last weigh-in" else " Down ${Fmt.kg(-d)} kg from your last weigh-in")
                        append(if (abs(d) <= 1.5) ", which is normal day-to-day change." else ".")
                    }
                }
                h.weekChange?.let { wc ->
                    if (wc <= -0.1) append(" Your trend is down ${Fmt.kg(-wc)} kg this week.")
                    else if (wc >= 0.1) append(" Your trend is up ${Fmt.kg(wc)} kg this week. Look at the weekly average, not single days.")
                }
            }
            InfoNote(note, Icons.Rounded.Check)
            TextButton(onClick = { editing = true }) { Text("Change today's weight") }
        }
    }
    if (editing) {
        NumberDialog(
            title = "Today's weight", label = "Weight", initial = h.todayWeight?.kg, suffix = "kg",
            range = 30.0..350.0, onDismiss = { editing = false },
        ) { vm.logWeight(it); editing = false }
    }
}

@Composable
private fun BigStat(label: String, value: String, modifier: Modifier = Modifier) {
    val c = App.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.ink2)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = c.ink)
    }
}

@Composable
private fun CaloriesCard(h: HealthState, onAddFood: () -> Unit, onOpenTab: (String) -> Unit) {
    val c = App.colors
    val target = h.calorieTarget
    val eaten = h.todayKcal
    val over = eaten > target
    AppCard {
        SectionHeader("Calories", "Food log") { onOpenTab("food") }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Ring(
                fraction = eaten / target.toFloat(),
                size = 124.dp, stroke = 12.dp,
                color = if (over) c.over else c.data,
                track = c.track,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Fmt.int(abs(target - eaten)), style = MaterialTheme.typography.headlineSmall, color = c.ink)
                    Text(if (over) "kcal over" else "kcal left", style = MaterialTheme.typography.bodySmall, color = c.ink2)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyValue("Eaten", Fmt.int(eaten))
                KeyValue("Target", Fmt.int(target))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    KeyValue("Protein", "${h.todayProtein.toInt()} / ${h.proteinTarget} g")
                    Meter((h.todayProtein / h.proteinTarget).toFloat(), height = 6.dp)
                }
            }
        }
        if (over) {
            Text(
                "Over today is fine. What counts is your weekly average, so aim a little lower tomorrow.",
                style = MaterialTheme.typography.bodySmall, color = c.ink2,
            )
        }
        PrimaryButton("Log food", onClick = onAddFood, icon = Icons.Rounded.Add)
    }
}

@Composable
private fun KeyValue(k: String, v: String) {
    val c = App.colors
    Row(Modifier.fillMaxWidth()) {
        Text(k, style = MaterialTheme.typography.bodyMedium, color = c.ink2, modifier = Modifier.weight(1f))
        Text(v, style = MaterialTheme.typography.titleSmall, color = c.ink)
    }
}

@Composable
private fun WaterCard(h: HealthState, vm: HealthViewModel) {
    val c = App.colors
    val s = h.s
    val goal = s.waterGoalMl
    val drunk = h.todayWaterMl
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = c.data, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Water", style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.weight(1f))
            Text("Goal ${Fmt.litres(goal)} L", style = MaterialTheme.typography.bodySmall, color = c.ink2)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${Fmt.litres(drunk)} L", style = MaterialTheme.typography.headlineLarge, color = c.ink)
            Text("of ${Fmt.litres(goal)} L", style = MaterialTheme.typography.bodyLarge, color = c.ink2, modifier = Modifier.padding(bottom = 4.dp))
        }
        Meter(drunk / goal.toFloat(), height = 10.dp)
        val note = when {
            drunk >= goal -> "Goal reached for today. Nice work."
            s.waterRemind -> {
                val next = Instant.ofEpochMilli(ReminderScheduler.nextWater(s, ZonedDateTime.now())).atZone(ZoneId.systemDefault())
                "${Fmt.litres(goal - drunk)} L left · next reminder ${Fmt.clock(next.hour * 60 + next.minute)}"
            }
            else -> "${Fmt.litres(goal - drunk)} L left"
        }
        Text(note, style = MaterialTheme.typography.bodyMedium, color = c.ink2)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Undo", onClick = { vm.undoWater() }, modifier = Modifier.weight(1f), enabled = drunk > 0)
            PrimaryButton("+250 ml", onClick = { vm.addWater(250) }, modifier = Modifier.weight(1.2f))
            PrimaryButton("+500", onClick = { vm.addWater(500) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActivityCard(h: HealthState, onOpenTab: (String) -> Unit) {
    val c = App.colors
    val steps = h.stepsToday
    val sleep = h.sleepLastNight
    AppCard {
        SectionHeader("Steps & sleep")
        if (steps == null && sleep == null) {
            Text(
                "Connect Health Connect in Plan to see your steps and sleep here. In Samsung Health, turn on syncing with Health Connect too.",
                style = MaterialTheme.typography.bodyMedium, color = c.ink2,
            )
            SecondaryButton("Set up in Plan", onClick = { onOpenTab("plan") })
            return@AppCard
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(steps?.let { Fmt.int(it) } ?: "—", style = MaterialTheme.typography.headlineSmall, color = c.ink)
            Text("  of ${Fmt.int(h.s.stepGoal)} steps", style = MaterialTheme.typography.bodyMedium, color = c.ink2, modifier = Modifier.padding(bottom = 3.dp))
        }
        Meter((steps ?: 0) / h.s.stepGoal.toFloat())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = c.ink2, modifier = Modifier.size(18.dp))
            Text(
                when {
                    sleep == null -> "No sleep recorded last night"
                    sleep < 360 -> "${Fmt.hours(sleep)} of sleep. Short nights make cravings stronger, so go easy on yourself today."
                    else -> "${Fmt.hours(sleep)} of sleep last night"
                },
                style = MaterialTheme.typography.bodyMedium, color = c.ink2,
            )
        }
    }
}

@Composable
private fun WaistCard(h: HealthState, vm: HealthViewModel) {
    val c = App.colors
    var open by remember { mutableStateOf(false) }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Straighten, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
            Text("Weekly waist check", style = MaterialTheme.typography.titleMedium, color = c.ink)
        }
        Text(
            "Measure around your belly button, relaxed, after breathing out. Your waist often keeps shrinking when the scale stalls.",
            style = MaterialTheme.typography.bodyMedium, color = c.ink2,
        )
        h.waistLatest?.let { Text("Last time: ${Fmt.one(it.cm)} cm", style = MaterialTheme.typography.bodySmall, color = c.ink2) }
        SecondaryButton("Add waist measurement", onClick = { open = true }, icon = Icons.Rounded.Straighten)
    }
    if (open) {
        NumberDialog(
            title = "Waist", label = "Waist", initial = h.waistLatest?.cm, suffix = "cm", range = 40.0..250.0,
            onDismiss = { open = false },
        ) { vm.logWaist(it); open = false }
    }
}
