package com.example.fieldmedic.util

import androidx.room.withTransaction
import com.example.fieldmedic.BuildConfig
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.Encounter
import com.example.fieldmedic.data.local.entity.EncounterStatus
import com.example.fieldmedic.data.local.entity.Gender
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.data.local.entity.Village
import kotlin.random.Random

object DebugSeeder {

    private val lastNames = listOf("Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng")
    private val middleNames = listOf("Văn", "Thị", "Hữu", "Đình", "Minh", "Gia", "Thành", "Thu", "Ngọc", "Hoài")
    private val firstNames = listOf("An", "Bình", "Cường", "Dũng", "Đức", "Hà", "Hải", "Hòa", "Hùng", "Hương", "Khánh", "Linh", "Long", "Nam", "Nga", "Phong", "Phúc", "Quân", "Sơn", "Tâm", "Thắng", "Thảo", "Thu", "Trang", "Trung", "Tuấn", "Việt", "Yến")

    suspend fun seed(db: AppDatabase, patientsCount: Int = 10_000, encountersPerPatient: Int = 5) {
        if (!BuildConfig.DEBUG) return

        val stationId = "st_debug"
        db.withTransaction {
            db.openHelper.writableDatabase.execSQL(
                "INSERT OR IGNORE INTO station (id, name, code, commune, district, province) VALUES ('$stationId', 'Trạm Y Tế Test', 'TEST01', 'Xã Test', 'Huyện Test', 'Tỉnh Test')"
            )

            val villages = (1..20).map { i ->
                Village(
                    id = "v_$i",
                    stationId = stationId,
                    name = "Thôn $i",
                    isActive = true
                )
            }
            db.villageDao().upsertAll(villages)
        }

        val batchSize = 500
        val now = System.currentTimeMillis()
        val random = Random(42)

        for (chunkStart in 0 until patientsCount step batchSize) {
            val chunkEnd = minOf(chunkStart + batchSize, patientsCount)
            val patientBatch = mutableListOf<Patient>()
            val encounterBatch = mutableListOf<Encounter>()

            for (i in chunkStart until chunkEnd) {
                val pId = "pat_${i + 1}"
                val lastName = lastNames[random.nextInt(lastNames.size)]
                val middleName = middleNames[random.nextInt(middleNames.size)]
                val firstName = firstNames[random.nextInt(firstNames.size)]
                val fullName = "$lastName $middleName $firstName"
                val searchName = normalizeSearch(fullName)
                val birthYear = random.nextInt(1940, 2023)
                val gender = if (random.nextBoolean()) Gender.MALE else Gender.FEMALE
                val villageId = "v_${random.nextInt(1, 21)}"
                val phone = "09" + String.format("%08d", random.nextInt(100000000))
                val patientCode = "P" + String.format("%06d", i + 1)

                val lastEncounterAt = now - random.nextLong(0, 365L * 24 * 3600 * 1000)

                val patient = Patient(
                    id = pId,
                    patientCode = patientCode,
                    stationId = stationId,
                    fullName = fullName,
                    searchName = searchName,
                    birthYear = birthYear,
                    gender = gender,
                    phone = phone,
                    villageId = villageId,
                    ownerStaffId = "staff_debug",
                    lastEncounterAt = lastEncounterAt,
                    syncMeta = SyncMeta(createdAt = now, updatedAt = now)
                )
                patientBatch.add(patient)

                for (e in 0 until encountersPerPatient) {
                    val encId = "enc_${i + 1}_$e"
                    val visitAt = lastEncounterAt - (e * 86400000L)
                    val encounter = Encounter(
                        id = encId,
                        patientId = pId,
                        staffId = "staff_debug",
                        stationId = stationId,
                        status = EncounterStatus.COMPLETED,
                        visitAt = visitAt,
                        completedAt = visitAt + 3600L,
                        chiefComplaint = "Khám định kỳ",
                        syncMeta = SyncMeta(createdAt = visitAt, updatedAt = visitAt)
                    )
                    encounterBatch.add(encounter)
                }
            }

            db.withTransaction {
                db.patientDao().upsertAll(patientBatch)
                for (enc in encounterBatch) {
                    db.encounterDao().upsert(enc)
                }
            }
        }
    }
}
