package com.example.wantuch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import coil.compose.AsyncImage
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import java.util.Calendar

@Composable
fun ResultsTabContent(
    viewModel: WantuchViewModel,
    isDark: Boolean,
    openWeb: (String) -> Unit
) {
    val schoolStructure by viewModel.schoolStructure.collectAsState()
    val classes = schoolStructure?.classes ?: emptyList()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedSubTab by remember { mutableStateOf("DMC") }
    
    // Filters State
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val sessionOptions = (currentYear downTo currentYear - 2).map { "Session $it" }
    var selectedSession by remember { mutableStateOf(sessionOptions.first()) }
    
    val printTypeOptions = listOf("Single Student", "Whole Class")
    var selectedPrintType by remember { mutableStateOf("Single Student") }
    
    val classOptions = remember(classes) { listOf("Class") + classes.map { it.name } }
    var selectedClassName by remember { mutableStateOf("Class") }
    val currentClassObj = classes.find { it.name == selectedClassName }
    
    val sectionOptions = remember(currentClassObj) { listOf("Section") + (currentClassObj?.sections?.map { it.name } ?: emptyList()) }
    var selectedSectionName by remember { mutableStateOf("Section") }

    var examList by remember { mutableStateOf<List<com.example.wantuch.domain.model.AwardListExam>>(emptyList()) }
    var selectedExamName by remember { mutableStateOf("Select Exam") }
    val examOptions = remember(examList) { listOf("Select Exam") + examList.map { "${it.exam_type} - ${it.subject_name}" } }

    var rollNumber by remember { mutableStateOf("") }
    
    // Settings State
    var selectedTemplate by remember { mutableStateOf("Prestige Royal (Elite)") }
    var selectedTheme by remember { mutableStateOf("Royal Blue (Standard)") }
    var selectedWatermark by remember { mutableStateOf("Enabled (Institutional)") }
    var selectedSigs by remember { mutableStateOf("Triple Verified (3)") }

    var isLoading by remember { mutableStateOf(false) }
    var generatedResults by remember { mutableStateOf<List<com.example.wantuch.domain.model.ConsolidatedStudentResult>?>(null) }
    var generatedFullResults by remember { mutableStateOf<List<com.example.wantuch.domain.model.FullResultStudent>?>(null) }
    
    var performanceSubTab by remember { mutableStateOf("school_toppers") }
    var topperResults by remember { mutableStateOf<List<com.example.wantuch.domain.model.TopperStats>?>(null) }
    var schoolStatsResult by remember { mutableStateOf<com.example.wantuch.domain.model.SchoolStats?>(null) }
    var staffStatsResult by remember { mutableStateOf<List<com.example.wantuch.domain.model.StaffStats>?>(null) }
    var trendStatsResult by remember { mutableStateOf<List<com.example.wantuch.domain.model.TrendStats>?>(null) }
    
    LaunchedEffect(selectedClassName, selectedSectionName) {
        if (selectedClassName != "Class" && selectedSectionName != "Section") {
            val cid = classes.find { it.name == selectedClassName }?.id ?: 0
            val sid = classes.find { it.name == selectedClassName }?.sections?.find { it.name == selectedSectionName }?.id ?: 0
            if (cid > 0 && sid > 0) {
                viewModel.getAwardListExams(cid, sid, onSuccess = { 
                    examList = it.exams.distinctBy { e -> e.exam_type }
                    selectedExamName = "Select Exam"
                }, onError = {
                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                })
            }
        }
    }

    if (generatedResults != null || generatedFullResults != null) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(onClick = { 
                generatedResults = null
                generatedFullResults = null
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("Back to Form", color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            if (generatedResults != null) {
                generatedResults!!.forEach { res ->
                    DmcCardNative(res)
                    Spacer(Modifier.height(24.dp))
                }
            } else if (generatedFullResults != null) {
                generatedFullResults!!.forEach { res ->
                    FullResultCardNative(res)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        return
    }

    if (topperResults != null || schoolStatsResult != null || staffStatsResult != null || trendStatsResult != null) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(onClick = { 
                topperResults = null
                schoolStatsResult = null
                staffStatsResult = null
                trendStatsResult = null
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("Back to Form", color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            if (topperResults != null) {
                TopperCardNative(topperResults!!, isSchoolLevel = performanceSubTab == "school_toppers")
            } else if (schoolStatsResult != null) {
                SchoolStatsCardNative(schoolStatsResult!!)
            } else if (staffStatsResult != null) {
                StaffStatsCardNative(staffStatsResult!!)
            } else if (trendStatsResult != null) {
                TrendStatsCardNative(trendStatsResult!!)
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Title & Sub Tabs
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B), RoundedCornerShape(8.dp)).padding(16.dp)) {
            
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.background(if (selectedSubTab == "DMC") Color(0xFF38BDF8) else Color.Transparent, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 6.dp).clickable { selectedSubTab = "DMC" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedSubTab == "DMC") Color.White else Color(0xFF94A3B8))
                    Spacer(Modifier.width(6.dp))
                    Text("DMC", color = if (selectedSubTab == "DMC") Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFFFBEB8C))
                }
                
                Row(
                    modifier = Modifier.background(if (selectedSubTab == "Result Card") Color(0xFF38BDF8) else Color.Transparent, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 6.dp).clickable { selectedSubTab = "Result Card" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedSubTab == "Result Card") Color.White else Color(0xFF94A3B8))
                    Spacer(Modifier.width(6.dp))
                    Text("Result Card", color = if (selectedSubTab == "Result Card") Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Row(
                    modifier = Modifier.background(if (selectedSubTab == "Performance") Color(0xFF38BDF8) else Color.Transparent, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 6.dp).clickable { selectedSubTab = "Performance" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedSubTab == "Performance") Color.White else Color(0xFF94A3B8))
                    Spacer(Modifier.width(6.dp))
                    Text("Performance", color = if (selectedSubTab == "Performance") Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (selectedSubTab == "Performance") {
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val perfTabs = listOf(
                        "school_toppers" to "SCHOOL TOPPERS",
                        "class_toppers" to "CLASS TOPPERS",
                        "school_performance" to "SCHOOL STATS",
                        "staff_performance" to "STAFF STATS",
                        "trends" to "SUBJECT TRENDS"
                    )
                    perfTabs.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .border(1.dp, if (performanceSubTab == key) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(4.dp))
                                .background(if (performanceSubTab == key) Color(0xFF38BDF8).copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clickable { performanceSubTab = key },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = if (performanceSubTab == key) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Flex Form Grid
            if (selectedSubTab != "Performance") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { DropdownSelector(value = selectedSession, options = sessionOptions, modifier = Modifier.fillMaxWidth().height(52.dp), isDark = isDark) { selectedSession = it } }
                    Box(Modifier.weight(1f)) { DropdownSelector(value = selectedPrintType, options = printTypeOptions, modifier = Modifier.fillMaxWidth().height(52.dp), isDark = isDark) { selectedPrintType = it } }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { DropdownSelector(value = selectedClassName, options = classOptions, modifier = Modifier.fillMaxWidth().height(52.dp), isDark = isDark) { selectedClassName = it; selectedSectionName = "Section"; selectedExamName = "Select Exam" } }
                Box(Modifier.weight(1f)) { DropdownSelector(value = selectedSectionName, options = sectionOptions, modifier = Modifier.fillMaxWidth().height(52.dp), isDark = isDark) { selectedSectionName = it; selectedExamName = "Select Exam" } }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selectedSubTab == "DMC") {
                    Box(Modifier.weight(1f)) { DropdownSelector(value = selectedExamName, options = examOptions, modifier = Modifier.fillMaxWidth().height(52.dp), isDark = isDark) { selectedExamName = it } }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                
                if (selectedSubTab != "Performance" && selectedPrintType == "Single Student") {
                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        placeholder = { Text("Roll No", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                Button(
                    onClick = {
                        val cid = classes.find { it.name == selectedClassName }?.id ?: 0
                        val sid = classes.find { it.name == selectedClassName }?.sections?.find { it.name == selectedSectionName }?.id ?: 0
                        
                        if (cid == 0 || sid == 0) {
                            android.widget.Toast.makeText(context, "Select Class and Section", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val yearNum = selectedSession.replace("Session ", "")
                        val fetchRoll = if (selectedPrintType == "Single Student" && rollNumber.isNotBlank()) rollNumber else null

                        isLoading = true
                        if (selectedSubTab == "DMC") {
                            if (selectedExamName == "Select Exam") {
                                android.widget.Toast.makeText(context, "Select Exam", android.widget.Toast.LENGTH_SHORT).show()
                                isLoading = false
                                return@Button
                            }
                            val eType = selectedExamName.split(" - ")[0]
                            viewModel.getConsolidatedResult(cid, sid, eType, yearNum, fetchRoll, onSuccess = {
                                generatedResults = it.data
                                isLoading = false
                            }, onError = {
                                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                                isLoading = false
                            })
                        } else if (selectedSubTab == "Result Card") {
                            viewModel.getFullResultCard(cid, sid, yearNum, fetchRoll, onSuccess = {
                                generatedFullResults = it.data
                                isLoading = false
                            }, onError = {
                                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                                isLoading = false
                            })
                        } else if (selectedSubTab == "Performance") {
                            if (performanceSubTab == "school_toppers" || performanceSubTab == "class_toppers") {
                                viewModel.getTopperAnalytics(performanceSubTab, cid, sid, onSuccess = {
                                    topperResults = it.data
                                    isLoading = false
                                }, onError = {
                                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                })
                            } else if (performanceSubTab == "school_performance") {
                                viewModel.getSchoolAnalytics(cid, sid, onSuccess = {
                                    schoolStatsResult = it.data
                                    isLoading = false
                                }, onError = {
                                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                })
                            } else if (performanceSubTab == "staff_performance") {
                                viewModel.getStaffAnalytics(cid, sid, onSuccess = {
                                    staffStatsResult = it.data
                                    isLoading = false
                                }, onError = {
                                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                })
                            } else if (performanceSubTab == "trends") {
                                viewModel.getTrendAnalytics(cid, sid, onSuccess = {
                                    trendStatsResult = it.data
                                    isLoading = false
                                }, onError = {
                                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                })
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369a1)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("GENERATE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }

                Button(
                    onClick = {
                        val cid = classes.find { it.name == selectedClassName }?.id ?: 0
                        val sid = classes.find { it.name == selectedClassName }?.sections?.find { it.name == selectedSectionName }?.id ?: 0
                        
                        if (cid == 0 || sid == 0) {
                            android.widget.Toast.makeText(context, "Select Class and Section to Print All", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val yearNum = selectedSession.replace("Session ", "")

                        isLoading = true
                        if (selectedSubTab == "DMC") {
                            if (selectedExamName == "Select Exam") {
                                android.widget.Toast.makeText(context, "Select Exam", android.widget.Toast.LENGTH_SHORT).show()
                                isLoading = false
                                return@Button
                            }
                            val eType = selectedExamName.split(" - ")[0]
                            viewModel.getConsolidatedResult(cid, sid, eType, yearNum, null, onSuccess = { results ->
                                isLoading = false
                                if(results.data.isEmpty()) {
                                    android.widget.Toast.makeText(context, "No results found to print.", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    printNativeDMCs(context, results.data)
                                }
                            }, onError = { err ->
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                isLoading = false
                            })
                        } else if (selectedSubTab == "Result Card") {
                            viewModel.getFullResultCard(cid, sid, yearNum, null, onSuccess = { results ->
                                isLoading = false
                                if(results.data.isEmpty()) {
                                    android.widget.Toast.makeText(context, "No results found to print.", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    printNativeFullResultCards(context, results.data)
                                }
                            }, onError = { err ->
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                isLoading = false
                            })
                        } else if (selectedSubTab == "Performance") {
                            android.widget.Toast.makeText(context, "Performance output is displayed directly under Generate.", android.widget.Toast.LENGTH_SHORT).show()
                            isLoading = false
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    border = BorderStroke(1.dp, Color.Transparent)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PRINT ALL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // Settings Box
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B).copy(alpha=0.5f), RoundedCornerShape(8.dp)).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("BASE TEMPLATE", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(4.dp))
                    DropdownSelector(value = selectedTemplate, options = listOf("Prestige Royal (Elite)", "Standard (Classic)"), modifier = Modifier.fillMaxWidth().height(52.dp), isDark = isDark) { selectedTemplate = it }
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatPaint, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ACCENT THEME", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(4.dp))
                    DropdownSelector(value = selectedTheme, options = listOf("Royal Blue (Standard)", "Burgundy", "Emerald"), modifier = Modifier.fillMaxWidth().height(52.dp), isDark = isDark) { selectedTheme = it }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("WATERMARK", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(4.dp))
                    DropdownSelector(value = selectedWatermark, options = listOf("Enabled (Institutional)", "Disabled"), modifier = Modifier.fillMaxWidth().height(52.dp), isDark = isDark) { selectedWatermark = it }
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SIGNATURES", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(4.dp))
                    DropdownSelector(value = selectedSigs, options = listOf("Triple Verified (3)", "Single"), modifier = Modifier.fillMaxWidth().height(52.dp), isDark = isDark) { selectedSigs = it }
                }
            }
        }
    }
}

@Composable
fun DmcCardNative(res: com.example.wantuch.domain.model.ConsolidatedStudentResult) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // App Header: Mobile Flow (Vertical)
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0xFF1D4ED8), RoundedCornerShape(bottomEnd = 32.dp, topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp))
                .padding(14.dp)
        ) {
            Text("RANA COLLEGE AND SCHOOL SYSTEM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("OFFICIAL ACADEMIC TRANSCRIPT", color = Color.White.copy(0.9f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(12.dp))
        
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            Text("DETAILED MARKS CERTIFICATE", color = Color(0xFF1D4ED8), fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text("FIRST TERM ASSESSMENT | 2026", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(20.dp))

        // Student Info & Propic
        val picUrl = if (!res.profile_pic.isNullOrEmpty() && res.profile_pic != "user.png") "https://www.wantuch.pk/uploads/students/${res.profile_pic}" else "https://ui-avatars.com/api/?name=${res.full_name ?: "S"}&background=f1f5f9&color=cbd5e1"
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("STUDENT NAME", fontSize = 8.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Black)
                        Text(res.full_name ?: "-", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("FATHER NAME", fontSize = 8.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Black)
                        Text(res.father_name ?: "-", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("ROLL NUMBER", fontSize = 8.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Black)
                        Text(res.roll_number ?: "-", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("CLASS", fontSize = 8.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Black)
                        Text(res.class_no ?: "-", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = picUrl, 
                contentDescription = null, 
                modifier = Modifier.size(54.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        Spacer(Modifier.height(20.dp))

        // Subjects Table
        val cyanBorder = Color(0xFF0EA5E9)
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().border(1.dp, Color.Transparent).drawBehind {
                drawLine(cyanBorder, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = 4f)
                drawLine(cyanBorder, start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 4f)
            }.padding(vertical = 8.dp)) {
                Text("SUBJECT", Modifier.weight(2f), color = cyanBorder, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text("MAX", Modifier.weight(1f), color = cyanBorder, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("OBT", Modifier.weight(1f), color = cyanBorder, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("GRADE", Modifier.weight(1f), color = cyanBorder, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("%", Modifier.weight(1f), color = cyanBorder, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
            
            res.subjects.forEachIndexed { index, sub ->
                val sColor = if (sub.status.equals("Pass", true)) Color(0xFF10B981) else Color(0xFFEF4444)
                val grade = if (sub.status.equals("Pass", true)) "P" else "F"
                val subPerc = if (sub.total > 0) ((sub.obtained?.toString()?.toDoubleOrNull() ?: 0.0) / sub.total * 100).toInt() else 0
                
                Row(Modifier.fillMaxWidth().drawBehind { drawLine(Color(0xFFF8FAFC), start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 2f) }.padding(vertical = 10.dp)) {
                    Text(sub.subject, Modifier.weight(2f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(sub.total.toInt().toString(), Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(sub.obtained?.toString() ?: "0", Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(grade, Modifier.weight(1f), color = sColor, fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("$subPerc%", Modifier.weight(1f), color = sColor, fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }
            
            val oColor = if (res.final_status?.equals("PASS", true) == true) Color(0xFF10B981) else Color(0xFFEF4444)
            val oGrade = if (res.final_status?.equals("PASS", true) == true) "P" else "F"
            
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text("All Subjects", Modifier.weight(2f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(res.total_max.toInt().toString(), Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(res.total_obtain?.toString() ?: "0", Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(oGrade, Modifier.weight(1f), color = oColor, fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("${res.percentage}%", Modifier.weight(1f), color = oColor, fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Stats grid - Wrapping rows
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val oColor = if (res.final_status?.equals("PASS", true) == true) Color(0xFF10B981) else Color(0xFFEF4444)
            Box(Modifier.weight(1f).border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).padding(6.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("TOTAL OBT", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(2.dp))
                    Text("${res.total_obtain ?: "0"} / ${res.total_max.toInt()}", color = Color(0xFF1E293B), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Box(Modifier.weight(1f).border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).padding(6.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("PERCENTAGE", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(2.dp))
                    Text("${res.percentage}%", color = Color(0xFF1E293B), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Box(Modifier.weight(1f).border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).padding(6.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("RESULT", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(2.dp))
                    Text("${res.final_status?.uppercase() ?: "UNKNOWN"}", color = oColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
             Text("This is a computer generated certificate. The authenticity can be verified by scanning the QR code or visiting the official portal. Any tampering with this document is a punishable offense.", fontSize = 7.sp, color = Color(0xFF9CA3AF), lineHeight = 10.sp, modifier = Modifier.weight(1f))
             Spacer(Modifier.width(12.dp))
             Box(Modifier.size(36.dp).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                 Text("QR", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
             }
        }
    }
}

fun printNativeDMCs(context: android.content.Context, results: List<com.example.wantuch.domain.model.ConsolidatedStudentResult>) {
    val webView = android.webkit.WebView(context)
    val htmlBuilder = StringBuilder()
    htmlBuilder.append("<html><head><style>")
    htmlBuilder.append("body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; padding: 0; margin: 0; background-color: white; -webkit-print-color-adjust: exact; }")
    htmlBuilder.append(".dmc { border: none; padding: 20px; margin: 0 auto; margin-bottom: 40px; page-break-after: always; max-width: 800px; }")
    htmlBuilder.append(".header-box { display: flex; justify-content: space-between; margin-bottom: 25px; }")
    htmlBuilder.append(".header-left { background-color: #1d4ed8; color: white; padding: 20px; border-bottom-right-radius: 40px; width: 60%; }")
    htmlBuilder.append(".header-left h2 { margin: 0; font-size: 16px; font-weight: 900; letter-spacing: 0.5px; }")
    htmlBuilder.append(".header-left p { margin: 5px 0 0 0; font-size: 9px; font-weight: bold; opacity: 0.9; }")
    htmlBuilder.append(".header-right { text-align: right; width: 35%; padding-top: 5px; }")
    htmlBuilder.append(".header-right h1 { color: #1d4ed8; font-size: 16px; font-weight: 900; margin: 0; line-height: 1.2; text-transform: uppercase; }")
    htmlBuilder.append(".header-right p { margin: 5px 0 0 0; font-size: 8px; color: #64748b; font-weight: bold; }")
    htmlBuilder.append(".student-grid { display: flex; justify-content: space-between; margin-bottom: 30px; }")
    htmlBuilder.append(".student-info { width: 70%; display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }")
    htmlBuilder.append(".info-block { margin-bottom: 5px; }")
    htmlBuilder.append(".info-label { font-size: 9px; color: #cbd5e1; font-weight: 900; text-transform: uppercase; margin-bottom: 2px; }")
    htmlBuilder.append(".info-value { font-size: 12px; font-weight: bold; color: #1e293b; text-transform: uppercase; }")
    htmlBuilder.append(".profile-pic { width: 80px; height: 80px; background-color: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 8px; float: right; object-fit: cover; }")
    htmlBuilder.append("table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }")
    htmlBuilder.append("th { color: #0ea5e9; font-size: 10px; text-transform: uppercase; font-weight: 900; text-align: center; padding: 12px 5px; border-top: 2px solid #0ea5e9; border-bottom: 2px solid #0ea5e9; }")
    htmlBuilder.append("th:first-child { text-align: left; }")
    htmlBuilder.append("td { padding: 12px 5px; font-size: 11px; text-align: center; border-bottom: 1px solid #f8fafc; color: #1e293b; font-weight: 600; }")
    htmlBuilder.append("td:first-child { text-align: left; font-weight: 800; }")
    htmlBuilder.append(".stats-grid { display: flex; justify-content: space-between; margin-bottom: 60px; }")
    htmlBuilder.append(".stat-box { background-color: #ffffff; border: 1px solid #f1f5f9; border-radius: 12px; padding: 15px 10px; text-align: center; width: 22%; box-shadow: 0 4px 6px rgba(0,0,0,0.02); }")
    htmlBuilder.append(".stat-label { font-size: 9px; font-weight: 900; color: #64748b; margin-bottom: 8px; text-transform: uppercase; }")
    htmlBuilder.append(".stat-value { font-size: 16px; font-weight: 900; color: #1e293b; }")
    htmlBuilder.append(".footer-row { display: flex; justify-content: space-between; align-items: flex-end; }")
    htmlBuilder.append(".disclaimer { font-size: 8px; color: #9ca3af; line-height: 1.5; width: 80%; }")
    htmlBuilder.append(".qr-code { width: 40px; height: 40px; background-color: white; border: 1px solid #000; display: flex; justify-content: center; align-items: center; font-size: 6px; }")
    htmlBuilder.append("</style></head><body>")
    
    for (res in results) {
       htmlBuilder.append("<div class='dmc'>")
       
       htmlBuilder.append("<div class='header-box'>")
       htmlBuilder.append("<div class='header-left'>")
       htmlBuilder.append("<h2>RANA COLLEGE AND SCHOOL SYSTEM, KHWAZA KHELA SWAT</h2>")
       htmlBuilder.append("<p>OFFICIAL ACADEMIC TRANSCRIPT</p>")
       htmlBuilder.append("</div>")
       htmlBuilder.append("<div class='header-right'>")
       htmlBuilder.append("<h1>DETAILED<br/>MARKS<br/>CERTIFICATE</h1>")
       htmlBuilder.append("<p>FIRST TERM ASSESSMENT | 2026</p>")
       htmlBuilder.append("</div>")
       htmlBuilder.append("</div>")
       
       val picUrl = if (!res.profile_pic.isNullOrEmpty() && res.profile_pic != "user.png") res.profile_pic else "https://ui-avatars.com/api/?name=${res.full_name ?: "S"}&background=f1f5f9&color=cbd5e1"
       
       htmlBuilder.append("<div class='student-grid'>")
       htmlBuilder.append("<div class='student-info'>")
       
       htmlBuilder.append("<div class='info-block'><div class='info-label'>STUDENT NAME</div><div class='info-value'>${res.full_name ?: "-"}</div></div>")
       htmlBuilder.append("<div class='info-block'><div class='info-label'>FATHER NAME</div><div class='info-value'>${res.father_name ?: "-"}</div></div>")
       htmlBuilder.append("<div class='info-block'><div class='info-label'>ROLL NUMBER</div><div class='info-value'>${res.roll_number ?: "-"}</div></div>")
       htmlBuilder.append("<div class='info-block'><div class='info-label'>CLASS NAME</div><div class='info-value'>${res.class_no ?: "-"}</div></div>")
       
       htmlBuilder.append("</div>")
       htmlBuilder.append("<div style='width: 25%;'><img src='$picUrl' class='profile-pic' onerror=\"this.style.display='none'\"></div>")
       htmlBuilder.append("</div>")
       
       htmlBuilder.append("<table><tr><th style='text-align:left;'>SUBJECT NAME</th><th>MAX MARKS</th><th>OBTAINED</th><th>GRADE</th><th>PERCENTAGE</th></tr>")
       for (sub in res.subjects) {
           val statusColor = if (sub.status.equals("Pass", true)) "#10B981" else "#EF4444"
           val grade = if (sub.status.equals("Pass", true)) "P" else "F"
           val subPerc = if (sub.total > 0) ((sub.obtained?.toString()?.toDoubleOrNull() ?: 0.0) / sub.total * 100).toInt() else 0
           
           htmlBuilder.append("<tr>")
           htmlBuilder.append("<td style='text-align:left; font-weight:800;'>${sub.subject}</td>")
           htmlBuilder.append("<td>${sub.total.toInt()}</td>")
           htmlBuilder.append("<td>${sub.obtained ?: "0"}</td>")
           htmlBuilder.append("<td style='color:$statusColor; font-weight:900;'>$grade</td>")
           htmlBuilder.append("<td style='color:$statusColor; font-weight:900;'>$subPerc%</td>")
           htmlBuilder.append("</tr>")
       }
       htmlBuilder.append("<tr>")
       htmlBuilder.append("<td style='text-align:left; font-weight:800; border-bottom:none;'>All Subjects</td>")
       htmlBuilder.append("<td style='border-bottom:none;'>${res.total_max.toInt()}</td>")
       htmlBuilder.append("<td style='border-bottom:none;'>${res.total_obtain ?: "0"}</td>")
       val oGrade = if (res.final_status.equals("PASS", true)) "P" else "F"
       val oColor = if (res.final_status.equals("PASS", true)) "#10B981" else "#EF4444"
       htmlBuilder.append("<td style='color:$oColor; font-weight:900; border-bottom:none;'>$oGrade</td>")
       htmlBuilder.append("<td style='color:$oColor; font-weight:900; border-bottom:none;'>${res.percentage}%</td>")
       htmlBuilder.append("</tr>")
       htmlBuilder.append("</table>")
       
       htmlBuilder.append("<div class='stats-grid'>")
       
       htmlBuilder.append("<div class='stat-box'>")
       htmlBuilder.append("<div class='stat-label'>TOTAL OBTAINED</div>")
       htmlBuilder.append("<div class='stat-value'>${res.total_obtain ?: "0"} / ${res.total_max.toInt()}</div>")
       htmlBuilder.append("</div>")
       
       htmlBuilder.append("<div class='stat-box'>")
       htmlBuilder.append("<div class='stat-label'>PERCENTAGE</div>")
       htmlBuilder.append("<div class='stat-value'>${res.percentage}%</div>")
       htmlBuilder.append("</div>")
       
       htmlBuilder.append("<div class='stat-box'>")
       htmlBuilder.append("<div class='stat-label'>RESULT STATUS</div>")
       htmlBuilder.append("<div class='stat-value' style='color:$oColor;'>${res.final_status?.uppercase() ?: "UNKNOWN"}</div>")
       htmlBuilder.append("</div>")
       
       htmlBuilder.append("<div class='stat-box'>")
       htmlBuilder.append("<div class='stat-label'>VERIFICATION</div>")
       htmlBuilder.append("<div class='stat-value' style='font-size: 11px; margin-top: 4px;'>AUTHENTIC COPY</div>")
       htmlBuilder.append("</div>")
       
       htmlBuilder.append("</div>")
       
       htmlBuilder.append("<div class='footer-row'>")
       htmlBuilder.append("<div class='disclaimer'>This is a computer generated certificate. The authenticity can be verified by scanning the QR code or visiting the official portal. Any tampering with this document is a punishable offense. Passing percentage for each subject is strictly enforced.</div>")
       htmlBuilder.append("<div class='qr-code'>QR CODE</div>")
       htmlBuilder.append("</div>")

       htmlBuilder.append("</div>")
    }
    
    htmlBuilder.append("</body></html>")
    
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView, url: String) {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val jobName = "WANTUCH_DMC_Result_${System.currentTimeMillis()}"
            printManager.print(jobName, view.createPrintDocumentAdapter(jobName), android.print.PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, htmlBuilder.toString(), "text/HTML", "UTF-8", null)
}

@Composable
fun FullResultCardNative(res: com.example.wantuch.domain.model.FullResultStudent) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0xFF0369a1), RoundedCornerShape(bottomEnd = 32.dp, topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp))
                .padding(14.dp)
        ) {
            Text("RANA COLLEGE AND SCHOOL SYSTEM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("COMPREHENSIVE RESULT CARD", color = Color.White.copy(0.9f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(12.dp))
        
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            Text("ANNUAL RESULT CARD", color = Color(0xFF0369a1), fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text("SESSION 2026", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(20.dp))

        // Student Info & Propic
        val picUrl = if (!res.profile_pic.isNullOrEmpty() && res.profile_pic != "user.png") "https://www.wantuch.pk/uploads/students/${res.profile_pic}" else "https://ui-avatars.com/api/?name=${res.full_name ?: "S"}&background=f1f5f9&color=cbd5e1"
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("STUDENT NAME", fontSize = 8.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Black)
                        Text(res.full_name ?: "-", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("FATHER NAME", fontSize = 8.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Black)
                        Text(res.father_name ?: "-", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("ROLL NUMBER", fontSize = 8.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Black)
                        Text(res.roll_number ?: "-", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("CLASS", fontSize = 8.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Black)
                        Text(res.class_no ?: "-", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = picUrl, 
                contentDescription = null, 
                modifier = Modifier.size(54.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        Spacer(Modifier.height(20.dp))

        // Terms Table
        res.report.forEach { term ->
            val cyanBorder = Color(0xFF0369a1)
            Row(Modifier.fillMaxWidth().background(cyanBorder).padding(6.dp)) {
                Text(term.exam_type.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("SUBJECT", Modifier.weight(2f), color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text("MAX", Modifier.weight(1f), color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("OBT", Modifier.weight(1f), color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
                
                term.subjects.forEach { sub ->
                    Row(Modifier.fillMaxWidth().drawBehind { drawLine(Color(0xFFF8FAFC), start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 2f) }.padding(vertical = 6.dp)) {
                        Text(sub.subject, Modifier.weight(2f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(sub.total.toInt().toString(), Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(sub.obtained?.toString() ?: "0", Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
                
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text("Total", Modifier.weight(2f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(term.total_max.toInt().toString(), Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(term.total_obtain?.toString() ?: "0", Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
             Text("This is a computer generated certificate. The authenticity can be verified by scanning the QR code or visiting the official portal. Any tampering with this document is a punishable offense.", fontSize = 7.sp, color = Color(0xFF9CA3AF), lineHeight = 10.sp, modifier = Modifier.weight(1f))
             Spacer(Modifier.width(12.dp))
             Box(Modifier.size(36.dp).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                 Text("QR", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
             }
        }
    }
}

fun printNativeFullResultCards(context: android.content.Context, results: List<com.example.wantuch.domain.model.FullResultStudent>) {
    val webView = android.webkit.WebView(context)
    val htmlBuilder = StringBuilder()
    htmlBuilder.append("<html><head><style>")
    htmlBuilder.append("body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; padding: 0; margin: 0; background-color: white; -webkit-print-color-adjust: exact; }")
    htmlBuilder.append(".dmc { border: none; padding: 20px; margin: 0 auto; margin-bottom: 40px; page-break-after: always; max-width: 800px; }")
    htmlBuilder.append(".header-box { display: flex; justify-content: space-between; margin-bottom: 25px; }")
    htmlBuilder.append(".header-left { background-color: #0369a1; color: white; padding: 20px; border-bottom-right-radius: 40px; width: 60%; }")
    htmlBuilder.append(".header-left h2 { margin: 0; font-size: 16px; font-weight: 900; letter-spacing: 0.5px; }")
    htmlBuilder.append(".header-left p { margin: 5px 0 0 0; font-size: 9px; font-weight: bold; opacity: 0.9; }")
    htmlBuilder.append(".header-right { text-align: right; width: 35%; padding-top: 5px; }")
    htmlBuilder.append(".header-right h1 { color: #0369a1; font-size: 16px; font-weight: 900; margin: 0; line-height: 1.2; text-transform: uppercase; }")
    htmlBuilder.append(".header-right p { margin: 5px 0 0 0; font-size: 8px; color: #64748b; font-weight: bold; }")
    htmlBuilder.append(".student-grid { display: flex; justify-content: space-between; margin-bottom: 30px; }")
    htmlBuilder.append(".student-info { width: 70%; display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }")
    htmlBuilder.append(".info-block { margin-bottom: 5px; }")
    htmlBuilder.append(".info-label { font-size: 9px; color: #cbd5e1; font-weight: 900; text-transform: uppercase; margin-bottom: 2px; }")
    htmlBuilder.append(".info-value { font-size: 12px; font-weight: bold; color: #1e293b; text-transform: uppercase; }")
    htmlBuilder.append(".profile-pic { width: 80px; height: 80px; background-color: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 8px; float: right; object-fit: cover; }")
    htmlBuilder.append("table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }")
    htmlBuilder.append(".term-header th { background-color: #0369a1; color: white; border: none; padding: 8px 5px; font-size: 11px; text-align: left; }")
    htmlBuilder.append("th { color: #64748b; font-size: 10px; text-transform: uppercase; font-weight: 900; text-align: center; padding: 8px 5px; border-bottom: 2px solid #e2e8f0; }")
    htmlBuilder.append("th:first-child { text-align: left; }")
    htmlBuilder.append("td { padding: 8px 5px; font-size: 11px; text-align: center; border-bottom: 1px solid #f8fafc; color: #1e293b; font-weight: 600; }")
    htmlBuilder.append("td:first-child { text-align: left; font-weight: 800; }")
    htmlBuilder.append(".footer-row { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 30px; }")
    htmlBuilder.append(".disclaimer { font-size: 8px; color: #9ca3af; line-height: 1.5; width: 80%; }")
    htmlBuilder.append(".qr-code { width: 40px; height: 40px; background-color: white; border: 1px solid #000; display: flex; justify-content: center; align-items: center; font-size: 6px; }")
    htmlBuilder.append("</style></head><body>")
    
    for (res in results) {
       htmlBuilder.append("<div class='dmc'>")
       
       htmlBuilder.append("<div class='header-box'>")
       htmlBuilder.append("<div class='header-left'>")
       htmlBuilder.append("<h2>RANA COLLEGE AND SCHOOL SYSTEM, KHWAZA KHELA SWAT</h2>")
       htmlBuilder.append("<p>COMPREHENSIVE RESULT CARD</p>")
       htmlBuilder.append("</div>")
       htmlBuilder.append("<div class='header-right'>")
       htmlBuilder.append("<h1>ANNUAL<br/>RESULT<br/>CARD</h1>")
       htmlBuilder.append("<p>SESSION 2026</p>")
       htmlBuilder.append("</div>")
       htmlBuilder.append("</div>")
       
       val picUrl = if (!res.profile_pic.isNullOrEmpty() && res.profile_pic != "user.png") res.profile_pic else "https://ui-avatars.com/api/?name=${res.full_name ?: "S"}&background=f1f5f9&color=cbd5e1"
       
       htmlBuilder.append("<div class='student-grid'>")
       htmlBuilder.append("<div class='student-info'>")
       htmlBuilder.append("<div class='info-block'><div class='info-label'>STUDENT NAME</div><div class='info-value'>${res.full_name ?: "-"}</div></div>")
       htmlBuilder.append("<div class='info-block'><div class='info-label'>FATHER NAME</div><div class='info-value'>${res.father_name ?: "-"}</div></div>")
       htmlBuilder.append("<div class='info-block'><div class='info-label'>ROLL NUMBER</div><div class='info-value'>${res.roll_number ?: "-"}</div></div>")
       htmlBuilder.append("<div class='info-block'><div class='info-label'>CLASS NAME</div><div class='info-value'>${res.class_no ?: "-"}</div></div>")
       htmlBuilder.append("</div>")
       htmlBuilder.append("<div style='width: 25%;'><img src='$picUrl' class='profile-pic' onerror=\"this.style.display='none'\"></div>")
       htmlBuilder.append("</div>")
       
       for (term in res.report) {
           htmlBuilder.append("<table>")
           htmlBuilder.append("<tr class='term-header'><th colspan='3'>${term.exam_type.uppercase()}</th></tr>")
           htmlBuilder.append("<tr><th style='text-align:left;'>SUBJECT NAME</th><th>MAX MARKS</th><th>OBTAINED</th></tr>")
           for (sub in term.subjects) {
               htmlBuilder.append("<tr>")
               htmlBuilder.append("<td style='text-align:left; font-weight:800;'>${sub.subject}</td>")
               htmlBuilder.append("<td>${sub.total.toInt()}</td>")
               htmlBuilder.append("<td>${sub.obtained ?: "0"}</td>")
               htmlBuilder.append("</tr>")
           }
           htmlBuilder.append("<tr>")
           htmlBuilder.append("<td style='text-align:left; font-weight:900;'>Total</td>")
           htmlBuilder.append("<td style='font-weight:900;'>${term.total_max.toInt()}</td>")
           htmlBuilder.append("<td style='font-weight:900;'>${term.total_obtain ?: "0"}</td>")
           htmlBuilder.append("</tr>")
           htmlBuilder.append("</table>")
       }
       
       htmlBuilder.append("<div class='footer-row'>")
       htmlBuilder.append("<div class='disclaimer'>This is a computer generated certificate. The authenticity can be verified by scanning the QR code or visiting the official portal. Any tampering with this document is a punishable offense.</div>")
       htmlBuilder.append("<div class='qr-code'>QR CODE</div>")
       htmlBuilder.append("</div>")

       htmlBuilder.append("</div>")
    }
    
    htmlBuilder.append("</body></html>")
    
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView, url: String) {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val jobName = "WANTUCH_FullResult_${System.currentTimeMillis()}"
            printManager.print(jobName, view.createPrintDocumentAdapter(jobName), android.print.PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, htmlBuilder.toString(), "text/HTML", "UTF-8", null)
}

@Composable
fun TopperCardNative(res: List<com.example.wantuch.domain.model.TopperStats>, isSchoolLevel: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2C52A0), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isSchoolLevel) Icons.Default.EmojiEvents else Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFF7C600), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isSchoolLevel) "INSTITUTIONAL TOPPERS" else "CLASS RANKING LIST", color = Color(0xFFF7C600), fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text("TOP PERFORMERS", color = Color.White.copy(0.8f), fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
        }
        
        if (res.isEmpty()) {
            Text("No records found for this period.", modifier = Modifier.fillMaxWidth().padding(40.dp), textAlign = TextAlign.Center, color = Color(0xFF64748B))
        } else {
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))) {
                res.forEachIndexed { index, st ->
                    val bgColor = if (index % 2 == 0) Color(0xFFFCFBF2) else Color(0xFFEEFCF5)
                    Row(modifier = Modifier.fillMaxWidth().background(bgColor).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${index + 1}", color = Color(0xFFF1C40F), fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.width(45.dp))
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(st.full_name, color = Color(0xFF1E293B), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("Roll: ${st.roll_number ?: "null"} | CNO: ${st.class_no ?: "null"}", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        if (isSchoolLevel) {
                            Text(st.class_name ?: "-", color = Color(0xFF334155), fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        } else {
                            Text("-", color = Color(0xFF334155), fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                            Text("${st.percentage ?: "0"}%", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text("AGGREGATE", color = Color(0xFF94A3B8), fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SchoolStatsCardNative(res: com.example.wantuch.domain.model.SchoolStats) {
    Column(
        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF2C52A0), RoundedCornerShape(8.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOTAL STUDENTS", color = Color.White.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(res.total_students ?: "0", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF10B981), RoundedCornerShape(8.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PASSED ENTRIES", color = Color.White.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(res.passed_entries ?: "0", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFEF4444), RoundedCornerShape(8.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FAILED ENTRIES", color = Color.White.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(res.failed_entries ?: "0", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF7C600), RoundedCornerShape(8.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("INSTITUTIONAL EFFICIENCY", color = Color(0xFF0F172A).copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("${res.pass_rate}%", color = Color(0xFF0F172A), fontSize = 36.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun StaffStatsCardNative(res: List<com.example.wantuch.domain.model.StaffStats>) {
    Column(
        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if(res.isEmpty()) {
            Text("No staff data available.", color = Color.Gray, modifier = Modifier.padding(16.dp))
        } else {
            res.forEach { entry ->
                Column(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(20.dp)) {
                    Text(entry.class_name, color = Color(0xFF2C52A0), fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).drawBehind {
                        drawLine(Color(0xFFE2E8F0), start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 3f)
                    })
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Students:", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(entry.total_students ?: "0", color = Color(0xFF1E293B), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Passed Entries:", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(entry.passed_entries ?: "0", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Failed Entries:", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(entry.failed_entries ?: "0", color = Color(0xFFEF4444), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("EFFICIENCY RATE", color = Color(0xFF94A3B8), fontWeight = FontWeight.Black, fontSize = 10.sp)
                        Text("${entry.pass_rate}%", color = Color(0xFF2C52A0), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))) {
                        Box(Modifier.fillMaxWidth((entry.pass_rate.toFloat() / 100).coerceIn(0f, 1f)).height(8.dp).background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB))), RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
    }
}

@Composable
fun TrendStatsCardNative(res: List<com.example.wantuch.domain.model.TrendStats>) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)).padding(15.dp)
    ) {
        Text("SUBJECT PERFORMANCE TRENDS", color = Color(0xFF1D4ED8), fontWeight = FontWeight.Black, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        if (res.isEmpty()) {
            Text("No trend data available.", color = Color.Gray, modifier = Modifier.padding(16.dp))
        } else {
            res.forEach { trend ->
                Text(trend.exam_type, color = Color(0xFF334155), fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                trend.subject_performance.forEach { sub ->
                    val avgPerc = if (sub.total != null && sub.total != "0") (sub.average / sub.total.toDouble()) * 100 else 0.0
                    val sColor = if (avgPerc >= 80) Color(0xFF2ECC71) else if (avgPerc >= 60) Color(0xFFF1C40F) else Color(0xFFE74C3C)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sub.subject, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Box(Modifier.weight(2f).height(12.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))) {
                            Box(Modifier.fillMaxWidth((avgPerc / 100).toFloat().coerceIn(0f, 1f)).height(12.dp).background(sColor, RoundedCornerShape(6.dp)))
                        }
                        Text("${avgPerc.toInt()}%", color = sColor, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp).width(30.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}
