package com.example.fieldmedic.data.local.repository

sealed class EncounterWriteException(message: String) : RuntimeException(message) {
    class AlreadyCompleted(val encounterId: String) :
        EncounterWriteException("Encounter already completed: " + encounterId)
    class InconsistentBundle(val reason: String) :
        EncounterWriteException("Inconsistent bundle: " + reason)
}
