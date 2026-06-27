package com.taptrack.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.taptrack.app.data.local.entity.LandmarkEntity
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.ui.screens.map.BoundaryOverlay
import com.taptrack.app.utils.createBoundaryLabelBitmap
import com.taptrack.app.utils.createLandmarkMarkerBitmap
import com.taptrack.app.utils.createLocationDotBitmap
import com.taptrack.app.utils.createSearchPinBitmap
import com.taptrack.app.utils.createTapMarkerBitmap
import com.taptrack.app.utils.getLastKnownLocation
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon as OsmPolygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@SuppressLint("MissingPermission")
@Composable
fun OsmMapView(
    tapStands: List<TapStandWithMeters>,
    modifier: Modifier = Modifier,
    locateTrigger: Int = 0,
    searchTarget: GeoPoint? = null,
    initialLat: Double = 14.5995,
    initialLng: Double = 120.9842,
    zoom: Double = 15.0,
    showUserLocation: Boolean = true,
    routePoints: List<GeoPoint>? = null,
    boundaryOverlays: List<BoundaryOverlay> = emptyList(),
    landmarks: List<LandmarkEntity> = emptyList(),
    searchPin: org.osmdroid.util.GeoPoint? = null,
    onMarkerClick: (TapStandWithMeters) -> Unit = {},
    onLandmarkClick: (LandmarkEntity) -> Unit = {},
    onSearchPinClick: (() -> Unit)? = null,
    onMapViewReady: (MapView) -> Unit = {},
    /** Fires on every long press. nearUser=true when pressing within ~80px of GPS dot. */
    onLongPress: ((lat: Double, lng: Double, nearUser: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Configuration.getInstance().userAgentValue = context.packageName

    val tapMarkerIcon = remember { createTapMarkerBitmap(context) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(initialLat, initialLng))
        }
    }

    LaunchedEffect(mapView) { onMapViewReady(mapView) }

    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            val dot = createLocationDotBitmap(context)
            setPersonIcon(dot)
            setPersonAnchor(.5f, .5f)
            mapView.overlays.add(0, this)
        }
    }

    val compassOverlay = remember {
        CompassOverlay(context, InternalCompassOrientationProvider(context), mapView).apply {
            enableCompass()
            mapView.overlays.add(this)
        }
    }

    val onLongPressRef = rememberUpdatedState(onLongPress)

    remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p ?: return false
                val handler = onLongPressRef.value ?: return false
                val userLoc = myLocationOverlay.myLocation
                val nearUser = if (userLoc != null) {
                    val userPx  = mapView.projection.toPixels(userLoc, android.graphics.Point())
                    val pressPx = mapView.projection.toPixels(p, android.graphics.Point())
                    val dx = (userPx.x - pressPx.x).toFloat()
                    val dy = (userPx.y - pressPx.y).toFloat()
                    kotlin.math.sqrt((dx * dx + dy * dy).toDouble()) <= 80.0
                } else false
                // Use precise GPS coords for tap stand accuracy; pressed coords for landmarks
                val finalLat = if (nearUser && userLoc != null) userLoc.latitude else p.latitude
                val finalLng = if (nearUser && userLoc != null) userLoc.longitude else p.longitude
                handler(finalLat, finalLng, nearUser)
                return true
            }
        }).also { mapView.overlays.add(it) }
    }

    // ── Boundary polygon/polyline overlays ───────────────────────────────────
    val activeBoundaryOverlays = remember { mutableListOf<org.osmdroid.views.overlay.Overlay>() }
    LaunchedEffect(boundaryOverlays) {
        activeBoundaryOverlays.forEach { mapView.overlays.remove(it) }
        activeBoundaryOverlays.clear()

        val density = context.resources.displayMetrics.density
        for (overlay in boundaryOverlays) {
            for (feature in overlay.polygons) {
                for (ring in feature.rings) {
                    val poly = OsmPolygon().apply {
                        points = ring.toMutableList()
                        fillPaint.color = overlay.fillColor
                        fillPaint.style = android.graphics.Paint.Style.FILL
                        outlinePaint.color = overlay.borderColor
                        outlinePaint.strokeWidth = 2.5f * density
                        outlinePaint.style = android.graphics.Paint.Style.STROKE
                    }
                    mapView.overlays.add(0, poly)
                    activeBoundaryOverlays.add(poly)
                }
            }
            for (feature in overlay.polylines) {
                for (pts in feature.rings) {
                    val polyline = Polyline().apply {
                        setPoints(pts)
                        outlinePaint.color = overlay.borderColor
                        outlinePaint.strokeWidth = 3.5f * density
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        outlinePaint.isAntiAlias = true
                    }
                    mapView.overlays.add(0, polyline)
                    activeBoundaryOverlays.add(polyline)
                }
            }
            // One label per named feature at its own centroid
            if (overlay.showLabel) {
                for (feature in overlay.polygons) {
                    val label = feature.name.ifBlank { overlay.name }
                    val centroid = computeFeatureCentroid(feature.rings) ?: continue
                    val icon = createBoundaryLabelBitmap(context, label, overlay.borderColor)
                    val marker = Marker(mapView).apply {
                        position = centroid; this.icon = icon; title = label; setAnchor(0.5f, 0.5f)
                    }
                    mapView.overlays.add(marker); activeBoundaryOverlays.add(marker)
                }
                for (feature in overlay.polylines) {
                    if (feature.name.isBlank()) continue
                    val label = feature.name
                    val centroid = computeFeatureCentroid(feature.rings) ?: continue
                    val icon = createBoundaryLabelBitmap(context, label, overlay.borderColor)
                    val marker = Marker(mapView).apply {
                        position = centroid; this.icon = icon; title = label; setAnchor(0.5f, 0.5f)
                    }
                    mapView.overlays.add(marker); activeBoundaryOverlays.add(marker)
                }
            }
        }
        mapView.invalidate()
    }

    // ── Route polyline ───────────────────────────────────────────────────────
    val routeOverlay = remember { mutableStateOf<Polyline?>(null) }
    LaunchedEffect(routePoints) {
        routeOverlay.value?.let { mapView.overlays.remove(it) }
        if (!routePoints.isNullOrEmpty()) {
            val density = context.resources.displayMetrics.density
            val line = Polyline().apply {
                setPoints(routePoints)
                outlinePaint.color = android.graphics.Color.parseColor("#4A90E2")
                outlinePaint.strokeWidth = 9f * density
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                outlinePaint.isAntiAlias = true
            }
            mapView.overlays.add(0, line)
            routeOverlay.value = line
        } else {
            routeOverlay.value = null
        }
        mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    myLocationOverlay.enableMyLocation()
                    compassOverlay.enableCompass()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    myLocationOverlay.disableMyLocation()
                    compassOverlay.disableCompass()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            myLocationOverlay.disableMyLocation()
            compassOverlay.disableCompass()
            mapView.onDetach()
        }
    }

    LaunchedEffect(locateTrigger) {
        if (locateTrigger > 0) {
            val location = myLocationOverlay.myLocation
                ?: context.getLastKnownLocation()?.let { GeoPoint(it.latitude, it.longitude) }
            if (location != null) {
                mapView.controller.animateTo(location)
            } else {
                myLocationOverlay.runOnFirstFix {
                    myLocationOverlay.myLocation?.let { point ->
                        mapView.post { mapView.controller.animateTo(point) }
                    }
                }
            }
        }
    }

    LaunchedEffect(searchTarget) {
        searchTarget?.let { mapView.controller.animateTo(it, 17.0, 1000L) }
    }

    // ── Search pin marker ────────────────────────────────────────────────────
    val searchPinIcon = remember { createSearchPinBitmap(context) }
    val searchPinMarkerRef = remember { mutableStateOf<Marker?>(null) }
    val onSearchPinClickRef = rememberUpdatedState(onSearchPinClick)
    LaunchedEffect(searchPin) {
        searchPinMarkerRef.value?.let { mapView.overlays.remove(it) }
        searchPinMarkerRef.value = null
        if (searchPin != null) {
            val marker = Marker(mapView).apply {
                position = searchPin
                icon = searchPinIcon
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ -> onSearchPinClickRef.value?.invoke(); true }
            }
            mapView.overlays.add(marker)
            searchPinMarkerRef.value = marker
        }
        mapView.invalidate()
    }

    // ── Tap stand markers ────────────────────────────────────────────────────
    val tapStandMarkers = remember { mutableListOf<Marker>() }
    LaunchedEffect(tapStands) {
        tapStandMarkers.forEach { mapView.overlays.remove(it) }
        tapStandMarkers.clear()
        tapStands.forEach { item ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(item.tapStand.latitude, item.tapStand.longitude)
                title = item.tapStand.name
                icon = tapMarkerIcon
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ -> onMarkerClick(item); true }
            }
            mapView.overlays.add(marker)
            tapStandMarkers.add(marker)
        }
        mapView.invalidate()
    }

    // ── Landmark markers ─────────────────────────────────────────────────────
    val landmarkMarkers = remember { mutableListOf<Marker>() }
    LaunchedEffect(landmarks) {
        landmarkMarkers.forEach { mapView.overlays.remove(it) }
        landmarkMarkers.clear()
        landmarks.forEach { lm ->
            val icon = createLandmarkMarkerBitmap(context, lm.color, lm.iconType)
            val marker = Marker(mapView).apply {
                position = GeoPoint(lm.latitude, lm.longitude)
                title = lm.name
                snippet = lm.description.ifBlank { null }
                this.icon = icon
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ -> onLandmarkClick(lm); true }
            }
            mapView.overlays.add(marker)
            landmarkMarkers.add(marker)
        }
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.post { compassOverlay.setCompassCenter(view.width - 80f, 80f) }
        }
    )
}

