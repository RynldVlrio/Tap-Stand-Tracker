package com.taptrack.app.utils

import android.util.Xml
import com.taptrack.app.data.local.entity.TapStandEntity
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ImportedWaypoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = "",
    val status: String = "Active",
    val installationDate: String = ""
)

object GpxUtils {

    fun parse(inputStream: InputStream): List<ImportedWaypoint> {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)
        val waypoints = mutableListOf<ImportedWaypoint>()

        var inWpt = false
        var lat = Double.NaN
        var lon = Double.NaN
        var name = ""
        var desc = ""
        var status = "Active"
        var date = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(":")
                    when (tag) {
                        "wpt" -> {
                            inWpt = true
                            lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: Double.NaN
                            lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: Double.NaN
                            name = ""; desc = ""; status = "Active"; date = ""
                        }
                        "name" -> if (inWpt) name = parser.nextText().trim()
                        "desc" -> if (inWpt) desc = parser.nextText().trim()
                        "time" -> if (inWpt && date.isEmpty()) {
                            val t = parser.nextText().trim()
                            date = if (t.length >= 10) t.take(10) else t
                        }
                        "status" -> if (inWpt) status = parser.nextText().trim()
                        "installationDate" -> if (inWpt) date = parser.nextText().trim()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.substringAfterLast(":") == "wpt" && inWpt) {
                        inWpt = false
                        if (!lat.isNaN() && !lon.isNaN()) {
                            waypoints.add(ImportedWaypoint(name, lat, lon, desc, status, date))
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return waypoints
    }

    fun generate(tapStands: List<TapStandEntity>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<gpx version="1.1" creator="TapTrack" xmlns="http://www.topografix.com/GPX/1/1" xmlns:taptrack="https://taptrack.app/gpx">""")
        appendLine("""  <metadata><name>TapTrack Export</name></metadata>""")
        for (ts in tapStands) {
            appendLine("""  <wpt lat="${ts.latitude}" lon="${ts.longitude}">""")
            appendLine("""    <name>${ts.name.escapeXml()}</name>""")
            if (ts.locationDescription.isNotEmpty())
                appendLine("""    <desc>${ts.locationDescription.escapeXml()}</desc>""")
            if (ts.installationDate.isNotEmpty())
                appendLine("""    <time>${ts.installationDate}T00:00:00Z</time>""")
            appendLine("""    <extensions>""")
            appendLine("""      <taptrack:status>${ts.status.escapeXml()}</taptrack:status>""")
            appendLine("""      <taptrack:installationDate>${ts.installationDate.escapeXml()}</taptrack:installationDate>""")
            appendLine("""    </extensions>""")
            appendLine("""  </wpt>""")
        }
        append("</gpx>")
    }
}

object KmlUtils {

    fun parseKml(inputStream: InputStream): List<ImportedWaypoint> {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)
        val waypoints = mutableListOf<ImportedWaypoint>()

        var inPlacemark = false
        var inExtendedData = false
        var inPoint = false
        var currentDataName = ""
        var name = ""
        var desc = ""
        var coordinates = ""
        var status = "Active"
        var date = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(":")
                    when (tag) {
                        "Placemark" -> {
                            inPlacemark = true
                            name = ""; desc = ""; coordinates = ""; status = "Active"; date = ""
                        }
                        "ExtendedData" -> inExtendedData = true
                        "Point" -> if (inPlacemark) inPoint = true
                        "Data" -> if (inExtendedData) currentDataName = parser.getAttributeValue(null, "name") ?: ""
                        "name" -> if (inPlacemark && !inExtendedData) name = parser.nextText().trim()
                        "description" -> if (inPlacemark && !inExtendedData) desc = parser.nextText().trim()
                        "coordinates" -> if (inPoint) coordinates = parser.nextText().trim()
                        "value" -> if (inExtendedData) {
                            val v = parser.nextText().trim()
                            when (currentDataName) {
                                "status" -> status = v
                                "installationDate" -> date = v
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfterLast(":")
                    when (tag) {
                        "Placemark" -> {
                            inPlacemark = false
                            // KML coordinates are lon,lat,alt
                            val parts = coordinates.split(",")
                            val lon = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
                            val lat = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
                            if (lat != null && lon != null) {
                                waypoints.add(ImportedWaypoint(name, lat, lon, desc, status, date))
                            }
                        }
                        "ExtendedData" -> inExtendedData = false
                        "Point" -> inPoint = false
                    }
                }
            }
            eventType = parser.next()
        }
        return waypoints
    }

    fun parseKmz(inputStream: InputStream): List<ImportedWaypoint> {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".kml", ignoreCase = true)) {
                    val kmlBytes = zip.readBytes()
                    return parseKml(kmlBytes.inputStream())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return emptyList()
    }

    fun generateKml(tapStands: List<TapStandEntity>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
        appendLine("""  <Document>""")
        appendLine("""    <name>TapTrack Export</name>""")
        for (ts in tapStands) {
            appendLine("""    <Placemark>""")
            appendLine("""      <name>${ts.name.escapeXml()}</name>""")
            if (ts.locationDescription.isNotEmpty())
                appendLine("""      <description>${ts.locationDescription.escapeXml()}</description>""")
            appendLine("""      <ExtendedData>""")
            appendLine("""        <Data name="status"><value>${ts.status.escapeXml()}</value></Data>""")
            appendLine("""        <Data name="installationDate"><value>${ts.installationDate.escapeXml()}</value></Data>""")
            appendLine("""      </ExtendedData>""")
            appendLine("""      <Point><coordinates>${ts.longitude},${ts.latitude},0</coordinates></Point>""")
            appendLine("""    </Placemark>""")
        }
        appendLine("""  </Document>""")
        append("</kml>")
    }

    fun packKmz(kmlContent: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("doc.kml"))
            zip.write(kmlContent.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return baos.toByteArray()
    }
}

private fun String.escapeXml() =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
