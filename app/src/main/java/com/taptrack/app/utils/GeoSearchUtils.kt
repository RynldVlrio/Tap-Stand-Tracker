package com.taptrack.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SearchResult(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lon: Double
)

// Detects "lat, lon" or "lat lon" input and returns a GeoPoint if valid
fun parseCoordinates(query: String): GeoPoint? {
    val match = Regex("""^(-?\d{1,3}\.?\d*)[,\s]+(-?\d{1,3}\.?\d*)$""")
        .find(query.trim()) ?: return null
    val lat = match.groupValues[1].toDoubleOrNull() ?: return null
    val lon = match.groupValues[2].toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return GeoPoint(lat, lon)
}

suspend fun geocodeSearch(query: String, userAgent: String): List<SearchResult> =
    withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val conn = URL(
                "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5&addressdetails=0"
            ).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", userAgent)
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode != 200) return@runCatching emptyList()
            val arr = JSONArray(conn.inputStream.bufferedReader().readText())
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val display = obj.getString("display_name")
                SearchResult(
                    title = display.substringBefore(",").trim(),
                    subtitle = display,
                    lat = obj.getString("lat").toDouble(),
                    lon = obj.getString("lon").toDouble()
                )
            }
        }.getOrDefault(emptyList())
    }
