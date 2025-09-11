package com.mortonfox.gpstest

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mortonfox.gpstest.ui.theme.GpstestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GpstestTheme(darkTheme = true) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        MainScreen(this)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(context: Context) {
    // For error messages or the coordinates.
    var locationInfo by remember { mutableStateOf("No location info") }

    // This is for turning on and off the permissions requestor.
    var requestPerms by remember { mutableStateOf(false) }

    Column {
        Button(
            onClick = {
                if (arePermissionsGranted(context)) {
                    // Already have permissions. Go directly to location query.
                    getLocation(
                        context = context,
                        onSuccess = { lat, lon ->
                            locationInfo = "Coordinates: $lat, $lon"
                        },
                        onFailure = { ex ->
                            locationInfo = "Failed to get location: $ex"
                        }
                    )
                } else {
                    // Otherwise we have to trigger the permissions requestor.
                    requestPerms = true
                }
            }
        ) {
            Text(text = "Get Location")
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = locationInfo
        )
    }

    if (requestPerms) {
        RequestLocationPermissions(
            onGranted = {
                getLocation(
                    context = context,
                    onSuccess = { lat, lon ->
                        locationInfo = "Coordinates: $lat, $lon"
                    },
                    onFailure = { ex ->
                        locationInfo = "Failed to get location: $ex"
                    }
                )
                requestPerms = false
            },
            onDenied = {
                locationInfo = "Location permissions denied"
                requestPerms = false
            }
        )
    }
}

fun arePermissionsGranted(context: Context): Boolean {
    // For some reason, we can't have only fine location access. We need both.
    return arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

@SuppressLint("MissingPermission")
fun getLocation(
    context: Context,
    onSuccess: (lat: Double, lon: Double) -> Unit,
    onFailure: (ex: Exception) -> Unit
) {
    val client = LocationServices.getFusedLocationProviderClient(context)

    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener {
            // The location result is nullable. So that could happen.
            if (it == null) {
                onFailure(RuntimeException("null location result"))
            } else {
                onSuccess(it.latitude, it.longitude)
            }
        }
        .addOnFailureListener(onFailure)
}

@Composable
fun RequestLocationPermissions(
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    // This launcher will pop up a dialog asking the user for location permissions.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        if (allGranted) {
            onGranted()
        } else {
            onDenied()
        }
    }

    LaunchedEffect(Unit) {
        // For some reason, we can't ask for only fine location access. We need to have both.
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}
