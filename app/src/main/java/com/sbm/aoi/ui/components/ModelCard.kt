package com.sbm.aoi.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sbm.aoi.data.model.AiModel
import com.sbm.aoi.data.model.HighlightMethod
import com.sbm.aoi.ui.theme.ModelColors
import com.sbm.aoi.ui.theme.Primary
import com.sbm.aoi.ui.theme.Surface
import com.sbm.aoi.ui.theme.SurfaceVariant

@Composable
fun ModelCard(
    model: AiModel,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onColorChange: (Int) -> Unit,
    onConfidenceChange: (Float) -> Unit,
    onIouChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onToggleLabels: () -> Unit,
    onToggleConfidence: () -> Unit,
    onHighlightMethodChange: (HighlightMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (model.isActive) SurfaceVariant else Surface,
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (model.isActive) {
            androidx.compose.foundation.BorderStroke(1.dp, Primary)
        } else null,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Цветовой индикатор
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(model.colorArgb))
                        .clickable { showColorPicker = !showColorPicker },
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${model.labels.size} классов | ${model.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (!model.isActive) {
                    IconButton(onClick = onActivate) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Активировать",
                            tint = Primary,
                        )
                    }
                }

                IconButton(onClick = onRename) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Переименовать",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Настройки",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Выбор цвета
            AnimatedVisibility(visible = showColorPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModelColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (color.toArgb() == model.colorArgb) {
                                        Modifier.border(2.dp, Color.White, CircleShape)
                                    } else Modifier
                                )
                                .clickable {
                                    onColorChange(color.toArgb())
                                    showColorPicker = false
                                },
                        )
                    }
                }
            }

            // Развёрнутые настройки
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Confidence
                    SettingSlider(
                        label = "Confidence",
                        value = model.confidenceThreshold,
                        onValueChange = onConfidenceChange,
                        valueRange = 0.05f..0.95f,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // IoU
                    SettingSlider(
                        label = "IoU",
                        value = model.iouThreshold,
                        onValueChange = onIouChange,
                        valueRange = 0.1f..0.9f,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Прозрачность
                    SettingSlider(
                        label = "Прозрачность",
                        value = model.overlayOpacity,
                        onValueChange = onOpacityChange,
                        valueRange = 0.1f..1.0f,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Переключатели
                    SettingSwitch(
                        label = "Подписи классов",
                        checked = model.showLabels,
                        onCheckedChange = { onToggleLabels() },
                    )

                    SettingSwitch(
                        label = "Показывать confidence",
                        checked = model.showConfidence,
                        onCheckedChange = { onToggleConfidence() },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Метод выделения
                    Text(
                        text = "Метод выделения",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HighlightMethod.entries.forEach { method ->
                            val label = when (method) {
                                HighlightMethod.FRAME_ONLY -> "Рамка"
                                HighlightMethod.FRAME_AND_FILL -> "Рамка+заливка"
                                HighlightMethod.MASK -> "Маска"
                            }
                            val isSelected = model.highlightMethod == method
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Primary.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable { onHighlightMethodChange(method) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(100.dp),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
            ),
        )
        Text(
            text = "%.2f".format(value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp),
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
