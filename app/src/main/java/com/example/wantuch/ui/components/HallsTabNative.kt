package com.example.wantuch.ui.components

import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.wantuch.domain.model.HallStaff
import com.example.wantuch.domain.model.HallSubject
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

data class RoomInfo(val id: Int, var name: String, var capacity: String)

@Composable
fun HallsTabNative(viewModel: WantuchViewModel, isDark: Boolean, openWeb: (String) -> Unit) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableStateOf("Hall Arrangement") }
    
    // Core states
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var shift by remember { mutableStateOf("Morning") }
    var selectedSubjectId by remember { mutableStateOf("") }
    var selectedSubjectName by remember { mutableStateOf("Select Scheduled Subject") }
    
    // Lists
    var staffList by remember { mutableStateOf<List<HallStaff>>(emptyList()) }
    var subjectsList by remember { mutableStateOf<List<HallSubject>>(emptyList()) }
    var selectedStaffIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingStaff by remember { mutableStateOf(true) }
    
    // Rooms
    var roomsList by remember { mutableStateOf(mutableListOf(RoomInfo(1, "", "30"))) }
    var nextRoomId by remember { mutableStateOf(2) }
    
    // Preview
    var generatedHtml by remember { mutableStateOf<String?>(null) }
    var isArranging by remember { mutableStateOf(false) }

    // Initial Loading
    LaunchedEffect(Unit) {
        viewModel.getHallStaffList(
            onSuccess = { 
                staffList = it
                isLoadingStaff = false
            },
            onError = { 
                isLoadingStaff = false
                android.widget.Toast.makeText(context, "Error fetching staff", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // Subject Reloading
    LaunchedEffect(date, shift) {
        viewModel.getHallSubjects(
            date = date, shift = shift,
            onSuccess = { subjects ->
                subjectsList = subjects
                if (!subjects.any { it.id == selectedSubjectId }) {
                    selectedSubjectId = ""
                    selectedSubjectName = "Select Scheduled Subject"
                }
            },
            onError = { }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0F172A), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Logistics & Inventory", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Manage hall arrangements, staff invigilation, and student attendance sheets.", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp).offset(x = 10.dp, y = 10.dp))
        }

        // Subtabs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { selectedSubTab = "Hall Arrangement" },
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedSubTab == "Hall Arrangement") Color(0xFF38BDF8) else Color.Transparent),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp), tint = if(selectedSubTab == "Hall Arrangement") Color.White else Color(0xFF94A3B8))
                Spacer(Modifier.width(8.dp))
                Text("Hall Arrangement", color = if(selectedSubTab == "Hall Arrangement") Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Button(
                onClick = { selectedSubTab = "Attendance Sheet" },
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedSubTab == "Attendance Sheet") Color(0xFF38BDF8) else Color.Transparent),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(16.dp), tint = if(selectedSubTab == "Attendance Sheet") Color.White else Color(0xFF94A3B8))
                Spacer(Modifier.width(8.dp))
                Text("Attendance Sheet", color = if(selectedSubTab == "Attendance Sheet") Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        
        Spacer(Modifier.height(8.dp))

        if (selectedSubTab == "Hall Arrangement") {
            // Filters View
            val subjectOptions = listOf("Select Scheduled Subject") + subjectsList.map { it.name }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Date", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray) }
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Shift (Default)", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    DropdownSelector(value = shift, options = listOf("Morning", "Evening"), modifier = Modifier.fillMaxWidth(), isDark = true) { shift = it }
                }
                Column(Modifier.weight(1f)) {
                    Text("Subject", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    DropdownSelector(value = selectedSubjectName, options = subjectOptions, modifier = Modifier.fillMaxWidth(), isDark = true) { choice ->
                        selectedSubjectName = choice
                        selectedSubjectId = subjectsList.find { it.name == choice }?.id ?: ""
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))

            // Main Core UI
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Configure Rooms Box
                Column(
                    modifier = Modifier.weight(1.5f).background(Color(0xFF1E293B), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)).padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).background(Color(0xFFF1C40F).copy(0.1f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text("1", color = Color(0xFFF1C40F), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Configure Rooms", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(20.dp))
                    
                    Row(Modifier.fillMaxWidth().background(Color(0xFF0F172A).copy(0.5f)).padding(12.dp)) {
                        Text("ROOM NAME", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(2f))
                        Text("CHAIRS (CAP)", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(30.dp))
                    }
                    
                    roomsList.forEach { room ->
                        Row(Modifier.fillMaxWidth().border(BorderStroke(1.dp, Color(0xFF334155))).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = room.name,
                                onValueChange = { n -> 
                                    room.name = n
                                    // Trigger recomposition trick
                                    roomsList = roomsList.toList().toMutableList()
                                },
                                placeholder = { Text("e.g. Hall B", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.weight(2f).height(48.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = room.capacity,
                                onValueChange = { n -> 
                                    room.capacity = n
                                    roomsList = roomsList.toList().toMutableList()
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { roomsList = roomsList.filter { it.id != room.id }.toMutableList() }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            roomsList.add(RoomInfo(nextRoomId++, "", "30"))
                            roomsList = roomsList.toList().toMutableList()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF0EA5E9)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0EA5E9))
                    ) {
                        Text("+ ADD ROOM", fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            roomsList = mutableListOf()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF0EA5E9)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0EA5E9))
                    ) {
                        Text("- CLEAR ALL ROOMS", fontWeight = FontWeight.Black)
                    }
                }

                // Invigilators Box
                Column(
                    modifier = Modifier.weight(1f).background(Color(0xFF1E293B), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)).padding(20.dp).heightIn(min=300.dp, max=400.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).background(Color(0xFF38BDF8).copy(0.1f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text("2", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Select Invigilators", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    if (isLoadingStaff) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        }
                    } else if (staffList.isEmpty()) {
                        Text("No staff available.", color = Color.Gray)
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            staffList.forEach { st ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).background(Color.Black.copy(0.2f), RoundedCornerShape(4.dp)).padding(8.dp).clickable {
                                        val newSet = selectedStaffIds.toMutableSet()
                                        if (newSet.contains(st.id)) newSet.remove(st.id) else newSet.add(st.id)
                                        selectedStaffIds = newSet
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedStaffIds.contains(st.id),
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8), uncheckedColor = Color(0xFF64748B))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(st.full_name, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))

            // ARRANGE EXAM HALL BUTTON
            OutlinedButton(
                onClick = {
                    if (date.isEmpty() || selectedSubjectId.isEmpty()) {
                        android.widget.Toast.makeText(context, "Please select Date and Subject", android.widget.Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    val validRooms = roomsList.filter { it.name.trim().isNotEmpty() && it.capacity.trim().isNotEmpty() }
                    if (validRooms.isEmpty()) {
                        android.widget.Toast.makeText(context, "Please add at least one valid room", android.widget.Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    if (selectedStaffIds.isEmpty()) {
                        android.widget.Toast.makeText(context, "Please select at least one invigilator", android.widget.Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    
                    isArranging = true
                    
                    val roomsArray = JSONArray()
                    validRooms.forEach {
                        val obj = JSONObject()
                        obj.put("name", it.name.trim())
                        obj.put("cap", it.capacity.trim())
                        roomsArray.put(obj)
                    }
                    
                    val staffArray = JSONArray()
                    selectedStaffIds.forEach { id ->
                        val obj = JSONObject()
                        obj.put("id", id)
                        obj.put("name", staffList.find { it.id == id }?.full_name ?: "Unknown")
                        staffArray.put(obj)
                    }

                    viewModel.generateHallView(
                        date = date, shift = shift, subjectId = selectedSubjectId, roomsJson = roomsArray.toString(), staffJson = staffArray.toString(),
                        onSuccess = { responseBody ->
                            isArranging = false
                            try {
                                generatedHtml = responseBody.string()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Error reading response", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onError = { err ->
                            isArranging = false
                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF0EA5E9)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White)
            ) {
                if (isArranging) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ARRANGE EXAM HALL", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }

            // Hall Preview
            if (generatedHtml != null) {
                Spacer(Modifier.height(30.dp))
                Text("Hall Preview Details", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(600.dp).background(Color.White)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.WHITE)
                                loadDataWithBaseURL(null, generatedHtml!!, "text/HTML", "UTF-8", null)
                            }
                        },
                        update = { view ->
                             view.loadDataWithBaseURL(null, generatedHtml!!, "text/HTML", "UTF-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

        } else {
            // "Attendance Sheet" Tab implementation
            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Construction, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(16.dp))
                Text("Attendance Sheet Functionality Pending", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
