package com.example.wantuch.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.example.wantuch.domain.model.*
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeworkManagementScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val assignmentsRes by viewModel.assignments.collectAsState()
    val submissions by viewModel.assignmentSubmissions.collectAsState()
    val dashboard by viewModel.dashboardData.collectAsState()
    
    val isDark = true // Matching the "Standard" theme of the web app (slate-900)
    val bgColor = Color(0xFF0F172A)
    val cardColor = Color(0xFF1E293B)
    val textColor = Color.White
    val primaryColor = Color(0xFF6366F1)
    val accentColor = Color(0xFFE11D48)

    var searchQuery by remember { mutableStateOf("") }
    
    // Filters
    val isAdmin = assignmentsRes?.is_admin ?: false
    val userRole = dashboard?.role ?: ""
    val isStudent = userRole == "student"
    var activeTab by remember(isStudent) { mutableStateOf(if (isStudent) "History" else "New") }

    // Hierarchy Support (for "New" tab)
    val staffProfile by viewModel.staffProfile.collectAsState()
    LaunchedEffect(dashboard) {
        viewModel.fetchAssignments()
        val userId = dashboard?.user_id
        if (!isStudent && userId != null) {
            viewModel.fetchStaffProfile(userId) // To get timetable/hierarchy
        }
    }

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .background(bgColor)
                    .statusBarsPadding()) {
                
                // Premium 3D Banner
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Decorative standoff pipes (simulated with canvas or spacers)
                    Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        HangingTube(Modifier.padding(start = 10.dp))
                        HangingTube(Modifier.padding(end = 10.dp))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.8f)),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f))
                    ) {
                        Row(
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(0.05f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            // Center Identity
                            Row(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Black, Color(0xFF450a0a), Color.Black)
                                        )
                                    )
                                    .border(1.dp, Color(0xFF7f1d1d).copy(0.4f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(0.6f))
                                        .border(1.dp, Color.Red.copy(0.3f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Assignment, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "ASN Hub",
                                        color = textColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        "Homework Distribution Node",
                                        color = Color.Red.copy(0.6f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            IconButton(
                                onClick = { },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(0.05f))
                            ) {
                                Icon(Icons.Default.Palette, null, tint = textColor)
                            }
                        }
                    }
                }

                // Tab Switcher
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-10).dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, Color.White.copy(0.05f))
                    ) {
                        Row(
                            Modifier
                                .padding(vertical = 4.dp, horizontal = 12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = if (isStudent) listOf("History") else listOf("New", "History", "Inbox")
                            tabs.forEach { tab ->
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clickable { activeTab = tab }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        tab.uppercase(),
                                        color = if (activeTab == tab) primaryColor else textColor.copy(0.4f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    if (activeTab == tab) {
                                        Spacer(Modifier.height(4.dp))
                                        Box(Modifier.size(12.dp, 2.dp).background(primaryColor, CircleShape))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (activeTab) {
                "New" -> NewAssignmentTab(viewModel, context)
                "History" -> HistoryTab(viewModel, assignmentsRes?.history ?: emptyList(), isStudent, context)
                "Inbox" -> InboxTab(viewModel, assignmentsRes?.inbox ?: emptyList())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewAssignmentTab(viewModel: WantuchViewModel, context: Context) {
    val staffProfile by viewModel.staffProfile.collectAsState()
    val timetable = staffProfile?.inst_timetable ?: emptyList()
    
    // Group timetable by Class/Section/Subject and filter out invalid/empty ones
    val filteredTimetable = timetable.filter { it["cname"]?.toString()?.isNotEmpty() == true }
    var selectedTimetableItem by remember { mutableStateOf<Map<String, Any?>?>(null) }
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(-1f) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedFileUri = uri
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color.White.copy(0.05f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddCircle, null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Direct Assignment", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Push homework directly to student dashboards", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Text("SELECT TARGET SUBJECT", color = Color(0xFF6366F1), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredTimetable.forEach { tt ->
                            val isSelected = selectedTimetableItem == tt
                            val className = tt["cname"]?.toString() ?: ""
                            val sectionName = tt["secname"]?.toString() ?: ""
                            val subjectName = tt["subname"]?.toString() ?: ""
                            
                            Box(
                                Modifier
                                    .width(150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFF6366F1).copy(0.2f) else Color.Black.copy(0.2f))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF6366F1) else Color.White.copy(0.05f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedTimetableItem = tt }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(className, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text(sectionName, color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(subjectName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (isSelected) {
                                    Box(Modifier.size(8.dp).align(Alignment.TopEnd).background(Color(0xFF6366F1), CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedTimetableItem != null) {
            item {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Assignment Title", color = Color(0xFF6366F1), fontSize = 10.sp, fontWeight = FontWeight.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            unfocusedLabelColor = Color.Gray
                        )
                    )

                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Completion Deadline (YYYY-MM-DD)", color = Color(0xFF6366F1), fontSize = 10.sp, fontWeight = FontWeight.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            unfocusedLabelColor = Color.Gray
                        )
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detailed Instructions", color = Color(0xFF6366F1), fontSize = 10.sp, fontWeight = FontWeight.Black) },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            unfocusedLabelColor = Color.Gray
                        )
                    )

                    // Attachment
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(0.05f))
                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                            .clickable { filePicker.launch("*/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudUpload, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            Text(
                                if (selectedFileUri != null) "FILE SELECTED" else "RESOURCES / ATTACHMENTS",
                                color = if (selectedFileUri != null) Color(0xFF10B981) else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    
                    if (uploadProgress >= 0f) {
                        LinearProgressIndicator(progress = uploadProgress, modifier = Modifier.fillMaxWidth(), color = Color(0xFF6366F1))
                    }

                    Button(
                        onClick = {
                            val tt = selectedTimetableItem ?: return@Button
                            if (title.isBlank() || dueDate.isBlank()) {
                                Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSaving = true
                            viewModel.createAssignment(
                                classId = (tt["class_id"]?.toString()?.toDoubleOrNull() ?: 0.0).toInt(),
                                sectionId = (tt["section_id"]?.toString()?.toDoubleOrNull() ?: 0.0).toInt(),
                                subjectId = (tt["subject_id"]?.toString()?.toDoubleOrNull() ?: 0.0).toInt(),
                                title = title,
                                description = description,
                                dueDate = dueDate,
                                uri = selectedFileUri,
                                context = context,
                                onProgress = { uploadProgress = it }
                            ) { success ->
                                isSaving = false
                                uploadProgress = -1f
                                if (success) {
                                    title = ""
                                    description = ""
                                    dueDate = ""
                                    selectedFileUri = null
                                    selectedTimetableItem = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(65.dp),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(10.dp))
                            Text("SAVE ASSIGNMENT", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryTab(viewModel: WantuchViewModel, history: List<Assignment>, isStudent: Boolean, context: Context) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("All Classes") }
    var selectedSubject by remember { mutableStateOf("All Subjects") }
    var selectedStaff by remember { mutableStateOf("All Staff") }
    var selectedDate by remember { mutableStateOf("All Dates") }

    val classes = remember(history) { listOf("All Classes") + history.mapNotNull { it.cname }.distinct().sorted() }
    val subjects = remember(history) { listOf("All Subjects") + history.mapNotNull { it.subname }.distinct().sorted() }
    val staffList = remember(history) { listOf("All Staff") + history.mapNotNull { it.teacher_name }.distinct().sorted() }
    val dates = remember(history) { listOf("All Dates") + history.map { it.due_date }.distinct().sorted() }

    val filtered = history.filter { asn ->
        val matchQuery = asn.title.contains(searchQuery, ignoreCase = true) || asn.subname?.contains(searchQuery, ignoreCase = true) == true
        val matchClass = selectedClass == "All Classes" || asn.cname == selectedClass
        val matchSubject = selectedSubject == "All Subjects" || asn.subname == selectedSubject
        val matchStaff = selectedStaff == "All Staff" || asn.teacher_name == selectedStaff
        val matchDate = selectedDate == "All Dates" || asn.due_date == selectedDate
        matchQuery && matchClass && matchSubject && matchStaff && matchDate
    }.take(50)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(0.4f)),
                border = BorderStroke(1.dp, Color.White.copy(0.05f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Total Assignments: ${filtered.size}", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedClass = "All Classes"
                                selectedSubject = "All Subjects"
                                selectedStaff = "All Staff"
                                selectedDate = "All Dates"
                            },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(0.3f)),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0EA5E9))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("CLEAR FILTERS", fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Dropdown Row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterDropdown("CLASS", selectedClass, classes, Modifier.weight(1f)) { selectedClass = it }
                        FilterDropdown("SUBJECT", selectedSubject, subjects, Modifier.weight(1f)) { selectedSubject = it }
                        FilterDropdown("STAFF", selectedStaff, staffList, Modifier.weight(1f)) { selectedStaff = it }
                        FilterDropdown("DUE DATE", selectedDate, dates, Modifier.weight(1f)) { selectedDate = it }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Search by title
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by title...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp)) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            focusedContainerColor = Color.Black.copy(0.2f),
                            unfocusedContainerColor = Color.Black.copy(0.2f)
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(50.dp), Alignment.Center) {
                    Text("NO HISTORY FOUND", color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        } else {
            items(filtered) { asn ->
                AssignmentCard(asn, isStudent, viewModel, context)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    selected: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when(label) {
                    "CLASS" -> Icons.Default.Class
                    "SUBJECT" -> Icons.Default.Book
                    "STAFF" -> Icons.Default.Person
                    "DUE DATE" -> Icons.Default.Event
                    else -> Icons.Default.Category
                },
                null, tint = Color.Gray, modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.White.copy(0.1f),
                    focusedContainerColor = Color.Black.copy(0.1f),
                    unfocusedContainerColor = Color.Black.copy(0.1f)
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1E293B)).padding(0.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White, fontSize = 12.sp) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        modifier = Modifier.background(Color(0xFF1E293B)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InboxTab(viewModel: WantuchViewModel, submissions: List<AssignmentSubmission>) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = submissions.filter { it.student_name?.contains(searchQuery, ignoreCase = true) == true || it.asn_title?.contains(searchQuery, ignoreCase = true) == true }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by student or title...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.White.copy(0.1f)
                )
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(50.dp), Alignment.Center) {
                    Text("NO SUBMISSIONS FOUND", color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        } else {
            items(filtered) { sub ->
                SubmissionInboxCard(sub, viewModel)
            }
        }
    }
}

@Composable
fun AssignmentCard(asn: Assignment, isStudent: Boolean, viewModel: WantuchViewModel, context: Context) {
    var showDetails by remember { mutableStateOf(false) }
    var showSubmissions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF6366F1).copy(0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(asn.subname ?: "Subject", color = Color(0xFF6366F1), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp))
                Text("DUE: ${asn.due_date}", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                
                if (isStudent) {
                    val status = asn.submission_status ?: "Pending"
                    val color = when(status) {
                        "Approved" -> Color(0xFF10B981)
                        "Submitted" -> Color(0xFFF59E0B)
                        "Rejected" -> Color(0xFFEF4444)
                        else -> Color.Gray
                    }
                    Text(status.uppercase(), color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SUBS:", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Text("${asn.submissions_count}", color = Color(0xFF6366F1), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(asn.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("CLASS: ${asn.cname} • ${asn.secname}", color = Color(0xFF6366F1).copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (!isStudent) {
                    Spacer(Modifier.width(10.dp))
                    Text("BY: ${asn.teacher_name}", color = Color.Gray.copy(0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isStudent && asn.submission_status != "Approved") {
                    Button(
                        onClick = { showDetails = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if(asn.submission_status == "Pending") "SUBMIT WORK" else "UPDATE WORK", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                } else if (!isStudent) {
                    IconButton(onClick = { showSubmissions = true; viewModel.fetchSubmissionDetails(asn.id) }, Modifier.size(40.dp).background(Color.White.copy(0.05f), RoundedCornerShape(10.dp))) {
                        Icon(Icons.Default.Visibility, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { /* Edit */ }, Modifier.size(40.dp).background(Color.White.copy(0.05f), RoundedCornerShape(10.dp))) {
                        Icon(Icons.Default.Edit, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { /* Delete */ }, Modifier.size(40.dp).background(Color.White.copy(0.05f), RoundedCornerShape(10.dp))) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
                
                if (!asn.attachment.isNullOrEmpty()) {
                    IconButton(onClick = { /* Download */ }, Modifier.size(40.dp).background(Color.Black.copy(0.2f), RoundedCornerShape(10.dp))) {
                        Icon(Icons.Default.FileDownload, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
                
                IconButton(onClick = { /* WP Share */ }, Modifier.size(40.dp).background(Color(0xFF10B981).copy(0.1f), RoundedCornerShape(10.dp))) {
                    Icon(Icons.Default.Share, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Modal simulators (Dialogs) would go here
}

@Composable
fun SubmissionInboxCard(sub: AssignmentSubmission, viewModel: WantuchViewModel) {
    val context = LocalContext.current
    var feedback by remember { mutableStateOf("") }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF6366F1).copy(0.1f)), Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF6366F1))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(sub.student_name ?: "Anonymous", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF6366F1).copy(0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(sub.asn_title ?: "Assignment", color = Color(0xFF6366F1), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFF59E0B).copy(0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("INCOMING", color = Color(0xFFF59E0B), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    Text(sub.submitted_at ?: "", color = Color.Gray, fontSize = 8.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(0.2f)).padding(16.dp)) {
                Column {
                    Text("STUDENT'S RESPONSE", color = Color(0xFF6366F1).copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text(sub.content ?: "", color = Color.LightGray, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                placeholder = { Text("Provide feedback...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1), unfocusedBorderColor = Color.White.copy(0.1f))
            )

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.reviewSubmission(sub.id, "Approved", feedback, context) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981).copy(0.2f), contentColor = Color(0xFF10B981))
                ) {
                    Text("VALIDATE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Button(
                    onClick = { viewModel.reviewSubmission(sub.id, "Rejected", feedback, context) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(0.2f), contentColor = Color(0xFFEF4444))
                ) {
                    Text("DISMISS", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }
    }
}
