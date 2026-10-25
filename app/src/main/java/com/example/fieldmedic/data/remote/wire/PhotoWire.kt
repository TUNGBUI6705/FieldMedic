package com.example.fieldmedic.data.remote.wire

import com.example.fieldmedic.data.local.entity.Photo
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object PhotoWire {

    val KEYS: Set<String> = setOf(
        "id", "ownerType", "ownerId", "patientId", "mime", "width", "height",
        "sizeBytes", "checksumSha256", "caption", "takenAt", "takenBy", "exifGpsStripped"
    )

    fun toPayload(photo: Photo): JsonObject {
        return buildJsonObject {
            put("id", photo.id)
            put("ownerType", photo.ownerType.name)
            put("ownerId", photo.ownerId)
            put("patientId", photo.patientId)
            put("mime", photo.mime)
            put("width", photo.width)
            put("height", photo.height)
            put("sizeBytes", photo.sizeBytes)
            put("checksumSha256", photo.checksumSha256)
            putNullableString("caption", photo.caption)
            put("takenAt", photo.takenAt)
            put("takenBy", photo.takenBy)
            put("exifGpsStripped", photo.exifGpsStripped)
        }
    }

    private fun JsonObjectBuilder.putNullableString(key: String, value: String?) {
        if (value != null) put(key, value) else put(key, JsonNull)
    }
}
