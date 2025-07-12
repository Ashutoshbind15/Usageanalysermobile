package com.example.greetingcard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.greetingcard.data.AggregatedUsageStats
import com.example.greetingcard.data.AppUsageDetails

@Composable
fun AppUsageItem(
        app: AggregatedUsageStats,
        rank: Int,
        context: android.content.Context,
        modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val details = app.getDetailedInfo(context)

    Card(
            modifier = modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main app info row
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = "$rank.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(30.dp)
                    )

                    Column {
                        Text(
                                text = details.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                        )
                        if (details.isSystemApp) {
                            Text(
                                    text = "System App",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = details.formattedUsageTime,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                            imageVector =
                                    if (isExpanded) Icons.Default.KeyboardArrowDown
                                    else Icons.Default.KeyboardArrowRight,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded details
            AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    AppUsageDetails(details = details)
                }
            }
        }
    }
}

@Composable
fun AppUsageDetails(details: AppUsageDetails, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow("Package Name", details.packageName)
        DetailRow("First Used", details.firstUsedFormatted)
        DetailRow("Last Used", details.lastUsedFormatted)
        DetailRow("Total Usage", details.formattedUsageTime)
        DetailRow("Usage (ms)", details.totalUsageTime.toString())

        if (details.isSystemApp) {
            DetailRow("Type", "System Application")
        } else {
            DetailRow("Type", "User Application")
        }

        // Add debugging info for problematic apps
        if (details.appName == "Android" || details.packageName.contains("android")) {
            DetailRow("⚠️ Debug", "This app shows as 'Android' - check package name")
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
        )
    }
}
