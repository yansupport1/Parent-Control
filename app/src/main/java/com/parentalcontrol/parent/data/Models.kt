package com.parentalcontrol.parent.data

/**
 * Merepresentasikan satu perangkat anak yang sudah dipasangkan (paired)
 * dengan akun parent. Path Firebase: /devices/{childId}
 */
data class ChildDevice(
    val childId: String = "",
    val name: String = "",
    val deviceModel: String = "",
    val status: String = "active",          // "active" | "locked"
    val dailyLimitMinutes: Int = 120,
    val usedMinutesToday: Int = 0,
    val lastSeenAt: Long = 0L,
    val isScreenSharing: Boolean = false,
    val blockedApps: Map<String, Boolean> = emptyMap(),
    val appUsage: Map<String, AppUsageInfo> = emptyMap()
) {
    val remainingMinutes: Int
        get() = (dailyLimitMinutes - usedMinutesToday).coerceAtLeast(0)

    val usagePercentage: Float
        get() = if (dailyLimitMinutes == 0) 0f
                else (usedMinutesToday.toFloat() / dailyLimitMinutes).coerceIn(0f, 1f)
}

data class AppUsageInfo(
    val appName: String = "",
    val packageName: String = "",
    val minutesToday: Int = 0,
    val isBlocked: Boolean = false
)

/**
 * Command yang dikirim parent ke child lewat /devices/{childId}/commands
 * Child app akan listen node ini secara realtime.
 */
data class DeviceCommand(
    val type: String = "",   // "LOCK" | "UNLOCK" | "REQUEST_SCREEN" | "STOP_SCREEN" | "SET_LIMIT" | "TOGGLE_APP_BLOCK"
    val payload: String = "", // contoh: package name untuk TOGGLE_APP_BLOCK, atau angka menit untuk SET_LIMIT
    val timestamp: Long = System.currentTimeMillis()
)

/** Session pairing sementara, path: /pairing_sessions/{sessionCode} */
data class PairingSession(
    val sessionCode: String = "",
    val parentId: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val used: Boolean = false
)

/** Daftar aplikasi sosmed umum yang bisa langsung ditoggle dari dashboard */
object CommonSocialApps {
    val list = listOf(
        "com.instagram.android" to "Instagram",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.whatsapp" to "WhatsApp",
        "com.facebook.katana" to "Facebook",
        "com.google.android.youtube" to "YouTube",
        "com.twitter.android" to "X (Twitter)",
        "com.snapchat.android" to "Snapchat"
    )
}
