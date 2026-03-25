package com.example.wantuch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import kotlinx.coroutines.launch

@Composable
fun MarksTabContent(
    viewModel: WantuchViewModel,
    isDark: Boolean,
    openWeb: (String) -> Unit
) {
    val schoolStructure by viewModel.schoolStructure.collectAsState()
    val classes = schoolStructure?.classes ?: emptyList()
    val context = androidx.compose.ui.platform.LocalContext.current

    val classOptions = remember(classes) { listOf("Select Class") + classes.map { it.name } }
    var selectedClassName by remember { mutableStateOf("Select Class") }
    val currentClassObj = classes.find { it.name == selectedClassName }
    
    val sectionOptions = remember(currentClassObj) { 
        listOf("Select Section") + (currentClassObj?.sections?.map { it.name } ?: emptyList()) 
    }
    var selectedSectionName by remember { mutableStateOf("Select Section") }

    var examList by remember { mutableStateOf<List<com.example.wantuch.domain.model.AwardListExam>>(emptyList()) }
    var selectedExamId by remember { mutableStateOf<String?>(null) }
    var selectedExamName by remember { mutableStateOf("Select Exam") }

    var studentsList by remember { mutableStateOf<List<com.example.wantuch.domain.model.AwardListStudent>?>(null) }
    var totalMarks by remember { mutableStateOf<String?>("100") }
    var isLoadingStudents by remember { mutableStateOf(false) }
    val editingMarks = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(selectedClassName, selectedSectionName) {
        if (selectedClassName != "Select Class" && selectedSectionName != "Select Section") {
            val cid = classes.find { it.name == selectedClassName }?.id ?: 0
            val sid = classes.find { it.name == selectedClassName }?.sections?.find { it.name == selectedSectionName }?.id ?: 0
            if (cid > 0 && sid > 0) {
                viewModel.getAwardListExams(cid, sid, onSuccess = { 
                    examList = it.exams
                    selectedExamName = "Select Exam"
                }, onError = {
                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                })
            }
        } else {
            examList = emptyList()
            selectedExamId = null
            selectedExamName = "Select Exam"
            studentsList = null
        }
    }

    LaunchedEffect(selectedExamId) {
        if (selectedExamId != null) {
            val cid = classes.find { it.name == selectedClassName }?.id ?: 0
            val sid = classes.find { it.name == selectedClassName }?.sections?.find { it.name == selectedSectionName }?.id ?: 0
            if (cid > 0 && sid > 0) {
                isLoadingStudents = true
                viewModel.getAwardListStudents(selectedExamId!!, cid, sid, onSuccess = {
                    studentsList = it.students.map { student -> 
                        student.copy(marks = student.marks?.takeIf { mark -> mark.isNotBlank() })
                    }
                    totalMarks = it.total_marks ?: "100"
                    editingMarks.clear()
                    it.students.forEach { s ->
                        editingMarks[s.student_id] = s.marks?.takeIf { m -> m.isNotBlank() } ?: "0"
                    }
                    isLoadingStudents = false
                }, onError = {
                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                    isLoadingStudents = false
                })
            }
        }
    }

    val examOptions = remember(examList) { listOf("Select Exam") + examList.map { "${it.exam_type} - ${it.subject_name}" } }
    
    var inputMode by remember { mutableStateOf("Manual") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Mark Entry", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // Dropdowns Container
        Column(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B), RoundedCornerShape(8.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    DropdownSelector(value = selectedClassName, options = classOptions, isDark = isDark) { 
                        selectedClassName = it; studentsList = null 
                        selectedSectionName = "Select Section"
                    }
                }
                Box(Modifier.weight(1f)) {
                    DropdownSelector(value = selectedSectionName, options = sectionOptions, isDark = isDark) { 
                        selectedSectionName = it; studentsList = null 
                    }
                }
            }
            DropdownSelector(value = selectedExamName, options = examOptions, isDark = isDark) {
                selectedExamName = it
                selectedExamId = examList.find { e -> "${e.exam_type} - ${e.subject_name}" == it }?.id
                studentsList = null
            }

            // Input format tabs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { inputMode = "Manual" },
                    colors = ButtonDefaults.buttonColors(containerColor = if (inputMode == "Manual") Color(0xFF38BDF8) else Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Manual", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                
                Button(
                    onClick = { inputMode = "Text" },
                    colors = ButtonDefaults.buttonColors(containerColor = if (inputMode == "Text") Color(0xFF38BDF8) else Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.TextSnippet, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Text", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = { inputMode = "CSV" },
                    colors = ButtonDefaults.buttonColors(containerColor = if (inputMode == "CSV") Color(0xFF38BDF8) else Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("CSV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        if (isLoadingStudents) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (studentsList != null) {
            if (inputMode == "Manual") {
            // Main Marks Area
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
            ) {
                // Top Info Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Marks: $totalMarks", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    OutlinedButton(
                        onClick = {
                            val view = android.app.AlertDialog.Builder(context)
                            view.setTitle("Delete All")
                            view.setMessage("Are you sure you want to delete ALL marks for this exam?")
                            view.setPositiveButton("Yes") { _, _ ->
                                val examIdInt = selectedExamId?.toIntOrNull() ?: 0
                                viewModel.deleteFullAwardList(examIdInt, onSuccess = {
                                    android.widget.Toast.makeText(context, "All Marks Cleared", android.widget.Toast.LENGTH_SHORT).show()
                                    studentsList = studentsList!!.map { it.copy(marks = null) }
                                }, onError = {
                                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                                })
                            }
                            view.setNegativeButton("No", null)
                            view.show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D9488)),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("DELETE ALL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF0F172A).copy(alpha = 0.5f)).padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Text("CNO NO", Modifier.weight(0.5f), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text("STUDENT NAME", Modifier.weight(1.5f), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text("MARKS", Modifier.weight(1f), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("STATUS", Modifier.weight(1f), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }

                if (studentsList!!.isEmpty()) {
                    Text("No students found in this section.", color = Color(0xFF94A3B8), modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                }

                studentsList!!.forEach { s ->
                    val markValue = editingMarks[s.student_id] ?: "0"
                    val isSaved = s.marks != null
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFF334155).copy(alpha = 0.5f)).padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.roll_number ?: "---", Modifier.weight(0.5f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(s.full_name ?: "Unknown", Modifier.weight(1.5f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            BasicTextField(
                                value = markValue,
                                onValueChange = { editingMarks[s.student_id] = it },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                                modifier = Modifier.fillMaxWidth(0.9f).height(28.dp)
                                    .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                                    .padding(top = 6.dp),
                                singleLine = true
                            )
                        }

                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            if (isSaved) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Saved", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Not Entered", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Button(
                onClick = {
                    if (editingMarks.isNotEmpty()) {
                        val marksObj = editingMarks.map { "{\"student_id\":\"${it.key}\", \"marks\":\"${it.value}\"}" }.joinToString(",")
                        val mJson = "[$marksObj]"
                        viewModel.saveAwardListMarks(selectedExamId ?: "", mJson, onSuccess = {
                            android.widget.Toast.makeText(context, "All Marks Saved", android.widget.Toast.LENGTH_SHORT).show()
                            studentsList = studentsList!!.map { 
                                it.copy(marks = editingMarks[it.student_id])
                            }
                        }, onError = {
                            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                        })
                    } else {
                        android.widget.Toast.makeText(context, "No changes to save.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(45.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D9488))
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF10B981))
                Spacer(Modifier.width(8.dp))
                Text("SAVE ALL MARKS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFFFBEB8C))
            }
            
            Spacer(Modifier.height(50.dp))
        } else if (inputMode == "Text") {
            TextMarksEntry(viewModel, studentsList!!, selectedExamId) { updates ->
                studentsList = studentsList!!.map { if (updates.containsKey(it.student_id)) it.copy(marks = updates[it.student_id]) else it }
                updates.forEach { entry -> editingMarks[entry.key] = entry.value }
            }
        } else if (inputMode == "CSV") {
            CsvMarksEntry(viewModel, studentsList!!, selectedExamId, selectedExamName) { updates ->
                studentsList = studentsList!!.map { if (updates.containsKey(it.student_id)) it.copy(marks = updates[it.student_id]) else it }
                updates.forEach { entry -> editingMarks[entry.key] = entry.value }
            }
        }
    }
}
}

@Composable
fun TextMarksEntry(
    viewModel: WantuchViewModel,
    studentsList: List<com.example.wantuch.domain.model.AwardListStudent>,
    selectedExamId: String?,
    onMarksSaved: (Map<String, String>) -> Unit
) {
    var textInputValue by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)).padding(16.dp)) {
        Text("Enter: Roll No [Space] Name [Space] Marks \n(Comma separate students)", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        
        OutlinedTextField(
            value = textInputValue,
            onValueChange = { textInputValue = it },
            placeholder = { Text("1 Ahmed 85, 2 Sara 90, ...", color = Color(0xFF94A3B8).copy(0.5f)) },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF334155),
                focusedBorderColor = Color(0xFF38BDF8)
            )
        )
        
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val entries = textInputValue.split(",")
                val updates = mutableMapOf<String, String>()
                for (entry in entries) {
                    if (entry.isBlank()) continue
                    val parts = entry.trim().split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        val rollNo = parts.first().trim()
                        val mark = parts.last().trim()
                        val student = studentsList.find { it.roll_number == rollNo }
                        if (student != null) updates[student.student_id] = mark
                    }
                }
                
                if (updates.isNotEmpty()) {
                    val marksObj = updates.map { "{\"student_id\":\"${it.key}\", \"marks\":\"${it.value}\"}" }.joinToString(",")
                    viewModel.saveAwardListMarks(selectedExamId ?: "", "[$marksObj]", onSuccess = {
                        android.widget.Toast.makeText(context, "Text Marks Parsed & Saved", android.widget.Toast.LENGTH_SHORT).show()
                        onMarksSaved(updates)
                        textInputValue = ""
                    }, onError = {
                        android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                    })
                } else {
                    android.widget.Toast.makeText(context, "No valid matching roll numbers found.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(45.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D9488))
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("PARSE & SAVE MARKS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFFFBEB8C))
        }
    }
}

@Composable
fun CsvMarksEntry(
    viewModel: WantuchViewModel,
    studentsList: List<com.example.wantuch.domain.model.AwardListStudent>,
    selectedExamId: String?,
    selectedExamName: String,
    onMarksSaved: (Map<String, String>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val csvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && selectedExamId != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.reader()?.readText() ?: ""
                val lines = content.split("\n").drop(1)
                val updates = mutableMapOf<String, String>()
                lines.forEach { line ->
                    val cols = line.split(",")
                    if (cols.size >= 4) {
                        val sid = cols[0].trim()
                        val mark = cols[3].trim()
                        if (sid.isNotEmpty() && mark.isNotEmpty() && sid != "Student ID") {
                            updates[sid] = mark
                        }
                    }
                }
                if (updates.isNotEmpty()) {
                    val marksObj = updates.map { "{\"student_id\":\"${it.key}\", \"marks\":\"${it.value}\"}" }.joinToString(",")
                    viewModel.saveAwardListMarks(selectedExamId ?: "", "[$marksObj]", onSuccess = {
                        android.widget.Toast.makeText(context, "CSV Marks Saved", android.widget.Toast.LENGTH_SHORT).show()
                        onMarksSaved(updates)
                    }, onError = {
                        android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                    })
                } else {
                    android.widget.Toast.makeText(context, "No valid marks found in CSV", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch(e: Exception) {
                android.widget.Toast.makeText(context, "Error reading CSV", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = {
                try {
                    val csvContent = "Student ID,Roll No,Student Name,Marks\n" + studentsList.joinToString("\n") { "${it.student_id},${it.roll_number ?: ""},${it.full_name ?: ""},${it.marks ?: ""}" }
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloadsDir, "Marks_Template_${selectedExamName.replace(" ", "_").replace("-", "_")}.csv")
                    file.writeText(csvContent)
                    android.widget.Toast.makeText(context, "Saved to Downloads: ${file.name}", android.widget.Toast.LENGTH_LONG).show()
                } catch(e: Exception) {
                    android.widget.Toast.makeText(context, "Error exporting CSV: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.weight(1f).height(45.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D9488))
        ) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("EXPORT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        Button(
            onClick = { csvLauncher.launch("text/*") },
            modifier = Modifier.weight(1f).height(45.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D9488))
        ) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("IMPORT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
