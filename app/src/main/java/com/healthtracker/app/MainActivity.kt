package com.healthtracker.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.healthtracker.app.reminders.Notifications
import com.healthtracker.app.ui.HealthViewModel
import com.healthtracker.app.ui.screens.AddFoodScreen
import com.healthtracker.app.ui.screens.FoodScreen
import com.healthtracker.app.ui.screens.OnboardingScreen
import com.healthtracker.app.ui.screens.PlanScreen
import com.healthtracker.app.ui.screens.ReviewScreen
import com.healthtracker.app.ui.screens.TodayScreen
import com.healthtracker.app.ui.screens.WeightScreen
import com.healthtracker.app.ui.theme.App
import com.healthtracker.app.ui.theme.HealthTheme

class MainActivity : ComponentActivity() {
    private val vm: HealthViewModel by viewModels()
    private val requestedTab = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedTab.value = intent?.getStringExtra(Notifications.EXTRA_TAB)
        setContent {
            HealthTheme { AppRoot(vm, requestedTab) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedTab.value = intent.getStringExtra(Notifications.EXTRA_TAB)
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("today", "Today", Icons.Rounded.Home),
    Tab("weight", "Weight", Icons.Rounded.MonitorWeight),
    Tab("food", "Food", Icons.Rounded.Restaurant),
    Tab("review", "Review", Icons.Rounded.Insights),
    Tab("plan", "Plan", Icons.Rounded.Tune),
)

@Composable
private fun AppRoot(vm: HealthViewModel, requestedTab: MutableState<String?>) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings
    when {
        s == null -> Box(Modifier.fillMaxSize().background(App.colors.bg))
        !s.onboarded -> OnboardingScreen(vm)
        else -> MainScaffold(vm, requestedTab)
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainScaffold(vm: HealthViewModel, requestedTab: MutableState<String?>) {
    val c = App.colors
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val message by vm.message.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    LaunchedEffect(requestedTab.value) {
        val t = requestedTab.value
        if (t != null && tabs.any { it.route == t }) nav.goTab(t)
        requestedTab.value = null
    }
    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        snackbar.showSnackbar(m)
        vm.clearMessage()
    }

    Scaffold(
        containerColor = c.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (route != "addfood") {
                NavigationBar(containerColor = c.card, tonalElevation = 0.dp) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { nav.goTab(t.route) },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = c.primary,
                                selectedTextColor = c.primary,
                                indicatorColor = c.soft,
                                unselectedIconColor = c.ink2,
                                unselectedTextColor = c.ink2,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding)) {
            composable("today") { TodayScreen(vm, onAddFood = { nav.navigate("addfood") }, onOpenTab = { nav.goTab(it) }) }
            composable("weight") { WeightScreen(vm) }
            composable("food") { FoodScreen(vm, onAddFood = { nav.navigate("addfood") }) }
            composable("review") { ReviewScreen(vm) }
            composable("plan") { PlanScreen(vm) }
            composable("addfood") { AddFoodScreen(vm, onDone = { nav.popBackStack() }) }
        }
    }
}

