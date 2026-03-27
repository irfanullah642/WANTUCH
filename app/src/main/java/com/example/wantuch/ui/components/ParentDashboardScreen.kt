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
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    BackHandler { onBack() }

    // State
    var childrenData by remember { mutableStateOf<JSONArray?>(null) }
    var dashboardStats by remember { mutableStateOf<JSONObject?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val subColor = if (isDark) Color.White.copy(0.5f) else Color.Gray
    val context = androidx.compose.ui.platform.LocalContext.current

    var retryTrigger by remember { mutableStateOf(0) }

    // Load parent data
    LaunchedEffect(retryTrigger) {
        isLoading = true
        errorMsg = ""
        viewModel.safeApiCall(
            "GET_PARENT_DASHBOARD",
            emptyMap()
        ) { json ->
            if (json.optString("status") == "success") {
                childrenData = json.optJSONArray("children")
                dashboardStats = json.optJSONObject("stats")
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
        // ── Header (Matching Web Parent Hub Style) ──────────────────────────
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
                    .background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp).background(Color.White.copy(0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("PARENT HUB", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Keep track of your children's journey", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                    // Web icons: Notification, Theme, Logout
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(16.dp))
                        Icon(Icons.Default.Logout, null, tint = Color.White, modifier = Modifier.size(20.dp).clickable { onLogout() })
                    }
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        } else if (errorMsg.isNotEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.Red.copy(0.5f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(errorMsg, color = Color.Red, textAlign = TextAlign.Center, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { retryTrigger++ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) {
                        Text("Try Again")
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Summary Cards (Horizontal Scroll-like but in Grid for App Feel)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ParentStatCard("Children", dashboardStats?.optString("children") ?: "0", Icons.Default.People, Color(0xFF3B82F6), Modifier.weight(1f))
                            ParentStatCard("Schools", dashboardStats?.optString("schools") ?: "0", Icons.Default.School, Color(0xFF10B981), Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ParentStatCard("Dues", dashboardStats?.optString("dues") ?: "0", Icons.Default.Payments, Color(0xFFEF4444), Modifier.weight(1f))
                            ParentStatCard("Notices", dashboardStats?.optString("notices") ?: "0", Icons.Default.Campaign, Color(0xFFF59E0B), Modifier.weight(1f))
                        }
                    }
                }

                // Child Header
                item {
                    Text(
                        "STUDENT PROFILES",
                        color = subColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Children Cards
                childrenData?.let { data ->
                    val count = data.length()
                    items((0 until count).toList()) { i ->
                        val child = data.optJSONObject(i) ?: return@items
                        NewChildCard(child, isDark)
                    }
                }
                
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun NewChildCard(child: JSONObject, isDark: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val subTextColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            // Child Info Row (Header of Card)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile Pic Placeholder
                Box(
                    Modifier.size(54.dp).clip(CircleShape).background(Color(0xFF3B82F6).copy(0.1f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(child.optString("full_name"), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = textColor)
                    Text(child.optString("school_name"), fontSize = 12.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("${child.optString("class_name")} (${child.optString("section_name")})", fontSize = 11.sp, color = subTextColor)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = textColor.copy(0.05f), thickness = 1.dp)
            Spacer(Modifier.height(20.dp))

            // Metrics Grid
            Row(Modifier.fillMaxWidth()) {
                ChildMetricItem("ATTENDANCE", "${child.optString("att_pct")}%", Icons.Default.Timeline, Color(0xFF10B981), Modifier.weight(1f))
                ChildMetricItem("FEE DUES", child.optString("fee_status"), Icons.Default.Wallet, Color(0xFFF43F5E), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                ChildMetricItem("PERFORMANCE", child.optString("performance", "N/A"), Icons.Default.Stars, Color(0xFFF59E0B), Modifier.weight(1f))
                ChildMetricItem("PEND. WORK", child.optString("pending_work", "0"), Icons.Default.EditNote, Color(0xFF8B5CF6), Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
            
            // View Report Action
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF3B82F6).copy(0.2f), RoundedCornerShape(12.dp))
                    .clickable {
                        android.widget.Toast.makeText(
                            context,
                            "Navigating to Student Profile (Coming Soon)",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    .padding(vertical = 10.dp),
                Alignment.Center
            ) {
                Text("View Full Report →", color = Color(0xFF3B82F6), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ChildMetricItem(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(0.1f)),
            Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ParentStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(0.1f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, fontSize = 10.sp, color = color.copy(0.7f), fontWeight = FontWeight.Bold)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
            }
        }
    }
}
