package com.example.fieldmedic.data.remote.auth

import com.example.fieldmedic.data.local.dao.StaffDao
import com.example.fieldmedic.data.local.entity.Staff
import com.example.fieldmedic.data.local.entity.StaffRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthTransport(
    private val staffDao: StaffDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun loginAndSyncStaff(email: String, pass: String): Staff? {
        val authResult = auth.signInWithEmailAndPassword(email, pass).await()
        val uid = authResult.user?.uid ?: return null

        val doc = firestore.collection("staffs").document(uid).get().await()
        if (!doc.exists()) return null

        val staff = Staff(
            id = uid,
            stationId = doc.getString("stationId"),
            fullName = doc.getString("fullName") ?: "Nhân viên y tế",
            role = StaffRole.valueOf(doc.getString("role") ?: StaffRole.NURSE.name),
            phone = doc.getString("phone"),
            isActive = doc.getBoolean("isActive") ?: true,
            isExternal = doc.getBoolean("isExternal") ?: false,
            lastSyncAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        staffDao.upsertAll(listOf(staff))
        return staff
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun signOut() {
        auth.signOut()
    }
}
