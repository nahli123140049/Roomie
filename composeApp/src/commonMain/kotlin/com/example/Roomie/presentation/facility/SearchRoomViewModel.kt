package com.example.Roomie.presentation.facility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Roomie.domain.model.Room
import com.example.Roomie.domain.usecase.SearchRoomsFilteredUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchRoomViewModel(
    private val searchRoomsFilteredUseCase: SearchRoomsFilteredUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _minCapacity = MutableStateFlow(0)
    val minCapacity = _minCapacity.asStateFlow()

    private val _maxCapacity = MutableStateFlow(1000) // Large default
    val maxCapacity = _maxCapacity.asStateFlow()

    val searchResults: StateFlow<List<Room>> = combine(
        _searchQuery.debounce(300),
        _minCapacity,
        _maxCapacity
    ) { query, min, max ->
        Triple(query, min, max)
    }.flatMapLatest { (query, min, max) ->
        searchRoomsFilteredUseCase(query, min, max)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCapacityFilterChange(min: Int, max: Int) {
        _minCapacity.value = min
        _maxCapacity.value = max
    }
}
