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
    @POST("modules/education/auth_parent.php")
    suspend fun authParent(
        @Field("CNIC") cnic: String,
        @Field("PASSWORD") pass: String,
        @Field("ACTION") action: String
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
}
