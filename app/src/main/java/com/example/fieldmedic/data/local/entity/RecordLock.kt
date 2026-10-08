package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_lock",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RecordLock(
    @PrimaryKey
    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean,

    @ColumnInfo(name = "kdf_algo")
    val kdfAlgo: String,

    @ColumnInfo(name = "kdf_params_json")
    val kdfParamsJson: String,

    @ColumnInfo(name = "kdf_salt", typeAffinity = ColumnInfo.BLOB)
    val kdfSalt: ByteArray,

    @ColumnInfo(name = "wrapped_dek", typeAffinity = ColumnInfo.BLOB)
    val wrappedDek: ByteArray,

    @ColumnInfo(name = "dek_version")
    val dekVersion: Int,

    @ColumnInfo(name = "created_by")
    val createdBy: String,

    @ColumnInfo(name = "password_changed_at")
    val passwordChangedAt: Long,

    @Embedded
    val syncMeta: SyncMeta
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecordLock

        if (patientId != other.patientId) return false
        if (enabled != other.enabled) return false
        if (kdfAlgo != other.kdfAlgo) return false
        if (kdfParamsJson != other.kdfParamsJson) return false
        if (!kdfSalt.contentEquals(other.kdfSalt)) return false
        if (!wrappedDek.contentEquals(other.wrappedDek)) return false
        if (dekVersion != other.dekVersion) return false
        if (createdBy != other.createdBy) return false
        if (passwordChangedAt != other.passwordChangedAt) return false
        return syncMeta == other.syncMeta
    }

    override fun hashCode(): Int {
        var result = patientId.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + kdfAlgo.hashCode()
        result = 31 * result + kdfParamsJson.hashCode()
        result = 31 * result + kdfSalt.contentHashCode()
        result = 31 * result + wrappedDek.contentHashCode()
        result = 31 * result + dekVersion
        result = 31 * result + createdBy.hashCode()
        result = 31 * result + passwordChangedAt.hashCode()
        result = 31 * result + syncMeta.hashCode()
        return result
    }
}
