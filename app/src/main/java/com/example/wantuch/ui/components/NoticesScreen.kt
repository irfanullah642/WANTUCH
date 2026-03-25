package com.example.wantuch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import com.example.wantuch.domain.model.Notice
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoticesScreen(viewModel: WantuchViewModel, onBack: () -> Unit) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    var showCreateModal by remember { mutableStateOf(false) }
    var noticeToEdit by remember { mutableStateOf<Notice?>(null) }
    var selectedNotice by remember { mutableStateOf<Notice?>(null) }
    
    val noticesResponse by viewModel.noticesData.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val notices = noticesResponse?.notices ?: emptyList()
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotices = remember(notices, searchQuery) {
        if (searchQuery.isEmpty()) notices
        else notices.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.detail.contains(searchQuery, ignoreCase = true) ||
            (it.creator_name ?: "").contains(searchQuery, ignoreCase = true)
        }
    }
    
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.updateRole()
        viewModel.fetchNotices()
    }
    
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val labelColor = if (isDark) Color.White.copy(0.6f) else Color.Gray

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, labelColor.copy(0.2f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Notice Hub Pill
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(30.dp),
                    border = BorderStroke(1.dp, labelColor.copy(0.1f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = labelColor.copy(0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Campaign, contentDescription = "Notice", tint = textColor, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "NOTICE HUB",
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.Lock, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF6366F1).copy(0.1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(0.3f))
                                ) {
                                    Text(
                                        "TACTILE",
                                        color = Color(0xFF6366F1),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    userRole,
                                    color = Color.Red,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "OFFICIAL ANNOUNCEMENT NODE",
                                color = labelColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Theme Toggle
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, labelColor.copy(0.2f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme", tint = textColor, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Filter/Search Bar
            Surface(
                color = cardColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, labelColor.copy(0.1f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = labelColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 14.sp),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search notices...", color = labelColor.copy(0.5f), fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = labelColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Add Button (Cyan Leaf Shape) - HIDDEN FROM STUDENTS
            if (userRole != "Student") {
                Surface(
                    color = Color(0xFFE0F7FA), // Light cyan
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
                    border = BorderStroke(2.dp, Color(0xFF00BCD4)),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = { showCreateModal = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Notice", tint = Color(0xFF0097A7))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (notices.isEmpty()) {
                Spacer(Modifier.weight(1f))
                Surface(
                    color = labelColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = "Empty",
                            tint = labelColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "NO NOTICES",
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Check back later for updates",
                    color = labelColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(Modifier.weight(1.5f))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredNotices) { notice ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, labelColor.copy(0.1f)),
                            modifier = Modifier.fillMaxWidth().clickable { selectedNotice = notice }
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    text = notice.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor,
                                    lineHeight = 22.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${notice.creator_name ?: "System"} • ${notice.notice_date}",
                                        fontSize = 11.sp,
                                        color = labelColor,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = notice.detail,
                                    fontSize = 14.sp,
                                    color = textColor.copy(0.85f),
                                    lineHeight = 22.sp,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                if (userRole != "Student") {
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { noticeToEdit = notice }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(onClick = { 
                                            viewModel.deleteNotice(
                                                noticeId = notice.id.toInt(),
                                                onSuccess = { viewModel.fetchNotices() },
                                                onError = { errorMsg ->
                                                    android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (showCreateModal || noticeToEdit != null) {
            CreateNoticeModal(
                isDark = isDark,
                viewModel = viewModel,
                noticeToEdit = noticeToEdit,
                onDismiss = { 
                    showCreateModal = false 
                    noticeToEdit = null
                },
                onSave = { data ->
                    viewModel.saveNotice(
                        noticeData = data,
                        onSuccess = { 
                            showCreateModal = false
                            noticeToEdit = null 
                            viewModel.fetchNotices() 
                        },
                        onError = { errorMsg ->
                            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }

        if (selectedNotice != null) {
            ViewNoticeModal(
                notice = selectedNotice!!,
                isDark = isDark,
                userRole = userRole,
                onDismiss = { selectedNotice = null },
                onEdit = {
                    noticeToEdit = selectedNotice
                    selectedNotice = null
                },
                onDelete = {
                    viewModel.deleteNotice(
                        noticeId = selectedNotice!!.id.toInt(),
                        onSuccess = { 
                            selectedNotice = null
                            viewModel.fetchNotices()
                        },
                        onError = { errorMsg ->
                            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onWhatsapp = {
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        putExtra(android.content.Intent.EXTRA_TEXT, "*${selectedNotice!!.title}*\n\n${selectedNotice!!.detail}")
                        type = "text/plain"
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(sendIntent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "WhatsApp not installed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun ViewNoticeModal(
    notice: Notice,
    isDark: Boolean,
    userRole: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWhatsapp: () -> Unit
) {
    val bgDialog = if (isDark) Color(0xFF1E293B) else Color(0xFFFCF9F4)
    val textMain = if (isDark) Color.White else Color.Black
    val borderLight = if (isDark) Color(0xFF334155) else Color.LightGray.copy(alpha = 0.5f)
    val readingPill = if (isDark) Color(0xFF334155) else Color(0xFFE8E5E0)
    
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgDialog),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Close button top right
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.TopEnd) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE0F7FA),
                        border = BorderStroke(1.dp, Color(0xFF00BCD4)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = notice.title.uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = textMain,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        letterSpacing = 2.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    // Reading Mode Pill
                    Surface(
                        color = readingPill,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "READING MODE",
                            color = textMain.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    androidx.compose.material3.Divider(color = borderLight)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = textMain.copy(0.05f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = textMain.copy(0.7f), modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "POSTED BY ${notice.creator_name ?: "SYSTEM"} • ${notice.notice_date}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = textMain.copy(0.8f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.Divider(color = borderLight)
                    Spacer(Modifier.height(24.dp))

                    // Message Content Box
                    Surface(
                        color = if(isDark) Color(0xFF0F172A) else Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notice.detail,
                            color = textMain.copy(0.9f),
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            modifier = Modifier.padding(24.dp)
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                    androidx.compose.material3.Divider(color = borderLight)
                    Spacer(Modifier.height(20.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NoticeActionBtn(
                            icon = Icons.Default.Share,
                            text = "WHATSAPP",
                            modifier = Modifier.weight(1f),
                            fgColor = textMain,
                            onClick = onWhatsapp
                        )
                        if (userRole != "Student") {
                            NoticeActionBtn(
                                icon = Icons.Default.Edit,
                                text = "EDIT",
                                modifier = Modifier.weight(1f),
                                fgColor = textMain,
                                onClick = onEdit
                            )
                            NoticeActionBtn(
                                icon = Icons.Default.Delete,
                                text = "DELETE",
                                modifier = Modifier.weight(1f),
                                fgColor = textMain,
                                onClick = onDelete
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoticeActionBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier, fgColor: Color, onClick: () -> Unit) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, fgColor),
        modifier = modifier.clickable { onClick() }.height(40.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = fgColor, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun CreateNoticeModal(
    isDark: Boolean,
    viewModel: WantuchViewModel,
    noticeToEdit: Notice? = null,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val schoolStructure by viewModel.schoolStructure.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchSchoolStructure()
    }

    var title by remember { mutableStateOf(noticeToEdit?.title ?: "") }
    var details by remember { mutableStateOf(noticeToEdit?.detail ?: "") }
    
    var targetStudents by remember { mutableStateOf(true) }
    var targetParents by remember { mutableStateOf(true) }
    var targetStaff by remember { mutableStateOf(false) }
    var targetWhatsapp by remember { mutableStateOf(false) }
    
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    // 30 days expiry prefilled
    val expiryTime = Date().time + (30L * 24 * 60 * 60 * 1000)
    val expiry = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(expiryTime))
    
    var noticeDate by remember { mutableStateOf(noticeToEdit?.notice_date ?: today) }
    var expiryDate by remember { mutableStateOf(noticeToEdit?.expiry_date ?: expiry) }

    var selectedClassId by remember { mutableStateOf("") }
    var selectedClassName by remember { mutableStateOf("All Classes / No Spec...") }
    var selectedSectionId by remember { mutableStateOf("") }
    var selectedSectionName by remember { mutableStateOf("All Sections") }

    val bgDialog = if (isDark) Color(0xFF1E293B) else Color.White
    val fgText = if (isDark) Color.White else Color.Black
    val primaryCyan = Color(0xFF00BCD4)
    val inputBg = if (isDark) Color(0xFF334155) else Color(0xFF64748B)

    val calendar = java.util.Calendar.getInstance()
    fun showDatePicker(onDateSelected: (String) -> Unit) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Prepare dropdown options
    val classOptions = mutableListOf("" to "All Classes / No Spec...")
    val sectionOptions = mutableListOf("" to "All Sections")

    schoolStructure?.classes?.forEach { cls ->
        classOptions.add(cls.id.toString() to cls.name)
    }

    if (selectedClassId.isNotEmpty()) {
        schoolStructure?.classes?.find { it.id.toString() == selectedClassId }?.sections?.forEach { sec ->
            sectionOptions.add(sec.id.toString() to sec.name)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = bgDialog),
                modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ) {
                        Row(
                            Modifier.padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "CREATE NOTICE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }

                    Column(Modifier.padding(16.dp)) {
                        // Title
                        Text("Title", color = fgText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        NoticeTextField(title, { title = it }, inputBg)
                        
                        Spacer(Modifier.height(16.dp))

                        // Dates
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Notice Date", color = fgText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                NoticeDateBox(noticeDate, inputBg, onClick = { showDatePicker { noticeDate = it } })
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Expiry Date", color = fgText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                NoticeDateBox(expiryDate, inputBg, onClick = { showDatePicker { expiryDate = it } })
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Message
                        Text("Detail / Message", color = fgText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        NoticeTextField(details, { details = it }, inputBg, minLines = 4)

                        Spacer(Modifier.height(16.dp))

                        // Audience
                        Text("Target Audience", color = fgText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            NoticeCheckbox("Students", targetStudents, { targetStudents = it }, primaryCyan, fgText)
                            NoticeCheckbox("Parents", targetParents, { targetParents = it }, primaryCyan, fgText)
                            NoticeCheckbox("Staff", targetStaff, { targetStaff = it }, primaryCyan, fgText)
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            NoticeCheckbox("WhatsApp", targetWhatsapp, { targetWhatsapp = it }, primaryCyan, fgText)
                        }

                        Spacer(Modifier.height(16.dp))

                        // Class/Section
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Target specific class", color = fgText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                NoticeDropdown(selectedClassName, classOptions, inputBg) { id, name -> 
                                    selectedClassId = id
                                    selectedClassName = name
                                    // Reset section when class changes
                                    selectedSectionId = ""
                                    selectedSectionName = "All Sections"
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Target specific section", color = fgText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                NoticeDropdown(selectedSectionName, sectionOptions, inputBg) { id, name -> 
                                    selectedSectionId = id
                                    selectedSectionName = name
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Footer Buttons
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(45.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, primaryCyan)
                            ) {
                                Text("CANCEL", color = primaryCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                            Button(
                                onClick = {
                                    val map = mutableMapOf(
                                        "title" to title,
                                        "detail" to details,
                                        "notice_date" to noticeDate,
                                        "expiry_date" to expiryDate,
                                        "target_students" to if (targetStudents) "1" else "0",
                                        "target_parents" to if (targetParents) "1" else "0",
                                        "target_staff" to if (targetStaff) "1" else "0",
                                        "target_whatsapp" to if (targetWhatsapp) "1" else "0",
                                        "class_id" to selectedClassId,
                                        "section_id" to selectedSectionId
                                    )
                                    if (noticeToEdit != null) {
                                        map["notice_id"] = noticeToEdit.id
                                    }
                                    onSave(map)
                                },
                                modifier = Modifier.weight(1f).height(45.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryCyan.copy(alpha = 0.2f))
                            ) {
                                Text("SAVE NOTICE", color = primaryCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoticeTextField(value: String, onValueChange: (String) -> Unit, bgColor: Color, minLines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = if(minLines > 1) 100.dp else 45.dp),
        shape = RoundedCornerShape(8.dp),
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = bgColor,
            focusedContainerColor = bgColor,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            unfocusedTextColor = Color.White,
            focusedTextColor = Color.White
        )
    )
}

@Composable
fun NoticeDateBox(value: String, bgColor: Color, onClick: () -> Unit) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(45.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(value, color = Color.White, fontSize = 12.sp)
            Icon(Icons.Default.DateRange, contentDescription = "Date", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun NoticeDropdown(
    label: String, 
    options: List<Pair<String, String>>, // list of id, name
    bgColor: Color,
    onOptionSelected: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(45.dp).clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(bgColor)
        ) {
            options.forEach { (id, name) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(name, color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        onOptionSelected(id, name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun NoticeCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, accentColor: Color, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) }.padding(end = 6.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                checkmarkColor = Color.White,
                uncheckedColor = Color.Gray
            ),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = textColor, fontSize = 12.sp)
    }
}
