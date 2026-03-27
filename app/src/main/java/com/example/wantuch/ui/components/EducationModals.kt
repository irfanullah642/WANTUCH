package com.example.wantuch.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wantuch.domain.model.Institution
import com.example.wantuch.ui.viewmodel.WantuchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduLoginModal(
    institutionType: String,
    viewModel: WantuchViewModel,
    onDismiss: () -> Unit,
    onSuccess: (String, Int) -> Unit
) {
    var step by remember { mutableStateOf("ROLE") }
    var selectedRole by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cnic by remember { mutableStateOf("") }
    var selectedInstId by remember { mutableStateOf(0) }
    var selectedInstName by remember { mutableStateOf("Select $institutionType") }
    var showPass by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val savedData = remember { viewModel.getSavedData() }
    val institutions by viewModel.institutions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    LaunchedEffect(Unit) {
        selectedInstId = savedData["last_inst"] as? Int ?: 0
        selectedRole = savedData["role"] as? String ?: ""

        (savedData["remember"] as? Boolean)?.let { remember ->
            if (remember) {
                username = savedData["user"] as? String ?: ""
                password = savedData["pass"] as? String ?: ""
                rememberMe = true
            }
        }
        (savedData["remember_parent"] as? Boolean)?.let { remember ->
            if (remember) {
                cnic = savedData["cnic"] as? String ?: ""
                password = savedData["parent_pass"] as? String ?: ""
                selectedRole = "parent"
                rememberMe = true
            }
        }
    }

    LaunchedEffect(institutions, selectedInstId) {
        if (selectedInstId != 0 && institutions.isNotEmpty()) {
            institutions.find { (it.id?.toString()?.toDoubleOrNull()?.toInt() ?: 0) == selectedInstId }?.let {
                selectedInstName = it.name ?: ""
            }
        }
    }

    LaunchedEffect(step) {
        if (step == "INST" && selectedRole != "parent") {
            viewModel.fetchInstitutions(institutionType)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xDD0D1B4E), Color(0xEE030714)))), Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth(0.94f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1ABC9C)), Alignment.Center) {
                        Icon(Icons.Default.School, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Select $institutionType", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(institutionType, color = Color(0xFF1ABC9C), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))

                if (errorMsg.isNotEmpty() && institutions.isEmpty()) {
                    Column(Modifier.fillMaxWidth().background(Color(0xFFE74C3C).copy(0.15f)).border(1.dp, Color(0xFFE74C3C).copy(0.4f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text(errorMsg, color = Color(0xFFE74C3C), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { 
                                if (step == "INST") viewModel.fetchInstitutions(institutionType)
                                else viewModel.clearError() 
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Retry Connection", color = Color(0xFFE74C3C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Box(Modifier.weight(1f, fill = false)) {
                    when (step) {
                        "ROLE" -> {
                            val roles = listOf(
                                Triple("admin", "Admin", Icons.Default.AdminPanelSettings),
                                Triple("staff", "Staff", Icons.Default.People),
                                Triple("student", "Student", Icons.Default.School),
                                Triple("parent", "Parent", Icons.Default.FamilyRestroom),
                                Triple("super_admin", "Super Admin", Icons.Default.Security)
                            )
                            Column {
                                roles.forEach { (key, label, icon) ->
                                    Button(
                                        onClick = { 
                                            selectedRole = key
                                            selectedInstId = 0
                                            selectedInstName = "Select $institutionType"
                                            dropdownExpanded = false
                                            step = if (key == "parent") "PARENT" else "INST" 
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedRole == key) Color(0xFF1ABC9C).copy(0.2f) else Color.White.copy(0.06f))
                                    ) {
                                        Icon(icon, null, tint = if (selectedRole == key) Color(0xFF1ABC9C) else Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(label, color = Color.White)
                                    }
                                }
                            }
                        }
                        "INST" -> {
                            Column {
                                if (isLoading && institutions.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF1ABC9C)) }
                                } else {
                                    Text("Institution Name", color = Color.White.copy(0.7f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = selectedInstName,
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true },
                                            enabled = false,
                                            trailingIcon = { 
                                                IconButton(onClick = { dropdownExpanded = true }) {
                                                    Icon(
                                                        if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                        null,
                                                        tint = Color.White
                                                    )
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = Color.White,
                                                disabledBorderColor = Color.White.copy(0.2f),
                                                disabledContainerColor = Color.Transparent,
                                                disabledLabelColor = Color.White.copy(0.7f),
                                                disabledTrailingIconColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        DropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.8f).background(Color(0xFF1E293B)),
                                            offset = DpOffset(0.dp, 8.dp)
                                        ) {
                                            institutions.forEach { inst ->
                                                DropdownMenuItem(
                                                    text = { Text(inst.name ?: "Unknown", color = Color.White) },
                                                    onClick = {
                                                        selectedInstId = inst.id?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                                                        selectedInstName = inst.name ?: ""
                                                        dropdownExpanded = false
                                                    }
                                                )
                                            }
                                            if (institutions.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("No institutions found", color = Color.White.copy(0.7f)) },
                                                    onClick = { dropdownExpanded = false }
                                                )
                                            }
                                        }
                                    }

                                    if (institutions.isEmpty() && !isLoading) {
                                        Text("No institutions found. (Check Connection)", color = Color(0xFFE74C3C), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                                    }

                                    Spacer(Modifier.height(24.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        BackBtn { step = "ROLE" }
                                        EduActionBtn("Next", Color(0xFF1ABC9C), Modifier.weight(1f), enabled = selectedInstId != 0) { step = "LOGIN" }
                                    }
                                }
                            }
                        }
                        "LOGIN" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                EduTextField(username, { username = it }, "Username", Icons.Default.Person)
                                EduTextField(password, { password = it }, "Password", Icons.Default.Lock, isPassword = true, showPass = showPass, onTogglePass = { showPass = !showPass })
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it }, 
                                        colors = CheckboxDefaults.colors(uncheckedColor = Color.White.copy(0.4f), checkedColor = Color(0xFF1ABC9C)))
                                    Text("Remember Me", color = Color.White.copy(0.6f), fontSize = 14.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    BackBtn { step = "INST" }
                                    EduActionBtn(if (isLoading) "Checking..." else "Login", Color(0xFF1ABC9C), Modifier.weight(1f), enabled = !isLoading) {
                                        viewModel.loginInstitution(selectedInstId, username, password, selectedRole, rememberMe) { url ->
                                            if (url != null) onSuccess(selectedRole, selectedInstId)
                                        }
                                    }
                                }
                            }
                        }
                        "PARENT" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                EduTextField(cnic, { cnic = it }, "CNIC (35202-xxxxxxx-x)", Icons.Default.AssignmentInd)
                                EduTextField(password, { password = it }, "Password", Icons.Default.Lock, isPassword = true, showPass = showPass, onTogglePass = { showPass = !showPass })
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it }, 
                                        colors = CheckboxDefaults.colors(uncheckedColor = Color.White.copy(0.4f), checkedColor = Color(0xFFEC4899)))
                                    Text("Remember Me", color = Color.White.copy(0.6f), fontSize = 14.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    BackBtn { step = "ROLE" }
                                    EduActionBtn(if (isLoading) "Authenticating..." else "Parent Login", Color(0xFFEC4899), Modifier.weight(1f), enabled = !isLoading) {
                                        viewModel.loginParent(cnic, password, rememberMe) { url ->
                                            if (url != null) onSuccess("parent", 0)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel", color = Color.White.copy(0.4f))
                }
            }
        }
    }
}
