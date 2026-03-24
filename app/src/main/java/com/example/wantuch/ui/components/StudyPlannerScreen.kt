package com.example.wantuch.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wantuch.domain.model.*
import com.example.wantuch.ui.viewmodel.WantuchViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val structure by viewModel.schoolStructure.collectAsState()
    val subjects by viewModel.studySubjects.collectAsState()
    val plannerData by viewModel.plannerData.collectAsState()
    val dashboard by viewModel.dashboardData.collectAsState()

    var activeTab by remember { mutableStateOf("Overview") }
    var selectedClass by remember { mutableStateOf("Select Class") }
    var selectedSection by remember { mutableStateOf("Select Section") }
    
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val accentColor = Color(0xFF6366F1)
    val successColor = Color(0xFF10B981)

    LaunchedEffect(Unit) {
        viewModel.fetchSchoolStructure()
    }

    LaunchedEffect(selectedClass, selectedSection) {
        val cls = structure?.classes?.find { it.name == selectedClass }
        val sec = cls?.sections?.find { it.name == selectedSection }
        if (cls != null && sec != null) {
            viewModel.fetchStudySubjects(cls.id, sec.id)
            viewModel.fetchPlannerData(cls.id, sec.id)
        }
    }

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .background(bgColor)
                    .statusBarsPadding()) {
                
                // Header Banner
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(Modifier.fillMaxSize().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack, modifier = Modifier.background(accentColor.copy(0.1f), CircleShape)) {
                                Icon(Icons.Default.ArrowBack, null, tint = accentColor)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoStories, null, tint = accentColor, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("STUDY PLANNER", color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                }
                                Text("TRANSFORMING CURRICULUM INTO SUCCESS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.toggleTheme() }, modifier = Modifier.background(accentColor.copy(0.1f), CircleShape)) {
                                Icon(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = accentColor)
                            }
                        }
                    }
                }

                // Filters Row
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val classNames = structure?.classes?.map { it.name } ?: emptyList()
                    Box(Modifier.weight(1f)) {
                        DropdownSelector(selectedClass, classNames, isDark) { sc ->
                            selectedClass = sc
                            selectedSection = "Select Section"
                        }
                    }
                    val cls = structure?.classes?.find { it.name == selectedClass }
                    val sectionNames = cls?.sections?.map { it.name } ?: emptyList()
                    Box(Modifier.weight(1f)) {
                        DropdownSelector(selectedSection, sectionNames, isDark) { ss -> selectedSection = ss }
                    }
                }

                // Premium Tabs
                val isStudent = dashboard?.status?.contains("student", ignoreCase = true) == true
                if (!isStudent) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardColor)
                            .padding(4.dp)
                    ) {
                        listOf("Overview", "Integrate").forEach { tab ->
                            val isSelected = activeTab == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accentColor else Color.Transparent)
                                    .clickable { activeTab = tab },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    tab.uppercase(),
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = bgColor
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val isStudent = dashboard?.status?.contains("student", ignoreCase = true) == true
            if (selectedClass == "Select Class" || selectedSection == "Select Section") {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, null, Modifier.size(64.dp), tint = Color.Gray.copy(0.3f))
                        Text("Please Select Class and Section", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else if (isLoading && plannerData.isEmpty() && subjects.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else {
                when (activeTab) {
                    "Overview" -> PlannerOverviewTab(plannerData, isDark, accentColor, successColor, textColor, viewModel)
                    "Integrate" -> PlannerIntegrateTab(subjects, plannerData, isDark, accentColor, successColor, textColor, viewModel) { config ->
                        val cls = structure?.classes?.find { it.name == selectedClass }
                        val sec = cls?.sections?.find { it.name == selectedSection }
                        if (cls != null && sec != null) {
                            viewModel.savePlannerConfig(cls.id, sec.id, config) {
                                Toast.makeText(context, "Study Plan Saved!", Toast.LENGTH_SHORT).show()
                                activeTab = "Overview"
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlannerOverviewTab(
    data: List<StudyPlanItem>,
    isDark: Boolean,
    accentColor: Color,
    successColor: Color,
    textColor: Color,
    viewModel: WantuchViewModel
) {
    if (data.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("No Study Plan integrated yet.", color = Color.Gray)
        }
        return
    }

    val groupedBySubject = data.groupBy { it.subject_name ?: "Unknown Subject" }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        groupedBySubject.forEach { (subject, topics) ->
            item {
                Text(subject.uppercase(), color = accentColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
            }
            items(topics) { item ->
                PlannerItemCard(item, isDark, accentColor, successColor, textColor) {
                    viewModel.completeTopic(item.subject_id, item.topic_name ?: "") {
                        // Optimistic update
                        viewModel.fetchPlannerData(item.id ?: 0, 0) // ids are tricky, usually we'd refresh for all
                    }
                }
            }
        }
    }
}

@Composable
fun PlannerItemCard(
    item: StudyPlanItem,
    isDark: Boolean,
    accentColor: Color,
    successColor: Color,
    textColor: Color,
    onComplete: () -> Unit = {}
) {
    val isDone = item.status.equals("Completed", ignoreCase = true)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (isDone) successColor else Color.Gray.copy(0.1f))
                    .clickable { if (!isDone) onComplete() },
                Alignment.Center
            ) {
                Icon(if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (isDone) Color.White else Color.Gray)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(item.chapter_name ?: "Chapter", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(item.topic_name ?: "Topic", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    TaskBadge("SQ: ${item.tasks_decoded?.get("sq") ?: item.short_qs ?: "0"}", Color(0xFF3B82F6))
                    TaskBadge("LQ: ${item.tasks_decoded?.get("lq") ?: item.long_qs ?: "0"}", Color(0xFF6366F1))
                    TaskBadge("NUM: ${item.tasks_decoded?.get("num") ?: item.numericals ?: "0"}", Color(0xFF10B981))
                }
            }
        }
    }
}

@Composable
fun TaskBadge(text: String, color: Color) {
    Surface(
        color = color.copy(0.1f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.height(18.dp)
    ) {
        Box(Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
            Text(text, color = color, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun PlannerIntegrateTab(
    subjects: List<StudySubject>,
    existingData: List<StudyPlanItem>,
    isDark: Boolean,
    accentColor: Color,
    successColor: Color,
    textColor: Color,
    viewModel: WantuchViewModel,
    onSave: (List<StudyPlanItem>) -> Unit
) {
    val configs = remember(subjects, existingData) { 
        mutableStateListOf<StudyPlanItem>().apply {
            addAll(existingData)
            if (isEmpty()) {
                subjects.forEach { sub ->
                    add(StudyPlanItem(subject_id = sub.id, subject_name = sub.name, chapter_name = "", topic_name = ""))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(configs.size) { index ->
                val config = configs[index]
                IntegrationRow(
                    config, isDark, accentColor, textColor,
                    onUpdate = { updated -> configs[index] = updated },
                    onDelete = { configs.removeAt(index) }
                )
            }
            item {
                TextButton(
                    onClick = { 
                        if (subjects.isNotEmpty()) {
                            val first = subjects[0]
                            configs.add(StudyPlanItem(subject_id = first.id, subject_name = first.name, chapter_name = "", topic_name = ""))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Text("ADD MORE TARGETS")
                }
            }
        }
        
        Button(
            onClick = { onSave(configs) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("DEPLOY STUDY PLAN")
        }
    }
}

@Composable
fun IntegrationRow(
    item: StudyPlanItem,
    isDark: Boolean,
    accentColor: Color,
    textColor: Color,
    onUpdate: (StudyPlanItem) -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.subject_name ?: "SUBJECT", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Chapter Name", fontSize = 9.sp, color = Color.Gray)
                    BasicInput(item.chapter_name ?: "", isDark) { onUpdate(item.copy(chapter_name = it)) }
                }
                Column(Modifier.weight(1f)) {
                    Text("Topic Name", fontSize = 9.sp, color = Color.Gray)
                    BasicInput(item.topic_name ?: "", isDark) { onUpdate(item.copy(topic_name = it)) }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Short Qs", fontSize = 9.sp, color = Color.Gray)
                    val currentVal = item.tasks_decoded?.get("sq") ?: item.short_qs ?: "0"
                    BasicInput(currentVal, isDark) { 
                        val config = (item.tasks_decoded ?: emptyMap()).toMutableMap()
                        config["sq"] = it
                        onUpdate(item.copy(tasks_decoded = config))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Long Qs", fontSize = 9.sp, color = Color.Gray)
                    val currentVal = item.tasks_decoded?.get("lq") ?: item.long_qs ?: "0"
                    BasicInput(currentVal, isDark) { 
                        val config = (item.tasks_decoded ?: emptyMap()).toMutableMap()
                        config["lq"] = it
                        onUpdate(item.copy(tasks_decoded = config))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Numericals", fontSize = 9.sp, color = Color.Gray)
                    val currentVal = item.tasks_decoded?.get("num") ?: item.numericals ?: "0"
                    BasicInput(currentVal, isDark) { 
                        val config = (item.tasks_decoded ?: emptyMap()).toMutableMap()
                        config["num"] = it
                        onUpdate(item.copy(tasks_decoded = config))
                    }
                }
            }
        }
    }
}

@Composable
fun BasicInput(value: String, isDark: Boolean, onEdit: (String) -> Unit) {
    val bg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val text = if (isDark) Color.White else Color(0xFF1E293B)
    
    BasicTextField(
        value = value,
        onValueChange = onEdit,
        textStyle = androidx.compose.ui.text.TextStyle(color = text, fontSize = 12.sp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(8.dp),
        singleLine = true
    )
}
