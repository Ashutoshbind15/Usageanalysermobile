package com.example.greetingcard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.greetingcard.auth.GoogleAuth
import com.example.greetingcard.data.NotificationDatabase
import com.example.greetingcard.data.NotificationEntity
import com.example.greetingcard.ui.screens.UsageStatsScreen
import com.example.greetingcard.ui.theme.GreetingCardTheme
import com.example.greetingcard.utils.getUsageStats
import com.example.greetingcard.utils.hasUsageStatsPermission
import com.example.greetingcard.utils.openUsageAccessSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    var showNotifications by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }
    var excludeSystemApps by remember { mutableStateOf(false) }
    var usageStatsData by remember {
        mutableStateOf<List<android.app.usage.UsageStats>>(emptyList())
    }
    var totalTimeInForeground by remember { mutableStateOf(0L) }
    var totalApps by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    val database = remember { NotificationDatabase.getDatabase(context) }
    val notifications by
            database.notificationDao().getAllNotifications().collectAsState(initial = emptyList())

    Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting Card

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { coroutineScope.launch { googleAuth.signInWithGoogle() } }) {
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

        // Notifications Button
        Button(
                onClick = {
                    if (isNotificationServiceEnabled(context)) {
                        showNotifications = !showNotifications
                    } else {
                        openNotificationAccessSettings(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                    text =
                            if (isNotificationServiceEnabled(context)) {
                                "🔔 View Notifications (${notifications.size})"
                            } else {
                                "⚠️ Enable Notification Access"
                            }
            )
        }

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

        // Notifications Display
        if (showNotifications) {
            NotificationsList(notifications = notifications)
        }
    }
}

@Composable
fun NotificationsList(notifications: List<NotificationEntity>) {
    if (notifications.isEmpty()) {
        Text(
                text =
                        "No notifications captured yet.\nMake sure the notification listener service is enabled.",
                modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyColumn(
                modifier = Modifier.fillMaxWidth().height(400.dp).padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { items(notifications) { notification -> NotificationItem(notification = notification) } }
    }
}

@Composable
fun NotificationItem(notification: NotificationEntity) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                        text = notification.packageName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                )
                Text(
                        text = formatTimestamp(notification.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                    text = notification.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                    text = notification.description,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun isNotificationServiceEnabled(context: android.content.Context): Boolean {
    val packageName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(packageName)
}

fun openNotificationAccessSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

// Add this function to check if the service is properly bound
fun isNotificationListenerServiceBound(context: android.content.Context): Boolean {
    return isNotificationServiceEnabled(context)
}
