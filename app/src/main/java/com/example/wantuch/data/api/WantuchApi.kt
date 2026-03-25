package com.example.wantuch.data.api
import com.example.wantuch.domain.model.*
import retrofit2.http.*

interface WantuchApi {
    @GET("modules/education/api_get_institutions.php")
    suspend fun getInstitutions(@Query("type") type: String): List<Institution>

    @FormUrlEncoded
    @POST("modules/education/auth_institution.php")
    suspend fun authInstitution(
        @Field("INSTITUTION_ID") instId: Int,
        @Field("USERNAME") user: String,
        @Field("PASSWORD") pass: String,
        @Field("ROLE") role: String,
        @Field("ACTION") action: String
    ): LoginResponse

    @FormUrlEncoded
    @POST("modules/education/auth_institution.php")
    suspend fun authParent(
        @Field("CNIC") cnic: String,
        @Field("PASSWORD") pass: String,
        @Field("ACTION") action: String // Always "PARENT_LOGIN"
    ): LoginResponse

    @FormUrlEncoded
    @POST("auth/ajax_login.php")
    suspend fun generalLogin(
        @Field("login_input") input: String,
        @Field("password") pass: String
    ): LoginResponse

    @GET("api/get_unread_counts.php")
    suspend fun getUnreadCounts(): Map<String, Int>

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getPortfolio(@Query("action") action: String = "GET_PORTFOLIO"): PortfolioResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun switchAndGetDashboard(
        @Query("action") action: String = "SWITCH_AND_GET_DASHBOARD",
        @Query("institution_id") instId: Int
    ): DashboardResponse
    
    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun genericGet(
        @Query("action") action: String,
        @QueryMap params: Map<String, String>
    ): okhttp3.ResponseBody

