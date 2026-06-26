package com.taptrack.app.utils

import android.util.Xml
import org.osmdroid.util.GeoPoint
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ParsedBoundary(
    val name: String,
    val polygons: List<List<GeoPoint>>,
    val polylines: List<List<GeoPoint>>
)

object BoundaryParser {

    /** Parse a ZIP archive that contains ESRI shapefile components (.shp required). */
    fun parseShapefileZip(inputStream: InputStream): ParsedBoundary {
        var shpBytes: ByteArray? = null
        var name = "Shapefile"
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val entryName = entry.name.substringAfterLast('/')
                if (entryName.endsWith(".shp", ignoreCase = true)) {
                    shpBytes = zip.readBytes()
                    name = entryName.substringBeforeLast('.')
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val data = shpBytes ?: return ParsedBoundary(name, emptyList(), emptyList())
        return try {
            val geo = ShapefileReader.readShp(data.inputStream())
            ParsedBoundary(name, geo.polygons, geo.polylines)
        } catch (_: Exception) {
            ParsedBoundary(name, emptyList(), emptyList())
        }
    }

    /** Parse a raw .shp file directly (no ZIP wrapper). */
    fun parseShpFile(inputStream: InputStream, name: String): ParsedBoundary {
        return try {
            val geo = ShapefileReader.readShp(inputStream)
            ParsedBoundary(name, geo.polygons, geo.polylines)
        } catch (_: Exception) {
            ParsedBoundary(name, emptyList(), emptyList())
        }
    }

    /** Parse a KMZ file (ZIP containing a .kml) and extract polygon/line geometry. */
    fun parseKmz(inputStream: InputStream): ParsedBoundary {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".kml", ignoreCase = true)) {
                    val bytes = zip.readBytes()
                    return parseKml(bytes.inputStream())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return ParsedBoundary("", emptyList(), emptyList())
    }

    /** Parse a KML file and extract Polygon outer rings and LineString geometry. */
    fun parseKml(inputStream: InputStream): ParsedBoundary {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)

        val polygons = mutableListOf<List<GeoPoint>>()
        val polylines = mutableListOf<List<GeoPoint>>()
        var docName = ""

        var inPlacemark = false
        var inPolygon = false
        var inOuterBoundary = false
        var inLineString = false
        var readingCoords = false
        val coordsBuf = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    when (tag) {
                        "name" -> if (!inPlacemark && docName.isEmpty()) {
                            val t = parser.nextText().trim()
                            if (t.isNotEmpty()) docName = t
                        }
                        "Placemark" -> inPlacemark = true
                        "Polygon" -> inPolygon = true
                        "outerBoundaryIs" -> inOuterBoundary = true
                        "LineString" -> inLineString = true
                        "coordinates" -> { readingCoords = true; coordsBuf.clear() }
                    }
                }
                XmlPullParser.TEXT -> if (readingCoords) coordsBuf.append(parser.text)
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    when (tag) {
                        "coordinates" -> {
                            if (readingCoords) {
                                val pts = parseKmlCoords(coordsBuf.toString())
                                when {
                                    inPolygon && inOuterBoundary && pts.size >= 3 -> polygons.add(pts)
                                    inLineString && pts.size >= 2 -> polylines.add(pts)
                                }
                                readingCoords = false
                            }
                        }
                        "Placemark" -> { inPlacemark = false; inPolygon = false; inLineString = false }
                        "Polygon" -> inPolygon = false
                        "outerBoundaryIs" -> inOuterBoundary = false
                        "LineString" -> inLineString = false
                    }
                }
            }
            event = parser.next()
        }
        return ParsedBoundary(docName, polygons, polylines)
    }

    /** Parse a GPX file and extract tracks and routes as polylines. */
    fun parseGpx(inputStream: InputStream): ParsedBoundary {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)

        val polylines = mutableListOf<List<GeoPoint>>()
        var docName = "GPX Track"
        var currentPts = mutableListOf<GeoPoint>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    when (tag) {
                        "name" -> {
                            val t = parser.nextText().trim()
                            if (docName == "GPX Track" && t.isNotEmpty()) docName = t
                        }
                        "trkseg", "rte" -> currentPts = mutableListOf()
                        "trkpt", "rtept" -> {
                            val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            if (lat != null && lon != null) currentPts.add(GeoPoint(lat, lon))
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    when (tag) {
                        "trkseg", "rte" -> {
                            if (currentPts.size >= 2) polylines.add(currentPts.toList())
                        }
                    }
                }
            }
            event = parser.next()
        }
        return ParsedBoundary(docName, emptyList(), polylines)
    }

    private fun parseKmlCoords(text: String): List<GeoPoint> =
        text.trim().split("\\s+".toRegex()).mapNotNull { coord ->
            val parts = coord.split(',')
            val lon = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return@mapNotNull null
            val lat = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return@mapNotNull null
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return@mapNotNull null
            GeoPoint(lat, lon)
        }
}
