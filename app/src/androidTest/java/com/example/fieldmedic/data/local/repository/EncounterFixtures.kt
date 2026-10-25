package com.example.fieldmedic.data.local.repository

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AlertLevel
import com.example.fieldmedic.data.local.entity.Encounter
import com.example.fieldmedic.data.local.entity.EncounterStatus
import com.example.fieldmedic.data.local.entity.Gender
import com.example.fieldmedic.data.local.entity.MedicationGiven
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.entity.Photo
import com.example.fieldmedic.data.local.entity.PhotoOwnerType
import com.example.fieldmedic.data.local.entity.Referral
import com.example.fieldmedic.data.local.entity.ReferralUrgency
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.data.local.entity.SyncStatus
import com.example.fieldmedic.data.local.entity.VitalSign
import com.example.fieldmedic.data.local.entity.VitalSource
import com.example.fieldmedic.data.local.entity.VitalType
import com.example.fieldmedic.util.normalizeSearch

object EncounterFixtures {

    fun fixturePatient(
        id: String = "p1",
        patientCode: String = "PAT001",
        fullName: String = "Nguyễn Văn Test",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): Patient {
        val now = 1000L
        return Patient(
            id = id,
            patientCode = patientCode,
            stationId = "st1",
            fullName = fullName,
            searchName = normalizeSearch(fullName),
            birthYear = 1990,
            gender = Gender.MALE,
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = now, updatedAt = now, syncStatus = syncStatus)
        )
    }

    fun fixtureEncounter(
        id: String = "enc1",
        patientId: String = "p1",
        status: EncounterStatus = EncounterStatus.DRAFT,
        alertLevel: AlertLevel = AlertLevel.NONE,
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): Encounter {
        val now = 1000L
        return Encounter(
            id = id,
            patientId = patientId,
            staffId = "staff1",
            stationId = "st1",
            status = status,
            visitAt = now,
            alertLevel = alertLevel,
            syncMeta = SyncMeta(createdAt = now, updatedAt = now, syncStatus = syncStatus)
        )
    }

    fun fixtureVital(
        id: String = "v1",
        encounterId: String = "enc1",
        patientId: String = "p1",
        type: VitalType = VitalType.TEMP,
        value: Double = 37.0,
        unit: String = "°C",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): VitalSign {
        val now = 1000L
        return VitalSign(
            id = id,
            encounterId = encounterId,
            patientId = patientId,
            type = type,
            value = value,
            unit = unit,
            source = VitalSource.MANUAL,
            thresholdSetVersion = 1,
            measuredAt = now,
            syncMeta = SyncMeta(createdAt = now, updatedAt = now, syncStatus = syncStatus)
        )
    }

    fun fixtureMedication(
        id: String = "m1",
        encounterId: String = "enc1",
        patientId: String = "p1",
        drugName: String = "Paracetamol",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): MedicationGiven {
        val now = 1000L
        return MedicationGiven(
            id = id,
            encounterId = encounterId,
            patientId = patientId,
            drugName = drugName,
            dose = "500mg",
            frequency = "q6h",
            days = 3,
            syncMeta = SyncMeta(createdAt = now, updatedAt = now, syncStatus = syncStatus)
        )
    }

    fun fixtureReferral(
        id: String = "ref1",
        encounterId: String = "enc1",
        patientId: String = "p1",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): Referral {
        val now = 1000L
        return Referral(
            id = id,
            encounterId = encounterId,
            patientId = patientId,
            urgency = ReferralUrgency.ROUTINE,
            destinationName = "Bệnh viện Đa khoa",
            issuedAt = now,
            issuedBy = "staff1",
            syncMeta = SyncMeta(createdAt = now, updatedAt = now, syncStatus = syncStatus)
        )
    }

    fun fixturePhoto(
        id: String = "ph1",
        encounterId: String = "enc1",
        patientId: String = "p1",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): Photo {
        val now = 1000L
        return Photo(
            id = id,
            ownerType = PhotoOwnerType.ENCOUNTER,
            ownerId = encounterId,
            patientId = patientId,
            localPath = "/local/path",
            width = 100,
            height = 100,
            sizeBytes = 1024L,
            checksumSha256 = "hash123",
            takenAt = now,
            takenBy = "staff1",
            syncMeta = SyncMeta(createdAt = now, updatedAt = now, syncStatus = syncStatus)
        )
    }

    fun fixtureBundle(
        encounter: Encounter = fixtureEncounter(),
        vitals: List<VitalSign> = listOf(fixtureVital()),
        medications: List<MedicationGiven> = listOf(fixtureMedication()),
        referral: Referral? = null,
        photos: List<Photo> = listOf(fixturePhoto())
    ): EncounterBundle {
        return EncounterBundle(
            encounter = encounter,
            vitals = vitals,
            medications = medications,
            referral = referral,
            photos = photos
        )
    }

    fun count(db: AppDatabase, table: String, where: String = "1", args: Array<String> = emptyArray()): Int {
        val sqliteDb: SupportSQLiteDatabase = db.openHelper.readableDatabase
        val cursor = sqliteDb.query("SELECT COUNT(*) FROM $table WHERE $where", args)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }
}
