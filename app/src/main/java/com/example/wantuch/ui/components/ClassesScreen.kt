package com.example.wantuch.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.wantuch.domain.model.SchoolClass
import com.example.wantuch.ui.viewmodel.WantuchViewModel

@Composable
fun ClassesScreen(viewModel: WantuchViewModel, onBack: () -> Unit) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val structure by viewModel.schoolStructure.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val userRole = dashboardData?.role?.lowercase() ?: ""
    val isStudent = userRole == "student"
    val studentClassId = dashboardData?.stats?.get("class_id")?.toString()?.toDoubleOrNull()?.toInt() ?: 0
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchSchoolStructure()
    }

    val bgMain = if (isDark) Color(0xFF0F172A) else Color(0xFFF1EFE9)
    val textMain = if (isDark) Color.White else Color.Black

    var showAddClassModal by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = bgMain,
        modifier = Modifier.fillMaxSize()
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
                            if (isStudent) "MY CLASS" else "CLASS MANAGEMENT",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = textMain,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isStudent) "View your class and sections" else "Managing classes and sections",
                            fontSize = 11.sp,
                            color = textMain.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
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

                    Spacer(Modifier.width(10.dp))

                    if (!isStudent) {
                        Button(
                            onClick = { showAddClassModal = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color.White else Color.Black),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = if(isDark) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("NEW CLASS", color = if(isDark) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Class Grid
            if (structure?.classes.isNullOrEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Classes Found", color = textMain.copy(0.5f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val filteredClasses = if (isStudent && studentClassId > 0) {
                        structure?.classes?.filter { it.id == studentClassId } ?: emptyList()
                    } else {
                        structure?.classes ?: emptyList()
                    }

                    items(filteredClasses) { schoolClass ->
                        ClassCard(
                            schoolClass = schoolClass,
                            isDark = isDark,
                            viewModel = viewModel,
                            context = context,
                            isStudent = isStudent
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (showAddClassModal) {
            AddClassModal(
                isDark = isDark,
                onDismiss = { showAddClassModal = false },
                onAdd = { name ->
                    viewModel.classAction(
                        action = "ADD_CLASS",
                        name = name,
                        onSuccess = { 
                            showAddClassModal = false
                        },
                        onError = { err -> 
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show() 
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun ClassCard(
    schoolClass: SchoolClass,
    isDark: Boolean,
    viewModel: WantuchViewModel,
    context: android.content.Context,
    isStudent: Boolean = false
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textMain = if (isDark) Color.White else Color.Black
    val borderLight = if (isDark) Color(0xFF334155) else Color.LightGray.copy(alpha = 0.3f)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var newSectionName by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isDark) 0.dp else 4.dp),
        border = if (isDark) BorderStroke(1.dp, borderLight) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Title & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = schoolClass.name.uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain,
                    letterSpacing = 1.sp
                )
                if (!isStudent) {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Sections Row (using horizontal scrolling for simplicity if many)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (schoolClass.sections.isNullOrEmpty()) {
                    item {
                        Text("No Sections", fontSize = 12.sp, color = textMain.copy(0.4f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                } else {
                    items(schoolClass.sections) { section ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    section.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textMain
                                )
                                if (!isStudent) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable {
                                                viewModel.classAction(
                                                    action = "DELETE_SECTION",
                                                    id = section.id,
                                                    onSuccess = {},
                                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                                )
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Add Section Row - Hidden for students
            if (!isStudent) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = newSectionName,
                        onValueChange = { newSectionName = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = textMain),
                        singleLine = true,
                        cursorBrush = SolidColor(textMain),
                        decorationBox = { innerTextField ->
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (newSectionName.isEmpty()) {
                                    Text("Add Section", fontSize = 12.sp, color = textMain.copy(alpha = 0.5f))
                                } else {
                                    innerTextField()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .background(Color.Transparent, RoundedCornerShape(8.dp))
                            .border(1.dp, borderLight, RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, borderLight),
                        modifier = Modifier
                            .size(46.dp)
                            .clickable {
                                if (newSectionName.isNotBlank()) {
                                    viewModel.classAction(
                                        action = "ADD_SECTION",
                                        classId = schoolClass.id,
                                        name = newSectionName,
                                        onSuccess = { newSectionName = "" },
                                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = textMain, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = cardBg,
            title = { Text("Delete Class?", color = textMain, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${schoolClass.name}? This will fail if students are enrolled.", color = textMain.copy(0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.classAction(
                            action = "DELETE_CLASS",
                            id = schoolClass.id,
                            onSuccess = { showDeleteConfirm = false },
                            onError = { err -> 
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                showDeleteConfirm = false 
                            }
                        )
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
fun AddClassModal(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
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
                Text("ADD NEW CLASS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = textMain, letterSpacing = 1.sp)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("E.g. Grade-X") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = textMain)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { if (name.isNotBlank()) onAdd(name) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color.White else Color.Black)
                    ) {
                        Text("Add", color = if(isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
