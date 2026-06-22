package com.taptrack.app.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.taptrack.app.TapTrackApplication
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.ui.components.MapDownloadDialog
import com.taptrack.app.ui.components.MapSearchBar
import com.taptrack.app.ui.components.OsmMapView
import com.taptrack.app.utils.formatCoordinates
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TapTrackApplication
    val vm: MapViewModel = viewModel(factory = MapViewModel.factory(app.repository))

    val tapStands by vm.tapStands.collectAsStateWithLifecycle()
    val selected by vm.selectedItem.collectAsStateWithLifecycle()

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    var locateTrigger by remember { mutableIntStateOf(0) }
    var searchTarget by remember { mutableStateOf<GeoPoint?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            locateTrigger++
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(Modifier.fillMaxSize()) {
        OsmMapView(
            tapStands = tapStands,
            locateTrigger = locateTrigger,
            searchTarget = searchTarget,
            modifier = Modifier.fillMaxSize(),
            onMarkerClick = { item -> vm.select(item) },
            onMapViewReady = { mapViewRef = it }
        )

        // Search bar (collapsed by default — shows only icon)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MapSearchBar(
                onResultSelected = { point, _ -> searchTarget = point },
                modifier = Modifier.fillMaxWidth()
            )

            if (!locationPermissions.allPermissionsGranted) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Location permission required for GPS tagging",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // FABs stacked at bottom-end
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Download offline map
            SmallFloatingActionButton(
                onClick = { showDownloadDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download map area for offline use"
                )
            }

            // Locate me — Google Maps style blue circle
            FloatingActionButton(
                onClick = { locateTrigger++ },
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Go to my location"
                )
            }
        }
    }

    if (selected != null) {
        ModalBottomSheet(
            onDismissRequest = { vm.select(null) },
            sheetState = sheetState
        ) {
            TapStandBottomSheet(
                item = selected!!,
                onViewDetails = {
                    vm.select(null)
                    onNavigateToDetail(selected!!.tapStand.id)
                }
            )
        }
    }

    if (showDownloadDialog) {
        val mv = mapViewRef
        if (mv != null) {
            MapDownloadDialog(
                mapView = mv,
                onDismiss = { showDownloadDialog = false }
            )
        }
    }
}

@Composable
private fun TapStandBottomSheet(
    item: TapStandWithMeters,
    onViewDetails: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val photoFile = File(item.tapStand.photoPath)
            if (item.tapStand.photoPath.isNotEmpty() && photoFile.exists()) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.tapStand.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatCoordinates(item.tapStand.latitude, item.tapStand.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${item.meters.size} meter(s) installed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                StatusChip(item.tapStand.status)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val lat = item.tapStand.latitude
                    val lng = item.tapStand.longitude
                    val label = Uri.encode(item.tapStand.name)
                    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                    } catch (_: Exception) {
                        // No navigation app installed
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Navigate")
            }

            Button(
                onClick = onViewDetails,
                modifier = Modifier.weight(1f)
            ) {
                Text("View Details")
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (containerColor, contentColor) = when (status) {
        "Active" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "Inactive" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}
