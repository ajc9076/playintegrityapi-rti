package com.ajc9076.maliciousintegrityapp.ui.model

import android.util.Log
import com.ajc9076.maliciousintegrityapp.data.ServerState
import com.ajc9076.maliciousintegrityapp.data.ServerStatus
import com.ajc9076.maliciousintegrityapp.data.CommandResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CheckIntegrity {
    private val TAG = "PlayIntegrityAPITest"
    private val URL = "https://play-integrity-9xfidw6bru2nqvd.ue.r.appspot.com"

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

    private val _serverState: MutableStateFlow<ServerState> =
        MutableStateFlow(ServerState(ServerStatus.INIT))
    val serverState = _serverState.asStateFlow()

    suspend fun computeResultAndParse(){
        _serverState.emit(ServerState(ServerStatus.WORKING, "", false))
        val integrityToken = ""

        // create server socket to receive communication from participating device
        // might use another server that will handle this since its hard lol


        // communicate the integrity token to the server
        try {
            val result = httpClient.post<CommandResult>("$URL/performCommand"){
                contentType(ContentType.Application.Json)
                body = ServerCommand(
                    "Log me in please!", integrityToken
                )
            }
            _serverState.emit(ServerState(ServerStatus.SUCCESS, result.diagnosticMessage, true))
        } catch (t: Throwable){
            Log.d(TAG, "performCommand exception " + t.message)
            _serverState.emit(ServerState(ServerStatus.FAILED, "performCommand exception " + t.message, false))
        }
    }
}