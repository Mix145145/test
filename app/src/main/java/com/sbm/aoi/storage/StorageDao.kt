package com.sbm.aoi.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: UserSettingsEntity)

    @Query("SELECT * FROM UserSettingsEntity WHERE id = 1")
    fun observeSettings(): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: StorageUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCell(cell: CellEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQrCode(qrCode: QrCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(query: SearchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanHistoryEntity)

    @Query("SELECT * FROM RoomEntity ORDER BY roomOrder ASC, name ASC")
    fun observeRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM StorageUnitEntity WHERE roomId=:roomId ORDER BY name")
    fun observeUnitsByRoom(roomId: String): Flow<List<StorageUnitEntity>>

    @Query("SELECT * FROM CellEntity WHERE unitId=:unitId ORDER BY name")
    fun observeCellsByUnit(unitId: String): Flow<List<CellEntity>>

    @Query("SELECT * FROM ItemEntity ORDER BY name")
    fun observeItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM QrCodeEntity ORDER BY createdAt DESC")
    fun observeCodes(): Flow<List<QrCodeEntity>>

    @Query("SELECT MAX(CAST(SUBSTR(codeIdString, LENGTH(:prefix) + 1) AS INTEGER)) FROM QrCodeEntity WHERE codeIdString LIKE :prefix || '%' ")
    suspend fun maxCodeNumber(prefix: String): Int?

    @Query("SELECT * FROM QrCodeEntity WHERE payloadString=:payload LIMIT 1")
    suspend fun findByPayload(payload: String): QrCodeEntity?

    @Query("SELECT * FROM QrCodeEntity WHERE codeIdString=:code LIMIT 1")
    suspend fun findByCode(code: String): QrCodeEntity?

    @Query("SELECT query FROM SearchHistoryEntity ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun recentSearches(limit: Int): List<String>

    @Query("SELECT * FROM ScanHistoryEntity ORDER BY scannedAt DESC LIMIT :limit")
    suspend fun recentScans(limit: Int): List<ScanHistoryEntity>

    @Query("DELETE FROM ScanHistoryEntity")
    suspend fun clearScanHistory()

    @Query("SELECT * FROM ItemEntity")
    suspend fun getAllItems(): List<ItemEntity>

    @Query("SELECT * FROM CellEntity")
    suspend fun getAllCells(): List<CellEntity>

    @Query("SELECT * FROM StorageUnitEntity")
    suspend fun getAllUnits(): List<StorageUnitEntity>

    @Query("SELECT * FROM RoomEntity")
    suspend fun getAllRooms(): List<RoomEntity>

    @Query("SELECT * FROM QrCodeEntity")
    suspend fun getAllCodes(): List<QrCodeEntity>

    @Transaction
    suspend fun insertCells(cells: List<CellEntity>) {
        cells.forEach { insertCell(it) }
    }
}
