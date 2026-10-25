package com.example.fieldmedic.di

import com.example.fieldmedic.data.local.auth.SessionManager
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.repository.AuditLogger
import com.example.fieldmedic.data.local.repository.EncounterWriter
import com.example.fieldmedic.data.local.repository.SyncEnqueuer
import com.example.fieldmedic.data.repository.CatalogRepositoryImpl
import com.example.fieldmedic.data.repository.ClinicalHistoryRepositoryImpl
import com.example.fieldmedic.data.repository.CommentRepositoryImpl
import com.example.fieldmedic.data.repository.EncounterRepositoryImpl
import com.example.fieldmedic.data.repository.PatientRepositoryImpl
import com.example.fieldmedic.domain.repository.CatalogRepository
import com.example.fieldmedic.domain.repository.ClinicalHistoryRepository
import com.example.fieldmedic.domain.repository.CommentRepository
import com.example.fieldmedic.domain.repository.EncounterRepository
import com.example.fieldmedic.domain.repository.PatientRepository

class RepositoryModule(
    private val dbProvider: () -> AppDatabase,
    private val sessionManager: SessionManager = SessionManager(),
    private val deviceId: String = "dev-device",
    private val appVersion: String = "1.0"
) {

    val syncEnqueuer: SyncEnqueuer by lazy {
        SyncEnqueuer(dbProvider)
    }

    val auditLogger: AuditLogger by lazy {
        AuditLogger(dbProvider, deviceId, appVersion)
    }

    val encounterWriter: EncounterWriter by lazy {
        EncounterWriter(dbProvider, syncEnqueuer, auditLogger)
    }

    val patientRepository: PatientRepository by lazy {
        PatientRepositoryImpl(dbProvider().patientDao(), dbProvider().patientAllergyDao(), dbProvider().villageDao())
    }

    val catalogRepository: CatalogRepository by lazy {
        CatalogRepositoryImpl(dbProvider().thresholdDao(), dbProvider().drugDao(), dbProvider().allergenDao())
    }

    val encounterRepository: EncounterRepository by lazy {
        EncounterRepositoryImpl(dbProvider().encounterDao(), encounterWriter)
    }

    val clinicalHistoryRepository: ClinicalHistoryRepository by lazy {
        ClinicalHistoryRepositoryImpl(
            dbProvider = dbProvider,
            allergyDao = dbProvider().patientAllergyDao(),
            conditionDao = dbProvider().patientConditionDao(),
            medicationDao = dbProvider().patientMedicationDao(),
            syncEnqueuer = syncEnqueuer,
            auditLogger = auditLogger,
            sessionManager = sessionManager
        )
    }

    val commentRepository: CommentRepository by lazy {
        CommentRepositoryImpl(
            dbProvider = dbProvider,
            commentDao = dbProvider().commentDao(),
            readStateDao = dbProvider().commentReadStateDao(),
            syncEnqueuer = syncEnqueuer,
            auditLogger = auditLogger,
            sessionManager = sessionManager
        )
    }
}
