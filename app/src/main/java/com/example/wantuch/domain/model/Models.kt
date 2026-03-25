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
    val institution_logo: String? = null,
    val full_name: String? = null,
    val profile_pic: String? = null,
    val user_id: Int? = null,
    val role: String,
    val stats: Map<String, Any?>? = null,
    val is_holiday: Boolean,
    val holiday_name: String? = null,
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
    val paper_id: Int? = null,
    val card_path: String? = null,
    val filename: String? = null
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

// ── Exams / Results / Analytics (from WANTUCH-2) ──────────────────────────

data class ExamL1Item(
    val exam_type: String,
    val semester: String?,
    val academic_year: String,
    val exam_count: Int
)

data class ExamL2Item(
    val class_id: Int,
    val section_id: Int,
    val class_name: String,
    val section_name: String,
    val subject_count: Int
)

data class ExamHierarchyResponse(
    val status: String,
    val message: String? = null,
    val data: List<ExamL1Item> = emptyList()
)

data class ExamL2HierarchyResponse(
    val status: String,
    val message: String? = null,
    val data: List<ExamL2Item> = emptyList()
)

data class RollNoSlipResponse(
    val status: String,
    val message: String? = null,
    val schedule: List<RollNoSchedule> = emptyList(),
    val students: List<RollNoStudent> = emptyList()
)

data class RollNoSchedule(
    val subject_id: String? = null,
    val exam_date: String,
    val start_time: String,
    val end_time: String? = null,
    val total_marks: String? = null,
    val sub_name: String
)

data class RollNoStudent(
    val id: String,
    val full_name: String,
    val username: String,
    val profile_pic: String? = null,
    val cname: String,
    val sname: String,
    val has_dues: Boolean = false,
    val balance: Double = 0.0
)

data class AwardListExam(
    val id: String,
    val exam_type: String,
    val subject_name: String
)

data class AwardListExamsResponse(
    val status: String,
    val message: String? = null,
    val exams: List<AwardListExam> = emptyList()
)

data class AwardListStudent(
    val student_id: String,
    val full_name: String?,
    val roll_number: String?,
    val marks: String? = null
)

data class AwardListStudentsResponse(
    val status: String,
    val message: String? = null,
    val total_marks: String? = null,
    val students: List<AwardListStudent> = emptyList()
)

data class ConsolidatedResultResponse(
    val status: String,
    val message: String? = null,
    val data: List<ConsolidatedStudentResult> = emptyList()
)

data class ConsolidatedStudentResult(
    val student_id: String,
    val roll_number: String?,
    val class_no: String?,
    val full_name: String?,
    val father_name: String?,
    val profile_pic: String?,
    val subjects: List<ConsolidatedSubject> = emptyList(),
    val total_max: Double = 0.0,
    val total_obtain: Any? = null,
    val percentage: Double = 0.0,
    val final_status: String?,
    val failed_papers: Any? = null
)

data class ConsolidatedSubject(
    val exam_id: String,
    val subject: String,
    val total: Double,
    val obtained: Any?,
    val pass_marks: Double,
    val status: String
)

data class FullResultCardResponse(
    val status: String,
    val message: String? = null,
    val data: List<FullResultStudent> = emptyList(),
    val is_bulk: Boolean = false
)

data class FullResultStudent(
    val student_id: String,
    val roll_number: String?,
    val class_no: String?,
    val full_name: String?,
    val father_name: String?,
    val profile_pic: String?,
    val report: List<FullResultReportGroup> = emptyList()
)

data class FullResultReportGroup(
    val exam_type: String,
    val subjects: List<FullResultSubject> = emptyList(),
    val total_max: Double = 0.0,
    val total_obtain: Any? = null,
    val percentage: Any? = null
)

data class FullResultSubject(
    val exam_id: String,
    val subject: String,
    val total: Double,
    val obtained: Any?
)

data class TopperAnalyticsResponse(
    val status: String,
    val data: List<TopperStats> = emptyList(),
    val message: String? = null
)

data class TopperStats(
    val full_name: String,
    val class_name: String? = null,
    val roll_number: String? = null,
    val class_no: String? = null,
    val total_obtained: String? = null,
    val total_max: String? = null,
    val percentage: String? = null
)

data class SchoolAnalyticsResponse(
    val status: String,
    val data: SchoolStats? = null,
    val message: String? = null
)

data class SchoolStats(
    val total_students: String? = null,
    val passed_entries: String? = null,
    val failed_entries: String? = null,
    val pass_rate: Double = 0.0
)

data class StaffAnalyticsResponse(
    val status: String,
    val data: List<StaffStats> = emptyList(),
    val message: String? = null
)

data class StaffStats(
    val class_name: String,
    val total_students: String? = null,
    val passed_entries: String? = null,
    val failed_entries: String? = null,
    val pass_rate: Double = 0.0
)

