package com.example.wantuch.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.activity.compose.BackHandler
import com.example.wantuch.domain.model.*
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import org.json.JSONObject
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSelectorScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onInstitutionSelected: () -> Unit
) {
    val portfolio by viewModel.portfolio.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchPortfolio()
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, Modifier.background(Color.White.copy(0.05f), RoundedCornerShape(12.dp))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Portfolio", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Select Institution", color = Color.White.copy(0.6f), fontSize = 14.sp)
                }
            }

            if (isLoading && portfolio == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            } else {
                portfolio?.let { data ->
                    // Stats Row
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PortfolioStatCard("Schools", data.stats?.get("schools")?.toString() ?: "0", Color(0xFF3B82F6), Modifier.weight(1f))
                        PortfolioStatCard("Students", data.stats?.get("students")?.toString() ?: "0", Color(0xFF10B981), Modifier.weight(1f))
                        PortfolioStatCard("Staff", data.stats?.get("staff")?.toString() ?: "0", Color(0xFFF59E0B), Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("ACTIVE INSTITUTIONS", color = Color.White.copy(0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        data.institutions?.let { list ->
                            items(list) { inst ->
                                InstitutionCard(inst) {
                                    inst.id.toString().toDoubleOrNull()?.toInt()?.let { id ->
                                        viewModel.selectInstitution(id, onInstitutionSelected)
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

@Composable
fun InstitutionCard(inst: Institution, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(70.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(0.05f)), Alignment.Center) {
                if (!inst.logo.isNullOrEmpty()) {
                    AsyncImage(model = "https://www.wantuch.pk/${inst.logo}", contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.School, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(inst.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2)
            Text(inst.type ?: "School", color = Color.White.copy(0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
fun PortfolioStatCard(label: String, value: String, accent: Color, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF1E293B)).border(1.dp, accent.copy(0.3f), RoundedCornerShape(18.dp)).padding(12.dp)) {
        Column {
            Text(label, color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationDashboardScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit = {},
    onOpenStaff: () -> Unit = {},
    onOpenStudents: () -> Unit = {},
    onOpenProfile: (Int) -> Unit = {},
    onOpenMyProfile: (Int) -> Unit = {},
    onOpenFee: () -> Unit = {}
) {
    val data by viewModel.dashboardData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    // Theme Palettes
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // Header Board
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .statusBarsPadding()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("WELCOME TO ${data?.institution_name ?: "WANTUCH"}", 
                            color = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB), 
                            fontSize = 16.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text("${data?.role?.uppercase() ?: "USER"} • DASHBOARD OVERVIEW • ${data?.full_name ?: ""}", 
                            color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val context = LocalContext.current
                            HeaderActionIcon(Icons.Default.ArrowBack, isDark, onBack)
                            HeaderActionIcon(Icons.Default.Palette, isDark) { viewModel.toggleTheme() }
                            HeaderActionIcon(Icons.Default.Logout, isDark, onBack)
                            HeaderActionIcon(Icons.Default.Sync, isDark) { 
                                Toast.makeText(context, "Syncing Data...", Toast.LENGTH_SHORT).show()
                                viewModel.refreshDashboard()
                            }
                        }
                    }
                }
            }

            if (isLoading && data == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            } else {
                data?.let { dash ->
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 15.dp)) {
                        // Suspended Stat Cards (4 in a row)
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val context = LocalContext.current
                            val rawStaff = dash.stats?.get("staff")?.toString() ?: "0"
                            val staffCount = rawStaff.toDoubleOrNull()?.toInt() ?: 0
                            val displayStaff = if (staffCount > 0) (staffCount - 1).toString() else "0"
                            
                            SuspendedStatCard("STAFF", displayStaff, Color(0xFF6366F1), isDark, Modifier.weight(1f)) {
                                onOpenStaff()
                            }
                            SuspendedStatCard("STUDENTS", dash.stats?.get("students")?.toString() ?: "0", Color(0xFF3B82F6), isDark, Modifier.weight(1f)) {
                                onOpenStudents()
                            }
                            SuspendedStatCard("FEE", dash.stats?.get("fee_today")?.toString() ?: "0", Color(0xFF10B981), isDark, Modifier.weight(1f)) {
                                onOpenFee()
                            }
                            SuspendedStatCard("ATTENDANCE", if (dash.is_holiday) "HOLIDAY" else "PRESENT", Color(0xFFF59E0B), isDark, Modifier.weight(1f)) {
                                Toast.makeText(context, "Opening Attendance...", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Spacer(Modifier.height(25.dp))
                        Text("DASHBOARD MODULES", color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp))
                        Spacer(Modifier.height(16.dp))

                        // Modules Grid with Flap Design
                        Column(Modifier.padding(horizontal = 10.dp)) {
                            val context = LocalContext.current
                            dash.modules?.chunked(2)?.forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { mod ->
                                        FlapModuleCard(mod.label, getIcon(mod.icon), isDark, Modifier.weight(1f)) {
                                            when(mod.id) {
                                                "profile" -> {
                                                    dash.user_id?.let { onOpenMyProfile(it) }
                                                }
                                                "staff" -> {
                                                    onOpenStaff()
                                                }
                                                "admission" -> {
                                                    onOpenStudents()
                                                }
                                                "fee" -> {
                                                    onOpenFee()
                                                }
                                                else -> {
                                                    val baseUrl = "https://www.wantuch.pk/"
                                                    val path = when(mod.id) {
                                                        "inst_profile" -> "modules/education/profile.php?tab=inst"
                                                        "quick_scan" -> "modules/education/attendance_quick_scan.php"
                                                        "notices" -> "modules/education/notices.php"
                                                        "classes" -> "modules/education/classes_manage.php"
                                                        "subjects" -> "modules/education/subjects_manage.php"
                                                        "exams" -> "modules/education/exams.php"
                                                        "timetable" -> "modules/education/timetable.php"
                                                        "syllabus" -> "modules/education/syllabus.php"
                                                        "homework" -> "modules/education/assignments.php"
                                                        "attendance" -> "modules/education/attendance.php"
                                                        "transport" -> "modules/education/transport.php"
                                                        "smart_id" -> "modules/education/idcard/idcard.php"
                                                        "promotion" -> "modules/education/promotion.php"
                                                        "proxies" -> "modules/education/substitution_manage.php"
                                                        else -> "modules/education/dashboard.php"
                                                    }
                                                    onOpenWeb(baseUrl + path)
                                                }
                                            }
                                        }
                                    }
                                    if (row.size < 2) Spacer(Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HangingTube(modifier: Modifier) {
    Column(modifier.width(4.dp).height(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF94A3B8), CircleShape))
        Box(Modifier.fillMaxWidth().weight(1f).background(Brush.horizontalGradient(listOf(Color(0xFF64748B), Color(0xFFCBD5E1), Color(0xFF64748B)))))
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF94A3B8), CircleShape))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(56.dp).menuAnchor(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null)
            }
        }
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(cardColor)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.uppercase(), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun HeaderActionIcon(icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp).background(if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f), RoundedCornerShape(8.dp))
    ) {
        Icon(icon, null, tint = if (isDark) Color.White else Color(0xFF1E293B), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SuspendedStatCard(label: String, value: String, accent: Color, isDark: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(85.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = if (isDark) Color.White.copy(0.5f) else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun FlapModuleCard(label: String, icon: ImageVector, isDark: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(38.dp).background(if (isDark) Color.White.copy(0.05f) else Color(0xFF3B82F6).copy(0.1f), CircleShape), Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(label.uppercase(), color = if (isDark) Color.White else Color(0xFF1E293B), fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Text("DASHBOARD", color = if (isDark) Color.White.copy(0.4f) else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun DashStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ModuleButton(label: String, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

fun getIcon(name: String): ImageVector {
    return when (name) {
        "person" -> Icons.Default.AccountCircle
        "apartment" -> Icons.Default.Apartment
        "bolt" -> Icons.Default.Bolt
        "campaign" -> Icons.Default.Campaign
        "class" -> Icons.Default.Class
        "menu_book" -> Icons.Default.MenuBook
        "assignment" -> Icons.Default.Assignment
        "schedule" -> Icons.Default.Schedule
        "book" -> Icons.Default.Book
        "tasks" -> Icons.Default.ListAlt
        "how_to_reg" -> Icons.Default.HowToReg
        "bus" -> Icons.Default.DirectionsBus
        "badge" -> Icons.Default.Badge
        "person_add" -> Icons.Default.PersonAdd
        "school" -> Icons.Default.School
        "payments" -> Icons.Default.Payments
        "person_search" -> Icons.Default.PersonSearch
        "swap_horiz" -> Icons.Default.SwapHoriz
        "event_available" -> Icons.Default.EventAvailable
        "analytics" -> Icons.Default.Analytics
        "database" -> Icons.Default.Storage
        else -> Icons.Default.Category
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    val staffData by viewModel.staffData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    
    var showActions by remember { mutableStateOf(false) }
    var selectedStaff by remember { mutableStateOf<com.example.wantuch.domain.model.StaffMember?>(null) }
    var subModalType by remember { mutableStateOf<String?>(null) } // "attendance" or "salary"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    LaunchedEffect(Unit) {
        viewModel.fetchStaff()
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            
            // App Bar
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, Modifier.background(if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp))) {
                    Icon(Icons.Default.ArrowBack, null, tint = textColor)
                }
                Spacer(Modifier.width(16.dp))
                Text("STAFF MANAGER", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                
                IconButton(onClick = { viewModel.toggleTheme() }, Modifier.background(if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp))) {
                    Icon(if(isDark) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = textColor)
                }
            }
            
            if (isLoading && staffData == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            } else {
                staffData?.let { data ->
                    var selectedTab by remember { mutableIntStateOf(0) }
                    
                    Column(Modifier.fillMaxSize()) {
                        
                        // Top Stats Bar
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).background(cardColor, RoundedCornerShape(12.dp)).padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            StaffPillStat("TOTAL", data.stats?.get("total")?.toString() ?: "0", Color(0xFF3B82F6), isDark)
                            StaffPillStat("PRESENT", data.stats?.get("present")?.toString() ?: "0", Color(0xFF10B981), isDark)
                            StaffPillStat("ABSENT", data.stats?.get("absent")?.toString() ?: "0", Color(0xFFEF4444), isDark)
                            StaffPillStat("LEAVE", data.stats?.get("leave")?.toString() ?: "0", Color(0xFF8B5CF6), isDark)
                        }
                        
                        Spacer(Modifier.height(20.dp))
                        
                        // Tabs
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp)).padding(4.dp)) {
                            TabItem("Teaching", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                            TabItem("Non-Teaching", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
                        }
                        
                        Spacer(Modifier.height(12.dp))

                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            if (selectedTab == 0) {
                                // Teaching Staff List
                                if (!data.teaching_staff.isNullOrEmpty()) {
                                    data.teaching_staff.forEach { member ->
                                        StaffMemberRow(member, cardColor, textColor, labelColor) {
                                            onOpenProfile((member.id as? Number)?.toInt() ?: 0)
                                        }
                                    }
                                } else {
                                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                        Text("No teaching staff found", color = textColor.copy(0.5f))
                                    }
                                }
                            } else {
                                // Non-Teaching Staff List
                                if (!data.non_teaching_staff.isNullOrEmpty()) {
                                    data.non_teaching_staff.forEach { member ->
                                        StaffMemberRow(member, cardColor, textColor, labelColor) {
                                            onOpenProfile((member.id as? Number)?.toInt() ?: 0)
                                        }
                                    }
                                } else {
                                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                        Text("No non-teaching staff found", color = textColor.copy(0.5f))
                                    }
                                }
                            }
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceSubModal(member: com.example.wantuch.domain.model.StaffMember, isDark: Boolean, onDismiss: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    Box(Modifier.fillMaxSize(), Alignment.BottomCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(cardColor)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, null, tint = textColor) }
                Text("Staff Attendance", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = textColor.copy(0.4f)) }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Status Card
            Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF3B82F6).copy(0.1f)), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CURRENT STATUS", color = Color(0xFF3B82F6), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("NOT MARKED", color = Color(0xFF3B82F6), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()), color = textColor.copy(0.4f), fontSize = 12.sp)
                }
            }
            
            Spacer(Modifier.height(30.dp))
            Text("UPDATE ATTENDANCE", color = textColor.copy(0.4f), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(15.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AttendanceOptionCard("Present", Icons.Default.CheckCircle, Color(0xFF10B981), Modifier.weight(1f))
                AttendanceOptionCard("Absent", Icons.Default.Cancel, Color(0xFFEF4444), Modifier.weight(1f))
                AttendanceOptionCard("Leave", Icons.Default.EventBusy, Color(0xFFF59E0B), Modifier.weight(1f))
            }
            
            Spacer(Modifier.height(30.dp))
            
            // Mini Stats
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttendanceMiniStat("Presents", "0", Color(0xFF10B981), Modifier.weight(1f))
                AttendanceMiniStat("Absents", "0", Color(0xFFEF4444), Modifier.weight(1f))
                AttendanceMiniStat("Leaves", "0", Color(0xFFF59E0B), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AttendanceOptionCard(label: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(0.1f))
            .border(1.dp, color.copy(0.2f), RoundedCornerShape(16.dp))
            .padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AttendanceMiniStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(0.05f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label.uppercase(), color = color.copy(0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SalarySubModal(member: com.example.wantuch.domain.model.StaffMember, isDark: Boolean, onDismiss: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    Box(Modifier.fillMaxSize(), Alignment.BottomCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(cardColor)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, null, tint = textColor) }
                Text("Salary Management", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = textColor.copy(0.4f)) }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Balance Card
            Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF10B981).copy(0.1f)), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OUTSTANDING BALANCE", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("RS ${member.balance ?: 0}", color = Color(0xFF10B981), fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Text("Base Salary: RS ${member.balance ?: 0}", color = textColor.copy(0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(25.dp))
            
            // Payment Form
            Text("PROCESS PAYOUT", color = textColor.copy(0.4f), fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("FOR MONTH", color = textColor.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(55.dp).clip(RoundedCornerShape(12.dp)).background(if(isDark) Color.Black.copy(0.2f) else Color.Black.copy(0.05f)).padding(15.dp), Alignment.CenterStart) {
                        Text(java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()), color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("AMOUNT (RS)", color = textColor.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(55.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF10B981).copy(0.05f)).border(1.dp, Color(0xFF10B981).copy(0.3f), RoundedCornerShape(12.dp)).padding(15.dp), Alignment.CenterStart) {
                        Text("${member.balance ?: 0}", color = Color(0xFF10B981), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Spacer(Modifier.height(25.dp))
            
            Button(
                onClick = { /* Process */ },
                modifier = Modifier.fillMaxWidth().height(65.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("FINALIZE PAYOUT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun TabItem(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color(0xFF3B82F6) else Color.Transparent
    val contentColor = if (isSelected) Color.White else (if(isSystemInDarkTheme()) Color.White.copy(0.6f) else Color.Black.copy(0.6f))
    
Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        Alignment.Center
    ) {
        Text(label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp)) {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Black)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}


@Composable
fun StaffActionSheetContent(member: com.example.wantuch.domain.model.StaffMember, textColor: Color, isDark: Boolean, onAction: (String) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 40.dp)) {
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
             Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF0F766E).copy(0.1f)), Alignment.Center) {
                 if (!member.profile_pic.isNullOrEmpty()) {
                     AsyncImage(
                         model = member.profile_pic,
                         contentDescription = null,
                         contentScale = ContentScale.Crop,
                         modifier = Modifier.fillMaxSize()
                     )
                 } else {
                     Text(member.initials, color = Color(0xFF0F766E), fontSize = 22.sp, fontWeight = FontWeight.Black)
                 }
             }
             Spacer(Modifier.width(20.dp))
             Column {
                 Text(member.name, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                 Text("${member.role} • BPS: ${member.bps}", color = textColor.copy(0.6f), fontSize = 13.sp)
             }
        }
        
        val actions = listOf(
            ActionItem("Profile", Icons.Default.AccountCircle, Color(0xFF3B82F6)),
            ActionItem("Attendance", Icons.Default.DateRange, Color(0xFF10B981)),
            ActionItem("Salary", Icons.Default.Payments, Color(0xFFF59E0B)),
            ActionItem("Home Works", Icons.Default.MenuBook, Color(0xFF8B5CF6)),
            ActionItem("Syllabus", Icons.Default.List, Color(0xFF06B6D4)),
            ActionItem("Formmaster", Icons.Default.School, Color(0xFFEC4899)),
            ActionItem("To Non-Teaching", Icons.Default.SwapHoriz, Color(0xFF64748B)),
            ActionItem("Make Admin", Icons.Default.AdminPanelSettings, Color(0xFFEF4444)),
            ActionItem("Biometrics", Icons.Default.Fingerprint, Color(0xFF8B5CF6)),
            ActionItem("Edit Profile", Icons.Default.Edit, Color(0xFF3B82F6)),
            ActionItem("Deactivate Staff", Icons.Default.PowerSettingsNew, Color(0xFFF97316)),
            ActionItem("Delete Staff", Icons.Default.Delete, Color(0xFFEF4444))
        )
        
        Column {
            actions.chunked(3).forEach { rowActions ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowActions.forEach { action ->
                        ActionTile(action, isDark, Modifier.weight(1f)) {
                            onAction(action.label)
                        }
                    }
                    if (rowActions.size < 3) {
                        repeat(3 - rowActions.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

data class ActionItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

@Composable
fun ActionTile(action: ActionItem, isDark: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgColor = if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    val contentColor = if (isDark) Color.White else Color.Black

    Column(
        modifier
            .height(85.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(action.icon, null, tint = action.color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = action.label,
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffMemberRow(
    member: com.example.wantuch.domain.model.StaffMember,
    cardColor: Color,
    textColor: Color,
    labelColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF0F766E).copy(0.1f)), Alignment.Center) {
                if (!member.profile_pic.isNullOrEmpty()) {
                    AsyncImage(
                        model = member.profile_pic,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(member.initials, color = Color(0xFF0F766E), fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                // Row 1: Name
                Text(member.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                // Row 2: Attendance & Role
                Text("${member.marked} • Role: ${member.role} (BPS: ${member.bps})", color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                // Row 3: Paid & Balance
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Paid: " + (member.paid?.toString() ?: "0.0"), color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Bal: " + (member.balance?.toString() ?: "0.0"), color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StaffProfileScreen(
    staffId: Int,
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    val profile by viewModel.staffProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val localContext = LocalContext.current
    
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(staffId) {
        viewModel.fetchStaffProfile(staffId)
    }

    val tabs = listOf(
        Triple("PROFILE", Icons.Default.AccountCircle, Color(0xFF3B82F6)),
        Triple("ATTEN", Icons.Default.DateRange, Color(0xFF10B981)),
        Triple("SALARY", Icons.Default.Payments, Color(0xFFF59E0B)),
        Triple("ROLE", Icons.Default.Badge, Color(0xFF6366F1)),
        Triple("DETAILS", Icons.Default.Description, Color(0xFFEC4899)),
        Triple("CLOUD", Icons.Default.Cloud, Color(0xFF06B6D4)),
        Triple("BIO", Icons.Default.Fingerprint, Color(0xFF00F3FF)),
        Triple("RESIGN", Icons.Default.ExitToApp, Color(0xFFEF4444))
    )

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // Header Section
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                // Cover Gradient
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF6366F1).copy(0.8f)))))
                
                Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, Modifier.background(Color.Black.copy(0.1f), CircleShape)) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { }, Modifier.background(Color.Black.copy(0.1f), CircleShape)) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White)
                        }
                    }
                    
                    Spacer(Modifier.height(15.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(85.dp).clip(RoundedCornerShape(25.dp)).background(Color.White.copy(0.1f)).border(2.dp, Color.White.copy(0.2f), RoundedCornerShape(25.dp)), Alignment.Center) {
                            val pic = profile?.basic?.get("profile_pic")?.toString()
                            if (!pic.isNullOrEmpty()) {
                                AsyncImage(model = pic, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text(profile?.basic?.get("full_name")?.toString()?.take(2)?.uppercase() ?: "ST", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        
                        Spacer(Modifier.width(20.dp))
                        
                        Column {
                            Text(profile?.basic?.get("full_name")?.toString()?.uppercase() ?: "LOADING...", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("${profile?.basic?.get("designation") ?: "STAFF"} • BPS: ${profile?.basic?.get("bps") ?: "N/A"}", color = Color.White.copy(0.8f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("STAFF ID: ${profile?.basic?.get("username") ?: "N/A"}", color = Color.White.copy(0.6f), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = cardColor,
                contentColor = Color(0xFF3B82F6),
                edgePadding = 20.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = Color(0xFF3B82F6), height = 3.dp)
                    }
                }
            ) {
                tabs.forEachIndexed { index, (label, icon, color) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black) },
                        icon = { Icon(icon, null, modifier = Modifier.size(20.dp), tint = if(selectedTab == index) color else Color.Gray) }
                    )
                }
            }

            if (isLoading && profile == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            } else {
                Box(Modifier.weight(1f).padding(15.dp)) {
                    when (selectedTab) {
                        0 -> StudentIdentityForm(profile?.basic ?: emptyMap(), isDark) { payload ->
                             viewModel.updateStaffProfile(staffId, payload,
                                onSuccess = { Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show() },
                                onError = { Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show() }
                             )
                        }
                        1 -> StaffAttendanceTab(profile?.stats ?: emptyMap(), isDark) { status, date ->
                             viewModel.markStaffAttendance(staffId, status, date) {
                                 Toast.makeText(localContext, "Attendance Marked!", Toast.LENGTH_SHORT).show()
                             }
                        }
                        2 -> StaffSalaryTab(profile?.stats ?: emptyMap(), isDark) { amt, mon, yr, mode ->
                             viewModel.recordSalary(staffId, amt, mon, yr, mode,
                                onSuccess = { Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show() },
                                onError = { Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show() }
                             )
                        }
                        3 -> StaffRoleTab(profile?.basic ?: emptyMap(), isDark)
                        4 -> StaffDetailsTab(profile?.basic ?: emptyMap(), isDark)
                        5 -> StaffCloudTab(profile ?: StaffProfileResponse(status="loading"), isDark, viewModel, staffId)
                        6 -> StudentBiometricTab(profile?.basic ?: emptyMap(), isDark)
                        7 -> StaffResignTab(profile?.basic ?: emptyMap(), isDark) { status ->
                             viewModel.updateStaffStatus(staffId, status,
                                onSuccess = { Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show() },
                                onError = { Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show() }
                             )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffAttendanceTab(stats: Map<String, Any?>, isDark: Boolean, onMark: (String, String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        // Quick Summary
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                StatPill("PRESENT", stats["present"]?.toString() ?: "0", Color(0xFF10B981))
                StatPill("ABSENT", stats["absent"]?.toString() ?: "0", Color(0xFFEF4444))
                StatPill("LEAVE", stats["leave"]?.toString() ?: "0", Color(0xFF8B5CF6))
            }
        }
        
        // Calendar Placeholder (Reusing logic for brevity, but could use full CalendarGrid)
        Text("CALENDAR HISTORY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 5.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
             Column(Modifier.padding(15.dp)) {
                 val rawAtten = stats["attendance"] as? Map<String, String> ?: emptyMap()
                 val calendarMap = rawAtten.entries.mapNotNull { (date, status) ->
                     // Assuming date is yyyy-MM-dd
                     date.split("-").lastOrNull()?.toIntOrNull()?.let { it to status }
                 }.toMap()
                 
                 CalendarGrid(initialStatus = calendarMap, isDark = isDark) { status, date ->
                     onMark(status, date)
                 }
             }
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StaffSalaryTab(stats: Map<String, Any?>, isDark: Boolean, onSave: (Double, String, String, String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    val currentMonth = java.text.SimpleDateFormat("MMMM").format(java.util.Date())
    val currentYear = java.text.SimpleDateFormat("yyyy").format(java.util.Date())
    
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMode by remember { mutableStateOf("Cash") }
    var amountText by remember { mutableStateOf("") }
    
    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val years = (2020..2030).map { it.toString() }
    val modes = listOf("Cash", "Bank Transfer", "Cheque")

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        
        // Ledger Header
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Text("FINANCIAL LEDGER", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Payable Balance:", color = textColor, fontSize = 14.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("Rs. ${stats["balance"] ?: "0"}", color = Color(0xFF10B981), fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        
        // Payment Payout Form
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text("PROCESS PAYOUT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DropdownSelector(selectedMonth, months, Modifier.weight(1f), isDark) { selectedMonth = it }
                    DropdownSelector(selectedYear, years, Modifier.weight(1f), isDark) { selectedYear = it }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DropdownSelector(selectedMode, modes, Modifier.weight(0.4f), isDark) { selectedMode = it }
                    
                    TextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier.weight(0.6f).height(55.dp).clip(RoundedCornerShape(14.dp)),
                        placeholder = { Text("Amount...", color = Color.Gray, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.04f),
                            unfocusedContainerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.04f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                
                Button(
                    onClick = { 
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if(amt > 0) onSave(amt, selectedMonth, selectedYear, selectedMode)
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.AddCard, null, tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("FINALIZE PAYMENT", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StaffRoleTab(basic: Map<String, Any?>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(25.dp)) {
                Box(Modifier.size(60.dp).background(Color(0xFF6366F1).copy(0.1f), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.Badge, null, tint = Color(0xFF6366F1), modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("CURRENT DESIGNATION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(basic["designation"]?.toString() ?: "N/A", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.background(Color(0xFF10B981).copy(0.1f), RoundedCornerShape(4.dp)).padding(6.dp)) {
                         Text("BPS SCORE: ${basic["bps"] ?: "0"}", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Regular Employee", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Text("EMPLOYMENT TIMELINE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(15.dp))
                DetailItem("Joining Date", basic["created_at"]?.toString() ?: "N/A", textColor)
                DetailItem("Username", basic["username"]?.toString() ?: "N/A", textColor)
                DetailItem("System Role", basic["role"]?.toString() ?: "N/A", textColor)
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StaffDetailsTab(basic: Map<String, Any?>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Text("PERSONAL & HEALTH", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(15.dp))
                DetailItem("Full Name", basic["full_name"]?.toString() ?: "N/A", textColor)
                DetailItem("Father Name", basic["father_name"]?.toString() ?: "N/A", textColor)
                DetailItem("Gender", basic["gender"]?.toString() ?: "N/A", textColor)
                DetailItem("DOB", basic["dob"]?.toString() ?: "N/A", textColor)
                DetailItem("CNIC", basic["cnic"]?.toString() ?: "N/A", textColor)
                DetailItem("Tribe", basic["tribe"]?.toString() ?: "N/A", textColor)
                DetailItem("Address", basic["address"]?.toString() ?: "N/A", textColor)
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Text("TRANSPORT & SOCIAL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(15.dp))
                DetailItem("WhatsApp", basic["whatsapp_no"]?.toString() ?: "N/A", textColor)
                DetailItem("Transport", basic["is_transport_active"]?.toString() ?: "No", textColor)
                DetailItem("Facebook", basic["facebook_link"]?.toString() ?: "N/A", textColor)
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StaffCloudTab(data: StaffProfileResponse, isDark: Boolean, viewModel: WantuchViewModel, staffId: Int) {
    var activeSubSection by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    BackHandler(enabled = activeSubSection != null) {
        activeSubSection = null
    }

    if (activeSubSection != null) {
        Box(Modifier.fillMaxSize()) {
            when (activeSubSection) {
                "Contacts" -> ProfileContactsList(data, isDark, 
                    onSave = { fields -> viewModel.saveSectionNode("CONTACT", staffId, fields, {}, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) },
                    onDelete = { id -> viewModel.deleteSectionNode("CONTACT", staffId, id, {}, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) }
                )
                "Education" -> ProfileAcademicsList(data, isDark,
                    onSave = { fields -> viewModel.saveSectionNode("ACADEMIC", staffId, fields, {}, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) },
                    onDelete = { id -> viewModel.deleteSectionNode("ACADEMIC", staffId, id, {}, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) }
                )
                "Experience" -> ProfileExperienceList(data, isDark,
                    onSave = { fields -> viewModel.saveSectionNode("EXPERIENCE", staffId, fields, {}, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) },
                    onDelete = { id -> viewModel.deleteSectionNode("EXPERIENCE", staffId, id, {}, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) }
                )
                "Banking" -> ProfileBankList(data, isDark,
                    onSave = { fields -> viewModel.saveSectionNode("BANK", staffId, fields, {}, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) },
                    onDelete = { id -> viewModel.deleteSectionNode("BANK", staffId, id, {}, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }) }
                )
            }
        }
    } else {
        PersonalHubGrid(data, isDark) { activeSubSection = it }
    }
}

@Composable
fun StaffResignTab(basic: Map<String, Any?>, isDark: Boolean, onAction: (String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    val currentStatus = basic["status"]?.toString() ?: "active"
    
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).background(Color(0xFFEF4444).copy(0.1f), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("STAFF RESIGNATION", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Process official resignation for this faculty member. Once resigned, the account will be deactivated.", 
                    color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                
                Spacer(Modifier.height(30.dp))
                
                if (currentStatus == "active") {
                    Button(
                        onClick = { onAction("resigned") },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFFEF4444))
                    ) {
                        Text("PROCESS RESIGNATION", color = Color.White, fontWeight = FontWeight.Black)
                    }
                } else {
                    Button(
                        onClick = { onAction("active") },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF10B981))
                    ) {
                        Text("RESTORE TO ACTIVE", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun MyProfileScreen(
    staffId: Int,
    viewModel: WantuchViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.staffProfile.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(staffId) {
        viewModel.fetchStaffProfile(staffId)
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // Cool Action Bar
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, Modifier.background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), CircleShape)) {
                    Icon(Icons.Default.ArrowBack, null, tint = textColor)
                }
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.background(Color(0xFF3B82F6).copy(0.1f), RoundedCornerShape(30.dp)).padding(horizontal = 15.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!profile?.institution?.get("logo")?.toString().isNullOrEmpty()) {
                                AsyncImage(model = "https://www.wantuch.pk/${profile?.institution?.get("logo")}", contentDescription = null, modifier = Modifier.size(16.dp).clip(CircleShape))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(profile?.institution?.get("name")?.toString()?.uppercase() ?: "WANTUCH", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Text("FACULTY HUB", color = textColor.copy(0.4f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                IconButton(onClick = { viewModel.toggleTheme() }, Modifier.background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), CircleShape)) {
                    Icon(Icons.Default.Palette, null, tint = textColor)
                }
            }

            Spacer(Modifier.height(10.dp))
            ProfileHeaderCard(profile?.basic, isDark)
            Spacer(Modifier.height(25.dp))

            // The Dual Path Switcher
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(54.dp).background(cardColor, RoundedCornerShape(16.dp)).padding(5.dp)
            ) {
                ProfileTabButton("Personal Cloud", selectedTab == 0, Modifier.weight(1f), isDark) { selectedTab = 0 }
                ProfileTabButton("Institution Node", selectedTab == 1, Modifier.weight(1f), isDark) { selectedTab = 1 }
            }

            Spacer(Modifier.height(25.dp))

            Box(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                profile?.let { data ->
                    if (selectedTab == 0) {
                        StaffCloudTab(data, isDark, viewModel, staffId)
                    } else {
                        InstitutionHubGrid(data, isDark) { section ->
                            Toast.makeText(context, "Node Section: $section", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            }
        }
    }
}

@Composable
fun ProfileHeaderCard(u: Map<String, Any?>?, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).background(cardColor, RoundedCornerShape(24.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        // Profile Pic
        Box(Modifier.size(70.dp).background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)), Alignment.Center) {
            val pic = u?.get("profile_pic")?.toString()
            if (!pic.isNullOrEmpty()) {
                AsyncImage(model = pic, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(u?.get("full_name")?.toString()?.take(2)?.uppercase() ?: "US", color = textColor.copy(0.3f), fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.width(20.dp))
        
        Column {
            Text(u?.get("full_name")?.toString() ?: "N/A", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("ROLE: ${u?.get("role")?.toString()?.uppercase() ?: "STAFF"}", color = textColor.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.background(Color(0xFF3B82F6).copy(0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("SECURE IDENTITY NODE", color = Color(0xFF3B82F6), fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun InstitutionHubGrid(data: StaffProfileResponse, isDark: Boolean, onSectionClick: (String) -> Unit) {
    val sections = listOf(
        Triple("Profile", Icons.Default.School, Color(0xFF3B82F6)),
        Triple("Posts", Icons.Default.Campaign, Color(0xFF10B981)),
        Triple("Assets", Icons.Default.Category, Color(0xFF8B5CF6)),
        Triple("Funds", Icons.Default.Payments, Color(0xFFEAB308)),
        Triple("Schedules", Icons.Default.CalendarToday, Color(0xFFF43F5E)),
        Triple("Bank Nodes", Icons.Default.AccountBalance, Color(0xFF6366F1))
    )

    Column(Modifier.verticalScroll(rememberScrollState())) {
        sections.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                row.forEach { (label, icon, color) ->
                    val cardBg = if(isDark) Color(0xFF1E293B) else Color.White
                    Box(
                        Modifier.weight(1f).height(130.dp).background(cardBg, RoundedCornerShape(12.dp)).border(1.dp, if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp)).clickable { onSectionClick(label) }.padding(20.dp), 
                        Alignment.Center
                    ) {
                        Box(Modifier.size(6.dp).align(Alignment.TopStart).offset(x = 10.dp, y = 10.dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.TopEnd).offset(x = (-10).dp, y = 10.dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.BottomStart).offset(x = 10.dp, y = (-10).dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.BottomEnd).offset(x = (-10).dp, y = (-10).dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(label.uppercase(), color = if(isDark) Color.White else Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            
                            val subLabel = when(label) {
                                "Profile" -> data.institution?.get("type")?.toString()?.uppercase() ?: "NODE"
                                "Posts" -> {
                                    val total = data.inst_posts?.sumOf { (it["total_posts"] as? Number)?.toInt() ?: 0 } ?: 0
                                    val filled = data.inst_posts?.sumOf { (it["filled_posts"] as? Number)?.toInt() ?: 0 } ?: 0
                                    "$filled / $total"
                                }
                                "Assets" -> {
                                    val count = data.inst_assets?.size ?: 0
                                    "$count ITEMS"
                                }
                                "Funds" -> {
                                    val total = data.inst_funds?.sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 } ?: 0.0
                                    "Rs. ${String.format("%.0f", total)}"
                                }
                                "Schedules" -> {
                                    val count = data.inst_timetable?.groupBy { "${it["class_id"]}-${it["section_id"]}" }?.size ?: 0
                                    "$count ACTIVE"
                                }
                                "Bank Nodes" -> {
                                    val count = data.inst_bank?.size ?: 0
                                    "$count NODES"
                                }
                                else -> "NODE"
                            }
                            
                            Box(Modifier.padding(top = 4.dp).background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(subLabel, color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(15.dp))
        }
    }
}

@Composable
fun ProfileTabButton(label: String, isSelected: Boolean, modifier: Modifier, isDark: Boolean, onClick: () -> Unit) {
    val bgColor = if(isSelected) (if(isDark) Color.Black else Color(0xFF0F172A)) else Color.Transparent
    val textColor = if(isSelected) Color(0xFF3B82F6) else (if(isDark) Color.White.copy(0.4f) else Color.Black.copy(0.3f))
    
    Box(
        modifier.fillMaxHeight().background(bgColor, RoundedCornerShape(8.dp)).clickable { onClick() },
        Alignment.Center
    ) {
        Text(label.uppercase(), color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
fun ProfileEditableField(label: String, value: String?, isDark: Boolean, onValueChange: (String) -> Unit) {
    val containerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    val textColor = if(isDark) Color.White else Color.Black
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box(Modifier.fillMaxWidth().height(45.dp).background(containerColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isNullOrEmpty()) {
                Text(label, color = textColor.copy(0.3f), fontSize = 14.sp)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value ?: "",
                onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDropdownField(label: String, value: String?, options: List<String>, isDark: Boolean, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val containerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    val textColor = if(isDark) Color.White else Color.Black

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            Box(
                Modifier.fillMaxWidth().height(45.dp).background(containerColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(value ?: "Select $label", color = if(value.isNullOrEmpty()) textColor.copy(0.3f) else textColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(if(expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(if(isDark) Color(0xFF1E293B) else Color.White)
            ) {
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption, color = textColor) },
                        onClick = {
                            onValueChange(selectionOption)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDateField(label: String, value: String?, isDark: Boolean, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    val containerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    val textColor = if(isDark) Color.White else Color.Black

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box(
            Modifier.fillMaxWidth().height(45.dp).background(containerColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp).clickable {
                // Show DatePicker Dialog
                val calendar = java.util.Calendar.getInstance()
                val dpd = android.app.DatePickerDialog(context, { _, year, month, dayOfMonth ->
                    onValueChange("$year-${month + 1}-$dayOfMonth")
                }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH))
                dpd.show()
            },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value ?: "YYYY-MM-DD", color = if(value.isNullOrEmpty()) textColor.copy(0.3f) else textColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AddNodeButton(label: String, isDark: Boolean, onClick: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp).background(cardColor, RoundedCornerShape(16.dp)).border(1.dp, if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f), RoundedCornerShape(16.dp)).clickable { onClick() }.padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Add, null, tint = textColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label.uppercase(), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
fun ActionIcons(isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val textColor = if(isDark) Color.White else Color.Black
    val bg = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    Row {
        Box(Modifier.size(36.dp).background(bg, CircleShape).clickable { onEdit() }, Alignment.Center) {
            Icon(Icons.Default.Edit, null, tint = textColor, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(36.dp).background(bg, CircleShape).clickable { onDelete() }, Alignment.Center) {
            Icon(Icons.Default.Delete, null, tint = textColor, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ProfileIdentityForm(data: StaffProfileResponse, isDark: Boolean, onUpdate: (Map<String, String>) -> Unit) {
    val basic = data.basic ?: emptyMap()
    val textColor = if (isDark) Color.White else Color.Black
    
    val formState = remember { mutableStateMapOf<String, String>().apply { 
        basic.forEach { (k, v) -> put(k, v?.toString() ?: "") }
        
        // Prioritize plain password. If only hash is present and it looks like a hash, don't show it to avoid confusion.
        val pass = basic["password"]?.toString() ?: ""
        val hash = basic["password_hash"]?.toString() ?: ""
        
        if (pass.isNotEmpty()) {
            put("password", pass)
        } else if (hash.isNotEmpty() && !hash.startsWith("$2y$")) { 
            // Only show if it's not a bcrypt hash (some old records might have plain text in the hash column)
            put("password", hash)
        } else if (hash.isNotEmpty()) {
            put("password", "") // Don't show the hash characters
        }
    } }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        ProfileDataSection("Account Config", isDark) {
            ProfileEditableField("Username", formState["username"], isDark) { formState["username"] = it }
            ProfileEditableField("Password", formState["password"], isDark) { formState["password"] = it }
            ProfileDropdownField("Role", formState["role"], listOf("super_admin", "admin", "staff", "student"), isDark) { formState["role"] = it }
            ProfileDropdownField("User Type", formState["user_type"], listOf("teaching", "non-teaching"), isDark) { formState["user_type"] = it }
        }
        
        Spacer(Modifier.height(16.dp))
        
        ProfileDataSection("Personal Details", isDark) {
            ProfileEditableField("Full Name", formState["full_name"], isDark) { formState["full_name"] = it }
            ProfileEditableField("Father Name", formState["father_name"], isDark) { formState["father_name"] = it }
            ProfileDropdownField("Gender", formState["gender"], listOf("Male", "Female"), isDark) { formState["gender"] = it }
            ProfileDateField("Date of Birth", formState["dob"], isDark) { formState["dob"] = it }
            ProfileEditableField("CNIC Number", formState["cnic"], isDark) { formState["cnic"] = it }
            ProfileEditableField("Residential Address", formState["address"], isDark) { formState["address"] = it }
        }
        
        Spacer(Modifier.height(16.dp))
        
        ProfileDataSection("Health & Tribe", isDark) {
            ProfileEditableField("Tribe", formState["tribe"], isDark) { formState["tribe"] = it }
            ProfileDropdownField("Religion", formState["religion"], listOf("Islam", "Christianity", "Hinduism", "Sikhism", "Buddhism", "Other"), isDark) { formState["religion"] = it }
            ProfileEditableField("Disability", formState["disability"], isDark) { formState["disability"] = it }
            ProfileEditableField("Permanent Disease", formState["permenent_disease"], isDark) { formState["permenent_disease"] = it }
        }

        Spacer(Modifier.height(16.dp))
        
        ProfileDataSection("Employment & Finance", isDark) {
            ProfileEditableField("Designation", formState["designation"], isDark) { formState["designation"] = it }
            ProfileEditableField("BPS Scale", formState["bps"], isDark) { formState["bps"] = it }
            ProfileEditableField("Monthly Salary", formState["salary"], isDark) { formState["salary"] = it }
            ProfileDropdownField("Account Status", formState["status"], listOf("active", "inactive"), isDark) { formState["status"] = it }
            ProfileDateField("Joined Date", formState["created_at"], isDark) { formState["created_at"] = it }
            ProfileEditableField("Special Bonus", formState["special_bonus"], isDark) { formState["special_bonus"] = it }
        }

        Spacer(Modifier.height(16.dp))
        
        ProfileDataSection("Social & Transport", isDark) {
            ProfileEditableField("WhatsApp No", formState["whatsapp_no"], isDark) { formState["whatsapp_no"] = it }
            ProfileEditableField("Facebook Link", formState["facebook_link"], isDark) { formState["facebook_link"] = it }
            ProfileEditableField("TikTok Link", formState["tiktok_link"], isDark) { formState["tiktok_link"] = it }
            ProfileDropdownField("Transport Active", formState["is_transport_active"], listOf("Yes", "No"), isDark) { formState["is_transport_active"] = it }
            ProfileEditableField("Transport Location", formState["transport_location"], isDark) { formState["transport_location"] = it }
            ProfileEditableField("Transport Charges", formState["transport_charges"], isDark) { formState["transport_charges"] = it }
        }

        Spacer(Modifier.height(30.dp))
        
        Button(
            onClick = { onUpdate(formState.toMap()) },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color.White else Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Upload, null, tint = if(isDark) Color.Black else Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("PUSH IDENTITY UPDATES", color = if(isDark) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun ProfileDataSection(title: String, isDark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val textColor = if (isDark) Color.White else Color.Black
    Column {
        Text(title.uppercase(), color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(16.dp)).padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun ProfileDataField(label: String, value: String?, isDark: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value ?: "N/A", color = if(isDark) Color.White else Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProfileEditDialog(
    title: String,
    fields: List<Triple<String, String, String>>, // Label, Key, Type (text, date, select)
    options: Map<String, List<String>> = emptyMap(), // For select fields
    initialData: Map<String, String>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val formState = remember { mutableStateMapOf<String, String>().apply {
        initialData.forEach { (k, v) -> put(k, v) }
    }}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = if(isDark) Color.White else Color.Black) },
        text = {
            Column {
                fields.forEach { (label, key, type) ->
                    when (type) {
                        "text", "number" -> ProfileEditableField(label, formState[key], isDark) { formState[key] = it }
                        "date" -> ProfileDateField(label, formState[key], isDark) { formState[key] = it }
                        "select" -> ProfileDropdownField(label, formState[key], options[key] ?: emptyList(), isDark) { formState[key] = it }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(formState.toMap()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = if(isDark) Color(0xFF1E293B) else Color.White,
        titleContentColor = if(isDark) Color.White else Color.Black,
        textContentColor = if(isDark) Color.White else Color.Black
    )
}

@Composable
fun ProfileContactsList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.contacts ?: emptyList()
    var editingContact by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingContact != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Contact" else "Edit Contact",
            fields = listOf(
                Triple("Type", "type", "select"),
                Triple("Value", "value", "text"),
                Triple("Detail", "detail", "text")
            ),
            options = mapOf("type" to listOf("Phone", "Email", "WhatsApp", "Facebook", "TikTok", "Instagram")),
            initialData = editingContact ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingContact = null },
            onSave = { fields -> 
                val payload = if(editingContact != null) fields + ("id" to (editingContact!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingContact = null 
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD NEW RECORD", isDark) { isAdding = true }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO RECORDS FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        items.forEach { contact ->
            val cMap = contact.mapValues { it.value?.toString() ?: "" }
            ContactCard(cMap, isDark, onEdit = { editingContact = cMap }, onDelete = { onDelete(cMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun ContactCard(c: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val type = c["type"] ?: "Phone"
    val icon = when(type.lowercase()) {
        "email" -> Icons.Default.Email
        "whatsapp" -> Icons.AutoMirrored.Filled.Chat
        else -> Icons.Default.Phone
    }
    
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF10B981).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(type.uppercase(), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(c["value"] ?: "N/A", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            val detail = c["detail"]
            if (!detail.isNullOrEmpty()) Text(detail, color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileAcademicsList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.academics ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Academic" else "Edit Academic",
            fields = listOf(
                Triple("Degree Title", "degree_title", "text"),
                Triple("Core Subject", "core_subject", "text"),
                Triple("Detail", "detail", "text")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields -> 
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null 
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD NEW RECORD", isDark) { isAdding = true }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO RECORDS FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            AcademicCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun AcademicCard(a: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF3B82F6).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.School, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(a["core_subject"]?.uppercase() ?: "SUBJECT", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(a["degree_title"] ?: "N/A", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(a["detail"] ?: "N/A", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileExperienceList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.experience ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Experience" else "Edit Experience",
            fields = listOf(
                Triple("Field Title", "field_title", "text"),
                Triple("Detail", "detail", "text"),
                Triple("From Date", "from_date", "date"),
                Triple("To Date", "to_date", "date")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields -> 
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null 
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD NEW RECORD", isDark) { isAdding = true }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO RECORDS FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            ExperienceCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun ExperienceCard(e: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF8B5CF6).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.Work, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("${e["from_date"]} - ${e["to_date"] ?: "Present"}", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(e["field_title"] ?: "N/A", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(e["detail"] ?: "N/A", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileBankList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.bank ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Bank Account" else "Edit Bank Account",
            fields = listOf(
                Triple("Bank Name", "bank_name", "text"),
                Triple("Account No", "account_no", "text"),
                Triple("Branch Name", "branch_name", "text"),
                Triple("Account Title", "account_title", "text")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields -> 
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null 
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD NEW RECORD", isDark) { isAdding = true }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO RECORDS FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            BankCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun BankCard(b: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFFEAB308).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.AccountBalance, null, tint = Color(0xFFEAB308), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(b["bank_name"]?.uppercase() ?: "BANK", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            val accNo = b["account_no"] ?: "XXXX XXXX"
            Text(accNo.chunked(4).joinToString(" "), color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${b["account_title"] ?: "N/A"} (${b["branch_name"] ?: "N/A"})", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun PlaceholderSection(title: String, isDark: Boolean) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Build, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
            Text(title.uppercase(), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileInstTimetableList(data: StaffProfileResponse, isDark: Boolean) {
    val items = data.inst_timetable ?: emptyList()
    
    // Group by Class + Section
    val grouped = items.groupBy { "${it["class_name"]} - ${it["section_name"]}" }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO SCHEDULES FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        
        grouped.forEach { (key, list) ->
            Text(key.uppercase(), color = Color(0xFF3B82F6), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            list.forEach { tt ->
                val ttMap = tt.mapValues { it.value?.toString() ?: "" }
                TimetableCard(ttMap, isDark)
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun TimetableCard(tt: Map<String, String>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF3B82F6).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.Schedule, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(tt["subject_name"] ?: "SUBJECT", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${tt["day"]} | ${tt["start_time"]} - ${tt["end_time"]}", color = Color.Gray, fontSize = 11.sp)
        }
        Text("V${tt["timetable_version"]}", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun InstitutionProfileDetail(inst: Map<String, Any?>?, isDark: Boolean, onSave: (Map<String, String>) -> Unit) {
    val textColor = if (isDark) Color.White else Color.Black
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    var isEditing by remember { mutableStateOf(false) }

    if (isEditing) {
        val iMap = inst?.mapValues { it.value?.toString() ?: "" } ?: emptyMap()
        ProfileEditDialog(
            title = "Edit Institution Profile",
            fields = listOf(
                Triple("Institution Name", "name", "text"),
                Triple("Category / Type", "category", "select"),
                Triple("Sector", "sector", "select"),
                Triple("Education Level", "edu_level", "text"),
                Triple("Registration Number", "reg_no", "text"),
                Triple("Full Address", "address", "text"),
                Triple("Contact Number", "contact_no", "text"),
                Triple("Institution Email", "email", "text"),
                Triple("Enable WhatsApp (0/1)", "whatsapp_enabled", "number"),
                Triple("API Provider", "wp_provider", "select"),
                Triple("API Key / Token", "wp_token", "text"),
                Triple("Instance ID / Sender ID", "wp_instance_id", "text")
            ),
            options = mapOf(
                "category" to listOf("School", "College", "University", "Academy", "Madrasa", "Other"),
                "sector" to listOf("Government", "Private", "Semi-Government", "NGO"),
                "wp_provider" to listOf("UltraMsg", "Twilio", "MessageBird", "Other")
            ),
            initialData = iMap,
            isDark = isDark,
            onDismiss = { isEditing = false },
            onSave = { onSave(it); isEditing = false }
        )
    }
    
    Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(100.dp).background(cardColor, RoundedCornerShape(30.dp)).padding(10.dp), Alignment.Center) {
            val logo = inst?.get("logo_path")?.toString()
            if (!logo.isNullOrEmpty()) {
                AsyncImage(model = "https://www.wantuch.pk/$logo", contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                Icon(Icons.Default.School, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(50.dp))
            }
        }
        
        Spacer(Modifier.height(20.dp))
        
        Text(inst?.get("name")?.toString() ?: "INSTITUTION", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(inst?.get("type")?.toString()?.uppercase() ?: "RECOGNIZED NODE", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        
        Spacer(Modifier.height(10.dp))
        Button(onClick = { isEditing = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)), shape = RoundedCornerShape(12.dp)) {
            Text("EDIT BASIC INFO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(30.dp))
        
        ProfileDataSection("Identification Nodes", isDark) {
            ProfileDataField("Category / Type", inst?.get("category")?.toString(), isDark)
            ProfileDataField("Sector / Affiliation", inst?.get("sector")?.toString(), isDark)
            ProfileDataField("Education Level", inst?.get("edu_level")?.toString(), isDark)
            ProfileDataField("Registration No", inst?.get("reg_no")?.toString(), isDark)
            ProfileDataField("Address", inst?.get("address")?.toString(), isDark)
            ProfileDataField("Contact", inst?.get("contact_no")?.toString(), isDark)
            ProfileDataField("E-Mail", inst?.get("email")?.toString(), isDark)
        }

        Spacer(Modifier.height(16.dp))

        ProfileDataSection("WhatsApp Automation", isDark) {
            ProfileDataField("Enabled", if(inst?.get("whatsapp_enabled")?.toString() == "1") "YES" else "NO", isDark)
            ProfileDataField("Provider", inst?.get("wp_provider")?.toString(), isDark)
            ProfileDataField("Instance ID", inst?.get("wp_instance_id")?.toString(), isDark)
        }
        
        Spacer(Modifier.height(16.dp))
        
        ProfileDataSection("Verification Status", isDark) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("VERIFIED INSTITUTION NODE", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun ProfileInstPostsList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.inst_posts ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Post" else "Edit Post",
            fields = listOf(
                Triple("Designation", "designation", "text"),
                Triple("BPS / Pay Scale", "bps", "text"),
                Triple("Total Posts", "total_posts", "number"),
                Triple("Filled Posts", "filled_posts", "number")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields -> 
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null 
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD NEW POST", isDark) { isAdding = true }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO POSTS FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            InstPostCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun InstPostCard(p: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF3B82F6).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.Person, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(p["designation"]?.uppercase() ?: "POST", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("BPS: ${p["bps"] ?: "N/A"}", color = Color.Gray, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("T: ${p["total_posts"]} ", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("F: ${p["filled_posts"]} ", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("V: ${p["vacant_posts"]} ", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileInstBankList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.inst_bank ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Inst Bank" else "Edit Inst Bank",
            fields = listOf(
                Triple("Bank Name", "bank_name", "text"),
                Triple("Branch Name", "branch_name", "text"),
                Triple("Branch Code", "branch_code", "text"),
                Triple("Account No", "account_no", "text"),
                Triple("Detail", "detail", "text")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields -> 
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null 
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD BANK NODE", isDark) { isAdding = true }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            InstBankCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun InstBankCard(b: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFFEAB308).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.AccountBalance, null, tint = Color(0xFFEAB308), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(b["bank_name"] ?: "BANK", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${b["account_no"]} (${b["branch_code"]})", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileInstAssetsList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.inst_assets ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Asset" else "Edit Asset",
            fields = listOf(
                Triple("Asset Name", "asset_name", "text"),
                Triple("Type / Category", "asset_type", "text"),
                Triple("Functional Qty", "functional_qty", "number"),
                Triple("Non-Functional", "non_functional_qty", "number")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields -> 
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null 
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD ASSET NODE", isDark) { isAdding = true }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            InstAssetCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun InstAssetCard(a: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF8B5CF6).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.Build, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a["asset_name"] ?: "ASSET", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.background(Color(0xFF8B5CF6).copy(0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(a["asset_type"]?.uppercase() ?: "GENERAL", color = Color(0xFF8B5CF6), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Functional: ${a["functional_qty"]} | Non-F: ${a["non_functional_qty"]}", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileInstFundsList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.inst_funds ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Fund" else "Edit Fund",
            fields = listOf(
                Triple("Fund Name", "name", "text"),
                Triple("Fund Type", "type", "select"),
                Triple("Account Number", "account_number", "text"),
                Triple("Initial Amount", "amount", "number")
            ),
            options = mapOf(
                "type" to listOf("Reserve", "Welfare", "Development", "Operational", "Other")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields -> 
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null 
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD FUND NODE", isDark) { isAdding = true }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            InstFundCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun InstFundCard(f: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF10B981).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.Payments, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(f["name"] ?: "FUND", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("A/C: ${f["account_number"]} | Rs. ${f["amount"]} (${f["type"]})", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun PersonalHubGrid(data: StaffProfileResponse, isDark: Boolean, onSectionClick: (String) -> Unit) {
    val sections = listOf(
        Triple("Identity", Icons.Default.Fingerprint, Color(0xFF00F3FF)),
        Triple("Contacts", Icons.Default.Phone, Color(0xFF10B981)),
        Triple("Education", Icons.Default.School, Color(0xFF6366F1)),
        Triple("Experience", Icons.Default.Work, Color(0xFFEC4899)),
        Triple("Banking", Icons.Default.AccountBalance, Color(0xFFF59E0B))
    )

    Column(Modifier.verticalScroll(rememberScrollState())) {
        sections.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                row.forEach { (label, icon, color) ->
                    val cardBg = if(isDark) Color(0xFF1E293B) else Color.White
                    Box(
                        Modifier.weight(1f).height(130.dp).background(cardBg, RoundedCornerShape(12.dp)).border(1.dp, if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp)).clickable { onSectionClick(label) }.padding(20.dp), 
                        Alignment.Center
                    ) {
                        Box(Modifier.size(6.dp).align(Alignment.TopStart).offset(x = 10.dp, y = 10.dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.TopEnd).offset(x = (-10).dp, y = 10.dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.BottomStart).offset(x = 10.dp, y = (-10).dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.BottomEnd).offset(x = (-10).dp, y = (-10).dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(label.uppercase(), color = if(isDark) Color.White else Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            
                            val count = when(label) {
                                "Contacts" -> data.contacts?.size ?: 0
                                "Education" -> data.academics?.size ?: 0
                                "Experience" -> data.experience?.size ?: 0
                                "Banking" -> data.bank?.size ?: 0
                                else -> 0
                            }
                            val unit = when(label) {
                                "Education" -> "RECORDS"
                                "Experience" -> "SLOTS"
                                "Banking" -> "ACCOUNTS"
                                else -> "NODES"
                            }
                            
                            Box(Modifier.padding(top = 4.dp).background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("$count $unit", color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(15.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentManagementScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    val data by viewModel.studentsData.collectAsState()
    val structure by viewModel.schoolStructure.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf(0) }
    var selectedSectionId by remember { mutableStateOf(0) }
    var showFilter by remember { mutableStateOf(false) }
    var showAddModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // No default fetch as per user request
        viewModel.fetchSchoolStructure()
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // New Sleek Header
            Box(Modifier.fillMaxWidth().background(cardColor).statusBarsPadding().padding(horizontal = 15.dp, vertical = 20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, Modifier.background(Color.Black.copy(0.05f), CircleShape)) {
                            Icon(Icons.Default.ArrowBack, null, tint = textColor)
                        }
                        Spacer(Modifier.width(15.dp))
                        Column(Modifier.weight(1f)) {
                            Text("STUDENTS", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Manage Admissions & Withdrawals", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { viewModel.fetchStudents(selectedClassId, selectedSectionId) }, Modifier.background(Color.Black.copy(0.05f), CircleShape)) {
                            Icon(Icons.Default.Refresh, null, tint = textColor)
                        }
                    }
                    
                    // Default selection removed as per user request
                    
                    Spacer(Modifier.height(20.dp))
                    
                    // Search & Filter Row
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name or username...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f),
                                unfocusedContainerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }
                        )
                        IconButton(
                            onClick = { showFilter = true },
                            modifier = Modifier.size(50.dp).background(if(selectedClassId > 0) Color(0xFF3B82F6) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.FilterList, null, tint = if(selectedClassId > 0) Color.White else Color.Gray)
                        }
                    }
                }
            }

            if (isLoading && data == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            } else {
                val students = data?.students?.filter { 
                    it.name.contains(searchQuery, true) || it.username.contains(searchQuery, true)
                } ?: emptyList()

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(15.dp, 10.dp, 15.dp, 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Stats Row
                    item {
                        data?.stats?.let { stats ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StudentHeaderStat("TOTAL", stats["total"]?.toString() ?: "0", Color(0xFF6366F1), isDark, Modifier.weight(1f))
                                StudentHeaderStat("PRESENT", stats["present"]?.toString() ?: "0", Color(0xFF10B981), isDark, Modifier.weight(1f))
                                StudentHeaderStat("ABSENT", stats["absent"]?.toString() ?: "0", Color(0xFFEF4444), isDark, Modifier.weight(1f))
                            }
                        }
                    }

                    if (students.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                val msg = if (selectedClassId == 0) "PLEASE SELECT A CLASS TO VIEW STUDENTS" else "No students found matches your search."
                                Text(msg, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(students) { student ->
                        StudentListCard(student, isDark) { onOpenProfile(student.id) }
                    }
                }
            }
        }

        // Add Student FAB
        Box(Modifier.fillMaxSize().padding(20.dp), Alignment.BottomEnd) {
             FloatingActionButton(
                 onClick = { showAddModal = true },
                 containerColor = Color(0xFF3B82F6),
                 contentColor = Color.White
             ) {
                 Icon(Icons.Default.Add, null)
             }
        }
    }

    // Enhanced Tabbed Enrollment Modal
    if (showAddModal) {
        EnrollStudentModal(
            viewModel = viewModel,
            structure = structure,
            isDark = isDark,
            onDismiss = { showAddModal = false }
        )
    }

    // Filter Dialog with Sections
    if (showFilter) {
        AlertDialog(
            onDismissRequest = { showFilter = false },
            containerColor = cardColor,
            title = { Text("Filter by Class", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(onClick = { selectedClassId = 0; selectedSectionId = 0; viewModel.fetchStudents(0, 0); showFilter = false }) {
                        Text("ALL CLASSES", color = if(selectedClassId == 0) Color(0xFF3B82F6) else Color.Gray, fontWeight = FontWeight.Black)
                    }
                    structure?.classes?.forEach { cls ->
                        Column {
                            TextButton(onClick = { selectedClassId = cls.id; selectedSectionId = 0; viewModel.fetchStudents(cls.id, 0); showFilter = false }) {
                                Text(cls.name.uppercase(), color = if(selectedClassId == cls.id && selectedSectionId == 0) Color(0xFF3B82F6) else textColor, fontWeight = FontWeight.Bold)
                            }
                            cls.sections?.forEach { sec ->
                                TextButton(
                                    onClick = { selectedClassId = cls.id; selectedSectionId = sec.id; viewModel.fetchStudents(cls.id, sec.id); showFilter = false },
                                    modifier = Modifier.padding(start = 20.dp)
                                ) {
                                    Text(sec.name.uppercase(), color = if(selectedSectionId == sec.id) Color(0xFF3B82F6) else Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFilter = false }) { Text("CLOSE") } }
        )
    }
}

@Composable
fun StudentHeaderStat(label: String, value: String, accent: Color, isDark: Boolean, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(if(isDark) Color(0xFF1E293B) else Color.White).shadow(2.dp).padding(12.dp)) {
        Column {
            Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun StudentListCard(st: StudentMember, isDark: Boolean, onClick: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(0.05f)), Alignment.Center) {
                if (!st.profile_pic.isNullOrEmpty()) {
                    AsyncImage(model = st.profile_pic, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Text(st.initials, color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(st.name, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text("${st.class_section} • #${st.class_no}", color = Color.Gray, fontSize = 11.sp)
                Text(st.username, color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.background(
                when(st.marked) {
                    "Present" -> Color(0xFF10B981).copy(0.1f)
                    "Absent" -> Color(0xFFEF4444).copy(0.1f)
                    "Leave" -> Color(0xFFF59E0B).copy(0.1f)
                    else -> Color.Gray.copy(0.1f)
                }, RoundedCornerShape(8.dp)
            ).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(st.marked.uppercase(), 
                    color = when(st.marked) {
                        "Present" -> Color(0xFF10B981)
                        "Absent" -> Color(0xFFEF4444)
                        "Leave" -> Color(0xFFF59E0B)
                        else -> Color.Gray
                    }, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun StudentProfileScreen(
    studentId: Int,
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    val profile by viewModel.studentProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        Triple("PROFILE", Icons.Default.AccountCircle, Color(0xFF3B82F6)),
        Triple("ATTEN", Icons.Default.EventAvailable, Color(0xFF10B981)),
        Triple("RESULT", Icons.Default.Assignment, Color(0xFF8B5CF6)),
        Triple("FEE", Icons.Default.Payments, Color(0xFFF59E0B)),
        Triple("ROLE", Icons.Default.Badge, Color(0xFF6366F1)),
        Triple("DETAILS", Icons.Default.Description, Color(0xFFEC4899)),
        Triple("BIO", Icons.Default.Fingerprint, Color(0xFF00F3FF)),
        Triple("STRUCK OFF", Icons.Default.Block, Color(0xFFEF4444))
    )

    val localContext = LocalContext.current
    LaunchedEffect(studentId) {
        viewModel.fetchStudentProfile(studentId)
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // Cool Profile Header
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                // Cover
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF6366F1)))))
                
                Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = onBack, Modifier.background(Color.White.copy(0.1f), CircleShape)) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        IconButton(onClick = { /* More options */ }, Modifier.background(Color.White.copy(0.1f), CircleShape)) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(85.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.2f)).padding(3.dp)) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)).background(Color.White)) {
                                if (profile?.basic?.get("profile_pic")?.toString()?.isNotEmpty() == true) {
                                    AsyncImage(model = profile?.basic?.get("profile_pic").toString(), contentDescription = null, contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.fillMaxSize().padding(15.dp))
                                }
                            }
                        }
                        Spacer(Modifier.width(15.dp))
                        Column {
                            Text(profile?.basic?.get("full_name")?.toString() ?: "Loading...", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Text("${profile?.basic?.get("class_name")} • ${profile?.basic?.get("section_name")}", color = Color.White.copy(0.8f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("ROLL NO: ${profile?.basic?.get("class_no")}", color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Sleek Text-Only Tab Grid
            Column(Modifier.fillMaxWidth().background(cardColor).padding(10.dp)) {
                tabs.chunked(4).forEach { rowTabs ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTabs.forEach { (title, _, color) ->
                            val index = tabs.indexOfFirst { it.first == title }
                            val isSelected = selectedTab == index
                            
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if(isSelected) color else Color.Transparent)
                                    .clickable { selectedTab = index },
                                Alignment.Center
                            ) {
                                Text(
                                    title, 
                                    color = if(isSelected) Color.White else Color.Gray, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    if(tabs.indexOf(rowTabs.last()) < tabs.size - 1) Spacer(Modifier.height(8.dp))
                }
            }

            if (isLoading && profile == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            } else {
                Box(Modifier.weight(1f).padding(15.dp)) {
                    when (selectedTab) {
                        0 -> StudentIdentityForm(profile?.basic ?: emptyMap(), isDark) { fields ->
                             val sid = (profile?.basic?.get("id") as? Number)?.toInt() ?: 0
                             val mutableFields = fields.toMutableMap()
                             mutableFields["id"] = sid.toString()
                             viewModel.saveStudent(mutableFields, 
                                onSuccess = { msg -> Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show() },
                                onError = { err -> Toast.makeText(localContext, err, Toast.LENGTH_SHORT).show() }
                             )
                        }
                        1 -> StudentAttendanceTab(profile?.stats ?: emptyMap(), isDark, onMark = { status, date ->
                             viewModel.markStudentAttendance(studentId, status, date) {
                                 Toast.makeText(localContext, "Attendance Marked: $status", Toast.LENGTH_SHORT).show()
                             }
                        })
                        2 -> StudentResultsTab(profile?.stats ?: emptyMap(), isDark)
                        3 -> StudentFeeTab(profile?.stats ?: emptyMap(), isDark) { amt, mode, cat, mon, yr ->
                             viewModel.collectFee(studentId, amt, mode, cat, mon, yr,
                                onSuccess = { msg -> Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show() },
                                onError = { err -> Toast.makeText(localContext, err, Toast.LENGTH_SHORT).show() }
                             )
                        }
                        4 -> StudentRoleTab(profile?.basic ?: emptyMap(), isDark) { role ->
                             viewModel.updateStudentRole(studentId, role,
                                onSuccess = { msg -> Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show() },
                                onError = { err -> Toast.makeText(localContext, err, Toast.LENGTH_SHORT).show() }
                             )
                        }
                        5 -> StudentDetailsTab(profile?.basic ?: emptyMap(), isDark)
                        6 -> StudentBiometricTab(profile?.basic ?: emptyMap(), isDark)
                        7 -> StudentStruckOffTab(profile?.basic ?: emptyMap(), isDark) { status ->
                             viewModel.changeStudentStatus(studentId, status,
                                onSuccess = { msg -> Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show() },
                                onError = { err -> Toast.makeText(localContext, err, Toast.LENGTH_SHORT).show() }
                             )
                        }
                    }
                }
            }
        }
        
        // Delete Fab
        Box(Modifier.fillMaxSize().padding(20.dp), Alignment.BottomEnd) {
             FloatingActionButton(
                 onClick = { 
                     viewModel.deleteStudent(studentId) { 
                        Toast.makeText(localContext, "Student Deleted", Toast.LENGTH_SHORT).show()
                        onBack()
                     }
                 },
                 containerColor = Color(0xFFEF4444),
                 contentColor = Color.White
             ) {
                 Icon(Icons.Default.Delete, null)
             }
        }
    }
}

@Composable
fun StudentIdentityForm(basic: Map<String, Any?>, isDark: Boolean, onUpdate: (Map<String, String>) -> Unit) {
    val formState = remember { mutableStateMapOf<String, String>() }
    
    LaunchedEffect(basic) {
        formState.clear()
        basic.forEach { (k, v) -> formState[k] = v?.toString() ?: "" }
    }
    
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        ProfileDataSection("Identity & Account", isDark) {
            ProfileEditableField("Full Name", formState["full_name"], isDark) { formState["full_name"] = it }
            ProfileEditableField("Username", formState["username"], isDark) { formState["username"] = it }
            ProfileEditableField("Password", formState["password_plain"] ?: "", isDark) { formState["password_plain"] = it }
            ProfileEditableField("Father Name", formState["father_name"], isDark) { formState["father_name"] = it }
            ProfileDropdownField("Gender", formState["gender"], listOf("Male", "Female"), isDark) { formState["gender"] = it }
        }

        ProfileDataSection("Record Details", isDark) {
            ProfileEditableField("Admission No", formState["adm_no"], isDark) { formState["adm_no"] = it }
            ProfileDateField("Admission Date", formState["date_of_admission"], isDark) { formState["date_of_admission"] = it }
            ProfileEditableField("Class No / Roll No", formState["class_no"], isDark) { formState["class_no"] = it }
            ProfileDropdownField("Status", formState["status"], listOf("active", "struck_off", "withdrawn", "graduated"), isDark) { formState["status"] = it }
        }

        ProfileDataSection("Contact & Social", isDark) {
            ProfileEditableField("WhatsApp", formState["whatsapp_no"], isDark) { formState["whatsapp_no"] = it }
            ProfileEditableField("Parent CNIC", formState["parent_cnic_no"], isDark) { formState["parent_cnic_no"] = it }
            ProfileEditableField("Residential Address", formState["address"], isDark) { formState["address"] = it }
        }

        Button(
            onClick = { onUpdate(formState.toMap()) },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("SAVE STUDENT UPDATES", fontWeight = FontWeight.Black)
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentAttendanceTab(stats: Map<String, Any?>, isDark: Boolean, onMark: (String, String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    val month = (stats["month"] as? Map<String, Any?>) ?: emptyMap()
    val year  = (stats["year"]  as? Map<String, Any?>) ?: emptyMap()

    // Calendar data from backend
    val rawCalendar = stats["calendar"]
    val backendCalendar: Map<Int, String> = when (rawCalendar) {
        is Map<*, *> -> rawCalendar.entries
            .mapNotNull { (k, v) -> k.toString().toIntOrNull()?.let { it to (v?.toString() ?: "") } }
            .toMap()
        else -> emptyMap()
    }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        // ── Calendar Card ──────────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(16.dp)) {
                CalendarGrid(
                    initialStatus = backendCalendar,
                    isDark = isDark,
                    onMark = onMark
                )
            }
        }

        // ── Monthly Stats ──────────────────────────────────────────────────────────
        Text("MONTHLY STATS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("PRESENT", month["present"]?.toString() ?: "0", Color(0xFF10B981), Modifier.weight(1f), isDark)
            StatBox("ABSENT",  month["absent"]?.toString()  ?: "0", Color(0xFFEF4444), Modifier.weight(1f), isDark)
            StatBox("LEAVE",   month["leave"]?.toString()   ?: "0", Color(0xFFFFB300), Modifier.weight(1f), isDark)
        }

        // ── Academic Year ──────────────────────────────────────────────────────────
        Text("ACADEMIC YEAR", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("PRESENT", year["present"]?.toString() ?: "0", Color(0xFF10B981), Modifier.weight(1f), isDark)
            StatBox("ABSENT",  year["absent"]?.toString()  ?: "0", Color(0xFFEF4444), Modifier.weight(1f), isDark)
            StatBox("LEAVE",   year["leave"]?.toString()   ?: "0", Color(0xFFFFB300), Modifier.weight(1f), isDark)
        }

        StatBox("All Time Days", year["total"]?.toString() ?: "0", Color(0xFF3B82F6), Modifier.fillMaxWidth(), isDark)

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun CalendarGrid(initialStatus: Map<Int, String>, isDark: Boolean, onMark: (String, String) -> Unit) {
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    val calendarState = remember(initialStatus) {
        mutableStateMapOf<Int, String>().also { it.putAll(initialStatus) }
    }

    val statusCycle = listOf(null, "Present", "Absent", "Leave", "Short", "Public Holiday")
    fun nextStatus(current: String?): String? {
        val idx = statusCycle.indexOf(current)
        return statusCycle[(idx + 1) % statusCycle.size]
    }

    fun statusColor(s: String?): Color = when(s) {
        "Present" -> Color(0xFF10B981)
        "Absent"  -> Color(0xFFEF4444)
        "Public Holiday", "Leave" -> Color(0xFFFFB300)
        "Short"   -> Color(0xFF6366F1)
        else      -> Color(0xFF334155)
    }
    fun statusLabel(s: String?): String = when(s) {
        "Present" -> "P"
        "Absent"  -> "A"
        "Leave"   -> "L"
        "Short"   -> "S"
        "Public Holiday" -> "PH"
        else      -> ""
    }

    val todayDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
    
    val calForMeta = java.util.Calendar.getInstance()
    val daysInMonth = calForMeta.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    calForMeta.set(java.util.Calendar.DAY_OF_MONTH, 1)
    val firstWeekday = calForMeta.get(java.util.Calendar.DAY_OF_WEEK) - 1
    val monthYearLabel = java.text.SimpleDateFormat("MMMM yyyy").format(calForMeta.time)

    Column {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("MARK ATTENDANCE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(monthYearLabel, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))

        // Day Names
        Row(Modifier.fillMaxWidth()) {
            listOf("SUN","MON","TUE","WED","THU","FRI","SAT").forEach { d ->
                Box(Modifier.weight(1f), Alignment.Center) {
                    Text(d, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val totalCells = firstWeekday + daysInMonth
        val rowsCount = (totalCells + 6) / 7
        for (row in 0 until rowsCount) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstWeekday + 1
                    if (day < 1 || day > daysInMonth) {
                        Spacer(Modifier.weight(1f).height(44.dp))
                    } else {
                        val status = calendarState[day]
                        val isToday = (day == todayDay)
                        val isFuture = (day > todayDay)
                        val isSunday = (col == 0)
                        val isFriday = (col == 5)
                        
                        val bgColor = when {
                            isFuture -> Color(0xFF1E293B).copy(if (isDark) 0.6f else 0.08f)
                            status != null -> statusColor(status)
                            isSunday -> Color(0xFF452726)
                            isFriday -> Color(0xFF112942)
                            else -> Color(0xFF1E293B).copy(if (isDark) 0.15f else 0.08f)
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(bgColor)
                                .border(width = if (isToday) 2.dp else 0.dp, color = if (isToday) Color.White.copy(0.8f) else Color.Transparent, shape = RoundedCornerShape(10.dp))
                                .then(if (!isFuture) Modifier.clickable {
                                    val next = nextStatus(calendarState[day])
                                    val calInst = java.util.Calendar.getInstance()
                                    calInst.set(java.util.Calendar.DAY_OF_MONTH, day)
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd").format(calInst.time)
                                    if (next == null) {
                                        calendarState.remove(day)
                                        onMark("Delete", dateStr)
                                    } else {
                                        calendarState[day] = next
                                        onMark(next, dateStr)
                                    }
                                } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = day.toString(), color = if(isFuture) Color.Gray.copy(0.5f) else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                if (!isFuture && status != null) {
                                    Text(text = statusLabel(status), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
            if (row < rowsCount - 1) Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(14.dp))

        // Legend
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Present" to "P", "Absent" to "A", "Leave" to "L", "Short" to "S", "Holiday" to "PH").forEach { (lbl, code) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor(if(lbl=="Holiday") "Public Holiday" else lbl)))
                    Spacer(Modifier.width(3.dp))
                    Text(code, color = statusColor(if(lbl=="Holiday") "Public Holiday" else lbl), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}


@Composable
fun StudentResultsTab(stats: Map<String, Any?>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("AVG SCORE", "${stats["exam_avg"] ?: "0"}%", Color(0xFF3B82F6), Modifier.weight(1f), isDark)
            
            val status = if((stats["exam_avg"] as? Number)?.toDouble() ?: 0.0 >= 40) "PASSING" else "FAILING"
            val statusColor = if(status == "PASSING") Color(0xFF10B981) else Color(0xFFEF4444)
            StatBox("STATUS", status, statusColor, Modifier.weight(1f), isDark)
        }

        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardColor).padding(20.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("LATEST RESULT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(15.dp))
                Text(stats["last_exam"]?.toString() ?: "No exam record found yet for this student.", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Student performance is calculated based on recent subject tests and terminal examinations.", color = Color.Gray, fontSize = 11.sp)
            }
        }
        
        Spacer(Modifier.height(20.dp))
        Text("PERFORMANCE INSIGHTS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 5.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                listOf("Concept Clarity" to 85, "Attendance Link" to 92, "Assignment Submission" to 78).forEach { (label, pct) ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("$pct%", color = Color(0xFF3B82F6), fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(4.dp).background(Color.Gray.copy(0.1f), CircleShape)) {
                            Box(Modifier.fillMaxWidth(pct/100f).fillMaxHeight().background(Color(0xFF3B82F6), CircleShape))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentFeeTab(stats: Map<String, Any?>, isDark: Boolean, onSave: (Double, String, String, String, String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    val currentMonth = java.text.SimpleDateFormat("MMMM").format(java.util.Date())
    val currentYear = java.text.SimpleDateFormat("yyyy").format(java.util.Date())
    
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMode by remember { mutableStateOf("Cash") }
    var selectedCategory by remember { mutableStateOf("All Fees") }
    var amountText by remember { mutableStateOf("") }
    
    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val years = (2020..2030).map { it.toString() }
    val modes = listOf("Cash", "Bank Transfer", "Cheque")
    val categories = listOf("All Fees", "Tuition Fee", "Admission Fee", "Exam Fee", "Security Fee")

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        
        // Ledger Header
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("STUDENT FEE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Ledger Status", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF00F3FF).copy(0.1f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                         Text("PENDING Rs. ${stats["outstanding_balance"] ?: "0"}", color = Color(0xFF00F3FF), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DropdownSelector(selectedMonth, months, Modifier.weight(1f), isDark) { selectedMonth = it }
                    DropdownSelector(selectedYear, years, Modifier.weight(1f), isDark) { selectedYear = it }
                }
            }
        }
        
        // Payment Form
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DropdownSelector(selectedMode, modes, Modifier.weight(1f), isDark) { selectedMode = it }
                    DropdownSelector(selectedCategory, categories, Modifier.weight(1f), isDark) { selectedCategory = it }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(14.dp)),
                        placeholder = { Text("Amount (Rs.)...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.04f),
                            unfocusedContainerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.04f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Spacer(Modifier.width(10.dp))
                    
                    Button(
                        onClick = { 
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onSave(amt, selectedMode, selectedCategory, selectedMonth, selectedYear)
                        },
                        modifier = Modifier.height(56.dp).clip(RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(Color(0xFF00F3FF)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Save, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("SAVE", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        
        // Fee History
        Text("PAYMENT HISTORY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 5.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                 Text("No records for this period.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
            }
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentRoleTab(basic: Map<String, Any?>, isDark: Boolean, onUpdate: (String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    val currentRole = basic["student_role"]?.toString() ?: "Regular Student"
    var selectedRole by remember { mutableStateOf(currentRole) }
    
    val roles = listOf("Regular Student", "Class Monitor", "In-Charge", "Sports Captain", "Prefect")
    
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(20.dp))
                Text("Student Role", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Assign specific responsibilities to this student.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                
                Spacer(Modifier.height(30.dp))
                
                DropdownSelector(selectedRole, roles, Modifier.fillMaxWidth(), isDark) { selectedRole = it }
                
                Spacer(Modifier.height(20.dp))
                
                Button(
                    onClick = { onUpdate(selectedRole) },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(Color(0xFF3B82F6))
                ) {
                    Text("UPDATE ROLE", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentDetailsTab(basic: Map<String, Any?>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Text("ACADEMIC DETAILS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(15.dp))
                
                // Show bits of info
                DetailItem("Admission Date", basic["admission_date"]?.toString() ?: "N/A", textColor)
                DetailItem("CNIC / B-Form",  basic["cnic"]?.toString() ?: "N/A", textColor)
                DetailItem("Date of Birth",  basic["dob"]?.toString() ?: "N/A", textColor)
                DetailItem("Place of Birth", basic["pob"]?.toString() ?: "N/A", textColor)
                DetailItem("Cast / Clan",    basic["cast"]?.toString() ?: "N/A", textColor)
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp)) {
                Text("CONTACT INFORMATION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(15.dp))
                DetailItem("Father Name", basic["father_name"]?.toString() ?: "N/A", textColor)
                DetailItem("Father Contact", basic["father_contact"]?.toString() ?: "N/A", textColor)
                DetailItem("WhatsApp", basic["whatsapp"]?.toString() ?: "N/A", textColor)
                DetailItem("Current Address", basic["address"]?.toString() ?: "N/A", textColor)
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun DetailItem(label: String, value: String, textColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StudentBiometricTab(basic: Map<String, Any?>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(cardColor.copy(0.5f))) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(0.1f)), Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = textColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(15.dp))
                Column {
                    Text(basic["full_name"]?.toString() ?: "Student", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("READY FOR CONFIGURATION", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
        
        Text("REGISTERED BIOMETRICS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 5.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Face, null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(15.dp))
                Text("FACE REGISTERED", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Button(
                onClick = { },
                modifier = Modifier.weight(1f).height(120.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF8B5CF6).copy(0.1f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Face, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("ENROLL FACE", color = Color(0xFF8B5CF6), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            Button(
                onClick = { },
                modifier = Modifier.weight(1f).height(120.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF10B981).copy(0.1f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Fingerprint, null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("ENROLL FINGER", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        
        OutlinedButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.Upload, null, tint = Color(0xFF00F3FF))
            Spacer(Modifier.width(8.dp))
            Text("UPLOAD PROFILE PHOTO", color = Color(0xFF00F3FF), fontWeight = FontWeight.Black)
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentStruckOffTab(basic: Map<String, Any?>, isDark: Boolean, onAction: (String) -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    val currentStatus = basic["status"]?.toString() ?: "active"
    
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(24.dp))
                Text("Struck OFF student", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Are you sure you want to change this student's status? Struck off students will no longer appear in active attendance rolls.", 
                    color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                
                Spacer(Modifier.height(30.dp))
                
                if (currentStatus == "active") {
                    Button(
                        onClick = { onAction("struck_off") },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFFEF4444))
                    ) {
                        Text("STRUCK OFF NOW", color = Color.White, fontWeight = FontWeight.Black)
                    }
                } else {
                    Button(
                        onClick = { onAction("active") },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF10B981))
                    ) {
                        Text("RESTORE TO ACTIVE", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}


@Composable
fun StaffPillStat(label: String, value: String, color: Color, isDark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun StatBox(label: String, value: String, accent: Color, modifier: Modifier, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(cardColor).padding(15.dp)) {
        Column {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color, textColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = textColor.copy(0.7f), fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun InfoItem(label: String, value: String, icon: ImageVector, textColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EnrollStudentModal(
    viewModel: com.example.wantuch.ui.viewmodel.WantuchViewModel,
    structure: com.example.wantuch.domain.model.SchoolStructureResponse?,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Quick List, 1: Manual
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val context = LocalContext.current

    var selectedClassId by remember { mutableStateOf(0) }
    var selectedSectionId by remember { mutableStateOf(0) }
    var selectedGender by remember { mutableStateOf("Male") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardColor,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("ENROLL STUDENTS", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Dropdowns for Class & Section
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        val className = structure?.classes?.find { it.id == selectedClassId }?.name ?: "Select Class"
                        
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(0.3f))
                        ) {
                            Text(className.uppercase(), color = textColor, fontSize = 12.sp)
                        }
                        
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            structure?.classes?.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text(cls.name.uppercase()) },
                                    onClick = { selectedClassId = cls.id; selectedSectionId = 0; expanded = false }
                                )
                            }
                        }
                    }
                    
                    Box(Modifier.weight(0.6f)) {
                        var expanded by remember { mutableStateOf(false) }
                        val sections = structure?.classes?.find { it.id == selectedClassId }?.sections ?: emptyList()
                        val sectionName = sections.find { it.id == selectedSectionId }?.name ?: "Section"
                        
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(0.3f))
                        ) {
                            Text(sectionName.uppercase(), color = textColor, fontSize = 12.sp)
                        }
                        
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            sections.forEach { sec ->
                                DropdownMenuItem(
                                    text = { Text(sec.name.uppercase()) },
                                    onClick = { selectedSectionId = sec.id; expanded = false }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(15.dp))

                // Tab Switcher
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(0.05f))) {
                    arrayOf("QUICK LIST", "MANUAL ENTRY").forEachIndexed { index, title ->
                        Box(
                            Modifier.weight(1f).height(40.dp).clickable { selectedTab = index }
                                .background(if(selectedTab == index) Color(0xFF3B82F6) else Color.Transparent),
                            Alignment.Center
                        ) {
                            Text(title, color = if(selectedTab == index) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (selectedTab == 0) {
                    // Quick List Tab
                    var namesText by remember { mutableStateOf("") }
                    
                    Column {
                        // Gender Selector for Quick List
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Gender for list:", color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            RadioButton(selected = selectedGender == "Male", onClick = { selectedGender = "Male" })
                            Text("Male", color = textColor, fontSize = 12.sp)
                            RadioButton(selected = selectedGender == "Female", onClick = { selectedGender = "Female" })
                            Text("Female", color = textColor, fontSize = 12.sp)
                        }
                        
                        Text("Paste names (one per line):", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                        
                        OutlinedTextField(
                            value = namesText,
                            onValueChange = { namesText = it },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            placeholder = { Text("1. Ali Khan\n2. Sarah Malik", fontSize = 12.sp, color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color(0xFF3B82F6)
                            )
                        )
                        
                        Spacer(Modifier.height(15.dp))
                        
                        Button(
                            onClick = {
                                if (selectedClassId == 0) { Toast.makeText(context, "Select Class", Toast.LENGTH_SHORT).show(); return@Button }
                                viewModel.bulkSaveStudents(selectedClassId, selectedSectionId, namesText, selectedGender,
                                    onSuccess = { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); onDismiss() },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("ENROLL STUDENTS", fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    // Manual Entry Tab
                    StudentIdentityForm(emptyMap(), isDark) { fields ->
                        if (selectedClassId == 0) { Toast.makeText(context, "Select Class", Toast.LENGTH_SHORT).show(); return@StudentIdentityForm }
                        val fullFields = fields.toMutableMap()
                        fullFields["class_id"] = selectedClassId.toString()
                        fullFields["section_id"] = selectedSectionId.toString()
                        
                        viewModel.saveStudent(fullFields,
                            onSuccess = { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); onDismiss() },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 14.sp, color = if(isDark) Color.White.copy(0.4f) else Color.Gray) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp)) },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = if(isDark) Color.White else Color.Black,
            unfocusedTextColor = if(isDark) Color.White else Color.Black,
            focusedBorderColor = Color(0xFF3B82F6),
            unfocusedBorderColor = if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f),
            focusedContainerColor = if(isDark) Color(0xFF1E293B) else Color.White,
            unfocusedContainerColor = if(isDark) Color(0xFF1E293B) else Color.White
        ),
        keyboardOptions = keyboardOptions,
        singleLine = true
    )
}

@Composable
fun FeeManagementScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenStudentFee: (Int) -> Unit
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val labelColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Intelligence", "Settings & Rules", "Fee Structure", "Students Fee Set", "Transport Setup", "Approvals", "WhatsApp")
    
    // Fetch stats and data
    val dashboardData by viewModel.dashboardData.collectAsState()
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0
    
    val feeDataState = remember { mutableStateOf<org.json.JSONObject?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(instId) {
        if (instId > 0) {
            isLoading.value = true
            viewModel.safeApiCall("GET_FEE_DASHBOARD", mapOf("institution_id" to instId.toString())) { json ->
                feeDataState.value = json
                isLoading.value = false
            }
        }
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderActionIcon(Icons.Default.ArrowBack, isDark, onBack)
                    Spacer(Modifier.width(16.dp))
                    Text("Fee Manager", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                
                // Statistical Header
                feeDataState.value?.optJSONObject("stats")?.let { stats ->
                    FeeStatHeader(stats, isDark)
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Premium Tab Bar (Horizontal Scroll)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF3B82F6),
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF3B82F6),
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) Color(0xFF3B82F6) else labelColor,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                if (isLoading.value) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFF3B82F6))
                } else {
                    FeeTabContent(selectedTab, feeDataState.value, viewModel, isDark, onOpenStudentFee)
                }
            }
        }
    }
}

@Composable
fun FeeStatHeader(stats: JSONObject, isDark: Boolean) {
    val list = listOf(
        Triple("TOTAL CHARGES", stats.optDouble("cumulative_charges", 0.0), Color(0xFF6366F1)),
        Triple("COLLECTED", stats.optDouble("total_collected", 0.0), Color(0xFF10B981)),
        Triple("OUTSTANDING", stats.optDouble("total_outstanding", 0.0), Color(0xFFEF4444)),
        Triple("FINES", stats.optDouble("attendance_fine", 0.0), Color(0xFFF59E0B)),
        Triple("PROJECTED", stats.optDouble("projected_month", 0.0), Color(0xFF3B82F6))
    )

    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(list) { item ->
            Column(
                Modifier.width(110.dp).background(
                    if (isDark) Color(0xFF1E293B) else Color.White,
                    RoundedCornerShape(12.dp)
                ).border(1.dp, if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
                Text(item.first, color = if (isDark) Color.White.copy(0.5f) else Color.Black.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Rs. ${String.format("%,.0f", item.second)}",
                    color = item.third,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun FeeTabContent(tabIdx: Int, data: JSONObject?, viewModel: WantuchViewModel, isDark: Boolean, onOpenStudentFee: (Int) -> Unit) {
    data ?: return
    when (tabIdx) {
        0 -> FeeIntelligenceTab(data, viewModel, isDark, onOpenStudentFee)
        1 -> FeeSettingsTab(data, viewModel, isDark)
        2 -> FeeStructureTab(data, viewModel, isDark)
        3 -> StudentFeeSetTab(data, viewModel, isDark)
        4 -> TransportSetupTab(data, viewModel, isDark)
        5 -> FeeApprovalsTab(data, viewModel, isDark)
        6 -> WhatsAppTab(data, viewModel, isDark)
    }
}

@Composable
fun FeeIntelligenceTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean, onOpenStudentFee: (Int) -> Unit) {
    val context = LocalContext.current
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)

    var selectedMonth by remember { mutableStateOf(java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date())) }
    var selectedYear by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date())) }
    var selectedClass by remember { mutableStateOf("All Classes") }
    var selectedClassId by remember { mutableIntStateOf(0) }
    var selectedSection by remember { mutableStateOf("All Sections") }
    var selectedSectionId by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }

    val students = remember { mutableStateListOf<JSONObject>() }
    val isLoading = remember { mutableStateOf(false) }

    // Use the same schoolStructure the student module uses — already loaded and reliable
    val structure by viewModel.schoolStructure.collectAsState()

    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val years = (2022..2030).map { it.toString() }

    // Build class options from structure (mirrors student filter logic)
    val classOptions = remember(structure) {
        val opts = mutableListOf("All Classes")
        structure?.classes?.forEach { opts.add(it.name) }
        opts
    }
    val classMap = remember(structure) {
        val map = mutableMapOf<String, Int>()
        structure?.classes?.forEach { map[it.name] = it.id }
        map
    }

    // Build section options from selected class in structure
    val sectionOptions = remember(structure, selectedClassId) {
        val opts = mutableListOf("All Sections")
        val cls = structure?.classes?.find { it.id == selectedClassId }
        cls?.sections?.forEach { opts.add(it.name) }
        opts
    }
    val sectionMap = remember(structure, selectedClassId) {
        val map = mutableMapOf<String, Int>()
        val cls = structure?.classes?.find { it.id == selectedClassId }
        cls?.sections?.forEach { map[it.name] = it.id }
        map
    }

    // Fee types from dashboard data
    val feeTypeOptions = remember(data) {
        val opts = mutableListOf("All Types")
        data.optJSONArray("fee_types")?.let { arr ->
            for (i in 0 until arr.length()) opts.add(arr.getJSONObject(i).optString("type_name"))
        }
        opts
    }

    // Trigger fetch of school structure if not already loaded
    LaunchedEffect(instId) {
        if (structure == null && instId > 0) viewModel.fetchSchoolStructure()
    }

    LaunchedEffect(selectedMonth, selectedYear, selectedClassId, selectedSectionId, search) {
        isLoading.value = true
        viewModel.safeApiCall("GET_FEE_INTELLIGENCE", mapOf(
            "institution_id" to instId.toString(),
            "month" to selectedMonth,
            "year" to selectedYear,
            "class_id" to selectedClassId.toString(),
            "section_id" to selectedSectionId.toString(),
            "search" to search
        )) { json ->
            isLoading.value = false
            students.clear()
            json?.optJSONArray("students")?.let { arr ->
                for (i in 0 until arr.length()) {
                    students.add(arr.getJSONObject(i))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Filters Section
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    DropdownSelector(selectedMonth, months, Modifier.fillMaxWidth(), isDark) { selectedMonth = it }
                }
                Box(Modifier.weight(1f)) {
                    DropdownSelector(selectedYear, years, Modifier.fillMaxWidth(), isDark) { selectedYear = it }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    DropdownSelector(selectedClass, classOptions, Modifier.fillMaxWidth(), isDark) { 
                        selectedClass = it
                        selectedClassId = classMap[it] ?: 0
                    }
                }
                Box(Modifier.weight(1f)) {
                    DropdownSelector(selectedSection, sectionOptions, Modifier.fillMaxWidth(), isDark) { 
                        selectedSection = it
                        selectedSectionId = sectionMap[it] ?: 0
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PremiumTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search Name or Username...",
                icon = Icons.Default.Search,
                isDark = isDark
            )
        }

        if (isLoading.value) {
            Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(students.size) { index ->
                    val student = students[index]
                    StudentFeeListItem(student, isDark) { onOpenStudentFee(student.optInt("id")) }
                }
                
                if (students.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 40.dp), Alignment.Center) {
                            Text("No students found covering these filters", color = labelColor, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentFeeListItem(student: JSONObject, isDark: Boolean, onClick: () -> Unit) {
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    
    val status = student.optString("status")
    val statusColor = when(status) {
        "PAID" -> Color(0xFF10B981)
        "PARTIAL" -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Surface(
        onClick = onClick,
        color = cardColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)),
        shadowElevation = 2.dp
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).clip(CircleShape).background(statusColor.copy(0.1f)), Alignment.Center) {
                val pic = student.optString("profile_pic")
                if (pic.isNotEmpty()) {
                    AsyncImage(model = pic, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(student.optString("name").take(2), color = statusColor, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                Text(student.optString("name"), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(student.optString("class_section"), color = labelColor, fontSize = 11.sp)
                Text("D/O: ${student.optString("father_name")}", color = labelColor, fontSize = 10.sp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("Payable: ${student.optDouble("payable")}", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = statusColor.copy(0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        status,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeeSettingsTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    val settings = data.optJSONObject("settings") ?: JSONObject()
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = Color.Gray
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        // Automation Rules
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(1.dp, Color.Gray.copy(0.2f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Automation Rules", fontWeight = FontWeight.Bold, color = textColor)
                }
                Text("Configure when fees are automatically generated.", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                var status by remember { mutableStateOf(settings.optString("fee_automation_status", "On")) }
                var day by remember { mutableStateOf(settings.optString("fee_automation_day", "25")) }
                var targetMonth by remember { mutableStateOf(settings.optString("fee_target_month", "January")) }
                var targetYear by remember { mutableStateOf(settings.optString("fee_target_year", "2024")) }

                Text("AUTOMATION STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                DropdownSelector(status, listOf("On", "Off"), Modifier.fillMaxWidth(), isDark) { status = it }
                Spacer(Modifier.height(8.dp))

                Text("RUN ON DAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                DropdownSelector(day, (1..28).map { it.toString() }, Modifier.fillMaxWidth(), isDark) { day = it }
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("TARGET MONTH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetMonth, listOf("January","February","March","April","May","June","July","August","September","October","November","December"), Modifier.fillMaxWidth(), isDark) { targetMonth = it }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("TARGET YEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetYear, (2020..2030).map { it.toString() }, Modifier.fillMaxWidth(), isDark) { targetYear = it }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.safeFeeApiCall("save_automation_settings", mapOf(
                            "state" to if (status == "On") "1" else "0", 
                            "day" to day, 
                            "month" to targetMonth, 
                            "year" to targetYear
                        )) {
                            // Handled via toast or snackbar
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE SETTINGS")
                }
                
                Spacer(Modifier.height(8.dp))
                Text(
                    "Manual Trigger\nInstantly generate fees for students who haven't been processed yet. This respects your \"Target Month\" setting above and will skip duplicates.",
                    fontSize = 11.sp, color = labelColor, modifier = Modifier.padding(horizontal = 4.dp), textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.safeFeeApiCall("run_automation_now", mapOf()) {} },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RUN GENERATION NOW", color = textColor)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Global Adjustments
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(1.dp, Color.Gray.copy(0.2f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Global Adjustments", fontWeight = FontWeight.Bold, color = textColor)
                }
                Text("Apply bulk changes across the entire institution.", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                var cat by remember { mutableStateOf("Tuition Fee") }
                var action by remember { mutableStateOf("% Increase") }
                var valStr by remember { mutableStateOf("") }
                
                Text("CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                DropdownSelector(cat, listOf("Tuition Fee", "Transport Fee", "Fine"), Modifier.fillMaxWidth(), isDark) { cat = it }
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("ACTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(action, listOf("% Increase", "% Decrease", "Fixed Add", "Fixed Sub"), Modifier.fillMaxWidth(), isDark) { action = it }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("VALUE (AMT OR %)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        PremiumTextField(valStr, { valStr = it }, "e.g. 5 or 500", Icons.Default.Add, isDark)
                    }
                }
                Spacer(Modifier.height(16.dp))

                var targetMonth by remember { mutableStateOf(java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date())) }
                var targetYear by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("TARGET MONTH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetMonth, listOf("January","February","March","April","May","June","July","August","September","October","November","December"), Modifier.fillMaxWidth(), isDark) { targetMonth = it }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("TARGET YEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetYear, (2020..2030).map { it.toString() }, Modifier.fillMaxWidth(), isDark) { targetYear = it }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.safeFeeApiCall("apply_global_adjustment", mapOf(
                            "category" to cat, 
                            "adjust_action" to action, 
                            "value" to valStr,
                            "month" to targetMonth,
                            "year" to targetYear
                        )) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("APPLY CHANGES")
                }
                
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.safeFeeApiCall("sync_absence_fines", mapOf("month" to targetMonth, "year" to targetYear)) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B5CF6))
                ) {
                    Text("UPDATE ABSENCE FINES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Bulk Fee Assignment
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(1.dp, Color.Gray.copy(0.2f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bulk Fee Assignment", fontWeight = FontWeight.Bold, color = textColor)
                }
                Text("Assign specific fees to multiple students at once.", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                var scope by remember { mutableStateOf("Globally (All Students)") }
                var feeTypeIdx by remember { mutableStateOf(0) }
                var bulkAmt by remember { mutableStateOf("") }
                
                val fTypesArr = data.optJSONArray("fee_types")
                val fTypeNames = mutableListOf<String>()
                for(i in 0 until (fTypesArr?.length() ?: 0)) fTypeNames.add(fTypesArr!!.getJSONObject(i).optString("type_name"))

                Text("SCOPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                DropdownSelector(scope, listOf("Globally (All Students)", "Class-wise", "Section-wise"), Modifier.fillMaxWidth(), isDark) { scope = it }
                Spacer(Modifier.height(8.dp))

                Text("FEE TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                DropdownSelector(if(fTypeNames.isNotEmpty()) fTypeNames[feeTypeIdx] else "No Types", fTypeNames, Modifier.fillMaxWidth(), isDark) { feeTypeIdx = fTypeNames.indexOf(it) }
                Spacer(Modifier.height(8.dp))

                Text("AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                PremiumTextField(bulkAmt, { bulkAmt = it }, "Enter amount", Icons.Default.Money, isDark)
                Spacer(Modifier.height(16.dp))

                var targetMonth by remember { mutableStateOf(java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date())) }
                var targetYear by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("MONTH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetMonth, listOf("January","February","March","April","May","June","July","August","September","October","November","December"), Modifier.fillMaxWidth(), isDark) { targetMonth = it }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("YEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        DropdownSelector(targetYear, (2020..2030).map { it.toString() }, Modifier.fillMaxWidth(), isDark) { targetYear = it }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val tid = fTypesArr?.optJSONObject(feeTypeIdx)?.optString("id") ?: ""
                        viewModel.safeFeeApiCall("bulk_assign_fee", mapOf(
                            "scope" to scope, 
                            "fee_type_id" to tid, 
                            "amount" to bulkAmt,
                            "month" to targetMonth,
                            "year" to targetYear
                        )) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ASSIGN FEES NOW", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.height(100.dp)) // Padding for bottom
    }
}

@Composable
fun FeeSettingItem(title: String, desc: String, initialValue: Boolean, isDark: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = if(isDark) Color.White.copy(0.5f) else Color.Black.copy(0.5f), fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}

@Composable
fun FeeStructureTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    val classes = data.optJSONArray("classes")
    val feeTypes = data.optJSONArray("fee_types")
    val structure = data.optJSONArray("fee_structure")
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    var editingClass by remember { mutableStateOf<JSONObject?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Class-wise Fee Structure", color = textColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
            TextButton(onClick = { 
                viewModel.safeFeeApiCall("clear_all_fees_for_school", mapOf()) {
                    // Refresh logic
                }
            }) {
                Text("CLEAR ALL", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(classes?.length() ?: 0) { i ->
                val cls = classes?.optJSONObject(i)
                val cid = cls?.optInt("id") ?: 0
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cls?.optString("name") ?: "Class", fontWeight = FontWeight.ExtraBold, color = textColor)
                            TextButton(onClick = { editingClass = cls }, contentPadding = PaddingValues(0.dp)) {
                                Text("UPDATE RATE", color = Color(0xFF3B82F6), fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        
                        feeTypes?.let { types ->
                            for (j in 0 until types.length()) {
                                val type = types.optJSONObject(j)
                                val tid = type.optInt("id")
                                
                                var amount = 0.0
                                structure?.let { sArr ->
                                    for (k in 0 until sArr.length()) {
                                        val s = sArr.getJSONObject(k)
                                        if (s.optInt("class_id") == cid && s.optInt("fee_type_id") == tid) {
                                            amount = s.optDouble("amount", 0.0)
                                            break
                                        }
                                    }
                                }

                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(type.optString("type_name"), color = Color.Gray, fontSize = 12.sp)
                                    Text("Rs. ${amount.toInt()}", color = if(amount > 0) Color(0xFF10B981) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingClass != null) {
        val cid = editingClass!!.optInt("id")
        val rates = remember { mutableStateMapOf<Int, String>() }
        
        // Pre-fill existing rates
        LaunchedEffect(editingClass) {
            feeTypes?.let { types ->
                for(i in 0 until types.length()) {
                    val tid = types.getJSONObject(i).optInt("id")
                    var amt = "0"
                    structure?.let { sArr ->
                        for (k in 0 until sArr.length()) {
                            val s = sArr.getJSONObject(k)
                            if (s.optInt("class_id") == cid && s.optInt("fee_type_id") == tid) {
                                amt = s.optDouble("amount", 0.0).toInt().toString()
                                break
                            }
                        }
                    }
                    rates[tid] = amt
                }
            }
        }

        AlertDialog(
            onDismissRequest = { editingClass = null },
            containerColor = bgColor,
            title = { Text("Update Rates: ${editingClass!!.optString("name")}", color = textColor) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    feeTypes?.let { types ->
                        for(i in 0 until types.length()) {
                            val t = types.getJSONObject(i)
                            val tid = t.optInt("id")
                            Text(t.optString("type_name"), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            PremiumTextField(rates[tid] ?: "0", { rates[tid] = it }, "Amount", Icons.Default.Money, isDark)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val ratesJson = JSONObject()
                    rates.forEach { (k, v) -> ratesJson.put(k.toString(), v) }
                    viewModel.safeFeeApiCall("save_fee_structure_bulk", mapOf("class_id" to cid.toString(), "rates" to ratesJson.toString())) {
                        editingClass = null
                    }
                }) { Text("SAVE CHANGES") }
            }
        )
    }
}

@Composable
fun StudentFeeSetTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0
    val textColor = if (isDark) Color.White else Color.Black
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    
    val allStudents = remember { mutableStateListOf<JSONObject>() }
    var displayedStudents by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Filters
    val structure by viewModel.schoolStructure.collectAsState()
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var month by remember { mutableStateOf(dateToMonth(System.currentTimeMillis())) }
    var year by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()) }

    LaunchedEffect(selectedClass, selectedSection, month, year) {
        isLoading = true
        viewModel.safeApiCall("GET_FEE_INTELLIGENCE", mapOf(
            "institution_id" to instId.toString(),
            "class_id" to (structure?.classes?.find { it.name == selectedClass }?.id?.toString() ?: "0"),
            "section_id" to (structure?.classes?.find { it.name == selectedClass }?.sections?.find { it.name == selectedSection }?.id?.toString() ?: "0"),
            "month" to month,
            "year" to year
        )) { json ->
            isLoading = false
            allStudents.clear()
            json.optJSONArray("students")?.let { arr ->
                for (i in 0 until arr.length()) allStudents.add(arr.getJSONObject(i))
            }
            displayedStudents = allStudents
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Filter UI
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val classNames = mutableListOf("All Classes")
            structure?.classes?.forEach { classNames.add(it.name) }
            Column(Modifier.weight(1f)) {
                DropdownSelector(selectedClass ?: "All Classes", classNames, Modifier.fillMaxWidth(), isDark) { 
                    selectedClass = if(it == "All Classes") null else it
                }
            }
            val sections = mutableListOf("All Sections")
            structure?.classes?.find { it.name == selectedClass }?.sections?.forEach { sections.add(it.name) }
            Column(Modifier.weight(1f)) {
                DropdownSelector(selectedSection ?: "All Sections", sections, Modifier.fillMaxWidth(), isDark) {
                    selectedSection = if(it == "All Sections") null else it
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                DropdownSelector(month, listOf("January","February","March","April","May","June","July","August","September","October","November","December"), Modifier.fillMaxWidth(), isDark) { month = it }
            }
            Column(Modifier.weight(1f)) {
                DropdownSelector(year, (2020..2030).map { it.toString() }, Modifier.fillMaxWidth(), isDark) { year = it }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(displayedStudents) { stu ->
                    val sid = stu.optString("id")
                    val cid = stu.optString("class_id")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                                val pic = stu.optString("profile_pic")
                                Box(Modifier.size(45.dp).clip(CircleShape).background(Color(0xFF3B82F6).copy(0.1f)), Alignment.Center) {
                                    if (pic.isNotEmpty()) {
                                        AsyncImage(model = pic, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp), tint = Color(0xFF3B82F6))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(stu.optString("full_name"), fontWeight = FontWeight.Black, color = textColor)
                                    Text("${stu.optString("class_name")} - ${stu.optString("section_name")}", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    viewModel.safeFeeApiCall("reset_student_fee", mapOf("student_id" to sid, "month" to month, "year" to year)) {
                                        // Trigger re-load for this student
                                    }
                                }) {
                                    Icon(Icons.Default.Refresh, "Reset Billing", tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Divider(color = Color.Gray.copy(0.1f))
                            Spacer(Modifier.height(12.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Tuition Column
                                Column(Modifier.weight(1f)) {
                                    var tuitionState by remember { mutableStateOf(stu.optString("tuition_enabled", "1") == "1") }
                                    var mode by remember { mutableStateOf(stu.optString("tuition_type", "Full")) }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("TUITION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Spacer(Modifier.weight(1f))
                                        Switch(tuitionState, { 
                                            tuitionState = it
                                            viewModel.safeFeeApiCall("toggle_student_fee", mapOf("enroll_id" to sid, "type" to "tuition", "state" to if(it) "1" else "0", "class_id" to cid, "tuition_mode" to mode, "target_month" to month, "target_year" to year)) {}
                                        }, modifier = Modifier.scale(0.7f))
                                    }
                                    if(tuitionState) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            DropdownSelector(mode, listOf("Full", "Half", "Custom"), if(mode == "Custom") Modifier.weight(1.1f) else Modifier.fillMaxWidth(), isDark) {
                                                mode = it
                                                viewModel.safeFeeApiCall("toggle_student_fee", mapOf("enroll_id" to sid, "type" to "tuition", "state" to "1", "class_id" to cid, "tuition_mode" to it, "target_month" to month, "target_year" to year)) {}
                                            }
                                            if (mode == "Custom") {
                                                var customAmt by remember { mutableStateOf(stu.optString("tuition_fee", "0")) }
                                                PremiumTextField(customAmt, { 
                                                    customAmt = it
                                                    viewModel.safeFeeApiCall("toggle_student_fee", mapOf("enroll_id" to sid, "type" to "tuition", "state" to "1", "class_id" to cid, "tuition_mode" to "Custom", "custom_amount" to it, "target_month" to month, "target_year" to year)) {}
                                                }, "Amt", Icons.Default.Money, isDark, modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }

                                // Transport Column
                                Column(Modifier.weight(1f)) {
                                    var transState by remember { mutableStateOf(stu.optString("transport_enabled", "0") == "1") }
                                    val locations = data.optJSONArray("trans_locations")
                                    val locNames = mutableListOf("No Location")
                                    for(i in 0 until (locations?.length() ?: 0)) locNames.add(locations!!.getJSONObject(i).optString("location"))
                                    
                                    var selectedLoc by remember { mutableStateOf(stu.optString("transport_location_id", "0")) }
                                    val locName = locations?.let { arr -> 
                                        for(i in 0 until arr.length()) if(arr.getJSONObject(i).optString("id") == selectedLoc) return@let arr.getJSONObject(i).optString("location")
                                        "No Location"
                                    } ?: "No Location"

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("TRANSPORT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Spacer(Modifier.weight(1f))
                                        Switch(transState, {
                                            transState = it
                                            viewModel.safeFeeApiCall("toggle_student_fee", mapOf("enroll_id" to sid, "type" to "transport", "state" to if(it) "1" else "0", "class_id" to cid, "location_id" to selectedLoc, "target_month" to month, "target_year" to year)) {}
                                        }, modifier = Modifier.scale(0.7f))
                                    }
                                    if(transState) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            val showCustom = (locName == "User Define")
                                            DropdownSelector(locName, locNames, if(showCustom) Modifier.weight(1.1f) else Modifier.fillMaxWidth(), isDark) { name ->
                                                val lid = locations?.let { arr ->
                                                    for(i in 0 until arr.length()) if(arr.getJSONObject(i).optString("location") == name) return@let arr.getJSONObject(i).optString("id")
                                                    "0"
                                                } ?: "0"
                                                selectedLoc = lid
                                                viewModel.safeFeeApiCall("toggle_student_fee", mapOf("enroll_id" to sid, "type" to "transport", "state" to "1", "class_id" to cid, "location_id" to lid, "target_month" to month, "target_year" to year)) {}
                                            }
                                            if (showCustom) {
                                                var customTrans by remember { mutableStateOf(stu.optString("transport_charges", "0")) }
                                                PremiumTextField(customTrans, {
                                                    customTrans = it
                                                    viewModel.safeFeeApiCall("toggle_student_fee", mapOf("enroll_id" to sid, "type" to "transport", "state" to "1", "class_id" to cid, "location_id" to "-1", "custom_amount" to it, "target_month" to month, "target_year" to year)) {}
                                                }, "Amt", Icons.Default.Money, isDark, modifier = Modifier.weight(1f))
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


@Composable
fun TransportSetupTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    val locations = data.optJSONArray("trans_locations")
    val textColor = if (isDark) Color.White else Color.Black
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    
    var selectedSubTab by remember { mutableStateOf(0) }
    val subTabs = listOf("PRICING SETUP", "DRIVER MANAGEMENT", "STAFF TRANSPORT")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Sub-tabs
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            subTabs.forEachIndexed { idx, title ->
                Surface(
                    onClick = { selectedSubTab = idx },
                    modifier = Modifier.weight(1f),
                    color = if(selectedSubTab == idx) Color(0xFF3B82F6) else bgColor,
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(title, modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center, 
                         fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if(selectedSubTab == idx) Color.White else Color.Gray)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        var showAddLocation by remember { mutableStateOf(false) }
        if (showAddLocation) {
            var locName by remember { mutableStateOf("") }
            var locAmt by remember { mutableStateOf("") }
            var locType by remember { mutableStateOf("Bus") }
            
            AlertDialog(
                onDismissRequest = { showAddLocation = false },
                containerColor = bgColor,
                title = { Text("Add Transport Route", color = textColor) },
                text = {
                    Column {
                        PremiumTextField(locName, { locName = it }, "Location Name", Icons.Default.LocationOn, isDark)
                        Spacer(Modifier.height(12.dp))
                        PremiumTextField(locAmt, { locAmt = it }, "Charges", Icons.Default.Money, isDark)
                        Spacer(Modifier.height(12.dp))
                        DropdownSelector(locType, listOf("Bus", "Van", "Coaster", "Other"), Modifier.fillMaxWidth(), isDark) { locType = it }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.safeFeeApiCall("save_transport_location", mapOf("location" to locName, "charges" to locAmt, "ride_type" to locType)) {
                            showAddLocation = false
                            // Refresh logic
                        }
                    }) { Text("ADD ROUTE") }
                }
            )
        }

        if (selectedSubTab == 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Routes & Fares", color = textColor, fontWeight = FontWeight.Black)
                IconButton(onClick = { showAddLocation = true }, modifier = Modifier.background(Color(0xFF3B82F6), RoundedCornerShape(8.dp)).size(30.dp)) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(locations?.length() ?: 0) { i ->
                    val loc = locations?.optJSONObject(i)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFF3B82F6).copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                               Icon(Icons.Default.DirectionsBus, null, Modifier.padding(8.dp).size(18.dp), Color(0xFF3B82F6))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(loc?.optString("location") ?: "", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(loc?.optString("ride_type") ?: "Bus", color = Color.Gray, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rs. ${loc?.optString("charges")}", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(14.dp).clickable { })
                            }
                        }
                    }
                }
            }
        } else if (selectedSubTab == 1) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Driver Management module active on Web", color = Color.Gray, fontSize = 12.sp)
            }
        } else if (selectedSubTab == 2) {
            Column(Modifier.fillMaxSize()) {
                Text("Staff Transport Enrollment", color = textColor, fontWeight = FontWeight.Black)
                Text("Manage transport settings for employees.", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))
                
                // Show a list of staff (we'd need staff list in data)
                val staffArr = data.optJSONArray("staff_list")
                if (staffArr == null || staffArr.length() == 0) {
                   Box(Modifier.fillMaxSize(), Alignment.Center) {
                       Text("No staff data loaded for transport", color = Color.Gray, fontSize = 12.sp)
                   }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(staffArr.length()) { i ->
                            val staff = staffArr.getJSONObject(i)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = Color(0xFF8B5CF6).copy(0.1f), shape = CircleShape) {
                                        Icon(Icons.Default.Person, null, Modifier.padding(8.dp).size(20.dp), Color(0xFF8B5CF6))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(staff.optString("name"), fontWeight = FontWeight.Bold, color = textColor)
                                        Text(staff.optString("designation"), fontSize = 11.sp, color = Color.Gray)
                                    }
                                    // Transport toggle for staff
                                    var stState by remember { mutableStateOf(staff.optString("transport_enabled", "0") == "1") }
                                    Switch(stState, {
                                        stState = it
                                        viewModel.safeFeeApiCall("toggle_staff_transport", mapOf("staff_id" to staff.optString("id"), "state" to if(it) "1" else "0")) {}
                                    }, modifier = Modifier.scale(0.8f))
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
fun FeeApprovalsTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    var approvals by remember { mutableStateOf(data.optJSONArray("approvals") ?: JSONArray()) }
    val textColor = if (isDark) Color.White else Color.Black
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pending Payment Approvals", color = textColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Review and confirm online fee submissions.", fontSize = 11.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        
        if(approvals.length() == 0) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981).copy(0.2f), modifier = Modifier.size(64.dp))
                    Text("All Clear!", color = textColor.copy(0.3f), fontWeight = FontWeight.Bold)
                    Text("No pending payments requiring approval.", fontSize = 10.sp, color = textColor.copy(0.3f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(approvals.length()) { i ->
                    val app = approvals.optJSONObject(i)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(app.optString("student_name"), fontWeight = FontWeight.ExtraBold, color = textColor)
                                    Text("Ref: ${app.optString("transaction_id")}", fontSize = 10.sp, color = Color.Gray)
                                }
                                Text("Rs. ${app.optString("amount")}", color = Color(0xFFF59E0B), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Method: ${app.optString("payment_method")}", fontSize = 12.sp, color = textColor)
                            Spacer(Modifier.height(16.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { 
                                        viewModel.safeFeeApiCall("approve_payment", mapOf("id" to app.optString("id"))) {
                                            // Ideally refresh dashboard data here
                                        }
                                    }, 
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("APPROVE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.safeFeeApiCall("reject_payment", mapOf("id" to app.optString("id"))) {}
                                    }, 
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("REJECT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
fun WhatsAppTab(data: JSONObject, viewModel: WantuchViewModel, isDark: Boolean) {
    val settings = data.optJSONObject("settings") ?: JSONObject()
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(1.dp, Color(0xFF25D366).copy(0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFF25D366))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("WhatsApp Automation", color = Color(0xFF25D366), fontWeight = FontWeight.Black)
                        Text("Send automated fee alerts to parents.", color = Color.Gray, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                
                var enabled by remember { mutableStateOf(settings.optString("whatsapp_enabled", "0") == "1") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Automation", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Switch(enabled, { 
                        enabled = it
                        viewModel.safeFeeApiCall("save_whatsapp_settings", mapOf("whatsapp_enabled" to if(it) "1" else "0")) {}
                    })
                }
                
                Spacer(Modifier.height(12.dp))
                Text("REMINDER DAY 1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                var day1 by remember { mutableStateOf(settings.optString("whatsapp_reminder_day_1", "25")) }
                DropdownSelector(day1, (1..31).map { it.toString() }, Modifier.fillMaxWidth(), isDark) { day1 = it }
                
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.safeFeeApiCall("save_whatsapp_settings", mapOf("whatsapp_reminder_day_1" to day1)) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE WHATSAPP SETTINGS")
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Message Templates", fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.height(12.dp))

        var msgPayment by remember { mutableStateOf(settings.optString("whatsapp_msg_payment", "Dear parent, fee of Rs. {amount} for {student} has been received. Thank you.")) }
        var msgOverdue by remember { mutableStateOf(settings.optString("whatsapp_msg_overdue", "Dear parent, fee for {student} is overdue. Please pay at your earliest.")) }

        Text("PAYMENT RECEIVED MESSAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        PremiumTextField(msgPayment, { msgPayment = it }, "Message template...", Icons.Default.Message, isDark)
        
        Spacer(Modifier.height(12.dp))
        
        Text("OVERDUE ALERT MESSAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        PremiumTextField(msgOverdue, { msgOverdue = it }, "Message template...", Icons.Default.Message, isDark)
        
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.safeFeeApiCall("save_whatsapp_settings", mapOf("whatsapp_msg_payment" to msgPayment, "whatsapp_msg_overdue" to msgOverdue)) {}
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("UPDATE TEMPLATES")
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StudentFeeDetailScreen(studentId: Int, viewModel: WantuchViewModel, onBack: () -> Unit) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
    val instId = viewModel.getSavedData()["last_inst"] as? Int ?: 0

    val ledgerData = remember { mutableStateListOf<JSONObject>() }
    val studentInfo = remember { mutableStateOf<JSONObject?>(null) }
    val feeTypes = remember { mutableStateListOf<JSONObject>() }
    val methods = remember { mutableStateListOf<String>() }
    val banks = remember { mutableStateListOf<String>() }
    val selectedFeeEntry = remember { mutableStateOf<JSONObject?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    fun refresh() {
        isLoading.value = true
        viewModel.safeApiCall("GET_STUDENT_FEE_LEDGER", mapOf("student_id" to studentId.toString(), "institution_id" to instId.toString())) { json ->
            isLoading.value = false
            ledgerData.clear()
            json?.optJSONArray("ledger")?.let { arr ->
                for (i in 0 until arr.length()) ledgerData.add(arr.getJSONObject(i))
            }
            // Fix: API returns key 'student', not 'basic'
            studentInfo.value = json?.optJSONObject("student")
            // Load fee types from same response (already included in ledger endpoint)
            feeTypes.clear()
            json?.optJSONArray("fee_types")?.let { arr ->
                for (i in 0 until arr.length()) feeTypes.add(arr.getJSONObject(i))
            }
        }
    }

    LaunchedEffect(studentId) {
        refresh()
        viewModel.safeApiCall("GET_PAYMENT_METADATA", mapOf("institution_id" to instId.toString())) { json ->
            methods.clear()
            banks.clear()
            json?.optJSONArray("methods")?.let { arr -> for (i in 0 until arr.length()) methods.add(arr.getString(i)) }
            json?.optJSONArray("banks")?.let { arr -> for (i in 0 until arr.length()) banks.add(arr.getString(i)) }
        }
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, Modifier.background(Color.White.copy(0.05f), CircleShape)) {
                    Icon(Icons.Default.ArrowBack, null, tint = if(isDark) Color.White else Color.Black)
                }
                Spacer(Modifier.width(12.dp))
                Text("Payment Working", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            // Student Header
            studentInfo.value?.let { info ->
                StudentDetailHeader(info, ledgerData, isDark)
            }

            Spacer(Modifier.height(16.dp))

            // Add Fee Section
            AddFeeSection(studentId, instId, feeTypes, viewModel, isDark) { refresh() }

            Spacer(Modifier.height(16.dp))

            // Ledger List
            Column(Modifier.padding(horizontal = 16.dp)) {
                ledgerData.forEach { entry ->
                    FeeEntryItem(
                        entry = entry, 
                        viewModel = viewModel, 
                        isDark = isDark, 
                        onCollect = { selectedFeeEntry.value = it }, 
                        onDone = { refresh() }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                
                if (ledgerData.isEmpty() && !isLoading.value) {
                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                        Text("No fee records found for this student", color = labelColor, fontSize = 12.sp)
                    }
                }
            }

            // MASS PAYMENT BOX
            val totalUnpaid = ledgerData.filter { it.optString("Status") == "Unpaid" }.sumOf { it.optDouble("amount") }
            if (totalUnpaid > 0 && !isLoading.value) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(0.3f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("MASS PAYMENT (WATERFALL)", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Automatically cascaded starting from the earliest unpaid month.", color = labelColor, fontSize = 10.sp)
                        Spacer(Modifier.height(16.dp))
                        
                        var customPayAllAmt by remember { mutableStateOf(totalUnpaid.toInt().toString()) }
                        Text("PAYMENT AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = labelColor)
                        PremiumTextField(customPayAllAmt, { customPayAllAmt = it }, "Enter sum to pay", Icons.Default.Money, isDark)
                        
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val payload = org.json.JSONObject()
                                payload.put("amount", customPayAllAmt.toDoubleOrNull() ?: totalUnpaid)
                                payload.put("bulk", true)
                                payload.put("fee_type", "Bulk Complete Payment")
                                payload.put("student_id", studentId)
                                payload.put("institution_id", instId)
                                selectedFeeEntry.value = payload
                            },
                            modifier = Modifier.fillMaxWidth().height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("PROCEED WITH MASS PAYMENT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
        
        selectedFeeEntry.value?.let { entry ->
            CollectionModal(
                entry = entry,
                methods = methods,
                banks = banks,
                viewModel = viewModel,
                isDark = isDark,
                onDismiss = { selectedFeeEntry.value = null },
                onDone = {
                    selectedFeeEntry.value = null
                    refresh()
                }
            )
        }
    }
}

@Composable
fun StudentDetailHeader(info: JSONObject, ledger: List<JSONObject>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
    
    val totalPending = ledger.filter { it.optString("Status") == "Unpaid" }.sumOf { it.optDouble("amount") }
    
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = cardColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.1f)),
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(Color(0xFF3B82F6).copy(0.1f)), Alignment.Center) {
                    val pic = info.optString("profile_pic")
                    if (pic.isNotEmpty()) {
                        AsyncImage(model = pic, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(info.optString("full_name").take(2).uppercase(), color = Color(0xFF3B82F6), fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(info.optString("full_name"), color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("Roll: ${info.optString("roll_number")}", color = labelColor, fontSize = 12.sp)
                    Text("${info.optString("class_name")} - ${info.optString("section_name")}", color = labelColor, fontSize = 11.sp)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("PENDING PAYMENT", color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(String.format("%,.0f", totalPending), color = Color(0xFF3B82F6), fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                var payComplete by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PAY COMPLETE?", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = payComplete, onCheckedChange = { payComplete = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF3B82F6)))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { /* Receipt */ },
                    colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Receipt, null, Modifier.size(16.dp), tint = textColor)
                    Spacer(Modifier.width(8.dp))
                    Text("RECEIPT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = textColor)
                }
            }
        }
    }
}

@Composable
fun AddFeeSection(studentId: Int, instId: Int, feeTypes: List<JSONObject>, viewModel: WantuchViewModel, isDark: Boolean, onDone: () -> Unit) {
    var month by remember { mutableStateOf(java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date())) }
    var year by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date())) }
    var selectedType by remember { mutableStateOf("Fee Type") }
    var selectedTypeId by remember { mutableIntStateOf(0) }
    var amount by remember { mutableStateOf("") }

    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val years = (2022..2030).map { it.toString() }
    val typeOptions = feeTypes.map { it.optString("type_name") }
    val typeMap = feeTypes.associate { it.optString("type_name") to it.optInt("id") }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { DropdownSelector(month, months, Modifier.fillMaxWidth(), isDark) { month = it } }
            Box(Modifier.weight(1f)) { DropdownSelector(year, years, Modifier.fillMaxWidth(), isDark) { year = it } }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { 
                DropdownSelector(selectedType, typeOptions, Modifier.fillMaxWidth(), isDark) { 
                    selectedType = it
                    selectedTypeId = typeMap[it] ?: 0
                }
            }
            PremiumTextField(
                value = amount,
                onValueChange = { amount = it },
                placeholder = "Amount",
                icon = Icons.Default.Edit,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                if (selectedTypeId > 0 && amount.isNotEmpty()) {
                    viewModel.safeApiCall("ADD_STUDENT_FEE", mapOf(
                        "student_id" to studentId.toString(),
                        "institution_id" to instId.toString(),
                        "fee_type_id" to selectedTypeId.toString(),
                        "month" to month,
                        "year" to year,
                        "amount" to amount
                    )) { onDone() }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(45.dp)
        ) {
            Text("ADD FEE", fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun FeeEntryItem(entry: JSONObject, viewModel: WantuchViewModel, isDark: Boolean, onCollect: (JSONObject) -> Unit, onDone: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
    
    val status = entry.optString("Status")
    val isPaid = status == "Paid"
    val statusColor = if (isPaid) Color(0xFF10B981) else Color(0xFFEF4444)

    Surface(
        Modifier.fillMaxWidth(),
        color = cardColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.optString("fee_type"), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${entry.optString("fee_month")} ${entry.optString("fee_year")}", color = labelColor, fontSize = 10.sp)
            }
            
            Text(String.format("%,.0f", entry.optDouble("amount")), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp))
            
            Surface(
                onClick = {
                    if (!isPaid) {
                        onCollect(entry)
                    }
                },
                color = statusColor.copy(0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    status.uppercase(),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            IconButton(onClick = { /* Edit */ }, Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, null, tint = labelColor, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = {
                viewModel.safeApiCall("DELETE_STUDENT_FEE", mapOf("id" to entry.optString("id"))) { onDone() }
            }, Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444).copy(0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun CollectionModal(
    entry: JSONObject,
    methods: List<String>,
    banks: List<String>,
    viewModel: WantuchViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Black.copy(0.6f)

    var amount by remember { mutableStateOf(entry.optDouble("amount").toString()) }
    var method by remember { mutableStateOf(methods.firstOrNull() ?: "Cash") }
    var bank by remember { mutableStateOf(banks.firstOrNull() ?: "") }
    var transId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Record Payment", fontSize = 18.sp, fontWeight = FontWeight.Black, color = textColor)
                Spacer(Modifier.height(4.dp))
                Text(entry.optString("fee_type"), color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Text("Amount Paying", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                PremiumTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    placeholder = "Amount",
                    icon = Icons.Default.Edit,
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))

                Text("Payment Method", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                DropdownSelector(method, methods.ifEmpty { listOf("Cash", "Online") }, Modifier.fillMaxWidth(), isDark) { method = it }
                
                if (method == "Online" || method == "Cheque") {
                    Spacer(Modifier.height(12.dp))
                    Text("Bank / Account", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    DropdownSelector(bank, banks.ifEmpty { listOf("Main Account") }, Modifier.fillMaxWidth(), isDark) { bank = it }
                    Spacer(Modifier.height(12.dp))
                    Text("Transaction ID", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    PremiumTextField(
                        value = transId,
                        onValueChange = { transId = it },
                        placeholder = "ID...",
                        icon = Icons.Default.Edit,
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = labelColor) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            if (entry.optBoolean("bulk", false)) {
                                viewModel.safeApiCall("COLLECT_BULK_FEE", mapOf(
                                    "student_id" to entry.optString("student_id"),
                                    "institution_id" to entry.optString("institution_id"),
                                    "amount" to amount,
                                    "payment_method" to method,
                                    "bank_account" to bank,
                                    "transaction_id" to transId
                                )) { json ->
                                    isLoading = false
                                    if (json?.optString("status") == "success") onDone()
                                }
                            } else {
                                viewModel.safeApiCall("COLLECT_FEE", mapOf(
                                    "id" to entry.optString("id"),
                                    "amount" to amount,
                                    "payment_method" to method,
                                    "bank_account" to bank,
                                    "transaction_id" to transId
                                )) { json ->
                                    isLoading = false
                                    if (json?.optString("status") == "success") onDone()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        enabled = !isLoading
                    ) {
                        Text(if (isLoading) "SAVING..." else "SAVE", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

fun dateToMonth(millis: Long): String {
    val date = java.util.Date(millis)
    val formatter = java.text.SimpleDateFormat("MMMM", java.util.Locale.ENGLISH)
    return formatter.format(date)
}
