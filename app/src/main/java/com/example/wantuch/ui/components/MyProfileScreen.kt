package com.example.wantuch.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.wantuch.domain.model.StaffProfileResponse
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import kotlin.collections.component1
import kotlin.collections.component2


@Composable
fun MyProfileScreen(
    staffId: Int,
    viewModel: WantuchViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.staffProfile.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(staffId) {
        viewModel.fetchStaffProfile(staffId)
    }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize()) {
            // Cool Action Bar
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, Modifier.background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), CircleShape)) {
                    Icon(Icons.Default.ArrowBack, null, tint = textColor)
                }
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.background(Color(0xFF3B82F6).copy(0.1f), RoundedCornerShape(30.dp)).padding(horizontal = 15.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!profile?.institution?.get("logo")?.toString().isNullOrEmpty()) {
                                AsyncImage(model = "https://www.wantuch.pk/${profile?.institution?.get("logo")}", contentDescription = null, modifier = Modifier.size(16.dp).clip(CircleShape))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(profile?.institution?.get("name")?.toString()?.uppercase() ?: "WANTUCH", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Text("FACULTY HUB", color = textColor.copy(0.4f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                IconButton(onClick = { viewModel.toggleTheme() }, Modifier.background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), CircleShape)) {
                    Icon(Icons.Default.Palette, null, tint = textColor)
                }
            }

            Spacer(Modifier.height(10.dp))
            ProfileHeaderCard(profile?.basic, isDark)
            Spacer(Modifier.height(25.dp))

            // The Dual Path Switcher
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(54.dp).background(cardColor, RoundedCornerShape(16.dp)).padding(5.dp)
            ) {
                ProfileTabButton("Personal Cloud", selectedTab == 0, Modifier.weight(1f), isDark) { selectedTab = 0 }
                ProfileTabButton("Institution Node", selectedTab == 1, Modifier.weight(1f), isDark) { selectedTab = 1 }
            }

            Spacer(Modifier.height(25.dp))

            Box(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                profile?.let { data ->
                    if (selectedTab == 0) {
                        StaffCloudTab(data, isDark, viewModel, staffId)
                    } else {
                        InstitutionHubGrid(data, isDark) { section ->
                            Toast.makeText(context, "Node Section: $section", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            }
        }
    }
}

@Composable
fun ProfileHeaderCard(u: Map<String, Any?>?, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)

    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).background(cardColor, RoundedCornerShape(24.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        // Profile Pic
        Box(Modifier.size(70.dp).background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)), Alignment.Center) {
            val pic = u?.get("profile_pic")?.toString()
            if (!pic.isNullOrEmpty()) {
                AsyncImage(model = pic, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(u?.get("full_name")?.toString()?.take(2)?.uppercase() ?: "US", color = textColor.copy(0.3f), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(20.dp))

        Column {
            Text(u?.get("full_name")?.toString() ?: "N/A", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("ROLE: ${u?.get("role")?.toString()?.uppercase() ?: "STAFF"}", color = textColor.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.background(Color(0xFF3B82F6).copy(0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("SECURE IDENTITY NODE", color = Color(0xFF3B82F6), fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun InstitutionHubGrid(data: StaffProfileResponse, isDark: Boolean, onSectionClick: (String) -> Unit) {
    val sections = listOf(
        Triple("Profile", Icons.Default.School, Color(0xFF3B82F6)),
        Triple("Posts", Icons.Default.Campaign, Color(0xFF10B981)),
        Triple("Assets", Icons.Default.Category, Color(0xFF8B5CF6)),
        Triple("Funds", Icons.Default.Payments, Color(0xFFEAB308)),
        Triple("Schedules", Icons.Default.CalendarToday, Color(0xFFF43F5E)),
        Triple("Bank Nodes", Icons.Default.AccountBalance, Color(0xFF6366F1))
    )

    Column(Modifier.verticalScroll(rememberScrollState())) {
        sections.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                row.forEach { (label, icon, color) ->
                    val cardBg = if(isDark) Color(0xFF1E293B) else Color.White
                    Box(
                        Modifier.weight(1f).height(130.dp).background(cardBg, RoundedCornerShape(12.dp)).border(1.dp, if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp)).clickable { onSectionClick(label) }.padding(20.dp),
                        Alignment.Center
                    ) {
                        Box(Modifier.size(6.dp).align(Alignment.TopStart).offset(x = 10.dp, y = 10.dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.TopEnd).offset(x = (-10).dp, y = 10.dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.BottomStart).offset(x = 10.dp, y = (-10).dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.BottomEnd).offset(x = (-10).dp, y = (-10).dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(label.uppercase(), color = if(isDark) Color.White else Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)

                            val subLabel = when(label) {
                                "Profile" -> data.institution?.get("type")?.toString()?.uppercase() ?: "NODE"
                                "Posts" -> {
                                    val total = data.inst_posts?.sumOf { (it["total_posts"] as? Number)?.toInt() ?: 0 } ?: 0
                                    val filled = data.inst_posts?.sumOf { (it["filled_posts"] as? Number)?.toInt() ?: 0 } ?: 0
                                    "$filled / $total"
                                }
                                "Assets" -> {
                                    val count = data.inst_assets?.size ?: 0
                                    "$count ITEMS"
                                }
                                "Funds" -> {
                                    val total = data.inst_funds?.sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 } ?: 0.0
                                    "Rs. ${String.format("%.0f", total)}"
                                }
                                "Schedules" -> {
                                    val count = data.inst_timetable?.groupBy { "${it["class_id"]}-${it["section_id"]}" }?.size ?: 0
                                    "$count ACTIVE"
                                }
                                "Bank Nodes" -> {
                                    val count = data.inst_bank?.size ?: 0
                                    "$count NODES"
                                }
                                else -> "NODE"
                            }

                            Box(Modifier.padding(top = 4.dp).background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(subLabel, color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(15.dp))
        }
    }
}

@Composable
fun ProfileTabButton(label: String, isSelected: Boolean, modifier: Modifier, isDark: Boolean, onClick: () -> Unit) {
    val bgColor = if(isSelected) (if(isDark) Color.Black else Color(0xFF0F172A)) else Color.Transparent
    val textColor = if(isSelected) Color(0xFF3B82F6) else (if(isDark) Color.White.copy(0.4f) else Color.Black.copy(0.3f))

    Box(
        modifier.fillMaxHeight().background(bgColor, RoundedCornerShape(8.dp)).clickable { onClick() },
        Alignment.Center
    ) {
        Text(label.uppercase(), color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
fun ProfileEditableField(label: String, value: String?, isDark: Boolean, onValueChange: (String) -> Unit) {
    val containerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    val textColor = if(isDark) Color.White else Color.Black
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box(Modifier.fillMaxWidth().height(45.dp).background(containerColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isNullOrEmpty()) {
                Text(label, color = textColor.copy(0.3f), fontSize = 14.sp)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value ?: "",
                onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDropdownField(label: String, value: String?, options: List<String>, isDark: Boolean, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val containerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    val textColor = if(isDark) Color.White else Color.Black

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            Box(
                Modifier.fillMaxWidth().height(45.dp).background(containerColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(value ?: "Select $label", color = if(value.isNullOrEmpty()) textColor.copy(0.3f) else textColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(if(expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(if(isDark) Color(0xFF1E293B) else Color.White)
            ) {
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption, color = textColor) },
                        onClick = {
                            onValueChange(selectionOption)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDateField(label: String, value: String?, isDark: Boolean, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    val containerColor = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    val textColor = if(isDark) Color.White else Color.Black

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box(
            Modifier.fillMaxWidth().height(45.dp).background(containerColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp).clickable {
                // Show DatePicker Dialog
                val calendar = java.util.Calendar.getInstance()
                val dpd = android.app.DatePickerDialog(context, { _, year, month, dayOfMonth ->
                    onValueChange("$year-${month + 1}-$dayOfMonth")
                }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH))
                dpd.show()
            },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value ?: "YYYY-MM-DD", color = if(value.isNullOrEmpty()) textColor.copy(0.3f) else textColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AddNodeButton(label: String, isDark: Boolean, onClick: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp).background(cardColor, RoundedCornerShape(16.dp)).border(1.dp, if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f), RoundedCornerShape(16.dp)).clickable { onClick() }.padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Add, null, tint = textColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label.uppercase(), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
fun ActionIcons(isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val textColor = if(isDark) Color.White else Color.Black
    val bg = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    Row {
        Box(Modifier.size(36.dp).background(bg, CircleShape).clickable { onEdit() }, Alignment.Center) {
            Icon(Icons.Default.Edit, null, tint = textColor, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(36.dp).background(bg, CircleShape).clickable { onDelete() }, Alignment.Center) {
            Icon(Icons.Default.Delete, null, tint = textColor, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ProfileIdentityForm(data: StaffProfileResponse, isDark: Boolean, onUpdate: (Map<String, String>) -> Unit) {
    val basic = data.basic ?: emptyMap()
    val textColor = if (isDark) Color.White else Color.Black

    val formState = remember { mutableStateMapOf<String, String>().apply {
        basic.forEach { (k, v) -> put(k, v?.toString() ?: "") }

        // Prioritize plain password. If only hash is present and it looks like a hash, don't show it to avoid confusion.
        val pass = basic["password"]?.toString() ?: ""
        val hash = basic["password_hash"]?.toString() ?: ""

        if (pass.isNotEmpty()) {
            put("password", pass)
        } else if (hash.isNotEmpty() && !hash.startsWith("$2y$")) {
            // Only show if it's not a bcrypt hash (some old records might have plain text in the hash column)
            put("password", hash)
        } else if (hash.isNotEmpty()) {
            put("password", "") // Don't show the hash characters
        }
    } }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        ProfileDataSection("Account Config", isDark) {
            ProfileEditableField("Username", formState["username"], isDark) { formState["username"] = it }
            ProfileEditableField("Password", formState["password"], isDark) { formState["password"] = it }
            ProfileDropdownField("Role", formState["role"], listOf("super_admin", "admin", "staff", "student"), isDark) { formState["role"] = it }
            ProfileDropdownField("User Type", formState["user_type"], listOf("teaching", "non-teaching"), isDark) { formState["user_type"] = it }
        }

        Spacer(Modifier.height(16.dp))

        ProfileDataSection("Personal Details", isDark) {
            ProfileEditableField("Full Name", formState["full_name"], isDark) { formState["full_name"] = it }
            ProfileEditableField("Father Name", formState["father_name"], isDark) { formState["father_name"] = it }
            ProfileDropdownField("Gender", formState["gender"], listOf("Male", "Female"), isDark) { formState["gender"] = it }
            ProfileDateField("Date of Birth", formState["dob"], isDark) { formState["dob"] = it }
            ProfileEditableField("CNIC Number", formState["cnic"], isDark) { formState["cnic"] = it }
            ProfileEditableField("Residential Address", formState["address"], isDark) { formState["address"] = it }
        }

        Spacer(Modifier.height(16.dp))

        ProfileDataSection("Health & Tribe", isDark) {
            ProfileEditableField("Tribe", formState["tribe"], isDark) { formState["tribe"] = it }
            ProfileDropdownField("Religion", formState["religion"], listOf("Islam", "Christianity", "Hinduism", "Sikhism", "Buddhism", "Other"), isDark) { formState["religion"] = it }
            ProfileEditableField("Disability", formState["disability"], isDark) { formState["disability"] = it }
            ProfileEditableField("Permanent Disease", formState["permenent_disease"], isDark) { formState["permenent_disease"] = it }
        }

        Spacer(Modifier.height(16.dp))

        ProfileDataSection("Employment & Finance", isDark) {
            ProfileEditableField("Designation", formState["designation"], isDark) { formState["designation"] = it }
            ProfileEditableField("BPS Scale", formState["bps"], isDark) { formState["bps"] = it }
            ProfileEditableField("Monthly Salary", formState["salary"], isDark) { formState["salary"] = it }
            ProfileDropdownField("Account Status", formState["status"], listOf("active", "inactive"), isDark) { formState["status"] = it }
            ProfileDateField("Joined Date", formState["created_at"], isDark) { formState["created_at"] = it }
            ProfileEditableField("Special Bonus", formState["special_bonus"], isDark) { formState["special_bonus"] = it }
        }

        Spacer(Modifier.height(16.dp))

        ProfileDataSection("Social & Transport", isDark) {
            ProfileEditableField("WhatsApp No", formState["whatsapp_no"], isDark) { formState["whatsapp_no"] = it }
            ProfileEditableField("Facebook Link", formState["facebook_link"], isDark) { formState["facebook_link"] = it }
            ProfileEditableField("TikTok Link", formState["tiktok_link"], isDark) { formState["tiktok_link"] = it }
            ProfileDropdownField("Transport Active", formState["is_transport_active"], listOf("Yes", "No"), isDark) { formState["is_transport_active"] = it }
            ProfileEditableField("Transport Location", formState["transport_location"], isDark) { formState["transport_location"] = it }
            ProfileEditableField("Transport Charges", formState["transport_charges"], isDark) { formState["transport_charges"] = it }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = { onUpdate(formState.toMap()) },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color.White else Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Upload, null, tint = if(isDark) Color.Black else Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("PUSH IDENTITY UPDATES", color = if(isDark) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun ProfileDataSection(title: String, isDark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val textColor = if (isDark) Color.White else Color.Black
    Column {
        Text(title.uppercase(), color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(16.dp)).padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun ProfileDataField(label: String, value: String?, isDark: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value ?: "N/A", color = if(isDark) Color.White else Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProfileEditDialog(
    title: String,
    fields: List<Triple<String, String, String>>, // Label, Key, Type (text, date, select)
    options: Map<String, List<String>> = emptyMap(), // For select fields
    initialData: Map<String, String>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val formState = remember { mutableStateMapOf<String, String>().apply {
        initialData.forEach { (k, v) -> put(k, v) }
    }}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = if(isDark) Color.White else Color.Black) },
        text = {
            Column {
                fields.forEach { (label, key, type) ->
                    when (type) {
                        "text", "number" -> ProfileEditableField(label, formState[key], isDark) { formState[key] = it }
                        "date" -> ProfileDateField(label, formState[key], isDark) { formState[key] = it }
                        "select" -> ProfileDropdownField(label, formState[key], options[key] ?: emptyList(), isDark) { formState[key] = it }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(formState.toMap()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = if(isDark) Color(0xFF1E293B) else Color.White,
        titleContentColor = if(isDark) Color.White else Color.Black,
        textContentColor = if(isDark) Color.White else Color.Black
    )
}

@Composable
fun ProfileContactsList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.contacts ?: emptyList()
    var editingContact by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingContact != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Contact" else "Edit Contact",
            fields = listOf(
                Triple("Type", "type", "select"),
                Triple("Value", "value", "text"),
                Triple("Detail", "detail", "text")
            ),
            options = mapOf("type" to listOf("Phone", "Email", "WhatsApp", "Facebook", "TikTok", "Instagram")),
            initialData = editingContact ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingContact = null },
            onSave = { fields ->
                val payload = if(editingContact != null) fields + ("id" to (editingContact!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingContact = null
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD NEW RECORD", isDark) { isAdding = true }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO RECORDS FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        items.forEach { contact ->
            val cMap = contact.mapValues { it.value?.toString() ?: "" }
            ContactCard(cMap, isDark, onEdit = { editingContact = cMap }, onDelete = { onDelete(cMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun ContactCard(c: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val type = c["type"] ?: "Phone"
    val icon = when(type.lowercase()) {
        "email" -> Icons.Default.Email
        "whatsapp" -> Icons.AutoMirrored.Filled.Chat
        else -> Icons.Default.Phone
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF10B981).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(type.uppercase(), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(c["value"] ?: "N/A", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            val detail = c["detail"]
            if (!detail.isNullOrEmpty()) Text(detail, color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileAcademicsList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.academics ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Academic" else "Edit Academic",
            fields = listOf(
                Triple("Degree Title", "degree_title", "text"),
                Triple("Core Subject", "core_subject", "text"),
                Triple("Detail", "detail", "text")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields ->
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD NEW RECORD", isDark) { isAdding = true }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO RECORDS FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            AcademicCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun AcademicCard(a: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF3B82F6).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.School, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(a["core_subject"]?.uppercase() ?: "SUBJECT", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(a["degree_title"] ?: "N/A", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(a["detail"] ?: "N/A", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileExperienceList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.experience ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Experience" else "Edit Experience",
            fields = listOf(
                Triple("Field Title", "field_title", "text"),
                Triple("Detail", "detail", "text"),
                Triple("From Date", "from_date", "date"),
                Triple("To Date", "to_date", "date")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields ->
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD NEW RECORD", isDark) { isAdding = true }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO RECORDS FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            ExperienceCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun ExperienceCard(e: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF8B5CF6).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.Work, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("${e["from_date"]} - ${e["to_date"] ?: "Present"}", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(e["field_title"] ?: "N/A", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(e["detail"] ?: "N/A", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileBankList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.bank ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Bank Account" else "Edit Bank Account",
            fields = listOf(
                Triple("Bank Name", "bank_name", "text"),
                Triple("Account No", "account_no", "text"),
                Triple("Branch Name", "branch_name", "text"),
                Triple("Account Title", "account_title", "text")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields ->
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD NEW RECORD", isDark) { isAdding = true }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO RECORDS FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            BankCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun BankCard(b: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFFEAB308).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.AccountBalance, null, tint = Color(0xFFEAB308), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(b["bank_name"]?.uppercase() ?: "BANK", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            val accNo = b["account_no"] ?: "XXXX XXXX"
            Text(accNo.chunked(4).joinToString(" "), color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${b["account_title"] ?: "N/A"} (${b["branch_name"] ?: "N/A"})", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun PlaceholderSection(title: String, isDark: Boolean) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Build, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
            Text(title.uppercase(), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileInstTimetableList(data: StaffProfileResponse, isDark: Boolean) {
    val items = data.inst_timetable ?: emptyList()

    // Group by Class + Section
    val grouped = items.groupBy { "${it["class_name"]} - ${it["section_name"]}" }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO SCHEDULES FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }

        grouped.forEach { (key, list) ->
            Text(key.uppercase(), color = Color(0xFF3B82F6), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            list.forEach { tt ->
                val ttMap = tt.mapValues { it.value?.toString() ?: "" }
                TimetableCard(ttMap, isDark)
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun TimetableCard(tt: Map<String, String>, isDark: Boolean) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF3B82F6).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.Schedule, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(tt["subject_name"] ?: "SUBJECT", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${tt["day"]} | ${tt["start_time"]} - ${tt["end_time"]}", color = Color.Gray, fontSize = 11.sp)
        }
        Text("V${tt["timetable_version"]}", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun InstitutionProfileDetail(inst: Map<String, Any?>?, isDark: Boolean, onSave: (Map<String, String>) -> Unit) {
    val textColor = if (isDark) Color.White else Color.Black
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    var isEditing by remember { mutableStateOf(false) }

    if (isEditing) {
        val iMap = inst?.mapValues { it.value?.toString() ?: "" } ?: emptyMap()
        ProfileEditDialog(
            title = "Edit Institution Profile",
            fields = listOf(
                Triple("Institution Name", "name", "text"),
                Triple("Category / Type", "category", "select"),
                Triple("Sector", "sector", "select"),
                Triple("Education Level", "edu_level", "text"),
                Triple("Registration Number", "reg_no", "text"),
                Triple("Full Address", "address", "text"),
                Triple("Contact Number", "contact_no", "text"),
                Triple("Institution Email", "email", "text"),
                Triple("Enable WhatsApp (0/1)", "whatsapp_enabled", "number"),
                Triple("API Provider", "wp_provider", "select"),
                Triple("API Key / Token", "wp_token", "text"),
                Triple("Instance ID / Sender ID", "wp_instance_id", "text")
            ),
            options = mapOf(
                "category" to listOf("School", "College", "University", "Academy", "Madrasa", "Other"),
                "sector" to listOf("Government", "Private", "Semi-Government", "NGO"),
                "wp_provider" to listOf("UltraMsg", "Twilio", "MessageBird", "Other")
            ),
            initialData = iMap,
            isDark = isDark,
            onDismiss = { isEditing = false },
            onSave = { onSave(it); isEditing = false }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(100.dp).background(cardColor, RoundedCornerShape(30.dp)).padding(10.dp), Alignment.Center) {
            val logo = inst?.get("logo_path")?.toString()
            if (!logo.isNullOrEmpty()) {
                AsyncImage(model = "https://www.wantuch.pk/$logo", contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                Icon(Icons.Default.School, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(50.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(inst?.get("name")?.toString() ?: "INSTITUTION", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(inst?.get("type")?.toString()?.uppercase() ?: "RECOGNIZED NODE", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)

        Spacer(Modifier.height(10.dp))
        Button(onClick = { isEditing = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)), shape = RoundedCornerShape(12.dp)) {
            Text("EDIT BASIC INFO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(30.dp))

        ProfileDataSection("Identification Nodes", isDark) {
            ProfileDataField("Category / Type", inst?.get("category")?.toString(), isDark)
            ProfileDataField("Sector / Affiliation", inst?.get("sector")?.toString(), isDark)
            ProfileDataField("Education Level", inst?.get("edu_level")?.toString(), isDark)
            ProfileDataField("Registration No", inst?.get("reg_no")?.toString(), isDark)
            ProfileDataField("Address", inst?.get("address")?.toString(), isDark)
            ProfileDataField("Contact", inst?.get("contact_no")?.toString(), isDark)
            ProfileDataField("E-Mail", inst?.get("email")?.toString(), isDark)
        }

        Spacer(Modifier.height(16.dp))

        ProfileDataSection("WhatsApp Automation", isDark) {
            ProfileDataField("Enabled", if(inst?.get("whatsapp_enabled")?.toString() == "1") "YES" else "NO", isDark)
            ProfileDataField("Provider", inst?.get("wp_provider")?.toString(), isDark)
            ProfileDataField("Instance ID", inst?.get("wp_instance_id")?.toString(), isDark)
        }

        Spacer(Modifier.height(16.dp))

        ProfileDataSection("Verification Status", isDark) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("VERIFIED INSTITUTION NODE", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun ProfileInstPostsList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.inst_posts ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Post" else "Edit Post",
            fields = listOf(
                Triple("Designation", "designation", "text"),
                Triple("BPS / Pay Scale", "bps", "text"),
                Triple("Total Posts", "total_posts", "number"),
                Triple("Filled Posts", "filled_posts", "number")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields ->
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD NEW POST", isDark) { isAdding = true }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("NO POSTS FOUND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            InstPostCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun InstPostCard(p: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF3B82F6).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.Person, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(p["designation"]?.uppercase() ?: "POST", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("BPS: ${p["bps"] ?: "N/A"}", color = Color.Gray, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("T: ${p["total_posts"]} ", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("F: ${p["filled_posts"]} ", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("V: ${p["vacant_posts"]} ", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileInstBankList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.inst_bank ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Inst Bank" else "Edit Inst Bank",
            fields = listOf(
                Triple("Bank Name", "bank_name", "text"),
                Triple("Branch Name", "branch_name", "text"),
                Triple("Branch Code", "branch_code", "text"),
                Triple("Account No", "account_no", "text"),
                Triple("Detail", "detail", "text")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields ->
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD BANK NODE", isDark) { isAdding = true }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            InstBankCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun InstBankCard(b: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFFEAB308).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.AccountBalance, null, tint = Color(0xFFEAB308), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(b["bank_name"] ?: "BANK", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${b["account_no"]} (${b["branch_code"]})", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileInstAssetsList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.inst_assets ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Asset" else "Edit Asset",
            fields = listOf(
                Triple("Asset Name", "asset_name", "text"),
                Triple("Type / Category", "asset_type", "text"),
                Triple("Functional Qty", "functional_qty", "number"),
                Triple("Non-Functional", "non_functional_qty", "number")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields ->
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD ASSET NODE", isDark) { isAdding = true }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            InstAssetCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun InstAssetCard(a: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF8B5CF6).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.Build, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a["asset_name"] ?: "ASSET", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.background(Color(0xFF8B5CF6).copy(0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(a["asset_type"]?.uppercase() ?: "GENERAL", color = Color(0xFF8B5CF6), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Functional: ${a["functional_qty"]} | Non-F: ${a["non_functional_qty"]}", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun ProfileInstFundsList(data: StaffProfileResponse, isDark: Boolean, onSave: (Map<String, String>) -> Unit, onDelete: (Int) -> Unit) {
    val items = data.inst_funds ?: emptyList()
    var editingNode by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingNode != null) {
        ProfileEditDialog(
            title = if(isAdding) "Add Fund" else "Edit Fund",
            fields = listOf(
                Triple("Fund Name", "name", "text"),
                Triple("Fund Type", "type", "select"),
                Triple("Account Number", "account_number", "text"),
                Triple("Initial Amount", "amount", "number")
            ),
            options = mapOf(
                "type" to listOf("Reserve", "Welfare", "Development", "Operational", "Other")
            ),
            initialData = editingNode ?: emptyMap(),
            isDark = isDark,
            onDismiss = { isAdding = false; editingNode = null },
            onSave = { fields ->
                val payload = if(editingNode != null) fields + ("id" to (editingNode!!["id"] ?: "0")) else fields
                onSave(payload)
                isAdding = false; editingNode = null
            }
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AddNodeButton("ADD FUND NODE", isDark) { isAdding = true }
        items.forEach { node ->
            val nMap = node.mapValues { it.value?.toString() ?: "" }
            InstFundCard(nMap, isDark, onEdit = { editingNode = nMap }, onDelete = { onDelete(nMap["id"]?.toInt() ?: 0) })
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun InstFundCard(f: Map<String, String>, isDark: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(cardColor, RoundedCornerShape(16.dp)).clickable { onEdit() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF10B981).copy(0.1f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.Payments, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(f["name"] ?: "FUND", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("A/C: ${f["account_number"]} | Rs. ${f["amount"]} (${f["type"]})", color = Color.Gray, fontSize = 11.sp)
        }
        ActionIcons(isDark, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
fun PersonalHubGrid(data: StaffProfileResponse, isDark: Boolean, onSectionClick: (String) -> Unit) {
    val sections = listOf(
        Triple("Identity", Icons.Default.Fingerprint, Color(0xFF00F3FF)),
        Triple("Contacts", Icons.Default.Phone, Color(0xFF10B981)),
        Triple("Education", Icons.Default.School, Color(0xFF6366F1)),
        Triple("Experience", Icons.Default.Work, Color(0xFFEC4899)),
        Triple("Banking", Icons.Default.AccountBalance, Color(0xFFF59E0B))
    )

    Column(Modifier.verticalScroll(rememberScrollState())) {
        sections.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                row.forEach { (label, icon, color) ->
                    val cardBg = if(isDark) Color(0xFF1E293B) else Color.White
                    Box(
                        Modifier.weight(1f).height(130.dp).background(cardBg, RoundedCornerShape(12.dp)).border(1.dp, if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(12.dp)).clickable { onSectionClick(label) }.padding(20.dp),
                        Alignment.Center
                    ) {
                        Box(Modifier.size(6.dp).align(Alignment.TopStart).offset(x = 10.dp, y = 10.dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.TopEnd).offset(x = (-10).dp, y = 10.dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.BottomStart).offset(x = 10.dp, y = (-10).dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))
                        Box(Modifier.size(6.dp).align(Alignment.BottomEnd).offset(x = (-10).dp, y = (-10).dp).background(if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.1f), CircleShape))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(label.uppercase(), color = if(isDark) Color.White else Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)

                            val count = when(label) {
                                "Contacts" -> data.contacts?.size ?: 0
                                "Education" -> data.academics?.size ?: 0
                                "Experience" -> data.experience?.size ?: 0
                                "Banking" -> data.bank?.size ?: 0
                                else -> 0
                            }
                            val unit = when(label) {
                                "Education" -> "RECORDS"
                                "Experience" -> "SLOTS"
                                "Banking" -> "ACCOUNTS"
                                else -> "NODES"
                            }

                            Box(Modifier.padding(top = 4.dp).background(if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("$count $unit", color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(15.dp))
        }
    }
}
