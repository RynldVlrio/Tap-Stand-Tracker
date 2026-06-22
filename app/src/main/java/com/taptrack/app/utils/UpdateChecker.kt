package com.taptrack.app.utils

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

// Apps Script web app URL — update this whenever the script is redeployed
private const val APPS_SCRIPT_URL =
    "https://script.google.com/macros/s/AKfycbx0ekn3u1CNItfWHA23K8oh2x9R1nNgEr_iPr5e7Q8PzGO7mDZDzinbXf1ZsTdL9W0vGg/exec"
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
        val connection = URL(APPS_SCRIPT_URL).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "TapTrack-Android/1.0")
        }
        if (connection.responseCode != 200) return@withContext null
        parseJson(connection.inputStream.bufferedReader().readText())
    } catch (_: Exception) {
        null
    }
}

private fun parseJson(json: String): AppControlConfig? {
    return try {
        val obj = JSONObject(json)
        AppControlConfig(
            status = obj.optString("status", "ACTIVE"),
            message = obj.optString("message", ""),
            latestVersionCode = obj.optString("latestVersionCode", "0").toIntOrNull() ?: 0,
            minSupportedVersionCode = obj.optString("minSupportedVersionCode", "0").toIntOrNull() ?: 0,
            apkUrl = obj.optString("apkUrl", ""),
            graceDays = obj.optString("graceDays", "7").toIntOrNull() ?: 7
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

    prefs.edit().remove(PREFS_KEY_FIRST_UPDATE_SEEN).apply()
    return AppUpdateState.Normal
}
