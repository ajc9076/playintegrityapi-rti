package com.ajc9076.maliciousintegrityapp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ajc9076.maliciousintegrityapp.data.ServerStatus
import com.ajc9076.maliciousintegrityapp.ui.model.MainViewModel
import com.ajc9076.maliciousintegrityapp.ui.model.MainViewState
import com.ajc9076.maliciousintegrityapp.ui.theme.MaliciousIntegrityAppTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class MainActivity : ComponentActivity() {

    // set up variables to store location data
    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private var locationString: String = ""

    // check if we have access to location data
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // fine granted
                getLocation(Priority.PRIORITY_HIGH_ACCURACY)
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // coarse granted
                getLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            }
        }

    }

    // init function to start the application
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // create an object to call the integrity API
            val viewModel = MainViewModel()
            // set up our app to be stateful
            val state by viewModel.state.collectAsState()
            // set the theme/color of the app
            MaliciousIntegrityAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // call the function to display a UI to the user
                    DisplayLogin(state, viewModel)
                }
            }
        }
        // create a coroutine to request location
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    // after permission check, get the location data.
    // DO NOT call this function without checking permissions first.
    @SuppressLint("MissingPermission")
    private fun getLocation(priority: Int){
        // create the location object
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        // launch a coroutine to get the location and store it in a global variable
        val result = fusedLocationProvider?.getCurrentLocation(
            priority,
            CancellationTokenSource().token
        )
        result?.addOnSuccessListener{ fetchedLocation ->
            locationString =
                "LAT: ${fetchedLocation?.latitude} LONG: ${fetchedLocation?.longitude}"
        }
    }

    // UI of the application
    @Composable
    fun DisplayLogin(state: MainViewState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
        // select an image to display to the user
        val imageResource = when(state.serverState.status) {
            ServerStatus.INIT1 -> R.drawable.three_dots
            ServerStatus.INIT2 -> R.drawable.three_dots
            ServerStatus.READY -> R.drawable.malicious
            ServerStatus.WORKING -> R.drawable.three_dots
            ServerStatus.SUCCESS -> R.drawable.green_check
            else -> R.drawable.red_x
        }
        Surface(color = Color.Gray) {
            // align everything in a single, centered column
            Column (
                modifier = modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                // display the image selected earlier
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = "Valid App"
                )
                Spacer(modifier = Modifier.height(16.dp))
                // display the name of the app
                Text(
                    text = "Malicious Play Integrity Verification Tester",
                    modifier = modifier
                )
                Spacer(modifier = Modifier.height(16.dp))
                // display the location data
                Text(
                    text = "Attacker Location: $locationString",
                    modifier = modifier
                )
                Spacer(modifier = Modifier.height(16.dp))
                // depending on the state of the program, launch various coroutines and display text.
                when(state.serverState.status){
                    // cycle back and forth seeing if the location variable has been set yet
                    ServerStatus.INIT1 -> {
                        viewModel.waitForLocation(locationString)
                    }

                    ServerStatus.INIT2 -> {
                        viewModel.waitForLocation(locationString)
                    }

                    // once location is acquired, provide a button to start verification
                    ServerStatus.READY -> {
                        Button(onClick = {
                            viewModel.performCommand()
                        }) {
                            Text(stringResource(R.string.verify))
                        }
                    }
                    // after the button was pressed, display a loading screen
                    ServerStatus.WORKING -> {
                        Text(
                            text = "Loading results...",
                            modifier = modifier
                        )
                    }
                    // if the verification passed, indicate such and display the victim's location
                    ServerStatus.SUCCESS -> {
                        Text(
                            text = "Verification Passed All Checks",
                            modifier = modifier
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Victim Location: ${state.serverState.location}",
                            modifier = modifier
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.serverState.verdict,
                            modifier = modifier
                        )
                    }
                    // if the verification failed, tell the user why it failed and display a "try again" button
                    ServerStatus.FAILED -> {
                        Text(
                            text = "Verification Failed One or More Checks",
                            modifier = modifier
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.serverState.verdict,
                            modifier = modifier
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            viewModel.performCommand()
                        }) {
                            Text(stringResource(R.string.again))
                        }
                    }
                }
            }
        }
    }
}