package com.healthtracker.app.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthtracker.app.BuildConfigInfo
import com.healthtracker.app.data.ActivityLevel
import com.healthtracker.app.data.KnownFoodApps
import com.healthtracker.app.data.Pace
import com.healthtracker.app.data.Sex
import com.healthtracker.app.domain.Calc
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.pause.PauseActivity
import com.healthtracker.app.ui.HealthViewModel
import com.healthtracker.app.ui.SetupChecklist
import com.healthtracker.app.ui.components.AppCard
import com.healthtracker.app.ui.components.DateDialog
import com.healthtracker.app.ui.components.HairlineDivider
import com.healthtracker.app.ui.components.NumberDialog
import com.healthtracker.app.ui.components.OptionsDialog
import com.healthtracker.app.ui.components.PrimaryButton
import com.healthtracker.app.ui.components.ScreenTitle
import com.healthtracker.app.ui.components.SecondaryButton
import com.healthtracker.app.ui.components.SectionHeader
import com.healthtracker.app.ui.components.Stepper
import com.healthtracker.app.ui.components.TextDialog
import com.healthtracker.app.ui.components.TimeDialog
import com.healthtracker.app.ui.components.ToggleRow
import com.healthtracker.app.ui.components.ValueRow
import com.healthtracker.app.ui.theme.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private enum class Edit {
    NAME, BIRTHDAY, SEX, HEIGHT, START, ACTIVITY, PACE, GOAL_KG, GOAL_NAME, GOAL_DATE, CALORIES,
    WEIGH_TIME, WATER_START, WATER_END, WATER_EVERY, MESSAGE, WAIT, APPS, IMPORT, MODE, ORDER_VALUE, GLASS, LOCK_START, LOCK_END, COACH_KEY, COACH_LIMIT, COACH_MODEL,
}

