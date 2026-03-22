package com.example.wantuch.data.local.entities

import androidx.room.*
import com.example.wantuch.domain.model.*

@Entity(tableName = "institutions")
data class InstitutionEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val type: String?,
    val logo: String?
)

@Entity(tableName = "dashboards")
data class DashboardEntity(
    @PrimaryKey val institutionId: Int,
    val institutionName: String,
    val fullName: String?,
    val userId: Int?,
    val role: String,
    val stats: Map<String, Any?>?,
    val isHoliday: Boolean,
    val modules: List<ModuleItem>?
)

@Entity(tableName = "portfolio")
data class PortfolioEntity(
    @PrimaryKey val id: Int = 1, // Single entry representing current user portfolio
    val institutions: List<InstitutionEntity>,
    val stats: Map<String, Any?>?
)

// Mappers
fun Institution.toEntity(): InstitutionEntity {
    val numericId = when (val originalId = id) {
        is Int -> originalId
        is String -> originalId.toIntOrNull() ?: 0
        is Double -> originalId.toInt()
        else -> 0
    }
    return InstitutionEntity(
        id = numericId,
        name = name,
        type = type,
        logo = logo
    )
}

fun InstitutionEntity.toDomain() = Institution(
    id = id,
    name = name,
    type = type,
    logo = logo
)

fun DashboardResponse.toEntity(instId: Int) = DashboardEntity(
    institutionId = instId,
    institutionName = institution_name,
    fullName = full_name,
    userId = user_id,
    role = role,
    stats = stats,
    isHoliday = is_holiday,
    modules = modules
)

fun DashboardEntity.toDomain() = DashboardResponse(
    status = "success",
    institution_name = institutionName,
    full_name = fullName,
    user_id = userId,
    role = role,
    stats = stats,
    is_holiday = isHoliday,
    modules = modules
)

fun PortfolioResponse.toEntity() = PortfolioEntity(
    id = 1,
    institutions = institutions?.map { it.toEntity() } ?: emptyList(),
    stats = stats
)

fun PortfolioEntity.toDomain() = PortfolioResponse(
    status = "success",
    stats = stats,
    institutions = institutions.map { it.toDomain() }
)
