package com.taptrack.app.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taptrack.app.data.local.entity.ProjectEntity
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.data.repository.TapStandRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class ListViewModel(private val repository: TapStandRepository) : ViewModel() {

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

    fun setQuery(q: String) { _query.value = q }
    fun setStatusFilter(status: String?) { _statusFilter.value = status }
    fun setFolderFilter(id: Long?) { _folderFilter.value = id }

    companion object {
        fun factory(repository: TapStandRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ListViewModel(repository) as T
        }
    }
}
