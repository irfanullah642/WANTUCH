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

    suspend fun getInstitutions(type: String): Result<List<Institution>> = runCatching {
        val response = api.getInstitutions(type)
        if (response.isNotEmpty()) {
            dao.insertInstitutions(response.map { it.toEntity() })
        }
        response
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

    suspend fun fetchPortfolio(): Result<PortfolioResponse> = runCatching {
        val response = api.getPortfolio()
        if (response.status == "success") {
            dao.insertPortfolio(response.toEntity())
        }
        response
    }

    suspend fun fetchDashboard(instId: Int): Result<DashboardResponse> = runCatching {
        val response = api.switchAndGetDashboard(instId = instId)
        if (response.status == "success") {
            dao.insertDashboard(response.toEntity(instId))
        }
        response
    }

    suspend fun genericGet(action: String, params: Map<String, String>) = runCatching {
        api.genericGet(action, params)
    }

    suspend fun fetchStaff(instId: Int): Result<StaffResponse> = runCatching {
        val response = api.getStaff(instId = instId)
        if (response.status == "success") {
            val allStaff = (response.teaching_staff ?: emptyList()) + (response.non_teaching_staff ?: emptyList())
            if (allStaff.isNotEmpty()) {
                val entities = allStaff.map { it.toEntity(instId) }
                dao.insertStaff(entities)
            }
        }
        response
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

    suspend fun fetchStudents(instId: Int, classId: Int = 0, sectionId: Int = 0, status: String = "active", year: String = ""): Result<StudentResponse> = runCatching {
        val response = api.getStudents(instId = instId, classId = classId, sectionId = sectionId, status = status, year = year)
        if (response.status == "success" && response.students != null) {
            val entities = response.students.map { it.toEntity(instId) }
            dao.insertStudents(entities)
        }
        response
    }

    suspend fun bulkSaveStudents(instId: Int, classId: Int, sectionId: Int, namesText: String, gender: String) = runCatching {
        api.bulkSaveStudents(institutionId = instId, classId = classId, sectionId = sectionId, namesText = namesText, gender = gender)
    }

    suspend fun fetchStudentProfile(studentId: Int, instId: Int): Result<StudentProfileResponse> = runCatching {
        api.getStudentProfile(studentId = studentId, instId = instId)
    }

    suspend fun fetchSchoolStructure(instId: Int): Result<SchoolStructureResponse> = runCatching {
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
        response
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
}
