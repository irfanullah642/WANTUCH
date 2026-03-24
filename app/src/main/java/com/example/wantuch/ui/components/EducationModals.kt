package com.example.wantuch.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wantuch.AccentPurple
import com.example.wantuch.domain.model.Institution
import com.example.wantuch.ui.viewmodel.WantuchViewModel

@Composable
fun EduLoginModal(
    institutionType: String,
    viewModel: WantuchViewModel,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var step by remember { mutableStateOf("ROLE") }
    var selectedRole by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cnic by remember { mutableStateOf("") }
    var selectedInst by remember { mutableStateOf(0) }
    var showPass by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    val savedData = remember { viewModel.getSavedData() }
    
    LaunchedEffect(Unit) {
        (savedData["remember"] as? Boolean)?.let { remember ->
            if (remember) {
                username = savedData["user"] as? String ?: ""
                password = savedData["pass"] as? String ?: ""
                selectedInst = savedData["last_inst"] as? Int ?: 0
                rememberMe = true
            }
        }
        (savedData["remember_parent"] as? Boolean)?.let { remember ->
            if (remember) {
                cnic = savedData["cnic"] as? String ?: ""
                password = savedData["parent_pass"] as? String ?: ""
                rememberMe = true
            }
        }
    }

    val institutions by viewModel.institutions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    LaunchedEffect(step) {
        if (step == "INST" && selectedRole != "parent") {
            viewModel.fetchInstitutions(institutionType)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xDD0D1B4E), Color(0xEE030714)))), Alignment.Center) {
            Column(Modifier.fillMaxWidth(0.94f).clip(RoundedCornerShape(28.dp)).background(Color(0xFF0F172A)).border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(28.dp)).padding(24.dp)) {
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

                if (errorMsg.isNotEmpty()) {
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

                when (step) {
                    "ROLE" -> {
                        val roles = listOf(
                            Triple("admin", "Admin", Icons.Default.AdminPanelSettings),
                            Triple("staff", "Staff", Icons.Default.People),
                            Triple("student", "Student", Icons.Default.School),
                            Triple("parent", "Parent", Icons.Default.FamilyRestroom),
                            Triple("super_admin", "Super Admin", Icons.Default.AdminPanelSettings)
                        )
                        roles.forEach { (key, label, icon) ->
                            Button(
                                onClick = { selectedRole = key; step = if (key == "parent") "PARENT" else "INST" },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.06f))
                            ) {
                                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(label, color = Color.White)
                            }
                        }
                    }
                    "INST" -> {
                        if (isLoading) {
                            Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF1ABC9C)) }
                        } else {
                            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                                institutions.forEach { inst ->
                                    val instId = inst.id.toString().toDoubleOrNull()?.toInt() ?: 0
                                    val sel = selectedInst == instId
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { selectedInst = instId }
                                        .background(if (sel) Color(0xFF1ABC9C).copy(0.15f) else Color.Transparent).padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (sel) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
                                            tint = if (sel) Color(0xFF1ABC9C) else Color.White.copy(0.3f))
                                        Spacer(Modifier.width(16.dp))
                                        Text(inst.name, color = Color.White, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                                if (institutions.isEmpty()) {
                                    Text("No institutions found.", color = Color.White.copy(0.5f), modifier = Modifier.padding(20.dp))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BackBtn { step = "ROLE" }
                                EduActionBtn("Next", Color(0xFF1ABC9C), Modifier.weight(1f), enabled = selectedInst != 0) { step = "LOGIN" }
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
                                    viewModel.loginInstitution(selectedInst, username, password, selectedRole, rememberMe) { url ->
                                        if (url != null) onSuccess(selectedRole)  // Pass role, not URL
                                    }
                                }
                            }
                        }
                    }
                    "PARENT" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            EduTextField(cnic, { cnic = it }, "CNIC (35202-xxxxxxx-x)", Icons.Default.AssignmentInd)
                            EduTextField(password, { password = it }, "Password", Icons.Default.Lock, isPassword = true, showPass = showPass, onTogglePass = { showPass = !showPass })
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it }, 
                                        colors = CheckboxDefaults.colors(uncheckedColor = Color.White.copy(0.4f), checkedColor = Color(0xFFEC4899)))
                                    Text("Remember Me", color = Color.White.copy(0.6f), fontSize = 14.sp)
                                }
                                TextButton(onClick = { /* Step to forgot password */ }) {
                                    Text("Forgot?", color = Color(0xFFEC4899), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BackBtn { step = "ROLE" }
                                EduActionBtn(if (isLoading) "Authenticating..." else "Parent Login", Color(0xFFEC4899), Modifier.weight(1f), enabled = !isLoading) {
                                    viewModel.loginParent(cnic, password, rememberMe) { url ->
                                        if (url != null) onSuccess("parent")  // Always parent role
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel", color = Color.White.copy(0.4f)) }
            }
        }
    }
}
