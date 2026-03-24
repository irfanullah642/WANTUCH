package com.example.wantuch.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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
    onOpenStudyPlan: () -> Unit = {}
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
                            val staffCount = rawStaff.toDoubleOrNull()?.toInt() ?: "0"
                            val displayStaff = if (staffCount.toString().toIntOrNull() ?: 0 > 0) ((staffCount.toString().toIntOrNull() ?: 0) - 1).toString() else "0"

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
                                onOpenAttendance()
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
                                                else -> {
                                                    val baseUrl = "https://wantuch.pk/"
                                                    val path = when(mod.id) {
                                                        "inst_profile" -> "modules/education/profile.php?tab=inst"
                                                        "quick_scan" -> "modules/education/attendance_quick_scan.php"
                                                        "notices" -> "modules/education/notices.php"
                                                        "classes" -> "modules/education/classes_manage.php"
                                                        "subjects" -> "modules/education/subjects_manage.php"
                                                        "exams" -> "modules/education/exams.php"
                                                        "timetable" -> "modules/education/timetable.php"
                                                        "smart_id" -> "modules/education/idcard/idcard.php"
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