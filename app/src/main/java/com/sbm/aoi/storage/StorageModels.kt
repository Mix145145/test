package com.sbm.aoi.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val prefix: String,
    val typoStrictness: String = "MEDIUM",
    val pdfPageType: String = "A4",
    val printMargin: String = "MEDIUM",
    val printIncludeTitle: Boolean = true,
)

@Entity
@Serializable
data class LocationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
)

@Entity(
    foreignKeys = [
        ForeignKey(entity = LocationEntity::class, parentColumns = ["id"], childColumns = ["locationId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("locationId")],
)
@Serializable
data class RoomEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val locationId: String,
    val name: String,
    val type: String = "Комната",
    val colorHex: String = "#E8F1FF",
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 100f,
    val height: Float = 60f,
    val roomOrder: Int = 0,
)

@Entity(
    foreignKeys = [
        ForeignKey(entity = RoomEntity::class, parentColumns = ["id"], childColumns = ["roomId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("roomId")],
)
@Serializable
data class StorageUnitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val roomId: String,
    val name: String,
    val type: String,
    val description: String = "",
)

@Entity(
    foreignKeys = [
        ForeignKey(entity = StorageUnitEntity::class, parentColumns = ["id"], childColumns = ["unitId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("unitId")],
)
@Serializable
data class CellEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val unitId: String,
    val name: String,
    val row: Int = 0,
    val col: Int = 0,
    val description: String = "",
)

@Entity(
    foreignKeys = [
        ForeignKey(entity = CellEntity::class, parentColumns = ["id"], childColumns = ["cellId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("cellId")],
)
@Serializable
data class ItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val tags: String = "",
    val photoUri: String? = null,
    val roomId: String? = null,
    val storageType: String = "Полка",
    val cellId: String? = null,
)

@Entity(indices = [Index("codeIdString", unique = true), Index("entityId")])
@Serializable
data class QrCodeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val codeIdString: String,
    val payloadString: String,
    val entityType: String,
    val entityId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "linked",
)

@Entity(primaryKeys = ["query"])
@Serializable
data class SearchHistoryEntity(
    val query: String,
    val lastUsedAt: Long = System.currentTimeMillis(),
)

@Entity(primaryKeys = ["payloadString", "scannedAt"])
@Serializable
data class ScanHistoryEntity(
    val payloadString: String,
    val scannedAt: Long = System.currentTimeMillis(),
    val resolvedEntityType: String? = null,
    val resolvedEntityId: String? = null,
)
