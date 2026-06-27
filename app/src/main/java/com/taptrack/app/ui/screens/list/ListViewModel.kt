package com.taptrack.app.ui.screens.list

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taptrack.app.data.local.entity.LandmarkEntity
import com.taptrack.app.data.local.entity.ProjectEntity
import com.taptrack.app.data.local.entity.TapStandEntity
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.data.repository.LandmarkRepository
import com.taptrack.app.data.repository.TapStandRepository
import com.taptrack.app.utils.GpxUtils
import com.taptrack.app.utils.KmlUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class ListViewModel(
    private val repository: TapStandRepository,
    private val landmarkRepository: LandmarkRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    private val _folderFilter = MutableStateFlow<Long?>(null)
    val folderFilter: StateFlow<Long?> = _folderFilter.asStateFlow()

    val folders: StateFlow<List<ProjectEntity>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tapStands: StateFlow<List<TapStandWithMeters>> = combine(
        repository.getAllWithMeters(),
        _query,
        _statusFilter,
        _folderFilter
    ) { all, q, status, folderId ->
        all
            .filter { status == null || it.tapStand.status == status }
            .filter { folderId == null || it.tapStand.folderId == folderId }
            .filter {
                q.isBlank() ||
                    it.tapStand.name.contains(q, ignoreCase = true) ||
                    it.tapStand.locationDescription.contains(q, ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val landmarks: StateFlow<List<LandmarkEntity>> = combine(
        landmarkRepository.getAll(),
        _query
    ) { all, q ->
        if (q.isBlank()) all
        else all.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.description.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setStatusFilter(status: String?) { _statusFilter.value = status }
    fun setFolderFilter(id: Long?) { _folderFilter.value = id }

    // ── Import / Export ──────────────────────────────────────────────────────

    sealed class ImportExportResult {
        data class Success(val message: String) : ImportExportResult()
        data class Error(val message: String) : ImportExportResult()
    }

    private val _importExportResult = MutableSharedFlow<ImportExportResult>()
    val importExportResult: SharedFlow<ImportExportResult> = _importExportResult.asSharedFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun importFile(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val fileName = resolveFileName(contentResolver, uri)
                val waypoints = contentResolver.openInputStream(uri)?.use { stream ->
                    when {
                        fileName.endsWith(".gpx", ignoreCase = true) -> GpxUtils.parse(stream)
                        fileName.endsWith(".kmz", ignoreCase = true) -> KmlUtils.parseKmz(stream)
                        fileName.endsWith(".kml", ignoreCase = true) -> KmlUtils.parseKml(stream)
                        else -> GpxUtils.parse(stream)
                    }
                } ?: emptyList()

                if (waypoints.isEmpty()) {
                    _importExportResult.emit(ImportExportResult.Error("No waypoints found in the file."))
                    return@launch
                }

                val today = LocalDate.now().toString()
                val validStatuses = setOf("Active", "Inactive", "Under Repair")
                for (wp in waypoints) {
                    repository.save(
                        TapStandEntity(
                            name = wp.name.ifBlank { "Imported Stand" },
                            locationDescription = wp.description,
                            latitude = wp.latitude,
                            longitude = wp.longitude,
                            photoPath = "",
                            installationDate = wp.installationDate.ifEmpty { today },
                            status = if (wp.status in validStatuses) wp.status else "Active"
                        ),
                        emptyList()
                    )
                }
                _importExportResult.emit(ImportExportResult.Success("Imported ${waypoints.size} tap stand(s)."))
            } catch (e: Exception) {
                _importExportResult.emit(ImportExportResult.Error("Import failed: ${e.message}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportGpx(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val tapStands = repository.getAllTapStandsOnce()
                if (tapStands.isEmpty()) {
                    _importExportResult.emit(ImportExportResult.Error("No tap stands to export."))
                    return@launch
                }
                val gpxContent = GpxUtils.generate(tapStands)
                contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(gpxContent.toByteArray(Charsets.UTF_8))
                }
                _importExportResult.emit(ImportExportResult.Success("Exported ${tapStands.size} tap stand(s) as GPX."))
            } catch (e: Exception) {
                _importExportResult.emit(ImportExportResult.Error("Export failed: ${e.message}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportKmz(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val tapStands = repository.getAllTapStandsOnce()
                if (tapStands.isEmpty()) {
                    _importExportResult.emit(ImportExportResult.Error("No tap stands to export."))
                    return@launch
                }
                val kmlContent = KmlUtils.generateKml(tapStands)
                val kmzBytes = KmlUtils.packKmz(kmlContent)
                contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(kmzBytes)
                }
                _importExportResult.emit(ImportExportResult.Success("Exported ${tapStands.size} tap stand(s) as KMZ."))
            } catch (e: Exception) {
                _importExportResult.emit(ImportExportResult.Error("Export failed: ${e.message}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun resolveFileName(contentResolver: ContentResolver, uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col >= 0 && cursor.moveToFirst()) return cursor.getString(col) ?: ""
        }
        return uri.lastPathSegment ?: ""
    }

    companion object {
        fun factory(repository: TapStandRepository, landmarkRepository: LandmarkRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ListViewModel(repository, landmarkRepository) as T
            }
    }
}
