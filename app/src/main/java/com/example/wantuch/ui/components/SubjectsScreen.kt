package com.example.wantuch.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.wantuch.domain.model.SchoolSubject
import com.example.wantuch.ui.viewmodel.WantuchViewModel

@Composable
fun SubjectsScreen(viewModel: WantuchViewModel, onBack: () -> Unit) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val subjectsResponse by viewModel.subjects.collectAsState()
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.updateRole()
        viewModel.fetchSubjects()
    }

    val bgMain = if (isDark) Color(0xFF0F172A) else Color(0xFFF1EFE9)
    val textMain = if (isDark) Color.White else Color.Black
    val subjects = subjectsResponse?.subjects ?: emptyList()

    var showAddModal by remember { mutableStateOf(false) }
    var editSubject by remember { mutableStateOf<SchoolSubject?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = bgMain,
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (userRole != "Student") {
                FloatingActionButton(
                    onClick = { showAddModal = true },
                    containerColor = Color(0xFF00F3FF),
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subject")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.statusBarsPadding())
            Spacer(Modifier.height(16.dp))

            // Header Section
            Surface(
                color = if (isDark) Color(0xFF1E293B) else Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isDark) Color(0xFF334155) else Color.White,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textMain)
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SUBJECT CONTROL",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = textMain,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        color = if (isDark) Color(0xFF334155) else Color.White,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(Icons.Default.Palette, contentDescription = "Theme", tint = textMain, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (userRole != "Student") {
                        Spacer(Modifier.width(10.dp))

                        Button(
                            onClick = { showDeleteAllConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color.White else Color.Black),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("DELETE ALL", color = if (isDark) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Subject Grid
            if (subjects.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Subjects Found", color = textMain.copy(0.5f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(subjects) { subject ->
                        SubjectCard(
                            subject = subject,
                            isDark = isDark,
                            userRole = userRole,
                            onEdit = { editSubject = subject },
                            onDelete = {
                                viewModel.subjectAction(
                                    action = "DELETE_SUBJECT",
                                    id = subject.id,
                                    onSuccess = { Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show() },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                )
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (showAddModal || editSubject != null) {
            SubjectInitModal(
                isDark = isDark,
                initialSubject = editSubject,
                onDismiss = {
                    showAddModal = false
                    editSubject = null
                },
                onSave = { name, type ->
                    val isEdit = editSubject != null
                    viewModel.subjectAction(
                        action = if (isEdit) "EDIT_SUBJECT" else "ADD_SUBJECT",
                        id = editSubject?.id,
                        name = name,
                        type = type,
                        onSuccess = {
                            showAddModal = false
                            editSubject = null
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }

        if (showDeleteAllConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirm = false },
                containerColor = if(isDark) Color(0xFF1E293B) else Color.White,
                title = { Text("Delete All Subjects?", color = textMain, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure? This action cannot be undone.", color = textMain.copy(0.8f)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.subjectAction(
                                action = "DELETE_ALL_SUBJECTS",
                                onSuccess = { showDeleteAllConfirm = false },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    showDeleteAllConfirm = false
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Delete All", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteAllConfirm = false }) {
                        Text("Cancel", color = textMain)
                    }
                }
            )
        }
    }
}

@Composable
fun SubjectCard(
    subject: SchoolSubject,
    isDark: Boolean,
    userRole: String = "Student",
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textMain = if (isDark) Color.White else Color.Black
    val borderLight = if (isDark) Color(0xFF334155) else Color.LightGray.copy(alpha = 0.3f)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isDark) 0.dp else 4.dp),
        border = if (isDark) BorderStroke(1.dp, borderLight) else null,
        modifier = Modifier.fillMaxWidth().wrapContentHeight().defaultMinSize(minHeight = 110.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.Top) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name.uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = textMain,
                        letterSpacing = 1.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subject.type.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (subject.type.equals("Optional", true)) Color(0xFFBC13FE) else Color(0xFFF59E0B)
                    )
                }
            }

            if (userRole != "Student") {
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = textMain.copy(0.5f), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = textMain.copy(0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = cardBg,
            title = { Text("Delete Subject?", color = textMain, fontWeight = FontWeight.Bold) },
            text = { Text("Delete ${subject.name} from the school database?", color = textMain.copy(0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = textMain)
                }
            }
        )
    }
}

@Composable
fun SubjectInitModal(
    isDark: Boolean,
    initialSubject: SchoolSubject?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialSubject?.name ?: "") }
    var type by remember { mutableStateOf(initialSubject?.type ?: "Compulsory") }

    val bgDialog = if (isDark) Color(0xFF1E293B) else Color.White
    val textMain = if (isDark) Color.White else Color.Black

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bgDialog)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (initialSubject == null) "INITIALIZE SUBJECT" else "UPDATE SUBJECT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(20.dp))

                var isBatch by remember { mutableStateOf(false) }

                if (initialSubject == null) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isBatch) Color(0xFF00F3FF).copy(0.1f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (!isBatch) Color(0xFF00F3FF) else textMain.copy(0.2f)),
                            modifier = Modifier.weight(1f).clickable { isBatch = false }
                        ) {
                            Text("🔒 SINGLE", textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp), fontSize = 12.sp, color = textMain, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isBatch) Color(0xFF00F3FF).copy(0.1f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isBatch) Color(0xFF00F3FF) else textMain.copy(0.2f)),
                            modifier = Modifier.weight(1f).clickable { isBatch = true }
                        ) {
                            Text("🔒 BATCH (CSV)", textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp), fontSize = 12.sp, color = textMain, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!isBatch) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("IDENTIFIER / NAME", fontSize = 10.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("CSV STRING (COMMA SEPARATED)", fontSize = 10.sp) },
                        placeholder = { Text("URDU, ENGLISH, MATHS...", fontSize = 12.sp, color = textMain.copy(0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("OPERATIONAL TYPE", fontSize = 10.sp, color = textMain.copy(0.7f), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = type == "Compulsory",
                                onClick = { type = "Compulsory" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00F3FF))
                            )
                            Text("Compulsory", color = textMain, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = type == "Optional",
                                onClick = { type = "Optional" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00F3FF))
                            )
                            Text("Optional", color = textMain, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = textMain)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { if (name.isNotBlank()) onSave(name, type) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F3FF))
                    ) {
                        Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
