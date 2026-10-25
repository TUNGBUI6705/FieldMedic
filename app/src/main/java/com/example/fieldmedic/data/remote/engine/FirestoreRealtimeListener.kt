package com.example.fieldmedic.data.remote.engine

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FirestoreRealtimeListener(
    private val pullSyncEngine: PullSyncEngine,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Tắt persistence SDK theo quy định trong AGENTS.md
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
    }
) {

    private val registrations = mutableListOf<ListenerRegistration>()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    fun startListening(collections: List<String> = listOf("patients", "encounters", "photos")) {
        stopListening()

        for (col in collections) {
            val registration = firestore.collection(col)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                    scope.launch {
                        try {
                            pullSyncEngine.pullCollection(col)
                        } catch (_: Exception) {}
                    }
                }
            registrations.add(registration)
        }
    }

    fun stopListening() {
        for (reg in registrations) {
            reg.remove()
        }
        registrations.clear()
    }
}
