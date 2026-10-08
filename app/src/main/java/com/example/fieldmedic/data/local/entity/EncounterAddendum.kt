package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "encounter_addendum",
    foreignKeys = [
        ForeignKey(
            entity = Encounter::class,
            parentColumns = ["id"],
            childColumns = ["encounter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["encounter_id"])
    ]
)
data class EncounterAddendum(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "encounter_id")
    val encounterId: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "kind")
    val kind: AddendumKind,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "changes_json")
    val changesJson: String,

    @ColumnInfo(name = "created_by")
    val createdBy: String,

    @ColumnInfo(name = "dek_version")
    val dekVersion: Int? = null,

    @Embedded
    val syncMeta: SyncMeta
)
