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
    val stats: String = "",
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
    val id: Any,
    val name: String,
    val username: String,
    val initials: String,
    val class_no: String,
    val class_section: String,
    val gender: String,
    val father_name: String,
    val marked: String,
    val stats: String = "",
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
    val message: String? = null,
    val file_path: String? = null,
    val paper_id: Int? = null
)

// ── Question Papers ──────────────────────────────────────────────────────────

data class QuestionPaper(
    val id: Int,
    val title: String,
    val subject: String? = null,
    val class_name: String? = null,
    val year: String? = null,
    val uploaded_by: String? = null,
    val file_path: String? = null,
    val created_at: String? = null,
    val total_marks: String? = null,
    val paper_type: String? = null
)

data class QuestionPaperResponse(
    val status: String,
    val message: String? = null,
    val papers: List<QuestionPaper>? = null,
    val stats: Map<String, Int>? = null,
    val classes: List<SchoolClass>? = null,
    val subjects: List<String>? = null,
    val years: List<String>? = null
)

data class AttendanceSubmissionItem(
    val class_id: Int,
    val class_name: String,
    val section_id: Int,
    val section_name: String,
    val submitted: Boolean,
    val submitted_at: String?,
    val teacher_name: String?
)

data class AttendanceSubmissionResponse(
    val status: String,
    val date: String,
    val items: List<AttendanceSubmissionItem>? = null,
    val message: String? = null
)

data class SmartStatusResponse(
    val status: String,
    val is_active: Boolean,
    val hardware: String,
    val ip: String,
    val engine: String,
    val confidence: String,
    val uptime: String
)

data class FaceSession(
    val id: Int,
    val name: String,
    val time: String,
    val type: String,
    val confidence: String
)

data class FaceSessionResponse(
    val status: String,
    val sessions: List<FaceSession>? = null
)

data class VerifyFaceResponse(
    val status: String,
    val matched: Boolean,
    val name: String? = null,
    val user_id: Int? = null,
    val role: String? = null,
    val confidence: String? = null,
    val message: String? = null
)

data class SyllabusTopic(
    val id: Int,
    val title: String,
    val status: String, // "Completed", "Pending", etc.
    val target_date: String? = null,
    val completion_date: String? = null,
    val duration: Int? = null,
    val description: String? = null
)

data class SyllabusChapter(
    val id: Int,
    val title: String,
    val start_date: String? = null,
    val end_date: String? = null,
    val status: String? = null,
    val percentage: Float = 0f,
    val topics: List<SyllabusTopic> = emptyList()
)

data class SyllabusItem(
    val id: Int,
    val class_id: Int,
    val section_id: Int,
    val subject_id: Int,
    val class_name: String,
    val section_name: String,
    val subject_name: String,
    val modules_done: Int,
    val modules_remaining: Int,
    val total_modules: Int,
    val percentage: Float,
    val chapters: List<SyllabusChapter> = emptyList()
)

data class SyllabusResponse(
    val status: String,
    val items: List<SyllabusItem>? = null,
    val message: String? = null
)

// ── Assignments (Homework) ───────────────────────────────────────────────────

data class Assignment(
    val id: Int,
    val title: String,
    val description: String? = null,
    val due_date: String,
    val subject_id: Int,
    val subname: String? = null,
    val class_id: Int,
    val cname: String? = null,
    val section_id: Int,
    val secname: String? = null,
    val teacher_id: Int,
    val teacher_name: String? = null,
    val attachment: String? = null,
    val created_at: String? = null,
    val submissions_count: Int = 0,
    // Student specific
    val submission_status: String? = "Pending",
    val submission_feedback: String? = null,
    val submitted_at: String? = null
)

data class AssignmentSubmission(
    val id: Int,
    val assignment_id: Int,
    val student_id: Int,
    val student_name: String? = null,
    val content: String? = null,
    val attachment: String? = null,
    val status: String, // Submitted, Approved, Rejected
    val feedback: String? = null,
    val submitted_at: String? = null,
    val asn_title: String? = null,
    val subname: String? = null,
    val class_name: String? = null,
    val section_name: String? = null,
    val roll_no: String? = null,
    val class_no: String? = null
)

data class AssignmentResponse(
    val status: String,
    val history: List<Assignment>? = null,
    val inbox: List<AssignmentSubmission>? = null,
    val is_admin: Boolean = false,
    val message: String? = null,
    val data: List<AssignmentSubmission>? = null // For GET_SUBMISSION_DETAILS
)

// Syllabus Wizard Models
data class SyllabusWizardPayload(
    val institution_id: Int,
    val class_id: Int,
    val section_id: Int,
    val subject_id: Int,
    val session_start: String,
    val session_end: String,
    val skip_sundays: Boolean,
    val skip_fridays: Boolean,
    val skip_holidays: Boolean,
    val leaves: List<SyllabusHoliday>,
    val chapters: List<SyllabusWizardChapter>
)

data class SyllabusHoliday(
    val name: String,
    val start: String,
    val end: String
)

data class SyllabusWizardChapter(
    val name: String,
    val topics: List<String>
)

data class EditChapterPayload(
    val chapter_id: Int,
    val chapter_name: String,
    val new_end_date: String,
    val topics: List<String>
)

data class PromotionStudent(
    val student_id: Int,
    val full_name: String,
    val roll_number: String?,
    val class_no: String?,
    val eligible: Boolean,
    val reason: String,
    val details: Map<String, Any>? = null
)

data class PromotionExamInfo(
    val type: String?,
    val year: String?
)

data class PromotionResponse(
    val status: String,
    val message: String? = null,
    val students: List<PromotionStudent>? = null,
    val exam_info: PromotionExamInfo? = null,
    val count: Int? = null
)

// ── Study Planner ────────────────────────────────────────────────────────────

data class StudySubject(
    val id: Int,
    val name: String
)

data class StudyPlanItem(
    val id: Int? = null,
    val subject_id: Int,
    val subject_name: String? = null,
    val chapter_name: String? = null,
    val topic_name: String? = null,
    val short_qs: String? = "0",
    val long_qs: String? = "0",
    val numericals: String? = "0",
    val status: String? = "Pending",
    val tasks_decoded: Map<String, String>? = null
)

data class StudyPlanResponse(
    val status: String,
    val data: List<StudyPlanItem>? = null,
    val message: String? = null
)

data class StudySubjectResponse(
    val status: String,
    val data: List<StudySubject>? = null,
    val message: String? = null
)
