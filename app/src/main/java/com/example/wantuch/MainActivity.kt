package com.example.wantuch

import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
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
const val BASE_URL = "https://www.wantuch.pk/" 

class MainActivity : ComponentActivity() {
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

enum class EduFlowState { SELECTOR, DASHBOARD, STAFF, STAFF_PROFILE, STUDENTS, STUDENT_PROFILE, MY_PROFILE, FEE_MANAGEMENT, STUDENT_FEE_DETAIL }

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
                    eduModalType = app.type
                } else if (app.type == "gen") {
                    genLoginVisible = true
                }
            })
        }

        // MODALS
        eduModalType?.let { type ->
            EduLoginModal(type, vm, onDismiss = { eduModalType = null }, onSuccess = { _ ->
                showEduFlow = true; eduFlowState = EduFlowState.SELECTOR; eduModalType = null
            })
        }
        if (genLoginVisible) {
            GeneralLoginModal(selectedApp?.label ?: "App", onDismiss = { genLoginVisible = false }, onSuccess = { /* handle */ })
        }

        // NATIVE EDUCATION FLOW
        AnimatedVisibility(showEduFlow, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
            when (eduFlowState) {
                EduFlowState.SELECTOR -> SchoolSelectorScreen(vm, onBack = { showEduFlow = false }, onInstitutionSelected = { eduFlowState = EduFlowState.DASHBOARD })
                EduFlowState.DASHBOARD -> EducationDashboardScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.SELECTOR },
                    onOpenWeb = { url -> webViewUrl = url },
                    onOpenStaff = { eduFlowState = EduFlowState.STAFF },
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
                        eduFlowState = EduFlowState.FEE_MANAGEMENT
                    }
                )
                EduFlowState.FEE_MANAGEMENT -> FeeManagementScreen(
                    viewModel = vm,
                    onBack = { eduFlowState = EduFlowState.DASHBOARD },
                    onOpenStudentFee = { id ->
                        currentStudentId = id
                        eduFlowState = EduFlowState.STUDENT_FEE_DETAIL
                    }
                )
                EduFlowState.STUDENT_FEE_DETAIL -> currentStudentId?.let { id ->
                    StudentFeeDetailScreen(
                        studentId = id,
                        viewModel = vm,
                        onBack = { eduFlowState = EduFlowState.FEE_MANAGEMENT }
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