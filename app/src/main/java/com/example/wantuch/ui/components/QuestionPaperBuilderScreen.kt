package com.example.wantuch.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max

// Data models for the paper builder
data class Question(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val marks: String = ""
)

data class Section(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val allocated_marks: String = "",
    val is_manual: Boolean = false,
    val questions: List<Question> = listOf(Question())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionPaperBuilderScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit,
    onSave: (title: String, subject: String, totalMarks: String, sections: List<Section>) -> Unit = { _, _, _, _ -> }
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val scope = rememberCoroutineScope()
    
    // Paper metadata
    var paperTitle by remember { mutableStateOf("") }
    var paperSubject by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var paperTotalMarks by remember { mutableStateOf("100") }
    
    // Sections
    var sections by remember { mutableStateOf(listOf<Section>()) }
    
    // Dialog states
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var selectedSectionIndex by remember { mutableStateOf(-1) }
    var newQuestionText by remember { mutableStateOf("") }
    var newQuestionMarks by remember { mutableStateOf("") }
    
    // Auto-Distributor Logic
    fun autoDistribute(currentSections: List<Section>, total: String): List<Section> {
        val tMarks = total.toIntOrNull() ?: 0
        if (tMarks <= 0 || currentSections.isEmpty()) return currentSections
        
        val manualSections = currentSections.filter { it.is_manual }
        val manualSum = manualSections.sumOf { it.allocated_marks.toIntOrNull() ?: 0 }
        
        val autoSectionsCount = currentSections.size - manualSections.size
        if (autoSectionsCount == 0) return currentSections // all manual
        
        val remaining = max(0, tMarks - manualSum)
        val perSection = remaining / autoSectionsCount
        
        return currentSections.map { sec ->
            if (sec.is_manual) sec else sec.copy(allocated_marks = perSection.toString())
        }
    }

    // Effect to distribute when total changes or new section added
    LaunchedEffect(paperTotalMarks, sections.size) {
        sections = autoDistribute(sections, paperTotalMarks)
    }
    
    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {
            
            // Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF3B82F6))))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Smart Paper Builder", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("CREATE CUSTOM EXAM PAPER", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    // Save button
                    Button(
                        onClick = {
                            if (paperTitle.isNotBlank() && paperSubject.isNotBlank()) {
                                onSave(paperTitle, paperSubject, paperTotalMarks, sections)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = paperTitle.isNotBlank() && paperSubject.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp)
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Smart Templates Row
                Text("Smart Templates", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF1E293B))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TemplateButton("MCQs", Icons.Default.FormatListBulleted, isDark) {
                        sections = sections.toMutableList().apply { add(Section(name = "Multiple Choice Questions")) }
                    }
                    TemplateButton("Short Q/A", Icons.Default.ShortText, isDark) {
                        sections = sections.toMutableList().apply { add(Section(name = "Short Questions")) }
                    }
                    TemplateButton("Match Columns", Icons.Default.CompareArrows, isDark) {
                        sections = sections.toMutableList().apply { add(Section(name = "Match the Following Columns")) }
                    }
                    TemplateButton("Custom", Icons.Default.Add, isDark) {
                        sections = sections.toMutableList().apply { add(Section(name = "Custom Section")) }
                    }
                }

                // Paper Metadata Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Paper Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF1E293B))
                        Spacer(Modifier.height(16.dp))
                        
                        BuilderTextField(
                            value = paperTitle,
                            onValueChange = { paperTitle = it },
                            placeholder = "Paper Title e.g. Annual Examination 2026",
                            label = "Paper Title *",
                            isDark = isDark
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        BuilderTextField(
                            value = paperSubject,
                            onValueChange = { paperSubject = it },
                            placeholder = "e.g. Mathematics, Physics, English",
                            label = "Subject *",
                            isDark = isDark
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BuilderTextField(
                                value = duration,
                                onValueChange = { duration = it },
                                placeholder = "Minutes",
                                label = "Duration (mins)",
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            BuilderTextField(
                                value = paperTotalMarks,
                                onValueChange = { paperTotalMarks = it },
                                placeholder = "Auto-distributes across sections",
                                label = "Total Marks",
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Sections and Questions
                sections.forEachIndexed { sectionIndex, section ->
                    SectionCard(
                        section = section,
                        sectionIndex = sectionIndex,
                        isDark = isDark,
                        onSectionNameChange = { newName ->
                            val updated = sections.toMutableList()
                            updated[sectionIndex] = updated[sectionIndex].copy(name = newName)
                            sections = updated
                        },
                        onSectionMarksChange = { newMarks ->
                            val updated = sections.toMutableList()
                            updated[sectionIndex] = updated[sectionIndex].copy(allocated_marks = newMarks, is_manual = true)
                            // Recalculate auto marks
                            sections = autoDistribute(updated, paperTotalMarks)
                        },
                        onSectionReset = {
                            val updated = sections.toMutableList()
                            updated[sectionIndex] = updated[sectionIndex].copy(is_manual = false)
                            // Recalculate auto marks
                            sections = autoDistribute(updated, paperTotalMarks)
                        },
                        onAddQuestion = {
                            selectedSectionIndex = sectionIndex
                            newQuestionText = ""
                            newQuestionMarks = ""
                            showAddQuestionDialog = true
                        },
                        onDeleteSection = {
                            val updated = sections.toMutableList()
                            updated.removeAt(sectionIndex)
                            sections = autoDistribute(updated, paperTotalMarks)
                        },
                        onDeleteQuestion = { questionIndex ->
                            val updated = sections.toMutableList()
                            val qList = updated[sectionIndex].questions.toMutableList()
                            qList.removeAt(questionIndex)
                            updated[sectionIndex] = updated[sectionIndex].copy(questions = qList)
                            sections = updated
                        },
                        onQuestionTextChange = { questionIndex, newText ->
                            val updated = sections.toMutableList()
                            val qList = updated[sectionIndex].questions.toMutableList()
                            qList[questionIndex] = qList[questionIndex].copy(text = newText)
                            updated[sectionIndex] = updated[sectionIndex].copy(questions = qList)
                            sections = updated
                        },
                        onQuestionMarksChange = { questionIndex, newMarks ->
                            val updated = sections.toMutableList()
                            val qList = updated[sectionIndex].questions.toMutableList()
                            qList[questionIndex] = qList[questionIndex].copy(marks = newMarks)
                            updated[sectionIndex] = updated[sectionIndex].copy(questions = qList)
                            sections = updated
                        }
                    )
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
    
    // Add Question Dialog
    if (showAddQuestionDialog && selectedSectionIndex >= 0) {
        Dialog(
            onDismissRequest = { showAddQuestionDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Add Question to ${sections[selectedSectionIndex].name}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF1E293B))
                    Spacer(Modifier.height(16.dp))
                    
                    BuilderTextField(
                        value = newQuestionText,
                        onValueChange = { newQuestionText = it },
                        placeholder = "Enter question text...",
                        label = "Question",
                        isDark = isDark
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    BuilderTextField(
                        value = newQuestionMarks,
                        onValueChange = { newQuestionMarks = it },
                        placeholder = "e.g. 5, 10, etc.",
                        label = "Marks",
                        isDark = isDark
                    )
                    Spacer(Modifier.height(20.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(
                            onClick = { showAddQuestionDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newQuestionText.isNotBlank()) {
                                    val updated = sections.toMutableList()
                                    val newQList = updated[selectedSectionIndex].questions.toMutableList()
                                    newQList.add(Question(text = newQuestionText, marks = newQuestionMarks))
                                    updated[selectedSectionIndex] = updated[selectedSectionIndex].copy(questions = newQList)
                                    sections = updated
                                    showAddQuestionDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = newQuestionText.isNotBlank()
                        ) {
                            Text("Add Question")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val contentColor = Color(0xFF6366F1)
    
    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionCard(
    section: Section,
    sectionIndex: Int,
    isDark: Boolean,
    onSectionNameChange: (String) -> Unit,
    onSectionMarksChange: (String) -> Unit,
    onSectionReset: () -> Unit,
    onAddQuestion: () -> Unit,
    onDeleteSection: () -> Unit,
    onDeleteQuestion: (Int) -> Unit,
    onQuestionTextChange: (Int, String) -> Unit,
    onQuestionMarksChange: (Int, String) -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(section.name) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
    ) {
        Column(Modifier.padding(16.dp)) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditingName) {
                    BasicTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        onSectionNameChange(tempName)
                        isEditingName = false
                    }) {
                        Icon(Icons.Default.Check, "Save", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                    }
                } else {
                    Text(
                        section.name.ifEmpty { "Section ${sectionIndex + 1}" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1E293B),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isEditingName = true }) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onDeleteSection) {
                    Icon(Icons.Default.Delete, "Delete Section", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
            
            // Marks Editor
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (section.is_manual) "Manual Marks :" else "Auto Marks :",
                    fontSize = 12.sp,
                    color = if (section.is_manual) Color(0xFFF59E0B) else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = section.allocated_marks,
                    onValueChange = { onSectionMarksChange(it) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    ),
                    modifier = Modifier.width(60.dp).background(if(isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.1f), RoundedCornerShape(4.dp)).padding(4.dp)
                )
                if (section.is_manual) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Reset",
                        fontSize = 10.sp,
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSectionReset() }.padding(4.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Questions List
            if (section.questions.isEmpty()) {
                Text(
                    "No questions added yet. Tap + to add questions.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Text(
                    "Questions & Marks:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                section.questions.forEachIndexed { qIndex, question ->
                    QuestionItem(
                        question = question,
                        isDark = isDark,
                        onTextChange = { onQuestionTextChange(qIndex, it) },
                        onMarksChange = { onQuestionMarksChange(qIndex, it) },
                        onDelete = { onDeleteQuestion(qIndex) }
                    )
                    if (qIndex < section.questions.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha=0.2f))
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Add Question Button
            Button(
                onClick = onAddQuestion,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6).copy(alpha=0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Question", color = Color(0xFF3B82F6), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun QuestionItem(
    question: Question,
    isDark: Boolean,
    onTextChange: (String) -> Unit,
    onMarksChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = question.text,
                onValueChange = onTextChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                ),
                decorationBox = { inner ->
                    if (question.text.isEmpty()) {
                        Text("Enter question...", color = Color.Gray, fontSize = 14.sp)
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            BasicTextField(
                value = question.marks,
                onValueChange = onMarksChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFF3B82F6)
                ),
                decorationBox = { inner ->
                    if (question.marks.isEmpty()) {
                        Text("Marks (e.g. 5, 2x5=10)", color = Color.Gray.copy(alpha=0.6f), fontSize = 12.sp)
                    }
                    inner()
                }
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun BuilderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String,
    isDark: Boolean,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color.White.copy(alpha=0.6f) else Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = if (isDark) Color.White else Color(0xFF1E293B)
            ),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color.White.copy(alpha=0.05f) else Color.Black.copy(alpha=0.04f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (value.isEmpty() && !readOnly) {
                        Text(placeholder, color = Color.Gray.copy(alpha=0.5f), fontSize = 14.sp)
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
