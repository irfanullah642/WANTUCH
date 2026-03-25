package com.example.wantuch.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.wantuch.ui.viewmodel.WantuchViewModel

@Composable
fun ReportsDashboardScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenQuestionPapers: () -> Unit,
    onOpenAnswerPapers: () -> Unit,
    onOpenBulkExams: () -> Unit,
    onOpenSmartIDCard: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val data by viewModel.dashboardData.collectAsState()
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    // Accents
    val blue = Color(0xFF3B82F6)
    val emerald = Color(0xFF10B981)
    val purple = Color(0xFF8B5CF6)
    val amber = Color(0xFFF59E0B)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            
            // Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Brush.horizontalGradient(listOf(blue, purple)))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Reports & Documents", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("PAPER MANAGEMENT MODULE", color = Color.White.copy(.65f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Body
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                
                // Stat Header (Optional)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cardColor).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(blue.copy(.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Analytics, null, tint = blue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Active Institute", color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(data?.institution_name ?: "WANTUCH Education", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Text("PAPER MANAGEMENT", color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(12.dp))

                // Question Papers Card
                ReportModuleCard(
                    title = "Question Papers",
                    subtitle = "Create & Manage Examination Papers",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    color = blue,
                    isDark = isDark,
                    onClick = onOpenQuestionPapers
                )
                
                // Answer Papers Card
                ReportModuleCard(
                    title = "Answer Papers",
                    subtitle = "Manage Answer Keys & Worksheets",
                    icon = Icons.Default.FactCheck,
                    color = emerald,
                    isDark = isDark,
                    onClick = onOpenAnswerPapers
                )

                // Bulk Exam Papers Card
                ReportModuleCard(
                    title = "Bulk Exam Papers",
                    subtitle = "Generate Batched Papers for Printing",
                    icon = Icons.Default.LibraryBooks,
                    color = purple,
                    isDark = isDark,
                    onClick = onOpenBulkExams
                )

                Spacer(Modifier.height(24.dp))
                Text("DOCUMENT GENERATORS", color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallDocumentCard("Date Sheet", Icons.Default.CalendarMonth, amber, isDark, Modifier.weight(1f)) {
                        onOpenWeb("https://wantuch.pk/modules/education/documents/date-sheet.php")
                    }
                    SmallDocumentCard("Result Card", Icons.Default.WorkspacePremium, blue, isDark, Modifier.weight(1f)) {
                        onOpenWeb("https://wantuch.pk/modules/education/documents/result-card.php")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallDocumentCard("Student ID", Icons.Default.Badge, emerald, isDark, Modifier.weight(1f)) {
                        onOpenSmartIDCard()
                    }
                    SmallDocumentCard("Staff ID", Icons.Default.AssignmentInd, purple, isDark, Modifier.weight(1f)) {
                        onOpenWeb("https://wantuch.pk/modules/education/documents/staff-id.php")
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun ReportModuleCard(
    title: String, subtitle: String, icon: ImageVector, color: Color, isDark: Boolean, onClick: () -> Unit
) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(.12f)).border(1.dp, color.copy(.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray.copy(.5f))
        }
    }
}

@Composable
private fun SmallDocumentCard(label: String, icon: ImageVector, color: Color, isDark: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(color.copy(.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, color = if (isDark) Color.White else Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
