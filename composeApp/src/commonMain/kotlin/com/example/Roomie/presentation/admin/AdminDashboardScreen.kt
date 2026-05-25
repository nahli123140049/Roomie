package com.example.Roomie.presentation.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.Roomie.domain.model.*
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentView by remember { mutableStateOf("OVERVIEW") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (currentView == "OVERVIEW") "DASHBOARD HUB" else currentView,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentView == "OVERVIEW") onBack() else currentView = "OVERVIEW"
                    }) {
                        Icon(
                            if (currentView == "OVERVIEW") Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is AdminUiState.Loading -> CircularProgressIndicator(
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
                is AdminUiState.Success -> {
                    AnimatedContent(
                        targetState = currentView,
                        transitionSpec = {
                            fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally()
                        }
                    ) { view ->
                        when (view) {
                            "OVERVIEW" -> AdminOverviewHub(
                                state = state,
                                onNavigate = { currentView = it }
                            )
                            "LAPORAN" -> ReportManagementTab(state, viewModel, onActionSuccess = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            })
                            "APPROVAL" -> ApprovalTab(state, viewModel, onActionSuccess = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            })
                            "KONTROL" -> SystemControlTab(viewModel, onActionSuccess = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            })
                            "HISTORY" -> HistoryTab(state)
                        }
                    }
                }
                is AdminUiState.Error -> Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun AdminOverviewHub(
    state: AdminUiState.Success,
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. System Health Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SYSTEM STATUS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(120.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            strokeWidth = 12.dp
                        )
                        CircularProgressIndicator(
                            progress = { 0.85f }, // Mock health data
                            modifier = Modifier.size(120.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 12.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("85%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            Text("Operational", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 2. Menu Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("MANAGEMENT HUB", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HubTile(
                        title = "Approval",
                        count = state.filteredBookings.count { it.status == BookingStatus.PENDING }.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("APPROVAL") }
                    )
                    HubTile(
                        title = "Laporan",
                        count = state.pendingCount.toString(),
                        icon = Icons.Default.Warning,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("LAPORAN") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HubTile(
                        title = "System",
                        count = "PRO",
                        icon = Icons.Default.SettingsSuggest,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("KONTROL") }
                    )
                    HubTile(
                        title = "Audit Log",
                        count = state.auditLogs.size.toString(),
                        icon = Icons.Default.History,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("HISTORY") }
                    )
                }
            }
        }
    }
}

@Composable
fun HubTile(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ApprovalTab(state: AdminUiState.Success, viewModel: AdminViewModel, onActionSuccess: (String) -> Unit) {
    val pendingBookings = state.allBookings.filter { it.status == BookingStatus.PENDING }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Pengajuan Menunggu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Review permohonan peminjaman ruangan mahasiwa", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
        }

        if (pendingBookings.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Semua pengajuan sudah diproses", color = Color.Gray)
                }
            }
        } else {
            items(pendingBookings) { booking ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(booking.subject ?: "Tanpa Judul", fontWeight = FontWeight.Bold)
                                Text("Ruang ${booking.roomName}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.alpha(0.1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { viewModel.approveBooking(booking); onActionSuccess("Berhasil disetujui") },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) { Text("SETUJUI", fontWeight = FontWeight.Bold) }
                            
                            OutlinedButton(
                                onClick = { viewModel.rejectBooking(booking); onActionSuccess("Berhasil ditolak") },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("TOLAK", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemControlTab(
    viewModel: AdminViewModel,
    onActionSuccess: (String) -> Unit
) {
    var announceTitle by remember { mutableStateOf("") }
    var announceMsg by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("System Control", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Broadcast Pengumuman", fontWeight = FontWeight.Bold)
                }
                
                OutlinedTextField(
                    value = announceTitle, 
                    onValueChange = { announceTitle = it }, 
                    label = { Text("Judul") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = announceMsg, 
                    onValueChange = { announceMsg = it }, 
                    label = { Text("Pesan") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3
                )
                
                Button(
                    onClick = {
                        viewModel.broadcastMessage(announceTitle, announceMsg)
                        onActionSuccess("Pengumuman Terkirim!")
                        announceTitle = ""; announceMsg = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = announceTitle.isNotBlank() && announceMsg.isNotBlank()
                ) {
                    Text("KIRIM BROADCAST", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun ReportManagementTab(state: AdminUiState.Success, viewModel: AdminViewModel, onActionSuccess: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Laporan Kerusakan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
        }

        items(state.filteredReports) { report ->
            ReportAdminCard(report, viewModel, onActionSuccess)
        }
    }
}

@Composable
fun ReportAdminCard(report: Report, viewModel: AdminViewModel, onActionSuccess: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape) {
                    Text(report.category, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text(report.status.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
            }
            Text(report.description, fontWeight = FontWeight.Bold)
            
            if (report.imageUrl != null) {
                AsyncImage(
                    model = report.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (report.status == ReportStatus.PENDING) {
                    Button(
                        onClick = { viewModel.updateReportStatus(report.id, ReportStatus.IN_PROGRESS); onActionSuccess("Diproses") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("PROSES") }
                }
                if (report.status != ReportStatus.DONE) {
                    OutlinedButton(
                        onClick = { viewModel.updateReportStatus(report.id, ReportStatus.DONE); onActionSuccess("Selesai") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("SELESAI") }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(state: AdminUiState.Success) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Riwayat Aktivitas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
        
        items(state.auditLogs) { log ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(16.dp))
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(log.action, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Oleh: ${log.userName}", style = MaterialTheme.typography.bodySmall)
                        log.details?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                    }
                }
            }
        }
    }
}
