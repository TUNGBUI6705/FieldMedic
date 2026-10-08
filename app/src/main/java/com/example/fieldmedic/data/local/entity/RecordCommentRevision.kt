package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_comment_revision",
    foreignKeys = [
        ForeignKey(
            entity = RecordComment::class,
            parentColumns = ["id"],
            childColumns = ["comment_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["comment_id"]),
        Index(value = ["patient_id"])
    ]
)
data class RecordCommentRevision(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "comment_id")
    val commentId: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "edited_by")
    val editedBy: String,

    @ColumnInfo(name = "edited_at")
    val editedAt: Long,

    @ColumnInfo(name = "dek_version")
    val dekVersion: Int? = null,

    @Embedded
    val syncMeta: SyncMeta
)
