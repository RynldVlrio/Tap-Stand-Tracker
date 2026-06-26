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
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.ui.screens.map.BoundaryOverlay
import com.taptrack.app.utils.createLocationDotBitmap
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
    onMarkerClick: (TapStandWithMeters) -> Unit = {},
    onMapViewReady: (MapView) -> Unit = {},
    onLongPressLocation: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Configuration.getInstance().userAgentValue = context.packageName

    val markerIcon = remember { createTapMarkerBitmap(context) }

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

    // Always-fresh reference to the long-press callback so the remember closure stays valid
    val onLongPressRef = rememberUpdatedState(onLongPressLocation)

    remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p ?: return false
                val userLoc = myLocationOverlay.myLocation ?: return false
                val handler = onLongPressRef.value ?: return false
                val userPx  = mapView.projection.toPixels(userLoc, android.graphics.Point())
                val pressPx = mapView.projection.toPixels(p, android.graphics.Point())
                val dx = (userPx.x - pressPx.x).toFloat()
                val dy = (userPx.y - pressPx.y).toFloat()
                if (kotlin.math.sqrt((dx * dx + dy * dy).toDouble()) <= 80.0) {
                    handler(userLoc.latitude, userLoc.longitude)
                    return true
                }
                return false
            }
        }).also { mapView.overlays.add(it) }
    }

    // Boundary overlays – polygons and polylines drawn below everything else
    val activeBoundaryOverlays = remember { mutableListOf<org.osmdroid.views.overlay.Overlay>() }
    LaunchedEffect(boundaryOverlays) {
        activeBoundaryOverlays.forEach { mapView.overlays.remove(it) }
        activeBoundaryOverlays.clear()

        val density = context.resources.displayMetrics.density
        for (overlay in boundaryOverlays) {
            val base = overlay.color
            val r = (base shr 16) and 0xFF
            val g = (base shr 8) and 0xFF
            val b = base and 0xFF
            val fillColor = (0x30 shl 24) or (r shl 16) or (g shl 8) or b
            val strokeColor = (0xDD shl 24) or (r shl 16) or (g shl 8) or b

            for (ring in overlay.polygons) {
                val poly = OsmPolygon().apply {
                    points = ring.toMutableList()
                    fillPaint.color = fillColor
                    fillPaint.style = android.graphics.Paint.Style.FILL
                    outlinePaint.color = strokeColor
                    outlinePaint.strokeWidth = 2.5f * density
                    outlinePaint.style = android.graphics.Paint.Style.STROKE
                }
                mapView.overlays.add(0, poly)
                activeBoundaryOverlays.add(poly)
            }

            for (line in overlay.polylines) {
                val polyline = Polyline().apply {
                    setPoints(line)
                    outlinePaint.color = strokeColor
                    outlinePaint.strokeWidth = 3.5f * density
                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    outlinePaint.isAntiAlias = true
                }
                mapView.overlays.add(0, polyline)
                activeBoundaryOverlays.add(polyline)
            }
        }
        mapView.invalidate()
    }

    // Route polyline – drawn at index 0 so it sits below markers and user dot
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

    // Animate to user's location when the FAB triggers it (or on auto-center at startup)
    LaunchedEffect(locateTrigger) {
        if (locateTrigger > 0) {
            val location = myLocationOverlay.myLocation
                ?: context.getLastKnownLocation()
                    ?.let { GeoPoint(it.latitude, it.longitude) }
            if (location != null) {
                mapView.controller.animateTo(location)
            } else {
                // No cached location yet — center as soon as GPS gets its first fix
                myLocationOverlay.runOnFirstFix {
                    myLocationOverlay.myLocation?.let { point ->
                        mapView.post { mapView.controller.animateTo(point) }
                    }
                }
            }
        }
    }

    // Animate to a searched location
    LaunchedEffect(searchTarget) {
        searchTarget?.let {
            mapView.controller.animateTo(it, 17.0, 1000L)
        }
    }

    LaunchedEffect(tapStands) {
        mapView.overlays.removeAll { it is Marker }
        tapStands.forEach { item ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(item.tapStand.latitude, item.tapStand.longitude)
                title = item.tapStand.name
                icon = markerIcon
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    onMarkerClick(item)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            // Position compass at top-right once the view has been measured
            view.post {
                compassOverlay.setCompassCenter(view.width - 80f, 80f)
            }
        }
    )
}

/**
 * Map for picking a location: the pin is fixed at the center of the view as a
 * Compose overlay; the user moves the map underneath to choose a spot.
 */
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

    // Report new center whenever the user scrolls or zooms
    DisposableEffect(Unit) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                val c = mapView.mapCenter
                currentOnLocationPicked(c.latitude, c.longitude)
                return false
            }
            override fun onZoom(event: ZoomEvent): Boolean {
                val c = mapView.mapCenter
                currentOnLocationPicked(c.latitude, c.longitude)
                return false
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    val pinColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Fixed pin overlay — tip lands exactly at map center
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Drop shadow ellipse at the tip
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 6.dp)
                    .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(50))
            )
            // Modern teardrop pin — Canvas so tip lands at center
            Canvas(
                modifier = Modifier
                    .size(52.dp)
                    .offset(y = (-26).dp)
            ) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val radius = w * 0.38f
                val cy = radius + 3f

                // Soft shadow under the balloon
                drawCircle(
                    color = Color.Black.copy(alpha = 0.18f),
                    radius = radius + 2f,
                    center = Offset(cx + 2f, cy + 4f)
                )
                // Balloon circle
                drawCircle(pinColor, radius = radius, center = Offset(cx, cy))
                // White inner dot
                drawCircle(Color.White, radius = radius * 0.4f, center = Offset(cx, cy))
                // Teardrop pointer
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
