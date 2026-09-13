package com.parentalcontrol.parent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage
import com.parentalcontrol.parent.data.ChildDevice
import com.parentalcontrol.parent.data.CommonSocialApps
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDetailScreen(
    child: ChildDevice,
    onBack: () -> Unit,
    onToggleLock: () -> Unit,
    onSetLimit: (Int) -> Unit,
    onToggleAppBlock: (String, Boolean) -> Unit,
    onRequestLiveScreen: () -> Unit,
    onStopLiveScreen: () -> Unit,
    onRemoveChild: () -> Unit
) {
    var limitSliderValue by remember(child.dailyLimitMinutes) {
        mutableFloatStateOf(child.dailyLimitMinutes.toFloat())
    }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    val isLocked = child.status == "locked"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(child.name.ifBlank { "Perangkat Anak" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showRemoveConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus perangkat")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Status & Lock ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Status Perangkat", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (isLocked) "Terkunci" else "Aktif",
                                color = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        Switch(checked = !isLocked, onCheckedChange = { onToggleLock() })
                    }
                }
            }

            // --- Batas Waktu Harian ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Batas Waktu Harian", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Terpakai: ${child.usedMinutesToday} menit dari ${limitSliderValue.toInt()} menit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = limitSliderValue,
                            onValueChange = { limitSliderValue = it },
                            onValueChangeFinished = { onSetLimit(limitSliderValue.toInt()) },
                            valueRange = 15f..600f,
                            steps = 38 // langkah 15 menit
                        )
                        Text(
                            "${limitSliderValue.toInt()} menit / hari",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // --- Live Screen ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Pantau Layar Langsung", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Anak akan melihat notifikasi bahwa layarnya sedang dipantau (ketentuan sistem Android).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        if (child.isScreenSharing) {
                            LiveScreenPreview(childId = child.childId)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = onStopLiveScreen,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.StopCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Hentikan Pemantauan")
                            }
                        } else {
                            Button(onClick = onRequestLiveScreen) {
                                Icon(Icons.Default.Visibility, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Mulai Pantau Layar")
                            }
                        }
                    }
                }
            }

            // --- Blokir Aplikasi Sosmed ---
            item {
                Text("Batasi Aplikasi", style = MaterialTheme.typography.titleMedium)
            }
            items(CommonSocialApps.list) { (packageName, appName) ->
                val isBlocked = child.blockedApps[packageName] == true
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(appName, style = MaterialTheme.typography.bodyLarge)
                            val usage = child.appUsage[packageName]
                            if (usage != null) {
                                Text(
                                    "${usage.minutesToday} menit hari ini",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isBlocked,
                            onCheckedChange = { onToggleAppBlock(packageName, it) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Hapus perangkat ini?") },
            text = { Text("Perangkat anak akan terputus dari akun kamu. Kamu bisa hubungkan ulang lewat kode pairing baru kapan saja.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveConfirm = false
                    onRemoveChild()
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Batal") }
            }
        )
    }
}

/**
 * Menampilkan gambar layar anak terbaru dari Firebase Storage.
 * Path: live_screens/{childId}/latest.jpg (di-overwrite tiap ~5 detik oleh Child App).
 * Pakai timestamp sebagai cache-buster supaya AsyncImage selalu ambil versi terbaru.
 */
@Composable
private fun LiveScreenPreview(childId: String) {
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        try {
            val ref = FirebaseStorage.getInstance().reference
                .child("live_screens").child(childId).child("latest.jpg")
            val url = ref.downloadUrl.await()
            imageUrl = "$url&t=${System.currentTimeMillis()}"
        } catch (e: Exception) {
            // Belum ada frame yang di-upload, atau gagal ambil URL — biarkan kosong dulu
        }
        kotlinx.coroutines.delay(5000)
        refreshTrigger++
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Layar anak saat ini",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

