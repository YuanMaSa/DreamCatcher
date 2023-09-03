package edu.vt.cs5254.dreamcatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

class DreamDetailViewModel(dreamId: UUID) : ViewModel() {

    private val dreamRepository = DreamRepository.get()
    private val _dream: MutableStateFlow<Dream?> = MutableStateFlow(null)
    var dream: StateFlow<Dream?> = _dream.asStateFlow()

    init {
        viewModelScope.launch {
            _dream.value = dreamRepository.getDream(dreamId)
        }
    }

    fun updateDream(onUpdate: (Dream) -> Dream) {
        _dream.update { prevDream ->
            val newDream = prevDream?.let { onUpdate(it) } ?: return
            if (newDream == prevDream && newDream.entries == prevDream.entries) {
                return
            }
            newDream.copy(lastUpdated = Date()).apply { entries = newDream.entries }
        }
    }

    override fun onCleared() {
        super.onCleared()
        dream.value?.let { dreamRepository.updateDream(it) }
    }

}

class DreamDetailViewModelFactory(
    private val dreamId: UUID
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DreamDetailViewModel(dreamId) as T
    }
}