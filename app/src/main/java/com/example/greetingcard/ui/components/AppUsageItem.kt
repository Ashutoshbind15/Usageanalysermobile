package com.example.greetingcard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
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

                    // App icon
                    AppIcon(
                            packageName = details.packageName,
                            context = context,
                            modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

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
fun AppIcon(packageName: String, context: android.content.Context, modifier: Modifier = Modifier) {
    val appIcon =
            remember(packageName) {
                try {
                    val packageManager = context.packageManager
                    val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                    val drawable = packageManager.getApplicationIcon(applicationInfo)
                    drawable.toBitmap().asImageBitmap()
                } catch (e: Exception) {
                    // Try alternative method for non-system apps
                    try {
                        val packageManager = context.packageManager
                        val drawable = packageManager.getApplicationIcon(packageName)
                        drawable.toBitmap().asImageBitmap()
                    } catch (e2: Exception) {
                        android.util.Log.d(
                                "AppIcon",
                                "Failed to load icon for $packageName: ${e2.message}"
                        )
                        null
                    }
                }
            }

    if (appIcon != null) {
        androidx.compose.foundation.Image(
                bitmap = appIcon,
                contentDescription = "App icon",
                modifier = modifier.clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Fit
        )
    } else {
        // Fallback icon if app icon cannot be loaded
        Surface(
                modifier = modifier.clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // Try to get app name for better fallback
                val appName =
                        remember(packageName) {
                            try {
                                val packageManager = context.packageManager
                                val applicationInfo =
                                        packageManager.getApplicationInfo(packageName, 0)
                                packageManager.getApplicationLabel(applicationInfo).toString()
                            } catch (e: Exception) {
                                packageName.substringAfterLast(".").take(1).uppercase()
                            }
                        }

                if (appName.length == 1) {
                    // Show first letter if we got a single character
                    Text(
                            text = appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Show generic app icon
                    Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "App icon placeholder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppUsageDetails(details: AppUsageDetails, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow("Package Name", details.packageName)
        DetailRow("Last Used", details.lastUsedFormatted)

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
