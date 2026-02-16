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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    StorageTab("Поиск", { Icon(Icons.Filled.Search, null) }, { Icon(Icons.Outlined.Search, null) }),
    StorageTab("Карта", { Icon(Icons.Filled.Map, null) }, { Icon(Icons.Outlined.Map, null) }),
    StorageTab("QR / Сканер", { Icon(Icons.Filled.QrCodeScanner, null) }, { Icon(Icons.Outlined.QrCodeScanner, null) }),
    StorageTab("Генератор", { Icon(Icons.Filled.QrCode, null) }, { Icon(Icons.Outlined.QrCode, null) }),
    StorageTab("Профиль", { Icon(Icons.Filled.Person, null) }, { Icon(Icons.Outlined.Person, null) }),
)

private val roomTypes = listOf("Комната", "Балкон", "Гараж", "Кладовка", "Подвал", "Другое")
private val storageTypes = listOf("Полка", "Ящик", "Коробка", "Шкаф", "Стеллаж", "Другое")
private val roomColors = listOf("#E8F1FF", "#E7F8EF", "#FFF4E5", "#F4ECFF", "#FFE8E8")

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
            1 -> MapScreen(state = state, onAddRoom = viewModel::addRoom, onSaveRoomLayout = viewModel::saveRoomLayout, padding = padding)
            2 -> ScanScreen(
                state = state,
                onScan = viewModel::scanPayload,
                onGenerateItemQr = viewModel::generateItemQr,
                onAddItem = viewModel::addItem,
                padding = padding,
            )

            3 -> GeneratorScreen(
                state = state,
                onCreateBatch = viewModel::createBatchCodes,
                onExportPdf = { viewModel.exportPdf(context) },
                padding = padding,
            )

            else -> ProfileScreen(state = state, onSave = viewModel::updateSettings, padding = padding)
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Поиск по вещам", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Например: аптечка, шурупы, удочка") },
            )
        }
        if (state.searchResult.items.isEmpty() && state.searchResult.places.isEmpty() && state.searchResult.codes.isEmpty()) {
            item { Text("Введите запрос для поиска") }
        } else {
            item { Text("Результаты", fontWeight = FontWeight.SemiBold) }
            items(state.searchResult.items + state.searchResult.places + state.searchResult.codes) { hit ->
                ResultCard(title = hit.title, path = hit.path)
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapScreen(
    state: StorageUiState,
    onAddRoom: (String, String, String) -> Unit,
    onSaveRoomLayout: (String, Float, Float, String, String) -> Unit,
    padding: PaddingValues,
) {
    var roomName by rememberSaveable { mutableStateOf("") }
    var roomType by rememberSaveable { mutableStateOf(roomTypes.first()) }
    var roomColor by rememberSaveable { mutableStateOf(roomColors.first()) }
    val offsets = remember { mutableStateMapOf<String, Pair<Float, Float>>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Карта квартиры", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Комнаты сохраняются в базе. Можно перетаскивать на карте и видеть список плитками")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Название комнаты") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                RoomTypePicker(value = roomType, onChange = { roomType = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                roomColors.forEach { colorHex ->
                    val color = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color(0xFFE8F1FF))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(color, RoundedCornerShape(8.dp))
                            .border(if (roomColor == colorHex) 2.dp else 1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                            .pointerInput(colorHex) { detectDragGestures(onDragStart = { roomColor = colorHex }, onDrag = { _, _ -> }) },
                    )
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(0.45f)) {
                items(state.rooms) { room ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(room.name, fontWeight = FontWeight.SemiBold)
                                Text("${room.type} • цвет ${room.colorHex}")
                            }
                            Text("x=${room.x.roundToInt()} y=${room.y.roundToInt()}")
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
                state.rooms.forEach { room ->
                    val offsetPair = offsets.getOrPut(room.id) { room.x to room.y }
                    var x by remember(room.id) { mutableFloatStateOf(offsetPair.first) }
                    var y by remember(room.id) { mutableFloatStateOf(offsetPair.second) }
                    val cardColor = runCatching { Color(android.graphics.Color.parseColor(room.colorHex)) }.getOrDefault(Color(0xFFE8F1FF))
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
                                    onDragEnd = { onSaveRoomLayout(room.id, x, y, room.type, room.colorHex) },
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(room.name, fontWeight = FontWeight.SemiBold)
                            Text(room.type, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    onAddRoom(roomName, roomType, roomColor)
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
private fun RoomTypePicker(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = !expanded }) { Text(value) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            roomTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onChange(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ScanScreen(
    state: StorageUiState,
    onScan: (String) -> Unit,
    onGenerateItemQr: (String) -> Unit,
    onAddItem: (String, String?, String, String, String?) -> Unit,
    padding: PaddingValues,
) {
    var payload by rememberSaveable { mutableStateOf("") }
    var itemName by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var selectedRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var storageType by rememberSaveable { mutableStateOf(storageTypes.first()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("QR / Сканер", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Камера может быть открыта на этом экране. Для dev-сборки доступен быстрый ввод payload.")
            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it },
                label = { Text("Payload QR") },
                placeholder = { Text("app-scheme://qfa/... ") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onScan(payload) }, modifier = Modifier.fillMaxWidth()) { Text("Сканировать") }
            Spacer(Modifier.height(8.dp))
            Text(state.status, color = MaterialTheme.colorScheme.primary)
        }

        if (state.scanResult.requiresBinding) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Новый QR: сначала выберите комнату и куда привязать", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Название предмета") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Заметка (текст)") }, modifier = Modifier.fillMaxWidth())
                        RoomIdPicker(
                            options = state.scanResult.roomOptions,
                            selected = selectedRoomId,
                            onSelected = { selectedRoomId = it },
                        )
                        StorageTypePicker(storageType = storageType, onSelected = { storageType = it })
                        Button(
                            onClick = {
                                if (itemName.isNotBlank()) {
                                    onAddItem(itemName, selectedRoomId, storageType, note, null)
                                    itemName = ""
                                    note = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Сохранить привязку") }
                    }
                }
            }
        }

        if (state.noteResults.isNotEmpty()) {
            item { Text("Записи пользователя", fontWeight = FontWeight.SemiBold) }
            items(state.noteResults) { row ->
                Card(Modifier.fillMaxWidth()) { Text(row, modifier = Modifier.padding(12.dp)) }
            }
        }

        item {
            Text("Быстрая привязка", fontWeight = FontWeight.SemiBold)
            if (state.items.isEmpty()) {
                Text("Сначала создайте объект")
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
                        Text(item.description.ifBlank { "Без заметки" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { onGenerateItemQr(item.id) }) { Text("Привязать QR") }
                }
            }
        }
    }
}

@Composable
private fun RoomIdPicker(
    options: List<com.sbm.aoi.storage.RoomEntity>,
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val title = options.firstOrNull { it.id == selected }?.name ?: "Выберите комнату"
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(title) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { room ->
                DropdownMenuItem(
                    text = { Text(room.name) },
                    onClick = {
                        onSelected(room.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun StorageTypePicker(storageType: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(storageType) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            storageTypes.forEach { type ->
                DropdownMenuItem(text = { Text(type) }, onClick = {
                    onSelected(type)
                    expanded = false
                })
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Генератор кодов", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Пакет по 5 шт: после генерации можно сразу сохранить PDF или отправить на печать")
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onCreateBatch(5) }) { Text("Сгенерировать 5") }
                        OutlinedButton(onClick = onExportPdf) { Text("Сохранить / Печать") }
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
                Text(code.codeIdString, modifier = Modifier.width(120.dp), fontWeight = FontWeight.Medium)
                Text(code.status, color = if (code.status == "free") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    state: StorageUiState,
    onSave: (String, String) -> Unit,
    padding: PaddingValues,
) {
    var name by rememberSaveable { mutableStateOf(state.settings?.displayName ?: "") }
    val generatedPrefix = remember(name) { if (name.isBlank()) "User" else name.filter { it.isLetterOrDigit() }.take(12).ifBlank { "User" } }
    var prefix by rememberSaveable(state.settings?.prefix) { mutableStateOf(state.settings?.prefix ?: generatedPrefix) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Профиль", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Имя участвует в ID меток")
        }
        item {
            OutlinedTextField(value = name, onValueChange = {
                name = it
                prefix = it.filter { ch -> ch.isLetterOrDigit() }.take(12).ifBlank { "User" }
            }, label = { Text("Ваше имя") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = prefix, onValueChange = { prefix = it.filter(Char::isLetterOrDigit) }, label = { Text("Префикс ID") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onSave(name.ifBlank { "User" }, prefix.ifBlank { "User" }) }, modifier = Modifier.fillMaxWidth()) {
                Text("Сохранить")
            }
        }
    }
}
