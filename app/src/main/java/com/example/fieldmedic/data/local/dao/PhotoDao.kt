package com.example.fieldmedic.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fieldmedic.data.local.entity.Photo
import com.example.fieldmedic.data.local.entity.PhotoUploadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Upsert
    suspend fun upsert(photo: Photo)

    @Upsert
    suspend fun upsertAll(photos: List<Photo>)

    @Query("SELECT * FROM photo WHERE owner_type = :ownerType AND owner_id = :ownerId AND is_deleted = 0")
    fun forOwner(ownerType: String, ownerId: String): Flow<List<Photo>>

    @Query("SELECT * FROM photo WHERE upload_status IN ('LOCAL', 'FAILED') AND is_deleted = 0")
    suspend fun pendingUploads(): List<Photo>

    @Query(
        """
        UPDATE photo
        SET upload_status = :status,
            bytes_uploaded = :bytesUploaded,
            remote_path = COALESCE(:remotePath, remote_path)
        WHERE id = :id
        """
    )
    suspend fun updateUpload(id: String, status: PhotoUploadStatus, bytesUploaded: Int, remotePath: String?): Int
}
