package com.example.fieldmedic.domain.usecase

import androidx.paging.PagingSource
import com.example.fieldmedic.data.local.relation.PatientListItem
import com.example.fieldmedic.domain.repository.PatientRepository
import com.example.fieldmedic.util.normalizeSearch

class GetPatientPagingUseCase(
    private val patientRepository: PatientRepository,
) {

    operator fun invoke(
        query: String = "",
        villageId: String? = null
    ): PagingSource<Int, PatientListItem> {
        val normalizedQuery = normalizeSearch(query)
        return patientRepository.getPatientPaging(query = normalizedQuery, villageId = villageId)
    }
}
