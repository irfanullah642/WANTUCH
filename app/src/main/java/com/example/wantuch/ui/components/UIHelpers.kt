package com.example.wantuch.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EduTextField(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    showPass: Boolean = false,
    onTogglePass: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color.White.copy(0.4f), fontSize = 14.sp) },
        leadingIcon = { Icon(icon, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) {
            { IconButton(onClick = onTogglePass) { Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color.White.copy(0.4f)) } }
        } else null,
        visualTransformation = if (isPassword && !showPass) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF1ABC9C),
            unfocusedBorderColor = Color.White.copy(0.12f),
            cursorColor = Color(0xFF1ABC9C)
        ),
        singleLine = true
    )
}

@Composable
fun EduActionBtn(label: String, color: Color, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = color.copy(0.4f))
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun BackBtn(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(52.dp).width(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
