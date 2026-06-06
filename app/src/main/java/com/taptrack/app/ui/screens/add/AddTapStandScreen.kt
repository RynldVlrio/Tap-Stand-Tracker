package com.taptrack.app.ui.screens.add

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.taptrack.app.TapTrackApplication
import com.taptrack.app.ui.components.CameraCapture
import com.taptrack.app.ui.components.CenterPinMapView
import com.taptrack.app.utils.formatCoordinates
import com.taptrack.app.utils.locationFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val STATUSES = listOf("Active", "Inactive", "Under Repair")

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTapStandScreen(
    initialLat: Double?,
    initialLng: Double?,
    tapStandId: Long?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TapTrackApplication
    val vm: AddTapStandViewModel = viewModel(
        factory = AddTapStandViewModel.factory(app.repository, tapStandId),
        key = "add_edit_${tapStandId}"
    )

    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCamera by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }

    val locationPerm = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPerm = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (initialLat != null && initialLng != null) {
            vm.setCoordinates(initialLat, initialLng)
        } else if (tapStandId == null && !locationPerm.status.isGranted) {
            locationPerm.launchPermissionRequest()
        }
    }

    LaunchedEffect(locationPerm.status.isGranted) {
        if (locationPerm.status.isGranted && tapStandId == null && initialLat == null) {
            context.locationFlow().collect { loc ->
                if (vm.state.value.latitude == null) {
                    vm.setCoordinates(loc.latitude, loc.longitude)
                }
            }
        }
    }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    if (showCamera) {
        CameraCapture(
            lat = state.latitude,
            lng = state.longitude,
            onImageCaptured = { path ->
                vm.setPhoto(path)
                showCamera = false
            },
            onDismiss = { showCamera = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tapStandId == null) "Add Tap Stand" else "Edit Tap Stand") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = vm::save,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Tap Stand")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // GPS Coordinates Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    if (state.latitude != null && state.longitude != null) {
                        Column {
                            Text(
                                "GPS Location",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                formatCoordinates(state.latitude!!, state.longitude!!),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        Text(
                            "Acquiring GPS location…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Center-pin map — move the map to position the pin
            if (state.latitude != null && state.longitude != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Adjust Pin Location",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Move the map to position the pin on the exact spot",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CenterPinMapView(
                    lat = state.latitude!!,
                    lng = state.longitude!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    onLocationPicked = { lat, lng -> vm.setCoordinates(lat, lng) }
                )
            }

            // Photo
            Text(
                "Photo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.photoPath.isNotEmpty() && File(state.photoPath).exists()) {
                Box {
                    AsyncImage(
                        model = File(state.photoPath),
                        contentDescription = "Captured photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    TextButton(
                        onClick = { showCamera = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retake")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        if (cameraPerm.status.isGranted) {
                            showCamera = true
                        } else {
                            cameraPerm.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Take Photo")
                }
            }

            // Form fields
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::setName,
                label = { Text("Tap Stand Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null) }
            )

            OutlinedTextField(
                value = state.locationDescription,
                onValueChange = vm::setLocationDescription,
                label = { Text("Location Description") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Near Barangay Hall") },
                minLines = 2,
                maxLines = 3,
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) }
            )

            // Installation Date
            OutlinedTextField(
                value = state.installationDate,
                onValueChange = {},
                label = { Text("Installation Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
                    }
                },
                placeholder = { Text("Tap to select") }
            )

            // Status dropdown
            ExposedDropdownMenuBox(
                expanded = statusMenuExpanded,
                onExpandedChange = { statusMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = { statusMenuExpanded = false }
                ) {
                    STATUSES.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s) },
                            onClick = { vm.setStatus(s); statusMenuExpanded = false }
                        )
                    }
                }
            }

            // Water Meters section
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Water Meters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = vm::addMeter) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Meter")
                }
            }

            if (state.meters.isEmpty()) {
                Text(
                    "No meters added yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            state.meters.forEachIndexed { index, meter ->
                MeterFormItem(
                    index = index,
                    form = meter,
                    onUpdate = { vm.updateMeter(index, it) },
                    onRemove = { vm.removeMeter(index) }
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val fmt = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                        vm.setInstallationDate(fmt.format(Date(ms)))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun MeterFormItem(
    index: Int,
    form: MeterForm,
    onUpdate: (MeterForm) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Meter ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            OutlinedTextField(
                value = form.serialNumber,
                onValueChange = { onUpdate(form.copy(serialNumber = it)) },
                label = { Text("Serial Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.consumerName,
                onValueChange = { onUpdate(form.copy(consumerName = it)) },
                label = { Text("Consumer Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = form.readingDate,
                    onValueChange = { onUpdate(form.copy(readingDate = it)) },
                    label = { Text("Reading Date") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("MM/DD/YYYY") }
                )
                OutlinedTextField(
                    value = form.initialReading,
                    onValueChange = { onUpdate(form.copy(initialReading = it)) },
                    label = { Text("Initial Reading") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
    }
}
