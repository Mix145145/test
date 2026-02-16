package com.sbm.aoi.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.aoi.ui.theme.OnSurface
import com.sbm.aoi.ui.theme.Primary
import com.sbm.aoi.ui.theme.Surface

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Общие настройки
        SettingsSection(title = "Общие") {
            // Частота анализа кадров
            SettingItem(label = "Частота анализа кадров") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1, 2, 3).forEach { rate ->
                        val selected = settings.frameAnalysisRate == rate
                        TextButton(
                            onClick = { viewModel.setFrameAnalysisRate(rate) },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (selected) Primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else OnSurface,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("$rate")
                        }
                    }
                }
            }

            // Качество фото
            SettingItem(label = "Качество фото: ${settings.photoQuality}%") {
                Slider(
                    value = settings.photoQuality.toFloat(),
                    onValueChange = { viewModel.setPhotoQuality(it.toInt()) },
                    valueRange = 50f..100f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                    ),
                )
            }

            // Переключатели
            SettingSwitch(
                label = "Сохранять фото в галерею",
                checked = settings.saveToGallery,
                onCheckedChange = { viewModel.toggleSaveToGallery() },
            )

            SettingSwitch(
                label = "Звук при обнаружении",
                checked = settings.soundOnDetection,
                onCheckedChange = { viewModel.toggleSoundOnDetection() },
            )

            SettingSwitch(
                label = "Вибрация при обнаружении",
                checked = settings.vibrationOnDetection,
                onCheckedChange = { viewModel.toggleVibrationOnDetection() },
            )

            SettingSwitch(
                label = "Автопауза при переходе",
                checked = settings.autoPauseOnSwitch,
                onCheckedChange = { viewModel.toggleAutoPauseOnSwitch() },
            )
        }

        // Интерфейс
        SettingsSection(title = "Интерфейс") {
            SettingSwitch(
                label = "Сверхтёмная тема",
                checked = settings.useDeeperDark,
                onCheckedChange = { viewModel.toggleDeeperDark() },
            )

            SettingItem(label = "Размер шрифта: ${"%.1f".format(settings.fontScale)}x") {
                Slider(
                    value = settings.fontScale,
                    onValueChange = { viewModel.setFontScale(it) },
                    valueRange = 0.8f..1.4f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                    ),
                )
            }
        }

        // Сброс
        Button(
            onClick = { showResetDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сбросить настройки")
        }

        // Информация о приложении
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SBM AOI",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Версия 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface,
                )
                Text(
                    text = "Автоматическая оптическая инспекция печатных плат",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сбросить настройки?") },
            text = { Text("Все настройки будут возвращены к значениям по умолчанию.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetSettings()
                    showResetDialog = false
                }) {
                    Text("Сбросить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена", color = OnSurface)
                }
            },
            containerColor = Surface,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingItem(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = Primary.copy(alpha = 0.3f),
            ),
        )
    }
}
