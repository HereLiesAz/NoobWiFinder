package com.hereliesaz.dumbwifinder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hereliesaz.dumbwifinder.data.CrackingStatus
import com.hereliesaz.dumbwifinder.data.WifiNetworkInfo
import org.osmdroid.util.GeoPoint
import com.hereliesaz.dumbwifinder.services.LocationService
import com.hereliesaz.dumbwifinder.services.WifiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _wifiList = MutableLiveData<List<WifiNetworkInfo>>()
    val wifiList: LiveData<List<WifiNetworkInfo>> = _wifiList

    private val _logMessages = MutableLiveData<String>()
    val logMessages: LiveData<String> = _logMessages

    private val _isCracking = MutableLiveData<Boolean>(false)
    val isCracking: LiveData<Boolean> = _isCracking

    private var crackingJob: Job? = null

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

            val location = locationService.getCurrentLocation()
            if (location == null) {
                _logMessages.postValue("Could not get location.")
                _isCracking.postValue(false)
                return@launch
            }
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            _logMessages.postValue("Got location: ${geoPoint.latitude}, ${geoPoint.longitude}")

            val wifiNetworks = wifiService.scanForWifiNetworks()
            val wifiNetworkInfos = wifiNetworks.map {
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
                networkInfo.status = CrackingStatus.IN_PROGRESS
                _wifiList.postValue(wifiNetworkInfos)
                delay(1000) // Simulate work

                networkInfo.status = CrackingStatus.FAIL
                _wifiList.postValue(wifiNetworkInfos)
            }

            _logMessages.postValue("Cracking process finished.")
            _isCracking.postValue(false)
        }
    }

    private fun stopCracking() {
        crackingJob?.cancel()
        _isCracking.value = false
        _logMessages.value = "Cracking process stopped by user."
    }
}
