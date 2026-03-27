package com.example.wantuch.ui.components

import android.app.DownloadManager
import android.content.Context
import androidx.core.net.toUri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wantuch.domain.model.QuestionPaper
import com.example.wantuch.domain.model.SchoolClass
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import java.util.Calendar

// ── Colour Helpers ─────────────────────────────────────────────────────────────
private val Blue    = Color(0xFF3B82F6)
private val Indigo  = Color(0xFF6366F1)
private val Emerald = Color(0xFF10B981)
private val Amber   = Color(0xFFF59E0B)
private val Red     = Color(0xFFEF4444)

private fun paperTypeColor(type: String?): Color = when (type?.lowercase()) {
    "annual"    -> Blue
    "mid term", "midterm" -> Emerald
    "quarterly" -> Amber
    "monthly"   -> Indigo
    else        -> Color(0xFF8B5CF6)
}

// ── Main Screen ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionPaperScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val data by viewModel.questionPapers.collectAsState()
    val structure by viewModel.schoolStructure.collectAsState()
    val context = LocalContext.current

    // Filter state
    var selectedClassId by remember { mutableIntStateOf(0) }
    var selectedClassName by remember { mutableStateOf("All Classes") }
    var selectedSubject by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var paperToDelete by remember { mutableStateOf<QuestionPaper?>(null) }

    // Colours
    val bg   = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)

    LaunchedEffect(Unit) {
        viewModel.fetchQuestionPapers()
        viewModel.fetchSchoolStructure()
    }

    // Apply filters when they change
    LaunchedEffect(selectedClassId, selectedSubject, selectedYear, selectedType) {
        viewModel.fetchQuestionPapers(selectedClassId, selectedSubject, selectedYear, selectedType)
    }

    val years = remember(data) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        (currentYear downTo currentYear - 5).map { it.toString() }
    }
    val paperTypes = listOf("", "Annual", "Mid Term", "Quarterly", "Monthly", "Test")

    val papers = remember(data, searchQuery) {
        (data?.papers ?: emptyList()).filter {
            searchQuery.isEmpty() ||
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subject?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(
                        Brush.horizontalGradient(listOf(Indigo, Blue))
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(.15f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Question Papers", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("REPORTS • EDUCATION MODULE", color = Color.White.copy(.65f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        // Stat badge
                        Box(
                            Modifier
                                .background(Color.White.copy(.15f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "${papers.size} Papers",
                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { onOpenWeb("https://wantuch.pk/modules/education/question_papers.php") },
                            modifier = Modifier
                                .background(Emerald.copy(.2f), RoundedCornerShape(10.dp))
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Public, "Open Web Builder", tint = Emerald, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Search Bar
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(.15f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.White.copy(.7f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) Text("Search papers or subjects…", color = Color.White.copy(.5f), fontSize = 14.sp)
                                inner()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.White.copy(.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // ── Filter Chips ──────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Class filter
                val classes = (structure?.classes ?: emptyList())
                item {
                    QPFilterChip(
                        label = selectedClassName,
                        icon = Icons.Default.Class,
                        active = selectedClassId != 0,
                        isDark = isDark
                    ) {
                        // Cycle through classes
                        if (selectedClassId == 0 && classes.isNotEmpty()) {
                            selectedClassId = classes.first().id
                            selectedClassName = classes.first().name
                        } else {
                            val idx = classes.indexOfFirst { it.id == selectedClassId }
                            if (idx >= 0 && idx < classes.size - 1) {
                                selectedClassId = classes[idx + 1].id
                                selectedClassName = classes[idx + 1].name
                            } else {
                                selectedClassId = 0
                                selectedClassName = "All Classes"
                            }
                        }
                    }
                }
                // Year filter
                items(years.take(3)) { yr ->
                    QPFilterChip(
                        label = yr,
                        icon = Icons.Default.CalendarToday,
                        active = selectedYear == yr,
                        isDark = isDark
                    ) { selectedYear = if (selectedYear == yr) "" else yr }
                }
                // Type filter chips
                items(paperTypes.drop(1)) { type ->
                    QPFilterChip(
                        label = type,
                        icon = Icons.Default.Assignment,
                        active = selectedType == type,
                        color = paperTypeColor(type),
                        isDark = isDark
                    ) { selectedType = if (selectedType == type) "" else type }
                }
            }

            // ── Stats Row ─────────────────────────────────────────────────
            data?.stats?.let { stats ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QPStatCard("Total",    stats["total"]?.toString()    ?: "0", Blue,    Modifier.weight(1f))
                    QPStatCard("Annual",   stats["annual"]?.toString()   ?: "0", Indigo,  Modifier.weight(1f))
                    QPStatCard("Mid Term", stats["mid_term"]?.toString() ?: "0", Emerald, Modifier.weight(1f))
                }
            }

            // ── Paper List ────────────────────────────────────────────────
            if (isLoading && data == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Blue)
                }
            } else if (papers.isEmpty()) {
                QPEmptyState(isDark)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(papers, key = { it.id }) { paper ->
                        QPPaperCard(
                            paper = paper,
                            isDark = isDark,
                            onOpenWeb = onOpenWeb,
                            onDelete = { paperToDelete = paper }
                        )
                    }
                }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────
        FloatingActionButton(
            onClick = { onOpenWeb("paper_builder") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .shadow(12.dp, CircleShape),
            containerColor = Indigo,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, "Add Question Paper", modifier = Modifier.size(26.dp))
        }
    }

    // ── Add Paper Dialog ──────────────────────────────────────────────────────
    if (showAddDialog) {
        QPAddDialog(
            isDark = isDark,
            classes = structure?.classes ?: emptyList(),
            paperTypes = paperTypes.drop(1),
            years = years,
            onDismiss = { showAddDialog = false },
            onSave = { title, subject, classId, year, totalMarks, paperType ->
                viewModel.saveQuestionPaper(
                    title, subject, classId, year, totalMarks, paperType,
                    onSuccess = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // ── Delete Confirmation Dialog ────────────────────────────────────────────
    paperToDelete?.let { paper ->
        AlertDialog(
            onDismissRequest = { paperToDelete = null },
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
            icon = { Icon(Icons.Default.DeleteForever, null, tint = Red, modifier = Modifier.size(32.dp)) },
            title = { Text("Delete Paper?", color = if (isDark) Color.White else Color(0xFF1E293B), fontWeight = FontWeight.Black) },
            text = { Text("\"${paper.title}\" will be permanently removed.", color = if (isDark) Color.White.copy(.6f) else Color.Gray, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteQuestionPaper(paper.id,
                            onSuccess = { paperToDelete = null },
                            onError = { paperToDelete = null }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { paperToDelete = null }) {
                    Text("Cancel", color = if (isDark) Color.White.copy(.5f) else Color.Gray)
                }
            }
        )
    }
}

// ── Paper Card ────────────────────────────────────────────────────────────────
@Composable
private fun QPPaperCard(paper: QuestionPaper, isDark: Boolean, onOpenWeb: (String) -> Unit, onDelete: () -> Unit) {
    val typeColor = paperTypeColor(paper.paper_type)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            // Left Icon Decoration
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(typeColor.copy(.12f))
                    .border(1.dp, typeColor.copy(.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = typeColor, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                // Category & Year Badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    paper.paper_type?.let { type ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(typeColor.copy(.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(type.uppercase(), color = typeColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    paper.year?.let { yr ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Gray.copy(.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(yr, color = if (isDark) Color.White.copy(.6f) else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                // Paper Title
                Text(
                    paper.title, 
                    color = textColor, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 2, 
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))
                // Metadata Row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    paper.subject?.let { QPMeta(Icons.Default.Book, it) }
                    paper.class_name?.let { QPMeta(Icons.Default.Class, it) }
                    paper.total_marks?.let { QPMeta(Icons.Default.Stars, "$it Marks") }
                }

                paper.uploaded_by?.let { uploader ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("By $uploader", color = Color.Gray, fontSize = 10.sp)
                        paper.created_at?.let { date ->
                            Text(" • $date", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Preview & Download Action Row ───────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview Button - Side-by-Side
                    IconButton(
                        onClick = {
                            val previewUrl = "https://wantuch.pk/modules/education/print_question_paper.php?id=${paper.id}"
                            onOpenWeb(previewUrl)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Indigo.copy(.10f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Preview Paper",
                            tint = Indigo,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Download Button - Side-by-Side
                    IconButton(
                        onClick = {
                            if (!paper.file_path.isNullOrEmpty()) {
                                try {
                                    val fileUrl = paper.file_path
                                    val rawName = fileUrl.substringAfterLast("/")
                                    val fileName = if (rawName.endsWith(".pdf", ignoreCase = true)) rawName 
                                                  else "${paper.title.replace(" ", "_").take(20)}.pdf"
                                    
                                    val request = DownloadManager.Request(fileUrl.toUri())
                                        .setTitle(paper.title)
                                        .setDescription("Downloading Question Paper PDF...")
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                        .setMimeType("application/pdf")
                                    
                                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    dm.enqueue(request)
                                    Toast.makeText(context, "Downloading \"$fileName\"...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Download link not available for this record.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Emerald.copy(.10f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Paper",
                            tint = Emerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Quick Delete Action
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(34.dp)
                    .background(Red.copy(.08f), RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = Red, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Filter Chip ───────────────────────────────────────────────────────────────
@Composable
private fun QPFilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    isDark: Boolean,
    color: Color = Blue,
    onClick: () -> Unit
) {
    val bg = if (active) color else if (isDark) Color.White.copy(.07f) else Color.Black.copy(.05f)
    val content = if (active) Color.White else if (isDark) Color.White.copy(.65f) else Color.Gray

    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, if (active) color else Color.Transparent, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = content, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
@Composable
private fun QPStatCard(label: String, value: String, accent: Color, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(.08f))
            .border(1.dp, accent.copy(.2f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(label.uppercase(), color = accent.copy(.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────
@Composable
private fun QPEmptyState(isDark: Boolean) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Blue.copy(.08f))
                    .border(1.dp, Blue.copy(.2f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MenuBook, null, tint = Blue, modifier = Modifier.size(46.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("No Papers Found", color = if (isDark) Color.White else Color(0xFF1E293B), fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Tap + to add a question paper\nor change your filters", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

// ── Meta Row Item ─────────────────────────────────────────────────────────────
@Composable
private fun QPMeta(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Add Paper Dialog ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QPAddDialog(
    isDark: Boolean,
    classes: List<SchoolClass>,
    paperTypes: List<String>,
    years: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var totalMarks by remember { mutableStateOf("100") }
    var selectedClassId by remember { mutableStateOf(if (classes.isNotEmpty()) classes.first().id else 0) }
    var selectedClassName by remember { mutableStateOf(if (classes.isNotEmpty()) classes.first().name else "Select Class") }
    var selectedYear by remember { mutableStateOf(years.first()) }
    var selectedType by remember { mutableStateOf(paperTypes.first()) }

    var classExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    val bg   = if (isDark) Color(0xFF0F172A) else Color.White
    val text = if (isDark) Color.White       else Color(0xFF1E293B)
    val sub  = if (isDark) Color.White.copy(.55f) else Color.Gray

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(.5f))
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.88f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(bg)
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Handle
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.Gray.copy(.3f)))
                }
                Spacer(Modifier.height(20.dp))

                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Indigo.copy(.12f)).border(1.dp, Indigo.copy(.3f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NoteAdd, null, tint = Indigo, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Add Question Paper", color = text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Fill in the paper details below", color = sub, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Fields ────────────────────────────────────────────────
                QPLabel("Paper Title *", isDark)
                QPTextField(title, { title = it }, "e.g. Annual Examination Paper 2025", isDark)
                Spacer(Modifier.height(14.dp))

                QPLabel("Subject *", isDark)
                QPTextField(subject, { subject = it }, "e.g. Mathematics, Physics", isDark)
                Spacer(Modifier.height(14.dp))

                QPLabel("Total Marks", isDark)
                QPTextField(totalMarks, { totalMarks = it }, "100", isDark, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                Spacer(Modifier.height(14.dp))

                // Class Dropdown
                QPLabel("Class *", isDark)
                QPDropdown(label = selectedClassName, expanded = classExpanded, isDark = isDark, onToggle = { classExpanded = !classExpanded }) {
                    classes.forEach { cls ->
                        DropdownMenuItem(
                            text = { Text(cls.name, color = if (isDark) Color.White else Color(0xFF1E293B)) },
                            onClick = {
                                selectedClassId = cls.id
                                selectedClassName = cls.name
                                classExpanded = false
                            }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Year Dropdown
                QPLabel("Year *", isDark)
                QPDropdown(label = selectedYear, expanded = yearExpanded, isDark = isDark, onToggle = { yearExpanded = !yearExpanded }) {
                    years.forEach { yr ->
                        DropdownMenuItem(
                            text = { Text(yr, color = if (isDark) Color.White else Color(0xFF1E293B)) },
                            onClick = { selectedYear = yr; yearExpanded = false }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Paper Type Dropdown
                QPLabel("Paper Type *", isDark)
                QPDropdown(
                    label = selectedType,
                    expanded = typeExpanded,
                    isDark = isDark,
                    accentColor = paperTypeColor(selectedType),
                    onToggle = { typeExpanded = !typeExpanded }
                ) {
                    paperTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, color = paperTypeColor(type), fontWeight = FontWeight.Bold) },
                            onClick = { selectedType = type; typeExpanded = false }
                        )
                    }
                }
                Spacer(Modifier.height(30.dp))

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(.3f))
                    ) {
                        Text("Cancel", color = if (isDark) Color.White.copy(.7f) else Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (title.isBlank() || subject.isBlank()) return@Button
                            onSave(title.trim(), subject.trim(), selectedClassId, selectedYear, totalMarks, selectedType)
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                        shape = RoundedCornerShape(14.dp),
                        enabled = title.isNotBlank() && subject.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Paper", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Small Helpers ─────────────────────────────────────────────────────────────

@Composable
private fun QPLabel(text: String, isDark: Boolean) {
    Text(
        text, color = if (isDark) Color.White.copy(.55f) else Color.Gray,
        fontSize = 10.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QPTextField(
    value: String, onValueChange: (String) -> Unit, placeholder: String, isDark: Boolean,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    val bg = if (isDark) Color.White.copy(.05f) else Color.Black.copy(.04f)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = if (isDark) Color.White else Color(0xFF1E293B),
            fontSize = 14.sp
        ),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, if (isDark) Color.White.copy(.08f) else Color.Black.copy(.08f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                if (value.isEmpty()) Text(placeholder, color = Color.Gray.copy(.5f), fontSize = 14.sp)
                inner()
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun QPDropdown(
    label: String,
    expanded: Boolean,
    isDark: Boolean,
    accentColor: Color = Blue,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val bg = if (isDark) Color.White.copy(.05f) else Color.Black.copy(.04f)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(1.dp, if (isDark) Color.White.copy(.08f) else Color.Black.copy(.08f), RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = if (isDark) Color.White else Color(0xFF1E293B), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null, tint = accentColor, modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onToggle,
            modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
        ) {
            content()
        }
    }
}
