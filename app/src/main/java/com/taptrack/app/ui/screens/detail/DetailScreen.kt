package com.taptrack.app.ui.screens.detail

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.taptrack.app.TapTrackApplication
import com.taptrack.app.data.local.entity.WaterMeterEntity
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.ui.screens.map.StatusChip
import com.taptrack.app.utils.formatCoordinates
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    tapStandId: Long,
    onNavigateToEdit: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TapTrackApplication
    val vm: DetailViewModel = viewModel(factory = DetailViewModel.factory(app.repository, tapStandId))

    val item by vm.tapStand.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.tapStand?.name ?: "Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    item?.let { i ->
                        IconButton(onClick = {
                            val text = buildShareText(i)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Tap Stand"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = onNavigateToEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        item?.let { i ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                PhotoSection(i.tapStand.photoPath)
                InfoSection(i)
                if (i.meters.isNotEmpty()) {
                    MetersSection(i.meters)
                }
                Spacer(Modifier.height(32.dp))
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Tap Stand") },
            text = { Text("Are you sure you want to delete this tap stand? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; vm.delete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PhotoSection(photoPath: String) {
    val file = File(photoPath)
    if (photoPath.isNotEmpty() && file.exists()) {
        AsyncImage(
            model = file,
            contentDescription = "Tap stand photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "No photo",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSection(item: TapStandWithMeters) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.tapStand.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(item.tapStand.status)
            }

            HorizontalDivider()

            InfoRow(label = "Location", value = item.tapStand.locationDescription.ifEmpty { "—" })
            InfoRow(
                label = "GPS",
                value = formatCoordinates(item.tapStand.latitude, item.tapStand.longitude)
            )
            InfoRow(
                label = "Installed",
                value = item.tapStand.installationDate.ifEmpty { "—" }
            )
            InfoRow(label = "Meters", value = "${item.meters.size} installed")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun MetersSection(meters: List<WaterMeterEntity>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Water Meters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        meters.forEachIndexed { index, meter ->
            MeterCard(index + 1, meter)
            if (index < meters.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MeterCard(index: Int, meter: WaterMeterEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Meter #$index",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
            InfoRow("Serial No.", meter.serialNumber.ifEmpty { "—" })
            InfoRow("Consumer", meter.consumerName.ifEmpty { "—" })
            InfoRow("Reading Date", meter.readingDate.ifEmpty { "—" })
            InfoRow("Initial Reading", if (meter.initialReading > 0) "${meter.initialReading}" else "0.0")
        }
    }
}

private fun buildShareText(item: TapStandWithMeters): String = buildString {
    appendLine("=== TapTrack — Tap Stand Info ===")
    appendLine("Name: ${item.tapStand.name}")
    appendLine("Location: ${item.tapStand.locationDescription}")
    appendLine("GPS: ${formatCoordinates(item.tapStand.latitude, item.tapStand.longitude)}")
    appendLine("Status: ${item.tapStand.status}")
    appendLine("Installed: ${item.tapStand.installationDate}")
    if (item.meters.isNotEmpty()) {
        appendLine("\n--- Water Meters (${item.meters.size}) ---")
        item.meters.forEachIndexed { i, m ->
            appendLine("Meter ${i + 1}: ${m.serialNumber} | ${m.consumerName} | Reading: ${m.initialReading}")
        }
    }
}
