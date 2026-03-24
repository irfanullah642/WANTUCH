package com.example.wantuch.data.repository

import com.example.wantuch.data.api.WantuchApi
import com.example.wantuch.domain.model.*
import com.example.wantuch.data.local.entities.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class WantuchRepository(private val baseUrl: String, private val dao: com.example.wantuch.data.local.dao.WantuchDao) {
    
    private val api: WantuchApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }
        val client = OkHttpClient.Builder()
            .cookieJar(object : okhttp3.CookieJar {
                private val cookieStore = mutableMapOf<String, MutableList<okhttp3.Cookie>>()
                override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
                    if (cookies.isNotEmpty()) {
                        val hostCookies = cookieStore.getOrPut(url.host) { mutableListOf() }
                        cookies.forEach { newCookie ->
                            hostCookies.removeAll { it.name == newCookie.name }
                            hostCookies.add(newCookie)
                        }
                    }
                    // Sync with WebView for seamless experience
                    val webkitManager = android.webkit.CookieManager.getInstance()
                    cookies.forEach { cookie ->
                        webkitManager.setCookie(url.toString(), cookie.toString())
                    }
                    webkitManager.flush()
                }
                override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
                    return cookieStore[url.host] ?: listOf()
                }
            })
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    // Some servers reset if they see "identity" or specific encodings from OkHttp
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WantuchApi::class.java)
    }

    // Offline caching streams
    fun getLocalStudents(instId: Int) = dao.getStudents(instId)
    fun getLocalStaff(instId: Int) = dao.getStaff(instId)

    val getLocalInstitutions: Flow<List<Institution>> = dao.getAllInstitutions().map { it.map { entity -> entity.toDomain() } }
    fun getLocalDashboard(instId: Int): Flow<DashboardResponse?> = dao.getDashboard(instId).map { it?.toDomain() }
    val getLocalPortfolio: Flow<PortfolioResponse?> = dao.getPortfolio().map { it?.toDomain() }

    suspend fun getInstitutions(type: String): Result<List<Institution>> = try {
        val response = api.getInstitutions(type)
        if (response.isNotEmpty()) {
            dao.insertInstitutions(response.map { it.toEntity() })
        }
        Result.success(response)
    } catch (e: Exception) {
        // Fallback to cache if network fails
        val cached = dao.getAllInstitutionsOnce()
        if (cached.isNotEmpty()) {
            Result.success(cached.map { it.toDomain() })
        } else {
            Result.failure(e)
        }
    }

    suspend fun authInstitution(instId: Int, user: String, pass: String, role: String): Result<LoginResponse> = runCatching {
        api.authInstitution(instId, user, pass, role, "LOGIN")
    }

    suspend fun authParent(cnic: String, pass: String): Result<LoginResponse> = runCatching {
        api.authParent(cnic, pass, "PARENT_LOGIN")
    }

    suspend fun generalLogin(user: String, pass: String): Result<LoginResponse> = runCatching {
        api.generalLogin(user, pass)
    }

    suspend fun getUnreadCounts(): Result<Map<String, Int>> = runCatching {
        api.getUnreadCounts()
    }

    suspend fun fetchPortfolio(): Result<PortfolioResponse> = try {
        val response = api.getPortfolio()
        if (response.status == "success") {
            dao.insertPortfolio(response.toEntity())
        }
        Result.success(response)
    } catch (e: Exception) {
        val cached = dao.getPortfolioOnce()
        if (cached != null) {
            Result.success(cached.toDomain())
        } else {
            Result.failure(e)
        }
    }

    suspend fun fetchDashboard(instId: Int): Result<DashboardResponse> = try {
        val response = api.switchAndGetDashboard(instId = instId)
        if (response.status == "success") {
            dao.insertDashboard(response.toEntity(instId))
        }
        Result.success(response)
    } catch (e: Exception) {
        val cached = dao.getDashboardOnce(instId)
        if (cached != null) {
            Result.success(cached.toDomain())
        } else {
            Result.failure(e)
        }
    }

    suspend fun genericGet(action: String, params: Map<String, String>) = runCatching {
        api.genericGet(action, params)
    }

    suspend fun fetchStaff(instId: Int): Result<StaffResponse> = try {
        val response = api.getStaff(instId = instId)
        if (response.status == "success") {
            val allStaff = (response.teaching_staff ?: emptyList()) + (response.non_teaching_staff ?: emptyList())
            if (allStaff.isNotEmpty()) {
                val entities = allStaff.map { it.toEntity(instId) }
                dao.insertStaff(entities)
            }
        }
        Result.success(response)
    } catch (e: Exception) {
        val cachedEntities = dao.getStaffOnce(instId)
        if (cachedEntities.isNotEmpty()) {
            val teaching = cachedEntities.filter { it.role.contains("Teaching", ignoreCase = true) }.map { it.toDomain() }
            val nonTeaching = cachedEntities.filter { !it.role.contains("Teaching", ignoreCase = true) }.map { it.toDomain() }
            Result.success(StaffResponse(status = "success", teaching_staff = teaching, non_teaching_staff = nonTeaching))
        } else {
            Result.failure(e)
        }
    }

    suspend fun fetchStaffProfile(staffId: Int, instId: Int): Result<StaffProfileResponse> = runCatching {
        api.getStaffProfile(staffId = staffId, instId = instId)
    }

    suspend fun updateStaffProfile(fields: Map<String, String>): Result<BasicResponse> = runCatching {
        api.updateStaffProfile(fields)
    }

    suspend fun updateInstitutionProfile(fields: Map<String, String>): Result<BasicResponse> = runCatching {
        api.updateInstitutionProfile(fields)
    }

    suspend fun saveContact(fields: Map<String, String>): Result<BasicResponse> = runCatching { api.saveContact(fields) }
    suspend fun deleteContact(id: Int, staffId: Int): Result<BasicResponse> = runCatching { api.deleteContact(id, staffId) }

    suspend fun saveAcademic(fields: Map<String, String>): Result<BasicResponse> = runCatching { api.saveAcademic(fields) }
    suspend fun deleteAcademic(id: Int, staffId: Int): Result<BasicResponse> = runCatching { api.deleteAcademic(id, staffId) }

    suspend fun saveExperience(fields: Map<String, String>): Result<BasicResponse> = runCatching { api.saveExperience(fields) }
    suspend fun deleteExperience(id: Int, staffId: Int): Result<BasicResponse> = runCatching { api.deleteExperience(id, staffId) }

    suspend fun updateSyllabusStatus(id: Int, status: String): Result<BasicResponse> = runCatching {
        api.updateSyllabusStatus(id = id, status = status)
    }

    suspend fun saveFullSyllabus(payload: SyllabusWizardPayload): Result<BasicResponse> = runCatching {
        api.saveFullSyllabus(payload = payload)
    }

    suspend fun editSyllabusChapter(payload: EditChapterPayload): Result<BasicResponse> = runCatching {
        api.editSyllabusChapter(payload = payload)
    }

    suspend fun deleteSyllabusTopic(id: Int, institutionId: Int): Result<BasicResponse> = runCatching {
        api.deleteSyllabusTopic(id = id, institution_id = institutionId)
    }

    suspend fun getStudentsForPromotion(instId: Int, classId: Int, sectionId: Int, criteria: String, year: String): Result<PromotionResponse> = runCatching {
        api.getStudentsForPromotion(instId, classId, sectionId, criteria, year)
    }

    suspend fun promoteStudents(instId: Int, promotions: String, targetClassId: Int, sourceClassId: Int, sourceSectionId: Int, year: String): Result<PromotionResponse> = runCatching {
        api.promoteStudents(instId, promotions, targetClassId, sourceClassId, sourceSectionId, year)
    }

    suspend fun shiftStudents(instId: Int, studentIds: String, targetClassId: Int, sourceClassId: Int, sourceSectionId: Int, year: String): Result<PromotionResponse> = runCatching {
        api.shiftStudents(instId, studentIds, targetClassId, sourceClassId, sourceSectionId, year)
    }

    suspend fun saveBank(fields: Map<String, String>): Result<BasicResponse> = runCatching { api.saveBank(fields) }
    suspend fun deleteBank(id: Int, staffId: Int): Result<BasicResponse> = runCatching { api.deleteBank(id, staffId) }

    suspend fun saveInstPost(fields: Map<String, String>): Result<BasicResponse> = runCatching { api.saveInstPost(fields) }
    suspend fun deleteInstPost(id: Int, instId: Int): Result<BasicResponse> = runCatching { api.deleteInstPost(id, instId) }

    suspend fun saveInstBank(fields: Map<String, String>): Result<BasicResponse> = runCatching { api.saveInstBank(fields) }
    suspend fun deleteInstBank(id: Int, instId: Int): Result<BasicResponse> = runCatching { api.deleteInstBank(id, instId) }

    suspend fun saveInstAsset(fields: Map<String, String>): Result<BasicResponse> = runCatching { api.saveInstAsset(fields) }
    suspend fun deleteInstAsset(id: Int, instId: Int): Result<BasicResponse> = runCatching { api.deleteInstAsset(id, instId) }

    suspend fun saveInstFund(fields: Map<String, String>): Result<BasicResponse> = runCatching { api.saveInstFund(fields) }
    suspend fun deleteInstFund(id: Int, instId: Int): Result<BasicResponse> = runCatching { api.deleteInstFund(id, instId) }

    suspend fun feeApiPost(action: String, params: Map<String, String>): Result<okhttp3.ResponseBody> = runCatching { api.feeApiPost(action, params) }

    suspend fun saveStudent(fields: Map<String, String>, instId: Int) = runCatching {
        api.saveStudent(fields = fields, institutionId = instId)
    }

    suspend fun markAttendance(studentId: Int, status: String, instId: Int, date: String = "") = runCatching {
        api.markAttendance(studentId = studentId, status = status, institutionId = instId, date = date)
    }

    suspend fun fetchStudents(instId: Int, classId: Int = 0, sectionId: Int = 0, status: String = "active", year: String = ""): Result<StudentResponse> = try {
        val response = api.getStudents(instId = instId, classId = classId, sectionId = sectionId, status = status, year = year)
        if (response.status == "success" && response.students != null) {
            val entities = response.students.map { it.toEntity(instId) }
            dao.insertStudents(entities)
        }
        Result.success(response)
    } catch (e: Exception) {
        val cached = dao.getStudentsOnce(instId)
        if (cached.isNotEmpty()) {
            Result.success(StudentResponse(status = "success", students = cached.map { it.toDomain() }))
        } else {
            Result.failure(e)
        }
    }

    suspend fun bulkSaveStudents(instId: Int, classId: Int, sectionId: Int, namesText: String, gender: String) = runCatching {
        api.bulkSaveStudents(institutionId = instId, classId = classId, sectionId = sectionId, namesText = namesText, gender = gender)
    }

    suspend fun fetchStudentProfile(studentId: Int, instId: Int): Result<StudentProfileResponse> = runCatching {
        api.getStudentProfile(studentId = studentId, instId = instId)
    }

    suspend fun fetchSchoolStructure(instId: Int): Result<SchoolStructureResponse> = try {
        val response = api.getStructure(instId = instId)
        if (response.status == "success" && response.classes != null) {
            val classEntities = response.classes.map { it.toEntity(instId) }
            dao.insertClasses(classEntities)
            response.classes.forEach { schoolClass ->
                schoolClass.sections?.let { sections ->
                    val sectionEntities = sections.map { it.toEntity(schoolClass.id) }
                    dao.insertSections(sectionEntities)
                }
            }
        }
        Result.success(response)
    } catch (e: Exception) {
        val classes = dao.getClassesOnce(instId)
        if (classes.isNotEmpty()) {
             // For now we map classes with empty sections to avoid compile error, 
             // since querying relation wasn't setup
             val domainClasses = classes.map { SchoolClass(id = it.id, name = it.name, sections = emptyList()) }
             Result.success(SchoolStructureResponse(status = "success", classes = domainClasses))
        } else {
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(studentId: Int, instId: Int): Result<BasicResponse> = runCatching {
        api.deleteStudent(studentId = studentId, instId = instId)
    }

    suspend fun collectFee(studentId: Int, instId: Int, amount: Double, mode: String, category: String, month: String, year: String) = runCatching {
        api.collectFee(studentId = studentId, amount = amount, mode = mode, category = category, month = month, year = year, institutionId = instId)
    }

    suspend fun updateStudentRole(studentId: Int, instId: Int, role: String) = runCatching {
        api.updateRole(studentId = studentId, role = role, institutionId = instId)
    }

    suspend fun changeStudentStatus(studentId: Int, instId: Int, status: String) = runCatching {
        api.changeStatus(studentId = studentId, status = status, institutionId = instId)
    }

    suspend fun markStaffAttendance(staffId: Int, instId: Int, status: String, date: String) = runCatching {
        api.markStaffAttendance(staffId = staffId, status = status, date = date, institutionId = instId)
    }

    suspend fun recordSalary(staffId: Int, instId: Int, amount: Double, month: String, year: String, mode: String) = runCatching {
        api.recordSalary(staffId = staffId, amount = amount, month = month, year = year, mode = mode, institutionId = instId)
    }

    suspend fun updateStaffStatus(staffId: Int, status: String) = runCatching {
        api.updateStaffStatus(staffId = staffId, status = status)
    }

    suspend fun getAttendanceSubmissionStatus(instId: Int, date: String) = runCatching {
        api.getAttendanceSubmissionStatus(instId = instId, date = date)
    }

    suspend fun markAttendanceSubmitted(body: Map<String, String>) = runCatching {
        api.markAttendanceSubmitted(body)
    }

    suspend fun clearAttendanceSubmission(body: Map<String, String>) = runCatching {
        api.clearAttendanceSubmission(body)
    }

    suspend fun downloadAttendanceCsv(instId: Int, dateFrom: String, dateTo: String, classSelection: String) = runCatching {
        api.downloadAttendanceCsv(instId = instId, dateFrom = dateFrom, dateTo = dateTo, classSelection = classSelection)
    }

    suspend fun saveBulkImportJson(instId: Int, jsonBody: String) = runCatching {
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        api.saveBulkImportJson(body)
    }

    suspend fun getSmartStatus(instId: Int) = runCatching { api.getSmartStatus(instId = instId) }
    suspend fun getFaceSessions(instId: Int, limit: Int = 10) = runCatching { api.getFaceSessions(instId = instId, limit = limit) }
    suspend fun enrollFace(instId: Int, userId: Int, category: String, base64: String) = runCatching {
        api.enrollFace(institutionId = instId, userId = userId, category = category, photoBase64 = base64)
    }

    suspend fun verifyFace(instId: Int, base64: String) = runCatching {
        api.verifyFace(institutionId = instId, photoBase64 = base64)
    }
    
    suspend fun fetchQuestionPapers(
        instId: Int,
        classId: Int = 0,
        subject: String = "",
        year: String = "",
        paperType: String = ""
    ): Result<QuestionPaperResponse> = runCatching {
        api.getQuestionPapers(
            instId = instId,
            classId = classId,
            subject = subject,
            year = year,
            paperType = paperType
        )
    }

    suspend fun saveQuestionPaper(
        instId: Int, title: String, subject: String,
        classId: Int, year: String, totalMarks: String, paperType: String
    ): Result<BasicResponse> = runCatching {
        api.saveQuestionPaper(
            action = "SAVE_QUESTION_PAPER",
            instId = instId, title = title, subject = subject,
            classId = classId, year = year, totalMarks = totalMarks, paperType = paperType
        )
    }

    suspend fun deleteQuestionPaper(paperId: Int, instId: Int): Result<BasicResponse> = runCatching {
        api.deleteQuestionPaper(action = "DELETE_QUESTION_PAPER", paperId = paperId, instId = instId)
    }

    suspend fun saveSmartPaper(instId: Int, totalMarks: String, sectionsData: String): Result<BasicResponse> = runCatching {
        api.saveSmartPaper(instId = instId, totalMarks = totalMarks, sectionsData = sectionsData)
    }

    suspend fun getAttendanceRules(instId: Int) = runCatching { api.getAttendanceRules(instId = instId) }
    suspend fun saveAttendanceRules(instId: Int, fields: Map<String, String>) = runCatching {
        api.saveAttendanceRules(institutionId = instId, fields = fields)
    }
    suspend fun getLeaveAppeals(instId: Int) = runCatching { api.getLeaveAppeals(instId = instId) }
    suspend fun updateAppealStatus(instId: Int, id: Int, status: String) = runCatching {
        api.updateAppealStatus(institutionId = instId, id = id, status = status)
    }
    suspend fun getMonthlyStudentLedger(instId: Int, classId: Int, month: String, year: Int) = runCatching {
        api.getMonthlyStudentLedger(instId = instId, classId = classId, month = month, year = year)
    }
    suspend fun getMonthlyStaffLedger(instId: Int, month: String, year: Int) = runCatching {
        api.getMonthlyStaffLedger(instId = instId, month = month, year = year)
    }

    suspend fun fetchSyllabus(instId: Int, classId: Int = 0, sectionId: Int = 0, subjectId: Int = 0) = runCatching {
        api.getSyllabus(instId = instId, classId = classId, sectionId = sectionId, subjectId = subjectId)
    }

    suspend fun importDatabase(fileBytes: ByteArray, filename: String, onProgress: (Float) -> Unit) = runCatching {
        val requestBody = ProgressRequestBody("application/sql".toMediaTypeOrNull(), fileBytes, onProgress)
        val part = okhttp3.MultipartBody.Part.createFormData("sql_file", filename, requestBody)
        api.importDatabase(part)
    }

    suspend fun fetchAssignments() = runCatching { api.getAssignments() }

    suspend fun createAssignment(
        classId: Int,
        sectionId: Int,
        subjectId: Int,
        title: String,
        description: String,
        dueDate: String,
        fileBytes: ByteArray?,
        filename: String?,
        onProgress: (Float) -> Unit
    ) = runCatching {
        val classRB = classId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val sectionRB = sectionId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val subjectRB = subjectId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val titleRB = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val descRB = description.toRequestBody("text/plain".toMediaTypeOrNull())
        val dateRB = dueDate.toRequestBody("text/plain".toMediaTypeOrNull())
        
        val part = if (fileBytes != null && filename != null) {
            val rb = ProgressRequestBody("application/octet-stream".toMediaTypeOrNull(), fileBytes, onProgress)
            okhttp3.MultipartBody.Part.createFormData("attachment", filename, rb)
        } else null
        
        api.createAssignment(classRB, sectionRB, subjectRB, titleRB, descRB, dateRB, part)
    }

    suspend fun reviewSubmission(subId: Int, status: String, feedback: String) = runCatching {
        api.reviewSubmission(subId, status, feedback)
    }

    suspend fun fetchSubmissionDetails(assignmentId: Int) = runCatching {
        api.getSubmissionDetails(assignmentId)
    }

    suspend fun updateAssignment(id: Int, title: String, description: String, dueDate: String) = runCatching {
        api.updateAssignment(id, title, description, dueDate)
    }

    // ── Study Planner ────────────────────────────────────────────────────────────

    suspend fun getStudySubjects(instId: Int, classId: Int, sectionId: Int) = runCatching {
        api.getStudySubjects(instId, classId, sectionId)
    }

    suspend fun getPlannerData(instId: Int, classId: Int, sectionId: Int) = runCatching {
        api.getPlannerData(instId, classId, sectionId)
    }

    suspend fun savePlannerConfig(instId: Int, classId: Int, sectionId: Int, config: String) = runCatching {
        api.savePlannerConfig(instId, classId, sectionId, config)
    }

    suspend fun completeTopic(instId: Int, subjectId: Int, topicName: String) = runCatching {
        api.completeTopic(instId, subjectId, topicName)
    }
}

class ProgressRequestBody(
    private val contentType: okhttp3.MediaType?,
    private val fileBytes: ByteArray,
    private val onProgress: (Float) -> Unit
) : okhttp3.RequestBody() {
    override fun contentType() = contentType
    override fun contentLength() = fileBytes.size.toLong()
    override fun writeTo(sink: okio.BufferedSink) {
        val contentLength = fileBytes.size.toLong()
        var uploaded = 0L
        val chunkSize = 8192
        var offset = 0
        while (offset < fileBytes.size) {
            val count = kotlin.math.min(chunkSize, fileBytes.size - offset)
            sink.write(fileBytes, offset, count)
            offset += count
            uploaded += count
            val progress = uploaded.toFloat() / contentLength
            onProgress(progress)
        }
    }
}
