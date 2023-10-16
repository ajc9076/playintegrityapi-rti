package com.ajc9076.playintegrityapitest.ui.model

import android.content.Context
import android.util.Log
import com.ajc9076.playintegrityapitest.data.CommandResult
import com.ajc9076.playintegrityapitest.data.GenerateNonce
import com.ajc9076.playintegrityapitest.data.IntegrityRandom
import com.ajc9076.playintegrityapitest.data.ServerCommand
import com.ajc9076.playintegrityapitest.data.ServerState
import com.ajc9076.playintegrityapitest.data.ServerStatus
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

class CheckIntegrity() {
    private val TAG = "PlayIntegrityAPITest"
    // purposely insecure http instead of https for demonstration purposes
    private val URL = "http://play-integrity-9xfidw6bru2nqvd.ue.r.appspot.com"

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

    suspend fun computeResultAndParse(context: Context, locationString: String) {
        val commandString = "Log in with location: $locationString"

        _serverState.emit(ServerState(ServerStatus.WORKING, "", false))

        // get nonce from server
        var integrityRandom = IntegrityRandom("", 0U)
        try {
            val returnedRandom = httpClient.get<IntegrityRandom>("$URL/getRandom")
            integrityRandom = returnedRandom
        } catch (t: Throwable){
            Log.d(TAG, "requestRandom exception " + t.message)
            _serverState.tryEmit(ServerState(ServerStatus.FAILED, "requestRandom exception " + t.message))
        }

        // create final nonce string appended to the actual command
        var nonceString = ""
        if(integrityRandom.random.isNotEmpty()){
            nonceString = GenerateNonce.generateNonceString(commandString,
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
                        val commandResult = getTokenResponse(integrityTokenResponse, integrityToken, httpClient, commandString)
                        if (commandResult.commandSuccess) {
                            _serverState.tryEmit(
                                ServerState(
                                    ServerStatus.SUCCESS,
                                    commandResult.diagnosticMessage,
                                    true
                                )
                            )
                        } else {
                            _serverState.tryEmit(ServerState(ServerStatus.FAILED, "", false))
                        }
                    }
                }
                // check if it failed
                integrityTokenResponse.addOnFailureListener { e ->
                    Log.d(TAG, "tokenGeneration exception " + e.message)
                    _serverState.tryEmit(ServerState(ServerStatus.FAILED, "tokenGeneration exception " + e.message))
                }
            } catch (t: Throwable) {
                Log.d(TAG, "requestIntegrityToken exception " + t.message)
                _serverState.tryEmit(ServerState(ServerStatus.FAILED, "requestIntegrityToken exception " + t.message))
            }
        }
    }

    private suspend fun getTokenResponse(integrityTokenResponse: Task<IntegrityTokenResponse>, integrityToken: String, httpClient: HttpClient, commandString: String): CommandResult{
        if (integrityTokenResponse.isSuccessful && integrityTokenResponse.result != null) {
            // Post the received token to our server
            try {
                val commandResult = httpClient.post<CommandResult>("$URL/performCommand") {
                    contentType(ContentType.Application.Json)
                    body = ServerCommand(
                        commandString, integrityToken
                    )
                }
                return commandResult
            } catch (t: Throwable) {
                Log.d(TAG, "performCommand exception " + t.message)
            }
        } else {
            Log.d(TAG, "requestIntegrityToken failed: " + integrityTokenResponse.result.toString())
        }
        return CommandResult(false, "getTokenResponse failed", "")
    }
}