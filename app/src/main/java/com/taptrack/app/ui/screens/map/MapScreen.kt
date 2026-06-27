package com.taptrack.app.ui.screens.map

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.taptrack.app.TapTrackApplication
import com.taptrack.app.data.local.entity.BoundaryEntity
import com.taptrack.app.data.local.entity.LandmarkEntity
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.ui.components.AddLandmarkSheet
import com.taptrack.app.ui.components.BoundaryColorEditDialog
import com.taptrack.app.ui.components.BoundaryManagerSheet
import com.taptrack.app.ui.components.MapDownloadDialog
import com.taptrack.app.ui.components.MapSearchBar
import com.taptrack.app.ui.components.OsmMapView
import com.taptrack.app.utils.RouteResult
import com.taptrack.app.utils.fetchRoute
import com.taptrack.app.utils.formatCoordinates
import com.taptrack.app.utils.formatDistance
import com.taptrack.app.utils.formatDuration
import com.taptrack.app.utils.getLastKnownLocation
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox
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
    val vm: MapViewModel = viewModel(
        factory = MapViewModel.factory(app.repository, app.boundaryRepository, app.landmarkRepository)
    )

    val tapStands        by vm.tapStands.collectAsStateWithLifecycle()
    val selected         by vm.selectedItem.collectAsStateWithLifecycle()
    val boundaries       by vm.boundaries.collectAsStateWithLifecycle()
    val boundaryOverlays by vm.boundaryOverlays.collectAsStateWithLifecycle()
    val landmarks        by vm.landmarks.collectAsStateWithLifecycle()
    val selectedLandmark by vm.selectedLandmark.collectAsStateWithLifecycle()
    val importState      by vm.importState.collectAsStateWithLifecycle()

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) locationPermissions.launchMultiplePermissionRequest()
    }

    var locateTrigger       by remember { mutableIntStateOf(0) }
    var searchTarget        by remember { mutableStateOf<GeoPoint?>(null) }
    var mapViewRef          by remember { mutableStateOf<MapView?>(null) }
    var showDownloadDialog  by remember { mutableStateOf(false) }
    var showLayersSheet     by remember { mutableStateOf(false) }
    var routeResult         by remember { mutableStateOf<RouteResult?>(null) }
    var isLoadingRoute      by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Long-press / search-pin state
    var longPressLat      by remember { mutableDoubleStateOf(0.0) }
    var longPressLng      by remember { mutableDoubleStateOf(0.0) }
    var longPressNearUser by remember { mutableStateOf(false) }
    var fromSearch        by remember { mutableStateOf(false) }
    var searchPinPoint    by remember { mutableStateOf<GeoPoint?>(null) }
    var searchPinLabel    by remember { mutableStateOf("") }
    var showPinChoiceSheet   by remember { mutableStateOf(false) }
    var showAddLandmarkSheet by remember { mutableStateOf(false) }

    // Boundary color editor
    var editingBoundary by remember { mutableStateOf<BoundaryEntity?>(null) }

    // Landmark edit dialog
    var showEditLandmarkDialog by remember { mutableStateOf(false) }
    var editLandmarkName       by remember { mutableStateOf("") }
    var editLandmarkDesc       by remember { mutableStateOf("") }

    // File picker for boundary import
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importBoundary(context, it) }
    }

    LaunchedEffect(importState) {
        when (val s = importState) {
            is BoundaryImportState.Success -> { Toast.makeText(context, "Layer \"${s.name}\" imported", Toast.LENGTH_SHORT).show(); vm.clearImportState() }
            is BoundaryImportState.Error   -> { Toast.makeText(context, s.message, Toast.LENGTH_LONG).show(); vm.clearImportState() }
            else -> {}
        }
    }
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) locateTrigger++
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(Modifier.fillMaxSize()) {
        OsmMapView(
            tapStands = tapStands,
            locateTrigger = locateTrigger,
            searchTarget = searchTarget,
            modifier = Modifier.fillMaxSize(),
            routePoints = routeResult?.points,
            boundaryOverlays = boundaryOverlays,
            landmarks = landmarks,
            searchPin = searchPinPoint,
            onMarkerClick = { item -> vm.select(item) },
            onLandmarkClick = { lm -> vm.selectLandmark(lm) },
            onSearchPinClick = {
                longPressLat = searchPinPoint!!.latitude
                longPressLng = searchPinPoint!!.longitude
                fromSearch = true
                showPinChoiceSheet = true
            },
            onMapViewReady = { mapViewRef = it },
            onLongPress = { lat, lng, nearUser ->
                // Long press replaces any existing search pin
                searchPinPoint = null
                fromSearch = false
                longPressLat = lat; longPressLng = lng; longPressNearUser = nearUser
                showPinChoiceSheet = true
            }
        )

        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MapSearchBar(
                onResultSelected = { point, label ->
                    searchTarget = point
                    searchPinPoint = point
                    searchPinLabel = label
                    longPressLat = point.latitude
                    longPressLng = point.longitude
                    longPressNearUser = false
                    fromSearch = true
                    showPinChoiceSheet = true
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (!locationPermissions.allPermissionsGranted) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "Location permission required for GPS tagging",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            routeResult?.let { result ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                        Text(text = "${formatDistance(result.distanceMeters)} · ${formatDuration(result.durationSeconds)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                        IconButton(onClick = { routeResult = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear route", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            if (isLoadingRoute) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = { showDownloadDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) { Icon(Icons.Default.Download, contentDescription = "Download map area") }

            SmallFloatingActionButton(
                onClick = { showLayersSheet = true },
                containerColor = if (boundaries.isNotEmpty()) MaterialTheme.colorScheme.tertiaryContainer
                                 else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (boundaries.isNotEmpty()) MaterialTheme.colorScheme.onTertiaryContainer
                               else MaterialTheme.colorScheme.onSecondaryContainer
            ) { Icon(Icons.Default.Layers, contentDescription = "Map layers") }

            FloatingActionButton(
                onClick = { locateTrigger++ },
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.MyLocation, contentDescription = "Go to my location") }
        }
    }

    // ── Loading indicator ────────────────────────────────────────────────────
    if (importState is BoundaryImportState.Loading) {
        AlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text("Importing layer…") },
            text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) })
    }

    // ── Pin choice sheet (long-press or search result) ───────────────────────
    if (showPinChoiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPinChoiceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (fromSearch) Icons.Default.Search else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (fromSearch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            if (fromSearch && searchPinLabel.isNotBlank()) searchPinLabel else "Selected Location",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                        Text(
                            "%.6f, %.6f".format(longPressLat, longPressLng),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Copy coordinates button (always visible)
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("coordinates", "$longPressLat, $longPressLng"))
                        Toast.makeText(context, "Coordinates copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Coordinates")
                }

                // Add Tap Stand: when near GPS OR from search (deliberate coordinate lookup)
                if (longPressNearUser || fromSearch) {
                    OutlinedButton(
                        onClick = {
                            showPinChoiceSheet = false
                            searchPinPoint = null
                            fromSearch = false
                            onNavigateToAdd(longPressLat, longPressLng)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Tap Stand here")
                    }
                }

                Button(
                    onClick = {
                        showPinChoiceSheet = false
                        showAddLandmarkSheet = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pin Landmark here")
                }
            }
        }
    }

    // ── Add landmark sheet ───────────────────────────────────────────────────
    if (showAddLandmarkSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddLandmarkSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AddLandmarkSheet(
                latitude = longPressLat,
                longitude = longPressLng,
                onSave = { name, desc ->
                    vm.addLandmark(name, desc, longPressLat, longPressLng)
                    showAddLandmarkSheet = false
                    searchPinPoint = null
                    fromSearch = false
                },
                onDismiss = { showAddLandmarkSheet = false }
            )
        }
    }

    // ── Landmark detail sheet ────────────────────────────────────────────────
    if (selectedLandmark != null) {
        val lm = selectedLandmark!!
        ModalBottomSheet(
            onDismissRequest = { vm.selectLandmark(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            LandmarkBottomSheet(
                landmark = lm,
                onEdit = {
                    editLandmarkName = lm.name
                    editLandmarkDesc = lm.description
                    showEditLandmarkDialog = true
                },
                onNavigate = {
                    val label = Uri.encode(lm.name)
                    val geoUri = Uri.parse("geo:${lm.latitude},${lm.longitude}?q=${lm.latitude},${lm.longitude}($label)")
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri)) }
                    catch (_: Exception) { Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show() }
                },
                onShare = {
                    val text = "${lm.name}\n${lm.latitude}, ${lm.longitude}" +
                        if (lm.description.isNotBlank()) "\n${lm.description}" else ""
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share landmark"))
                },
                onRoute = {
                    val lmCopy = lm; val mv = mapViewRef
                    vm.selectLandmark(null); isLoadingRoute = true
                    coroutineScope.launch {
                        val loc = context.getLastKnownLocation()
                        if (loc != null) {
                            val result = fetchRoute(loc.latitude, loc.longitude, lmCopy.latitude, lmCopy.longitude, context.packageName)
                            routeResult = result
                            if (result != null && mv != null) {
                                val bbox = BoundingBox(
                                    maxOf(loc.latitude, lmCopy.latitude) + 0.002,
                                    maxOf(loc.longitude, lmCopy.longitude) + 0.002,
                                    minOf(loc.latitude, lmCopy.latitude) - 0.002,
                                    minOf(loc.longitude, lmCopy.longitude) - 0.002
                                )
                                mv.post { mv.zoomToBoundingBox(bbox, true, 100) }
                            }
                        } else { Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show() }
                        isLoadingRoute = false
                    }
                },
                onCopyCoords = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("coordinates", "${lm.latitude}, ${lm.longitude}"))
                    Toast.makeText(context, "Coordinates copied", Toast.LENGTH_SHORT).show()
                },
                onDelete = { vm.deleteLandmark(lm.id); vm.selectLandmark(null) },
                onDismiss = { vm.selectLandmark(null) }
            )
        }
    }

    // ── Landmark edit dialog ─────────────────────────────────────────────────
    if (showEditLandmarkDialog && selectedLandmark != null) {
        AlertDialog(
            onDismissRequest = { showEditLandmarkDialog = false },
            title = { Text("Edit Landmark") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editLandmarkName,
                        onValueChange = { editLandmarkName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLandmarkDesc,
                        onValueChange = { editLandmarkDesc = it },
                        label = { Text("Description (optional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editLandmarkName.isNotBlank()) {
                            vm.updateLandmark(selectedLandmark!!.copy(
                                name = editLandmarkName.trim(),
                                description = editLandmarkDesc.trim()
                            ))
                        }
                        showEditLandmarkDialog = false
                    },
                    enabled = editLandmarkName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditLandmarkDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Tap stand detail sheet ───────────────────────────────────────────────
    if (selected != null) {
        ModalBottomSheet(onDismissRequest = { vm.select(null) }, sheetState = sheetState) {
            TapStandBottomSheet(
                item = selected!!,
                onViewDetails = { vm.select(null); onNavigateToDetail(selected!!.tapStand.id) },
                onDirections = {
                    val tapItem = selected!!; val mv = mapViewRef; vm.select(null); isLoadingRoute = true
                    coroutineScope.launch {
                        val loc = context.getLastKnownLocation()
                        if (loc != null) {
                            val result = fetchRoute(loc.latitude, loc.longitude, tapItem.tapStand.latitude, tapItem.tapStand.longitude, context.packageName)
                            routeResult = result
                            if (result != null && mv != null) {
                                val bbox = BoundingBox(
                                    maxOf(loc.latitude, tapItem.tapStand.latitude) + 0.002,
                                    maxOf(loc.longitude, tapItem.tapStand.longitude) + 0.002,
                                    minOf(loc.latitude, tapItem.tapStand.latitude) - 0.002,
                                    minOf(loc.longitude, tapItem.tapStand.longitude) - 0.002
                                )
                                mv.post { mv.zoomToBoundingBox(bbox, true, 100) }
                            }
                        }
                        isLoadingRoute = false
                    }
                }
            )
        }
    }

    // ── Layers sheet ─────────────────────────────────────────────────────────
    if (showLayersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLayersSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BoundaryManagerSheet(
                boundaries = boundaries,
                onToggleVisibility = { vm.toggleVisibility(it) },
                onDelete = { vm.deleteBoundary(it) },
                onEditStyle = { boundary -> editingBoundary = boundary; showLayersSheet = false },
                onImport = { showLayersSheet = false; importLauncher.launch(arrayOf("*/*")) }
            )
        }
    }

    // ── Boundary color edit dialog ────────────────────────────────────────────
    editingBoundary?.let { entity ->
        BoundaryColorEditDialog(
            entity = entity,
            onDismiss = { editingBoundary = null },
            onApply = { fill, border, label ->
                vm.updateBoundaryStyle(entity, fill, border, label)
                editingBoundary = null
            }
        )
    }

    if (showDownloadDialog) {
        mapViewRef?.let { MapDownloadDialog(mapView = it, onDismiss = { showDownloadDialog = false }) }
    }
}

@Composable
private fun LandmarkBottomSheet(
    landmark: LandmarkEntity,
    onEdit: () -> Unit,
    onNavigate: () -> Unit,
    onShare: () -> Unit,
    onRoute: () -> Unit,
    onCopyCoords: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateStr = remember(landmark.createdAt) {
        SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()).format(Date(landmark.createdAt))
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text(landmark.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Pinned Landmark", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()

        // Coordinates row with copy button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Coordinates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${landmark.latitude}, ${landmark.longitude}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onCopyCoords, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy coordinates", modifier = Modifier.size(18.dp))
            }
        }

        // Description
        if (landmark.description.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(landmark.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Created date
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider()

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionIconButton(icon = Icons.Default.Edit,       label = "Edit",     onClick = onEdit,     modifier = Modifier.weight(1f))
            ActionIconButton(icon = Icons.Default.Navigation, label = "Navigate", onClick = onNavigate, modifier = Modifier.weight(1f))
            ActionIconButton(icon = Icons.Default.Share,      label = "Share",    onClick = onShare,    modifier = Modifier.weight(1f))
        }

        // Route on map
        OutlinedButton(onClick = onRoute, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Show Route on Map")
        }

        // Close / Delete
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Close") }
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete")
            }
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TapStandBottomSheet(item: TapStandWithMeters, onViewDetails: () -> Unit, onDirections: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val photoFile = File(item.tapStand.photoPath)
            if (item.tapStand.photoPath.isNotEmpty() && photoFile.exists()) {
                AsyncImage(model = photoFile, contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            } else {
                Surface(modifier = Modifier.size(80.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(item.tapStand.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(formatCoordinates(item.tapStand.latitude, item.tapStand.longitude), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text("${item.meters.size} meter(s) installed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                StatusChip(item.tapStand.status)
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDirections, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp)); Text("Show Route on Map")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val lat = item.tapStand.latitude; val lng = item.tapStand.longitude
                        val label = Uri.encode(item.tapStand.name)
                        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri)) } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f)
                ) { Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Navigate") }
                Button(onClick = onViewDetails, modifier = Modifier.weight(1f)) { Text("View Details") }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (containerColor, contentColor) = when (status) {
        "Active"   -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "Inactive" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else       -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(shape = RoundedCornerShape(50), color = containerColor) {
        Text(status, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}
