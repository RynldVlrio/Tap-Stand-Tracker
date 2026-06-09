package com.taptrack.app.ui.screens.list

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
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
import com.taptrack.app.data.local.entity.ProjectEntity
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.ui.screens.map.StatusChip
import java.io.File

private val STATUS_FILTERS = listOf<String?>(null, "Active", "Inactive", "Under Repair")
private val STATUS_LABELS = listOf("All", "Active", "Inactive", "Under Repair")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(onNavigateToDetail: (Long) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TapTrackApplication
    val vm: ListViewModel = viewModel(factory = ListViewModel.factory(app.repository))

    val tapStands by vm.tapStands.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val statusFilter by vm.statusFilter.collectAsStateWithLifecycle()
    val folderFilter by vm.folderFilter.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val isProcessing by vm.isProcessing.collectAsStateWithLifecycle()

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search tap stands…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(50)
            )
            IconButton(onClick = { showImportExportSheet = true }) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Import / Export",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Folder filter row — only shown when folders exist
        if (folders.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
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

        // Status filter row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
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
            EmptyState(Modifier.weight(1f))
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
private fun EmptyState(modifier: Modifier = Modifier) {
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
