package com.parentalcontrol.parent.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Semua akses ke Firebase Realtime Database dipusatkan di sini
 * supaya ViewModel/UI tidak perlu tahu detail path & listener.
 */
class FirebaseRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    val currentParentId: String?
        get() = auth.currentUser?.uid

    // ---------- PAIRING ----------

    /**
     * Generate 6 digit session code, valid 5 menit.
     * Child app akan input code ini untuk pairing.
     */
    suspend fun generatePairingSession(): String {
        val parentId = currentParentId ?: throw IllegalStateException("Parent belum login")
        val code = (100000..999999).random().toString()
        val session = PairingSession(
            sessionCode = code,
            parentId = parentId,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 5 * 60 * 1000,
            used = false
        )
        db.child("pairing_sessions").child(code).setValue(session).await()
        return code
    }

    /** Dengarkan apakah session code sudah dipakai (child berhasil pairing) */
    fun listenPairingStatus(sessionCode: String): Flow<Boolean> = callbackFlow {
        val ref = db.child("pairing_sessions").child(sessionCode).child("used")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Boolean::class.java) ?: false)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ---------- DAFTAR ANAK ----------

    /** Realtime list semua anak yang terdaftar di bawah parent ini */
    fun listenChildren(): Flow<List<ChildDevice>> = callbackFlow {
        val parentId = currentParentId
        if (parentId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val childIdsRef = db.child("parents").child(parentId).child("children")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { it.key }
                if (ids.isEmpty()) {
                    trySend(emptyList())
                    return
                }
                // Ambil detail tiap device dari /devices/{childId}
                val devicesRef = db.child("devices")
                devicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(devicesSnap: DataSnapshot) {
                        val result = ids.mapNotNull { id ->
                            devicesSnap.child(id).getValue(ChildDevice::class.java)?.copy(childId = id)
                        }
                        trySend(result)
                    }
                    override fun onCancelled(error: DatabaseError) { close(error.toException()) }
                })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        childIdsRef.addValueEventListener(listener)
        awaitClose { childIdsRef.removeEventListener(listener) }
    }

    /** Listen satu device spesifik secara realtime (dipakai di detail screen) */
    fun listenChildDevice(childId: String): Flow<ChildDevice?> = callbackFlow {
        val ref = db.child("devices").child(childId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(ChildDevice::class.java)?.copy(childId = childId))
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ---------- KONTROL ----------

    suspend fun sendCommand(childId: String, type: String, payload: String = "") {
        val command = DeviceCommand(type = type, payload = payload)
        db.child("devices").child(childId).child("commands").push().setValue(command).await()
    }

    suspend fun lockDevice(childId: String) {
        db.child("devices").child(childId).child("status").setValue("locked").await()
        sendCommand(childId, "LOCK")
    }

    suspend fun unlockDevice(childId: String) {
        db.child("devices").child(childId).child("status").setValue("active").await()
        sendCommand(childId, "UNLOCK")
    }

    suspend fun setDailyLimit(childId: String, minutes: Int) {
        db.child("devices").child(childId).child("dailyLimitMinutes").setValue(minutes).await()
        sendCommand(childId, "SET_LIMIT", minutes.toString())
    }

    suspend fun toggleAppBlock(childId: String, packageName: String, blocked: Boolean) {
        db.child("devices").child(childId).child("blockedApps").child(packageName).setValue(blocked).await()
        sendCommand(childId, "TOGGLE_APP_BLOCK", packageName)
    }

    suspend fun requestLiveScreen(childId: String) {
        // Child app WAJIB menampilkan notifikasi persisten selama sesi ini berjalan
        // (ketentuan sistem Android untuk MediaProjection, tidak bisa dihilangkan/disembunyikan).
        sendCommand(childId, "REQUEST_SCREEN")
    }

    suspend fun stopLiveScreen(childId: String) {
        sendCommand(childId, "STOP_SCREEN")
    }

    suspend fun renamePairingForChild(childId: String, newName: String) {
        db.child("devices").child(childId).child("name").setValue(newName).await()
    }

    suspend fun removeChild(childId: String) {
        val parentId = currentParentId ?: return
        db.child("parents").child(parentId).child("children").child(childId).removeValue().await()
        db.child("devices").child(childId).removeValue().await()
    }
}
