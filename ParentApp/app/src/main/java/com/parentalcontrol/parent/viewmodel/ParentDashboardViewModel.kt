package com.parentalcontrol.parent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.parent.data.ChildDevice
import com.parentalcontrol.parent.data.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PairingUiState {
    object Idle : PairingUiState()
    data class Generating(val dummy: Boolean = true) : PairingUiState()
    data class CodeReady(val code: String) : PairingUiState()
    object Paired : PairingUiState()
    data class Error(val message: String) : PairingUiState()
}

class ParentDashboardViewModel(
    private val repo: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _children = MutableStateFlow<List<ChildDevice>>(emptyList())
    val children: StateFlow<List<ChildDevice>> = _children.asStateFlow()

    private val _pairingState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val pairingState: StateFlow<PairingUiState> = _pairingState.asStateFlow()

    private val _selectedChild = MutableStateFlow<ChildDevice?>(null)
    val selectedChild: StateFlow<ChildDevice?> = _selectedChild.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        observeChildren()
    }

    private fun observeChildren() {
        viewModelScope.launch {
            try {
                repo.listenChildren().collect { list ->
                    _children.value = list
                    // sinkronkan selectedChild kalau sedang dibuka detailnya
                    _selectedChild.value?.let { current ->
                        _selectedChild.value = list.find { it.childId == current.childId }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat daftar anak: ${e.message}"
            }
        }
    }

    fun selectChild(childId: String) {
        viewModelScope.launch {
            try {
                repo.listenChildDevice(childId).collect { device ->
                    _selectedChild.value = device
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat detail perangkat: ${e.message}"
            }
        }
    }

    fun clearSelectedChild() {
        _selectedChild.value = null
    }

    fun startPairing() {
        viewModelScope.launch {
            _pairingState.value = PairingUiState.Generating()
            try {
                val code = repo.generatePairingSession()
                _pairingState.value = PairingUiState.CodeReady(code)
                repo.listenPairingStatus(code).collect { used ->
                    if (used) {
                        _pairingState.value = PairingUiState.Paired
                    }
                }
            } catch (e: Exception) {
                _pairingState.value = PairingUiState.Error(e.message ?: "Gagal membuat kode pairing")
            }
        }
    }

    fun resetPairingState() {
        _pairingState.value = PairingUiState.Idle
    }

    fun toggleLock(childId: String, currentlyLocked: Boolean) {
        viewModelScope.launch {
            try {
                if (currentlyLocked) repo.unlockDevice(childId) else repo.lockDevice(childId)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengubah status kunci: ${e.message}"
            }
        }
    }

    fun setDailyLimit(childId: String, minutes: Int) {
        viewModelScope.launch {
            try {
                repo.setDailyLimit(childId, minutes)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengatur batas waktu: ${e.message}"
            }
        }
    }

    fun toggleAppBlock(childId: String, packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            try {
                repo.toggleAppBlock(childId, packageName, blocked)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengubah status blokir aplikasi: ${e.message}"
            }
        }
    }

    fun requestLiveScreen(childId: String) {
        viewModelScope.launch {
            try {
                repo.requestLiveScreen(childId)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memulai live screen: ${e.message}"
            }
        }
    }

    fun stopLiveScreen(childId: String) {
        viewModelScope.launch {
            try {
                repo.stopLiveScreen(childId)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghentikan live screen: ${e.message}"
            }
        }
    }

    fun removeChild(childId: String) {
        viewModelScope.launch {
            try {
                repo.removeChild(childId)
                if (_selectedChild.value?.childId == childId) _selectedChild.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus perangkat: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
