package com.example.fieldmedic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patient",
    foreignKeys = [
        ForeignKey(
            entity = Village::class,
            parentColumns = ["id"],
            childColumns = ["village_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["patient_code"], unique = true),
        Index(value = ["search_name"]),
        Index(value = ["village_id"]),
        Index(value = ["station_id"]),
        Index(value = ["last_encounter_at"]),
        Index(value = ["national_id_hash"]),
        Index(value = ["full_name", "birth_year", "village_id"])
    ]
)
data class Patient(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "patient_code")
    val patientCode: String,

    @ColumnInfo(name = "station_id")
    val stationId: String,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "search_name")
    val searchName: String,

    @ColumnInfo(name = "dob")
    val dob: String? = null,

    @ColumnInfo(name = "birth_year")
    val birthYear: Int,

    @ColumnInfo(name = "dob_estimated")
    val dobEstimated: Boolean = false,

    @ColumnInfo(name = "gender")
    val gender: Gender,

    @ColumnInfo(name = "national_id_enc")
    val nationalIdEnc: ByteArray? = null,

    @ColumnInfo(name = "national_id_hash")
    val nationalIdHash: String? = null,

    @ColumnInfo(name = "national_id_last4")
    val nationalIdLast4: String? = null,

    @ColumnInfo(name = "phone")
    val phone: String? = null,

    @ColumnInfo(name = "village_id")
    val villageId: String? = null,

    @ColumnInfo(name = "address")
    val address: String? = null,

    @ColumnInfo(name = "lat")
    val lat: Double? = null,

    @ColumnInfo(name = "lng")
    val lng: Double? = null,

    @ColumnInfo(name = "blood_type")
    val bloodType: String? = null,

    @ColumnInfo(name = "blood_type_source")
    val bloodTypeSource: BloodTypeSource = BloodTypeSource.SELF_REPORTED,

    @ColumnInfo(name = "allergy_status")
    val allergyStatus: AllergyStatus = AllergyStatus.UNKNOWN,

    @ColumnInfo(name = "emergency_contact_name")
    val emergencyContactName: String? = null,

    @ColumnInfo(name = "emergency_contact_phone")
    val emergencyContactPhone: String? = null,

    @ColumnInfo(name = "emergency_contact_relation")
    val emergencyContactRelation: String? = null,

    @ColumnInfo(name = "avatar_photo_id")
    val avatarPhotoId: String? = null,

    @ColumnInfo(name = "is_provisional")
    val isProvisional: Boolean = false,

    @ColumnInfo(name = "is_watchlist")
    val isWatchlist: Boolean = false,

    @ColumnInfo(name = "access_policy")
    val accessPolicy: AccessPolicy = AccessPolicy.STATION,

    @ColumnInfo(name = "owner_staff_id")
    val ownerStaffId: String,

    @ColumnInfo(name = "lock_enabled")
    val lockEnabled: Boolean = false,

    @ColumnInfo(name = "last_encounter_at")
    val lastEncounterAt: Long? = null,

    @Embedded
    val syncMeta: SyncMeta
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Patient

        if (id != other.id) return false
        if (patientCode != other.patientCode) return false
        if (stationId != other.stationId) return false
        if (fullName != other.fullName) return false
        if (searchName != other.searchName) return false
        if (dob != other.dob) return false
        if (birthYear != other.birthYear) return false
        if (dobEstimated != other.dobEstimated) return false
        if (gender != other.gender) return false
        if (nationalIdEnc != null) {
            if (other.nationalIdEnc == null) return false
            if (!nationalIdEnc.contentEquals(other.nationalIdEnc)) return false
        } else if (other.nationalIdEnc != null) return false
        if (nationalIdHash != other.nationalIdHash) return false
        if (nationalIdLast4 != other.nationalIdLast4) return false
        if (phone != other.phone) return false
        if (villageId != other.villageId) return false
        if (address != other.address) return false
        if (lat != other.lat) return false
        if (lng != other.lng) return false
        if (bloodType != other.bloodType) return false
        if (bloodTypeSource != other.bloodTypeSource) return false
        if (allergyStatus != other.allergyStatus) return false
        if (emergencyContactName != other.emergencyContactName) return false
        if (emergencyContactPhone != other.emergencyContactPhone) return false
        if (emergencyContactRelation != other.emergencyContactRelation) return false
        if (avatarPhotoId != other.avatarPhotoId) return false
        if (isProvisional != other.isProvisional) return false
        if (isWatchlist != other.isWatchlist) return false
        if (accessPolicy != other.accessPolicy) return false
        if (ownerStaffId != other.ownerStaffId) return false
        if (lockEnabled != other.lockEnabled) return false
        if (lastEncounterAt != other.lastEncounterAt) return false
        if (syncMeta != other.syncMeta) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + patientCode.hashCode()
        result = 31 * result + stationId.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + searchName.hashCode()
        result = 31 * result + (dob?.hashCode() ?: 0)
        result = 31 * result + birthYear
        result = 31 * result + dobEstimated.hashCode()
        result = 31 * result + gender.hashCode()
        result = 31 * result + (nationalIdEnc?.contentHashCode() ?: 0)
        result = 31 * result + (nationalIdHash?.hashCode() ?: 0)
        result = 31 * result + (nationalIdLast4?.hashCode() ?: 0)
        result = 31 * result + (phone?.hashCode() ?: 0)
        result = 31 * result + (villageId?.hashCode() ?: 0)
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (lat?.hashCode() ?: 0)
        result = 31 * result + (lng?.hashCode() ?: 0)
        result = 31 * result + (bloodType?.hashCode() ?: 0)
        result = 31 * result + bloodTypeSource.hashCode()
        result = 31 * result + allergyStatus.hashCode()
        result = 31 * result + (emergencyContactName?.hashCode() ?: 0)
        result = 31 * result + (emergencyContactPhone?.hashCode() ?: 0)
        result = 31 * result + (emergencyContactRelation?.hashCode() ?: 0)
        result = 31 * result + (avatarPhotoId?.hashCode() ?: 0)
        result = 31 * result + isProvisional.hashCode()
        result = 31 * result + isWatchlist.hashCode()
        result = 31 * result + accessPolicy.hashCode()
        result = 31 * result + ownerStaffId.hashCode()
        result = 31 * result + lockEnabled.hashCode()
        result = 31 * result + (lastEncounterAt?.hashCode() ?: 0)
        result = 31 * result + syncMeta.hashCode()
        return result
    }
}
