package com.example.wantuch.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.wantuch.ui.viewmodel.WantuchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenSyllabus: () -> Unit = {},
    onOpenHomework: () -> Unit = {},
    onOpenStudyPlan: () -> Unit = {},
    onOpenMyProfile: (Int) -> Unit = {},
    onOpenNotices: () -> Unit = {},
    onOpenSubjects: () -> Unit = {},
    onOpenExams: () -> Unit = {},
    onOpenTimetable: () -> Unit = {},
    onOpenAttendance: () -> Unit = {},
    onOpenClasses: () -> Unit = {},
    onOpenFee: (Int) -> Unit = {},
    onOpenSmartIDCard: () -> Unit = {},
    onOpenWeb: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    BackHandler { onBack() }

    val isDark by viewModel.isDarkTheme.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val staffProfile by viewModel.staffProfile.collectAsState()

    LaunchedEffect(dashboardData?.user_id) {
        dashboardData?.user_id?.let { uid ->
             // If profile isn't loaded or belongs to a different user, fetch fresh data
             if (staffProfile == null || (staffProfile?.basic?.get("id")?.toString() != uid.toString())) {
                 viewModel.fetchStaffProfile(uid)
             }
        }
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val subColor = if (isDark) Color.White.copy(0.5f) else Color.Gray

    // Student-specific modules from the server (already filtered by backend RBAC)
    val modules = dashboardData?.modules ?: listOf(
        com.example.wantuch.domain.model.ModuleItem("profile", "My Profile", "person"),
        com.example.wantuch.domain.model.ModuleItem("notices", "Notices", "campaign"),
        com.example.wantuch.domain.model.ModuleItem("timetable", "Timetable", "schedule"),
        com.example.wantuch.domain.model.ModuleItem("syllabus", "Syllabus", "book"),
        com.example.wantuch.domain.model.ModuleItem("homework", "Homework", "tasks"),
        com.example.wantuch.domain.model.ModuleItem("exams", "My Exams", "assignment"),
        com.example.wantuch.domain.model.ModuleItem("study_plan", "Study Plan", "event_available")
    )

    val userName = dashboardData?.full_name ?: "Student"
    val instName = dashboardData?.institution_name ?: ""

    Column(
        Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFF6366F1))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        // Top Left Area: Back Icon and then Title
                        Column {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(0.2f), CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Text(
                                "STUDENT PORTAL",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )

                            val fullName = dashboardData?.full_name ?: ""
                            if (fullName.isNotEmpty()) {
                                Text(
                                    "Welcome back,",
                                    color = Color.White.copy(0.7f),
                                    fontSize = 13.sp
                                )
                                Text(
                                    fullName,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Top Right Area: Icons and then Profile Picture
                        Column(horizontalAlignment = Alignment.End) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { viewModel.toggleTheme() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(0.2f), CircleShape)
                                ) {
                                    Icon(
                                        if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onLogout,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(0.2f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.PowerSettingsNew,
                                        null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            val profilePic = staffProfile?.basic?.get("profile_pic")?.toString() ?: ""
                            val rawPicInStats = (dashboardData?.stats?.get("profile_pic") ?: dashboardData?.stats?.get("pic") ?: dashboardData?.stats?.get("photo") ?: dashboardData?.stats?.get("user_pic") ?: dashboardData?.stats?.get("student_pic"))?.toString() ?: ""
                            val rawPicRoot = dashboardData?.profile_pic ?: ""
                            
                            // Prefer the one from the profile as we know it works
                            val rawPic = if (profilePic.isNotEmpty()) profilePic else if (rawPicRoot.isNotEmpty()) rawPicRoot else rawPicInStats

                            val picUrl = if (rawPic.startsWith("http")) rawPic 
                                        else if (rawPic.isNotEmpty() && rawPic != "user.png") {
                                            if (rawPic.startsWith("uploads/")) "https://wantuch.pk/$rawPic"
                                            else if (rawPic.contains("/")) "https://wantuch.pk/assets/$rawPic"
                                            else "https://wantuch.pk/uploads/students/$rawPic"
                                        }
                                        else ""

                            Box(
                                Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                                    .border(2.dp, Color.White.copy(0.4f), CircleShape),
                                Alignment.Center
                            ) {
                                if (picUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = picUrl,
                                        contentDescription = "Profile",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // --- STUDENT STATS CARDS ---
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudentStatCard(
                            "ATTENDANCE",
                            dashboardData?.stats?.get("attendance_per")?.toString() ?: (if(dashboardData?.is_holiday == true) "HOLIDAY" else "PRESENT"),
                            Color(0xFF10B981),
                            isDark,
                            Modifier.weight(1f)
                        ) { onOpenAttendance() }
                        
                        val feeStatus = dashboardData?.stats?.get("fee_status")?.toString() ?: "N/A"
                        StudentStatCard(
                            "FEE STATUS",
                            feeStatus.uppercase(),
                            if (feeStatus.contains("UNPAID")) Color(0xFFEF4444) else Color(0xFF10B981),
                            isDark,
                            Modifier.weight(1f)
                        ) { 
                            val userId = dashboardData?.user_id ?: 0
                            onOpenFee(userId)
                        }
                        
                        val myClass = dashboardData?.stats?.get("my_class")?.toString() ?: "N/A"
                        StudentStatCard(
                            "MY CLASS",
                            myClass.uppercase(),
                            Color(0xFFF59E0B),
                            isDark,
                            Modifier.weight(1f)
                        ) { onOpenClasses() }
                    }
                }
            }
        }

        // ── Modules Grid ───────────────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        Text(
            "MY MODULES",
            color = subColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(modules) { mod ->
                StudentModuleCard(
                    label = mod.label,
                    icon = getIcon(mod.icon),
                    cardColor = cardColor,
                    textColor = textColor,
                    isDark = isDark
                ) {
                    // Navigate based on module id
                    when (mod.id) {
                        "syllabus", "syllabus_planner" -> onOpenSyllabus()
                        "homework", "edu_assignments" -> onOpenHomework()
                        "study_plan", "planner", "study_planner" -> onOpenStudyPlan()
                        "notices" -> onOpenNotices()
                        "subjects" -> onOpenSubjects()
                        "exams" -> onOpenExams()
                        "timetable" -> onOpenTimetable()
                        "attendance" -> onOpenAttendance()
                        "smart_id" -> onOpenSmartIDCard()
                        "profile" -> {
                            val userId = dashboardData?.user_id ?: 0
                            onOpenMyProfile(userId)
                        }
                        else -> {
                            val baseUrl = "https://wantuch.pk/"
                            val path = when(mod.id) {
                                "inst_profile" -> "modules/education/profile.php?tab=inst"
                                else -> "modules/education/dashboard.php"
                            }
                            onOpenWeb(baseUrl + path)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentModuleCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cardColor: Color,
    textColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    // Assign a unique accent color per module type
    val accent = when {
        label.contains("Profile", true)  -> Color(0xFF3B82F6)
        label.contains("Notices", true)  -> Color(0xFFF59E0B)
        label.contains("Timetable", true) -> Color(0xFF10B981)
        label.contains("Syllabus", true)  -> Color(0xFF8B5CF6)
        label.contains("Homework", true)  -> Color(0xFFEC4899)
        label.contains("Exam", true)      -> Color(0xFFEF4444)
        label.contains("Study Plan", true) -> Color(0xFF06B6D4)
        label.contains("Subject", true)    -> Color(0xFF10B981)
        label.contains("Transport", true)  -> Color(0xFF6366F1)
        label.contains("ID", true)         -> Color(0xFF3B82F6)
        else                               -> Color(0xFF3B82F6)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 4.dp),
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, accent.copy(0.25f)) else null
    ) {
        Box(
            Modifier.fillMaxSize().padding(10.dp),
            Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(0.15f)),
                    Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    label,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun StudentStatCard(
    label: String,
    value: String,
    accent: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(65.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(0.15f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.White.copy(0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}
