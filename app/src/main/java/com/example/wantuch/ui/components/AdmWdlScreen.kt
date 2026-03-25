package com.example.wantuch.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.wantuch.BASE_URL
import com.example.wantuch.domain.model.AdmWdlRecord
import com.example.wantuch.ui.viewmodel.WantuchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Colours (dynamic, set inside composable) ───────────────────────────────
private val LocalDBg     = compositionLocalOf { Color(0xFF070A10) }
private val LocalDSurface = compositionLocalOf { Color(0xFF0E121A) }
private val LocalDCard    = compositionLocalOf { Color(0xFF141A26) }
private val LocalDCard2   = compositionLocalOf { Color(0xFF1E293B) }
private val LocalTextOn   = compositionLocalOf { Color(0xFFF1F5F9) }
private val LocalTextDim  = compositionLocalOf { Color(0xFF94A3B8) }

private val Gold         = Color(0xFFFBBF24)
private val Emerald      = Color(0xFF10B981)
private val Violet       = Color(0xFF8B5CF6)
private val Rose         = Color(0xFFF43F5E)
private val Cyan         = Color(0xFF06B6D4)

// Certificate URL builder
private fun certUrl(type: String, studentId: Int): String =
    "${BASE_URL}modules/education/certificate_print.php?type=$type&student_id=$studentId"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmWdlScreen(
    viewModel: WantuchViewModel,
    onBack: () -> Unit
) {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()

    val isDark     by viewModel.isDarkTheme.collectAsState()

    val DBg      = if (isDark) Color(0xFF070A10) else Color(0xFFF1F5F9)
    val DSurface = if (isDark) Color(0xFF0E121A) else Color(0xFFE2E8F0)
    val DCard    = if (isDark) Color(0xFF141A26) else Color.White
    val DCard2   = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val TextOn   = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val TextDim  = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val freshData   by viewModel.admFreshData.collectAsState()
    val oldData     by viewModel.admOldData.collectAsState()
    val certData    by viewModel.certSearchData.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val dashData    by viewModel.dashboardData.collectAsState()
    val instList    by viewModel.institutions.collectAsState()

    val currInstId  = context.getSharedPreferences("wantuch_prefs", android.content.Context.MODE_PRIVATE).getInt("last_inst", 0)
    val currInst    = instList.find { it.id.toString() == currInstId.toString() }
    val instName    = dashData?.institution_name ?: currInst?.name ?: "INSTITUTION NAME"
    
    val staffProfile by viewModel.staffProfile.collectAsState()
    val instNodeLogo = dashData?.institution_logo ?: staffProfile?.institution?.get("logo_path")?.toString() ?: staffProfile?.institution?.get("logo")?.toString()
    val logoPath    = if (!instNodeLogo.isNullOrEmpty()) instNodeLogo else currInst?.logo ?: ""
    val cleanPath   = logoPath.trimStart('/')
    val logoUrl     = if (cleanPath.isNotEmpty() && !cleanPath.startsWith("http")) "${BASE_URL}$cleanPath" else cleanPath

    // 0=Fresh  1=Old  2=Certificates
    var tab         by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf("") }

    // Dialog states
    var showAddFresh   by remember { mutableStateOf(false) }
    var showAddOld     by remember { mutableStateOf(false) }
    var editFresh      by remember { mutableStateOf<AdmWdlRecord?>(null) }
    var editOld        by remember { mutableStateOf<AdmWdlRecord?>(null) }
    var withdrawRec    by remember { mutableStateOf<AdmWdlRecord?>(null) }
    var deleteRec      by remember { mutableStateOf<Pair<AdmWdlRecord, String>?>(null) }
    var certStudent    by remember { mutableStateOf<AdmWdlRecord?>(null) }  // cert picker

    // Debounced search
    LaunchedEffect(searchQuery, tab) {
        delay(450)
        when (tab) {
            0 -> viewModel.loadAdmFresh(q = searchQuery, cls = classFilter)
            1 -> viewModel.loadAdmOld(q = searchQuery)
            2 -> {
                if (searchQuery.length >= 2) viewModel.searchCertStudents(searchQuery)
                else if (searchQuery.isEmpty()) viewModel.clearCertSearch()
            }
        }
    }

    // Initial load
    LaunchedEffect(tab) {
        searchQuery = ""
        when (tab) {
            0 -> viewModel.loadAdmFresh()
            1 -> viewModel.loadAdmOld()
            2 -> viewModel.clearCertSearch()
        }
    }

    val freshList = freshData?.data ?: emptyList()
    val oldList   = oldData?.data ?: emptyList()
    val certList  = certData?.data ?: emptyList()

    val accentCol = when (tab) { 0 -> Gold; 1 -> Violet; else -> Emerald }

    CompositionLocalProvider(
        LocalDBg provides DBg,
        LocalDSurface provides DSurface,
        LocalDCard provides DCard,
        LocalDCard2 provides DCard2,
        LocalTextOn provides TextOn,
        LocalTextDim provides TextDim
    ) {
    Box(Modifier.fillMaxSize().background(DBg)) {

        Column(Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(DCard, DBg)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onBack, Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.ArrowBack, null, tint = Gold)
                }
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ADM / WDL", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("ADMISSION & WITHDRAWAL REGISTER", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            // ── Tab Row ─────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MainTab("FRESH\n${freshList.size}", Gold,   tab == 0, Modifier.weight(1f)) { tab = 0 }
                MainTab("OLD REC.\n${oldList.size}",  Violet, tab == 1, Modifier.weight(1f)) { tab = 1 }
                MainTab("CERTS",                      Emerald, tab == 2, Modifier.weight(1f)) { tab = 2 }
            }

            // ── Search Bar ───────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),

                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        val ph = when (tab) {
                            0 -> "Search name, adm no, father..."
                            1 -> "Search name, adm no..."
                            else -> "Search student for certificate..."
                        }
                        Text(ph, color = TextDim, fontSize = 12.sp)
                    },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = accentCol) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton({ searchQuery = "" }) { Icon(Icons.Default.Clear, null, tint = TextDim) } }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentCol,
                        unfocusedBorderColor = TextDim.copy(0.3f),
                        focusedTextColor = TextOn, unfocusedTextColor = TextOn,
                        cursorColor = accentCol,
                        focusedContainerColor = DCard2, unfocusedContainerColor = DCard
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(52.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = TextOn)
                )
                // Add button — only on fresh + old tabs
                if (tab != 2) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                            .background(accentCol)
                            .clickable { if (tab == 0) showAddFresh = true else showAddOld = true },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.PersonAdd, null, tint = Color.Black) }
                }
            }

            // ── Content ──────────────────────────────────────────────────────────
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> RecordList(
                        list = freshList,
                        isLoading = isLoading,
                        emptyMsg = "No enrolled students found\nfor the current academic year",
                        emptyIcon = Icons.Default.PersonSearch
                    ) { rec ->
                        FreshCard(rec,
                            onEdit = { editFresh = it },
                            onWithdraw = { withdrawRec = it },
                            onDelete = { deleteRec = it to "fresh" }
                        )
                    }
                    1 -> RecordList(
                        list = oldList,
                        isLoading = isLoading,
                        emptyMsg = "No historical records found",
                        emptyIcon = Icons.Default.History
                    ) { rec ->
                        OldCard(rec,
                            onEdit = { editOld = it },
                            onDelete = { deleteRec = it to "old" }
                        )
                    }
                    2 -> CertificatesTab(
                        certList   = certList,
                        isLoading  = isLoading,
                        hasQuery   = searchQuery.length >= 2,
                        onCertClick = { certStudent = it }
                    )
                }

                // Refresh FAB (not on cert tab)
                if (tab != 2) {
                    FloatingActionButton(
                        onClick = { if (tab == 0) viewModel.loadAdmFresh() else viewModel.loadAdmOld() },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        containerColor = DCard2, contentColor = accentCol
                    ) { Icon(Icons.Default.Refresh, null) }
                }
            }
        }

        // Loading bar
        if (isLoading) {
            LinearProgressIndicator(
                color = accentCol,
                trackColor = accentCol.copy(0.1f),
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
            )
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────────
    if (showAddFresh) AddFreshDialog(null, viewModel, { showAddFresh = false }) { fields ->
        viewModel.saveAdmFresh(fields) { ok, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); if (ok) showAddFresh = false }
    }

    editFresh?.let { rec ->
        AddFreshDialog(rec, viewModel, { editFresh = null }) { fields ->
            viewModel.saveAdmFresh(fields) { ok, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); if (ok) editFresh = null }
        }
    }

    if (showAddOld) AddOldDialog(null, { showAddOld = false }) { fields ->
        viewModel.saveAdmOld(fields) { ok, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); if (ok) showAddOld = false }
    }

    editOld?.let { rec ->
        AddOldDialog(rec, { editOld = null }) { fields ->
            viewModel.saveAdmOld(fields) { ok, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); if (ok) editOld = null }
        }
    }

    withdrawRec?.let { rec ->
        WithdrawDialog(rec, { withdrawRec = null }) { sid, admNo, date, cls, slc ->
            viewModel.withdrawStudentAdm(sid, admNo, date, cls, slc) { ok, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                if (ok) withdrawRec = null
            }
        }
    }

    deleteRec?.let { (rec, source) ->
        AlertDialog(
            onDismissRequest = { deleteRec = null },
            containerColor = DCard2,
            icon = { Icon(Icons.Default.DeleteForever, null, tint = Rose) },
            title = { Text("Delete Record?", color = TextOn, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove: ${rec.displayName} (${rec.adm_no})", color = TextDim) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteAdmEntry(rec.idInt, source) { ok, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        if (ok) deleteRec = null
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Rose)) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteRec = null }) { Text("Cancel", color = TextDim) }
            }
        )
    }

    // Certificate type picker
    certStudent?.let { stu ->
        CertPickerDialog(student = stu, onDismiss = { certStudent = null }) { type ->
            printCertificateHtml(context, stu, type, instName, logoUrl)
            certStudent = null
        }
    }
    } // END CompositionLocalProvider
}

