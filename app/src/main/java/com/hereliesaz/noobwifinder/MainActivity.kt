package com.hereliesaz.noobwifinder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.GravityCompat
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

import com.google.android.material.button.MaterialButton
import androidx.core.content.ContextCompat

import android.content.Intent
import android.content.SharedPreferences
import com.google.android.material.navigation.NavigationView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import android.util.Log

class MainActivity : AppCompatActivity() {

    private enum class SelectionState {
        DEFAULT,
        WIFI,
        PASSWORD,
        LOG
    }
    private val debounceHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null
    private val debounceDelayMs = 300L

    private var currentSelection = SelectionState.DEFAULT
    private var pendingSelection: SelectionState? = null

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var wifiListAdapter: WifiListAdapter
    private lateinit var passwordListAdapter: PasswordListAdapter
    private lateinit var mapView: MapView
    private lateinit var locationService: LocationService
    private var isManualLocationMode = false
    private lateinit var sharedPrefs: SharedPreferences

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
                    val manualPoint = GeoPoint(lat, lon)
                    setManualLocation(manualPoint)
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

        sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        isManualLocationMode = sharedPrefs.getBoolean("manual_mode", false)

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
                    if (isManualLocationMode) {
                        setAutomaticLocationMode()
                    } else {
                        val intent = Intent(this, ChooseLocationActivity::class.java)
                        chooseLocationLauncher.launch(intent)
                    }
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

        setupClickListeners()

        binding.rootLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
            }

            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == R.id.default_set && pendingSelection != null) {
                    val selection = pendingSelection!!
                    pendingSelection = null
                    handleSelection(selection)
                }
            }

            override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {
            }
        })
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

        if (currentSelection != SelectionState.DEFAULT && targetState != SelectionState.DEFAULT) {
            // A different card is selected. First, go back to default.
            pendingSelection = targetState
            binding.rootLayout.transitionToState(R.id.default_set)
        } else {
            // Handle text size change before transition
            if (targetState == SelectionState.LOG) {
                binding.logConsole.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            } else {
                binding.logConsole.setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Body1)
            }
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
    }

    private fun checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    private val debounceHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null
    private val debounceDelayMs = 300L

    private fun handleMapChange() {
        val boundingBox = mapView.boundingBox
        Log.d("MainActivity", "Map bounds changed: $boundingBox")

        // Debounce logic: cancel previous runnable and post a new one
        debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
        debounceRunnable = Runnable {
            viewModel.onMapBoundsChanged(boundingBox)
        }
        debounceHandler.postDelayed(debounceRunnable!!, debounceDelayMs)
    }

    private fun handleMapChange() {
        val boundingBox = mapView.boundingBox
        Log.d("MainActivity", "Map bounds changed: $boundingBox")
        // TODO: Debounce this call to avoid excessive processing
        viewModel.onMapBoundsChanged(boundingBox)
    }

    private fun setupMap() {
        val mapController = mapView.controller
        mapController.setZoom(20.0)

        if (isManualLocationMode) {
            val lat = sharedPrefs.getFloat("manual_lat", 0.0f).toDouble()
            val lon = sharedPrefs.getFloat("manual_lon", 0.0f).toDouble()
            if (lat != 0.0 && lon != 0.0) {
                mapController.setCenter(GeoPoint(lat, lon))
            }
            updateMenuForManualMode(true)
        } else {
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
            updateMenuForManualMode(false)
        }

        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                handleMapChange()
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                handleMapChange()
                return true
            }
        })
    }

    private fun observeLocationUpdates() {
        locationService.locationUpdates.observe(this) { location ->
            if (!isManualLocationMode) {
                val userLocation = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(userLocation)
                mapView.controller.setZoom(20.0)
            }
        }
    }

    private fun setManualLocation(point: GeoPoint) {
        isManualLocationMode = true
        locationService.stopLocationUpdates()
        mapView.controller.setCenter(point)
        with(sharedPrefs.edit()) {
            putBoolean("manual_mode", true)
            putFloat("manual_lat", point.latitude.toFloat())
            putFloat("manual_lon", point.longitude.toFloat())
            apply()
        }
        updateMenuForManualMode(true)
    }

    private fun setAutomaticLocationMode() {
        isManualLocationMode = false
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationService.startLocationUpdates()
        }
        with(sharedPrefs.edit()) {
            putBoolean("manual_mode", false)
            apply()
        }
        updateMenuForManualMode(false)
    }

    private fun updateMenuForManualMode(isManual: Boolean) {
        val menuItem = binding.navView.menu.findItem(R.id.nav_choose_location)
        menuItem.title = if (isManual) "Auto-Locate" else "Choose Location"
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (!isManualLocationMode && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            locationService.startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationService.stopLocationUpdates()
    }


