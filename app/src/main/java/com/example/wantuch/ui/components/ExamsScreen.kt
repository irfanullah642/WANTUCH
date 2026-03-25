package com.example.wantuch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import com.example.wantuch.domain.model.*
import coil.compose.AsyncImage

@Composable
fun ExamsScreen(
    viewModel: WantuchViewModel,
    openWeb: (String) -> Unit,
    onBack: () -> Unit
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgMain = if (isDark) Color(0xFF0F172A) else Color(0xFFF1EFE9)
    val textMain = if (isDark) Color.White else Color.Black
    val instTitle = "RANA COLLEGE AND SCHOOL SYSTEM, KHWAZA KHELA SWAT" // Example from screenshot

    var selectedTab by remember { mutableStateOf("SCHEDULE") }
    
    val tabs = listOf(
        Triple("SCHEDULE", Icons.Default.Event, true),
        Triple("MARKS", Icons.Default.Create, false),
        Triple("RESULTS", Icons.Default.Description, false),
        Triple("INSIGHTS", Icons.Default.ShowChart, true),
        Triple("HALLS", Icons.Default.AccountBalance, true),
        Triple("RULES", Icons.Default.Settings, true)
    )

    Scaffold(
        containerColor = bgMain,
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(Modifier.statusBarsPadding())
            Spacer(Modifier.height(8.dp))

            // Upper Header
            Surface(
                color = if (isDark) Color(0xFF1E293B) else Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textMain)
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Icon(Icons.Default.School, contentDescription = "Exam", tint = textMain, modifier = Modifier.size(32.dp))
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Exams",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = textMain
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = textMain, modifier = Modifier.size(12.dp))
                        }
                        Text(
                            instTitle.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMain.copy(0.5f)
                        )
                    }

                    IconButton(onClick = { viewModel.toggleTheme() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme", tint = textMain)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Horizontal Tab Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tabs.forEach { (title, icon, isLocked) ->
                    val isSelected = selectedTab == title
                    val tabColor = if (isSelected) 
                        if (isDark) Color(0xFF1D4ED8) else Color(0xFFE0E7FF)
                    else 
                        if (isDark) Color(0xFF1E293B) else Color.White
                        
                    val titleColor = if (isSelected) 
                        if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                    else 
                        textMain
                        
                    val borderColor = if (isSelected) 
                        if (isDark) Color(0xFF3B82F6) else Color(0xFF3B82F6)
                    else 
                        if (isDark) Color(0xFF334155) else Color.LightGray.copy(0.3f)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = tabColor,
                        border = BorderStroke(1.dp, borderColor),
                        shadowElevation = if (isSelected) 4.dp else 2.dp,
                        modifier = Modifier
                            .width(110.dp)
                            .height(60.dp)
                            .clickable { selectedTab = title }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(icon, contentDescription = title, tint = titleColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    title,
                                    color = titleColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                                if (isLocked) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = titleColor, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == "SCHEDULE") {
                    var isScheduleExpanded by remember { mutableStateOf(true) }
                    ExamActionAccordion(
                        title = "Schedule New Exam", 
                        icon = Icons.Default.Event, 
                        isExpanded = isScheduleExpanded, 
                        isLocked = true,
                        isDark = isDark,
                        onClick = { isScheduleExpanded = !isScheduleExpanded }
                    ) {
                        ScheduleExamNativeForm(viewModel)
                    }
                    var isSlipsExpanded by remember { mutableStateOf(false) }
                    ExamActionAccordion(
                        title = "Generate Roll No Slips", 
                        icon = Icons.Default.AccountBox, 
                        hasLockOnLeft = true, 
                        isExpanded = isSlipsExpanded,
                        isDark = isDark,
                        onClick = { isSlipsExpanded = !isSlipsExpanded }
                    ) {
                        GenerateSlipsNativeForm(viewModel, isDark, openWeb)
                    }
                    var isAwardExpanded by remember { mutableStateOf(false) }
                    ExamActionAccordion(
                        title = "Generate Award List", 
                        icon = Icons.Default.Print, 
                        hasLockOnLeft = true, 
                        isExpanded = isAwardExpanded,
                        isDark = isDark,
                        onClick = { isAwardExpanded = !isAwardExpanded }
                    ) {
                        GenerateAwardListNativeForm(viewModel, isDark)
                    }
                    var isQuickExpanded by remember { mutableStateOf(false) }
                    ExamActionAccordion(
                        title = "Quick Datasheet", 
                        icon = Icons.Default.FlashOn, 
                        isExpanded = isQuickExpanded,
                        isDark = isDark,
                        onClick = { isQuickExpanded = !isQuickExpanded }
                    ) {
                        QuickDatasheetNativeForm(viewModel)
                    }
                } else if (selectedTab == "MARKS") {
                    MarksTabContent(viewModel, isDark, openWeb)
                } else if (selectedTab == "RESULTS") {
                    ResultsTabContent(viewModel, isDark, openWeb)
                } else if (selectedTab == "HALLS") {
                    HallsTabNative(viewModel, isDark, openWeb)
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Module '$selectedTab' is currently restricted. Please use the Web Dashboard.", color = textMain.copy(0.5f), fontSize = 12.sp)
                    }
                }
                
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ExamActionAccordion(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean = false,
    isLocked: Boolean = false,
    hasLockOnLeft: Boolean = false,
    hasArrowRight: Boolean = false,
    isDark: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasLockOnLeft) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Icon(icon, contentDescription = title, tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(
                    title,
                    color = if (isDark) Color.White else Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                if (isLocked && !hasLockOnLeft) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(12.dp))
                }
                Spacer(Modifier.weight(1f))
                
                if (hasArrowRight) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Open", tint = Color.Gray, modifier = Modifier.size(16.dp))
                } else {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (isExpanded) {
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    content()
                }
            }
        }
    }
}

