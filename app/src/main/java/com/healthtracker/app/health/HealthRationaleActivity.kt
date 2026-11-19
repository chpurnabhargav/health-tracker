package com.healthtracker.app.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.healthtracker.app.ui.components.PrimaryButton
import com.healthtracker.app.ui.theme.App
import com.healthtracker.app.ui.theme.HealthTheme

/** Shown by Health Connect when you ask why this app wants your data. */
class HealthRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HealthTheme {
                val c = App.colors
                Column(
                    Modifier.fillMaxSize().background(c.bg).safeDrawingPadding().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("How Health Tracker uses your health data", style = MaterialTheme.typography.headlineMedium, color = c.ink)
                    Text(
                        "Health Tracker reads two things from Health Connect: your daily step count and how long you slept. " +
                            "They appear on your Today screen and in your weekly review.",
                        style = MaterialTheme.typography.bodyLarge, color = c.ink,
                    )
                    Text(
                        "Your data stays on this phone. The app has no account, no server and no ads, and it never writes to Health Connect.",
                        style = MaterialTheme.typography.bodyLarge, color = c.ink,
                    )
                    PrimaryButton("Got it", onClick = { finish() })
                }
            }
        }
    }
}
