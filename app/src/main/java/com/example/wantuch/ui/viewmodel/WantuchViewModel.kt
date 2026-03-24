package com.example.wantuch.ui.viewmodel

import android.util.Log

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wantuch.BASE_URL
import com.example.wantuch.data.repository.WantuchRepository
import com.example.wantuch.domain.model.Institution
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.wantuch.domain.model.*
import com.example.wantuch.data.local.entities.*
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import android.widget.Toast

class WantuchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = com.example.wantuch.data.local.database.WantuchDatabase.getDatabase(application)
    private val repository = WantuchRepository(BASE_URL, database.wantuchDao())
    private val prefs = application.getSharedPreferences("wantuch_prefs", Context.MODE_PRIVATE)
    
    fun getInstitutionId(): Int = prefs.getInt("last_inst", 0)
    
    // Face Recognition Initialization
    private val faceEngine = com.example.wantuch.ml.FaceRecognitionEngine(application)
    private val _localFaceDB = MutableStateFlow<List<FaceEmbeddingEntity>>(emptyList())
    val localFaceDB = _localFaceDB.asStateFlow()
    
    // Performance Optimization: Cache parsed embeddings in memory
    private val _parsedFaceDB = mutableListOf<Pair<FaceEmbeddingEntity, FloatArray>>()
    
    // Session State: Users already recognized in the current camera session (ID -> Name)
    private val _recognizedInSession = MutableStateFlow<Map<Int, String>>(emptyMap())
    val recognizedInSession = _recognizedInSession.asStateFlow()

    private val _institutions = MutableStateFlow<List<Institution>>(emptyList())
    val institutions = _institutions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMsg = MutableStateFlow("")
    val errorMsg = _errorMsg.asStateFlow()

    private val _portfolio = MutableStateFlow<PortfolioResponse?>(null)
    val portfolio = _portfolio.asStateFlow()

    private val _dashboardData = MutableStateFlow<DashboardResponse?>(null)
    val dashboardData = _dashboardData.asStateFlow()

    private val _staffData = MutableStateFlow<StaffResponse?>(null)
    val staffData = _staffData.asStateFlow()

    private val _staffProfile = MutableStateFlow<StaffProfileResponse?>(null)
    val staffProfile = _staffProfile.asStateFlow()

    private val _studentsData = MutableStateFlow<StudentResponse?>(null)
    val studentsData = _studentsData.asStateFlow()

    private val _promotionData = MutableStateFlow<PromotionResponse?>(null)
    val promotionData = _promotionData.asStateFlow()

    // Last enrolled user mapping for fingerprint simulation
    private val _lastEnrolledUser = MutableStateFlow<Pair<Int, String>?>(null)
    val lastEnrolledUser = _lastEnrolledUser.asStateFlow()

    fun setLastEnrolledUser(id: Int, name: String) {
        _lastEnrolledUser.value = id to name
    }

    private val _smartConfig = MutableStateFlow<Map<String, Any>>(emptyMap())
    val smartConfig = _smartConfig.asStateFlow()

    fun fetchSmartConfig(instId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.genericGet("GET_SMART_CONFIG", mapOf("institution_id" to instId.toString()))
                response.onSuccess { body ->
                    val json = org.json.JSONObject(body.string())
                    if (json.optString("status") == "success") {
                        val config = json.optJSONObject("config")
                        val map = mutableMapOf<String, Any>()
                        config?.keys()?.forEach { key ->
                            map[key] = config.get(key)
                        }
                        _smartConfig.value = map
                    }
                }
            } catch (e: Exception) { Log.e("Wantuch", "Fetch Config Error: ${e.message}") }
        }
    }

    fun saveSmartConfig(instId: Int, params: Map<String, String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // We'll use a specific call in repository or generic post if available
                // For now, let's assume we implement it in repository
                val res = repository.markAttendanceSubmitted(params + mapOf("action" to "SAVE_SMART_CONFIG", "institution_id" to instId.toString()))
                if (res.isSuccess) {
                    _errorMsg.value = "Settings Deployed Successfully!"
                    fetchSmartConfig(instId)
                }
            } catch (e: Exception) { _errorMsg.value = e.message ?: "Update Failed" }
            _isLoading.value = false
        }
    }

    private val _studentLedger = MutableStateFlow<org.json.JSONObject?>(null)
    val studentLedger = _studentLedger.asStateFlow()

    private val _staffLedger = MutableStateFlow<org.json.JSONObject?>(null)
    val staffLedger = _staffLedger.asStateFlow()

    fun fetchMonthlyStudentLedger(instId: Int, classId: Int, month: String, year: Int) {
        viewModelScope.launch {
            _studentLedger.value = null
            _isLoading.value = true
            try {
                val res = repository.genericGet("GET_MONTHLY_STUDENT_LEDGER", mapOf("institution_id" to instId.toString(), "class_id" to classId.toString(), "month" to month, "year" to year.toString()))
                res.onSuccess { body ->
                    _studentLedger.value = org.json.JSONObject(body.string())
                }
            } catch (e: Exception) { _errorMsg.value = e.message ?: "Fetch Ledger Error" }
            _isLoading.value = false
        }
    }

    fun fetchMonthlyStaffLedger(instId: Int, month: String, year: Int) {
        viewModelScope.launch {
            _staffLedger.value = null
            _isLoading.value = true
            try {
                val res = repository.genericGet("GET_MONTHLY_STAFF_LEDGER", mapOf("institution_id" to instId.toString(), "month" to month, "year" to year.toString()))
                res.onSuccess { body ->
                    _staffLedger.value = org.json.JSONObject(body.string())
                }
            } catch (e: Exception) { _errorMsg.value = e.message ?: "Fetch Staff Ledger Error" }
            _isLoading.value = false
        }
    }

    private val _lastInstId = MutableStateFlow(0)
    val lastInstId = _lastInstId.asStateFlow()

    init {
        _lastInstId.value = prefs.getInt("last_inst", 0)
        // Collect cached data to show immediately when app starts
        viewModelScope.launch {
            try {
                // Portfolio & Institutions (Global)
                launch {
                    try {
                        repository.getLocalInstitutions.collect { entities ->
                            if (entities.isNotEmpty() && _institutions.value.isEmpty()) {
                                _institutions.value = entities
                            }
                        }
                    } catch (e: Exception) { println("Local Inst Error: ${e.message}") }
                }
                launch {
                    try {
                        repository.getLocalPortfolio.collect { data ->
                            if (data != null && _portfolio.value == null) {
                                _portfolio.value = data
                            }
                        }
                    } catch (e: Exception) { println("Local Portfolio Error: ${e.message}") }
                }

                // Institution-specific data
                val lastId = prefs.getInt("last_inst", 0)
                if (lastId != 0) {
                    launch {
                        try {
                            repository.getLocalDashboard(lastId).collect { data ->
                                if (data != null && _dashboardData.value == null) {
                                    _dashboardData.value = data
                                }
                            }
                        } catch (e: Exception) { println("Local Dash Error: ${e.message}") }
                    }
                    launch {
                        try {
                            repository.getLocalStudents(lastId).collect { entities ->
                                if (entities.isNotEmpty() && _studentsData.value == null) {
                                    val domainStudents = entities.map { it.toDomain() }
                                    _studentsData.value = StudentResponse(status = "success", students = domainStudents)
                                }
                            }
                        } catch (e: Exception) { println("Local Student Error: ${e.message}") }
                    }
                    launch {
                        try {
                            repository.getLocalStaff(lastId).collect { entities ->
                                if (entities.isNotEmpty() && _staffData.value == null) {
                                    val teaching = entities.filter { it.role.contains("Teaching", ignoreCase = true) }.map { it.toDomain() }
                                    val nonTeaching = entities.filter { !it.role.contains("Teaching", ignoreCase = true) }.map { it.toDomain() }
                                    _staffData.value = StaffResponse(status = "success", teaching_staff = teaching, non_teaching_staff = nonTeaching)
                                }
                            }
                        } catch (e: Exception) { println("Local Staff Error: ${e.message}") }
                    }
                }
                
                // Load local face embeddings and pre-parse them for speed
                launch {
                    try {
                        database.faceEmbeddingDao().getAllEmbeddings().collect { list ->
                            _localFaceDB.value = list
                            _parsedFaceDB.clear()
                            list.forEach { entity ->
                                try {
                                    val emb = com.google.gson.Gson().fromJson(entity.embedding, FloatArray::class.java)
                                    _parsedFaceDB.add(entity to emb)
                                } catch (e: Exception) { Log.e("WantuchVM", "Parse Error: ${e.message}") }
                            }
                            Log.d("WantuchVM", "Loaded and parsed ${_parsedFaceDB.size} face embeddings locally")
                        }
                    } catch (e: Exception) { Log.e("WantuchVM", "Face DB Error: ${e.message}") }
                }
            } catch (e: Exception) {
                 println("Global Init Error: ${e.message}")
            }
        }
    }

    private val _studentProfile = MutableStateFlow<StudentProfileResponse?>(null)
    val studentProfile = _studentProfile.asStateFlow()

    private val _schoolStructure = MutableStateFlow<SchoolStructureResponse?>(null)
    val schoolStructure = _schoolStructure.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    fun safeApiCall(action: String, params: Map<String, String>, onResult: (org.json.JSONObject) -> Unit) {
        viewModelScope.launch {
            repository.genericGet(action, params).onSuccess { body ->
                try {
                    val json = org.json.JSONObject(body.string())
                    onResult(json)
                } catch (e: Exception) {
                    onResult(org.json.JSONObject().put("status", "error").put("message", "JSON Parse Error"))
                }
            }.onFailure {
                onResult(org.json.JSONObject().put("status", "error").put("message", it.message))
            }
        }
    }

    fun safeFeeApiCall(action: String, params: Map<String, String>, onResult: (org.json.JSONObject) -> Unit) {
        viewModelScope.launch {
            repository.feeApiPost(action, params).onSuccess { body ->
                try {
                    val json = org.json.JSONObject(body.string())
                    onResult(json)
                } catch (e: Exception) {
                    onResult(org.json.JSONObject().put("status", "error").put("message", "JSON Parse Error"))
                }
            }.onFailure {
                onResult(org.json.JSONObject().put("status", "error").put("message", it.message))
            }
        }
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // Mock/Actual state for app items unread counts
    val unreadCounts = mutableMapOf<String, Int>()

    fun fetchInstitutions(type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = ""
            val result = repository.getInstitutions(type)
            if (result.isSuccess) {
                _institutions.value = result.getOrNull() ?: emptyList()
            } else {
                _errorMsg.value = "Load Error: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun loginInstitution(instId: Int, user: String, pass: String, role: String, remember: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMsg.value = ""
                val result = repository.authInstitution(instId, user, pass, role)
                if (result.isSuccess) {
                    val resp = result.getOrNull()
                    if (resp?.status == "success") {
                        if (remember) {
                            prefs.edit().apply {
                                putString("user", user)
                                putString("pass", pass)
                                putInt("last_inst", instId)
                                putString("role", role)
                                putBoolean("remember", true)
                                apply()
                            }
                        } else {
                            prefs.edit().remove("user").remove("pass").remove("remember").apply()
                        }
                        onResult(resp.redirect ?: "modules/education/dashboard.php")
                    } else {
                        _errorMsg.value = resp?.message ?: "Login failed"
                        onResult(null)
                    }
                } else {
                    _errorMsg.value = "Network error: ${result.exceptionOrNull()?.message}"
                    onResult(null)
                }
            } catch (e: Exception) {
                _errorMsg.value = "Unexpected Error: ${e.message}"
                onResult(null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loginParent(cnic: String, pass: String, remember: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMsg.value = ""
                val result = repository.authParent(cnic, pass)
                if (result.isSuccess) {
                    val resp = result.getOrNull()
                    if (resp?.status == "success") {
                        if (remember) {
                            prefs.edit().apply {
                                putString("cnic", cnic)
                                putString("parent_pass", pass)
                                putBoolean("remember_parent", true)
                                apply()
                            }
                        }
                        onResult(resp.redirect ?: "modules/education/parent_dashboard.php")
                    } else {
                        _errorMsg.value = resp?.message ?: "Login failed"
                        onResult(null)
                    }
                } else {
                    _errorMsg.value = "Network error: ${result.exceptionOrNull()?.message}"
                    onResult(null)
                }
            } catch (e: Exception) {
                _errorMsg.value = "Unexpected Error: ${e.message}"
                onResult(null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startNotificationLoop(onUpdate: (Map<String, Int>) -> Unit) {
        viewModelScope.launch {
            while (true) {
                repository.getUnreadCounts().onSuccess {
                    onUpdate(it)
                }
                delay(60000)
            }
        }
    }

    fun clearError() {
        _errorMsg.value = ""
    }

    fun fetchPortfolio() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMsg.value = ""
                val result = repository.fetchPortfolio()
                if (result.isSuccess) {
                    _portfolio.value = result.getOrNull()
                } else {
                    _errorMsg.value = "Portfolio Load Error: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMsg.value = "Portfolio Crash: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectInstitution(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMsg.value = ""
                val result = repository.fetchDashboard(id)
                if (result.isSuccess) {
                    _dashboardData.value = result.getOrNull()
                    _lastInstId.value = id
                    prefs.edit().putInt("last_inst", id).apply()
                    onSuccess()
                } else {
                    _errorMsg.value = "Dashboard Load Error: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMsg.value = "Dashboard Crash: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshDashboard() {
        val lastId = prefs.getInt("last_inst", 0)
        if (lastId != 0) {
            selectInstitution(lastId) {}
        }
    }

    fun fetchStaff() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMsg.value = ""
                val lastId = prefs.getInt("last_inst", 0)
                if (lastId == 0) {
                    _errorMsg.value = "No institution selected"
                    return@launch
                }
                val result = repository.fetchStaff(lastId)
                if (result.isSuccess) {
                    val resp = result.getOrNull()
                    if (resp?.status == "success") {
                        _staffData.value = resp
                    } else {
                        _errorMsg.value = resp?.message ?: "Failed to load staff"
                    }
                } else {
                    _errorMsg.value = "Staff Load Error: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMsg.value = "Staff Crash: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchStaffProfile(staffId: Int) {
        _staffProfile.value = null
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMsg.value = ""
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.fetchStaffProfile(staffId, lastId)
                if (result.isSuccess) {
                    val resp = result.getOrNull()
                    if (resp?.status == "success") {
                        _staffProfile.value = resp
                    } else {
                        _errorMsg.value = resp?.message ?: "Status Error: ${resp?.status}"
                    }
                } else {
                    _errorMsg.value = "Network Error: ${result.exceptionOrNull()?.localizedMessage}"
                }
            } catch (e: Exception) {
                _errorMsg.value = "Crash: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStaffProfile(staffId: Int, fields: Map<String, String>, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val payload = fields.toMutableMap()
                payload["staff_id"] = staffId.toString()
                payload["institution_id"] = lastId.toString()
                
                val result = repository.updateStaffProfile(payload)
                if (result.isSuccess) {
                    val resp = result.getOrNull()
                    if (resp?.status == "success") {
                        onSuccess(resp.message ?: "Profile updated successfully")
                        fetchStaffProfile(staffId)
                    } else {
                        onError(resp?.message ?: "Failed to update profile")
                    }
                } else {
                    onError("Network Error: ${result.exceptionOrNull()?.localizedMessage}")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateInstitutionProfile(fields: Map<String, String>, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val payload = fields.toMutableMap()
                payload["institution_id"] = lastId.toString()
                
                val result = repository.updateInstitutionProfile(payload)
                if (result.isSuccess) {
                    val resp = result.getOrNull()
                    if (resp?.status == "success") {
                        onSuccess(resp.message ?: "Institution updated")
                        // Refresh profile to get updated inst data
                        val staffId = (_staffProfile.value?.basic?.get("id") as? Number)?.toInt() ?: 0
                        if (staffId > 0) fetchStaffProfile(staffId)
                    } else {
                        onError(resp?.message ?: "Failed to update institution")
                    }
                } else {
                    onError("Network Error: ${result.exceptionOrNull()?.localizedMessage}")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // CRUD for Sections
    fun saveSectionNode(action: String, staffId: Int, fields: Map<String, String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val payload = fields.toMutableMap()
                payload["staff_id"] = staffId.toString()
                
                val result = when(action) {
                    "CONTACT" -> repository.saveContact(payload)
                    "ACADEMIC" -> repository.saveAcademic(payload)
                    "EXPERIENCE" -> repository.saveExperience(payload)
                    "BANK" -> repository.saveBank(payload)
                    else -> Result.failure(Exception("Unknown Action"))
                }

                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess()
                    fetchStaffProfile(staffId)
                } else {
                    onError(result.getOrNull()?.message ?: "Operation failed")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Crash")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSectionNode(action: String, staffId: Int, id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = when(action) {
                    "CONTACT" -> repository.deleteContact(id, staffId)
                    "ACADEMIC" -> repository.deleteAcademic(id, staffId)
                    "EXPERIENCE" -> repository.deleteExperience(id, staffId)
                    "BANK" -> repository.deleteBank(id, staffId)
                    else -> Result.failure(Exception("Unknown Action"))
                }

                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess()
                    fetchStaffProfile(staffId)
                } else {
                    onError(result.getOrNull()?.message ?: "Delete failed")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Crash")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveInstitutionNode(action: String, staffId: Int, fields: Map<String, String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val payload = fields.toMutableMap()
                payload["institution_id"] = lastId.toString()
                
                val result = when(action) {
                    "POST" -> repository.saveInstPost(payload)
                    "BANK" -> repository.saveInstBank(payload)
                    "ASSET" -> repository.saveInstAsset(payload)
                    "FUND" -> repository.saveInstFund(payload)
                    else -> Result.failure(Exception("Unknown Action"))
                }

                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess()
                    fetchStaffProfile(staffId)
                } else {
                    onError(result.getOrNull()?.message ?: "Operation failed")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Crash")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteInstitutionNode(action: String, staffId: Int, id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = when(action) {
                    "POST" -> repository.deleteInstPost(id, lastId)
                    "BANK" -> repository.deleteInstBank(id, lastId)
                    "ASSET" -> repository.deleteInstAsset(id, lastId)
                    "FUND" -> repository.deleteInstFund(id, lastId)
                    else -> Result.failure(Exception("Unknown Action"))
                }

                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess()
                    fetchStaffProfile(staffId)
                } else {
                    onError(result.getOrNull()?.message ?: "Delete failed")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Crash")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getSavedData(): Map<String, Any?> {
        return mapOf(
            "user" to prefs.getString("user", ""),
            "pass" to prefs.getString("pass", ""),
            "last_inst" to prefs.getInt("last_inst", 0),
            "role" to prefs.getString("role", ""),
            "remember" to prefs.getBoolean("remember", false),
            "cnic" to prefs.getString("cnic", ""),
            "parent_pass" to prefs.getString("parent_pass", ""),
            "remember_parent" to prefs.getBoolean("remember_parent", false)
        )
    }

    fun fetchStudents(classId: Int = 0, sectionId: Int = 0, status: String = "active") {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMsg.value = ""
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.fetchStudents(lastId, classId, sectionId, status)
                if (result.isSuccess) {
                    _studentsData.value = result.getOrNull()
                } else {
                    _errorMsg.value = "Students Load Error: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMsg.value = "Students Crash: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun bulkSaveStudents(classId: Int, sectionId: Int, namesText: String, gender: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.bulkSaveStudents(lastId, classId, sectionId, namesText, gender)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Enrolled")
                    fetchStudents(classId, sectionId)
                } else {
                    onError(result.getOrNull()?.message ?: "Failed")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Crash")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchStudentProfile(studentId: Int) {
        _studentProfile.value = null
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMsg.value = ""
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.fetchStudentProfile(studentId, lastId)
                if (result.isSuccess) {
                    _studentProfile.value = result.getOrNull()
                } else {
                    _errorMsg.value = "Student Profile Error: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMsg.value = "Student Profile Crash: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchSchoolStructure() {
        viewModelScope.launch {
            try {
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.fetchSchoolStructure(lastId)
                if (result.isSuccess) {
                    _schoolStructure.value = result.getOrNull()
                }
            } catch (e: Exception) {}
        }
    }

    fun saveStudent(fields: Map<String, String>, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.saveStudent(fields, lastId)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Saved")
                    fetchStudents()
                } else {
                    onError(result.getOrNull()?.message ?: "Failed to save")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Crash")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markStudentAttendance(studentId: Int, status: String, date: String = "", onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.markAttendance(studentId, status, lastId, date)
                if (result.isSuccess) {
                    onSuccess()
                    fetchStudentProfile(studentId)
                    fetchStudents()
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteStudent(studentId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.deleteStudent(studentId, lastId)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess()
                    fetchStudents()
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun collectFee(studentId: Int, amount: Double, mode: String, category: String, month: String, year: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.collectFee(studentId, lastId, amount, mode, category, month, year)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Fee Collected")
                    fetchStudentProfile(studentId)
                } else {
                    onError(result.getOrNull()?.message ?: "Failed")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStudentRole(studentId: Int, role: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.updateStudentRole(studentId, lastId, role)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Role Updated")
                    fetchStudentProfile(studentId)
                } else {
                    onError(result.getOrNull()?.message ?: "Failed")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changeStudentStatus(studentId: Int, status: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.changeStudentStatus(studentId, lastId, status)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Status changed")
                    fetchStudentProfile(studentId)
                    fetchStudents()
                } else {
                    onError(result.getOrNull()?.message ?: "Failed")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun markStaffAttendance(staffId: Int, status: String, date: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.markStaffAttendance(staffId, lastId, status, date)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Marked")
                    fetchStaffProfile(staffId)
                    fetchStaff()
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun recordSalary(staffId: Int, amount: Double, month: String, year: String, mode: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.recordSalary(staffId, lastId, amount, month, year, mode)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Salary Recorded")
                    fetchStaffProfile(staffId)
                    fetchStaff()
                } else {
                    onError(result.getOrNull()?.message ?: "Failed")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStaffStatus(staffId: Int, status: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.updateStaffStatus(staffId, status)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Status Updated")
                    fetchStaffProfile(staffId)
                    fetchStaff()
                } else {
                    onError(result.getOrNull()?.message ?: "Failed")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _submissionStatus = MutableStateFlow<AttendanceSubmissionResponse?>(null)
    val submissionStatus = _submissionStatus.asStateFlow()

    fun fetchSubmissionStatus(date: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.getAttendanceSubmissionStatus(lastId, date)
                if (result.isSuccess) {
                    _submissionStatus.value = result.getOrNull()
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    fun markAttendanceSubmitted(classId: Int, sectionId: Int, date: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val body = mapOf(
                    "class_id" to classId.toString(),
                    "section_id" to sectionId.toString(),
                    "date" to date
                )
                val result = repository.markAttendanceSubmitted(body)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess()
                    fetchSubmissionStatus(date)
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    private val _attendanceRules = MutableStateFlow<Map<String, Any?>?>(null)
    val attendanceRules = _attendanceRules.asStateFlow()

    fun fetchAttendanceRules() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.getAttendanceRules(lastId)
                if (result.isSuccess) {
                    val json = result.getOrNull()?.string() ?: ""
                    val obj = org.json.JSONObject(json)
                    val map = mutableMapOf<String, Any?>()
                    val keys = obj.keys()
                    while(keys.hasNext()) {
                        val k = keys.next()
                        map[k] = obj.get(k)
                    }
                    _attendanceRules.value = map
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    fun saveAttendanceRules(fields: Map<String, String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.saveAttendanceRules(lastId, fields)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess()
                    fetchAttendanceRules()
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    private val _leaveAppeals = MutableStateFlow<Map<String, Any?>?>(null)
    val leaveAppeals = _leaveAppeals.asStateFlow()

    fun fetchLeaveAppeals() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.getLeaveAppeals(lastId)
                if (result.isSuccess) {
                    val json = result.getOrNull()?.string() ?: ""
                    val obj = org.json.JSONObject(json)
                    val map = mutableMapOf<String, Any?>()
                    val keys = obj.keys()
                    while(keys.hasNext()) {
                        val k = keys.next()
                        map[k] = obj.get(k)
                    }
                    _leaveAppeals.value = map
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    fun updateAppealStatus(id: Int, status: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.updateAppealStatus(lastId, id, status)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess()
                    fetchLeaveAppeals()
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    // ── Question Papers ───────────────────────────────────────────────────────

    private val _questionPapers = MutableStateFlow<QuestionPaperResponse?>(null)
    val questionPapers = _questionPapers.asStateFlow()

    fun fetchQuestionPapers(classId: Int = 0, subject: String = "", year: String = "", paperType: String = "") {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMsg.value = ""
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.fetchQuestionPapers(lastId, classId, subject, year, paperType)
                if (result.isSuccess) {
                    _questionPapers.value = result.getOrNull()
                } else {
                    _errorMsg.value = "Papers Load Error: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMsg.value = "Papers Crash: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveQuestionPaper(
        title: String, subject: String, classId: Int,
        year: String, totalMarks: String, paperType: String,
        onSuccess: (String) -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.saveQuestionPaper(lastId, title, subject, classId, year, totalMarks, paperType)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Paper saved successfully")
                    fetchQuestionPapers()
                } else {
                    onError(result.getOrNull()?.message ?: "Failed to save paper")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteQuestionPaper(paperId: Int, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.deleteQuestionPaper(paperId, lastId)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Deleted")
                    fetchQuestionPapers()
                } else {
                    onError(result.getOrNull()?.message ?: "Failed")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveSmartPaper(
        totalMarks: String,
        sections: List<com.example.wantuch.ui.components.Section>,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)

                // 1. Prepare Sections JSON
                val sectionsArray = org.json.JSONArray()
                sections.forEach { section ->
                    val sectionObj = org.json.JSONObject()
                    sectionObj.put("title", section.name)
                    sectionObj.put("marks", section.allocated_marks)

                    val questionsArray = org.json.JSONArray()
                    section.questions.forEach { question ->
                        if (question.text.isNotBlank()) {
                            val questionObj = org.json.JSONObject()
                            questionObj.put("text", question.text)
                            questionObj.put("marks", question.marks)
                            questionsArray.put(questionObj)
                        }
                    }
                    sectionObj.put("questions", questionsArray)
                    sectionsArray.put(sectionObj)
                }

                val sectionsJson = sectionsArray.toString()

                // 2. Call Repository
                val result = repository.saveSmartPaper(lastId, totalMarks, sectionsJson)

                if (result.isSuccess) {
                    val resp = result.getOrNull()
                    if (resp?.status == "success") {
                        val filePath = resp.file_path ?: ""
                        onSuccess(filePath, resp.message ?: "Question paper PDF generated successfully.")
                        fetchQuestionPapers()
                    } else {
                        onError(resp?.message ?: "Backend failed to generate PDF")
                    }
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Network error")
                }
            } catch (e: Exception) {
                onError("App Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearAttendanceSubmission(classId: Int, sectionId: Int, date: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val body = mapOf(
                    "class_id" to classId.toString(),
                    "section_id" to sectionId.toString(),
                    "date" to date
                )
                val result = repository.clearAttendanceSubmission(body)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess()
                    fetchSubmissionStatus(date)
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    fun downloadAttendanceCsv(
        dateFrom: String,
        dateTo: String,
        classSelection: String,
        onSuccess: (ByteArray, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.downloadAttendanceCsv(lastId, dateFrom, dateTo, classSelection)
                if (result.isSuccess) {
                    val bytes = result.getOrNull()?.bytes() ?: byteArrayOf()
                    val fileName = "attendance_${classSelection.replace("|", "_")}_${dateFrom}_to_${dateTo}.csv"
                    onSuccess(bytes, fileName)
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Download failed")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveBulkImportJson(
        jsonBody: String,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.saveBulkImportJson(lastId, jsonBody)
                if (result.isSuccess) {
                    val body = result.getOrNull()?.string() ?: "{}"
                    val json = org.json.JSONObject(body)
                    if (json.optString("status") == "success") {
                        onSuccess(json.optInt("processed", 0))
                    } else {
                        onError(json.optString("message", "Save failed"))
                    }
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Network error")
                }
            } catch (e: Exception) {
                onError("Crash: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _smartStatus = MutableStateFlow<SmartStatusResponse?>(null)
    val smartStatus = _smartStatus.asStateFlow()

    fun fetchSmartStatus() {
        viewModelScope.launch {
            val lastId = prefs.getInt("last_inst", 0)
            repository.getSmartStatus(lastId).onSuccess {
                _smartStatus.value = it
            }
        }
    }

    private val _faceSessions = MutableStateFlow<FaceSessionResponse?>(null)
    val faceSessions = _faceSessions.asStateFlow()

    fun fetchFaceSessions(limit: Int = 10) {
        viewModelScope.launch {
            val lastId = prefs.getInt("last_inst", 0)
            repository.getFaceSessions(lastId, limit).onSuccess {
                _faceSessions.value = it
            }
        }
    }

    fun enrollFace(userId: Int, category: String, bitmap: android.graphics.Bitmap, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                
                // Convert bitmap to base64
                val baos = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, baos)
                val base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT)
                
                val result = repository.enrollFace(lastId, userId, category, base64)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess(result.getOrNull()?.message ?: "Enrolled Successfully")
                } else {
                    onError(result.getOrNull()?.message ?: "Enrollment Failed")
                }
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _verificationResult = MutableStateFlow<VerifyFaceResponse?>(null)
    val verificationResult = _verificationResult.asStateFlow()

    fun verifyFace(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            try {
                // Local recognition first (Instant)
                val localResult = recognizeFaceLocally(bitmap)
                _verificationResult.value = localResult
                
                // If local match is found, we can optionally notify backend here
                // But localResult is enough for real-time UI
                
                /* OLD REFRESH: Backend fallback if needed
                val lastId = prefs.getInt("last_inst", 0)
                ...
                */
            } catch (e: Exception) {
                // Keep UI alive during errors
            }
        }
    }

    private val _syllabusData = MutableStateFlow<SyllabusResponse?>(null)
    val syllabusData = _syllabusData.asStateFlow()

    fun fetchSyllabus(classId: Int = 0, sectionId: Int = 0, subjectId: Int = 0) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.fetchSyllabus(lastId, classId, sectionId, subjectId)
                if (result.isSuccess) {
                    _syllabusData.value = result.getOrNull()
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    fun updateSyllabusStatus(id: Int, status: String, classId: Int, sectionId: Int, subjectId: Int) {
        viewModelScope.launch {
            val result = repository.updateSyllabusStatus(id, status)
            if (result.isSuccess) {
                fetchSyllabus(classId, sectionId, subjectId)
            }
        }
    }

    fun saveFullSyllabus(payload: SyllabusWizardPayload, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.saveFullSyllabus(payload)
                if (result.isSuccess) {
                    fetchSyllabus(payload.class_id, payload.section_id, payload.subject_id)
                    onComplete()
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    fun editSyllabusChapter(payload: EditChapterPayload, classId: Int, sectionId: Int, subjectId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.editSyllabusChapter(payload)
                if (result.isSuccess) {
                    fetchSyllabus(classId, sectionId, subjectId)
                    onComplete()
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    fun deleteSyllabusTopic(id: Int, classId: Int, sectionId: Int, subjectId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.deleteSyllabusTopic(id, lastId)
                if (result.isSuccess) {
                    fetchSyllabus(classId, sectionId, subjectId)
                }
            } catch (e: Exception) {}
            finally { _isLoading.value = false }
        }
    }

    fun fetchStudentsForPromotion(classId: Int, sectionId: Int, criteria: String, year: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val instId = getInstitutionId()
                val result = repository.getStudentsForPromotion(instId, classId, sectionId, criteria, year)
                result.onSuccess { _promotionData.value = it }
            } catch (e: Exception) { Log.e("Wantuch", "Fetch Promo Error: ${e.message}") }
            finally { _isLoading.value = false }
        }
    }

    fun promoteStudent(studentId: Int, force: Boolean, targetClassId: Int, sourceClassId: Int, sourceSectionId: Int, year: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val instId = getInstitutionId()
                val promoArray = listOf(mapOf("student_id" to studentId, "force" to force))
                val promoJson = com.google.gson.Gson().toJson(promoArray)
                val result = repository.promoteStudents(instId, promoJson, targetClassId, sourceClassId, sourceSectionId, year)
                result.onSuccess { fetchStudentsForPromotion(sourceClassId, sourceSectionId, "percentage", year) }
            } catch (e: Exception) { Log.e("Wantuch", "Promote Error: ${e.message}") }
            finally { _isLoading.value = false }
        }
    }

    fun shiftPromotableStudents(studentIds: List<Int>, targetClassId: Int, sourceClassId: Int, sourceSectionId: Int, year: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val instId = getInstitutionId()
                val idJson = com.google.gson.Gson().toJson(studentIds)
                val result = repository.shiftStudents(instId, idJson, targetClassId, sourceClassId, sourceSectionId, year)
                result.onSuccess { fetchStudentsForPromotion(sourceClassId, sourceSectionId, "percentage", year) }
            } catch (e: Exception) { Log.e("Wantuch", "Shift Error: ${e.message}") }
            finally { _isLoading.value = false }
        }
    }

    /**
     * Matches a seen biometric bitmap against the entire local database.
     * Returns the name and ID of the best match if confidence > threshold.
     */
    fun recognizeFaceLocally(bitmap: android.graphics.Bitmap): VerifyFaceResponse {
        val currentEmbedding = faceEngine.getEmbedding(bitmap) ?: return VerifyFaceResponse("error", false, message = "No face detected")
        
        var bestMatch: FaceEmbeddingEntity? = null
        var minDistance = Float.MAX_VALUE
        val threshold = 0.75f // Tightened (was 0.82) for better accuracy

        // Use pre-parsed database for high speed
        for (pair in _parsedFaceDB) {
            val dist = faceEngine.calculateL2Distance(currentEmbedding, pair.second)
            if (dist < minDistance) {
                minDistance = dist
                bestMatch = pair.first
            }
        }

        return if (bestMatch != null && minDistance <= threshold) {
            VerifyFaceResponse(
                status = "success",
                matched = true,
                name = bestMatch.userName,
                user_id = bestMatch.userId,
                role = bestMatch.category,
                confidence = String.format("Acc: %.2f", (1.0f - minDistance).coerceIn(0f, 1f) * 100) + "%"
            )
        } else {
            VerifyFaceResponse("success", false, message = "User not recognized", confidence = String.format("Diff: %.2f", minDistance))
        }
    }

    /**
     * Synchronous version — called directly from camera thread for per-face processing.
     * Identical logic to recognizeFaceLocally but safe to call without a coroutine.
     */
    fun recognizeFaceLocallySync(bitmap: android.graphics.Bitmap): VerifyFaceResponse =
        recognizeFaceLocally(bitmap)

    fun addRecognizedToSession(userId: Int, userName: String) {
        if (!_recognizedInSession.value.containsKey(userId)) {
            _recognizedInSession.value = _recognizedInSession.value + (userId to userName)
        }
    }

    fun clearRecognizedSession() {
        _recognizedInSession.value = emptyMap()
    }

    fun submitRecognizedAttendance(date: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val users = _recognizedInSession.value.keys
            val lastId = prefs.getInt("last_inst", 0)
            var count = 0
            users.forEach { userId ->
                // Basic implementation: mark each as present
                repository.markAttendance(userId, "Present", lastId, date)
                count++
            }
            onComplete(count)
        }
    }

    fun enrollFaceLocally(userId: Int, category: String, userName: String, bitmap: android.graphics.Bitmap, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val embedding = faceEngine.getEmbedding(bitmap)
                if (embedding != null) {
                    val entity = FaceEmbeddingEntity(
                        userId = userId,
                        category = category,
                        userName = userName,
                        embedding = com.google.gson.Gson().toJson(embedding)
                    )
                    database.faceEmbeddingDao().insertEmbedding(entity)
                    // _localFaceDB collection will trigger refresh and re-parse _parsedFaceDB
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("WantuchVM", "Enroll error: ${e.message}")
                onComplete(false)
            }
        }
    }

    fun exportDatabase(tables: List<String>, dataOnly: Boolean, context: Context, onProgress: (Float) -> Unit, onComplete: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val tablesStr = tables.joinToString(",")
                val dataOnlyParam = if (dataOnly) "1" else "0"
                
                val response = repository.genericGet("EXPORT_DATABASE", mapOf("tables" to tablesStr, "data_only" to dataOnlyParam))
                if (response.isSuccess) {
                    val body = response.getOrNull()
                    if (body != null) {
                        val length = body.contentLength()
                        val source = body.source()
                        val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "db_backup_${System.currentTimeMillis()}.sql")
                        java.io.FileOutputStream(file).use { output ->
                            val buffer = okio.Buffer()
                            var totalRead = 0L
                            while (true) {
                                val read = source.read(buffer, 8192)
                                if (read == -1L) break
                                val bytes = buffer.readByteArray()
                                output.write(bytes)
                                totalRead += read
                                val progress = if (length > 0) totalRead.toFloat() / length else -1f
                                launch(kotlinx.coroutines.Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            }
                        }
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Database exported to Downloads folder", android.widget.Toast.LENGTH_LONG).show()
                        }
                    } else {
                        throw Exception("Empty response body")
                    }
                } else {
                    val err = response.exceptionOrNull()
                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Export Failed: ${err?.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Crash: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } finally {
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    fun importDatabase(uri: android.net.Uri, context: Context, onProgress: (Float) -> Unit, onComplete: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    launch(kotlinx.coroutines.Dispatchers.Main) { android.widget.Toast.makeText(context, "Failed to read file", android.widget.Toast.LENGTH_SHORT).show() }
                    return@launch
                }
                val bytes = inputStream.readBytes()
                inputStream.close()
                
                val filename = "upload.sql"
                val result = repository.importDatabase(bytes, filename) { p ->
                    launch(kotlinx.coroutines.Dispatchers.Main) { onProgress(p) }
                }
                
                if (result.isSuccess) {
                    val resp = result.getOrNull()
                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        if (resp?.status == "success") {
                            android.widget.Toast.makeText(context, resp.message ?: "Import Successful", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            android.widget.Toast.makeText(context, "Import failed: ${resp?.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Import failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Crash: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } finally {
                launch(kotlinx.coroutines.Dispatchers.Main) { onComplete() }
            }
        }
    }

    // ── Assignments (Homework) Logic ──────────────────────────────────────────

    private val _assignments = MutableStateFlow<AssignmentResponse?>(null)
    val assignments: StateFlow<AssignmentResponse?> = _assignments

    private val _assignmentSubmissions = MutableStateFlow<List<AssignmentSubmission>>(emptyList())
    val assignmentSubmissions: StateFlow<List<AssignmentSubmission>> = _assignmentSubmissions

    fun fetchAssignments() {
        viewModelScope.launch {
            repository.fetchAssignments()
                .onSuccess { _assignments.value = it }
                .onFailure { Log.e("WantuchVM", "Fetch Assignments error: ${it.message}") }
        }
    }

    fun createAssignment(
        classId: Int,
        sectionId: Int,
        subjectId: Int,
        title: String,
        description: String,
        dueDate: String,
        uri: android.net.Uri?,
        context: Context,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var fileBytes: ByteArray? = null
                var filename: String? = null
                
                if (uri != null) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    fileBytes = inputStream?.readBytes()
                    inputStream?.close()
                    
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) filename = it.getString(nameIndex)
                        }
                    }
                }

                repository.createAssignment(classId, sectionId, subjectId, title, description, dueDate, fileBytes, filename, onProgress)
                    .onSuccess {
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "Assignment Created Path: History", Toast.LENGTH_LONG).show()
                            fetchAssignments()
                            onComplete(true)
                        }
                    }
                    .onFailure { e ->
                        launch(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            onComplete(false)
                        }
                    }
            } catch (e: Exception) {
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }
        }
    }

    fun reviewSubmission(subId: Int, status: String, feedback: String, context: Context) {
        viewModelScope.launch {
            repository.reviewSubmission(subId, status, feedback)
                .onSuccess {
                    Toast.makeText(context, "Review Saved", Toast.LENGTH_SHORT).show()
                    fetchAssignments()
                }
                .onFailure { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    fun fetchSubmissionDetails(assignmentId: Int) {
        viewModelScope.launch {
            repository.fetchSubmissionDetails(assignmentId)
                .onSuccess { _assignmentSubmissions.value = it.data ?: emptyList() }
                .onFailure { Log.e("WantuchVM", "Fetch Submission Details error: ${it.message}") }
        }
    }

    fun updateAssignment(id: Int, title: String, description: String, dueDate: String, context: Context) {
        viewModelScope.launch {
            repository.updateAssignment(id, title, description, dueDate)
                .onSuccess {
                    Toast.makeText(context, "Assignment Updated", Toast.LENGTH_SHORT).show()
                    fetchAssignments()
                }
                .onFailure { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    // ── Study Planner ────────────────────────────────────────────────────────────

    private val _studySubjects = MutableStateFlow<List<StudySubject>>(emptyList())
    val studySubjects = _studySubjects.asStateFlow()

    private val _plannerData = MutableStateFlow<List<StudyPlanItem>>(emptyList())
    val plannerData = _plannerData.asStateFlow()

    fun fetchStudySubjects(classId: Int, sectionId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.getStudySubjects(lastId, classId, sectionId)
                if (result.isSuccess) {
                    _studySubjects.value = result.getOrNull()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                _errorMsg.value = e.message ?: "Failed to fetch subjects"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchPlannerData(classId: Int, sectionId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.getPlannerData(lastId, classId, sectionId)
                if (result.isSuccess) {
                    _plannerData.value = result.getOrNull()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                _errorMsg.value = e.message ?: "Failed to fetch planner data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun savePlannerConfig(classId: Int, sectionId: Int, config: List<StudyPlanItem>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lastId = prefs.getInt("last_inst", 0)
                val json = com.google.gson.Gson().toJson(config)
                val result = repository.savePlannerConfig(lastId, classId, sectionId, json)
                if (result.isSuccess && result.getOrNull()?.status == "success") {
                    onSuccess()
                    fetchPlannerData(classId, sectionId)
                } else {
                    _errorMsg.value = result.getOrNull()?.message ?: "Save Failed"
                }
            } catch (e: Exception) {
                _errorMsg.value = e.message ?: "Crash"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun completeTopic(subjectId: Int, topicName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val lastId = prefs.getInt("last_inst", 0)
                val result = repository.completeTopic(lastId, subjectId, topicName)
                if (result.isSuccess) {
                    onSuccess()
                }
            } catch (e: Exception) {}
        }
    }
}
