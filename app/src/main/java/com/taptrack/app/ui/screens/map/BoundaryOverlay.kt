package com.taptrack.app.ui.screens.map

import org.osmdroid.util.GeoPoint

data class BoundaryOverlay(
    val id: Long,
    val name: String,
    val color: Int,
    val polygons: List<List<GeoPoint>>,
    val polylines: List<List<GeoPoint>>
)
