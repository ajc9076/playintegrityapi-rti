package com.ajc9076.playintegrityapitest

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ajc9076.playintegrityapitest.data.CommandResult
import com.ajc9076.playintegrityapitest.data.IntegrityRandom
import com.ajc9076.playintegrityapitest.data.ServerCommand
import com.ajc9076.playintegrityapitest.ui.theme.PlayIntegrityAPITestTheme
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlayIntegrityAPITestTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DisplayLogin()
                }
            }
        }
    }
}

@Composable
fun DisplayLogin(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var result by remember { mutableStateOf(1) }
    val imageResource = when(result) {
        1 -> R.drawable.green_check
        2 -> R.drawable.three_dots
        3 -> R.drawable.green_check
        else -> R.drawable.red_x
    }
    var computedIntegrityResult by remember { mutableStateOf(CommandResult(false, "", "")) }
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
            if (result == 1){
                Button(onClick = {
                    result = 2
                }) {
                    Text(stringResource(R.string.verify))
                }
            }
            else if (result == 2){
                Text(
                    text = "Loading results...",
                    modifier = modifier
                )
                result = 5
            }
            else if (result == 3){
                Text(
                    text = "Verification Passed All Checks",
                    modifier = modifier
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = computedIntegrityResult.diagnosticMessage,
                    modifier = modifier
                )
            }
            else if (result == 5){
                runBlocking {
                    computedIntegrityResult = computeResultAndParse(context)
                }
                if (computedIntegrityResult.commandSuccess) {
                    result = 3
                } else {
                    result = 4
                }
            }
            else {
                Text(
                    text = "Verification Failed One or More Checks",
                    modifier = modifier
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = computedIntegrityResult.diagnosticMessage,
                    modifier = modifier
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    result = 1
                    computedIntegrityResult = CommandResult(false, "", "")
                }) {
                    Text(stringResource(R.string.again))
                }
            }
        }
    }
}

suspend fun computeResultAndParse(context: Context): CommandResult {
    var commandResult = CommandResult(false, "Failed", "")
    // set up httpClient
    val httpClient: HttpClient = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.ALL
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20000
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer()
            acceptContentTypes = acceptContentTypes + ContentType.Any
        }
    }

    // get nonce from server
    var integrityRandom = IntegrityRandom("", 0U)
    try {
        val returnedRandom = httpClient.get<IntegrityRandom>("http://periodicgaming.ddns.net:8085/getRandom")
        integrityRandom = returnedRandom
    } catch (t: Throwable){
        Log.d("PlayIntegrityAPITest", "requestRandom exception " + t.message)
    }

    // create final nonce string appended to the actual command
    var nonceString = ""
    if(integrityRandom.random.isNotEmpty()){
        nonceString = GenerateNonce.generateNonceString("Log me in please!",
            integrityRandom.random)
    }
    // Create an instance of an IntegrityManager
    val integrityManager = IntegrityManagerFactory.create(context)
    // Use the nonce to configure a request for an integrity token
    if (nonceString != "") {
        try {
            val integrityTokenResponse: Task<IntegrityTokenResponse> =
                integrityManager.requestIntegrityToken(
                    IntegrityTokenRequest.builder()
                        .setNonce(nonceString)
                        .build()
                )
            // Wait for the integrity token to be generated
            integrityTokenResponse.addOnSuccessListener { integrityTokenResponse1 ->
                val integrityToken = integrityTokenResponse1.token()
                runBlocking {
                    commandResult = getTokenResponse(integrityTokenResponse, integrityToken, httpClient)
                }
            }
            // check if it failed
            integrityTokenResponse.addOnFailureListener { e ->
                Log.d("PlayIntegrityAPITest", "tokenGeneration exception " + e.message)
            }
        } catch (t: Throwable) {
            Log.d("PlayIntegrityAPITest", "requestIntegrityToken exception " + t.message)
        }
    }
    return commandResult
}

suspend fun getTokenResponse(integrityTokenResponse: Task<IntegrityTokenResponse>, integrityToken: String, httpClient: HttpClient): CommandResult{
    if (integrityTokenResponse.isSuccessful && integrityTokenResponse.result != null) {
        // Post the received token to our server
        try {
            val commandResult = httpClient.post<CommandResult>(
                "http://periodicgaming.ddns.net:8085/performCommand"
            ) {
                contentType(ContentType.Application.Json)
                body = ServerCommand(
                    "Log me in please!", integrityToken
                )
            }
            return commandResult
        } catch (t: Throwable) {
            Log.d("PlayIntegrityAPITest", "performCommand exception " + t.message)
        }
    } else {
        Log.d(
            "PlayIntegrityAPITest", "requestIntegrityToken failed: " +
                    integrityTokenResponse.result.toString()
        )
    }
    return CommandResult(false, "getTokenResponse failed", "")
}

fun ByteArray.toHexString(): String = joinToString(separator = "") {
        currentByte -> "%02x".format(currentByte)
}

object GenerateNonce {
    // Generate a nonce for Play Integrity using the following steps:
    // 1. Generate a SHA-256 hash of the command string
    // 2. Convert the hash value to a hex string
    // 3. Take the random number string from the server and append the hash
    // hex string to it to create the nonce string
    // Play Integrity expects a URL encoded, non-padded Base64 string,
    // our hex string is a valid Base64 string, even though we don't actually
    // need to encode/decode it.
    fun generateNonceString(commandString: String, randomString: String) : String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val commandHashBytes = messageDigest.digest(
            commandString.toByteArray(Charsets.UTF_8))
        val commandHashString = commandHashBytes.toHexString()
        val nonceString = randomString + commandHashString
        Log.d("PlayIntegrityAPITest", "nonce: $nonceString")
        return nonceString
    }
}

@Preview(showBackground = true)
@Composable
fun LoginAndVerifyApp() {
    PlayIntegrityAPITestTheme {
        DisplayLogin()
    }
}

