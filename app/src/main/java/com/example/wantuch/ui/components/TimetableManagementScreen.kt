package com.example.wantuch.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ManagementTab(val label: String, val icon: ImageVector) {
    EXISTING("Existing Timetable", Icons.Default.Assignment),
    CREATE("CREATE", Icons.Default.Add),
    AUTO("AUTO", Icons.Default.SmartToy),
    BULK("BULK", Icons.Default.Layers)
}

@Composable
fun TimetableManagementScreen(
    viewModel: WantuchViewModel,
    initialTab: ManagementTab = ManagementTab.EXISTING,
    onBack: () -> Unit
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val summary by viewModel.managementSummary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val bgColor    = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val textColor  = if (isDark) Color.White       else Color(0xFF1E293B)

    LaunchedEffect(Unit) { viewModel.fetchManagementSummary() }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = textColor)
                    }
                    Text(
                        initialTab.label,
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (initialTab) {
                ManagementTab.EXISTING -> TasksView(viewModel, summary, isLoading, isDark)
                ManagementTab.CREATE   -> CreateWizardView(viewModel, summary, isDark)
                ManagementTab.AUTO     -> AutoGeneratorView(viewModel, summary, isDark)
                ManagementTab.BULK     -> BulkToolView(viewModel, summary, isDark)
            }
        }
    }
}

@Composable
fun ManagementHeader(title: String, subtitle: String, textColor: Color = Color.White, labelColor: Color = Color.Gray) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(title, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = labelColor, fontSize = 12.sp)
    }
}

