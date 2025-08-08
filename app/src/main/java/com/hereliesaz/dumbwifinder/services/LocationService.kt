package com.hereliesaz.dumbwifinder.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getNearbyAddresses(location: Location): List<Address> {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            10
                        ) { addresses ->
                            continuation.resume(addresses)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(location.latitude, location.longitude, 10) ?: emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
