package com.taptrack.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val latestVersion: String, val releaseUrl: String)

suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(
            "https://api.github.com/repos/RynldVlrio/Tap-Stand-Tracker/releases/latest"
        ).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            connectTimeout = 5000
            readTimeout = 5000
        }
        if (connection.responseCode != 200) return@withContext null
        val json = JSONObject(connection.inputStream.bufferedReader().readText())
        val tagName = json.getString("tag_name").removePrefix("v")
        val releaseUrl = json.getString("html_url")
        if (isNewer(remote = tagName, current = currentVersion)) UpdateInfo(tagName, releaseUrl)
        else null
    } catch (_: Exception) {
        null
    }
}

private fun isNewer(remote: String, current: String): Boolean {
    val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
    val c = current.split(".").map { it.toIntOrNull() ?: 0 }
    val len = maxOf(r.size, c.size)
    for (i in 0 until len) {
        val rv = r.getOrElse(i) { 0 }
        val cv = c.getOrElse(i) { 0 }
        if (rv > cv) return true
        if (rv < cv) return false
    }
    return false
}