@Composable
fun TasksView(viewModel: WantuchViewModel, summary: com.example.wantuch.domain.model.TimetableManagementSummary?, isLoading: Boolean, isDark: Boolean = true) {
    val context = LocalContext.current
    val bgColor     = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor   = if (isDark) Color(0xFF1E293B) else Color.White
    val surfColor   = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textColor   = if (isDark) Color.White       else Color(0xFF1E293B)
    val labelColor  = if (isDark) Color.White.copy(0.55f) else Color(0xFF64748B)
    val headerBg    = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val rowBg       = if (isDark) Color(0xFF1E3A8A) else Color(0xFF1D4ED8)
    LaunchedEffect(Unit) { viewModel.fetchStaff() }

    val archiveData by viewModel.timetableArchive.collectAsState()
    
    var selectedView by remember { mutableStateOf("Summary View (Global)") }
    var selectedClassId by remember { mutableStateOf(0) }
    var selectedSectionId by remember { mutableStateOf(0) }
    var selectedVersionLabel by remember { mutableStateOf("MASTER") }
    var selectedVersionId by remember { mutableStateOf(0) }
    var selectedStaffFilter by remember { mutableStateOf("All Staff (Individual)") }
    var selectedStaffId by remember { mutableStateOf(0) }
    var selectedLevel by remember { mutableStateOf("ALL") }

    LaunchedEffect(selectedVersionId, selectedLevel, selectedStaffId, selectedClassId, selectedSectionId) {
        viewModel.fetchTimetableArchive(
            version = selectedVersionId, 
            level = selectedLevel, 
            staffId = selectedStaffId,
            classId = selectedClassId,
            sectionId = selectedSectionId
        )
    }

    if (isLoading && archiveData == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFF59E0B))
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth().background(cardColor.copy(0.7f)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            val classOptions = summary?.classes?.map { it.cname }?.distinct() ?: emptyList()
                            val viewOptions = listOf("Summary View (Global)") + classOptions
                            TimetableFieldMini(selectedView, viewOptions, isDark) { label ->
                                selectedView = label
                                if (label == "Summary View (Global)") { selectedClassId = 0; selectedSectionId = 0 }
                                else {
                                    val match = summary?.classes?.find { it.cname == label }
                                    selectedClassId = match?.cid ?: 0; selectedSectionId = 0
                                }
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            val staffList = (viewModel.staffData.value?.teaching_staff ?: emptyList()) + (viewModel.staffData.value?.non_teaching_staff ?: emptyList())
                            val staffOptions = listOf("All Staff (Individual)") + staffList.map { it.name }
                            TimetableFieldMini(selectedStaffFilter, staffOptions, isDark) { name ->
                                selectedStaffFilter = name
                                val matchedStaff = staffList.find { it.name == name }
                                selectedStaffId = when (val rawId = matchedStaff?.id) {
                                    is Number -> rawId.toInt(); is String -> rawId.toIntOrNull() ?: 0; else -> 0
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            val versionLabels = summary?.versions?.map { it.label } ?: listOf("MASTER")
                            TimetableFieldMini(selectedVersionLabel, versionLabels, isDark) { label ->
                                selectedVersionLabel = label
                                selectedVersionId = summary?.versions?.find { it.label == label }?.id ?: 0
                            }
                        }
                        Surface(color = if (isDark) Color.Black else Color(0xFF334155), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Article, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("B&W STYLE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("FILTER BY LEVEL:", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    listOf("ALL", "PRIMARY", "MIDDLE", "HIGHER").forEach { level ->
                        val isSelected = selectedLevel == level
                        Surface(
                            color = if (isSelected) Color(0xFF3B82F6) else surfColor,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedLevel = level }
                        ) {
                            Text(level, color = if (isSelected) Color.White else textColor, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                        }
                    }
                }
            }

            // Action Buttons Row
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val actionModifier = Modifier.weight(1f)
                    Surface(
                        color = Color(0xFF3B82F6), shape = RoundedCornerShape(8.dp), modifier = actionModifier,
                        onClick = { Toast.makeText(context, "Separate view coming soon", Toast.LENGTH_SHORT).show() }
                    ) {
                       Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                           Icon(Icons.Default.Layers, null, tint = Color.White, modifier = Modifier.size(14.dp))
                           Spacer(Modifier.width(6.dp))
                           Text("Separate", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                       }
                    }
                    Surface(
                        color = Color(0xFF10B981), shape = RoundedCornerShape(8.dp), modifier = actionModifier,
                        onClick = {
                            if (archiveData != null && !archiveData?.items.isNullOrEmpty()) {
                                Toast.makeText(context, "Preparing PDF...", Toast.LENGTH_SHORT).show()
                                printTimetablePdf(context, archiveData!!, selectedLevel, selectedVersionLabel)
                            } else {
                                Toast.makeText(context, "No timetable data available", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                       Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                           Icon(Icons.Default.Print, null, tint = Color.White, modifier = Modifier.size(14.dp))
                           Spacer(Modifier.width(6.dp))
                           Text("Print", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                       }
                    }
                    Surface(
                        color = Color(0xFFF59E0B), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1.5f),
                        onClick = { Toast.makeText(context, "Custom options coming soon", Toast.LENGTH_SHORT).show() }
                    ) {
                       Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                           Icon(Icons.Default.CheckBox, null, tint = Color.White, modifier = Modifier.size(14.dp))
                           Spacer(Modifier.width(6.dp))
                           Text("CUSTOM (ALL)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                       }
                    }
                }
            }

            item {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column {
                        Row(Modifier.fillMaxWidth().background(headerBg).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("OFFICIAL ACADEMIC ARCHIVE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                Text("| $selectedLevel", color = Color.White, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("MASTER TIMETABLE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Text(selectedVersionLabel, color = Color.White, fontSize = 8.sp)
                            }
                        }
                        
                        Row(Modifier.fillMaxWidth().background(rowBg)) {
                            TimetableHeaderCell("TIME", "DAY", Modifier.width(60.dp))
                            val refTimes = archiveData?.items?.filter { it.day_of_week?.equals("Monday", true) == true }
                                ?.sortedBy { try { SimpleDateFormat("hh:mm a", Locale.US).parse(it.start_time ?: "")?.time ?: 0L } catch (e: Exception) { 0L } }
                                ?.distinctBy { it.start_time }
                            repeat(7) { i ->
                                val bucketIdx = if (i > 4) i - 1 else i
                                val timeLabel = if (i == 4) "BRK" else refTimes?.getOrNull(bucketIdx)?.start_time ?: ""
                                TimetableColumnHeader("${i + 1}", timeLabel, Modifier.weight(1f))
                            }
                        }
                        val daysFull = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                        val dayLabelsShort = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
                        
                        daysFull.forEachIndexed { dayIdx, dayText ->
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).border(0.5.dp, labelColor.copy(0.15f))) {
                                Box(Modifier.width(60.dp).fillMaxHeight().background(rowBg).border(0.5.dp, Color.White.copy(0.08f)), contentAlignment = Alignment.Center) {
                                    Text(dayLabelsShort[dayIdx], color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                val dayItems = archiveData?.items?.filter { it.day_of_week?.equals(dayText, true) == true }
                                val dayBuckets = dayItems?.groupBy { it.start_time ?: "" }
                                    ?.toList()
                                    ?.sortedBy { (time, _) -> try { SimpleDateFormat("hh:mm a", Locale.US).parse(time)?.time ?: 0L } catch (e: Exception) { 0L } }
                                    ?.map { it.second }
                                repeat(7) { pIdx ->
                                    val isBreak = pIdx == 4
                                    val slotsAtThisPeriod = if (isBreak) emptyList() else {
                                        val bucketIdx = if (pIdx > 4) pIdx - 1 else pIdx
                                        dayBuckets?.getOrNull(bucketIdx) ?: emptyList()
                                    }
                                    Box(Modifier.weight(1f).heightIn(min = 40.dp).border(0.5.dp, labelColor.copy(0.08f)), contentAlignment = Alignment.Center) {
                                        if (isBreak) {
                                            Text("BRK", color = Color(0xFFEF4444).copy(0.6f), fontSize = 7.sp, fontWeight = FontWeight.Black)
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(2.dp), verticalArrangement = Arrangement.Center) {
                                                slotsAtThisPeriod.forEach { slot ->
                                                    Text(slot.sub_name ?: "", color = textColor, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    if (selectedClassId == 0) Text(slot.class_name ?: "", color = labelColor, fontSize = 6.sp, maxLines = 1)
                                                    Text(slot.teacher_name ?: "", color = Color(0xFF3B82F6), fontSize = 5.sp, maxLines = 1)
                                                    slot.id?.let { slotId ->
                                                        IconButton(onClick = { viewModel.deleteTimetableSlot(slotId) }, modifier = Modifier.size(16.dp)) {
                                                            Icon(Icons.Default.DeleteForever, null, tint = Color(0xFFEF4444).copy(0.7f), modifier = Modifier.size(10.dp))
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
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.35f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFF59E0B))
            }
        }
    }
}

@Composable
fun TimetableHeaderCell(label1: String, label2: String, modifier: Modifier) {
    Box(modifier.height(40.dp).border(0.5.dp, Color.White.copy(0.2f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
           Text(label1, color = Color.White, fontSize = 8.sp)
           Divider(color = Color.White.copy(0.2f), modifier = Modifier.width(20.dp))
           Text(label2, color = Color.White, fontSize = 8.sp)
        }
    }
}

@Composable
fun TimetableColumnHeader(pNo: String, time: String, modifier: Modifier) {
    Box(modifier.height(40.dp).border(0.5.dp, Color.White.copy(0.2f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(pNo, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(time, color = Color.White.copy(0.7f), fontSize = 7.sp)
        }
    }
}

@Composable
fun TimetableFieldMini(value: String, options: List<String>, isDark: Boolean = true, onSelect: (String) -> Unit) {
    val surfBg   = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textCol  = if (isDark) Color.White       else Color(0xFF1E293B)
    val menuBg   = if (isDark) Color(0xFF1E293B)  else Color.White
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            color = surfBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = textCol, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, null, tint = textCol.copy(0.5f), modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(menuBg)) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt, color = textCol, fontSize = 12.sp) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

@Composable
fun CreateWizardView(viewModel: WantuchViewModel, summary: com.example.wantuch.domain.model.TimetableManagementSummary?, isDark: Boolean = true) {
    val bgColor    = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor  = if (isDark) Color(0xFF1E293B) else Color.White
    val surfColor  = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textColor  = if (isDark) Color.White       else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.55f) else Color(0xFF64748B)

    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val wizardData by viewModel.wizardData.collectAsState()

    var timetableFor by remember { mutableStateOf("All (Global Default)") }
    var semester by remember { mutableStateOf("ANUAL SYSTEM") }
    var session by remember { mutableStateOf("2026") }
    var isAssembly by remember { mutableStateOf(false) }
    var isNazira by remember { mutableStateOf(false) }
    var totalPeriods by remember { mutableStateOf("8") }
    var breakAfter by remember { mutableStateOf("4") }
    var periodDur by remember { mutableStateOf("45") }
    var breakDur by remember { mutableStateOf("15") }

    var selectedStaffId   by remember { mutableStateOf<Int?>(null) }
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }
    var selectedSectionId by remember { mutableStateOf<Int?>(null) }
    var selectedClassId   by remember { mutableStateOf<Int?>(null) }
    var selectedDays      by remember { mutableStateOf(setOf<String>()) }
    var selectedPeriodNo  by remember { mutableStateOf("1") }
    var selectedStartTime by remember { mutableStateOf("08:00 AM") }
    var selectedEndTime   by remember { mutableStateOf("08:45 AM") }
    var selectedVersion   by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) { viewModel.fetchWizardData() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            WizardJumpItem("1. Settings", true) { scope.launch { scrollState.animateScrollToItem(0) } }
            WizardJumpItem("2. Staff", true)    { scope.launch { scrollState.animateScrollToItem(2) } }
            WizardJumpItem("3. Subject", true)  { scope.launch { scrollState.animateScrollToItem(4) } }
            WizardJumpItem("4. Finalize", true) { scope.launch { scrollState.animateScrollToItem(6) } }
        }
        Divider(color = labelColor.copy(0.1f))

        LazyColumn(Modifier.weight(1f), state = scrollState, contentPadding = PaddingValues(bottom = 120.dp)) {
            item { SectionHeader("1. INITIAL SETTINGS", Icons.Default.Settings, isDark) }
            item {
                WizardStepInitialSettings(
                    timetableFor, { timetableFor = it },
                    semester, { semester = it },
                    session, { session = it },
                    isAssembly, { isAssembly = it },
                    isNazira, { isNazira = it },
                    totalPeriods, { totalPeriods = it },
                    breakAfter, { breakAfter = it },
                    periodDur, { periodDur = it },
                    breakDur, { breakDur = it },
                    summary?.groups?.map { it.setting_value } ?: listOf("All (Global Default)")
                )
            }
            item { SectionHeader("2. SELECT STAFF", Icons.Default.People, isDark) }
            item { WizardStepStaff(wizardData?.staff ?: emptyList(), selectedStaffId, isDark) { selectedStaffId = it } }
            item { SectionHeader("3. SUBJECT & SCHEDULE", Icons.Default.MenuBook, isDark) }
            item { WizardStepSubject(wizardData?.subjects ?: emptyList(), selectedSubjectId, isDark) { selectedSubjectId = it } }
            item { SectionHeader("4. FINALIZE ASSIGNMENT", Icons.Default.AssignmentTurnedIn, isDark) }
            item {
                WizardStepFinalize(
                    classes      = summary?.classes ?: emptyList(),
                    selectedSectionId = selectedSectionId,
                    selectedDays      = selectedDays,
                    periodNo          = selectedPeriodNo,
                    startTime         = selectedStartTime,
                    endTime           = selectedEndTime,
                    onSectionSelect   = { sid, cid -> selectedSectionId = sid; selectedClassId = cid },
                    onDaysChange      = { selectedDays = it },
                    onPeriodNoChange  = { selectedPeriodNo = it },
                    onStartChange     = { selectedStartTime = it },
                    onEndChange       = { selectedEndTime = it }
                )
            }
        }

        Surface(Modifier.fillMaxWidth(), color = cardColor, tonalElevation = 12.dp) {
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val staffName = wizardData?.staff?.find { it.id == selectedStaffId }?.name ?: "—"
                val subName   = wizardData?.subjects?.find { it.id == selectedSubjectId }?.name ?: "—"
                val secName   = summary?.classes?.find { it.sid == selectedSectionId }?.let { "${it.cname} ${it.sname}" } ?: "—"
                Text("$staffName  ·  $subName  ·  $secName", color = labelColor, fontSize = 11.sp, maxLines = 1)
                val daysLabel = if (selectedDays.isEmpty()) "No days selected" else selectedDays.joinToString(", ")
                Text(daysLabel, color = Color(0xFFF59E0B), fontSize = 10.sp)

                val canAssign = selectedStaffId != null && selectedSubjectId != null &&
                        selectedSectionId != null && selectedClassId != null && selectedDays.isNotEmpty()

                Button(
                    onClick = {
                        val days = selectedDays.toList()
                        var remaining = days.size
                        var anyFailed = false
                        days.forEach { day ->
                            viewModel.saveTimetableSlot(
                                staffId   = selectedStaffId!!,
                                subjectId = selectedSubjectId!!,
                                classId   = selectedClassId!!,
                                sectionId = selectedSectionId!!,
                                day       = day,
                                startTime = selectedStartTime,
                                endTime   = selectedEndTime,
                                periodNo  = selectedPeriodNo.toIntOrNull() ?: 1,
                                version   = selectedVersion
                            ) { ok, msg ->
                                if (!ok) anyFailed = true
                                remaining--
                                if (remaining == 0) {
                                    Toast.makeText(
                                        context,
                                        if (anyFailed) "Some slots failed" else "Slots assigned for ${days.size} day(s)!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    enabled = canAssign,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        disabledContainerColor = Color(0xFFF59E0B).copy(0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ASSIGN ${if (selectedDays.size > 1) "(${selectedDays.size} DAYS)" else ""}", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun AutoGeneratorView(viewModel: WantuchViewModel, summary: com.example.wantuch.domain.model.TimetableManagementSummary?, isDark: Boolean = true) {
    val cardColor  = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor  = if (isDark) Color.White       else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.55f) else Color(0xFF64748B)

    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val wizardData by viewModel.wizardData.collectAsState()

    var timetableFor by remember { mutableStateOf("All (Global Default)") }
    var semester by remember { mutableStateOf("ANUAL SYSTEM") }
    var session by remember { mutableStateOf("2026") }
    var isAssembly by remember { mutableStateOf(false) }
    var isNazira by remember { mutableStateOf(false) }
    var totalPeriods by remember { mutableStateOf("8") }
    var breakAfter by remember { mutableStateOf("4") }
    var periodDur by remember { mutableStateOf("45") }
    var breakDur by remember { mutableStateOf("15") }
    
    var selectedStaffId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchWizardData()
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            WizardJumpItem("1. Settings", true) { scope.launch { scrollState.animateScrollToItem(0) } }
            WizardJumpItem("2. Staff", true) { scope.launch { scrollState.animateScrollToItem(2) } }
            WizardJumpItem("3. Preview", true) { scope.launch { scrollState.animateScrollToItem(4) } }
        }
        
        Divider(color = labelColor.copy(0.1f))

        LazyColumn(Modifier.weight(1f), state = scrollState, contentPadding = PaddingValues(bottom = 100.dp)) {
            item { SectionHeader("1. INITIAL SETTINGS", Icons.Default.Settings, isDark) }
            item {
                WizardStepInitialSettings(
                    timetableFor, { timetableFor = it },
                    semester, { semester = it },
                    session, { session = it },
                    isAssembly, { isAssembly = it },
                    isNazira, { isNazira = it },
                    totalPeriods, { totalPeriods = it },
                    breakAfter, { breakAfter = it },
                    periodDur, { periodDur = it },
                    breakDur, { breakDur = it },
                    summary?.groups?.map { it.setting_value } ?: listOf("All (Global Default)")
                )
            }

            item { SectionHeader("2. SELECT STAFF", Icons.Default.People, isDark) }
            item { WizardStepStaff(wizardData?.staff ?: emptyList(), selectedStaffId, isDark) { selectedStaffId = it } }

            item { SectionHeader("3. ASSIGNMENT PREVIEW (GENERATION AREA)", Icons.Default.AutoAwesome, isDark) }
            item { 
                Box(Modifier.fillMaxWidth().height(300.dp).padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(0.2f))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No staff processed yet. Select a staff from section 2.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        Surface(Modifier.fillMaxWidth(), color = cardColor, tonalElevation = 12.dp) {
            Row(Modifier.navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("50% Automatic Mode", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("AI Engine Ready", color = textColor, fontSize = 10.sp)
                }
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.width(160.dp)
                ) {
                    Icon(Icons.Default.AutoMode, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("INITIATE AGENT")
                }
            }
        }
    }
}

@Composable
fun BulkToolView(viewModel: WantuchViewModel, summary: com.example.wantuch.domain.model.TimetableManagementSummary?, isDark: Boolean = true) {
    val cardColor  = if (isDark) Color(0xFF1E293B) else Color.White
    val surfColor  = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textColor  = if (isDark) Color.White       else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.55f) else Color(0xFF64748B)

    var ttFor by remember { mutableStateOf("All (Global Default)") }
    var autoCalc by remember { mutableStateOf(false) }
    var bulkText by remember { mutableStateOf("") }
    
    val versions = summary?.groups?.map { it.setting_value } ?: listOf("All (Global Default)")

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            ManagementHeader("Bulk Quick Text Timetable", "Import schedule from plain text", textColor, labelColor)
        }
        
        item {
            TimetableField("TIMETABLE FOR", ttFor, versions) { ttFor = it }
        }
        
        item {
            Surface(
                color = Color(0xFFA855F7).copy(0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AUTO-CALCULATE TIMINGS", color = Color(0xFFD8B4FE), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Use Step 1 settings for periods?", color = Color(0xFFD8B4FE).copy(0.7f), fontSize = 11.sp)
                    }
                    Switch(checked = autoCalc, onCheckedChange = { autoCalc = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD8B4FE)))
                }
            }
        }
        
        item {
            BulkFormatSection(
                title = "REQUIRED FORMAT:",
                color = Color(0xFFF59E0B),
                content = """
                    SYSTEM | YEAR
                    
                    Assembly | Start Time | End Time | Y/N
                    
                    NAZIRA | Start Time | End Time | Y/N
                    
                    Periods Total | Break After Period No | Half Day Y/N
                    
                    CLASS | SECTION | DAY | PERIOD | SUBJECT | STAFF ...
                """.trimIndent()
            )
        }
        
        item {
            BulkFormatSection(
                title = "EXAMPLE:",
                color = Color(0xFF10B981),
                content = """
                    ANNUAL | 2026
                    
                    Assembly | 07:30 am | 07:45 am | Y
                """.trimIndent()
            )
        }
        
        item {
            Text("INPUT DATA", color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Surface(
                Modifier.fillMaxWidth().height(300.dp),
                color = cardColor,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, labelColor.copy(0.2f))
            ) {
                OutlinedTextField(
                    value = bulkText,
                    onValueChange = { bulkText = it },
                    modifier = Modifier.fillMaxSize(),
                    placeholder = { Text("Enter plain text here...", color = labelColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor
                    )
                )
            }
        }
        
        item {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) {
                Text("PROCESS BATCH", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun BulkFormatSection(title: String, color: Color, content: String, isDark: Boolean = true) {
    val bg       = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val textCol  = if (isDark) Color.LightGray   else Color(0xFF475569)
    Column {
        Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
            Text(
                content,
                color = textCol,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, isDark: Boolean = true) {
    val bg   = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val text = if (isDark) Color.White       else Color(0xFF1E293B)
    Row(
        Modifier.fillMaxWidth().background(bg).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun WizardJumpItem(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.clip(CircleShape)
    ) {
        Text(
            label, 
            color = if(active) Color(0xFFF59E0B) else Color.Gray, 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun WizardStepInitialSettings(
    ttFor: String, onTtFor: (String) -> Unit,
    sem: String, onSem: (String) -> Unit,
    sess: String, onSess: (String) -> Unit,
    assembly: Boolean, onAssembly: (Boolean) -> Unit,
    nazira: Boolean, onNazira: (Boolean) -> Unit,
    totalP: String, onTotalP: (String) -> Unit,
    breakP: String, onBreakP: (String) -> Unit,
    pDur: String, onPDur: (String) -> Unit,
    bDur: String, onBDur: (String) -> Unit,
    versions: List<String>
) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TimetableField("TIMETABLE FOR", ttFor, versions, onTtFor)
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.weight(1f)) { TimetableField("PERIOD SYSTEM", sem, listOf("ANUAL SYSTEM", "SEMESTER SYSTEM"), onSem) }
            Box(Modifier.weight(1f)) { TimetableField("SESSION", sess, listOf("2024", "2025", "2026"), onSess) }
        }
        
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Assembly?", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Include morning assembly", color = Color.Gray, fontSize = 11.sp)
            }
            Switch(checked = assembly, onCheckedChange = onAssembly, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF59E0B)))
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text("Nazira?", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Include Quran Nazira", color = Color.Gray, fontSize = 11.sp)
            }
            Switch(checked = nazira, onCheckedChange = onNazira, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF59E0B)))
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Total Periods", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = totalP, onValueChange = onTotalP,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Break After", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = breakP, onValueChange = onBreakP,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                )
            }
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Period Dur (M)", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pDur, onValueChange = onPDur,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Break Dur (M)", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bDur, onValueChange = onBDur,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                )
            }
        }
        
        Text("PERIOD TIMINGS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        
        val periods = totalP.toIntOrNull() ?: 0
        val pDurInt = pDur.toIntOrNull() ?: 45
        val bDurInt = bDur.toIntOrNull() ?: 15
        val bAfterInt = breakP.toIntOrNull() ?: 0
        
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 8)
        calendar.set(Calendar.MINUTE, 0)
        
        Column {
            repeat(periods) { i ->
                val startTime = sdf.format(calendar.time)
                calendar.add(Calendar.MINUTE, pDurInt)
                val endTime = sdf.format(calendar.time)
                
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("P${i+1}", color = Color(0xFFF59E0B), fontWeight = FontWeight.Black, modifier = Modifier.width(30.dp))
                    OutlinedTextField(value = startTime, onValueChange = {}, modifier = Modifier.weight(1f), enabled = false, shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = endTime, onValueChange = {}, modifier = Modifier.weight(1f), enabled = false, shape = RoundedCornerShape(8.dp))
                }
                
                if (i + 1 == bAfterInt) {
                    calendar.add(Calendar.MINUTE, bDurInt)
                    Surface(color = Color(0xFFF59E0B).copy(0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("--- BREAK ---", color = Color(0xFFF59E0B), fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WizardStepStaff(staff: List<com.example.wantuch.domain.model.WizardStaff>, selectedId: Int?, isDark: Boolean = true, onSelect: (Int) -> Unit) {
    val cardColor  = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
    val textColor  = if (isDark) Color.White       else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.55f) else Color(0xFF64748B)

    var searchQuery by remember { mutableStateOf("") }
    val filteredStaff = staff.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text("Search staff...", color = labelColor) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = labelColor) },
            colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = textColor, focusedTextColor = textColor)
        )
        Spacer(Modifier.height(12.dp))
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filteredStaff.forEach { s ->
                val isSelected = s.id == selectedId
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFFF59E0B).copy(0.1f) else cardColor)
                        .clickable { onSelect(s.id) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, null, tint = if (isSelected) Color(0xFFF59E0B) else labelColor)
                    Spacer(Modifier.width(12.dp))
                    Text(s.name, color = textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Default.ChevronRight, null, tint = labelColor)
                }
            }
        }
    }
}

@Composable
fun WizardStepSubject(subjects: List<com.example.wantuch.domain.model.WizardSubject>, selectedId: Int?, isDark: Boolean = true, onSelect: (Int) -> Unit) {
    val cardColor  = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
    val textColor  = if (isDark) Color.White       else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.55f) else Color(0xFF64748B)

    var searchQuery by remember { mutableStateOf("") }
    val filteredSubjects = subjects.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text("Search subject...", color = labelColor) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = labelColor) },
            colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = textColor, focusedTextColor = textColor)
        )
        Spacer(Modifier.height(12.dp))
        Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filteredSubjects.forEach { s ->
                val isSelected = s.id == selectedId
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFFF59E0B).copy(0.1f) else cardColor)
                        .clickable { onSelect(s.id) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Book, null, tint = if (isSelected) Color(0xFFF59E0B) else labelColor)
                    Spacer(Modifier.width(12.dp))
                    Text(s.name, color = textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("STAFF SCHEDULE PREVIEW", color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Surface(Modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp), color = cardColor, shape = RoundedCornerShape(16.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("No preview data", color = labelColor, fontSize = 11.sp) }
        }
    }
}

