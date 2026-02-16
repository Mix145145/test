package com.sbm.aoi.ui.models

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.aoi.ui.components.ModelCard
import com.sbm.aoi.ui.theme.OnSurface
import com.sbm.aoi.ui.theme.Primary
import com.sbm.aoi.ui.theme.Surface

@Composable
fun ModelsScreen(
    viewModel: ModelsViewModel = hiltViewModel(),
) {
    val models by viewModel.models.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRenameDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    val modelPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadModelFromUri(it) }
    }

    val labelsPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.addLabelsToModel(it) }
    }

    // Snackbar для сообщений
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    modelPickerLauncher.launch(arrayOf("*/*"))
                },
                containerColor = Primary,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Добавить модель",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (models.isEmpty()) {
                // Пустой экран
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Primary.copy(alpha = 0.5f),
                    )
                    Text(
                        text = "Нет моделей",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Нажмите + чтобы добавить AI-модель",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(models, key = { it.id }) { model ->
                        ModelCard(
                            model = model,
                            onActivate = { viewModel.activateModel(model.id) },
                            onDelete = { showDeleteDialog = model.id },
                            onRename = {
                                renameText = model.name
                                showRenameDialog = model.id
                            },
                            onColorChange = { viewModel.setModelColor(model.id, it) },
                            onConfidenceChange = { viewModel.updateConfidence(model.id, it) },
                            onIouChange = { viewModel.updateIou(model.id, it) },
                            onOpacityChange = { viewModel.updateOverlayOpacity(model.id, it) },
                            onToggleLabels = { viewModel.toggleShowLabels(model.id) },
                            onToggleConfidence = { viewModel.toggleShowConfidence(model.id) },
                            onHighlightMethodChange = { viewModel.setHighlightMethod(model.id, it) },
                        )
                    }
                }
            }

            // Индикатор загрузки
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
    }

    // Диалог выбора labels
    if (uiState.showLabelsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.skipLabels() },
            title = { Text("Файл меток") },
            text = { Text("Выберите файл labels.txt с именами классов или пропустите этот шаг.") },
            confirmButton = {
                TextButton(onClick = {
                    labelsPickerLauncher.launch(arrayOf("text/*"))
                }) {
                    Text("Выбрать файл", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.skipLabels() }) {
                    Text("Пропустить", color = OnSurface)
                }
            },
            containerColor = Surface,
        )
    }

    // Диалог переименования
    showRenameDialog?.let { modelId ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Переименовать модель") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Название") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameModel(modelId, renameText)
                    showRenameDialog = null
                }) {
                    Text("Сохранить", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Отмена", color = OnSurface)
                }
            },
            containerColor = Surface,
        )
    }

    // Диалог удаления
    showDeleteDialog?.let { modelId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить модель?") },
            text = { Text("Модель и все связанные данные будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteModel(modelId)
                    showDeleteDialog = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Отмена", color = OnSurface)
                }
            },
            containerColor = Surface,
        )
    }
}
