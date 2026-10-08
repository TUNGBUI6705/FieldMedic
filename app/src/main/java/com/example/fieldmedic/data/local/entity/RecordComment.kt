package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_comment",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["patient_id", "created_at"]),
        Index(value = ["target_type", "target_id"]),
        Index(value = ["parent_id"])
    ]
)
data class RecordComment(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "target_type")
    val targetType: CommentTarget,

    @ColumnInfo(name = "target_id")
    val targetId: String? = null,

    @ColumnInfo(name = "parent_id")
    val parentId: String? = null,

    @ColumnInfo(name = "author_id")
    val authorId: String,

    @ColumnInfo(name = "author_name_snapshot")
    val authorNameSnapshot: String,

    @ColumnInfo(name = "author_role_snapshot")
    val authorRoleSnapshot: String,

    @ColumnInfo(name = "type")
    val type: CommentType,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "mentions_json")
    val mentionsJson: String = "[]",

    @ColumnInfo(name = "is_resolved")
    val isResolved: Boolean = false,

    @ColumnInfo(name = "resolved_by")
    val resolvedBy: String? = null,

    @ColumnInfo(name = "resolved_at")
    val resolvedAt: Long? = null,

    @ColumnInfo(name = "edited_at")
    val editedAt: Long? = null,

    @ColumnInfo(name = "edit_count")
    val editCount: Int = 0,

    @ColumnInfo(name = "dek_version")
    val dekVersion: Int? = null,

    @Embedded
    val syncMeta: SyncMeta
)
