package com.hereliesaz.noobwifinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.noobwifinder.data.WifiNetworkInfo
import com.hereliesaz.noobwifinder.services.LocationService
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.runtime.mutableIntStateOf
import com.hereliesaz.noobwifinder.R
import com.hereliesaz.noobwifinder.ui.theme.NoobWifiFinderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var locationService: LocationService

    private val locationPermissionRequest = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                locationService.startLocationUpdates()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                locationService.startLocationUpdates()
            } else -> {
                // No location access granted.
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationService = LocationService(this)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    MainScreen(
                        viewModel = viewModel,
                        locationService = locationService,
                        onChooseLocation = {
                            navController.navigate("choose_location")
                        }
                    )
                }
                composable("choose_location") {
                    ChooseLocationScreen(
                        onSave = { geoPoint ->
                            viewModel.onLocationSelected(geoPoint)
                            navController.popBackStack()
                        },
                        onCancel = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
        checkAndRequestLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationService.startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        locationService.stopLocationUpdates()
    }

    private fun checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    locationService: LocationService,
    onChooseLocation: () -> Unit
) {
    val wifiList by viewModel.wifiList.observeAsState(initial = emptyList())
    val passwordList by viewModel.passwordList.observeAsState(initial = emptyList())
    val logMessages by viewModel.logMessages.observeAsState(initial = "")
    val isCracking by viewModel.isCracking.observeAsState(initial = false)
    val isGeneratingFromLocation by viewModel.isGeneratingFromLocation.observeAsState(initial = false)
    val userLocation by locationService.locationUpdates.observeAsState()

    MainScreenContent(
        wifiList = wifiList,
        passwordList = passwordList,
        logMessages = logMessages,
        isCracking = isCracking,
        isGeneratingFromLocation = isGeneratingFromLocation,
        userLocation = userLocation,
        onChooseLocation = onChooseLocation,
        onStartStopClick = { viewModel.startStopCracking() }
    )
}

@Composable
fun MainScreenContent(
    wifiList: List<WifiNetworkInfo>,
    passwordList: List<String>,
    logMessages: String,
    isCracking: Boolean,
    isGeneratingFromLocation: Boolean,
    userLocation: Location?,
    onChooseLocation: () -> Unit,
    onStartStopClick: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(id = R.string.wifi),
        stringResource(id = R.string.passwords),
        stringResource(id = R.string.logs)
    )

    NoobWifiFinderTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Button(
                    onClick = onChooseLocation,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(stringResource(id = R.string.choose_location))
                }
                Button(
                    onClick = onStartStopClick,
                    enabled = !isGeneratingFromLocation,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(if (isCracking) stringResource(id = R.string.pause) else stringResource(id = R.string.scan))
                }
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    factory = { context ->
                        MapView(context).apply {
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            setOnTouchListener { _, _ -> true }
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()
                        wifiList.forEach { wifiInfo ->
                            val marker = Marker(mapView)
                            marker.position = wifiInfo.location
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = wifiInfo.ssid
                            marker.snippet = "Strength: ${wifiInfo.signalStrength} dBm"
                            mapView.overlays.add(marker)
                        }
                        userLocation?.let {
                            val geoPoint = GeoPoint(it.latitude, it.longitude)
                            mapView.controller.setCenter(geoPoint)
                        }
                        mapView.invalidate()
                    }
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(text = title) }
                        )
                    }
                }
                when (selectedTabIndex) {
                    0 -> WifiList(wifiList)
                    1 -> PasswordList(passwordList)
                    2 -> LogConsole(logMessages)
                }
            }
        }
    }
}

@Composable
fun WifiList(wifiList: List<WifiNetworkInfo>) {
    AnimatedVisibility(
        visible = wifiList.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(durationMillis = 1000)),
        exit = fadeOut(animationSpec = tween(durationMillis = 1000))
    ) {
        LazyColumn(modifier = Modifier.padding(8.dp)) {
            items(wifiList) { wifiNetwork ->
                WifiListItem(wifiNetwork)
            }
        }
    }
}

@Composable
fun PasswordList(passwordList: List<String>) {
    AnimatedVisibility(
        visible = passwordList.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(durationMillis = 1000)),
        exit = fadeOut(animationSpec = tween(durationMillis = 1000))
    ) {
        LazyColumn(modifier = Modifier.padding(8.dp)) {
            items(passwordList) { password ->
                PasswordListItem(password)
            }
        }
    }
}

@Composable
fun LogConsole(logMessages: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = logMessages)
    }
}

@Composable
fun PasswordListItem(password: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = password,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun WifiListItem(wifiNetwork: WifiNetworkInfo) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded }
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = wifiNetwork.ssid,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = stringResource(id = R.string.signal_strength, wifiNetwork.signalStrength),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic
                )
            )
            if (expanded) {
                Text(
                    text = stringResource(id = R.string.bssid, wifiNetwork.bssid),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WifiListItemPreview() {
    NoobWifiFinderTheme {
        WifiListItem(
            WifiNetworkInfo(
                "My Wifi",
                "00:11:22:33:44:55",
                -50,
                "WPA2",
                GeoPoint(0.0, 0.0)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val sampleWifiList = listOf(
        WifiNetworkInfo("Wifi 1", "00:11:22:33:44:55", -50, "WPA2", GeoPoint(0.0, 0.0)),
        WifiNetworkInfo("Wifi 2", "00:11:22:33:44:56", -75, "WEP", GeoPoint(0.0, 0.0)),
        WifiNetworkInfo("Wifi 3", "00:11:22:33:44:57", -90, "OPEN", GeoPoint(0.0, 0.0))
    )
    val samplePasswordList = listOf("password123", "12345678", "qwerty")
    val sampleLogMessages = "Log message 1\nLog message 2\nLog message 3"
    MainScreenContent(
        wifiList = sampleWifiList,
        passwordList = samplePasswordList,
        logMessages = sampleLogMessages,
        isCracking = false,
        isGeneratingFromLocation = false,
        userLocation = null,
        onChooseLocation = {},
        onStartStopClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PasswordListItemPreview() {
    NoobWifiFinderTheme {
        PasswordListItem("password123")
    }
}
