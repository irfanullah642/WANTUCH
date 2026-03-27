package com.example.wantuch.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wantuch.domain.model.*
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SyllabusPlannerScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit
) {
    val syllabusData by viewModel.syllabusData.collectAsState()
    val structure by viewModel.schoolStructure.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isMgmt = listOf("admin", "super_admin", "super admin", "developer").contains(userRole.lowercase())
    val isStudent = userRole.equals("Student", ignoreCase = true)

    var selectedClass by remember { mutableStateOf("All Classes") }
    var selectedSection by remember { mutableStateOf("All Sections") }
    var selectedSubject by remember { mutableStateOf("All Subjects") }

    var showWizard by remember { mutableStateOf(false) }
    var showEditChapter by remember { mutableStateOf<SyllabusChapter?>(null) }
    var activeSubjectContext by remember { mutableStateOf<SyllabusItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchSyllabus()
        viewModel.fetchSchoolStructure()
    }

    LaunchedEffect(selectedClass) {
        selectedSection = "All Sections"
        selectedSubject = "All Subjects"
    }

    LaunchedEffect(selectedSection) {
        selectedSubject = "All Subjects"
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray
    val goldColor = Color(0xFFFFD700)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).statusBarsPadding()) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(Modifier.fillMaxSize().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        HeaderActionIcon(Icons.Default.ArrowBack, isDark, onBack)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, null, tint = goldColor, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("SYLLABUS PLANNER", color = goldColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                            Text("ACADEMIC ADVANCEMENT AUTHORITY", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        HeaderActionIcon(Icons.Default.DarkMode, isDark) { viewModel.toggleTheme() }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Filters & Wizard Button - RESTRICTED TO MGMT
            if (isMgmt) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val classNames = listOf("All Classes") + (structure?.classes?.map { it.name } ?: emptyList())
                        Box(Modifier.weight(1f)) {
                            DropdownSelector(selectedClass, classNames, isDark = isDark) { selectedClass = it }
                        }
                        val selectedClassObj = structure?.classes?.find { it.name.equals(selectedClass, ignoreCase = true) }
                        val sectionNames = listOf("All Sections") + (selectedClassObj?.sections?.map { it.name } ?: emptyList())
                        Box(Modifier.weight(1f)) {
                            DropdownSelector(selectedSection, sectionNames, isDark = isDark) { selectedSection = it }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val allItems = syllabusData?.items ?: emptyList()
                        val derivedSubjects = allItems.filter { it2 ->
                            (selectedClass == "All Classes" || it2.class_name.equals(selectedClass, ignoreCase = true)) &&
                            (selectedSection == "All Sections" || it2.section_name.equals(selectedSection, ignoreCase = true))
                        }.map { it2 -> it2.subject_name }.distinct()
                        val subjectNames = listOf("All Subjects") + derivedSubjects

                        Box(Modifier.weight(1.5f)) {
                            DropdownSelector(selectedSubject, subjectNames, isDark = isDark) { selectedSubject = it }
                        }
                        Button(
                            onClick = {
                                val cls = structure?.classes?.find { it.name.equals(selectedClass, ignoreCase = true) }
                                val sec = cls?.sections?.find { it.name.equals(selectedSection, ignoreCase = true) }
                                val subItem = syllabusData?.items?.find { it3 ->
                                    it3.class_id == (cls?.id ?: -1) &&
                                    it3.section_id == (sec?.id ?: -1) &&
                                    it3.subject_name.equals(selectedSubject, ignoreCase = true)
                                }
                                
                                if (subItem != null) {
                                    activeSubjectContext = subItem
                                    showWizard = true
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Bolt, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("SYLLABUS WIZARD", fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else {
                // Header for students when filters are hidden
                Text(
                    "MY CLASS CURRICULUM", 
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = textColor, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            if (isLoading && syllabusData == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            } else {
                val list: List<SyllabusItem> = syllabusData?.items ?: emptyList()
                val filteredList = list.filter { itX ->
                    (selectedClass == "All Classes" || itX.class_name.equals(selectedClass, ignoreCase = true)) &&
                    (selectedSection == "All Sections" || itX.section_name.equals(selectedSection, ignoreCase = true)) &&
                    (selectedSubject == "All Subjects" || itX.subject_name.equals(selectedSubject, ignoreCase = true))
                }

                if (filteredList.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EventNote, null, Modifier.size(64.dp), tint = labelColor.copy(0.2f))
                            Spacer(Modifier.height(16.dp))
                            Text("No Syllabus Records Found", color = labelColor)
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(filteredList) { sItem: SyllabusItem ->
                            SyllabusItemCard(
                                item = sItem,
                                isDark = isDark,
                                textColor = textColor,
                                labelColor = labelColor,
                                cardColor = cardColor,
                                onStatusChange = { tid, stat ->
                                    val newStat = if (stat.equals("Completed", true)) "Pending" else "Completed"
                                    viewModel.updateSyllabusStatus(tid, newStat, sItem.class_id, sItem.section_id, sItem.subject_id)
                                },
                                onEditChapter = { ch ->
                                    activeSubjectContext = sItem
                                    showEditChapter = ch
                                },
                                onDeleteTopic = { tid ->
                                    viewModel.deleteSyllabusTopic(tid, sItem.class_id, sItem.section_id, sItem.subject_id)
                                },
                                userRole = userRole
                            )
                        }
                    }
                }
            }
        }

        if (showWizard && activeSubjectContext != null) {
            SyllabusWizardDialog(
                context = activeSubjectContext!!,
                viewModel = viewModel,
                onDismiss = { showWizard = false }
            )
        }

        if (showEditChapter != null && activeSubjectContext != null) {
            EditChapterDialog(
                chapter = showEditChapter!!,
                item = activeSubjectContext!!,
                viewModel = viewModel,
                onDismiss = { showEditChapter = null }
            )
        }
    }
}

@Composable
fun SyllabusItemCard(
    item: SyllabusItem, 
    isDark: Boolean, 
    textColor: Color, 
    labelColor: Color, 
    cardColor: Color, 
    onStatusChange: (Int, String) -> Unit,
    onEditChapter: (SyllabusChapter) -> Unit,
    onDeleteTopic: (Int) -> Unit,
    userRole: String = "Student"
) {
    val isMgmt = listOf("admin", "super_admin", "super admin", "developer").contains(userRole.lowercase())
    val isStudent = userRole.equals("Student", ignoreCase = true)
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, labelColor.copy(0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded }
            ) {
                // Progress Circle
                Box(Modifier.size(50.dp), Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { item.percentage / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF10B981),
                        strokeWidth = 4.dp,
                        trackColor = labelColor.copy(0.1f)
                    )
                    Text("${item.percentage.toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Black, color = textColor)
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text("${item.class_name} - ${item.section_name}", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(item.subject_name.uppercase(), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        CounterChipMini("DONE: ${item.modules_done}", Color(0xFF10B981))
                        CounterChipMini("REM: ${item.modules_remaining}", Color(0xFFEF4444))
                        CounterChipMini("TOTAL: ${item.total_modules}", Color(0xFF6366F1))
                    }
                }

                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null, tint = labelColor, modifier = Modifier.size(28.dp)
                )
            }

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item.chapters.forEach { chapter ->
                        SyllabusChapterCard(
                            chapter = chapter, 
                            isDark = isDark, 
                            textColor = textColor, 
                            labelColor = labelColor, 
                            onStatusChange = onStatusChange,
                            onEditChapter = onEditChapter,
                            onDeleteTopic = onDeleteTopic,
                            userRole = userRole
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyllabusChapterCard(
    chapter: SyllabusChapter, 
    isDark: Boolean, 
    textColor: Color, 
    labelColor: Color, 
    onStatusChange: (Int, String) -> Unit,
    onEditChapter: (SyllabusChapter) -> Unit,
    onDeleteTopic: (Int) -> Unit,
    userRole: String = "Student"
) {
    val isMgmt = listOf("admin", "super_admin", "super admin", "developer").contains(userRole.lowercase())
    val isStudent = userRole.equals("Student", ignoreCase = true)
    var expanded by remember { mutableStateOf(false) }
    val cardColor = if (isDark) Color(0xFF1E293B).copy(0.5f) else Color.White.copy(0.5f)

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, labelColor.copy(0.1f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded }
            ) {
                // Chapter Progress
                Box(Modifier.size(36.dp), Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { chapter.percentage / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF10B981),
                        strokeWidth = 3.dp,
                        trackColor = labelColor.copy(0.1f)
                    )
                    Text("${chapter.percentage.toInt()}%", fontSize = 8.sp, fontWeight = FontWeight.Black, color = textColor)
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(chapter.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = labelColor, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${chapter.start_date ?: ""} - ${chapter.end_date ?: ""}",
                            color = labelColor,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isMgmt) {
                        Box(
                            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF0EA5E9).copy(0.1f))
                                .clickable { onEditChapter(chapter) },
                            Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(14.dp))
                        }
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = labelColor, modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    chapter.topics.forEach { topic ->
                        SyllabusTopicRow(
                            topic = topic, 
                            isDark = isDark, 
                            textColor = textColor, 
                            labelColor = labelColor, 
                            onStatusChange = onStatusChange, 
                            onDelete = onDeleteTopic,
                            userRole = userRole
                        )
                    }
                }
                
                if (isMgmt) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                            .clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF0EA5E9).copy(0.5f), RoundedCornerShape(8.dp))
                            .clickable { onEditChapter(chapter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ ADD / EDIT TOPICS", color = Color(0xFF0EA5E9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SyllabusTopicRow(
    topic: SyllabusTopic, 
    isDark: Boolean, 
    textColor: Color, 
    labelColor: Color, 
    onStatusChange: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    userRole: String = "Student"
) {
    val isMgmt = listOf("admin", "super_admin", "super admin", "developer").contains(userRole.lowercase())
    val isStudent = userRole.equals("Student", ignoreCase = true)
    val isDone = topic.status.equals("Completed", ignoreCase = true)
    val rowColor = if (isDark) Color(0xFF0F172A).copy(0.4f) else Color(0xFFF1F5F9)

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowColor)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp)
                .clip(CircleShape)
                .background(if (isDone) Color(0xFF10B981) else labelColor.copy(0.2f))
                .clickable { onStatusChange(topic.id, topic.status) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(topic.title, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (isDone) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(10.dp))
                }
            }
            Text(topic.target_date ?: "", color = labelColor, fontSize = 9.sp)
        }

        if (isMgmt) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFEF4444).copy(0.3f), RoundedCornerShape(8.dp))
                    .clickable { onDelete(topic.id) },
                Alignment.Center
            ) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun SyllabusWizardDialog(
    context: SyllabusItem,
    viewModel: WantuchViewModel,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val isDark by viewModel.isDarkTheme.collectAsState()
    
    // Step 1 states
    var startDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd").format(Date())) }
    var endDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().apply { add(Calendar.MONTH, 3) }.time)) }
    var skipSundays by remember { mutableStateOf(true) }
    var skipFridays by remember { mutableStateOf(false) }
    
    // Step 2 states
    var holidays by remember { mutableStateOf(mutableListOf<SyllabusHoliday>()) }
    
    // Step 3 states
    var chapters by remember { mutableStateOf(mutableListOf(SyllabusWizardChapter("New Chapter", mutableListOf("")))) }

    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SYLLABUS WIZARD", fontWeight = FontWeight.Black, fontSize = 18.sp, color = textColor)
                        Text("Step $step: " + when(step) {
                            1 -> "Configuration"
                            2 -> "Holidays & Breaks"
                            3 -> "Curriculum Content"
                            else -> "Review"
                        }, color = Color(0xFF0EA5E9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = textColor) }
                }
                
                // Progress Bar
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { i ->
                        Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i + 1 <= step) Color(0xFF6366F1) else textColor.copy(0.1f)))
                    }
                }

                // Step Content
                Box(Modifier.weight(1f).padding(20.dp)) {
                    when(step) {
                        1 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("ACTIVE CONTEXT", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.Gray)
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(textColor.copy(0.05f)).padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MenuBook, null, tint = Color(0xFF6366F1))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(context.subject_name, fontWeight = FontWeight.Black, color = textColor)
                                        Text("${context.class_name} - ${context.section_name}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                            
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                WizardField("Start Date", startDate, Modifier.weight(1f)) { startDate = it }
                                WizardField("End Date", endDate, Modifier.weight(1f)) { endDate = it }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(skipSundays, { skipSundays = it })
                                Text("Skip Sundays", color = textColor, fontSize = 12.sp)
                                Spacer(Modifier.width(20.dp))
                                Checkbox(skipFridays, { skipFridays = it })
                                Text("Skip Fridays", color = textColor, fontSize = 12.sp)
                            }
                        }
                        2 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("HOLIDAYS", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                                TextButton(onClick = { holidays = holidays.toMutableList().apply { add(SyllabusHoliday("", "", "")) } }) {
                                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                    Text("Add Holiday", fontSize = 10.sp)
                                }
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(holidays.size) { i ->
                                    val h = holidays[i]
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        TextField(h.name, { holidays = holidays.toMutableList().apply { this[i] = h.copy(name = it) } }, modifier = Modifier.weight(1.5f), placeholder = { Text("Event Name", fontSize = 10.sp) })
                                        WizardFieldSmall(h.start, Modifier.weight(1f)) { holidays = holidays.toMutableList().apply { this[i] = h.copy(start = it) } }
                                        WizardFieldSmall(h.end, Modifier.weight(1f)) { holidays = holidays.toMutableList().apply { this[i] = h.copy(end = it) } }
                                        IconButton(onClick = { holidays = holidays.toMutableList().apply { removeAt(i) } }) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
                                    }
                                }
                            }
                        }
                        3 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("CHAPTERS & TOPICS", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                                TextButton(onClick = { chapters = chapters.toMutableList().apply { add(SyllabusWizardChapter("", mutableListOf(""))) } }) {
                                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                    Text("Add Chapter", fontSize = 10.sp)
                                }
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(chapters.size) { i ->
                                    val ch = chapters[i]
                                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(textColor.copy(0.05f)).padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TextField(ch.name, { chapters = chapters.toMutableList().apply { this[i] = ch.copy(name = it) } }, modifier = Modifier.weight(1f), placeholder = { Text("Chapter Name", fontSize = 12.sp) })
                                            IconButton(onClick = { chapters = chapters.toMutableList().apply { removeAt(i) } }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        ch.topics.forEachIndexed { ti, topic ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF6366F1)))
                                                Spacer(Modifier.width(8.dp))
                                                TextField(topic, { 
                                                    val newList = ch.topics.toMutableList().apply { this[ti] = it }
                                                    chapters = chapters.toMutableList().apply { this[i] = ch.copy(topics = newList) }
                                                }, modifier = Modifier.weight(1f), placeholder = { Text("Sub-topic...", fontSize = 10.sp) })
                                                IconButton(onClick = { 
                                                    val newList = ch.topics.toMutableList().apply { removeAt(ti) }
                                                    if (newList.isEmpty()) newList.add("")
                                                    chapters = chapters.toMutableList().apply { this[i] = ch.copy(topics = newList) }
                                                }) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp)) }
                                            }
                                        }
                                        TextButton(onClick = { 
                                            val newList = ch.topics.toMutableList().apply { add("") }
                                            chapters = chapters.toMutableList().apply { this[i] = ch.copy(topics = newList) }
                                        }) { Text("+ Add Topic", fontSize = 10.sp) }
                                    }
                                }
                            }
                        }
                        4 -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.RocketLaunch, null, modifier = Modifier.size(80.dp), tint = Color(0xFF10B981))
                            Spacer(Modifier.height(20.dp))
                            Text("Ready to Deploy?", fontWeight = FontWeight.Black, fontSize = 24.sp, color = textColor)
                            Text("AI will distribute ${chapters.size} chapters from $startDate to $endDate", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                // Actions
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (step > 1) {
                        OutlinedButton(onClick = { step-- }, shape = RoundedCornerShape(12.dp)) { Text("BACK") }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    
                    Button(
                        onClick = { 
                            if (step < 4) step++ 
                            else {
                                viewModel.saveFullSyllabus(SyllabusWizardPayload(
                                    institution_id = viewModel.getInstitutionId(),
                                    class_id = context.class_id,
                                    section_id = context.section_id,
                                    subject_id = context.subject_id,
                                    session_start = startDate,
                                    session_end = endDate,
                                    skip_sundays = skipSundays,
                                    skip_fridays = skipFridays,
                                    skip_holidays = true,
                                    leaves = holidays,
                                    chapters = chapters
                                )) { onDismiss() }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (step == 4) Color(0xFF10B981) else Color(0xFF6366F1))
                    ) { 
                        Text(if (step == 4) "DEPLOY SYLLABUS" else "CONTINUE")
                        Icon(if (step == 4) Icons.Default.RocketLaunch else Icons.Default.ArrowForward, null, Modifier.padding(start = 8.dp).size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EditChapterDialog(
    chapter: SyllabusChapter,
    item: SyllabusItem,
    viewModel: WantuchViewModel,
    onDismiss: () -> Unit
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    var chapterName by remember { mutableStateOf(chapter.title) }
    var endDate by remember { mutableStateOf(chapter.end_date ?: "") }
    var topics by remember { mutableStateOf(chapter.topics.map { it.title }.toMutableList()) }
    var newTopic by remember { mutableStateOf("") }

    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("EDIT CHAPTER", fontWeight = FontWeight.Black, fontSize = 18.sp, color = textColor)
                        Text(item.subject_name.uppercase(), color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = textColor) }
                }
                
                Spacer(Modifier.height(20.dp))
                
                TextField(chapterName, { chapterName = it }, label = { Text("Chapter Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                TextField(endDate, { endDate = it }, label = { Text("End Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(Modifier.height(20.dp))
                Text("SUB-TOPICS BREAKDOWN", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
                
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(topics.size) { i ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(textColor.copy(0.05f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(topics[i], color = textColor, modifier = Modifier.weight(1f), fontSize = 12.sp)
                            IconButton(onClick = { topics = topics.toMutableList().apply { removeAt(i) } }) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
                
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(newTopic, { newTopic = it }, placeholder = { Text("New topic...") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { if (newTopic.isNotBlank()) { topics = topics.toMutableList().apply { add(newTopic) }; newTopic = "" } }, modifier = Modifier.background(Color(0xFF6366F1), CircleShape)) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        viewModel.editSyllabusChapter(
                            EditChapterPayload(
                                chapter_id = chapter.id,
                                chapter_name = chapterName,
                                new_end_date = endDate,
                                topics = topics
                            ),
                            classId = item.class_id,
                            sectionId = item.section_id,
                            subjectId = item.subject_id
                        ) { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("SAVE & RESCHEDULE")
                }
            }
        }
    }
}

@Composable
fun WizardField(label: String, value: String, modifier: Modifier, onEdit: (String) -> Unit) {
    Column(modifier) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
        TextField(value, onEdit, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun WizardFieldSmall(value: String, modifier: Modifier, onEdit: (String) -> Unit) {
    TextField(value, onEdit, modifier = modifier, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp))
}

@Composable
fun CounterChipMini(label: String, color: Color) {
    Surface(
        color = color.copy(0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(18.dp)
    ) {
        Box(Modifier.padding(horizontal = 6.dp), Alignment.Center) {
            Text(label, color = color, fontSize = 7.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun CounterChip(label: String, color: Color) {
    Surface(
        color = color.copy(0.1f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(24.dp)
    ) {
        Box(Modifier.padding(horizontal = 8.dp), Alignment.Center) {
            Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun DropdownSelector(selected: String, options: List<String>, isDark: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box {
        Card(
            modifier = Modifier.fillMaxWidth().height(45.dp).clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, textColor.copy(0.1f))
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selected, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null, tint = textColor)
            }
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.background(cardColor)) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt, color = textColor, fontSize = 12.sp) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}
