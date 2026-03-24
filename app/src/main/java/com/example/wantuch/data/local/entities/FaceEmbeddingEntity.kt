package com.example.wantuch.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "face_embeddings")
data class FaceEmbeddingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int,
    val category: String, // STUDENT or STAFF
    val userName: String,
    val embedding: String, // JSON array of 128 floats
    val createdAt: Long = System.currentTimeMillis()
)
