package com.ajc9076.maliciousintegrityapp.ui.model

import android.content.Context
import com.ajc9076.maliciousintegrityapp.data.ServerState
import com.ajc9076.maliciousintegrityapp.data.ServerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    suspend fun computeResultAndParse(context: Context){
        _serverState.emit(ServerState(ServerStatus.FAILED, "", false))

    }
}