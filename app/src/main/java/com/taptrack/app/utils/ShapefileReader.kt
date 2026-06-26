package com.taptrack.app.utils

import org.osmdroid.util.GeoPoint
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ShapefileReader {

    data class ShapeGeometry(
        val polygons: List<List<GeoPoint>>,
        val polylines: List<List<GeoPoint>>
    )

    /**
     * Parses an ESRI shapefile (.shp) stream.
     * Handles types: PolyLine (3), Polygon (5), PolyLineZ (13), PolygonZ (15),
     * PolyLineM (23), PolygonM (25). Assumes WGS84 coordinates (X=lon, Y=lat).
     */
    fun readShp(inputStream: InputStream): ShapeGeometry {
        val bytes = inputStream.readBytes()
        if (bytes.size < 100) return ShapeGeometry(emptyList(), emptyList())

        val buf = ByteBuffer.wrap(bytes)
        val polygons = mutableListOf<List<GeoPoint>>()
        val polylines = mutableListOf<List<GeoPoint>>()

        buf.position(100) // skip file header

        while (buf.remaining() >= 8) {
            buf.order(ByteOrder.BIG_ENDIAN)
            @Suppress("UNUSED_VARIABLE")
            val recNum = buf.int
            val contentLengthBytes = buf.int * 2

            if (contentLengthBytes < 4 || buf.remaining() < contentLengthBytes) break

            val contentStart = buf.position()
            buf.order(ByteOrder.LITTLE_ENDIAN)
            val shapeType = buf.int

            try {
                when (shapeType) {
                    3, 13, 23 -> { // PolyLine, PolyLineZ, PolyLineM
                        readPartsAndPoints(buf).forEach { segment ->
                            if (segment.size >= 2) polylines.add(segment)
                        }
                    }
                    5, 15, 25 -> { // Polygon, PolygonZ, PolygonM
                        readPartsAndPoints(buf).forEach { ring ->
                            if (ring.size >= 3) polygons.add(ring)
                        }
                    }
                    // 0 = Null, 1 = Point, etc. — skip
                }
            } catch (_: Exception) { }

            buf.position(contentStart + contentLengthBytes)
        }

        return ShapeGeometry(polygons, polylines)
    }

    private fun readPartsAndPoints(buf: ByteBuffer): List<List<GeoPoint>> {
        // Skip bounding box (4 doubles = 32 bytes)
        buf.position(buf.position() + 32)
        val numParts = buf.int
        val numPoints = buf.int
        if (numParts <= 0 || numPoints <= 0 || numParts > numPoints) return emptyList()

        val partStarts = IntArray(numParts) { buf.int }
        val allPoints = Array(numPoints) {
            val x = buf.double // longitude
            val y = buf.double // latitude
            GeoPoint(y, x)
        }

        return (0 until numParts).map { i ->
            val start = partStarts[i]
            val end = if (i < numParts - 1) partStarts[i + 1] else numPoints
            allPoints.slice(start until end)
        }
    }
}
