package com.taptrack.app.ui.screens.list

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.taptrack.app.TapTrackApplication
import com.taptrack.app.data.local.entity.LandmarkEntity
import com.taptrack.app.data.local.entity.ProjectEntity
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.ui.screens.map.StatusChip
import com.taptrack.app.utils.formatCoordinates
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val STATUS_FILTERS = listOf<String?>(null, "Active", "Inactive", "Under Repair")
private val STATUS_LABELS = listOf("All", "Active", "Inactive", "Under Repair")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(onNavigateToDetail: (Long) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TapTrackApplication
    val vm: ListViewModel = viewModel(
        factory = ListViewModel.factory(app.repository, app.landmarkRepository)
    )

    val tapStands by vm.tapStands.collectAsStateWithLifecycle()
    val landmarks by vm.landmarks.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val statusFilter by vm.statusFilter.collectAsStateWithLifecycle()
    val folderFilter by vm.folderFilter.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val isProcessing by vm.isProcessing.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showImportExportSheet by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importFile(context.contentResolver, it) }
    }
    val exportGpxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri ->
        uri?.let { vm.exportGpx(context.contentResolver, it) }
    }
    val exportKmzLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.google-earth.kmz")
    ) { uri ->
        uri?.let { vm.exportKmz(context.contentResolver, it) }
    }

    LaunchedEffect(Unit) {
        vm.importExportResult.collect { result ->
            val msg = when (result) {
                is ListViewModel.ImportExportResult.Success -> result.message
                is ListViewModel.ImportExportResult.Error -> result.message
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    if (showImportExportSheet) {
        ImportExportSheet(
            isProcessing = isProcessing,
            onDismiss = { showImportExportSheet = false },
            onImport = {
                showImportExportSheet = false
                importLauncher.launch(arrayOf("*/*"))
            },
            onExportGpx = {
                showImportExportSheet = false
                exportGpxLauncher.launch("taptrack_export.gpx")
            },
            onExportKmz = {
                showImportExportSheet = false
                exportKmzLauncher.launch("taptrack_export.kmz")
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        // ── Search bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(if (selectedTab == 0) "Search tap stands…" else "Search landmarks…")
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(50)
            )
            if (selectedTab == 0) {
                IconButton(onClick = { showImportExportSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Import / Export",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }

        // ── Tab row ───────────────────────────────────────────────────────────
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                text = {
                    Text(
                        if (tapStands.isNotEmpty()) "Tap Stands (${tapStands.size})"
                        else "Tap Stands"
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                text = {
                    Text(
                        if (landmarks.isNotEmpty()) "Landmarks (${landmarks.size})"
                        else "Landmarks"
                    )
                }
            )
        }

        // ── Tab content ───────────────────────────────────────────────────────
        when (selectedTab) {
            0 -> {
                // Folder filter — only shown when folders exist
                if (folders.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = folderFilter == null,
                                onClick = { vm.setFolderFilter(null) },
                                label = { Text("All Folders") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        items(folders) { folder ->
                            FilterChip(
                                selected = folderFilter == folder.id,
                                onClick = { vm.setFolderFilter(folder.id) },
                                label = { Text(folder.name) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Status filter
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(
                        top = if (folders.isEmpty()) 8.dp else 0.dp,
                        bottom = 8.dp
                    )
                ) {
                    items(STATUS_FILTERS.indices.toList()) { i ->
                        FilterChip(
                            selected = statusFilter == STATUS_FILTERS[i],
                            onClick = { vm.setStatusFilter(STATUS_FILTERS[i]) },
                            label = { Text(STATUS_LABELS[i]) }
                        )
                    }
                }

                if (tapStands.isEmpty()) {
                    TapStandEmptyState(Modifier.weight(1f))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(tapStands, key = { it.tapStand.id }) { item ->
                            TapStandListItem(
                                item = item,
                                folders = folders,
                                onClick = { onNavigateToDetail(item.tapStand.id) }
                            )
                        }
                    }
                }
            }

            1 -> {
                if (landmarks.isEmpty()) {
                    LandmarkEmptyState(Modifier.weight(1f))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(landmarks, key = { it.id }) { landmark ->
                            LandmarkListItem(landmark = landmark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TapStandListItem(
    item: TapStandWithMeters,
    folders: List<ProjectEntity>,
    onClick: () -> Unit
) {
    val folder = folders.find { it.id == item.tapStand.folderId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val photoFile = File(item.tapStand.photoPath)
            if (item.tapStand.photoPath.isNotEmpty() && photoFile.exists()) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.tapStand.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.tapStand.locationDescription.isNotEmpty()) {
                    Text(
                        text = item.tapStand.locationDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(item.tapStand.status)
                    Text(
                        text = "${item.meters.size} meter(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (folder != null) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LandmarkListItem(landmark: LandmarkEntity) {
    val context = LocalContext.current
    val dateStr = remember(landmark.createdAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(landmark.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFF3E0)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = landmark.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatCoordinates(landmark.latitude, landmark.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (landmark.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = landmark.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        val label = Uri.encode(landmark.name)
                        val geoUri = Uri.parse(
                            "geo:${landmark.latitude},${landmark.longitude}" +
                                "?q=${landmark.latitude},${landmark.longitude}($label)"
                        )
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                        } catch (_: Exception) {
                            Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val text = "${landmark.name}\n${landmark.latitude}, ${landmark.longitude}" +
                            if (landmark.description.isNotBlank()) "\n${landmark.description}" else ""
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share landmark"))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TapStandEmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No tap stands recorded yet.\nTap + to add your first one.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LandmarkEmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No pinned landmarks yet.\nLong-press the map or use search to pin a location.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportExportSheet(
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onExportGpx: () -> Unit,
    onExportKmz: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Import / Export",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isProcessing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Processing…", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    text = "IMPORT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Import GPX / KML / KMZ") },
                    supportingContent = { Text("Add tap stands from a file saved by Google Earth, OsmAnd+, or any GPX/KML app") },
                    leadingContent = {
                        Icon(Icons.Default.FileUpload, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onImport)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "EXPORT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Export as GPX") },
                    supportingContent = { Text("Save all tap stands as GPX waypoints (compatible with OsmAnd+, GPS apps)") },
                    leadingContent = {
                        Icon(Icons.Default.FileDownload, contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onExportGpx)
                )
                ListItem(
                    headlineContent = { Text("Export as KMZ") },
                    supportingContent = { Text("Save all tap stands as KMZ (compatible with Google Earth)") },
                    leadingContent = {
                        Icon(Icons.Default.FileDownload, contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onExportKmz)
                )
            }
        }
    }
}
