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
import android.util.TypedValue
import androidx.recyclerview.widget.LinearLayoutManager
import com.hereliesaz.noobwifinder.databinding.ActivityMainBinding
import com.hereliesaz.noobwifinder.services.LocationService
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import android.widget.ImageView
import androidx.core.view.GravityCompat
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.content.ContextCompat
import android.content.Intent
import org.osmdroid.util.BoundingBox

class MainActivity : AppCompatActivity() {

    private var currentSelection = SelectionState.DEFAULT

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var wifiListAdapter: WifiListAdapter
    private lateinit var passwordListAdapter: PasswordListAdapter
    private lateinit var mapView: MapView
    private lateinit var locationService: LocationService

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

    private val chooseLocationLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let {
                val lat = it.getDoubleExtra(ChooseLocationActivity.EXTRA_LATITUDE, 0.0)
                val lon = it.getDoubleExtra(ChooseLocationActivity.EXTRA_LONGITUDE, 0.0)
                if (lat != 0.0 && lon != 0.0) {
                    val point = GeoPoint(lat, lon)
                    val boundingBox = BoundingBox(
                        point.latitude + 0.009,
                        point.longitude + 0.009,
                        point.latitude - 0.009,
                        point.longitude - 0.009
                    )
                    viewModel.onMapBoundsChanged(boundingBox)
                    mapView.controller.setCenter(point)
                }
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.mipmap.ic_launcher)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navView.getHeaderView(0).findViewById<ImageView>(R.id.nav_header_icon).setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_choose_location -> {
                    val intent = Intent(this, ChooseLocationActivity::class.java)
                    chooseLocationLauncher.launch(intent)
                    true
                }
                else -> false
            }
        }

        locationService = LocationService(this)

        mapView = binding.mapView
        mapView.setMultiTouchControls(false)
        mapView.setOnTouchListener { _, _ -> true }

        setupRecyclerViews()
        observeViewModel()
        observeLocationUpdates()
        checkAndRequestLocationPermission()
        setupMap()


        binding.startStopButton.setOnClickListener {
            viewModel.startStopCracking()
        }

        binding.mapCard.setOnClickListener {
            viewModel.startStopCracking()
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.wifiListCard.setOnClickListener {
            handleSelection(SelectionState.WIFI)
        }
        binding.passwordListCard.setOnClickListener {
            handleSelection(SelectionState.PASSWORD)
        }
        binding.logCard.setOnClickListener {
            handleSelection(SelectionState.LOG)
        }
    }

    private fun handleSelection(selection: SelectionState) {
        val targetState = if (currentSelection == selection) {
            SelectionState.DEFAULT
        } else {
            selection
        }

        // Handle text size change before transition
        if (targetState == SelectionState.LOG) {
            binding.logConsole.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        } else {
            binding.logConsole.setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Body1)
        }

        currentSelection = targetState

        val transitionId = when (targetState) {
            SelectionState.DEFAULT -> R.id.default_set
            SelectionState.WIFI -> R.id.wifi_selected
            SelectionState.PASSWORD -> R.id.password_selected
            SelectionState.LOG -> R.id.log_selected
        }
        binding.rootLayout.transitionToState(transitionId)
    }

    private fun setupRecyclerViews() {
        wifiListAdapter = WifiListAdapter(emptyList())
        binding.wifiList.adapter = wifiListAdapter
        binding.wifiList.layoutManager = LinearLayoutManager(this)

        passwordListAdapter = PasswordListAdapter(emptyList())
        binding.passwordList.adapter = passwordListAdapter
        binding.passwordList.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        viewModel.wifiList.observe(this) { wifiList ->
            wifiListAdapter.updateData(wifiList)

            val markerMap = mapView.getTag(R.id.marker_map) as? MutableMap<String, Marker>
                ?: mutableMapOf<String, Marker>().also {
                    mapView.setTag(R.id.marker_map, it)
                }

            val wifiSsids = wifiList.map { it.ssid }.toSet()
            val obsoleteMarkers = markerMap.keys.filter { it !in wifiSsids }
            obsoleteMarkers.forEach { ssid ->
                val marker = markerMap[ssid]
                if (marker != null) {
                    mapView.overlays.remove(marker)
                    markerMap.remove(ssid)
                }
            }

            wifiList.forEach { wifiInfo ->
                val existingMarker = markerMap[wifiInfo.ssid]
                if (existingMarker != null) {
                    if (existingMarker.position != wifiInfo.location ||
                        existingMarker.snippet != "Strength: ${wifiInfo.signalStrength} dBm") {
                        existingMarker.position = wifiInfo.location
                        existingMarker.snippet = "Strength: ${wifiInfo.signalStrength} dBm"
                    }
                } else {
                    val marker = Marker(mapView)
                    marker.position = wifiInfo.location
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = wifiInfo.ssid
                    marker.snippet = "Strength: ${wifiInfo.signalStrength} dBm"
                    mapView.overlays.add(marker)
                    markerMap[wifiInfo.ssid] = marker
                }
            }
            mapView.invalidate()
        }

        viewModel.passwordList.observe(this) { passwords ->
            passwordListAdapter.updateData(passwords)
        }

        viewModel.logMessages.observe(this) { message ->
            binding.logConsole.append("$message\n")
        }

        viewModel.isCracking.observe(this) { isCracking ->
            if (isCracking) {
                binding.startStopButton.text = "Pause"
                binding.startStopButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.muted_red)
            } else {
                binding.startStopButton.text = "Scan"
                binding.startStopButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.muted_green)
            }
        }

        viewModel.isGeneratingFromLocation.observe(this) { isGenerating ->
            binding.startStopButton.isEnabled = !isGenerating
            binding.mapCard.isEnabled = !isGenerating
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
        mapController.setZoom(20.0)

        val startPoint = GeoPoint(48.858370, 2.294481);
        mapController.setCenter(startPoint);
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                val userLocation = GeoPoint(lastKnownLocation.latitude, lastKnownLocation.longitude)
                mapView.controller.setCenter(userLocation)
            }
        }
    }

    private fun observeLocationUpdates() {
        locationService.locationUpdates.observe(this) { location ->
            val userLocation = GeoPoint(location.latitude, location.longitude)
            mapView.controller.animateTo(userLocation)
            mapView.controller.setZoom(20.0)
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
}
