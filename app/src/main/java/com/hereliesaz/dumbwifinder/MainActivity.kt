package com.hereliesaz.dumbwifinder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var wifiListView: RecyclerView
    private lateinit var startStopButton: Button
    private var googleMap: GoogleMap? = null
    private val viewModel: MainViewModel by viewModels()
    private lateinit var wifiListAdapter: WifiListAdapter

    private val locationPermissionRequest = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Fine location access granted.
                setupMap()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                setupMap()
            } else -> {
                // No location access granted.
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiListView = findViewById(R.id.wifi_list)
        startStopButton = findViewById(R.id.start_stop_button)

        setupRecyclerView()
        observeViewModel()
        requestLocationPermission()

        startStopButton.setOnClickListener {
            viewModel.startStopCracking()
        }
    }

    private fun setupRecyclerView() {
        wifiListAdapter = WifiListAdapter(emptyList())
        wifiListView.adapter = wifiListAdapter
        wifiListView.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        viewModel.wifiList.observe(this) {
            wifiListAdapter.updateData(it)
        }

        viewModel.logMessages.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.isCracking.observe(this) { isCracking ->
            startStopButton.text = if (isCracking) "Stop" else "Start"
        }
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun setupMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map_container) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
        }
    }
}
