package com.example.wantuch.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import kotlin.collections.chunked
import kotlin.collections.forEach


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
    onOpenFee: () -> Unit = {},
    onOpenAttendance: () -> Unit = {},
    onOpenQuestionPapers: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenSyllabus: () -> Unit = {},
    onOpenHomework: () -> Unit = {},
    onOpenPromotion: () -> Unit = {},
    onOpenDatabase: () -> Unit = {},
    onOpenStudyPlan: () -> Unit = {},
    onOpenNotices: () -> Unit = {},
    onOpenClasses: () -> Unit = {},
    onOpenSubjects: () -> Unit = {},
    onOpenExams: () -> Unit = {},
    onOpenTimetable: () -> Unit = {},
    onOpenAdmWdl: () -> Unit = {},
    onOpenSmartIDCard: () -> Unit = {},
    onOpenSubstitution: () -> Unit = {},
    onLogout: () -> Unit = {}
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
                            
                            // Manual red logout button to avoid breaking global HeaderActionIcon
                            IconButton(onClick = onLogout, modifier = Modifier.size(32.dp).background(if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f), RoundedCornerShape(8.dp))) {
                                Icon(Icons.Default.PowerSettingsNew, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }

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
                        val role = dash.role?.lowercase() ?: ""
                        val isHighAdmin = listOf("admin", "super_admin", "developer", "principal", "coordinator", "management", "headmaster", "head", "school_admin").contains(role)

                        val isStudent = role == "student"

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isStudent) {
                                // --- STUDENT DASHBOARD CARDS (Matches Web) ---
                                val attPer = dash.stats?.get("attendance_per")?.toString() ?: (if(dash.is_holiday) "HOLIDAY" else "PRESENT")
                                SuspendedStatCard("ATTENDANCE", attPer, Color(0xFF10B981), isDark, Modifier.weight(1f)) {
                                    onOpenAttendance()
                                }
                                
                                val feeStatus = dash.stats?.get("fee_status")?.toString() ?: "N/A"
                                val feeColor = if (feeStatus.contains("UNPAID", ignoreCase = true)) Color(0xFFEF4444) else Color(0xFF10B981)
                                SuspendedStatCard("FEE STATUS", feeStatus.uppercase(), feeColor, isDark, Modifier.weight(1f)) {
                                    onOpenFee()
                                }
                            } else {
                                // --- STAFF / ADMIN DASHBOARD CARDS ---
                                if (isHighAdmin) {
                                    val rawStaff = dash.stats?.get("staff")?.toString() ?: "0"
                                    val staffCountInt = rawStaff.toDoubleOrNull()?.toInt() ?: 0
                                    val displayStaff = if (staffCountInt > 0) (staffCountInt - 1).toString() else "0"

                                    SuspendedStatCard("STAFF", displayStaff, Color(0xFF6366F1), isDark, Modifier.weight(1f)) {
                                        onOpenStaff()
                                    }
                                }
                                SuspendedStatCard("STUDENTS", dash.stats?.get("students")?.toString() ?: "0", Color(0xFF3B82F6), isDark, Modifier.weight(1f)) {
                                    onOpenStudents()
                                }

                                if (isHighAdmin) {
                                    val feeToday = dash.stats?.get("fee_today")?.toString() ?: "0"
                                    SuspendedStatCard("FEE TODAY", "PKR $feeToday", Color(0xFF10B981), isDark, Modifier.weight(1f)) {
                                        onOpenFee()
                                    }
                                }
                                
                                val attLabel = if (dash.is_holiday) (dash.holiday_name?.uppercase() ?: "HOLIDAY") else "PRESENT"
                                SuspendedStatCard("ATTENDANCE", attLabel, Color(0xFFF59E0B), isDark, Modifier.weight(1f)) {
                                    onOpenAttendance()
                                }
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
                                        FlapModuleCard(mod.label, getIcon(mod.icon), isDark, mod.sub, Modifier.weight(1f)) {
                                            when(mod.id) {
                                                "profile" -> {
                                                    dash.user_id?.let { onOpenMyProfile(it) }
                                                }
                                                "staff" -> {
                                                    onOpenStaff()
                                                }
                                                "students" -> {
                                                    onOpenStudents()
                                                }
                                                "admission" -> {
                                                    onOpenAdmWdl()
                                                }
                                                "fee" -> {
                                                    onOpenFee()
                                                }
                                                "attendance" -> {
                                                    onOpenAttendance()
                                                }
                                                "question_papers", "paper" -> {
                                                    onOpenQuestionPapers()
                                                }
                                                "reports" -> {
                                                    onOpenReports()
                                                }
                                                "syllabus", "syllabus_planner" -> {
                                                    onOpenSyllabus()
                                                }
                                                "homework", "edu_assignments", "edu_assignments_submissions" -> {
                                                    onOpenHomework()
                                                }
                                                "promotion" -> {
                                                    onOpenPromotion()
                                                }
                                                "database" -> {
                                                    onOpenDatabase()
                                                }
                                                "study_plan", "planner", "study_planner" -> {
                                                    onOpenStudyPlan()
                                                }
                                                "notices" -> {
                                                    onOpenNotices()
                                                }
                                                "classes" -> {
                                                    onOpenClasses()
                                                }
                                                "subjects" -> {
                                                    onOpenSubjects()
                                                }
                                                "exams" -> {
                                                    onOpenExams()
                                                }
                                                "timetable" -> {
                                                    onOpenTimetable()
                                                }
                                                "smart_id" -> {
                                                    onOpenSmartIDCard()
                                                }
                                                "proxies" -> {
                                                    onOpenSubstitution()
                                                }
                                                else -> {
                                                    val baseUrl = "https://wantuch.pk/"
                                                    val path = when(mod.id) {
                                                        "inst_profile" -> "modules/education/profile.php?tab=inst"
                                                        "quick_scan" -> "modules/education/attendance_quick_scan.php"
                                                        "transport" -> "modules/education/transport.php"
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
