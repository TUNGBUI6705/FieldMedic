package com.example.fieldmedic.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fieldmedic.data.local.db.AppDatabase
import com.example.fieldmedic.data.local.entity.AuditAction
import com.example.fieldmedic.data.local.entity.AuditLog
import com.example.fieldmedic.data.local.entity.AuditResult
import com.example.fieldmedic.data.local.entity.CommentTarget
import com.example.fieldmedic.data.local.entity.CommentType
import com.example.fieldmedic.data.local.entity.ConflictStatus
import com.example.fieldmedic.data.local.entity.Gender
import com.example.fieldmedic.data.local.entity.MemberStatus
import com.example.fieldmedic.data.local.entity.Patient
import com.example.fieldmedic.data.local.entity.QueueOperation
import com.example.fieldmedic.data.local.entity.QueueStatus
import com.example.fieldmedic.data.local.entity.RecordComment
import com.example.fieldmedic.data.local.entity.RecordMember
import com.example.fieldmedic.data.local.entity.RecordRole
import com.example.fieldmedic.data.local.entity.SyncConflict
import com.example.fieldmedic.data.local.entity.SyncEntityType
import com.example.fieldmedic.data.local.entity.SyncQueue
import com.example.fieldmedic.data.local.entity.SyncMeta
import com.example.fieldmedic.util.normalizeSearch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollabSyncAndAuditDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var patientDao: PatientDao
    private lateinit var commentDao: CommentDao
    private lateinit var readStateDao: CommentReadStateDao
    private lateinit var memberDao: MemberDao
    private lateinit var syncDao: SyncDao
    private lateinit var conflictDao: SyncConflictDao
    private lateinit var auditLogDao: AuditLogDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        patientDao = database.patientDao()
        commentDao = database.commentDao()
        readStateDao = database.commentReadStateDao()
        memberDao = database.memberDao()
        syncDao = database.syncDao()
        conflictDao = database.syncConflictDao()
        auditLogDao = database.auditLogDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    private suspend fun insertSamplePatient(id: String = "p1") {
        val p = Patient(
            id = id,
            patientCode = "PAT001",
            stationId = "st1",
            fullName = "Phạm Văn Z",
            searchName = normalizeSearch("Phạm Văn Z"),
            birthYear = 1990,
            gender = Gender.MALE,
            ownerStaffId = "staff1",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        patientDao.upsert(p)
    }

    @Test
    fun testNextBatch_dependsOnDependency() = runBlocking {
        val parent = SyncQueue(
            id = "q1",
            entityType = SyncEntityType.PATIENT,
            entityId = "p1",
            operation = QueueOperation.CREATE,
            payload = "{}",
            baseValues = "{}",
            baseVersion = 1,
            status = QueueStatus.PENDING,
            priority = 1,
            idempotencyKey = "key1",
            createdAt = 1000L,
            nextAttemptAt = 0L
        )
        val child = SyncQueue(
            id = "q2",
            entityType = SyncEntityType.PATIENT_ALLERGY,
            entityId = "a1",
            operation = QueueOperation.CREATE,
            payload = "{}",
            baseValues = "{}",
            baseVersion = 1,
            status = QueueStatus.PENDING,
            priority = 1,
            dependsOn = "p1", // depends on parent entityId
            idempotencyKey = "key2",
            createdAt = 1100L,
            nextAttemptAt = 0L
        )

        syncDao.upsert(parent)
        syncDao.upsert(child)

        // When parent is in queue, nextBatch should only return parent
        val batch1 = syncDao.nextBatch(now = 2000L, limit = 10)
        assertEquals(1, batch1.size)
        assertEquals("q1", batch1[0].id)

        // Delete parent from queue
        syncDao.delete("q1")

        // Now nextBatch should return child
        val batch2 = syncDao.nextBatch(now = 2000L, limit = 10)
        assertEquals(1, batch2.size)
        assertEquals("q2", batch2[0].id)
    }

    @Test
    fun testNextBatch_orderingAndFiltering() = runBlocking {
        val itemPending1 = SyncQueue(
            id = "q1",
            entityType = SyncEntityType.PATIENT,
            entityId = "p1",
            operation = QueueOperation.CREATE,
            payload = "{}",
            baseValues = "{}",
            baseVersion = 1,
            status = QueueStatus.PENDING,
            priority = 2,
            idempotencyKey = "k1",
            createdAt = 1000L,
            nextAttemptAt = 0L
        )
        val itemPending2 = SyncQueue(
            id = "q2",
            entityType = SyncEntityType.PATIENT,
            entityId = "p2",
            operation = QueueOperation.CREATE,
            payload = "{}",
            baseValues = "{}",
            baseVersion = 1,
            status = QueueStatus.PENDING,
            priority = 1, // higher priority (lower number)
            idempotencyKey = "k2",
            createdAt = 1000L,
            nextAttemptAt = 0L
        )
        val itemFuture = SyncQueue(
            id = "q3",
            entityType = SyncEntityType.PATIENT,
            entityId = "p3",
            operation = QueueOperation.CREATE,
            payload = "{}",
            baseValues = "{}",
            baseVersion = 1,
            status = QueueStatus.PENDING,
            priority = 1,
            idempotencyKey = "k3",
            createdAt = 1000L,
            nextAttemptAt = 5000L // in future
        )
        val itemError = SyncQueue(
            id = "q4",
            entityType = SyncEntityType.PATIENT,
            entityId = "p4",
            operation = QueueOperation.CREATE,
            payload = "{}",
            baseValues = "{}",
            baseVersion = 1,
            status = QueueStatus.ERROR,
            priority = 1,
            idempotencyKey = "k4",
            createdAt = 1000L,
            nextAttemptAt = 0L
        )

        syncDao.upsert(itemPending1)
        syncDao.upsert(itemPending2)
        syncDao.upsert(itemFuture)
        syncDao.upsert(itemError)

        val batch = syncDao.nextBatch(now = 2000L, limit = 10)
        assertEquals(2, batch.size)
        assertEquals("q2", batch[0].id) // priority 1 comes first
        assertEquals("q1", batch[1].id) // priority 2 comes second
    }

    @Test
    fun testSyncSummary_view() = runBlocking {
        val q1 = SyncQueue(id = "1", entityType = SyncEntityType.PATIENT, entityId = "p1", operation = QueueOperation.CREATE, payload = "{}", baseValues = "{}", baseVersion = 1, status = QueueStatus.PENDING, priority = 1, idempotencyKey = "k1", createdAt = 1000L)
        val q2 = SyncQueue(id = "2", entityType = SyncEntityType.PATIENT, entityId = "p2", operation = QueueOperation.CREATE, payload = "{}", baseValues = "{}", baseVersion = 1, status = QueueStatus.PENDING, priority = 1, idempotencyKey = "k2", createdAt = 500L)
        val q3 = SyncQueue(id = "3", entityType = SyncEntityType.PATIENT, entityId = "p3", operation = QueueOperation.CREATE, payload = "{}", baseValues = "{}", baseVersion = 1, status = QueueStatus.PENDING, priority = 1, idempotencyKey = "k3", createdAt = 2000L)
        val q4 = SyncQueue(id = "4", entityType = SyncEntityType.PATIENT, entityId = "p4", operation = QueueOperation.CREATE, payload = "{}", baseValues = "{}", baseVersion = 1, status = QueueStatus.IN_FLIGHT, priority = 1, idempotencyKey = "k4", createdAt = 300L)
        val q5 = SyncQueue(id = "5", entityType = SyncEntityType.PATIENT, entityId = "p5", operation = QueueOperation.CREATE, payload = "{}", baseValues = "{}", baseVersion = 1, status = QueueStatus.ERROR, priority = 1, idempotencyKey = "k5", createdAt = 1500L)
        val q6 = SyncQueue(id = "6", entityType = SyncEntityType.PATIENT, entityId = "p6", operation = QueueOperation.CREATE, payload = "{}", baseValues = "{}", baseVersion = 1, status = QueueStatus.BLOCKED, priority = 1, idempotencyKey = "k6", createdAt = 100L)

        syncDao.upsert(q1)
        syncDao.upsert(q2)
        syncDao.upsert(q3)
        syncDao.upsert(q4)
        syncDao.upsert(q5)
        syncDao.upsert(q6)

        val c1 = SyncConflict(id = "c1", entityType = SyncEntityType.PATIENT, entityId = "p1", fieldDiffsJson = "{}", remoteVersion = 2, status = ConflictStatus.OPEN, detectedAt = 1000L)
        val c2 = SyncConflict(id = "c2", entityType = SyncEntityType.PATIENT, entityId = "p2", fieldDiffsJson = "{}", remoteVersion = 2, status = ConflictStatus.ESCALATED, detectedAt = 1000L)
        conflictDao.upsert(c1)
        conflictDao.upsert(c2)

        val summary = syncDao.observeSummary().first()
        assertEquals(3, summary.pending)
        assertEquals(1, summary.sending)
        assertEquals(1, summary.error)
        assertEquals(2, summary.conflict)
        assertEquals(300L, summary.oldestUnsyncedAt) // min created_at among PENDING/IN_FLIGHT/ERROR (q4 has 300L)
    }

    @Test
    fun testActiveRole() = runBlocking {
        insertSamplePatient()
        val now = 5000L
        val memberActive = RecordMember(
            id = "m1",
            patientId = "p1",
            staffId = "staff2",
            displayNameSnapshot = "Dr. A",
            role = RecordRole.EDITOR,
            source = com.example.fieldmedic.data.local.entity.MemberSource.OWNER,
            grantedBy = "staff1",
            grantedAt = 1000L,
            expiresAt = 10000L, // valid
            status = MemberStatus.ACTIVE,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val memberExpired = RecordMember(
            id = "m2",
            patientId = "p1",
            staffId = "staff3",
            displayNameSnapshot = "Dr. B",
            role = RecordRole.EDITOR,
            source = com.example.fieldmedic.data.local.entity.MemberSource.OWNER,
            grantedBy = "staff1",
            grantedAt = 1000L,
            expiresAt = 2000L, // expired
            status = MemberStatus.ACTIVE,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val memberRevoked = RecordMember(
            id = "m3",
            patientId = "p1",
            staffId = "staff4",
            displayNameSnapshot = "Dr. C",
            role = RecordRole.EDITOR,
            source = com.example.fieldmedic.data.local.entity.MemberSource.OWNER,
            grantedBy = "staff1",
            grantedAt = 1000L,
            status = MemberStatus.REVOKED,
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )

        memberDao.upsertAll(listOf(memberActive, memberExpired, memberRevoked))

        assertEquals(RecordRole.EDITOR, memberDao.activeRole("p1", "staff2", now))
        assertNull(memberDao.activeRole("p1", "staff3", now))
        assertNull(memberDao.activeRole("p1", "staff4", now))
    }

    @Test
    fun testObserveUnread_comments() = runBlocking {
        insertSamplePatient()
        val c1 = RecordComment(
            id = "c1",
            patientId = "p1",
            targetType = CommentTarget.RECORD,
            authorId = "staff2", // other person
            authorNameSnapshot = "Dr. B",
            authorRoleSnapshot = "EDITOR",
            type = CommentType.NOTE,
            body = "Hello",
            syncMeta = SyncMeta(createdAt = 1000L, updatedAt = 1000L)
        )
        val c2 = RecordComment(
            id = "c2",
            patientId = "p1",
            targetType = CommentTarget.RECORD,
            authorId = "staff1", // my comment
            authorNameSnapshot = "Me",
            authorRoleSnapshot = "OWNER",
            type = CommentType.NOTE,
            body = "Hi",
            syncMeta = SyncMeta(createdAt = 1500L, updatedAt = 1500L)
        )
        val c3 = RecordComment(
            id = "c3",
            patientId = "p1",
            targetType = CommentTarget.RECORD,
            authorId = "staff2", // other person
            authorNameSnapshot = "Dr. B",
            authorRoleSnapshot = "EDITOR",
            type = CommentType.NOTE,
            body = "Follow up",
            syncMeta = SyncMeta(createdAt = 2000L, updatedAt = 2000L)
        )

        commentDao.upsert(c1)
        commentDao.upsert(c2)
        commentDao.upsert(c3)

        // Before markRead, unread count for staff1 should be 2 (c1 and c3)
        val unreadBefore = commentDao.observeUnread("p1", "staff1").first()
        assertEquals(2, unreadBefore)

        // Mark read at 1500L (reads c1 at 1000L, but not c3 at 2000L)
        readStateDao.markRead("p1", 1500L)
        val unreadAfter = commentDao.observeUnread("p1", "staff1").first()
        assertEquals(1, unreadAfter) // only c3 is unread
    }

    @Test
    fun testAuditLogDao_immutabilityAndInsertion() = runBlocking {
        insertSamplePatient()
        val log1 = AuditLog(
            id = "l1",
            seq = 1L,
            actorId = "staff1",
            action = AuditAction.VIEW_RECORD,
            entityType = "PATIENT",
            patientId = "p1",
            deviceId = "dev1",
            appVersion = "1.0",
            result = AuditResult.OK,
            prevHash = "0",
            entryHash = "hash1",
            createdAt = 1000L
        )
        val log2 = AuditLog(
            id = "l2",
            seq = 2L,
            actorId = "staff1",
            action = AuditAction.LOCK_ENABLE,
            entityType = "RECORD_LOCK",
            patientId = "p1",
            deviceId = "dev1",
            appVersion = "1.0",
            result = AuditResult.OK,
            prevHash = "hash1",
            entryHash = "hash2",
            createdAt = 1500L
        )

        auditLogDao.insert(log1)
        auditLogDao.insert(log2)

        val last = auditLogDao.lastEntry()
        assertNotNull(last)
        assertEquals(2L, last?.seq)
        assertEquals("l2", last?.id)

        val all = auditLogDao.allOrdered()
        assertEquals(2, all.size)
        assertEquals(1L, all[0].seq)
        assertEquals(2L, all[1].seq)

        // Verify AuditLogDao has no update/delete methods via reflection/API availability
        val daoClass = AuditLogDao::class.java
        val updateOrDeleteMethods = daoClass.methods.filter {
            val name = it.name.lowercase()
            name.contains("update") || name.contains("delete") || name.contains("remove") || name.contains("edit")
        }
        assertTrue(updateOrDeleteMethods.isEmpty())
    }
}
