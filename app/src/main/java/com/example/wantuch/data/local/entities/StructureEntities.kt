package com.example.wantuch.data.local.entities

import androidx.room.*

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey val id: Int,
    val institutionId: Int,
    val name: String
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: Int,
    val classId: Int,
    val name: String
)

fun com.example.wantuch.domain.model.SchoolClass.toEntity(institutionId: Int): ClassEntity {
    return ClassEntity(id = id, institutionId = institutionId, name = name)
}

fun com.example.wantuch.domain.model.SchoolSection.toEntity(classId: Int): SectionEntity {
    return SectionEntity(id = id, classId = classId, name = name)
}

fun ClassEntity.toDomain(sections: List<SectionEntity>): com.example.wantuch.domain.model.SchoolClass {
    return com.example.wantuch.domain.model.SchoolClass(
        id = id,
        name = name,
        sections = sections.map { com.example.wantuch.domain.model.SchoolSection(it.id, it.name) }
    )
}