// ── Tab Button ───────────────────────────────────────────────────────────────
@Composable
private fun MainTab(label: String, color: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (selected) color.copy(0.18f) else LocalDCard.current
    val border = if (selected) color else LocalTextDim.current.copy(0.2f)
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) color else LocalTextDim.current, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
    }
}

// ── Generic Record List ───────────────────────────────────────────────────────
@Composable
private fun RecordList(
    list: List<AdmWdlRecord>,
    isLoading: Boolean,
    emptyMsg: String,
    emptyIcon: ImageVector,
    itemContent: @Composable (AdmWdlRecord) -> Unit
) {
    if (isLoading && list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold)
        }
        return
    }
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(emptyIcon, null, tint = LocalTextDim.current, modifier = Modifier.size(56.dp))
                Text(emptyMsg, color = LocalTextDim.current, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("${list.size} records", color = LocalTextDim.current, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp)) }
        items(list, key = { it.idInt.toString() + (it.adm_no ?: "") }) { rec ->
            itemContent(rec)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Fresh Student Card ───────────────────────────────────────────────────────
@Composable
private fun FreshCard(
    record: AdmWdlRecord,
    onEdit: (AdmWdlRecord) -> Unit,
    onWithdraw: (AdmWdlRecord) -> Unit,
    onDelete: (AdmWdlRecord) -> Unit
) {
    val isWithdrawn = !record.date_withdrawal.isNullOrEmpty()
    val statusColor = if (isWithdrawn) Rose else Emerald

    Card(
        colors = CardDefaults.cardColors(containerColor = LocalDCard.current),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, statusColor.copy(0.22f)),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Avatar
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Gold.copy(0.3f), LocalDCard2.current))), contentAlignment = Alignment.Center) {
                Text(record.displayName.take(2).uppercase(), color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f)) {
                Text(record.displayName, color = LocalTextOn.current, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Adm# ${record.adm_no ?: "N/A"} • F: ${record.father_name ?: "—"}", color = LocalTextDim.current, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(record.displayClass, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (!record.section_name.isNullOrEmpty()) Text("• Sec ${record.section_name}", color = LocalTextDim.current, fontSize = 10.sp)
                }
                if (!record.displayAdmDate.isEmpty()) Text("Adm: ${record.displayAdmDate}", color = Emerald.copy(0.8f), fontSize = 9.sp)
                if (isWithdrawn) Text("WITHDRAWN: ${record.date_withdrawal}", color = Rose, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                StatusBadge(if (isWithdrawn) "OUT" else "ACTIVE", statusColor)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ActionBtn(Icons.Default.Edit, Gold) { onEdit(record) }
                    if (!isWithdrawn) ActionBtn(Icons.Default.ExitToApp, Rose) { onWithdraw(record) }
                    ActionBtn(Icons.Default.Delete, Rose.copy(0.7f)) { onDelete(record) }
                }
            }
        }
    }
}