@Composable
fun WizardStepFinalize(
    classes: List<com.example.wantuch.domain.model.ManagementClass>,
    selectedSectionId: Int?,
    selectedDays: Set<String>,
    periodNo: String,
    startTime: String,
    endTime: String,
    onSectionSelect: (sectionId: Int, classId: Int) -> Unit,
    onDaysChange: (Set<String>) -> Unit,
    onPeriodNoChange: (String) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit
) {
    val allDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        val classLabels = classes.map { "${it.cname} - ${it.sname}" }
        var selectedLabel by remember(selectedSectionId) {
            mutableStateOf(
                selectedSectionId?.let { sid ->
                    classLabels.getOrNull(classes.indexOfFirst { it.sid == sid })
                } ?: "Select Class"
            )
        }
        TimetableField("CHOOSE CLASS/SECTION", selectedLabel, classLabels) { label ->
            selectedLabel = label
            val idx = classLabels.indexOf(label)
            if (idx != -1) onSectionSelect(classes[idx].sid, classes[idx].cid)
        }

        // Day selector (multi-select)
        Column {
            Text("DAYS (multi-select)", color = Color.Gray, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                allDays.forEachIndexed { idx, fullDay ->
                    val isOn = fullDay in selectedDays
                    Surface(
                        onClick = {
                            onDaysChange(
                                if (isOn) selectedDays - fullDay else selectedDays + fullDay
                            )
                        },
                        color = if (isOn) Color(0xFFF59E0B) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(dayLabels[idx], color = if (isOn) Color.White else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Period / Time row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(0.8f)) {
                Text("P.No", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = periodNo, onValueChange = onPeriodNoChange,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                )
            }
            Column(Modifier.weight(1.2f)) {
                Text("Start Time", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = startTime, onValueChange = onStartChange,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.AccessTime, null, Modifier.size(16.dp), tint = Color(0xFFF59E0B)) },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                )
            }
            Column(Modifier.weight(1.2f)) {
                Text("End Time", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = endTime, onValueChange = onEndChange,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.AccessTime, null, Modifier.size(16.dp), tint = Color(0xFFF59E0B)) },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                )
            }
        }

        // Quick-fill preset slots
        Column {
            Text("QUICK FILL PRESETS", color = Color.Gray, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), spacing = 8.dp) {
                listOf(
                    Triple("P1", "08:00 AM", "08:45 AM"),
                    Triple("P2", "08:45 AM", "09:30 AM"),
                    Triple("P3", "09:30 AM", "10:15 AM"),
                    Triple("P4", "10:15 AM", "11:00 AM"),
                    Triple("P5", "11:15 AM", "12:00 PM"),
                    Triple("P6", "12:00 PM", "12:45 PM"),
                    Triple("P7", "12:45 PM", "01:30 PM"),
                    Triple("P8", "01:30 PM", "02:15 PM")
                ).forEach { (pLabel, s, e) ->
                    Surface(
                        onClick = {
                            onPeriodNoChange(pLabel.removePrefix("P"))
                            onStartChange(s)
                            onEndChange(e)
                        },
                        color = Color(0xFF1E293B), shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(0.3f))
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(pLabel, color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text(s, color = Color.Gray, fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableField(label: String, value: String, options: List<String> = emptyList(), onSelect: (String) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1E293B), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(0.1f)), onClick = { expanded = true }) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(value, color = Color.White, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF1E293B)).fillMaxWidth(0.8f)) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option, color = Color.White) }, onClick = { onSelect(option); expanded = false }) }
            }
        }
    }
}

