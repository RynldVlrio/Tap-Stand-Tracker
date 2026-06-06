package com.taptrack.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.data.repository.TapStandRepository
import kotlinx.coroutines.flow.*

class MapViewModel(private val repository: TapStandRepository) : ViewModel() {

    val tapStands: StateFlow<List<TapStandWithMeters>> = repository.getAllWithMeters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedItem = MutableStateFlow<TapStandWithMeters?>(null)
    val selectedItem: StateFlow<TapStandWithMeters?> = _selectedItem.asStateFlow()

    fun select(item: TapStandWithMeters?) { _selectedItem.value = item }

    companion object {
        fun factory(repository: TapStandRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MapViewModel(repository) as T
        }
    }
}
