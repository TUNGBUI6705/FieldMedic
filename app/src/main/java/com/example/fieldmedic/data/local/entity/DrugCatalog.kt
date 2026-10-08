package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "drug_catalog",
    indices = [
        Index(value = ["search_name"]),
        Index(value = ["allergy_class_code"])
    ]
)
data class DrugCatalog(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "search_name")
    val searchName: String,

    @ColumnInfo(name = "active_ingredient")
    val activeIngredient: String? = null,

    @ColumnInfo(name = "strength")
    val strength: String? = null,

    @ColumnInfo(name = "form")
    val form: String? = null,

    @ColumnInfo(name = "allergy_class_code")
    val allergyClassCode: String? = null,

    @ColumnInfo(name = "pregnancy_safety")
    val pregnancySafety: Safety = Safety.UNKNOWN,

    @ColumnInfo(name = "breastfeeding_safety")
    val breastfeedingSafety: Safety = Safety.UNKNOWN,

    @ColumnInfo(name = "min_age_months")
    val minAgeMonths: Int? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
