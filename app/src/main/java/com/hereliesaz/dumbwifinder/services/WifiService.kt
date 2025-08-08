package com.hereliesaz.dumbwifinder.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

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
    suspend fun connectToWifi(ssid: String, password: String): Boolean {
        // On modern Android versions (Q+), this will likely trigger a system dialog
        // for the user to confirm the connection, which makes automated password testing
        // impossible. The original app's "cracking" functionality is no longer
        // viable on modern Android.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            suspendCancellableCoroutine { continuation ->
                val networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        connectivityManager.bindProcessToNetwork(network)
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                        connectivityManager.unregisterNetworkCallback(this)
                    }

                    override fun onUnavailable() {
                        super.onUnavailable()
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                        connectivityManager.unregisterNetworkCallback(this)
                    }
                }
                connectivityManager.requestNetwork(request, networkCallback)

                continuation.invokeOnCancellation {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
            }
        } else {
            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = "\"$ssid\""
            wifiConfig.preSharedKey = "\"$password\""

            val netId = wifiManager.addNetwork(wifiConfig)
            if (netId == -1) {
                return false
            }

            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
        }
    }
}
