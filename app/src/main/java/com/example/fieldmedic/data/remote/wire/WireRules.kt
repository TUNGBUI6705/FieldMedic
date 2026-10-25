package com.example.fieldmedic.data.remote.wire

object WireRules {
    val FORBIDDEN_KEYS: Set<String> = setOf(
        "syncMeta", "syncStatus", "serverUpdatedAt", "localPath", "version",
        "createdAt", "updatedAt", "updatedBy", "isDeleted", "lastEncounterAt",
        "uploadStatus", "uploadSession", "bytesUploaded", "remotePath", "dekVersion", "isFileEncrypted"
    )
}
