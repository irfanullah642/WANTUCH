package com.example.wantuch.ui.components

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataExploration
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseManagementScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current
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

    val tables = listOf(
        "edu_admin_settings", "edu_admission_withdrawal", "edu_assignment_submissions",
        "edu_assignments", "edu_attendance", "edu_attendance_reedit_requests",
        "edu_attendance_submission", "edu_bank_accounts", "edu_classes", "edu_driver",
        "edu_element_access", "edu_exam_marks", "edu_exam_settings", "edu_exams",
        "edu_extra_classes", "edu_fee_management", "edu_fee_payments_log", "edu_fee_structure",
        "edu_fee_types", "edu_fee_years", "edu_fines", "edu_fingerprints", "edu_fingerprints_logs",
        "edu_fund_transactions", "edu_fund_types", "edu_institution_assets", "edu_institution_bank",
        "edu_institution_contacts", "edu_institution_fund_history", "edu_institution_funds",
        "edu_institution_posts", "edu_institutions", "edu_leaves", "edu_lectures", "edu_modules",
        "edu_notice_targets", "edu_notices", "edu_notifications", "edu_parent_child",
        "edu_parent_wards", "edu_payment_methods", "edu_payroll_records", "edu_promotion_log",
        "edu_public_holidays", "edu_question_papers", "edu_role_permissions", "edu_salary_payments",
        "edu_sections", "edu_staff_assignments", "edu_staff_attendance", "edu_staff_competencies",
        "edu_staff_payments", "edu_student_enrollment", "edu_student_exam_papers",
        "edu_student_results", "edu_study_plans", "edu_subjects", "edu_substitutions",
        "edu_syllabus", "edu_timetable", "edu_transport_assignments", "edu_transport_charges",
        "edu_transport_locations", "edu_transport_types", "edu_transports_expands",
        "edu_user_academics", "edu_user_bank", "edu_user_contacts", "edu_user_experience",
        "edu_user_professional", "edu_users", "edu_vehicle_types", "edu_vehicles",
        "edu_visitor_logs", "edu_whatsapp_logs", "edu_wizard_settings"
    )

    var selectedTables by remember { mutableStateOf(setOf<String>()) }
    var dataOnlyExport by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(-1f) }
    var importProgress by remember { mutableStateOf(-1f) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = { Text("Database Management", fontWeight = FontWeight.Bold, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Storage,
                contentDescription = null,
                tint = Color(0xFF4F46E5),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Database Management",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                "Export, backup or merge-safe import for Education module tables.",
                fontSize = 13.sp,
                color = labelColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // EXPORT DATA CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Data", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                        }
                        TextButton(
                            onClick = {
                                selectedTables = if (selectedTables.size == tables.size) setOf() else tables.toSet()
                            }
                        ) {
                            Text(
                                if (selectedTables.size == tables.size) "Deselect All" else "Select All",
                                color = Color(0xFF4F46E5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // TABLE LIST
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        ) {
                            items(tables) { table ->
                                val isSelected = selectedTables.contains(table)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTables = if (isSelected) selectedTables - table else selectedTables + table
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedTables = if (checked) selectedTables + table else selectedTables - table
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                    )
                                    Text(table, fontSize = 14.sp, color = textColor)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dataOnlyExport = !dataOnlyExport }
                    ) {
                        Checkbox(
                            checked = dataOnlyExport,
                            onCheckedChange = { dataOnlyExport = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                        )
                        Column {
                            Text("Data Only Export", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                            Text("Exclude table structure queries (CREATE TABLE)", fontSize = 12.sp, color = labelColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (selectedTables.isEmpty()) {
                                Toast.makeText(context, "Please select at least one table", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isExporting = true
                            exportProgress = 0f
                            viewModel.exportDatabase(
                                tables = selectedTables.toList(),
                                dataOnly = dataOnlyExport,
                                context = context,
                                onProgress = { progress -> exportProgress = progress },
                                onComplete = {
                                    isExporting = false
                                    exportProgress = -1f
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isExporting) {
                            if (exportProgress >= 0f) {
                                CircularProgressIndicator(progress = exportProgress, color = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("${(exportProgress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate .sql file", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // IMPORT DATA CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DataExploration, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import & Merge", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Warning Alert
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Merge Safe Mode: Use INSERT IGNORE to append data without overwriting existing records.",
                                color = Color(0xFFB45309),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select SQL File", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // File selection row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Choose file", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (selectedFileUri != null) "File selected" else "No file chosen",
                            fontSize = 13.sp,
                            color = labelColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info Alert
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Auto-backup Enabled",
                                    color = Color(0xFF1D4ED8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "A recovery point will be created automatically before starting the import.",
                                    color = Color(0xFF2563EB),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (selectedFileUri == null) {
                                Toast.makeText(context, "Please choose a SQL file to import", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isImporting = true
                            importProgress = 0f
                            viewModel.importDatabase(
                                uri = selectedFileUri!!,
                                context = context,
                                onProgress = { progress -> importProgress = progress },
                                onComplete = {
                                    isImporting = false
                                    importProgress = -1f
                                    selectedFileUri = null
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isImporting) {
                            if (importProgress >= 0f) {
                                CircularProgressIndicator(progress = importProgress, color = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("${(importProgress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Data", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
