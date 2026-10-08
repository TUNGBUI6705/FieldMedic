package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threshold_set")
data class ThresholdSet(
    @PrimaryKey
    @ColumnInfo(name = "version")
    val version: Int,

    @ColumnInfo(name = "effective_from")
    val effectiveFrom: Long,

    @ColumnInfo(name = "source_note")
    val sourceNote: String,

    @ColumnInfo(name = "approved_by")
    val approvedBy: String? = null
)
