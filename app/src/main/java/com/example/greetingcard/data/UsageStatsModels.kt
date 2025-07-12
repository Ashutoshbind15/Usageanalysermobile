package com.example.greetingcard.data

import android.content.Context
import android.content.pm.ApplicationInfo
import java.util.concurrent.TimeUnit

/** Data class to hold aggregated usage statistics */
data class AggregatedUsageStats(
        val packageName: String,
        val totalTimeInForeground: Long,
        val lastTimeUsed: Long,
        val firstTimeStamp: Long,
        val lastTimeStamp: Long
) {
    /** Gets the display name of the app */
    fun getDisplayName(context: Context): String {
        return getAppDisplayName(context, packageName)
    }

    /** Gets formatted usage time string */
    fun getFormattedUsageTime(): String {
        val hours = TimeUnit.MILLISECONDS.toHours(totalTimeInForeground)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTimeInForeground) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    /** Gets detailed usage information */
    fun getDetailedInfo(context: Context): AppUsageDetails {
        return AppUsageDetails(
                appName = getDisplayName(context),
                packageName = packageName,
                totalUsageTime = totalTimeInForeground,
                formattedUsageTime = getFormattedUsageTime(),
                lastUsed = lastTimeUsed,
                firstUsed = firstTimeStamp,
                lastUsedFormatted = formatDate(lastTimeUsed),
                firstUsedFormatted = formatDate(firstTimeStamp),
                isSystemApp = isSystemApp(context, packageName)
        )
    }
}

/** Data class for detailed app usage information */
data class AppUsageDetails(
        val appName: String,
        val packageName: String,
        val totalUsageTime: Long,
        val formattedUsageTime: String,
        val lastUsed: Long,
        val firstUsed: Long,
        val lastUsedFormatted: String,
        val firstUsedFormatted: String,
        val isSystemApp: Boolean
)

/** Gets the display name of an app from its package name */
fun getAppDisplayName(context: Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val appName = packageManager.getApplicationLabel(applicationInfo).toString()

        // If the app name is "Android" or empty, try to get a better name
        if (appName == "Android" || appName.isEmpty()) {
            getBetterAppName(packageName, applicationInfo)
        } else {
            appName
        }
    } catch (e: Exception) {
        // Return a cleaned version of the package name if app name not found
        getBetterAppName(packageName, null)
    }
}

/** Gets a better app name when the default name is "Android" or unavailable */
private fun getBetterAppName(packageName: String, applicationInfo: ApplicationInfo?): String {
    // Common app package name mappings
    val appNameMappings =
            mapOf(
                    "com.instagram.android" to "Instagram",
                    "com.twitter.android" to "X (Twitter)",
                    "com.facebook.katana" to "Facebook",
                    "com.whatsapp" to "WhatsApp",
                    "com.google.android.youtube" to "YouTube",
                    "com.google.android.apps.maps" to "Google Maps",
                    "com.google.android.gm" to "Gmail",
                    "com.google.android.apps.photos" to "Google Photos",
                    "com.google.android.apps.docs.editors.docs" to "Google Docs",
                    "com.google.android.apps.docs.editors.sheets" to "Google Sheets",
                    "com.google.android.apps.docs.editors.slides" to "Google Slides",
                    "com.google.android.apps.drive" to "Google Drive",
                    "com.google.android.calendar" to "Google Calendar",
                    "com.google.android.apps.messaging" to "Messages",
                    "com.google.android.contacts" to "Contacts",
                    "com.google.android.dialer" to "Phone",
                    "com.google.android.apps.camera" to "Camera",
                    "com.google.android.apps.gallery" to "Gallery",
                    "com.android.settings" to "Settings",
                    "com.android.systemui" to "System UI",
                    "com.android.launcher3" to "Launcher",
                    "com.android.launcher2" to "Launcher",
                    "com.android.launcher" to "Launcher",
                    "com.google.android.apps.nexuslauncher" to "Pixel Launcher",
                    "com.oneplus.launcher" to "OnePlus Launcher",
                    "com.samsung.android.launcher" to "Samsung Launcher",
                    "com.miui.home" to "MIUI Launcher",
                    "com.huawei.android.launcher" to "EMUI Launcher",
                    "com.oppo.launcher" to "OPPO Launcher",
                    "com.vivo.launcher" to "vivo Launcher",
                    "com.realme.launcher" to "realme Launcher"
            )

    // Check if we have a mapping for this package
    appNameMappings[packageName]?.let {
        return it
    }

    // If no mapping, try to extract a meaningful name from the package
    return when {
        packageName.startsWith("com.android.") -> {
            val suffix = packageName.substringAfter("com.android.")
            suffix.split(".").lastOrNull()?.capitalize() ?: "Android System"
        }
        packageName.startsWith("android.") -> {
            val suffix = packageName.substringAfter("android.")
            suffix.split(".").lastOrNull()?.capitalize() ?: "Android System"
        }
        packageName.startsWith("com.google.android.") -> {
            val suffix = packageName.substringAfter("com.google.android.")
            suffix.split(".").lastOrNull()?.capitalize() ?: "Google App"
        }
        packageName.startsWith("com.samsung.") -> {
            val suffix = packageName.substringAfter("com.samsung.")
            suffix.split(".").lastOrNull()?.capitalize() ?: "Samsung App"
        }
        packageName.startsWith("com.oneplus.") -> {
            val suffix = packageName.substringAfter("com.oneplus.")
            suffix.split(".").lastOrNull()?.capitalize() ?: "OnePlus App"
        }
        packageName.startsWith("com.miui.") -> {
            val suffix = packageName.substringAfter("com.miui.")
            suffix.split(".").lastOrNull()?.capitalize() ?: "MIUI App"
        }
        packageName.startsWith("com.huawei.") -> {
            val suffix = packageName.substringAfter("com.huawei.")
            suffix.split(".").lastOrNull()?.capitalize() ?: "Huawei App"
        }
        else -> {
            // Extract the last meaningful part of the package name
            val parts = packageName.split(".")
            parts.lastOrNull()?.capitalize() ?: packageName
        }
    }
}

