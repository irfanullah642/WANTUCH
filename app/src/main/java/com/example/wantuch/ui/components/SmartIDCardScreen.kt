package com.example.wantuch.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.widget.Toast
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.wantuch.domain.model.*
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartIDCardScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White

    // Data states
    val students by viewModel.students.collectAsState()
    val staffList by viewModel.staffList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var isStaffMode by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<StudentMember?>(null) }
    var selectedStaff by remember { mutableStateOf<com.example.wantuch.domain.model.StaffMember?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generatedCardPath by remember { mutableStateOf("") }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            // Upload immediately
            selectedStudent?.let { student ->
                val base64 = bitmapToBase64(bitmap)
                viewModel.uploadProfilePic((student.id as? Number)?.toInt() ?: 0, base64) {
                    Toast.makeText(context, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchStudents()
        // Ensure staff is loaded if not already triggered by dashboard
        if (staffList.isEmpty()) {
            val lastId = context.getSharedPreferences("wantuch_prefs", Context.MODE_PRIVATE).getInt("last_inst", 0)
            if (lastId != 0) viewModel.fetchStaff()
        }
    }

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Box(Modifier.fillMaxWidth().statusBarsPadding().background(Brush.horizontalGradient(listOf(Color(0xFF1E40AF), Color(0xFF3B82F6)))).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, Modifier.background(Color.White.copy(0.1f), RoundedCornerShape(10.dp))) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(15.dp))
                    Column {
                        Text("Smart ID Card", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("DIGITAL IDENTITY SYSTEM", color = Color.White.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                
                // --- TOGGLE TABS (STUDENT vs STAFF) ---
                Row(Modifier.fillMaxWidth().height(45.dp).clip(RoundedCornerShape(12.dp)).background(cardBg), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)).background(if(!isStaffMode) Color(0xFF3B82F6) else Color.Transparent).clickable { isStaffMode = false; capturedBitmap = null; generatedCardPath = "" }, Alignment.Center) {
                        Text("Student IDs", color = if(!isStaffMode) Color.White else Color.Gray, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)).background(if(isStaffMode) Color(0xFF10B981) else Color.Transparent).clickable { isStaffMode = true; capturedBitmap = null; generatedCardPath = "" }, Alignment.Center) {
                        Text("Staff & Service", color = if(isStaffMode) Color.White else Color.Gray, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                // Step 1: Select Member
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardBg)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("1. Select Member", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(10.dp))
                        
                        var expanded by remember { mutableStateOf(false) }
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                val text = if (!isStaffMode) selectedStudent?.name ?: "Select Student..." else selectedStaff?.name ?: "Select Staff..."
                                Text(text)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                if (!isStaffMode) {
                                    students.forEach { student ->
                                        DropdownMenuItem(
                                            text = { Text("${student.name} (${student.class_no})") },
                                            onClick = {
                                                selectedStudent = student
                                                expanded = false
                                                generatedCardPath = ""
                                            }
                                        )
                                    }
                                } else {
                                    staffList.forEach { staff ->
                                        DropdownMenuItem(
                                            text = { Text("${staff.name} (${staff.role})") },
                                            onClick = {
                                                selectedStaff = staff
                                                expanded = false
                                                generatedCardPath = ""
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if ((!isStaffMode && selectedStudent != null) || (isStaffMode && selectedStaff != null)) {
                    // Step 2: Photo Capture
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardBg)) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("2. Profile Photo", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(15.dp))
                            
                            Box(Modifier.size(120.dp).clip(RoundedCornerShape(20.dp)).background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)).border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(20.dp)), Alignment.Center) {
                                if (capturedBitmap != null) {
                                    Image(capturedBitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    val picUrl = if (!isStaffMode) selectedStudent?.profile_pic else selectedStaff?.profile_pic
                                    if (!picUrl.isNullOrEmpty()) {
                                        AsyncImage(picUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(50.dp), tint = Color.Gray)
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(15.dp))
                            
                            Button(
                                onClick = { cameraLauncher.launch() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Capture Photo")
                            }
                        }
                    }

                    // Step 3: Card Preview
                    Card(
                        Modifier.fillMaxWidth().height(200.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            // Professional Background
                            Box(Modifier.fillMaxWidth().height(50.dp).background(Color(0xFF1E40AF)))
                            
                            Column(Modifier.fillMaxSize()) {
                                Text("RANA COLLEGE OF SCIENCE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = TextAlign.Center)
                                
                                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)) {
                                        val picUrl = if (!isStaffMode) selectedStudent?.profile_pic else selectedStaff?.profile_pic
                                        if (capturedBitmap != null) {
                                            Image(capturedBitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else if (!picUrl.isNullOrEmpty()) {
                                            AsyncImage(picUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                    }
                                    Spacer(Modifier.width(15.dp))
                                    Column {
                                        if (!isStaffMode) {
                                            Text(selectedStudent?.name?.uppercase() ?: "", color = Color(0xFF1E40AF), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                            Text("Roll No: ${selectedStudent?.class_no}", color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Class: ${selectedStudent?.class_section ?: "N/A"}", color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("ID: #${selectedStudent?.id}", color = Color.Gray, fontSize = 10.sp)
                                        } else {
                                            Text(selectedStaff?.name?.uppercase() ?: "", color = Color(0xFF10B981), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                            Text("Role: ${selectedStaff?.role}", color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Emp ID: #${selectedStaff?.id}", color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.weight(1f))
                                Box(Modifier.fillMaxWidth().height(25.dp).background(Color(0xFFF1F5F9)), Alignment.Center) {
                                    Text("Valid till: 2026", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            // QR Placeholder
                            Icon(Icons.Default.QrCode, null, modifier = Modifier.size(45.dp).align(Alignment.BottomEnd).padding(end = 15.dp, bottom = 35.dp), tint = Color(0xFF64748B))
                        }
                    }

                    // Step 4: Actions
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val targetId = if (!isStaffMode) (selectedStudent?.id as? Number)?.toInt() ?: 0 else selectedStaff?.id?.toString()?.toIntOrNull() ?: 0
                                val targetType = if (!isStaffMode) "student" else "service"
                                viewModel.generateIdCard(targetId, targetType,
                                    onSuccess = { path -> generatedCardPath = path; Toast.makeText(context, "ID Generated!", Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            else {
                                Icon(Icons.Default.CreditCard, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generate ID")
                            }
                        }

                        if (generatedCardPath.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                val brandBlue = Color(0xFF1E40AF)
                                IconButton(onClick = { onOpenWeb(generatedCardPath) }) {
                                    Icon(
                                        Icons.Default.Visibility, 
                                        contentDescription = "Preview", 
                                        tint = brandBlue, 
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { 
                                        val filename = if (!isStaffMode) "ID_Card_${selectedStudent?.name}.pdf" else "ID_Card_${selectedStaff?.name}.pdf"
                                        downloadFile(context, generatedCardPath, filename) 
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.FileDownload, 
                                        contentDescription = "Download", 
                                        tint = brandBlue, 
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}

private fun downloadFile(context: Context, url: String, name: String) {
    try {
        val request = android.app.DownloadManager.Request(url.toUri())
            .setTitle(name)
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, name)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