data class SelectedExamSubject(
    val id: Int,
    val name: String,
    var date: String,
    var time: String,
    var endTime: String,
    var shift: String,
    var totalMarks: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleExamNativeForm(viewModel: com.example.wantuch.ui.viewmodel.WantuchViewModel) {
    val structureRes by viewModel.schoolStructure.collectAsState()
    val classes = structureRes?.classes ?: emptyList()
    
    val subjectsRes by viewModel.subjects.collectAsState()
    var subjects = subjectsRes?.subjects ?: emptyList()
    
    val instTitle = "RANA COLLEGE AND SCHOOL SYSTEM, KHWAZA KHELA SWAT"

    
    // UI Fallback: If DB hasn't synced GET_SUBJECTS, use visual test subjects
    if (subjects.isEmpty()) {
        subjects = listOf(
            com.example.wantuch.domain.model.SchoolSubject(1, "ASSEMBLY", "General"),
            com.example.wantuch.domain.model.SchoolSubject(2, "BIOLOGY", "General"),
            com.example.wantuch.domain.model.SchoolSubject(3, "CHEMISTRY", "General"),
            com.example.wantuch.domain.model.SchoolSubject(4, "COMPUTER SCIENCE", "General"),
            com.example.wantuch.domain.model.SchoolSubject(5, "ENGLISH", "General"),
            com.example.wantuch.domain.model.SchoolSubject(6, "GENERAL KNOWLEDGE", "General"),
            com.example.wantuch.domain.model.SchoolSubject(7, "GENERAL SCIENCE", "General"),
            com.example.wantuch.domain.model.SchoolSubject(8, "ISLAMIAT", "General"),
            com.example.wantuch.domain.model.SchoolSubject(9, "MATHEMATICS", "General"),
            com.example.wantuch.domain.model.SchoolSubject(10, "PAKISTAN STUDIES", "General"),
            com.example.wantuch.domain.model.SchoolSubject(11, "PHYSICS", "General"),
            com.example.wantuch.domain.model.SchoolSubject(12, "URDU", "General")
        )
    }

    val isDark by viewModel.isDarkTheme.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchSchoolStructure()
        viewModel.fetchSubjects()
    }
    
    var activeTab by remember { mutableStateOf("VIEW") }
    var examType by remember { mutableStateOf("Select Type") }
    var semester by remember { mutableStateOf("Select Semester") }
    var year by remember { mutableStateOf("Select Year") }
    var classId by remember { mutableStateOf<Int?>(null) }
    var sectionId by remember { mutableStateOf<Int?>(null) }
    var startDate by remember { mutableStateOf("Select Date") }
    var startTime by remember { mutableStateOf("Select Time") }
    var endTime by remember { mutableStateOf("Select Time") }
    var shift by remember { mutableStateOf("Select Shift") }
    var totalMarks by remember { mutableStateOf("") }
    
    val selectedSubjects = remember { mutableStateListOf<SelectedExamSubject>() }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { activeTab = "CREATE" },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == "CREATE") Color(0xFF3B82F6) else if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
                modifier = Modifier.weight(1f).height(45.dp),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = if (activeTab == "CREATE") Color.White else if(isDark) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create New Exam", color = if (activeTab == "CREATE") Color.White else if (isDark) Color.White else Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { activeTab = "VIEW" },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == "VIEW") Color(0xFF3B82F6) else if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
                modifier = Modifier.weight(1f).height(45.dp),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = if (activeTab == "VIEW") Color.White else if(isDark) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("View Exam List", color = if (activeTab == "VIEW") Color.White else if (isDark) Color.White else Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (activeTab == "CREATE") {
            val labelStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)

        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Exam Type", style = labelStyle)
                Spacer(Modifier.height(4.dp))
                DropdownSelector(
                    value = examType,
                    options = listOf("First Term", "Mid Term", "Final Term", "Monthly Test"),
                    isDark = isDark,
                    onValueChange = { examType = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Semester", style = labelStyle)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = if(isDark) Color.White else Color.Black, modifier = Modifier.size(10.dp))
                }
                Spacer(Modifier.height(4.dp))
                DropdownSelector(
                    value = semester,
                    options = listOf("First Semester", "Second Semester", "Third Semester", "Fourth Semester", "Fifth Semester", "Sixth Semester", "Seventh Semester", "Eighth Semester"),
                    isDark = isDark,
                    onValueChange = { semester = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(0.5f)) {
                Text("Year", style = labelStyle)
                Spacer(Modifier.height(4.dp))
                DropdownSelector(
                    value = year,
                    options = listOf("2024", "2025", "2026", "2027", "2028"),
                    isDark = isDark,
                    onValueChange = { year = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Class", style = labelStyle)
                Spacer(Modifier.height(4.dp))
                var expandedClass by remember { mutableStateOf(false) }
                val selectedClass = classes.find { it.id == classId }?.name ?: "Select Class"
                ExposedDropdownMenuBox(
                    expanded = expandedClass,
                    onExpandedChange = { expandedClass = !expandedClass }
                ) {
                    OutlinedTextField(
                        value = selectedClass,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClass) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = if(isDark) Color.White else Color.Black, fontSize = 12.sp)
                    )
                    ExposedDropdownMenu(expanded = expandedClass, onDismissRequest = { expandedClass = false }, modifier = Modifier.background(if(isDark) Color(0xFF1E293B) else Color.White)) {
                        classes.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name, color = if(isDark) Color.White else Color.Black) }, onClick = { classId = c.id; sectionId = null; expandedClass = false })
                        }
                    }
                }
            }
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Section", style = labelStyle)
                Spacer(Modifier.height(4.dp))
                var expandedSection by remember { mutableStateOf(false) }
                val currentClass = classes.find { it.id == classId }
                val availableSections = currentClass?.sections ?: emptyList()
                val selectedSection = availableSections.find { it.id == sectionId }?.name ?: "Select Section"
                ExposedDropdownMenuBox(expanded = expandedSection, onExpandedChange = { expandedSection = !expandedSection }) {
                    OutlinedTextField(
                        value = selectedSection, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedSection) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = if(isDark) Color.White else Color.Black, fontSize = 12.sp)
                    )
                    ExposedDropdownMenu(expanded = expandedSection, onDismissRequest = { expandedSection = false }, modifier = Modifier.background(if(isDark) Color(0xFF1E293B) else Color.White)) {
                        availableSections.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name, color = if(isDark) Color.White else Color.Black) }, onClick = { sectionId = s.id; expandedSection = false })
                        }
                    }
                }
            }
        }
        
        Text("Subject", style = labelStyle)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(1.dp, if(isDark) Color.White.copy(0.2f) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (subjects.isEmpty() || classId == null) {
                Text(
                    if (classId == null) "Please select a Class first to view subjects" else "No subjects available for this class",
                    color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    item {
                        val allSelected = subjects.isNotEmpty() && selectedSubjects.size == subjects.size
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            if (allSelected) {
                                selectedSubjects.clear()
                            } else {
                                selectedSubjects.clear()
                                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                val cal = java.util.Calendar.getInstance()
                                try {
                                    if (startDate.isNotBlank()) cal.time = format.parse(startDate) ?: java.util.Date()
                                } catch(e: Exception) {}
                                
                                subjects.forEachIndexed { idx, s ->
                                    val safeCal = cal.clone() as java.util.Calendar
                                    safeCal.add(java.util.Calendar.DAY_OF_YEAR, idx)
                                    selectedSubjects.add(SelectedExamSubject(
                                        id = s.id, name = s.name,
                                        date = format.format(safeCal.time),
                                        time = if (startTime.contains("Select")) "09:00" else startTime, 
                                        endTime = if (endTime.contains("Select")) "12:00" else endTime,
                                        shift = if (shift.contains("Select")) "Morning" else shift, 
                                        totalMarks = if (totalMarks.isBlank()) "100" else totalMarks
                                    ))
                                }
                            }
                        }) {
                            androidx.compose.material3.Checkbox(checked = allSelected, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text("All Subjects", color = if(isDark) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.Divider(color = Color(0xFFE2E8F0).copy(alpha = 0.5f))
                    }
                    items(subjects.size) { index ->
                    val s = subjects[index]
                    val isSelected = selectedSubjects.any { it.id == s.id }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                        if (isSelected) {
                            selectedSubjects.removeAll { it.id == s.id }
                        } else {
                            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            val cal = java.util.Calendar.getInstance()
                            try {
                                if (startDate.isNotBlank()) cal.time = format.parse(startDate) ?: java.util.Date()
                            } catch(e: Exception) {}
                            cal.add(java.util.Calendar.DAY_OF_YEAR, selectedSubjects.size)
                            selectedSubjects.add(SelectedExamSubject(
                                id = s.id, name = s.name,
                                date = format.format(cal.time),
                                time = if (startTime.contains("Select")) "09:00" else startTime, 
                                endTime = if (endTime.contains("Select")) "12:00" else endTime,
                                shift = if (shift.contains("Select")) "Morning" else shift, 
                                totalMarks = if (totalMarks.isBlank()) "100" else totalMarks
                            ))
                        }
                    }) {
                        androidx.compose.material3.Checkbox(checked = isSelected, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text(s.name, color = if(isDark) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (index < subjects.size - 1) {
                        androidx.compose.material3.Divider(color = Color(0xFFE2E8F0).copy(alpha = 0.5f))
                    }
                }
            }
        }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Column(Modifier.weight(1f)) {
                Text("Start Date (Auto-increments)", style = labelStyle.copy(fontSize = 10.sp))
                Spacer(Modifier.height(4.dp))
                Box(contentAlignment = Alignment.CenterStart) {
                    OutlinedTextField(
                        value = startDate, onValueChange = {}, readOnly = true, enabled = false,
                        trailingIcon = { Icon(Icons.Default.Event, "Calendar", tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            disabledTextColor = if(isDark) Color.White else Color.Black,
                            disabledBorderColor = if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f),
                            disabledTrailingIconColor = if(isDark) Color.White else Color.Black
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable {
                        val cal = java.util.Calendar.getInstance()
                        android.app.DatePickerDialog(context, { _, y, m, d ->
                            startDate = "$y-${String.format("%02d", m + 1)}-${String.format("%02d", d)}"
                        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
                    })
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Start Time (Def)", style = labelStyle.copy(fontSize = 10.sp))
                Spacer(Modifier.height(4.dp))
                DropdownSelector(
                    value = startTime,
                    options = listOf("08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30"),
                    isDark = isDark,
                    onValueChange = { startTime = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("End Time (Def)", style = labelStyle.copy(fontSize = 10.sp))
                Spacer(Modifier.height(4.dp))
                DropdownSelector(
                    value = endTime,
                    options = listOf("09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00"),
                    isDark = isDark,
                    onValueChange = { endTime = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Total Marks (Default)", style = labelStyle.copy(fontSize = 10.sp))
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = totalMarks, onValueChange = { totalMarks = it }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(color = if(isDark) Color.White else Color.Black, fontSize = 12.sp))
            }
        }

        Text("Shift (Default)", style = labelStyle.copy(fontSize = 10.sp))
        Spacer(Modifier.height(-12.dp))
        var expandedShift by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expandedShift, onExpandedChange = { expandedShift = !expandedShift }) {
            OutlinedTextField(
                value = shift, onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedShift) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = if(isDark) Color.White else Color.Black, fontSize = 12.sp)
            )
            ExposedDropdownMenu(expanded = expandedShift, onDismissRequest = { expandedShift = false }, modifier = Modifier.background(if(isDark) Color(0xFF1E293B) else Color.White)) {
                listOf("Morning", "Evening").forEach { s ->
                    DropdownMenuItem(text = { Text(s, color = if(isDark) Color.White else Color.Black) }, onClick = { shift = s; expandedShift = false })
                }
            }
        }
        
        if (selectedSubjects.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.List, contentDescription = null, tint = if(isDark) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Selected Subjects & Exam Dates", color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            
            // Render table-like horizontal scroll to match web app wide row design perfectly
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    // Table Header
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("SUBJECT NAME", style = labelStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), modifier = Modifier.width(140.dp))
                        Text("EXAM DATE", style = labelStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), modifier = Modifier.width(130.dp))
                        Text("START TIME", style = labelStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), modifier = Modifier.width(130.dp))
                        Text("END TIME", style = labelStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), modifier = Modifier.width(130.dp))
                        Text("SHIFT", style = labelStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), modifier = Modifier.width(135.dp))
                        Text("MARKS", style = labelStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), modifier = Modifier.width(120.dp))
                        Spacer(Modifier.width(32.dp))
                    }
                    
                    // Table Rows
                    selectedSubjects.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 4.dp).background(if(isDark) Color(0xFF1E293B) else Color.White, RoundedCornerShape(8.dp)).padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                        ) {
                            // Subject Column
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(140.dp)) {
                                Box(
                                    modifier = Modifier.size(22.dp).background(Color(0xFF3B82F6), androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(item.name, color = if(isDark) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            
                            // Exam Date
                            OutlinedTextField(
                                value = item.date, onValueChange = { selectedSubjects[index] = item.copy(date = it) },
                                modifier = Modifier.width(130.dp).height(48.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Bold),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent, 
                                    unfocusedBorderColor = if(isDark) Color.Gray.copy(0.3f) else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            
                            // Start Time
                            OutlinedTextField(
                                value = item.time, onValueChange = { selectedSubjects[index] = item.copy(time = it) },
                                modifier = Modifier.width(130.dp).height(48.dp),
                                trailingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = Color.Gray) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Bold),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent, 
                                    unfocusedBorderColor = if(isDark) Color.Gray.copy(0.3f) else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            
                            // End Time
                            OutlinedTextField(
                                value = item.endTime, onValueChange = { selectedSubjects[index] = item.copy(endTime = it) },
                                modifier = Modifier.width(130.dp).height(48.dp),
                                trailingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = Color.Gray) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Bold),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent, 
                                    unfocusedBorderColor = if(isDark) Color.Gray.copy(0.3f) else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            
                            // Shift
                            var rowShiftExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = rowShiftExpanded, onExpandedChange = { rowShiftExpanded = !rowShiftExpanded }) {
                                OutlinedTextField(
                                    value = item.shift, onValueChange = {}, readOnly = true,
                                    modifier = Modifier.width(135.dp).height(48.dp).menuAnchor(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rowShiftExpanded) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Bold),
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent, 
                                        unfocusedBorderColor = if(isDark) Color.Gray.copy(0.3f) else Color(0xFFE2E8F0)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                ExposedDropdownMenu(expanded = rowShiftExpanded, onDismissRequest = { rowShiftExpanded = false }, modifier = Modifier.background(if(isDark) Color(0xFF1E293B) else Color.White)) {
                                    listOf("Morning", "Evening").forEach { shiftOption ->
                                        DropdownMenuItem(text = { Text(shiftOption, color = if(isDark) Color.White else Color.Black) }, onClick = { 
                                            selectedSubjects[index] = item.copy(shift = shiftOption)
                                            rowShiftExpanded = false 
                                        })
                                    }
                                }
                            }
                            
                            // Marks
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(120.dp)) {
                                Text("Marks:", color = Color.Gray, fontSize = 10.sp)
                                Spacer(Modifier.width(4.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = item.totalMarks, onValueChange = { selectedSubjects[index] = item.copy(totalMarks = it) },
                                    modifier = Modifier.weight(1f).height(40.dp).border(1.dp, if(isDark) Color.Gray.copy(0.3f) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp)).padding(horizontal = 4.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.Center) { innerTextField() }
                                    }
                                )
                            }
                            
                            // Trash Icon
                            androidx.compose.material3.IconButton(
                                onClick = { selectedSubjects.removeAt(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = if(isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
        
        statusMessage?.let {
            Text(it, color = if (it.contains("successfully", true)) Color(0xFF059669) else Color.Red, fontSize = 12.sp)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (classId != null && sectionId != null && selectedSubjects.isNotEmpty()) {
                        val payloadArr = selectedSubjects.map {
                            "{\"class_id\":$classId,\"section_id\":$sectionId,\"subject_id\":${it.id},\"date\":\"${it.date}\",\"time\":\"${it.time}\",\"end_time\":\"${it.endTime}\",\"shift\":\"${it.shift}\",\"total\":\"${it.totalMarks}\"}"
                        }
                        val payload = payloadArr.joinToString(",", "[", "]")

                        viewModel.createExam(
                            type = examType,
                            semester = semester,
                            year = year,
                            classId = classId!!,
                            sectionId = sectionId!!,
                            subjectsJson = payload,
                            onSuccess = { 
                                statusMessage = it
                                selectedSubjects.clear()
                            },
                            onError = { statusMessage = it }
                        )
                    } else {
                        statusMessage = "Please select Class, Section, and at least 1 Subject"
                    }
                },
                modifier = Modifier.weight(1f).height(45.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCFF8E3))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("SAVE EXAM", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
            }
            
            val printContext = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = { 
                    if (selectedSubjects.isNotEmpty() && classId != null) {
                        activeTab = "DATASHEET_PREVIEW"
                    } else {
                        statusMessage = "Select Class and Subjects to Preview!"
                    }
                },
                modifier = Modifier.weight(1f).height(45.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3F2FE))
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("PREVIEW DATE SHEET", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
            }
        }
    } else if (activeTab == "DATASHEET_PREVIEW") {
        val printContext = androidx.compose.ui.platform.LocalContext.current
        
        DatasheetPreview(
            isDark = isDark,
            instTitle = instTitle,
            classes = classes,
            initialExamType = examType,
            initialYear = year,
            initialSemester = semester,
            initialClassId = classId,
            initialSectionId = sectionId,
            initialShift = shift,
            subjects = selectedSubjects,
            onBack = { activeTab = "CREATE" },
            onPrintClick = { currentEType, currentYear, currentCName, currentSName ->
                printDatasheetToPdf(printContext, selectedSubjects, currentEType, currentYear, currentCName, currentSName, instTitle)
            }
        )
    } else {
        // VIEW TAB
        val examGroups by viewModel.examGroups.collectAsState()
        val examGroupsL2 by viewModel.examGroupsL2.collectAsState()
        var selectedL1Item by remember { mutableStateOf<com.example.wantuch.domain.model.ExamL1Item?>(null) }
        var showConfirmDeleteL1 by remember { mutableStateOf<com.example.wantuch.domain.model.ExamL1Item?>(null) }
        var showConfirmDeleteL2 by remember { mutableStateOf<com.example.wantuch.domain.model.ExamL2Item?>(null) }

        val context = androidx.compose.ui.platform.LocalContext.current
        
        LaunchedEffect(Unit) {
            viewModel.fetchExamGroups { msg ->
                if (!msg.contains("Refreshed")) {
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { 
                viewModel.fetchExamGroups { msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color(0xFF1E293B) else Color(0xFF285296)),
            modifier = Modifier.fillMaxWidth().height(40.dp).border(1.dp, Color(0xFF0EA5E9), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("REFRESH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f), RoundedCornerShape(8.dp)).background(if(isDark) Color(0xFF1E293B) else Color.White, RoundedCornerShape(8.dp))
        ) {
            // Header Row
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF0F172A), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("EXAM TYPE", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.5f))
                Text("ACADEMIC YEAR", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("TOTAL EXAMS", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("ACTIONS", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            
            if (examGroups.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No scheduled exams found.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                examGroups.forEachIndexed { idx, group ->
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if(idx % 2 == 0) Color.Transparent else if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.02f))
                            .clickable {
                                selectedL1Item = group
                                viewModel.fetchExamGroupsL2(group.exam_type, group.semester ?: "", group.academic_year)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${group.exam_type} " + if (group.semester.isNullOrBlank()) "" else "(${group.semester})", 
                            color = Color(0xFFFACC15), fontSize = 12.sp, modifier = Modifier.weight(1.5f)
                        )
                        Text(group.academic_year, color = if(isDark) Color.White else Color.Black, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                            Box(Modifier.background(Color(0xFF3B82F6), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("${group.exam_count}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                            androidx.compose.material3.IconButton(
                                onClick = { showConfirmDeleteL1 = group },
                                modifier = Modifier.size(32.dp).background(Color(0xFF0F172A), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF0EA5E9), RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    if (idx < examGroups.size - 1) {
                        androidx.compose.material3.Divider(color = if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f))
                    }
                }
            } // closes else for empty check
        } // closes Column for View Table

        if (selectedL1Item != null) {
            val group = selectedL1Item!!
            val modalTitle = "${group.exam_type} " + (if (group.semester.isNullOrBlank()) "" else "(${group.semester}) ") + "- Classes"
            androidx.compose.ui.window.Dialog(onDismissRequest = { 
                selectedL1Item = null 
                viewModel.clearExamGroupsL2()
            }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if(isDark) Color(0xFF1E293B) else Color.White,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(modalTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if(isDark) Color.White else Color.Black)
                            androidx.compose.material3.IconButton(
                                onClick = { 
                                    selectedL1Item = null 
                                    viewModel.clearExamGroupsL2()
                                }, 
                                modifier = Modifier.size(24.dp).background(Color(0xFFEF4444).copy(0.1f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth().border(1.dp, if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f), RoundedCornerShape(8.dp)).background(if(isDark) Color(0xFF1E293B) else Color.White, RoundedCornerShape(8.dp))
                        ) {
                            // Header Row
                            Row(
                                Modifier.fillMaxWidth().background(Color(0xFF0F172A), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CLASS", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.2f))
                                Text("SECTION", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                                Text("SUBJECTS", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                                Text("ACTIONS", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }

                            if (examGroupsL2.isEmpty()) {
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            } else {
                                examGroupsL2.forEachIndexed { idx, l2Item ->
                                    Row(
                                        Modifier.fillMaxWidth().background(if(idx % 2 == 0) Color.Transparent else if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.02f)).padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(l2Item.class_name, color = Color(0xFF22C55E), fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                                        Text(l2Item.section_name, color = if(isDark) Color.White else Color.Black, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                        Text("${l2Item.subject_count}", color = if(isDark) Color.White else Color.Black, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                                            androidx.compose.material3.IconButton(
                                                onClick = { showConfirmDeleteL2 = l2Item },
                                                modifier = Modifier.size(28.dp).background(Color(0xFF0F172A), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF0EA5E9), RoundedCornerShape(8.dp))
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                    if (idx < examGroupsL2.size - 1) {
                                        androidx.compose.material3.Divider(color = if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f))
                                    }
                                } // closes foreach
                            } // closes else
                        } // closes l2 table column
                    } // closes dialog inner column
                } // closes dialog surface
            } // closes Dialog
        } // closes if (selectedL1Item)

        if (showConfirmDeleteL1 != null) {
            val group = showConfirmDeleteL1!!
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showConfirmDeleteL1 = null },
                title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if(isDark) Color.White else Color.Black) },
                text = { Text("Are you sure you want to delete all exams for '${group.exam_type} ${group.academic_year}'?", fontSize = 14.sp, color = if(isDark) Color.LightGray else Color.DarkGray) },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.deleteExamGroup(
                                type = group.exam_type,
                                semester = group.semester ?: "",
                                year = group.academic_year,
                                onSuccess = { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() },
                                onError = { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() }
                            )
                            showConfirmDeleteL1 = null
                        }
                    ) { Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showConfirmDeleteL1 = null }) { Text("Cancel", color = if(isDark) Color.White else Color.Black) }
                },
                containerColor = if(isDark) Color(0xFF1E293B) else Color.White
            )
        }

        if (showConfirmDeleteL2 != null && selectedL1Item != null) {
            val l2Item = showConfirmDeleteL2!!
            val parentGroup = selectedL1Item!!
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showConfirmDeleteL2 = null },
                title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if(isDark) Color.White else Color.Black) },
                text = { Text("Are you sure you want to delete exams for '${l2Item.class_name} ${l2Item.section_name}'?", fontSize = 14.sp, color = if(isDark) Color.LightGray else Color.DarkGray) },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.deleteExamGroupL2(
                                type = parentGroup.exam_type,
                                semester = parentGroup.semester ?: "",
                                year = parentGroup.academic_year,
                                classId = l2Item.class_id,
                                sectionId = l2Item.section_id,
                                onSuccess = { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() },
                                onError = { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() }
                            )
                            showConfirmDeleteL2 = null
                        }
                    ) { Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showConfirmDeleteL2 = null }) { Text("Cancel", color = if(isDark) Color.White else Color.Black) }
                },
                containerColor = if(isDark) Color(0xFF1E293B) else Color.White
            )
        }
    } // closes else (VIEW TAB)
} // closes Main Column
} // closes ScheduleExamNativeForm

