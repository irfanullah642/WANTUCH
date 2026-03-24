package com.example.wantuch.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.wantuch.domain.model.StaffMember

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: Int,
    val institutionId: Int,
    val name: String,
    val initials: String,
    val role: String,
    val bps: String,
    val marked: String,
    val stats: String,
    val profile_pic: String?
)

fun StaffMember.toEntity(institutionId: Int): StaffEntity {
    val staffId = when (id) {
        is Double -> id.toInt()
        is Int -> id
        is String -> id.toIntOrNull() ?: 0
        else -> 0
    }
    return StaffEntity(
        id = staffId,
        institutionId = institutionId,
        name = name,
        initials = initials,
        role = role,
        bps = bps,
        marked = marked,
        stats = stats,
        profile_pic = profile_pic
    )
}

fun StaffEntity.toDomain(): StaffMember {
    return StaffMember(
        id = id,
        name = name,
        initials = initials,
        role = role,
        bps = bps,
        marked = marked,
        paid = null,
        balance = null,
        stats = stats,
        profile_pic = profile_pic
    )
}
