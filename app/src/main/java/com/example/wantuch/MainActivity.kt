package com.example.wantuch

import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wantuch.domain.model.AppItem
import com.example.wantuch.ui.components.*
import com.example.wantuch.ui.theme.WANTUCHTheme
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import kotlin.math.abs

// ── Configuration ─────────────────────────────────────────────────────────────
const val BASE_URL = "https://wantuch.pk/" 

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WANTUCHTheme { WantuchApp() } }
    }
}

// ── Global Styles ─────────────────────────────────────────────────────────────
val AccentPurple = Color(0xFF6366F1)
val Glass20      = Color(0x33FFFFFF)
val UnreadRed    = Color(0xFFE74C3C)
val OrangeAccent = Color(0xFFF5A623)

val defaultApps = listOf(
    AppItem("My Profile",        Icons.Default.AccountCircle,        Color(0xFF1ABC9C)),
    AppItem("Freelancer",        Icons.Default.Work,                  Color(0xFFF39C12)),
    AppItem("Transport",         Icons.Default.DirectionsCar,         Color(0xFFE74C3C)),
    AppItem("Transfer\n[Money]", Icons.Default.AttachMoney,           Color(0xFF2ECC71)),
    AppItem("Bills",             Icons.Default.Receipt,               Color(0xFFE67E22)),
    AppItem("Medical",           Icons.Default.LocalHospital,         Color(0xFFFF5E62)),
    AppItem("Rentals",           Icons.Default.Home,                  Color(0xFF16A085)),
    AppItem("Marketplace",       Icons.Default.Store,                 Color(0xFFF39C12)),
    AppItem("Schools",           Icons.Default.School,                Color(0xFF1ABC9C), type = "School"),
    AppItem("Colleges",          Icons.Default.MenuBook,              Color(0xFF3498DB), type = "College"),
    AppItem("Universities",      Icons.Default.AccountBalance,        Color(0xFF9B59B6), type = "University"),
    AppItem("Madrasa",           Icons.Default.Star,                  Color(0xFF27AE60), type = "Madrasa"),
    AppItem("Wallet",            Icons.Default.AccountBalanceWallet,  Color(0xFF2ECC71)),
    AppItem("Jobs",              Icons.Default.Work,                  Color(0xFF3498DB)),
    AppItem("Labour",            Icons.Default.Build,                 Color(0xFF8E44AD)),
    AppItem("Advocate",          Icons.Default.Gavel,                 Color(0xFF34495E)),
    AppItem("Loans",             Icons.Default.MonetizationOn,        Color(0xFFE67E22)),
    AppItem("Download App",      Icons.Default.Download,              Color(0xFF3498DB), type = "none")
)

