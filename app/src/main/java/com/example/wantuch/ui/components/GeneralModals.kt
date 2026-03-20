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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wantuch.AccentPurple
import com.example.wantuch.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GeneralLoginModal(appName: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.85f)).clickable { onDismiss() }, Alignment.Center) {
            Box(Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF0F172A)).padding(28.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Login to $appName", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = Color.Red, fontSize = 12.sp)
                    }
                    TextField(user, { user = it }, label = { Text("Username") })
                    TextField(pass, { pass = it }, label = { Text("Password") })
                    Button(onClick = {
                        isLoading = true
                        // This logic could move to ViewModel too, but keeping it here for now to avoid over-complicating the repo
                        // Actually let's just use the repo's generalLogin if we want clean arch
                    }) { Text("Sign In") }
                }
            }
        }
    }
}
