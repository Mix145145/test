package com.sbm.aoi.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sbm.aoi.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val FRAME_ANALYSIS_RATE = intPreferencesKey("frame_analysis_rate")
        val PHOTO_QUALITY = intPreferencesKey("photo_quality")
        val SAVE_TO_GALLERY = booleanPreferencesKey("save_to_gallery")
        val SOUND_ON_DETECTION = booleanPreferencesKey("sound_on_detection")
        val VIBRATION_ON_DETECTION = booleanPreferencesKey("vibration_on_detection")
        val AUTO_PAUSE_ON_SWITCH = booleanPreferencesKey("auto_pause_on_switch")
        val USE_DEEPER_DARK = booleanPreferencesKey("use_deeper_dark")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val IS_ACTIVATED = booleanPreferencesKey("is_activated")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            frameAnalysisRate = prefs[Keys.FRAME_ANALYSIS_RATE] ?: 2,
            photoQuality = prefs[Keys.PHOTO_QUALITY] ?: 90,
            saveToGallery = prefs[Keys.SAVE_TO_GALLERY] ?: false,
            soundOnDetection = prefs[Keys.SOUND_ON_DETECTION] ?: true,
            vibrationOnDetection = prefs[Keys.VIBRATION_ON_DETECTION] ?: true,
            autoPauseOnSwitch = prefs[Keys.AUTO_PAUSE_ON_SWITCH] ?: true,
            useDeeperDark = prefs[Keys.USE_DEEPER_DARK] ?: false,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
        )
    }

    val isActivated: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_ACTIVATED] ?: false
    }

    suspend fun setActivated(isActivated: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_ACTIVATED] = isActivated
        }
    }

    suspend fun updateSettings(update: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                frameAnalysisRate = prefs[Keys.FRAME_ANALYSIS_RATE] ?: 2,
                photoQuality = prefs[Keys.PHOTO_QUALITY] ?: 90,
                saveToGallery = prefs[Keys.SAVE_TO_GALLERY] ?: false,
                soundOnDetection = prefs[Keys.SOUND_ON_DETECTION] ?: true,
                vibrationOnDetection = prefs[Keys.VIBRATION_ON_DETECTION] ?: true,
                autoPauseOnSwitch = prefs[Keys.AUTO_PAUSE_ON_SWITCH] ?: true,
                useDeeperDark = prefs[Keys.USE_DEEPER_DARK] ?: false,
                fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            )
            val updated = update(current)
            prefs[Keys.FRAME_ANALYSIS_RATE] = updated.frameAnalysisRate
            prefs[Keys.PHOTO_QUALITY] = updated.photoQuality
            prefs[Keys.SAVE_TO_GALLERY] = updated.saveToGallery
            prefs[Keys.SOUND_ON_DETECTION] = updated.soundOnDetection
            prefs[Keys.VIBRATION_ON_DETECTION] = updated.vibrationOnDetection
            prefs[Keys.AUTO_PAUSE_ON_SWITCH] = updated.autoPauseOnSwitch
            prefs[Keys.USE_DEEPER_DARK] = updated.useDeeperDark
            prefs[Keys.FONT_SCALE] = updated.fontScale
        }
    }

    suspend fun resetSettings() {
        context.dataStore.edit { it.clear() }
    }
}
