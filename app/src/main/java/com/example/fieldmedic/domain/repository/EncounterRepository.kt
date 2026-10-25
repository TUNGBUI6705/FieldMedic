package com.example.fieldmedic.domain.repository

import com.example.fieldmedic.data.local.entity.Encounter
import com.example.fieldmedic.data.local.repository.EncounterBundle
import kotlinx.coroutines.flow.Flow

interface EncounterRepository {
    fun observeEncounterHistory(patientId: String): Flow<List<Encounter>>
    suspend fun getDraftEncounter(patientId: String): Encounter?
    suspend fun saveCompletedEncounter(bundle: EncounterBundle)
}
