package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "comment_read_state",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CommentReadState(
    @PrimaryKey
    @ColumnInfo(name = "patient_id")
    val patientId: String,

    @ColumnInfo(name = "last_read_at")
    val lastReadAt: Long,

    @ColumnInfo(name = "last_read_comment_id")
    val lastReadCommentId: String? = null
)
