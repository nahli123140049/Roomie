package com.example.Roomie.presentation.facility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Roomie.domain.model.Room
import com.example.Roomie.domain.model.RoomStatus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRoomScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    viewModel: SearchRoomViewModel = koinViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val minCap by viewModel.minCapacity.collectAsState()
    val maxCap by viewModel.maxCapacity.collectAsState()
    
    val today = remember { 
        val now = kotlinx.datetime.Clock.System.now()
        val local = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        local.date.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Ketik nomor ruangan (misal: 101)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Capacity Filter Chips (Nahli's UI Design)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text(
                        "Filter Kapasitas Kursi", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CapacityFilterChip(
                            label = "Semua", 
                            selected = minCap == 0 && maxCap > 100, 
                            onClick = { viewModel.onCapacityFilterChange(0, 1000) }
                        )
                        CapacityFilterChip(
                            label = "35-40", 
                            selected = minCap == 35 && maxCap == 40, 
                            onClick = { viewModel.onCapacityFilterChange(35, 40) }
                        )
                        CapacityFilterChip(
                            label = "41-50", 
                            selected = minCap == 41 && maxCap == 50, 
                            onClick = { viewModel.onCapacityFilterChange(41, 50) }
                        )
                        CapacityFilterChip(
                            label = "51-60", 
                            selected = minCap == 51, 
                            onClick = { viewModel.onCapacityFilterChange(51, 1000) }
                        )
                    }
                }
            }

            if (results.isEmpty() && query.isNotEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ruangan tidak ditemukan dengan kriteria ini")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results) { room ->
                        SearchResultItem(
                            room = room,
                            onClick = { onNavigateToDetail(room.id, today) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CapacityFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun SearchResultItem(room: Room, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        ListItem(
            headlineContent = { Text("Ruangan ${room.name}", fontWeight = FontWeight.Bold) },
            supportingContent = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Lantai ${room.floor} - GKU 2")
                    Spacer(Modifier.width(8.dp))
                    // Capacity Badge (Nahli's UI Touch)
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(room.capacity.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            trailingContent = {
                val color = when(room.status) {
                    RoomStatus.AVAILABLE -> Color(0xFF4CAF50)
                    RoomStatus.BOOKED -> Color(0xFFF44336)
                    RoomStatus.MAINTENANCE -> MaterialTheme.colorScheme.primary
                }
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = room.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
