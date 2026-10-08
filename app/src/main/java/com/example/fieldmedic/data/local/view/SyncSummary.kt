package com.example.fieldmedic.data.local.view

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    viewName = "v_sync_summary",
    value = """
        SELECT
          COALESCE(SUM(CASE WHEN status = 'PENDING'   THEN 1 ELSE 0 END), 0) AS pending,
          COALESCE(SUM(CASE WHEN status = 'IN_FLIGHT' THEN 1 ELSE 0 END), 0) AS sending,
          COALESCE(SUM(CASE WHEN status = 'ERROR'     THEN 1 ELSE 0 END), 0) AS error,
          (SELECT COUNT(*) FROM sync_conflict WHERE status IN ('OPEN','ESCALATED')) AS `conflict`,
          MIN(CASE WHEN status IN ('PENDING','IN_FLIGHT','ERROR') THEN created_at END) AS oldestUnsyncedAt
        FROM sync_queue
    """
)
data class SyncSummary(
    @ColumnInfo(name = "pending")
    val pending: Int,

    @ColumnInfo(name = "sending")
    val sending: Int,

    @ColumnInfo(name = "error")
    val error: Int,

    @ColumnInfo(name = "conflict")
    val conflict: Int,

    @ColumnInfo(name = "oldestUnsyncedAt")
    val oldestUnsyncedAt: Long?,
)
