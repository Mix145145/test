package com.sbm.aoi.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.aoi.data.model.AppSettings
import com.sbm.aoi.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings(),
    )

    fun setFrameAnalysisRate(rate: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(frameAnalysisRate = rate) }
        }
    }

    fun setPhotoQuality(quality: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(photoQuality = quality) }
        }
    }

    fun toggleSaveToGallery() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(saveToGallery = !it.saveToGallery) }
        }
    }

    fun toggleSoundOnDetection() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(soundOnDetection = !it.soundOnDetection) }
        }
    }

    fun toggleVibrationOnDetection() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(vibrationOnDetection = !it.vibrationOnDetection) }
        }
    }

    fun toggleAutoPauseOnSwitch() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(autoPauseOnSwitch = !it.autoPauseOnSwitch) }
        }
    }

    fun toggleDeeperDark() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(useDeeperDark = !it.useDeeperDark) }
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(fontScale = scale) }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.resetSettings()
        }
    }
}
