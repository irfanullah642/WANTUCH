package com.example.wantuch.ui.components

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.*
import coil.compose.AsyncImage
import com.example.wantuch.domain.model.StudentMember
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import org.json.JSONArray
import org.json.JSONObject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentManagementScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    val data by viewModel.studentsData.collectAsState()
    val structure by viewModel.schoolStructure.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf(0) }
    var selectedSectionId by remember { mutableStateOf(0) }
    var showFilter by remember { mutableStateOf(false) }
    var showAddModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchSchoolStructure()
        viewModel.fetchStudents(0, 0) // Load all students immediately to show accurate totals
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // New Sleek Header
            Box(Modifier.fillMaxWidth().background(cardColor).statusBarsPadding().padding(horizontal = 15.dp, vertical = 20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, Modifier.background(Color.Black.copy(0.05f), CircleShape)) {
                            Icon(Icons.Default.ArrowBack, null, tint = textColor)
                        }
                        Spacer(Modifier.width(15.dp))
                        Column(Modifier.weight(1f)) {
                            Text("STUDENTS", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Manage Admissions & Withdrawals", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { viewModel.fetchStudents(selectedClassId, selectedSectionId) }, Modifier.background(Color.Black.copy(0.05f), CircleShape)) {
                            Icon(Icons.Default.Refresh, null, tint = textColor)
                        }
                    }

                    // Default selection removed as per user request

                    Spacer(Modifier.height(20.dp))

                    // Search & Filter Row
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name or username...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f),
                                unfocusedContainerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }
                        )
                        IconButton(
                            onClick = { showFilter = true },
                            modifier = Modifier.size(50.dp).background(if(selectedClassId > 0) Color(0xFF3B82F6) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.FilterList, null, tint = if(selectedClassId > 0) Color.White else Color.Gray)
                        }
                    }
                }
            }

            if (isLoading && data == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            } else {
                val students = data?.students?.filter {
                    it.name.contains(searchQuery, true) || it.username.contains(searchQuery, true)
                } ?: emptyList()

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(15.dp, 10.dp, 15.dp, 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Stats Row — always visible, uses live data or dashboard fallback
                    item {
                        val totalStudents = data?.stats?.get("total")?.toString()
                            ?: dashboardData?.stats?.get("students")?.toString()?.toDoubleOrNull()?.toInt()?.toString()
                            ?: "0"
                        val presentStudents = data?.stats?.get("present")?.toString() ?: "0"
                        val absentStudents = data?.stats?.get("absent")?.toString() ?: "0"
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudentHeaderStat("TOTAL", totalStudents, Color(0xFF6366F1), isDark, Modifier.weight(1f))
                            StudentHeaderStat("PRESENT", presentStudents, Color(0xFF10B981), isDark, Modifier.weight(1f))
                            StudentHeaderStat("ABSENT", absentStudents, Color(0xFFEF4444), isDark, Modifier.weight(1f))
                        }
                    }

                    if (students.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                val msg = if (selectedClassId == 0) "PLEASE SELECT A CLASS TO VIEW STUDENTS" else "No students found matches your search."
                                Text(msg, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(students) { student ->
                        StudentListCard(student, isDark) { onOpenProfile((student.id as? Number)?.toInt() ?: 0) }
                    }
                }
            }
        }

        // Add Student FAB
        Box(Modifier.fillMaxSize().padding(20.dp), Alignment.BottomEnd) {
            FloatingActionButton(
                onClick = { showAddModal = true },
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    }

    // Enhanced Tabbed Enrollment Modal
    if (showAddModal) {
        EnrollStudentModal(
            viewModel = viewModel,
            structure = structure,
            isDark = isDark,
            onDismiss = { showAddModal = false }
        )
    }

    // Filter Dialog with Sections
    if (showFilter) {
        AlertDialog(
            onDismissRequest = { showFilter = false },
            containerColor = cardColor,
            title = { Text("Filter by Class", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(onClick = { selectedClassId = 0; selectedSectionId = 0; viewModel.fetchStudents(0, 0); showFilter = false }) {
                        Text("ALL CLASSES", color = if(selectedClassId == 0) Color(0xFF3B82F6) else Color.Gray, fontWeight = FontWeight.Black)
                    }
                    structure?.classes?.forEach { cls ->
                        Column {
                            TextButton(onClick = { selectedClassId = cls.id; selectedSectionId = 0; viewModel.fetchStudents(cls.id, 0); showFilter = false }) {
                                Text(cls.name.uppercase(), color = if(selectedClassId == cls.id && selectedSectionId == 0) Color(0xFF3B82F6) else textColor, fontWeight = FontWeight.Bold)
                            }
                            cls.sections?.forEach { sec ->
                                TextButton(
                                    onClick = { selectedClassId = cls.id; selectedSectionId = sec.id; viewModel.fetchStudents(cls.id, sec.id); showFilter = false },
                                    modifier = Modifier.padding(start = 20.dp)
                                ) {
                                    Text(sec.name.uppercase(), color = if(selectedSectionId == sec.id) Color(0xFF3B82F6) else Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFilter = false }) { Text("CLOSE") } }
        )
    }
}

@Composable
fun StudentHeaderStat(label: String, value: String, accent: Color, isDark: Boolean, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(if(isDark) Color(0xFF1E293B) else Color.White).shadow(2.dp).padding(12.dp)) {
        Column {
            Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun StudentListCard(st: StudentMember, isDark: Boolean, onClick: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(0.05f)), Alignment.Center) {
                if (!st.profile_pic.isNullOrEmpty()) {
                    AsyncImage(model = st.profile_pic, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Text(st.initials, color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(st.name, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text("${st.class_section} • #${st.class_no}", color = Color.Gray, fontSize = 11.sp)
                Text(st.username, color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.background(
                when(st.marked) {
                    "Present" -> Color(0xFF10B981).copy(0.1f)
                    "Absent" -> Color(0xFFEF4444).copy(0.1f)
                    "Leave" -> Color(0xFFF59E0B).copy(0.1f)
                    else -> Color.Gray.copy(0.1f)
                }, RoundedCornerShape(8.dp)
            ).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(st.marked.uppercase(),
                    color = when(st.marked) {
                        "Present" -> Color(0xFF10B981)
                        "Absent" -> Color(0xFFEF4444)
                        "Leave" -> Color(0xFFF59E0B)
                        else -> Color.Gray
                    }, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun StudentProfileScreen(
    studentId: Int,
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    val profile by viewModel.studentProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        Triple("PROFILE", Icons.Default.AccountCircle, Color(0xFF3B82F6)),
        Triple("ATTEN", Icons.Default.EventAvailable, Color(0xFF10B981)),
        Triple("RESULT", Icons.Default.Assignment, Color(0xFF8B5CF6)),
        Triple("FEE", Icons.Default.Payments, Color(0xFFF59E0B)),
        Triple("ROLE", Icons.Default.Badge, Color(0xFF6366F1)),
        Triple("DETAILS", Icons.Default.Description, Color(0xFFEC4899)),
        Triple("BIO", Icons.Default.Fingerprint, Color(0xFF00F3FF)),
        Triple("STRUCK OFF", Icons.Default.Block, Color(0xFFEF4444))
    )

    val localContext = LocalContext.current
    LaunchedEffect(studentId) {
        viewModel.fetchStudentProfile(studentId)
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // Cool Profile Header
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                // Cover
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF6366F1)))))

                Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = onBack, Modifier.background(Color.White.copy(0.1f), CircleShape)) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        IconButton(onClick = { /* More options */ }, Modifier.background(Color.White.copy(0.1f), CircleShape)) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(85.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.2f)).padding(3.dp)) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)).background(Color.White)) {
                                if (profile?.basic?.get("profile_pic")?.toString()?.isNotEmpty() == true) {
                                    AsyncImage(model = profile?.basic?.get("profile_pic").toString(), contentDescription = null, contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.fillMaxSize().padding(15.dp))
                                }
                            }
                        }
                        Spacer(Modifier.width(15.dp))
                        Column {
                            Text(profile?.basic?.get("full_name")?.toString() ?: "Loading...", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Text("${profile?.basic?.get("class_name")} • ${profile?.basic?.get("section_name")}", color = Color.White.copy(0.8f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("CLASS NO: ${profile?.basic?.get("class_no")}", color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Sleek Text-Only Tab Grid
            Column(Modifier.fillMaxWidth().background(cardColor).padding(10.dp)) {
                tabs.chunked(4).forEach { rowTabs ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTabs.forEach { (title, _, color) ->
                            val index = tabs.indexOfFirst { it.first == title }
                            val isSelected = selectedTab == index

                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if(isSelected) color else Color.Transparent)
                                    .clickable { selectedTab = index },
                                Alignment.Center
                            ) {
                                Text(
                                    title,
                                    color = if(isSelected) Color.White else Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    if(tabs.indexOf(rowTabs.last()) < tabs.size - 1) Spacer(Modifier.height(8.dp))
                }
            }

            if (isLoading && profile == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            } else {
                Box(Modifier.weight(1f).padding(15.dp)) {
                    when (selectedTab) {
                        0 -> StudentIdentityForm(profile?.basic ?: emptyMap(), isDark) { fields ->
                            val sid = (profile?.basic?.get("id") as? Number)?.toInt() ?: 0
                            val mutableFields = fields.toMutableMap()
                            mutableFields["id"] = sid.toString()
                            viewModel.saveStudent(mutableFields,
                                onSuccess = { msg -> Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show() },
                                onError = { err -> Toast.makeText(localContext, err, Toast.LENGTH_SHORT).show() }
                            )
                        }
                        1 -> StudentAttendanceTab(profile?.stats ?: emptyMap(), isDark, onMark = { status, date ->
                            viewModel.markStudentAttendance(studentId, status, date) {
                                Toast.makeText(localContext, "Attendance Marked: $status", Toast.LENGTH_SHORT).show()
                            }
                        })
                        2 -> StudentResultsTab(profile?.stats ?: emptyMap(), isDark)
                        3 -> StudentFeeTab(profile?.stats ?: emptyMap(), isDark) { amt, mode, cat, mon, yr ->
                            viewModel.collectFee(studentId, amt, mode, cat, mon, yr,
                                onSuccess = { msg -> Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show() },
                                onError = { err -> Toast.makeText(localContext, err, Toast.LENGTH_SHORT).show() }
                            )
                        }
                        4 -> StudentRoleTab(profile?.basic ?: emptyMap(), isDark) { role ->
                            viewModel.updateStudentRole(studentId, role,
                                onSuccess = { msg -> Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show() },
                                onError = { err -> Toast.makeText(localContext, err, Toast.LENGTH_SHORT).show() }
                            )
                        }
                        5 -> StudentDetailsTab(profile?.basic ?: emptyMap(), isDark)
                        6 -> StudentBiometricTab(profile?.basic ?: emptyMap(), isDark)
                        7 -> StudentStruckOffTab(profile?.basic ?: emptyMap(), isDark) { status ->
                            viewModel.changeStudentStatus(studentId, status,
                                onSuccess = { msg -> Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show() },
                                onError = { err -> Toast.makeText(localContext, err, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
            }
        }

        // Delete Fab
        Box(Modifier.fillMaxSize().padding(20.dp), Alignment.BottomEnd) {
            FloatingActionButton(
                onClick = {
                    viewModel.deleteStudent(studentId) {
                        Toast.makeText(localContext, "Student Deleted", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Delete, null)
            }
        }
    }
}

@Composable
fun StudentIdentityForm(basic: Map<String, Any?>, isDark: Boolean, isStaff: Boolean = false, onUpdate: (Map<String, String>) -> Unit) {
    val formState = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(basic) {
        formState.clear()
        basic.forEach { (k, v) -> formState[k] = v?.toString() ?: "" }
        
        // Auto-bridge father_cnic to parent_cnic for backwards compatibility with raw JSON
        if (!isStaff) {
            val fCnic = basic["father_cnic"]?.toString()?.takeIf { it.isNotBlank() }
            if (fCnic != null) formState["parent_cnic_no"] = fCnic
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        ProfileDataSection("Identity & Account", isDark) {
            val currentPass = formState["password"] ?: formState["password_plain"] ?: ""
            ProfileEditableField("Full Name", formState["full_name"], isDark) { formState["full_name"] = it }
            ProfileEditableField("Username", formState["username"], isDark) { formState["username"] = it }
            ProfileEditableField("Password", currentPass, isDark) { 
                formState["password"] = it
                formState["password_plain"] = it
            }
            ProfileEditableField("Father Name", formState["father_name"], isDark) { formState["father_name"] = it }
            ProfileDropdownField("Gender", formState["gender"], listOf("Male", "Female"), isDark) { formState["gender"] = it }
        }

        ProfileDataSection("Record Details", isDark) {
            if (isStaff) {
                ProfileDateField("Date of Birth", formState["dob"], isDark) { formState["dob"] = it }
                ProfileEditableField("CNIC", formState["cnic"], isDark) { formState["cnic"] = it }
                ProfileEditableField("Tribe / Cast", formState["tribe"], isDark) { formState["tribe"] = it }
                ProfileDropdownField("Status", formState["status"], listOf("active", "resigned", "suspended"), isDark) { formState["status"] = it }
            } else {
                ProfileEditableField("Admission No", formState["adm_no"], isDark) { formState["adm_no"] = it }
                ProfileDateField("Admission Date", formState["date_of_admission"], isDark) { formState["date_of_admission"] = it }
                ProfileEditableField("Class No / Roll No", formState["class_no"], isDark) { formState["class_no"] = it }
                ProfileDropdownField("Status", formState["status"], listOf("active", "struck_off", "withdrawn", "graduated"), isDark) { formState["status"] = it }
            }
        }

        ProfileDataSection("Contact & Social", isDark) {
            ProfileEditableField("WhatsApp", formState["whatsapp_no"], isDark) { formState["whatsapp_no"] = it }
            if (isStaff) {
                ProfileEditableField("Facebook Link", formState["facebook_link"], isDark) { formState["facebook_link"] = it }
            } else {
                ProfileEditableField("Parent CNIC", formState["parent_cnic_no"], isDark) { formState["parent_cnic_no"] = it }
            }
            ProfileEditableField("Residential Address", formState["address"], isDark) { formState["address"] = it }
        }

        Button(
            onClick = { onUpdate(formState.toMap()) },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isStaff) "SAVE STAFF UPDATES" else "SAVE STUDENT UPDATES", fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentAttendanceTab(stats: Map<String, Any?>, isDark: Boolean, onMark: (String, String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    val month = (stats["month"] as? Map<String, Any?>) ?: emptyMap()
    val year  = (stats["year"]  as? Map<String, Any?>) ?: emptyMap()

    // Calendar data from backend
    val rawCalendar = stats["calendar"]
    val backendCalendar: Map<Int, String> = when (rawCalendar) {
        is Map<*, *> -> rawCalendar.entries
            .mapNotNull { (k, v) -> k.toString().toIntOrNull()?.let { it to (v?.toString() ?: "") } }
            .toMap()
        else -> emptyMap()
    }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        // ── Calendar Card ──────────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(16.dp)) {
                CalendarGrid(
                    initialStatus = backendCalendar,
                    isDark = isDark,
                    onMark = onMark
                )
            }
        }

        // ── Monthly Stats ──────────────────────────────────────────────────────────
        Text("MONTHLY STATS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("PRESENT", month["present"]?.toString() ?: "0", Color(0xFF10B981), Modifier.weight(1f), isDark)
            StatBox("ABSENT",  month["absent"]?.toString()  ?: "0", Color(0xFFEF4444), Modifier.weight(1f), isDark)
            StatBox("LEAVE",   month["leave"]?.toString()   ?: "0", Color(0xFFFFB300), Modifier.weight(1f), isDark)
        }

        // ── Academic Year ──────────────────────────────────────────────────────────
        Text("ACADEMIC YEAR", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("PRESENT", year["present"]?.toString() ?: "0", Color(0xFF10B981), Modifier.weight(1f), isDark)
            StatBox("ABSENT",  year["absent"]?.toString()  ?: "0", Color(0xFFEF4444), Modifier.weight(1f), isDark)
            StatBox("LEAVE",   year["leave"]?.toString()   ?: "0", Color(0xFFFFB300), Modifier.weight(1f), isDark)
        }

        StatBox("All Time Days", year["total"]?.toString() ?: "0", Color(0xFF3B82F6), Modifier.fillMaxWidth(), isDark)

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun CalendarGrid(initialStatus: Map<Int, String>, isDark: Boolean, onMark: (String, String) -> Unit) {
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    val calendarState = remember(initialStatus) {
        mutableStateMapOf<Int, String>().also { it.putAll(initialStatus) }
    }

    val statusCycle = listOf(null, "Present", "Absent", "Leave", "Short", "Public Holiday")
    fun nextStatus(current: String?): String? {
        val idx = statusCycle.indexOf(current)
        return statusCycle[(idx + 1) % statusCycle.size]
    }

    fun statusColor(s: String?): Color = when(s) {
        "Present" -> Color(0xFF10B981)
        "Absent"  -> Color(0xFFEF4444)
        "Public Holiday", "Leave" -> Color(0xFFFFB300)
        "Short"   -> Color(0xFF6366F1)
        else      -> Color(0xFF334155)
    }
    fun statusLabel(s: String?): String = when(s) {
        "Present" -> "P"
        "Absent"  -> "A"
        "Leave"   -> "L"
        "Short"   -> "S"
        "Public Holiday" -> "PH"
        else      -> ""
    }

    val todayDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)

    val calForMeta = java.util.Calendar.getInstance()
    val daysInMonth = calForMeta.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    calForMeta.set(java.util.Calendar.DAY_OF_MONTH, 1)
    val firstWeekday = calForMeta.get(java.util.Calendar.DAY_OF_WEEK) - 1
    val monthYearLabel = java.text.SimpleDateFormat("MMMM yyyy").format(calForMeta.time)

    Column {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("MARK ATTENDANCE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(monthYearLabel, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))

        // Day Names
        Row(Modifier.fillMaxWidth()) {
            listOf("SUN","MON","TUE","WED","THU","FRI","SAT").forEach { d ->
                Box(Modifier.weight(1f), Alignment.Center) {
                    Text(d, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val totalCells = firstWeekday + daysInMonth
        val rowsCount = (totalCells + 6) / 7
        for (row in 0 until rowsCount) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstWeekday + 1
                    if (day < 1 || day > daysInMonth) {
                        Spacer(Modifier.weight(1f).height(44.dp))
                    } else {
                        val status = calendarState[day]
                        val isToday = (day == todayDay)
                        val isFuture = (day > todayDay)
                        val isSunday = (col == 0)
                        val isFriday = (col == 5)

                        val bgColor = when {
                            isFuture -> Color(0xFF1E293B).copy(if (isDark) 0.6f else 0.08f)
                            status != null -> statusColor(status)
                            isSunday -> Color(0xFF452726)
                            isFriday -> Color(0xFF112942)
                            else -> Color(0xFF1E293B).copy(if (isDark) 0.15f else 0.08f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(bgColor)
                                .border(width = if (isToday) 2.dp else 0.dp, color = if (isToday) Color.White.copy(0.8f) else Color.Transparent, shape = RoundedCornerShape(10.dp))
                                .then(if (!isFuture) Modifier.clickable {
                                    val next = nextStatus(calendarState[day])
                                    val calInst = java.util.Calendar.getInstance()
                                    calInst.set(java.util.Calendar.DAY_OF_MONTH, day)
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd").format(calInst.time)
                                    if (next == null) {
                                        calendarState.remove(day)
                                        onMark("Delete", dateStr)
                                    } else {
                                        calendarState[day] = next
                                        onMark(next, dateStr)
                                    }
                                } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = day.toString(), color = if(isFuture) Color.Gray.copy(0.5f) else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                if (!isFuture && status != null) {
                                    Text(text = statusLabel(status), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
            if (row < rowsCount - 1) Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(14.dp))

        // Legend
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Present" to "P", "Absent" to "A", "Leave" to "L", "Short" to "S", "Holiday" to "PH").forEach { (lbl, code) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor(if(lbl=="Holiday") "Public Holiday" else lbl)))
                    Spacer(Modifier.width(3.dp))
                    Text(code, color = statusColor(if(lbl=="Holiday") "Public Holiday" else lbl), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}


@Composable
fun StudentResultsTab(stats: Map<String, Any?>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("AVG SCORE", "${stats["exam_avg"] ?: "0"}%", Color(0xFF3B82F6), Modifier.weight(1f), isDark)

            val status = if((stats["exam_avg"] as? Number)?.toDouble() ?: 0.0 >= 40) "PASSING" else "FAILING"
            val statusColor = if(status == "PASSING") Color(0xFF10B981) else Color(0xFFEF4444)
            StatBox("STATUS", status, statusColor, Modifier.weight(1f), isDark)
        }

        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardColor).padding(20.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("LATEST RESULT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(15.dp))
                Text(stats["last_exam"]?.toString() ?: "No exam record found yet for this student.", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Student performance is calculated based on recent subject tests and terminal examinations.", color = Color.Gray, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("PERFORMANCE INSIGHTS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 5.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                listOf("Concept Clarity" to 85, "Attendance Link" to 92, "Assignment Submission" to 78).forEach { (label, pct) ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("$pct%", color = Color(0xFF3B82F6), fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(4.dp).background(Color.Gray.copy(0.1f), CircleShape)) {
                            Box(Modifier.fillMaxWidth(pct/100f).fillMaxHeight().background(Color(0xFF3B82F6), CircleShape))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentFeeTab(stats: Map<String, Any?>, isDark: Boolean, onSave: (Double, String, String, String, String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    val currentMonth = java.text.SimpleDateFormat("MMMM").format(java.util.Date())
    val currentYear = java.text.SimpleDateFormat("yyyy").format(java.util.Date())

    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMode by remember { mutableStateOf("Cash") }
    var selectedCategory by remember { mutableStateOf("All Fees") }
    var amountText by remember { mutableStateOf("") }

    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val years = (2020..2030).map { it.toString() }
    val modes = listOf("Cash", "Bank Transfer", "Cheque")
    val categories = listOf("All Fees", "Tuition Fee", "Admission Fee", "Exam Fee", "Security Fee")

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {

        // Ledger Header
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("STUDENT FEE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Ledger Status", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF00F3FF).copy(0.1f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("PENDING Rs. ${stats["outstanding_balance"] ?: "0"}", color = Color(0xFF00F3FF), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DropdownSelector(selectedMonth, months, Modifier.weight(1f), isDark) { selectedMonth = it }
                    DropdownSelector(selectedYear, years, Modifier.weight(1f), isDark) { selectedYear = it }
                }
            }
        }

        // Payment Form
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DropdownSelector(selectedMode, modes, Modifier.weight(1f), isDark) { selectedMode = it }
                    DropdownSelector(selectedCategory, categories, Modifier.weight(1f), isDark) { selectedCategory = it }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(14.dp)),
                        placeholder = { Text("Amount (Rs.)...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.04f),
                            unfocusedContainerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.04f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(Modifier.width(10.dp))

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onSave(amt, selectedMode, selectedCategory, selectedMonth, selectedYear)
                        },
                        modifier = Modifier.height(56.dp).clip(RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(Color(0xFF00F3FF)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Save, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("SAVE", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Fee History
        Text("PAYMENT HISTORY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 5.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Text("No records for this period.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentRoleTab(basic: Map<String, Any?>, isDark: Boolean, onUpdate: (String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    val currentRole = basic["student_role"]?.toString() ?: "Regular Student"
    var selectedRole by remember { mutableStateOf(currentRole) }

    val roles = listOf("Regular Student", "Class Monitor", "In-Charge", "Sports Captain", "Prefect")

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(20.dp))
                Text("Student Role", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Assign specific responsibilities to this student.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)

                Spacer(Modifier.height(30.dp))

                DropdownSelector(selectedRole, roles, Modifier.fillMaxWidth(), isDark) { selectedRole = it }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onUpdate(selectedRole) },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(Color(0xFF3B82F6))
                ) {
                    Text("UPDATE ROLE", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentDetailsTab(basic: Map<String, Any?>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Text("ACADEMIC DETAILS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(15.dp))

                // Show bits of info
                DetailItem("Admission Date", basic["admission_date"]?.toString() ?: "N/A", textColor)
                DetailItem("CNIC / B-Form",  basic["cnic"]?.toString() ?: "N/A", textColor)
                DetailItem("Date of Birth",  basic["dob"]?.toString() ?: "N/A", textColor)
                DetailItem("Place of Birth", basic["pob"]?.toString() ?: "N/A", textColor)
                DetailItem("Cast / Clan",    basic["cast"]?.toString() ?: "N/A", textColor)
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Text("CONTACT INFORMATION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(15.dp))
                DetailItem("Father Name", basic["father_name"]?.toString() ?: "N/A", textColor)
                DetailItem("Father Contact", basic["father_contact"]?.toString() ?: "N/A", textColor)
                DetailItem("WhatsApp", basic["whatsapp"]?.toString() ?: "N/A", textColor)
                DetailItem("Current Address", basic["address"]?.toString() ?: "N/A", textColor)
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun DetailItem(label: String, value: String, textColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StudentBiometricTab(basic: Map<String, Any?>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(cardColor.copy(0.5f))) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(0.1f)), Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = textColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(15.dp))
                Column {
                    Text(basic["full_name"]?.toString() ?: "Student", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("READY FOR CONFIGURATION", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }

        Text("REGISTERED BIOMETRICS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 5.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Face, null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(15.dp))
                Text("FACE REGISTERED", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Button(
                onClick = { },
                modifier = Modifier.weight(1f).height(120.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF8B5CF6).copy(0.1f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Face, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("ENROLL FACE", color = Color(0xFF8B5CF6), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            Button(
                onClick = { },
                modifier = Modifier.weight(1f).height(120.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF10B981).copy(0.1f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Fingerprint, null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("ENROLL FINGER", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        OutlinedButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.Upload, null, tint = Color(0xFF00F3FF))
            Spacer(Modifier.width(8.dp))
            Text("UPLOAD PROFILE PHOTO", color = Color(0xFF00F3FF), fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentStruckOffTab(basic: Map<String, Any?>, isDark: Boolean, onAction: (String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    val currentStatus = basic["status"]?.toString() ?: "active"

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(24.dp))
                Text("Struck OFF student", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Are you sure you want to change this student's status? Struck off students will no longer appear in active attendance rolls.",
                    color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)

                Spacer(Modifier.height(30.dp))

                if (currentStatus == "active") {
                    Button(
                        onClick = { onAction("struck_off") },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFFEF4444))
                    ) {
                        Text("STRUCK OFF NOW", color = Color.White, fontWeight = FontWeight.Black)
                    }
                } else {
                    Button(
                        onClick = { onAction("active") },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF10B981))
                    ) {
                        Text("RESTORE TO ACTIVE", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}


@Composable
fun StaffPillStat(label: String, value: String, color: Color, isDark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun StatBox(label: String, value: String, accent: Color, modifier: Modifier, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(cardColor).padding(15.dp)) {
        Column {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color, textColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = textColor.copy(0.7f), fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun InfoItem(label: String, value: String, icon: ImageVector, textColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EnrollStudentModal(
    viewModel: com.example.wantuch.ui.viewmodel.WantuchViewModel,
    structure: com.example.wantuch.domain.model.SchoolStructureResponse?,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Quick List, 1: Manual
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val context = LocalContext.current

    var selectedClassId by remember { mutableStateOf(0) }
    var selectedSectionId by remember { mutableStateOf(0) }
    var selectedGender by remember { mutableStateOf("Male") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardColor,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("ENROLL STUDENTS", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Dropdowns for Class & Section
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        val className = structure?.classes?.find { it.id == selectedClassId }?.name ?: "Select Class"

                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(0.3f))
                        ) {
                            Text(className.uppercase(), color = textColor, fontSize = 12.sp)
                        }

                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            structure?.classes?.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text(cls.name.uppercase()) },
                                    onClick = { selectedClassId = cls.id; selectedSectionId = 0; expanded = false }
                                )
                            }
                        }
                    }

                    Box(Modifier.weight(0.6f)) {
                        var expanded by remember { mutableStateOf(false) }
                        val sections = structure?.classes?.find { it.id == selectedClassId }?.sections ?: emptyList()
                        val sectionName = sections.find { it.id == selectedSectionId }?.name ?: "Section"

                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(0.3f))
                        ) {
                            Text(sectionName.uppercase(), color = textColor, fontSize = 12.sp)
                        }

                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            sections.forEach { sec ->
                                DropdownMenuItem(
                                    text = { Text(sec.name.uppercase()) },
                                    onClick = { selectedSectionId = sec.id; expanded = false }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(15.dp))

                // Tab Switcher
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(0.05f))) {
                    arrayOf("QUICK LIST", "MANUAL ENTRY").forEachIndexed { index, title ->
                        Box(
                            Modifier.weight(1f).height(40.dp).clickable { selectedTab = index }
                                .background(if(selectedTab == index) Color(0xFF3B82F6) else Color.Transparent),
                            Alignment.Center
                        ) {
                            Text(title, color = if(selectedTab == index) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (selectedTab == 0) {
                    // Quick List Tab
                    var namesText by remember { mutableStateOf("") }

                    Column {
                        // Gender Selector for Quick List
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Gender for list:", color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            RadioButton(selected = selectedGender == "Male", onClick = { selectedGender = "Male" })
                            Text("Male", color = textColor, fontSize = 12.sp)
                            RadioButton(selected = selectedGender == "Female", onClick = { selectedGender = "Female" })
                            Text("Female", color = textColor, fontSize = 12.sp)
                        }

                        Text("Paste names (one per line):", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))

                        OutlinedTextField(
                            value = namesText,
                            onValueChange = { namesText = it },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            placeholder = { Text("1. Ali Khan\n2. Sarah Malik", fontSize = 12.sp, color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color(0xFF3B82F6)
                            )
                        )

                        Spacer(Modifier.height(15.dp))

                        Button(
                            onClick = {
                                if (selectedClassId == 0) { Toast.makeText(context, "Select Class", Toast.LENGTH_SHORT).show(); return@Button }
                                viewModel.bulkSaveStudents(selectedClassId, selectedSectionId, namesText, selectedGender,
                                    onSuccess = { 
                                        Toast.makeText(context, "record successfully saved", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                        viewModel.fetchStudents() // force global refresh to show new students
                                    },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("ENROLL STUDENTS", fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    // Manual Entry Tab
                    StudentIdentityForm(emptyMap(), isDark) { fields ->
                        if (selectedClassId == 0) { Toast.makeText(context, "Select Class", Toast.LENGTH_SHORT).show(); return@StudentIdentityForm }
                        val fullFields = fields.toMutableMap()
                        fullFields["assigned_class_id"] = selectedClassId.toString()
                        fullFields["assigned_section_id"] = selectedSectionId.toString()
                        fullFields["class_id"] = selectedClassId.toString()
                        fullFields["section_id"] = selectedSectionId.toString()
                        fullFields["user_type"] = "student"

                        viewModel.saveStudent(fullFields,
                            onSuccess = { 
                                Toast.makeText(context, "record successfully saved", Toast.LENGTH_LONG).show()
                                onDismiss()
                                viewModel.fetchStudents() // force global refresh
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 14.sp, color = if(isDark) Color.White.copy(0.4f) else Color.Gray) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp)) },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = if(isDark) Color.White else Color.Black,
            unfocusedTextColor = if(isDark) Color.White else Color.Black,
            focusedBorderColor = Color(0xFF3B82F6),
            unfocusedBorderColor = if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f),
            focusedContainerColor = if(isDark) Color(0xFF1E293B) else Color.White,
            unfocusedContainerColor = if(isDark) Color(0xFF1E293B) else Color.White
        ),
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else keyboardOptions,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true
    )
}

@Composable
fun PremiumTabButton(title: String, isSelected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color(0xFF3B82F6) else if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    val textColor = if (isSelected) Color.White else if (isDark) Color.White.copy(0.5f) else Color.Black.copy(0.5f)

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
            Text(
                title.uppercase(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeManagementScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenStudentFee: (Int) -> Unit
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val labelColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Intelligence", "Rules", "Structure", "Fee Set", "Transport", "Approvals", "WhatsApp")
    
    // Bottom Sheet State for Payment
    var selectedStudentIdForSheet by remember { mutableIntStateOf(0) }
    var showPaymentSheet by remember { mutableStateOf(false) }

    // Fetch stats and data
    val dashboardData by viewModel.dashboardData.collectAsState()
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0

    val feeDataState = remember { mutableStateOf<org.json.JSONObject?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(instId) {
        if (instId > 0) {
            isLoading.value = true
            viewModel.safeApiCall("GET_FEE_DASHBOARD", mapOf("institution_id" to instId.toString())) { json ->
                feeDataState.value = json
                isLoading.value = false
            }
        }
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderActionIcon(Icons.Default.ArrowBack, isDark, onBack)
                    Spacer(Modifier.width(16.dp))
                    Text("Fee Manager", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }

                // Statistical Header (Summary)
                feeDataState.value?.optJSONObject("stats")?.let { stats ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatSmallBadge("COLLECTED", "Rs. ${String.format("%,.0f", stats.optDouble("total_collected", 0.0))}", Color(0xFF10B981), isDark)
                        StatSmallBadge("OUTSTANDING", "Rs. ${String.format("%,.0f", stats.optDouble("total_outstanding", 0.0))}", Color(0xFFEF4444), isDark)
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Premium Two-Row Tab Grid
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ROW 1: First 4 tabs
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.take(4).forEachIndexed { i, title ->
                        Box(Modifier.weight(1f)) {
                            PremiumTabButton(title, selectedTab == i, isDark) { selectedTab = i }
                        }
                    }
                }
                // ROW 2: Remaining 3 tabs
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.drop(4).forEachIndexed { i, title ->
                        val actualIdx = i + 4
                        Box(Modifier.weight(1f)) {
                            PremiumTabButton(title, actualIdx == selectedTab, isDark) { selectedTab = actualIdx }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                if (isLoading.value) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFF3B82F6))
                } else {
                    FeeTabContent(selectedTab, feeDataState.value, viewModel, isDark) { studentId ->
                        selectedStudentIdForSheet = studentId
                        showPaymentSheet = true
                    }
                }
            }
        }

        if (showPaymentSheet && selectedStudentIdForSheet != 0) {
            ModalBottomSheet(
                onDismissRequest = { showPaymentSheet = false },
                containerColor = bgColor,
                dragHandle = { BottomSheetDefaults.DragHandle(color = labelColor.copy(0.2f)) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                StudentFeeDetailScreen(
                    studentId = selectedStudentIdForSheet,
                    viewModel = viewModel,
                    onBack = { showPaymentSheet = false },
                    isBottomSheet = true
                )
            }
        }
    }
}

@Composable
fun FeeStatDashboardHeader(stats: JSONObject, isDark: Boolean) {
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    
    val list = listOf(
        Triple("TOTAL CHARGES", stats.optDouble("cumulative_charges", 0.0), Color(0xFF6366F1)),
        Triple("COLLECTED", stats.optDouble("total_collected", 0.0), Color(0xFF10B981)),
        Triple("OUTSTANDING", stats.optDouble("total_outstanding", 0.0), Color(0xFFEF4444)),
        Triple("FINES", stats.optDouble("attendance_fine", 0.0), Color(0xFFF59E0B))
    )

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("INSTITUTIONAL PERFORMANCE", color = if(isDark) Color.White.copy(0.5f) else Color.Black.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            list.take(2).forEach { item ->
                Column(
                    Modifier.weight(1f).background(bgColor, RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp)).padding(16.dp)
                ) {
                    Text(item.first, color = if(isDark) Color.White.copy(0.4f) else Color.Black.copy(0.4f), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text("Rs. " + String.format("%,.0f", item.second), color = item.third, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            list.drop(2).forEach { item ->
                Column(
                    Modifier.weight(1f).background(bgColor, RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp)).padding(16.dp)
                ) {
                    Text(item.first, color = if(isDark) Color.White.copy(0.4f) else Color.Black.copy(0.4f), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text("Rs. " + String.format("%,.0f", item.second), color = item.third, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun FeeTabContent(tabIdx: Int, data: JSONObject?, viewModel: WantuchViewModel, isDark: Boolean, onOpenStudentFee: (Int) -> Unit) {
    data ?: return
    when (tabIdx) {
        0 -> FeeIntelligenceTab(data, viewModel, isDark, onOpenStudentFee)
        1 -> FeeSettingsTab(data, viewModel, isDark)
        2 -> FeeStructureTab(data, viewModel, isDark)
        3 -> StudentFeeSetTab(data, viewModel, isDark)
        4 -> TransportSetupTab(data, viewModel, isDark)
        5 -> FeeApprovalsTab(data, viewModel, isDark)
        6 -> WhatsAppTab(data, viewModel, isDark)
    }
}

@Composable
fun FeeIntelligenceTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean, onOpenStudentFee: (Int) -> Unit) {
    val context = LocalContext.current
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)

    var selectedMonth by remember { mutableStateOf(java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date())) }
    var selectedYear by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date())) }
    var selectedClass by remember { mutableStateOf("All Classes") }
    var selectedClassId by remember { mutableIntStateOf(0) }
    var selectedSection by remember { mutableStateOf("All Sections") }
    var selectedSectionId by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }

    var selectedFeeType by remember { mutableStateOf("All Fees") }

    val students = remember { mutableStateListOf<JSONObject>() }
    val isLoading = remember { mutableStateOf(false) }

    // Use the same schoolStructure the student module uses — already loaded and reliable
    val structure by viewModel.schoolStructure.collectAsState()

    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val years = (2022..2030).map { it.toString() }

    // Build class options from structure (mirrors student filter logic)
    val classOptions = remember(structure) {
        val opts = mutableListOf("All Classes")
        structure?.classes?.forEach { opts.add(it.name) }
        opts
    }
    val classMap = remember(structure) {
        val map = mutableMapOf<String, Int>()
        structure?.classes?.forEach { map[it.name] = it.id }
        map
    }

    // Build section options from selected class in structure
    val sectionOptions = remember(structure, selectedClassId) {
        val opts = mutableListOf("All Sections")
        val cls = structure?.classes?.find { it.id == selectedClassId }
        cls?.sections?.forEach { opts.add(it.name) }
        opts
    }
    val sectionMap = remember(structure, selectedClassId) {
        val map = mutableMapOf<String, Int>()
        val cls = structure?.classes?.find { it.id == selectedClassId }
        cls?.sections?.forEach { map[it.name] = it.id }
        map
    }

    // Fee types from dashboard data
    val feeTypeOptions = remember(data) {
        val opts = mutableListOf("All Types")
        data.optJSONArray("fee_types")?.let { arr ->
            for (i in 0 until arr.length()) opts.add(arr.getJSONObject(i).optString("type_name"))
        }
        opts
    }

    // Trigger fetch of school structure if not already loaded
    LaunchedEffect(instId) {
        if (structure == null && instId > 0) viewModel.fetchSchoolStructure()
    }

    LaunchedEffect(selectedMonth, selectedYear, selectedClassId, selectedSectionId, search, selectedFeeType) {
        isLoading.value = true
        viewModel.safeApiCall("GET_FEE_INTELLIGENCE", mapOf(
            "institution_id" to instId.toString(),
            "month" to selectedMonth,
            "year" to selectedYear,
            "class_id" to selectedClassId.toString(),
            "section_id" to selectedSectionId.toString(),
            "search" to search,
            "fee_type" to selectedFeeType
        )) { json ->
            isLoading.value = false
            students.clear()
            json?.optJSONArray("students")?.let { arr ->
                for (i in 0 until arr.length()) {
                    students.add(arr.getJSONObject(i))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Intelligence Dashboard Header (Institutional Stats)
        data.optJSONObject("stats")?.let { stats ->
            FeeStatDashboardHeader(stats, isDark)
        }

        // Toolbar Actions & Search
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            // ROW 1: Actions (Send, Print, Summary, Delete)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Background badge logic for actions
                ActionIconButton(Icons.Default.Send, "Send", Color(0xFF10B981), isDark) { /* WhatsApp/Send */ }
                ActionIconButton(Icons.Default.Print, "Print", Color(0xFF3B82F6), isDark) { /* Print List */ }
                ActionIconButton(Icons.Default.Summarize, "Summary", Color(0xFFF59E0B), isDark) { /* Print Summary */ }
                ActionIconButton(Icons.Default.Delete, "Delete", Color(0xFFEF4444), isDark) { /* Bulk Delete */ }

                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // ROW 2: First 3 filters (Search Name/Roll, Month, Year)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Search Input as a semi-compact field
                Box(Modifier.weight(1.2f).height(44.dp).background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(10.dp)).border(1.dp, if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = search,
                        onValueChange = { search = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 11.sp),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (search.isEmpty()) Text("Search...", color = labelColor, fontSize = 11.sp, maxLines = 1)
                            innerTextField()
                        }
                    )
                }

                Box(Modifier.weight(1f)) {
                    DropdownSelector(selectedMonth, months, Modifier.height(44.dp), isDark) { selectedMonth = it }
                }
                Box(Modifier.weight(1f)) {
                    DropdownSelector(selectedYear, years, Modifier.height(44.dp), isDark) { selectedYear = it }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ROW 3: Second 3 filters (Classes, Sec, All Fees)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    DropdownSelector(selectedClass, classOptions, Modifier.height(44.dp), isDark) {
                        selectedClass = it
                        selectedClassId = classMap[it] ?: 0
                    }
                }
                Box(Modifier.weight(1f)) {
                    DropdownSelector(selectedSection, sectionOptions, Modifier.height(44.dp), isDark) {
                        selectedSection = it
                        selectedSectionId = sectionMap[it] ?: 0
                    }
                }
                Box(Modifier.weight(1f)) {
                    DropdownSelector(selectedFeeType, feeTypeOptions, Modifier.height(44.dp), isDark) {
                        selectedFeeType = it
                    }
                }
            }
        }

        if (isLoading.value) {
            Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(students.size) { index ->
                    val student = students[index]
                    StudentFeeListItem(student, isDark) { onOpenStudentFee(student.optInt("id")) }
                }

                if (students.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 40.dp), Alignment.Center) {
                            Text("No students found covering these filters", color = labelColor, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentFeeListItem(student: JSONObject, isDark: Boolean, onClick: () -> Unit) {
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White

    val status = student.optString("status")
    val statusColor = when(status) {
        "PAID" -> Color(0xFF10B981)
        "PARTIAL" -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Surface(
        onClick = onClick,
        color = cardColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)),
        shadowElevation = 2.dp
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).clip(CircleShape).background(statusColor.copy(0.1f)), Alignment.Center) {
                val pic = student.optString("profile_pic")
                if (pic.isNotEmpty()) {
                    AsyncImage(model = pic, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(student.optString("name").take(2), color = statusColor, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(student.optString("name"), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(student.optString("class_section"), color = labelColor, fontSize = 11.sp)
                Text("D/O: ${student.optString("father_name")}", color = labelColor, fontSize = 10.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Payable: ${student.optDouble("payable")}", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = statusColor.copy(0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        status,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeeSettingsTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    val settings = data.optJSONObject("settings") ?: JSONObject()
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = Color.Gray
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        // Automation Rules
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(1.dp, Color.Gray.copy(0.2f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Automation Rules", fontWeight = FontWeight.Bold, color = textColor)
                }
                Text("Configure when fees are automatically generated.", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                var status by remember { mutableStateOf(settings.optString("fee_automation_status", "On")) }
                var day by remember { mutableStateOf(settings.optString("fee_automation_day", "25")) }
                var targetMonth by remember { mutableStateOf(settings.optString("fee_target_month", "January")) }
                var targetYear by remember { mutableStateOf(settings.optString("fee_target_year", "2024")) }

                Text("AUTOMATION STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                DropdownSelector(status, listOf("On", "Off"), Modifier.fillMaxWidth(), isDark) { status = it }
                Spacer(Modifier.height(8.dp))

                Text("RUN ON DAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                DropdownSelector(day, (1..28).map { it.toString() }, Modifier.fillMaxWidth(), isDark) { day = it }
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("TARGET MONTH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetMonth, listOf("January","February","March","April","May","June","July","August","September","October","November","December"), Modifier.fillMaxWidth(), isDark) { targetMonth = it }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("TARGET YEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetYear, (2020..2030).map { it.toString() }, Modifier.fillMaxWidth(), isDark) { targetYear = it }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.safeFeeApiCall("save_automation_settings", mapOf(
                            "state" to if (status == "On") "1" else "0",
                            "day" to day,
                            "month" to targetMonth,
                            "year" to targetYear
                        )) {
                            // Handled via toast or snackbar
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE SETTINGS")
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Manual Trigger\nInstantly generate fees for students who haven't been processed yet. This respects your \"Target Month\" setting above and will skip duplicates.",
                    fontSize = 11.sp, color = labelColor, modifier = Modifier.padding(horizontal = 4.dp), textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.safeFeeApiCall("run_automation_now", mapOf()) {} },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RUN GENERATION NOW", color = textColor)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Global Adjustments
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(1.dp, Color.Gray.copy(0.2f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Global Adjustments", fontWeight = FontWeight.Bold, color = textColor)
                }
                Text("Apply bulk changes across the entire institution.", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                var cat by remember { mutableStateOf("Tuition Fee") }
                var action by remember { mutableStateOf("% Increase") }
                var valStr by remember { mutableStateOf("") }

                Text("CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                DropdownSelector(cat, listOf("Tuition Fee", "Transport Fee", "Fine"), Modifier.fillMaxWidth(), isDark) { cat = it }
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("ACTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(action, listOf("% Increase", "% Decrease", "Fixed Add", "Fixed Sub"), Modifier.fillMaxWidth(), isDark) { action = it }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("VALUE (AMT OR %)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        PremiumTextField(valStr, { valStr = it }, "e.g. 5 or 500", Icons.Default.Add, isDark)
                    }
                }
                Spacer(Modifier.height(16.dp))

                var targetMonth by remember { mutableStateOf(java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date())) }
                var targetYear by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("TARGET MONTH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetMonth, listOf("January","February","March","April","May","June","July","August","September","October","November","December"), Modifier.fillMaxWidth(), isDark) { targetMonth = it }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("TARGET YEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetYear, (2020..2030).map { it.toString() }, Modifier.fillMaxWidth(), isDark) { targetYear = it }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.safeFeeApiCall("apply_global_adjustment", mapOf(
                            "category" to cat,
                            "adjust_action" to action,
                            "value" to valStr,
                            "month" to targetMonth,
                            "year" to targetYear
                        )) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("APPLY CHANGES")
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.safeFeeApiCall("sync_absence_fines", mapOf("month" to targetMonth, "year" to targetYear)) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B5CF6))
                ) {
                    Text("UPDATE ABSENCE FINES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Bulk Fee Assignment
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(1.dp, Color.Gray.copy(0.2f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bulk Fee Assignment", fontWeight = FontWeight.Bold, color = textColor)
                }
                Text("Assign specific fees to multiple students at once.", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                var scope by remember { mutableStateOf("Globally (All Students)") }
                var feeTypeIdx by remember { mutableStateOf(0) }
                var bulkAmt by remember { mutableStateOf("") }

                val fTypesArr = data.optJSONArray("fee_types")
                val fTypeNames = mutableListOf<String>()
                for(i in 0 until (fTypesArr?.length() ?: 0)) fTypeNames.add(fTypesArr!!.getJSONObject(i).optString("type_name"))

                Text("SCOPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                DropdownSelector(scope, listOf("Globally (All Students)", "Class-wise", "Section-wise"), Modifier.fillMaxWidth(), isDark) { scope = it }
                Spacer(Modifier.height(8.dp))

                Text("FEE TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                DropdownSelector(if(fTypeNames.isNotEmpty()) fTypeNames[feeTypeIdx] else "No Types", fTypeNames, Modifier.fillMaxWidth(), isDark) { feeTypeIdx = fTypeNames.indexOf(it) }
                Spacer(Modifier.height(8.dp))

                Text("AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                PremiumTextField(bulkAmt, { bulkAmt = it }, "Enter amount", Icons.Default.Money, isDark)
                Spacer(Modifier.height(16.dp))

                var targetMonth by remember { mutableStateOf(java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date())) }
                var targetYear by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("MONTH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetMonth, listOf("January","February","March","April","May","June","July","August","September","October","November","December"), Modifier.fillMaxWidth(), isDark) { targetMonth = it }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("YEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetYear, (2020..2030).map { it.toString() }, Modifier.fillMaxWidth(), isDark) { targetYear = it }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val tid = fTypesArr?.optJSONObject(feeTypeIdx)?.optString("id") ?: ""
                        viewModel.safeFeeApiCall("bulk_assign_fee", mapOf(
                            "scope" to scope,
                            "fee_type_id" to tid,
                            "amount" to bulkAmt,
                            "month" to targetMonth,
                            "year" to targetYear
                        )) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ASSIGN FEES NOW", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(100.dp)) // Padding for bottom
    }
}

@Composable
fun FeeSettingItem(title: String, desc: String, initialValue: Boolean, isDark: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = if(isDark) Color.White.copy(0.5f) else Color.Black.copy(0.5f), fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}

@Composable
fun FeeStructureTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0
    val structureState by viewModel.schoolStructure.collectAsState()

    // Fallback if 'classes' is missing from dashboard data
    val classes = data.optJSONArray("classes") ?: JSONArray().apply {
        structureState?.classes?.forEach { cls ->
            put(JSONObject().apply {
                put("id", cls.id)
                put("name", cls.name)
            })
        }
    }
    val feeTypes = data.optJSONArray("fee_types") ?: data.optJSONArray("fee_categories") ?: JSONArray()
    val structure = data.optJSONArray("fee_structure")

    LaunchedEffect(instId) {
        if (structureState == null && instId > 0) viewModel.fetchSchoolStructure()
    }

    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    var editingClass by remember { mutableStateOf<JSONObject?>(null) }

    val searchBg  = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val borderCol = Color.Gray.copy(0.2f)
    val iconBg    = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        // ROW 1: Actions (Matching Web)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    viewModel.safeFeeApiCall("clear_all_fees_for_school", mapOf()) {
                        viewModel.refreshDashboard()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp).border(1.dp, Color(0xFF3B82F6).copy(0.3f), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("CLEAR ALL", color = if(isDark) Color.White else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Lock, null, tint = Color.Cyan, modifier = Modifier.size(12.dp))
            }

            Text("Fee Structure", color = textColor, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(classes.length()) { i ->
                val cls = classes.optJSONObject(i)
                val cid = cls?.optInt("id") ?: 0
                val className = cls?.optString("name") ?: "Class"

                Card(
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    var selectedTypeIdx by remember { mutableIntStateOf(0) }
                    var amountText by remember { mutableStateOf("0") }

                    val typeNames = remember(feeTypes) {
                        val names = mutableListOf<String>()
                        for (i in 0 until feeTypes.length()) {
                            names.add(feeTypes.getJSONObject(i).optString("type_name"))
                        }
                        if (names.isEmpty()) names.addAll(listOf("Tuition Fee", "Transport Fee", "Attendance Fine", "Late Fee"))
                        names
                    }

                    // Update amount whenever type or data changes
                    LaunchedEffect(selectedTypeIdx, structure) {
                        if (typeNames.isNotEmpty() && feeTypes.length() > 0) {
                            val tid = feeTypes.optJSONObject(selectedTypeIdx)?.optInt("id") ?: 0
                            var foundAmount = 0.0
                            structure?.let { sArr ->
                                for (k in 0 until sArr.length()) {
                                    val s = sArr.getJSONObject(k)
                                    if (s.optInt("class_id") == cid && s.optInt("fee_type_id") == tid) {
                                        foundAmount = s.optDouble("amount", 0.0)
                                        break
                                    }
                                }
                            }
                            amountText = if (foundAmount % 1 == 0.0) foundAmount.toInt().toString() else foundAmount.toString()
                        }
                    }

                    Column(Modifier.padding(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Class Name
                            Text(
                                text = className,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                modifier = Modifier.weight(1.5f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            // Fee Type Dropdown with Overlapping + Icon
                            Box(Modifier.weight(3f), contentAlignment = Alignment.Center) {
                                DropdownSelector(
                                    value = if (typeNames.isNotEmpty()) typeNames[selectedTypeIdx.coerceIn(0, typeNames.size - 1)] else "Select Type",
                                    options = typeNames,
                                    isDark = isDark,
                                    modifier = Modifier.height(34.dp).padding(end = 4.dp) // Subtle padding for better overlap feel
                                ) { name ->
                                    selectedTypeIdx = typeNames.indexOf(name)
                                }

                                // + Icon (Overlapping Top-Right Corner)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (4).dp, y = (-6).dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6)) // Solid deep purple for visibility
                                        .border(1.5.dp, Color.White.copy(0.4f), CircleShape) // Thin border to distinguish from dropdown
                                        .shadow(4.dp, CircleShape)
                                        .clickable { editingClass = cls },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }

                            // Amount Box
                            Box(
                                Modifier
                                    .weight(1.4f)
                                    .height(34.dp)
                                    .background(if(isDark) Color.Black.copy(0.2f) else Color.Black.copy(0.05f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray.copy(0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp),
                                Alignment.CenterStart
                            ) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = amountText,
                                    onValueChange = { amountText = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            // Update button
                            Button(
                                onClick = {
                                    val tid = if (feeTypes.length() > 0) feeTypes.getJSONObject(selectedTypeIdx).optInt("id") else 0
                                    val ratesJson = JSONObject().apply { put(tid.toString(), amountText) }
                                    viewModel.safeFeeApiCall("save_fee_structure_bulk", mapOf(
                                        "class_id" to cid.toString(),
                                        "rates" to ratesJson.toString()
                                    )) {
                                        viewModel.refreshDashboard()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("UPDATE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingClass != null) {
        val cid = editingClass!!.optInt("id")
        val rates = remember { mutableStateMapOf<Int, String>() }

        // Pre-fill existing rates
        LaunchedEffect(editingClass) {
            feeTypes?.let { types ->
                for(i in 0 until types.length()) {
                    val tid = types.getJSONObject(i).optInt("id")
                    var amt = "0"
                    structure?.let { sArr ->
                        for (k in 0 until sArr.length()) {
                            val s = sArr.getJSONObject(k)
                            if (s.optInt("class_id") == cid && s.optInt("fee_type_id") == tid) {
                                amt = s.optDouble("amount", 0.0).toInt().toString()
                                break
                            }
                        }
                    }
                    rates[tid] = amt
                }
            }
        }

        AlertDialog(
            onDismissRequest = { editingClass = null },
            containerColor = bgColor,
            title = { Text("Update Rates: ${editingClass!!.optString("name")}", color = textColor) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    feeTypes?.let { types ->
                        for(i in 0 until types.length()) {
                            val t = types.getJSONObject(i)
                            val tid = t.optInt("id")
                            Text(t.optString("type_name"), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            PremiumTextField(rates[tid] ?: "0", { rates[tid] = it }, "Amount", Icons.Default.Money, isDark)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val ratesJson = JSONObject()
                    rates.forEach { (k, v) -> ratesJson.put(k.toString(), v) }
                    viewModel.safeFeeApiCall("save_fee_structure_bulk", mapOf("class_id" to cid.toString(), "rates" to ratesJson.toString())) {
                        editingClass = null
                    }
                }) { Text("SAVE CHANGES") }
            }
        )
    }
}

@Composable
fun StudentFeeSetTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0
    val textColor = if (isDark) Color.White else Color.Black
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White

    val allStudents = remember { mutableStateListOf<JSONObject>() }
    var displayedStudents by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Filters
    val structure by viewModel.schoolStructure.collectAsState()
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var month by remember { mutableStateOf(dateToMonth(System.currentTimeMillis())) }
    var year by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) }

    LaunchedEffect(selectedClass, selectedSection, month, year) {
        isLoading = true
        viewModel.safeApiCall("GET_FEE_INTELLIGENCE", mapOf(
            "institution_id" to instId.toString(),
            "class_id" to (structure?.classes?.find { it.name == selectedClass }?.id?.toString() ?: "0"),
            "section_id" to (structure?.classes?.find { it.name == selectedClass }?.sections?.find { it.name == selectedSection }?.id?.toString() ?: "0"),
            "month" to month,
            "year" to year
        )) { json ->
            isLoading = false
            allStudents.clear()
            json.optJSONArray("students")?.let { arr ->
                for (i in 0 until arr.length()) allStudents.add(arr.getJSONObject(i))
            }
            displayedStudents = allStudents
        }
    }

    // Extra filter / action state
    var searchQuery by remember { mutableStateOf("") }
    var exportFormat by remember { mutableStateOf("CSV") }
    var allTuitionEnabled by remember { mutableStateOf(true) }
    var allTransEnabled by remember { mutableStateOf(false) }
    var showAddSpecificFee by remember { mutableStateOf(false) }

    // Keep displayedStudents filtered by search
    LaunchedEffect(searchQuery, allStudents.size) {
        displayedStudents = if (searchQuery.isBlank()) allStudents.toList()
        else allStudents.filter {
            it.optString("full_name").contains(searchQuery, ignoreCase = true) ||
                    it.optString("parent_name").contains(searchQuery, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {

        val searchBg  = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        val borderCol = Color.Gray.copy(0.2f)
        val iconBg    = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)

        var statusFilter by remember { mutableStateOf("All Status") }
        val statuses = listOf("All Status", "Active", "Inactive", "Late", "Struck-off")

        // ══════════════════════════════════════════════
        // ROW 1: Class · Section · Month
        // ══════════════════════════════════════════════
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val classNames = mutableListOf("All")
            structure?.classes?.forEach { classNames.add(it.name) }
            val sections = mutableListOf("All")
            structure?.classes?.find { it.name == selectedClass }?.sections?.forEach { sections.add(it.name) }

            DropdownSelector(selectedClass ?: "All", classNames, Modifier.weight(1f), isDark) {
                selectedClass = if (it == "All") null else it; selectedSection = null
            }
            DropdownSelector(selectedSection ?: "All", sections, Modifier.weight(1f), isDark) {
                selectedSection = if (it == "All") null else it
            }
            DropdownSelector(month,
                listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"),
                Modifier.weight(1f), isDark) {
                month = when(it) {
                    "Jan"->"January"; "Feb"->"February"; "Mar"->"March"; "Apr"->"April"
                    "May"->"May"; "Jun"->"June"; "Jul"->"July"; "Aug"->"August"
                    "Sep"->"September"; "Oct"->"October"; "Nov"->"November"; else->"December"
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ══════════════════════════════════════════════
        // ROW 2: Year · Export Format · Status
        // ══════════════════════════════════════════════
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DropdownSelector(year, (2020..2030).map { it.toString() }, Modifier.weight(1f), isDark) { year = it }
            DropdownSelector(exportFormat, listOf("CSV","PDF","Excel"), Modifier.weight(1f), isDark) {
                exportFormat = it
            }
            DropdownSelector(statusFilter, statuses, Modifier.weight(1f), isDark) { statusFilter = it }
        }

        Spacer(Modifier.height(10.dp))

        // ══════════════════════════════════════════════
        // ROW 3: Icons and Buttons
        // ══════════════════════════════════════════════
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Export Icons
            IconButton(
                onClick = {},
                modifier = Modifier.size(32.dp).background(iconBg, RoundedCornerShape(8.dp)).border(1.dp, borderCol, RoundedCornerShape(8.dp))
            ) { Icon(Icons.Default.Share, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp)) }

            IconButton(
                onClick = {},
                modifier = Modifier.size(32.dp).background(iconBg, RoundedCornerShape(8.dp)).border(1.dp, borderCol, RoundedCornerShape(8.dp))
            ) { Icon(Icons.Default.Description, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp)) }

            Spacer(Modifier.weight(1f))

            // ADD SPECIFIC FEE button
            Button(
                onClick = { showAddSpecificFee = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("ADD SPECIFIC FEE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
            }

            // CLEAR button
            Button(
                onClick = {
                    viewModel.safeFeeApiCall("clear_all_fees", mapOf(
                        "institution_id" to instId.toString(),
                        "month" to month, "year" to year
                    )) { viewModel.refreshDashboard() }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("CLEAR", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        Spacer(Modifier.height(6.dp))

        // ══════════════════════════════════════════════
        // ROW 4 – Global toggles: ALL TUITION · ALL TRANS
        // ══════════════════════════════════════════════
        Row(
            Modifier.fillMaxWidth()
                .background(searchBg, RoundedCornerShape(8.dp))
                .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text("ALL TUITION", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            Switch(
                allTuitionEnabled,
                {
                    allTuitionEnabled = it
                    viewModel.safeFeeApiCall("toggle_all_tuition", mapOf(
                        "institution_id" to instId.toString(),
                        "state" to if (it) "1" else "0",
                        "month" to month, "year" to year
                    )) {}
                },
                modifier = Modifier.scale(0.6f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3B82F6)
                )
            )
            Spacer(Modifier.width(16.dp))
            Text("ALL TRANS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            Switch(
                allTransEnabled,
                {
                    allTransEnabled = it
                    viewModel.safeFeeApiCall("toggle_all_transport", mapOf(
                        "institution_id" to instId.toString(),
                        "state" to if (it) "1" else "0",
                        "month" to month, "year" to year
                    )) {}
                },
                modifier = Modifier.scale(0.6f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3B82F6)
                )
            )
        }

        Spacer(Modifier.height(8.dp))
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                itemsIndexed(displayedStudents) { idx, stu ->
                    val sid = stu.optString("id")
                    val cid = stu.optString("class_id")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {

                            // ── Row 1: serial · photo · name/parent · reset ──
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Serial badge
                                Box(
                                    Modifier.size(22.dp)
                                        .background(Color(0xFF3B82F6).copy(0.12f), RoundedCornerShape(5.dp)),
                                    Alignment.Center
                                ) {
                                    Text("${idx + 1}", color = Color(0xFF3B82F6), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.width(6.dp))
                                // Photo
                                val pic = stu.optString("profile_pic")
                                Box(
                                    Modifier.size(38.dp).clip(CircleShape)
                                        .background(Color(0xFF3B82F6).copy(0.1f)),
                                    Alignment.Center
                                ) {
                                    if (pic.isNotEmpty()) {
                                        AsyncImage(
                                            model = pic, contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp), tint = Color(0xFF3B82F6))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                // Name + class info
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stu.optString("full_name"),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = textColor,
                                        maxLines = 1
                                    )
                                    val parent = stu.optString("parent_name", "")
                                    val classInfo = "${stu.optString("class_name")} · ${stu.optString("section_name")}"
                                    Text(
                                        if (parent.isNotEmpty()) "$parent · $classInfo" else classInfo,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                                // Reset button
                                IconButton(
                                    onClick = {
                                        viewModel.safeFeeApiCall(
                                            "reset_student_fee",
                                            mapOf("student_id" to sid, "month" to month, "year" to year)
                                        ) {}
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, "Reset", tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Divider(color = Color.Gray.copy(0.12f))
                            Spacer(Modifier.height(10.dp))

                            // ── Row 2: TUITION toggle | TRANSPORT toggle (side by side) ──
                            var tuitionState by remember { mutableStateOf(stu.optString("tuition_enabled", "1") == "1") }
                            var mode by remember { mutableStateOf(stu.optString("tuition_type", "Full")) }
                            var transState by remember { mutableStateOf(stu.optString("transport_enabled", "0") == "1") }
                            val locations = data.optJSONArray("trans_locations")
                            val locNames = mutableListOf("Select Loc")
                            for (i in 0 until (locations?.length() ?: 0))
                                locNames.add(locations!!.getJSONObject(i).optString("location"))
                            var selectedLoc by remember { mutableStateOf(stu.optString("transport_location_id", "0")) }
                            val locName = locations?.let { arr ->
                                for (i in 0 until arr.length())
                                    if (arr.getJSONObject(i).optString("id") == selectedLoc)
                                        return@let arr.getJSONObject(i).optString("location")
                                "Select Loc"
                            } ?: "Select Loc"

                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                // TUITION toggle
                                Text("TUITION", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray)
                                Switch(
                                    tuitionState,
                                    {
                                        tuitionState = it
                                        viewModel.safeFeeApiCall("toggle_student_fee", mapOf(
                                            "enroll_id" to sid, "type" to "tuition",
                                            "state" to if (it) "1" else "0",
                                            "class_id" to cid, "tuition_mode" to mode,
                                            "target_month" to month, "target_year" to year
                                        )) {}
                                    },
                                    modifier = Modifier.scale(0.65f)
                                )
                                Spacer(Modifier.weight(1f))
                                // TRANSPORT toggle
                                Text("TRANSPORT", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray)
                                Switch(
                                    transState,
                                    {
                                        transState = it
                                        viewModel.safeFeeApiCall("toggle_student_fee", mapOf(
                                            "enroll_id" to sid, "type" to "transport",
                                            "state" to if (it) "1" else "0",
                                            "class_id" to cid, "location_id" to selectedLoc,
                                            "target_month" to month, "target_year" to year
                                        )) {}
                                    },
                                    modifier = Modifier.scale(0.65f)
                                )
                            }

                            // ── Row 3: Tuition controls (full width) ──
                            if (tuitionState) {
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DropdownSelector(
                                        mode,
                                        listOf("Full", "Half", "Custom"),
                                        if (mode == "Custom") Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                        isDark
                                    ) {
                                        mode = it
                                        viewModel.safeFeeApiCall("toggle_student_fee", mapOf(
                                            "enroll_id" to sid, "type" to "tuition",
                                            "state" to "1", "class_id" to cid,
                                            "tuition_mode" to it,
                                            "target_month" to month, "target_year" to year
                                        )) {}
                                    }
                                    if (mode == "Custom") {
                                        var customAmt by remember { mutableStateOf(stu.optString("tuition_fee", "0")) }
                                        PremiumTextField(customAmt, {
                                            customAmt = it
                                            viewModel.safeFeeApiCall("toggle_student_fee", mapOf(
                                                "enroll_id" to sid, "type" to "tuition",
                                                "state" to "1", "class_id" to cid,
                                                "tuition_mode" to "Custom",
                                                "custom_amount" to it,
                                                "target_month" to month, "target_year" to year
                                            )) {}
                                        }, "Amount", Icons.Default.Money, isDark, modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            // ── Row 4: Transport controls (full width) ──
                            if (transState) {
                                val showCustom = (locName == "User Define")
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DropdownSelector(
                                        locName,
                                        locNames,
                                        if (showCustom) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                        isDark
                                    ) { name ->
                                        val lid = locations?.let { arr ->
                                            for (i in 0 until arr.length())
                                                if (arr.getJSONObject(i).optString("location") == name)
                                                    return@let arr.getJSONObject(i).optString("id")
                                            "0"
                                        } ?: "0"
                                        selectedLoc = lid
                                        viewModel.safeFeeApiCall("toggle_student_fee", mapOf(
                                            "enroll_id" to sid, "type" to "transport",
                                            "state" to "1", "class_id" to cid,
                                            "location_id" to lid,
                                            "target_month" to month, "target_year" to year
                                        )) {}
                                    }
                                    if (showCustom) {
                                        var customTrans by remember { mutableStateOf(stu.optString("transport_charges", "0")) }
                                        PremiumTextField(customTrans, {
                                            customTrans = it
                                            viewModel.safeFeeApiCall("toggle_student_fee", mapOf(
                                                "enroll_id" to sid, "type" to "transport",
                                                "state" to "1", "class_id" to cid,
                                                "location_id" to "-1",
                                                "custom_amount" to it,
                                                "target_month" to month, "target_year" to year
                                            )) {}
                                        }, "Amount", Icons.Default.Money, isDark, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TransportSetupTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0
    val locations = data.optJSONArray("trans_locations")
    val textColor = if (isDark) Color.White else Color.Black
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White

    var selectedSubTab by remember { mutableStateOf(0) }
    val subTabs = listOf("PRICING SETUP", "DRIVER", "STAFF TRANSPORT")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Sub-tabs
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            subTabs.forEachIndexed { idx, title ->
                Surface(
                    onClick = { selectedSubTab = idx },
                    modifier = Modifier.weight(1f),
                    color = if(selectedSubTab == idx) Color(0xFF3B82F6) else bgColor,
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(title, modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if(selectedSubTab == idx) Color.White else Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        var showAddLocation by remember { mutableStateOf(false) }
        if (showAddLocation) {
            var locName by remember { mutableStateOf("") }
            var locAmt by remember { mutableStateOf("") }
            var locType by remember { mutableStateOf("Bus") }

            AlertDialog(
                onDismissRequest = { showAddLocation = false },
                containerColor = bgColor,
                title = { Text("Add Transport Route", color = textColor) },
                text = {
                    Column {
                        PremiumTextField(locName, { locName = it }, "Location Name", Icons.Default.LocationOn, isDark)
                        Spacer(Modifier.height(12.dp))
                        PremiumTextField(locAmt, { locAmt = it }, "Charges", Icons.Default.Money, isDark)
                        Spacer(Modifier.height(12.dp))
                        DropdownSelector(locType, listOf("Bus", "Van", "Coaster", "Other"), Modifier.fillMaxWidth(), isDark) { locType = it }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.safeFeeApiCall("save_transport_location", mapOf("location" to locName, "charges" to locAmt, "ride_type" to locType)) {
                            showAddLocation = false
                            // Refresh logic
                        }
                    }) { Text("ADD ROUTE") }
                }
            )
        }

        if (selectedSubTab == 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Routes & Fares", color = textColor, fontWeight = FontWeight.Black)
                IconButton(onClick = { showAddLocation = true }, modifier = Modifier.background(Color(0xFF3B82F6), RoundedCornerShape(8.dp)).size(30.dp)) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(locations?.length() ?: 0) { i ->
                    val loc = locations?.optJSONObject(i)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFF3B82F6).copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.DirectionsBus, null, Modifier.padding(8.dp).size(18.dp), Color(0xFF3B82F6))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(loc?.optString("location") ?: "", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(loc?.optString("ride_type") ?: "Bus", color = Color.Gray, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rs. ${loc?.optString("charges")}", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(14.dp).clickable { })
                                    Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(14.dp).clickable {
                                        viewModel.safeFeeApiCall("delete_transport_location", mapOf("id" to (loc?.optString("id") ?: ""))) {
                                            viewModel.refreshDashboard()
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedSubTab == 1) {
            val drivers = data.optJSONArray("drivers") ?: JSONArray()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Drivers", color = textColor, fontWeight = FontWeight.Black)
                IconButton(onClick = { /* showAddDriver = true */ }, modifier = Modifier.background(Color(0xFF3B82F6), RoundedCornerShape(8.dp)).size(30.dp)) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (drivers.length() == 0) {
                Box(Modifier.fillMaxSize().padding(top = 40.dp), Alignment.TopCenter) {
                    Text("Driver Management module active on Web. No mobile data yet.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(drivers.length()) { i ->
                        val driver = drivers.optJSONObject(i)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = Color(0xFF8B5CF6).copy(0.1f), shape = CircleShape) {
                                    Icon(Icons.Default.Person, null, Modifier.padding(8.dp).size(18.dp), Color(0xFF8B5CF6))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(driver?.optString("name") ?: "Generic Driver", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(driver?.optString("phone") ?: "No Phone", color = Color.Gray, fontSize = 11.sp)
                                }
                                IconButton(onClick = { /* Delete Driver */ }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedSubTab == 2) {
            val staffDataState by viewModel.staffData.collectAsState()

            LaunchedEffect(instId) {
                if (staffDataState == null && instId > 0) viewModel.fetchStaff()
            }

            Column(Modifier.fillMaxSize()) {
                Text("Staff Transport Enrollment", color = textColor, fontWeight = FontWeight.Black)
                Text("Manage transport settings for employees.", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))

                // Fallback if 'staff_list' is missing from dashboard data
                val dashboardStaff = data.optJSONArray("staff_list")
                val staffArr = if (dashboardStaff == null || dashboardStaff.length() == 0) JSONArray().apply {
                    staffDataState?.teaching_staff?.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("name", s.name)
                            put("designation", s.role)
                            put("transport_enabled", "0")
                        })
                    }
                    staffDataState?.non_teaching_staff?.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("name", s.name)
                            put("designation", s.role)
                            put("transport_enabled", "0")
                        })
                    }
                } else dashboardStaff

                if (staffArr.length() == 0) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("No staff data loaded for transport", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(staffArr.length()) { i ->
                            val staff = staffArr.getJSONObject(i)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = Color(0xFF8B5CF6).copy(0.1f), shape = CircleShape) {
                                        Icon(Icons.Default.Person, null, Modifier.padding(8.dp).size(20.dp), Color(0xFF8B5CF6))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(staff.optString("name"), fontWeight = FontWeight.Bold, color = textColor)
                                        Text(staff.optString("designation"), fontSize = 11.sp, color = Color.Gray)
                                    }
                                    // Transport toggle for staff
                                    var stState by remember { mutableStateOf(staff.optString("transport_enabled", "0") == "1") }
                                    Switch(stState, {
                                        stState = it
                                        viewModel.safeFeeApiCall("toggle_staff_transport", mapOf("staff_id" to staff.optString("id"), "state" to if(it) "1" else "0")) {}
                                    }, modifier = Modifier.scale(0.7f))

                                    Spacer(Modifier.width(8.dp))

                                    // Select Area Dropdown
                                    val areaOptions = remember(locations) {
                                        val opts = mutableListOf("Select Area")
                                        for(k in 0 until (locations?.length() ?: 0)) opts.add(locations!!.getJSONObject(k).optString("location"))
                                        opts
                                    }
                                    var selectedArea by remember { mutableStateOf("Select Area") }
                                    DropdownSelector(selectedArea, areaOptions, Modifier.width(90.dp), isDark) { selectedArea = it }

                                    Spacer(Modifier.width(4.dp))

                                    // Restore icon
                                    IconButton(onClick = { /* Action */ }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Refresh, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeeApprovalsTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    var approvals by remember { mutableStateOf(data.optJSONArray("approvals") ?: JSONArray()) }
    val textColor = if (isDark) Color.White else Color.Black
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pending Payment Approvals", color = textColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Review and confirm online fee submissions.", fontSize = 11.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        if(approvals.length() == 0) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981).copy(0.2f), modifier = Modifier.size(64.dp))
                    Text("All Clear!", color = textColor.copy(0.3f), fontWeight = FontWeight.Bold)
                    Text("No pending payments requiring approval.", fontSize = 10.sp, color = textColor.copy(0.3f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(approvals.length()) { i ->
                    val app = approvals.optJSONObject(i)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(app.optString("student_name"), fontWeight = FontWeight.ExtraBold, color = textColor)
                                    Text("Ref: ${app.optString("transaction_id")}", fontSize = 10.sp, color = Color.Gray)
                                }
                                Text("Rs. ${app.optString("amount")}", color = Color(0xFFF59E0B), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Method: ${app.optString("payment_method")}", fontSize = 12.sp, color = textColor)
                            Spacer(Modifier.height(16.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.safeFeeApiCall("approve_payment", mapOf("id" to app.optString("id"))) {
                                            // Ideally refresh dashboard data here
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("APPROVE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.safeFeeApiCall("reject_payment", mapOf("id" to app.optString("id"))) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("REJECT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    val settings = data.optJSONObject("settings") ?: JSONObject()
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("SETTINGS", "MESSAGE LOGS")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Sub-tabs
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            subTabs.forEachIndexed { idx, title ->
                Surface(
                    onClick = { selectedSubTab = idx },
                    modifier = Modifier.weight(1f),
                    color = if(selectedSubTab == idx) Color(0xFF8B5CF6) else if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(title, modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if(selectedSubTab == idx) Color.White else labelColor)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (selectedSubTab == 0) {
            // SETTINGS SUB-TAB
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Text("WhatsApp API Config", color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Use a 3rd party API (like UltraMsg, Evolution, etc.) to send automated messages.", color = labelColor, fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))

                var status by remember { mutableStateOf(if(settings.optString("whatsapp_enabled", "0") == "1") "Enabled" else "Disabled") }
                var provider by remember { mutableStateOf(settings.optString("whatsapp_provider", "UltraMsg")) }
                var instanceId by remember { mutableStateOf(settings.optString("whatsapp_instance_id", "")) }
                var apiKey by remember { mutableStateOf(settings.optString("whatsapp_api_key", "")) }
                var day1 by remember { mutableStateOf(settings.optString("whatsapp_reminder_day_1", "25")) }
                var day2 by remember { mutableStateOf(settings.optString("whatsapp_reminder_day_2", "28")) }

                // Status & Provider row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("WHATSAPP STATUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                        DropdownSelector(status, listOf("Disabled", "Enabled"), Modifier.fillMaxWidth().height(42.dp), isDark) { status = it }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("API PROVIDER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                        DropdownSelector(provider, listOf("UltraMsg", "Evolution", "Twilio", "Interakt"), Modifier.fillMaxWidth().height(42.dp), isDark) { provider = it }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("API INSTANCE ID / TWILIO SID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                PremiumTextField(instanceId, { instanceId = it }, "e.g. instance1234", Icons.Default.VpnKey, isDark)

                Spacer(Modifier.height(12.dp))
                Text("API KEY / TOKEN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                PremiumTextField(apiKey, { apiKey = it }, "Enter your API token", Icons.Default.Lock, isDark)

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("REMINDER DAY 1", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                        DropdownSelector(day1, (1..31).map { it.toString() }, Modifier.fillMaxWidth().height(42.dp), isDark) { day1 = it }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("REMINDER DAY 2", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                        DropdownSelector(day2, (1..31).map { it.toString() }, Modifier.fillMaxWidth().height(42.dp), isDark) { day2 = it }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        viewModel.safeFeeApiCall("save_whatsapp_settings", mapOf(
                            "whatsapp_enabled" to if(status == "Enabled") "1" else "0",
                            "whatsapp_provider" to provider,
                            "whatsapp_instance_id" to instanceId,
                            "whatsapp_api_key" to apiKey,
                            "whatsapp_reminder_day_1" to day1,
                            "whatsapp_reminder_day_2" to day2
                        )) {}
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SAVE WHATSAPP SETTINGS", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(30.dp))
                Text("Message Templates", color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Variables: [Student Name], [Class], [Month], [Total Amount], [Monthly Fee], [Previous Balance], [Remaining Balance], [Paid Amount], [Due Date], [Payment Date], [Receipt Link], [School Name]",
                    color = labelColor, fontSize = 9.sp, lineHeight = 12.sp)
                Spacer(Modifier.height(16.dp))

                var msgReminder by remember { mutableStateOf(settings.optString("whatsapp_msg_reminder", "Dear Parent, Fee for [Student Name] of class [Class] for [Month] is Rs.[Total Amount]. Please pay by [Due Date].")) }
                var msgConfirmation by remember { mutableStateOf(settings.optString("whatsapp_msg_payment", "Payment Received! Fee of Rs.[Paid Amount] for [Student Name] has been recorded. Current Balance: [Remaining Balance].")) }
                var msgOverdue by remember { mutableStateOf(settings.optString("whatsapp_msg_overdue", "URGENT: Fee for [Student Name] is overdue. Remaining Balance: Rs.[Remaining Balance]. Please pay immediately.")) }

                Text("FEE REMINDER (SENT ON REMINDER DAYS)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                PremiumTextField(msgReminder, { msgReminder = it }, "Template...", Icons.Default.Chat, isDark)

                Spacer(Modifier.height(12.dp))
                Text("PAYMENT CONFIRMATION (SENT INSTANTLY)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                PremiumTextField(msgConfirmation, { msgConfirmation = it }, "Template...", Icons.Default.Chat, isDark)

                Spacer(Modifier.height(12.dp))
                Text("OVERDUE FEE ALERT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                PremiumTextField(msgOverdue, { msgOverdue = it }, "Template...", Icons.Default.Chat, isDark)

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        viewModel.safeFeeApiCall("save_whatsapp_settings", mapOf(
                            "whatsapp_msg_reminder" to msgReminder,
                            "whatsapp_msg_payment" to msgConfirmation,
                            "whatsapp_msg_overdue" to msgOverdue
                        )) {}
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("UPDATE ALL TEMPLATES", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(60.dp))
            }
        } else {
            // MESSAGE LOGS SUB-TAB
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Message History", color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Track sent messages and their delivery status.", color = labelColor, fontSize = 11.sp)
                    }
                    Button(onClick = { /* Clear Logs */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(0.1f)),
                        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("CLEAR LOGS", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))

                val logs = data.optJSONArray("whatsapp_logs") ?: JSONArray()
                if (logs.length() == 0) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, null, tint = labelColor.copy(0.2f), modifier = Modifier.size(64.dp))
                            Text("No Message Logs Yet", color = labelColor.copy(0.5f), fontWeight = FontWeight.Bold)
                            Text("Wait for scheduled reminders or payments.", color = labelColor.copy(0.3f), fontSize = 10.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(logs.length()) { i ->
                            val log = logs.getJSONObject(i)
                            LogCard(log, isDark, textColor, labelColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogCard(log: JSONObject, isDark: Boolean, textColor: Color, labelColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if(isDark) Color.White.copy(0.04f) else Color.Black.copy(0.02f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(log.optString("date"), color = labelColor, fontSize = 10.sp)
                Text(log.optString("status").uppercase(), color = if(log.optString("status") == "sent") Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(4.dp))
            Text(log.optString("student_name"), color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(log.optString("number"), color = labelColor, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFF3B82F6).copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(log.optString("type").uppercase(), color = Color(0xFF3B82F6), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(log.optString("response"), color = textColor.copy(0.6f), fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFeeDetailScreen(
    studentId: Int,
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    isBottomSheet: Boolean = false
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
    val userRole by viewModel.userRole.collectAsState()
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0

    val ledgerData = remember { mutableStateListOf<JSONObject>() }
    val studentInfo = remember { mutableStateOf<JSONObject?>(null) }
    val feeTypes = remember { mutableStateListOf<JSONObject>() }
    val methods = remember { mutableStateListOf<String>() }
    val banks = remember { mutableStateListOf<String>() }
    val selectedFeeEntry = remember { mutableStateOf<JSONObject?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val errorDetail = remember { mutableStateOf("") }
    val institutionName = remember { mutableStateOf("OFFICIAL SCHOOL RECEIPT") }

    val now = java.util.Calendar.getInstance()
    var monthFilter by remember { mutableStateOf(java.text.SimpleDateFormat("MMMM", java.util.Locale.US).format(now.time)) }
    var yearFilter by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(now.time)) }
    var typeFilter by remember { mutableStateOf("All Types") }
    val monthsList = listOf("All", "Unpaid", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    fun refresh() {
        isLoading.value = true
        hasError.value = false
        val params = mapOf(
            "student_id" to studentId.toString(),
            "institution_id" to instId.toString(),
            "month" to monthFilter,
            "year" to yearFilter
        )
        viewModel.safeApiCall("GET_STUDENT_FEE_LEDGER", params) { json ->
            isLoading.value = false
            val status = json.optString("status", "error")
            if (status == "success") {
                hasError.value = false
                ledgerData.clear()
                json.optJSONArray("ledger")?.let { arr ->
                    for (i in 0 until arr.length()) ledgerData.add(arr.getJSONObject(i))
                }
                val studentObj = json.optJSONObject("student_info")
                if (studentObj != null) studentInfo.value = studentObj
                institutionName.value = json.optString("institution_name", "OFFICIAL SCHOOL RECEIPT")
                feeTypes.clear()
                json.optJSONArray("fee_types")?.let { arr ->
                    for (i in 0 until arr.length()) feeTypes.add(arr.getJSONObject(i))
                }
            } else {
                hasError.value = true
                errorDetail.value = json.optString("message", "Failed to load fee data. Check your connection or session.")
            }
        }
    }

    val errorMsg by viewModel.errorMsg.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(errorMsg) {
        if (errorMsg.isNotEmpty()) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(studentId, monthFilter, yearFilter) {
        refresh()
        if (methods.isEmpty()) {
            viewModel.safeApiCall("GET_PAYMENT_METADATA", mapOf("institution_id" to instId.toString())) { json ->
                methods.clear()
                banks.clear()
                json?.optJSONArray("methods")?.let { arr -> for (i in 0 until arr.length()) methods.add(arr.getString(i)) }
                json?.optJSONArray("banks")?.let { arr -> for (i in 0 until arr.length()) banks.add(arr.getString(i)) }
            }
        }
    }

    if (isBottomSheet) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            StudentFeeDetailBody(studentId, viewModel, onBack, isDark, textColor, labelColor, methods, banks, ledgerData, studentInfo, isLoading, hasError, errorDetail, selectedFeeEntry, institutionName, feeTypes, instId, userRole, monthFilter, yearFilter, typeFilter, { monthFilter = it }, { yearFilter = it }, { typeFilter = it }, { refresh() })
        }
    } else {
        Scaffold(
            containerColor = bgColor,
            topBar = {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, Modifier.background(Color.White.copy(0.05f), CircleShape)) {
                        Icon(Icons.Default.ArrowBack, null, tint = if(isDark) Color.White else Color.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Payment Working", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
        ) { padding ->
            // Loading State
            if (isLoading.value) {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
                return@Scaffold
            }
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
                StudentFeeDetailBody(studentId, viewModel, onBack, isDark, textColor, labelColor, methods, banks, ledgerData, studentInfo, isLoading, hasError, errorDetail, selectedFeeEntry, institutionName, feeTypes, instId, userRole, monthFilter, yearFilter, typeFilter, { monthFilter = it }, { yearFilter = it }, { typeFilter = it }, { refresh() })
            }
        }
    }
}

@Composable
fun StudentFeeDetailBody(
    studentId: Int,
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    isDark: Boolean,
    textColor: Color,
    labelColor: Color,
    methods: MutableList<String>,
    banks: MutableList<String>,
    ledgerData: MutableList<JSONObject>,
    studentInfo: MutableState<JSONObject?>,
    isLoading: MutableState<Boolean>,
    hasError: MutableState<Boolean>,
    errorDetail: MutableState<String>,
    selectedFeeEntry: MutableState<JSONObject?>,
    institutionName: MutableState<String>,
    feeTypes: MutableList<JSONObject>,
    instId: Int,
    userRole: String,
    monthFilter: String,
    yearFilter: String,
    typeFilter: String,
    onMonthChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    refresh: () -> Unit
) {
    // Error State
    if (hasError.value) {
        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444).copy(0.7f), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Could Not Load Data", color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(errorDetail.value, color = labelColor, fontSize = 11.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { refresh() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) {
                    Text("RETRY", fontWeight = FontWeight.Black)
                }
            }
        }
        return
    }

    // Student Header
    studentInfo.value?.let { info ->
        StudentDetailHeader(info, ledgerData, isDark, userRole, institutionName.value)
    }

    Spacer(Modifier.height(16.dp))

    // Add Fee / Filter Section
    AddFeeSection(
        studentId = studentId,
        instId = instId,
        feeTypes = feeTypes,
        viewModel = viewModel,
        isDark = isDark,
        month = monthFilter,
        year = yearFilter,
        selectedType = typeFilter,
        onMonthChange = onMonthChange,
        onYearChange = onYearChange,
        onTypeChange = onTypeChange,
        onDone = { refresh() }
    )

    Spacer(Modifier.height(16.dp))

    // Ledger List aggregation with filtering
    val filteredLedger = ledgerData.filter { item ->
        typeFilter == "All Types" || item.optString("fee_type") == typeFilter
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        if (filteredLedger.isNotEmpty()) {
            val label = when {
                monthFilter == "All" -> "ALL RECORDS"
                monthFilter == "Unpaid" -> "OUTSTANDING ARREARS & UNPAID"
                else -> "LEDGER FOR ${monthFilter.uppercase()} ${yearFilter.uppercase()}"
            }
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = labelColor, modifier = Modifier.padding(bottom = 8.dp))
            filteredLedger.forEach { entry ->
                FeeEntryItem(entry, viewModel, isDark, userRole, { selectedFeeEntry.value = it }, { refresh() })
                Spacer(Modifier.height(8.dp))
            }
        } else if (!isLoading.value) {
            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Text("No matching records found", color = labelColor, fontSize = 12.sp)
            }
        }
    }

    // PAY NOW BOX
    val totalUnpaid = ledgerData.filter { it.optString("Status") == "Unpaid" }.sumOf { it.optDouble("amount") }
    
    val selectedMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val cal = java.util.Calendar.getInstance()
    val currentMonthIdx = cal.get(java.util.Calendar.MONTH)
    val currentYearNum = cal.get(java.util.Calendar.YEAR)
    val selMonthIdx = selectedMonths.indexOf(monthFilter)
    val selYearNum = yearFilter.toIntOrNull() ?: currentYearNum
    val monthDiff = if (selMonthIdx != -1) (selYearNum - currentYearNum) * 12 + (selMonthIdx - currentMonthIdx) else 0
    val tuitionFee = studentInfo.value?.optDouble("tuition_fee") ?: 0.0
    val transportFee = studentInfo.value?.optDouble("transport_fee") ?: 0.0
    val upcomingFeeTotal = if (monthDiff > 0) monthDiff * (tuitionFee + transportFee) else 0.0
    val effectiveTotalToPay = totalUnpaid + upcomingFeeTotal

    if (effectiveTotalToPay > 0 && !isLoading.value) {
        Spacer(Modifier.height(16.dp))
        Surface(
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(0.4f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("TOTAL TO PAY", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Rs. ${String.format("%,.0f", effectiveTotalToPay)}", color = Color(0xFF10B981), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(14.dp))
                var customPayAmt by remember(effectiveTotalToPay) { mutableStateOf(effectiveTotalToPay.toInt().toString()) }
                PremiumTextField(customPayAmt, { customPayAmt = it }, "Amount", Icons.Default.Money, isDark, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val payload = JSONObject()
                        payload.put("amount", customPayAmt.toDoubleOrNull() ?: effectiveTotalToPay)
                        payload.put("bulk", true)
                        payload.put("fee_type", "Bulk Payment")
                        payload.put("student_id", studentId)
                        payload.put("institution_id", instId)
                        selectedFeeEntry.value = payload
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Payments, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("PAY NOW", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
    
    Spacer(Modifier.height(40.dp))

    selectedFeeEntry.value?.let { entry ->
        CollectionModal(entry = entry, methods = methods, banks = banks, viewModel = viewModel, isDark = isDark, instId = instId, onDismiss = { selectedFeeEntry.value = null }, onDone = { selectedFeeEntry.value = null; refresh() })
    }
}

@Composable
fun RowScope.StatSmallBadge(label: String, value: String, color: Color, isDark: Boolean) {
    Column(
        Modifier.background(if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp).weight(1f)
    ) {
        Text(label, color = if (isDark) Color.White.copy(0.4f) else Color.Black.copy(0.4f), fontSize = 8.sp, fontWeight = FontWeight.Black)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun StudentDetailHeader(info: JSONObject, ledger: List<JSONObject>, isDark: Boolean, userRole: String, instName: String = "") {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)

    val totalPending = ledger.filter { it.optString("Status") == "Unpaid" }.sumOf { it.optDouble("amount") }
    var showReceipt by remember { mutableStateOf(false) }

    if (showReceipt) {
        PaymentReceiptModal(info = info, ledger = ledger, instName = instName) { showReceipt = false }
    }

    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = cardColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.1f)),
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(Color(0xFF3B82F6).copy(0.1f)), Alignment.Center) {
                    val rawPic = info.optString("profile_pic")
                    val picUrl = if (rawPic.startsWith("http")) rawPic 
                                else if (rawPic.isNotEmpty() && rawPic != "user.png") {
                                    if (rawPic.startsWith("uploads/")) "https://wantuch.pk/$rawPic"
                                    else if (rawPic.contains("/")) "https://wantuch.pk/assets/$rawPic"
                                    else "https://wantuch.pk/uploads/students/$rawPic"
                                }
                                else ""

                    if (picUrl.isNotEmpty()) {
                        AsyncImage(model = picUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(info.optString("full_name").take(2).uppercase(), color = Color(0xFF3B82F6), fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(info.optString("full_name"), color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("${info.optString("class_name")} - ${info.optString("section_name")}", color = labelColor, fontSize = 11.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("PENDING PAYMENT", color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(String.format("%,.0f", totalPending), color = Color(0xFF3B82F6), fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }

            if (userRole != "Student") {
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var payComplete by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PAY COMPLETE?", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = payComplete, onCheckedChange = { payComplete = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF3B82F6)))
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { showReceipt = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Receipt, null, Modifier.size(16.dp), tint = textColor)
                        Spacer(Modifier.width(8.dp))
                        Text("RECEIPT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
fun AddFeeSection(
    studentId: Int,
    instId: Int,
    feeTypes: List<JSONObject>,
    viewModel: WantuchViewModel,
    isDark: Boolean,
    month: String,
    year: String,
    selectedType: String,
    onMonthChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val typeOptions = remember(feeTypes.size) { listOf("All Types") + feeTypes.map { it.optString("type_name") } }
    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val years = (2022..2030).map { it.toString() }
    val labelColor = if (isDark) Color.White.copy(0.5f) else Color.Black.copy(0.5f)

    val now = java.util.Date()
    val effectiveMonth = when (month) {
        "All", "Unpaid" -> java.text.SimpleDateFormat("MMMM", java.util.Locale.US).format(now)
        else -> month
    }
    val effectiveYear = when (year) {
        "All" -> java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(now)
        else -> year
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.weight(1f)) {
                Text("MONTH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                Spacer(Modifier.height(4.dp))
                DropdownSelector(effectiveMonth, months, Modifier.fillMaxWidth(), isDark) { onMonthChange(it) }
            }
            Column(Modifier.weight(1f)) {
                Text("YEAR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                Spacer(Modifier.height(4.dp))
                DropdownSelector(effectiveYear, years, Modifier.fillMaxWidth(), isDark) { onYearChange(it) }
            }
            Column(Modifier.weight(1f)) {
                Text("FEE TYPE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
                Spacer(Modifier.height(4.dp))
                DropdownSelector(selectedType, typeOptions, Modifier.fillMaxWidth(), isDark, onTypeChange)
            }
        }
    }
}

@Composable
fun FeeEntryItem(entry: JSONObject, viewModel: WantuchViewModel, isDark: Boolean, userRole: String, onCollect: (JSONObject) -> Unit, onDone: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)

    val isStatic = entry.optBoolean("is_static", false)
    val status = if (isStatic) "Expected" else entry.optString("Status")
    val isPaid = status == "Paid"
    val statusColor = when {
        isPaid -> Color(0xFF10B981)
        isStatic -> Color(0xFF3B82F6)
        else -> Color(0xFFEF4444)
    }

    Surface(
        Modifier.fillMaxWidth(),
        color = cardColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.optString("fee_type"), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                val subText = if (isStatic) "Not yet in database" else "${entry.optString("fee_month")} ${entry.optString("fee_year")}"
                Text(subText, color = labelColor, fontSize = 10.sp)
            }

            Text(String.format("%,.0f", entry.optDouble("amount")), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp))

            Surface(
                onClick = {
                    if (!isPaid && !userRole.equals("Student", ignoreCase = true)) {
                        onCollect(entry)
                    }
                },
                color = statusColor.copy(0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    status.uppercase(),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (!userRole.equals("Student", ignoreCase = true) && !isStatic) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { /* Edit */ }, Modifier.size(30.dp)) {
                    Icon(Icons.Default.Edit, null, tint = labelColor, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = {
                    viewModel.safeFeeApiCall("DELETE_STUDENT_FEE", mapOf("id" to entry.optString("id"))) { onDone() }
                }, Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444).copy(0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun CollectionModal(
    entry: JSONObject,
    methods: List<String>,
    banks: List<String>,
    viewModel: WantuchViewModel,
    isDark: Boolean,
    instId: Int,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)

    var amount by remember { mutableStateOf(entry.optDouble("amount").toString()) }
    var method by remember { mutableStateOf(methods.firstOrNull() ?: "Cash") }
    var bank by remember { mutableStateOf(banks.firstOrNull() ?: "") }
    var transId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Record Payment", fontSize = 18.sp, fontWeight = FontWeight.Black, color = textColor)
                Spacer(Modifier.height(4.dp))
                Text(entry.optString("fee_type"), color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Text("Amount Paying", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                PremiumTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    placeholder = "Amount",
                    icon = Icons.Default.Edit,
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))

                Text("Payment Method", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                DropdownSelector(method, methods.ifEmpty { listOf("Cash", "Online") }, Modifier.fillMaxWidth(), isDark) { method = it }

                if (method == "Online" || method == "Cheque") {
                    Spacer(Modifier.height(12.dp))
                    Text("Bank / Account", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    DropdownSelector(bank, banks.ifEmpty { listOf("Main Account") }, Modifier.fillMaxWidth(), isDark) { bank = it }
                    Spacer(Modifier.height(12.dp))
                    Text("Transaction ID", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    PremiumTextField(
                        value = transId,
                        onValueChange = { transId = it },
                        placeholder = "ID...",
                        icon = Icons.Default.Edit,
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = labelColor) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            if (entry.optBoolean("bulk", false)) {
                                viewModel.safeFeeApiCall("COLLECT_BULK_FEE", mapOf(
                                    "student_id" to entry.optString("student_id"),
                                    "institution_id" to entry.optString("institution_id"),
                                    "amount" to amount,
                                    "payment_method" to method,
                                    "bank_account" to bank,
                                    "transaction_id" to transId
                                )) { json ->
                                    isLoading = false
                                    if (json.optString("status") == "success") {
                                        onDone()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            json.optString("message", "Payment failed"),
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } else {
                                val params = mutableMapOf(
                                    "id" to entry.optString("id"),
                                    "amount" to amount,
                                    "payment_method" to method,
                                    "bank_account" to bank,
                                    "transaction_id" to transId,
                                    "institution_id" to instId.toString()
                                )
                                // If it's a static preview (ID 0), pass context
                                if (entry.optString("id") == "0") {
                                    params["student_id"] = entry.optString("student_id")
                                    params["fee_month"] = entry.optString("fee_month")
                                    params["fee_year"] = entry.optString("fee_year")
                                    params["fee_type_id"] = entry.optString("fee_type_id")
                                }
                                viewModel.safeFeeApiCall("COLLECT_FEE", params) { json ->
                                    isLoading = false
                                    if (json.optString("status") == "success") {
                                        onDone()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            json.optString("message", "Payment failed"),
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        enabled = !isLoading
                    ) {
                        Text(if (isLoading) "SAVING..." else "SAVE", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentReceiptModal(
    info: org.json.JSONObject,
    ledger: List<org.json.JSONObject>,
    instName: String,
    onDismiss: () -> Unit
) {
    val paidItems = ledger.filter { it.optString("Status").equals("Paid", ignoreCase = true) }
    val totalPaid = paidItems.sumOf { it.optDouble("amount") }
    val timestamp = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.US).format(java.util.Date())
    val exactDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(java.util.Date())
    val receiptNo = "#" + (100000..999999).random()

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
    val realInstName = if (instName.isNotEmpty()) instName 
                     else prefs.getString("institution_name", "School") ?: "School"

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val view = androidx.compose.ui.platform.LocalView.current
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp)
        ) {
            Column {
                // Diagonal Blue Header with Logo
                Box(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width * 0.48f, 0f)
                            lineTo(size.width * 0.40f, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, color = Color(0xFF3B82F6))
                        drawLine(color = Color(0xFF3B82F6), start = androidx.compose.ui.geometry.Offset(0f, size.height - 2), end = androidx.compose.ui.geometry.Offset(size.width, size.height - 2), strokeWidth = 8f)
                    }
                    
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.padding(start = 16.dp, end = 8.dp).weight(1.2f)) {
                            Text(realInstName.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, lineHeight = 14.sp)
                            Text("OFFICIAL PAYMENT RECORD", color = Color.White.copy(0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                        Row(Modifier.weight(1f).padding(end = 16.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("PAYMENT RECEIPT", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Black)
                                Text(receiptNo, fontSize = 9.sp, color = Color.Gray)
                                Text(timestamp, fontSize = 9.sp, color = Color.Gray)
                            }
                            Spacer(Modifier.width(8.dp))
                            // Placeholder Graphic for School Logo
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.School, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Student Information
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text("STUDENT NAME", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                                Text(info.optString("full_name").uppercase(), fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row {
                                Text("CLASS / SEC", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                                Text("${info.optString("class_name")} - ${info.optString("section_name")}".uppercase(), fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                        Column(Modifier.weight(0.8f)) {
                            Row {
                                Text("S/O NAME", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                                Text(info.optString("father_name").uppercase().ifEmpty { "N/A" }, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row {
                                Text("ROLL NO", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                                Text(info.optString("adm_no").ifEmpty { "N/A" }, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Divider(color = Color.LightGray.copy(0.5f))

                // Table Headers
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("FEE DESCRIPTION", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Black, modifier = Modifier.weight(2f))
                    Text("MONTH/YEAR", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                    Text("PAYMENT MODE", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                    Text("STATUS", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("AMOUNT", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
                
                Divider(color = Color.Black)

                // Table Body with Stamp Overlay
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f, fill = false)) {
                    // Transparent Table List
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(paidItems) { item ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.optString("fee_type"), fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                                Text("${item.optString("fee_month")} ${item.optString("fee_year")}", fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                                Text(item.optString("payment_method", "Cash").ifEmpty { "Cash" }, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                Text("Paid", fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(String.format("%,.0f", item.optDouble("amount")), fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                            Divider(color = Color.LightGray.copy(0.4f))
                        }
                        
                        // Footer Totals Inline
                        item {
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth().background(Color(0xFFF1F5F9).copy(0.5f)).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                                Text("Total Paid", fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 32.dp))
                                Text("Rs. " + String.format("%,.0f", totalPaid), fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Black)
                            }
                            Divider(color = Color.Black, thickness = 2.dp)
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                Text("GRAND TOTAL", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 24.dp))
                                Text("Rs. " + String.format("%,.0f", totalPaid), fontSize = 14.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Black)
                            }
                            Divider(color = Color.Black, thickness = 2.dp)
                        }
                    }

                    // Watermark Stamp
                    if (paidItems.isNotEmpty()) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
                            val stampColor = Color.Red.copy(alpha = 0.25f)
                            drawContext.canvas.nativeCanvas.rotate(-20f, size.width / 2, size.height / 2)
                            
                            drawCircle(color = stampColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
                            drawCircle(color = stampColor, radius = size.minDimension / 2 - 12f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(64, 255, 0, 0)
                                textAlign = android.graphics.Paint.Align.CENTER
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            paint.textSize = 48f
                            drawContext.canvas.nativeCanvas.drawText("PAID", size.width / 2, size.height / 2 + 16f, paint)
                            paint.textSize = 14f
                            drawContext.canvas.nativeCanvas.drawText("***", size.width / 2, size.height / 2 - 24f, paint)
                            paint.textSize = 12f
                            drawContext.canvas.nativeCanvas.drawText("DATE: $exactDate", size.width / 2, size.height / 2 + 40f, paint)
                            
                            drawContext.canvas.nativeCanvas.save()
                            drawContext.canvas.nativeCanvas.rotate(180f, size.width / 2, size.height / 2)
                            paint.textSize = 16f
                            drawContext.canvas.nativeCanvas.drawText("FINANCE DEPARTMENT", size.width / 2, 28f, paint)
                            drawContext.canvas.nativeCanvas.restore()
                            paint.textSize = 18f
                            drawContext.canvas.nativeCanvas.drawText("APPROVED", size.width / 2, 28f, paint)
                        }
                    }
                }

                // Action Buttons Footer
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray), modifier = Modifier.weight(1f)) {
                        Text("CLOSE", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = { 
                        try {
                            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            val bgPaint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE }
                            canvas.drawRect(0f, 0f, view.width.toFloat(), view.height.toFloat(), bgPaint)
                            view.draw(canvas)
                            
                            val cv = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "Receipt_${receiptNo}.jpg")
                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Wantuch")
                                }
                            }
                            val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                            if (uri != null) {
                                context.contentResolver.openOutputStream(uri)?.use { 
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it) 
                                }
                                android.widget.Toast.makeText(context, "Receipt saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } catch(e: Exception) {
                             e.printStackTrace()
                             android.widget.Toast.makeText(context, "Failed to completely save image", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("SAVE JPG", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun dateToMonth(millis: Long): String {
    val date = java.util.Date(millis)
    val formatter = java.text.SimpleDateFormat("MMMM", java.util.Locale.ENGLISH)
    return formatter.format(date)
}
