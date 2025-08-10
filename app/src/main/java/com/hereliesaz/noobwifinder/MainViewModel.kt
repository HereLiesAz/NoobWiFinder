package com.hereliesaz.noobwifinder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hereliesaz.noobwifinder.data.CrackingStatus
import com.hereliesaz.noobwifinder.data.WifiNetworkInfo
import com.hereliesaz.noobwifinder.services.LocationService
import com.hereliesaz.noobwifinder.services.WifiScanResult
import com.hereliesaz.noobwifinder.services.WifiService
import android.location.Geocoder
import android.util.Log
import com.hereliesaz.noobwifinder.utils.PasswordGenerator
import kotlinx.coroutines.*
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.io.IOException
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _wifiList = MutableLiveData<List<WifiNetworkInfo>>()
    val wifiList: LiveData<List<WifiNetworkInfo>> = _wifiList

    private val _passwordList = MutableLiveData<List<String>>()
    val passwordList: LiveData<List<String>> = _passwordList

    private val _logMessages = MutableLiveData<String>()
    val logMessages: LiveData<String> = _logMessages

    private val _isCracking = MutableLiveData<Boolean>(false)
    val isCracking: LiveData<Boolean> = _isCracking

    private var crackingJob: Job? = null
    private var fetchAddressesJob: Job? = null

    private val locationService = LocationService(application)
    private val wifiService = WifiService(application)

    fun startStopCracking() {
        if (isCracking.value == true) {
            stopCracking()
        } else {
            startCracking()
        }
    }

    private fun startCracking() {
        _isCracking.value = true
        crackingJob = viewModelScope.launch {
            _logMessages.postValue("Starting cracking process...")
            _logMessages.postValue("Attempting to get device location...")
            val location = locationService.getCurrentLocation()
            if (location == null) {
                _logMessages.postValue("ERROR: Could not get device location. Aborting.")
                _isCracking.postValue(false)
                return@launch
            }
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            _logMessages.postValue("Location acquired: ${geoPoint.latitude}, ${geoPoint.longitude}")

            _logMessages.postValue("Scanning for nearby Wi-Fi networks...")
            when (val scanResult = wifiService.scanForWifiNetworks()) {
                is WifiScanResult.Success -> {
                    _logMessages.postValue("Found ${scanResult.results.size} Wi-Fi networks.")
                    val wifiNetworkInfos = scanResult.results.map {
                        WifiNetworkInfo(
                            ssid = it.SSID,
                            bssid = it.BSSID,
                            signalStrength = it.level,
                            securityType = it.capabilities,
                            location = geoPoint
                        )
                    }
                    _wifiList.postValue(wifiNetworkInfos)

                    for (networkInfo in wifiNetworkInfos) {
                        if (crackingJob?.isCancelled == true) break
                        _logMessages.postValue("--- Starting attack on ${networkInfo.ssid} ---")
                        networkInfo.status = CrackingStatus.IN_PROGRESS
                        _wifiList.postValue(wifiNetworkInfos)

                        _logMessages.postValue("Generating password dictionary for ${networkInfo.ssid}...")
                        val passwords = PasswordGenerator.generatePasswords(networkInfo.ssid)
                        _passwordList.postValue(passwords)
                        _logMessages.postValue("Password dictionary generated with ${passwords.size} candidates.")

                        for ((index, password) in passwords.withIndex()) {
                            if (crackingJob?.isCancelled == true) break
                            _logMessages.postValue("Testing password ${index + 1}/${passwords.size}: '$password'")
                            networkInfo.password = password
                            _wifiList.postValue(wifiNetworkInfos)
                            delay(100) // Simulate trying a password
                        }

                        if (crackingJob?.isCancelled != true) {
                            _logMessages.postValue("Attack on ${networkInfo.ssid} finished. No password found.")
                            networkInfo.status = CrackingStatus.FAIL
                            networkInfo.password = null // Clear password
                            _wifiList.postValue(wifiNetworkInfos)
                        } else {
                            _logMessages.postValue("Attack on ${networkInfo.ssid} cancelled.")
                        }
                    }
                    _logMessages.postValue("Cracking process finished for all networks.")
                    _isCracking.postValue(false)
                }
                is WifiScanResult.PermissionDenied -> {
                    _logMessages.postValue("ERROR: Permission denied to scan for Wi-Fi networks. Aborting.")
                    _isCracking.postValue(false)
                }
            }
        }
    }

    private fun stopCracking() {
        crackingJob?.cancel()
        _isCracking.value = false
        _logMessages.value = "Cracking process stopped by user."
    }

    fun onMapBoundsChanged(boundingBox: BoundingBox) {
        fetchAddressesJob?.cancel() // Cancel previous job
        fetchAddressesJob = viewModelScope.launch {
            delay(500) // Debounce for 500ms
            _logMessages.postValue("Fetching addresses in bounds...")
            val addresses = fetchAddressesInBounds(boundingBox)
            if (addresses.isNotEmpty()) {
                _logMessages.postValue("Found ${addresses.size} unique addresses in the visible area.")
                val passwords = addresses.flatMap { PasswordGenerator.generateAddressVariations(it) }.distinct()
                _passwordList.postValue(passwords)
                _logMessages.postValue("Generated ${passwords.size} password candidates.")
            } else {
                _logMessages.postValue("No addresses found in the visible area.")
            }
        }
    }

    private suspend fun fetchAddressesInBounds(boundingBox: BoundingBox): List<String> {
        return withContext(Dispatchers.IO) {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            val addresses = mutableSetOf<String>() // Use a set to avoid duplicates
            val latSpan = boundingBox.latNorth - boundingBox.latSouth
            val lonSpan = boundingBox.lonEast - boundingBox.lonWest

            // Create a 5x5 grid of points
            val gridSize = 5
            val latStep = latSpan / gridSize
            val lonStep = lonSpan / gridSize

            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    val lat = boundingBox.latSouth + (i * latStep)
                    val lon = boundingBox.lonWest + (j * lonStep)
                    try {
                        val geocoderAddresses = geocoder.getFromLocation(lat, lon, 1)
                        if (geocoderAddresses != null && geocoderAddresses.isNotEmpty()) {
                            val address = geocoderAddresses[0]
                            val addressLine = address.getAddressLine(0)
                            if (addressLine != null) {
                                addresses.add(addressLine)
                            }
                        }
                    } catch (e: IOException) {
                        // This can happen if the geocoder service is not available.
                        // We can log it, but we don't want to spam the user.
                        Log.w("MainViewModel", "Geocoder failed for point ($lat, $lon)", e)
                    }
                }
            }
            addresses.toList()
        }
    }
}
