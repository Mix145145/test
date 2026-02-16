package com.sbm.aoi.ui.storage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

private data class StorageTab(
    val title: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit,
)

private val tabs = listOf(
    StorageTab("Поиск", { Icon(Icons.Filled.Search, contentDescription = null) }, { Icon(Icons.Outlined.Search, contentDescription = null) }),
    StorageTab("Карта", { Icon(Icons.Filled.Map, contentDescription = null) }, { Icon(Icons.Outlined.Map, contentDescription = null) }),
    StorageTab("QR / Сканер", { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) }, { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null) }),
    StorageTab("Генератор", { Icon(Icons.Filled.QrCode, contentDescription = null) }, { Icon(Icons.Outlined.QrCode, contentDescription = null) }),
)

private val quickCategories = listOf("Медикаменты", "Инструменты", "Рыбалка", "Обувь", "Электроника", "Химия")
private val roomTypes = listOf("Комната", "Балкон", "Гараж", "Кладовка", "Подвал", "Другое")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(viewModel: StorageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
            0 -> SearchScreen(state = state, onSearch = viewModel::search, padding = padding)
            1 -> MapScreen(state = state, onAddRoom = viewModel::addRoom, padding = padding)
            2 -> ScanScreen(
                state = state,
                onScan = viewModel::scanPayload,
                onGenerateItemQr = viewModel::generateItemQr,
                padding = padding,
            )

            else -> GeneratorScreen(
                state = state,
                onCreateBatch = viewModel::createBatchCodes,
                onExportPdf = { viewModel.exportPdf(context) },
                padding = padding,
            )
        }
    }
}

@Composable
private fun SearchScreen(
    state: StorageUiState,
    onSearch: (String) -> Unit,
    padding: PaddingValues,
) {
    var query by rememberSaveable { mutableStateOf(state.query) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Поиск", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Найдите по названию, содержимому, QR ID или типу хранения", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Например: кросовки, Gosha12, шкаф") },
                singleLine = true,
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickCategories) { category ->
                    AssistChip(
                        onClick = {
                            query = category
                            onSearch(category)
                        },
                        label = { Text(category) },
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Результаты", fontWeight = FontWeight.SemiBold)
                    if (state.searchResult.items.isEmpty() && state.searchResult.places.isEmpty() && state.searchResult.codes.isEmpty()) {
                        Text("Начните вводить запрос. Поиск учитывает опечатки и историю.")
                    }
                    state.searchResult.items.forEach {
                        ResultCard(title = it.title, path = it.path)
                    }
                    state.searchResult.places.forEach {
                        ResultCard(title = it.title, path = it.path)
                    }
                    state.searchResult.codes.forEach {
                        ResultCard(title = it.title, path = it.path)
                    }
                }
            }
        }
        item {
            Text("Последние действия", fontWeight = FontWeight.SemiBold)
            Text(state.status.ifBlank { "Пока нет действий" })
        }
    }
}

@Composable
private fun ResultCard(title: String, path: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(path.ifBlank { "Квартира → Комната → Шкаф → Полка" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = {}, modifier = Modifier.align(Alignment.End)) { Text("Показать на карте") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapScreen(
    state: StorageUiState,
    onAddRoom: (String) -> Unit,
    padding: PaddingValues,
) {
    var roomName by rememberSaveable { mutableStateOf("") }
    var roomType by rememberSaveable { mutableStateOf(roomTypes.first()) }
    val offsets = remember { mutableStateMapOf<String, Pair<Float, Float>>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Карта квартиры", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Комнаты можно перетаскивать и быстро редактировать", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Название комнаты") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = !expanded }) { Text(roomType) }
                    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        roomTypes.forEach { type ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    roomType = type
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFF9FAFB), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFD8DEE4), RoundedCornerShape(20.dp)),
            ) {
                state.rooms.forEachIndexed { index, room ->
                    val offsetPair = offsets.getOrPut(room.id) { room.x to room.y }
                    var x by remember(room.id) { mutableFloatStateOf(offsetPair.first) }
                    var y by remember(room.id) { mutableFloatStateOf(offsetPair.second) }
                    val colors = listOf(Color(0xFFE8F1FF), Color(0xFFE7F8EF), Color(0xFFFFF4E5), Color(0xFFF4ECFF))
                    Card(
                        modifier = Modifier
                            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                            .size(width = room.width.dp + 80.dp, height = room.height.dp + 30.dp)
                            .pointerInput(room.id) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        x += dragAmount.x
                                        y += dragAmount.y
                                        offsets[room.id] = x to y
                                    },
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = colors[index % colors.size]),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(room.name, fontWeight = FontWeight.SemiBold)
                            Text(roomType, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (state.rooms.isEmpty()) {
                    Text(
                        "Добавьте первую комнату кнопкой +",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (roomName.isNotBlank()) {
                    onAddRoom(roomName)
                    roomName = ""
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Добавить комнату")
        }
    }
}

@Composable
private fun ScanScreen(
    state: StorageUiState,
    onScan: (String) -> Unit,
    onGenerateItemQr: (String) -> Unit,
    padding: PaddingValues,
) {
    var payload by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("QR / Сканер", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Сканируйте код, чтобы открыть или привязать место хранения")
        }
        item {
            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it },
                label = { Text("Payload QR") },
                placeholder = { Text("app-scheme://qfa/... ") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onScan(payload) }, modifier = Modifier.fillMaxWidth()) { Text("Обработать скан") }
            Spacer(Modifier.height(8.dp))
            Text(state.status, color = MaterialTheme.colorScheme.primary)
        }
        item {
            Text("Быстрая привязка к объекту", fontWeight = FontWeight.SemiBold)
            if (state.items.isEmpty()) {
                Text("Сначала создайте объект на экране генератора")
            }
        }
        items(state.items) { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(item.name, fontWeight = FontWeight.Medium)
                        Text("Коробка / Полка / Шкаф", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { onGenerateItemQr(item.id) }) { Text("Привязать QR") }
                }
            }
        }
    }
}

@Composable
private fun GeneratorScreen(
    state: StorageUiState,
    onCreateBatch: (Int) -> Unit,
    onExportPdf: () -> Unit,
    padding: PaddingValues,
) {
    var count by rememberSaveable { mutableStateOf("50") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Генератор кодов", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Генерирует пустые QR-стикеры 50×40 мм для A4/рулона")
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = count,
                        onValueChange = { count = it.filter(Char::isDigit) },
                        label = { Text("Количество кодов") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onCreateBatch(count.toIntOrNull() ?: 50) }) { Text("Сгенерировать") }
                        OutlinedButton(onClick = onExportPdf) { Text("Создать PDF") }
                    }
                    Text("Prefix: ${state.settings?.prefix ?: "User"}")
                    Text("Свободных кодов: ${state.codes.count { it.status == "free" }}")
                    Text("Всего кодов: ${state.codes.size}")
                }
            }
        }
        item {
            Text("Последние ID", fontWeight = FontWeight.SemiBold)
        }
        items(state.codes.take(20)) { code ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(code.codeIdString, modifier = Modifier.width(100.dp), fontWeight = FontWeight.Medium)
                Text(code.status, color = if (code.status == "free") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
