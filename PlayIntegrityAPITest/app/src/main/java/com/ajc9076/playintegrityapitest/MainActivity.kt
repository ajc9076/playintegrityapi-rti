package com.ajc9076.playintegrityapitest

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ajc9076.playintegrityapitest.data.ServerStatus
import com.ajc9076.playintegrityapitest.ui.model.MainViewModel
import com.ajc9076.playintegrityapitest.ui.model.MainViewState
import com.ajc9076.playintegrityapitest.ui.theme.PlayIntegrityAPITestTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private var locationString: String = ""

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = MainViewModel()
            val state by viewModel.state.collectAsState()
            PlayIntegrityAPITestTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DisplayLogin(state, viewModel)
                }
            }
        }
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(priority: Int){
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        val result = fusedLocationProvider?.getCurrentLocation(
            priority,
            CancellationTokenSource().token
        )
        result?.addOnSuccessListener{ fetchedLocation ->
            locationString =
                "LAT: ${fetchedLocation?.latitude} LONG: ${fetchedLocation?.longitude}"
        }
    }

    @Composable
    fun DisplayLogin(state: MainViewState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val imageResource = when(state.serverState.status) {
            ServerStatus.INIT -> R.drawable.green_check
            ServerStatus.WORKING -> R.drawable.three_dots
            ServerStatus.SUCCESS -> R.drawable.green_check
            else -> R.drawable.red_x
        }
        Surface(color = Color.Gray) {
            Column (
                modifier = modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = "Valid App"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Play Integrity Verification Tester",
                    modifier = modifier
                )
                Spacer(modifier = Modifier.height(16.dp))

                when(state.serverState.status) {
                    ServerStatus.INIT -> {
                        Button(onClick = {
                            if (locationString == "") {
                                runBlocking {
                                    delay(5000)
                                }
                            }
                            viewModel.performCommand(context, locationString)
                        }) {
                            Text(stringResource(R.string.verify))
                        }
                    }

                    ServerStatus.WORKING -> {
                        Text(
                            text = "Loading results...",
                            modifier = modifier
                        )
                    }

                    ServerStatus.SUCCESS -> {
                        Text(
                            text = "Verification Passed All Checks",
                            modifier = modifier
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.serverState.verdict,
                            modifier = modifier
                        )
                    }

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
                            viewModel.performCommand(context, locationString)
                        }) {
                            Text(stringResource(R.string.again))
                        }
                    }
                }
            }
        }
    }
}



