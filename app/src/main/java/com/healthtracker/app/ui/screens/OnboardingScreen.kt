package com.healthtracker.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.healthtracker.app.data.ActivityLevel
import com.healthtracker.app.data.Pace
import com.healthtracker.app.data.Sex
import com.healthtracker.app.data.UserSettings
import com.healthtracker.app.domain.Calc
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.ui.HealthViewModel
import com.healthtracker.app.ui.SetupChecklist
import com.healthtracker.app.ui.components.AppCard
import com.healthtracker.app.ui.components.DateDialog
import com.healthtracker.app.ui.components.InfoNote
import com.healthtracker.app.ui.components.Meter
import com.healthtracker.app.ui.components.PrimaryButton
import com.healthtracker.app.ui.components.SecondaryButton
import com.healthtracker.app.ui.components.Stepper
import com.healthtracker.app.ui.theme.App
import java.time.LocalDate
import kotlin.math.roundToInt

private const val STEPS = 8

@Composable
fun OnboardingScreen(vm: HealthViewModel) {
    val c = App.colors
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var birthDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var sex by rememberSaveable { mutableStateOf(Sex.MALE) }
    var heightText by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf(ActivityLevel.SEDENTARY) }
    var pace by rememberSaveable { mutableStateOf(Pace.STEADY) }
    var goalText by rememberSaveable { mutableStateOf("") }
    var waterMl by rememberSaveable { mutableIntStateOf(0) }
    var stepGoal by rememberSaveable { mutableIntStateOf(8000) }
    var datePicker by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val height = Fmt.parseNumber(heightText)?.takeIf { it in 120.0..230.0 }
    val weight = Fmt.parseNumber(weightText)?.takeIf { it in 30.0..350.0 }
    val nextBirthday = birthDay?.let { Calc.nextBirthday(it, today) }
    val daysToBirthday = nextBirthday?.let { it.toEpochDay() - today.toEpochDay() }

    fun draft(): UserSettings {
        val w = weight ?: 80.0
        val goal = Fmt.parseNumber(goalText) ?: w
        return UserSettings(
            name = name.trim(), birthDay = birthDay, sex = sex, heightCm = height ?: 175.0, startWeight = w,
            startDay = today.toEpochDay(), activity = activity, pace = pace, calcWeight = w,
            goalName = "your birthday", goalKg = goal, goalDay = nextBirthday?.toEpochDay(),
            goalStartDay = today.toEpochDay(), goalStartKg = w,
            waterGoalMl = if (waterMl > 0) waterMl else Calc.defaultWaterMl(w), stepGoal = stepGoal,
        )
    }

    val canNext = when (step) {
        1 -> name.isNotBlank()
        2 -> birthDay != null
        3 -> height != null && weight != null
        5 -> Fmt.parseNumber(goalText)?.let { weight != null && it in 30.0..weight } ?: false
        else -> true
    }

    fun next() {
        if (step == 3 && weight != null) {
            if (waterMl == 0) waterMl = Calc.defaultWaterMl(weight)
        }
        if (step == 4 && weight != null && daysToBirthday != null && goalText.isBlank()) {
            goalText = Fmt.kg(Calc.suggestGoalKg(weight, pace, daysToBirthday.coerceAtLeast(14), height ?: 175.0))
        }
        if (step < STEPS - 1) step++ else vm.completeOnboarding(draft())
    }

    BackHandler(enabled = step > 0) { step-- }

    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            if (step > 0) {
                Meter(step / (STEPS - 1).toFloat(), height = 6.dp)
                Text("Step $step of ${STEPS - 1}", style = MaterialTheme.typography.bodySmall, color = c.ink2, modifier = Modifier.padding(top = 8.dp))
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (step) {
                    0 -> Welcome()
                    1 -> {
                        Question("What should we call you?", "Your name appears in your reminders and on the pause screen.")
                        OutlinedTextField(
                            name, { name = it.take(30) }, label = { Text("Your name") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (name.isNotBlank()) InfoNote("Your pause message will say: \"No ${name.trim()}, not till your birthday.\"")
                    }
                    2 -> {
                        Question("When's your birthday?", "Your birthday is your first goal date. We'll also use your age for the calorie maths.")
                        SecondaryButton(
                            birthDay?.let { Fmt.fullDate(LocalDate.ofEpochDay(it)) } ?: "Choose your birth date",
                            onClick = { datePicker = true }, icon = Icons.Rounded.Cake,
                        )
                        if (daysToBirthday != null) {
                            InfoNote(
                                if (daysToBirthday == 0L) "That's today. Happy birthday! Your countdown will start for next year."
                                else "$daysToBirthday days to your next birthday, on ${Fmt.longDate(nextBirthday!!)}."
                            )
                        }
                        Text("Sex", style = MaterialTheme.typography.titleSmall, color = c.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Sex.entries.forEach { sx -> Choice(sx.label, null, sex == sx, Modifier.weight(1f)) { sex = sx } }
                        }
                    }
                    3 -> {
                        Question("Your height and weight", "Weigh yourself in the morning, before breakfast, for the most accurate start.")
                        OutlinedTextField(
                            heightText, { heightText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                            label = { Text("Height") }, suffix = { Text("cm") }, singleLine = true,
                            supportingText = { Text("6 ft 2 in = 188 cm · 5 ft 10 in = 178 cm") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            weightText, { weightText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6) },
                            label = { Text("Current weight") }, suffix = { Text("kg") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                        )
                        if (height != null && weight != null) {
                            val bmi = Calc.bmi(weight, height)
                            InfoNote("Your BMI is ${Fmt.one(bmi)}. A BMI of 25 at your height is ${Fmt.kg(Calc.weightForBmi(25.0, height))} kg, but your first goal should be much closer than that.")
                        }
                    }
                    4 -> {
                        Question("How active are you now?", "Be honest. You can change it later as you get more active.")
                        ActivityLevel.entries.forEach { a -> Choice(a.label, a.detail, activity == a) { activity = a } }
                    }
                    5 -> {
                        Question("Your pace and birthday goal", "Steady is a good start: fast enough to see progress, gentle enough to keep up.")
                        Pace.entries.forEach { p ->
                            Choice(p.label, "${Fmt.int(Calc.dailyDeficit(p))} kcal a day below maintenance", pace == p) {
                                pace = p
                                if (weight != null && daysToBirthday != null) {
                                    goalText = Fmt.kg(Calc.suggestGoalKg(weight, p, daysToBirthday.coerceAtLeast(14), height ?: 175.0))
                                }
                            }
                        }
                        OutlinedTextField(
                            goalText, { goalText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6) },
                            label = { Text("Goal weight for your birthday") }, suffix = { Text("kg") }, singleLine = true,
                            supportingText = { Text("We suggested a realistic number. You can change it.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                        )
                        val d = draft()
                        AppCard {
                            Text("Your daily calorie target", style = MaterialTheme.typography.bodySmall, color = c.ink2)
                            Text("${Fmt.int(Calc.calorieTarget(d, today))} kcal", style = MaterialTheme.typography.headlineMedium, color = c.ink)
                            Text(
                                "Maintenance is about ${Fmt.int(Calc.maintenance(d, today))} kcal. It updates every 5 kg you lose. Protein target: ${Calc.proteinTarget(d.heightCm)} g a day.",
                                style = MaterialTheme.typography.bodyMedium, color = c.ink2,
                            )
                        }
                    }
                    6 -> {
                        Question("Water and steps", "Start where you can, then build up. You can change these any time.")
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Water a day", style = MaterialTheme.typography.titleSmall, color = c.ink)
                                    Text("Reminders every 90 min", style = MaterialTheme.typography.bodySmall, color = c.ink2)
                                }
                                Stepper("${Fmt.litres(waterMl)} L", { waterMl = (waterMl - 250).coerceAtLeast(1000) }, { waterMl = (waterMl + 250).coerceAtMost(6000) }, "water")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Steps a day", style = MaterialTheme.typography.titleSmall, color = c.ink)
                                    Text("Add about 1,000 a week", style = MaterialTheme.typography.bodySmall, color = c.ink2)
                                }
                                Stepper(Fmt.int(stepGoal), { stepGoal = (stepGoal - 500).coerceAtLeast(2000) }, { stepGoal = (stepGoal + 500).coerceAtMost(25000) }, "steps")
                            }
                        }
                        InfoNote("Before you start, it's worth seeing a doctor for a blood test: HbA1c, cholesterol, thyroid and liver. It gives you a \"before\" to compare against.")
                    }
                    7 -> {
                        Question("Set up your phone", "Samsung phones are strict about background apps. Turn these on so everything works.")
                        AppCard { SetupChecklist() }
                        Text("You can do this later in Plan too.", style = MaterialTheme.typography.bodySmall, color = c.ink2)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) SecondaryButton("Back", onClick = { step-- }, modifier = Modifier.weight(1f))
                PrimaryButton(
                    when (step) { 0 -> "Let's start"; STEPS - 1 -> "Start my plan"; else -> "Next" },
                    onClick = ::next, enabled = canNext, modifier = Modifier.weight(if (step > 0) 2f else 1f),
                )
            }
        }
    }

    if (datePicker) {
        DateDialog(birthDay, onDismiss = { datePicker = false }, onConfirm = { birthDay = it; datePicker = false })
    }
}

