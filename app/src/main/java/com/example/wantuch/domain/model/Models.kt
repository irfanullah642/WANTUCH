package com.example.wantuch.domain.model

data class Institution(
    val id: Any, // Changed to Any to handle Int or String from JSON
    val name: String,
    val type: String? = null,
    val logo: String? = null
)

data class PortfolioResponse(
    val status: String,
    val stats: Map<String, Any?>? = null,
    val institutions: List<Institution>? = null
)

data class DashboardResponse(
    val status: String,
    val institution_name: String,
    val full_name: String? = null,
    val user_id: Int? = null,
    val role: String,
    val stats: Map<String, Any?>? = null,
    val is_holiday: Boolean,
    val modules: List<ModuleItem>? = null
)

data class ModuleItem(
    val id: String,
    val label: String,
    val icon: String
)

data class LoginResponse(
    val status: String,
    val message: String? = null,
    val redirect: String? = null
)

data class UnreadCounts(
    val my_profile: Int = 0,
    val freelancer: Int = 0,
    val transport: Int = 0,
    val transfer_money: Int = 0,
    val bills: Int = 0,
    val medical: Int = 0,
    val rentals: Int = 0,
    val marketplace: Int = 0,
    val schools: Int = 0,
    val colleges: Int = 0,
    val universities: Int = 0,
    val madrasa: Int = 0,
    val wallet: Int = 0,
    val jobs: Int = 0
)

data class AppItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: androidx.compose.ui.graphics.Color,
    val unread: Int = 0,
    val type: String = "gen"
)

data class StaffMember(
    val id: Any,
    val name: String,
    val initials: String,
    val role: String,
    val bps: String,
    val marked: String,
    val paid: Any?,
    val balance: Any?,
    val profile_pic: String? = null
)

data class StaffResponse(
    val status: String,
    val message: String? = null,
    val stats: Map<String, Int>? = null,
    val teaching_staff: List<StaffMember>? = null,
    val non_teaching_staff: List<StaffMember>? = null
)

data class StudentMember(
    val id: Int,
    val name: String,
    val username: String,
    val initials: String,
    val class_no: String,
    val class_section: String,
    val gender: String,
    val father_name: String,
    val marked: String,
    val profile_pic: String? = null
)

data class StudentResponse(
    val status: String,
    val message: String? = null,
    val stats: Map<String, Int>? = null,
    val students: List<StudentMember>? = null
)

data class StudentProfileResponse(
    val status: String,
    val message: String? = null,
    val basic: Map<String, Any?>? = null,
    val stats: Map<String, Any?>? = null
)

data class SchoolStructureResponse(
    val status: String,
    val classes: List<SchoolClass>? = null
)

data class SchoolClass(
    val id: Int,
    val name: String,
    val sections: List<SchoolSection>? = null
)

data class SchoolSection(
    val id: Int,
    val name: String
)

data class StaffProfileResponse(
    val status: String = "",
    val message: String? = null,
    val basic: Map<String, Any?>? = null,
    val stats: Map<String, Any?>? = null,
    val contacts: List<Map<String, Any?>>? = null,
    val academics: List<Map<String, Any?>>? = null,
    val experience: List<Map<String, Any?>>? = null,
    val bank: List<Map<String, Any?>>? = null,
    val institution: Map<String, Any?>? = null,
    val inst_posts: List<Map<String, Any?>>? = null,
    val inst_bank: List<Map<String, Any?>>? = null,
    val inst_assets: List<Map<String, Any?>>? = null,
    val inst_funds: List<Map<String, Any?>>? = null,
    val inst_timetable: List<Map<String, Any?>>? = null
)

data class BasicResponse(
    val status: String,
    val message: String? = null
)
