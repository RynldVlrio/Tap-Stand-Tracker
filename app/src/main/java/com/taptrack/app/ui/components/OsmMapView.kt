package com.taptrack.app.ui.components

import android.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.utils.createTapMarkerBitmap
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapView(
    tapStands: List<TapStandWithMeters>,
    modifier: Modifier = Modifier,
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
        modifier = modifier
    )
}

@Composable
fun PinMapView(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
    onLocationPicked: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Configuration.getInstance().userAgentValue = context.packageName

    val markerIcon = remember { createTapMarkerBitmap(context) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
        }
    }

    val markerRef = remember { mutableStateOf<Marker?>(null) }

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

    LaunchedEffect(lat, lng) {
        val geoPoint = GeoPoint(lat, lng)
        mapView.controller.animateTo(geoPoint)
        markerRef.value?.let { mapView.overlays.remove(it) }
        val marker = Marker(mapView).apply {
            position = geoPoint
            icon = markerIcon
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            isDraggable = true
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragEnd(marker: Marker) {
                    onLocationPicked(marker.position.latitude, marker.position.longitude)
                }
                override fun onMarkerDragStart(marker: Marker) {}
            })
        }
        mapView.overlays.add(marker)
        markerRef.value = marker
        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
