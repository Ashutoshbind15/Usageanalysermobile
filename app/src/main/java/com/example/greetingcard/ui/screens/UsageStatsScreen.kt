package com.example.greetingcard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.greetingcard.ui.components.AppUsageItem
import com.example.greetingcard.utils.processUsageStats

@Composable
fun UsageStatsScreen(
        usageStats: List<android.app.usage.UsageStats>,
        totalTimeInForeground: Long,
        totalApps: Int,
        currentPage: Int,
        excludeSystemApps: Boolean,
        onPageChange: (Int) -> Unit,
        onExcludeSystemAppsChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val appsPerPage = 10
    val totalTimeInHours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(totalTimeInForeground)
    val totalTimeInDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(totalTimeInForeground)

    // Process and filter apps
    val filteredApps = processUsageStats(usageStats, excludeSystemApps, context)

    val totalPages = (filteredApps.size + appsPerPage - 1) / appsPerPage
    val startIndex = currentPage * appsPerPage
    val endIndex = minOf(startIndex + appsPerPage, filteredApps.size)
    val currentPageApps = filteredApps.subList(startIndex, endIndex)

    Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Section
        Text(
                text =
                        buildString {
                            appendLine("📊 Usage Statistics (Past 6 Months)")
                            appendLine(
                                    "Total screen time: ${totalTimeInDays} days, ${totalTimeInHours % 24} hours"
                            )
                            appendLine("Total apps used: ${filteredApps.size}")
                            if (excludeSystemApps) {
                                appendLine("(System apps excluded)")
                            }
                            appendLine("Raw data: ${usageStats.size} entries")
                        },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
        )

        // System Apps Toggle
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Exclude System Apps", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = excludeSystemApps, onCheckedChange = onExcludeSystemAppsChange)
        }

        // Apps List
        Text(
                text = "🏆 Most Used Apps:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
        )

        // App items with expand functionality
        currentPageApps.forEachIndexed { index, app ->
            val globalIndex = startIndex + index + 1
            AppUsageItem(
                    app = app,
                    rank = globalIndex,
                    context = context,
                    modifier = Modifier.fillMaxWidth()
            )
        }

        // Pagination Controls
        if (totalPages > 1) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 0) {
                    Text("← Previous")
                }

                Text(
                        text = "Page ${currentPage + 1} of $totalPages",
                        style = MaterialTheme.typography.bodyMedium
                )

                Button(
                        onClick = { onPageChange(currentPage + 1) },
                        enabled = currentPage < totalPages - 1
                ) { Text("Next →") }
            }
        }
    }
}
