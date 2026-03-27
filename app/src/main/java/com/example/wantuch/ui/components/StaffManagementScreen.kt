package com.example.wantuch.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.wantuch.domain.model.StaffProfileResponse
import com.example.wantuch.ui.viewmodel.WantuchViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    val staffData by viewModel.staffData.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showActions by remember { mutableStateOf(false) }
    var showAddModal by remember { mutableStateOf(false) }
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
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, Modifier.background(if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp))) {
                        Icon(Icons.Default.ArrowBack, null, tint = textColor)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text("STAFF MANAGER", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                    IconButton(onClick = { viewModel.toggleTheme() }, Modifier.background(if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp))) {
                        Icon(if(isDark) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = textColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showAddModal = true }, Modifier.background(Color(0xFF3B82F6), RoundedCornerShape(12.dp))) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                }
                
                Spacer(Modifier.height(15.dp))
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("Search by name, role or ID...", fontSize = 13.sp, color = labelColor) },
                    leadingIcon = { Icon(Icons.Default.Cloud, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                        focusedBorderColor = Color(0xFF3B82F6).copy(0.5f),
                        unfocusedBorderColor = if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f)
                    )
                )
            }

            if (isLoading && staffData == null) {
                // Show placeholder stats from dashboard while loading
                val dashTotal = dashboardData?.stats?.get("staff")?.toString()?.toDoubleOrNull()?.toInt()?.toString() ?: "0"
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .background(cardColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StaffPillStat("TOTAL", dashTotal, Color(0xFF3B82F6), isDark)
                    StaffPillStat("PRESENT", "...", Color(0xFF10B981), isDark)
                    StaffPillStat("ABSENT", "...", Color(0xFFEF4444), isDark)
                    StaffPillStat("LEAVE", "...", Color(0xFF8B5CF6), isDark)
                }
                Spacer(Modifier.height(20.dp))
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
                                val filteredTeaching = data.teaching_staff?.filter { 
                                    it.name.contains(searchQuery, true) || it.role.contains(searchQuery, true) || it.id.toString().contains(searchQuery, true)
                                } ?: emptyList()

                                if (filteredTeaching.isNotEmpty()) {
                                    filteredTeaching.forEach { member ->
                                        StaffMemberRow(member, cardColor, textColor, labelColor) {
                                            onOpenProfile((member.id as? Number)?.toInt() ?: 0)
                                        }
                                    }
                                } else {
                                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                        Text(if(searchQuery.isEmpty()) "No teaching staff found" else "No matches for '$searchQuery'", color = textColor.copy(0.5f))
                                    }
                                }
                            } else {
                                // Non-Teaching Staff List
                                val filteredNonTeaching = data.non_teaching_staff?.filter { 
                                    it.name.contains(searchQuery, true) || it.role.contains(searchQuery, true) || it.id.toString().contains(searchQuery, true)
                                } ?: emptyList()

                                if (filteredNonTeaching.isNotEmpty()) {
                                    filteredNonTeaching.forEach { member ->
                                        StaffMemberRow(member, cardColor, textColor, labelColor) {
                                            onOpenProfile((member.id as? Number)?.toInt() ?: 0)
                                        }
                                    }
                                } else {
                                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                                        Text(if(searchQuery.isEmpty()) "No non-teaching staff found" else "No matches for '$searchQuery'", color = textColor.copy(0.5f))
                                    }
                                }
                            }
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
        
        if (showAddModal) {
            EnrollStaffModal(
                viewModel = viewModel,
                isDark = isDark,
                onDismiss = { showAddModal = false }
            )
        }
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
                // Row 1: Name & ID
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(member.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, modifier = Modifier.weight(1f))
                    Box(Modifier.background(Color(0xFF3B82F6).copy(0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("ID: ${member.id}", color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
        Triple("PROFILE", Icons.Default.Cloud, Color(0xFF06B6D4)),
        Triple("ATTEN", Icons.Default.DateRange, Color(0xFF10B981)),
        Triple("SALARY", Icons.Default.Payments, Color(0xFFF59E0B)),
        Triple("HOME WORKS", Icons.Default.MenuBook, Color(0xFF8B5CF6)),
        Triple("SYLLABUS", Icons.Default.List, Color(0xFFEC4899)),
        Triple("ROLE", Icons.Default.Badge, Color(0xFF6366F1)),
        Triple("BIO", Icons.Default.Fingerprint, Color(0xFF00F3FF)),
        Triple("ACCOUNT", Icons.Default.PowerSettingsNew, Color(0xFFEF4444))
    )

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box(Modifier.fillMaxSize().background(bgColor).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
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
                        0 -> StaffCloudTab(profile ?: StaffProfileResponse(status="loading"), isDark, viewModel, staffId)
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
                        3 -> StaffHomeworkTab(profile?.stats?.get("homeworks") as? Map<String, Any?> ?: emptyMap(), isDark)
                        4 -> StaffSyllabusTab(profile?.stats?.get("syllabus") as? Map<String, Any?> ?: emptyMap(), isDark)
                        5 -> StaffRoleTab(profile?.basic ?: emptyMap(), isDark)
                        6 -> StudentBiometricTab(profile?.basic ?: emptyMap(), isDark)
                        7 -> StaffStatusTab(profile?.basic ?: emptyMap(), isDark,
                            onUpdateStatus = { status ->
                                viewModel.updateStaffStatus(staffId, status,
                                    onSuccess = { Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show() }
                                )
                            },
                            onDelete = {
                                viewModel.deleteStaff(staffId,
                                    onSuccess = { 
                                        Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show()
                                        onBack() 
                                    },
                                    onError = { Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show() }
                                )
                            }
                        )
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
fun StaffCloudTab(data: StaffProfileResponse, isDark: Boolean, viewModel: WantuchViewModel, staffId: Int) {
    var activeSubSection by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    BackHandler(enabled = activeSubSection != null) {
        activeSubSection = null
    }

    if (activeSubSection != null) {
        Box(Modifier.fillMaxSize()) {
            when (activeSubSection) {
                "Identity" -> StudentIdentityForm(data.basic ?: emptyMap(), isDark, isStaff = true) { payload ->
                    viewModel.updateStaffProfile(staffId, payload,
                        onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    )
                }
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
fun StaffStatusTab(
    basic: Map<String, Any?>, 
    isDark: Boolean, 
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val currentStatus = basic["status"]?.toString() ?: "active"
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Staff Record") },
            text = { Text("Are you sure you want to permanently delete this staff member? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL")
                }
            },
            containerColor = cardColor,
            titleContentColor = textColor,
            textContentColor = textColor.copy(0.7f)
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        // Deactivate Section
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(70.dp).background(Color(0xFFF59E0B).copy(0.1f), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.PowerSettingsNew, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(35.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(if(currentStatus == "active") "DEACTIVATE STAFF" else "RE-ACTIVATE STAFF", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(if(currentStatus == "active") "Temporarily disable this staff account. They won't be able to login." else "Restore this staff member to active status.",
                    color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { onUpdateStatus(if(currentStatus == "active") "deactivated" else "active") },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(if(currentStatus == "active") Color(0xFFF59E0B) else Color(0xFF10B981))
                ) {
                    Text(if(currentStatus == "active") "DEACTIVATE NOW" else "ACTIVATE NOW", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }

        // Delete Section
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(70.dp).background(Color(0xFFEF4444).copy(0.1f), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(35.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("DELETE STAFF", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Permanently remove this staff member from the institutional database. This is irreversible.",
                    color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(Color(0xFFEF4444))
                ) {
                    Text("DELETE PERMANENTLY", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StaffHomeworkTab(stats: Map<String, Any?>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val list = stats["list"] as? List<Map<String, Any?>> ?: emptyList()

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ASSIGNED HOMEWORKS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(stats["total"]?.toString() ?: "0", color = Color(0xFF8B5CF6), fontSize = 48.sp, fontWeight = FontWeight.Black)
                Text("Total Works Created", color = Color.Gray, fontSize = 11.sp)
            }
        }

        Text("RECENT ASSIGNMENTS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 5.dp))

        if (list.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Text("No recent homeworks found.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            list.forEach { hw ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(cardColor)) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(hw["title"]?.toString() ?: "Untitled", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${hw["cname"]} ${hw["sname"]} • ${hw["subname"]}", color = Color.Gray, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(hw["stats"]?.toString() ?: "0/0", color = Color(0xFF8B5CF6), fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("SUBMISSIONS", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StaffSyllabusTab(stats: Map<String, Any?>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val list = stats["list"] as? List<Map<String, Any?>> ?: emptyList()

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("COURSE PROGRESS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("${stats["covered"] ?: 0} / ${stats["total_topics"] ?: 0}", color = Color(0xFFEC4899), fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text("Topics Covered", color = Color.Gray, fontSize = 11.sp)
            }
        }

        Text("SYLLABUS TOPICS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 5.dp))

        if (list.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Text("No syllabus records found.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            list.forEach { item ->
                val status = item["status"]?.toString() ?: "Pending"
                val statusColor = when(status) {
                    "Completed" -> Color(0xFF10B981)
                    "In Progress" -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(cardColor)) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item["topic"]?.toString() ?: "Untitled", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(item["subject"]?.toString() ?: "No Subject", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(Modifier.background(statusColor.copy(0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(status.uppercase(), color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}
@Composable
fun EnrollStaffModal(
    viewModel: com.example.wantuch.ui.viewmodel.WantuchViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Quick List, 1: Manual
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val context = LocalContext.current

    var selectedRole by remember { mutableStateOf("teacher") }
    var selectedGender by remember { mutableStateOf("Male") }
    var selectedType by remember { mutableStateOf("teaching") } // teaching, non-teaching

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardColor,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("ADD STAFF MEMBERS", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Role & Gender Selection
                // Type Selection (Full Width)
                Box(Modifier.fillMaxWidth()) {
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(0.3f))
                    ) {
                        Text("STAFF TYPE: ${selectedType.uppercase()}", color = textColor, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("teaching", "non-teaching").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.uppercase()) },
                                onClick = { 
                                    selectedType = type
                                    selectedRole = if(type == "teaching") "teacher" else "staff"
                                    expanded = false 
                                }
                            )
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
                    var namesText by remember { mutableStateOf("") }
                    Column {
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
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (namesText.isBlank()) {
                                    Toast.makeText(context, "Please enter names", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.bulkSaveStaff(namesText, selectedRole, selectedGender, selectedType,
                                onSuccess = { 
                                    Toast.makeText(context, "records successfuly saved", Toast.LENGTH_LONG).show()
                                    onDismiss() 
                                },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("ADD STAFF MEMBERS", fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    StudentIdentityForm(emptyMap(), isDark, isStaff = true) { fields ->
                        val payload = fields.toMutableMap()
                        payload["role"] = selectedRole
                        payload["gender"] = selectedGender
                        payload["user_type"] = selectedType
                        
                        viewModel.saveStaff(payload,
                            onSuccess = { 
                                Toast.makeText(context, "records successfuly saved", Toast.LENGTH_LONG).show()
                                onDismiss()
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