// ── Old Record Card ───────────────────────────────────────────────────────────
@Composable
private fun OldCard(
    record: AdmWdlRecord,
    onEdit: (AdmWdlRecord) -> Unit,
    onDelete: (AdmWdlRecord) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LocalDCard.current),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Violet.copy(0.2f)),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Violet.copy(0.3f), LocalDCard2.current))), contentAlignment = Alignment.Center) {
                Text(record.displayName.take(2).uppercase(), color = Violet, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f)) {
                Text(record.displayName, color = LocalTextOn.current, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Adm# ${record.adm_no ?: "N/A"} • ${record.father_name ?: "—"}", color = LocalTextDim.current, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!record.class_admission.isNullOrEmpty()) Text("In: ${record.class_admission}", color = Cyan, fontSize = 9.sp)
                    if (!record.class_withdrawal.isNullOrEmpty()) Text("Out: ${record.class_withdrawal}", color = Rose, fontSize = 9.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!record.displayAdmDate.isEmpty()) Text("DOA: ${record.displayAdmDate}", color = Emerald.copy(0.8f), fontSize = 9.sp)
                    if (!record.date_withdrawal.isNullOrEmpty()) Text("WDL: ${record.date_withdrawal}", color = Rose.copy(0.8f), fontSize = 9.sp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                record.slc_status?.let { slc ->
                    val slcColor = when (slc.lowercase()) { "issued" -> Emerald; "pending" -> Gold; else -> LocalTextDim.current }
                    StatusBadge(slc.uppercase(), slcColor)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ActionBtn(Icons.Default.Edit, Gold) { onEdit(record) }
                    ActionBtn(Icons.Default.Delete, Rose.copy(0.7f)) { onDelete(record) }
                }
            }
        }
    }
}

