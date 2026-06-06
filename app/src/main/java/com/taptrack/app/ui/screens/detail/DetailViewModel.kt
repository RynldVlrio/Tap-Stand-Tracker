package com.taptrack.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taptrack.app.data.model.TapStandWithMeters
import com.taptrack.app.data.repository.TapStandRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: TapStandRepository,
    private val tapStandId: Long
) : ViewModel() {

    private val _tapStand = MutableStateFlow<TapStandWithMeters?>(null)
    val tapStand: StateFlow<TapStandWithMeters?> = _tapStand.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        viewModelScope.launch {
            _tapStand.value = repository.getWithMetersById(tapStandId)
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(tapStandId)
            _deleted.value = true
        }
    }

    companion object {
        fun factory(repository: TapStandRepository, id: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DetailViewModel(repository, id) as T
        }
    }
}
