package com.example.wantuch.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wantuch.domain.model.TimetableItem
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimetableScreen(
    viewModel: WantuchViewModel, 
    onBack: () -> Unit, 
    onOpenSubstitution: () -> Unit = {},
    onOpenManagement: (ManagementTab) -> Unit = {}
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    val timetableData by viewModel.timetableData.collectAsState()
    val metadata by viewModel.timetableMetadata.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedDay by remember { mutableStateOf(Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Monday") }
    var selectedClassId by remember { mutableIntStateOf(0) }
    var selectedSectionId by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.fetchTimetableMetadata()
        viewModel.fetchTimetable(day = selectedDay)
    }

    LaunchedEffect(selectedDay, selectedClassId, selectedSectionId) {
        viewModel.fetchTimetable(classId = selectedClassId, sectionId = selectedSectionId, day = selectedDay)
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderActionIcon(Icons.Default.ArrowBack, isDark, onBack)
                    Spacer(Modifier.width(16.dp))
                    Text("Time Table", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }

                // Premium Web-like Action Chips
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { TimetableActionChip("PROXY", Icons.Default.SwapHoriz, Color(0xFF4F46E5), isDark, onOpenSubstitution) }
                    item { TimetableActionChip("CREATE", Icons.Default.Add, Color(0xFF059669), isDark) { onOpenManagement(ManagementTab.CREATE) } }
                    item { TimetableActionChip("AUTO", Icons.Default.SmartToy, Color(0xFF7C3AED), isDark) { onOpenManagement(ManagementTab.AUTO) } }
                    item { TimetableActionChip("BULK", Icons.Default.FileCopy, Color(0xFFD97706), isDark) { onOpenManagement(ManagementTab.BULK) } }
                    item { TimetableActionChip("TASKS", Icons.Default.List, Color(0xFF0284C7), isDark) { onOpenManagement(ManagementTab.EXISTING) } }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Day Selector
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    items(days) { day ->
                        val isSelected = day == selectedDay
                        Surface(
                            onClick = { selectedDay = day },
                            color = if(isSelected) Color(0xFF3B82F6) else cardColor,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if(isSelected) Color(0xFF3B82F6) else labelColor.copy(0.2f))
                        ) {
                            Text(day.take(3).uppercase(), 
                                color = if(isSelected) Color.White else labelColor, 
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFF3B82F6))
            } else {
                val items = timetableData?.items ?: emptyList()
                if (items.isEmpty()) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventBusy, null, Modifier.size(64.dp), tint = labelColor.copy(0.3f))
                        Text("No Schedule for $selectedDay", color = labelColor)
                    }
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        if (timetableData?.mode == "dashboard") {
                            // Admin Grid View
                            items.chunked(2).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { item ->
                                        DashboardTimetableCard(item, isDark, textColor, labelColor, cardColor, Modifier.weight(1f)) {
                                            selectedClassId = item.cid ?: 0
                                            selectedSectionId = item.sid ?: 0
                                        }
                                    }
                                    if (row.size < 2) Spacer(Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        } else {
                            // Focused Timeline View
                            items.forEach { item ->
                                TimelinePeriodRow(item, isDark, textColor, labelColor, cardColor)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableActionChip(label: String, icon: ImageVector, accent: Color, isDark: Boolean, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        color = accent,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun DashboardTimetableCard(item: TimetableItem, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, labelColor.copy(0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.class_name ?: "CLASS", color = textColor, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text(item.section_name?.uppercase() ?: "SEC", color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.size(8.dp).background(if(item.cur_subject != null) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape))
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (item.cur_subject != null) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("UPCOMING", color = Color(0xFF3B82F6), fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.weight(1f))
                            Box(Modifier.size(6.dp).background(Color(0xFFF59E0B), CircleShape))
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (item.pno ?: "1").toString(), 
                                color = Color(0xFFF59E0B), 
                                fontSize = 34.sp, 
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.width(12.dp))
                            VerticalDivider(modifier = Modifier.height(30.dp), color = Color.White.copy(0.1f))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(item.cur_subject.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text(item.cur_teacher ?: "No Teacher", color = Color.White.copy(0.5f), fontSize = 10.sp)
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        PeriodCountdown(targetTime = item.cur_start_time ?: "")
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(40.dp).background(labelColor.copy(0.05f), RoundedCornerShape(8.dp)), Alignment.Center) {
                    Text("WEEKEND / OFF", color = labelColor.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun PeriodCountdown(targetTime: String) {
    var timeRemaining by remember { mutableStateOf("00:00:00") }
    
    LaunchedEffect(targetTime) {
        if (targetTime.isEmpty()) return@LaunchedEffect
        
        while(true) {
            val now = Calendar.getInstance()
            val formats = listOf("hh:mm a", "h:mm a", "HH:mm:ss", "HH:mm")
            var parsedDate: Date? = null
            
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    parsedDate = sdf.parse(targetTime.trim().uppercase())
                    if (parsedDate != null) break
                } catch (e: Exception) {}
            }
            
            if (parsedDate != null) {
                val targetCal = Calendar.getInstance()
                targetCal.time = parsedDate
                targetCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
                
                var diff = targetCal.timeInMillis - now.timeInMillis
                
                // If it's more than 12 hours ago, it's likely for the next day or a time zone issue
                // Here we just focus on the closest upcoming time
                if (diff < -1000 * 60 * 60 * 12) {
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                    diff = targetCal.timeInMillis - now.timeInMillis
                }

                if (diff > 0) {
                    val hours = diff / (1000 * 60 * 60)
                    val minutes = (diff / (1000 * 60)) % 60
                    val seconds = (diff / 1000) % 60
                    timeRemaining = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    timeRemaining = "00:00:00"
                }
            } else {
                timeRemaining = "--:--:--"
            }
            delay(1000L)
        }
    }

    Text(
        timeRemaining, 
        color = Color(0xFFF59E0B), 
        fontSize = 20.sp, 
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
fun TimelinePeriodRow(item: TimetableItem, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(60.dp), horizontalAlignment = Alignment.End) {
            Text(item.start_time?.take(5) ?: "", color = textColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(item.end_time?.take(5) ?: "", color = labelColor, fontSize = 10.sp)
        }
        
        Spacer(Modifier.width(16.dp))
        
        val statusColor = when(item.live_status) {
            "ongoing" -> Color(0xFF3B82F6)
            "taken" -> Color(0xFF10B981)
            else -> labelColor.copy(0.3f)
        }
        
        Card(
            Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, statusColor.copy(0.3f))
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(40.dp).background(statusColor, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.sub_name ?: "No Subject", color = textColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(item.teacher_name ?: "No Teacher", color = labelColor, fontSize = 11.sp)
                    if (item.sub_teacher != null) {
                        Text("Proxy: ${item.sub_teacher}", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (item.live_status == "ongoing") {
                    Surface(color = Color(0xFF3B82F6).copy(0.1f), shape = CircleShape) {
                        Text("LIVE", color = Color(0xFF3B82F6), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