/** Checks if an app is a system app or should be filtered out */
fun isSystemApp(context: Context, packageName: String): Boolean {
    return try {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)

        // Check various system app indicators
        val isSystemFlag = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystemFlag =
                (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

        // Check if it's a system app by flags
        if (isSystemFlag || isUpdatedSystemFlag) {
            return true
        }

        // Check if it's a known system package
        if (isKnownSystemPackage(packageName)) {
            return true
        }

        // Check if it's a launcher (should be filtered)
        if (isLauncherApp(packageName)) {
            return true
        }

        // Check if it's a system UI component
        if (isSystemUIComponent(packageName)) {
            return true
        }

        false
    } catch (e: Exception) {
        // If we can't get app info, check if it's a known system package
        isKnownSystemPackage(packageName)
    }
}

/** Checks if a package is a known system package */
private fun isKnownSystemPackage(packageName: String): Boolean {
    val systemPackages =
            setOf(
                    "android",
                    "com.android.settings",
                    "com.android.systemui",
                    "com.android.launcher3",
                    "com.android.launcher2",
                    "com.android.launcher",
                    "com.google.android.apps.nexuslauncher",
                    "com.oneplus.launcher",
                    "com.samsung.android.launcher",
                    "com.miui.home",
                    "com.huawei.android.launcher",
                    "com.oppo.launcher",
                    "com.vivo.launcher",
                    "com.realme.launcher",
                    "com.android.providers.telephony",
                    "com.android.providers.media",
                    "com.android.providers.downloads",
                    "com.android.providers.contacts",
                    "com.android.providers.calendar",
                    "com.android.providers.settings",
                    "com.android.providers.userdictionary",
                    "com.android.providers.applications",
                    "com.android.providers.media.module",
                    "com.android.providers.telephony.overlay",
                    "com.android.providers.media.overlay",
                    "com.android.providers.settings.overlay",
                    "com.android.providers.telephony.overlay.common",
                    "com.android.providers.media.overlay.common",
                    "com.android.providers.settings.overlay.common",
                    "com.android.providers.telephony.overlay.common.overlay",
                    "com.android.providers.media.overlay.common.overlay",
                    "com.android.providers.settings.overlay.common.overlay",
                    "com.android.providers.telephony.overlay.common.overlay.common",
                    "com.android.providers.media.overlay.common.overlay.common",
                    "com.android.providers.settings.overlay.common.overlay.common",
                    "com.android.providers.telephony.overlay.common.overlay.common.overlay",
                    "com.android.providers.media.overlay.common.overlay.common.overlay",
                    "com.android.providers.settings.overlay.common.overlay.common.overlay"
            )

    return systemPackages.contains(packageName) ||
            packageName.startsWith("android.") ||
            packageName.startsWith("com.android.") ||
            packageName.startsWith("com.google.android.apps.") ||
            packageName.startsWith("com.google.android.gms") ||
            packageName.startsWith("com.google.android.gsf") ||
            packageName.startsWith("com.google.android.packageinstaller") ||
            packageName.startsWith("com.google.android.permissioncontroller")
}

/** Checks if a package is a launcher app */
private fun isLauncherApp(packageName: String): Boolean {
    return packageName.contains("launcher", ignoreCase = true) ||
            packageName.contains("home", ignoreCase = true)
}

/** Checks if a package is a system UI component */
private fun isSystemUIComponent(packageName: String): Boolean {
    return packageName.contains("systemui", ignoreCase = true) ||
            packageName.contains("statusbar", ignoreCase = true) ||
            packageName.contains("navigation", ignoreCase = true) ||
            packageName.contains("quickstep", ignoreCase = true)
}

/** Formats a timestamp to a readable date string */
fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Never"

    val date = java.util.Date(timestamp)
    val formatter =
            java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}
