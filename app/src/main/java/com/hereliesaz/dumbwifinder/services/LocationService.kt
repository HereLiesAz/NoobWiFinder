package com.hereliesaz.dumbwifinder.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.location.LocationListener

class LocationService(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val geocoder = Geocoder(context, Locale.getDefault())
    private val _locationUpdates = MutableLiveData<Location>()
    val locationUpdates: LiveData<Location> = _locationUpdates

    private val locationListener = LocationListener { location ->
        _locationUpdates.postValue(location)
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, locationListener)
        } catch (e: Exception) {
            // Handle exception
        }
    }

    fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return withContext(Dispatchers.IO) {
            try {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getNearbyAddresses(location: Location): List<Address> {
        return withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocation(location.latitude, location.longitude, 10) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
