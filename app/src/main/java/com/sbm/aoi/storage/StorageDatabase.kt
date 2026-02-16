package com.sbm.aoi.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserSettingsEntity::class,
        LocationEntity::class,
        RoomEntity::class,
        StorageUnitEntity::class,
        CellEntity::class,
        ItemEntity::class,
        QrCodeEntity::class,
        SearchHistoryEntity::class,
        ScanHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class StorageDatabase : RoomDatabase() {
    abstract fun dao(): StorageDao
}