// ── Certificates Tab ──────────────────────────────────────────────────────────
@Composable
private fun CertificatesTab(
    certList: List<AdmWdlRecord>,
    isLoading: Boolean,
    hasQuery: Boolean,
    onCertClick: (AdmWdlRecord) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Emerald)
            }
            !hasQuery -> {
                // Cert type grid — before searching
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.WorkspacePremium, null, tint = Emerald, modifier = Modifier.size(52.dp))
                        Text("Search a student above\nto generate a certificate", color = LocalTextDim.current, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        // Certificate type pills (decorative)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SLC" to Rose, "CHARACTER" to Violet, "SPORT" to Emerald, "BISP" to Cyan).forEach { (label, col) ->
                                Box(Modifier.background(col.copy(0.12f), RoundedCornerShape(20.dp)).border(1.dp, col.copy(0.3f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(label, color = col, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
            certList.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null, tint = LocalTextDim.current, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No students found", color = LocalTextDim.current, fontSize = 13.sp)
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Text("${certList.size} students found", color = LocalTextDim.current, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp)) }
                items(certList, key = { it.idInt }) { stu ->
                    CertStudentCard(stu, onCertClick)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun CertStudentCard(student: AdmWdlRecord, onCertClick: (AdmWdlRecord) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LocalDCard.current),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Emerald.copy(0.2f)),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Gold.copy(0.3f), LocalDCard2.current))), contentAlignment = Alignment.Center) {
                Text(student.displayName.take(2).uppercase(), color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f)) {
                Text(student.displayName, color = LocalTextOn.current, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val so = if (student.gender?.lowercase() == "female") "D/O" else "S/O"
                Text("$so ${student.father_name ?: "—"}", color = LocalTextDim.current, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(student.displayClass, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (!student.roll_number.isNullOrEmpty()) Text("• Roll #${student.roll_number}", color = LocalTextDim.current, fontSize = 10.sp)
                }
            }
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Gold, Color(0xFFF59E0B)))).clickable { onCertClick(student) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Print, null, tint = Color.Black, modifier = Modifier.size(20.dp)) }
        }
    }
}

// ── Certificate Type Picker Dialog ────────────────────────────────────────────
private data class CertDesign(
    val id: String,
    val backendType: String,
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color,
    val btnLabel: String
)

@Composable
private fun CertPickerDialog(student: AdmWdlRecord, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var selectedType by remember { mutableStateOf<CertDesign?>(null) }

    val designs = listOf(
        CertDesign("slc", "slc", "SLC Certificate", "School Leaving certificate layout", Icons.Default.Description, Color(0xFF0EA5E9), "SELECT SLC"),
        CertDesign("character", "character", "Character Cert", "Student character and conduct profile", Icons.Default.Stars, Color(0xFFA855F7), "SELECT CHARACTER"),
        CertDesign("sport", "sport", "Sport Certificate", "Achievement in sports and activities", Icons.Default.WorkspacePremium, Color(0xFFD97706), "SELECT SPORT"),
        CertDesign("bisp", "bisp", "BISP Form", "Benazir Income Support Programme form", Icons.Default.Article, Color(0xFF10B981), "SELECT BISP")
    )

    if (selectedType == null) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LocalDCard2.current),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Certificate Categories", color = LocalTextOn.current, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("Select a category for ${student.displayName}", color = LocalTextDim.current, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Clear, null, tint = LocalTextDim.current)
                        }
                    }
                    Divider(color = LocalTextDim.current.copy(0.1f))

                    // Grid of designs
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 135.dp),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(designs) { design ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LocalDCard.current),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, design.color.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().clickable { selectedType = design }
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Icon Box
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(design.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(design.icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            design.title,
                                            color = LocalTextOn.current,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            design.desc,
                                            color = LocalTextDim.current,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp,
                                            minLines = 2,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    // Select Button exactly like screenshot: Cyan glow border
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFF06B6D4).copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                brush = Brush.horizontalGradient(
                                                    listOf(Color(0xFF06B6D4), Color(0xFF06B6D4).copy(alpha = 0.4f))
                                                ),
                                                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp)
                                            )
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            design.btnLabel,
                                            color = LocalTextOn.current,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Dialog(
            onDismissRequest = { selectedType = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            NativeCertificatePreview(
                student = student,
                design = selectedType!!,
                onPrint = { onSelect(selectedType!!.backendType) },
                onClose = { selectedType = null }
            )
        }
    }
}

