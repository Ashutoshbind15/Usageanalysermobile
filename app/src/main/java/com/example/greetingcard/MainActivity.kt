package com.example.greetingcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.greetingcard.auth.GoogleAuth
import com.example.greetingcard.ui.screens.UsageStatsScreen
import com.example.greetingcard.ui.theme.GreetingCardTheme
import com.example.greetingcard.utils.getUsageStats
import com.example.greetingcard.utils.hasUsageStatsPermission
import com.example.greetingcard.utils.openUsageAccessSettings
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val googleAuth = GoogleAuth(context = LocalContext.current, lifecycleScope)

            GreetingCardTheme {
                Surface(color = MaterialTheme.colorScheme.background) { MainContent(googleAuth) }
            }
        }
    }
}

@Composable
fun MainContent(googleAuth: GoogleAuth) {
    val context = LocalContext.current
    var showUsageStats by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }
    var excludeSystemApps by remember { mutableStateOf(false) }
    var usageStatsData by remember {
        mutableStateOf<List<android.app.usage.UsageStats>>(emptyList())
    }
    var totalTimeInForeground by remember { mutableStateOf(0L) }
    var totalApps by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting Card

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = {
            coroutineScope.launch {
                googleAuth.signInWithGoogle()
            }
        }) {
            Text(text = "Sign in with google")
        }

        // Usage Statistics Button
        Button(
                onClick = {
                    if (hasUsageStatsPermission(context)) {
                        val stats = getUsageStats(context)
                        usageStatsData = stats
                        totalTimeInForeground = stats.sumOf { it.totalTimeInForeground }
                        totalApps = stats.size
                        showUsageStats = true
                        currentPage = 0
                    } else {
                        openUsageAccessSettings(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
        ) { Text("📊 View Usage Statistics (6 Months)") }

        // Usage Statistics Display
        if (showUsageStats && usageStatsData.isNotEmpty()) {
            UsageStatsScreen(
                    usageStats = usageStatsData,
                    totalTimeInForeground = totalTimeInForeground,
                    totalApps = totalApps,
                    currentPage = currentPage,
                    excludeSystemApps = excludeSystemApps,
                    onPageChange = { currentPage = it },
                    onExcludeSystemAppsChange = { excludeSystemApps = it }
            )
        }
    }
}