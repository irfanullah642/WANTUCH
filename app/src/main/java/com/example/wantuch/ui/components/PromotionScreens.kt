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
import com.example.wantuch.domain.model.*
import com.example.wantuch.ui.viewmodel.WantuchViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromotionScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit
) {
    val promotionData by viewModel.promotionData.collectAsState()
    val structure by viewModel.schoolStructure.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedYear by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) }
    var selectedClass by remember { mutableStateOf<SchoolClass?>(null) }
    var selectedSection by remember { mutableStateOf<SchoolSection?>(null) }
    var selectedCriteria by remember { mutableStateOf("percentage") }
    var targetClass by remember { mutableStateOf<SchoolClass?>(null) }

    var classMenu by remember { mutableStateOf(false) }
    var sectionMenu by remember { mutableStateOf(false) }
    var targetClassMenu by remember { mutableStateOf(false) }
    var criteriaMenu by remember { mutableStateOf(false) }
    var yearMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchSchoolStructure()
    }

    LaunchedEffect(selectedClass, selectedSection, selectedYear, selectedCriteria) {
        if (selectedClass != null && selectedSection != null) {
            viewModel.fetchStudentsForPromotion(selectedClass!!.id, selectedSection!!.id, selectedCriteria, selectedYear)
        }
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray
    val goldColor = Color(0xFFFFD700)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // Header Board (Flap Design)
            Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).statusBarsPadding()) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(Modifier.fillMaxSize().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        HeaderActionIcon(Icons.Filled.ArrowBack, isDark, onBack)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, null, tint = goldColor, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("STUDENT PROMOTION", color = goldColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                            Text("ACADEMIC ADVANCEMENT AUTHORITY", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        HeaderActionIcon(Icons.Filled.DarkMode, isDark) { viewModel.toggleTheme() }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Filters Grid (Spongy Cards)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Year and Class
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterCard("ACADEMIC YEAR", selectedYear, isDark, Modifier.weight(0.8f)) {
                        yearMenu = true
                    }
                    FilterCard("CURRENT CLASS", selectedClass?.name ?: "Select", isDark, Modifier.weight(1f)) {
                        classMenu = true
                    }
                }

                // Section and Criteria
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterCard("SECTION", selectedSection?.name ?: "Select", isDark, Modifier.weight(1f)) {
                        sectionMenu = true
                    }
                    FilterCard("CRITERIA", selectedCriteria.uppercase(), isDark, Modifier.weight(1.2f)) {
                        criteriaMenu = true
                    }
                }

                // Target Class
                FilterCard("TARGET CLASS", targetClass?.name ?: "Select Target Class", isDark, Modifier.fillMaxWidth()) {
                    targetClassMenu = true
                }

                Spacer(Modifier.height(8.dp))

                // Shift Button & Count
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            if (targetClass != null && selectedClass != null && selectedSection != null) {
                                val eligibleIds = promotionData?.students?.filter { it.eligible }?.map { it.student_id } ?: emptyList()
                                if (eligibleIds.isNotEmpty()) {
                                    viewModel.shiftPromotableStudents(eligibleIds, targetClass!!.id, selectedClass!!.id, selectedSection!!.id, selectedYear)
                                }
                            }
                        },
                        modifier = Modifier.height(45.dp).weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f))
                    ) {
                        Icon(Icons.Filled.Layers, null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("SHIFT PROMOTABLE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }

                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${promotionData?.students?.count { it.eligible } ?: 0} OF ${promotionData?.students?.size ?: 0} ELIGIBLE",
                            color = Color(0xFF6366F1), fontSize = 9.sp, fontWeight = FontWeight.Black
                        )
                        promotionData?.exam_info?.let { info ->
                            Text("BASED ON: ${info.type} ${info.year}", color = labelColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Student List
            Box(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFF6366F1))
                } else if (promotionData?.students.isNullOrEmpty()) {
                    Text(
                        "SELECT CLASS AND SECTION TO LOAD STUDENTS",
                        modifier = Modifier.align(Alignment.Center).padding(40.dp),
                        color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(promotionData!!.students!!) { student ->
                            PromotionStudentRow(
                                student = student,
                                isDark = isDark,
                                onPromote = { force ->
                                    if (targetClass != null && selectedClass != null && selectedSection != null) {
                                        viewModel.promoteStudent(student.student_id, force, targetClass!!.id, selectedClass!!.id, selectedSection!!.id, selectedYear)
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(100.dp)) }
                    }
                }
            }
        }


        // Dropdown Overlays

        if (classMenu) {
            SelectorDialog("CURRENT CLASS", structure?.classes?.map { it.name } ?: emptyList(), isDark, onDismiss = { classMenu = false }) { name ->
                selectedClass = structure?.classes?.find { it.name == name }
                selectedSection = null
                classMenu = false
            }
        }
        if (sectionMenu && selectedClass != null) {
            SelectorDialog("SECTION", selectedClass!!.sections?.map { it.name } ?: emptyList(), isDark, onDismiss = { sectionMenu = false }) { name ->
                selectedSection = selectedClass!!.sections?.find { it.name == name }
                sectionMenu = false
            }
        }
        if (targetClassMenu) {
            SelectorDialog("TARGET CLASS", structure?.classes?.map { it.name } ?: emptyList(), isDark, onDismiss = { targetClassMenu = false }) { name ->
                targetClass = structure?.classes?.find { it.name == name }
                targetClassMenu = false
            }
        }
        if (criteriaMenu) {
            SelectorDialog("PROMOTION CRITERIA", listOf("percentage", "dmc_status", "subject_count"), isDark, onDismiss = { criteriaMenu = false }) { c ->
                selectedCriteria = c
                criteriaMenu = false
            }
        }
        if (yearMenu) {
            val years = (2020..2030).map { it.toString() }
            SelectorDialog("ACADEMIC YEAR", years, isDark, onDismiss = { yearMenu = false }) { y ->
                selectedYear = y
                yearMenu = false
            }
        }
    }
}

@Composable
fun SelectorDialog(title: String, options: List<String>, isDark: Boolean, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.6f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(title, color = Color(0xFF6366F1), fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options) { opt ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(opt) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f))
                        ) {
                            Text(opt, color = if (isDark) Color.White else Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterCard(label: String, value: String, isDark: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun PromotionStudentRow(
    student: PromotionStudent,
    isDark: Boolean,
    onPromote: (Boolean) -> Unit
) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val statusColor = if (student.eligible) Color(0xFF10B981) else Color(0xFFEF4444)

    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, textColor.copy(0.05f))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Status Pipe
            Box(Modifier.width(3.dp).fillMaxHeight(0.6f).clip(CircleShape).background(statusColor))
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (student.eligible) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = statusColor
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(student.full_name.uppercase(), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                Text("ROLL: ${student.roll_number ?: "N/A"} | CLASS NO: ${student.class_no ?: "N/A"}", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(student.reason, color = textColor.copy(0.7f), fontSize = 9.sp, fontWeight = FontWeight.Normal, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
            }

            Button(
                onClick = { onPromote(!student.eligible) },
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    if (student.eligible) Icons.Filled.School else Icons.Filled.Bolt,
                    null,
                    modifier = Modifier.size(12.dp),
                    tint = if (student.eligible) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (student.eligible) "PROMOTE" else "FORCE", fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
