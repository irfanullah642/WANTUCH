package com.example.wantuch.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wantuch.domain.model.AppItem
import com.example.wantuch.Glass20
import com.example.wantuch.UnreadRed

@Composable
fun HomeScreen(items: List<AppItem>, onLock: () -> Unit, onAppClick: (AppItem) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        // Decorative background
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0x55CC2200), Color.Transparent), center = Offset(size.width * 0.75f, size.height * 0.28f), radius = 420f))
        }
        
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Search Bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp).clip(CircleShape).background(Glass20).border(1.dp, Color.White.copy(0.18f), CircleShape).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("G", color = Color(0xFF4285F4), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text("Search", color = Color.White.copy(0.4f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Mic, null, tint = Color(0xFF4285F4), modifier = Modifier.size(20.dp))
            }
            // Grid
            LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f).padding(horizontal = 10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                items(items) { app ->
                    HomeIcon(app) { onAppClick(app) }
                }
            }
            // Dock
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 14.dp).height(62.dp).clip(RoundedCornerShape(31.dp)).background(Glass20).border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(31.dp)), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                DockBtn(Icons.Default.Phone, Color(0xFF2ECC71)) {}
                DockBtn(Icons.Default.Message, Color(0xFF3498DB)) {}
                DockBtn(Icons.Default.CameraAlt, Color(0xFFE74C3C)) {}
                DockBtn(Icons.Default.Lock, Color(0xFFE74C3C), onClick = onLock)
            }
        }
    }
}

@Composable fun DockBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Box(Modifier.size(46.dp).clip(CircleShape).background(color.copy(0.35f)).clickable { onClick() }, Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun HomeIcon(app: AppItem, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(vertical = 8.dp)) {
        Box {
            Box(Modifier.size(54.dp).clip(CircleShape).background(app.tint), Alignment.Center) {
                Icon(app.icon, null, tint = Color.White, modifier = Modifier.size(27.dp))
            }
            if (app.unread > 0) {
                Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(UnreadRed), Alignment.Center) {
                    Text(app.unread.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(app.label, color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
    }
}
