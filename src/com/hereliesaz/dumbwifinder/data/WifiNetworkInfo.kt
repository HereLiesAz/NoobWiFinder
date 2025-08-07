package com.hereliesaz.dumbwifinder.data

data class WifiNetworkInfo(
    val ssid: String,
    val signalStrength: Int,
    var status: CrackingStatus = CrackingStatus.PENDING,
    var password: String? = null
)

enum class CrackingStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAIL
}
