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
import com.example.wantuch.ui.viewmodel.WantuchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenSyllabus: () -> Unit = {},
    onOpenHomework: () -> Unit = {},
    onOpenStudyPlan: () -> Unit = {},
    onOpenMyProfile: (Int) -> Unit = {}
) {
    BackHandler { onBack() }

    val isDark by viewModel.isDarkTheme.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "STUDENT PORTAL",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                if (instName.isNotEmpty()) instName else "Education Dashboard",
                                color = Color.White.copy(0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            Modifier
                                .size(42.dp)
                                .background(Color.White.copy(0.2f), CircleShape),
                            Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.School,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Welcome message
                    Text(
                        "Welcome back,",
                        color = Color.White.copy(0.7f),
                        fontSize = 13.sp
                    )
                    Text(
                        userName,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(Modifier.height(16.dp))

                    // --- STUDENT STATS CARDS ---
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudentStatCard(
                            "ATTENDANCE",
                            dashboardData?.stats?.get("attendance_per")?.toString() ?: "0%",
                            Color(0xFF10B981),
                            isDark,
                            Modifier.weight(1f)
                        )
                        StudentStatCard(
                            "FEE STATUS",
                            dashboardData?.stats?.get("fee_status")?.toString() ?: "N/A",
                            if ((dashboardData?.stats?.get("fee_status")?.toString() ?: "").contains("UNPAID")) Color(0xFFEF4444) else Color(0xFF10B981),
                            isDark,
                            Modifier.weight(1f)
                        )
                        StudentStatCard(
                            "MY CLASS",
                            dashboardData?.stats?.get("my_class")?.toString() ?: "N/A",
                            Color(0xFFF59E0B),
                            isDark,
                            Modifier.weight(1f)
                        )
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
                        "syllabus"   -> onOpenSyllabus()
                        "homework"   -> onOpenHomework()
                        "study_plan" -> onOpenStudyPlan()
                        "profile"    -> {
                            val userId = dashboardData?.user_id ?: 0
                            onOpenMyProfile(userId)
                        }
                        else -> { /* Coming soon */ }
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
    val accent = when (label) {
        "My Profile"  -> Color(0xFF3B82F6)
        "Notices"     -> Color(0xFFF59E0B)
        "Timetable"   -> Color(0xFF10B981)
        "Syllabus"    -> Color(0xFF8B5CF6)
        "Homework"    -> Color(0xFFEC4899)
        "My Exams"    -> Color(0xFFEF4444)
        "Study Plan"  -> Color(0xFF06B6D4)
        else          -> Color(0xFF3B82F6)
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
    modifier: Modifier = Modifier
) {
    Card(
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