    @FormUrlEncoded
    @POST("modules/education/fee_api.php")
    suspend fun feeApiPost(
        @Field("action") action: String,
        @FieldMap params: Map<String, String>
    ): okhttp3.ResponseBody

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_STAFF")
    suspend fun saveStaff(
        @FieldMap fields: Map<String, String>
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun bulkSaveStaff(
        @Field("action") action: String = "BULK_SAVE_STAFF",
        @Field("institution_id") instId: Int,
        @Field("names") names: String,
        @Field("role") role: String,
        @Field("gender") gender: String,
        @Field("user_type") userType: String
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=DELETE_STAFF")
    suspend fun deleteStaff(
        @Field("staff_id") staffId: Int,
        @Field("institution_id") instId: Int
    ): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getStaff(
        @Query("action") action: String = "GET_STAFF",
        @Query("institution_id") instId: Int
    ): StaffResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getStaffProfile(
        @Query("action") action: String = "GET_STAFF_PROFILE",
        @Query("staff_id") staffId: Int,
        @Query("institution_id") instId: Int
    ): StaffProfileResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=UPDATE_STAFF_PROFILE")
    suspend fun updateStaffProfile(
        @FieldMap fields: Map<String, String>
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=UPDATE_INSTITUTION_PROFILE")
    suspend fun updateInstitutionProfile(
        @FieldMap fields: Map<String, String>
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_CONTACT")
    suspend fun saveContact(@FieldMap fields: Map<String, String>): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=DELETE_CONTACT")
    suspend fun deleteContact(@Field("id") id: Int, @Field("staff_id") staffId: Int): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_ACADEMIC")
    suspend fun saveAcademic(@FieldMap fields: Map<String, String>): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=DELETE_ACADEMIC")
    suspend fun deleteAcademic(@Field("id") id: Int, @Field("staff_id") staffId: Int): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_EXPERIENCE")
    suspend fun saveExperience(@FieldMap fields: Map<String, String>): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=DELETE_EXPERIENCE")
    suspend fun deleteExperience(@Field("id") id: Int, @Field("staff_id") staffId: Int): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_BANK")
    suspend fun saveBank(@FieldMap fields: Map<String, String>): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=DELETE_BANK")
    suspend fun deleteBank(@Field("id") id: Int, @Field("staff_id") staffId: Int): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_INST_POST")
    suspend fun saveInstPost(@FieldMap fields: Map<String, String>): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=DELETE_INST_POST")
    suspend fun deleteInstPost(@Field("id") id: Int, @Field("institution_id") instId: Int): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_INST_BANK")
    suspend fun saveInstBank(@FieldMap fields: Map<String, String>): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=DELETE_INST_BANK")
    suspend fun deleteInstBank(@Field("id") id: Int, @Field("institution_id") instId: Int): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_INST_ASSET")
    suspend fun saveInstAsset(@FieldMap fields: Map<String, String>): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=DELETE_INST_ASSET")
    suspend fun deleteInstAsset(@Field("id") id: Int, @Field("institution_id") instId: Int): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_INST_FUND")
    suspend fun saveInstFund(@FieldMap fields: Map<String, String>): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=DELETE_INST_FUND")
    suspend fun deleteInstFund(@Field("id") id: Int, @Field("institution_id") instId: Int): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getStudents(
        @Query("action") action: String = "GET_STUDENTS",
        @Query("institution_id") instId: Int,
        @Query("class_id") classId: Int = 0,
        @Query("section_id") sectionId: Int = 0,
        @Query("status") status: String = "active",
        @Query("year") year: String = ""
    ): StudentResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getStudentProfile(
        @Query("action") action: String = "GET_STUDENT_PROFILE",
        @Query("student_id") studentId: Int,
        @Query("institution_id") instId: Int
    ): StudentProfileResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getStructure(
        @Query("action") action: String = "GET_STRUCTURE",
        @Query("institution_id") instId: Int
    ): SchoolStructureResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=DELETE_STUDENT")
    suspend fun deleteStudent(
        @Field("student_id") studentId: Int,
        @Field("institution_id") instId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveStudent(
        @Field("action") action: String = "SAVE_STUDENT",
        @FieldMap fields: Map<String, String>,
        @Field("institution_id") institutionId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun bulkSaveStudents(
        @Field("action") action: String = "BULK_SAVE_STUDENTS",
        @Field("institution_id") institutionId: Int,
        @Field("class_id") classId: Int,
        @Field("section_id") sectionId: Int,
        @Field("names_text") namesText: String,
        @Field("gender") gender: String = "Male"
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun markAttendance(
        @Field("action") action: String = "MARK_ATTENDANCE",
        @Field("student_id") studentId: Int,
        @Field("status") status: String,
        @Field("institution_id") institutionId: Int,
        @Field("date") date: String = ""
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun collectFee(
        @Field("action") action: String = "COLLECT_FEE",
        @Field("student_id") studentId: Int,
        @Field("amount") amount: Double,
        @Field("mode") mode: String,
        @Field("category") category: String,
        @Field("month") month: String,
        @Field("year") year: String,
        @Field("institution_id") institutionId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun updateRole(
        @Field("action") action: String = "UPDATE_STUDENT_ROLE",
        @Field("student_id") studentId: Int,
        @Field("role") role: String,
        @Field("institution_id") institutionId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun changeStatus(
        @Field("action") action: String = "CHANGE_STUDENT_STATUS",
        @Field("student_id") studentId: Int,
        @Field("status") status: String,
        @Field("institution_id") institutionId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun markStaffAttendance(
        @Field("action") action: String = "MARK_STAFF_ATTENDANCE",
        @Field("staff_id") staffId: Int,
        @Field("status") status: String,
        @Field("date") date: String,
        @Field("institution_id") institutionId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun recordSalary(
        @Field("action") action: String = "RECORD_SALARY",
        @Field("staff_id") staffId: Int,
        @Field("amount") amount: Double,
        @Field("month") month: String,
        @Field("year") year: String,
        @Field("mode") mode: String,
        @Field("institution_id") institutionId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun updateStaffStatus(
        @Field("action") action: String = "UPDATE_STAFF_STATUS",
        @Field("staff_id") staffId: Int,
        @Field("status") status: String
    ): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getAttendanceSubmissionStatus(
        @Query("action") action: String = "GET_ATTENDANCE_SUBMISSION_STATUS",
        @Query("institution_id") instId: Int,
        @Query("date") date: String
    ): AttendanceSubmissionResponse

    @POST("modules/education/api_mobile_dashboard.php?action=MARK_ATTENDANCE_SUBMITTED")
    suspend fun markAttendanceSubmitted(
        @Body body: Map<String, String>
    ): AttendanceSubmissionResponse

    @POST("modules/education/api_mobile_dashboard.php?action=CLEAR_ATTENDANCE_SUBMISSION")
    suspend fun clearAttendanceSubmission(
        @Body body: Map<String, String>
    ): AttendanceSubmissionResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun downloadAttendanceCsv(
        @Query("action") action: String = "EXPORT_ATTENDANCE_HORIZONTAL_CSV",
        @Query("institution_id") instId: Int,
        @Query("date_from") dateFrom: String,
        @Query("date_to") dateTo: String,
        @Query("class_selection") classSelection: String = "all"
    ): okhttp3.ResponseBody

    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_BULK_IMPORT_JSON")
    suspend fun saveBulkImportJson(
        @Body body: okhttp3.RequestBody
    ): okhttp3.ResponseBody

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getSmartStatus(
        @Query("action") action: String = "GET_SMART_STATUS",
        @Query("institution_id") instId: Int
    ): SmartStatusResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getFaceSessions(
        @Query("action") action: String = "GET_FACE_SESSIONS",
        @Query("institution_id") instId: Int,
        @Query("limit") limit: Int = 10
    ): FaceSessionResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun enrollFace(
        @Field("action") action: String = "ENROLL_FACE",
        @Field("inst_id") institutionId: Int,
        @Field("user_id") userId: Int,
        @Field("category") category: String,
        @Field("photo_base64") photoBase64: String
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun verifyFace(
        @Field("action") action: String = "VERIFY_FACE",
        @Field("inst_id") institutionId: Int,
        @Field("photo_base64") photoBase64: String
    ): VerifyFaceResponse
    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getQuestionPapers(
        @Query("action") action: String = "GET_QUESTION_PAPERS",
        @Query("institution_id") instId: Int,
        @Query("class_id") classId: Int = 0,
        @Query("subject") subject: String = "",
        @Query("year") year: String = "",
        @Query("paper_type") paperType: String = ""
    ): QuestionPaperResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveQuestionPaper(
        @Field("action") action: String = "SAVE_QUESTION_PAPER",
        @Field("institution_id") instId: Int,
        @Field("title") title: String,
        @Field("subject") subject: String,
        @Field("class_id") classId: Int,
        @Field("year") year: String,
        @Field("total_marks") totalMarks: String,
        @Field("paper_type") paperType: String
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun deleteQuestionPaper(
        @Field("action") action: String = "DELETE_QUESTION_PAPER",
        @Field("paper_id") paperId: Int,
        @Field("institution_id") instId: Int
    ): BasicResponse

    // Smart Paper Builder → PDF generation endpoint
    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveSmartPaper(
        @Field("action") action: String = "SAVE_QUESTION_PAPER",
        @Field("institution_id") instId: Int,
        @Field("title") title: String,
        @Field("subject") subject: String,
        @Field("total_marks") totalMarks: String,
        @Field("sections_data") sectionsData: String
    ): BasicResponse
    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getSmartConfig(
        @Query("action") action: String = "GET_SMART_CONFIG",
        @Query("institution_id") instId: Int
    ): okhttp3.ResponseBody

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveSmartConfig(
        @Field("action") action: String = "SAVE_SMART_CONFIG",
        @Field("institution_id") instId: Int,
        @Field("vision_engine") vEng: Int,
        @Field("pulse_engine") pEng: Int,
        @Field("smart_alerts") sAlt: Int,
        @Field("ip_stream_address") ip: String,
        @Field("auth_user") user: String,
        @Field("auth_pass") pass: String,
        @Field("attendance_window") window: String,
        @Field("check_in_time_start") start: String,
        @Field("check_in_time_end") end: String
    ): BasicResponse
    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getMonthlyStudentLedger(
        @Query("action") action: String = "GET_MONTHLY_STUDENT_LEDGER",
        @Query("institution_id") instId: Int,
        @Query("class_id") classId: Int,
        @Query("month") month: String,
        @Query("year") year: Int
    ): okhttp3.ResponseBody

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getMonthlyStaffLedger(
        @Query("action") action: String = "GET_MONTHLY_STAFF_LEDGER",
        @Query("institution_id") instId: Int,
        @Query("month") month: String,
        @Query("year") year: Int
    ): okhttp3.ResponseBody

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getAttendanceRules(
        @Query("action") action: String = "GET_ATTENDANCE_RULES",
        @Query("institution_id") instId: Int
    ): okhttp3.ResponseBody

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveAttendanceRules(
        @Field("action") action: String = "SAVE_ATTENDANCE_RULES",
        @FieldMap fields: Map<String, String>,
        @Field("institution_id") institutionId: Int
    ): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getLeaveAppeals(
        @Query("action") action: String = "GET_LEAVE_APPEALS",
        @Query("institution_id") instId: Int
    ): okhttp3.ResponseBody

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun updateAppealStatus(
        @Field("action") action: String = "UPDATE_LEAVE_APPEAL",
        @Field("appeal_id") id: Int,
        @Field("status") status: String,
        @Field("institution_id") institutionId: Int
    ): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getSyllabus(
        @Query("action") action: String = "GET_SYLLABUS",
        @Query("institution_id") instId: Int,
        @Query("class_id") classId: Int = 0,
        @Query("section_id") sectionId: Int = 0,
        @Query("subject_id") subjectId: Int = 0
    ): SyllabusResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun updateSyllabusStatus(
        @Field("action") action: String = "UPDATE_SYLLABUS_STATUS",
        @Field("id") id: Int,
        @Field("status") status: String
    ): BasicResponse

    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveFullSyllabus(
        @Query("action") action: String = "SAVE_FULL_SYLLABUS",
        @Body payload: SyllabusWizardPayload
    ): BasicResponse

    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun editSyllabusChapter(
        @Query("action") action: String = "EDIT_SYLLABUS_CHAPTER",
        @Body payload: EditChapterPayload
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun deleteSyllabusTopic(
        @Field("action") action: String = "DELETE_SYLLABUS_TOPIC",
        @Field("id") id: Int,
        @Field("institution_id") institution_id: Int
    ): BasicResponse

    @Multipart
    @POST("modules/education/api_mobile_dashboard.php?action=IMPORT_DATABASE")
    suspend fun importDatabase(
        @Part file: okhttp3.MultipartBody.Part
    ): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php?action=GET_ASSIGNMENTS")
    suspend fun getAssignments(): AssignmentResponse

    @Multipart
    @POST("modules/education/api_mobile_dashboard.php?action=CREATE_ASSIGNMENT")
    suspend fun createAssignment(
        @Part("class_id") classId: okhttp3.RequestBody,
        @Part("section_id") sectionId: okhttp3.RequestBody,
        @Part("subject_id") subjectId: okhttp3.RequestBody,
        @Part("title") title: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part("due_date") dueDate: okhttp3.RequestBody,
        @Part attachment: okhttp3.MultipartBody.Part? = null
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=REVIEW_SUBMISSION")
    suspend fun reviewSubmission(
        @Field("submission_id") subId: Int,
        @Field("status") status: String,
        @Field("feedback") feedback: String
    ): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php?action=GET_SUBMISSION_DETAILS")
    suspend fun getSubmissionDetails(
        @Query("assignment_id") assignmentId: Int
    ): AssignmentResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=UPDATE_ASSIGNMENT")
    suspend fun updateAssignment(
        @Field("id") id: Int,
        @Field("title") title: String,
        @Field("description") description: String,
        @Field("due_date") dueDate: String
    ): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php?action=GET_STUDENTS_FOR_PROMOTION")
    suspend fun getStudentsForPromotion(
        @Query("institution_id") instId: Int,
        @Query("class_id") classId: Int,
        @Query("section_id") sectionId: Int,
        @Query("criteria") criteria: String,
        @Query("year") year: String
    ): PromotionResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=PROMOTE_STUDENTS")
    suspend fun promoteStudents(
        @Field("institution_id") instId: Int,
        @Field("promotions") promotions: String, // JSON array
        @Field("target_class_id") targetClassId: Int,
        @Field("source_class_id") sourceClassId: Int,
        @Field("source_section_id") sourceSectionId: Int,
        @Field("current_year") year: String
    ): PromotionResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SHIFT_STUDENTS")
    suspend fun shiftStudents(
        @Field("institution_id") instId: Int,
        @Field("student_ids") studentIds: String, // JSON array
        @Field("target_class_id") targetClassId: Int,
        @Field("source_class_id") sourceClassId: Int,
        @Field("source_section_id") sourceSectionId: Int,
        @Field("current_year") year: String
    ): PromotionResponse

    // ── Study Planner ────────────────────────────────────────────────────────────

    @GET("modules/education/api_mobile_dashboard.php?action=GET_SUBJECTS")
    suspend fun getStudySubjects(
        @Query("institution_id") instId: Int,
        @Query("class_id") classId: Int,
        @Query("section_id") sectionId: Int
    ): StudySubjectResponse

    @GET("modules/education/api_mobile_dashboard.php?action=GET_PLANNER_DATA")
    suspend fun getPlannerData(
        @Query("institution_id") instId: Int,
        @Query("class_id") classId: Int,
        @Query("section_id") sectionId: Int
    ): StudyPlanResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=SAVE_PLANNER_CONFIG")
    suspend fun savePlannerConfig(
        @Field("institution_id") instId: Int,
        @Field("class_id") classId: Int,
        @Field("section_id") sectionId: Int,
        @Field("config") config: String // JSON
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=COMPLETE_TOPIC")
    suspend fun completeTopic(
        @Field("institution_id") instId: Int,
        @Field("subject_id") subjectId: Int,
        @Field("topic_name") topicName: String
    ): BasicResponse

    // ── Exams / Results / Analytics (from WANTUCH-2) ─────────────────────────

    @FormUrlEncoded
    @POST("modules/education/exams_api.php")
    suspend fun addExammarks(
        @Field("action") action: String,
        @FieldMap fields: Map<String, String>
    ): BasicResponse

    @GET("modules/education/exams_api.php")
    suspend fun getAwardListExams(
        @Query("action") action: String = "get_award_list_exams",
        @Query("institution_id") instId: Int,
        @Query("class_id") classId: Int,
        @Query("section_id") sectionId: Int
    ): AwardListExamsResponse

    @GET("modules/education/exams_api.php")
    suspend fun getAwardListStudents(
        @Query("action") action: String = "get_award_list_students",
        @Query("institution_id") instId: Int,
        @Query("exam_id") examId: String,
        @Query("class_id") classId: Int,
        @Query("section_id") sectionId: Int
    ): AwardListStudentsResponse

    @FormUrlEncoded
    @POST("modules/education/exams_api.php")
    suspend fun saveAwardListMarks(
        @Field("action") action: String = "save_award_list_marks",
        @Field("institution_id") instId: Int,
        @Field("exam_id") examId: String,
        @Field("marks_json") marksJson: String
    ): BasicResponse

    @GET("modules/education/exams_api.php")
    suspend fun getExamHierarchyL1(
        @Query("action") action: String = "get_exam_hierarchy_l1",
        @Query("institution_id") instId: Int
    ): ExamHierarchyResponse

    @GET("modules/education/exams_api.php")
    suspend fun getExamHierarchyL2(
        @Query("action") action: String = "get_exam_hierarchy_l2",
        @Query("institution_id") instId: Int,
        @Query("type") type: String,
        @Query("semester") semester: String,
        @Query("year") year: String
    ): ExamL2HierarchyResponse

    @FormUrlEncoded
    @POST("modules/education/exams_api.php")
    suspend fun deleteExamGroupL1(
        @Field("action") action: String = "delete_exam_group_l1",
        @Field("institution_id") instId: Int,
        @Field("type") type: String,
        @Field("semester") semester: String,
        @Field("year") year: String
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/exams_api.php")
    suspend fun deleteExamGroupL2(
        @Field("action") action: String = "delete_exam_group_l2",
        @Field("institution_id") instId: Int,
        @Field("type") type: String,
        @Field("semester") semester: String,
        @Field("year") year: String,
        @Field("class_id") classId: Int,
        @Field("section_id") sectionId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php?action=GET_CONSOLIDATED_RESULT")
    suspend fun getConsolidatedResult(
        @Field("institution_id") instId: Int,
        @Field("class_id") classId: Int,
        @Field("section_id") sectionId: Int,
        @Field("exam_type") examType: String,
        @Field("year") year: String,
        @Field("roll") roll: String? = null,
        @Field("user_id") userId: Int? = null,
        @Field("role") role: String? = null
    ): ConsolidatedResultResponse

    @GET("modules/education/exams_api.php?action=get_full_result_card")
    suspend fun getFullResultCard(
        @Query("institution_id") instId: Int,
        @Query("class_id") classId: Int,
        @Query("section_id") sectionId: Int,
        @Query("year") year: String,
        @Query("roll") roll: String? = null,
        @Query("user_id") userId: Int? = null,
        @Query("role") role: String? = null
    ): FullResultCardResponse

    @GET("modules/education/exams_api.php?action=get_performance_analytics")
    suspend fun getTopperAnalytics(
        @Query("institution_id") instId: Int,
        @Query("type") type: String,
        @Query("class_id") classId: Int? = null,
        @Query("section_id") sectionId: Int? = null
    ): TopperAnalyticsResponse

    @GET("modules/education/exams_api.php?action=get_performance_analytics")
    suspend fun getSchoolAnalytics(
        @Query("institution_id") instId: Int,
        @Query("type") type: String = "school_performance",
        @Query("class_id") classId: Int? = null,
        @Query("section_id") sectionId: Int? = null
    ): SchoolAnalyticsResponse

    @GET("modules/education/exams_api.php?action=get_performance_analytics")
    suspend fun getStaffAnalytics(
        @Query("institution_id") instId: Int,
        @Query("type") type: String = "staff_performance",
        @Query("class_id") classId: Int? = null,
        @Query("section_id") sectionId: Int? = null
    ): StaffAnalyticsResponse

    @GET("modules/education/exams_api.php?action=get_performance_analytics")
    suspend fun getTrendAnalytics(
        @Query("institution_id") instId: Int,
        @Query("type") type: String = "trends",
        @Query("class_id") classId: Int? = null,
        @Query("section_id") sectionId: Int? = null
    ): TrendAnalyticsResponse

    @GET("modules/education/exams_api.php?action=get_staff_list")
    suspend fun getHallStaffList(@Query("institution_id") instId: Int): List<HallStaff>

    @GET("modules/education/exams_api.php?action=get_scheduled_subjects")
    suspend fun getHallSubjects(
        @Query("institution_id") instId: Int,
        @Query("date") date: String,
        @Query("shift") shift: String
    ): List<HallSubject>

    @FormUrlEncoded
    @POST("modules/education/exams_api.php?action=generate_hall_view")
    suspend fun generateHallView(
        @Field("institution_id") instId: Int,
        @Field("date") date: String,
        @Field("shift") shift: String,
        @Field("subject_id") subjectId: String,
        @Field("rooms") roomsJson: String,
        @Field("staff") staffJson: String
    ): okhttp3.ResponseBody

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun classAction(
        @Field("action") action: String,
        @Field("institution_id") instId: Int,
        @Field("id") id: Int? = null,
        @Field("class_id") classId: Int? = null,
        @Field("name") name: String? = null
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun subjectAction(
        @Field("action") action: String,
        @Field("institution_id") instId: Int,
        @Field("id") id: Int? = null,
        @Field("name") name: String? = null,
        @Field("type") type: String? = null
    ): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getSubjects(
        @Query("action") action: String = "GET_SUBJECTS",
        @Query("institution_id") instId: Int
    ): SubjectResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getNotices(
        @Query("action") action: String = "GET_NOTICES",
        @Query("institution_id") instId: Int
    ): NoticeResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveNotice(
        @Field("action") action: String = "SAVE_NOTICE",
        @FieldMap fields: Map<String, String>,
        @Field("institution_id") instId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun deleteNotice(
        @Field("action") action: String = "DELETE_NOTICE",
        @Field("id") id: Int,
        @Field("institution_id") instId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/exams_api.php")
    suspend fun createExam(
        @Field("action") action: String = "create_exam",
        @Field("institution_id") institutionId: Int,
        @Field("type") type: String,
        @Field("semester") semester: String,
        @Field("year") year: String,
        @Field("class_id") classId: Int,
        @Field("section_id") sectionId: Int,
        @Field("subjects_json") subjectsJson: String
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/exams_api.php")
    suspend fun quickScheduleExam(
        @Field("action") action: String = "quick_schedule_exam",
        @Field("type") type: String,
        @Field("year") year: String,
        @Field("text") text: String,
        @Field("institution_id") institutionId: Int
    ): BasicResponse

    @GET("modules/education/exams_api.php")
    suspend fun getRollNoSlips(
        @Query("action") action: String = "get_roll_no_slips",
        @Query("type") examType: String,
        @Query("class_id") classId: Int,
        @Query("institution_id") institutionId: Int
    ): RollNoSlipResponse

    @FormUrlEncoded
    @POST("modules/education/exams_api.php")
    suspend fun deleteStudentAwardMark(
        @Field("action") action: String = "delete_student_award_mark",
        @Field("institution_id") institutionId: Int,
        @Field("exam_id") examId: Int,
        @Field("student_id") studentId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/exams_api.php")
    suspend fun deleteFullAwardList(
        @Field("action") action: String = "delete_full_award_list",
        @Field("institution_id") institutionId: Int,
        @Field("exam_id") examId: Int
    ): BasicResponse

    // ── Timetable & Substitution (from WANTUCH-3) ───────────────────────────────
    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getTimetableMetadata(
        @Query("action") action: String = "GET_TIMETABLE_METADATA",
        @Query("institution_id") instId: Int
    ): TimetableMetadataResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getTimetable(
        @Query("action") action: String = "GET_TIMETABLE",
        @Query("institution_id") instId: Int,
        @Query("class_id") classId: Int = 0,
        @Query("section_id") sectionId: Int = 0,
        @Query("day_of_week") day: String = ""
    ): TimetableResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getSubstitutionData(
        @Query("action") action: String = "GET_SUBSTITUTION_DATA",
        @Query("institution_id") instId: Int,
        @Query("version") version: Int = 0
    ): SubstitutionResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun assignSubstitution(
        @Field("action") action: String = "ASSIGN_SUBSTITUTION",
        @Field("institution_id") instId: Int,
        @Field("timetable_id") ttId: Int,
        @Field("original_staff_id") origSid: Int,
        @Field("substitute_staff_id") subSid: Int,
        @Field("is_paid") isPaid: Int = 1
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun removeSubstitution(
        @Field("action") action: String = "REMOVE_SUBSTITUTION",
        @Field("institution_id") instId: Int,
        @Field("timetable_id") ttId: Int
    ): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getTimetableManagementSummary(
        @Query("action") action: String = "GET_TIMETABLE_MANAGEMENT_SUMMARY",
        @Query("institution_id") instId: Int
    ): TimetableManagementSummary

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getTimetableArchive(
        @Query("action") action: String = "GET_TIMETABLE_ARCHIVE",
        @Query("institution_id") instId: Int,
        @Query("version") version: Int = 0,
        @Query("level") level: String = "ALL",
        @Query("staff_id") staffId: Int = 0,
        @Query("class_id") classId: Int = 0,
        @Query("section_id") sectionId: Int = 0
    ): TimetableArchiveResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getWizardData(
        @Query("action") action: String = "GET_WIZARD_DATA",
        @Query("institution_id") instId: Int
    ): WizardPreRequisites

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveTimetableSlot(
        @Field("action") action: String = "SAVE_TIMETABLE_SLOT",
        @Field("institution_id") instId: Int,
        @Field("id") id: Int = 0,
        @Field("staff_id") staffId: Int,
        @Field("subject_id") subjectId: Int,
        @Field("class_id") classId: Int,
        @Field("section_id") sectionId: Int,
        @Field("day_of_week") day: String,
        @Field("start_time") startTime: String,
        @Field("end_time") endTime: String,
        @Field("period_number") periodNo: Int,
        @Field("timetable_version") version: Int = 1,
        @Field("activity_type") actType: String = "lesson"
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun deleteTimetableSlot(
        @Field("action") action: String = "DELETE_TIMETABLE_SLOT",
        @Field("institution_id") instId: Int,
        @Field("id") id: Int
    ): BasicResponse

    // ── ADM / WDL Module (from WANTUCH-3) ──────────────────────────────────────

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getAdmWdlFresh(
        @Query("action") action: String = "GET_ADM_WDL_FRESH",
        @Query("institution_id") instId: Int,
        @Query("q") q: String = "",
        @Query("class") cls: String = "",
        @Query("limit") limit: Int = 200
    ): AdmWdlResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun getAdmWdlOld(
        @Query("action") action: String = "GET_ADM_WDL_OLD",
        @Query("institution_id") instId: Int,
        @Query("q") q: String = "",
        @Query("limit") limit: Int = 200
    ): AdmWdlResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveAdmWdlFresh(
        @Field("action") action: String = "SAVE_ADM_WDL_FRESH",
        @Field("institution_id") instId: Int,
        @FieldMap fields: Map<String, String>
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveAdmWdlOld(
        @Field("action") action: String = "SAVE_ADM_WDL_OLD",
        @Field("institution_id") instId: Int,
        @FieldMap fields: Map<String, String>
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun withdrawStudent(
        @Field("action") action: String = "WITHDRAW_STUDENT_MOBILE",
        @Field("institution_id") instId: Int,
        @Field("student_id") studentId: Int,
        @Field("adm_no") admNo: String,
        @Field("with_date") withDate: String,
        @Field("with_class") withClass: String,
        @Field("slc_status") slcStatus: String
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun deleteAdmEntry(
        @Field("action") action: String = "DELETE_ADM_ENTRY",
        @Field("institution_id") instId: Int,
        @Field("id") id: Int,
        @Field("source") source: String
    ): BasicResponse

    @GET("modules/education/api_mobile_dashboard.php")
    suspend fun searchCertStudents(
        @Query("action") action: String = "SEARCH_CERT_STUDENTS",
        @Query("institution_id") instId: Int,
        @Query("q") q: String,
        @Query("limit") limit: Int = 50
    ): AdmWdlResponse

    // ── Smart ID Card & Profile Upload (from WANTUCH-4) ───────────────────────

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun uploadProfilePic(
        @Field("action") action: String = "UPLOAD_PROFILE_PIC",
        @Field("user_id") userId: Int,
        @Field("image") base64Image: String
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun generateIdCard(
        @Field("action") action: String = "GENERATE_ID_CARD",
        @Field("user_id") userId: Int,
        @Field("user_type") userType: String,
        @Field("institution_id") instId: Int
    ): BasicResponse

    @FormUrlEncoded
    @POST("modules/education/api_mobile_dashboard.php")
    suspend fun saveLeaveAppeal(
        @Field("action") action: String = "SAVE_LEAVE_APPEAL",
        @Field("institution_id") instId: Int,
        @Field("user_id") userId: Int,
        @Field("from_date") fromDate: String,
        @Field("to_date") toDate: String,
        @Field("leave_type") leaveType: String,
        @Field("reason") reason: String
    ): BasicResponse



}

