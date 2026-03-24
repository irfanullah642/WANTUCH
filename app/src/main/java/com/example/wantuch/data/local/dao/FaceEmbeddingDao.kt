package com.example.wantuch.data.local.dao

import androidx.room.*
import com.example.wantuch.data.local.entities.FaceEmbeddingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceEmbeddingDao {
    @Query("SELECT * FROM face_embeddings")
    fun getAllEmbeddings(): Flow<List<FaceEmbeddingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(face: FaceEmbeddingEntity): Long

    @Query("DELETE FROM face_embeddings WHERE userId = :userId AND category = :category")
    suspend fun deleteForUser(userId: Int, category: String): Int

    @Query("SELECT * FROM face_embeddings WHERE userId = :userId AND category = :category")
    suspend fun getEmbeddingsForUser(userId: Int, category: String): List<FaceEmbeddingEntity>
}
