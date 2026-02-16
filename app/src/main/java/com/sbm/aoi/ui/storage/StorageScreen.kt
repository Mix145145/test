package com.sbm.aoi.ui.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StorageScreen(viewModel: StorageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf("") }
    var prefix by rememberSaveable { mutableStateOf("") }
    var roomName by rememberSaveable { mutableStateOf("") }
    var itemName by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var scanPayload by rememberSaveable { mutableStateOf("") }

    Scaffold { p ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("QFA — учет хранения по QR", style = MaterialTheme.typography.headlineSmall)
                Text(state.status, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Онбординг и настройки", fontWeight = FontWeight.Bold)
                        OutlinedTextField(name, { name = it }, label = { Text("Имя пользователя") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.onNameEntered(name) }) { Text("Сохранить имя") }
                            Text("Prefix: ${state.settings?.prefix ?: "—"}")
                        }
                        OutlinedTextField(prefix, { prefix = it }, label = { Text("Prefix вручную") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { viewModel.updateSettings(name.ifBlank { state.settings?.displayName ?: "User" }, prefix.ifBlank { state.settings?.prefix ?: "User" }) }) {
                            Text("Обновить prefix")
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Карта и комнаты", fontWeight = FontWeight.Bold)
                        OutlinedTextField(roomName, { roomName = it }, label = { Text("Новая комната") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { if (roomName.isNotBlank()) viewModel.addRoom(roomName) }) { Text("Добавить комнату") }
                        state.rooms.forEach { Text("• ${it.name} (x=${it.x.toInt()} y=${it.y.toInt()} w=${it.width.toInt()} h=${it.height.toInt()})") }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Объекты и QR", fontWeight = FontWeight.Bold)
                        OutlinedTextField(itemName, { itemName = it }, label = { Text("Новый объект") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { if (itemName.isNotBlank()) viewModel.addItem(itemName) }) { Text("Добавить объект") }
                        state.items.forEach { item ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.name)
                                Button(onClick = { viewModel.generateItemQr(item.id) }) { Text("QR") }
                            }
                        }
                        Button(onClick = { viewModel.createBatchCodes(30) }) { Text("Создать 30 новых свободных кодов") }
                        Button(onClick = { viewModel.exportPdf(context) }) { Text("Экспорт стикеров PDF 50x40") }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Сканер (MVP: ввод payload)", fontWeight = FontWeight.Bold)
                        OutlinedTextField(scanPayload, { scanPayload = it }, label = { Text("app-scheme://qfa/...") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { viewModel.scanPayload(scanPayload) }) { Text("Обработать скан") }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Поиск с опечатками", fontWeight = FontWeight.Bold)
                        OutlinedTextField(search, { search = it; viewModel.search(it) }, label = { Text("Поиск") }, modifier = Modifier.fillMaxWidth())
                        Text("Объекты")
                        state.searchResult.items.forEach { Text("• ${it.title} | ${it.path}") }
                        Text("Места")
                        state.searchResult.places.forEach { Text("• ${it.title} | ${it.path}") }
                        Text("Коды")
                        state.searchResult.codes.forEach { Text("• ${it.title} | ${it.path}") }
                    }
                }
            }
            item {
                Button(onClick = { viewModel.exportBackup(context) }) { Text("Экспорт базы (ZIP)") }
            }
        }
    }
}
