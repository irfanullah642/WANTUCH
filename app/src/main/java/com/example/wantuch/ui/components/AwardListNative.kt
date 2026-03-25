package com.example.wantuch.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Visibility
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
fun GenerateAwardListNativeForm(
    viewModel: WantuchViewModel,
    isDark: Boolean
) {
    val schoolStructure by viewModel.schoolStructure.collectAsState()
    val classes = schoolStructure?.classes ?: emptyList()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
    var totalMarks by remember { mutableStateOf<String?>("N/A") }
    var isLoadingStudents by remember { mutableStateOf(false) }

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

    val examOptions = remember(examList) { listOf("Select Exam") + examList.map { "${it.exam_type} - ${it.subject_name}" } }
    
    val editingMarks = remember { mutableStateMapOf<String, String>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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

        OutlinedButton(
            onClick = {
                if (selectedExamId != null) {
                    val cid = classes.find { it.name == selectedClassName }?.id ?: 0
                    val sid = classes.find { it.name == selectedClassName }?.sections?.find { it.name == selectedSectionName }?.id ?: 0
                    if (cid > 0 && sid > 0) {
                        isLoadingStudents = true
                        viewModel.getAwardListStudents(selectedExamId!!, cid, sid, onSuccess = {
                            studentsList = it.students
                            totalMarks = it.total_marks ?: "N/A"
                            isLoadingStudents = false
                            editingMarks.clear()
                        }, onError = {
                            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                            isLoadingStudents = false
                        })
                    } else {
                        android.widget.Toast.makeText(context, "Select Class and Section first", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, "Select an exam first", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(45.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
        ) {
            if (isLoadingStudents) {
                CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("LOADING...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            } else {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("VIEW & PRINT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (studentsList != null) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedExamName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$selectedClassName - $selectedSectionName",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF0F172A).copy(alpha = 0.5f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                ) {
                    Text("ROLL\nNO", Modifier.weight(0.8f), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("STUDENT NAME", Modifier.weight(1.5f), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Left)
                    Text("MARKS\n($totalMarks)", Modifier.weight(1f), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("ACTIONS", Modifier.weight(1f), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }

                if (studentsList!!.isEmpty()) {
                    Text("No students found.", color = Color(0xFF94A3B8), modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                }

                studentsList!!.forEach { s ->
                    val isEditing = editingMarks.containsKey(s.student_id)
                    val markValue = editingMarks[s.student_id] ?: (s.marks ?: "")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .border(0.5.dp, Color(0xFF334155))
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.roll_number ?: "---", Modifier.weight(0.8f), color = Color(0xFFCBD5E1), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                        Text(s.full_name ?: "Unknown", Modifier.weight(1.5f), color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Left)
                        
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (isEditing) {
                                BasicTextField(
                                    value = markValue,
                                    onValueChange = { editingMarks[s.student_id] = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                                    modifier = Modifier.fillMaxWidth().height(25.dp)
                                        .background(Color(0xFF334155), RoundedCornerShape(4.dp))
                                        .padding(top = 4.dp),
                                    singleLine = true
                                )
                            } else {
                                Text(
                                    text = if (markValue.isNullOrBlank()) "---" else markValue, 
                                    color = Color.White, 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            if (isEditing) {
                                IconButton(
                                    onClick = {
                                        val mJson = "[{\"student_id\":\"${s.student_id}\", \"marks\":\"${editingMarks[s.student_id]}\"}]"
                                        viewModel.saveAwardListMarks(selectedExamId ?: "", mJson, onSuccess = {
                                            android.widget.Toast.makeText(context, "Saved", android.widget.Toast.LENGTH_SHORT).show()
                                            editingMarks.remove(s.student_id)
                                            studentsList = studentsList!!.map { if (it.student_id == s.student_id) it.copy(marks = markValue) else it }
                                        }, onError = {
                                            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                                        })
                                    },
                                    modifier = Modifier.size(28.dp).background(Color(0xFF38BDF8).copy(0.1f), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                }
                            } else {
                                IconButton(
                                    onClick = { editingMarks[s.student_id] = s.marks ?: "" },
                                    modifier = Modifier.size(28.dp).background(Color(0xFF38BDF8).copy(0.1f), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val examIdInt = selectedExamId?.toIntOrNull() ?: 0
                                    val sIdInt = s.student_id.toIntOrNull() ?: 0
                                    viewModel.deleteStudentAwardMark(examIdInt, sIdInt, onSuccess = {
                                        android.widget.Toast.makeText(context, "Deleted", android.widget.Toast.LENGTH_SHORT).show()
                                        studentsList = studentsList!!.map { if (it.student_id == s.student_id) it.copy(marks = "") else it }
                                        editingMarks.remove(s.student_id)
                                    }, onError = {
                                        android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                                    })
                                },
                                modifier = Modifier.size(28.dp).background(Color(0xFF38BDF8).copy(0.1f), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(
                    onClick = {
                        val view = android.app.AlertDialog.Builder(context)
                        view.setTitle("Delete List")
                        view.setMessage("Are you sure you want to delete the entire award list?")
                        view.setPositiveButton("Yes") { _, _ ->
                            val examIdInt = selectedExamId?.toIntOrNull() ?: 0
                            viewModel.deleteFullAwardList(examIdInt, onSuccess = {
                                android.widget.Toast.makeText(context, "List Cleared", android.widget.Toast.LENGTH_SHORT).show()
                                studentsList = studentsList!!.map { it.copy(marks = "") }
                            }, onError = {
                                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                            })
                        }
                        view.setNegativeButton("No", null)
                        view.show()
                    },
                    modifier = Modifier.weight(1f).height(45.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DELETE LIST", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        val response = com.example.wantuch.domain.model.AwardListStudentsResponse("success", null, totalMarks, studentsList!!)
                        printAwardListToPdf(context, response, selectedExamName, "$selectedClassName - $selectedSectionName")
                    },
                    modifier = Modifier.weight(1f).height(45.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PRINT AWARD LIST", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
    }
}

fun printAwardListToPdf(
    context: Context,
    response: com.example.wantuch.domain.model.AwardListStudentsResponse,
    examTitle: String,
    classSection: String
) {
    val instName = "RANA COLLEGE AND SCHOOL SYSTEM"
    val students = response.students
    val halfway = Math.ceil(students.size / 2.0).toInt()
    val col1 = students.subList(0, halfway)
    val col2 = if (students.size > halfway) students.subList(halfway, students.size) else emptyList()
    
    val renderRows = { list: List<com.example.wantuch.domain.model.AwardListStudent> -> 
        list.joinToString("") { s -> 
            "<tr><td class='c-center' style='width:40px;'>${s.roll_number}</td><td class='c-name'>${s.full_name}</td><td class='c-center' style='width:60px; font-weight:800; color:#1e293b;'>${s.marks?.ifEmpty { "---" } ?: "---"}</td></tr>"
        }
    }

    val tableHtml = """
        <div class="award-table-column">
            <div class="table-rounded-box">
                <table>
                    <thead><tr><th class="c-center" style="width:40px;">Roll</th><th>Student Name</th><th class="c-center" style="width:60px;">Obt.</th></tr></thead>
                    <tbody>${renderRows(col1)}</tbody>
                </table>
            </div>
        </div>
    """ + if (col2.isNotEmpty()) """
        <div class="award-table-column">
            <div class="table-rounded-box">
                <table>
                    <thead><tr><th class="c-center" style="width:40px;">Roll</th><th>Student Name</th><th class="c-center" style="width:60px;">Obt.</th></tr></thead>
                    <tbody>${renderRows(col2)}</tbody>
                </table>
            </div>
        </div>
    """ else ""

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; padding: 20px; color: #333; }
                .print-header { text-align: center; margin-bottom: 20px; }
                .print-header h1 { font-size: 20px; color: #2b559e; margin: 0; font-weight: 800; text-transform: uppercase; }
                .print-header p { font-size: 14px; font-weight: bold; margin-top: 5px; color: #555; }
                .meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 20px; font-size: 13px; font-weight: 700; color: #444; }
                .meta-item { border-bottom: 1px dashed #ccc; padding-bottom: 5px; }
                .award-table-container { display: flex; justify-content: space-between; gap: 20px; }
                .award-table-column { flex: 1; min-width: 0; }
                .table-rounded-box { border: 1px solid #ddd; border-radius: 8px; overflow: hidden; }
                table { width: 100%; border-collapse: collapse; font-size: 12px; }
                th { background-color: #f8f9fa; color: #2b559e; font-weight: 800; text-align: left; padding: 10px; border-bottom: 2px solid #e0e0e0; }
                td { padding: 8px 10px; border-bottom: 1px solid #f0f0f0; font-weight: 600; color: #444; }
                .c-center { text-align: center; }
                .c-name { width: 60%; }
                .print-footer { display: flex; justify-content: space-between; margin-top: 60px; padding: 0 40px; }
                .signature-line { width: 180px; border-top: 1px solid #333; text-align: center; font-size: 12px; font-weight: 700; padding-top: 5px; }
            </style>
        </head>
        <body>
            <div class="print-header">
                <h1>$instName</h1>
                <p>AWARD LIST</p>
            </div>
            
            <div class="meta-grid">
                <div class="meta-item">Exam: $examTitle</div>
                <div class="meta-item">Class: $classSection</div>
                <div class="meta-item">Total Marks: ${response.total_marks ?: "100"}</div>
                <div class="meta-item">Date: ${java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())}</div>
            </div>

            <div class="award-table-container">
                $tableHtml
            </div>
            
            <div class="print-footer">
                <div class="signature-line">Class Teacher</div>
                <div class="signature-line">Principal</div>
            </div>
        </body>
        </html>
    """.trimIndent()
    
    val webView = android.webkit.WebView(context)
    webView.settings.javaScriptEnabled = true
    
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView, url: String) {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val jobName = "Award_List_Print"
            val printAdapter = view.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}
