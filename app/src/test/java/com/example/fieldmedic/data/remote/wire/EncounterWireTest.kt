package com.example.fieldmedic.data.remote.wire

import com.example.fieldmedic.data.local.entity.AlertLevel
import com.example.fieldmedic.data.local.entity.Encounter
import com.example.fieldmedic.data.local.entity.EncounterStatus
import com.example.fieldmedic.data.local.entity.MedicationGiven
import com.example.fieldmedic.data.local.entity.Photo
import com.example.fieldmedic.data.local.entity.PhotoOwnerType
import com.example.fieldmedic.data.local.entity.Referral
import com.example.fieldmedic.data.local.entity.ReferralStatus
import com.example.fieldmedic.data.local.entity.ReferralUrgency
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.data.local.entity.VitalSign
import com.example.fieldmedic.data.local.entity.VitalSource
import com.example.fieldmedic.data.local.entity.VitalType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterWireTest {

    private fun createSampleEncounter(): Encounter {
        return Encounter(
            id = "enc1",
            patientId = "p1",
            staffId = "s1",
            stationId = "st1",
            status = EncounterStatus.DRAFT,
            visitAt = 1000L,
            chiefComplaint = null, // test JsonNull
            alertLevel = AlertLevel.NONE,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
    }

    private fun createSampleVital(): VitalSign {
        return VitalSign(
            id = "v1",
            encounterId = "enc1",
            patientId = "p1",
            readingId = null, // test JsonNull
            type = VitalType.TEMP,
            value = 37.0,
            unit = "°C",
            source = VitalSource.MANUAL,
            thresholdSetVersion = 1,
            measuredAt = 1000L,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
    }

    private fun createSampleMedication(): MedicationGiven {
        return MedicationGiven(
            id = "m1",
            encounterId = "enc1",
            patientId = "p1",
            drugName = "Paracetamol",
            dose = "500mg",
            frequency = "q6h",
            days = 3,
            quantity = null, // test JsonNull
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
    }

    private fun createSampleReferral(): Referral {
        return Referral(
            id = "r1",
            encounterId = "enc1",
            patientId = "p1",
            urgency = ReferralUrgency.ROUTINE,
            destinationName = "Hospital",
            reason = null, // test JsonNull
            status = ReferralStatus.ISSUED,
            issuedAt = 1000L,
            issuedBy = "s1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
    }

    private fun createSamplePhoto(): Photo {
        return Photo(
            id = "ph1",
            ownerType = PhotoOwnerType.ENCOUNTER,
            ownerId = "enc1",
            patientId = "p1",
            localPath = "/local/path",
            width = 100,
            height = 100,
            sizeBytes = 1000L,
            checksumSha256 = "hash",
            caption = null, // test JsonNull
            takenAt = 1000L,
            takenBy = "s1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
    }

    @Test
    fun topLevelKeys_matchExpected() {
        val encounter = createSampleEncounter()
        val payloadWithoutReferral = EncounterWire.toPayload(encounter, listOf(createSampleVital()), listOf(createSampleMedication()), null)
        val expectedKeysWithoutReferral = EncounterWire.TOP_LEVEL_KEYS + setOf("vitals", "medications")
        assertEquals(expectedKeysWithoutReferral, payloadWithoutReferral.keys)

        val payloadWithReferral = EncounterWire.toPayload(encounter, listOf(createSampleVital()), listOf(createSampleMedication()), createSampleReferral())
        val expectedKeysWithReferral = EncounterWire.TOP_LEVEL_KEYS + setOf("vitals", "medications", "referral")
        assertEquals(expectedKeysWithReferral, payloadWithReferral.keys)
    }

    @Test
    fun referralNull_omitsReferralKey() {
        val encounter = createSampleEncounter()
        val payload = EncounterWire.toPayload(encounter, emptyList(), emptyList(), null)
        assertFalse(payload.containsKey("referral"))
    }

    @Test
    fun photoKeys_matchExpected() {
        val photo = createSamplePhoto()
        val payload = PhotoWire.toPayload(photo)
        assertEquals(PhotoWire.KEYS, payload.keys)
    }

    @Test
    fun noForbiddenKeysInPayloads() {
        val encounter = createSampleEncounter()
        val encPayload = EncounterWire.toPayload(encounter, listOf(createSampleVital()), listOf(createSampleMedication()), createSampleReferral())
        val photoPayload = PhotoWire.toPayload(createSamplePhoto())

        fun assertNoForbiddenKeys(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    for ((key, value) in element) {
                        assertFalse("Payload contains forbidden key: $key", WireRules.FORBIDDEN_KEYS.contains(key))
                        assertNoForbiddenKeys(value)
                    }
                }
                is JsonArray -> {
                    for (item in element) {
                        assertNoForbiddenKeys(item)
                    }
                }
                else -> {}
            }
        }

        assertNoForbiddenKeys(encPayload)
        assertNoForbiddenKeys(photoPayload)
    }

    @Test
    fun nullableFields_areEncodedAsJsonNull() {
        val encounter = createSampleEncounter()
        val encPayload = EncounterWire.toPayload(encounter, listOf(createSampleVital()), listOf(createSampleMedication()), createSampleReferral())

        assertTrue(encPayload.containsKey("chiefComplaint"))
        assertEquals(JsonNull, encPayload["chiefComplaint"])

        val vitalObj = encPayload["vitals"]!!.jsonArray[0] as JsonObject
        assertTrue(vitalObj.containsKey("readingId"))
        assertEquals(JsonNull, vitalObj["readingId"])

        val medObj = encPayload["medications"]!!.jsonArray[0] as JsonObject
        assertTrue(medObj.containsKey("quantity"))
        assertEquals(JsonNull, medObj["quantity"])

        val refObj = encPayload["referral"] as JsonObject
        assertTrue(refObj.containsKey("reason"))
        assertEquals(JsonNull, refObj["reason"])

        val photoPayload = PhotoWire.toPayload(createSamplePhoto())
        assertTrue(photoPayload.containsKey("caption"))
        assertEquals(JsonNull, photoPayload["caption"])
    }

    @Test
    fun enums_areEncodedByName() {
        val encounter = createSampleEncounter()
        val encPayload = EncounterWire.toPayload(encounter, listOf(createSampleVital()), listOf(createSampleMedication()), createSampleReferral())

        assertEquals(EncounterStatus.DRAFT.name, encPayload["status"]!!.jsonPrimitive.content)
        assertEquals(AlertLevel.NONE.name, encPayload["alertLevel"]!!.jsonPrimitive.content)

        val vitalObj = encPayload["vitals"]!!.jsonArray[0] as JsonObject
        assertEquals(VitalType.TEMP.name, vitalObj["type"]!!.jsonPrimitive.content)
        assertEquals(VitalSource.MANUAL.name, vitalObj["source"]!!.jsonPrimitive.content)

        val photoPayload = PhotoWire.toPayload(createSamplePhoto())
        assertEquals(PhotoOwnerType.ENCOUNTER.name, photoPayload["ownerType"]!!.jsonPrimitive.content)
    }
}
