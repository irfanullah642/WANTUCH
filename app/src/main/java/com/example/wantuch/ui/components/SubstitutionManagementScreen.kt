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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wantuch.domain.model.AbsentPeriod
import com.example.wantuch.domain.model.AbsentStaff
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SubstitutionManagementScreen(viewModel: WantuchViewModel, onBack: () -> Unit) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    val substitutionData by viewModel.substitutionData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedVersion) {
        viewModel.fetchSubstitutionData(selectedVersion)
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderActionIcon(Icons.Default.ArrowBack, isDark, onBack)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("SUBSTITUTION HUB", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Daily Reassignments", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.weight(1f))
                    HeaderActionIcon(Icons.Default.Sync, isDark) { viewModel.fetchSubstitutionData(selectedVersion) }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && substitutionData == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFF3B82F6))
            } else {
                val absentStaff = substitutionData?.absent_staff ?: emptyList()
                
                if (absentStaff.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(0.1f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Full Attendance Today!", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("EVERY MASTER IS ON DUTY", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        // Roadmap Header
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Surface(shape = CircleShape, color = Color(0xFF6366F1).copy(0.1f)) {
                                Icon(Icons.Default.EventNote, null, tint = Color(0xFF6366F1), modifier = Modifier.padding(12.dp).size(30.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("ABSENT STAFF ROADMAP", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            val dateStr = SimpleDateFormat("EEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
                            Text(dateStr.uppercase(), color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.width(40.dp).height(3.dp).background(Color(0xFF6366F1).copy(0.3f), CircleShape))
                        }

                        Spacer(Modifier.height(30.dp))

                        absentStaff.forEach { staff ->
                            StaffAbsentCard(staff, isDark, textColor, labelColor, cardColor, viewModel, selectedVersion)
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffAbsentCard(staff: AbsentStaff, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color, viewModel: WantuchViewModel, version: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, labelColor.copy(0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            // Staff Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = labelColor.copy(0.05f)) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF6366F1), modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(staff.full_name, color = textColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(staff.status.uppercase(), color = if(staff.status == "Absent") Color(0xFFEF4444) else Color(0xFF3B82F6), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                Box(Modifier.size(8.dp).background(if(staff.status == "Absent") Color(0xFFEF4444) else Color(0xFF3B82F6), CircleShape))
            }

            Spacer(Modifier.height(20.dp))

            // Periods
            staff.periods?.forEach { period ->
                AbsentPeriodStrip(period, staff.id, isDark, textColor, labelColor, cardColor, viewModel, version)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun AbsentPeriodStrip(period: AbsentPeriod, originalStaffId: Int, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color, viewModel: WantuchViewModel, version: Int) {
    val isAssigned = period.sub_sid != null
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if(isAssigned) Color(0xFF10B981).copy(0.05f) else Color.Transparent),
        border = BorderStroke(1.dp, if(isAssigned) Color(0xFF10B981).copy(0.3f) else labelColor.copy(0.1f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PERIOD ${period.period_number}", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text("${period.start_time} - ${period.end_time}", color = Color(0xFF6366F1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(period.sub_name.uppercase(), color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("${period.class_name} ${period.sec_name}", color = labelColor, fontSize = 9.sp)
                }
            }

            if (isAssigned) {
                // Showing who is assigned
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().background(labelColor.copy(0.05f), RoundedCornerShape(10.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HowToReg, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(period.sub_name_ext ?: "Substitute", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(if(period.sub_status == "accepted") "Approved" else "Registered", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = { viewModel.removeSubstitution(period.id, version) }) {
                        Icon(Icons.Default.Cancel, null, tint = Color(0xFFEF4444).copy(0.5f), modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                // Available substitutes selection
                Spacer(Modifier.height(16.dp))
                Text("AVAILABLE SUBSTITUTES", color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                
                if (period.available_staff.isNullOrEmpty()) {
                    Text("No free masters found", color = Color(0xFFEF4444).copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(period.available_staff) { staff ->
                            Surface(
                                onClick = { viewModel.assignSubstitution(period.id, originalStaffId, staff.id, 1, version) },
                                color = labelColor.copy(0.05f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, labelColor.copy(0.1f))
                            ) {
                                Text(staff.full_name, color = labelColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