@Composable
fun FlowRow(modifier: Modifier = Modifier, spacing: androidx.compose.ui.unit.Dp = 0.dp, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, constraints ->
        var rowWidth = 0
        var totalHeight = 0
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var rowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            if (rowWidth + placeable.width + spacing.roundToPx() > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                totalHeight += rowHeight + spacing.roundToPx()
                currentRow = mutableListOf()
                rowWidth = 0
                rowHeight = 0
            }
            currentRow.add(placeable)
            rowWidth += placeable.width + spacing.roundToPx()
            rowHeight = maxOf(rowHeight, placeable.height)
        }
        rows.add(currentRow)
        totalHeight += rowHeight
        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                var maxH = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + spacing.roundToPx()
                    maxH = maxOf(maxH, placeable.height)
                }
                y += maxH + spacing.roundToPx()
            }
        }
    }
}

fun printTimetablePdf(
    context: Context, 
    archiveData: com.example.wantuch.domain.model.TimetableArchiveResponse, 
    level: String,
    versionLabel: String
) {
    if (archiveData.items.isNullOrEmpty()) {
        Toast.makeText(context, "No data to print", Toast.LENGTH_SHORT).show()
        return
    }

    val classGroups = archiveData.items.groupBy { "${it.class_name ?: ""} - ${it.section_name ?: ""}".trim('-', ' ') }
        .filterKeys { it.isNotBlank() }.toSortedMap()

    val allUniqueTimes = archiveData.items.mapNotNull { it.start_time }.distinct()
        .sortedBy { try { SimpleDateFormat("hh:mm a", Locale.US).parse(it)?.time ?: 0L } catch (e: Exception) { 0L } }

    val htmlBuilder = java.lang.StringBuilder()
    htmlBuilder.append("""
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 0; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                .top-banner { background-color: #2F5597; color: white; display: flex; justify-content: space-between; padding: 25px 30px; align-items: stretch; position: relative; overflow: hidden; height: 90px;}
                .banner-left { z-index: 2; padding-top: 15px; }
                .banner-left h2 { margin: 0; font-size: 16px; font-weight: normal; }
                .banner-left h3 { margin: 6px 0 0 0; font-size: 20px; letter-spacing: 1px; font-weight: 500;}
                .banner-right { background-color: #FFFFFF; color: black; padding: 20px 40px; transform: skewX(-30deg); position: absolute; right: -30px; top: 0; bottom: 0; width: 55%; z-index: 1; border-left: 8px solid white; display: flex; flex-direction: column; align-items: flex-end; justify-content: center;}
                .banner-right-content { transform: skewX(30deg); text-align: right; margin-right: 50px;}
                .banner-right-content h1 { margin: 0; color: #2F5597; font-size: 32px; font-weight: 800; letter-spacing: 2px;}
                .banner-right-content p { margin: 4px 0; font-size: 12px; font-weight: bold; }
                
                table { width: 100%; border-collapse: collapse; border-bottom: 2px solid #e0e0e0;}
                th, td { border: 1px solid #d0d0d0; text-align: center; vertical-align: middle; }
                th { background-color: #2F5597; color: white; padding: 12px 4px; font-size: 12px; }
                .special-col { background-color: #FFFBFB; color: #C00000; font-weight: 900; font-size: 18px; padding: 10px 4px; letter-spacing: 2px; text-transform: uppercase;}
                .class-name { font-weight: 800; font-size: 18px; padding: 10px; color: #000; background-color: #FAFAFA; width: 100px;}
                
                .sub-title { color: #2F5597; font-weight: bold; font-size: 14px; margin-bottom: 4px; text-transform: uppercase;}
                .teacher { font-weight: 800; font-size: 11px; color: #000; margin-bottom: 4px; text-transform: uppercase;}
                .days { color: #2F5597; font-size: 8px; text-transform: uppercase; font-weight: bold;}
                .slot-block { padding: 10px 4px; }
                .slot-border { border-bottom: 1px solid #e0e0e0; }
                
                .footer { display: flex; justify-content: space-between; padding: 40px 60px 10px 60px; font-size: 11px; font-weight: bold; margin-top: 30px;}
                .footer-sig { border-top: 1px dashed #888; padding-top: 8px; width: 23%; text-align: center;}
            </style>
        </head>
        <body>
            <div class="top-banner">
                <div class="banner-left">
                    <h2>GMS PEOCHAR</h2>
                    <h3>OFFICIAL ACADEMIC ARCHIVE</h3>
                </div>
                <div class="banner-right">
                    <div class="banner-right-content">
                        <h1>MASTER TIMETABLE</h1>
                        <p>Village Awaro P O Gat Tehsil Matta district Swat kpk Pakistan</p>
                        <p>${java.text.SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())} | Group $versionLabel</p>
                    </div>
                </div>
            </div>
            <table>
                <tr>
                    <th style="background-color: #1e4588; border-right: none; border-bottom: none; width: 100px;">
                       <span style="font-size: 10px; float: right; margin-right: 5px;">DAY</span><br>
                       <span style="font-size: 10px; float: left; margin-left: 5px;">TIME</span>
                    </th>
                    <th style="background-color: #FFFBFB; color: #C00000;">A<br><span style="font-size: 10px;">7:45</span></th>
                    <th style="background-color: #FFFBFB; color: #C00000;">N<br><span style="font-size: 10px;">7:55</span></th>
    """.trimIndent())

    for ((idx, pTime) in allUniqueTimes.withIndex()) {
        val p = idx + 1
        htmlBuilder.append("<th>$p<br><span style='font-size: 10px;'>$pTime</span></th>")
    }
    
    htmlBuilder.append("""
                    <th style="background-color: #FFFBFB; color: #C00000;">B<br><span style="font-size: 10px;">12:10</span></th>
                </tr>
    """)

    var isFirstRow = true
    val totalClasses = classGroups.size
    
    for ((className, items) in classGroups) {
        htmlBuilder.append("<tr>")
        htmlBuilder.append("<td class='class-name'>$className</td>")
        
        if (isFirstRow) {
            htmlBuilder.append("<td rowspan='$totalClasses' class='special-col'>A<br>S<br>S<br>E<br>M<br>B<br>L<br>Y</td>")
            htmlBuilder.append("<td rowspan='$totalClasses' class='special-col'>N<br>A<br>Z<br>I<br>R<br>A</td>")
        }
        
        for (pTime in allUniqueTimes) {
            htmlBuilder.append("<td>")
            val pItems = items.filter { it.start_time == pTime }
            val groupedBySubAndTeacher = pItems.groupBy { "${it.sub_name ?: ""}||${it.teacher_name ?: ""}" }
            
            val sortedGroups = groupedBySubAndTeacher.entries.toList()
            for ((idx, entry) in sortedGroups.withIndex()) {
                val subName = entry.key.split("||")[0]
                val teacherName = entry.key.split("||").getOrNull(1) ?: ""
                
                val daysArr = entry.value.mapNotNull { it.day_of_week?.take(3)?.uppercase() }.distinct()
                val daysStr = daysArr.joinToString(" | ")
                
                val borderClass = if (idx < sortedGroups.size - 1) "slot-border" else ""
                
                htmlBuilder.append("""
                    <div class="slot-block $borderClass">
                        <div class="sub-title">$subName</div>
                        <div class="teacher">${teacherName.ifBlank { "—" }}</div>
                        <div class="days">$daysStr</div>
                    </div>
                """)
            }
            htmlBuilder.append("</td>")
        }
        
        if (isFirstRow) {
            htmlBuilder.append("<td rowspan='$totalClasses' class='special-col'>B<br>R<br>E<br>A<br>K</td>")
        }
        
        htmlBuilder.append("</tr>")
        isFirstRow = false
    }

    htmlBuilder.append("""
            </table>
            
            <div class="footer">
                <div class="footer-sig">HEAD OF INSTITUTION</div>
                <div class="footer-sig">TIMETABLE COORDINATOR</div>
                <div class="footer-sig">ACADEMIC COUNCIL</div>
            </div>
        </body>
        </html>
    """.trimIndent())

    val webView = WebView(context)
    webView.loadDataWithBaseURL(null, htmlBuilder.toString(), "text/html", "UTF-8", null)

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter("TimetableDocument")
            val jobName = "Timetable-$level"
            val attributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                .build()
            printManager.print(jobName, printAdapter, attributes)
        }
    }
}
