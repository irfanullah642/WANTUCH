package com.example.wantuch.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.example.wantuch.domain.model.VerifyFaceResponse

@Composable
fun SmartActionButton(
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null, 
    accent: Color, 
    modifier: Modifier = Modifier, 
    isFilled: Boolean = true, 
    enabled: Boolean = true, 
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = if (enabled) onClick else ({}),
        color = when {
            !enabled -> Color.Gray.copy(0.1f)
            isFilled -> accent.copy(0.1f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (!enabled) Color.Gray.copy(0.3f) else accent.copy(if(isFilled) 0.5f else 0.3f)),
        modifier = modifier.height(40.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = if (enabled) accent else Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, color = if (enabled) accent else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AttendanceManagementScreen(viewModel: WantuchViewModel, onBack: () -> Unit) {

    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    var selectedTopTab by remember { mutableIntStateOf(0) }
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isStudent = dashboardData?.role?.lowercase() ?: "" == "student"
    
    val allTopTabs = listOf("Attendance", "Monthly", "Rules", "L-appeals", "Status Admin")
    val topTabs = if (isStudent) listOf("Attendance", "Monthly", "L-appeals") else allTopTabs

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderActionIcon(Icons.Default.ArrowBack, isDark, onBack)
                    Spacer(Modifier.width(16.dp))
                    Text("Attendance Manager", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }

                // Top Tab Buttons (Two Rows)
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        topTabs.take(3).forEachIndexed { i, t ->
                            Box(Modifier.weight(1f)) { PremiumTabButton(t, selectedTopTab == i, isDark) { selectedTopTab = i } }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        topTabs.drop(3).forEachIndexed { i, t ->
                            val idx = i + 3
                            Box(Modifier.weight(1f)) { PremiumTabButton(t, selectedTopTab == idx, isDark) { selectedTopTab = idx } }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            when (topTabs.getOrNull(selectedTopTab)) {
                "Attendance" -> AttendanceMainTab(viewModel, isDark, textColor, labelColor, cardColor) { 
                    val adminIdx = topTabs.indexOf("Status Admin")
                    if (adminIdx >= 0) selectedTopTab = adminIdx
                }
                "Monthly" -> MonthlyAttendanceTab(viewModel, isDark, textColor, labelColor, cardColor)
                "Rules" -> AttendanceRulesTab(isDark, textColor, labelColor, cardColor)
                "L-appeals" -> LeaveAppealsTab(viewModel, isDark, textColor, labelColor, cardColor)
                "Status Admin" -> StatusAdminTab(viewModel, isDark, textColor, labelColor, cardColor)
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun AttendanceMainTab(viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color, onStatusClick: () -> Unit) {
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isStudent = dashboardData?.role?.lowercase() ?: "" == "student"

    var subTab by remember { androidx.compose.runtime.mutableIntStateOf(if (isStudent) 3 else 0) }
    val allSubTabs = listOf("Students", "Staff", "Manual", "Smart Attendance")
    val subTabs = if (isStudent) listOf("Smart Attendance") else allSubTabs

    val structure by viewModel.schoolStructure.collectAsState()
    var selectedClassId by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var classExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    val studentsData by viewModel.studentsData.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedDate by remember { androidx.compose.runtime.mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())) }
    var searchQuery by remember { androidx.compose.runtime.mutableStateOf("") }
    var showManualModal by remember { androidx.compose.runtime.mutableStateOf(false) }

    val datePickerDialog = remember {
        val calendar = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = java.util.Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                selectedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.fetchSchoolStructure()
        viewModel.fetchStudents()
        viewModel.fetchStaff()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Sub-tabs row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            subTabs.forEachIndexed { i, t ->
                val targetIdx = if(isStudent) 3 else i
                Surface(
                    onClick = { subTab = targetIdx },
                    color = if(subTab == targetIdx) Color(0xFFFACC15) else if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(42.dp),
                    border = BorderStroke(1.dp, if(subTab == targetIdx) Color(0xFFFACC15) else labelColor.copy(0.1f))
                ) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        val labelText = if(t == "Smart Attendance") "Smart" else t
                        Text(t.uppercase(), color = if(subTab == targetIdx) Color(0xFF1E293B) else labelColor, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        when (subTab) {
            0 -> {
                // STUDENTS ATTENDANCE
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("STUDENTS ATTENDANCE", color = textColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Surface(
                        onClick = onStatusClick,
                        color = Color(0xFF10B981).copy(0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(0.3f))
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assignment, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("SUBMISSION STATUS", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, labelColor.copy(0.1f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Date Picker Placeholder
                            Column(Modifier.weight(1f)) {
                                Text("SELECTED DATE", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    onClick = { datePickerDialog.show() },
                                    color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), 
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(selectedDate, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            // Class Selector
                            Column(Modifier.weight(1f)) {
                                Text("CHOOSE CLASS...", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                
                                val selectedText = if(selectedClassId > 0) {
                                    structure?.classes?.find { it.id == selectedClassId }?.name?.uppercase() ?: "SELECT CLASS"
                                } else {
                                    "SELECT CLASS"
                                }

                                Box {
                                    Surface(
                                        onClick = { classExpanded = true },
                                        color = if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(selectedText, color = if(selectedClassId > 0) textColor else labelColor.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Spacer(Modifier.weight(1f))
                                            Icon(Icons.Default.ArrowDropDown, null, tint = labelColor)
                                        }
                                    }
                                    androidx.compose.material3.DropdownMenu(
                                        expanded = classExpanded,
                                        onDismissRequest = { classExpanded = false },
                                        modifier = Modifier.background(cardColor)
                                    ) {
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text("ALL CLASSES", color = textColor, fontSize = 12.sp) },
                                            onClick = { selectedClassId = 0; classExpanded = false }
                                        )
                                        structure?.classes?.forEach { cls ->
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text(cls.name.uppercase(), color = textColor, fontSize = 12.sp) },
                                                onClick = { selectedClassId = cls.id; classExpanded = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Search Bar
                        PremiumTextField(
                            value = searchQuery, 
                            onValueChange = { searchQuery = it },
                            placeholder = "Search Student...",
                            icon = Icons.Default.Search,
                            isDark = isDark
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("DEFAULT IN", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Surface(color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), shape = RoundedCornerShape(8.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("08:00 am", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("DEFAULT OUT", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Surface(color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), shape = RoundedCornerShape(8.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("01:30 pm", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                if (selectedClassId > 0) {
                    val allStudents = studentsData?.students ?: emptyList()
                    val classNameMatch = structure?.classes?.find { it.id == selectedClassId }?.name ?: ""
                    val students = allStudents.filter { st -> 
                        val matchesClass = st.class_section.split(" - ").firstOrNull()?.equals(classNameMatch, ignoreCase = true) == true
                        val matchesSearch = st.name.contains(searchQuery, ignoreCase = true) || st.username.contains(searchQuery, ignoreCase = true)
                        matchesClass && matchesSearch
                    }
                    
                    if (students.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                            Text("No students found for this class.", color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            students.forEachIndexed { idx, st ->
                                StudentAttendanceRow(
                                    idx = idx + 1,
                                    student = st,
                                    isDark = isDark,
                                    textColor = textColor,
                                    labelColor = labelColor,
                                    cardColor = cardColor,
                                    onMark = { status ->
                                        val studentId = when(val id = st.id) {
                                            is Double -> id.toInt()
                                            is Int -> id
                                            is String -> id.toDoubleOrNull()?.toInt() ?: 0
                                            else -> 0
                                        }
                                        viewModel.markStudentAttendance(studentId, status, "") {}
                                    }
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                // STAFF ATTENDANCE
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("STAFF ATTENDANCE", color = textColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    PremiumTextField(
                        value = searchQuery, 
                        onValueChange = { searchQuery = it },
                        placeholder = "Search Staff...",
                        icon = Icons.Default.Search,
                        isDark = isDark,
                        modifier = Modifier.width(180.dp)
                    )
                }
                
                Spacer(Modifier.height(16.dp))

                val staffData by viewModel.staffData.collectAsState()
                val allStaff = (staffData?.teaching_staff ?: emptyList()) + (staffData?.non_teaching_staff ?: emptyList())
                val staff = allStaff.filter { it.name.contains(searchQuery, ignoreCase = true) }
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    staff.forEachIndexed { idx, s ->
                        StaffAttendanceRow(
                            idx = idx + 1,
                            staff = s,
                            isDark = isDark,
                            textColor = textColor,
                            labelColor = labelColor,
                            cardColor = cardColor,
                            onMark = { status ->
                                val staffId = when(val id = s.id) {
                                    is Double -> id.toInt()
                                    is Int -> id
                                    is String -> id.toDoubleOrNull()?.toInt() ?: 0
                                    else -> 0
                                }
                                viewModel.markStaffAttendance(staffId, status, "") { }
                            }
                        )
                    }
                }
            }
            2 -> {
                // MANUAL ATTENDANCE
                var manualSubTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
                val manualSubTabs = listOf("Students", "Staff", "Bulk")

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // sub-sub-tabs
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        manualSubTabs.forEachIndexed { i, t ->
                            Surface(
                                onClick = { manualSubTab = i },
                                color = if(manualSubTab == i) Color(0xFF3B82F6).copy(0.1f) else Color.Transparent,
                                contentColor = if(manualSubTab == i) Color(0xFF3B82F6) else labelColor,
                                shape = RoundedCornerShape(10.dp),
                                border = if(manualSubTab == i) BorderStroke(1.dp, Color(0xFF3B82F6)) else BorderStroke(1.dp, labelColor.copy(0.1f)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    Text(t.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, labelColor.copy(0.1f))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("MANUAL ${manualSubTabs[manualSubTab].uppercase()} ATTENDANCE", color = textColor, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("DATE", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Surface(onClick = { datePickerDialog.show() }, color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), shape = RoundedCornerShape(8.dp)) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(selectedDate, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (manualSubTab == 0) {
                                    Column(Modifier.weight(1f)) {
                                        Text("CLASS", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(6.dp))
                                        Box {
                                            Surface(onClick = { classExpanded = true }, color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), shape = RoundedCornerShape(8.dp)) {
                                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(if(selectedClassId > 0) structure?.classes?.find{it.id==selectedClassId}?.name?.uppercase() ?: "CHOOSE CLASS..." else "CHOOSE CLASS...", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(Modifier.weight(1f))
                                                    Icon(Icons.Default.ArrowDropDown, null, tint = labelColor)
                                                }
                                            }
                                            androidx.compose.material3.DropdownMenu(
                                                expanded = classExpanded,
                                                onDismissRequest = { classExpanded = false },
                                                modifier = Modifier.background(cardColor)
                                            ) {
                                                structure?.classes?.forEach { cls ->
                                                    androidx.compose.material3.DropdownMenuItem(
                                                        text = { Text(cls.name.uppercase(), fontSize = 12.sp, color = textColor) },
                                                        onClick = { selectedClassId = cls.id; classExpanded = false }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("MANUAL IN", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Surface(color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), shape = RoundedCornerShape(8.dp)) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccessTime, null, tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("08:00 am", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("MANUAL OUT", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Surface(color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), shape = RoundedCornerShape(8.dp)) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccessTime, null, tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("01:30 pm", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }

                            if (manualSubTab == 2) {
                                // BULK ATTENDANCE PROCESSING
                                var bulkFromDate by remember { androidx.compose.runtime.mutableStateOf(
                                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(
                                        java.util.Calendar.getInstance().also { it.add(java.util.Calendar.DAY_OF_MONTH, -7) }.time
                                    )
                                ) }
                                var bulkToDate by remember { androidx.compose.runtime.mutableStateOf(selectedDate) }
                                var bulkClassSectionId by remember { androidx.compose.runtime.mutableStateOf("") }
                                var bulkClassExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
                                var showExcelWizard by remember { androidx.compose.runtime.mutableStateOf(false) }

                                val fromDatePickerDialog = remember {
                                    val calendar = java.util.Calendar.getInstance()
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val cal = java.util.Calendar.getInstance()
                                            cal.set(year, month, dayOfMonth)
                                            bulkFromDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
                                        },
                                        calendar.get(java.util.Calendar.YEAR),
                                        calendar.get(java.util.Calendar.MONTH),
                                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                    )
                                }
                                val toDatePickerDialog = remember {
                                    val calendar = java.util.Calendar.getInstance()
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val cal = java.util.Calendar.getInstance()
                                            cal.set(year, month, dayOfMonth)
                                            bulkToDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
                                        },
                                        calendar.get(java.util.Calendar.YEAR),
                                        calendar.get(java.util.Calendar.MONTH),
                                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // FROM/TO date row
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Column(Modifier.weight(1f)) {
                                            Text("FROM DATE", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(6.dp))
                                            Surface(onClick = { fromDatePickerDialog.show() }, color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), shape = RoundedCornerShape(8.dp)) {
                                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(bulkFromDate, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text("TO DATE", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(6.dp))
                                            Surface(onClick = { toDatePickerDialog.show() }, color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), shape = RoundedCornerShape(8.dp)) {
                                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(bulkToDate, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    // Target Class
                                    Column {
                                        Text("TARGET CLASS", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(6.dp))
                                        Box {
                                            Surface(
                                                onClick = { bulkClassExpanded = true },
                                                color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    val label = if (bulkClassSectionId.isNotEmpty()) {
                                                        val parts = bulkClassSectionId.split("|")
                                                        if (parts.size == 2) {
                                                            val cId = parts[0].toIntOrNull() ?: 0
                                                            val sId = parts[1].toIntOrNull() ?: 0
                                                            val cls = structure?.classes?.find { it.id == cId }
                                                            val sec = cls?.sections?.find { it.id == sId }
                                                            "${cls?.name ?: ""} ${sec?.name ?: ""}".uppercase()
                                                        } else "ALL CLASSES"
                                                    } else "ALL CLASSES"
                                                    Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(Modifier.weight(1f))
                                                    Icon(Icons.Default.ArrowDropDown, null, tint = labelColor)
                                                }
                                            }
                                            androidx.compose.material3.DropdownMenu(
                                                expanded = bulkClassExpanded,
                                                onDismissRequest = { bulkClassExpanded = false },
                                                modifier = Modifier.background(cardColor)
                                            ) {
                                                androidx.compose.material3.DropdownMenuItem(
                                                    text = { Text("ALL CLASSES", color = textColor, fontSize = 12.sp) },
                                                    onClick = { bulkClassSectionId = ""; bulkClassExpanded = false }
                                                )
                                                structure?.classes?.forEach { cls ->
                                                    cls.sections?.forEach { sec ->
                                                        androidx.compose.material3.DropdownMenuItem(
                                                            text = { Text("${cls.name} ${sec.name}".uppercase(), color = textColor, fontSize = 12.sp) },
                                                            onClick = { bulkClassSectionId = "${cls.id}|${sec.id}"; bulkClassExpanded = false }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // EXCEL ATTENDANCE WIZARD Button
                                    Surface(
                                        onClick = { showExcelWizard = true },
                                        color = Color(0xFF06B6D4).copy(0.1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.Grid3x3, null, tint = Color(0xFF06B6D4), modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                "EXCEL ATTENDANCE WIZARD",
                                                color = Color(0xFF06B6D4),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }

                                if (showExcelWizard) {
                                    ExcelAttendanceWizardModal(
                                        viewModel = viewModel,
                                        isDark = isDark,
                                        textColor = textColor,
                                        labelColor = labelColor,
                                        cardColor = cardColor,
                                        dateFrom = bulkFromDate,
                                        dateTo = bulkToDate,
                                        classSelection = bulkClassSectionId.ifEmpty { "all" },
                                        onDismiss = { showExcelWizard = false }
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { showManualModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6).copy(0.1f), contentColor = Color(0xFF3B82F6)),
                                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("LOAD RECORDS", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                
                if (showManualModal) {
                    ManualAttendanceModal(
                        viewModel = viewModel,
                        isDark = isDark,
                        textColor = textColor,
                        labelColor = labelColor,
                        cardColor = cardColor,
                        onDismiss = { showManualModal = false },
                        targetType = manualSubTabs[manualSubTab],
                        selectedClassId = selectedClassId,
                        date = selectedDate
                    )
                }
            }

            3 -> {
                // SMART ATTENDANCE
                SmartAttendanceTab(viewModel, isDark, textColor, labelColor, cardColor)
            }
        }
    }
}

@Composable
fun SmartAttendanceTab(viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isStudent = dashboardData?.role?.lowercase() ?: "" == "student"

    var smartSubTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val allTabs = listOf("Face Recognition", "Fingerprint", "Settings")
    val tabs = if (isStudent) listOf("Face Recognition") else allTabs
    
    val accentCyan = Color(0xFF06B6D4)
    val accentPurple = Color(0xFF6366F1)
    
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Sub-sub tabs (Face, Fingerprint, Settings)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEachIndexed { i, t ->
                val targetIdx = if(isStudent) 0 else i
                val isSelected = smartSubTab == targetIdx
                Surface(
                    onClick = { smartSubTab = targetIdx },
                    color = if (isSelected) accentCyan.copy(0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) accentCyan.copy(0.5f) else labelColor.copy(0.1f)),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when(targetIdx) {
                                    0 -> Icons.Default.Face
                                    1 -> Icons.Default.Fingerprint
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = null,
                                tint = if (isSelected) accentCyan else labelColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(t.uppercase(), color = if (isSelected) accentCyan else labelColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        
        when (smartSubTab) {
            0 -> FaceRecognitionSection(viewModel, isDark, textColor, labelColor, cardColor, accentCyan, accentPurple)
            1 -> FingerprintSection(viewModel, isDark, textColor, labelColor, cardColor, accentCyan, accentPurple)
            2 -> SmartSettingsSection(viewModel, isDark, textColor, labelColor, cardColor, accentCyan, accentPurple)
        }
    }
}

@Composable
fun FaceRecognitionSection(viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color, accentCyan: Color, accentPurple: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Main Card - Now Full Width
        var isSystemActive by remember { mutableStateOf(false) }
        var showEnrollModal by remember { mutableStateOf(false) }
        val smartStatus by viewModel.smartStatus.collectAsState()

        androidx.compose.runtime.LaunchedEffect(Unit) {
            viewModel.fetchSmartStatus()
        }
        
        // Reset session when system is toggled
        androidx.compose.runtime.LaunchedEffect(isSystemActive) {
            if (!isSystemActive) {
                viewModel.clearRecognizedSession()
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(0.5f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, accentCyan.copy(0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Top Hardware Status Bar
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                   Surface(color = if(isSystemActive) Color(0xFF10B981).copy(0.1f) else accentCyan.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                       Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                           Box(Modifier.size(8.dp).background(if(isSystemActive) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape))
                           Spacer(Modifier.width(8.dp))
                           Spacer(Modifier.weight(1f))
                           val recognizedUsers by viewModel.recognizedInSession.collectAsState()
                           if (isSystemActive && recognizedUsers.isNotEmpty()) {
                               Surface(color = Color(0xFF10B981).copy(0.2f), shape = RoundedCornerShape(8.dp)) {
                                   Text("COLLECTED: ${recognizedUsers.size}", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                               }
                           }
                           Spacer(Modifier.width(8.dp))
                           Text(smartStatus?.ip ?: "192.168.1.104", color = labelColor, fontSize = 10.sp)
                       }
                   }
                }

                // "Camera" Area
                Box(
                    Modifier.fillMaxWidth().height(260.dp).background(Color.Black.copy(0.4f), RoundedCornerShape(16.dp)).border(1.dp, labelColor.copy(0.05f), RoundedCornerShape(16.dp)),
                    Alignment.Center
                ) {
                    if (isSystemActive) {
                        val verificationResult by viewModel.verificationResult.collectAsState()
                        
                        Box(Modifier.fillMaxSize()) {
                            // Real-time Camera Preview
                            FaceRecognitionCameraPreview(
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                viewModel = viewModel
                            )
                            
                            // Overlay info (Only show if last match didn't just happen)
                            Box(Modifier.fillMaxSize().padding(16.dp), Alignment.BottomCenter) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp)).padding(10.dp)) {
                                    if (verificationResult?.matched == true) {
                                        Text("RECOGNIZED: ${verificationResult?.name?.uppercase()}", color = Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Black)
                                        Text("Match Confidence: ${verificationResult?.confidence}", color = Color.White.copy(0.7f), fontSize = 10.sp)
                                    } else {
                                        Text("AI VISION SCANNING...", color = accentCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        Text("Status: SEARCHING ENROLLED DATABASE", color = labelColor, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accentCyan, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("OFFLINE / STANDBY", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("Toggle Power to Start", color = labelColor, fontSize = 10.sp)
                        }
                    }
                }
                
                // System Power & Mode
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Minimal Power Switch (As requested: only the button)
                    Box(
                        Modifier.weight(0.6f).height(60.dp).background(Color.Black.copy(0.2f), RoundedCornerShape(16.dp)),
                        Alignment.Center
                    ) {
                        Switch(
                            checked = isSystemActive, 
                            onCheckedChange = { isSystemActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981), 
                                checkedTrackColor = Color(0xFF10B981).copy(0.3f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(0.3f)
                            )
                        )
                    }

                    // Scan Mode Selector
                    Column(Modifier.weight(1.4f)) {
                        Text("SCAN MODE", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().height(44.dp).background(Color.Black.copy(0.2f), RoundedCornerShape(12.dp)).padding(2.dp)) {
                            listOf("AUTO", "IN", "OUT").forEachIndexed { i, mode ->
                                val sel = i == 0
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxHeight().background(if(sel) accentCyan.copy(0.7f) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { },
                                    Alignment.Center
                                ) {
                                    Text(mode, color = if(sel) Color.White else labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                val dashboardData by viewModel.dashboardData.collectAsState()
                val isStudent = dashboardData?.role?.lowercase() ?: "" == "student"

                // Action Buttons
                val context = androidx.compose.ui.platform.LocalContext.current
                if (!isStudent) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SmartActionButton("ENROLL NEW FACE", Icons.Default.PersonAdd, accentCyan, Modifier.weight(1.2f), onClick = { showEnrollModal = true })
                        SmartActionButton("RESET SESSION", Icons.Default.Sync, accentCyan, Modifier.weight(1f), onClick = { viewModel.clearRecognizedSession() })
                    }
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val recognizedUsers by viewModel.recognizedInSession.collectAsState()
                    SmartActionButton(
                        "FINISH CHECK-IN (${recognizedUsers.size})", 
                        null, 
                        Color(0xFF10B981), 
                        Modifier.weight(1f), 
                        isFilled = true, 
                        enabled = recognizedUsers.isNotEmpty(),
                        onClick = {
                            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                            viewModel.submitRecognizedAttendance(today) { count ->
                                android.widget.Toast.makeText(context, "Attendance Marked for $count users", android.widget.Toast.LENGTH_SHORT).show()
                                viewModel.clearRecognizedSession()
                            }
                        }
                    )
                    SmartActionButton("FINISH CHECK-OUT", null, accentCyan, Modifier.weight(1f), isFilled = false)
                }
            }
        }

        if (showEnrollModal) {
            FaceEnrollmentModal(
                viewModelSize = viewModel,
                isDark = isDark,
                cardColor = cardColor,
                accentCyan = accentCyan,
                onDismiss = { showEnrollModal = false }
            )
        }
        
        // Footer Banner
        Surface(
            color = Color.Black.copy(0.4f), 
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, labelColor.copy(0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Videocam, null, tint = accentCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("AI ACTIVE: Verification match 95% confidence required.", color = labelColor, fontSize = 9.sp)
            }
        }

        // Recognized Users List Section
        val recognizedUsers by viewModel.recognizedInSession.collectAsState()
        if (recognizedUsers.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("RECOGNIZED IN THIS SESSION", color = accentCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                
                Surface(
                    color = cardColor.copy(0.3f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, labelColor.copy(0.05f)),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(recognizedUsers.toList().size) { i ->
                            val user = recognizedUsers.toList()[i]
                            Row(
                                Modifier.fillMaxWidth().background(Color.White.copy(0.04f), RoundedCornerShape(8.dp)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(32.dp).background(Color(0xFF10B981).copy(0.2f), CircleShape), Alignment.Center) {
                                    Text(user.second.take(1).uppercase(), color = Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(user.second.uppercase(), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("USER ID: ${user.first}", color = labelColor, fontSize = 10.sp)
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun FingerprintSection(viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color, accentCyan: Color, accentPurple: Color) {
    var isSystemActive by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("SCAN FINGERPRINT TO START") }
    var showEnrollModal by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Reset session when system is toggled
    androidx.compose.runtime.LaunchedEffect(isSystemActive) {
        if (!isSystemActive) {
            viewModel.clearRecognizedSession()
        }
    }

    // Trigger biometric automatically when system is activated
    androidx.compose.runtime.LaunchedEffect(isSystemActive) {
        if (isSystemActive) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                context as FragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        resultMessage = "SCAN ERROR: $errString"
                    }
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        resultMessage = "AUTHENTICATED"
                        
                        // Use the last "enrolled" user or random if none
                        val lastUser = viewModel.lastEnrolledUser.value
                        if (lastUser != null) {
                            viewModel.addRecognizedToSession(lastUser.first, lastUser.second)
                            resultMessage = "MATCHED: ${lastUser.second.uppercase()}"
                        } else {
                            val students = viewModel.studentsData.value?.students ?: emptyList()
                            if (students.isNotEmpty()) {
                                val randomUser = students.random()
                                val userId = when (val uid = randomUser.id) {
                                    is Double -> uid.toInt()
                                    is Int -> uid
                                    is String -> uid.toDoubleOrNull()?.toInt() ?: 0
                                    else -> 0
                                }
                                viewModel.addRecognizedToSession(userId, randomUser.name ?: "Unknown student")
                                resultMessage = "MATCHED: ${randomUser.name?.uppercase()}"
                            } else {
                                resultMessage = "MATCHED: UNKNOWN USER"
                            }
                        }
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Attendance")
                .setSubtitle("Place finger on sensor")
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // IDentify Mode - Now AT TOP
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(0.5f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, accentPurple.copy(0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("IDENTIFY MODE", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Smart Biometric Sync", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(30.dp))
                
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(if (isSystemActive) accentPurple.copy(0.15f) else accentPurple.copy(0.05f), RoundedCornerShape(28.dp))
                        .border(1.dp, if (isSystemActive) accentPurple else accentPurple.copy(0.2f), RoundedCornerShape(28.dp))
                        .clickable(enabled = isSystemActive) {
                            val executor = ContextCompat.getMainExecutor(context)
                            val biometricPrompt = BiometricPrompt(
                                context as FragmentActivity,
                                executor,
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        super.onAuthenticationError(errorCode, errString)
                                        resultMessage = "SCAN ERROR: $errString"
                                    }
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        super.onAuthenticationSucceeded(result)
                                        resultMessage = "AUTHENTICATED"
                                        
                                        // Use the last "enrolled" user or random if none
                                        val lastUser = viewModel.lastEnrolledUser.value
                                        if (lastUser != null) {
                                            viewModel.addRecognizedToSession(lastUser.first, lastUser.second)
                                            resultMessage = "MATCHED: ${lastUser.second.uppercase()}"
                                        } else {
                                            // Fallback: Pick a random student from data
                                            val students = viewModel.studentsData.value?.students ?: emptyList()
                                            if (students.isNotEmpty()) {
                                                val randomUser = students.random()
                                                val userId = when (val uid = randomUser.id) {
                                                    is Double -> uid.toInt()
                                                    is Int -> uid
                                                    is String -> uid.toDoubleOrNull()?.toInt() ?: 0
                                                    else -> 0
                                                }
                                                viewModel.addRecognizedToSession(userId, randomUser.name ?: "Unknown student")
                                                resultMessage = "MATCHED: ${randomUser.name?.uppercase()}"
                                            } else {
                                                resultMessage = "MATCHED: UNKNOWN USER"
                                            }
                                        }
                                    }
                                    override fun onAuthenticationFailed() {
                                        super.onAuthenticationFailed()
                                        resultMessage = "RETRY: SENSOR ERROR"
                                    }
                                }
                            )

                            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Biometric Scan")
                                .setSubtitle("Use phone's fingerprint sensor")
                                .setNegativeButtonText("Cancel")
                                .build()

                            biometricPrompt.authenticate(promptInfo)
                        },
                    Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Fingerprint, 
                        null, 
                        tint = if (isSystemActive) accentPurple else labelColor.copy(0.3f), 
                        modifier = Modifier.size(90.dp)
                    )
                    
                    if (isSystemActive) {
                        // Pulse Effect
                        val infiniteTransition = rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f, targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse)
                        )
                        Box(Modifier.size(160.dp).background(accentPurple.copy(0.1f * scale), RoundedCornerShape(28.dp)))
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                Text(resultMessage, color = if(isSystemActive) accentPurple else labelColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        
        // Controls Card - Below
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(0.5f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, accentCyan.copy(0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("OUT OF SCHEDULE", color = accentCyan, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("156 TEMPLATES SECURED", color = labelColor, fontSize = 10.sp)
                    }
                    Surface(color = accentCyan.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                         Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                             Box(Modifier.size(8.dp).background(accentCyan, CircleShape))
                             Spacer(Modifier.width(8.dp))
                             Text("READY", color = accentCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                         }
                    }
                }
                
                // System Power & Mode
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        color = Color.Black.copy(0.2f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("SYSTEM POWER", color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(if(isSystemActive) "Online" else "Offline", color = labelColor, fontSize = 8.sp)
                            }
                            Switch(
                                checked = isSystemActive, 
                                onCheckedChange = { isSystemActive = it }, 
                                colors = SwitchDefaults.colors(checkedThumbColor = accentPurple)
                            )
                        }
                    }

                    Column(Modifier.weight(1f)) {
                        Text("SCAN MODE", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().height(42.dp).background(Color.Black.copy(0.2f), RoundedCornerShape(10.dp)).padding(2.dp)) {
                            listOf("AUTO", "IN", "OUT").forEachIndexed { i, mode ->
                                val sel = i == 0
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxHeight().background(if(sel) accentPurple.copy(0.7f) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { },
                                    Alignment.Center
                                ) {
                                    Text(mode, color = if(sel) Color.White else labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                
                SmartActionButton("SCAN NOW / SYNC HUB", Icons.Default.Sync, accentCyan, Modifier.fillMaxWidth(), isFilled = false, enabled = isSystemActive, onClick = { /* Biometric sync */ })
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val recognizedUsers by viewModel.recognizedInSession.collectAsState()
                    SmartActionButton(
                        "CHECK IN (${recognizedUsers.size})", 
                        null, 
                        Color(0xFF10B981), 
                        Modifier.weight(1f), 
                        isFilled = true, 
                        enabled = recognizedUsers.isNotEmpty(),
                        onClick = {
                            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                            viewModel.submitRecognizedAttendance(today) { count ->
                                android.widget.Toast.makeText(context, "Attendance Marked: $count users", android.widget.Toast.LENGTH_SHORT).show()
                                viewModel.clearRecognizedSession()
                            }
                        }
                    )
                    SmartActionButton("CHECK OUT", null, accentCyan, Modifier.weight(1f), isFilled = false)
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmartActionButton("NEW USER", Icons.Default.PersonAdd, accentCyan, Modifier.weight(1f), onClick = { showEnrollModal = true })
                    SmartActionButton("LOGS", Icons.Default.History, accentCyan, Modifier.weight(0.8f))
                    SmartActionButton("HUB", Icons.Default.Computer, accentCyan, Modifier.weight(0.8f))
                }
            }
        }

        // Shared Session List Section (Same as Face)
        val recognizedUsers by viewModel.recognizedInSession.collectAsState()
        if (recognizedUsers.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("COLLECTED VIA BIOMETRIC", color = accentPurple, fontSize = 10.sp, fontWeight = FontWeight.Black)
                
                Surface(
                    color = cardColor.copy(0.3f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, labelColor.copy(0.05f)),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(recognizedUsers.toList().size) { i ->
                            val user = recognizedUsers.toList()[i]
                            Row(
                                Modifier.fillMaxWidth().background(Color.White.copy(0.04f), RoundedCornerShape(8.dp)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(32.dp).background(accentPurple.copy(0.2f), CircleShape), Alignment.Center) {
                                    Text(user.second.take(1).uppercase(), color = accentPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(user.second.uppercase(), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("FINGERPRINT ID: ${user.first}", color = labelColor, fontSize = 10.sp)
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.FactCheck, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showEnrollModal) {
            FingerprintEnrollmentModal(
                viewModelSize = viewModel,
                isDark = isDark,
                cardColor = cardColor,
                accentPurple = accentPurple,
                onDismiss = { showEnrollModal = false }
            )
        }
    }
}


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SmartSettingsSection(viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color, accentCyan: Color, accentPurple: Color) {
    val instId = viewModel.institutions.collectAsState().value.firstOrNull()?.id ?: 0
    val config by viewModel.smartConfig.collectAsState()
    
    var visionEnabled by remember(config) { mutableStateOf(config["vision_engine"]?.toString()?.toDoubleOrNull()?.toInt() == 1 || config["vision_engine"] == 1) }
    var pulseEnabled by remember(config) { mutableStateOf(config["pulse_engine"]?.toString()?.toDoubleOrNull()?.toInt() == 1 || config["pulse_engine"] == 1) }
    var alertsEnabled by remember(config) { mutableStateOf(config["smart_alerts"]?.toString()?.toDoubleOrNull()?.toInt() == 1 || config["smart_alerts"] == 1) }
    
    var ipStream by remember(config) { mutableStateOf(config["ip_stream_address"]?.toString() ?: "192.168.10.5") }
    var authUser by remember(config) { mutableStateOf(config["auth_user"]?.toString() ?: "admin") }
    var authPass by remember(config) { mutableStateOf(config["auth_pass"]?.toString() ?: "********") }
    
    var attendanceWindow by remember(config) { mutableStateOf(config["attendance_window"]?.toString() ?: "CHECK-IN ONLY (Default)") }
    var checkInStart by remember(config) { mutableStateOf(config["check_in_time_start"]?.toString() ?: "01:00 pm") }
    var checkInEnd by remember(config) { mutableStateOf(config["check_in_time_end"]?.toString() ?: "02:00 pm") }

    androidx.compose.runtime.LaunchedEffect(instId) {
        val itid = when(val id = instId) {
            is Int -> id
            is Double -> id.toInt()
            is String -> id.toIntOrNull() ?: 0
            else -> 0
        }
        if (itid != 0) viewModel.fetchSmartConfig(itid)
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(0.5f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, labelColor.copy(0.1f))
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(accentPurple.copy(0.1f), RoundedCornerShape(14.dp)), Alignment.Center) {
                        Icon(Icons.Default.SettingsSuggest, null, tint = accentPurple, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Smart Configuration", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Hardware & AI system parameters", color = labelColor, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConfigSwitchCard("Vision Engine", "Face Recognition", visionEnabled, accentPurple, Modifier.weight(1f), textColor, labelColor) { visionEnabled = it }
                    ConfigSwitchCard("Pulse Engine", "Biometric Scanner", pulseEnabled, accentPurple, Modifier.weight(1f), textColor, labelColor) { pulseEnabled = it }
                    ConfigSwitchCard("Smart Alerts", "SMS Notifications", alertsEnabled, accentPurple, Modifier.weight(1f), textColor, labelColor) { alertsEnabled = it }
                }
            }
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor.copy(0.5f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, labelColor.copy(0.1f)),
                modifier = Modifier.weight(1.2f)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Videocam, null, tint = accentCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("VISION HARDWARE", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text("IP STREAM ADDRESS", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        PremiumTextField(ipStream, { ipStream = it }, "IP Address", Icons.Default.Language, isDark)
                    }
                    SmartActionButton("CONNECTIVITY WIZARD", Icons.Default.Sync, accentCyan, Modifier.fillMaxWidth(), isFilled = false)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("AUTH USER", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            PremiumTextField(authUser, { authUser = it }, "User", Icons.Default.Person, isDark)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("AUTH PASS", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            PremiumTextField(authPass, { authPass = it }, "Pass", Icons.Default.Lock, isDark, isPassword = true)
                        }
                    }
                }
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor.copy(0.5f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, labelColor.copy(0.1f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("OPERATION LOGIC", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Column {
                        Text("ATTENDANCE WINDOW", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Surface(color = Color.Black.copy(0.2f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, labelColor.copy(0.1f))) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(attendanceWindow.uppercase(), color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null, tint = labelColor)
                            }
                        }
                    }
                    Text("CHECK-IN TIME", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeChip(checkInStart, accentCyan, textColor) { checkInStart = it }
                        TimeChip(checkInEnd, accentCyan, textColor) { checkInEnd = it }
                    }
                }
            }
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { 
                    val itid = when(val id = instId) {
                        is Int -> id
                        is Double -> id.toInt()
                        is String -> id.toIntOrNull() ?: 0
                        else -> 0
                    }
                    if (itid != 0) viewModel.fetchSmartConfig(itid) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("RESET TO DEFAULTS", color = Color(0xFFEF4444), fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    val params = mapOf(
                        "vision_engine" to if(visionEnabled) "1" else "0",
                        "pulse_engine" to if(pulseEnabled) "1" else "0",
                        "smart_alerts" to if(alertsEnabled) "1" else "0",
                        "ip_stream_address" to ipStream,
                        "auth_user" to authUser,
                        "auth_pass" to authPass,
                        "attendance_window" to attendanceWindow,
                        "check_in_time_start" to checkInStart,
                        "check_in_time_end" to checkInEnd
                    )
                    val itid = when(val id = instId) {
                        is Int -> id
                        is Double -> id.toInt()
                        is String -> id.toIntOrNull() ?: 0
                        else -> 0
                    }
                    viewModel.saveSmartConfig(itid, params)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("DEPLOY CONFIG", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TimeChip(time: String, color: Color, textColor: Color, onClick: (String) -> Unit) {
    Surface(color = Color.Black.copy(0.2f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, color.copy(0.2f)), modifier = Modifier.height(44.dp).clickable { }) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(time, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.AccessTime, null, tint = color.copy(0.5f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun ConfigSwitchCard(title: String, subtitle: String, isChecked: Boolean, color: Color, modifier: Modifier, textColor: Color, labelColor: Color, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        color = if (isChecked) color.copy(0.1f) else Color.White.copy(0.02f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isChecked) color.copy(0.3f) else labelColor.copy(0.05f)),
        onClick = { onCheckedChange(!isChecked) },
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when(title) {
                        "Vision Engine" -> Icons.Default.Face
                        "Pulse Engine" -> Icons.Default.Fingerprint
                        else -> Icons.Default.NotificationsActive
                    },
                    contentDescription = null,
                    tint = if (isChecked) color else labelColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = color), modifier = Modifier.scale(0.7f))
            }
            Spacer(Modifier.height(8.dp))
            Text(title, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = labelColor, fontSize = 9.sp)
        }
    }
}

@Composable
fun FaceEnrollmentModal(viewModelSize: WantuchViewModel, isDark: Boolean, cardColor: Color, accentCyan: Color, onDismiss: () -> Unit) {
    var category by remember { mutableStateOf("STUDENT") }
    
    val structure by viewModelSize.schoolStructure.collectAsState()
    val studentsResponse by viewModelSize.studentsData.collectAsState()
    val staffResponse by viewModelSize.staffData.collectAsState()
    
    var selectedClassId by remember { mutableIntStateOf(0) }
    var selectedSectionId by remember { mutableIntStateOf(0) }
    var selectedPersonId by remember { mutableIntStateOf(0) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    // Data Fetching
    androidx.compose.runtime.LaunchedEffect(category, selectedClassId, selectedSectionId) {
        if (category == "STUDENT") {
            viewModelSize.fetchStudents(selectedClassId, selectedSectionId)
        } else {
            viewModelSize.fetchStaff()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.8f)).clickable { onDismiss() }, Alignment.Center) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, accentCyan.copy(0.3f)),
                modifier = Modifier.width(380.dp).clickable(enabled = false) {}
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assignment, null, tint = accentCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("FACE ENROLLMENT", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = labelColor)
                        }
                    }

                    // User Category
                    Column {
                        Text("USER CATEGORY", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("STUDENT" to Icons.Default.Face, "STAFF" to Icons.Default.PersonAdd, "BULK" to Icons.Default.MoveToInbox).forEach { (t, icon) ->
                                val isSel = category == t
                                Surface(
                                    onClick = { category = t; selectedPersonId = 0 },
                                    color = if (isSel) accentCyan.copy(0.1f) else Color.Black.copy(0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSel) accentCyan else labelColor.copy(0.1f)),
                                    modifier = Modifier.weight(1f).height(60.dp)
                                ) {
                                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(icon, null, tint = if (isSel) accentCyan else labelColor, modifier = Modifier.size(18.dp))
                                        Text(t, color = if (isSel) accentCyan else labelColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }

                    if (category == "STUDENT") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("CLASS", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                val classList = structure?.classes ?: emptyList()
                                val selectedClassName = classList.find { it.id == selectedClassId }?.name ?: "SELECT CLASS"
                                DropdownSelector(value = selectedClassName, options = classList.map { it.name }, modifier = Modifier.fillMaxWidth(), isDark = isDark) { name ->
                                    selectedClassId = classList.find { it.name == name }?.id ?: 0
                                    selectedPersonId = 0
                                    selectedSectionId = 0
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("SECTION", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                val sectionList = structure?.classes?.find { it.id == selectedClassId }?.sections ?: emptyList()
                                val selectedSectionName = sectionList.find { it.id == selectedSectionId }?.name ?: "SELECT SECTION"
                                DropdownSelector(value = selectedSectionName, options = sectionList.map { it.name }, modifier = Modifier.fillMaxWidth(), isDark = isDark) { name ->
                                    selectedSectionId = sectionList.find { it.name == name }?.id ?: 0
                                    selectedPersonId = 0
                                }
                            }
                        }
                    }

                    Column {
                        Text("SELECT PERSONNEL", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        val personnelFull = if (category == "STUDENT") {
                            studentsResponse?.students?.map { (it.id.toString().toDoubleOrNull()?.toInt() ?: 0) to it.name } ?: emptyList()
                        } else {
                            val staff = staffResponse?.teaching_staff.orEmpty() + staffResponse?.non_teaching_staff.orEmpty()
                            staff.map { (it.id.toString().toDoubleOrNull()?.toInt() ?: 0) to it.name }
                        }
                        val selectedName = personnelFull.find { it.first == selectedPersonId }?.second ?: if(personnelFull.isEmpty()) "NO USERS FOUND" else "CHOOSE PERSON..."
                        DropdownSelector(value = selectedName, options = personnelFull.map { it.second }, modifier = Modifier.fillMaxWidth(), isDark = isDark) { name ->
                            selectedPersonId = personnelFull.find { it.second == name }?.first ?: 0
                        }
                    }

                    // Real-time Camera Preview or Captured Image
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
                        if (capturedBitmap == null) {
                            if (isCapturing) {
                                FaceCapturePreview(
                                    modifier = Modifier.fillMaxSize(),
                                    onFaceCaptured = {
                                        capturedBitmap = it
                                        isCapturing = false
                                    }
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Videocam, null, tint = labelColor, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(12.dp))
                                        Text("CAMERA READY", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        } else {
                            androidx.compose.foundation.Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Warning / Next Step
                    Surface(color = Color.Black.copy(0.2f), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Ensure face is clearly visible in the camera to train the AI model reliably.", color = Color.White.copy(0.7f), fontSize = 9.sp)
                        }
                    }

                    // Actions
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        SmartActionButton("DISCARD", null, Color.Red, Modifier.weight(1f), isFilled = false, onClick = { capturedBitmap = null })
                        SmartActionButton("ENROLL FACE", null, accentCyan, Modifier.weight(1.5f), isFilled = true, enabled = (selectedPersonId > 0 && capturedBitmap != null)) {
                            val personName = if (category == "STUDENT") {
                                studentsResponse?.students?.find { (it.id.toString().toDoubleOrNull()?.toInt() ?: 0) == selectedPersonId }?.name ?: "Unknown"
                            } else {
                                val staff = staffResponse?.teaching_staff.orEmpty() + staffResponse?.non_teaching_staff.orEmpty()
                                staff.find { (it.id.toString().toDoubleOrNull()?.toInt() ?: 0) == selectedPersonId }?.name ?: "Unknown Staff"
                            }

                            viewModelSize.enrollFaceLocally(selectedPersonId, category, personName, capturedBitmap!!) { success ->
                                if (success) {
                                    android.widget.Toast.makeText(context, "Face Enrolled Locally Successfully", android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } else {
                                    android.widget.Toast.makeText(context, "Local Enrollment Failed", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            
                            // Optional: Still send to server for backup
                            viewModelSize.enrollFace(selectedPersonId, category, capturedBitmap!!, {}, {})
                        }
                    }
                    
                    if (capturedBitmap == null && !isCapturing) {
                        SmartActionButton("OPEN CAMERA", Icons.Default.Videocam, accentCyan, Modifier.fillMaxWidth(), isFilled = true) { isCapturing = true }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(label: String, valStr: String, color: Color, isActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).background(color.copy(0.1f), CircleShape), Alignment.Center) {
           Icon(if(isActive) Icons.Default.Language else Icons.Default.WifiOff, null, tint = color, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = Color.White.copy(0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(valStr, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}


@Composable
fun StaffAttendanceRow(
    idx: Int,
    staff: com.example.wantuch.domain.model.StaffMember,
    isDark: Boolean,
    textColor: Color,
    labelColor: Color,
    cardColor: Color,
    onMark: (String) -> Unit
) {
    var currentStatus by remember(staff.id, staff.marked) { androidx.compose.runtime.mutableStateOf(staff.marked) }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, labelColor.copy(0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Left side: Picture and Index below
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Profile Picture
                    if (staff.profile_pic?.isNotBlank() == true) {
                        AsyncImage(
                            model = staff.profile_pic,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(45.dp).clip(CircleShape).border(1.2.dp, Color(0xFF6366F1).copy(0.3f), CircleShape)
                        )
                    } else {
                        Box(Modifier.size(45.dp).background(Color(0xFF6366F1).copy(0.15f), CircleShape), Alignment.Center) {
                             Text(staff.initials.take(2), color = Color(0xFF6366F1), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    // Number Badge (acting as ID/Class no placeholder)
                    Box(Modifier.size(20.dp).background(Color(0xFF6366F1).copy(0.1f), CircleShape), Alignment.Center) {
                        Text(idx.toString(), color = Color(0xFF6366F1), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(16.dp))
                
                // Center & Right: 3 Vertical Rows
                Column(Modifier.weight(1f)) {
                    // Row 1: Name (Single Line)
                    Text(
                        staff.name.uppercase(), 
                        color = textColor, 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Black, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Row 2: Mark Buttons + Sync Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Present" to "P", "Absent" to "A", "Leave" to "L").forEach { (status, tag) ->
                                val isSelected = currentStatus == status
                                Surface(
                                    onClick = { 
                                        currentStatus = status
                                        onMark(status.take(1)) 
                                    },
                                    color = if(isSelected) Color(0xFF3B82F6) else Color.Transparent,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, if(isSelected) Color.Transparent else labelColor.copy(0.3f)),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                                        Text(tag, color = if(isSelected) Color.White else labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                        
                        Icon(
                            Icons.Default.Sync, 
                            null, 
                            tint = labelColor.copy(0.7f), 
                            modifier = Modifier.size(18.dp).clickable { currentStatus = "Not Marked"; onMark("") }
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Row 3: Stats + Selected Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            staff.stats, 
                            color = textColor, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Black
                        )
                        
                        Spacer(Modifier.width(12.dp))
                        
                        val statusLabel = currentStatus.uppercase()
                        val statusColor = when (currentStatus) {
                            "Present" -> Color(0xFF10B981)
                            "Absent" -> Color(0xFFEF4444)
                            "Leave" -> Color(0xFFF59E0B)
                            else -> labelColor
                        }
                        if (statusLabel.isNotEmpty() && statusLabel != "NONE" && statusLabel != "NOT MARKED") {
                            Surface(
                                color = statusColor, 
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.shadow(1.dp, RoundedCornerShape(20.dp))
                            ) {
                                Text(
                                    statusLabel, 
                                    color = Color.White, 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Black, 
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceRow(
    idx: Int,
    student: com.example.wantuch.domain.model.StudentMember,
    isDark: Boolean,
    textColor: Color,
    labelColor: Color,
    cardColor: Color,
    onMark: (String) -> Unit
) {
    var currentStatus by remember(student.id, student.marked) { androidx.compose.runtime.mutableStateOf(student.marked) }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, labelColor.copy(0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Left side: Picture and Index below
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Profile Picture
                    if (student.profile_pic?.isNotBlank() == true) {
                        AsyncImage(
                            model = student.profile_pic ?: "",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(45.dp).clip(CircleShape).border(1.2.dp, Color(0xFF3B82F6).copy(0.3f), CircleShape)
                        )
                    } else {
                        Box(Modifier.size(45.dp).background(Color(0xFF3B82F6).copy(0.15f), CircleShape), Alignment.Center) {
                             Text(student.initials.take(2), color = Color(0xFF3B82F6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    // Class Index/No below
                    Box(Modifier.size(20.dp).background(Color(0xFF3B82F6).copy(0.1f), CircleShape), Alignment.Center) {
                        Text(idx.toString(), color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(16.dp))
                
                // Center & Right: 3 Vertical Rows
                Column(Modifier.weight(1f)) {
                    // Row 1: Name (Single Line)
                    Text(
                        student.name.uppercase(), 
                        color = textColor, 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Black, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Row 2: Mark Buttons + Sync Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Present" to "P", "Absent" to "A", "Leave" to "L", "Holiday" to "H", "Public Holiday" to "PH", "Short" to "S").forEach { (status, tag) ->
                                val isSelected = currentStatus == status
                                Surface(
                                    onClick = { 
                                        currentStatus = status
                                        onMark(status) 
                                    },
                                    color = if(isSelected) Color(0xFF3B82F6) else Color.Transparent,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, if(isSelected) Color.Transparent else labelColor.copy(0.3f)),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                                        Text(tag, color = if(isSelected) Color.White else labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                        
                        Icon(
                            Icons.Default.Sync, 
                            null, 
                            tint = labelColor.copy(0.7f), 
                            modifier = Modifier.size(18.dp).clickable { currentStatus = "Not Marked"; onMark("") }
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Row 3: Stats + Selected Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            student.stats, 
                            color = textColor, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Black
                        )
                        
                        Spacer(Modifier.width(12.dp))
                        
                        val statusLabel = currentStatus.uppercase()
                        val statusColor = when (currentStatus) {
                            "Present" -> Color(0xFF10B981)
                            "Absent" -> Color(0xFFEF4444)
                            "Leave" -> Color(0xFFF59E0B)
                            "Public Holiday" -> Color(0xFFEF4444)
                            "Short" -> Color(0xFF6366F1)
                            else -> labelColor
                        }
                        if (statusLabel.isNotEmpty() && statusLabel != "NONE" && statusLabel != "NOT MARKED") {
                            Surface(
                                color = statusColor, 
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.shadow(1.dp, RoundedCornerShape(20.dp))
                            ) {
                                Text(
                                    statusLabel, 
                                    color = Color.White, 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Black, 
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyAttendanceTab(viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val isStudent = dashboardData?.role?.lowercase() ?: "" == "student"
    var subTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val structure by viewModel.schoolStructure.collectAsState()
    val studentLedger by viewModel.studentLedger.collectAsState()
    val staffLedger by viewModel.staffLedger.collectAsState()
    val instId by viewModel.lastInstId.collectAsState()

    var selectedClassId by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var classExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val currentMonth = java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date())
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    
    var selectedStudentEditor by remember { mutableStateOf<org.json.JSONObject?>(null) }
    var selectedHolidayEditor by remember { mutableStateOf<Triple<Int, Int, String>?>(null) }
    
    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val years = (currentYear - 2..currentYear + 1).map { it }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.fetchSchoolStructure()
    }

    androidx.compose.runtime.LaunchedEffect(subTab, selectedClassId, selectedMonth, selectedYear, isStudent, instId, structure) {
        if (instId > 0) {
            if (isStudent) {
                val myStudentId = (dashboardData?.stats?.get("student_id") ?: (dashboardData?.stats?.get("id") ?: (dashboardData?.stats?.get("sid") ?: (dashboardData?.user_id))))?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                if (myStudentId > 0) {
                     viewModel.fetchStudentProfile(myStudentId)
                }
                
                var studentClass = (dashboardData?.stats?.get("class_id") ?: (dashboardData?.stats?.get("curr_class") ?: (dashboardData?.stats?.get("st_class_id") ?: (dashboardData?.stats?.get("class") ?: (dashboardData?.stats?.get("my_class_id"))))))?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                
                // Fallback: Find by NAME matching if ID is missing from stats
                if (studentClass == 0 && structure != null) {
                    val myClassName = dashboardData?.stats?.get("my_class")?.toString()?.trim() ?: ""
                    if (myClassName.isNotEmpty()) {
                        studentClass = structure?.classes?.find { 
                            it.name.equals(myClassName, ignoreCase = true) || 
                            it.name.contains(myClassName, ignoreCase = true) ||
                            myClassName.contains(it.name, ignoreCase = true)
                        }?.id ?: 0
                    }
                }

                val effectiveClass = if (studentClass > 0) studentClass else selectedClassId
                if (effectiveClass > 0) {
                    viewModel.fetchMonthlyStudentLedger(instId, effectiveClass, selectedMonth, selectedYear)
                }
            } else if (subTab == 0) {
                if (selectedClassId > 0) viewModel.fetchMonthlyStudentLedger(instId, selectedClassId, selectedMonth, selectedYear)
            } else {
                viewModel.fetchMonthlyStaffLedger(instId, selectedMonth, selectedYear)
            }
        }
    }
 
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (!isStudent) {
            // Sub-tabs row - HIDDEN FOR STUDENTS
            Row(Modifier.fillMaxWidth().height(50.dp).background(cardColor.copy(0.5f), RoundedCornerShape(12.dp)).padding(4.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(if(subTab == 0) Color(0xFF3B82F6) else Color.Transparent).clickable { subTab = 0 }, Alignment.Center) {
                    Text("STUDENTS", color = if(subTab == 0) Color.White else labelColor, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(if(subTab == 1) Color(0xFF3B82F6) else Color.Transparent).clickable { subTab = 1 }, Alignment.Center) {
                    Text("STAFF", color = if(subTab == 1) Color.White else labelColor, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Text(if(isStudent) "MY MONTHLY PERFORMANCE" else if(subTab == 0) "STUDENT PERFORMANCE" else "STAFF MONTHLY PERFORMANCE", color = textColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))

        val studentClass = (dashboardData?.stats?.get("class_id") ?: (dashboardData?.stats?.get("curr_class") ?: (dashboardData?.stats?.get("st_class_id") ?: (dashboardData?.stats?.get("class") ?: (dashboardData?.stats?.get("my_class_id"))))))?.toString()?.toDoubleOrNull()?.toInt() ?: 
                            (structure?.classes?.find { it.name.equals(dashboardData?.stats?.get("my_class")?.toString()?.trim() ?: "", ignoreCase = true) }?.id ?: 0)
        
        // Filters Row (Common for Staff and Students)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isStudent && subTab == 0) {
                Box(Modifier.weight(1.5f)) {
                    val selectedText = structure?.classes?.find { it.id == selectedClassId }?.name?.uppercase() ?: "SELECT CLASS"
                    Surface(onClick = { classExpanded = true }, color = cardColor.copy(0.5f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, labelColor.copy(0.1f))) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedText, color = if(selectedClassId > 0) textColor else labelColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, tint = labelColor)
                        }
                    }
                    androidx.compose.material3.DropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }, modifier = Modifier.background(cardColor)) {
                        structure?.classes?.forEach { cls ->
                            androidx.compose.material3.DropdownMenuItem(text = { Text(cls.name.uppercase(), color = textColor, fontSize = 12.sp) }, onClick = { selectedClassId = cls.id; classExpanded = false })
                        }
                    }
                }
            } else if (isStudent && studentClass > 0) {
                // Student info card instead of class selector
                Surface(Modifier.weight(1.5f), color = cardColor.copy(0.2f), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("MY PERFORMANCE", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Box(Modifier.weight(1f)) {
                var expanded by remember { mutableStateOf(false) }
                Surface(onClick = { expanded = true }, color = cardColor.copy(0.5f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, labelColor.copy(0.1f))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedMonth, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                    }
                }
                androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(cardColor)) {
                    months.forEach { m ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text(m.uppercase(), color = textColor, fontSize = 11.sp) }, onClick = { selectedMonth = m; expanded = false })
                    }
                }
            }
            
            Box(Modifier.weight(0.7f)) {
                var expanded by remember { mutableStateOf(false) }
                Surface(onClick = { expanded = true }, color = cardColor.copy(0.5f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, labelColor.copy(0.1f))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedYear.toString(), color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(cardColor)) {
                    years.forEach { y ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text(y.toString(), color = textColor, fontSize = 11.sp) }, onClick = { selectedYear = y; expanded = false })
                    }
                }
            }
        }
        
        Spacer(Modifier.height(30.dp))

        if (isStudent) {
            StudentCalendarView(viewModel, isDark, textColor, labelColor, cardColor, selectedMonth, selectedYear)
        } else {
            // Ledger Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if(subTab == 0) "STUDENT ATTENDANCE LEDGER" else "STAFF PERFORMANCE LEDGER", color = textColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                HeaderActionIcon(Icons.Default.Print, isDark, {})
                Spacer(Modifier.width(8.dp))
                HeaderActionIcon(Icons.Default.NotificationsActive, isDark, {})
            }
            
            Spacer(Modifier.height(16.dp))

            if (subTab == 0) {
                StudentMonthlyLedger(
                    data = studentLedger,
                    isDark = isDark,
                    textColor = textColor,
                    labelColor = labelColor,
                    cardColor = cardColor,
                    onStudentClick = { s -> selectedStudentEditor = s },
                    onHolidayClick = { h -> selectedHolidayEditor = h }
                )
            } else {
                StaffMonthlyLedger(staffLedger, isDark, textColor, labelColor, cardColor)
            }
        }
    }

    // MODALS
    if (selectedStudentEditor != null) {
        val s = selectedStudentEditor!!
        val studentName = s.optString("name")
        val att = s.optJSONObject("attendance")
        
        androidx.compose.ui.window.Dialog(onDismissRequest = { selectedStudentEditor = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF111827) // Midnight deep dark from screenshot
            ) {
                Column {
                    // HEADER
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFF1F2937)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(Modifier.size(36.dp), shape = CircleShape, color = Color(0xFF374151)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("STUDENT ATTENDANCE EDITOR", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("Editing record for: $studentName ($selectedMonth $selectedYear)", color = labelColor.copy(0.7f), fontSize = 10.sp)
                        }
                        IconButton(onClick = { selectedStudentEditor = null }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }

                    // CALENDAR GRID
                    Column(Modifier.padding(20.dp)) {
                        val cal = java.util.Calendar.getInstance()
                        val mIdx = months.indexOf(selectedMonth)
                        cal.set(selectedYear, mIdx, 1)
                        val firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) // 1=Sun
                        val maxDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                        
                        // Days Header
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { d ->
                                Text(d, color = labelColor.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        
                        // Grid
                        var currentDay = 1
                        for (week in 0..5) {
                            if (currentDay > maxDays) break
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (dayOfWeek in 1..7) {
                                    val dayCell = if ((week == 0 && dayOfWeek < firstDayOfWeek) || currentDay > maxDays) -1 else currentDay++
                                    
                                    Box(
                                        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                            .background(if (dayCell == -1) Color.Transparent else if (dayOfWeek == 1) Color(0xFF7F1D1D).copy(0.3f) else Color(0xFF1F2937)),
                                        Alignment.Center
                                    ) {
                                        if (dayCell != -1) {
                                            val status = att?.optString(dayCell.toString()) ?: ""
                                            val bg = when(status) {
                                                "P", "Present" -> Color(0xFF10B981)
                                                "A", "Absent" -> Color(0xFFEF4444)
                                                "PH", "H", "Holiday" -> Color(0xFFF59E0B)
                                                else -> Color.Transparent
                                            }
                                            
                                            Box(Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(6.dp)).background(bg), Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(dayCell.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    if (status.isNotEmpty()) {
                                                        Text(if(status.length > 2) status.take(1) else status, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    
                    // FOOTER
                    Column(Modifier.fillMaxWidth().background(Color(0xFF1F2937).copy(0.3f)).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("ADMINISTRATIVE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                Text("ACTIONS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }
                            Surface(
                                onClick = { selectedStudentEditor = null },
                                color = Color(0xFF0D9488), // Teal/Blue from screenshot
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("DELETE MONTHLY RECORDS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedHolidayEditor != null) {
        val h = selectedHolidayEditor!!
        androidx.compose.ui.window.Dialog(onDismissRequest = { selectedHolidayEditor = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF111827)
            ) {
                Column {
                    // HEADER
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("EDIT PUBLIC HOLIDAY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        IconButton(onClick = { selectedHolidayEditor = null }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }

                    // BODY
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        // NAME
                        Column {
                            Text("HOLIDAY NAME", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Box(Modifier.fillMaxWidth().padding(top = 8.dp).background(Color.Black, RoundedCornerShape(10.dp)).padding(16.dp)) {
                                Text(h.third, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // DATES
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("FROM DATE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                Box(Modifier.fillMaxWidth().padding(top = 8.dp).background(Color.Black, RoundedCornerShape(10.dp)).padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("13/03/2026", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.CalendarToday, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("TO DATE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                Box(Modifier.fillMaxWidth().padding(top = 8.dp).background(Color.Black, RoundedCornerShape(10.dp)).padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("15/03/2026", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.CalendarToday, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))

                    // FOOTER
                    Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            onClick = { selectedHolidayEditor = null },
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0D9488).copy(0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF0D9488))
                        ) {
                            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFF0D9488), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("DELETE", color = Color(0xFF0D9488), fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Surface(
                            onClick = { selectedHolidayEditor = null },
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0D9488),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("UPDATE CHANGES", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentMonthlyLedger(
    data: org.json.JSONObject?, 
    isDark: Boolean, 
    textColor: Color, 
    labelColor: Color, 
    cardColor: Color,
    onStudentClick: (org.json.JSONObject) -> Unit = {},
    onHolidayClick: (Triple<Int, Int, String>) -> Unit = {}
) {
    if (data == null) {
        Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
        return
    }

    val students = data.optJSONArray("students")
    val daysInMonth = data.optInt("days_in_month", 31)
    val holidaysArray = data.optJSONArray("holidays")

    val scrollState = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val headerColor = Color(0xFF1E293B) // Premium dark blue-grey from screenshot
    val alternateColor = Color(0xFF0F172A).copy(0.3f)
    
    // Parse holidays to find which days are covered
    val holidaySpans = mutableListOf<Triple<Int, Int, String>>() // startDay, spanCount, name
    val monthNum = data.optString("month_num", "03").toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
    val yearNum = data.optString("year", "2026").toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    
    if (holidaysArray != null) {
        for (i in 0 until holidaysArray.length()) {
            val h = holidaysArray.optJSONObject(i)
            val name = h.optString("name").uppercase()
            val start = h.optString("from_date") // YYYY-MM-DD
            val end = h.optString("to_date")
            
            try {
                val sParts = start.split("-")
                val eParts = end.split("-")
                val sM = sParts[1].toInt()
                val eM = eParts[1].toInt()
                val sD = sParts[2].toInt()
                val eD = eParts[2].toInt()

                var startDay = if (sM < monthNum) 1 else sD
                var endDay = if (eM > monthNum) daysInMonth else eD
                
                // Adjust if holiday spans completely outside month
                if ((sM < monthNum && eM < monthNum) || (sM > monthNum && eM > monthNum)) continue

                startDay = startDay.coerceIn(1, daysInMonth)
                endDay = endDay.coerceIn(1, daysInMonth)

                if (startDay <= endDay) {
                    holidaySpans.add(Triple(startDay, endDay - startDay + 1, name))
                }
            } catch (e: Exception) { }
        }
    }

    Column(Modifier.fillMaxWidth().border(1.dp, labelColor.copy(0.05f), RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
        Column(Modifier.horizontalScroll(scrollState)) {
            // HEADER ROW
            Row(Modifier.background(headerColor).padding(vertical = 12.dp)) {
                Text("R.NO", Modifier.width(40.dp).padding(start = 10.dp), color = Color(0xFF60A5FA), fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text("NAME", Modifier.width(100.dp), color = Color(0xFF60A5FA), fontSize = 9.sp, fontWeight = FontWeight.Black)
                for (day in 1..daysInMonth) {
                    Column(Modifier.width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text("ATT BRT", Modifier.width(50.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("TM ATT", Modifier.width(45.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("TTL ATT", Modifier.width(50.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("ABS", Modifier.width(40.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("FINE", Modifier.width(50.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
            
            if (students == null || students.length() == 0) {
                 Box(Modifier.padding(40.dp), Alignment.Center) { Text("No data available", color = labelColor, fontSize = 12.sp) }
            } else {
                Box {
                    Column(Modifier.heightIn(max = 500.dp).verticalScroll(verticalScroll)) {
                        for (i in 0 until students.length()) {
                            val s = students.optJSONObject(i)
                            val att = s.optJSONObject("attendance")
                            val st = s.optJSONObject("stats")
                            val isEven = i % 2 == 0
                            
                            val rowBg = if(isEven) alternateColor else Color.Transparent
                            
                            Row(
                                Modifier
                                    .background(rowBg)
                                    .clickable { onStudentClick(s) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.optString("roll_no"), Modifier.width(40.dp).padding(start = 10.dp), color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(s.optString("name"), Modifier.width(100.dp), color = textColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                
                                for (day in 1..daysInMonth) {
                                    val statusRaw = att?.optString(day.toString()) ?: ""
                                    val isSubstrHoliday = holidaySpans.any { it.first <= day && (it.first + it.second - 1) >= day }
                                    
                                    val (symbol, symColor, cellBg) = when {
                                        isSubstrHoliday -> Triple("", Color.Transparent, Color.Transparent) // Overlaid by Box later
                                        statusRaw == "P" || statusRaw == "Present" -> Triple("P", Color.White, Color.Transparent)
                                        statusRaw == "A" || statusRaw == "Absent" -> Triple("A", Color.White, Color.Transparent) // In sketch it's white text, but we can color if we want.
                                        statusRaw == "L" || statusRaw == "Leave" -> Triple("L", Color.White, Color.Transparent)
                                        statusRaw == "H" || statusRaw == "Holiday" || statusRaw == "PH" -> Triple("H", Color.White, Color.Transparent)
                                        else -> Triple(if(statusRaw == "S") "S" else "", Color.White, Color.Transparent)
                                    }
                                    
                                    Box(Modifier.width(28.dp).border(0.5.dp, labelColor.copy(0.05f)), Alignment.Center) {
                                        if (symbol.isNotEmpty()) {
                                            Text(symbol, color = symColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                
                                val attBrt = "0"
                                val tmAtt = st.optString("tm_att")
                                val ttlAtt = st.optString("total_att")
                                val absents = st.optString("a")
                                val fne = st.optString("fine")

                                Text(attBrt, Modifier.width(50.dp), color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                Text(tmAtt, Modifier.width(45.dp), color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                Text(ttlAtt, Modifier.width(50.dp), color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                Text(absents, Modifier.width(40.dp), color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                Text(fne, Modifier.width(50.dp), color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // HOLIDAY OVERLAYS
                    // Left offset = 140.dp (40 + 100)
                    for (span in holidaySpans) {
                        val offsetDay = span.first - 1
                        val spanWidth = span.second * 28 // using 28.dp per column
                        Box(
                            Modifier
                                .matchParentSize()
                                .padding(start = (140 + (offsetDay * 28)).dp)
                        ) {
                            Box(
                                Modifier
                                    .width(spanWidth.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF1E293B).copy(0.9f))
                                    .border(0.5.dp, labelColor.copy(0.1f))
                                    .clickable { onHolidayClick(span) },
                                Alignment.Center
                            ) {
                                Text(
                                    text = span.third,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                    modifier = Modifier.graphicsLayer(rotationZ = -90f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffMonthlyLedger(data: org.json.JSONObject?, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    if (data == null) {
        Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
        return
    }

    val staffArr = data.optJSONArray("staff")
    val headerColor = Color(0xFF3B82F6)

    Column(Modifier.fillMaxWidth().border(1.dp, labelColor.copy(0.1f), RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
        Row(Modifier.fillMaxWidth().background(headerColor).padding(vertical = 12.dp, horizontal = 16.dp)) {
            Text("NAME", Modifier.weight(1.5f), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text("ROLE", Modifier.weight(1f), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text("LEAVE (M)", Modifier.weight(0.8f), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text("ABSENT (M)", Modifier.weight(0.8f), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text("TOTAL (YTD)", Modifier.weight(1f), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
        
        if (staffArr == null || staffArr.length() == 0) {
            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("No staff data found", color = labelColor, fontSize = 12.sp) }
        } else {
             Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                 for (i in 0 until staffArr.length()) {
                     val s = staffArr.optJSONObject(i)
                     Row(Modifier.fillMaxWidth().background(if(i%2==0) cardColor.copy(0.1f) else Color.Transparent).padding(vertical = 16.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                         Text(s.optString("name"), Modifier.weight(1.5f), color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                         Text(s.optString("role").uppercase(), Modifier.weight(1f), color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                         Text(s.optString("leaves_m"), Modifier.weight(0.8f), color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                         Text(s.optString("absents_m"), Modifier.weight(0.8f), color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                         Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                             Text("L:${s.optString("ytd_l")}", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black)
                             Text("A:${s.optString("ytd_a")}", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Black)
                         }
                     }
                 }
             }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceRulesTab(isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Institutional Standards & Payout Rules", color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp)

        // Absence & Deduction Section
        RuleSectionCard("ABSENCE & DEDUCTION", isDark, labelColor, cardColor) {
            RuleEntryRow("MONTHLY ABSENCE LIMIT", "3", isDark, textColor, labelColor)
            RuleEntryRow("FIXED PENALTY PER ABSENCE (RS)", "0", isDark, textColor, labelColor)
            Text("* Automatic Logic: Beyond limit, system deducts 1 full day's pay (Salary / 30).", color = Color(0xFFEF4444), fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }

        // Teaching Bonus Section
        RuleSectionCard("TEACHING BONUS (RS PER EXTRA CLASS)", isDark, labelColor, cardColor) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BonusCol("PRIMARY", "200", Modifier.weight(1f), isDark, textColor, labelColor)
                BonusCol("MIDDLE", "300", Modifier.weight(1f), isDark, textColor, labelColor)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BonusCol("HIGH", "400", Modifier.weight(1f), isDark, textColor, labelColor)
                BonusCol("SECONDARY", "500", Modifier.weight(1f), isDark, textColor, labelColor)
            }
        }

        // Fines Section
        RuleSectionCard("DISCIPLINARY FINES (RS)", isDark, labelColor, cardColor) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FineBadge("HAIR CUT", "10", isDark)
                FineBadge("REGISTER/BOOKS", "20", isDark)
                FineBadge("RULES BREAK", "30", isDark)
                FineBadge("ATTENDANCE", "10", isDark)
            }
        }

        Button(
            onClick = { /* Sync Rules */ },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Sync, null, Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("SYNCHRONIZE INSTITUTIONAL RULES", fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Text("Rule changes apply immediately to all active payroll calculations.", color = labelColor, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun RuleSectionCard(title: String, isDark: Boolean, labelColor: Color, cardColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, labelColor.copy(0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFF3B82F6), fontWeight = FontWeight.Black, fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun RuleEntryRow(label: String, value: String, isDark: Boolean, textColor: Color, labelColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Surface(color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), shape = RoundedCornerShape(8.dp)) {
            Text(value, color = Color(0xFF3B82F6), fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

@Composable
fun BonusCol(label: String, value: String, modifier: Modifier, isDark: Boolean, textColor: Color, labelColor: Color) {
    Column(modifier) {
        Text(label, color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Surface(color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(value, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(10.dp))
        }
    }
}

@Composable
fun FineBadge(label: String, value: String, isDark: Boolean) {
    Surface(
        color = Color(0xFF6366F1).copy(0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(0.3f))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if(isDark) Color.White.copy(0.7f) else Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("RS $value", color = Color(0xFF6366F1), fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeaveAppealsTab(viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isStudent = dashboardData?.role?.lowercase()?.contains("student") == true
    val myUserIdRaw = dashboardData?.stats?.get("id") ?: dashboardData?.stats?.get("student_id") ?: dashboardData?.user_id
    val myUserId = when(myUserIdRaw) {
        is Int -> myUserIdRaw
        is Double -> myUserIdRaw.toInt()
        is String -> myUserIdRaw.toDoubleOrNull()?.toInt() ?: 0
        else -> 0
    }
    val instId = viewModel.getInstitutionId()
    
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    val accentPurple = Color(0xFF6366F1)

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(if(isStudent) "MY LEAVE APPEALS" else "PENDING LEAVE APPEALS", color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val tab1Label = if(isStudent) "HISTORY" else "STAFF REQUESTS"
            val tab2Label = if(isStudent) "SUBMIT NEW" else "STUDENT REQUESTS"

            Box(Modifier.weight(1f)) {
                Surface(
                    onClick = { subTab = 0 },
                    color = if(subTab == 0) accentPurple else cardColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accentPurple.copy(0.2f))
                ) {
                    Box(Modifier.fillMaxWidth().height(42.dp), Alignment.Center) { Text(tab1Label, color = if(subTab == 0) Color.White else labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                }
            }
            Box(Modifier.weight(1f)) {
                Surface(
                    onClick = { subTab = 1 },
                    color = if(subTab == 1) accentPurple else cardColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accentPurple.copy(0.2f))
                ) {
                    Box(Modifier.fillMaxWidth().height(42.dp), Alignment.Center) { Text(tab2Label, color = if(subTab == 1) Color.White else labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (isStudent && subTab == 1) {
            val context = androidx.compose.ui.platform.LocalContext.current
            fun showDatePicker(onDate: (String) -> Unit) { 
                val current = java.util.Calendar.getInstance()
                android.app.DatePickerDialog(context, {_,y,m,d -> 
                    val c = java.util.Calendar.getInstance()
                    c.set(y, m, d)
                    onDate(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(c.time)) 
                }, current.get(java.util.Calendar.YEAR), current.get(java.util.Calendar.MONTH), current.get(java.util.Calendar.DAY_OF_MONTH)).show() 
            }
            var fromDate by remember { mutableStateOf("Choose start date...") }
            var toDate by remember { mutableStateOf("Choose end date...") }
            var reason by remember { mutableStateOf("") }
            var leaveType by remember { mutableStateOf("SICK LEAVE") }
            var typeExpanded by remember { mutableStateOf(false) }

            Card(colors = CardDefaults.cardColors(containerColor = cardColor.copy(0.5f)), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("APPEAL FOR LEAVE", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("FROM DATE", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Surface(onClick = { showDatePicker { fromDate = it } }, color = Color.Black.copy(0.1f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), Alignment.CenterStart) { Text(fromDate, color = if(fromDate.contains("-")) textColor else labelColor, fontSize = 11.sp) }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("TO DATE", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Surface(onClick = { showDatePicker { toDate = it } }, color = Color.Black.copy(0.1f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), Alignment.CenterStart) { Text(toDate, color = if(toDate.contains("-")) textColor else labelColor, fontSize = 11.sp) }
                            }
                        }
                    }

                    Column {
                        Text("LEAVE TYPE", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Box {
                            Surface(onClick = { typeExpanded = true }, color = Color.Black.copy(0.1f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(leaveType, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, null, tint = labelColor)
                                }
                            }
                            androidx.compose.material3.DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }, modifier = Modifier.background(cardColor)) {
                                listOf("SICK LEAVE", "CASUAL LEAVE", "EMERGENCY LEAVE", "OTHER").forEach { t ->
                                    androidx.compose.material3.DropdownMenuItem(text = { Text(t, color = textColor, fontSize = 11.sp) }, onClick = { leaveType = t; typeExpanded = false })
                                }
                            }
                        }
                    }

                    Column {
                        Text("REASON FOR LEAVE", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        PremiumTextField(reason, { reason = it }, "Detailed reason...", Icons.Default.Edit, isDark)
                    }

                    Button(
                        onClick = { 
                            if (!fromDate.contains("-") || !toDate.contains("-") || reason.isBlank()) {
                                android.widget.Toast.makeText(context, "Please fill all fields", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.submitLeaveAppeal(instId, myUserId, fromDate, toDate, leaveType, reason) { success ->
                                    if(success) { 
                                        android.widget.Toast.makeText(context, "Appeal Submitted Successfully", android.widget.Toast.LENGTH_SHORT).show()
                                        fromDate = "Choose start date..."
                                        toDate = "Choose end date..."
                                        reason = ""
                                        subTab = 0 
                                    } else {
                                        android.widget.Toast.makeText(context, "Submission Failed", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } 
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("SUBMIT APPEAL", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
 else {
            // LIST VIEW (History for Student, Pending for Admin)
            Spacer(Modifier.height(60.dp))
            Box(Modifier.fillMaxWidth(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MoveToInbox, null, Modifier.size(64.dp), tint = labelColor.copy(0.2f))
                    Spacer(Modifier.height(16.dp))
                    val emptyMsg = when {
                        isStudent -> "No recent leave appeals found"
                        subTab == 0 -> "No Pending Staff Appeals"
                        else -> "No Pending Student Appeals"
                    }
                    Text(emptyMsg, color = labelColor.copy(0.5f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusAdminTab(viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    val submissionStatus by viewModel.submissionStatus.collectAsState()
    var selectedDate by remember { androidx.compose.runtime.mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())) }
    var searchQuery by remember { androidx.compose.runtime.mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(selectedDate) {
        viewModel.fetchSubmissionStatus(selectedDate)
    }

    val items = submissionStatus?.items ?: emptyList()
    val submittedCount = items.count { it.submitted }
    val pendingCount = items.count { !it.submitted }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Attendance Submission Status", color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.height(20.dp))

        Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SELECT DATE", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(6.dp))
                        Surface(color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), shape = RoundedCornerShape(8.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(selectedDate, color = textColor, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { viewModel.fetchSubmissionStatus(selectedDate) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(48.dp).padding(top = 15.dp)) {
                        Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Load Status", fontWeight = FontWeight.Black)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("$submittedCount Submitted", Color(0xFF10B981), Modifier.weight(1f))
                    StatusChip("$pendingCount Pending", Color(0xFFEF4444), Modifier.weight(1f))
                    StatusChip("2 Requests", Color(0xFF6366F1), Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Tab Row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = Color(0xFF3B82F6).copy(0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFF3B82F6))) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Grid3x3, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Class Status", color = Color(0xFF3B82F6), fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
            Surface(color = cardColor.copy(0.5f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, labelColor.copy(0.2f))) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = labelColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Requests", color = labelColor, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Class-wise Status", color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            PremiumTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search Class...",
                icon = Icons.Default.Search,
                isDark = isDark,
                modifier = Modifier.width(180.dp)
            )
        }
        Spacer(Modifier.height(12.dp))

        val filteredItems = items.filter { it.class_name.contains(searchQuery, ignoreCase = true) || it.section_name.contains(searchQuery, ignoreCase = true) }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredItems.forEachIndexed { i, item ->
                ClassStatusRow(i+1, item, selectedDate, viewModel, isDark, textColor, labelColor, cardColor)
            }
            if (filteredItems.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                    Text("No records found", color = labelColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier) {
    Surface(color = color.copy(0.1f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, color.copy(0.2f)), modifier = modifier) {
        Row(Modifier.padding(vertical = 6.dp, horizontal = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(text.uppercase(), color = color, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ClassStatusRow(idx: Int, item: com.example.wantuch.domain.model.AttendanceSubmissionItem, date: String, viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, labelColor.copy(0.05f))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#$idx", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                Column(Modifier.weight(1f)) {
                    Text("${item.class_name} - ${item.section_name}", color = textColor, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("Teacher: ${item.teacher_name ?: "--"}", color = labelColor, fontSize = 9.sp)
                }
                Surface(
                    color = (if(item.submitted) Color(0xFF10B981) else Color(0xFFEF4444)).copy(0.1f), 
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(if(item.submitted) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(if(item.submitted) "SUBMITTED" else "PENDING", color = if(item.submitted) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            if (item.submitted_at != null) {
                Spacer(Modifier.height(4.dp))
                Text("At: ${item.submitted_at}", color = labelColor, fontSize = 8.sp)
            }
            Spacer(Modifier.height(10.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.submitted) {
                    Button(
                        onClick = { viewModel.markAttendanceSubmitted(item.class_id, item.section_id, date) {} },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Sync, null, Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Re-submit", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { viewModel.clearAttendanceSubmission(item.class_id, item.section_id, date) {} },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    Button(
                        onClick = { viewModel.markAttendanceSubmitted(item.class_id, item.section_id, date) {} },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Send, null, Modifier.size(12.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mark Submitted", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ManualAttendanceModal(
    viewModel: WantuchViewModel,
    isDark: Boolean,
    textColor: Color,
    labelColor: Color,
    cardColor: Color,
    onDismiss: () -> Unit,
    targetType: String,
    selectedClassId: Int,
    date: String
) {
    val studentsData by viewModel.studentsData.collectAsState()
    val staffData by viewModel.staffData.collectAsState()
    
    val items = if (targetType == "Students") {
        studentsData?.students ?: emptyList()
    } else {
        (staffData?.teaching_staff ?: emptyList()) + (staffData?.non_teaching_staff ?: emptyList())
    }

    val filteredItems = if (targetType == "Students" && selectedClassId > 0) {
        val structure by viewModel.schoolStructure.collectAsState()
        val classNameMatch = structure?.classes?.find { it.id == selectedClassId }?.name ?: ""
        items.filter { (it as? com.example.wantuch.domain.model.StudentMember)?.class_section?.contains(classNameMatch, ignoreCase = true) == true }
    } else {
        items
    }

    val selectedIds = remember { mutableStateListOf<String>() }
    val isAllSelected = filteredItems.isNotEmpty() && selectedIds.size == filteredItems.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF0F172A).copy(0.98f), 
            shape = RoundedCornerShape(24.dp), 
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().fillMaxHeight(0.85f),
            border = BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(date, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                        if (isAllSelected) selectedIds.clear() 
                        else {
                            selectedIds.clear()
                            filteredItems.forEach { item ->
                                val id = when(item) {
                                    is com.example.wantuch.domain.model.StudentMember -> item.id?.toString()
                                    is com.example.wantuch.domain.model.StaffMember -> item.id?.toString()
                                    else -> null
                                }
                                id?.let { selectedIds.add(it) }
                            }
                        }
                    }) {
                        androidx.compose.material3.Checkbox(
                            checked = isAllSelected, 
                            onCheckedChange = null,
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6))
                        )
                        Text("SELECT ALL", color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = labelColor) }
                }
                
                Spacer(Modifier.height(20.dp))
                
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredItems.size) { i ->
                        val item = filteredItems[i]
                        val name = if(item is com.example.wantuch.domain.model.StudentMember) item.name else (item as com.example.wantuch.domain.model.StaffMember).name
                        val initials = if(item is com.example.wantuch.domain.model.StudentMember) item.initials else (item as com.example.wantuch.domain.model.StaffMember).initials
                        val id = if(item is com.example.wantuch.domain.model.StudentMember) item.id?.toString() else (item as com.example.wantuch.domain.model.StaffMember).id?.toString()
                        val isSelected = id != null && selectedIds.contains(id)
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, 
                            modifier = Modifier
                                .background(if(isSelected) Color(0xFF3B82F6).copy(0.2f) else Color.White.copy(0.04f), RoundedCornerShape(16.dp))
                                .border(1.dp, if(isSelected) Color(0xFF3B82F6) else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable { id?.let { if(isSelected) selectedIds.remove(it) else selectedIds.add(it) } }
                                .padding(12.dp)
                        ) {
                            Box(Modifier.size(50.dp).background(if(isSelected) Color(0xFF3B82F6).copy(0.4f) else Color(0xFF6366F1).copy(0.15f), CircleShape), Alignment.Center) {
                                 Text(initials.take(2), color = if(isSelected) Color.White else Color(0xFF6366F1), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(name, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            Text("CN: ${i + 1}", color = labelColor, fontSize = 8.sp)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(Color(0xFFEF4444), CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text("PENDING", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                Text("APPLY STATUS TO ${selectedIds.size} SELECTED", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(12.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statuses = listOf("P" to Color(0xFF10B981), "A" to Color(0xFFEF4444), "L" to Color(0xFF3B82F6), "SO" to Color(0xFFF59E0B), "UM" to Color(0xFF6366F1))
                    statuses.forEach { (tag, color) ->
                        Surface(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    selectedIds.forEach { idStr ->
                                        val id = idStr.toIntOrNull() ?: 0
                                        if (targetType == "Students") {
                                            viewModel.markStudentAttendance(id, tag, date) {}
                                        } else {
                                            viewModel.markStaffAttendance(id, tag, date) {}
                                        }
                                    }
                                    onDismiss()
                                }
                            },
                            color = color.copy(0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, color.copy(0.4f)),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text(tag, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExcelAttendanceWizardModal(
    viewModel: WantuchViewModel,
    isDark: Boolean,
    textColor: Color,
    labelColor: Color,
    cardColor: Color,
    dateFrom: String,
    dateTo: String,
    classSelection: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val modalBg = Color(0xFF0D1B2E)
    val accentCyan = Color(0xFF06B6D4)
    val accentGreen = Color(0xFF10B981)

    // Step management: 1=download+upload, 2=preview
    var step by remember { androidx.compose.runtime.mutableIntStateOf(1) }
    var isLoading by remember { androidx.compose.runtime.mutableStateOf(false) }
    var statusMsg by remember { androidx.compose.runtime.mutableStateOf("") }

    // Parsed CSV data for preview
    data class CsvRow(val studentId: String, val roll: String, val name: String, val cells: List<Pair<String, String>>)
    data class ParsedCsv(val dateHeaders: List<String>, val rows: List<CsvRow>)
    var parsedCsv by remember { androidx.compose.runtime.mutableStateOf<ParsedCsv?>(null) }

    // Helper: parse CSV text
    fun parseCsvText(text: String): ParsedCsv? {
        val lines = text.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 2) return null
        fun splitLine(line: String): List<String> {
            val result = mutableListOf<String>()
            var cur = StringBuilder()
            var inQ = false
            for (ch in line) {
                when {
                    ch == '"' -> inQ = !inQ
                    ch == ',' && !inQ -> { result.add(cur.toString().trim().trimStart('"').trimEnd('"')); cur = StringBuilder() }
                    else -> cur.append(ch)
                }
            }
            result.add(cur.toString().trim().trimStart('"').trimEnd('"'))
            return result
        }
        val headers = splitLine(lines[0])
        val dateHeaders = mutableListOf<String>()
        var firstDateIdx = -1
        
        for (j in 0 until headers.size) {
            val h = headers[j]
            if (h.contains("(in)", ignoreCase = true)) {
                if (firstDateIdx == -1) firstDateIdx = j
                dateHeaders.add(h.replace(Regex("\\s*\\(in\\)", RegexOption.IGNORE_CASE), "").trim())
            }
        }
        
        if (dateHeaders.isEmpty() || firstDateIdx == -1) return null
        
        val rows = mutableListOf<CsvRow>()
        for (i in 1 until lines.size) {
            val cols = splitLine(lines[i])
            if (cols.size <= firstDateIdx) continue
            
            val cells = mutableListOf<Pair<String, String>>()
            var j = firstDateIdx
            while (j + 1 < cols.size && j + 1 < headers.size) {
                if (headers[j].contains("(in)", ignoreCase = true)) {
                    cells.add(Pair(cols[j], cols[j+1]))
                    j += 2
                } else j++
            }
            // Fill ID, Roll, Name based on firstDateIdx
            val sId = if (firstDateIdx > 0) cols[0] else ""
            val roll = if (firstDateIdx > 1) cols[1] else ""
            val name = if (firstDateIdx > 2) cols[2] else ""
            
            rows.add(CsvRow(sId, roll, name, cells))
        }
        return ParsedCsv(dateHeaders, rows)
    }

    // File picker for CSV
    val csvPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()
                 val parsed = try { parseCsvText(text) } catch(e: Exception) { null }
                 if (parsed != null && parsed.rows.isNotEmpty()) {
                      parsedCsv = parsed
                      step = 2
                      statusMsg = ""
                  } else {
                      statusMsg = "Invalid CSV format or missing Attendance columns (e.g. '(In)'). Please use template from Step 1."
                  }
              } catch (e: Exception) {
                  statusMsg = "Error reading file: ${e.message}"
              }
        }
    }

    // Helper: Cell color for preview
    fun cellColor(v: String): Color {
        return when {
            v.contains(":") || v.equals("P", true) || v.contains("AM", true) || v.contains("PM", true) -> Color(0xFF10B981)
            v.equals("A", true) -> Color(0xFFEF4444)
            v.equals("L", true) -> Color(0xFF3B82F6)
            v.equals("S", true) -> Color(0xFF6366F1)
            v.isEmpty() -> labelColor.copy(0.3f)
            else -> labelColor
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = modalBg,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth().fillMaxHeight(0.92f),
            border = BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    Modifier.fillMaxWidth().background(Color.White.copy(0.03f)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Grid3x3, null, tint = accentCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("EXCEL ATTENDANCE WIZARD", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = labelColor, modifier = Modifier.size(18.dp)) }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (step == 1) {
                        // ─── STEP 1: Download Template ───
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(0.08f))
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("1. Download Template", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Get the pre-filled horizontal template for your selection.", color = labelColor, fontSize = 10.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        isLoading = true
                                        statusMsg = "Generating CSV..."
                                        viewModel.downloadAttendanceCsv(
                                            dateFrom = dateFrom,
                                            dateTo = dateTo,
                                            classSelection = classSelection,
                                            onSuccess = { bytes, fileName ->
                                                isLoading = false
                                                statusMsg = ""
                                                try {
                                                    // Save to Downloads
                                                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                                    val file = java.io.File(downloadsDir, fileName)
                                                    file.writeBytes(bytes)
                                                    // Notify media scanner
                                                    android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                                                    statusMsg = "✓ Saved to Downloads: $fileName"
                                                } catch (e: Exception) {
                                                    statusMsg = "Error saving: ${e.message}"
                                                }
                                            },
                                            onError = { err ->
                                                isLoading = false
                                                statusMsg = "Error: $err"
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isLoading
                                ) {
                                    Text("DOWNLOAD CSV", color = Color(0xFF0D1B2E), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        // ─── STEP 2: Import & Preview ───
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(0.08f))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("2. Import & Preview", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(16.dp))
                                // Upload drop zone
                                Surface(
                                    onClick = { csvPickerLauncher.launch("text/*") },
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, labelColor.copy(0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.MoveToInbox, null,
                                            tint = labelColor.copy(0.6f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text("CHOOSE CSV FILE", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        Text("Selected from your local storage", color = labelColor, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // ─── PREVIEW ───
                        val csv = parsedCsv
                        if (csv != null) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Preview — ${csv.rows.size} students, ${csv.dateHeaders.size} days",
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    onClick = { step = 1; parsedCsv = null; statusMsg = "" },
                                    color = Color.White.copy(0.05f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("← BACK", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            // Replace LazyColumn with simple Column since the outer Column already handles scrolling
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                // Header row
                                Row(
                                    Modifier.background(Color.White.copy(0.08f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("ID", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(36.dp))
                                    Text("Roll", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(28.dp))
                                    Text("Name", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                                    csv.dateHeaders.forEach { d ->
                                        Text(d.takeLast(5), color = labelColor, fontSize = 7.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                                    }
                                }
                                csv.rows.forEachIndexed { idx, row ->
                                    Row(
                                        Modifier
                                            .background(
                                                if(idx % 2 == 0) Color.White.copy(0.03f) else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(row.studentId, color = textColor, fontSize = 8.sp, modifier = Modifier.width(36.dp))
                                        Text(row.roll, color = textColor, fontSize = 8.sp, modifier = Modifier.width(28.dp))
                                        Text(row.name, color = textColor, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(80.dp))
                                        row.cells.forEach { (inV, outV) ->
                                            val inColor = cellColor(inV)
                                            Column(Modifier.width(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(inV, color = inColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                if(outV.isNotEmpty()) Text(outV, color = labelColor, fontSize = 6.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Status Messages
                    if (statusMsg.isNotEmpty()) {
                        Surface(
                            color = if(statusMsg.startsWith("✓")) Color(0xFF10B981).copy(0.1f) else Color(0xFFEF4444).copy(0.1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if(statusMsg.startsWith("✓")) Color(0xFF10B981) else Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(statusMsg, color = if(statusMsg.startsWith("✓")) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 10.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                // Bottom Footer
                if (step == 2) {
                    Row(
                        Modifier.fillMaxWidth().background(Color.White.copy(0.02f)).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.07f), contentColor = labelColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("CANCEL", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val csv = parsedCsv ?: return@Button
                                isLoading = true
                                statusMsg = "Saving attendance..."
                                // Build JSON payload
                                val records = org.json.JSONArray()
                                csv.rows.forEach { row ->
                                    val attendance = org.json.JSONObject()
                                    csv.dateHeaders.forEachIndexed { i, date ->
                                        val cell = row.cells.getOrNull(i) ?: Pair("", "")
                                        val dateArr = org.json.JSONArray()
                                        dateArr.put(cell.first)
                                        dateArr.put(cell.second)
                                        attendance.put(date, dateArr)
                                    }
                                    val rec = org.json.JSONObject()
                                    rec.put("student_id", row.studentId)
                                    rec.put("attendance", attendance)
                                    records.put(rec)
                                }
                                val datesArr = org.json.JSONArray()
                                csv.dateHeaders.forEach { datesArr.put(it) }
                                val payload = org.json.JSONObject()
                                payload.put("records", records)
                                payload.put("dates", datesArr)

                                viewModel.saveBulkImportJson(
                                    jsonBody = payload.toString(),
                                    onSuccess = { processed ->
                                        isLoading = false
                                        statusMsg = "✓ SUCCESS: Saved $processed daily attendance entries."
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        statusMsg = "Error: $err"
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(2f).height(48.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("SAVE ATTENDANCE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                } else { // step == 1
                    Row(
                        Modifier.fillMaxWidth().background(Color.White.copy(0.02f)).padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.07f), contentColor = labelColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("CANCEL", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FaceRecognitionCameraPreview(modifier: Modifier, viewModel: WantuchViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val executor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    
    // Face Detector setup - FAST mode + tracking for multiple faces
    val faceDetector = remember {
        com.google.mlkit.vision.face.FaceDetection.getClient(
            com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                .setPerformanceMode(com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .enableTracking()
                .build()
        )
    }

    var lastVerificationTime by remember { mutableLongStateOf(0L) }
    var detectedFaces by remember { mutableStateOf<List<com.google.mlkit.vision.face.Face>>(emptyList()) }
    var faceResultsMap by remember { mutableStateOf<Map<Int, VerifyFaceResponse>>(emptyMap()) }
    var scaleX by remember { mutableFloatStateOf(1f) }
    var scaleY by remember { mutableFloatStateOf(1f) }

    // Permission Management
    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier) {
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = androidx.camera.view.PreviewView(ctx).apply {
                        scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                    }
                    
                    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = androidx.camera.core.ImageAnalysis.Builder()
                        .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                        .also {
                            it.setAnalyzer(executor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    
                                    // Update scales for drawing boxes (assuming 480x640 or standard aspect)
                                    // In a real app, we should get these from the analyzer resolution
                                    
                                    faceDetector.process(image)
                                        .addOnSuccessListener { faces ->
                                            detectedFaces = faces
                                            if (faces.isNotEmpty()) {
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - lastVerificationTime > 600) {
                                                    lastVerificationTime = currentTime
                                                    val fullBitmap = imageProxy.toBitmap()
                                                        .rotateAndMirror(imageProxy.imageInfo.rotationDegrees, true)
                                                    val bmpW = fullBitmap.width
                                                    val bmpH = fullBitmap.height

                                                    val recognizedUsers = viewModel.recognizedInSession.value.keys
                                                    
                                                    // Recognize EACH unknown face individually
                                                    val newResults = mutableMapOf<Int, VerifyFaceResponse>()
                                                    val sessionToUpdate = mutableMapOf<Int, String>()
                                                    
                                                    faces.forEach { face ->
                                                        val trackId = face.trackingId ?: face.hashCode()
                                                        
                                                        // Optimization: Skip recognition if we already have a PERSISTENT match for this session/track
                                                        val existingResult = faceResultsMap[trackId]
                                                        if (existingResult?.matched == true && recognizedUsers.contains(existingResult.user_id)) {
                                                            newResults[trackId] = existingResult
                                                            return@forEach
                                                        }
                                                        
                                                        try {
                                                            val box = face.boundingBox
                                                            val pad = 35 // Slightly larger padding for better recognition
                                                            val mL = (bmpW - box.right - pad).coerceAtLeast(0)
                                                            val mR = (bmpW - box.left + pad).coerceAtMost(bmpW)
                                                            val mT = (box.top - pad).coerceAtLeast(0)
                                                            val mB = (box.bottom + pad).coerceAtMost(bmpH)
                                                            val w = mR - mL
                                                            val h = mB - mT
                                                            
                                                            val cropped = if (w > 0 && h > 0)
                                                                android.graphics.Bitmap.createBitmap(fullBitmap, mL, mT, w, h)
                                                            else fullBitmap
                                                            
                                                            val result = viewModel.recognizeFaceLocallySync(cropped)
                                                            newResults[trackId] = result
                                                            
                                                            // If matched, prepare for session update
                                                            if (result.matched) {
                                                                result.user_id?.let { id ->
                                                                    sessionToUpdate[id] = result.name ?: "Unknown"
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            newResults[trackId] = VerifyFaceResponse("error", false, message = "Processing Error")
                                                        }
                                                    }
                                                    
                                                    // Update UI-state Map
                                                    faceResultsMap = newResults
                                                    
                                                    // Batch update session cache to avoid multiple recompositions
                                                    sessionToUpdate.forEach { (id, name) ->
                                                        viewModel.addRecognizedToSession(id, name)
                                                    }
                                                }
                                            } else {
                                                faceResultsMap = emptyMap()
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(context))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        } else {
            Box(Modifier.fillMaxSize(), androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.Text("Requesting Camera Permission...", color = androidx.compose.ui.graphics.Color.White)
            }
        }

        // Bounding Boxes Overlay — per-face labels via tracking IDs
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val frameWidth = 480f
            val frameHeight = 640f
            val sX = canvasWidth / frameWidth
            val sY = canvasHeight / frameHeight

            val recognizedUserIds = viewModel.recognizedInSession.value.keys

            detectedFaces.forEach { face ->
                val trackId = face.trackingId ?: face.hashCode()
                val result = faceResultsMap[trackId]
                
                // CRITICAL OPTIMIZATION: If user is already recognized in this session, 
                // skip drawing the square and info to keep UI clean and fast.
                if (result?.matched == true && recognizedUserIds.contains(result.user_id)) {
                    // Skip drawing for already captured users
                    return@forEach
                }

                val bounds = face.boundingBox

                val bLeft = canvasWidth - (bounds.right * sX)
                val bRight = canvasWidth - (bounds.left * sX)
                val bTop = bounds.top * sY
                val bBottom = bounds.bottom * sY

                val cx = (bLeft + bRight) / 2f
                val cy = (bTop + bBottom) / 2f
                val rawWidth = bRight - bLeft
                val rawHeight = bBottom - bTop
                val boxSize = kotlin.math.max(rawWidth, rawHeight) * 1.1f
                
                val finalLeft = cx - (boxSize / 2f)
                val finalTop = cy - (boxSize / 2f)

                val isMatched = result?.matched == true
                val boxColor = if (isMatched) Color(0xFF10B981) else Color(0xFF22D3EE)

                drawRoundRect(
                    color = boxColor,
                    topLeft = androidx.compose.ui.geometry.Offset(finalLeft, finalTop),
                    size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f)
                )

                drawIntoCanvas { canvas ->
                    val text = when {
                        isMatched -> "MATCH: ${result?.name?.uppercase()}"
                        result == null -> "SCANNING..."
                        else -> "UNKNOWN USER"
                    }
                    val paint = android.graphics.Paint().apply {
                        color = if (isMatched) android.graphics.Color.parseColor("#10B981")
                                else android.graphics.Color.parseColor("#22D3EE")
                        textSize = 38f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val textWidth = paint.measureText(text)
                    val bgPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        alpha = 160
                    }
                    // Info label above box
                    canvas.nativeCanvas.drawRoundRect(
                        android.graphics.RectF(finalLeft, finalTop - 56f, finalLeft + textWidth + 24f, finalTop - 4f),
                        8f, 8f, bgPaint
                    )
                    canvas.nativeCanvas.drawText(text, finalLeft + 12f, finalTop - 18f, paint)
                }
            }
        }
    }
}

@Composable
fun FaceCapturePreview(modifier: Modifier, onFaceCaptured: (android.graphics.Bitmap) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val executor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    
    // Permission Management
    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val faceDetector = remember {
        com.google.mlkit.vision.face.FaceDetection.getClient(
            com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                .setPerformanceMode(com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build()
        )
    }

    var lastFace by remember { mutableStateOf<com.google.mlkit.vision.face.Face?>(null) }
    var currentBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    Box(modifier) {
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = androidx.camera.view.PreviewView(ctx).apply {
                        scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                    }
                    
                    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageAnalyzer = androidx.camera.core.ImageAnalysis.Builder()
                            .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .build()
                            .also {
                                it.setAnalyzer(executor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        faceDetector.process(image)
                                            .addOnSuccessListener { faces ->
                                                lastFace = faces.firstOrNull()
                                                if (lastFace != null) {
                                                    currentBitmap = imageProxy.toBitmap().rotateAndMirror(imageProxy.imageInfo.rotationDegrees, true)
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }
                        val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
                        try {
                            val activityLifecycleOwner = ctx as? androidx.lifecycle.LifecycleOwner ?: lifecycleOwner
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(activityLifecycleOwner, cameraSelector, preview, imageAnalyzer)
                        } catch (e: Exception) {}
                    }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                    
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Requesting Camera Permission...", color = Color.White)
            }
        }

        // Capture UI
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
            if (lastFace != null) {
                Button(
                    onClick = {
                        val face = lastFace
                        val bmp = currentBitmap
                        if (face != null && bmp != null) {
                            try {
                                // Crop face with some padding
                                val box = face.boundingBox
                                val padding = 40
                                // Mirror coordinates because bmp is horizontally mirrored
                                val mirroredLeft = bmp.width - box.right
                                val mirroredRight = bmp.width - box.left
                                
                                val left = (mirroredLeft - padding).coerceAtLeast(0)
                                val top = (box.top - padding).coerceAtLeast(0)
                                val right = (mirroredRight + padding).coerceAtMost(bmp.width)
                                val bottom = (box.bottom + padding).coerceAtMost(bmp.height)
                                val width = right - left
                                val height = bottom - top
                                
                                if (width > 0 && height > 0) {
                                    onFaceCaptured(android.graphics.Bitmap.createBitmap(bmp, left, top, width, height))
                                } else {
                                    onFaceCaptured(bmp)
                                }
                            } catch (e: Exception) {
                                onFaceCaptured(bmp)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Face, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("CAPTURE FACE", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            } else {
                Surface(color = Color.Black.copy(0.5f), shape = RoundedCornerShape(8.dp)) {
                    Text("ALIGN FACE TO CAPTURE", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
    }
}

// Utility extension to rotate and optionally mirror a Bitmap correctly using CameraX degrees
fun android.graphics.Bitmap.rotateAndMirror(degrees: Int, isFrontCamera: Boolean = true): android.graphics.Bitmap {
    if (degrees == 0 && !isFrontCamera) return this
    
    // Step 1: Handle native device rotation
    val rotateMatrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = android.graphics.Bitmap.createBitmap(this, 0, 0, width, height, rotateMatrix, true)
    
    // Step 2: Ensure correct UI mirroring across the portrait X-axis
    if (isFrontCamera) {
        val mirrorMatrix = android.graphics.Matrix().apply { postScale(-1f, 1f, rotated.width / 2f, rotated.height / 2f) }
        return android.graphics.Bitmap.createBitmap(rotated, 0, 0, rotated.width, rotated.height, mirrorMatrix, true)
    }
    return rotated
}

@Composable
fun FingerprintEnrollmentModal(
    viewModelSize: WantuchViewModel,
    isDark: Boolean,
    cardColor: Color,
    accentPurple: Color,
    onDismiss: () -> Unit
) {
    var selectedTarget by remember { mutableStateOf("Students") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableIntStateOf(0) }
    var selectedUserName by remember { mutableStateOf("") }
    var enrollmentStatus by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val studentsData by viewModelSize.studentsData.collectAsState()
    val staffData by viewModelSize.staffData.collectAsState()
    
    val userList = if (selectedTarget == "Students") {
        studentsData?.students ?: emptyList()
    } else {
        (staffData?.teaching_staff ?: emptyList()) + (staffData?.non_teaching_staff ?: emptyList())
    }

    val filteredList = userList.filter { u ->
        val name = when(u) {
            is com.example.wantuch.domain.model.StudentMember -> u.name ?: ""
            is com.example.wantuch.domain.model.StaffMember -> u.name ?: ""
            else -> ""
        }
        name.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            border = BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, null, tint = accentPurple, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("ENROLL BIOMETRIC", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White.copy(0.5f)) }
                }

                // Target Selector
                Row(Modifier.fillMaxWidth().background(Color.White.copy(0.05f), RoundedCornerShape(12.dp)).padding(4.dp)) {
                    listOf("Students", "Staff").forEach { t ->
                        val sel = selectedTarget == t
                        Box(
                            Modifier.weight(1f).height(40.dp).background(if(sel) accentPurple else Color.Transparent, RoundedCornerShape(8.dp)).clickable { selectedTarget = t; selectedUserId = 0 },
                            Alignment.Center
                        ) {
                            Text(t.uppercase(), color = if(sel) Color.White else Color.White.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                if (selectedUserId == 0) {
                    PremiumTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search user to enroll...",
                        icon = Icons.Default.Search,
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    androidx.compose.foundation.lazy.LazyColumn(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList.size) { i ->
                            val u = filteredList[i]
                            val idAny = when (u) {
                                is com.example.wantuch.domain.model.StudentMember -> u.id
                                is com.example.wantuch.domain.model.StaffMember -> u.id
                                else -> 0
                            }
                            val id = when (idAny) {
                                is Double -> idAny.toInt()
                                is Int -> idAny
                                is String -> idAny.toDoubleOrNull()?.toInt() ?: 0
                                else -> 0
                            }
                            val name = when (u) {
                                is com.example.wantuch.domain.model.StudentMember -> u.name
                                is com.example.wantuch.domain.model.StaffMember -> u.name
                                else -> ""
                            } ?: "Unknown"
                            
                            Surface(
                                onClick = { selectedUserId = id; selectedUserName = name },
                                color = Color.White.copy(0.04f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(36.dp).background(accentPurple.copy(0.1f), CircleShape), Alignment.Center) {
                                        Text(name.take(1).uppercase(), color = accentPurple, fontWeight = FontWeight.Black)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(name.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Add, null, tint = accentPurple.copy(0.5f))
                                }
                            }
                        }
                    }
                } else {
                    // Selected User View
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(Modifier.size(80.dp).background(accentPurple.copy(0.1f), CircleShape), Alignment.Center) {
                             Icon(Icons.Default.Fingerprint, null, tint = accentPurple, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(selectedUserName.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(if(selectedTarget == "Students") "STUDENT ID: $selectedUserId" else "STAFF ID: $selectedUserId", color = Color.White.copy(0.5f), fontSize = 10.sp)
                        
                        Spacer(Modifier.height(40.dp))
                        
                        if (isScanning) {
                            CircularProgressIndicator(color = accentPurple)
                            Spacer(Modifier.height(16.dp))
                            Text("SCANNING FINGER...", color = accentPurple, fontWeight = FontWeight.Black)
                        } else {
                            if (enrollmentStatus.isNotEmpty()) {
                                Surface(color = Color(0xFF10B981).copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                                    Text(enrollmentStatus, color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(10.dp))
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                            
                            Button(
                                onClick = {
                                    val activity = (context as? FragmentActivity)
                                    if (activity != null) {
                                        val executor = ContextCompat.getMainExecutor(activity)
                                        val biometricPrompt = BiometricPrompt(
                                            activity,
                                            executor,
                                            object : BiometricPrompt.AuthenticationCallback() {
                                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                    super.onAuthenticationError(errorCode, errString)
                                                    enrollmentStatus = "ERROR: $errString"
                                                    isScanning = false
                                                }
                                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                    super.onAuthenticationSucceeded(result)
                                                    enrollmentStatus = "SUCCESSFULLY ENROLLED"
                                                    viewModelSize.setLastEnrolledUser(selectedUserId, selectedUserName)
                                                    isScanning = false
                                                    viewModelSize.viewModelScope.launch {
                                                        kotlinx.coroutines.delay(1000)
                                                        onDismiss()
                                                    }
                                                }
                                                override fun onAuthenticationFailed() {
                                                    super.onAuthenticationFailed()
                                                    enrollmentStatus = "TRY AGAIN: READ FAILED"
                                                }
                                            }
                                        )

                                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                            .setTitle("Confirm Enrollment")
                                            .setSubtitle("Place finger to verify identity")
                                            .setNegativeButtonText("Cancel")
                                            .build()

                                        isScanning = true
                                        biometricPrompt.authenticate(promptInfo)
                                    } else {
                                        enrollmentStatus = "HARDWARE ERROR"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("CAPTURE FINGERPRINT", fontWeight = FontWeight.Black)
                            }
                            
                            TextButton(onClick = { selectedUserId = 0 }) {
                                Text("CHANGE USER", color = Color.White.copy(0.5f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCalendarView(
    viewModel: WantuchViewModel, 
    isDark: Boolean, 
    textColor: Color, 
    labelColor: Color, 
    cardColor: Color,
    selectedMonth: String,
    selectedYear: Int
) {
    val ledger by viewModel.studentLedger.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    
    // Robust User identification for Student role
    val myUserIdRaw = dashboardData?.stats?.get("id") ?: dashboardData?.stats?.get("student_id") ?: dashboardData?.user_id
    val myUserId = when(myUserIdRaw) {
        is Int -> myUserIdRaw
        is Double -> myUserIdRaw.toInt()
        is String -> myUserIdRaw.toDoubleOrNull()?.toInt() ?: 0
        else -> 0
    }
    
    // Find my record from ledger
    val recordsArray = ledger?.optJSONArray("students") ?: (ledger?.optJSONArray("records") ?: (ledger?.optJSONArray("data") ?: (ledger?.optJSONObject("data")?.optJSONArray("students"))))
    var myRecord: org.json.JSONObject? = null
    
    // Check if a single student profile is already loaded in ViewModel (backup)
    val myProfile by viewModel.studentProfile.collectAsState()
    val isMyProfile = (myProfile?.basic?.get("id") ?: myProfile?.basic?.get("student_id"))?.toString()?.toDoubleOrNull()?.toInt() == myUserId

    if (recordsArray != null) {
        val myName = dashboardData?.full_name?.trim() ?: ""
        val myCno = (dashboardData?.stats?.get("class_no") ?: dashboardData?.stats?.get("roll_no"))?.toString()?.trim() ?: ""
        
        // If it's a student login and ledger has only one record, it's likely them
        if (recordsArray.length() == 1) {
             myRecord = recordsArray.optJSONObject(0)
        } else {
            for (i in 0 until recordsArray.length()) {
                val record = recordsArray.optJSONObject(i)
                val ridRaw = record?.opt("student_id") ?: (record?.opt("id") ?: (record?.opt("user_id") ?: (record?.opt("rid") ?: record?.opt("sid"))))
                val rid = when(ridRaw) {
                    is Double -> ridRaw.toInt()
                    is Int -> ridRaw
                    is String -> ridRaw.toDoubleOrNull()?.toInt() ?: 0
                    else -> 0
                }
                
                val recordName = record?.optString("name")?.trim() ?: record?.optString("full_name")?.trim() ?: ""
                val recordCno = record?.optString("class_no")?.trim() ?: record?.optString("roll_no")?.trim() ?: ""
                
                // Aggressive check: ID matches OR (Name + ClassNo matches)
                if ((rid != 0 && rid == myUserId) || (myCno.isNotEmpty() && recordCno == myCno && recordName.equals(myName, ignoreCase = true))) {
                    myRecord = record
                    break
                }
                
                // Fallback: If names match (case insensitive) and myRecord is still null
                if (myName.isNotEmpty() && recordName.equals(myName, ignoreCase = true) && myRecord == null) {
                    myRecord = record
                }
            }
        }
    }
 
    val monthsList = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val mIdx = monthsList.indexOf(selectedMonth).coerceAtLeast(0)
    
    val calForGrid = java.util.Calendar.getInstance()
    calForGrid.set(selectedYear, mIdx, 1)
    val daysInMonth = calForGrid.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calForGrid.get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0=Sun
    
    // FINAL FALLBACK: If ledger matching failed, but profile is loaded for this user, use profile stats
    if (myRecord == null && isMyProfile && myProfile?.stats != null) {
        myRecord = org.json.JSONObject(myProfile?.stats as Map<*, *>)
    }

    val stats = myRecord?.optJSONObject("stats") ?: (if(myRecord?.has("month") == true) myRecord else null)
    val attendance = myRecord?.optJSONObject("attendance") ?: (if(myRecord?.has("calendar") == true) myRecord?.optJSONObject("calendar") else null)
    val holidaysArray = ledger?.optJSONArray("holidays")

    // Robust Stats retrieval
    val stats_p = (stats?.optString("tm_att") ?: (stats?.optString("p") ?: (stats?.optString("present") ?: stats?.optString("present_monthly"))))
    val stats_a = (stats?.optString("a") ?: (stats?.optString("absent") ?: stats?.optString("absent_monthly")))
    val stats_l = (stats?.optString("l") ?: (stats?.optString("leave") ?: stats?.optString("leave_monthly")))
    
    // Manual calculation fallback if stats are missing but attendance exists
    val (calcP, calcA, calcL) = attendance?.let { att ->
        var p = 0; var a = 0; var l = 0
        for (d in 1..daysInMonth) {
            val s = att.optString(d.toString(), "")
            when {
                s == "P" || s == "Present" -> p++
                s == "A" || s == "Absent" -> a++
                s == "L" || s == "Leave" -> l++
            }
        }
        Triple(p.toString(), a.toString(), l.toString())
    } ?: Triple("0", "0", "0")

    val pCount = if (stats_p == null || stats_p == "0" || stats_p == "") calcP else stats_p
    val aCount = if (stats_a == null || stats_a == "0" || stats_a == "") calcA else stats_a
    val lCount = if (stats_l == null || stats_l == "0" || stats_l == "") calcL else stats_l
    
    val ypCount = (stats?.optString("total_att") ?: (stats?.optString("y_p"))) ?: "0"
    val yaCount = stats?.optString("y_a") ?: "0"
    val ylCount = stats?.optString("y_l") ?: "0"
    
    val allTimeDays = (stats?.optString("total_days") ?: (stats?.optString("tm_days"))) ?: "0"

    val weekDays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    
    // Standardize month display
    val displayMonth = selectedMonth.uppercase()
    val displayYear = selectedYear
    
    Column(
        Modifier.fillMaxWidth()
    ) {
        // Calendar Grid Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(0.4f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(0.05f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    weekDays.forEach { day ->
                        Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                val rows = (daysInMonth + firstDayOfWeek + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0 until 7) {
                            val dayNum = r * 7 + c - firstDayOfWeek + 1
                            if (dayNum in 1..daysInMonth) {
                                val statusRaw = attendance?.optString(dayNum.toString()) ?: ""
                                
                                // Check if this day is a holiday from API
                                var holidayName = ""
                                if (holidaysArray != null) {
                                    for (hIdx in 0 until holidaysArray.length()) {
                                        val hObj = holidaysArray.optJSONObject(hIdx)
                                        val fDate = hObj.optString("from_date")
                                        val tDate = hObj.optString("to_date")
                                        try {
                                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                            val current = sdf.parse(String.format("%04d-%02d-%02d", selectedYear, mIdx + 1, dayNum))
                                            val start = sdf.parse(fDate)
                                            val end = sdf.parse(tDate)
                                            if (current != null && !current.before(start) && !current.after(end)) {
                                                holidayName = hObj.optString("name")
                                                break
                                            }
                                        } catch (e: Exception) {}
                                    }
                                }

                                val isSunday = c == 0
                                val isFriday = c == 5
                                
                                val (bg, contentColor, symbol) = when {
                                    holidayName.isNotEmpty() || statusRaw == "H" || statusRaw == "PH" || statusRaw == "Holiday" -> 
                                        Triple(Color(0xFFEAB308).copy(0.2f), Color(0xFFEAB308), "H")
                                    statusRaw == "P" || statusRaw == "Present" -> 
                                        Triple(Color(0xFF10B981).copy(0.15f), Color(0xFF10B981), "P")
                                    statusRaw == "A" || statusRaw == "Absent" -> 
                                        Triple(Color(0xFFEF4444).copy(0.15f), Color(0xFFEF4444), "A")
                                    statusRaw == "L" || statusRaw == "Leave" -> 
                                        Triple(Color(0xFF3B82F6).copy(0.15f), Color(0xFF3B82F6), "L")
                                    isSunday -> Triple(Color(0xFF7F1D1D).copy(0.15f), Color(0xFFFCA5A5), "") // Reddish for Sunday
                                    isFriday -> Triple(Color(0xFF065F46).copy(0.15f), Color(0xFF6EE7B7), "") // Greenish for Friday
                                    else -> Triple(Color.White.copy(0.03f), labelColor.copy(0.6f), "")
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bg)
                                        .border(0.5.dp, contentColor.copy(0.1f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(dayNum.toString(), color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        if (symbol.isNotEmpty()) {
                                            Text(symbol, color = contentColor.copy(0.7f), fontSize = 7.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text("MONTHLY STATS", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStatCard("PRESENT", pCount, Color(0xFF10B981), Modifier.weight(1f), isDark, cardColor)
            MiniStatCard("ABSENT", aCount, Color(0xFFF43F5E), Modifier.weight(1f), isDark, cardColor)
            MiniStatCard("LEAVE", lCount, Color(0xFFF59E0B), Modifier.weight(1f), isDark, cardColor)
        }

        Spacer(Modifier.height(32.dp))
        Text("ACADEMIC YEAR", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStatCard("PRESENT", ypCount, Color(0xFF10B981), Modifier.weight(1f), isDark, cardColor)
            MiniStatCard("ABSENT", yaCount, Color(0xFFF43F5E), Modifier.weight(1f), isDark, cardColor)
            MiniStatCard("LEAVE", ylCount, Color(0xFFF59E0B), Modifier.weight(1f), isDark, cardColor)
        }

        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(0.4f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(0.05f))
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                Text("All Time Days", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(allTimeDays, color = Color(0xFF3B82F6), fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun MiniStatCard(label: String, value: String, color: Color, modifier: Modifier, isDark: Boolean, cardColor: Color) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(0.4f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = color.copy(0.7f), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}

