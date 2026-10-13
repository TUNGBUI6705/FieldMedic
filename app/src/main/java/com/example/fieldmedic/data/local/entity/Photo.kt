package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "photo",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["upload_status"]),
        Index(value = ["owner_type", "owner_id"]),
        Index(value = ["patient_id"])
    ]
)
data class Photo(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "owner_type")
    val ownerType: PhotoOwnerType,

    @ColumnInfo(name = "owner_id")
    val ownerId: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "local_path")
    val localPath: String,

    @ColumnInfo(name = "remote_path")
    val remotePath: String? = null,

    @ColumnInfo(name = "mime")
    val mime: String = "image/webp",

    @ColumnInfo(name = "width")
    val width: Int,

    @ColumnInfo(name = "height")
    val height: Int,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "checksum_sha256")
    val checksumSha256: String,

    @ColumnInfo(name = "upload_status")
    val uploadStatus: PhotoUploadStatus = PhotoUploadStatus.LOCAL,

    @ColumnInfo(name = "upload_session")
    val uploadSession: String? = null,

    @ColumnInfo(name = "bytes_uploaded")
    val bytesUploaded: Int = 0,

    @ColumnInfo(name = "exif_gps_stripped")
    val exifGpsStripped: Boolean = true,

    @ColumnInfo(name = "is_file_encrypted")
    val isFileEncrypted: Boolean = false,

    @ColumnInfo(name = "caption")
    val caption: String? = null,

    @ColumnInfo(name = "dek_version")
    val dekVersion: Int? = null,

    @ColumnInfo(name = "taken_at")
    val takenAt: Long,

    @ColumnInfo(name = "taken_by")
    val takenBy: String,

    @Embedded
    val syncMeta: SyncMeta
)
