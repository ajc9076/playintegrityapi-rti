package com.ajc9076.maliciousintegrityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.ajc9076.maliciousintegrityapp.data.ServerStatus
import com.ajc9076.maliciousintegrityapp.ui.model.MainViewModel
import com.ajc9076.maliciousintegrityapp.ui.model.MainViewState
import com.ajc9076.maliciousintegrityapp.ui.theme.MaliciousIntegrityAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = MainViewModel()
            val state by viewModel.state.collectAsState()
            MaliciousIntegrityAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DisplayLogin(state, viewModel)
                }
            }
        }
    }
}

@Composable
fun DisplayLogin(state: MainViewState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageResource = when(state.serverState.status) {
        ServerStatus.INIT -> R.drawable.malicious
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
            if (state.serverState.status == ServerStatus.INIT){
                Button(onClick = {
                    viewModel.performCommand(context)
                }) {
                    Text(stringResource(R.string.verify))
                }
            }
            else if (state.serverState.status == ServerStatus.WORKING){
                Text(
                    text = "Loading results...",
                    modifier = modifier
                )
            }
            else if (state.serverState.status == ServerStatus.SUCCESS){
                Text(
                    text = "Verification Passed All Checks",
                    modifier = modifier
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    //text = computedIntegrityResult.diagnosticMessage,
                    text = state.serverState.verdict,
                    modifier = modifier
                )
            }
            else {
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
                    viewModel.performCommand(context)
                }) {
                    Text(stringResource(R.string.again))
                }
            }
        }
    }
}