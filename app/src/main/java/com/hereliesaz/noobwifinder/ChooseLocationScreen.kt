package com.hereliesaz.noobwifinder

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.views.MapView
import android.location.Geocoder
import androidx.compose.ui.platform.LocalContext
import org.osmdroid.util.GeoPoint
import androidx.compose.ui.res.stringResource
import com.hereliesaz.noobwifinder.R
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChooseLocationScreen(
    onSave: (GeoPoint) -> Unit,
    onCancel: () -> Unit
) {
    var address by remember { mutableStateOf("") }
    val context = LocalContext.current
    val geocoder = Geocoder(context)
    var location by remember { mutableStateOf<GeoPoint?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(stringResource(id = R.string.enter_address)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row {
            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        val addresses = geocoder.getFromLocationName(address, 1)
                        if (addresses?.isNotEmpty() == true) {
                            val newLocation = addresses[0]
                            withContext(Dispatchers.Main) {
                                location = GeoPoint(newLocation.latitude, newLocation.longitude)
                            }
                        }
                    } catch (e: Exception) {
                        // Handle exception
                    }
                }
            }) {
                Text(stringResource(id = R.string.search))
            }
            Button(onClick = onCancel) {
                Text(stringResource(id = R.string.cancel))
            }
            Button(onClick = { location?.let { onSave(it) } }) {
                Text(stringResource(id = R.string.save))
            }
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            factory = {
                MapView(it).apply {
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                }
            },
            update = { mapView ->
                location?.let {
                    mapView.controller.setCenter(it)
                    mapView.overlays.clear()
                    val marker = org.osmdroid.views.overlay.Marker(mapView)
                    marker.position = it
                    marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(marker)
                    mapView.invalidate()
                }
            }
        )
    }
}
