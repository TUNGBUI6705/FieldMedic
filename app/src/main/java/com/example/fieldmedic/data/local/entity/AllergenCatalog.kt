package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "allergen_catalog",
    indices = [
        Index(value = ["search_name"]),
        Index(value = ["class_code"])
    ]
)
data class AllergenCatalog(
    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "name_vi")
    val nameVi: String,

    @ColumnInfo(name = "class_code")
    val classCode: String,

    @ColumnInfo(name = "search_name")
    val searchName: String,

    @ColumnInfo(name = "synonyms")
    val synonyms: String? = null
)