@Composable
private fun Welcome() {
    val c = App.colors
    Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(c.primary), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(40.dp))
    }
    Text("Let's build your plan", style = MaterialTheme.typography.headlineLarge, color = c.ink)
    Text(
        "A structured, honest way to get fit. It takes about two minutes to set up.",
        style = MaterialTheme.typography.bodyLarge, color = c.ink2,
    )
    Feature(Icons.Rounded.MonitorWeight, "Daily weigh-in, with a trend line", "So one bad day never shakes you.")
    Feature(Icons.Rounded.WaterDrop, "Water, food and steps", "Reminders, Indian foods and saved meals.")
    Feature(Icons.Rounded.PauseCircle, "A pause before Swiggy and Zomato", "Your own message, and a 10-second wait.")
    Feature(Icons.Rounded.Insights, "Analytics and a weekly review", "One clear focus for each week.")
}

@Composable
private fun Feature(icon: ImageVector, title: String, sub: String) {
    val c = App.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.soft), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.primary)
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = c.ink2)
        }
    }
}

@Composable
private fun Question(title: String, sub: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = App.colors.ink)
        Text(sub, style = MaterialTheme.typography.bodyLarge, color = App.colors.ink2)
    }
}

@Composable
private fun Choice(title: String, sub: String?, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = App.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (selected) c.soft else c.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.primary else c.hairline, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = c.ink2)
        }
        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = c.primary)
    }
}
