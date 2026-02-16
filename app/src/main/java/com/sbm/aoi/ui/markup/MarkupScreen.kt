package com.sbm.aoi.ui.markup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.aoi.data.model.DefectType
import com.sbm.aoi.ui.components.BboxDrawer
import com.sbm.aoi.ui.theme.OnSurface
import com.sbm.aoi.ui.theme.Primary

@Composable
fun MarkupScreen(
    viewModel: MarkupViewModel = hiltViewModel(),
) {
    val bitmap by viewModel.bitmap.collectAsState()
    val defects by viewModel.defects.collectAsState()
    val selectedDefectId by viewModel.selectedDefectId.collectAsState()
    val isDrawing by viewModel.isDrawing.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { viewModel.loadImage(it) }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val selectedDefect = defects.firstOrNull { it.id == selectedDefectId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (bitmap == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Draw,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Primary.copy(alpha = 0.5f),
                )
                Text(
                    text = "Ручная разметка",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Откройте изображение и выделяйте дефекты с зумом и панорамированием.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                )
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Открыть фото")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.toggleDrawingMode() }) {
                        Icon(
                            if (isDrawing) Icons.Default.Draw else Icons.Default.TouchApp,
                            contentDescription = if (isDrawing) "Режим рисования" else "Режим выбора",
                            tint = Primary,
                        )
                    }

                    Text(
                        text = if (isDrawing) "Рисование" else "Панорамирование/выбор",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Дефектов: ${defects.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurface,
                    )

                    if (selectedDefectId != null) {
                        IconButton(onClick = { viewModel.deleteSelectedDefect() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = OnSurface)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                ) {
                    bitmap?.let { bmp ->
                        BboxDrawer(
                            defects = defects,
                            selectedDefectId = selectedDefectId,
                            imageWidth = bmp.width,
                            imageHeight = bmp.height,
                            bitmap = bmp,
                            isDrawing = isDrawing,
                            onNewBbox = { x1, y1, x2, y2 -> viewModel.addBbox(x1, y1, x2, y2) },
                            onSelectDefect = { viewModel.selectDefect(it) },
                        )
                    }
                }

                if (selectedDefect != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Тип дефекта:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            DefectType.entries.forEach { type ->
                                val isSelected = selectedDefect.defectType == type
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Color.White else OnSurface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        .clickable { viewModel.updateDefectType(type) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }

                        if (selectedDefect.defectType == DefectType.OTHER ||
                            selectedDefect.defectType == DefectType.ANOMALY
                        ) {
                            OutlinedTextField(
                                value = selectedDefect.description,
                                onValueChange = { viewModel.updateDefectDescription(it) },
                                label = { Text("Описание (обязательно)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    cursorColor = Primary,
                                ),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { viewModel.saveAnnotation() },
                        enabled = defects.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Сохранить")
                    }

                    Button(
                        onClick = { viewModel.exportToZip() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ),
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("ZIP")
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
