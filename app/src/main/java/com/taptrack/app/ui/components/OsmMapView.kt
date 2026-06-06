package com.taptrack.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.utils.createTapMarkerBitmap
import com.taptrack.app.utils.getLastKnownLocation
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
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
    onMarkerClick: (TapStandWithMeters) -> Unit = {}
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

    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            mapView.overlays.add(0, this)
        }
    }

    val compassOverlay = remember {
        CompassOverlay(context, InternalCompassOrientationProvider(context), mapView).apply {
            enableCompass()
            mapView.overlays.add(this)
        }
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

    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Fixed pin overlay — tip of the icon aligns with exact map center
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Drop shadow at the tip point (stays at center, no offset)
            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 5.dp)
                    .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(50))
            )
            // Pin icon — shifted up so its tip lands exactly at center
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Pin location",
                modifier = Modifier
                    .size(44.dp)
                    .offset(y = (-22).dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