@Composable
fun QuickDatasheetNativeForm(viewModel: com.example.wantuch.ui.viewmodel.WantuchViewModel) {
    var examType by remember { mutableStateOf("First Term") }
    var year by remember { mutableStateOf("2026") }
    var csvText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val isDark by viewModel.isDarkTheme.collectAsState()
    val labelStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Exam Type", style = labelStyle)
                DropdownSelector(
                    value = examType,
                    options = listOf("First Term", "Mid Term", "Final Term", "Monthly Test"),
                    isDark = isDark,
                    onValueChange = { examType = it },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }
            Column(Modifier.weight(0.5f)) {
                Text("Year", style = labelStyle)
                DropdownSelector(
                    value = year,
                    options = listOf("2024", "2025", "2026", "2027", "2028"),
                    isDark = isDark,
                    onValueChange = { year = it },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }
        }
        
        Column(Modifier.fillMaxWidth()) {
            Text("Format: Class Date(YYYY-MM-DD) Time Shift Subject (Comma separate entries)", style = labelStyle.copy(fontSize = 10.sp))
            OutlinedTextField(
                value = csvText, onValueChange = { csvText = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Example: 10th 2026-05-20 09:00 Morning Math, 10th 2026-05-21 09:00 Morning English", fontSize = 10.sp) },
                maxLines = 6
            )
        }

        statusMessage?.let {
            Text(it, color = if (it.contains("successfully") || it.contains("Processed")) Color(0xFF059669) else Color.Red, fontSize = 12.sp)
        }

        Button(
            onClick = {
                if (csvText.isNotBlank()) {
                    viewModel.quickScheduleExam(examType, year, csvText, 
                        onSuccess = { statusMessage = it; csvText = "" }, 
                        onError = { statusMessage = it }
                    )
                } else {
                    statusMessage = "Please enter data in the requested format."
                }
            },
            modifier = Modifier.fillMaxWidth().height(45.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("DEPLOY QUICK DATASHEET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasheetPreview(
    isDark: Boolean,
    instTitle: String,
    classes: List<com.example.wantuch.domain.model.SchoolClass>,
    initialExamType: String,
    initialYear: String,
    initialSemester: String,
    initialClassId: Int?,
    initialSectionId: Int?,
    initialShift: String,
    subjects: List<SelectedExamSubject>,
    onBack: () -> Unit,
    onPrintClick: (String, String, String, String) -> Unit
) {
    val bg = if (isDark) Color(0xFF1E293B) else Color.White
    val text = if (isDark) Color.White else Color.Black

    var examType by remember { mutableStateOf(initialExamType) }
    var year by remember { mutableStateOf(initialYear) }
    var semester by remember { mutableStateOf(initialSemester) }
    var classId by remember { mutableStateOf(initialClassId) }
    var sectionId by remember { mutableStateOf(initialSectionId) }
    var shift by remember { mutableStateOf(initialShift) }

    val currentClass = classes.find { it.id == classId }
    val className = currentClass?.name ?: "Unknown Class"
    val availableSections = currentClass?.sections ?: emptyList()
    val sectionName = availableSections.find { it.id == sectionId }?.name ?: "Unknown Section"

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        // Top Filter Bar mimicking screenshot via fully dynamic Dropdown selection boxes
        Row(modifier = Modifier.fillMaxWidth().background(bg, RoundedCornerShape(8.dp)).padding(10.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            val labelStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

            Column { Text("EXAM TYPE", style = labelStyle); Spacer(Modifier.height(4.dp)); DropdownSelector(value = examType, options = listOf("First Term", "Mid Term", "Final Term", "Monthly Test"), isDark = isDark, onValueChange = { examType = it }, modifier = Modifier.width(130.dp).height(48.dp)) }
            Spacer(Modifier.width(12.dp))
            Column { Text("ACADEMIC YEAR", style = labelStyle); Spacer(Modifier.height(4.dp)); DropdownSelector(value = year, options = listOf("2024", "2025", "2026", "2027", "2028"), isDark = isDark, onValueChange = { year = it }, modifier = Modifier.width(100.dp).height(48.dp)) }
            Spacer(Modifier.width(12.dp))
            Column { Text("SEMESTER", style = labelStyle); Spacer(Modifier.height(4.dp)); DropdownSelector(value = semester, options = listOf("First Semester", "Second Semester", "Third Semester", "Fourth Semester"), isDark = isDark, onValueChange = { semester = it }, modifier = Modifier.width(150.dp).height(48.dp)) }
            Spacer(Modifier.width(12.dp))
            
            // Interactive Class Dropdown
            Column {
                Text("CLASS", style = labelStyle); Spacer(Modifier.height(4.dp))
                var expandedClass by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedClass, onExpandedChange = { expandedClass = !expandedClass }) {
                    OutlinedTextField(
                        value = className, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClass) },
                        modifier = Modifier.menuAnchor().width(130.dp).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = text, fontSize = 11.sp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent, 
                            unfocusedBorderColor = if(isDark) Color.Gray.copy(0.3f) else Color.LightGray.copy(0.3f)
                        )
                    )
                    ExposedDropdownMenu(expanded = expandedClass, onDismissRequest = { expandedClass = false }, modifier = Modifier.background(bg)) {
                        classes.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name, color = text) }, onClick = { classId = c.id; sectionId = null; expandedClass = false })
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            
            // Interactive Section Dropdown
            Column {
                Text("SECTION", style = labelStyle); Spacer(Modifier.height(4.dp))
                var expandedSection by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedSection, onExpandedChange = { expandedSection = !expandedSection }) {
                    OutlinedTextField(
                        value = sectionName, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedSection) },
                        modifier = Modifier.menuAnchor().width(120.dp).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = text, fontSize = 11.sp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent, 
                            unfocusedBorderColor = if(isDark) Color.Gray.copy(0.3f) else Color.LightGray.copy(0.3f)
                        )
                    )
                    ExposedDropdownMenu(expanded = expandedSection, onDismissRequest = { expandedSection = false }, modifier = Modifier.background(bg)) {
                        availableSections.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name, color = text) }, onClick = { sectionId = s.id; expandedSection = false })
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            
            Column { Text("SHIFT", style = labelStyle); Spacer(Modifier.height(4.dp)); DropdownSelector(value = shift, options = listOf("Morning", "Evening"), isDark = isDark, onValueChange = { shift = it }, modifier = Modifier.width(120.dp).height(48.dp)) }
            
            Spacer(Modifier.width(24.dp))
            
            Button(onClick = { onPrintClick(examType, year, className, sectionName) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF285296)), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("PRINT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("RESET", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Document Canvas
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth().background(Color.White),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                // Blue Banner perfectly recreating slanted web styling using Canvas Path overrides
                Box(Modifier.fillMaxWidth().height(100.dp)) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width * 0.60f, 0f)
                            lineTo(size.width * 0.45f, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, color = Color(0xFF285296))
                        
                        val path2 = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.90f, size.height)
                            lineTo(size.width, size.height * 0.70f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(path2, color = Color(0xFF285296))
                    }
                    
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(0.45f)) {
                            Text("DATE SHEET", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("${examType.uppercase()} $year", color = Color.White.copy(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        // Inst Title
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(0.55f).padding(start = 8.dp)) {
                            Text(instTitle, color = Color(0xFF285296), fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.End, lineHeight = 13.sp)
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Class Name Title Component
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(4.dp).height(20.dp).background(Color(0xFFFACC15)))
                    Spacer(Modifier.width(8.dp))
                    Text("$className $sectionName".uppercase(), color = Color(0xFF285296), fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Content Table Data enclosed in dedicated Row to prevent fillMaxWidth cutoff
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Column(Modifier.width(550.dp)) {
                        // Header Frame
                        Row(Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).border(0.5.dp, Color(0xFFE2E8F0)).padding(vertical = 12.dp, horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DATE & DAY", color = Color(0xFF285296), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(120.dp))
                            Text("SUBJECT", color = Color(0xFF285296), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(150.dp))
                            Text("TIME", color = Color(0xFF285296), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(100.dp))
                            Text("SHIFT", color = Color(0xFF285296), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                        }
                        
                        // Native UI Row Generation
                        subjects.forEachIndexed { index, s ->
                            Row(Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFE2E8F0)).padding(vertical = 12.dp, horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.width(120.dp)) {
                                    Text(formatDatasheetDate(s.date), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(getDayOfWeek(s.date), color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(s.name, color = Color(0xFF285296), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp))
                                Text(s.time + " AM", color = Color.Black, fontSize = 11.sp, modifier = Modifier.width(100.dp))
                                
                                Box(modifier = Modifier.width(80.dp)) {
                                    Box(modifier = Modifier.background(Color(0xFFFFF7ED), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(s.shift, color = Color(0xFFEA580C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(48.dp))
                
                // Formal Signatures Output
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.width(120.dp).height(1.dp).background(Color.Black))
                        Spacer(Modifier.height(8.dp))
                        Text("CONTROLLER OF EXAMINATIONS", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.width(100.dp).height(1.dp).background(Color.Black))
                        Spacer(Modifier.height(8.dp))
                        Text("PRINCIPAL", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}



fun formatDatasheetDate(dateStr: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = sdf.parse(dateStr)
        val outSdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        outSdf.format(date!!)
    } catch (e: Exception) {
        dateStr
    }
}

fun getDayOfWeek(dateStr: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = sdf.parse(dateStr)
        val outSdf = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
        outSdf.format(date!!).uppercase()
    } catch (e: Exception) {
        "MONDAY"
    }
}

fun printDatasheetToPdf(
    context: android.content.Context, 
    subjects: List<SelectedExamSubject>, 
    examType: String, 
    year: String, 
    className: String, 
    sectionName: String, 
    instTitle: String
) {
    val webView = android.webkit.WebView(context)
    val html = """
        <html>
        <head>
            <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; color: #333; }
                .header-container { display: flex; justify-content: space-between; align-items: stretch; border-bottom: 2px solid #e0e0e0; padding-bottom: 15px; margin-bottom: 25px; }
                .header-left { background-color: #2b559e; color: white; padding: 25px 30px; width: 40%; clip-path: polygon(0 0, 100% 0, 85% 100%, 0% 100%); }
                .header-left h1 { margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 1px; }
                .header-left p { margin: 5px 0 0 0; font-size: 12px; text-transform: uppercase; font-weight: 600; }
                .header-right { width: 55%; text-align: right; display: flex; flex-direction: column; justify-content: center; padding-right: 10px; }
                .header-right h2 { margin: 0; font-size: 16px; font-weight: 800; color: #2b559e; text-transform: uppercase; }
                .class-title { font-size: 18px; font-weight: 800; color: #2b559e; margin-bottom: 20px; text-transform: uppercase; display: flex; align-items: center; }
                .class-title::before { content: ''; display: inline-block; width: 4px; height: 20px; background-color: #f1c40f; margin-right: 10px; }
                table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 11px; }
                th { color: #2b559e; font-weight: 800; text-align: left; padding: 12px 10px; border-bottom: 2px solid #e0e0e0; border-top: 1px solid #e0e0e0; }
                td { padding: 12px 10px; border-bottom: 1px solid #f0f0f0; color: #555; font-weight: 700; }
                .date-col strong { color: #333; display: block; }
                .date-col span { font-size: 9px; color: #888; text-transform: uppercase; }
                .subject-name { color: #2b559e; font-weight: 800; text-transform: uppercase; }
                .shift-text { color: #e74c3c; font-weight: 800; }
                .signatures { display: flex; justify-content: space-between; margin-top: 80px; padding: 0 40px; }
                .sig-box { text-align: center; font-size: 10px; font-weight: 800; color: #333; }
                .sig-line { width: 220px; border-top: 2px solid #333; margin-bottom: 5px; }
            </style>
        </head>
        <body>
            <div class="header-container">
                <div class="header-left">
                    <h1>DATE SHEET</h1>
                    <p>${examType.uppercase()} $year</p>
                </div>
                <div class="header-right">
                    <h2>$instTitle</h2>
                </div>
            </div>
            
            <div class="class-title">$className $sectionName</div>
            
            <table>
                <tr>
                    <th>DATE & DAY</th>
                    <th>SUBJECT</th>
                    <th>START TIME</th>
                    <th>END TIME</th>
                    <th>SHIFT</th>
                </tr>
                ${subjects.map { s -> 
                    val formattedDate = formatDatasheetDate(s.date)
                    val dayOfWeek = getDayOfWeek(s.date)
                    val endT = if (s.endTime.isNullOrBlank()) "--- : ---" else s.endTime
                    """
                    <tr>
                        <td class="date-col"><strong>$formattedDate</strong><span>$dayOfWeek</span></td>
                        <td class="subject-name">${s.name}</td>
                        <td>${s.time} AM</td>
                        <td>$endT</td>
                        <td class="shift-text">${s.shift}</td>
                    </tr>
                    """ 
                }.joinToString("")}
            </table>
            
            <div class="signatures">
                <div class="sig-box">
                    <div class="sig-line"></div>
                    CONTROLLER OF EXAMINATIONS
                </div>
                <div class="sig-box">
                    <div class="sig-line"></div>
                    PRINCIPAL
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()

    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView, url: String) {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val printAdapter = view.createPrintDocumentAdapter("Datasheet_$className")
            printManager.print("Datasheet_$className", printAdapter, android.print.PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
}


@Composable
fun GenerateSlipsNativeForm(
    viewModel: com.example.wantuch.ui.viewmodel.WantuchViewModel,
    isDark: Boolean,
    openWeb: (String) -> Unit
) {
    val examGroups by viewModel.examGroups.collectAsState()
    val schoolStructure by viewModel.schoolStructure.collectAsState()
    val classes = schoolStructure?.classes ?: emptyList()
    val context = androidx.compose.ui.platform.LocalContext.current

    val examTypes = remember(examGroups) {
        listOf("Select Exam") + examGroups.map { it.exam_type }.distinct()
    }

    val classOptions = remember(classes) {
        listOf("Select Class") + classes.map { it.name }
    }
    val classMap = remember(classes) {
        classes.associate { it.name to it.id }
    }
    
    val instTitle = "RANA COLLEGE AND SCHOOL SYSTEM"

    var selectedType by remember { mutableStateOf("Select Exam") }
    var selectedClassName by remember { mutableStateOf("Select Class") }

    val hiddenSlips = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val showDeleteDialogFor = remember { mutableStateOf<String?>(null) }
    val editedNames = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    val editedRollNos = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    val editedClasses = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    val editingModes = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DropdownSelector(
            value = selectedType,
            options = examTypes,
            isDark = isDark
        ) { newValue ->
            selectedType = newValue
            viewModel.clearRollNoSlips()
        }

        DropdownSelector(
            value = selectedClassName,
            options = classOptions,
            isDark = isDark
        ) { newValue ->
            selectedClassName = newValue
            viewModel.clearRollNoSlips()
        }

        val rollNoSlips by viewModel.rollNoSlips.collectAsState()

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Button(
                onClick = {
                    if (selectedType != "Select Exam" && selectedClassName != "Select Class") {
                        val classId = classMap[selectedClassName] ?: 0
                        viewModel.fetchRollNoSlips(selectedType, classId) {
                            hiddenSlips.clear()
                            editedNames.clear()
                            editedRollNos.clear()
                            editedClasses.clear()
                            editingModes.clear()
                            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Please select Exam Type & Class", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f).height(45.dp),
                shape = RoundedCornerShape(8.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFCFFFE5))
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("GENERATE SLIPS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            
            if (rollNoSlips != null && rollNoSlips?.students?.isNotEmpty() == true) {
                androidx.compose.material3.Button(
                    onClick = { 
                        val printedSlips = rollNoSlips!!.students?.filter { !hiddenSlips.contains(it.id) }?.map { 
                            it.copy(
                                full_name = editedNames[it.id] ?: it.full_name,
                                username = editedRollNos[it.id] ?: it.username,
                                cname = editedClasses[it.id] ?: "${it.cname} - ${it.sname}",
                                sname = ""
                            )
                        } ?: emptyList()
                        val res = com.example.wantuch.domain.model.RollNoSlipResponse(
                            status = "success", schedule = rollNoSlips!!.schedule, students = printedSlips
                        )
                        printRollNoSlipsToPdf(context, res, selectedType, instTitle) 
                    },
                    modifier = Modifier.height(45.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PRINT SLIPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        if (rollNoSlips == null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.02f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "PENDING GENERATION",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Please select examination Type & Class to load slips",
                        color = if(isDark) Color.White.copy(alpha=0.5f) else Color.Black.copy(alpha=0.5f),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            val slips = rollNoSlips!!
            if (slips.status == "error") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(slips.message ?: "Failed to generate slips.", color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            } else if (slips.students?.isNotEmpty() != true) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No students or schedule found for this class.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Text("Showing ${slips.students?.size ?: 0} students", color = if(isDark) Color.LightGray else Color.DarkGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                
                if (showDeleteDialogFor.value != null) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialogFor.value = null },
                        title = { Text("Hide Roll No Slip", fontWeight = FontWeight.Bold) },
                        text = { Text("Are you sure you want to remove this slip from the current view? This will refrain it from printing.") },
                        confirmButton = {
                            TextButton(onClick = { 
                                hiddenSlips.add(showDeleteDialogFor.value!!)
                                showDeleteDialogFor.value = null 
                            }) { Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialogFor.value = null }) { Text("Cancel", color = Color.Gray) }
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    slips.students?.forEach { stu ->
                        if (hiddenSlips.contains(stu.id)) return@forEach
                        
                        val isEditing = editingModes[stu.id] == true
                        val currentName = editedNames[stu.id] ?: stu.full_name
                        val currentRollNo = editedRollNos[stu.id] ?: stu.username
                        val currentClass = editedClasses[stu.id] ?: "${stu.cname} - ${stu.sname}"
                        if (stu.has_dues) {
                            // ─── Dues Warning Card ───
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .background(Color(0xFFB91C1C), RoundedCornerShape(8.dp))
                                            .padding(vertical = 8.dp, horizontal = 12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("DUES PENDING - SLIP BLOCKED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(stu.full_name, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Roll No: ${stu.username}", color = Color(0xFF991B1B), fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Balance Due", color = Color(0xFF991B1B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text("Rs. ${String.format("%.0f", stu.balance)}", color = Color(0xFFDC2626), fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        } else {
                            // ─── Roll No Slip Card (Web App Design) ───
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 4.dp,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    // ═══ Action Buttons ═══
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val printedStu = stu.copy(
                                                    full_name = currentName,
                                                    username = currentRollNo,
                                                    cname = currentClass,
                                                    sname = ""
                                                )
                                                val singleStudentResponse = com.example.wantuch.domain.model.RollNoSlipResponse(
                                                    status = "success", schedule = rollNoSlips!!.schedule, students = listOf(printedStu)
                                                )
                                                printRollNoSlipsToPdf(context, singleStudentResponse, selectedType, instTitle)
                                            },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                editingModes[stu.id] = !(editingModes[stu.id] ?: false)
                                                if (editingModes[stu.id] == true) {
                                                    if (!editedNames.containsKey(stu.id)) editedNames[stu.id] = stu.full_name
                                                    if (!editedRollNos.containsKey(stu.id)) editedRollNos[stu.id] = stu.username
                                                    if (!editedClasses.containsKey(stu.id)) editedClasses[stu.id] = "${stu.cname} - ${stu.sname}"
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFF7ED), contentColor = Color(0xFF334155)),
                                            border = BorderStroke(1.5.dp, Color(0xFF06B6D4)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                        }
                                        Button(
                                            onClick = { showDeleteDialogFor.value = stu.id },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    // ═══ Header Title  ═══
                                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("RANA COLLEGE AND", color = Color(0xFFB91C1C), fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.5.sp)
                                        Text("SCHOOL SYSTEM, KHWAZA", color = Color(0xFFB91C1C), fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.5.sp)
                                        Text("KHELA SWAT", color = Color(0xFFB91C1C), fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.5.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text("$selectedType – Examination Pass", color = Color(0xFF475569), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(14.dp))
                                        Divider(color = Color(0xFF334155), thickness = 2.dp, modifier = Modifier.fillMaxWidth())
                                    }

                                    // ═══ Student Info ═══
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text("STUDENT NAME", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                            if (isEditing) {
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = currentName,
                                                    onValueChange = { editedNames[stu.id] = it },
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B)),
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp)).padding(4.dp)
                                                )
                                            } else {
                                                Text(currentName, color = Color(0xFF1E293B), fontSize = 15.sp, fontWeight = FontWeight.Black)
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Row {
                                                Column(Modifier.weight(1f)) {
                                                    Text("ROLL NO", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    if (isEditing) {
                                                        androidx.compose.foundation.text.BasicTextField(
                                                            value = currentRollNo,
                                                            onValueChange = { editedRollNos[stu.id] = it },
                                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155)),
                                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp, end = 8.dp).border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp)).padding(4.dp)
                                                        )
                                                    } else {
                                                        Text(currentRollNo, color = Color(0xFF334155), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Column(Modifier.weight(1.2f)) {
                                                    Text("CLASS", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    if (isEditing) {
                                                        androidx.compose.foundation.text.BasicTextField(
                                                            value = currentClass,
                                                            onValueChange = { editedClasses[stu.id] = it },
                                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155)),
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp)).padding(4.dp)
                                                        )
                                                    } else {
                                                        Text(currentClass, color = Color(0xFF334155), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        val picUrl = if (stu.profile_pic.isNullOrEmpty() || stu.profile_pic == "null") null else if (stu.profile_pic.startsWith("http")) stu.profile_pic else "${com.example.wantuch.BASE_URL}assets/uploads/${stu.profile_pic}"
                                        if (picUrl != null) {
                                            coil.compose.SubcomposeAsyncImage(
                                                model = picUrl,
                                                contentDescription = "Profile Photo",
                                                modifier = Modifier.size(60.dp, 80.dp)
                                                    .border(2.dp, Color(0xFF475569)),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                loading = {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Gray, strokeWidth = 2.dp)
                                                    }
                                                },
                                                error = {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize()
                                                            .background(Color(0xFFF1F5F9)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(currentName.take(2).uppercase(), color = Color(0xFF64748B), fontWeight = FontWeight.Black, fontSize = 16.sp)
                                                    }
                                                }
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.size(60.dp, 80.dp)
                                                    .background(Color(0xFFF1F5F9))
                                                    .border(2.dp, Color(0xFF475569)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(currentName.take(2).uppercase(), color = Color(0xFF64748B), fontWeight = FontWeight.Black, fontSize = 16.sp)
                                            }
                                        }
                                    }

                                    // ═══ Schedule Table ═══
                                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                                        // Table Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .background(Color(0xFFE0F2FE), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .padding(vertical = 12.dp, horizontal = 8.dp)
                                        ) {
                                            Text("DATE & \nDAY", Modifier.weight(1.3f), color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            Text("START \nTIME", Modifier.weight(1.1f), color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            Text("END \nTIME", Modifier.weight(1.1f), color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            Text("SUBJECT", Modifier.weight(1.5f), color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        }
                                        // Table Rows
                                        slips.schedule?.forEachIndexed { index, s ->
                                            val formatIn = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                            val dateParsed = try { formatIn.parse(s.exam_date) } catch(ex: Exception) { null }
                                            val dayFmt = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
                                            val dateFmt = java.text.SimpleDateFormat("dd-MMM-\nyyyy", java.util.Locale.getDefault())
                                            val dayStr = dateParsed?.let { dayFmt.format(it) } ?: ""
                                            val dateStr = dateParsed?.let { dateFmt.format(it) } ?: s.exam_date
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .background(Color.White)
                                                    .border(0.5.dp, Color(0xFFE2E8F0))
                                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(dayStr, color = Color(0xFFDC2626), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                                    Text(dateStr, color = Color(0xFF475569), fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                }
                                                val sTimeParts = s.start_time.split(":")
                                                val sTimeFormat = if (sTimeParts.size >= 2) "${sTimeParts[0]}:${sTimeParts[1]}\nAM" else s.start_time
                                                Text(sTimeFormat, Modifier.weight(1.1f), color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                
                                                val eTime = if (s.end_time.isNullOrBlank() || s.end_time == "00:00:00") "N/A" else {
                                                    val eTimeParts = s.end_time.split(":")
                                                    if (eTimeParts.size >= 2) "${eTimeParts[0]}:${eTimeParts[1]}\nAM" else s.end_time
                                                }
                                                Text(eTime, Modifier.weight(1.1f), color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                Text(s.sub_name.uppercase(), Modifier.weight(1.5f), color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // ═══ Signature Area ═══
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(Modifier.width(80.dp).height(1.dp).background(Color(0xFF334155)))
                                            Spacer(Modifier.height(4.dp))
                                            Text("Exam Cell", color = Color(0xFF334155), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(Modifier.width(80.dp).height(1.dp).background(Color(0xFF334155)))
                                            Spacer(Modifier.height(4.dp))
                                            Text("Principal", color = Color(0xFF334155), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun printRollNoSlipsToPdf(
    context: android.content.Context,
    rollNoSlips: com.example.wantuch.domain.model.RollNoSlipResponse,
    examType: String,
    instTitle: String
) {
    if (rollNoSlips.students.isNullOrEmpty()) return
    
    val webView = android.webkit.WebView(context)
    val slipsHtml = rollNoSlips.students?.filter { !it.has_dues }?.map { stu ->
        """
        <div class="slip-card">
            <div class="header">
                <div class="school-title">$instTitle</div>
                <div class="exam-title">$examType - Examination Pass</div>
            </div>
            <div class="info-container">
                <div class="info-left">
                    <div>
                        <div class="student-name">
                            <span class="label">Student Name</span>
                            <strong>${stu.full_name}</strong>
                        </div>
                        <div class="student-roll">
                            <span class="label">Roll No</span>
                            <strong>${stu.username}</strong>
                        </div>
                    </div>
                    <div class="student-class">
                        <span class="label">Class</span>
                        <strong>${stu.cname} - ${stu.sname}</strong>
                    </div>
                </div>
                <div class="photo-box">
                    <img src="${stu.profile_pic ?: ""}" onerror="this.src='https://via.placeholder.com/60x80?text=Pic'" />
                </div>
            </div>
            <table>
                <tr><th>Date & Day</th><th>Start Time</th><th>End Time</th><th>Subject</th></tr>
                ${rollNoSlips.schedule?.joinToString("") { s ->
                    val formatIn = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val dateParsed = try { formatIn.parse(s.exam_date) } catch(e: Exception) { null }
                    val formatOutDay = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
                    val formatOutDate = java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.getDefault())
                    val dayStr = dateParsed?.let { formatOutDay.format(it) } ?: ""
                    val dateStr = dateParsed?.let { formatOutDate.format(it) } ?: s.exam_date
                    """
                    <tr>
                        <td><strong style="color:#b91c1c; font-size:9px;">$dayStr</strong><br><span style="font-size:8px;">$dateStr</span></td>
                        <td>${s.start_time}</td>
                        <td>${if (s.end_time.isNullOrBlank() || s.end_time == "00:00:00") "---" else s.end_time}</td>
                        <td><strong>${s.sub_name.uppercase()}</strong></td>
                    </tr>
                    """
                }}
            </table>
            <div class="signatures">
                <div class="sig-box"><div class="sig-line"></div>Exam Cell</div>
                <div class="sig-box"><div class="sig-line"></div>Principal Sig</div>
            </div>
        </div>
        """
    }?.joinToString("") ?: ""

    val html = """
        <html><head><style>
            @page { margin: 8mm; size: A4 portrait; }
            body { font-family: 'Segoe UI', Arial, sans-serif; background: #fff; margin: 0; padding: 0; -webkit-print-color-adjust: exact; }
            .slip-container { width: 100%; text-align: left; }
            .slip-card { 
                display: inline-block; width: 48%; margin: 0 0.5% 20px 0.5%; vertical-align: top; box-sizing: border-box;
                border: 1px solid #ddd; border-radius: 12px; padding: 15px; page-break-inside: avoid;
            }
            .header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 8px; margin-bottom: 15px; }
            .school-title { font-size: 14px; font-weight: 800; color: #b91c1c; text-transform: uppercase; }
            .exam-title { font-size: 10px; color: #555; margin-top: 4px; font-weight: 700; }
            .info-container { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 15px; }
            .info-left { flex: 1; display: flex; justify-content: space-between; align-items: flex-end; padding-right: 15px; }
            .label { font-size: 8px; text-transform: uppercase; font-weight: 800; color: #888; display: block; margin-bottom: 2px; }
            strong { color: #111; }
            .student-name strong { font-size: 14px; }
            .student-roll { margin-top: 8px; }
            .student-roll strong { font-size: 11px; }
            .student-class { text-align: right; }
            .student-class strong { font-size: 11px; }
            .photo-box { width: 60px; height: 80px; flex-shrink: 0; border: 2px solid #555; padding: 2px; }
            .photo-box img { width: 100%; height: 100%; object-fit: cover; }
            table { width: 100%; border-collapse: collapse; font-size: 9px; margin-bottom: 15px; }
            th, td { border: 1px solid #eee; padding: 6px 4px; text-align: center; }
            th { background: #f8f9fa; font-weight: 800; color: #333; font-size: 9px; }
            td { color: #555; font-weight: 600; }
            td strong { color: #111; font-weight: 800; }
            .signatures { display: flex; justify-content: space-between; font-size: 9px; font-weight: 800; color: #333; margin-top: 25px; }
            .sig-box { text-align: center; width: 90px; }
            .sig-line { border-top: 2px solid #333; margin-bottom: 5px; }
        </style></head><body>
            <div class="slip-container">
                $slipsHtml
            </div>
        </body></html>
    """.trimIndent()

    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView, url: String) {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val jobName = "Roll_No_Slips_${examType}"
            val printAdapter = view.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

