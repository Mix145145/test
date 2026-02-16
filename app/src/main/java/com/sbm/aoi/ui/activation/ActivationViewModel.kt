package com.sbm.aoi.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.aoi.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val isActivated: StateFlow<Boolean?> = settingsRepository.isActivated
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun submitActivationKey(key: String) {
        if (key.trim() == "060606") {
            viewModelScope.launch {
                settingsRepository.setActivated(true)
                _error.value = null
            }
        } else {
            _error.value = "Неверный ключ. Проверьте ввод или получите актуальный ключ на сайте."
        }
    }

    fun clearError() {
        _error.value = null
    }
}
