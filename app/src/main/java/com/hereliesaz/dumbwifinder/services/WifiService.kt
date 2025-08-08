package com.hereliesaz.dumbwifinder.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WifiService(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    suspend fun scanForWifiNetworks(): List<ScanResult> {
        // Note: For newer Android versions, a more modern approach using NetworkManager is recommended.
        // This implementation uses the older WifiManager for simplicity and to match the original code.
        return if (wifiManager.isWifiEnabled) {
            suspendCoroutine { continuation ->
                val wifiScanReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                            context.unregisterReceiver(this)
                            continuation.resume(wifiManager.scanResults)
                        }
                    }
                }

                val intentFilter = IntentFilter()
                intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                context.registerReceiver(wifiScanReceiver, intentFilter)

                @Suppress("DEPRECATION")
                val success = wifiManager.startScan()
                if (!success) {
                    context.unregisterReceiver(wifiScanReceiver)
                    continuation.resume(emptyList())
                }
            }
        } else {
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    fun connectToWifi(ssid: String, password: String): Boolean {
        val wifiConfig = WifiConfiguration()
        wifiConfig.SSID = "\"$ssid\""
        wifiConfig.preSharedKey = "\"$password\""

        val netId = wifiManager.addNetwork(wifiConfig)
        if (netId == -1) {
            return false
        }

        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        return wifiManager.reconnect()
    }
}