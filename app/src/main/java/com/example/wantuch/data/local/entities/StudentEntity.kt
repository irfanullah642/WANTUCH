package com.example.wantuch.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.wantuch.domain.model.StudentMember

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: Int,
    val institutionId: Int,
    val name: String,
    val username: String,
    val initials: String,
    val class_no: String,
    val class_section: String,
    val gender: String,
    val father_name: String,
    val marked: String,
    val stats: String,
    val profile_pic: String?
)

fun StudentMember.toEntity(institutionId: Int): StudentEntity {
    val studentId = when (id) {
        is Double -> id.toInt()
        is Int -> id
        is String -> id.toIntOrNull() ?: 0
        else -> 0
    }
    return StudentEntity(
        id = studentId,
        institutionId = institutionId,
        name = name,
        username = username,
        initials = initials,
        class_no = class_no,
        class_section = class_section,
        gender = gender,
        father_name = father_name,
        marked = marked,
        stats = stats,
        profile_pic = profile_pic
    )
}

fun StudentEntity.toDomain(): StudentMember {
    return StudentMember(
        id = id,
        name = name,
        username = username,
        initials = initials,
        class_no = class_no,
        class_section = class_section,
        gender = gender,
        father_name = father_name,
        marked = marked,
        stats = stats,
        profile_pic = profile_pic
    )
}
