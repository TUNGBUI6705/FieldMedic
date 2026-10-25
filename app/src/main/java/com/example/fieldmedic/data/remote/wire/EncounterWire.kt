package com.example.fieldmedic.data.remote.wire

import com.example.fieldmedic.data.local.entity.Encounter
import com.example.fieldmedic.data.local.entity.MedicationGiven
import com.example.fieldmedic.data.local.entity.Referral
import com.example.fieldmedic.data.local.entity.VitalSign
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object EncounterWire {

    val TOP_LEVEL_KEYS: Set<String> = setOf(
        "stationId", "patientId", "staffId", "status", "visitAt", "completedAt", "isQuickExam",
        "chiefComplaint", "symptomNote", "diagnosis", "treatmentNote", "onsetAt", "onsetText",
        "symptomsJson", "dangerSignsJson", "pregnancyStatus", "gestationalWeeks", "diagnosisCode",
        "referralNeeded", "followUpDate", "alertLevel", "thresholdSetVersion", "hasDangerSign",
        "photoConsent", "photoConsentBy", "photoConsentAt", "lat", "lng", "deviceId"
    )

    fun toPayload(
        encounter: Encounter,
        vitals: List<VitalSign>,
        medications: List<MedicationGiven>,
        referral: Referral?
    ): JsonObject {
        return buildJsonObject {
            put("stationId", encounter.stationId)
            put("patientId", encounter.patientId)
            put("staffId", encounter.staffId)
            put("status", encounter.status.name)
            put("visitAt", encounter.visitAt)
            putNullableLong("completedAt", encounter.completedAt)
            put("isQuickExam", encounter.isQuickExam)
            putNullableString("chiefComplaint", encounter.chiefComplaint)
            putNullableString("symptomNote", encounter.symptomNote)
            putNullableString("diagnosis", encounter.diagnosis)
            putNullableString("treatmentNote", encounter.treatmentNote)
            putNullableLong("onsetAt", encounter.onsetAt)
            putNullableString("onsetText", encounter.onsetText)
            put("symptomsJson", encounter.symptomsJson)
            put("dangerSignsJson", encounter.dangerSignsJson)
            put("pregnancyStatus", encounter.pregnancyStatus.name)
            putNullableInt("gestationalWeeks", encounter.gestationalWeeks)
            putNullableString("diagnosisCode", encounter.diagnosisCode)
            put("referralNeeded", encounter.referralNeeded)
            putNullableString("followUpDate", encounter.followUpDate)
            put("alertLevel", encounter.alertLevel.name)
            putNullableInt("thresholdSetVersion", encounter.thresholdSetVersion)
            put("hasDangerSign", encounter.hasDangerSign)
            put("photoConsent", encounter.photoConsent)
            putNullableEnum("photoConsentBy", encounter.photoConsentBy)
            putNullableLong("photoConsentAt", encounter.photoConsentAt)
            putNullableDouble("lat", encounter.lat)
            putNullableDouble("lng", encounter.lng)
            putNullableString("deviceId", encounter.deviceId)

            put(
                "vitals",
                buildJsonArray {
                    for (vital in vitals) {
                        add(
                            buildJsonObject {
                                put("id", vital.id)
                                putNullableString("readingId", vital.readingId)
                                put("type", vital.type.name)
                                put("value", vital.value)
                                put("unit", vital.unit)
                                put("source", vital.source.name)
                                putNullableString("deviceId", vital.deviceId)
                                putNullableDouble("originalValue", vital.originalValue)
                                put("isOverridden", vital.isOverridden)
                                putNullableEnum("context", vital.context)
                                put("plausibilityConfirmed", vital.plausibilityConfirmed)
                                put("abnormalLevel", vital.abnormalLevel.name)
                                put("thresholdSetVersion", vital.thresholdSetVersion)
                                put("measuredAt", vital.measuredAt)
                            }
                        )
                    }
                }
            )

            put(
                "medications",
                buildJsonArray {
                    for (med in medications) {
                        add(
                            buildJsonObject {
                                put("id", med.id)
                                putNullableString("drugId", med.drugId)
                                put("drugName", med.drugName)
                                put("dose", med.dose)
                                put("frequency", med.frequency)
                                put("days", med.days)
                                putNullableString("quantity", med.quantity)
                                putNullableString("instruction", med.instruction)
                                put("allergyCheck", med.allergyCheck.name)
                                putNullableString("allergyOverrideReason", med.allergyOverrideReason)
                                put("pregnancyCheck", med.pregnancyCheck.name)
                                put("duplicateAck", med.duplicateAck)
                            }
                        )
                    }
                }
            )

            if (referral != null) {
                put(
                    "referral",
                    buildJsonObject {
                        put("id", referral.id)
                        put("urgency", referral.urgency.name)
                        put("destinationName", referral.destinationName)
                        putNullableString("reason", referral.reason)
                        put("status", referral.status.name)
                        put("issuedAt", referral.issuedAt)
                        put("issuedBy", referral.issuedBy)
                        putNullableString("slipSnapshotJson", referral.slipSnapshotJson)
                    }
                )
            }
        }
    }

    private fun JsonObjectBuilder.putNullableString(key: String, value: String?) {
        if (value != null) put(key, value) else put(key, JsonNull)
    }

    private fun JsonObjectBuilder.putNullableInt(key: String, value: Int?) {
        if (value != null) put(key, value) else put(key, JsonNull)
    }

    private fun JsonObjectBuilder.putNullableLong(key: String, value: Long?) {
        if (value != null) put(key, value) else put(key, JsonNull)
    }

    private fun JsonObjectBuilder.putNullableDouble(key: String, value: Double?) {
        if (value != null) put(key, value) else put(key, JsonNull)
    }

    private fun <E : Enum<E>> JsonObjectBuilder.putNullableEnum(key: String, value: E?) {
        if (value != null) put(key, value.name) else put(key, JsonNull)
    }
}
