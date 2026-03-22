package com.example.wantuch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wantuch.ui.viewmodel.WantuchViewModel


@Composable
fun AttendanceManagementScreen(viewModel: WantuchViewModel, onBack: () -> Unit) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    var selectedTopTab by remember { mutableIntStateOf(0) }
    val topTabs = listOf("Attendance", "Monthly", "Rules", "L-appeals", "Status Admin")

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
            when (selectedTopTab) {
                0 -> AttendanceMainTab(viewModel, isDark, textColor, labelColor, cardColor)
                1 -> MonthlyAttendanceTab(viewModel, isDark, textColor, labelColor, cardColor)
                2 -> AttendanceRulesTab(isDark, textColor, labelColor, cardColor)
                3 -> LeaveAppealsTab(isDark, textColor, labelColor, cardColor)
                4 -> StatusAdminTab(isDark, textColor, labelColor, cardColor)
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun AttendanceMainTab(viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    var subTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val subTabs = listOf("Students", "Staff", "Manual", "Smart")

    val structure by viewModel.schoolStructure.collectAsState()
    var selectedClassId by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var classExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.fetchSchoolStructure()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Sub-tabs row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            subTabs.forEachIndexed { i, t ->
                Surface(
                    onClick = { subTab = i },
                    color = if(subTab == i) Color(0xFFF59E0B).copy(0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp),
                    border = BorderStroke(1.dp, if(subTab == i) Color(0xFFF59E0B) else labelColor.copy(0.2f))
                ) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(t.uppercase(), color = if(subTab == i) Color(0xFFF59E0B) else labelColor, fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (subTab == 0) {
            // STUDENTS ATTENDANCE
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("STUDENTS ATTENDANCE", color = textColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = { /* Check Status */ },
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
                            Surface(color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f), shape = RoundedCornerShape(8.dp)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("21 Mar 2026", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        value = "", onValueChange = {},
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
        }
    }
}

@Composable
fun MonthlyAttendanceTab(viewModel: WantuchViewModel, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    var subTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val structure by viewModel.schoolStructure.collectAsState()
    var selectedClassId by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var classExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.fetchSchoolStructure()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                Surface(
                    onClick = { subTab = 0 },
                    color = if(subTab == 0) Color(0xFF3B82F6) else cardColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(0.3f)),
                    modifier = Modifier.fillMaxWidth().height(45.dp)
                ) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("STUDENTS", color = if(subTab == 0) Color.White else labelColor, fontWeight = FontWeight.Black, fontSize = 11.sp) }
                }
            }
            Box(Modifier.weight(1f)) {
                Surface(
                    onClick = { subTab = 1 },
                    color = if(subTab == 1) Color(0xFF3B82F6) else cardColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(0.3f)),
                    modifier = Modifier.fillMaxWidth().height(45.dp)
                ) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("STAFF", color = if(subTab == 1) Color.White else labelColor, fontWeight = FontWeight.Black, fontSize = 11.sp) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("STUDENT PERFORMANCE", color = textColor, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1.5f)) {
                val selectedText = if(selectedClassId > 0) {
                    structure?.classes?.find { it.id == selectedClassId }?.name?.uppercase() ?: "CHOOSE CLASS..."
                } else {
                    "CHOOSE CLASS..."
                }

                Box {
                    Surface(
                        onClick = { classExpanded = true },
                        color = cardColor, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, labelColor.copy(0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedText, color = if(selectedClassId > 0) textColor else labelColor, fontSize = 13.sp, maxLines = 1)
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
            Box(Modifier.weight(1f)) {
                Surface(color = cardColor, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, labelColor.copy(0.2f))) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("March", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
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
fun LeaveAppealsTab(isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    var subTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("PENDING LEAVE APPEALS", color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                Surface(
                    onClick = { subTab = 0 },
                    color = if(subTab == 0) Color(0xFF6366F1) else cardColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(0.2f))
                ) {
                    Box(Modifier.fillMaxWidth().height(42.dp), Alignment.Center) { Text("STAFF REQUESTS", color = if(subTab == 0) Color.White else labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                }
            }
            Box(Modifier.weight(1f)) {
                Surface(
                    onClick = { subTab = 1 },
                    color = if(subTab == 1) Color(0xFF6366F1) else cardColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(0.2f))
                ) {
                    Box(Modifier.fillMaxWidth().height(42.dp), Alignment.Center) { Text("STUDENT REQUESTS", color = if(subTab == 1) Color.White else labelColor, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                }
            }
        }

        Spacer(Modifier.height(60.dp))
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MoveToInbox, null, Modifier.size(64.dp), tint = labelColor.copy(0.2f))
                Spacer(Modifier.height(16.dp))
                Text(if(subTab == 0) "No Pending staff Appeals" else "No Pending Student Appeals", color = labelColor.copy(0.5f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatusAdminTab(isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
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
                                Text("21/03/2026", color = textColor, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(48.dp).padding(top = 15.dp)) {
                        Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Load Status", fontWeight = FontWeight.Black)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("0 Submitted", Color(0xFF10B981), Modifier.weight(1f))
                    StatusChip("13 Pending", Color(0xFFEF4444), Modifier.weight(1f))
                    StatusChip("2 Requests", Color(0xFF6366F1), Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Tab Row for Class Status/Edit Requests
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = Color(0xFF3B82F6).copy(0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFF3B82F6))) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Grid3x3, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Class Status", color = Color(0xFF3B82F6), fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
            Surface(color = cardColor, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, labelColor.copy(0.2f))) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = labelColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Requests", color = labelColor, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.size(16.dp).background(Color(0xFFEF4444), CircleShape), Alignment.Center) { Text("2", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Class-wise Status", color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))

        // Mock Class Status Table
        val classes = listOf("FSc-I A", "FSc-II A", "Grade-I A", "Grade-II A")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            classes.forEachIndexed { i, c ->
                ClassStatusRow(i+1, c, isDark, textColor, labelColor, cardColor)
            }
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier) {
    Surface(color = color.copy(0.1f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, color.copy(0.2f)), modifier = modifier) {
        Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ClassStatusRow(idx: Int, className: String, isDark: Boolean, textColor: Color, labelColor: Color, cardColor: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, labelColor.copy(0.05f))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#$idx", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(className, color = textColor, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("Teacher: --", color = labelColor, fontSize = 9.sp)
                }
                Surface(color = Color(0xFFEF4444).copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Color(0xFFEF4444), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("PENDING", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {},
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