@Composable
fun PlanScreen(vm: HealthViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    val h = st ?: return
    val s = h.s
    val c = App.colors
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var edit by remember { mutableStateOf<Edit?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val json = vm.exportJson()
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            vm.showMessage(if (ok) "Backup saved." else "Couldn't save the backup.")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
            }
            if (text == null) vm.showMessage("Couldn't read that file.") else { pendingImport = text; edit = Edit.IMPORT }
        }
    }

    ScreenScroll {
        ScreenTitle("Your plan", "Set it once, adjust it any time")

        AppCard {
            SectionHeader("Phone setup")
            Text("Turn these on so reminders and the food-app pause work on your Samsung.", style = MaterialTheme.typography.bodyMedium, color = c.ink2)
            SetupChecklist()
        }

        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp), spacing = 0.dp) {
            SectionHeader("Profile & goal")
            ValueRow("Name", s.displayName, onClick = { edit = Edit.NAME })
            HairlineDivider()
            ValueRow("Birthday", s.birthDay?.let { Fmt.fullDate(LocalDate.ofEpochDay(it)) } ?: "Add", onClick = { edit = Edit.BIRTHDAY })
            HairlineDivider()
            ValueRow("Sex", s.sex.label, sub = "Used for the calorie formula", onClick = { edit = Edit.SEX })
            HairlineDivider()
            ValueRow("Height", "${Fmt.int(s.heightCm)} cm", onClick = { edit = Edit.HEIGHT })
            HairlineDivider()
            ValueRow("Starting weight", "${Fmt.kg(s.startWeight)} kg", sub = "Started ${Fmt.dayMonth(LocalDate.ofEpochDay(s.startDay))}", onClick = { edit = Edit.START })
            HairlineDivider()
            ValueRow("Activity", s.activity.label, sub = s.activity.detail, onClick = { edit = Edit.ACTIVITY })
            HairlineDivider()
            ValueRow("Pace", s.pace.label, onClick = { edit = Edit.PACE })
            HairlineDivider()
            ValueRow("Goal", if (h.isBirthdayGoal) "Your birthday" else s.goalName, onClick = { edit = Edit.GOAL_NAME })
            HairlineDivider()
            ValueRow("Goal date", h.goalDate?.let { Fmt.fullDate(it) } ?: "Add", onClick = { edit = Edit.GOAL_DATE })
            HairlineDivider()
            ValueRow("Goal weight", "${Fmt.kg(s.goalKg)} kg", onClick = { edit = Edit.GOAL_KG })
            Text(
                "When the goal date passes, the next goal starts by itself, so the countdown never just ends.",
                style = MaterialTheme.typography.bodySmall, color = c.ink2, modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp), spacing = 0.dp) {
            SectionHeader("Daily targets")
            ValueRow(
                "Calories", "${Fmt.int(h.calorieTarget)} kcal",
                sub = if (s.calorieOverride != null) "Custom target" else "Automatic: maintenance ${Fmt.int(h.maintenance)} − ${Fmt.int(Calc.dailyDeficit(s.pace))} for your pace",
                onClick = { edit = Edit.CALORIES },
            )
            HairlineDivider()
            ValueRow("Protein", "${h.proteinTarget} g", sub = "1.6 g per kg of a healthy weight for your height")
            HairlineDivider()
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Water", style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f))
                Stepper(
                    "${Fmt.litres(s.waterGoalMl)} L",
                    onMinus = { vm.updateSettings { it.copy(waterGoalMl = (it.waterGoalMl - 250).coerceAtLeast(1000)) } },
                    onPlus = { vm.updateSettings { it.copy(waterGoalMl = (it.waterGoalMl + 250).coerceAtMost(6000)) } },
                    label = "water",
                )
            }
            HairlineDivider()
            ValueRow("Glass size", "${s.glassMl} ml", sub = "What one tap of Drank adds, in reminders and on Today", onClick = { edit = Edit.GLASS })
            HairlineDivider()
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Steps", style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f))
                Stepper(
                    Fmt.int(s.stepGoal),
                    onMinus = { vm.updateSettings { it.copy(stepGoal = (it.stepGoal - 500).coerceAtLeast(2000)) } },
                    onPlus = { vm.updateSettings { it.copy(stepGoal = (it.stepGoal + 500).coerceAtMost(25000)) } },
                    label = "steps",
                )
            }
        }

        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp), spacing = 0.dp) {
            SectionHeader("Reminders")
            ToggleRow("Water", "Every ${s.waterEveryMin} min · ${Fmt.clock(s.waterStartMin)} to ${Fmt.clock(s.waterEndMin)}", s.waterRemind) { v ->
                vm.updateSettings { it.copy(waterRemind = v) }
            }
            if (s.waterRemind) {
                ValueRow("Start", Fmt.clock(s.waterStartMin), onClick = { edit = Edit.WATER_START })
                ValueRow("End", Fmt.clock(s.waterEndMin), onClick = { edit = Edit.WATER_END })
                ValueRow("Every", "${s.waterEveryMin} min", onClick = { edit = Edit.WATER_EVERY })
            }
            HairlineDivider()
            ToggleRow("Water pop-ups", "A big card on screen for 12 seconds, and a full-screen alert that wakes a locked phone", s.waterPopup) { v ->
                vm.updateSettings { it.copy(waterPopup = v) }
            }
            HairlineDivider()
            ToggleRow("Morning weigh-in", "${Fmt.clock(s.weighMin)}, one follow-up if you forget. Type your weight right in the notification.", s.weighRemind) { v ->
                vm.updateSettings { it.copy(weighRemind = v) }
            }
            if (s.weighRemind) ValueRow("Time", Fmt.clock(s.weighMin), onClick = { edit = Edit.WEIGH_TIME })
            HairlineDivider()
            ToggleRow("Log your meals", "10 AM, 2:30 PM and 9:30 PM, only if you haven't logged recently", s.mealRemind) { v ->
                vm.updateSettings { it.copy(mealRemind = v) }
            }
            HairlineDivider()
            ToggleRow("Protein check", "4:30 PM, only if you're under half your protein target", s.proteinNudge) { v ->
                vm.updateSettings { it.copy(proteinNudge = v) }
            }
            HairlineDivider()
            ToggleRow("Weekly review", "Sunday 9 AM", s.weeklyRemind) { v -> vm.updateSettings { it.copy(weeklyRemind = v) } }
        }

        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp), spacing = 0.dp) {
            SectionHeader("Food-app pause")
            ToggleRow("Pause before food apps", "Shows your message and a short wait", s.pauseEnabled) { v ->
                vm.updateSettings { it.copy(pauseEnabled = v) }
            }
            HairlineDivider()
            ValueRow("Strictness", s.pauseMode.label, sub = s.pauseMode.detail, onClick = { edit = Edit.MODE })
            HairlineDivider()
            ToggleRow(
                "Night lockdown",
                "Lockdown from ${Fmt.clock(s.lockStartMin)} to ${Fmt.clock(s.lockEndMin)}, whatever your strictness. Late nights are when most orders happen.",
                s.nightLockdown,
            ) { v -> vm.updateSettings { it.copy(nightLockdown = v) } }
            if (s.nightLockdown) {
                ValueRow("Starts", Fmt.clock(s.lockStartMin), onClick = { edit = Edit.LOCK_START })
                ValueRow("Ends", Fmt.clock(s.lockEndMin), onClick = { edit = Edit.LOCK_END })
                ToggleRow("Also all day on weekends", "Saturday and Sunday, until Monday morning", s.weekendLockdown) { v ->
                    vm.updateSettings { it.copy(weekendLockdown = v) }
                }
            }
            HairlineDivider()
            ValueRow("Your message", s.pauseMessage, onClick = { edit = Edit.MESSAGE })
            HairlineDivider()
            ValueRow("Usual order value", "₹${Fmt.int(s.orderValue)}", sub = "Each time you back out, this counts as money saved", onClick = { edit = Edit.ORDER_VALUE })
            HairlineDivider()
            val appNames = s.pauseApps.map { p -> KnownFoodApps.find { it.pkg == p }?.name ?: labelFor(ctx.packageManager, p) }.sorted()
            ValueRow("Apps", if (appNames.isEmpty()) "None" else appNames.joinToString(", "), onClick = { edit = Edit.APPS })
            HairlineDivider()
            ValueRow("Wait before continuing", "${s.pauseWaitSec} seconds", onClick = { edit = Edit.WAIT })
            HairlineDivider()
            ToggleRow("Junk-food check", "Same message when you log junk food in this app", s.junkCheck) { v ->
                vm.updateSettings { it.copy(junkCheck = v) }
            }
            HairlineDivider()
            ToggleRow(
                "One planned meal out a week",
                "Lets you order once a week on purpose. It's logged and counts toward your weekly calories.",
                s.plannedMealsPerWeek > 0,
            ) { v -> vm.updateSettings { it.copy(plannedMealsPerWeek = if (v) 1 else 0) } }
            Spacer(Modifier.padding(4.dp))
            SecondaryButton("Preview the pause screen", icon = Icons.Rounded.PauseCircle, onClick = {
                ctx.startActivity(Intent(ctx, PauseActivity::class.java))
            })
            Spacer(Modifier.padding(4.dp))
        }

        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp), spacing = 0.dp) {
            SectionHeader("Coach (AI)")
            if (s.coachKey.isBlank()) {
                Text("Not set up yet. Open the Coach tab to add your free Gemini key.", style = MaterialTheme.typography.bodyMedium, color = c.ink2, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                ToggleRow("Coach", "Only a short summary of your app data is sent with each question", s.coachEnabled) { v ->
                    vm.updateSettings { it.copy(coachEnabled = v) }
                }
                HairlineDivider()
                ValueRow("API key", "•••• " + s.coachKey.takeLast(4), sub = "Saved only on this phone", onClick = { edit = Edit.COACH_KEY })
                HairlineDivider()
                ValueRow("Daily limit", "${s.coachDailyLimit} questions", sub = "Keeps your free quota lasting. Tips and reviews count too.", onClick = { edit = Edit.COACH_LIMIT })
                HairlineDivider()
                ValueRow("Model", s.coachModel.ifBlank { "Automatic" }, sub = s.coachWorkingModel.takeIf { it.isNotBlank() }?.let { "Last used: $it" }, onClick = { edit = Edit.COACH_MODEL })
            }
        }

        AppCard {
            SectionHeader("Your data")
            Text(
                "Everything stays on this phone. Save a backup before installing a new version, so nothing is lost if you ever need to reinstall.",
                style = MaterialTheme.typography.bodyMedium, color = c.ink2,
            )
            PrimaryButton("Save a backup", icon = Icons.Rounded.Download, onClick = {
                exportLauncher.launch("health-tracker-backup-${LocalDate.now()}.json")
            })
            SecondaryButton("Restore from a backup", icon = Icons.Rounded.Upload, onClick = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            })
        }

        Text(
            "Health Tracker ${BuildConfigInfo.versionName(ctx)} · no account, no ads, no servers.",
            style = MaterialTheme.typography.bodySmall, color = c.ink2,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

    val close = { edit = null }
    when (edit) {
        Edit.NAME -> TextDialog("Your name", "Name", s.name, onDismiss = close) { v -> vm.updateSettings { it.copy(name = v) }; close() }
        Edit.BIRTHDAY -> DateDialog(s.birthDay, onDismiss = close, onConfirm = { day ->
            vm.updateSettings { cur ->
                val next = Calc.nextBirthday(day, LocalDate.now()).toEpochDay()
                if (cur.goalName == "your birthday" || cur.goalDay == null) {
                    cur.copy(
                        birthDay = day, goalName = "your birthday", goalDay = next,
                        goalStartDay = cur.goalStartDay ?: LocalDate.now().toEpochDay(),
                        goalStartKg = cur.goalStartKg ?: h.currentKg,
                    )
                } else cur.copy(birthDay = day)
            }
            close()
        })
        Edit.SEX -> OptionsDialog("Sex", Sex.entries, s.sex, { it.label }, onDismiss = close) { v -> vm.updateSettings { it.copy(sex = v) }; close() }
        Edit.HEIGHT -> NumberDialog("Height", "Height", s.heightCm, "cm", decimals = false, range = 120.0..230.0,
            help = "6 ft 2 in is 188 cm. 1 inch is 2.54 cm.", onDismiss = close) { v -> vm.updateSettings { it.copy(heightCm = v) }; close() }
        Edit.START -> NumberDialog("Starting weight", "Weight", s.startWeight, "kg", range = 30.0..350.0, onDismiss = close) { v ->
            vm.updateSettings { it.copy(startWeight = v) }; close()
        }
        Edit.ACTIVITY -> OptionsDialog("Activity", ActivityLevel.entries, s.activity, { it.label }, { it.detail }, onDismiss = close) { v ->
            vm.updateSettings { it.copy(activity = v) }; close()
        }
        Edit.PACE -> OptionsDialog("Pace", Pace.entries, s.pace, { it.label }, { "${Fmt.int(Calc.dailyDeficit(it))} kcal a day below maintenance" }, onDismiss = close) { v ->
            vm.updateSettings { it.copy(pace = v) }; close()
        }
        Edit.GOAL_KG -> NumberDialog("Goal weight", "Goal", s.goalKg, "kg", range = 30.0..350.0, onDismiss = close) { v ->
            vm.updateSettings { it.copy(goalKg = v) }; close()
        }
        Edit.GOAL_NAME -> TextDialog(
            "Goal name", "Goal", if (h.isBirthdayGoal) "your birthday" else s.goalName,
            help = "Keep \"your birthday\" for the birthday countdown, or name your own goal, like \"Diwali\" or \"BMI under 30\".",
            onDismiss = close,
        ) { v -> vm.updateSettings { it.copy(goalName = v.ifBlank { "your birthday" }) }; close() }
        Edit.GOAL_DATE -> DateDialog(s.goalDay ?: LocalDate.now().plusMonths(2).toEpochDay(), onDismiss = close, onConfirm = { day ->
            vm.updateSettings { it.copy(goalDay = day, goalStartDay = it.goalStartDay ?: LocalDate.now().toEpochDay(), goalStartKg = it.goalStartKg ?: h.currentKg) }
            close()
        })
        Edit.CALORIES -> NumberDialog(
            "Daily calories", "Target", (s.calorieOverride ?: h.calorieTarget).toDouble(), "kcal", decimals = false, range = 0.0..6000.0,
            help = "Your automatic target is ${Fmt.int(Calc.computedTarget(s, h.today))} kcal. Enter 0 to go back to automatic. Going below ${Fmt.int(Calc.minimumCalories(s.sex))} isn't recommended without a doctor.",
            onDismiss = close,
        ) { v -> vm.updateSettings { it.copy(calorieOverride = v.toInt().takeIf { n -> n > 0 }) }; close() }
        Edit.WEIGH_TIME -> TimeDialog("Weigh-in time", s.weighMin, onDismiss = close) { v -> vm.updateSettings { it.copy(weighMin = v) }; close() }
        Edit.WATER_START -> TimeDialog("First water reminder", s.waterStartMin, onDismiss = close) { v -> vm.updateSettings { it.copy(waterStartMin = v) }; close() }
        Edit.WATER_END -> TimeDialog("Last water reminder", s.waterEndMin, onDismiss = close) { v -> vm.updateSettings { it.copy(waterEndMin = v) }; close() }
        Edit.WATER_EVERY -> OptionsDialog("Remind me every", listOf(45, 60, 90, 120, 180), s.waterEveryMin, { "$it minutes" }, onDismiss = close) { v ->
            vm.updateSettings { it.copy(waterEveryMin = v) }; close()
        }
        Edit.MESSAGE -> TextDialog(
            "Pause message", "Message", s.customPauseMessage ?: "",
            help = "Leave it empty to use \"No ${s.displayName}, not till ${s.goalName}.\"",
            onDismiss = close,
        ) { v -> vm.updateSettings { it.copy(customPauseMessage = v.ifBlank { null }) }; close() }
        Edit.COACH_KEY -> TextDialog(
            "Gemini API key", "New key", "",
            help = "Paste a new key from Google AI Studio. Leave it empty and save to remove the key from this phone.",
            onDismiss = close,
        ) { v -> vm.updateSettings { it.copy(coachKey = v.trim()) }; close() }
        Edit.COACH_LIMIT -> OptionsDialog("Coach questions per day", listOf(10, 25, 50, 100), s.coachDailyLimit, { "$it a day" }, onDismiss = close) { v ->
            vm.updateSettings { it.copy(coachDailyLimit = v) }; close()
        }
        Edit.COACH_MODEL -> TextDialog(
            "Gemini model", "Model name", s.coachModel,
            help = "Leave empty for automatic (the app tries current Flash models). Or enter one from Google AI Studio, like gemini-2.5-flash.",
            onDismiss = close,
        ) { v -> vm.updateSettings { it.copy(coachModel = v.trim(), coachWorkingModel = "") }; close() }
        Edit.LOCK_START -> TimeDialog("Lockdown starts", s.lockStartMin, onDismiss = close) { v -> vm.updateSettings { it.copy(lockStartMin = v) }; close() }
        Edit.LOCK_END -> TimeDialog("Lockdown ends", s.lockEndMin, onDismiss = close) { v -> vm.updateSettings { it.copy(lockEndMin = v) }; close() }
        Edit.GLASS -> OptionsDialog("Glass size", listOf(150, 200, 250, 300, 350, 400, 500, 750, 1000), s.glassMl, { "$it ml" }, onDismiss = close) { v ->
            vm.updateSettings { it.copy(glassMl = v) }; close()
        }
        Edit.MODE -> OptionsDialog("Strictness", com.healthtracker.app.data.PauseMode.entries, s.pauseMode, { it.label }, { it.detail }, onDismiss = close) { v ->
            vm.updateSettings { it.copy(pauseMode = v) }; close()
        }
        Edit.ORDER_VALUE -> NumberDialog("Usual order value", "Amount", s.orderValue.toDouble(), "₹", decimals = false, range = 50.0..5000.0, onDismiss = close) { v ->
            vm.updateSettings { it.copy(orderValue = v.toInt()) }; close()
        }
        Edit.WAIT -> OptionsDialog("Wait before continuing", listOf(5, 10, 20, 30), s.pauseWaitSec, { "$it seconds" }, onDismiss = close) { v ->
            vm.updateSettings { it.copy(pauseWaitSec = v) }; close()
        }
        Edit.APPS -> AppsDialog(selected = s.pauseApps, onDismiss = close) { set -> vm.updateSettings { it.copy(pauseApps = set) }; close() }
        Edit.IMPORT -> AlertDialog(
            onDismissRequest = { pendingImport = null; close() },
            title = { Text("Restore this backup?") },
            text = { Text("This replaces everything in the app with the backup's data.") },
            confirmButton = { TextButton(onClick = { pendingImport?.let { vm.importJson(it) }; pendingImport = null; close() }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { pendingImport = null; close() }) { Text("Cancel") } },
        )
        null -> {}
    }
}

private fun labelFor(pm: PackageManager, pkg: String): String =
    runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)

@Composable
private fun AppsDialog(selected: Set<String>, onDismiss: () -> Unit, onSave: (Set<String>) -> Unit) {
    val ctx = LocalContext.current
    var chosen by remember { mutableStateOf(selected) }
    var showAll by remember { mutableStateOf(false) }
    val installed = remember {
        val pm = ctx.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != ctx.packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
    }
    val known = KnownFoodApps.map { it.pkg to it.name }
    val list = if (showAll) known + installed.filter { i -> known.none { it.first == i.first } } else
        known + installed.filter { i -> i.first in selected && known.none { it.first == i.first } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apps that get a pause") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(list, key = { it.first }) { (pkg, name) ->
                    val isInstalled = installed.any { it.first == pkg }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Checkbox) {
                            chosen = if (pkg in chosen) chosen - pkg else chosen + pkg
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = pkg in chosen, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                            if (!isInstalled) Text("Not installed", style = MaterialTheme.typography.bodySmall, color = App.colors.ink2)
                        }
                    }
                }
                if (!showAll) {
                    item {
                        TextButton(onClick = { showAll = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add another app")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(chosen) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
