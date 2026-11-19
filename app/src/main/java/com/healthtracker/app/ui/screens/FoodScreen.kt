package com.healthtracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthtracker.app.data.FoodCatalog
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.domain.HealthState
import com.healthtracker.app.ui.HealthViewModel
import com.healthtracker.app.ui.components.AppCard
import com.healthtracker.app.ui.components.BarDatum
import com.healthtracker.app.ui.components.DayBarChart
import com.healthtracker.app.ui.components.HairlineDivider
import com.healthtracker.app.ui.components.LegendLine
import com.healthtracker.app.ui.components.LegendSwatch
import com.healthtracker.app.ui.components.Meter
import com.healthtracker.app.ui.components.PrimaryButton
import com.healthtracker.app.ui.components.ScreenTitle
import com.healthtracker.app.ui.components.SectionHeader
import com.healthtracker.app.ui.components.Segment
import com.healthtracker.app.ui.components.SegmentedBar
import com.healthtracker.app.ui.components.Stepper
import com.healthtracker.app.ui.theme.App
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
fun FoodScreen(vm: HealthViewModel, onAddFood: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val h = st ?: return
    val c = App.colors
    var picked by rememberSaveable { mutableStateOf(6) }

    ScreenScroll {
        ScreenTitle("Food & calories", "Target ${Fmt.int(h.calorieTarget)} kcal a day")

        AppCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Today", style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.weight(1f))
                Text("${Fmt.int(h.todayKcal)} of ${Fmt.int(h.calorieTarget)} kcal", style = MaterialTheme.typography.titleSmall, color = c.ink)
            }
            Meter(h.todayKcal / h.calorieTarget.toFloat(), color = if (h.todayKcal > h.calorieTarget) c.over else c.data)
            if (h.todayFood.isEmpty()) {
                Text("Nothing logged yet today.", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
            } else {
                Column {
                    h.todayFood.sortedBy { it.time }.forEach { f ->
                        val t = Instant.ofEpochMilli(f.time).atZone(ZoneId.systemDefault()).toLocalTime()
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(f.name + if (f.servings != 1.0) " × ${Fmt.one(f.servings).removeSuffix(".0")}" else "", style = MaterialTheme.typography.bodyLarge, color = c.ink)
                                Text("${Fmt.clock(t.hour * 60 + t.minute)} · ${f.protein.roundToInt()} g protein", style = MaterialTheme.typography.bodySmall, color = c.ink2)
                            }
                            Text("${Fmt.int(f.kcal)}", style = MaterialTheme.typography.titleSmall, color = c.ink)
                            IconButton(onClick = { vm.deleteFood(f.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${f.name}", tint = c.ink2)
                            }
                        }
                    }
                }
            }
            PrimaryButton("Add food", onClick = onAddFood, icon = Icons.Rounded.Add)
        }

        val r = h.review
        AppCard {
            Text("Weekly average · ${Fmt.dayMonth(r.from)}–${Fmt.dayMonth(r.to)}", style = MaterialTheme.typography.bodySmall, color = c.ink2)
            if (r.avgKcal == null) {
                Text("Log meals for a few days to see your weekly average.", style = MaterialTheme.typography.titleMedium, color = c.ink)
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(Fmt.int(r.avgKcal), style = MaterialTheme.typography.displaySmall, color = c.ink)
                    Text(" kcal", style = MaterialTheme.typography.titleLarge, color = c.ink2, modifier = Modifier.padding(bottom = 6.dp))
                }
                val diff = r.avgKcal - h.calorieTarget
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (diff <= 0) Icon(Icons.Rounded.Check, contentDescription = null, tint = c.good, modifier = Modifier.size(18.dp))
                    Text(
                        if (diff <= 0) "${Fmt.int(-diff)} under target" + if (r.overDays.isNotEmpty()) ", even with ${r.overDays.size} big ${if (r.overDays.size == 1) "day" else "days"}" else ""
                        else "${Fmt.int(diff)} over target on average",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (diff <= 0) c.good else c.ink,
                    )
                }
            }
        }

        AppCard {
            SectionHeader("Last 7 days")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendSwatch(c.data, "On target")
                LegendSwatch(c.over, "Over")
                LegendLine(c.ink2, "Target")
            }
            val days = h.last7Days
            val maxV = maxOf(h.calorieTarget * 1.35f, days.maxOf { it.kcal }.toFloat())
            DayBarChart(
                bars = days.mapIndexed { i, d ->
                    BarDatum(
                        label = if (i == days.lastIndex) "Today" else Fmt.weekday(d.date),
                        value = d.kcal.toFloat(),
                        color = if (d.kcal > h.calorieTarget) c.over else c.data,
                        valueLabel = Fmt.int(d.kcal),
                        description = "${Fmt.weekdayLong(d.date)}: ${Fmt.int(d.kcal)} kcal",
                    )
                },
                maxValue = maxV,
                target = h.calorieTarget.toFloat(),
                selected = picked,
                onSelect = { picked = it },
            )
            val d = days[picked.coerceIn(0, days.lastIndex)]
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.bg).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row {
                    Text("${Fmt.weekdayLong(d.date)} ${Fmt.dayMonth(d.date)}", style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
                    Text("${Fmt.int(d.kcal)} kcal", style = MaterialTheme.typography.titleSmall, color = c.ink)
                }
                if (d.foodLogged) {
                    val diff = d.kcal - h.calorieTarget
                    Text(
                        "${if (diff > 0) "${Fmt.int(diff)} over" else "${Fmt.int(-diff)} under"} target · Protein ${d.protein.roundToInt()} g",
                        style = MaterialTheme.typography.bodyMedium, color = c.ink2,
                    )
                    d.topItem?.let { Text("Biggest item: ${it.name} · ${Fmt.int(it.kcal)} kcal", style = MaterialTheme.typography.bodyMedium, color = c.ink) }
                } else {
                    Text("Nothing logged", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
                }
            }
        }

        MacroCard(h)
        SourcesCard(h)

        AppCard(contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.soft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = c.primary)
                }
                Column {
                    Text("Eating after 9 PM", style = MaterialTheme.typography.titleSmall, color = c.ink)
                    val nights = h.lateEntries.map { it.day }.distinct().size
                    Text(
                        if (nights == 0) "None in the last 7 days." else "$nights of 7 nights · ${Fmt.int(h.lateEntries.sumOf { it.kcal })} kcal in total",
                        style = MaterialTheme.typography.bodyMedium, color = c.ink2,
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroCard(h: HealthState) {
    val c = App.colors
    val (pk, ck, fk) = h.macroKcal
    val total = (pk + ck + fk)
    AppCard {
        SectionHeader("Where your calories come from")
        if (total <= 0) {
            Text("Log a few meals to see your protein, carbs and fat split.", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
            return@AppCard
        }
        val p = (pk / total).toFloat(); val cb = (ck / total).toFloat(); val f = (fk / total).toFloat()
        fun pct(x: Float) = "${(x * 100).roundToInt()}%"
        SegmentedBar(
            listOf(Segment(p, c.data, pct(p)), Segment(cb, c.over, pct(cb)), Segment(f, c.aqua, pct(f))),
            description = "Protein ${pct(p)}, carbs ${pct(cb)}, fat ${pct(f)} of calories in the last 7 days",
        )
        val (pg, cg, fg) = h.macroGramsPerDay
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroLegend("Protein", "$pg g a day · ${pct(p)}", c.data, Modifier.weight(1f))
            MacroLegend("Carbs", "$cg g a day · ${pct(cb)}", c.over, Modifier.weight(1f))
            MacroLegend("Fat", "$fg g a day · ${pct(f)}", c.aqua, Modifier.weight(1f))
        }
        Text(
            "Protein target is ${h.proteinTarget} g a day. Protein keeps you full for longer and protects muscle while you lose fat.",
            style = MaterialTheme.typography.bodyMedium, color = c.ink,
        )
    }
}

@Composable
private fun MacroLegend(name: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LegendSwatch(color, name)
        Text(value, style = MaterialTheme.typography.labelMedium, color = App.colors.ink)
    }
}

@Composable
private fun SourcesCard(h: HealthState) {
    val c = App.colors
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Top calorie sources", style = MaterialTheme.typography.titleMedium, color = c.ink)
            Text("Last 7 days · ${Fmt.int(h.last7Kcal)} kcal in total", style = MaterialTheme.typography.bodySmall, color = c.ink2)
        }
        if (h.topSources.isEmpty()) {
            Text("Nothing logged yet.", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
            return@AppCard
        }
        val max = h.topSources.first().kcal.coerceAtLeast(1)
        h.topSources.forEach { s ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(s.name + if (s.count > 1) " · ${s.count}×" else "", style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f))
                    Text("${Fmt.int(s.kcal)} · ${(s.share * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
                }
                Meter(s.kcal / max.toFloat(), height = 6.dp, track = c.soft)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Add food

private data class Pickable(
    val name: String, val serving: String, val kcal: Int, val protein: Double, val carbs: Double, val fat: Double,
    val junk: Boolean, val mine: Boolean,
)

@Composable
fun AddFoodScreen(vm: HealthViewModel, onDone: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val h = st ?: return
    val c = App.colors
    var query by rememberSaveable { mutableStateOf("") }
    var junkFor by remember { mutableStateOf<Pickable?>(null) }
    var servingsFor by remember { mutableStateOf<Pickable?>(null) }
    var custom by remember { mutableStateOf(false) }

    val mine = h.myFoods.map { Pickable(it.name, it.serving, it.kcal, it.protein, it.carbs, it.fat, it.junk, true) }
    val q = query.trim().lowercase()
    val mineShown = if (q.isEmpty()) mine.take(8) else mine.filter { it.name.lowercase().contains(q) }
    val catalog = FoodCatalog.search(query)
        .filter { cf -> mineShown.none { it.name == cf.name } }
        .map { Pickable(it.name, it.serving, it.kcal, it.protein, it.carbs, it.fat, it.junk, false) }

    fun pick(p: Pickable) {
        if (p.junk && h.s.junkCheck) junkFor = p else servingsFor = p
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Log food", style = MaterialTheme.typography.titleLarge, color = c.ink, modifier = Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Done", style = MaterialTheme.typography.titleSmall) }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("Search: roti, dal, biryani, chai…") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            item {
                TextButton(onClick = { custom = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Create your own food", style = MaterialTheme.typography.titleSmall)
                }
            }
            if (mineShown.isNotEmpty()) {
                item { ListLabel(if (q.isEmpty()) "Your usual foods" else "Your foods") }
                items(mineShown, key = { "m_" + it.name }) { FoodRow(it) { pick(it) } }
            }
            item { ListLabel(if (q.isEmpty()) "All foods" else "Results") }
            if (catalog.isEmpty()) {
                item {
                    Text(
                        "No match. Tap \"Create your own food\" to add it with its calories from the packet or restaurant menu.",
                        style = MaterialTheme.typography.bodyMedium, color = c.ink2, modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            items(catalog, key = { "c_" + it.name }) { FoodRow(it) { pick(it) } }
        }
    }

    junkFor?.let { p ->
        AlertDialog(
            onDismissRequest = { junkFor = null },
            title = { Text(h.s.pauseMessage) },
            text = {
                Text(
                    "${p.name} is marked as junk food. You can still log it: honest logging is what keeps the plan working.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = { TextButton(onClick = { junkFor = null; servingsFor = p }) { Text("Log it honestly") } },
            dismissButton = { TextButton(onClick = { junkFor = null }) { Text("Skip it") } },
        )
    }

    servingsFor?.let { p ->
        ServingsDialog(p, onDismiss = { servingsFor = null }) { n ->
            vm.addCustomFood(p.name, p.serving, p.kcal, p.protein, p.carbs, p.fat, p.junk, n)
            vm.showMessage("Added ${p.name} · ${Fmt.int(p.kcal * n)} kcal")
            servingsFor = null
        }
    }

    if (custom) {
        CustomFoodDialog(initialName = query.trim(), onDismiss = { custom = false }) { name, serving, kcal, pr, cb, ft, junk ->
            vm.addCustomFood(name, serving, kcal, pr, cb, ft, junk, 1.0)
            vm.showMessage("Added $name · ${Fmt.int(kcal)} kcal. It's saved in your foods.")
            custom = false
        }
    }
}

@Composable
private fun ListLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = App.colors.ink2,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun FoodRow(p: Pickable, onClick: () -> Unit) {
    val c = App.colors
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(onClick = onClick).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.name, style = MaterialTheme.typography.bodyLarge, color = c.ink)
                Text("${p.serving} · ${p.protein.roundToInt()} g protein", style = MaterialTheme.typography.bodySmall, color = c.ink2)
            }
            if (p.junk) {
                Text(
                    "Junk",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.ink,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.soft).padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text("${Fmt.int(p.kcal)} kcal", style = MaterialTheme.typography.titleSmall, color = c.ink)
        }
        HairlineDivider()
    }
}

@Composable
private fun ServingsDialog(p: Pickable, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var n by remember { mutableDoubleStateOf(1.0) }
    val c = App.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Serving: ${p.serving}", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("How many?", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Stepper(
                        value = Fmt.one(n).removeSuffix(".0"),
                        onMinus = { n = (n - 0.5).coerceAtLeast(0.5) },
                        onPlus = { n = (n + 0.5).coerceAtMost(10.0) },
                        label = "servings",
                    )
                }
                Text(
                    "${Fmt.int(p.kcal * n)} kcal · ${(p.protein * n).roundToInt()} g protein",
                    style = MaterialTheme.typography.titleMedium, color = c.ink,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(n) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CustomFoodDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Double, Double, Double, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var serving by remember { mutableStateOf("1 serving") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var junk by remember { mutableStateOf(false) }
    val k = kcal.toIntOrNull()
    val valid = name.isNotBlank() && k != null && k in 0..5000
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create your own food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(serving, { serving = it.take(40) }, label = { Text("Serving") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    kcal, { kcal = it.filter(Char::isDigit).take(4) }, label = { Text("Calories") }, suffix = { Text("kcal") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Protein" to protein, "Carbs" to carbs, "Fat" to fat).forEachIndexed { i, (label, v) ->
                        OutlinedTextField(
                            v,
                            { nv ->
                                val f = nv.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                                when (i) { 0 -> protein = f; 1 -> carbs = f; else -> fat = f }
                            },
                            label = { Text(label) }, suffix = { Text("g") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Junk food", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = junk, onCheckedChange = { junk = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onConfirm(
                    name.trim(), serving.trim().ifBlank { "1 serving" }, k!!,
                    protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0, junk,
                )
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