@Composable
private fun NativeCertificatePreview(student: AdmWdlRecord, design: CertDesign, onPrint: () -> Unit, onClose: () -> Unit) {
    val beigeBg = Color(0xFFFDFBF7)
    val textNavy = Color(0xFF1E293B)
    val dividerColor = Color(0xFFCBD5E1)

    val title = when (design.backendType) {
        "slc" -> "SCHOOL LEAVING\nCERTIFICATE"
        "character" -> "CHARACTER\nCERTIFICATE"
        "sport" -> "SPORTS EXCELLENCE\nCERTIFICATE"
        "bisp", "professional" -> "EDUCATIONAL\nVERIFICATION FORM"
        else -> "OFFICIAL\nCERTIFICATE"
    }

    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)) {
        // The Certificate Paper
        Card(
            colors = CardDefaults.cardColors(containerColor = beigeBg),
            shape = RoundedCornerShape(4.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxSize().padding(top = 28.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
                // Header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Spacer(Modifier.weight(0.1f))
                    Text(title, color = textNavy, fontSize = 16.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Right, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(24.dp))

                // Metadata Fields Grid
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PrintField("STUDENT NAME", student.displayName)
                        val so = if (student.gender?.lowercase() == "female") "D/O NAME" else "S/O NAME"
                        PrintField(so, student.father_name ?: "—")
                        PrintField("DATE OF BIRTH", student.dob ?: "—")
                        PrintField("ADMISSION DATE", student.displayAdmDate.ifEmpty { "—" })
                        PrintField("WITHDRAWAL DATE", student.date_withdrawal ?: "—")
                        PrintField("CLASS ON ADMISSION", student.class_admission ?: "—")
                    }
                    Spacer(Modifier.width(24.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PrintField("ADMISSION NO", student.adm_no ?: "—")
                        PrintField("GENDER", student.gender ?: "—")
                        PrintField("DOB IN WORDS", "—")
                        PrintField("CLASS/SECTION", student.displayClass)
                        PrintField("DATE OF ADMISSION", student.displayAdmDate.ifEmpty { "—" })
                        PrintField("CLASS ON WITHDRAWAL", student.class_withdrawal ?: "—")
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Paragraph Text Content
                val heShe = if (student.gender?.lowercase() == "female") "She" else "He"
                val hisHer = if (student.gender?.lowercase() == "female") "her" else "his"

                when (design.backendType) {
                    "slc" -> {
                        // More grid fields for SLC
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PrintField("REMARKS/CONDUCT", "Satisfactory")
                                PrintField("REASON FOR LEAVING", "Completed Session")
                            }
                            Spacer(Modifier.width(24.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PrintField("SLC STATUS", student.slc_status ?: "—")
                                PrintField("TOTAL ATTENDANCE", "—")
                            }
                        }
                    }
                    "character" -> {
                        Text("TO WHOM IT MAY CONCERN,", color = textNavy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "This is to certify that ${student.displayName} has been a student of this school in Class ${student.displayClass}. During the period of study, I found $hisHer to be hardworking, honest, and regular in $hisHer studies. $heShe possesses a good moral character and always participated in co-curricular activities with zeal. I wish $hisHer every success in $hisHer future endeavors.",
                            color = textNavy, fontSize = 14.sp, lineHeight = 28.sp, textAlign = TextAlign.Justify
                        )
                    }
                    "sport" -> {
                        Text(
                            text = "This certificate is awarded to ${student.displayName} in recognition of $hisHer outstanding performance and dedication in the field of Sports. $heShe has actively participated in various athletic events representing the institution. We appreciate $hisHer commitment to physical excellence and teamwork.",
                            color = textNavy, fontSize = 14.sp, lineHeight = 28.sp, textAlign = TextAlign.Justify
                        )
                    }
                    "bisp" -> {
                        Box(Modifier.fillMaxWidth().background(Color(0xFF064E3B)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Text("BENAZIR INCOME SUPPORT PROGRAMME - EDUCATIONAL VERIFICATION", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("This is to certify that the following student is currently enrolled and attending classes regularly at this institution.", color = textNavy, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        // Table-like outline
                        Column(Modifier.border(1.dp, dividerColor)) {
                            GridRow("Student Name", student.displayName)
                            val soLabel = if (student.gender?.lowercase() == "female") "D/O Name" else "S/O Name"
                            GridRow(soLabel, student.father_name ?: "—")
                            GridRow("Roll Number / ID", student.roll_number ?: "—")
                            GridRow("Class / Section", student.displayClass)
                        }
                    }
                    else -> {
                        Text("Design layout template preview.", color = textNavy)
                    }
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(60.dp))
                // Signatures
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("CLASS INCHARGE", color = dividerColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text("OFFICE SUPERINTENDENT", color = dividerColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text("PRINCIPAL / SEAL", color = dividerColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // Floating Action Buttons (Top Center) exactly like screenshots
        Row(
            Modifier.align(Alignment.TopCenter).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E293B)),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Row(
                Modifier.clickable { onPrint() }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Print, null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(16.dp))
                Text("Generate Official Print", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.width(1.dp).height(36.dp).background(Color.White.copy(0.1f)))
            Row(
                Modifier.clickable { onClose() }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                Text("Close Document", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PrintField(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(label, color = Color(0xFF0F172A), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.40f).padding(bottom = 2.dp))
        Column(Modifier.weight(0.60f)) {
            Text(value, color = Color(0xFF0F172A), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Divider(color = Color(0xFFCBD5E1), thickness = 1.dp, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun GridRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFCBD5E1))) {
        Text(label, color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.4f).padding(8.dp))
        Box(Modifier.width(1.dp).height(IntrinsicSize.Min).background(Color(0xFFCBD5E1)))
        Text(value, color = Color(0xFF0F172A), fontSize = 11.sp, modifier = Modifier.weight(0.6f).padding(8.dp))
    }
}

private fun printCertificateHtml(context: android.content.Context, student: AdmWdlRecord, type: String, instName: String, logoUrl: String) {
    val webView = android.webkit.WebView(context)
    val title = when (type) {
        "slc" -> "SCHOOL LEAVING CERTIFICATE"
        "character" -> "CHARACTER CERTIFICATE"
        "sport" -> "SPORTS EXCELLENCE CERTIFICATE"
        "bisp", "professional" -> "EDUCATIONAL VERIFICATION FORM"
        else -> "OFFICIAL CERTIFICATE"
    }

    val so = if (student.gender?.lowercase() == "female") "D/O" else "S/O"
    val heShe = if (student.gender?.lowercase() == "female") "She" else "He"
    val hisHer = if (student.gender?.lowercase() == "female") "her" else "his"

    // DEBUG: always show logoUrl value on certificate so we can confirm path
    val logoDebug = "<div style='font-size:9px; color:#888; word-break:break-all;'>Logo: $logoUrl</div>"
    val logoHtml = if (logoUrl.isNotEmpty())
        """<img src="$logoUrl" style="max-height:90px; max-width:130px; object-fit:contain;">$logoDebug"""
    else
        """<div style="font-size:10px; color:gray;">No logo configured</div>"""
    val headerHtml = """
        <table style="width:100%; border-collapse:collapse; border:none; margin-bottom:16px;">
            <tr>
                <td style="border:none; text-align:left; vertical-align:middle; width:70%;">
                    <div style="font-size:22px; font-weight:bold; color:#1E293B;">${instName.uppercase()}</div>
                    <div style="font-size:14px; color:#64748B; margin-top:4px;">$title</div>
                </td>
                <td style="border:none; text-align:right; vertical-align:middle; width:30%;">$logoHtml</td>
            </tr>
        </table>
        <hr style="border:none; border-top:2px solid #1E293B; margin-bottom:20px;">
    """

    val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=CERT-${student.adm_no ?: student.idInt}"

    fun row(l1: String, v1: String, l2: String = "", v2: String = "") = """
        <tr>
            <td style="width:22%; font-size:10px; font-weight:bold; color:#64748B; padding:8px 6px; border:1px solid #E2E8F0; background:#F8FAFC;">$l1</td>
            <td style="width:28%; font-size:13px; padding:8px 6px; border:1px solid #E2E8F0;">$v1</td>
            <td style="width:22%; font-size:10px; font-weight:bold; color:#64748B; padding:8px 6px; border:1px solid #E2E8F0; background:#F8FAFC;">$l2</td>
            <td style="width:28%; font-size:13px; padding:8px 6px; border:1px solid #E2E8F0;">$v2</td>
        </tr>"""

    val genericTable = if (type != "bisp" && type != "professional") """
        <table style="width:100%; border-collapse:collapse; margin-bottom:20px;">
            ${row("STUDENT NAME", student.displayName, "ADMISSION NO", student.adm_no ?: "-")}
            ${row("$so NAME", student.father_name ?: "-", "GENDER", student.gender ?: "-")}
            ${row("DATE OF BIRTH", student.dob ?: "-", "DOB IN WORDS", "-")}
            ${row("ADMISSION DATE", student.displayAdmDate.ifEmpty { "-" }, "CLASS / SECTION", student.displayClass)}
            ${row("WITHDRAWAL DATE", student.date_withdrawal ?: "-", "DATE OF ADMISSION", student.displayAdmDate.ifEmpty { "-" })}
            ${row("CLASS ON ADMISSION", student.class_admission ?: "-", "CLASS ON WITHDRAWAL", student.class_withdrawal ?: "-")}
            ${if (type == "slc") row("REMARKS/CONDUCT", "Satisfactory", "SLC STATUS", student.slc_status ?: "-") else ""}
            ${if (type == "slc") row("REASON FOR LEAVING", "Completed Session", "TOTAL ATTENDANCE", "-") else ""}
        </table>
    """ else ""

    val specificContent = when (type) {
        "character" -> """
            <br><p style="font-weight:bold; font-size:15px;">TO WHOM IT MAY CONCERN,</p>
            <p style="text-align:justify; line-height:1.9; font-size:14px;">This is to certify that <b>${student.displayName}</b> has been a student of this school in Class <b>${student.displayClass}</b>. During the period of study, I found $hisHer to be hardworking, honest, and regular in $hisHer studies. $heShe possesses a good moral character and always participated in co-curricular activities with zeal. I wish $hisHer every success in $hisHer future endeavors.</p>
        """
        "sport" -> """
            <p style="text-align:justify; line-height:1.9; margin-top:20px; font-size:14px;">This certificate is awarded to <b>${student.displayName}</b> in recognition of $hisHer outstanding performance and dedication in the field of Sports. $heShe has actively participated in various athletic events representing the institution. We appreciate $hisHer commitment to physical excellence and teamwork.</p>
        """
        else -> ""
    }

    val bispTable = if (type == "bisp" || type == "professional") """
        <div style="background:#064E3B; color:white; text-align:center; padding:12px; font-weight:bold; font-size:13px; margin:20px 0;">
            BENAZIR INCOME SUPPORT PROGRAMME — EDUCATIONAL VERIFICATION
        </div>
        <p style="font-size:13px;">This is to certify that the following student is currently enrolled and attending classes regularly at this institution.</p>
        <table style="width:100%; border-collapse:collapse; margin-top:10px;">
            ${row("Student Name", student.displayName, "", "")}
            ${row("$so Name", student.father_name ?: "-", "", "")}
            ${row("Roll Number", student.roll_number ?: "-", "", "")}
            ${row("Class / Section", student.displayClass, "", "")}
        </table>
    """ else ""

    val html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="UTF-8">
        <style>
            @page { size: A4; margin: 15mm 20mm; }
            * { box-sizing: border-box; }
            body { font-family: Arial, sans-serif; background:#fff; color:#1E293B; margin:0; padding:0; width:100%; }
            .footer { display:flex; justify-content:space-between; margin-top:60px; padding-top:16px; border-top:1px solid #CBD5E1; font-size:12px; font-weight:bold; color:#64748B; }
        </style>
        </head>
        <body>
            $headerHtml
            $genericTable
            $specificContent
            $bispTable
            <div class="footer">
                <div>CLASS INCHARGE</div>
                <div>OFFICE SUPERINTENDENT</div>
                <div>PRINCIPAL / SEAL</div>
            </div>
            <div style="text-align:right; margin-top:10px;">
                <img src="$qrUrl" style="width:80px; height:80px;" alt="QR">
            </div>
        </body>
        </html>
    """.trimIndent()

    webView.settings.apply {
        javaScriptEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = true
        setSupportZoom(false)
    }
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView, url: String) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                    val jobName = "${student.displayName}_Certificate"
                    val adapter = view.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, adapter, android.print.PrintAttributes.Builder().build())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 1200)
        }
    }
    webView.loadDataWithBaseURL(com.example.wantuch.BASE_URL, html, "text/html", "UTF-8", null)
}


// ── Shared small components ───────────────────────────────────────────────────
@Composable
private fun StatusBadge(label: String, color: Color) {
    Box(Modifier.background(color.copy(0.15f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ActionBtn(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(0.12f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
    }
}

// ── Add Fresh Dialog ──────────────────────────────────────────────────────────
@Composable
private fun AddFreshDialog(record: AdmWdlRecord?, viewModel: WantuchViewModel, onDismiss: () -> Unit, onSave: (Map<String, String>) -> Unit) {
    var admNo    by remember { mutableStateOf(record?.adm_no ?: "") }
    var name     by remember { mutableStateOf(if (record?.displayName == "N/A") "" else record?.displayName ?: "") }
    var fName    by remember { mutableStateOf(record?.father_name ?: "") }
    var dob      by remember { mutableStateOf(record?.dob ?: "") }
    var gender   by remember { mutableStateOf(record?.gender ?: "Male") }
    var pCnic    by remember { mutableStateOf(record?.parent_cnic_no ?: "") }
    var admDate  by remember { mutableStateOf(record?.displayAdmDate ?: "") }
    var clsName  by remember { mutableStateOf(if (record?.displayClass == "N/A") "" else record?.displayClass ?: "") }
    
    var isLoading by remember { mutableStateOf(record != null) }

    LaunchedEffect(record) {
        if (record != null && record.idInt > 0) {
            try {
                viewModel.fetchStudentProfileWithCallback(record.idInt) { profile ->
                    if (profile?.basic != null) {
                        val basic = profile.basic
                        admNo = (basic["adm_no"] as? String)?.takeIf { it.isNotEmpty() } ?: (basic["roll_number"] as? String) ?: admNo
                        name = basic["full_name"] as? String ?: name
                        fName = basic["father_name"] as? String ?: fName
                        dob = basic["dob"] as? String ?: dob
                        
                        val fetchedGender = basic["gender"] as? String
                        if (!fetchedGender.isNullOrEmpty()) gender = fetchedGender
                        else if (gender.isBlank()) gender = "Male" // prevent empty lock
                        
                        val rawParentCnic = (basic["father_cnic"] as? String)?.takeIf { it.isNotBlank() } 
                            ?: (basic["parent_cnic_no"] as? String)?.takeIf { it.isNotBlank() } 
                            ?: (basic["cnic"] as? String)
                        pCnic = rawParentCnic ?: pCnic
                        
                        admDate = basic["date_of_admission"] as? String ?: basic["admission_date"] as? String ?: admDate
                        clsName = basic["class_name"] as? String ?: clsName
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = LocalDCard2.current), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PersonAdd, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                    Text(if (record == null) "Add Fresh Student" else "Edit Student", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    if (isLoading) {
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(Modifier.size(16.dp), color = Gold, strokeWidth = 2.dp)
                    }
                }
                HorizontalDivider(color = Gold.copy(0.2f))
                AdmField("Admission No *", admNo, Icons.Default.Badge) { admNo = it }
                AdmField("Full Name *", name, Icons.Default.Person) { name = it }
                AdmField("Father Name", fName, Icons.Default.FamilyRestroom) { fName = it }
                AdmField("Class *  (e.g. KG)", clsName, Icons.Default.Class) { clsName = it }
                AdmField("Admission Date (YYYY-MM-DD)", admDate, Icons.Default.CalendarToday) { admDate = it }
                AdmField("Date of Birth (YYYY-MM-DD)", dob, Icons.Default.Cake) { dob = it }
                AdmField("Parent CNIC", pCnic, Icons.Default.CreditCard) { pCnic = it }

                Text("Gender:", color = LocalTextDim.current, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Male","Female","Other").forEach { g ->
                        FilterChip(g == gender, { gender = g }, label = { Text(g, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold, selectedLabelColor = Color.Black, containerColor = LocalDCard.current, labelColor = LocalTextDim.current))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val context = LocalContext.current
                    OutlinedButton(onDismiss, Modifier.weight(1f)) { Text("Cancel", color = LocalTextDim.current) }
                    Button(onClick = {
                        if (admNo.isBlank() || name.isBlank() || clsName.isBlank()) {
                            Toast.makeText(context, "Adm No, Name, and Class are required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onSave(buildMap {
                            val idVal = record?.idInt ?: 0
                            if (idVal > 0) put("id", idVal.toString())
                            put("adm_no", admNo); put("name", name); put("father_name", fName)
                            put("dob", dob); put("gender", gender); put("parent_cnic_no", pCnic)
                            put("adm_date", admDate); put("class_name", clsName)
                        })
                    }, colors = ButtonDefaults.buttonColors(containerColor = Gold), modifier = Modifier.weight(1f)) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Add Old Dialog ────────────────────────────────────────────────────────────
@Composable
private fun AddOldDialog(record: AdmWdlRecord?, onDismiss: () -> Unit, onSave: (Map<String, String>) -> Unit) {
    var admNo     by remember { mutableStateOf(record?.adm_no ?: "") }
    var name      by remember { mutableStateOf(if (record?.displayName == "N/A") "" else record?.displayName ?: "") }
    var fName     by remember { mutableStateOf(record?.father_name ?: "") }
    var dob       by remember { mutableStateOf(record?.dob ?: "") }
    var admDate   by remember { mutableStateOf(record?.date_admission ?: "") }
    var withDate  by remember { mutableStateOf(record?.date_withdrawal ?: "") }
    var classAdm  by remember { mutableStateOf(record?.class_admission ?: "") }
    var classWdl  by remember { mutableStateOf(record?.class_withdrawal ?: "") }
    var slcStatus by remember { mutableStateOf(record?.slc_status ?: "Pending") }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = LocalDCard2.current), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Violet), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Archive, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text(if (record == null) "Add Old Record" else "Edit Old Record", color = Violet, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
                Divider(color = Violet.copy(0.2f))
                AdmField("Admission No *", admNo, Icons.Default.Badge) { admNo = it }
                AdmField("Student Name *", name, Icons.Default.Person) { name = it }
                AdmField("Father Name", fName, Icons.Default.FamilyRestroom) { fName = it }
                AdmField("DOB (YYYY-MM-DD)", dob, Icons.Default.Cake) { dob = it }
                AdmField("Date of Admission", admDate, Icons.Default.Login) { admDate = it }
                AdmField("Date of Withdrawal", withDate, Icons.Default.Logout) { withDate = it }
                AdmField("Class at Admission", classAdm, Icons.Default.Class) { classAdm = it }
                AdmField("Class at Withdrawal", classWdl, Icons.Default.School) { classWdl = it }
                Text("SLC Status:", color = LocalTextDim.current, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Pending","Issued","Not Required").forEach { s ->
                        FilterChip(s == slcStatus, { slcStatus = s }, label = { Text(s, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (s == "Issued") Emerald else Gold,
                                selectedLabelColor = Color.Black, containerColor = LocalDCard.current, labelColor = LocalTextDim.current))
                    }
                }
                val idVal = record?.idInt ?: 0
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onDismiss, Modifier.weight(1f)) { Text("Cancel", color = LocalTextDim.current) }
                    Button(onClick = {
                        if (admNo.isBlank() || name.isBlank()) return@Button
                        onSave(buildMap {
                            put("adm_no", admNo); put("name", name); put("father_name", fName)
                            put("dob", dob); put("date_admission", admDate); put("date_withdrawal", withDate)
                            put("class_admission", classAdm); put("class_withdrawal", classWdl)
                            put("slc_status", slcStatus)
                            if (idVal > 0) put("id", idVal.toString())
                        })
                    }, colors = ButtonDefaults.buttonColors(containerColor = Violet), modifier = Modifier.weight(1f)) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Withdraw Dialog ───────────────────────────────────────────────────────────
@Composable
private fun WithdrawDialog(
    record: AdmWdlRecord, onDismiss: () -> Unit,
    onWithdraw: (Int, String, String, String, String) -> Unit
) {
    var withDate  by remember { mutableStateOf("") }
    var withClass by remember { mutableStateOf(record.displayClass) }
    var slc       by remember { mutableStateOf("Pending") }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = LocalDCard2.current), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Rose), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ExitToApp, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("WITHDRAW STUDENT", color = Rose, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text(record.displayName, color = LocalTextDim.current, fontSize = 11.sp)
                    }
                }
                Divider(color = Rose.copy(0.2f))
                AdmField("Withdrawal Date * (YYYY-MM-DD)", withDate, Icons.Default.CalendarToday) { withDate = it }
                AdmField("Final Class", withClass, Icons.Default.School) { withClass = it }
                Text("SLC Status:", color = LocalTextDim.current, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Pending","Issued","Not Required").forEach { s ->
                        FilterChip(s == slc, { slc = s }, label = { Text(s, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (s == "Issued") Emerald else Gold, selectedLabelColor = Color.Black, containerColor = LocalDCard.current, labelColor = LocalTextDim.current))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onDismiss, Modifier.weight(1f)) { Text("Cancel", color = LocalTextDim.current) }
                    Button(onClick = {
                        if (withDate.isBlank()) return@Button
                        onWithdraw(record.idInt, record.adm_no ?: "", withDate, withClass, slc)
                    }, colors = ButtonDefaults.buttonColors(containerColor = Rose), modifier = Modifier.weight(1f)) {
                        Text("Withdraw", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Shared Field ──────────────────────────────────────────────────────────────
@Composable
private fun AdmField(label: String, value: String, icon: ImageVector, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 10.sp) },
        leadingIcon = { Icon(icon, null, tint = Gold, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold, unfocusedBorderColor = LocalTextDim.current.copy(0.3f),
            focusedTextColor = LocalTextOn.current, unfocusedTextColor = LocalTextOn.current,
            focusedLabelColor = Gold, unfocusedLabelColor = LocalTextDim.current,
            cursorColor = Gold,
            focusedContainerColor = LocalDCard.current, unfocusedContainerColor = LocalDCard.current
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = LocalTextOn.current)
    )
}
