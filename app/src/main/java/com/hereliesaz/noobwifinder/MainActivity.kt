package com.hereliesaz.noobwifinder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.content.Context
import android.location.LocationManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hereliesaz.noobwifinder.databinding.ActivityMainBinding
import com.hereliesaz.noobwifinder.services.LocationService
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var wifiListAdapter: WifiListAdapter
    private lateinit var mapView: MapView
    private lateinit var locationService: LocationService
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private val locationPermissionRequest = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Fine location access granted.
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
            } else -> {
                // No location access granted.
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        drawerToggle = ActionBarDrawerToggle(this, binding.drawerLayout, R.string.drawer_open, R.string.drawer_close)
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.navView.getHeaderView(0).findViewById<ImageView>(R.id.nav_header_icon).setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }


        locationService = LocationService(this)

        mapView = binding.mapView
        mapView.setMultiTouchControls(true)

        setupRecyclerView()
        observeViewModel()
        observeLocationUpdates()
        checkAndRequestLocationPermission()
        setupMap()


        binding.startStopButton.setOnClickListener {
            viewModel.startStopCracking()
        }
    }

    private fun setupRecyclerView() {
        wifiListAdapter = WifiListAdapter(emptyList())
        binding.wifiList.adapter = wifiListAdapter
        binding.wifiList.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        viewModel.wifiList.observe(this) { wifiList ->
            wifiListAdapter.updateData(wifiList)
            mapView.overlays.clear()
            wifiList.forEach { wifiInfo ->
                val marker = Marker(mapView)
                marker.position = wifiInfo.location
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = wifiInfo.ssid
                marker.snippet = "Strength: ${wifiInfo.signalStrength} dBm"
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }

        viewModel.logMessages.observe(this) { message ->
            binding.logConsole.append("$message\n")
        }

        viewModel.isCracking.observe(this) { isCracking ->
            binding.startStopButton.text = if (isCracking) "Stop" else "Start"
        }
    }

    private fun checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun setupMap() {
        val mapController = mapView.controller
        mapController.setZoom(9.5)
        val startPoint = GeoPoint(48.858370, 2.294481);
        mapController.setCenter(startPoint);

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                val userLocation = GeoPoint(lastKnownLocation.latitude, lastKnownLocation.longitude)
                mapView.controller.setCenter(userLocation)
                mapView.controller.setZoom(17.0)
            }
        }
    }

    private fun observeLocationUpdates() {
        locationService.locationUpdates.observe(this) { location ->
            val userLocation = GeoPoint(location.latitude, location.longitude)
            mapView.controller.animateTo(userLocation)
            mapView.controller.setZoom(17.0)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationService.startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationService.stopLocationUpdates()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
