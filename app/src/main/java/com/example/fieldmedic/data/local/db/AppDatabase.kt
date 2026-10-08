package com.example.fieldmedic.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fieldmedic.data.local.entity.AllergenCatalog
import com.example.fieldmedic.data.local.entity.Appointment
import com.example.fieldmedic.data.local.entity.AuditLog
import com.example.fieldmedic.data.local.entity.CommentReadState
import com.example.fieldmedic.data.local.entity.DrugCatalog
import com.example.fieldmedic.data.local.entity.Encounter
import com.example.fieldmedic.data.local.entity.EncounterAddendum
import com.example.fieldmedic.data.local.entity.ErrorLog
import com.example.fieldmedic.data.local.entity.MedicationGiven
import com.example.fieldmedic.data.local.entity.PairedDevice
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.entity.PatientAllergy
import com.example.fieldmedic.data.local.entity.PatientCondition
import com.example.fieldmedic.data.local.entity.PatientMedication
import com.example.fieldmedic.data.local.entity.Photo
import com.example.fieldmedic.data.local.entity.RecordComment
import com.example.fieldmedic.data.local.entity.RecordCommentRevision
import com.example.fieldmedic.data.local.entity.RecordInvite
import com.example.fieldmedic.data.local.entity.RecordLock
import com.example.fieldmedic.data.local.entity.RecordLockState
import com.example.fieldmedic.data.local.entity.RecordMember
import com.example.fieldmedic.data.local.entity.Referral
import com.example.fieldmedic.data.local.entity.Staff
import com.example.fieldmedic.data.local.entity.Station
import com.example.fieldmedic.data.local.entity.SyncConflict
import com.example.fieldmedic.data.local.entity.SyncCursor
import com.example.fieldmedic.data.local.entity.SyncQueue
import com.example.fieldmedic.data.local.entity.ThresholdRule
import com.example.fieldmedic.data.local.entity.ThresholdSet
import com.example.fieldmedic.data.local.entity.Village
import com.example.fieldmedic.data.local.entity.VitalSign
import com.example.fieldmedic.data.local.view.SyncSummary

@Database(
    entities = [
        Station::class,
        Village::class,
        AllergenCatalog::class,
        DrugCatalog::class,
        ThresholdSet::class,
        ThresholdRule::class,
        Staff::class,
        Patient::class,
        PatientAllergy::class,
        PatientCondition::class,
        PatientMedication::class,
        Appointment::class,
        Encounter::class,
        VitalSign::class,
        MedicationGiven::class,
        Referral::class,
        EncounterAddendum::class,
        Photo::class,
        SyncQueue::class,
        SyncConflict::class,
        SyncCursor::class,
        RecordComment::class,
        RecordCommentRevision::class,
        RecordMember::class,
        RecordInvite::class,
        RecordLock::class,
        RecordLockState::class,
        CommentReadState::class,
        AuditLog::class,
        PairedDevice::class,
        ErrorLog::class,
    ],
    views = [
        SyncSummary::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val NAME = "fieldmedic.db"
    }
}
