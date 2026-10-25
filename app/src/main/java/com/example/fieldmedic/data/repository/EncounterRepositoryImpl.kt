package com.example.fieldmedic.data.repository

import com.example.fieldmedic.data.local.dao.EncounterDao
import com.example.fieldmedic.data.local.entity.Encounter
import com.example.fieldmedic.data.local.repository.EncounterBundle
import com.example.fieldmedic.data.local.repository.EncounterWriter
import com.example.fieldmedic.domain.repository.EncounterRepository
import kotlinx.coroutines.flow.Flow

class EncounterRepositoryImpl(
    private val encounterDao: EncounterDao,
    private val encounterWriter: EncounterWriter
) : EncounterRepository {

    override fun observeEncounterHistory(patientId: String): Flow<List<Encounter>> {
        return encounterDao.observeHistory(patientId)
    }

    override suspend fun getDraftEncounter(patientId: String): Encounter? {
        return encounterDao.getDraft(patientId)
    }

    override suspend fun saveCompletedEncounter(bundle: EncounterBundle) {
        encounterWriter.saveCompletedEncounter(bundle)
    }
}