data class TrendAnalyticsResponse(
    val status: String,
    val data: List<TrendStats> = emptyList(),
    val message: String? = null
)

data class TrendStats(
    val exam_type: String,
    val subject_performance: List<TrendSubject> = emptyList()
)

data class TrendSubject(
    val subject: String,
    val average: Double = 0.0,
    val total: String? = null
)

data class HallStaff(
    val id: String,
    val full_name: String
)

data class HallSubject(
    val id: String,
    val name: String
)

data class SchoolSubject(
    val id: Int,
    val name: String,
    val type: String
)

data class SubjectResponse(
    val status: String,
    val message: String? = null,
    val subjects: List<SchoolSubject> = emptyList()
)

// ── Timetable & Substitution (from WANTUCH-3) ───────────────────────────────

data class TimetableItem(
    val class_id: Int,
    val section_id: Int,
    val class_name: String? = null,
    val section_name: String? = null,
    val sub_name: String? = null,
    val cur_subject: String? = null,
    val cur_teacher: String? = null,
    val cur_start_time: String? = null,
    val cur_end_time: String? = null,
    val cur_type: String? = null,
    val teacher_name: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val live_status: String? = null,
    val sub_teacher: String? = null,
    val activity_type: String? = null,
    val pno: Int? = null,
    val cid: Int? = null,
    val sid: Int? = null
)

data class TimetableResponse(
    val status: String,
    val mode: String? = null,
    val items: List<TimetableItem>? = null
)

data class TimetableMetadataResponse(
    val status: String,
    val classes: List<SchoolClass>? = null,
    val days: List<String>? = null
)

data class AbsentStaff(
    val id: Int,
    val full_name: String,
    val status: String,
    val periods: List<AbsentPeriod>? = null
)

data class AbsentPeriod(
    val id: Int,
    val period_number: Int,
    val start_time: String,
    val end_time: String,
    val sub_name: String,
    val class_name: String,
    val sec_name: String,
    val sub_sid: Int? = null,
    val is_paid: Int? = null,
    val sub_status: String? = null,
    val sub_name_ext: String? = null,
    val available_staff: List<AvailableStaff>? = null
)

data class AvailableStaff(
    val id: Int,
    val full_name: String
)

data class SubstitutionResponse(
    val status: String,
    val absent_staff: List<AbsentStaff>? = null
)

data class ManagementClass(
    val cid: Int,
    val cname: String,
    val sid: Int,
    val sname: String,
    val slot_count: Int
)

data class ManagementGroup(
    val id: Int,
    val setting_value: String
)

data class TimetableManagementSummary(
    val status: String,
    val versions: List<TimetableManagementVersion>? = null,
    val groups: List<ManagementGroup>? = null,
    val classes: List<ManagementClass>? = null
)

data class TimetableManagementVersion(
    val id: Int,
    val label: String
)

data class ArchiveItem(
    val id: Int? = null,
    val class_id: Int? = null,
    val section_id: Int? = null,
    val staff_id: Int? = null,
    val subject_id: Int? = null,
    val day_of_week: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val activity_type: String? = null,
    val timetable_version: Int? = null,
    val sub_name: String? = null,
    val teacher_name: String? = null,
    val class_name: String? = null,
    val section_name: String? = null
)

data class TimetableArchiveResponse(
    val status: String,
    val items: List<ArchiveItem>? = null
)

data class WizardStaff(val id: Int, val name: String)
data class WizardSubject(val id: Int, val name: String)
data class WizardPreRequisites(
    val status: String,
    val staff: List<WizardStaff>? = null,
    val subjects: List<WizardSubject>? = null
)

// ADM / WDL Module ─────────────────────────────────────────────────────────────
data class AdmWdlRecord(
    val id: Any? = null,
    val adm_no: String? = null,
    val name: String? = null,
    val full_name: String? = null,
    val father_name: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val parent_cnic_no: String? = null,
    val class_name: String? = null,
    val section_name: String? = null,
    val class_admission: String? = null,
    val class_withdrawal: String? = null,
    val admission_date: String? = null,
    val date_admission: String? = null,
    val date_withdrawal: String? = null,
    val roll_number: String? = null,
    val enrollment_status: String? = null,
    val slc_status: String? = null,
    val academic_year: String? = null
) {
    val displayName: String get() = full_name ?: name ?: "N/A"
    val displayClass: String get() = class_name ?: class_admission ?: "N/A"
    val displayAdmDate: String get() = admission_date ?: date_admission ?: ""
    val idInt: Int get() = id?.toString()?.toDoubleOrNull()?.toInt() ?: 0
}

data class AdmWdlResponse(
    val status: String,
    val message: String? = null,
    val data: List<AdmWdlRecord>? = null,
    val count: Int? = null
)
