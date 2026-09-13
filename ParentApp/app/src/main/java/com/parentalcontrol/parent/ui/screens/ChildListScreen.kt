package com.parentalcontrol.parent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.parentalcontrol.parent.data.ChildDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildListScreen(
    children: List<ChildDevice>,
    onChildClick: (ChildDevice) -> Unit,
    onAddChildClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Kontrol Anak") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddChildClick) {
                Icon(Icons.Default.Add, contentDescription = "Tambah anak")
            }
        }
    ) { padding ->
        if (children.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding), onAddChildClick = onAddChildClick)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(children, key = { it.childId }) { child ->
                    ChildCard(child = child, onClick = { onChildClick(child) })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onAddChildClick: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text("Belum ada perangkat anak terhubung", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tambahkan perangkat anak dengan membuat kode pairing, lalu masukkan kode itu di aplikasi anak.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddChildClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Hubungkan Perangkat Anak")
        }
    }
}

@Composable
private fun ChildCard(child: ChildDevice, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(child.name.ifBlank { "Perangkat Anak" }, style = MaterialTheme.typography.titleMedium)
                Text(child.deviceModel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { child.usagePercentage },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (child.usagePercentage >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${child.usedMinutesToday}m / ${child.dailyLimitMinutes}m hari ini",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge(isLocked = child.status == "locked")
        }
    }
}

@Composable
private fun StatusBadge(isLocked: Boolean) {
    val bg = if (isLocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = if (isLocked) "Terkunci" else "Aktif", modifier = Modifier.size(18.dp))
    }
}
