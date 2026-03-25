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

    suspend fun saveStaff(fields: Map<String, String>): Result<BasicResponse> = runCatching {
        api.saveStaff(fields)
    }

    suspend fun bulkSaveStaff(
        instId: Int, names: String, role: String, gender: String, userType: String
    ): Result<BasicResponse> = runCatching {
        api.bulkSaveStaff(instId = instId, names = names, role = role, gender = gender, userType = userType)
    }

    suspend fun deleteStaff(staffId: Int, instId: Int): Result<BasicResponse> = runCatching {
        api.deleteStaff(staffId = staffId, instId = instId)
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

    // ── Missing Methods for Merged UI ───────────────────────────────────────

    suspend fun fetchSubjects(instId: Int): Result<SubjectResponse> = runCatching {
        api.getSubjects(instId = instId)
    }

    suspend fun getAwardListExams(instId: Int, classId: Int, sectionId: Int) = runCatching {
        api.getAwardListExams(instId = instId, classId = classId, sectionId = sectionId)
    }

    suspend fun getAwardListStudents(instId: Int, examId: String, classId: Int, sectionId: Int) = runCatching {
        api.getAwardListStudents(instId = instId, examId = examId, classId = classId, sectionId = sectionId)
    }

    suspend fun saveAwardListMarks(instId: Int, examId: String, marksJson: String) = runCatching {
        api.saveAwardListMarks(instId = instId, examId = examId, marksJson = marksJson)
    }

    suspend fun deleteStudentAwardMark(instId: Int, examId: Int, studentId: Int) = runCatching {
        api.deleteStudentAwardMark(institutionId = instId, examId = examId, studentId = studentId)
    }

    suspend fun deleteFullAwardList(instId: Int, examId: Int) = runCatching {
        api.deleteFullAwardList(institutionId = instId, examId = examId)
    }

    suspend fun getExamHierarchyL1(instId: Int): Result<ExamHierarchyResponse> = runCatching {
        api.getExamHierarchyL1(instId = instId)
    }

    suspend fun getExamHierarchyL2(instId: Int, type: String, semester: String, year: String): Result<ExamL2HierarchyResponse> = runCatching {
        api.getExamHierarchyL2(instId = instId, type = type, semester = semester, year = year)
    }

    suspend fun deleteExamGroupL1(instId: Int, type: String, semester: String, year: String): Result<BasicResponse> = runCatching {
        api.deleteExamGroupL1(instId = instId, type = type, semester = semester, year = year)
    }

    suspend fun deleteExamGroupL2(instId: Int, type: String, semester: String, year: String, classId: Int, sectionId: Int): Result<BasicResponse> = runCatching {
        api.deleteExamGroupL2(instId = instId, type = type, semester = semester, year = year, classId = classId, sectionId = sectionId)
    }

    suspend fun createExam(type: String, semester: String, year: String, classId: Int, sectionId: Int, subjectsJson: String, instId: Int): Result<BasicResponse> = runCatching {
        api.createExam(institutionId = instId, type = type, semester = semester, year = year, classId = classId, sectionId = sectionId, subjectsJson = subjectsJson)
    }

    suspend fun quickScheduleExam(type: String, year: String, text: String, instId: Int): Result<BasicResponse> = runCatching {
        api.quickScheduleExam(type = type, year = year, text = text, institutionId = instId)
    }

    suspend fun getRollNoSlips(examType: String, classId: Int, instId: Int): Result<RollNoSlipResponse> = runCatching {
        api.getRollNoSlips(examType = examType, classId = classId, institutionId = instId)
    }

    suspend fun fetchNotices(instId: Int): Result<NoticeResponse> = runCatching {
        api.getNotices(instId = instId)
    }

    suspend fun saveNotice(fields: Map<String, String>, instId: Int) = runCatching {
        api.saveNotice(fields = fields, instId = instId)
    }

    suspend fun deleteNotice(id: Int, instId: Int) = runCatching {
        api.deleteNotice(id = id, instId = instId)
    }

    suspend fun classAction(action: String, instId: Int, id: Int?, classId: Int?, name: String?) = runCatching {
        api.classAction(action = action, instId = instId, id = id, classId = classId, name = name)
    }

    suspend fun subjectAction(action: String, instId: Int, id: Int?, name: String?, type: String?) = runCatching {
        api.subjectAction(action = action, instId = instId, id = id, name = name, type = type)
    }

    suspend fun getConsolidatedResult(instId: Int, classId: Int, sectionId: Int, examType: String, year: String, roll: String? = null, userId: Int? = null, role: String? = null) = runCatching {
        api.getConsolidatedResult(instId = instId, classId = classId, sectionId = sectionId, examType = examType, year = year, roll = roll, userId = userId, role = role)
    }

    suspend fun getFullResultCard(instId: Int, classId: Int, sectionId: Int, year: String, roll: String? = null, userId: Int? = null, role: String? = null) = runCatching {
        api.getFullResultCard(instId = instId, classId = classId, sectionId = sectionId, year = year, roll = roll, userId = userId, role = role)
    }

    suspend fun getTopperAnalytics(instId: Int, type: String, classId: Int? = null, sectionId: Int? = null) = runCatching {
        api.getTopperAnalytics(instId = instId, type = type, classId = classId, sectionId = sectionId)
    }

    suspend fun getSchoolAnalytics(instId: Int, classId: Int? = null, sectionId: Int? = null) = runCatching {
        api.getSchoolAnalytics(instId = instId, type = "school_performance", classId = classId, sectionId = sectionId)
    }

    suspend fun getStaffAnalytics(instId: Int, classId: Int? = null, sectionId: Int? = null) = runCatching {
        api.getStaffAnalytics(instId = instId, type = "staff_performance", classId = classId, sectionId = sectionId)
    }

    suspend fun getTrendAnalytics(instId: Int, classId: Int? = null, sectionId: Int? = null) = runCatching {
        api.getTrendAnalytics(instId = instId, type = "trends", classId = classId, sectionId = sectionId)
    }

    suspend fun getHallStaffList(instId: Int) = runCatching { api.getHallStaffList(instId) }
    suspend fun getHallSubjects(instId: Int, date: String, shift: String) = runCatching { api.getHallSubjects(instId, date, shift) }
    suspend fun generateHallView(instId: Int, date: String, shift: String, subjectId: String, roomsJson: String, staffJson: String) = runCatching {
        api.generateHallView(instId, date, shift, subjectId, roomsJson, staffJson)
    }

    // ── Timetable & Substitution (from WANTUCH-3) ───────────────────────────────

    suspend fun fetchTimetableMetadata(instId: Int): Result<TimetableMetadataResponse> = runCatching {
        api.getTimetableMetadata(instId = instId)
    }

    suspend fun fetchTimetable(instId: Int, classId: Int = 0, sectionId: Int = 0, day: String = "") = runCatching {
        api.getTimetable(instId = instId, classId = classId, sectionId = sectionId, day = day)
    }

    suspend fun fetchSubstitutionData(instId: Int, version: Int = 0) = runCatching {
        api.getSubstitutionData(instId = instId, version = version)
    }

    suspend fun assignSubstitution(instId: Int, ttId: Int, origSid: Int, subSid: Int, isPaid: Int) = runCatching {
        api.assignSubstitution(instId = instId, ttId = ttId, origSid = origSid, subSid = subSid, isPaid = isPaid)
    }

    suspend fun removeSubstitution(instId: Int, ttId: Int) = runCatching {
        api.removeSubstitution(instId = instId, ttId = ttId)
    }

    suspend fun fetchTimetableManagementSummary(instId: Int) = runCatching {
        api.getTimetableManagementSummary(instId = instId)
    }

    suspend fun fetchArchive(instId: Int, version: Int, level: String, staffId: Int, classId: Int = 0, sectionId: Int = 0) = runCatching {
        api.getTimetableArchive(instId = instId, version = version, level = level, staffId = staffId, classId = classId, sectionId = sectionId)
    }

    suspend fun fetchWizardData(instId: Int) = runCatching {
        api.getWizardData(instId = instId)
    }

    suspend fun saveTimetableSlot(
        instId: Int, id: Int = 0,
        staffId: Int, subjectId: Int, classId: Int, sectionId: Int,
        day: String, startTime: String, endTime: String,
        periodNo: Int, version: Int = 1, actType: String = "lesson"
    ) = runCatching {
        api.saveTimetableSlot(
            instId = instId, id = id,
            staffId = staffId, subjectId = subjectId,
            classId = classId, sectionId = sectionId,
            day = day, startTime = startTime, endTime = endTime,
            periodNo = periodNo, version = version, actType = actType
        )
    }

    suspend fun deleteTimetableSlot(instId: Int, id: Int) = runCatching {
        api.deleteTimetableSlot(instId = instId, id = id)
    }

    // ── ADM / WDL Module (from WANTUCH-3) ──────────────────────────────────────

    suspend fun fetchAdmWdlFresh(instId: Int, q: String = "", cls: String = "", limit: Int = 200) = runCatching {
        api.getAdmWdlFresh(instId = instId, q = q, cls = cls, limit = limit)
    }

    suspend fun fetchAdmWdlOld(instId: Int, q: String = "", limit: Int = 200) = runCatching {
        api.getAdmWdlOld(instId = instId, q = q, limit = limit)
    }

    suspend fun saveAdmWdlFresh(instId: Int, fields: Map<String, String>) = runCatching {
        api.saveAdmWdlFresh(instId = instId, fields = fields)
    }

    suspend fun saveAdmWdlOld(instId: Int, fields: Map<String, String>) = runCatching {
        api.saveAdmWdlOld(instId = instId, fields = fields)
    }

    suspend fun withdrawStudent(instId: Int, studentId: Int, admNo: String, withDate: String, withClass: String, slcStatus: String) = runCatching {
        api.withdrawStudent(instId = instId, studentId = studentId, admNo = admNo, withDate = withDate, withClass = withClass, slcStatus = slcStatus)
    }

    suspend fun deleteAdmEntry(instId: Int, id: Int, source: String) = runCatching {
        api.deleteAdmEntry(instId = instId, id = id, source = source)
    }

    suspend fun searchCertStudents(instId: Int, q: String) = runCatching {
        api.searchCertStudents(instId = instId, q = q)
    }

    // ── Smart ID Card & Profile Upload (from WANTUCH-4) ───────────────────────

    suspend fun uploadProfilePic(userId: Int, base64: String) = runCatching {
        api.uploadProfilePic(userId = userId, base64Image = base64)
    }

    suspend fun generateIdCard(userId: Int, userType: String, instId: Int) = runCatching {
        api.generateIdCard(userId = userId, userType = userType, instId = instId)
    }

    // ── Question Papers (from WANTUCH-4) ─────────────────────────────────────────

    suspend fun fetchQuestionPapers(instId: Int, classId: Int, subject: String, year: String, paperType: String): Result<QuestionPaperResponse> = runCatching {
        api.getQuestionPapers(instId = instId, classId = classId, subject = subject, year = year, paperType = paperType)
    }

    suspend fun saveQuestionPaper(instId: Int, title: String, subject: String, classId: Int, year: String, totalMarks: String, paperType: String): kotlin.Result<BasicResponse> = runCatching {
        api.saveQuestionPaper(instId = instId, title = title, subject = subject, classId = classId, year = year, totalMarks = totalMarks, paperType = paperType)
    }

    suspend fun deleteQuestionPaper(paperId: Int, instId: Int): kotlin.Result<BasicResponse> = runCatching {
        api.deleteQuestionPaper(paperId = paperId, instId = instId)
    }

    suspend fun saveSmartPaper(instId: Int, title: String, subject: String, totalMarks: String, sectionsData: String) = runCatching {
        api.saveSmartPaper(instId = instId, title = title, subject = subject, totalMarks = totalMarks, sectionsData = sectionsData)
    }
    suspend fun saveLeaveAppeal(instId: Int, userId: Int, fromDate: String, toDate: String, leaveType: String, reason: String) = runCatching {
        api.saveLeaveAppeal(instId = instId, userId = userId, fromDate = fromDate, toDate = toDate, leaveType = leaveType, reason = reason)
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
