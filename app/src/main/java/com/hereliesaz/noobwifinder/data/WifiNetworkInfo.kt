package com.hereliesaz.noobwifinder.data

import org.osmdroid.util.GeoPoint

data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,
    val securityType: String,
    val location: GeoPoint,
    var status: CrackingStatus = CrackingStatus.PENDING,
    var password: String? = null
)

enum class CrackingStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAIL
}
