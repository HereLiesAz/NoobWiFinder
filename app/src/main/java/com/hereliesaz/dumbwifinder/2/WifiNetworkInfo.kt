package com.hereliesaz.dumbwifinder.`2`

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
