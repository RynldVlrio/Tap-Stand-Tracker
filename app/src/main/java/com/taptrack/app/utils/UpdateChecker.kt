package com.taptrack.app.utils

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

// Published CSV URL from Google Sheets: File → Share → Publish to web → AppControl → CSV
private const val APP_CONTROL_CSV_URL =
    "https://docs.google.com/spreadsheets/d/e/2PACX-1vSEi5JnHjl2_P9a2abrFZPt4n4CFQm4Zq1fPg9N3ODDSmGD9hYyOAl5vqV2J3rSdRVNIo0bZ-s7ieJi/pub?gid=0&single=true&output=csv"
private const val PREFS_KEY_FIRST_UPDATE_SEEN = "first_update_seen_ms"

data class AppControlConfig(
    val status: String,
    val message: String,
    val latestVersionCode: Int,
    val minSupportedVersionCode: Int,
    val apkUrl: String,
    val graceDays: Int
)

sealed class AppUpdateState {
    object Normal : AppUpdateState()
    data class UpdateAvailable(val apkUrl: String, val latestVersionCode: Int) : AppUpdateState()
    data class ForceUpdate(val apkUrl: String, val latestVersionCode: Int) : AppUpdateState()
    data class Maintenance(val message: String) : AppUpdateState()
}

suspend fun fetchAppControl(): AppControlConfig? = withContext(Dispatchers.IO) {
    try {
        val url = APP_CONTROL_CSV_URL
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "TapTrack-Android/1.0")
        }
        if (connection.responseCode != 200) return@withContext null
        parseCsv(connection.inputStream.bufferedReader().readText())
    } catch (_: Exception) {
        null
    }
}

private fun parseCsv(csv: String): AppControlConfig? {
    val map = mutableMapOf<String, String>()
    val lines = csv.lines().drop(1) // skip header row
    for (line in lines) {
        if (line.isBlank()) continue
        // Split on first comma only so URLs/messages with commas are preserved
        val commaIndex = line.indexOf(',')
        if (commaIndex < 0) continue
        val key = line.substring(0, commaIndex).trim().removeSurrounding("\"")
        val value = line.substring(commaIndex + 1).trim().removeSurrounding("\"")
        map[key] = value
    }
    return try {
        AppControlConfig(
            status = map["status"]?.trim() ?: "ACTIVE",
            message = map["message"]?.trim() ?: "",
            latestVersionCode = map["latestVersionCode"]?.trim()?.toIntOrNull() ?: 0,
            minSupportedVersionCode = map["minSupportedVersionCode"]?.trim()?.toIntOrNull() ?: 0,
            apkUrl = map["apkUrl"]?.trim() ?: "",
            graceDays = map["graceDays"]?.trim()?.toIntOrNull() ?: 7
        )
    } catch (_: Exception) {
        null
    }
}

fun resolveUpdateState(
    config: AppControlConfig,
    currentVersionCode: Int,
    prefs: SharedPreferences
): AppUpdateState {
    if (config.status.uppercase() != "ACTIVE") {
        return AppUpdateState.Maintenance(config.message)
    }

    if (currentVersionCode < config.minSupportedVersionCode) {
        return AppUpdateState.ForceUpdate(config.apkUrl, config.latestVersionCode)
    }

    if (currentVersionCode < config.latestVersionCode) {
        val now = System.currentTimeMillis()
        val firstSeen = prefs.getLong(PREFS_KEY_FIRST_UPDATE_SEEN, 0L)
        if (firstSeen == 0L) {
            prefs.edit().putLong(PREFS_KEY_FIRST_UPDATE_SEEN, now).apply()
        } else {
            val daysSinceFirstSeen = TimeUnit.MILLISECONDS.toDays(now - firstSeen)
            if (daysSinceFirstSeen >= config.graceDays) {
                return AppUpdateState.ForceUpdate(config.apkUrl, config.latestVersionCode)
            }
        }
        return AppUpdateState.UpdateAvailable(config.apkUrl, config.latestVersionCode)
    }

    // App is up to date — clear grace period tracker
    prefs.edit().remove(PREFS_KEY_FIRST_UPDATE_SEEN).apply()
    return AppUpdateState.Normal
}
