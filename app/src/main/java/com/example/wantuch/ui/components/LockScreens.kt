package com.example.wantuch.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wantuch.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainLockScreen(swipeMod: Modifier) {
    val time = remember { SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()) }
    val date = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()) }
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF0D1B4E),Color(0xFF080E28),Color(0xFF030714)),radius=1400f)).then(swipeMod)) {
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width;val h=size.height
            for(i in 0..6){val p=Path();p.moveTo(w*(i*0.15f),0f);p.cubicTo(w*(0.8f-i*0.05f),h*0.25f,w*(0.1f+i*0.1f),h*0.65f,w*(0.4f+i*0.05f),h);drawPath(p,Color(0xFF1A3A8F).copy(0.25f+i*0.02f),style=Stroke(28f))}
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),horizontalAlignment=Alignment.CenterHorizontally){
            Spacer(Modifier.height(40.dp))
            Icon(Icons.Default.Lock,null,tint=Color.White.copy(0.85f),modifier=Modifier.size(26.dp))
            Spacer(Modifier.weight(0.35f))
            Text(time,color=Color.White,fontSize=84.sp,fontWeight=FontWeight.Thin,letterSpacing=(-2).sp)
            Spacer(Modifier.height(6.dp))
            Text(date,color=Color.White.copy(0.72f),fontSize=16.sp)
            Spacer(Modifier.weight(0.65f))
            Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.KeyboardArrowUp,null,tint=Color.White.copy(0.5f),modifier=Modifier.size(32.dp));Text("Swipe up to open",color=Color.White.copy(0.38f),fontSize=12.sp)}
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal=48.dp,vertical=12.dp),Arrangement.SpaceBetween){QuickBtn(Icons.Default.FlashOn);QuickBtn(Icons.Default.CameraAlt)}
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            repeat(3){i->Box(Modifier.size(if(i==1)8.dp else 6.dp).clip(CircleShape).background(if(i==1)Color.White else Color.White.copy(0.35f)))}
        }
    }
}

@Composable fun QuickBtn(icon:ImageVector){Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(0.2f)),Alignment.Center){Icon(icon,null,tint=Color.White,modifier=Modifier.size(22.dp))}}

@Composable
fun OrangeLockScreen(swipeMod:Modifier){
    val hr=remember{SimpleDateFormat("hh",Locale.getDefault()).format(Date())};val min=remember{SimpleDateFormat("mm",Locale.getDefault()).format(Date())};val day=remember{SimpleDateFormat("EEEE\nMMM d",Locale.getDefault()).format(Date())}
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFFF8C00),Color(0xFFFFD000)),Offset.Zero,Offset(600f,1400f))).then(swipeMod)){
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=28.dp)){
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){
                Column{Text(hr,color=Color.White,fontSize=108.sp,fontWeight=FontWeight.Black,letterSpacing=(-3).sp,lineHeight=104.sp);Text(min,color=Color.White.copy(0.88f),fontSize=108.sp,fontWeight=FontWeight.Black,letterSpacing=(-3).sp,lineHeight=104.sp)}
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).padding(top=18.dp),horizontalAlignment=Alignment.CenterHorizontally){Thermometer(0.65f,Color.White,Color.White.copy(0.3f));Spacer(Modifier.height(10.dp));WeatherCard("39°C",day,Color.Black.copy(0.25f),Color.White)}
            }
            Spacer(Modifier.weight(1f));LockDock(Color.White,Color.Black.copy(0.2f));Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun DarkLockScreen(swipeMod:Modifier){
    val hr=remember{SimpleDateFormat("hh",Locale.getDefault()).format(Date())};val min=remember{SimpleDateFormat("mm",Locale.getDefault()).format(Date())};val day=remember{SimpleDateFormat("EEEE\nMMM d",Locale.getDefault()).format(Date())}
    Box(Modifier.fillMaxSize().background(Color(0xFF111827)).then(swipeMod)){
        Box(Modifier.fillMaxWidth().height(3.dp).background(OrangeAccent).align(Alignment.TopCenter))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=28.dp)){
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){
                Column{Text(hr,color=Color.White,fontSize=108.sp,fontWeight=FontWeight.Black,letterSpacing=(-3).sp,lineHeight=104.sp);Text(min,color=OrangeAccent,fontSize=108.sp,fontWeight=FontWeight.Black,letterSpacing=(-3).sp,lineHeight=104.sp)}
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).padding(top=18.dp),horizontalAlignment=Alignment.CenterHorizontally){Thermometer(0.5f,OrangeAccent,Color.White.copy(0.12f));Spacer(Modifier.height(10.dp));WeatherCard("30°C",day,Color.White.copy(0.05f),OrangeAccent,border=true)}
            }
            Spacer(Modifier.weight(1f));LockDock(Color.White.copy(0.75f),Color.White.copy(0.07f),firstTint=OrangeAccent,border=true);Spacer(Modifier.height(16.dp))
        }
    }
}
@Composable fun Thermometer(f:Float,fc:Color,tc:Color){Box(Modifier.width(6.dp).height(155.dp).clip(RoundedCornerShape(3.dp)).background(tc)){Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(f).background(fc))}}
@Composable fun WeatherCard(temp:String,day:String,bg:Color,tc:Color,border:Boolean=false){val m=Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).then(if(border)Modifier.border(1.dp,Color.White.copy(0.1f),RoundedCornerShape(10.dp))else Modifier).padding(10.dp);Box(m){Column{Text(temp,color=tc,fontSize=18.sp,fontWeight=FontWeight.Bold);Text("Clear",color=tc.copy(0.7f),fontSize=11.sp);Text(day,color=tc.copy(0.6f),fontSize=10.sp)}}}
@Composable fun LockDock(it:Color,bg:Color,firstTint:Color=it,border:Boolean=false){val m=Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).then(if(border)Modifier.border(1.dp,Color.White.copy(0.1f),RoundedCornerShape(20.dp))else Modifier).padding(horizontal=20.dp,vertical=12.dp);Row(m,Arrangement.SpaceEvenly){listOf(Icons.Default.Phone,Icons.Default.Message,Icons.Default.CameraAlt,Icons.Default.Language).forEachIndexed{i,icon->Icon(icon,null,tint=if(i==0)firstTint else it,modifier=Modifier.size(26.dp))}}}
