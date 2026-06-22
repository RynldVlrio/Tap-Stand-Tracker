package com.taptrack.app.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taptrack.app.data.local.entity.ProjectEntity
import com.taptrack.app.data.local.entity.TapStandEntity
import com.taptrack.app.data.local.entity.WaterMeterEntity
import com.taptrack.app.data.repository.TapStandRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MeterForm(
    val id: Long = 0,
    val serialNumber: String = "",
    val consumerName: String = "",
    val readingDate: String = "",
    val initialReading: String = "",
    val isNameAutoFilled: Boolean = false
)

data class FormState(
    val name: String = "",
    val locationDescription: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPath: String = "",
    val installationDate: String = "",
    val status: String = "Active",
    val folderId: Long? = null,
    val meters: List<MeterForm> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class AddTapStandViewModel(
    private val repository: TapStandRepository,
    private val editId: Long? = null
) : ViewModel() {

    private val _state = MutableStateFlow(FormState())
    val state: StateFlow<FormState> = _state.asStateFlow()

    val folders: StateFlow<List<ProjectEntity>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (editId != null) {
            viewModelScope.launch {
                repository.getWithMetersById(editId)?.let { item ->
                    _state.update {
                        it.copy(
                            name = item.tapStand.name,
                            locationDescription = item.tapStand.locationDescription,
                            latitude = item.tapStand.latitude,
                            longitude = item.tapStand.longitude,
                            photoPath = item.tapStand.photoPath,
                            installationDate = item.tapStand.installationDate,
                            status = item.tapStand.status,
                            folderId = item.tapStand.folderId,
                            meters = item.meters.map { m ->
                                MeterForm(
                                    id = m.id,
                                    serialNumber = m.serialNumber,
                                    consumerName = m.consumerName,
                                    readingDate = m.readingDate,
                                    initialReading = m.initialReading.toString()
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setLocationDescription(v: String) = _state.update { it.copy(locationDescription = v) }
    fun setCoordinates(lat: Double, lng: Double) = _state.update { it.copy(latitude = lat, longitude = lng) }
    fun setPhoto(path: String) = _state.update { it.copy(photoPath = path) }
    fun setInstallationDate(v: String) = _state.update { it.copy(installationDate = v) }
    fun setStatus(v: String) = _state.update { it.copy(status = v) }
    fun setFolder(id: Long?) = _state.update { it.copy(folderId = id) }

    fun createFolder(name: String, description: String) {
        viewModelScope.launch {
            val newId = repository.saveProject(name.trim(), description.trim())
            _state.update { it.copy(folderId = newId) }
        }
    }

    fun addMeter() = _state.update { it.copy(meters = it.meters + MeterForm()) }

    fun updateMeter(index: Int, form: MeterForm) = _state.update {
        it.copy(meters = it.meters.toMutableList().also { list -> list[index] = form })
    }

    fun removeMeter(index: Int) = _state.update {
        it.copy(meters = it.meters.toMutableList().also { list -> list.removeAt(index) })
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "Tap stand name is required") }
            return
        }
        if (s.latitude == null || s.longitude == null) {
            _state.update { it.copy(error = "GPS coordinates are required") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val entity = TapStandEntity(
                    id = editId ?: 0,
                    name = s.name.trim(),
                    locationDescription = s.locationDescription.trim(),
                    latitude = s.latitude,
                    longitude = s.longitude,
                    photoPath = s.photoPath,
                    installationDate = s.installationDate,
                    status = s.status,
                    folderId = s.folderId
                )
                val meters = s.meters.map {
                    WaterMeterEntity(
                        id = it.id,
                        tapStandId = editId ?: 0,
                        serialNumber = it.serialNumber.trim(),
                        consumerName = it.consumerName.trim(),
                        readingDate = it.readingDate,
                        initialReading = it.initialReading.toDoubleOrNull() ?: 0.0
                    )
                }
                if (editId == null) repository.save(entity, meters)
                else repository.update(entity, meters)
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    companion object {
        fun factory(repository: TapStandRepository, editId: Long?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AddTapStandViewModel(repository, editId) as T
            }
    }
}
