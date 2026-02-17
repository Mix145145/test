package com.sbm.aoi.ui.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class StorageTab(
    val title: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit,
)

private val tabs = listOf(
    StorageTab("Главная", { Icon(Icons.Filled.Home, null) }, { Icon(Icons.Outlined.Home, null) }),
    StorageTab("Поиск", { Icon(Icons.Filled.Search, null) }, { Icon(Icons.Outlined.Search, null) }),
    StorageTab("Комнаты", { Icon(Icons.Filled.MeetingRoom, null) }, { Icon(Icons.Outlined.MeetingRoom, null) }),
    StorageTab("QR Печать", { Icon(Icons.Filled.Print, null) }, { Icon(Icons.Outlined.Print, null) }),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(viewModel: StorageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { if (selectedTab == index) tab.selectedIcon() else tab.unselectedIcon() },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(state = state, onOpenScanner = { viewModel.scanPayload(it) }, padding = padding)
            1 -> SearchScreen(state = state, onSearch = viewModel::search, onScan = viewModel::scanPayload, padding = padding)
            2 -> RoomsScreen(state = state, onAddRoom = viewModel::addRoom, padding = padding)
            3 -> PrintScreen(state = state, onCreateBatch = viewModel::createBatchCodes, padding = padding)
        }
    }
}

@Composable
private fun HomeScreen(
    state: StorageUiState,
    onOpenScanner: (String) -> Unit,
    padding: PaddingValues,
) {
    var scannerValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("qa://")) }
    val recentCodes = state.codes.sortedByDescending { it.createdAt }.take(10)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = state.settings?.displayName?.ifBlank { "Пользователь" } ?: "Пользователь",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text("Добро пожаловать в ALL QR", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(title = "Всего QR", value = state.codes.size.toString(), modifier = Modifier.weight(1f))
                StatCard(title = "Описано мест", value = state.rooms.size.toString(), modifier = Modifier.weight(1f))
                StatCard(title = "Вещей", value = state.items.size.toString(), modifier = Modifier.weight(1f))
            }
        }

        item {
            Button(
                onClick = { onOpenScanner(scannerValue.text) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.padding(6.dp))
                Text("Сканировать QR", style = MaterialTheme.typography.titleMedium)
            }
        }

        item {
            OutlinedTextField(
                value = scannerValue,
                onValueChange = { scannerValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Тестовый payload") },
                supportingText = { Text("Для отладки: qa://ABC-123") },
                singleLine = true,
            )
        }

        if (recentCodes.isEmpty()) {
            item {
                EmptyHintCard(
                    title = "Пока нет недавних мест",
                    subtitle = "Создай QR → распечатай → сканируй и опиши.",
                )
            }
        } else {
            item { Text("Недавние", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            items(recentCodes) { code ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Комната · ${code.entityType}", fontWeight = FontWeight.SemiBold)
                        Text(code.codeIdString)
                        Text(
                            SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(code.createdAt)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SearchScreen(
    state: StorageUiState,
    onSearch: (String) -> Unit,
    onScan: (String) -> Unit,
    padding: PaddingValues,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var scannerInput by rememberSaveable { mutableStateOf("qa://") }
    val results = remember(state.searchResult) {
        state.searchResult.items + state.searchResult.places + state.searchResult.codes
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Поиск", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Найти вещь (например: дрель)") },
                trailingIcon = {
                    Text(
                        text = "Очистить",
                        modifier = Modifier.clickable {
                            query = ""
                            onSearch("")
                        },
                    )
                },
                singleLine = true,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Все комнаты") })
                AssistChip(onClick = {}, label = { Text("Полка") })
                AssistChip(onClick = {}, label = { Text("Без архива") })
            }
        }

        item {
            OutlinedTextField(
                value = scannerInput,
                onValueChange = { scannerInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Сканер QR (ввод) ") },
                singleLine = true,
            )
            Button(onClick = { onScan(scannerInput) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
                Text("Проверить QR")
            }
        }
        if (query.isNotBlank() && results.isEmpty()) {
            item { EmptyHintCard("Ничего не найдено", "Попробуй часть слова или проверь раскладку.") }
        }
        items(results) { hit ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(hit.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(hit.path, style = MaterialTheme.typography.bodyMedium)
                    Text("qrId: ${hit.id.take(8)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RoomsScreen(
    state: StorageUiState,
    onAddRoom: (String, String, String) -> Unit,
    padding: PaddingValues,
) {
    var roomName by rememberSaveable { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (roomName.isNotBlank()) {
                    onAddRoom(roomName, "Комната", "#1F7A4C")
                    roomName = ""
                }
            }) {
                Text("+")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Комнаты", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Добавить комнату") },
                    singleLine = true,
                )
            }
            items(state.rooms.sortedBy { it.name }) { room ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(room.name, style = MaterialTheme.typography.titleMedium)
                        Text(state.items.count { it.roomId == room.id }.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintScreen(
    state: StorageUiState,
    onCreateBatch: (Int) -> Unit,
    padding: PaddingValues,
) {
    var count by rememberSaveable { mutableStateOf("20") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Создать QR-коды", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = count,
                onValueChange = { count = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Количество") },
                singleLine = true,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(20, 50, 100, 200).forEach { quick ->
                    AssistChip(onClick = { count = quick.toString() }, label = { Text(quick.toString()) })
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Формат печати: 50×40 мм", style = MaterialTheme.typography.titleMedium)
                    Text("Под термопринтер стикеров", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(
                onClick = { onCreateBatch(count.toIntOrNull() ?: 20) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text("Сгенерировать PDF")
            }
        }
        item { Text("История", style = MaterialTheme.typography.titleLarge) }
        items(state.codes.sortedByDescending { it.createdAt }.take(10)) { code ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(code.codeIdString, fontWeight = FontWeight.SemiBold)
                    Text(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(code.createdAt)))
                }
            }
        }
    }
}

@Composable
private fun EmptyHintCard(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
