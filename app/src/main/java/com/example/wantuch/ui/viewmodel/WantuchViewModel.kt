package com.example.wantuch.ui.viewmodel

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

class WantuchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = com.example.wantuch.data.local.database.WantuchDatabase.getDatabase(application)
    private val repository = WantuchRepository(BASE_URL, database.wantuchDao())
    private val prefs = application.getSharedPreferences("wantuch_prefs", Context.MODE_PRIVATE)

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

    init {
        // Collect cached data to show immediately when app starts/switches institution
        viewModelScope.launch {
            val lastId = prefs.getInt("last_inst", 0)
            if (lastId != 0) {
                launch {
                    repository.getLocalStudents(lastId).collect { entities ->
                        if (entities.isNotEmpty() && _studentsData.value == null) {
                            val domainStudents = entities.map { it.toDomain() }
                            _studentsData.value = StudentResponse(status = "success", students = domainStudents)
                        }
                    }
                }
                launch {
                    repository.getLocalStaff(lastId).collect { entities ->
                        if (entities.isNotEmpty() && _staffData.value == null) {
                            val teaching = entities.filter { it.role.contains("Teaching", ignoreCase = true) }.map { it.toDomain() }
                            val nonTeaching = entities.filter { !it.role.contains("Teaching", ignoreCase = true) }.map { it.toDomain() }
                            _staffData.value = StaffResponse(status = "success", teaching_staff = teaching, non_teaching_staff = nonTeaching)
                        }
                    }
                }
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
}
