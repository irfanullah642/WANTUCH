package com.example.wantuch.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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

    Box(Modifier
        .fillMaxSize()
        .background(Color(0xFF0F172A))) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    Row(Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(Modifier
            .fillMaxSize()
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(0.05f)), Alignment.Center) {
                if (!inst.logo.isNullOrEmpty()) {
                    AsyncImage(model = "https://wantuch.pk/${inst.logo}", contentDescription = null, contentScale = ContentScale.Crop)
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
    Box(modifier
        .clip(RoundedCornerShape(18.dp))
        .background(Color(0xFF1E293B))
        .border(1.dp, accent.copy(0.3f), RoundedCornerShape(18.dp))
        .padding(12.dp)) {
        Column {
            Text(label, color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}


@Composable
fun HangingTube(modifier: Modifier) {
    Column(modifier
        .width(4.dp)
        .height(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color(0xFF94A3B8), CircleShape))
        Box(Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF64748B),
                        Color(0xFFCBD5E1),
                        Color(0xFF64748B)
                    )
                )
            ))
        Box(Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color(0xFF94A3B8), CircleShape))
    }
}


@Composable
fun ActionIconButton(icon: ImageVector, label: String, color: Color, isDark: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(0.1f))
                .border(1.dp, color.copy(0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = color.copy(0.8f))
    }
}

@Composable
fun HeaderActionIcon(icon: ImageVector, isDark: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .background(
                if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f),
                RoundedCornerShape(8.dp)
            )
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
        Column(Modifier
            .fillMaxSize()
            .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
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
        Column(Modifier
            .fillMaxSize()
            .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier
                .size(38.dp)
                .background(
                    if (isDark) Color.White.copy(0.05f) else Color(0xFF3B82F6).copy(0.1f),
                    CircleShape
                ), Alignment.Center) {
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
        Column(Modifier
            .fillMaxSize()
            .padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
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
        Column(Modifier
            .fillMaxSize()
            .padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
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
        "family_restroom" -> Icons.Default.FamilyRestroom
        "people" -> Icons.Default.People
        else -> Icons.Default.Category
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
            Box(Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF3B82F6).copy(0.1f)), Alignment.Center) {
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
            Box(Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF10B981).copy(0.1f)), Alignment.Center) {
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
                    Box(Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color.Black.copy(0.2f) else Color.Black.copy(0.05f))
                        .padding(15.dp), Alignment.CenterStart) {
                        Text(java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()), color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("AMOUNT (RS)", color = textColor.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(0.05f))
                        .border(1.dp, Color(0xFF10B981).copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(15.dp), Alignment.CenterStart) {
                        Text("${member.balance ?: 0}", color = Color(0xFF10B981), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Spacer(Modifier.height(25.dp))
            
            Button(
                onClick = { /* Process */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
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