private fun computeFeatureCentroid(rings: List<List<GeoPoint>>): GeoPoint? {
    val all = rings.flatten().ifEmpty { return null }
    return GeoPoint(all.sumOf { it.latitude } / all.size, all.sumOf { it.longitude } / all.size)
}

@Composable
fun CenterPinMapView(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
    onLocationPicked: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnLocationPicked by rememberUpdatedState(onLocationPicked)

    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(lat, lng))
        }
    }

    DisposableEffect(Unit) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                val c = mapView.mapCenter; currentOnLocationPicked(c.latitude, c.longitude); return false
            }
            override fun onZoom(event: ZoomEvent): Boolean {
                val c = mapView.mapCenter; currentOnLocationPicked(c.latitude, c.longitude); return false
            }
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); mapView.onDetach() }
    }

    val pinColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(width = 18.dp, height = 6.dp).background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(50)))
            Canvas(modifier = Modifier.size(52.dp).offset(y = (-26).dp)) {
                val w = size.width; val h = size.height; val cx = w / 2f
                val radius = w * 0.38f; val cy = radius + 3f
                drawCircle(Color.Black.copy(alpha = 0.18f), radius = radius + 2f, center = Offset(cx + 2f, cy + 4f))
                drawCircle(pinColor, radius = radius, center = Offset(cx, cy))
                drawCircle(Color.White, radius = radius * 0.4f, center = Offset(cx, cy))
                val path = Path().apply {
                    moveTo(cx - radius * 0.5f, cy + radius * 0.65f)
                    lineTo(cx + radius * 0.5f, cy + radius * 0.65f)
                    lineTo(cx, h - 2f)
                    close()
                }
                drawPath(path, pinColor)
            }
        }
    }
}
