package com.example.greetingcard.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.example.greetingcard.data.AggregatedUsageStats
import com.example.greetingcard.data.isSystemApp
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val TAG = "UsageStatsUtils"

/**
 * Fetches usage statistics for the past 6 months
 * @param context The application context
 * @return List of usage statistics for the past 6 months
 */
fun getUsageStats(context: Context): List<android.app.usage.UsageStats> {
    val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // Calculate time range for past 7 days
    val calendar = Calendar.getInstance()
    val endTime = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, -7)
    val startTime = calendar.timeInMillis

    // Query usage statistics
    val usageStats =
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

    Log.d(TAG, "Retrieved ${usageStats.size} usage stats entries")
    return usageStats
}

/**
 * Checks if the app has permission to access usage statistics
 * @param context The application context
 * @return true if permission is granted, false otherwise
 */
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
    val mode =
            appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
            )
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}

/**
 * Opens the usage access settings screen
 * @param context The application context
 */
fun openUsageAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    context.startActivity(intent)
}

/**
 * Processes and aggregates usage statistics
 * @param usageStats Raw usage statistics
 * @param excludeSystemApps Whether to exclude system apps
 * @param context Application context
 * @return List of aggregated usage statistics
 */
fun processUsageStats(
        usageStats: List<android.app.usage.UsageStats>,
        excludeSystemApps: Boolean,
        context: Context
): List<AggregatedUsageStats> {
    Log.d(
            TAG,
            "Processing ${usageStats.size} usage stats entries, excludeSystemApps: $excludeSystemApps"
    )

    val filteredStats =
            usageStats
                    .filter { it.totalTimeInForeground > 0 }
                    .also { Log.d(TAG, "After filtering for usage time > 0: ${it.size} entries") }
                    .filter { stat ->
                        if (excludeSystemApps) {
                            val isSystem = isSystemApp(context, stat.packageName)
                            if (isSystem) {
                                Log.d(TAG, "Filtering out system app: ${stat.packageName}")
                            }
                            !isSystem
                        } else {
                            true
                        }
                    }
                    .also { Log.d(TAG, "After system app filtering: ${it.size} entries") }

    // Group by package name and aggregate
    val groupedStats = filteredStats.groupBy { it.packageName }
    Log.d(TAG, "Grouped into ${groupedStats.size} unique apps")

    val aggregatedStats =
            groupedStats
                    .mapValues { (packageName, stats) ->
                        stats.fold(
                                AggregatedUsageStats(
                                        packageName = packageName,
                                        totalTimeInForeground = 0L,
                                        lastTimeUsed = 0L,
                                        firstTimeStamp = Long.MAX_VALUE,
                                        lastTimeStamp = 0L
                                )
                        ) { acc, stat ->
                            AggregatedUsageStats(
                                    packageName = packageName,
                                    totalTimeInForeground =
                                            acc.totalTimeInForeground + stat.totalTimeInForeground,
                                    lastTimeUsed = maxOf(acc.lastTimeUsed, stat.lastTimeUsed),
                                    firstTimeStamp = minOf(acc.firstTimeStamp, stat.firstTimeStamp),
                                    lastTimeStamp = maxOf(acc.lastTimeStamp, stat.lastTimeStamp)
                            )
                        }
                    }
                    .values
                    .toList()

    // Sort by usage time descending
    val sortedStats = aggregatedStats.sortedByDescending { it.totalTimeInForeground }

    Log.d(TAG, "Final result: ${sortedStats.size} apps")

    // Log top 10 apps for debugging
    sortedStats.take(10).forEachIndexed { index, stat ->
        Log.d(TAG, "Top ${index + 1}: ${stat.packageName} - ${stat.getFormattedUsageTime()}")
    }

    return sortedStats
}

/**
 * Debug function to analyze usage stats and identify problematic apps
 * @param context The application context
 * @param usageStats Raw usage statistics
 */
fun debugUsageStats(context: Context, usageStats: List<android.app.usage.UsageStats>) {
    Log.d(TAG, "=== DEBUG: Usage Stats Analysis ===")
    Log.d(TAG, "Total entries: ${usageStats.size}")

    // Find apps that might be problematic
    val problematicApps =
            usageStats.filter { stat ->
                stat.totalTimeInForeground > 0 &&
                        (stat.packageName.contains("android") ||
                                stat.packageName.contains("instagram") ||
                                stat.packageName.contains("twitter") ||
                                stat.packageName.contains("facebook") ||
                                stat.packageName.contains("whatsapp"))
            }

    Log.d(TAG, "Potentially problematic apps: ${problematicApps.size}")

    problematicApps.forEach { stat ->
        try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(stat.packageName, 0)
            val appName = packageManager.getApplicationLabel(applicationInfo).toString()
            val isSystem =
                    (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem =
                    (applicationInfo.flags and
                            android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            Log.d(TAG, "App: ${stat.packageName}")
            Log.d(TAG, "  Display Name: $appName")
            Log.d(TAG, "  System Flags: system=$isSystem, updatedSystem=$isUpdatedSystem")
            Log.d(TAG, "  Usage Time: ${stat.totalTimeInForeground}ms")
        } catch (e: Exception) {
            Log.d(TAG, "App: ${stat.packageName} - Error getting info: ${e.message}")
        }
    }

    Log.d(TAG, "=== END DEBUG ===")
}

/**
 * Gets a summary of usage statistics for the past 6 months
 * @param context The application context
 * @return A formatted string with usage summary
 */
fun getUsageStatsSummary(context: Context): String {
    if (!hasUsageStatsPermission(context)) {
        return "Usage statistics permission not granted. Please enable in Settings > Usage Access."
    }

    val usageStats = getUsageStats(context)
    if (usageStats.isEmpty()) {
        return "No usage statistics available for the past 6 months."
    }

    // Debug the usage stats
    debugUsageStats(context, usageStats)

    val totalTimeInForeground = usageStats.sumOf { it.totalTimeInForeground }
    val totalTimeInHours = TimeUnit.MILLISECONDS.toHours(totalTimeInForeground)
    val totalTimeInDays = TimeUnit.MILLISECONDS.toDays(totalTimeInForeground)

    return buildString {
        appendLine("📊 Usage Statistics (Past 6 Months)")
        appendLine("Total screen time: ${totalTimeInDays} days, ${totalTimeInHours % 24} hours")
        appendLine("Total apps used: ${usageStats.size}")
    }
}
