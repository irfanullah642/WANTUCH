package com.example.wantuch.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ParentDashboardScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    // State
    var childrenData by remember { mutableStateOf<JSONArray?>(null) }
    var selectedChild by remember { mutableStateOf<JSONObject?>(null) }
    var activeTab by remember { mutableStateOf("children") }  // children | fee | notices
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val subColor = if (isDark) Color.White.copy(0.5f) else Color.Gray

    // Load parent data
    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.safeApiCall(
            "GET_PARENT_DASHBOARD",
            emptyMap()
        ) { json ->
            if (json.optString("status") == "success") {
                childrenData = json.optJSONArray("children")
                if (childrenData != null && childrenData!!.length() > 0) {
                    selectedChild = childrenData!!.optJSONObject(0)
                }
            } else {
                errorMsg = json.optString("message", "Failed to load parent data")
            }
            isLoading = false
        }
    }

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
            // Gradient bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFEC4899), Color(0xFF9333EA))
                        )
                    )
                    .padding(16.dp)
            ) {
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
                            "PARENT PORTAL",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Monitor your child's progress",
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
                            Icons.Default.FamilyRestroom,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFEC4899))
                    Spacer(Modifier.height(16.dp))
                    Text("Loading parent dashboard...", color = subColor, fontSize = 14.sp)
                }
            }
        } else if (errorMsg.isNotEmpty()) {
            // ── Error / No Data State ──────────────────────────────────────
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.FamilyRestroom, null, tint = Color(0xFFEC4899).copy(0.4f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Parent Dashboard",
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMsg.ifEmpty { "Connect to the school portal to view your children's data. The school must add your CNIC to the student records." },
                        color = subColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    // Quick access tiles
                    ParentQuickAccessGrid()
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Children selector
                if (childrenData != null && childrenData!!.length() > 0) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "MY CHILDREN",
                            color = subColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val count = childrenData!!.length()
                            repeat(count) { i ->
                                val child = childrenData!!.optJSONObject(i) ?: return@repeat
                                val isSelected = selectedChild == child
                                ChildChip(
                                    name = child.optString("full_name", "Child ${i+1}"),
                                    className = child.optString("class_name", ""),
                                    isSelected = isSelected,
                                    modifier = Modifier.weight(1f)
                                ) { selectedChild = child }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }

                // Selected child info
                selectedChild?.let { child ->
                    item {
                        // Stat cards
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val attPct = child.optInt("att_pct", 0)
                            ParentStatCard(
                                label = "ATTENDANCE",
                                value = "$attPct%",
                                icon = Icons.Default.HowToReg,
                                color = if (attPct >= 75) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                            val feeStatus = child.optString("fee_status", "Unpaid")
                            ParentStatCard(
                                label = "FEE STATUS",
                                value = feeStatus,
                                icon = Icons.Default.Payments,
                                color = if (feeStatus == "Paid") Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                            ParentStatCard(
                                label = "CLASS",
                                value = child.optString("class_name", "N/A"),
                                icon = Icons.Default.Class,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    // Today's attendance
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardColor)
                                .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp))
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(
                                    "TODAY'S STATUS",
                                    color = subColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.height(12.dp))
                                val todayStatus = child.optString("today_status", "Not Marked")
                                val statusColor = when (todayStatus) {
                                    "Present" -> Color(0xFF10B981)
                                    "Absent"  -> Color(0xFFEF4444)
                                    "Leave"   -> Color(0xFFF59E0B)
                                    else      -> subColor
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(12.dp)
                                            .background(statusColor, CircleShape)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        todayStatus,
                                        color = statusColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    child.optString("full_name", "Student"),
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${child.optString("class_name", "")} • ${child.optString("section_name", "")}",
                                    color = subColor,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }

                // Quick access modules
                item {
                    Text(
                        "QUICK ACCESS",
                        color = subColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    ParentQuickAccessGrid()
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun ChildChip(
    name: String,
    className: String,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFFEC4899) else Color(0xFF1E293B))
            .border(
                1.dp,
                if (isSelected) Color(0xFFEC4899) else Color.White.copy(0.1f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Default.Person,
                null,
                tint = if (isSelected) Color.White else Color.White.copy(0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                name.split(" ").firstOrNull() ?: name,
                color = if (isSelected) Color.White else Color.White.copy(0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (className.isNotEmpty()) {
                Text(
                    className,
                    color = if (isSelected) Color.White.copy(0.8f) else Color.White.copy(0.4f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ParentStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier
) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(0.1f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1)
            Text(label, color = color.copy(0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ParentQuickAccessGrid() {
    val modules = listOf(
        Triple("Attendance", Icons.Default.HowToReg, Color(0xFF3B82F6)),
        Triple("Fee Status", Icons.Default.Payments, Color(0xFF10B981)),
        Triple("Notices", Icons.Default.Campaign, Color(0xFFF59E0B)),
        Triple("Results", Icons.Default.Assignment, Color(0xFF8B5CF6))
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        modules.forEach { (label, icon, color) ->
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(0.1f))
                    .border(1.dp, color.copy(0.2f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        label,
                        color = color,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