enum class EduFlowState { 
    SELECTOR, DASHBOARD, PARENT_DASHBOARD, STUDENT_DASHBOARD,
    STAFF, STAFF_PROFILE, STUDENTS, STUDENT_PROFILE, 
    MY_PROFILE, FEE_MANAGEMENT, STUDENT_FEE_DETAIL, ATTENDANCE_MANAGEMENT,
    REPORTS_DASHBOARD, QUESTION_PAPERS, QUESTION_PAPER_BUILDER, SYLLABUS,
    HOMEWORK, DATABASE_MANAGEMENT, PROMOTION, STUDY_PLAN,
    NOTICES, CLASSES, SUBJECTS, EXAMS, 
    TIMETABLE, SUBSTITUTION, ADM_WDL, SMART_ID_CARD
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WantuchApp(vm: WantuchViewModel = viewModel()) {
    var showHome      by remember { mutableStateOf(false) }
    var webViewUrl    by remember { mutableStateOf<String?>(null) }
    var genLoginVisible by remember { mutableStateOf(false) }
    var selectedApp     by remember { mutableStateOf<AppItem?>(null) }
    var eduModalType    by remember { mutableStateOf<String?>(null) }
    var showEduFlow     by remember { mutableStateOf(false) }
    var eduFlowState    by remember { mutableStateOf(EduFlowState.SELECTOR) }
    var profileBackState by remember { mutableStateOf(EduFlowState.DASHBOARD) }
    var currentStaffId  by remember { mutableStateOf<Int?>(null) }
    var currentStudentId by remember { mutableStateOf<Int?>(null) }

    val appList = remember { mutableStateListOf(*defaultApps.toTypedArray()) }
    val lockPager = rememberPagerState(initialPage = 1) { 3 }

    // Update unread counts
    LaunchedEffect(Unit) {
        vm.startNotificationLoop { counts ->
            appList.indices.forEach { i ->
                val key = appList[i].label.lowercase().replace("\n","").replace(" ","_")
                if (counts.containsKey(key)) {
                    appList[i] = appList[i].copy(unread = counts[key] ?: 0)
                }
            }
        }
    }

    // Auto-lock on screen off
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { showHome = false; webViewUrl = null }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        // LOCK SCREENS
        HorizontalPager(state = lockPager, modifier = Modifier.fillMaxSize()) { page ->
            val swipeMod = Modifier.pointerInput(Unit) {
                var dragAmount = 0f
                detectVerticalDragGestures(
                    onDragEnd = { if (dragAmount > 80f) showHome = true; dragAmount = 0f },
                    onDragCancel = { dragAmount = 0f },
                    onVerticalDrag = { _, amt -> if (amt < 0) dragAmount += abs(amt) }
                )
            }
            when (page) {
                0 -> OrangeLockScreen(swipeMod)
                1 -> MainLockScreen(swipeMod)
                else -> DarkLockScreen(swipeMod)
            }
        }

        // HOME SCREEN
        AnimatedVisibility(showHome, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
            HomeScreen(items = appList, onLock = { showHome = false }, onAppClick = { app ->
                selectedApp = app
                if (app.type in listOf("School","College","University","Madrasa")) {
                    val saved = vm.getSavedData()
                    val isLogged = saved["is_logged_in"] as? Boolean ?: false
                    val remember = saved["remember"] as? Boolean ?: false
                    val instType = saved["inst_type"] as? String ?: ""
                    val lastInstId = saved["last_inst"] as? Int ?: 0
                    val lastRole = saved["role"] as? String ?: ""

                    if (remember && isLogged && instType == app.type && lastInstId != 0 && lastRole.isNotEmpty()) {
                        // Restore session
                        showEduFlow = true
                        val lowerRole = lastRole.lowercase()
                        if (lowerRole == "student") {
                            eduFlowState = EduFlowState.STUDENT_DASHBOARD
                        } else if (lowerRole == "parent") {
                            eduFlowState = EduFlowState.PARENT_DASHBOARD
                        } else {
                            // Staff/Admin, reload dashboard
                            vm.selectInstitution(lastInstId) {
                                eduFlowState = EduFlowState.DASHBOARD
                            }
                            // Call again even if callback doesn't run (offline case)
                            if (eduFlowState == EduFlowState.SELECTOR) {
                                eduFlowState = EduFlowState.DASHBOARD
                            }
                        }
                    } else {
                        eduModalType = app.type
                    }
                } else if (app.type == "gen") {
                    genLoginVisible = true
                }
            })
        }

        // MODALS
        eduModalType?.let { type ->
            EduLoginModal(type, vm, onDismiss = { eduModalType = null }, onSuccess = { role ->
                showEduFlow = true
                val lowerRole = role.lowercase()
                when (lowerRole) {
                    "parent" -> {
                        // Parent has own portal – no institution needed
                        eduFlowState = EduFlowState.PARENT_DASHBOARD
                    }
                    "admin" -> {
                        // Admin goes through school selector since they may manage multiple
                        eduFlowState = EduFlowState.SELECTOR
                    }
                    "super_admin", "developer" -> {
                        // Super admin / developer sees portfolio + selector
                        vm.fetchPortfolio()
                        eduFlowState = EduFlowState.SELECTOR
                    }
                    else -> {
                        // Staff / Teacher / Student: auto-load their specific institution's dashboard
                        val storedInstId = vm.getInstitutionId()
                        if (storedInstId != 0) {
                            vm.selectInstitution(storedInstId) {
                                eduFlowState = if (lowerRole == "student")
                                    EduFlowState.STUDENT_DASHBOARD
                                else
                                    EduFlowState.DASHBOARD  // Staff/Teacher use the full dashboard with role-filtered modules
                            }
                        } else {
                            eduFlowState = EduFlowState.SELECTOR
                        }
                    }
                }
                eduModalType = null
            })
        }
        if (genLoginVisible) {
            GeneralLoginModal(selectedApp?.label ?: "App", onDismiss = { genLoginVisible = false }, onSuccess = { /* handle */ })
        }

        // NATIVE EDUCATION FLOW
        AnimatedVisibility(showEduFlow, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
            when (eduFlowState) {
                EduFlowState.PARENT_DASHBOARD -> ParentDashboardScreen(
                    viewModel = vm,
                    onBack = { showEduFlow = false; eduFlowState = EduFlowState.SELECTOR }
                )
                EduFlowState.STUDENT_DASHBOARD -> StudentDashboardScreen(
                    viewModel = vm,
                    onBack = { showEduFlow = false; eduFlowState = EduFlowState.SELECTOR },
                    onOpenSyllabus = { eduFlowState = EduFlowState.SYLLABUS },
                    onOpenHomework = { eduFlowState = EduFlowState.HOMEWORK },
                    onOpenStudyPlan = { eduFlowState = EduFlowState.STUDY_PLAN },
                    onOpenMyProfile = { id ->
                        currentStaffId = id
                        profileBackState = EduFlowState.STUDENT_DASHBOARD
                        eduFlowState = EduFlowState.MY_PROFILE
                    },
                    onOpenNotices = { eduFlowState = EduFlowState.NOTICES },
                    onOpenSubjects = { eduFlowState = EduFlowState.SUBJECTS },
                    onOpenExams = { eduFlowState = EduFlowState.EXAMS },
                    onOpenTimetable = { eduFlowState = EduFlowState.TIMETABLE },
                    onOpenAttendance = { eduFlowState = EduFlowState.ATTENDANCE_MANAGEMENT },
                    onOpenClasses = { eduFlowState = EduFlowState.CLASSES },
                    onOpenFee = { id ->
                        currentStudentId = id
                        profileBackState = EduFlowState.STUDENT_DASHBOARD
                        eduFlowState = EduFlowState.STUDENT_FEE_DETAIL
                    },
                    onOpenSmartIDCard = { eduFlowState = EduFlowState.SMART_ID_CARD },
                    onOpenWeb = { url -> webViewUrl = url },
                    onLogout = {
                        vm.logout()
                        showEduFlow = false
                        eduFlowState = EduFlowState.SELECTOR
                    }
                )
                EduFlowState.SELECTOR -> SchoolSelectorScreen(vm, onBack = { showEduFlow = false }, onInstitutionSelected = { eduFlowState = EduFlowState.DASHBOARD })
                EduFlowState.DASHBOARD -> {
                    val dashboardData by vm.dashboardData.collectAsState()
                    val role = dashboardData?.role?.lowercase() ?: ""

                    if (role == "student") {
                        StudentDashboardScreen(
                            viewModel = vm,
                            onBack = { eduFlowState = EduFlowState.SELECTOR },
                            onOpenSyllabus = { eduFlowState = EduFlowState.SYLLABUS },
                            onOpenHomework = { eduFlowState = EduFlowState.HOMEWORK },
                            onOpenStudyPlan = { eduFlowState = EduFlowState.STUDY_PLAN },
                            onOpenMyProfile = { id ->
                                currentStaffId = id
                                profileBackState = EduFlowState.DASHBOARD
                                eduFlowState = EduFlowState.MY_PROFILE
                            },
                            onOpenNotices = { eduFlowState = EduFlowState.NOTICES },
                            onOpenSubjects = { eduFlowState = EduFlowState.SUBJECTS },
                            onOpenExams = { eduFlowState = EduFlowState.EXAMS },
                            onOpenTimetable = { eduFlowState = EduFlowState.TIMETABLE },
                            onOpenAttendance = { eduFlowState = EduFlowState.ATTENDANCE_MANAGEMENT },
                            onOpenClasses = { eduFlowState = EduFlowState.CLASSES },
                            onOpenFee = { id ->
                                currentStudentId = id
                                profileBackState = EduFlowState.DASHBOARD
                                eduFlowState = EduFlowState.STUDENT_FEE_DETAIL
                            },
                            onOpenSmartIDCard = { eduFlowState = EduFlowState.SMART_ID_CARD },
                            onOpenWeb = { url -> webViewUrl = url },
                            onLogout = {
                                vm.logout()
                                showEduFlow = false
                                eduFlowState = EduFlowState.SELECTOR
                            }
                        )
                    } else {
                        EducationDashboardScreen(
                            viewModel = vm,
                            onBack = { eduFlowState = EduFlowState.SELECTOR },
                            onOpenWeb = { url -> webViewUrl = url },
                            onOpenStaff = { eduFlowState = EduFlowState.STAFF },
                            onLogout = {
                                vm.logout()
                                showEduFlow = false
                                eduFlowState = EduFlowState.SELECTOR
                            },
                            onOpenStudents = { eduFlowState = EduFlowState.STUDENTS },
                            onOpenProfile = { id ->
                                currentStaffId = id
                                profileBackState = EduFlowState.DASHBOARD
                                eduFlowState = EduFlowState.STAFF_PROFILE
                            },
                            onOpenMyProfile = { id ->
                                currentStaffId = id
                                profileBackState = EduFlowState.DASHBOARD
                                eduFlowState = EduFlowState.MY_PROFILE
                            },
                            onOpenFee = {
                                val userRole = dashboardData?.role?.lowercase() ?: ""
                                if (userRole == "student") {
                                    currentStudentId = dashboardData?.user_id
                                    profileBackState = EduFlowState.DASHBOARD
                                    eduFlowState = EduFlowState.STUDENT_FEE_DETAIL
                                } else {
                                    eduFlowState = EduFlowState.FEE_MANAGEMENT
                                }
                            },
                            onOpenAttendance = {
                                eduFlowState = EduFlowState.ATTENDANCE_MANAGEMENT
                            },
                            onOpenQuestionPapers = {
                                eduFlowState = EduFlowState.QUESTION_PAPERS
                            },
                            onOpenReports = {
                                eduFlowState = EduFlowState.REPORTS_DASHBOARD
                            },
                            onOpenSyllabus = {
                                eduFlowState = EduFlowState.SYLLABUS
                            },
                            onOpenHomework = {
                                eduFlowState = EduFlowState.HOMEWORK
                            },
                            onOpenPromotion = {
                                eduFlowState = EduFlowState.PROMOTION
                            },
                            onOpenDatabase = {
                                eduFlowState = EduFlowState.DATABASE_MANAGEMENT
                            },
                            onOpenStudyPlan = {
                                eduFlowState = EduFlowState.STUDY_PLAN
                            },
                            onOpenNotices = {
                                eduFlowState = EduFlowState.NOTICES
                            },
                            onOpenClasses = {
                                eduFlowState = EduFlowState.CLASSES
                            },
                            onOpenSubjects = {
                                eduFlowState = EduFlowState.SUBJECTS
                            },
                            onOpenExams = {
                                eduFlowState = EduFlowState.EXAMS
                            },
                            onOpenTimetable = {
                                eduFlowState = EduFlowState.TIMETABLE
                            },
                            onOpenAdmWdl = {
                                eduFlowState = EduFlowState.ADM_WDL
                            },
                            onOpenSmartIDCard = {
                                eduFlowState = EduFlowState.SMART_ID_CARD
                            },
                            onOpenSubstitution = {
                                eduFlowState = EduFlowState.SUBSTITUTION
                            }
                        )
                    }
                }
                EduFlowState.PROMOTION -> PromotionScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.SYLLABUS -> SyllabusPlannerScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.HOMEWORK -> HomeworkManagementScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.DATABASE_MANAGEMENT -> DatabaseManagementScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.STUDY_PLAN -> StudyPlannerScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.FEE_MANAGEMENT -> FeeManagementScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD },
                    onOpenStudentFee = { id ->
                        currentStudentId = id
                        profileBackState = EduFlowState.FEE_MANAGEMENT
                        eduFlowState = EduFlowState.STUDENT_FEE_DETAIL
                    }
                )
                EduFlowState.ATTENDANCE_MANAGEMENT -> AttendanceManagementScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.STUDENT_FEE_DETAIL -> currentStudentId?.let { id ->
                    StudentFeeDetailScreen(
                        studentId = id,
                        viewModel = vm,
                        onBack = { eduFlowState = profileBackState }
                    )
                }
                EduFlowState.STAFF -> StaffManagementScreen(
                    viewModel = vm, 
                    onBack = { eduFlowState = EduFlowState.DASHBOARD },
                    onOpenWeb = { url -> webViewUrl = url },
                    onOpenProfile = { id ->
                        currentStaffId = id
                        profileBackState = EduFlowState.STAFF
                        eduFlowState = EduFlowState.STAFF_PROFILE
                    }
                )
                EduFlowState.STAFF_PROFILE -> currentStaffId?.let { id ->
                    StaffProfileScreen(
                        staffId = id,
                        viewModel = vm,
                        onBack = { eduFlowState = profileBackState },
                        onOpenWeb = { url -> webViewUrl = url }
                    )
                }
                EduFlowState.MY_PROFILE -> currentStaffId?.let { id ->
                    MyProfileScreen(
                        staffId = id,
                        viewModel = vm,
                        onBack = { eduFlowState = profileBackState }
                    )
                }
                EduFlowState.STUDENTS -> StudentManagementScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD },
                    onOpenWeb = { url -> webViewUrl = url },
                    onOpenProfile = { id ->
                        currentStaffId = id
                        profileBackState = EduFlowState.STUDENTS
                        eduFlowState = EduFlowState.STUDENT_PROFILE
                    }
                )
                EduFlowState.STUDENT_PROFILE -> currentStaffId?.let { id ->
                    StudentProfileScreen(
                        studentId = id,
                        viewModel = vm,
                        onBack = { eduFlowState = profileBackState },
                        onOpenWeb = { url -> webViewUrl = url }
                    )
                }
                EduFlowState.QUESTION_PAPERS -> QuestionPaperScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.REPORTS_DASHBOARD },
                    onOpenWeb = { url -> 
                        if (url == "paper_builder") eduFlowState = EduFlowState.QUESTION_PAPER_BUILDER
                        else webViewUrl = url 
                    }
                )
                EduFlowState.REPORTS_DASHBOARD -> ReportsDashboardScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD },
                    onOpenQuestionPapers = { eduFlowState = EduFlowState.QUESTION_PAPERS },
                    onOpenAnswerPapers = { webViewUrl = "https://wantuch.pk/modules/education/answer_papers.php" },
                    onOpenBulkExams = { webViewUrl = "https://wantuch.pk/modules/education/bulk_exam_papers.php" },
                    onOpenSmartIDCard = { eduFlowState = EduFlowState.SMART_ID_CARD },
                    onOpenWeb = { url -> webViewUrl = url }
                )
                EduFlowState.QUESTION_PAPER_BUILDER -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    QuestionPaperBuilderScreen(
                        viewModel = vm,
                        onBack = { eduFlowState = EduFlowState.QUESTION_PAPERS },
                        onSave = { title, subject, totalMarks, sections ->
                            vm.saveSmartPaper(
                                title = title,
                                subject = subject,
                                totalMarks = totalMarks,
                                sections = sections,
                                onSuccess = { filePath, message ->
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                    // Auto-download the generated PDF
                                    if (filePath.isNotEmpty()) {
                                        try {
                                            val fileName = filePath.substringAfterLast("/")
                                            val request = android.app.DownloadManager.Request(android.net.Uri.parse(filePath))
                                                .setTitle("Question Paper PDF")
                                                .setDescription("Downloading generated paper...")
                                                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                                                .setMimeType("application/pdf")
                                            val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                            dm.enqueue(request)
                                            android.widget.Toast.makeText(context, "Downloading $fileName...", android.widget.Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Download failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    eduFlowState = EduFlowState.QUESTION_PAPERS
                                },
                                onError = { error ->
                                    android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )
                }
                EduFlowState.NOTICES -> NoticesScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.CLASSES -> ClassesScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.SUBJECTS -> SubjectsScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.EXAMS -> ExamsScreen(
                    viewModel = vm,
                    openWeb = { url -> webViewUrl = url },
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.TIMETABLE -> TimetableScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD },
                    onOpenSubstitution = { eduFlowState = EduFlowState.SUBSTITUTION }
                )
                EduFlowState.SUBSTITUTION -> SubstitutionManagementScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.TIMETABLE }
                )
                EduFlowState.ADM_WDL -> AdmWdlScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD }
                )
                EduFlowState.SMART_ID_CARD -> SmartIDCardScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD },
                    onOpenWeb = { url -> webViewUrl = url }
                )
            }
        }

        // WEBVIEW
        AnimatedVisibility(webViewUrl != null, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
            webViewUrl?.let { url -> WebDashboard(url, onBack = { webViewUrl = null }) }
        }
    }
}

@Composable
fun WebDashboard(url: String, onBack: () -> Unit) {
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().background(Color(0xFF0F172A)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("Dashboard", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { /* reload */ }) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
        }
        AndroidView(factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        }, modifier = Modifier.weight(1f))
    }
}