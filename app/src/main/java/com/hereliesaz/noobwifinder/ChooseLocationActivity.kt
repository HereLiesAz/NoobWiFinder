package com.hereliesaz.noobwifinder

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hereliesaz.noobwifinder.databinding.ActivityChooseLocationBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.IOException

class ChooseLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChooseLocationBinding
    private lateinit var mapView: MapView
    private var selectedPoint: GeoPoint? = null
    private lateinit var locationMarker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        binding = ActivityChooseLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        setupButtons()
    }

    private fun setupMap() {
        mapView = binding.mapViewChoose
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(9.5)
        val startPoint = GeoPoint(48.858370, 2.294481) // Default to Paris
        mapController.setCenter(startPoint)

        locationMarker = Marker(mapView)
        locationMarker.isDraggable = true
        locationMarker.position = startPoint
        locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(locationMarker)
        selectedPoint = startPoint

        locationMarker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                selectedPoint = marker.position
            }
            override fun onMarkerDragStart(marker: Marker) {}
        })
    }

    private fun setupButtons() {
        binding.searchAddressButton.setOnClickListener {
            performSearch()
        }

        binding.cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.saveButton.setOnClickListener {
            selectedPoint?.let {
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_LATITUDE, it.latitude)
                resultIntent.putExtra(EXTRA_LONGITUDE, it.longitude)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } ?: run {
                Toast.makeText(this, "No location selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSearch() {
        val addressText = binding.searchAddressInput.text.toString()
        if (addressText.isBlank()) {
            Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val geocoder = Geocoder(this)
            val addresses = geocoder.getFromLocationName(addressText, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val geoPoint = GeoPoint(address.latitude, address.longitude)
                selectedPoint = geoPoint
                mapView.controller.animateTo(geoPoint)
                mapView.controller.setZoom(15.0)
                locationMarker.position = geoPoint
                mapView.invalidate()
            } else {
                Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Geocoder service not available", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_LATITUDE = "com.hereliesaz.noobwifinder.EXTRA_LATITUDE"
        const val EXTRA_LONGITUDE = "com.hereliesaz.noobwifinder.EXTRA_LONGITUDE"
    }
}
