package com.example.fieldmedic.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fieldmedic.data.local.dao.AllergenDao
import com.example.fieldmedic.data.local.dao.AppointmentDao
import com.example.fieldmedic.data.local.dao.AuditLogDao
import com.example.fieldmedic.data.local.dao.CommentDao
import com.example.fieldmedic.data.local.dao.CommentReadStateDao
import com.example.fieldmedic.data.local.dao.CommentRevisionDao
import com.example.fieldmedic.data.local.dao.DrugDao
import com.example.fieldmedic.data.local.dao.EncounterAddendumDao
import com.example.fieldmedic.data.local.dao.EncounterDao
import com.example.fieldmedic.data.local.dao.ErrorLogDao
import com.example.fieldmedic.data.local.dao.InviteDao
import com.example.fieldmedic.data.local.dao.MedicationGivenDao
import com.example.fieldmedic.data.local.dao.MemberDao
import com.example.fieldmedic.data.local.dao.PairedDeviceDao
import com.example.fieldmedic.data.local.dao.PatientAllergyDao
import com.example.fieldmedic.data.local.dao.PatientConditionDao
import com.example.fieldmedic.data.local.dao.PatientDao
import com.example.fieldmedic.data.local.dao.PatientMedicationDao
import com.example.fieldmedic.data.local.dao.PhotoDao
import com.example.fieldmedic.data.local.dao.RecordLockDao
import com.example.fieldmedic.data.local.dao.RecordLockStateDao
import com.example.fieldmedic.data.local.dao.ReferralDao
import com.example.fieldmedic.data.local.dao.StaffDao
import com.example.fieldmedic.data.local.dao.SyncConflictDao
import com.example.fieldmedic.data.local.dao.SyncCursorDao
import com.example.fieldmedic.data.local.dao.SyncDao
import com.example.fieldmedic.data.local.dao.ThresholdDao
import com.example.fieldmedic.data.local.dao.VillageDao
import com.example.fieldmedic.data.local.dao.VitalDao
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

    abstract fun villageDao(): VillageDao
    abstract fun staffDao(): StaffDao
    abstract fun allergenDao(): AllergenDao
    abstract fun drugDao(): DrugDao
    abstract fun thresholdDao(): ThresholdDao
    abstract fun patientDao(): PatientDao
    abstract fun patientAllergyDao(): PatientAllergyDao
    abstract fun patientConditionDao(): PatientConditionDao
    abstract fun patientMedicationDao(): PatientMedicationDao
    abstract fun encounterDao(): EncounterDao
    abstract fun vitalDao(): VitalDao
    abstract fun medicationGivenDao(): MedicationGivenDao
    abstract fun referralDao(): ReferralDao
    abstract fun encounterAddendumDao(): EncounterAddendumDao
    abstract fun photoDao(): PhotoDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun commentDao(): CommentDao
    abstract fun commentReadStateDao(): CommentReadStateDao
    abstract fun commentRevisionDao(): CommentRevisionDao
    abstract fun memberDao(): MemberDao
    abstract fun inviteDao(): InviteDao
    abstract fun recordLockDao(): RecordLockDao
    abstract fun recordLockStateDao(): RecordLockStateDao
    abstract fun syncDao(): SyncDao
    abstract fun syncConflictDao(): SyncConflictDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun errorLogDao(): ErrorLogDao

    companion object {
        const val NAME = "fieldmedic.db"
    }
}
