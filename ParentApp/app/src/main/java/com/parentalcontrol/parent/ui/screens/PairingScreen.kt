package com.parentalcontrol.parent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parentalcontrol.parent.viewmodel.PairingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    pairingState: PairingUiState,
    onStartPairing: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        onStartPairing()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hubungkan Perangkat Anak") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (pairingState) {
                is PairingUiState.Generating -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Membuat kode pairing...")
                }
                is PairingUiState.CodeReady -> {
                    Text("Masukkan kode ini di aplikasi anak:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = pairingState.code.chunked(3).joinToString(" "),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Kode berlaku 5 menit. Buka aplikasi anak, pilih 'Hubungkan ke Orang Tua', lalu masukkan kode ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = onStartPairing) {
                        Text("Buat Kode Baru")
                    }
                }
                is PairingUiState.Paired -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Berhasil terhubung!", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onDone) {
                        Text("Selesai")
                    }
                }
                is PairingUiState.Error -> {
                    Text(
                        "Gagal: ${pairingState.message}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onStartPairing) {
                        Text("Coba Lagi")
                    }
                }
                is PairingUiState.Idle -> {
                    Text("Menyiapkan...")
                }
            }
        }
    }
}
