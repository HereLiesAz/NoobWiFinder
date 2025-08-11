package com.hereliesaz.noobwifinder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.osmdroid.util.GeoPoint

class ChooseLocationViewModel : ViewModel() {
    private val _selectedPoint = MutableLiveData<GeoPoint?>()
    val selectedPoint: LiveData<GeoPoint?> = _selectedPoint

    fun setSelectedPoint(geoPoint: GeoPoint?) {
        _selectedPoint.value = geoPoint
    }
}
